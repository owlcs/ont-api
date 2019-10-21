/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
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

import org.apache.jena.graph.*;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.jena.UnionGraph;
import com.github.owlcs.ontapi.jena.utils.Graphs;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A manager to perform transformations on a {@link Graph}.
 * Intended to transform OWL and RDFS ontological graphs, possibly with missed declarations and different RDF garbage,
 * into a strictly valid OWL2-DL graph, but may be used for any transformation purposes.
 * Used to fix "mistaken" ontologies in accordance with OWL2 specification after loading from io-stream
 * but before using common (ONT-)API.
 * <p>
 * Created by szuev on 28.10.2016.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class GraphTransformers {

    // NOTE: the order may be important
    protected static Store converters = new Store()
            .add(OWLIDTransform.class)
            .add(OWLRecursiveTransform.class)
            .add(RDFSTransform.class)
            .add(OWLCommonTransform.class)
            .add(OWLDeclarationTransform.class)
            .add(SWRLTransform.class);

    /**
     * Sets global transformers store.
     *
     * @param store {@link Store} the store
     * @return previous store
     */
    public static Store setTransformers(Store store) {
        Objects.requireNonNull(store, "Null converter store specified.");
        Store prev = converters;
        converters = store;
        return prev;
    }

    /**
     * Gets global transformers store
     *
     * @return {@link Store}
     */
    public static Store getTransformers() {
        return converters;
    }

    /**
     * Converts the given {@code Graph} to a new one using global transformation settings.
     * No changes is made on the input graph.
     *
     * @param graph input graph
     * @return output graph, <b>new</b> instance
     * @throws TransformException in case something wrong while processing
     */
    public static Graph convert(Graph graph) throws TransformException {
        Graph res = Factory.createGraphMem();
        GraphUtil.addInto(res, graph);
        return transform(res);
    }

    /**
     * Transforms the {@code Graph} according system-wide settings.
     *
     * @param graph {@link Graph}, not {@code null}
     * @return the same graph
     * @throws TransformException in case something is wrong while processing
     */
    public static Graph transform(Graph graph) throws TransformException {
        getTransformers().transform(graph);
        return graph;
    }

    /**
     * Transforms creator.
     * Extends Serializable due to OWL-API requirements.
     * As a functional interface for convenience.
     */
    @FunctionalInterface
    public interface Maker extends Serializable {

        Transform create(Graph graph);

        /**
         * Returns identifier, expected to be unique in bounds of Store.
         *
         * @return String
         */
        default String id() {
            return getClass().getName() + "@" + Objects.hashCode(this);
        }

        /**
         * Creates a Maker from {@link Predicate} and {@link Consumer} with specified id.
         * A factory method just for convenience.
         *
         * @param id        String transform identifier, not null
         * @param filter    {@link Predicate} to test Graph, nullable
         * @param transform Function to perform transformation, not null
         * @return {@link Maker}
         */
        static Maker create(String id, Predicate<Graph> filter, Consumer<Graph> transform) {
            Objects.requireNonNull(id, "Null id");
            Objects.requireNonNull(transform, "Null transform function");
            return create(id, g -> new Transform(g) {
                @Override
                public void perform() throws TransformException {
                    transform.accept(g);
                }

                @Override
                public boolean test() {
                    return filter == null || filter.test(graph);
                }

                @Override
                public String name() {
                    return id;
                }
            });
        }

        /**
         * Wraps the given function-factory as Maker.
         * A factory method just for convenience.
         *
         * @param id      String, not null
         * @param factory {@link Function}, the input {@link Graph}, the output {@link Transform}
         * @return {@link Maker}
         */
        static Maker create(String id, Function<Graph, Transform> factory) {
            Objects.requireNonNull(id, "Null id");
            Objects.requireNonNull(factory, "Null transform factory");
            return new Maker() {
                @Override
                public Transform create(Graph graph) {
                    return factory.apply(graph);
                }

                @Override
                public String id() {
                    return id;
                }
            };
        }

    }

    /**
     * Graph filter, used in Store.
     * Extends Serializable due to OWL-API requirements.
     */
    @FunctionalInterface
    public interface Filter extends Predicate<Graph>, Serializable {
    }

    /**
     * Immutable store of graph-transform Makers with predictable iteration order and 'engine' to perform transformation on a graph.
     * Extends Serializable due to OWL-API requirements, immutability is also due to OWL-API restrictions.
     *
     * @see Maker
     * @see Filter
     */
    public static class Store implements Serializable {
        private static final Logger LOGGER = LoggerFactory.getLogger(Store.class);
        private static final long serialVersionUID = -1;

        // use linked map to save inserting order:
        protected Map<String, Maker> set = new LinkedHashMap<>();
        // by default any graph is allowed to be transformed:
        protected Filter filter = g -> true;

        /**
         * Makes a deep copy of this Store instance.
         *
         * @return new instance
         */
        public Store copy() {
            Store res = empty();
            res.set.putAll(this.set);
            return res;
        }

        /**
         * Creates an empty Store with only filter copied.
         *
         * @return new instance
         */
        protected Store empty() {
            Store res = new Store();
            res.filter = this.filter;
            return res;
        }

        /**
         * Returns {@code true} if this store contains a Maker with specified id.
         *
         * @param id String
         * @return boolean
         */
        public boolean contains(String id) {
            return get(id).isPresent();
        }

        /**
         * Returns Maker for specified id.
         *
         * @param id String
         * @return Optional around {@link Maker}
         */
        public Optional<Maker> get(String id) {
            return set.containsKey(id) ? Optional.of(set.get(id)) : Optional.empty();
        }

        /**
         * Adds a Maker and returns new Store instance.
         * It is a synonym for {@code #add(Maker)}.
         *
         * @param f {@link Maker}
         * @return {@link Store}
         */
        public Store addLast(Maker f) {
            return add(f);
        }

        /**
         * Creates default implementation of {@link Maker} around the specified {@link Transform transformer}
         * and adds it to collection of new Store in immutable way.
         * It is a synonym for {@code #addLast(Maker)}.
         *
         * @param impl {@link Class} type of Transform
         * @return {@link Store}
         */
        protected Store add(Class<? extends Transform> impl) {
            return add(new DefaultMaker(impl));
        }

        /**
         * Creates a duplicate of this Store by adding the specified element-Maker at the end of the copied internal queue.
         *
         * @param f {@link Maker} to add
         * @return a copy of this store
         */
        public Store add(Maker f) {
            Store res = copy();
            res.set.put(f.id(), f);
            return res;
        }

        /**
         * Creates a duplicate of this Store by adding the specified element-Maker at the beginning of the copied internal queue.
         *
         * @param f {@link Maker} to add
         * @return a copy of this store
         */
        public Store addFirst(Maker f) {
            Store res = empty();
            res.set.put(f.id(), f);
            res.set.putAll(this.set);
            return res;
        }

        /**
         * Creates a duplicate of this Store by adding the specified element-Maker after the selected position.
         *
         * @param id String id of the maker which should go before specified
         * @param f  {@link Maker}
         * @return new Store
         */
        public Store insertAfter(String id, Maker f) {
            if (!set.containsKey(id)) throw new IllegalArgumentException("Can't find " + id);
            Store res = empty();
            set.keySet().forEach(i -> {
                res.set.put(i, set.get(i));
                if (Objects.equals(i, id)) {
                    res.set.put(f.id(), f);
                }
            });
            return res;
        }

        /**
         * Creates a duplicate of this Store without first element-Maker.
         *
         * @return a copy of this Store without first element.
         */
        public Store removeFirst() {
            if (set.isEmpty()) throw new IllegalStateException("Nothing to remove");
            return delete((String) set.keySet().toArray()[0]);
        }

        /**
         * Creates a duplicate of this Store without the last element-Maker.
         *
         * @return a copy of this Store without last element
         */
        public Store removeLast() {
            if (set.isEmpty()) throw new IllegalStateException("Nothing to remove");
            return delete((String) set.keySet().toArray()[set.size() - 1]);
        }

        /**
         * Removes Maker by specified id.
         *
         * @param id String
         * @return a copy of this Store without specified element
         */
        public Store remove(String id) {
            if (set.isEmpty()) throw new IllegalStateException("Nothing to remove");
            if (!set.containsKey(id)) throw new IllegalArgumentException("Can't find " + id);
            return delete(id);
        }

        protected Store delete(String id) {
            Store res = copy();
            res.set.remove(id);
            return res;
        }

        /**
         * Lists all makers ids.
         *
         * @return Stream of ids
         */
        public Stream<String> ids() {
            return set.keySet().stream();
        }

        /**
         * Lists all {@link Transform transformation actions} applicable to the specified graph.
         *
         * @param graph {@link Graph}
         * @return Stream of {@link Transform}s.
         */
        protected Stream<Transform> actions(Graph graph) {
            return getFilter().test(graph) ? makers().map(f -> f.create(graph)).filter(Transform::test) : Stream.empty();
        }

        /**
         * Lists all Makers.
         *
         * @return Stream  of {@link Maker}s
         */
        protected Stream<Maker> makers() {
            return set.values().stream();
        }

        /**
         * Appends the given filter to existing one, returns a copy of this Store
         *
         * @param filter {@link Filter}, not null;
         * @return new instance
         */
        public Store addFilter(Filter filter) {
            Objects.requireNonNull(filter);
            return setFilter(g -> Store.this.filter.test(g) && filter.test(g));
        }

        /**
         * Creates a copy of this Store with a new filter.
         *
         * @param f {@link Filter}
         * @return new instance
         */
        public Store setFilter(Filter f) {
            Store res = copy();
            res.filter = Objects.requireNonNull(f, "Null filter");
            return res;
        }

        /**
         * Returns encapsulated filter.
         *
         * @return {@link Filter}
         */
        public Filter getFilter() {
            return filter;
        }

        /**
         * @param graph {@link Graph} to perform operations on
         * @return {@link Stats} a transform outcome object
         * @throws TransformException if something wrong while transformations
         */
        public Stats transform(Graph graph) throws TransformException {
            return transform(graph, new HashSet<>());
        }

        /**
         * Recursively performs all graph transformations operations.
         *
         * @param graph {@link Graph}, in most cases it is {@link UnionGraph} with sub-graphs, which will be processed first
         * @param skip  Set of {@link Graph}s to exclude from transformations,
         *              it is used to avoid processing transformation multiple times on the same graph
         *              and therefore it should be modifiable
         * @return {@link Stats} a container with result
         * @throws TransformException if something is wrong
         * @see Transform
         */
        public Stats transform(Graph graph, Set<Graph> skip) throws TransformException {
            List<Graph> children = Graphs.toUnion(graph).getUnderlying().listGraphs().toList();
            Graph base = Graphs.getBase(graph);
            Stats res = new Stats(base);
            for (Graph g : children) {
                try {
                    res.putStats(transform(g, skip));
                } catch (StoreException t) {
                    throw t.putParent(graph);
                }
            }
            if (skip.contains(base)) return res;
            List<Transform> actions = actions(graph).collect(Collectors.toList());
            for (Transform action : actions) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Process <%s> on <%s>", action.name(), Graphs.getName(base)));
                }
                GraphEventManager events = base.getEventManager();
                TransformListener listener = createTrackListener();
                try {
                    events.register(listener);
                    action.perform();
                } catch (JenaException e) {
                    throw new StoreException(action, e);
                } finally {
                    events.unregister(listener);
                }
                res.putTriples(action,
                        listener.getAdded(),
                        listener.getDeleted(),
                        action.uncertainTriples()
                                .collect(Collectors.toSet()));
            }
            skip.add(base);
            return res;
        }

        protected TransformListener createTrackListener() {
            return new TransformListener();
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Store && set.equals(((Store) o).set) && filter.equals(((Store) o).filter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(set, filter);
        }
    }

    /**
     * Transform statistic object, an outcome of transformation process.
     * Notice that it holds everything in memory.
     * <p>
     * Created by @szuev on 27.06.2018.
     */
    public static class Stats {
        protected final Graph graph;
        protected Map<Type, Map<String, Set<Triple>>> triples = new EnumMap<>(Type.class);
        protected Set<Stats> sub = new HashSet<>();

        protected Stats(Graph graph) {
            this.graph = Objects.requireNonNull(graph);
        }

        protected void putTriples(Transform transform,
                                  Set<Triple> added,
                                  Set<Triple> deleted,
                                  Set<Triple> unparsed) {
            String name = transform.name();
            put(Type.ADDED, name, added);
            put(Type.DELETED, name, deleted);
            put(Type.UNPARSED, name, unparsed);
        }

        protected void put(Type type, String name, Set<Triple> triples) {
            map(type).computeIfAbsent(name, s -> new HashSet<>()).addAll(triples);
        }

        protected void putStats(Stats other) {
            this.sub.add(other);
        }

        public Set<Triple> getTriples(Type type, String name) {
            return getUnmodifiable(map(type), name);
        }

        public Stream<Triple> triples(Type type) {
            return map(type).values().stream().flatMap(Collection::stream);
        }

        public boolean hasTriples(Type type, String name) {
            if (!triples.containsKey(type)) return false;
            if (!triples.get(type).containsKey(name)) return false;
            return !triples.get(type).get(name).isEmpty();
        }

        public boolean hasTriples(Type type) {
            if (!triples.containsKey(type)) return false;
            return triples.get(type).values().stream().anyMatch(x -> !x.isEmpty());
        }

        public boolean hasTriples() {
            return !triples.isEmpty();
        }

        public boolean isNotEmpty() {
            return hasTriples() && Arrays.stream(Type.values()).anyMatch(this::hasTriples);
        }

        protected Map<String, Set<Triple>> map(Type type) {
            return triples.computeIfAbsent(type, t -> new HashMap<>());
        }

        public Graph getGraph() {
            return graph;
        }

        /**
         * Lists all encapsulated Stats object.
         *
         * @param deep if {@code true} all sub-stats will be included recursively in the result stream also,
         *             otherwise only top-level sub-stats are expected in the return stream
         * @return Stream of {@link Stats}
         */
        public Stream<Stats> listStats(boolean deep) {
            if (!deep) return sub.stream();
            return sub.stream().flatMap(s -> Stream.concat(Stream.of(s), s.listStats(true)));
        }

        private static <T> Set<T> getUnmodifiable(Map<String, Set<T>> map, String key) {
            return map.containsKey(key) ? Collections.unmodifiableSet(map.get(key)) : Collections.emptySet();
        }

        public enum Type {
            ADDED,
            DELETED,
            UNPARSED
        }
    }

    /**
     * Listener to control graph changes while transformations.
     * Note: it keeps any tracked (added and removed) {@code Triple} in memory.
     * This may be inappropriate for a huge graphs containing a lot of missed declarations.
     * <p>
     * Created by @szuev on 27.06.2018.
     */
    public static class TransformListener extends GraphListenerBase {

        private final Set<Triple> added = new HashSet<>();
        private final Set<Triple> deleted = new HashSet<>();

        @Override
        protected void addEvent(Triple t) {
            added.add(t);
            deleted.remove(t);
        }

        @Override
        protected void deleteEvent(Triple t) {
            added.remove(t);
            deleted.add(t);
        }

        @Override
        public void notifyAddGraph(Graph g, Graph other) {
            other.find(Triple.ANY).forEachRemaining(this::addEvent);
        }

        @Override
        public void notifyDeleteGraph(Graph g, Graph other) {
            other.find(Triple.ANY).forEachRemaining(this::deleteEvent);
        }

        public Set<Triple> getAdded() {
            return Collections.unmodifiableSet(added);
        }

        public Set<Triple> getDeleted() {
            return Collections.unmodifiableSet(deleted);
        }
    }

    /**
     * An exception, which can be thrown by {@link Store#transform(Graph, Set)} method.
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
            StringBuilder sb = new StringBuilder();
            if (this.parent != null) {
                sb.append(Graphs.getName(this.parent)).append(" => ");
            }
            sb.append(transform);
            Throwable cause = getCause();
            if (cause != null) {
                sb.append(": ").append(cause.getMessage());
            }
            return sb.toString();
        }
    }

    /**
     * Default impl of {@link Maker}.
     */
    public static class DefaultMaker implements Maker {
        protected final Class<? extends Transform> impl;

        public DefaultMaker(Class<? extends Transform> impl) throws IllegalArgumentException {
            try {
                if (!Modifier.isPublic(impl.getDeclaredConstructor(Graph.class).getModifiers())) {
                    throw new IllegalArgumentException(impl.getName() + ": no public constructor.");
                }
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(impl.getName() + " must have public constructor with " + Graph.class.getName() + " as the only parameter.", e);
            }
            this.impl = impl;
        }

        @Override
        public Transform create(Graph graph) throws IllegalStateException {
            try {
                return impl.getDeclaredConstructor(Graph.class).newInstance(graph);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException("Can't init " + impl.getName(), e);
            }
        }

        @Override
        public String id() {
            return impl.getName();
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof DefaultMaker && impl.equals(((DefaultMaker) o).impl);
        }

        @Override
        public int hashCode() {
            return impl.hashCode();
        }

        @Override
        public String toString() {
            return String.format("DefaultMaker{impl=%s}", impl);
        }
    }

}