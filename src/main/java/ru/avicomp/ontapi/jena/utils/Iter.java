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

package ru.avicomp.ontapi.jena.utils;

import org.apache.jena.atlas.iterator.FilterUnique;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.impl.StmtIteratorImpl;
import org.apache.jena.util.iterator.ClosableIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
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
     * Creates an unmodifiable Set of {@link Node}s from the collection of {@link RDFNode RDF Node}s.
     * Placed here as it is widely used.
     *
     * @param nodes Collection of {@link RDFNode}s
     * @return Set of {@link Node}
     */
    public static Set<Node> asUnmodifiableNodeSet(Collection<? extends RDFNode> nodes) {
        return nodes.stream().map(FrontsNode::asNode).collect(toUnmodifiableSet());
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
     * @param base   {@link ExtendedIterator} with elements of type {@link F}
     * @param mapper {@link Function} map-function with Object of type of {@link F} (or any super type) as an input,
     *               and an {@link Iterator} of type {@link T} (or any extended type) as an output
     * @param <F>    the element type of the base iterator (from)
     * @param <T>    the element type of the new iterator (to)
     * @return new {@link ExtendedIterator} of type {@link F}
     */
    @SuppressWarnings("unchecked")
    public static <T, F> ExtendedIterator<T> flatMap(ExtendedIterator<F> base,
                                                     Function<? super F, ? extends Iterator<? extends T>> mapper) {
        return WrappedIterator.createIteratorIterator(base.mapWith((Function<F, Iterator<T>>) mapper));
    }

    /**
     * Creates a lazily concatenated {@link ExtendedIterator Extended Iterator} whose elements are all the
     * elements of the first iterator followed by all the elements of the second iterator.
     * A functional equivalent of {@link Stream#concat(Stream, Stream)}, but for {@link ExtendedIterator}s.
     *
     * @param a   the first iterator
     * @param b   the second iterator
     * @param <T> the type of iterator elements
     * @return the concatenation of the two input iterators
     */
    @SuppressWarnings("unchecked")
    public static <T> ExtendedIterator<T> concat(ExtendedIterator<? extends T> a, ExtendedIterator<? extends T> b) {
        return ((ExtendedIterator<T>) a).andThen(b);
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
     * Returns whether any elements of the given iterator match the provided predicate.
     * A functional equivalent of {@link Stream#anyMatch(Predicate)}, but for {@link ExtendedIterator}s.
     *
     * @param iterator  {@link ExtendedIterator} with elements of type {@link X}
     * @param predicate {@link Predicate} to apply to elements of the iterator
     * @param <X>       the element type of the input and output iterators
     * @return {@code true} if any elements of the stream match the provided predicate, otherwise {@code false}
     */
    public static <X> boolean anyMatch(ExtendedIterator<X> iterator, Predicate<? super X> predicate) {
        if (iterator instanceof NullIterator) return false;
        try {
            while (iterator.hasNext()) {
                if (predicate.test(iterator.next())) return true;
            }
        } finally {
            iterator.close();
        }
        return false;
    }

    /**
     * Returns an extended iterator consisting of the elements of the specified extended iterator
     * that match the given predicate.
     * A functional equivalent of {@link Stream#filter(Predicate)}, but for {@link ExtendedIterator}s.
     *
     * @param iterator  {@link ExtendedIterator} with elements of type {@link X}
     * @param predicate {@link Predicate} to apply to elements of the iterator
     * @param <X>       the element type of the input and output iterators
     * @return a new iterator
     */
    @SuppressWarnings("unchecked")
    public static <X> ExtendedIterator<X> filter(ExtendedIterator<X> iterator, Predicate<? super X> predicate) {
        return iterator.filterKeep((Predicate<X>) predicate);
    }

    /**
     * Returns an {@link Optional} describing the first element of the iterator,
     * or an empty {@code Optional} if the iterator is empty.
     * A functional equivalent of {@link Stream#findFirst()}, but for {@link ExtendedIterator}s.
     * Warning: the method closes the specified iterator, so it is no possible to reuse it after calling this method.
     *
     * @param iterator {@link ClosableIterator}, not {@code null}
     * @param <X>      the element type of the iterator
     * @return {@link Optional} of {@link X}
     * @throws NullPointerException if the element selected is {@code null}
     */
    public static <X> Optional<X> findFirst(ClosableIterator<X> iterator) {
        if (iterator instanceof NullIterator) return Optional.empty();
        try {
            return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
        } finally {
            iterator.close();
        }
    }

    /**
     * Creates a new {@link ExtendedIterator Extended Iterator}} containing the specified elements.
     *
     * @param members Array of elements of the type {@link X}
     * @param <X>     the element type of the new iterator
     * @return a fresh {@link ExtendedIterator} instance
     */
    @SafeVarargs // Creating an iterator from an array is safe
    public static <X> ExtendedIterator<X> of(X... members) {
        return create(Arrays.asList(members));
    }

    /**
     * Creates a new {@link ExtendedIterator Extended Iterator}} over all elements of the specified collection.
     *
     * @param members {@code Collection} of elements of the type {@link X}
     * @param <X>     the element type of the new iterator
     * @return a fresh {@link ExtendedIterator} instance
     */
    public static <X> ExtendedIterator<X> create(Collection<X> members) {
        return members.isEmpty() ? NullIterator.instance() : WrappedIterator.create(members.iterator());
    }

}
