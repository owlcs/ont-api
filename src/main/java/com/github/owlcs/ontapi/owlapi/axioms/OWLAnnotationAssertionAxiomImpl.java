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
import com.github.owlcs.ontapi.owlapi.objects.OWLAnnotationImplNotAnnotated;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 1.2.0
 */
public class OWLAnnotationAssertionAxiomImpl extends OWLAxiomImpl implements OWLAnnotationAssertionAxiom {

    private final OWLAnnotationSubject subject;
    private final OWLAnnotationProperty property;
    private final OWLAnnotationValue value;

    /**
     * @param subject     {@link OWLAnnotationSubject}, subject for axiom
     * @param property    {@link OWLAnnotationProperty}, annotation property
     * @param value       {@link OWLAnnotationValue}, annotation value
     * @param annotations a {@code Collection} of annotations on the axiom
     */
    public OWLAnnotationAssertionAxiomImpl(OWLAnnotationSubject subject,
                                           OWLAnnotationProperty property,
                                           OWLAnnotationValue value,
                                           Collection<OWLAnnotation> annotations) {
        super(annotations);
        this.subject = Objects.requireNonNull(subject, "subject cannot be null");
        this.property = Objects.requireNonNull(property, "property cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
    }

    @SuppressWarnings("unchecked")
    @Override
    public OWLAnnotationAssertionAxiomImpl getAxiomWithoutAnnotations() {
        if (!isAnnotated()) {
            return this;
        }
        return new OWLAnnotationAssertionAxiomImpl(getSubject(), getProperty(), getValue(), NO_ANNOTATIONS);
    }

    /**
     * Determines if this annotation assertion deprecates the IRI that is the
     * subject of the annotation.
     *
     * @return {@code true} if this annotation assertion deprecates the subject IRI of the
     * assertion, otherwise {@code false}.
     * @see org.semanticweb.owlapi.model.OWLAnnotation#isDeprecatedIRIAnnotation()
     */
    @Override
    public boolean isDeprecatedIRIAssertion() {
        return property.isDeprecated() && getAnnotation().isDeprecatedIRIAnnotation();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OWLAxiom> T getAnnotatedAxiom(@Nonnull Stream<OWLAnnotation> anns) {
        return (T) new OWLAnnotationAssertionAxiomImpl(getSubject(), getProperty(), getValue(),
                mergeAnnotations(this, anns));
    }

    @Override
    public OWLAnnotationValue getValue() {
        return value;
    }

    @Override
    public OWLAnnotationSubject getSubject() {
        return subject;
    }

    @Override
    public OWLAnnotationProperty getProperty() {
        return property;
    }

    @Override
    public OWLAnnotation getAnnotation() {
        return new OWLAnnotationImplNotAnnotated(property, value);
    }
}
