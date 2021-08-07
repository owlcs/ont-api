/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2021, owl.cs group.
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

import com.github.owlcs.ontapi.owlapi.objects.ce.ObjectOneOfImpl;
import org.semanticweb.owlapi.model.*;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;


public class SameIndividualAxiomImpl extends NaryIndividualAxiomImpl implements OWLSameIndividualAxiom {

    /**
     * @param individuals a {@code Collection} of {@link OWLIndividual}s
     * @param annotations a {@code Collection} of {@link OWLAnnotation}s on the axiom
     */
    public SameIndividualAxiomImpl(Collection<? extends OWLIndividual> individuals,
                                   Collection<OWLAnnotation> annotations) {
        super(individuals, annotations);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SameIndividualAxiomImpl getAxiomWithoutAnnotations() {
        if (!isAnnotated()) {
            return this;
        }
        return new SameIndividualAxiomImpl(individuals, NO_ANNOTATIONS);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends OWLAxiom> T getAnnotatedAxiom(@Nonnull Stream<OWLAnnotation> anns) {
        return (T) new SameIndividualAxiomImpl(individuals, mergeAnnotations(this, anns));
    }

    @Override
    public Collection<OWLSameIndividualAxiom> asPairwiseAxioms() {
        if (individuals.size() == 2) {
            return createSet(this);
        }
        return walkPairwise((a, b) -> new SameIndividualAxiomImpl(Arrays.asList(a, b), NO_ANNOTATIONS));
    }

    @Override
    public Collection<OWLSameIndividualAxiom> splitToAnnotatedPairs() {
        if (individuals.size() == 2) {
            return createSet(this);
        }
        return walkPairwise((a, b) -> new SameIndividualAxiomImpl(Arrays.asList(a, b), annotations));
    }

    @Override
    public boolean containsAnonymousIndividuals() {
        return individuals().anyMatch(OWLIndividual::isAnonymous);
    }

    @Override
    public Set<OWLSubClassOfAxiom> asOWLSubClassOfAxioms() {
        List<OWLClassExpression> nominalsList = new ArrayList<>();
        individuals().forEach(i -> nominalsList.add(new ObjectOneOfImpl(i)));
        Set<OWLSubClassOfAxiom> result = new HashSet<>();
        for (int i = 0; i < nominalsList.size() - 1; i++) {
            OWLClassExpression ceI = nominalsList.get(i);
            OWLClassExpression ceJ = nominalsList.get(i + 1);
            result.add(new SubClassOfAxiomImpl(ceI, ceJ, NO_ANNOTATIONS));
            result.add(new SubClassOfAxiomImpl(ceJ, ceI, NO_ANNOTATIONS));
        }
        return result;
    }
}
