package ru.avicomp.ontapi.jena.converters;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;

import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * The base class for any graph-converter.
 * todo: add configurable logger to record all changes.
 */
@SuppressWarnings("WeakerAccess")
public abstract class TransformAction {
    protected static final Node RDF_TYPE = RDF.type.asNode();
    protected static final Set<Node> BUILT_IN = BuiltIn.ALL.stream().map(FrontsNode::asNode).collect(Collectors.toSet());

    private final Graph graph;
    private Model whole;
    private Model base;

    protected TransformAction(Graph graph) {
        this.graph = graph;
    }

    /**
     * performs the graph transformation.
     */
    public abstract void perform();

    /**
     * decides is the transformation needed or not.
     *
     * @return true to process, false to skip
     */
    public boolean test() {
        return true;
    }

    public void process() {
        if (test()) {
            if (GraphConverter.LOGGER.isDebugEnabled())
                GraphConverter.LOGGER.debug("Process <" + getClass().getSimpleName() + ">");
            perform();
        }
    }

    public Graph getGraph() {
        return graph;
    }

    public Graph getBaseGraph() {
        return graph instanceof UnionGraph ? ((UnionGraph) graph).getBaseGraph() : graph;
    }

    public Model getModel() {
        return whole == null ? whole = ModelFactory.createModelForGraph(getGraph()) : whole;
    }

    public Model getBaseModel() {
        return base == null ? base = ModelFactory.createModelForGraph(getBaseGraph()) : base;
    }

    public Stream<Statement> listStatements(Resource s, Property p, RDFNode o) {
        Model m = getModel();
        return statements(getBaseModel(), s, p, o)
                .map(st -> m.createStatement(st.getSubject(), st.getPredicate(), st.getObject()));
    }

    protected void addType(Resource subject, Resource type) {
        addType(subject.asNode(), type.asNode());
    }

    protected void addType(Node subject, Resource type) {
        addType(subject, type.asNode());
    }

    protected void addType(Node subject, Node type) {
        getGraph().add(Triple.create(subject, RDF_TYPE, type));
    }

    protected void deleteType(Node subject, Resource type) {
        getGraph().delete(Triple.create(subject, RDF_TYPE, type.asNode()));
    }

    protected void deleteType(Resource subject, Resource type) {
        deleteType(subject.asNode(), type);
    }

    protected void replaceType(Resource realType, Resource newType) {
        Set<Resource> toFix = listStatements(null, RDF.type, realType).map(Statement::getSubject).collect(Collectors.toSet());
        toFix.forEach(subject -> {
            deleteType(subject, realType);
            addType(subject, newType);
        });
    }

    protected boolean containsType(Resource type) {
        return getBaseGraph().contains(Node.ANY, RDF_TYPE, type.asNode());
    }

    protected Set<Node> getTypes(Node subject) {
        return getGraph().find(subject, RDF_TYPE, Node.ANY).mapWith(Triple::getObject).toSet();
    }

    /**
     * returns Stream of types for specified {@link RDFNode}, or empty stream if the input is not uri-resource.
     *
     * @param node node, attached to model.
     * @return Stream of {@link Resource}s
     */
    protected Stream<Resource> types(RDFNode node) {
        return types(node, true);
    }

    protected Stream<Resource> types(RDFNode node, boolean requireURI) {
        return requireURI && !node.isURIResource() ? Stream.empty() :
                Iter.asStream(node.asResource().listProperties(RDF.type)
                        .mapWith(Statement::getObject)
                        .filterKeep(RDFNode::isURIResource)
                        .mapWith(RDFNode::asResource));
    }

    public static Stream<Statement> statements(Model m, Resource s, Property p, RDFNode o) {
        return Iter.asStream(m.listStatements(s, p, o));
    }
}
