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

package com.github.owlcs.ontapi.internal.axioms;

import com.github.owlcs.ontapi.internal.HasConfig;
import com.github.owlcs.ontapi.internal.HasObjectFactory;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.objects.AsStatement;
import com.github.owlcs.ontapi.internal.objects.ONTComposite;
import com.github.owlcs.ontapi.internal.objects.WithAnnotations;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Set;

/**
 * The base interface for any ONTAxiom.
 * Provides access to main triple's parts.
 * <p>
 * Created by @ssz on 02.10.2019.
 *
 * @since 2.0.0
 */
interface WithTriple extends AsStatement, WithAnnotations, ONTComposite, HasObjectFactory, HasConfig, OWLAxiom {

    /**
     * Answers {@code true} iff the subject is a URI resource.
     *
     * @return boolean
     */
    default boolean hasURIObject() {
        return asTriple().getObject().isURI();
    }

    /**
     * Answers {@code true} iff the object is a URI resource.
     *
     * @return boolean
     */
    default boolean hasURISubject() {
        return asTriple().getSubject().isURI();
    }

    /**
     * Answers a URI of the triple's subject.
     *
     * @return URI of the subject
     * @throws RuntimeException in case the main triple has no subject uri (b-node instead)
     */
    default String getSubjectURI() {
        return asTriple().getSubject().getURI();
    }

    /**
     * Answers a URI of the triple's object.
     *
     * @return URI of the object
     * @throws RuntimeException in case the main triple has no object uri (b-node or literal instead)
     */
    default String getObjectURI() {
        return asTriple().getObject().getURI();
    }

    /**
     * Gets all OWL characteristic components in the form of {@code Set}.
     * {@link org.semanticweb.owlapi.model.OWLAnnotation OWLAnnotation}s are not included.
     *
     * @param factory {@link ModelObjectFactory}, not {@code null}
     * @return a sorted {@code Set} of {@link OWLObject}s
     * @see ONTComposite#objects()
     */
    Set<? extends OWLObject> getOWLComponentsAsSet(ModelObjectFactory factory);

    /**
     * Gets all OWL characteristic components in the form of {@code Set}.
     *
     * @return a sorted {@code Set} of {@link OWLObject}s
     */
    default Set<? extends OWLObject> getOWLComponentsAsSet() {
        return getOWLComponentsAsSet(getObjectFactory());
    }
}
