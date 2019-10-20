/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.internal.objects;

import org.semanticweb.owlapi.model.HasComponents;
import org.semanticweb.owlapi.model.HasOperands;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.internal.ONTObject;

import java.util.stream.Stream;

/**
 * A generic interface describing an {@link ONTObject} that has components and signature.
 *
 * Created by @szz on 29.08.2019.
 * @since 1.4.3
 */
public interface ONTComposite extends ONTComponent {

    /**
     * Lists all components in the form of {@code Stream}.
     * Neither this object or parts of its components are not included in the result:
     * it content only top-level direct components.
     * Note that {@link HasComponents#components()} may also contain non-{@link OWLObject} things:
     * integers (e.g. cardinality), {@code List}s (e.g. {@code ObjectOneOf}), etc,
     * while this method is only for {@link OWLObject}s which are represented as {@link ONTObject}s.
     *
     * @return {@link Stream} of {@link ONTObject}s
     * @see ONTComposite#objects()
     * @see HasComponents#components()
     * @see HasOperands#operands()
     */
    Stream<ONTObject<? extends OWLObject>> objects();

    /**
     * Answers {@code true} if this object-container is allowed to contain nested class-expressions.
     *
     * @return boolean
     */
    default boolean canContainClassExpressions() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container is allowed to contain anonymous individuals.
     *
     * @return boolean
     */
    default boolean canContainAnonymousIndividuals() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container is allowed to contain (named) OWL classes.
     *
     * @return boolean
     */
    default boolean canContainNamedClasses() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container is allowed to contain named individuals.
     *
     * @return boolean
     */
    default boolean canContainNamedIndividuals() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container may contain datatypes.
     *
     * @return boolean
     */
    default boolean canContainDatatypes() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container may contain named object properties.
     *
     * @return boolean
     */
    default boolean canContainObjectProperties() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container may contain data properties.
     *
     * @return boolean
     */
    default boolean canContainDataProperties() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container may contain annotation properties.
     *
     * @return boolean
     */
    default boolean canContainAnnotationProperties() {
        return true;
    }
}
