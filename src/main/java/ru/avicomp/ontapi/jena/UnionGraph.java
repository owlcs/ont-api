package ru.avicomp.ontapi.jena;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.Dyadic;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * Union Graph.
 * The difference between {@link MultiUnion} and this graph is it doesn't allow to modify any graphs with except of base.
 * Underling sub graphs are used only for searching, the adding and removing are performed only over the base (left) graph.
 * <p>
 * Created by szuev on 28.10.2016.
 */
public class UnionGraph extends Dyadic {

    /**
     * @param base Graph
     */
    public UnionGraph(Graph base) {
        super(base, new MultiUnion());
    }

    public Graph getBaseGraph() {
        return L;
    }

    protected MultiUnion getSubGraphs() {
        return (MultiUnion) R;
    }

    @Override
    protected ExtendedIterator<Triple> _graphBaseFind(Triple m) {
        return L.find(m).andThen(R.find(m));
    }

    @Override
    public boolean graphBaseContains(Triple t) {
        return L.contains(t) || R.contains(t);
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

    public void addGraph(Graph graph) {
        getSubGraphs().addGraph(graph);
    }

    public void removeGraph(Graph graph) {
        getSubGraphs().removeGraph(graph);
    }

}
