/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphListener;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.CompositionBase;
import org.apache.jena.graph.impl.SimpleEventManager;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.util.CollectionFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Iter;

import java.util.*;
import java.util.stream.Stream;

/**
 * An Union Graph.
 * It consists of two parts: a {@link #base base} graph and an {@link #sub Underlying} graphs collection.
 * The difference between {@link org.apache.jena.graph.compose.MultiUnion MultiUnion} and this implementation is that
 * this graph explicitly requires primary base graph which is the only one that can be modified directly.
 * Underlying sub graphs are used only for searching; add and delete operations are performed only on the base graph.
 * Such structure allows to build graph hierarchy which is used to reference between different models.
 * Also note: this graph supports recursions, that is, it may contain itself somewhere in the hierarchy.
 * The {@link PrefixMapping} of this graph is taken from the base graph,
 * and, therefore, any changes in it reflects both the base and this graph.
 * <p>
 * Created by szuev on 28.10.2016.
 *
 * @see ru.avicomp.ontapi.jena.impl.UnionModel
 */
@SuppressWarnings({"WeakerAccess"})
public class UnionGraph extends CompositionBase {

    protected final Graph base;
    protected final Underlying sub;
    protected final boolean distinct;

    /**
     * A set of parents, used to control {@link #graphs cache}.
     * Items of this {@code Set} are removed automatically
     * if there are no more strong references (a graph/model is removed, there is no usage anymore).
     */
    protected Set<UnionGraph> parents = Collections.newSetFromMap(new WeakHashMap<>());
    /**
     * Internal cache to hold all base graphs, used while {@link Graph#find(Triple) #find(..)}.
     * This {@code Set} cannot contain {@link UnionGraph}s.
     */
    protected Set<Graph> graphs;

    /**
     * Creates an instance with default settings.
     *
     * @param base {@link Graph}, not {@code null}
     */
    public UnionGraph(Graph base) {
        this(base, null);
    }

    /**
     * Creates ab instance with default settings and specified event manager.
     *
     * @param base {@link Graph}, not {@code null}
     * @param gem  {@link OntEventManager}
     */
    public UnionGraph(Graph base, OntEventManager gem) {
        this(base, null, gem);
    }

    /**
     * Constructs an instance from the given arguments.
     * Please note: it is distinct graph (parameter {@link #distinct} is {@code true}).
     * So method {@link #find(Triple)} will not return duplicates.
     * The additional duplicate checking may lead to storing the whole graph in the memory in the form of Set,
     * and for huge ontologies it is unacceptable.
     * But it is not important in case of working with OWL-API interfaces, since it uses a different approach.
     * Also notice, that checking is not performed if the graph is single (underlying part is empty).
     *
     * @param base {@link Graph}, not {@code null}
     * @param sub  {@link Underlying} or {@code null} to use default empty sub graphs
     * @param gem  {@link OntEventManager} or {@code null} to use default
     * @throws NullPointerException if base graph is {@code null}
     */
    public UnionGraph(Graph base, Underlying sub, OntEventManager gem) {
        this(base, sub, gem, true);
    }

    /**
     * The base constructor.
     *
     * @param base     {@link Graph}, not {@code null}
     * @param sub      {@link Underlying} or {@code null} to use default empty sub graphs
     * @param gem      {@link OntEventManager} or {@code null} to use default
     * @param distinct if {@code true}, the method {@link #find(Triple)} will return distinct iterator.
     * @throws NullPointerException if base graph is {@code null}
     */
    public UnionGraph(Graph base, Underlying sub, OntEventManager gem, boolean distinct) {
        this.base = Objects.requireNonNull(base, "Null base graph.");
        this.sub = sub == null ? new Underlying() : sub;
        this.gem = gem == null ? new OntEventManager() : gem;
        this.distinct = distinct;
    }

    @Override
    public PrefixMapping getPrefixMapping() {
        return getBaseGraph().getPrefixMapping();
    }

    /**
     * Answers the ont event manager for this graph.
     *
     * @return {@link OntEventManager}, not {@code null}
     */
    @Override
    public OntEventManager getEventManager() {
        return (OntEventManager) gem;
    }

    /**
     * Returns the base (primary) graph.
     *
     * @return {@link Graph}, not {@code null}
     */
    public Graph getBaseGraph() {
        return base;
    }

    /**
     * Returns the underlying graph, possible empty.
     *
     * @return {@link Underlying}, not {@code null}
     */
    public Underlying getUnderlying() {
        return sub;
    }

    @Override
    public void performAdd(Triple t) {
        if (!sub.contains(t))
            base.add(t);
    }

    @Override
    public void performDelete(Triple t) {
        base.delete(t);
    }

    /**
     * Adds the specified graph to the underlying graph collection.
     *
     * @param graph {@link Graph}, not {@code null}
     * @return this instance
     */
    public UnionGraph addGraph(Graph graph) {
        checkOpen();
        getUnderlying().add(graph);
        if (graph instanceof UnionGraph) {
            ((UnionGraph) graph).parents.add(this);
        }
        resetGraphsCache();
        return this;
    }

