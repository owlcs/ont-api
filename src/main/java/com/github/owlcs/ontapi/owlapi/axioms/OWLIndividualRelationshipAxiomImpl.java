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
package com.github.owlcs.ontapi.owlapi.axioms;

import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.Objects;

/**
 * @param <P> the property expression
 * @param <O> the object
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 1.2.0
 */
@SuppressWarnings("WeakerAccess")
public abstract class OWLIndividualRelationshipAxiomImpl<P extends OWLPropertyExpression, O extends OWLPropertyAssertionObject>
        extends OWLLogicalAxiomImpl implements OWLPropertyAssertionAxiom<P, O> {

    private final OWLIndividual subject;
    private final P predicate;
    private final O object;

    /**
     * @param subject     {@link OWLIndividual}, the subject
     * @param property    {@link P}, the predicate
     * @param object      {@link O}, the object
     * @param annotations a {@code Collection} of {@link OWLAnnotation}s
     */
    public OWLIndividualRelationshipAxiomImpl(OWLIndividual subject,
                                              P property,
                                              O object,
                                              Collection<OWLAnnotation> annotations) {
        super(annotations);
        this.subject = Objects.requireNonNull(subject, "subject cannot be null");
        this.predicate = Objects.requireNonNull(property, "property cannot be null");
        this.object = Objects.requireNonNull(object, "object cannot be null");
    }

    @Override
    public OWLIndividual getSubject() {
        return subject;
    }

    @Override
    public P getProperty() {
        return predicate;
    }

    @Override
    public O getObject() {
        return object;
    }
}
