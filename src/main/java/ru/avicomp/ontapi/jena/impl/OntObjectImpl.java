package ru.avicomp.ontapi.jena.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.configuration.*;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * base resource.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public class OntObjectImpl extends ResourceImpl implements OntObject {

    public static Configurable<OntObjectFactory> objectFactory = m ->
            new CommonOntObjectFactory(new OntMaker.Default(OntObjectImpl.class), OntFinder.ANY_SUBJECT, OntFilter.URI.or(OntFilter.BLANK));

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

    @Override
    public OntStatement getRoot() {
        List<Resource> types = types().collect(Collectors.toList());
        return types.isEmpty() ? null : new OntStatementImpl.RootImpl(this, RDF.type, types.get(0), getModel());
    }

    protected OntStatement getRoot(Property property, Resource type) {
        return hasProperty(property, type) ? new OntStatementImpl.RootImpl(this, property, type, getModel()) : null;
    }

    @Override
    public Stream<OntStatement> content() {
        OntStatement root = getRoot();
        return root == null ? Stream.empty() : Stream.of(root);
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
        OntStatement root = getRoot();
        return Iter.asStream(listProperties(property))
                .map(s -> getModel().toOntStatement(root, s));
        //return statements().filter(s -> s.getPredicate().equals(property));
    }

    @Override
    public Stream<OntStatement> statements() {
        OntStatement main = getRoot();
        return Iter.asStream(listProperties())
                .map(s -> getModel().toOntStatement(main, s));
    }

    @Override
    public Literal asLiteral() {
        return as(Literal.class);
    }

    /**
     * gets rdf:List content as Stream of RDFNode's.
     * if the object is not rdf:List then empty stream expected.
     * if there are several lists with the same predicate the contents of all will be merged.
     * <p>
     * Note: here we use the "tolerant" approach.
     * Generally speaking, the case when we have several lists on a single predicate is _not_ correct (in terms of OWL2).
     * This case indicates that we deal with the wrong ontology.
     *
     * @param property the predicate to search for rdf:List.
     * @return Distinct Stream of RDFNode (maybe empty if there are no rdf:List)
     */
    public Stream<RDFNode> rdfListMembers(Property property) {
        return Iter.asStream(listProperties(property)
                .mapWith(Statement::getObject)
                .filterKeep(n -> n.canAs(RDFList.class))
                .mapWith(n -> n.as(RDFList.class)))
                .map(RDFList::asJavaList)
                .map(Collection::stream)
                .flatMap(Function.identity()).distinct();
    }

    /**
     * gets the stream of nodes with the specified type from rdf:List.
     * Note: In OWL2 the type of rdf:List members is always the same (with except of owl:hasKey construction).
     *
     * @param predicate to search for rdf:Lists
     * @param view      Class, the type of returned nodes.
     * @return Stream of {@link RDFNode} with specified type.
     */
    public <O extends RDFNode> Stream<O> rdfListMembers(Property predicate, Class<O> view) {
        return rdfListMembers(predicate).map(n -> getModel().getNodeAs(n.asNode(), view));
    }

    public Stream<OntStatement> rdfListContent(Property property) {
        OntStatement r = getRoot();
        return Iter.asStream(listProperties(property)
                .mapWith(Statement::getObject)
                .filterKeep(n -> n.canAs(RDFListImpl.class))
                .mapWith(n -> n.as(RDFListImpl.class)))
                .map(RDFListImpl::collectStatements)
                .map(Collection::stream)
                .flatMap(Function.identity())
                .map(s -> getModel().toOntStatement(r, s));
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


    public <T extends RDFNode> T getObject(Property predicate, Class<T> view) {
        return object(predicate, view).orElse(null);
    }

    public <T extends RDFNode> T getRequiredObject(Property predicate, Class<T> view) {
        return object(predicate, view)
                .orElseThrow(OntJenaException.supplier(String.format("Can't find required object [%s @%s %s]", this, predicate, view)));
    }

    public Stream<RDFNode> objects(Property predicate) {
        return Iter.asStream(listProperties(predicate).mapWith(Statement::getObject));
    }

    public <T extends RDFNode> Optional<T> object(Property predicate, Class<T> view) {
        return objects(predicate, view).findFirst();
    }

    @Override
    public <O extends RDFNode> Stream<O> objects(Property predicate, Class<O> view) {
        return objects(predicate).filter(node -> node.canAs(view)).map(FrontsNode::asNode).map(node -> getModel().getNodeAs(node, view));
    }

    @Override
    public OntGraphModelImpl getModel() {
        return (OntGraphModelImpl) super.getModel();
    }

    @SuppressWarnings("unchecked")
    public Class<? extends OntObject> getActualClass() {
        return Arrays.stream(getClass().getInterfaces()).filter(OntObject.class::isAssignableFrom).map(c -> (Class<? extends OntObject>) c).findFirst().orElse(null);
    }

    public static String toString(Class<? extends RDFNode> view) {
        return view.getName().replace(OntObject.class.getPackage().getName() + ".", "");
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", asNode(), toString(getActualClass()));
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

    @SafeVarargs
    static Configurable<MultiOntObjectFactory> createMultiFactory(OntFinder finder, Configurable<? extends OntObjectFactory>... factories) {
        return mode -> new MultiOntObjectFactory(finder, Stream.of(factories).map(c -> c.get(mode)).toArray(OntObjectFactory[]::new));
    }

}
