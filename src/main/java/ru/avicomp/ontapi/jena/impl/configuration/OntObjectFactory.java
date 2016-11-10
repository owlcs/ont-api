package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;

import ru.avicomp.ontapi.OntException;

/**
 * Extended {@link Implementation} factory, the class base for any OntObject factories.
 * Used to bind implementation and interface.
 * <p>
 * Created by @szuev on 03.11.2016.
 */
public abstract class OntObjectFactory extends Implementation {

    /**
     * Makes some changes in graph and returns wrapped Node
     *
     * @param node The node to be wrapped
     * @param eg   The graph containing the node
     * @return A new enhanced node which wraps node but presents the interface(s) that this factory encapsulates.
     * @throws OntException in case modification of graph is not allowed for the specified node.
     */
    public EnhNode create(Node node, EnhGraph eg) {
        throw new OntException("Creation is not allowed: " + node);
    }

    /**
     * Returns stream of nodes with interface that this factory encapsulates.
     * @param eg The graph containing the node
     * @return the stream of enhanced and suitability nodes.
     */
    public Stream<EnhNode> find(EnhGraph eg) {
        return Stream.empty();
    }
}
