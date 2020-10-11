/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.owlcs.owlapi.tests.api.syntax.rdf;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 */
public class RendererAndParserTestCase extends TestBase {

    public static List<AxiomBuilder> getData() {
        return Arrays.asList(
                // AnonymousIndividual
                () -> singleton(df.getOWLClassAssertionAxiom(df.getOWLObjectComplementOf(OWLFunctionalSyntaxFactory.createClass()),
                        OWLFunctionalSyntaxFactory.createIndividual())),
                // ClassAssertionAxioms
                () -> singleton(df.getOWLClassAssertionAxiom(
                        OWLFunctionalSyntaxFactory.createClass(),
                        OWLFunctionalSyntaxFactory.createIndividual())),
                // DifferentIndividualsAxiom
                () -> singleton(df.getOWLDifferentIndividualsAxiom(
                        OWLFunctionalSyntaxFactory.createIndividual(),
                        OWLFunctionalSyntaxFactory.createIndividual(),
                        OWLFunctionalSyntaxFactory.createIndividual(),
                        OWLFunctionalSyntaxFactory.createIndividual(),
                        OWLFunctionalSyntaxFactory.createIndividual())),
                // EquivalentClasses
                () -> singleton(df.getOWLEquivalentClassesAxiom(
                        OWLFunctionalSyntaxFactory.createClass(),
                        df.getOWLObjectSomeValuesFrom(OWLFunctionalSyntaxFactory.createObjectProperty(), df.getOWLThing()))),
                // NegativeDataPropertyAssertionAxiom
                () -> singleton(df.getOWLNegativeDataPropertyAssertionAxiom(
                        OWLFunctionalSyntaxFactory.createDataProperty(),
                        OWLFunctionalSyntaxFactory.createIndividual(),
                        df.getOWLLiteral("TestConstant"))),
                // NegativeObjectPropertyAssertionAxiom
                () -> singleton(df.getOWLNegativeObjectPropertyAssertionAxiom(
                        OWLFunctionalSyntaxFactory.createObjectProperty(),
                        OWLFunctionalSyntaxFactory.createIndividual(),
                        OWLFunctionalSyntaxFactory.createIndividual())),
                // QCR
                () -> singleton(df.getOWLSubClassOfAxiom(
                        OWLFunctionalSyntaxFactory.createClass(),
                        df.getOWLObjectMinCardinality(3, OWLFunctionalSyntaxFactory.createObjectProperty(),
                                df.getOWLObjectIntersectionOf(OWLFunctionalSyntaxFactory.createClass(),
                                        OWLFunctionalSyntaxFactory.createClass())))));
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testSaveAndReload(AxiomBuilder axioms) throws Exception {
        OWLOntology ontA = getOWLOntology();
        ontA.add(axioms.build());
        OWLOntology ontB = roundTrip(ontA);
        Set<OWLLogicalAxiom> aMinusB = ontA.logicalAxioms().collect(Collectors.toSet());
        ontB.logicalAxioms().forEach(aMinusB::remove);
        Set<OWLLogicalAxiom> bMinusA = ontB.logicalAxioms().collect(Collectors.toSet());
        ontA.logicalAxioms().forEach(bMinusA::remove);
        StringBuilder msg = new StringBuilder();
        if (aMinusB.isEmpty() && bMinusA.isEmpty()) {
            msg.append("Ontology save/load roundtrip OK.\n");
        } else {
            msg.append("Ontology save/load roundtripping error.\n");
            msg.append("=> ").append(aMinusB.size()).append(" axioms lost in roundtripping.\n");
            for (OWLAxiom axiom : aMinusB) {
                msg.append(axiom).append("\n");
            }
            msg.append("=> ").append(bMinusA.size()).append(" axioms added after roundtripping.\n");
            for (OWLAxiom axiom : bMinusA) {
                msg.append(axiom).append("\n");
            }
        }
        Assertions.assertTrue(aMinusB.isEmpty() && bMinusA.isEmpty(), msg.toString());
    }
}
