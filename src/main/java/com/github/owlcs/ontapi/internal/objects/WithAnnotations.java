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

package com.github.owlcs.ontapi.internal.objects;

import org.semanticweb.owlapi.model.HasAnnotations;
import org.semanticweb.owlapi.model.OWLAnnotation;

import java.util.List;
import java.util.stream.Stream;

/**
 * The generic interface describing an object that has annotation.
 * Created by @szz on 01.10.2019.
 *
 * @since 2.0.0
 */
public interface WithAnnotations extends HasAnnotations {

    /**
     * Answers {@code true} if this object (axiom or annotation) has sub-annotations.
     *
     * @return boolean
     * @see org.semanticweb.owlapi.model.OWLAxiom#isAnnotated()
     */
    boolean isAnnotated();

    /**
     * Lists all {@link OWLAnnotation}s on this object.
     * The stream must be {@link java.util.Spliterator#ORDERED ordered}, {@link java.util.Spliterator#NONNULL nonull},
     * {@link java.util.Spliterator#DISTINCT distinct} and {@link java.util.Spliterator#SORTED sorted}.
     *
     * @return a {@code Stream} of {@link OWLAnnotation}s
     * @see HasAnnotations#annotations()
     */
    Stream<OWLAnnotation> annotations();

    /**
     * Answers a sorted and distinct {@code List} of {@link OWLAnnotation}s on this object.
     * The returned {@code List} is unmodifiable.
     *
     * @return an unmodifiable {@code List} of {@link OWLAnnotation}s
     * @see HasAnnotations#annotationsAsList()
     */
    List<OWLAnnotation> annotationsAsList();
}