    /**
     * Removes the specified graph from the underlying graph collection.
     *
     * @param graph {@link Graph}, not {@code null}
     * @return this instance
     */
    public UnionGraph removeGraph(Graph graph) {
        checkOpen();
        getUnderlying().remove(graph);
        if (graph instanceof UnionGraph) {
            ((UnionGraph) graph).parents.remove(this);
        }
        resetGraphsCache();
        return this;
    }

    /**
     * Clears the {@link #graphs cache}.
     */
    protected void resetGraphsCache() {
        collectAllUnionGraphs().forEach(x -> x.graphs = null);
    }

    /**
     * Lists all base {@code Graph}s encapsulated in this hierarchy graph.
     *
     * @return {@link ExtendedIterator} of {@link Graph}s
     */
    public ExtendedIterator<Graph> listBaseGraphs() {
        return WrappedIterator.create((graphs == null ? graphs = collectBaseGraphs() : graphs).iterator());
    }

    /**
     * Performs the find operation.
     * Override {@code graphBaseFind} to return an iterator that will report when a deletion occurs.
     *
     * @param m {@link Triple} the matcher to match against, not {@code null}
     * @return an {@link ExtendedIterator iterator} of all triples matching {@code m} in the union of the graphs
     * @see org.apache.jena.graph.compose.MultiUnion#graphBaseFind(Triple)
     */
    @Override
    protected final ExtendedIterator<Triple> graphBaseFind(Triple m) {
        return SimpleEventManager.notifyingRemove(this, createFindIterator(m));
    }

    /**
     * Answers {@code true} if the graph contains any triple matching {@code t}.
     *
     * @param t {@link Triple}, not {@code null}
     * @return boolean
     */
    @Override
    public boolean graphBaseContains(Triple t) {
        if (base.contains(t)) return true;
        if (sub.isEmpty()) return false;
        Iterator<Graph> graphs = listBaseGraphs();
        while (graphs.hasNext()) {
            Graph g = graphs.next();
            if (g == base) continue;
            if (g.contains(t)) return true;
        }
        return false;
    }

    /**
     * Creates an extended iterator to be used in {@link Graph#find(Triple)}.
     *
     * @param m {@link Triple} pattern, not {@code null}
     * @return {@link ExtendedIterator} of {@link Triple}s
     * @see org.apache.jena.graph.compose.Union#_graphBaseFind(Triple)
     * @see org.apache.jena.graph.compose.MultiUnion#multiGraphFind(Triple)
     */
    @SuppressWarnings("JavadocReference")
    protected ExtendedIterator<Triple> createFindIterator(Triple m) {
        if (sub.isEmpty()) {
            return base.find(m);
        }
        if (!distinct) {
            return Iter.flatMap(listBaseGraphs(), x -> x.find(m));
        }
        // The logic and the comment below have been copy-pasted from the org.apache.jena.graph.compose.Union:
        // To find in the union, find in the components, concatenate the results, and omit duplicates.
        // That last is a performance penalty,
        // but I see no way to remove it unless we know the graphs do not overlap.
        Set<Triple> seen = createSet();
        return Iter.flatMap(listBaseGraphs(), x -> recording(rejecting(x.find(m), seen), seen));
    }

    /**
     * Creates a {@code Set} to be used while {@link Graph#find()}.
     * The returned set may contain a huge number of items.
     * And that's why this method has protected access -
     * implementations are allowed to override it for better performance.
     *
     * @return Set of {@link Triple}s
     */
    protected Set<Triple> createSet() {
        return CollectionFactory.createHashedSet();
    }

    /**
     * Closes the graph including all related graph.
     * Caution: this is an irreversible operation,
     * once closed graph can not be reopened.
     */
    @Override
    public void close() {
        listBaseGraphs().forEachRemaining(Graph::close);
        collectUnionGraphs().forEach(x -> x.closed = true);
    }

    @Override
    public boolean isEmpty() {
        // the default implementation use size(), which is extremely ineffective for this case
        return !Iter.findFirst(find()).isPresent();
    }

    /**
     * Generic dependsOn, returns {@code true} iff this graph or any its sub-graphs depend on the specified graph.
     *
     * @param other {@link Graph}
     * @return boolean
     */
    @Override
    public boolean dependsOn(Graph other) {
        return Graphs.dependsOn(base, other) || sub.dependsOn(other); // todo: recursion ?
    }

    /**
     * Calculates and returns a {@code Set} of all base {@link Graph graph}s that are placed lower in the hierarchy.
     *
     * @return Set (ordered) of {@link Graph}s
     */
    protected Set<Graph> collectBaseGraphs() {
        // use LinkedHasSet to save the order: the base graph from this UnionGraph must be the first
        Set<Graph> res = new LinkedHashSet<>();
        res.add(getBaseGraph());
        collectBaseGraphs(res);
        return res;
    }

