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

package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import ru.avicomp.ontapi.jena.OntJenaException;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A parameterized analogue of the {@link RDFList Jena []-List} that behaves like a java collection.
 * Please note: currently it is not a Personality resource and, therefore, Jena Polymorphism is not supported for it.
 * The latter means that attempt to cast any {@link RDFNode RDF Node} to this view
 * will cause {@link org.apache.jena.enhanced.UnsupportedPolymorphismException UnsupportedPolymorphismException},
 * but it is possible do the opposite: cast an instance of this interface to the {@link RDFList Jena []-List} view
 * using the expression {@code ont-list.as(RDFList.class)}.
 * Also note: switching to nil-list (by any of the add/remove/clean operations) from a not-empty list and vice verse
 * violates a Jena invariant, this means that this {@link OntResource} behaves not always like pure {@link Resource Jena Resource}
 * and all of the methods may throw {@link OntJenaException.IllegalState}
 * in case of usage different instances encapsulating the same resource-list.
 * <p>
 * Unlike the standard {@link RDFList []-List}, ONT-List can be typed.
 * This means that each resource-member of []-List can have an {@link ru.avicomp.ontapi.jena.vocabulary.RDF#type rdf:type} declaration,
 * while the standard RDF []-List implementation does not support typing.
 * See below for an example of a typed []-list in Turle format:
 * <pre>{@code
 * [ rdf:type   <type> ;
 *   rdf:first  <A> ;
 *   rdf:rest   [ rdf:type   <type> ;
 *                rdf:first  <B> ;
 *                rdf:rest   rdf:nil
 *              ]
 * ] .
 * Note, that an empty []-list (i.e. {@link ru.avicomp.ontapi.jena.vocabulary.RDF#nil nil}-list) cannot be typed.
 * }</pre>
 *
 * Created by @szuev on 10.07.2018.
 *
 * @param <E> the type of {@link RDFNode rdf-node}s in this list
 * @since 1.2.1
 */
public interface OntList<E extends RDFNode> extends OntResource {

    /**
     * Answers {@code true} if this list is the empty list (nil).
     *
     * @return boolean
     */
    boolean isEmpty();

    /**
     * Lists all elements of type {@link E} from this list.
     * Note: the list may contain nodes with incompatible type, in this case they will be skipped.
     *
     * @return Stream of {@link E}-elements
     */
    Stream<E> members();

    /**
     * Adds the given value to the end of the list.
     *
     * @param e {@link E} rdf-node
     * @return this list instance
     */
    OntList<E> addLast(E e);

    /**
     * Removes the last element from this list.
     * No-op in case of empty list.
     * Note: the last element can be of any type, not necessarily of type {@link E}.
     *
     * @return this list instance
     */
    OntList<E> removeLast();

    /**
     * Inserts the specified element at the beginning of this list.
     *
     * @param e {@link E} rdf-node
     * @return this list instance
     */
    OntList<E> addFirst(E e);

    /**
     * Removes and the first element from this list.
     * No-op in case of empty list.
     * Note: the last element can be of any type, not necessarily of type {@link E}.
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
     * Note: the result list is not annotable.
     * This method can be used to insert/remove/clear the parent list at any position,
     * e.g. the operation {@code get(1).addFirst(e)} will insert element {@code e} at second position.
     *
     * @param index int, not negative
     * @return new {@code OntList} instance
     * @throws OntJenaException.IllegalArgument if the specified index is out of list bounds
     */
    OntList<E> get(int index) throws OntJenaException;

    /**
     * Answers the resource-type of this ONT-list, if it is typed.
     * A standard RDF-list does not contain any {@link ru.avicomp.ontapi.jena.vocabulary.RDF#type rdf:type} in its deeps.
     * For its description, predicates {@link ru.avicomp.ontapi.jena.vocabulary.RDF#first rdf:first},
     * {@link ru.avicomp.ontapi.jena.vocabulary.RDF#rest rdf:rest} and {@link ru.avicomp.ontapi.jena.vocabulary.RDF#nil rdf:nil} are sufficient.
     * In this case this method returns {@link Optional#empty() empty}.
     * But in some rare semantics (e.g. see {@link ru.avicomp.ontapi.jena.vocabulary.SWRL}), the []-list must to be typed
     * (see {@link ru.avicomp.ontapi.jena.vocabulary.SWRL#AtomList}).
     * In that case this method returns an URI-{@code Resource}(in {@code Optional}) describing the []-list type.
     *
     * @return {@link Optional} around the URI-{@link Resource}, can be empty.
     */
    Optional<Resource> type();

    /**
     * Lists all statements related to this list.
     * For nil-list empty stream is expected.
     * Inherit java-docs:
     * {@inheritDoc}
     *
     * @return Stream of {@link OntStatement Ontology Statement}s that does not support annotations
     */
    @Override
    Stream<OntStatement> spec();

    /**
     * Returns the root statement plus spec.
     *
     * @return Stream of {@link OntStatement Ontology Statement}s, only the first item of stream can be annotated
     */
    default Stream<OntStatement> content() {
        return Stream.concat(Stream.of(getRoot()), spec());
    }

    /**
     * Answers the number of {@link RDFNode rdf-node}s in the list.
     * Note: in general this operation is not equivalent to {@code this.members().count()}.
     *
     * @return the real size of the list as an integer
     */
    default int size() {
        return as(RDFList.class).size();
    }

    /**
     * Adds the given value to the end of the list.
     * This is a synonym for the {@code this.addLast(e)}.
     *
     * @param e {@link E} rdf-node
     * @return this list instance
     */
    default OntList<E> add(E e) {
        return addLast(e);
    }

    /**
     * Removes the last element from this list.
     * This is a synonym for the {@code this.removeLast(e)}.
     *
     * @return this list instance
     */
    default OntList<E> remove() {
        return removeLast();
    }

    /**
     * Answers the first element with type {@link E}.
     *
     * @return Optional around rdf-node
     */
    default Optional<E> first() {
        return members().findFirst();
    }

    /**
     * Answers the last element with type {@link E}.
     *
     * @return Optional around rdf-node
     */
    default Optional<E> last() {
        return members().reduce((f, s) -> s);
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

    @Override
    default boolean isLocal() {
        return getRoot().isLocal();
    }

    @Override
    default OntStatement addAnnotation(OntNAP property, RDFNode value) {
        return getRoot().addAnnotation(property, value);
    }

    @Override
    default OntList<E> clearAnnotations() {
        getRoot().clearAnnotations();
        return this;
    }
}
