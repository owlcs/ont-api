package ru.avicomp.ontapi.jena;

import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphListener;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.graph.compose.Union;
import org.apache.jena.graph.impl.SimpleEventManager;
import org.apache.jena.shared.PrefixMapping;

/**
 * Union Graph.
 * The difference between {@link MultiUnion} and this graph is it doesn't allow to modify any graphs with except of base.
 * Underling sub graphs are used only for searching, the adding and removing are performed only over the base (left) graph.
 * <p>
 * Created by szuev on 28.10.2016.
 */
public class UnionGraph extends Union {

    /**
     * @param base Graph
     */
    public UnionGraph(Graph base) {
        this(base, new OntEventManager());
    }

    /**
     * @param base Graph
     * @param gem  {@link OntEventManager}
     */
    public UnionGraph(Graph base, OntEventManager gem) {
        super(base, new OntMultiUnion());
        this.gem = OntJenaException.notNull(gem, "Null event manager.");
    }

    public Graph getBaseGraph() {
        return L;
    }

    public OntMultiUnion getUnderlying() {
        return (OntMultiUnion) R;
    }

    @Override
    public void performDelete(Triple t) {
        L.delete(t);
    }

    @Override
    public void performAdd(Triple t) {
        if (!R.contains(t))
            L.add(t);
    }

    @Override
    public OntEventManager getEventManager() {
        return (OntEventManager) gem;
    }

    @Override
    public PrefixMapping getPrefixMapping() {
        return L.getPrefixMapping();
    }

    public void addGraph(Graph graph) {
        getUnderlying().addGraph(graph);
    }

    public void removeGraph(Graph graph) {
        getUnderlying().removeGraph(graph);
    }

    public static class OntMultiUnion extends MultiUnion {

        public OntMultiUnion() {
            super();
        }

        public OntMultiUnion(Graph[] graphs) {
            super(graphs);
        }

        public OntMultiUnion(Iterator<Graph> graphs) {
            super(graphs);
        }

        public Stream<Graph> graphs() {
            return m_subGraphs.stream();
        }

        public boolean hasSubGraphs() {
            return !m_subGraphs.isEmpty();
        }
    }

    public static class OntEventManager extends SimpleEventManager {

        public Stream<GraphListener> listeners() {
            return listeners.stream();
        }

        public boolean hasListeners(Class<? extends GraphListener> view) {
            return listeners().anyMatch(l -> view.isAssignableFrom(l.getClass()));
        }
    }
}
