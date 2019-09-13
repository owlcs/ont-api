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

import org.semanticweb.owlapi.model.OWLObject;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * A technical interface that describes a merge operation.
 * An object that implements this interface can be merged with another one.
 * <p>
 * Created by @szz on 12.09.2019.
 *
 * @param <X> a subtype of {@link ONTObject}
 */
public interface WithMerge<X extends ONTObject> {

    /**
     * Merges this object and the given {@code other} into a new one of the same type {@link X}.
     *
     * @param other that equals to this object, not {@code null}
     * @return a new {@link X} which equal to this
     */
    X merge(X other);

    /**
     * A {@link BiFunction} that accepts two {@link ONTObject}s and returns a fresh {@link ONTObject}.
     * For internal usage only.
     * @see #getMerger()
     */
    @SuppressWarnings("unchecked")
    BiFunction ONT_OBJECT_MERGER = (left, right) -> ((WithMerge<ONTObject>) left).merge((ONTObject) right);

    /**
     * Returns a merge that maps two objects to a new one.
     * These two objects must be equal (see {@link Object#equals(Object)})
     * and both must implement {@link WithMerge} interface.
     * For internal usage only.
     *
     * @param <X> a subtype of {@link OWLObject}
     * @return a merger as a {@code BiFunction}
     */
    @SuppressWarnings("unchecked")
    static <X extends OWLObject> BiFunction<ONTObject<X>, ONTObject<X>, ONTObject<X>> getMerger() {
        return (BiFunction<ONTObject<X>, ONTObject<X>, ONTObject<X>>) ONT_OBJECT_MERGER;
    }

    /**
     * Adds the given {@code value}, that is an {@code ONTObject},
     * into the specified {@code map} of {@link OWLObject}s.
     *
     * @param map   {@code Map} to add new value, not {@code null}
     * @param value {@link ONTObject}, the value, not {@code null}
     * @param <X>   a subtype of {@link OWLObject}
     */
    static <X extends OWLObject> void add(Map<X, ONTObject<X>> map, ONTObject<X> value) {
        map.merge(value.getOWLObject(), value, getMerger());
    }
}