    /**
     * Recursively collects a {@code Set} of all {@link UnionGraph UnionGraph}s
     * that are related to this instance somehow,
     * i.e. are present in the hierarchy lower or higher.
     * This graph instance is also included in the returned {@code Set}.
     *
     * @return Set of {@link UnionGraph}s
     */
    protected Set<UnionGraph> collectAllUnionGraphs() {
        Set<UnionGraph> res = collectUnionGraphs();
        collectParents(res);
        return res;
    }

    /**
     * Recursively collects all {@link UnionGraph}s that underlies this instance, inclusive.
     *
     * @return Set of {@link UnionGraph}s
     */
    protected Set<UnionGraph> collectUnionGraphs() {
        Set<UnionGraph> res = new HashSet<>();
        res.add(this);
        collectChildren(res);
        return res;
    }

    /**
     * Recursively collects all base {@link Graph graph}s
     * that are present in this collection or anywhere under the hierarchy.
     *
     * @param res {@code Set} of {@link Graph}s
     */
    private void collectBaseGraphs(Set<Graph> res) {
        getUnderlying().graphs().forEach(g -> {
            if (!(g instanceof UnionGraph)) {
                res.add(g);
                return;
            }
            UnionGraph u = (UnionGraph) g;
            if (res.add(u.getBaseGraph())) {
                u.collectBaseGraphs(res);
            }
        });
    }

    /**
     * Recursively collects all {@link UnionGraph}s
     * that are present in somewhere higher in the hierarchy.
     *
     * @param res {@code Set} of {@link UnionGraph}s
     */
    private void collectParents(Set<UnionGraph> res) {
        parents.stream()
                .filter(res::add)
                .forEach(u -> u.collectParents(res));
    }

    /**
     * Recursively collects all {@link UnionGraph}s
     * that are present in this collection or anywhere down the hierarchy.
     *
     * @param res {@code Set} of {@link UnionGraph}s
     */
    private void collectChildren(Set<UnionGraph> res) {
        getUnderlying().graphs()
                .filter(x -> x instanceof UnionGraph)
                .map(UnionGraph.class::cast)
                .filter(res::add)
                .forEach(u -> u.collectChildren(res));
    }

    @Override
    public String toString() {
        return String.format("%s(%s)@%s", getClass().getName(), Graphs.getName(this), Integer.toHexString(hashCode()));
    }

    /**
     * A container to hold sub-graphs.
     */
    public static class Underlying {
        protected final Collection<Graph> graphs;

        protected Underlying() {
            this.graphs = new ArrayList<>();
        }

        /**
         * Lists all sub-graphs.
         *
         * @return {@link ExtendedIterator} of sub-{@link Graph graph}s
         */
        public ExtendedIterator<Graph> listGraphs() {
            return graphs.isEmpty() ? NullIterator.instance() : WrappedIterator.create(graphs.iterator());
        }

        /**
         * Lists all sub-graphs.
         *
         * @return {@code Stream} of sub-{@link Graph graph}s
         */
        public Stream<Graph> graphs() {
            return graphs.stream();
        }

        /**
         * Answers {@code true} iff this container is empty.
         *
         * @return boolean
         */
        public boolean isEmpty() {
            return graphs.isEmpty();
        }

        /**
         * Removes the given graph from the underlying collection.
         * May be overridden to produce corresponding event.
         *
         * @param graph {@link Graph}
         */
        protected void remove(Graph graph) {
            graphs.remove(graph);
        }

        /**
         * Adds the given graph into the underlying collection.
         * May be overridden to produce corresponding event.
         *
         * @param graph {@link Graph}
         */
        protected void add(Graph graph) {
            graphs.add(Objects.requireNonNull(graph));
        }

        /**
         * Answers {@code true} if this collection depends on the specified graph.
         *
         * @param other {@link Graph}
         * @return boolean
         * @see Graph#dependsOn(Graph)
         */
        protected boolean dependsOn(Graph other) {
            for (Graph g : graphs) {
                if (Graphs.dependsOn(g, other)) return true;
            }
            return false;
        }

        /**
         * Tests if the given triple belongs to any of the sub-graphs.
         *
         * @param t {@link Triple} to test
         * @return boolean
         * @see Graph#contains(Triple)
         */
        protected boolean contains(Triple t) {
            if (graphs.isEmpty()) return false;
            for (Graph g : graphs) {
                if (g.contains(t)) return true;
            }
            return false;
        }
    }

    /**
     * An extended {@link org.apache.jena.graph.GraphEventManager Jena Graph Event Manager},
     * a holder for {@link GraphListener}s.
     */
    public static class OntEventManager extends SimpleEventManager {

        /**
         * Lists all encapsulated listeners.
         *
         * @return Stream of {@link GraphListener}s
         */
        public Stream<GraphListener> listeners() {
            return listeners.stream();
        }

        /**
         * Answers {@code true} if there is a {@link GraphListener listener}
         * which is a subtype of the specified class-type.
         *
         * @param view {@code Class}-type of {@link GraphListener}
         * @return boolean
         */
        public boolean hasListeners(Class<? extends GraphListener> view) {
            return listeners().anyMatch(l -> view.isAssignableFrom(l.getClass()));
        }
    }
}
