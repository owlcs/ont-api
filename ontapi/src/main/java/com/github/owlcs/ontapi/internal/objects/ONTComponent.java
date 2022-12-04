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

package com.github.owlcs.ontapi.internal.objects;

/**
 * A technical interface that describes a typed object.
 * Created by @ssz on 31.08.2019.
 *
 * @since 2.0.0
 */
interface ONTComponent {

    /**
     * Answers {@code true} iff
     * this is an {@link org.semanticweb.owlapi.model.OWLClassExpression OWL Class Expression}.
     *
     * @return boolean
     */
    default boolean isClassExpression() {
        return false;
    }

    /**
     * Answers {@code true} iff
     * this is an {@link org.semanticweb.owlapi.model.OWLAnonymousIndividual OWL Anonymous Individual}.
     *
     * @return boolean
     */
    default boolean isAnonymousIndividual() {
        return false;
    }

    /**
     * Answers {@code true} iff
     * this is an {@link org.semanticweb.owlapi.model.OWLClass OWL Class}.
     *
     * @return boolean
     */
    default boolean isNamedClass() {
        return false;
    }

    /**
     * Answers {@code true} iff
     * this is an {@link org.semanticweb.owlapi.model.OWLNamedIndividual OWL Named Individual}.
     *
     * @return boolean
     */
    default boolean isNamedIndividual() {
        return false;
    }

    /**
     * Answers {@code true} iff
     * this is an {@link org.semanticweb.owlapi.model.OWLDatatype OWL Datatype}.
     *
     * @return boolean
     */
    default boolean isDatatype() {
        return false;
    }

    /**
     * Answers {@code true} iff
     * this is an {@link org.semanticweb.owlapi.model.OWLObjectProperty OWL Object Property}.
     *
     * @return boolean
     */
    default boolean isObjectProperty() {
        return false;
    }

    /**
     * Answers {@code true} iff
     * this is an {@link org.semanticweb.owlapi.model.OWLDataProperty OWL Data Property}.
     *
     * @return boolean
     */
    default boolean isDataProperty() {
        return false;
    }

    /**
     * Answers {@code true} iff
     * this is an {@link org.semanticweb.owlapi.model.OWLAnnotationProperty OWL Annotation Property}.
     *
     * @return boolean
     */
    default boolean isAnnotationProperty() {
        return false;
    }
}
