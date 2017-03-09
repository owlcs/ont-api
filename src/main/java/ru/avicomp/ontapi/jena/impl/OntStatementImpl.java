package ru.avicomp.ontapi.jena.impl;

import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.StatementImpl;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * Annotated statement.
 * how to write annotation in RDF see for example this:
 * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.2 Translation of Annotations</a>
 * <p>
 * Created by @szuev on 12.11.2016.
 */
public class OntStatementImpl extends StatementImpl implements OntStatement {

    public OntStatementImpl(Resource subject, Property predicate, RDFNode object, OntGraphModel model) {
        super(subject, predicate, object, (ModelCom) model);
    }

    @Override
    public OntGraphModel getModel() {
        return (OntGraphModel) super.getModel();
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public boolean isLocal() {
        return !((UnionGraph) getModel().getGraph()).getUnderlying().hasSubGraphs() || getModel().isInBaseModel(this);
    }

    @Override
    public OntObject getSubject() {
        return super.getSubject().as(OntObject.class);
    }

    @Override
    public OntStatement addAnnotation(OntNAP property, RDFNode value) {
        checkAnnotationInput(property, value);
        return addAnnotation(this, getAnnotationRoot(), property, value);
    }

    @Override
    public Stream<OntStatement> annotations() {
        Resource root = findAnnotationRoot(this, getAnnotationRoot());
        if (root == null) return Stream.empty();
        return children(root, getModel());
    }

    @Override
    public boolean hasAnnotations() {
        Resource root = findAnnotationRoot(this, getAnnotationRoot());
        if (root == null) return false;
        // skip rdf:type, owl:annotatedSource, owl:annotatedSource, owl:annotatedSource
        return root.listProperties().toSet().size() > 4;
    }

    @Override
    public void deleteAnnotation(OntNAP property, RDFNode value) {
        checkAnnotationInput(property, value);
        Resource root = findAnnotationRoot(this, getAnnotationRoot());
        if (root == null) return;
        if (getModel().contains(root, property, value)) {
            CommonAnnotationImpl res = new CommonAnnotationImpl(root, property, value, getModel());
            if (res.hasAnnotations()) throw new OntJenaException("Can't delete " + res + ": it has children");
            getModel().removeAll(root, property, value);
        }
        if (children(root, getModel()).count() == 0) { // if no children remove whole parent section.
            getModel().removeAll(root, null, null);
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
        throw new OntJenaException("It never happens.");
    }

    protected void changeSubject(Resource resource) {
        this.subject = OntJenaException.notNull(resource, "Null subject.").inModel(getModel());
    }

    protected Resource getAnnotationRoot() {
        return OWL.Axiom;
    }

    /**
     * Example of annotation:
     * [ a                      owl:Axiom;
     * rdfs:comment             <http://test.org#some-annotation-property>;
     * rdfs:label               "some-label", "comment here"@s;
     * owl:annotatedProperty    rdf:type;
     * owl:annotatedSource      <http://test.org#SomeClass1>;
     * owl:annotatedTarget      owl:Class];
     *
     * @param root  the root of annotation section
     * @param model Model
     * @return Stream of annotation statement, in the example above the output will contain three statements (two labels and one comment).
     */
    public static Stream<OntStatement> children(Resource root, OntGraphModel model) {
        return Iter.asStream(root.listProperties()
                .filterDrop(s -> RDF.type.equals(s.getPredicate()))
                .filterDrop(s -> OWL.annotatedSource.equals(s.getPredicate()))
                .filterDrop(s -> OWL.annotatedProperty.equals(s.getPredicate()))
                .filterDrop(s -> OWL.annotatedTarget.equals(s.getPredicate()))
                .filterKeep(s -> s.getPredicate().canAs(OntNAP.class))
                .mapWith(s -> new CommonAnnotationImpl(s.getSubject(), s.getPredicate().as(OntNAP.class), s.getObject(), model))
                .mapWith(OntStatement.class::cast));
    }

    /**
     * add annotation
     *
     * @param base     base ont-statement
     * @param type     owl:Axiom or owl:Annotation
     * @param property named annotation property
     * @param value    RDFNode-Value
     * @return {@link CommonAnnotationImpl}
     */
    public static CommonAnnotationImpl addAnnotation(OntStatement base, Resource type, OntNAP property, RDFNode value) {
        Resource root = findAnnotationRoot(base, type);
        if (root == null) {
            root = createAnnotation(base, type);
        }
        root.addProperty(property, value);
        return new CommonAnnotationImpl(root, property, value, base.getModel());
    }

    /**
     * finds the root resource which corresponds specified statement and type
     *
     * @param base base ont-statement
     * @param type owl:Axiom or owl:Annotation
     * @return Anonymous resource
     */
    public static Resource findAnnotationRoot(OntStatement base, Resource type) {
        return Iter.asStream(base.getModel().listResourcesWithProperty(RDF.type, type))
                .filter(r -> r.hasProperty(OWL.annotatedSource, base.getSubject()))
                .filter(r -> r.hasProperty(OWL.annotatedProperty, base.getPredicate()))
                .filter(r -> r.hasProperty(OWL.annotatedTarget, base.getObject()))
                .findFirst().orElse(null);
    }

    /**
     * creates new annotation section
     *
     * @param base base ont-statement
     * @param type owl:Axiom or owl:Annotation
     * @return Anonymous resource
     */
    public static Resource createAnnotation(OntStatement base, Resource type) {
        Resource res = base.getModel().createResource();
        res.addProperty(RDF.type, type);
        res.addProperty(OWL.annotatedSource, base.getSubject());
        res.addProperty(OWL.annotatedProperty, base.getPredicate());
        res.addProperty(OWL.annotatedTarget, base.getObject());
        return res;
    }


    /**
     * the base for assertion and bulk annotation.
     */
    public static class CommonAnnotationImpl extends OntStatementImpl {

        public CommonAnnotationImpl(Resource subject, OntNAP predicate, RDFNode object, OntGraphModel model) {
            super(subject, predicate, object, model);
        }

        @Override
        public OntNAP getPredicate() {
            return (OntNAP) super.getPredicate();
        }

        @Override
        protected Resource getAnnotationRoot() {
            return OWL.Annotation;
        }

        @Override
        public String toString() {
            return String.format("Annotation[%s %s]", getPredicate().getURI(), getObject());
        }
    }

    /**
     * Main Ont-Object triplet.
     * see {@link OntObject#getRoot()}
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
            return getSubject().isURIResource() ?
                    new AssertionAnnotationImpl(this, property, value) :
                    new CommonAnnotationImpl(getSubject(), property, value, getModel());
        }

        private OntStatement toAnnotation(OntNAP property, RDFNode value) {
            return getSubject().isURIResource() ?
                    new AssertionAnnotationImpl(this, property, value) :
                    new CommonAnnotationImpl(getSubject(), property, value, getModel());
        }

        @Override
        public Stream<OntStatement> annotations() {
            Stream<OntStatement> res = Iter.asStream(getModel()
                    .listStatements(getSubject(), null, (RDFNode) null))
                    .filter(s -> s.getPredicate().canAs(OntNAP.class))
                    .map(s -> toAnnotation(s.getPredicate().as(OntNAP.class), s.getObject()));
            return Stream.concat(res, super.annotations());
        }

        @Override
        public boolean hasAnnotations() {
            return Iter.asStream(getSubject().listProperties()).map(Statement::getPredicate).anyMatch(p -> p.canAs(OntNAP.class)) || super.hasAnnotations();
        }

        @Override
        public void deleteAnnotation(OntNAP property, RDFNode value) {
            checkAnnotationInput(property, value);
            getModel().removeAll(getSubject(), property, value);
            super.deleteAnnotation(property, value);
        }
    }

    /**
     * Class for assertion annotation.
     * Assertion annotation is a plain annotation like "@root_ont_object rdfs:comment "some comment"@fr"
     * It has no children. But instance for this object could be convert to common(bulk) annotation by adding new child.
     */
    public static class AssertionAnnotationImpl extends CommonAnnotationImpl {
        private final OntStatement base;

        public AssertionAnnotationImpl(OntStatement base, OntNAP predicate, RDFNode object) {
            super(base.getSubject(), predicate, object, base.getModel());
            this.base = base;
        }

        @Override
        public OntStatement addAnnotation(OntNAP property, RDFNode value) {
            checkAnnotationInput(property, value);
            if (isAssertion()) {
                // expand to owl:Axiom form
                CommonAnnotationImpl annotation = OntStatementImpl.addAnnotation(base, OWL.Axiom, getPredicate(), getObject());
                getModel().remove(this);
                changeSubject(annotation.getSubject());
            }
            return super.addAnnotation(property, value);
        }

        @Override
        public void deleteAnnotation(OntNAP property, RDFNode value) {
            checkAnnotationInput(property, value);
            if (isAssertion()) return;
            super.deleteAnnotation(property, value);
            if (!hasAnnotations()) { // if no sub-annotations collapse back to assertion:
                Resource prev = getSubject();
                getModel().remove(this);
                getModel().add(base.getSubject(), getPredicate(), getObject());
                changeSubject(base.getSubject());
                // remove section if it is empty.
                if (OntStatementImpl.children(prev, getModel()).count() == 0) {
                    getModel().removeAll(prev, null, null);
                }
            }
        }

        @Override
        public Stream<OntStatement> annotations() {
            if (isAssertion()) return Stream.empty();
            return super.annotations();
        }

        @Override
        public boolean hasAnnotations() {
            return !isAssertion() && super.hasAnnotations();
        }

        /**
         * @return true if it is true-assertion annotation.
         */
        public boolean isAssertion() {
            return getModel().contains(base.getSubject(), getPredicate(), getObject());
        }
    }
}
