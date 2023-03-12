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
package com.github.owlcs.owlapi.tests.api.dataproperties;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Matthew Horridge, The University Of Manchester, Information Management Group
 */
public class EquivalentClassesAxiomTestCase extends TestBase {

    @Test
    public void testContainsNamedClass() {
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("B"));
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(propP, clsB);
        OWLClassExpression desc2 = OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(propP, clsA);
        OWLEquivalentClassesAxiom ax = OWLFunctionalSyntaxFactory.EquivalentClasses(clsA, desc);
        Assertions.assertTrue(ax.containsNamedEquivalentClass());
        OWLEquivalentClassesAxiom ax2 = OWLFunctionalSyntaxFactory.EquivalentClasses(desc, desc2);
        Assertions.assertFalse(ax2.containsNamedEquivalentClass());
    }

    @Test
    public void testGetNamedClasses() {
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("B"));
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(propP, clsB);
        OWLEquivalentClassesAxiom ax = OWLFunctionalSyntaxFactory.EquivalentClasses(clsA, desc);
        Set<OWLClass> clses = ax.namedClasses().collect(Collectors.toSet());
        Assertions.assertEquals(1, clses.size());
        Assertions.assertTrue(clses.contains(clsA));
    }

    @Test
    public void testGetNamedClassesWithNothing() {
        OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("B"));
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(propP, clsB);
        OWLEquivalentClassesAxiom ax = OWLFunctionalSyntaxFactory.EquivalentClasses(OWLFunctionalSyntaxFactory.OWLNothing(), desc);
        Set<OWLClass> clses = ax.namedClasses().collect(Collectors.toSet());
        Assertions.assertTrue(clses.isEmpty());
        Assertions.assertFalse(ax.containsOWLThing());
        Assertions.assertTrue(ax.containsOWLNothing());
    }

    @Test
    public void testGetNamedClassesWithThing() {
        OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("B"));
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLClassExpression desc = OWLFunctionalSyntaxFactory.ObjectSomeValuesFrom(propP, clsB);
        OWLEquivalentClassesAxiom ax = OWLFunctionalSyntaxFactory.EquivalentClasses(OWLFunctionalSyntaxFactory.OWLThing(), desc);
        Set<OWLClass> clses = ax.namedClasses().collect(Collectors.toSet());
        Assertions.assertTrue(clses.isEmpty());
        Assertions.assertFalse(ax.containsOWLNothing());
        Assertions.assertTrue(ax.containsOWLThing());
    }

    @Test
    public void testSplit() {
        OWLClass clsA = OWLFunctionalSyntaxFactory.Class(iri("A"));
        OWLClass clsB = OWLFunctionalSyntaxFactory.Class(iri("B"));
        OWLClass clsC = OWLFunctionalSyntaxFactory.Class(iri("C"));
        OWLEquivalentClassesAxiom ax = OWLFunctionalSyntaxFactory.EquivalentClasses(clsA, clsB, clsC);
        Collection<OWLSubClassOfAxiom> scas = ax.asOWLSubClassOfAxioms();
        Assertions.assertEquals(6, scas.size());
        Assertions.assertTrue(scas.contains(OWLFunctionalSyntaxFactory.SubClassOf(clsA, clsB)));
        Assertions.assertTrue(scas.contains(OWLFunctionalSyntaxFactory.SubClassOf(clsB, clsA)));
        Assertions.assertTrue(scas.contains(OWLFunctionalSyntaxFactory.SubClassOf(clsA, clsC)));
        Assertions.assertTrue(scas.contains(OWLFunctionalSyntaxFactory.SubClassOf(clsC, clsA)));
        Assertions.assertTrue(scas.contains(OWLFunctionalSyntaxFactory.SubClassOf(clsB, clsC)));
        Assertions.assertTrue(scas.contains(OWLFunctionalSyntaxFactory.SubClassOf(clsC, clsB)));
    }
}
