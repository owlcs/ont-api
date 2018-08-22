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

package ru.avicomp.ontapi.jena.utils;

import org.apache.jena.atlas.iterator.FilterUnique;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.StmtIteratorImpl;
import org.apache.jena.util.iterator.ClosableIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Misc utils to work with Iterators, Streams, Collections, etc,
 * which are related somehow to Jena or/and used inside {@link ru.avicomp.ontapi.jena} package.
 * Created by szuev on 11.04.2017.
 *
 * @see org.apache.jena.util.iterator.ExtendedIterator
 * @see org.apache.jena.atlas.iterator.Iter
 * @see ClosableIterator
 */
public class Iter {

    /**
     * Wraps CloseableIterator as Stream.
     * Don't forget to call explicit {@link Stream#close()} if the inner iterator are not exhausted
     * ({@link Iterator#hasNext()} is still true).
     * It seems it should be called for such operations as {@link Stream#findFirst()}, {@link Stream#findAny()}, {@link Stream#anyMatch(Predicate)} etc.
     *
     * @param iterator {@link ClosableIterator}
     * @param <T>      the class-type of iterator
     * @return Stream
     */
    public static <T> Stream<T> asStream(ClosableIterator<T> iterator) {
        return org.apache.jena.atlas.iterator.Iter.asStream(iterator).onClose(iterator::close);
    }

    /**
     * Creates an iterator which returns RDF Statements based on the given extended iterator of triples.
     *
     * @param triples {@link ExtendedIterator} of {@link Triple}s
     * @param map     a Function to map {@link Triple} -&gt; {@link Statement}
     * @return {@link StmtIterator}
     * @see org.apache.jena.rdf.model.impl.IteratorFactory#asStmtIterator(Iterator, org.apache.jena.rdf.model.impl.ModelCom)
     */
    public static StmtIterator createStmtIterator(ExtendedIterator<Triple> triples, Function<Triple, Statement> map) {
        return new StmtIteratorImpl(triples.mapWith(map));
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new unmodifiable {@code Set}.
     *
     * @param <T> The type of input elements for the new collector
     * @return a {@link Collector} which collects all the input elements into a unmodifiable {@code Set}
     * @see Collectors#toSet()
     */
    public static <T> Collector<T, ?, Set<T>> toUnmodifiableSet() {
        return Collectors.collectingAndThen(Collectors.toSet(), Collections::unmodifiableSet);
    }

    /**
     * Returns a {@code Collector} that accumulates the input elements into a new unmodifiable {@code List}.
     *
     * @param <T> the type of the input elements
     * @return a {@code Collector} which collects all the input elements into a unmodifiable {@code List}, in encounter order
     * @see Collectors#toList()
     */
    public static <T> Collector<T, ?, List<T>> toUnmodifiableList() {
        return Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList);
    }

    /**
     * Returns an {@link ExtendedIterator Extended Iterator} consisting of the results of replacing each element of
     * the given {@code base} iterator with the contents of a mapped iterator produced
     * by applying the provided mapping function ({@code map}) to each element.
     * A functional equivalent of {@link Stream#flatMap(Function)}, but for {@link ExtendedIterator}s.
     *
     * @param base {@link ExtendedIterator} with elements of type {@link F}
     * @param map  {@link Function} map-function, Object of type {@link F} is an input, an {@link Iterator} of type {@link T} is an output
     * @param <F>  the element type of the base iterator
     * @param <T>  the element type of the new iterator
     * @return new {@link ExtendedIterator} of type {@link T}
     */
    public static <F, T> ExtendedIterator<T> flatMap(ExtendedIterator<F> base, Function<F, ? extends Iterator<T>> map) {
        return WrappedIterator.createIteratorIterator(base.mapWith(map).mapWith(i -> i));
    }

    /**
     * Returns an {@link ExtendedIterator Extended Iterator} consisting of the elements
     * of the given {@code base} iterator, additionally performing the provided {@code action}
     * on each element as elements are consumed from the resulting iterator.
     * A functional equivalent of {@link Stream#peek(Consumer)}, but for {@link ExtendedIterator}s.
     *
     * @param base   {@link ExtendedIterator} with elements of type {@link X}
     * @param action {@link Consumer} action
     * @param <X>    the element type of the input and output iterators
     * @return new {@link ExtendedIterator} of type {@link X}
     */
    public static <X> ExtendedIterator<X> peek(ExtendedIterator<X> base, Consumer<? super X> action) {
        return base.mapWith(x -> {
            action.accept(x);
            return x;
        });
    }

    /**
     * Returns an {@link ExtendedIterator Extended Iterator} consisting of the distinct elements
     * (according to {@link Object#equals(Object)}) of the given iterator.
     * A functional equivalent of {@link Stream#distinct()}, but for {@link ExtendedIterator}s.
     * Warning: the result is temporary stored in memory!
     *
     * @param base {@link ExtendedIterator} with elements of type {@link X}
     * @param <X>  the element type of the input and output iterators
     * @return new {@link ExtendedIterator} of type {@link X} without duplicates
     */
    public static <X> ExtendedIterator<X> distinct(ExtendedIterator<X> base) {
        return base.filterKeep(new FilterUnique<>());
    }

    /**
     * Creates a new {@link ExtendedIterator Extended Iterator}} containing the specified elements
     *
     * @param members Array of elements of the type {@link X}
     * @param <X>     the element type of the new iterator
     * @return a fresh {@link ExtendedIterator} instance
     */
    @SafeVarargs // Creating an iterator from an array is safe
    public static <X> ExtendedIterator<X> of(X... members) {
        return WrappedIterator.create(Arrays.asList(members).iterator());
    }
}
