/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;

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
     */
    public static Graph convert(Graph graph) {
        getTransformers().actions(graph).forEach(Transform::process);
        return graph;
    }

    /**
     * Transforms creator.
     * @param <GC> {@link Transform}
     */
    @FunctionalInterface
    public interface Maker<GC extends Transform> extends Serializable {
        GC create(Graph graph);
    }

    /**
     * Immutable store of graph-transform makers
     *
     * @see Maker
     */
    public static class Store implements Serializable {
        protected Set<Maker> set = new LinkedHashSet<>();

        public Store copy() {
            Store res = new Store();
            res.set = new LinkedHashSet<>(this.set);
            return res;
        }

        public Store add(Maker f) {
            Store res = copy();
            res.set.add(f);
            return res;
        }

        public Store addFirst(Maker f) {
            Store res = new Store();
            res.set.add(f);
            res.set.addAll(this.set);
            return res;
        }

        public Store remove(Maker f) {
            Store res = copy();
            res.set.remove(f);
            return res;
        }

        public Stream<Transform> actions(Graph graph) {
            return set.stream().map(f -> f.create(graph));
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
