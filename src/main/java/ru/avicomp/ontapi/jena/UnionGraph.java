/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
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
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.graph.impl.SimpleEventManager;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.GraphSink;
import org.apache.jena.util.CollectionFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Iter;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * An Union Graph.
 * It consists of two parts: a {@link #base base} graph and an {@link #sub underlying} graph.
 * The difference between {@link MultiUnion MultiUnion} and this implementation is that
 * this graph explicitly requires primary base graph which is the only one that can be modified directly.
 * Underlying sub graphs are used only for searching; add and delete are done on the base graph.
 * Such structure allows to build graph hierarchy which is used to reference between different models.
 * The {@link PrefixMapping} of this graph is taken from the base graph,
 * and, therefore, any changes in it reflects both the base and this graph.
 * <p>
 * Created by szuev on 28.10.2016.
 *
 * @see ru.avicomp.ontapi.jena.impl.UnionModel
 */
@SuppressWarnings({"WeakerAccess", "unused", "UnusedReturnValue"})
public class UnionGraph extends CompositionBase {

    protected final Graph base;
    protected final Underlying sub;
    protected final boolean distinct;

    /**
     * @param base {@link Graph}, not {@code null}
     */
    public UnionGraph(Graph base) {
        this(base, null);
    }

    /**
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
    public void performDelete(Triple t) {
        base.delete(t);
    }

    @Override
    public void performAdd(Triple t) {
        if (!sub.contains(t))
            base.add(t);
    }

    @Override
    public OntEventManager getEventManager() {
        return (OntEventManager) gem;
    }

    /**
     * Adds the specified graph to the underlying graph collection.
     *
     * @param graph {@link Graph}, not {@code null}
     * @return this instance
     */
    public UnionGraph addGraph(Graph graph) {
        getUnderlying().addGraph(Objects.requireNonNull(graph));
        return this;
    }

    /**
     * Removes the specified graph from the underlying graph collection.
     *
     * @param graph {@link Graph}, not {@code null}
     * @return this instance
     */
    public UnionGraph removeGraph(Graph graph) {
        getUnderlying().removeGraph(Objects.requireNonNull(graph));
        return this;
    }

    /**
     * Performs find operation.
     * Override {@code graphBaseFind} to return an iterator that will report when a deletion occurs.
     */
    @Override
    protected final ExtendedIterator<Triple> graphBaseFind(Triple m) {
        return SimpleEventManager.notifyingRemove(this, createFindIterator(m));
    }

    /**
     * Creates an extended iterator to be used in {@link #find(Triple)}.
     *
     * @param m {@link Triple} pattern, not {@code null}
     * @return {@link ExtendedIterator} of {@link Triple}s
     * @see org.apache.jena.graph.compose.Union#_graphBaseFind(Triple)
     * @see MultiUnion#multiGraphFind(Triple)
     */
    protected ExtendedIterator<Triple> createFindIterator(Triple m) {
        if (sub.hasSubGraphs()) {
            if (!distinct) {
                return base.find(m).andThen(Iter.flatMap(sub.listGraphs(), x -> x.find(m)));
            }
            // The logic and comment below have been copy-pasted from the org.apache.jena.graph.compose.Union:
            // To find in the union, find in the components, concatenate the results, and omit duplicates.
            // That last is a performance penalty,
            // but I see no way to remove it unless we know the graphs do not overlap.
            Set<Triple> seen = createSet();
            return recording(base.find(m), seen)
                    .andThen(Iter.flatMap(sub.listGraphs(), x -> recording(rejecting(x.find(m), seen), seen)));
        }
        return base.find(m);
    }

    protected Set<Triple> createSet() {
        return CollectionFactory.createHashedSet();
    }

    @Override
    public boolean graphBaseContains(Triple t) {
        return base.contains(t) || sub.contains(t);
    }

    @Override
    public void close() {
        base.close();
        sub.close();
        this.closed = true;
    }

    /**
     * Generic dependsOn, true iff it depends on either of the sub-graphs.
     */
    @Override
    public boolean dependsOn(Graph other) {
        return other == this || base.dependsOn(other) || sub.dependsOn(other);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)@%s", getClass().getName(), Graphs.getName(this), Integer.toHexString(hashCode()));
    }

    /**
     * An extended {@link MultiUnion Standard Jena MultiUnion Graph} with several useful methods.
     * It has a {@link GraphSink sink-graph} as primary.
     */
    public static class Underlying extends MultiUnion {

        public Underlying() {
            super();
        }

        public Stream<Graph> graphs() {
            return m_subGraphs.stream();
        }

        public ExtendedIterator<Graph> listGraphs() {
            return WrappedIterator.create(m_subGraphs.iterator());
        }

        @Override
        public Graph getBaseGraph() {
            return GraphSink.instance();
        }

        @Override
        public List<Graph> getSubGraphs() {
            return Collections.unmodifiableList(m_subGraphs);
        }

        public boolean hasSubGraphs() {
            return !m_subGraphs.isEmpty();
        }
    }

    /**
     * An extended {@link org.apache.jena.graph.GraphEventManager Jena Graph Event Manager},
     * a holder for {@link GraphListener}s.
     */
    public static class OntEventManager extends SimpleEventManager {

        public Stream<GraphListener> listeners() {
            return listeners.stream();
        }

        public boolean hasListeners(Class<? extends GraphListener> view) {
            return listeners().anyMatch(l -> view.isAssignableFrom(l.getClass()));
        }
    }
}
