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

package ru.avicomp.ontapi.transforms;

import org.apache.jena.graph.Graph;
import org.apache.jena.shared.JenaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.utils.Graphs;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class to perform some transformation action on the specified graph.
 * Currently it is to convert the OWL and RDFS ontological graphs to the OWL2-DL graph and to fix missed declarations.
 * Can be used to fix "mistaken" ontologies in accordance with OWL2 specification after loading from io-stream
 * but before using common (ONT-)API.
 * <p>
 * Created by szuev on 28.10.2016.
 */
@SuppressWarnings("WeakerAccess")
public abstract class GraphTransformers {

    // NOTE: the order may be important
    protected static Store converters = new Store()
            .add(OWLIDTransform::new)
            .add(OWLRecursiveTransform::new)
            .add(RDFSTransform::new)
            .add(OWLCommonTransform::new)
            .add(OWLDeclarationTransform::new);

    /**
     * Sets global transformers store
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
     * A helper method to perform {@link Graph graph} transformation using global settings.
     * Note: it returns the same graph, not a fixed copy.
     *
     * @param graph input graph
     * @return output graph
     * @throws TransformException in case something wrong while processing
     */
    public static Graph convert(Graph graph) throws TransformException {
        getTransformers().transform(graph);
        return graph;
    }

    /**
     * Transforms creator.
     * Extends Serializable due to OWL-API requirements.
     *
     * @param <GC> {@link Transform}
     */
    @FunctionalInterface
    public interface Maker<GC extends Transform> extends Serializable {
        GC create(Graph graph);

        /**
         * Returns identifier, expected to be unique in bounds of store
         *
         * @return String
         */
        default String id() {
            return toString();
        }
    }

    /**
     * Immutable store of graph-transform makers and engine to perform transformation on graph.
     *
     * @see Maker
     */
    public static class Store implements Serializable {
        protected static final Logger LOGGER = LoggerFactory.getLogger(Store.class);
        protected Map<String, Maker> set = new LinkedHashMap<>();

        /**
         * Makes a deep copy of this Store instance.
         *
         * @return new instance
         */
        public Store copy() {
            Store res = new Store();
            res.set = new LinkedHashMap<>(this.set);
            return res;
        }

        /**
         * Adds the specified Maker to the end of the queue in this Store.
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
         * Inserts the specified element Maker at the beginning of the queue in this Store.
         *
         * @param f {@link Maker} to add
         * @return a copy of this store
         */
        public Store addFirst(Maker f) {
            Store res = new Store();
            res.set.put(f.id(), f);
            this.set.forEach((k, v) -> res.set.put(k, v));
            return res;
        }

        /**
         * Removes the first element Maker from internal queue from this Store.
         *
         * @return a copy of this store without first element.
         */
        public Store removeFirst() {
            String key = set.keySet().stream().findFirst().orElseThrow(() -> new IllegalStateException("Nothing to remove"));
            Store res = copy();
            res.set.remove(key);
            return res;
        }

        /**
         * Removes the last element Maker from internal queue from this Store.
         *
         * @return a copy of this store without last element.
         */
        public Store remove() {
            if (set.isEmpty()) throw new IllegalStateException("Nothing to remove");
            String key = (String) set.keySet().toArray()[set.size() - 1];
            Store res = copy();
            res.set.remove(key);
            return res;
        }

        public boolean contains(String id) {
            return set.containsKey(id);
        }

        public Stream<Maker> makers() {
            return set.values().stream();
        }

        public Stream<Transform> actions(Graph graph) {
            return makers().map(f -> f.create(graph)).filter(Transform::test);
        }

        /**
         * @param graph {@link Graph} to perform operations on
         * @throws TransformException if something wrong while transformations
         */
        public void transform(Graph graph) throws TransformException {
            transform(graph, new HashSet<>());
        }

        /**
         * Recursively performs graph transformation.
         * todo: should return transform statistic object.
         *
         * @param graph     {@link Graph}, in most cases it is {@link UnionGraph} with sub-graphs, which will be processed first.
         * @param processed Set of {@link Graph}s to avoid processing transformation multiple times on the same graph.
         * @throws TransformException if something is wrong
         * @see Transform
         */
        public void transform(Graph graph, Set<Graph> processed) throws TransformException {
            List<Graph> children = Graphs.subGraphs(graph).collect(Collectors.toList());
            for (Graph g : children) {
                try {
                    transform(g, processed);
                } catch (StoreException t) {
                    throw t.putParent(graph);
                }
            }
            Graph base = Graphs.getBase(graph);
            if (processed.contains(base)) return;
            List<Transform> actions = actions(graph).collect(Collectors.toList());
            for (Transform action : actions) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("Process <%s> on <%s>", action.name(), Graphs.getName(base)));
                }
                try {
                    action.perform();
                } catch (JenaException e) {
                    throw new StoreException(action, e);
                }
            }
            processed.add(base);
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Store && set.equals(((Store) o).set);
        }

        @Override
        public int hashCode() {
            return set.hashCode();
        }
    }

    private static class StoreException extends TransformException {
        private final Transform transform;
        private Graph parent;

        public StoreException(Transform transform, Throwable cause) {
            super(cause);
            this.transform = OntApiException.notNull(transform, "Null transform");
        }

        public StoreException putParent(Graph graph) {
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
     * Default impl of {@link Maker}
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
