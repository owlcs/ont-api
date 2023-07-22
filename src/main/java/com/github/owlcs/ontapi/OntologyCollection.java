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

package com.github.owlcs.ontapi;

import org.semanticweb.owlapi.model.HasOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyID;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * A collection to store anything that has {@link OWLOntologyID Ontology ID},
 * it maps these key-ids to element-containers and behaves like a {@link java.util.Map}.
 * But unlike the standard {@link java.util.Map},
 * this collection should not lose a value in case of an unpredictable ontology id change of element-container.
 * This means that if such a change, that is external to this collection, has occurred,
 * the value can be found using a new key-id with the method {@link #get(OWLOntologyID)},
 * whereas a search, that uses the old key-id, should return an empty result.
 * In this sense, this collection behaves like any java {@link java.util.Collection}
 * and implementations, actually, might be based on it.
 * On the other hand, in case there are no external changes in relation of key-id,
 * it is expected that access by a key-id will be as fast as for a standard java {@link java.util.HashMap}.
 * <p>
 * Created by @ssz on 09.12.2018.
 *
 * @param <O> any subclass of {@link HasOntologyID}
 * @since 1.3.2
 */
public interface OntologyCollection<O extends HasOntologyID> {

    /**
     * Lists all values as a java {@code Stream} with no duplicates and no {@code null}-elements.
     *
     * @return Stream of {@link O}s
     */
    Stream<O> values();

    /**
     * Returns the value which owns the specified key-id.
     * The result is empty in case the collection does not contain a container with the given key-id inside.
     * In case some key-id for some element-container is changed to a value,
     * that equals to the value of some other element-container from this collection,
     * this method returns the last container.
     *
     * @param key {@link OWLOntologyID key-id}, not {@code null}
     * @return {@code Optional} around the {@link O}, possible empty
     */
    Optional<O> get(OWLOntologyID key);

    /**
     * Adds the specified element-container into the collection.
     *
     * @param value {@link O}, not {@code null}
     * @return this collection, to allow cascading calls
     */
    OntologyCollection<O> add(O value);

    /**
     * Removes and returns the element-container with the specified key-id inside from the collection.
     * An empty result is expected if there are no such element-container inside the collection.
     *
     * @param key {@link OWLOntologyID key-id}, not {@code null}
     * @return {@code Optional} around the {@link O}, possible empty
     */
    Optional<O> remove(OWLOntologyID key);

    /**
     * Deletes the given element-container from the collection.
     *
     * @param value {@link O}, not {@code null}
     * @return this collection, to allow cascading calls
     */
    OntologyCollection<O> delete(O value);

    /**
     * Removes all elements from this collection
     *
     * @return this collection, to allow cascading calls
     */
    OntologyCollection<O> clear();

    /**
     * Returns the number of elements in this collection.
     *
     * @return long
     */
    long size();

    /**
     * Answers {@code true} if the collection is empty.
     *
     * @return boolean
     */
    default boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Lists all {@link OWLOntologyID key-id}s for all element-containers from this collection.
     *
     * @return Stream of {@link OWLOntologyID key-id}, with no {@code null}-elements
     */
    default Stream<OWLOntologyID> keys() {
        return values().map(HasOntologyID::getOntologyID);
    }

    /**
     * Answers {@code true} iff this collection contains the element-container with the given key-id.
     *
     * @param key {@link OWLOntologyID key-id}, not {@code null}
     * @return boolean
     */
    default boolean contains(OWLOntologyID key) {
        return get(key).isPresent();
    }

}
