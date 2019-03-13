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

package ru.avicomp.ontapi.internal;

import org.apache.jena.graph.GraphListener;
import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An object that maps {@link OWLObject}s, considered as keys, to {@link Triple}s collection.
 * A map cannot contain duplicate or {@code null} keys.
 * It is a loading map: it may contain both manually added pairs or loaded by an internal process,
 * which relies on a {@link org.apache.jena.graph.Graph}.
 * <p>
 * Created by @ssz on 07.03.2019.
 *
 * @param <O> any {@link OWLObject}
 */
public interface ObjectTriplesMap<O extends OWLObject> {

    /**
     * Answers {@code true}
     * if any of the encapsulated object-triples pair
     * has been added manually through the method {@link #addListener(OWLObject)},
     * not just loaded by the internal loader.
     * This flag is for optimization.
     *
     * @return boolean
     */
    boolean hasNew();

    /**
     * Answers {@code true} if this map is loaded, i.e. contains all object-triple pairs in memory.
     *
     * @return boolean
     */
    boolean isLoaded();

    /**
     * Loads a map using internal loader.
     * No-op in case of {@link #hasNew()} or {@link #isLoaded()} is {@code true}.
     */
    void load();

    /**
     * Lists all {@code OWLObjects}s encapsulated by this map.
     *
     * @return {@code Stream} of {@link O}s
     */
    Stream<O> objects();

    /**
     * Lists all {@code Triple}s associated with the object-key.
     *
     * @param key {@link O} key-object, not {@code null}
     * @return {@code Stream} of {@link Triple}s
     * @throws RuntimeException in case object's triple-structure is broken
     */
    Stream<Triple> triples(O key) throws RuntimeException;

    /**
     * Creates a graph listener that handles adding {@link O OWLObject} while changing a {@code Graph}.
     *
     * @param key {@link O} key-object, not {@code null}
     * @return {@link GraphListener}
     */
    GraphListener addListener(O key);

    /**
     * Deletes the given object and all its associated triples.
     *
     * @param key {@link O} key-object, not {@code null}
     */
    void delete(O key);

    /**
     * Clears the whole map-cache.
     */
    void clear();

    /**
     * List all {@code Triple}s encapsulated by this map.
     *
     * @return Stream of {@link Triple}s
     */
    default Stream<Triple> triples() {
        return objects().flatMap(this::triples);
    }

    /**
     * Answers {@code true} is the map contains the object.
     *
     * @param key {@link O} key-object, not {@code null}
     * @return boolean
     */
    default boolean contains(O key) {
        return objects().anyMatch(key::equals);
    }

    /**
     * Answers {@code true} if the given object-triple pair is present into the map.
     *
     * @param key    {@link O} key-object, not {@code null}
     * @param triple {@link Triple}, not {@code null}
     * @return boolean
     */
    default boolean contains(O key, Triple triple) {
        return triples(key).anyMatch(triple::equals);
    }

    /**
     * Answers {@code true} if the given {@link Triple} is present into the map.
     *
     * @param triple {@link Triple}, not {@code null}
     * @return boolean
     */
    default boolean contains(Triple triple) {
        return objects().anyMatch(o -> contains(o, triple));
    }

    /**
     * Answers a {@code Set} of {@code Triple}s associated with the specified object.
     *
     * @param key {@link O} key-object, not {@code null}
     * @return {@code Set} of {@link Triple}s
     */
    default Set<Triple> getTripleSet(O key) {
        return triples(key).collect(Collectors.toSet());
    }

}
