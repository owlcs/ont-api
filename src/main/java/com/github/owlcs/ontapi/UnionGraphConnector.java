package com.github.owlcs.ontapi;

import org.apache.jena.graph.Graph;
import org.apache.jena.ontapi.UnionGraph;
import org.apache.jena.ontapi.impl.UnionGraphImpl;
import org.apache.jena.ontapi.utils.Graphs;

/**
 * A {@link org.apache.jena.graph.GraphListener} that allows to connect two {@link UnionGraph}
 * so that changing hierarchy structures of one graph will reflect the other.
 */
public class UnionGraphConnector extends UnionGraphImpl.EventManagerImpl {

    private final UnionGraph connection;

    public UnionGraphConnector(UnionGraph connection) {
        this.connection = connection;
    }

    /**
     * Creates a connected copy of the specified graph replacing the base graph by the specified one.
     *
     * @param union {@link UnionGraph} original graph
     * @param base  {@link Graph} base graph for new connected instance
     * @return {@link UnionGraph}
     */
    public static UnionGraph withBase(UnionGraph union, Graph base) {
        UnionGraph res = new UnionGraphImpl(base, true);
        union.subGraphs().forEach(res::addSubGraph);
        union.getEventManager().listeners().forEach(res.getEventManager()::register);
        UnionGraphConnector.connect(union, res);
        return res;
    }

    /**
     * Connects two graphs so that changing hierarchy structures of one graph will reflect the other.
     *
     * @param a {@link UnionGraph}
     * @param b {@link UnionGraph}
     */
    public static void connect(UnionGraph a, UnionGraph b) {
        if (a.getEventManager().listeners(UnionGraphConnector.class).noneMatch(it -> b.equals(it.connection))) {
            a.getEventManager().register(new UnionGraphConnector(b));
        }
        if (b.getEventManager().listeners(UnionGraphConnector.class).noneMatch(it -> a.equals(it.connection))) {
            b.getEventManager().register(new UnionGraphConnector(a));
        }
    }

    @Override
    public void notifySubGraphAdded(UnionGraph graph, Graph subGraph) {
        if (connection.subGraphs().noneMatch(subGraph::equals)) {
            connection.addSubGraph(subGraph);
        }
        super.notifySubGraphAdded(graph, subGraph);
    }

    @Override
    public void notifySubGraphRemoved(UnionGraph graph, Graph subGraph) {
        connection.subGraphs().filter(it -> Graphs.isSameBase(subGraph, it)).findFirst()
                .ifPresent(connection::removeSubGraph);
        super.notifySubGraphRemoved(graph, subGraph);
    }
}
