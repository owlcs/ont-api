/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
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

import org.semanticweb.owlapi.model.OWLObject;

import java.util.stream.Stream;

/**
 * The generic interface, similar to {@code java.util.Map}, that provides access to a collection of {@link ONTObject}s.
 * It cannot contain {@code null} keys.
 * It is a loading map: it may contain both manually added {@code ONTObject}s or loaded by an internal process,
 * which relies on a {@link org.apache.jena.graph.Graph}.
 * <p>
 * Created by @ssz on 07.03.2019.
 *
 * @param <X> any {@link OWLObject}
 */
public interface ObjectMap<X extends OWLObject> {

    /**
     * Answers {@code true} if any of the encapsulated {@code ONTObject}s
     * has been added manually through the method {@link #add(ONTObject)},
     * not just loaded by the internal loader.
     * This flag is for optimization.
     *
     * @return boolean
     */
    boolean hasNew();

    /**
     * Answers {@code true} if this {@code ObjectMap} is loaded,
     * i.e. already contains all {@code ONTObject}s in memory.
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
     * Answers the {@link ONTObject} associated with the given {@code key}.
     *
     * @param key {@link X} key-object, not {@code null}
     * @return {@link ONTObject} or {@code null}
     */
    ONTObject<X> get(X key);

    /**
     * Lists all {@code OWLObjects}s encapsulated by this map.
     *
     * @return {@code Stream} of {@link X}s
     */
    Stream<X> keys();

    /**
     * Lists all {@code ONTObject}s encapsulated by this map.
     * @return {@code Stream} of {@link ONTObject} that wrap {@link X}s
     */
    Stream<ONTObject<X>> values();

    /**
     * Adds the given object with all its associated triples to internal map, if it is supported.
     *
     * @param value {@link ONTObject} of {@link X}, not {@code null}
     */
    void add(ONTObject<X> value);

    /**
     * Removes the given object and all its associated triples from internal map, if it is supported.
     *
     * @param key {@link X} key-object, not {@code null}
     */
    void remove(X key);

    /**
     * Clears the whole map-cache.
     */
    void clear();

    /**
     * Answers {@code true} is the map contains the object.
     *
     * @param key {@link X} key-object, not {@code null}
     * @return boolean
     */
    default boolean contains(X key) {
        return get(key) != null;
    }

    /**
     * Returns the count of objects in this map.
     *
     * @return long
     */
    default long count() {
        return values().count();
    }

}
