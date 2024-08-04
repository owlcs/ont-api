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

import javax.annotation.Nonnull;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;


public class EquivalentClassesAxiomImpl extends NaryClassAxiomImpl implements OWLEquivalentClassesAxiom {

    /**
     * @param classes     a {@code Collection} of {@link OWLClassExpression}s, the equivalent classes
     * @param annotations a {@code Collection} of {@link OWLAnnotation}s
     */
    public EquivalentClassesAxiomImpl(Collection<? extends OWLClassExpression> classes,
                                      Collection<OWLAnnotation> annotations) {
        super(classes, annotations);
    }

    /**
     * Answers {@code true} if the given class expression is a named class,
     * but not {@link OWL#Thing owl:Thing}
     * and not {@link OWL#Nothing owl:Nothing}.
     *
     * @param ce {@link OWLClassExpression}, not {@code null}
     * @return boolean
     */
    public static boolean isNamed(OWLClassExpression ce) {
        return !ce.isAnonymous() && !ce.isOWLNothing() && !ce.isOWLThing();
    }

    @SuppressWarnings("unchecked")
    @Override
    public EquivalentClassesAxiomImpl getAxiomWithoutAnnotations() {
        if (!isAnnotated()) {
            return this;
        }
        return new EquivalentClassesAxiomImpl(classes, NO_ANNOTATIONS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OWLAxiom> T getAnnotatedAxiom(@Nonnull Stream<OWLAnnotation> anns) {
        return (T) new EquivalentClassesAxiomImpl(classes, mergeAnnotations(this, anns));
    }

    @Override
    public Collection<OWLEquivalentClassesAxiom> asPairwiseAxioms() {
        if (classes.size() < 3) {
            return createSet(this);
        }
        return walkPairwise((a, b) -> new EquivalentClassesAxiomImpl(Arrays.asList(a, b), NO_ANNOTATIONS));
    }

    @Override
    public Collection<OWLEquivalentClassesAxiom> splitToAnnotatedPairs() {
        if (classes.size() < 3) {
            return createSet(this);
        }
        return walkPairwise((a, b) -> new EquivalentClassesAxiomImpl(Arrays.asList(a, b), annotations));
    }

    @Override
    public boolean containsNamedEquivalentClass() {
        return classExpressions().anyMatch(EquivalentClassesAxiomImpl::isNamed);
    }

    @Override
    public boolean containsOWLNothing() {
        return classExpressions().anyMatch(OWLClassExpression::isOWLNothing);
    }

    @Override
    public boolean containsOWLThing() {
        return classExpressions().anyMatch(OWLClassExpression::isOWLThing);
    }

    @Override
    public Stream<OWLClass> namedClasses() {
        return classExpressions().filter(EquivalentClassesAxiomImpl::isNamed)
                .map(OWLClassExpression::asOWLClass);
    }

    @Override
    public Collection<OWLSubClassOfAxiom> asOWLSubClassOfAxioms() {
        return walkAllPairwise((a, b) -> new SubClassOfAxiomImpl(a, b, NO_ANNOTATIONS));
    }
}
