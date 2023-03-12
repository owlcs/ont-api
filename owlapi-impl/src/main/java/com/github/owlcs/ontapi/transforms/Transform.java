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

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents a transformation operation
 * that accepts {@link Graph RDF Graph} and returns a set of {@link Triple RDF Triple}s.
 * It is designed as a functional interface for convenience
 * and extends {@link Serializable} due to OWL-API requirements.
 *
 * @see Factory
 */
@FunctionalInterface
public interface Transform extends Serializable {

    /**
     * Performs the transformation operation over the specified {@code Graph},
     * returning {@code Triple}s that cannot be handled property by this transform-operator
     *
     * @param g {@link Graph RDF Graph}, not {@code null}
     * @return a {@code Stream} of unparsable {@link Triple}s
     */
    Stream<Triple> apply(Graph g);

    /**
     * Answers {@code true} iff
     * the specified {@code Graph} is good enough to perform the {@link #apply(Graph) transformation}.
     *
     * @param g {@link Graph RDF Graph}, not {@code null}
     * @return boolean, {@code true} if the operation can be performed, {@code false} otherwise
     */
    default boolean test(Graph g) {
        return true;
    }

    /**
     * Returns the operation identifier.
     * It is expected to be unique within the bounds of {@link GraphTransformers}.
     *
     * @return {@code String}
     */
    default String id() {
        return getClass().getName() + "@" + Objects.hashCode(this);
    }

    /**
     * A factory to produce {@link Transform}s.
     *
     * @see TransformationModel
     */
    class Factory {

        /**
         * Creates a Maker from {@link Predicate} and {@link Consumer} with specified id.
         * A factory method just for convenience.
         *
         * @param id        {@code String} transform identifier, not {@code null}
         * @param filter    {@link Predicate} to test Graph, nullable
         * @param transform Function to perform transformation, not {@code null}
         * @return {@link Transform}
         */
        public static Transform create(String id, Predicate<Graph> filter, Consumer<Graph> transform) {
            Objects.requireNonNull(id, "Null id");
            Objects.requireNonNull(transform, "Null transform function");
            return create(id, g -> new TransformationModel(g) {
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
         * @param id      {@code String}, not {@code null}
         * @param factory {@link Function} which accepts {@link Graph} and produces {@link TransformationModel}
         * @return {@link Transform}
         */
        public static Transform create(String id, Function<Graph, TransformationModel> factory) {
            Objects.requireNonNull(id, "Null id");
            Objects.requireNonNull(factory, "Null transform factory");
            return new Transform() {
                @Override
                public Stream<Triple> apply(Graph g) {
                    TransformationModel r = factory.apply(g);
                    r.perform();
                    return r.uncertainTriples();
                }

                @Override
                public boolean test(Graph g) {
                    return factory.apply(g).test();
                }

                @Override
                public String id() {
                    return id;
                }
            };
        }

        /**
         * Creates a {@link Transform} using the {@link TransformationModel}.
         * @param impl {@code Class}-type of {@link TransformationModel}, not {@code null}
         * @return {@link Transform}
         */
        public static Transform create(Class<? extends TransformationModel> impl) {
            return new DefaultMaker(impl);
        }

        /**
         * The default impl of {@link Transform}, which is based on {@link TransformationModel}.
         */
        public static class DefaultMaker implements Transform {
            protected final Class<? extends TransformationModel> impl;

            protected DefaultMaker(Class<? extends TransformationModel> impl) throws IllegalArgumentException {
                this.impl = checkHasOneParameterConstructor(impl, Graph.class);
            }

            @SuppressWarnings("SameParameterValue")
            public static <X> Class<X> checkHasOneParameterConstructor(Class<X> impl, Class<?> param) {
                try {
                    if (!Modifier.isPublic(impl.getDeclaredConstructor(param).getModifiers())) {
                        throw new IllegalArgumentException(impl.getName() + ": no public constructor.");
                    }
                } catch (NoSuchMethodException e) {
                    throw new IllegalArgumentException(impl.getName() +
                            " must have public constructor with " + param.getName() + " as the only parameter.", e);
                }
                return impl;
            }

            public TransformationModel create(Graph graph) throws IllegalStateException {
                try {
                    return impl.getDeclaredConstructor(Graph.class).newInstance(graph);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalStateException("Can't init " + impl.getName(), e);
                }
            }

            @Override
            public Stream<Triple> apply(Graph g) {
                TransformationModel r = create(g);
                r.perform();
                return r.uncertainTriples();
            }

            @Override
            public boolean test(Graph g) {
                return create(g).test();
            }

            @Override
            public String id() {
                if (DefaultMaker.class.getPackage().equals(impl.getPackage()))
                    return impl.getSimpleName();
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

}
