package ru.avicomp.ontapi.jena.impl;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.StatementImpl;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.*;
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
        return addAnnotationStatement(this, getAnnotationRootType(), property, value);
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
            CommonAnnotationImpl res = new CommonAnnotationImpl(root.get(), property, value, getModel());
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

    protected void changeSubject(Resource newSubject) {
        this.subject = OntJenaException.notNull(newSubject, "Null subject.").inModel(getModel());
    }

    public Optional<OntAnnotation> resource() {
        return Optional.ofNullable(findAnnotationObject(this, getAnnotationRootType()));
    }

    protected Resource getAnnotationRootType() {
        return OWL.Axiom;
    }

    /**
     * Adds an annotation statement.
     * For internal use only.
     *
     * @param base     the base ont-statement to which the result annotation will belong
     * @param type     {@link OWL#Axiom} or {@link OWL#Annotation}
     * @param property {@link OntNAP} the named annotation property
     * @param value    {@link RDFNode}
     * @return {@link CommonAnnotationImpl}
     */
    protected static CommonAnnotationImpl addAnnotationStatement(OntStatement base, Resource type, OntNAP property, RDFNode value) {
        Resource root = findAnnotationObject(base, type);
        if (root == null) {
            root = createAnnotationObject(base, type);
        }
        root.addProperty(property, value);
        return new CommonAnnotationImpl(root, property, value, base.getModel());
    }

    /**
     * Creates an annotation statement.
     * For internal use only.
     *
     * @param base     the base {@link OntStatement} to which the result annotation will belong
     * @param property {@link OntNAP} the named annotation property
     * @param value    {@link RDFNode}
     * @return {@link CommonAnnotationImpl} or {@link AssertionAnnotationImpl}
     */
    protected static OntStatement createAnnotationStatement(OntStatement base, OntNAP property, RDFNode value) {
        return base.getSubject().isURIResource() ?
                new AssertionAnnotationImpl(base, property, value) :
                new CommonAnnotationImpl(base.getSubject(), property, value, base.getModel());
    }

    /**
     * Finds the root resource which corresponds specified statement and type
     *
     * @param base base ont-statement
     * @param type owl:Axiom or owl:Annotation
     * @return {@link OntAnnotation} the anonymous resource with specified type.
     */
    public static OntAnnotation findAnnotationObject(OntStatement base, Resource type) {
        return Iter.asStream(base.getModel().listResourcesWithProperty(RDF.type, type))
                .filter(r -> r.hasProperty(OWL.annotatedSource, base.getSubject()))
                .filter(r -> r.hasProperty(OWL.annotatedProperty, base.getPredicate()))
                .filter(r -> r.hasProperty(OWL.annotatedTarget, base.getObject()))
                .map(r -> r.as(OntAnnotation.class))
                .findFirst().orElse(null);
    }

    /**
     * Creates new annotation section (resource).
     *
     * @param base base ont-statement
     * @param type owl:Axiom or owl:Annotation
     * @return {@link OntAnnotation} the anonymous resource with specified type.
     */
    public static OntAnnotation createAnnotationObject(OntStatement base, Resource type) {
        Resource res = base.getModel().createResource();
        res.addProperty(RDF.type, type);
        res.addProperty(OWL.annotatedSource, base.getSubject());
        res.addProperty(OWL.annotatedProperty, base.getPredicate());
        res.addProperty(OWL.annotatedTarget, base.getObject());
        return res.as(OntAnnotation.class);
    }

    /**
     * Implementation of Annotation OntObject.
     */
    public static class OntAnnotationImpl extends OntObjectImpl implements OntAnnotation {
        public static final Set<Property> SPEC = Stream.of(RDF.type, OWL.annotatedSource, OWL.annotatedProperty, OWL.annotatedTarget)
                .collect(Collectors.toSet());
        public static Configurable<OntObjectFactory> annotationFactory = m -> new CommonOntObjectFactory(
                new OntMaker.Default(OntAnnotationImpl.class),
                new OntFinder.ByType(OWL.Axiom),
                OntAnnotationImpl::testAnnotation);

        public OntAnnotationImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Stream<OntStatement> content() {
            return SPEC.stream().map(this::getRequiredProperty);
        }

        @Override
        public Stream<OntStatement> assertions() {
            return Iter.asStream(listProperties())
                    .filter(st -> !SPEC.contains(st.getPredicate()))
                    .filter(st -> st.getPredicate().canAs(OntNAP.class))
                    .map(st -> new CommonAnnotationImpl(this, st.getPredicate().as(OntNAP.class), st.getObject(), getModel()))
                    .map(OntStatement.class::cast);
        }

        public static boolean testAnnotation(Node node, EnhGraph graph) {
            if (!node.isBlank()) return false;
            Set<Node> types = graph.asGraph().find(node, RDF.type.asNode(), Node.ANY).mapWith(Triple::getObject).toSet();
            if ((types.contains(OWL.Axiom.asNode()) || types.contains(OWL.Annotation.asNode())) &&
                    Stream.of(OWL.annotatedSource, OWL.annotatedProperty, OWL.annotatedTarget)
                            .map(FrontsNode::asNode)
                            .allMatch(p -> graph.asGraph().contains(node, p, Node.ANY))) {
                return true;
            }
            // special cases: owl:AllDisjointClasses, owl:AllDisjointProperties, owl:AllDifferent or owl:NegativePropertyAssertion
            return Stream.of(OWL.AllDisjointClasses, OWL.AllDisjointProperties, OWL.AllDifferent, OWL.NegativePropertyAssertion)
                    .map(FrontsNode::asNode).anyMatch(types::contains);
        }
    }

    /**
     * The base class for any annotation ont-statements (both plain(assertion) and bulk) attach.
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
        protected Resource getAnnotationRootType() {
            return OWL.Annotation;
        }

        @Override
        public String toString() {
            return String.format("Annotation[%s %s]", getPredicate().getURI(), getObject());
        }
    }

    /**
     * The class-implementation for root statements.
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
            return createAnnotationStatement(this, property, value);
        }

        @Override
        public Stream<OntStatement> annotations() {
            Stream<OntStatement> res = Iter.asStream(getModel()
                    .listStatements(getSubject(), null, (RDFNode) null))
                    .filter(s -> s.getPredicate().canAs(OntNAP.class))
                    .map(s -> createAnnotationStatement(this, s.getPredicate().as(OntNAP.class), s.getObject()));
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
     * Class for assertion annotations.
     * Assertion annotation is a plain annotation like "@root rdfs:comment "some comment"@fr"
     * It has no children.
     * But instance of this object could be converted to common(bulk) annotation by adding new child.
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
                CommonAnnotationImpl annotation = addAnnotationStatement(base, OWL.Axiom, getPredicate(), getObject());
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
                OntAnnotation prev = getSubject().as(OntAnnotation.class);
                getModel().remove(this);
                getModel().add(base.getSubject(), getPredicate(), getObject());
                changeSubject(base.getSubject());
                // remove section if it is empty.
                if (prev.assertions().count() == 0) {
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
