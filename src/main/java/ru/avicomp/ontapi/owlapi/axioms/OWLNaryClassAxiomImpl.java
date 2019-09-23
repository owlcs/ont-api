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
package ru.avicomp.ontapi.owlapi.axioms;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLNaryClassAxiom;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 1.2.0
 */
@SuppressWarnings("WeakerAccess")
public abstract class OWLNaryClassAxiomImpl extends OWLClassAxiomImpl implements OWLNaryClassAxiom {

    protected final List<OWLClassExpression> classes;

    /**
     * @param classes a {@code Collection} of {@link OWLClassExpression}s
     * @param annotations a {@code Collection} of annotations
     */
    public OWLNaryClassAxiomImpl(Collection<? extends OWLClassExpression> classes,
                                 Collection<OWLAnnotation> annotations) {
        super(annotations);
        this.classes = toContentList(classes, "Classes cannot be null");
    }

    @Override
    public Stream<OWLClassExpression> classExpressions() {
        return classes.stream();
    }

    @Override
    public List<OWLClassExpression> getOperandsAsList() {
        return classes;
    }

    @Override
    public boolean contains(@Nonnull OWLClassExpression ce) {
        return classes.contains(ce);
    }

    @Override
    public Set<OWLClassExpression> getClassExpressionsMinus(@Nonnull OWLClassExpression... desc) {
        // classExpressions is sorted, use a linked set so there is no need to sort again
        Set<OWLClassExpression> result = new LinkedHashSet<>(classes);
        for (OWLClassExpression d : desc) {
            result.remove(d);
        }
        return result;
    }
}
