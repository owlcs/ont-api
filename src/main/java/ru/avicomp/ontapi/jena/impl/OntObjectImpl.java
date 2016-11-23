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
import org.apache.jena.vocabulary.RDF;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.JenaUtils;
import ru.avicomp.ontapi.jena.impl.configuration.OntFinder;
import ru.avicomp.ontapi.jena.impl.configuration.OntObjectFactory;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * base resource.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public class OntObjectImpl extends ResourceImpl implements OntObject {
    static final Node RDF_TYPE = RDF.type.asNode();

    public static OntObjectFactory objectFactory = new OntObjectFactory() {
        @Override
        public Stream<EnhNode> find(EnhGraph eg) {
            return OntFinder.ANY_SUBJECT.find(eg).filter(n -> canWrap(n, eg)).map(n -> wrap(n, eg));
        }

        @Override
        public EnhNode wrap(Node n, EnhGraph eg) {
            if (canWrap(n, eg)) {
                return new OntObjectImpl(n, eg);
            }
            throw new OntApiException("Cannot convert node " + n + " to OntObject");
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
        return objects(RDF.type, Resource.class);
    }

    boolean hasType(Resource type) {
        return types().anyMatch(type::equals);
    }

    OntStatement addType(Resource type) {
        return addStatement(RDF.type, OntApiException.notNull(type, "Null rdf:type"));
    }

    void removeType(Resource type) {
        remove(RDF.type, type);
    }

    void changeType(Resource property, boolean add) {
        if (add) {
            addType(property);
        } else {
            removeType(property);
        }
    }

    public <T extends RDFNode> T getOntProperty(Property predicate, Class<T> view) {
        Statement st = getProperty(predicate);
        return st == null ? null : getModel().getNodeAs(st.getObject().asNode(), view);
    }

    public <T extends RDFNode> T getRequiredOntProperty(Property predicate, Class<T> view) {
        return getModel().getNodeAs(getRequiredProperty(predicate).getObject().asNode(), view);
    }

    @Override
    public OntStatement getRoot() {
        List<Resource> types = types().collect(Collectors.toList());
        if (types.isEmpty()) {
            //throw new OntApiException("Can't determine main triple, no types: " + this);
            return null;
        }
        return new OntStatementImpl.RootImpl(this, RDF.type, types.get(0), getModel());
    }

    @Override
    public OntStatement getStatement(Property property, RDFNode object) {
        return statements(property).filter(s -> s.getObject().equals(object)).findFirst().orElse(null);
    }

    @Override
    public OntStatement addStatement(Property property, RDFNode value) {
        Statement st = getModel().createStatement(this,
                OntApiException.notNull(property, "Null property."),
                OntApiException.notNull(value, "Null value."));
        getModel().add(st);
        return toOntStatement(getRoot(), st);
    }

    @Override
    public void remove(Property property, RDFNode value) {
        getModel().removeAll(this, OntApiException.notNull(property, "Null property."), OntApiException.notNull(value, "Null value."));
    }

    @Override
    public Stream<OntStatement> statements(Property property) {
        return statements().filter(s -> s.getPredicate().equals(property));
    }

    @Override
    public Stream<OntStatement> statements() {
        OntStatement main = getRoot();
        return JenaUtils.asStream(listProperties()).map(s -> toOntStatement(main, s));
    }

    private OntStatement toOntStatement(OntStatement main, Statement st) {
        if (st.equals(main)) return main;
        if (main != null && st.getPredicate().canAs(OntNAP.class)) {
            // if subject is anon -> general annotation wrapper.
            return main.getSubject().isURIResource() ? new OntStatementImpl.AssertionAnnotationImpl(main, st.getPredicate().as(OntNAP.class), st.getObject()) :
                    new OntStatementImpl.CommonAnnotationImpl(main.getSubject(), st.getPredicate().as(OntNAP.class), st.getObject(), main.getModel());
        }
        return new OntStatementImpl(st.getSubject(), st.getPredicate(), st.getObject(), getModel());
    }

    /**
     * gets rdf:List content as Stream of RDFNode's.
     * if object is not rdf:List empty stream expected.
     * if there are several lists with the same predicate the contents of all will be merged.
     *
     * @param property predicate
     * @return Distinct Stream of RDFNode
     */
    public Stream<RDFNode> rdfList(Property property) {
        return JenaUtils.asStream(listProperties(property)
                .mapWith(Statement::getObject)
                .filterKeep(n -> n.canAs(RDFList.class))
                .mapWith(n -> n.as(RDFList.class)))
                .map(list -> list.asJavaList().stream())
                .flatMap(Function.identity()).distinct();
    }

    public <O extends RDFNode> Stream<O> rdfList(Property predicate, Class<O> view) {
        return rdfList(predicate).map(n -> getModel().getNodeAs(n.asNode(), view));
    }

    /**
     * removes all objects for predicate (if object is rdf:List removes all content)
     *
     * @param predicate Property
     */
    public void clearAll(Property predicate) {
        listProperties(predicate).mapWith(Statement::getObject)
                .filterKeep(n -> n.canAs(RDFList.class))
                .mapWith(n -> n.as(RDFList.class)).forEachRemaining(RDFList::removeList);
        removeAll(predicate);
    }

    public Stream<RDFNode> objects(Property predicate) {
        return JenaUtils.asStream(listProperties(predicate).mapWith(Statement::getObject)).distinct();
    }

    public <O extends RDFNode> Stream<O> objects(Property predicate, Class<O> view) {
        return objects(predicate).filter(node -> node.canAs(view)).map(node -> getModel().getNodeAs(node.asNode(), view)).distinct();
    }

    @Override
    public OntGraphModelImpl getModel() {
        return (OntGraphModelImpl) super.getModel();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends OntObject> getActualClass() {
        return Arrays.stream(getClass().getInterfaces()).filter(OntObject.class::isAssignableFrom).map(c -> (Class<? extends OntObject>) c).findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", asNode(), getActualClass().getSimpleName());
    }

    static Node checkNamed(Node res) {
        if (OntApiException.notNull(res, "Null node").isURI()) {
            return res;
        }
        throw new OntApiException("Not uri node " + res);
    }

    static Resource checkNamed(Resource res) {
        if (OntApiException.notNull(res, "Null resource").isURIResource()) {
            return res;
        }
        throw new OntApiException("Not uri resource " + res);
    }
}
