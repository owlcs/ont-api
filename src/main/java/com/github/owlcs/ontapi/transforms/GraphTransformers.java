/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.transforms;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.OntGraphUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphEventManager;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.UnionGraph;
import org.apache.jena.ontapi.impl.GraphListenerBase;
import org.apache.jena.ontapi.utils.Graphs;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.graph.GraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a manager to perform transformations over a {@link Graph RDF Graph}.
 * It is intended to transform a graphs into a form containing OWL2 data,
 * but may be used for any other transformation purposes.
 * In the system, it is used to fix "mistaken" ontologies in accordance with OWL2 specification
 * after loading data from IO-stream but before the rest of ONT-API come into play.
 * The manager is designed as an immutable queue of {@link Transform graph-transform}s
 * with user-defined iteration order and 'engine' to perform transformation on a graph.
 * It extends {@code Serializable} due to OWL-API requirements,
 * immutability is also due to OWL-API restrictions.
 *
 * @see Transform
 * @see GraphFilter
 * @see GraphStats
 */
public class GraphTransformers implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphTransformers.class);
    @Serial
    private static final long serialVersionUID = -1;

    // NOTE: the order is important
    protected static GraphTransformers instance = new GraphTransformers()
            .put(OWLIDTransform.class)
            .put(OWLRecursiveTransform.class)
            .put(RDFSTransform.class)
            .put(OWLCommonTransform.class)
            .put(OWLDeclarationTransform.class)
            .put(SWRLTransform.class);

    // use linked map to save inserting order:
    protected final Map<String, Transform> set = new LinkedHashMap<>();
    // by default any graph is allowed to be transformed:
    protected GraphFilter filter = GraphFilter.TRUE;

    /**
     * Sets the global (system-wide) transformation manager.
     *
     * @param store {@link GraphTransformers} the transformation manager, not {@code null}
     * @return the previously associated transformation manager
     */
    public static GraphTransformers set(GraphTransformers store) {
        Objects.requireNonNull(store, "Null converter store specified.");
        GraphTransformers prev = instance;
        instance = store;
        return prev;
    }

    /**
     * Gets the global (system-wide) transformers transformation manager.
     *
     * @return {@link GraphTransformers}
     */
    public static GraphTransformers get() {
        return instance;
    }

    /**
     * Converts the given {@code Graph} to a new in-memory graph using the global transformation settings.
     * No changes are made on the input graph.
     *
     * @param from the input {@link Graph graph}, not {@code null}
     * @return the output {@link Graph}, <b>new</b> in-memory instance
     * @throws TransformException in case something wrong while processing
     */
    public static Graph convert(Graph from) throws TransformException {
        Graph res = GraphFactory.createGraphMem();
        GraphUtil.addInto(res, from);
        get().transform(res);
        return res;
    }

    /**
     * Makes a deep copy of this {@link GraphTransformers Store} instance.
     *
     * @return a new instance
     */
    public GraphTransformers copy() {
        GraphTransformers res = empty();
        res.set.putAll(this.set);
        return res;
    }

    /**
     * Creates an empty transformation manager with the only filter copied.
     *
     * @return new instance
     */
    protected GraphTransformers empty() {
        GraphTransformers res = new GraphTransformers();
        res.filter = this.filter;
        return res;
    }

    /**
     * Finds a {@link Transform} by its identifier.
     *
     * @param id - the transform identifier, not {@code null}
     * @return {@code Optional} around {@link Transform}
     * @see Transform#id()
     */
    public Optional<Transform> get(String id) {
        return set.containsKey(id) ? Optional.of(set.get(id)) : Optional.empty();
    }

    /**
     * Creates a default implementation of {@link Transform}
     * around the specified {@link TransformationModel transformer}
     * and adds it to the collection of new manager in the immutable way.
     *
     * @param impl {@link Class}, the type of {@link Transform}, not {@code null}
     * @return {@link GraphTransformers}
     */
    protected GraphTransformers put(Class<? extends TransformationModel> impl) {
        return addLast(Transform.Factory.create(impl));
    }

    /**
     * Creates a duplicate of this {@link GraphTransformers manager}
     * by adding the specified element-{@link Transform} at the beginning of the copied internal queue.
     *
     * @param tf {@link Transform} to add, not {@code null}
     * @return a copy of this {@link GraphTransformers}
     */
    public GraphTransformers addFirst(Transform tf) {
        Objects.requireNonNull(tf);
        GraphTransformers res = empty();
        res.set.put(tf.id(), tf);
        res.set.putAll(this.set);
        return res;
    }

    /**
     * Creates a duplicate of this {@link GraphTransformers store} by adding
     * the specified element-transform at the end of the copied internal queue.
     *
     * @param tf {@link Transform}, not {@code null}
     * @return {@link GraphTransformers}
     */
    public GraphTransformers addLast(Transform tf) {
        Objects.requireNonNull(tf);
        GraphTransformers res = copy();
        res.set.put(tf.id(), tf);
        return res;
    }

    /**
     * Creates a duplicate of this {@link GraphTransformers manager}
     * by adding the specified element-{@link Transform} after the selected position.
     *
     * @param id {@code String}, the identifier of the transform that should go before the specified one, not {@code null}
     * @param tf {@link Transform} to insert, not {@code null}
     * @return a copy of this {@link GraphTransformers}
     * @see Transform#id()
     */
    public GraphTransformers insertAfter(String id, Transform tf) {
        Objects.requireNonNull(tf);
        if (!set.containsKey(id)) {
            throw new IllegalArgumentException("Can't find " + id);
        }
        GraphTransformers res = empty();
        set.keySet().forEach(i -> {
            res.set.put(i, set.get(i));
            if (Objects.equals(i, id)) {
                res.set.put(tf.id(), tf);
            }
        });
        return res;
    }

    /**
     * Creates a duplicate of this {@link GraphTransformers manager}
     * but without the first element-{@link Transform}.
     *
     * @return a copy of this {@link GraphTransformers Store} without the first element
     */
    public GraphTransformers removeFirst() {
        if (set.isEmpty()) throw new IllegalStateException("Nothing to remove");
        return delete((String) set.keySet().toArray()[0]);
    }

    /**
     * Creates a duplicate of this {@link GraphTransformers manager}
     * but without the last element-{@link Transform}.
     *
     * @return a copy of this {@link GraphTransformers Store} without the last element
     */
    public GraphTransformers removeLast() {
        if (set.isEmpty()) throw new IllegalStateException("Nothing to remove");
        return delete((String) set.keySet().toArray()[set.size() - 1]);
    }

    /**
     * Removes the {@link Transform} by its id.
     *
     * @param id {@code String}, the identifier of the transform to be removed, not {@code null}
     * @return a copy of this {@link GraphTransformers Store} without the specified element
     */
    public GraphTransformers remove(String id) {
        if (set.isEmpty()) {
            throw new IllegalStateException("Nothing to remove");
        }
        if (!set.containsKey(id)) {
            throw new IllegalArgumentException("Can't find " + id);
        }
        return delete(id);
    }

    protected GraphTransformers delete(String id) {
        GraphTransformers res = copy();
        res.set.remove(id);
        return res;
    }

    /**
     * Lists all {@link Transform}s.
     *
     * @return a {@code Stream} of {@link Transform}s
     */
    public Stream<Transform> transforms() {
        return set.values().stream();
    }

    /**
     * Lists types of default {@link Transform}'s.
     *
     * @return a {@code Stream} of {@link Transform}s
     */
    public Stream<Class<? extends TransformationModel>> serializableTypes() {
        return set.values().stream()
                .filter(it -> it instanceof Transform.Factory.DefaultMaker)
                .map(it -> ((Transform.Factory.DefaultMaker) it).impl);
    }

    /**
     * Creates a copy of this {@link GraphTransformers Store} with a new filter.
     *
     * @param f {@link GraphFilter}, not {@code null}
     * @return a copy of this {@link GraphTransformers manager} with new filter
     */
    public GraphTransformers setFilter(GraphFilter f) {
        GraphTransformers res = copy();
        res.filter = Objects.requireNonNull(f, "Null filter");
        return res;
    }

    /**
     * Returns the encapsulated filter.
     *
     * @return {@link GraphFilter}
     */
    public GraphFilter getFilter() {
        return filter;
    }

    /**
     * Performs all graph transformations operations.
     *
     * @param graph {@link Graph} to perform operations on, not {@code null}
     * @return {@link GraphStats} a transform outcome object, not {@code null}
     * @throws TransformException if something wrong while transformations
     */
    public GraphStats transform(Graph graph) throws TransformException {
        return transform(graph, new HashSet<>());
    }

    /**
     * Recursively performs all graph transformations operations.
     *
     * @param graph {@link Graph}, in most cases it is {@link UnionGraph} with sub-graphs, which will be processed first
     * @param skip  a {@code Set} of {@link Graph}s to exclude from transformations,
     *              it is used to avoid processing transformation multiple times on the same graph,
     *              and therefore it should be modifiable
     * @return {@link GraphStats} a container with result
     * @throws TransformException if something is wrong
     */
    public GraphStats transform(Graph graph, Set<Graph> skip) throws TransformException {
        UnionGraph u = Graphs.makeOntUnionFrom(graph, OntModelFactory::createUnionGraph);
        List<Graph> children = u.subGraphs().toList();
        Graph base = u.getBaseGraph();
        GraphStats res = new GraphStats(base);
        for (Graph g : children) {
            try {
                res.putStats(transform(g, skip));
            } catch (StoreException t) {
                throw t.putParent(graph);
            }
        }
        if (skip.contains(base)) {
            return res;
        }
        if (!getFilter().test(graph)) {
            skip.add(base);
            return res;
        }
        transforms()
                .filter(x -> x.test(graph))
                .forEach(x -> {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format("Process <%s> on <%s>", x.id(), OntGraphUtils.getOntologyGraphPrintName(base)));
                    }
                    GraphEventManager events = base.getEventManager();
                    TransformListener listener = createTrackListener();
                    Set<Triple> uncertainTriples;
                    try {
                        events.register(listener);
                        uncertainTriples = x.apply(graph).collect(Collectors.toSet());
                    } catch (JenaException e) {
                        throw new StoreException(x, e);
                    } finally {
                        events.unregister(listener);
                    }
                    res.putTriples(x,
                            listener.getAdded(),
                            listener.getDeleted(),
                            uncertainTriples);

                });
        skip.add(base);
        return res;
    }

    protected TransformListener createTrackListener() {
        return new TransformListener();
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof GraphTransformers
                && set.equals(((GraphTransformers) o).set)
                && filter.equals(((GraphTransformers) o).filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(set, filter);
    }

    /**
     * The listener to control graph changes while transformations.
     * Note: it keeps any tracked (added and removed) {@code Triple} in memory.
     * This may be inappropriate for a huge graphs containing a lot of missed declarations.
     * <p>
     * Created by @ssz on 27.06.2018.
     */
    public static class TransformListener extends GraphListenerBase {

        private final Set<Triple> added = new HashSet<>();
        private final Set<Triple> deleted = new HashSet<>();

        @Override
        protected void addTripleEvent(Graph g, Triple t) {
            added.add(t);
            deleted.remove(t);
        }

        @Override
        protected void deleteTripleEvent(Graph g, Triple t) {
            added.remove(t);
            deleted.add(t);
        }

        @Override
        public void notifyAddGraph(Graph g, Graph other) {
            other.find(Triple.ANY).forEachRemaining(t -> addTripleEvent(g, t));
        }

        @Override
        public void notifyDeleteGraph(Graph g, Graph other) {
            other.find(Triple.ANY).forEachRemaining(t -> deleteTripleEvent(g, t));
        }

        public Set<Triple> getAdded() {
            return Collections.unmodifiableSet(added);
        }

        public Set<Triple> getDeleted() {
            return Collections.unmodifiableSet(deleted);
        }
    }

    /**
     * An exception, which can be thrown by {@link GraphTransformers#transform(Graph, Set)} method.
     * It is a {@link JenaException}.
     */
    public static class StoreException extends TransformException {
        protected final Transform transform;
        protected Graph parent;

        protected StoreException(Transform transform, Throwable cause) {
            super(cause);
            this.transform = OntApiException.notNull(transform, "Null transform");
        }

        protected StoreException putParent(Graph graph) {
            this.parent = OntApiException.notNull(graph, "Null parent graph");
            return this;
        }

        @Override
        public String getMessage() {
            StringBuilder res = new StringBuilder();
            if (this.parent != null) {
                res.append(OntGraphUtils.getOntologyGraphPrintName(this.parent)).append(" => ");
            }
            res.append(transform);
            Throwable cause = getCause();
            if (cause != null) {
                res.append(": ").append(cause.getMessage());
            }
            return res.toString();
        }
    }
}
