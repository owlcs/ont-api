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

package ru.avicomp.ontapi.internal.objects;

import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.internal.ONTObject;

import java.util.Set;
import java.util.stream.Stream;

/**
 * A generic interface describing an {@link ONTObject} that has components and signature.
 * Created by @szz on 29.08.2019.
 *
 * @see ru.avicomp.ontapi.owlapi.OWLObjectImpl
 * @since 1.4.3
 */
public interface WithComponents {

    /**
     * Lists all components in the form of {@code Stream}.
     * Neither this object or parts of its components are not included in the result:
     * it content only top-level direct components.
     * Note that {@link HasComponents#components()} may also contain non-{@link OWLObject} things:
     * integers (e.g. cardinality), {@code List}s (e.g. {@code ObjectOneOf}), etc,
     * while this method is only for {@link OWLObject}s which are represented as {@link ONTObject}s.
     *
     * @return {@link Stream} of {@link ONTObject}s
     * @see WithComponents#objects()
     * @see HasComponents#components()
     * @see HasOperands#operands()
     */
    Stream<ONTObject<? extends OWLObject>> objects();

    /**
     * Gets all of the nested (includes top level) class expressions that are used in this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable unordered {@code Set} of {@link OWLClassExpression}s
     * that represent the nested class expressions used in this object
     */
    Set<OWLClassExpression> getClassExpressionSet();

    /**
     * Gets the anonymous individuals occurring in this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} of the anonymous individuals
     */
    Set<OWLAnonymousIndividual> getAnonymousIndividualSet();

    /**
     * Gets the classes in the signature of this object.
     * The returned set is a subset of the signature, and is not backed by the signature;
     * it is a modifiable collection and changes are not reflected by the signature.
     *
     * @return a modifiable sorted {@code Set} containing the classes
     * that are in the signature of this object
     */
    Set<OWLClass> getNamedClassSet();

    /**
     * Gets all of the individuals that are in the signature of this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} containing the individuals that are in the signature of this object
     */
    Set<OWLNamedIndividual> getNamedIndividualSet();

    /**
     * Gets the datatypes that are in the signature of this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} of the datatypes that are in the signature of this object
     */
    Set<OWLDatatype> getDatatypeSet();

    /**
     * Obtains the (named) object properties that are in the signature of this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} of the object properties that are in the signature of this object
     */
    Set<OWLObjectProperty> getObjectPropertySet();

    /**
     * Obtains the data properties that are in the signature of this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} of the data properties that are in the signature of this object
     */
    Set<OWLDataProperty> getDataPropertySet();

    /**
     * Obtains the annotation properties that are in the signature of this object.
     * The set is a copy, changes are not reflected back.
     *
     * @return a modifiable sorted {@code Set} of the annotation properties that are in the signature of this object
     */
    Set<OWLAnnotationProperty> getAnnotationPropertySet();

    /**
     * Answers {@code true} if this object-container is allowed to contain nested class-expressions.
     *
     * @return boolean
     * @see #getClassExpressionSet()
     */
    default boolean canContainClassExpressions() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container is allowed to contain anonymous individuals.
     *
     * @return boolean
     * @see #getAnonymousIndividualSet()
     */
    default boolean canContainAnonymousIndividuals() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container is allowed to contain (named) OWL classes.
     *
     * @return boolean
     * @see #getNamedClassSet()
     */
    default boolean canContainNamedClasses() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container is allowed to contain named individuals.
     *
     * @return boolean
     * @see #getNamedIndividualSet()
     */
    default boolean canContainNamedIndividuals() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container may contain datatypes.
     *
     * @return boolean
     * @see #getDatatypeSet()
     */
    default boolean canContainDatatypes() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container may contain named object properties.
     *
     * @return boolean
     * @see #getObjectPropertySet()
     */
    default boolean canContainObjectProperties() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container may contain data properties.
     *
     * @return boolean
     * @see #getDataPropertySet()
     */
    default boolean canContainDataProperties() {
        return true;
    }

    /**
     * Answers {@code true} if this object-container may contain annotation properties.
     *
     * @return boolean
     * @see #getAnnotationPropertySet()
     */
    default boolean canContainAnnotationProperties() {
        return true;
    }
}
