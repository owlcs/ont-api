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
     * Merges two objects to a new one.
     * These two objects must be equal (see {@link Object#equals(Object)})
     * and both must implement {@link WithMerge} interface.
     * For internal usage only.
     *
     * @param left  {@link ONTObject}, not {@code null}
     * @param right {@link ONTObject}, not {@code null}
     * @param <X>   subtype of {@link ONTObject} and {@link WithMerge}
     * @return a new {@link X} which equal to {@code left}
     * @throws ClassCastException if the given objects do not implement {@link WithMerge}
     */
    @SuppressWarnings("unchecked")
    static <X extends OWLObject> ONTObject<X> merge(ONTObject<X> left, ONTObject<X> right) throws ClassCastException {
        return ((WithMerge<ONTObject<X>>) left).merge(right);
    }
}
