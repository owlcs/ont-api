package ru.avicomp.ontapi.jena.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.impl.configuration.OntFinder;
import ru.avicomp.ontapi.jena.impl.configuration.OntObjectFactory;
import ru.avicomp.ontapi.jena.model.*;

/**
 * base resource.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public class OntObjectImpl extends ResourceImpl implements OntObject {
    static final Node RDF_TYPE = RDF.type.asNode();
    static final Node OWL_DATATYPE_PROPERTY = OWL2.DatatypeProperty.asNode();
    static final Node OWL_OBJECT_PROPERTY = OWL2.ObjectProperty.asNode();
    static final Node OWL_CLASS = OWL2.Class.asNode();
    static final Node OWL_RESTRICTION = OWL2.Restriction.asNode();

    public static OntObjectFactory objectFactory = new OntObjectFactory() {
        @Override
        public Stream<EnhNode> find(EnhGraph eg) {
            return OntFinder.ANYTHING.find(eg).filter(n -> canWrap(n, eg)).map(n -> wrap(n, eg));
        }

        @Override
        public EnhNode wrap(Node n, EnhGraph eg) {
            if (canWrap(n, eg)) {
                return new OntObjectImpl(n, eg);
            }
            throw new OntException("Cannot convert node " + n + " to OntObject");
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return node.isURI() || node.isBlank();
        }
    };

    public OntObjectImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    public Stream<Resource> types() {
        return JenaUtils.asStream(getModel().listObjectsOfProperty(this, RDF.type)
                .filterKeep(RDFNode::isURIResource).mapWith(Resource.class::cast)).distinct();
    }

    boolean hasType(Resource type) {
        return types().filter(type::equals).findAny().isPresent();
    }

    void addType(Resource type) {
        getModel().add(this, RDF.type, type);
    }

    void removeType(Resource type) {
        getModel().remove(this, RDF.type, type);
    }

    void changeType(Resource property, boolean add) {
        if (add) {
            addType(property);
        } else {
            removeType(property);
        }
    }


    public OntStatement getMainStatement() {
        List<Resource> types = types().collect(Collectors.toList());
        if (types.isEmpty()) throw new OntException("Can't determine main triple: no types.");
        return new MainStatementImpl(this, RDF.type, types.get(0), getModel());
    }

    public OntStatement getStatement(Property property, OntObject object) {
        return statements(property).filter(s -> s.getObject().equals(object)).findFirst().orElse(null);
    }

    public Stream<OntStatement> statements(Property property) {
        return statements().filter(s -> s.getPredicate().equals(property));
    }

    public Stream<OntStatement> statements() {
        OntStatement main = getMainStatement();
        return JenaUtils.asStream(listProperties()).map(s -> wrapStatement(main, s));
    }

    private OntStatement wrapStatement(OntStatement main, Statement statement) {
        if (statement.equals(main)) return main;
        if (statement.getPredicate().canAs(OntNAP.class)) {
            return new OntAStatementImpl(statement); //todo:
        }
        return new OntStatementImpl(statement);
    }

    public <T extends OntObject> T getOntProperty(Property predicate, Class<T> view) {
        Statement st = getProperty(predicate);
        return st == null ? null : getModel().getNodeAs(st.getObject().asNode(), view);
    }

    public <T extends OntObject> T getRequiredOntProperty(Property predicate, Class<T> view) {
        return getModel().getNodeAs(getRequiredProperty(predicate).getObject().asNode(), view);
    }

    /**
     * todo:
     */
    public class MainStatementImpl extends OntStatementImpl {
        public MainStatementImpl(Resource subject, Property predicate, RDFNode object, GraphModel model) {
            super(subject, predicate, object, model);
        }
    }

    @Override
    public GraphModelImpl getModel() {
        return (GraphModelImpl) super.getModel();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends OntObject> getActualClass() {
        return Arrays.stream(getClass().getInterfaces()).filter(OntObject.class::isAssignableFrom).map(c -> (Class<? extends OntObject>) c).findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", asNode(), getActualClass().getSimpleName());
    }

    @Override
    public Stream<OntAnnotation> annotations() {
        return Stream.concat(assertionAnnotations(), bulkAnnotations());
    }

    public Stream<OntAnnotation> assertionAnnotations() {
        return JenaUtils.asStream(listProperties()
                .filterKeep(s -> s.getPredicate().canAs(OntNAP.class))
                .mapWith(s -> new OntAStatementImpl(s.getSubject(), s.getPredicate().as(OntNAP.class), s.getObject(), getModel())));
    }

    public Stream<OntAnnotation> bulkAnnotations() {
        Stream<Resource> subjects = JenaUtils.asStream(getModel().listStatements(null, OWL2.annotatedSource, this)
                .mapWith(Statement::getSubject)
                .filterKeep(s -> getModel().contains(s, RDF.type, OWL2.Axiom))
                .filterKeep(s -> getModel().contains(s, OWL2.annotatedProperty, (RDFNode) null))
                .filterKeep(s -> getModel().contains(s, OWL2.annotatedProperty, (RDFNode) null)));
        return subjects.map(r -> OntAStatementImpl.children(r, getModel())).flatMap(Function.identity());
    }

    @Override
    public OntAnnotation addAnnotation(OntNAP property, Resource uri) {
        return addAnnotation(OntException.notNull(property, "Null property."), (RDFNode) checkNamed(uri));
    }

    @Override
    public OntAnnotation addAnnotation(OntNAP property, Literal literal) {
        return addAnnotation(OntException.notNull(property, "Null property."), (RDFNode) OntException.notNull(literal, "Null literal."));
    }

    @Override
    public OntAnnotation addAnnotation(OntNAP property, OntIndividual.Anonymous anon) {
        return addAnnotation(OntException.notNull(property, "Null property."), (RDFNode) OntException.notNull(anon, "Null individual."));
    }

    private OntAnnotation addAnnotation(OntNAP property, RDFNode value) {
        addProperty(property, value);
        return new OntAStatementImpl(this, property, value, getModel());
    }

    static Node checkNamed(Node res) {
        if (OntException.notNull(res, "Null node").isURI()) {
            return res;
        }
        throw new OntException("Not uri node " + res);
    }

    static Resource checkNamed(Resource res) {
        if (OntException.notNull(res, "Null resource").isURIResource()) {
            return res;
        }
        throw new OntException("Not uri resource " + res);
    }
}
