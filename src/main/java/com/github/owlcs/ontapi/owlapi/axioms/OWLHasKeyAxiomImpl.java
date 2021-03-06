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

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 * @since 1.2.0
 */
public class OWLHasKeyAxiomImpl extends OWLLogicalAxiomImpl implements OWLHasKeyAxiom {

    private final OWLClassExpression expression;
    private final List<OWLPropertyExpression> propertyExpressions;

    /**
     * @param clazz  a {@link OWLClassExpression}, the subject class expression
     * @param properties a {@code Collection} of {@link OWLObjectPropertyExpression}s
     * @param annotations a {@code Collection} of {@link OWLAnnotation}s on the axiom
     */
    public OWLHasKeyAxiomImpl(OWLClassExpression clazz,
                              Collection<? extends OWLPropertyExpression> properties,
                              Collection<OWLAnnotation> annotations) {
        super(annotations);
        this.expression = Objects.requireNonNull(clazz, "The primary class cannot be null");
        this.propertyExpressions = toContentList(properties, "Properties cannot be null");
    }

    @SuppressWarnings("unchecked")
    @Override
    public OWLHasKeyAxiomImpl getAxiomWithoutAnnotations() {
        if (!isAnnotated()) {
            return this;
        }
        return new OWLHasKeyAxiomImpl(getClassExpression(), propertyExpressions, NO_ANNOTATIONS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OWLAxiom> T getAnnotatedAxiom(@Nonnull Stream<OWLAnnotation> anns) {
        return (T) new OWLHasKeyAxiomImpl(getClassExpression(), propertyExpressions,
                mergeAnnotations(this, anns));
    }

    @Override
    public OWLClassExpression getClassExpression() {
        return expression;
    }

    @Override
    public Stream<OWLPropertyExpression> propertyExpressions() {
        return propertyExpressions.stream();
    }

    @Override
    public Stream<OWLPropertyExpression> operands() {
        return propertyExpressions();
    }

    @Override
    public List<OWLPropertyExpression> getOperandsAsList() {
        return propertyExpressions;
    }
}
