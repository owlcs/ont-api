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
package com.github.owlcs.ontapi.owlapi.axioms;

import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectUnionOfImpl;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;


public class DisjointUnionAxiomImpl extends ClassAxiomImpl implements OWLDisjointUnionAxiom {

    private final OWLClass clazz;
    private final List<OWLClassExpression> classes;

    /**
     * @param clazz       {@link OWLClass}, the union
     * @param ces         a {@code Collection} of {@link OWLClassExpression}s, disjoint classes
     * @param annotations a {@code Collection} of {@link OWLAnnotation}s
     */
    public DisjointUnionAxiomImpl(OWLClass clazz,
                                  Collection<? extends OWLClassExpression> ces,
                                  Collection<OWLAnnotation> annotations) {
        super(annotations);
        this.clazz = Objects.requireNonNull(clazz, "Class cannot be null");
        this.classes = toContentList(ces, "Class expressions cannot be null");
    }

    @Override
    public Stream<OWLClassExpression> classExpressions() {
        return classes.stream();
    }

    @Override
    public Stream<OWLClassExpression> operands() {
        return classExpressions();
    }

    @Override
    public List<OWLClassExpression> getOperandsAsList() {
        return classes;
    }

    @SuppressWarnings("unchecked")
    @Override
    public OWLDisjointUnionAxiom getAxiomWithoutAnnotations() {
        if (!isAnnotated()) {
            return this;
        }
        return new DisjointUnionAxiomImpl(clazz, classes, NO_ANNOTATIONS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OWLAxiom> T getAnnotatedAxiom(@Nonnull Stream<OWLAnnotation> annotations) {
        return (T) new DisjointUnionAxiomImpl(clazz, classes, mergeAnnotations(this, annotations));
    }

    @Override
    public OWLClass getOWLClass() {
        return clazz;
    }

    @Override
    public OWLEquivalentClassesAxiom getOWLEquivalentClassesAxiom() {
        return new EquivalentClassesAxiomImpl(Arrays.asList(clazz, new ObjectUnionOfImpl(classes)),
                NO_ANNOTATIONS);
    }

    @Override
    public OWLDisjointClassesAxiom getOWLDisjointClassesAxiom() {
        return new DisjointClassesAxiomImpl(classes, NO_ANNOTATIONS);
    }
}
