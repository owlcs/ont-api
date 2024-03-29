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
package com.github.owlcs.owlapi.tests.api.anonymous;

import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.util.OWLObjectDuplicator;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.AnonymousIndividual;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asSet;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class AnonymousIndividualsNormaliser extends OWLObjectDuplicator {

    private final Map<OWLAnonymousIndividual, OWLAnonymousIndividual> renamingMap = new HashMap<>();
    private int counter = 0;

    /**
     * Creates an object duplicator that duplicates objects using the specified
     * data factory.
     *
     * @param m The manager providing data factory and config to be used for the
     *          duplication.
     */
    public AnonymousIndividualsNormaliser(OWLOntologyManager m) {
        super(m);
    }

    @SuppressWarnings("unused")
    public Set<OWLAxiom> getNormalisedAxioms(Collection<OWLAxiom> axioms) {
        return getNormalisedAxioms(axioms.stream());
    }

    public Set<OWLAxiom> getNormalisedAxioms(Stream<OWLAxiom> axioms) {
        return asSet(axioms.map(this::t));
    }

    @Override
    public OWLAnonymousIndividual visit(@Nonnull OWLAnonymousIndividual individual) {
        OWLAnonymousIndividual ind = renamingMap.get(individual);
        if (ind == null) {
            counter++;
            ind = AnonymousIndividual("anon-ind-" + counter);
            renamingMap.put(individual, ind);
        }
        return ind;
    }
}
