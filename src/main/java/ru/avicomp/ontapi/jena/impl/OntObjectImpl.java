package ru.avicomp.ontapi.jena.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.OntFinder;
import ru.avicomp.ontapi.jena.impl.configuration.OntObjectFactory;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Streams;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * base resource.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public class OntObjectImpl extends ResourceImpl implements OntObject {

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
            throw new OntJenaException("Cannot convert node " + n + " to OntObject");
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return node.isURI() || node.isBlank();
        }
    };

    public OntObjectImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    OntStatement addType(Resource type) {
        return addStatement(RDF.type, OntJenaException.notNull(type, "Null rdf:type"));
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
        return types.isEmpty() ? null : new OntStatementImpl.RootImpl(this, RDF.type, types.get(0), getModel());
    }

    protected OntStatement getRoot(Property property, Resource type) {
        return hasProperty(property, type) ? new OntStatementImpl.RootImpl(this, property, type, getModel()) : null;
    }

    @Override
    public boolean isLocal() {
        OntStatement declaration = getRoot(); // built-in could have null root-declaration
        return declaration != null && declaration.isLocal();
    }

    @Override
    public OntStatement getStatement(Property property) {
        return statements(property).findFirst().orElse(null);
    }

    @Override
    public OntStatement getStatement(Property property, RDFNode object) {
        return statements(property).filter(s -> s.getObject().equals(object)).findFirst().orElse(null);
    }

    @Override
    public OntStatement addStatement(Property property, RDFNode value) {
        Statement st = getModel().createStatement(this,
                OntJenaException.notNull(property, "Null property."),
                OntJenaException.notNull(value, "Null value."));
        getModel().add(st);
        return getModel().toOntStatement(getRoot(), st);
    }

    @Override
    public void remove(Property property, RDFNode value) {
        getModel().removeAll(this, OntJenaException.notNull(property, "Null property."), OntJenaException.notNull(value, "Null value."));
    }

    @Override
    public Stream<OntStatement> statements(Property property) {
        return statements().filter(s -> s.getPredicate().equals(property));
    }

    @Override
    public Stream<OntStatement> statements() {
        OntStatement main = getRoot();
        return Streams.asStream(listProperties()).map(s -> getModel().toOntStatement(main, s));
    }

    @Override
    public Literal asLiteral() {
        return as(Literal.class);
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
        return Streams.asStream(listProperties(property)
                .mapWith(Statement::getObject)
                .filterKeep(n -> n.canAs(RDFList.class))
                .mapWith(n -> n.as(RDFList.class)))
                .map(RDFList::asJavaList)
                .map(Collection::stream)
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
        return Streams.asStream(listProperties(predicate).mapWith(Statement::getObject)).distinct();
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
        if (OntJenaException.notNull(res, "Null node").isURI()) {
            return res;
        }
        throw new OntJenaException("Not uri node " + res);
    }

    static Resource checkNamed(Resource res) {
        if (OntJenaException.notNull(res, "Null resource").isURIResource()) {
            return res;
        }
        throw new OntJenaException("Not uri resource " + res);
    }
}
