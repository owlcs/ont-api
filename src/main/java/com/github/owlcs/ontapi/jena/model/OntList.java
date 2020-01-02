/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.jena.model;

import com.github.owlcs.ontapi.jena.OntJenaException;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A parameterized analogue of the {@link RDFList Jena []-List} that behaves like a java collection.
 * Please note: currently it is not a Personality resource and, therefore, Jena Polymorphism is not supported for it.
 * The latter means that attempt to cast any {@link RDFNode RDF Node} to this view
 * will cause {@link org.apache.jena.enhanced.UnsupportedPolymorphismException UnsupportedPolymorphismException},
 * but it is possible do the opposite: cast an instance of this interface to the {@link RDFList Jena []-List} view
 * using the expression {@code OntList.as(RDFList.class)}.
 * Also note: switching to nil-list (by any of the add/remove/clean operations) from a not-empty list and vice verse
 * violates a Jena invariant, this means that this {@link OntResource} behaves not always like pure {@link Resource Jena Resource}
 * and all of the methods may throw {@link OntJenaException.IllegalState}
 * in case of usage different instances encapsulating the same resource-list.
 * <p>
 * Unlike the standard {@link RDFList []-List} implementation, ONT-List can be typed.
 * This means that each resource-member of []-List may have an {@link com.github.owlcs.ontapi.jena.vocabulary.RDF#type rdf:type} declaration,
 * while the standard RDF []-List impl does not support typing.
 * See below for an example of a typed []-list in Turtle format:
 * <pre>{@code
 * [ rdf:type   <type> ;
 *   rdf:first  <A> ;
 *   rdf:rest   [ rdf:type   <type> ;
 *                rdf:first  <B> ;
 *                rdf:rest   rdf:nil
 *              ]
 * ] .
 * }</pre>
 * Note, that an empty []-list (i.e. {@link com.github.owlcs.ontapi.jena.vocabulary.RDF#nil nil}-list) cannot be typed.
 * <p>
 * Using the method {@link #getMainStatement()} it is possible to add annotations with any nesting depth.
 * <p>
 * Created by @szuev on 10.07.2018.
 *
 * @param <E> the type of {@link RDFNode rdf-node}s in this list
 * @see RDFNodeList
 */
public interface OntList<E extends RDFNode> extends RDFNodeList<E>, OntResource {

    /**
     * Adds the given value to the end of the list.
     *
     * @param e {@link E} rdf-node
     * @return this list instance
     * @see #add(RDFNode)
     */
    OntList<E> addLast(E e);

    /**
     * Removes the last element from this list.
     * No-op in case of nil-list.
     * Note: the removed element can be of any type, not necessarily of the type {@link E}.
     *
     * @return this list instance
     * @see #remove()
     */
    OntList<E> removeLast();

    /**
     * Inserts the specified element at the beginning of this list.
     * As a rule, this operation is faster than {@link #addLast(RDFNode)},
     * since it does not require iteration to the end of the list.
     *
     * @param e {@link E} rdf-node
     * @return this list instance
     */
    OntList<E> addFirst(E e);

    /**
     * Removes and the first element from this list.
     * No-op in case of empty list.
     * Note: the last element can be of any type, not necessarily of type {@link E}.
     * As a rule, this operation is faster than {@link #removeLast()} ,
     * since the last one requires iteration to the end of the list.
     *
     * @return the first element from this list
     */
    OntList<E> removeFirst();

    /**
     * Removes all of the elements from this list.
     * The list will be empty (nil) after this call returns.
     *
     * @return this (empty) instance
     */
    OntList<E> clear();

    /**
     * Answers the list that is the tail of this list starting from the given position.
     * Note: the returned list cannot be annotated.
     * This method can be used to insert/remove/clear the parent list at any position,
     * e.g. the operation {@code get(1).addFirst(e)} will insert the element {@code e} at second position.
     *
     * @param index int, not negative
     * @return new {@code OntList} instance
     * @throws OntJenaException.IllegalArgument if the specified index is out of list bounds
     */
    OntList<E> get(int index) throws OntJenaException;

    /**
     * Answers the resource-type of this ONT-list, if it is typed.
     * A standard RDF-list does not require any {@link com.github.owlcs.ontapi.jena.vocabulary.RDF#type rdf:type}
     * in its RDF-deeps, since predicates {@link com.github.owlcs.ontapi.jena.vocabulary.RDF#first rdf:first},
     * {@link com.github.owlcs.ontapi.jena.vocabulary.RDF#rest rdf:rest}
     * and {@link com.github.owlcs.ontapi.jena.vocabulary.RDF#nil rdf:nil} are sufficient for its description.
     * In this case the method returns {@link Optional#empty() empty} result.
     * But in some rare semantics (e.g. see {@link com.github.owlcs.ontapi.jena.vocabulary.SWRL}),
     * the []-list must to be typed.
     * In that case this method returns an URI-{@code Resource} (that is wrapped as {@code Optional})
     * describing the []-list's type
     * (for SWRL it is {@link com.github.owlcs.ontapi.jena.vocabulary.SWRL#AtomList swrl:AtomList}).
     *
     * @return {@link Optional} around the URI-{@link Resource}, can be empty.
     */
    Optional<Resource> type();

    /**
     * Lists all statements related to this list.
     * For nil-list an empty stream is expected.
     * Note: it returns all statements even if the list contains incompatible types.
     * <p>
     * See also inherit java-docs:
     * {@inheritDoc}
     *
     * @return Stream of {@link OntStatement Ontology Statement}s that does not support annotations
     */
    @Override
    Stream<OntStatement> spec();

    /**
     * Returns the root statement plus spec.
     * Please note: only the first item (root) is allowed to be annotated.
     *
     * @return {@code Stream} of {@link OntStatement Ontology Statement}s
     */
    default Stream<OntStatement> content() {
        return Stream.concat(Stream.of(getMainStatement()), spec());
    }

    /**
     * Adds the given value to the end of the list.
     * This is a synonym for the {@code this.addLast(e)}.
     *
     * @param e {@link E} rdf-node
     * @return this list instance
     * @see #addLast(RDFNode)
     */
    default OntList<E> add(E e) {
        return addLast(e);
    }

    /**
     * Removes the last element from this list.
     * This is a synonym for the {@code this.removeLast(e)}.
     *
     * @return this list instance
     * @see #removeLast()
     */
    default OntList<E> remove() {
        return removeLast();
    }

    /**
     * Appends all of the elements in the specified collection to the end of this list,
     * in the order that they are returned by the specified collection's iterator.
     *
     * @param c Collection of {@link E}-elements
     * @return this list instance
     */
    default OntList<E> addAll(Collection<? extends E> c) {
        c.forEach(this::add);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean isLocal() {
        return getMainStatement().isLocal();
    }

}
