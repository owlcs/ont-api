package ru.avicomp.ontapi.jena.impl;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.jena.graph.FrontsNode;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.StatementImpl;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * An Ont Statement.
 * This is extended Jena {@link Statement} with possibility to add annotations in the same form of ont-statement.
 * Annotations could be plain (annotation assertion) or bulk (anonymous resource with rdf:type owl:Axiom or owl:Annotation).
 * The examples of how to write bulk-annotations in RDF-graph see here:
 * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.2 Translation of Annotations</a>
 * <p>
 * Created by @szuev on 12.11.2016.
 */
public class OntStatementImpl extends StatementImpl implements OntStatement {

    public OntStatementImpl(Resource subject, Property predicate, RDFNode object, OntGraphModel model) {
        super(subject, predicate, object, (ModelCom) model);
    }

    @Override
    public OntGraphModelImpl getModel() {
        return (OntGraphModelImpl) super.getModel();
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public boolean isLocal() {
        return !getModel().getGraph().getUnderlying().hasSubGraphs() || getModel().isInBaseModel(this);
    }

    @Override
    public OntObject getSubject() {
        return super.getSubject().as(OntObject.class);
    }

    @Override
    public OntStatement addAnnotation(OntNAP property, RDFNode value) {
        checkAnnotationInput(property, value);
        Resource type = getAnnotationRootType(getSubject());
        Resource root = findAnnotationObject(this, type);
        if (root == null) {
            root = createAnnotationObject(this, type);
        }
        root.addProperty(property, value);
        return new OntStatementImpl(root, property, value, getModel());
    }

    @Override
    public Stream<OntStatement> annotations() {
        return resource().map(OntAnnotation::assertions).orElse(Stream.empty());
    }

    @Override
    public boolean hasAnnotations() {
        Optional<OntAnnotation> root = resource();
        return root.isPresent() && root.get().assertions().count() > 0;
    }

    @Override
    public void deleteAnnotation(OntNAP property, RDFNode value) {
        checkAnnotationInput(property, value);
        Optional<OntAnnotation> root = resource();
        if (!root.isPresent()) return;
        if (getModel().contains(root.get(), property, value)) {
            OntStatement res = new OntStatementImpl(root.get(), property, value, getModel());
            if (res.hasAnnotations()) {
                throw new OntJenaException("Can't delete " + res + ": it has children");
            }
            getModel().removeAll(root.get(), property, value);
        }
        if (root.get().assertions().count() == 0) { // if no children remove whole parent section.
            getModel().removeAll(root.get(), null, null);
        }
    }

    protected void checkAnnotationInput(OntNAP property, RDFNode value) {
        OntJenaException.notNull(property, "Null property.");
        if (OntJenaException.notNull(value, "Null value.").isResource()) {
            if (value.isURIResource()) return;
            if (value.canAs(OntIndividual.Anonymous.class)) return;
            throw new OntJenaException("Incorrect resource specified " + value + ": should be either uri-resource or anonymous individual.");
        } else if (value.isLiteral()) {
            return;
        }
        throw new OntJenaException("Should never happen.");
    }

    public Optional<OntAnnotation> resource() {
        return Optional.ofNullable(findAnnotationObject(this, getAnnotationRootType(getSubject())));
    }

    /**
     * Finds the root resource which corresponds specified statement and type
     *
     * @param base base ont-statement
     * @param type owl:Axiom or owl:Annotation
     * @return {@link OntAnnotation} the anonymous resource with specified type.
     */
    protected static OntAnnotation findAnnotationObject(OntStatementImpl base, Resource type) {
        try (Stream<Resource> resources = Iter.asStream(base.getModel().listResourcesWithProperty(RDF.type, type))) {
            return resources.filter(r -> r.hasProperty(OWL.annotatedSource, base.getSubject()))
                    .filter(r -> r.hasProperty(OWL.annotatedProperty, base.getPredicate()))
                    .filter(r -> r.hasProperty(OWL.annotatedTarget, base.getObject()))
                    //.map(r -> r.as(OntAnnotation.class))
                    .map(FrontsNode::asNode)
                    .map(r -> base.getModel().getNodeAs(r, OntAnnotation.class)).findFirst().orElse(null);
        }
    }

    /**
     * Creates the new annotation section (resource).
     *
     * @param base base ont-statement
     * @param type owl:Axiom or owl:Annotation
     * @return {@link OntAnnotation} the anonymous resource with specified type.
     */
    protected static OntAnnotation createAnnotationObject(OntStatementImpl base, Resource type) {
        Resource res = base.getModel().createResource();
        res.addProperty(RDF.type, type);
        res.addProperty(OWL.annotatedSource, base.getSubject());
        res.addProperty(OWL.annotatedProperty, base.getPredicate());
        res.addProperty(OWL.annotatedTarget, base.getObject());
        return res.as(OntAnnotation.class);
    }

    /**
     * Determines the annotation type.
     * Root annotations (including some anon-axioms bodies) go with the type owl:Axiom {@link OWL#Axiom}
     *
     * @param subject {@link Resource} the subject resource to test
     * @return {@link OWL#Axiom} or {@link OWL#Annotation}
     */
    public static Resource getAnnotationRootType(Resource subject) {
        return subject.isAnon() && subject.canAs(OntAnnotation.class) ? OWL.Annotation : OWL.Axiom;
    }

    /**
     * The class-implementation of the root statement.
     * The new annotations comes in the form of plain annotation-assertions
     * while in the base {@link OntStatement} the would be {@link OntAnnotation} resource.
     *
     * @see OntObject#getRoot
     */
    public static class RootImpl extends OntStatementImpl {

        public RootImpl(Resource subject, Property predicate, RDFNode object, OntGraphModel model) {
            super(subject, predicate, object, model);
        }

        @Override
        public boolean isRoot() {
            return true;
        }

        @Override
        public OntStatement addAnnotation(OntNAP property, RDFNode value) {
            checkAnnotationInput(property, value);
            getModel().add(getSubject(), property, value);
            return new OntStatementImpl(getSubject(), property, value, getModel());
        }

        @Override
        public Stream<OntStatement> annotations() {
            Stream<OntStatement> res = Iter.asStream(getModel()
                    .listStatements(getSubject(), null, (RDFNode) null))
                    .filter(s -> s.getPredicate().canAs(OntNAP.class))
                    .map(s -> new OntStatementImpl(s.getSubject(), s.getPredicate().as(OntNAP.class), s.getObject(), getModel()));
            return Stream.concat(res, super.annotations());
        }

        @Override
        public boolean hasAnnotations() {
            try (Stream<Statement> statements = Iter.asStream(getSubject().listProperties())) {
                return statements.map(Statement::getPredicate).anyMatch(p -> p.canAs(OntNAP.class)) || super.hasAnnotations();
            }
        }

        @Override
        public void deleteAnnotation(OntNAP property, RDFNode value) {
            checkAnnotationInput(property, value);
            getModel().removeAll(getSubject(), property, value);
            super.deleteAnnotation(property, value);
        }
    }

}
