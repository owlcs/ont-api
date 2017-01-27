package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.ConversionException;

import ru.avicomp.ontapi.jena.OntJenaException;

/**
 * Extended {@link Implementation} factory, the base class for any our ont object factories.
 * Used to bind implementation(node) and interface.
 * Also for searching nodes and for performing graph changes related to the node (e.g. creating a resource-declaration by interface).
 * <p>
 * Created by @szuev on 03.11.2016.
 */
public abstract class OntObjectFactory extends Implementation {

    /**
     * Makes some changes the in graph and returns the wrapped node
     *
     * @param node The node to be wrapped
     * @param eg   The graph which would contain the result{@link EnhNode}.
     * @return a new enhanced node which wraps node but presents the interface(s) that this factory encapsulates.
     * @throws OntJenaException.Creation in case modification of graph is not allowed for the specified node.
     */
    public EnhNode create(Node node, EnhGraph eg) {
        throw new OntJenaException("Creation is not allowed: " + node);
    }

    /**
     * Determines if modifying of the graph ({@link EnhGraph}) is allowed for the node ({@link Node}).
     *
     * @param node {@link Node} the node to test that changes are permitted.
     * @param eg   {@link EnhGraph} the graph to be changed.
     * @return true if creation is allowed.
     */
    public boolean canCreate(Node node, EnhGraph eg) {
        return false;
    }

    /**
     * Returns stream of nodes with interface that this factory encapsulates.
     *
     * @param eg The graph containing the node
     * @return the stream of enhanced and suitability nodes.
     */
    public Stream<EnhNode> find(EnhGraph eg) {
        return Stream.empty();
    }

    /**
     * checks that the wrapping (node, eg) would succeed.
     *
     * @param node node the node to test for suitability
     * @param eg   the enhanced graph the node appears in
     * @return true iff the node can represent our type in that graph
     */
    public abstract boolean canWrap(Node node, EnhGraph eg);

    /**
     * For internal use only.
     * Does wrapping without any checking.
     *
     * @param node {@link Node}
     * @param eg   {@link EnhGraph}
     * @return {@link EnhNode}. Note: some realisation may allow null
     */
    protected abstract EnhNode doWrap(Node node, EnhGraph eg);

    /**
     * Creates a new {@link EnhNode} wrapping a {@link Node} node in the context of an graph {@link EnhGraph}
     *
     * @param node the node to be wrapped
     * @param eg   the graph containing the node
     * @return A new enhanced node which wraps node but presents the interface(s) that this factory encapsulates.
     * @throws ConversionException in case wrapping is impossible.
     */
    @Override
    public EnhNode wrap(Node node, EnhGraph eg) {
        if (!canWrap(node, eg))
            throw new ConversionException("Can't wrap node " + node + ". Use direct factory.");
        return doWrap(node, eg);
    }
}
