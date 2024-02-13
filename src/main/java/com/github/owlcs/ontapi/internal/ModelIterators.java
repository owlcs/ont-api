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

package com.github.owlcs.ontapi.internal;

import com.github.sszuev.jena.ontapi.utils.Iterators;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper; a collection of auxiliary methods to work with {@link ExtendedIterator}s and {@code Stream}s
 * in the context of {@link InternalGraphModel}.
 * <p>
 * In case of {@link InternalConfig#parallel()} is {@code true}
 * any stream\iterator providing by the model should have immutable base, in other words they cannot be try-lazy,
 * otherwise there is no possibility to guarantee thread-safety.
 * <p>
 * Created by @ssz on 10.05.2020.
 *
 * @see Iterators
 */
class ModelIterators {

    /**
     * Performs a final operation over the specified {@code stream} before releasing it out.
     * <p>
     * It is for ensuring safety in case of multithreading environment,
     * as indicated by the parameter {@link InternalConfig#parallel()}.
     * If {@code parallel} is {@code true} and {@code stream} is unknown nature
     * then the collecting must not go beyond this method, otherwise it is allowed to be lazy.
     * Although the upper API uses {@code ReadWriteLock R/W lock} everywhere
     * (that is an original OWL-API locking style), it does not guarantee thread-safety on iterating,
     * and, therefore, without the help of this method,
     * there is a danger of {@link java.util.ConcurrentModificationException} (at best),
     * if some processing go outside a method who spawned the stream, in spite of the dedicated lock-section.
     * So need to make sure stream is created from a snapshot state.
     * <p>
     * Notice that this class does not produce parallel streams.
     * It comes with danger live locks or even deadlocks while interacting with loading-caches,
     * since all of them are based on the standard Java {@code ConcurrentHashMap}.
     *
     * @param stream {@code Stream} of {@link R}s, expected to be distinct
     * @param conf   {@link InternalConfig} to configure behaviour
     * @param <R>    anything
     * @return a {@code Stream} of {@link R}s
     */
    static <R> Stream<R> reduce(Stream<R> stream, InternalConfig conf) {
        // model is non-modifiable if cache is disabled
        if (!conf.parallel() || !conf.useContentCache()) {
            return stream;
        }
        // use ArrayList since it is faster while iterating,
        // Uniqueness is guaranteed by other mechanisms.
        // 1024 is a magic approximate number of axioms/objects; it is not tested yet.
        ArrayList<R> res = new ArrayList<>(1024);
        stream.forEach(res::add);
        res.trimToSize();
        return res.stream();
    }

    /**
     * Performs a final operation over the specified {@code stream} before releasing it out.
     *
     * @param stream {@code ExtendedIterator} of {@link R}s, expected to be distinct
     * @param conf   {@link InternalConfig} to configure behaviour
     * @param <R>    anything
     * @return a {@code Stream} of {@link R}s
     */
    static <R> Stream<R> reduce(ExtendedIterator<R> stream, InternalConfig conf) {
        if (!conf.parallel() || !conf.useContentCache()) {
            return Iterators.asStream(stream);
        }
        ArrayList<R> res = new ArrayList<>(1024);
        stream.forEachRemaining(res::add);
        res.trimToSize();
        return res.stream();
    }

    /**
     * Performs a final actions over the given stream trying to make it distinct.
     * If there is no content cache inside a model - the stream is returning as it is.
     *
     * @param stream {@code ExtendedIterator} of {@link R}s, expected to be distinct
     * @param conf   {@link InternalConfig} to configure behaviour
     * @param <R>    anything
     * @return a {@code Stream} of {@link R}s (distinct in case of default settings)
     */
    static <R> Stream<R> reduceDistinct(ExtendedIterator<R> stream, InternalConfig conf) {
        if (!conf.useContentCache()) {
            // no content cache -> no sense to provide a distinct stream
            return Iterators.asStream(stream);
        }
        // make the result to be distinct:
        if (conf.parallel()) {
            // no calculations should go beyond this method in case of parallel usage
            return Iterators.addAll(stream, new LinkedHashSet<>()).stream();
        }
        // lazy distinct
        return Iterators.asStream(Iterators.distinct(stream), Spliterator.NONNULL | Spliterator.DISTINCT);
    }

    /**
     * Performs a final actions over the given stream trying to make it distinct.
     *
     * @param stream {@code Stream} of {@link R}s, expected to be distinct
     * @param conf   {@link InternalConfig} to configure behaviour
     * @param <R>    anything
     * @return a {@code Stream} of {@link R}s (distinct in case of default settings)
     * @see #reduceDistinct(ExtendedIterator, InternalConfig)
     */
    static <R> Stream<R> reduceDistinct(Stream<R> stream, InternalConfig conf) {
        if (!conf.useContentCache()) {
            return stream;
        }
        if (conf.parallel()) {
            // snapshot:
            return stream.collect(Collectors.toCollection(LinkedHashSet::new)).stream();
        }
        // lazy distinct
        return stream.distinct();
    }

    /**
     * Returns a stream consisting of the results of replacing each element of this stream
     * with the contents of a mapped stream produced by applying the provided mapping function to each element.
     * The purpose of this method is the same as for {@link #reduce(Stream, InternalConfig)}:
     * for thread-safety reasons calculations should not go beyond the bounds of this method.
     *
     * @param stream {@code Stream} of {@link X}
     * @param map    a {@link Function} for mapping {@link X} to {@code Stream} of {@link R}
     * @param conf   {@link InternalConfig} to configure behaviour
     * @param <R>    anything
     * @param <X>    anything
     * @return a {@code Stream} of {@link R}s
     */
    @SuppressWarnings("DataFlowIssue")
    static <R, X> Stream<R> flatMap(Stream<X> stream, Function<X, Stream<? extends R>> map, InternalConfig conf) {
        if (!conf.parallel() || !conf.useContentCache()) {
            return stream.flatMap(map);
        }
        // force put everything into cache (memory) and get data snapshot
        // for now there is no any better solution
        return stream.map(map).collect(Collectors.toList()).stream().flatMap(Function.identity());
    }

}
