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
package com.github.owlcs.owlapi.tests.api.syntax;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.model.*;

import java.util.Set;
import java.util.stream.Collectors;

public class InvalidAxiomRoundTripTestCase extends TestBase {

    private OWLOntology o;

    private static void assertCorrectResult(OWLAxiom wrongAxiom, OWLAxiom validAxiom, OWLOntology reloaded) {
        Assertions.assertNotNull(reloaded);
        Assertions.assertTrue(reloaded.containsAxiom(validAxiom));
        Assertions.assertFalse(reloaded.containsAxiom(wrongAxiom));
        Assertions.assertEquals(1, reloaded.getLogicalAxiomCount());
    }

    @BeforeEach
    public void setUp() {
        o = getOWLOntology();
    }

    private OWLOntology saveAndReload() throws OWLOntologyStorageException, OWLOntologyCreationException {
        return roundTrip(o, new FunctionalSyntaxDocumentFormat());
    }

    @Test
    public void testShouldRoundTripInvalidDifferentIndividuals() throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        // given
        OWLNamedIndividual e1 = OWLFunctionalSyntaxFactory.NamedIndividual(IRI.create("urn:tes#", "t1"));
        OWLNamedIndividual e2 = OWLFunctionalSyntaxFactory.NamedIndividual(IRI.create("urn:tes#", "t2"));
        OWLNamedIndividual e3 = OWLFunctionalSyntaxFactory.NamedIndividual(IRI.create("urn:tes#", "t3"));
        // given
        OWLAxiom wrongAxiom = OWLFunctionalSyntaxFactory.DifferentIndividuals(e1);
        OWLAxiom validAxiom = OWLFunctionalSyntaxFactory.DifferentIndividuals(e2, e3);
        // when
        o.add(wrongAxiom, validAxiom);
        // then
        assertCorrectResult(wrongAxiom, validAxiom, saveAndReload());
    }

    @Test
    public void testShouldRoundTripInvalidDisjointObjectProperties() throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        // given
        OWLObjectProperty e1 = OWLFunctionalSyntaxFactory.ObjectProperty(IRI.create("urn:tes#", "t1"));
        OWLObjectProperty e2 = OWLFunctionalSyntaxFactory.ObjectProperty(IRI.create("urn:tes#", "t2"));
        OWLObjectProperty e3 = OWLFunctionalSyntaxFactory.ObjectProperty(IRI.create("urn:tes#", "t3"));
        // given
        OWLAxiom wrongAxiom = OWLFunctionalSyntaxFactory.DisjointObjectProperties(e1);
        OWLAxiom validAxiom = OWLFunctionalSyntaxFactory.DisjointObjectProperties(e2, e3);
        // when
        o.add(wrongAxiom, validAxiom);
        OWLOntology reloaded = saveAndReload();
        // then
        Assertions.assertNotNull(reloaded);
        Assertions.assertTrue(reloaded.containsAxiom(validAxiom));
        Assertions.assertFalse(reloaded.containsAxiom(wrongAxiom));
        Assertions.assertEquals(1, reloaded.getLogicalAxiomCount());
    }

    @Test
    public void testShouldRoundTripInvalidDisjointClasses() throws Exception {
        // given
        OWLClass e1 = OWLFunctionalSyntaxFactory.Class(IRI.create("urn:tes#", "t1"));
        OWLClass e2 = OWLFunctionalSyntaxFactory.Class(IRI.create("urn:tes#", "t2"));
        OWLClass e3 = OWLFunctionalSyntaxFactory.Class(IRI.create("urn:tes#", "t3"));
        // The implementation now checks for classes that only have a single
        // distinct element
        // Note: we cannot distinguish between a self-disjoint axiom and an
        // FSS/API etc created single element axiom.
        // but this is coding around a problem in the spec.
        checkSingletonDisjointFixup(e1, OWLFunctionalSyntaxFactory.DisjointClasses(e1, e1));
        OWLDisjointClassesAxiom singleClassDisjointAxiom = OWLFunctionalSyntaxFactory.DisjointClasses(e1);
        checkSingletonDisjointFixup(e1, singleClassDisjointAxiom);
        OWLAxiom validAxiom = OWLFunctionalSyntaxFactory.DisjointClasses(e2, e3);
        // when
        o.add(singleClassDisjointAxiom, validAxiom);
        OWLOntology reloaded = roundTrip(o, new FunctionalSyntaxDocumentFormat());
        // then
        Assertions.assertNotNull(reloaded);
        Assertions.assertTrue(reloaded.containsAxiom(validAxiom));
        Assertions.assertTrue(reloaded.containsAxiom(singleClassDisjointAxiom));
        Assertions.assertEquals(2, reloaded.getLogicalAxiomCount());
    }

    protected void checkSingletonDisjointFixup(OWLClass e1, OWLDisjointClassesAxiom wrongAxiom) {
        Set<OWLClassExpression> classExpressions = wrongAxiom.classExpressions().collect(Collectors.toSet());
        Assertions.assertEquals(2, classExpressions.size());
        Assertions.assertTrue(classExpressions.contains(e1));
        if (!e1.isOWLThing()) {
            Assertions.assertTrue(classExpressions.contains(OWLFunctionalSyntaxFactory.OWLThing()));
        } else {
            Assertions.assertTrue(classExpressions.contains(OWLFunctionalSyntaxFactory.OWLNothing()));
        }
        Assertions.assertTrue(wrongAxiom.isAnnotated());
    }

    @Test
    public void testShouldRoundTripInvalidDisjointDataProperties() throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        // given
        OWLDataProperty e1 = OWLFunctionalSyntaxFactory.DataProperty(IRI.create("urn:tes#", "t1"));
        OWLDataProperty e2 = OWLFunctionalSyntaxFactory.DataProperty(IRI.create("urn:tes#", "t2"));
        OWLDataProperty e3 = OWLFunctionalSyntaxFactory.DataProperty(IRI.create("urn:tes#", "t3"));
        // given
        OWLAxiom wrongAxiom = OWLFunctionalSyntaxFactory.DisjointDataProperties(e1);
        OWLAxiom validAxiom = OWLFunctionalSyntaxFactory.DisjointDataProperties(e2, e3);
        // when
        o.add(wrongAxiom, validAxiom);
        OWLOntology reloaded = saveAndReload();
        // then
        Assertions.assertNotNull(reloaded);
        Assertions.assertTrue(reloaded.containsAxiom(validAxiom));
        Assertions.assertFalse(reloaded.containsAxiom(wrongAxiom));
        Assertions.assertEquals(1, reloaded.getLogicalAxiomCount());
    }

    @Test
    public void testShouldRoundTripInvalidSameIndividuals() throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        // given
        OWLNamedIndividual e1 = OWLFunctionalSyntaxFactory.NamedIndividual(IRI.create("urn:tes#", "t1"));
        OWLNamedIndividual e2 = OWLFunctionalSyntaxFactory.NamedIndividual(IRI.create("urn:tes#", "t2"));
        OWLNamedIndividual e3 = OWLFunctionalSyntaxFactory.NamedIndividual(IRI.create("urn:tes#", "t3"));
        // given
        OWLAxiom wrongAxiom = OWLFunctionalSyntaxFactory.SameIndividual(e1);
        OWLAxiom validAxiom = OWLFunctionalSyntaxFactory.SameIndividual(e2, e3);
        // when
        o.add(wrongAxiom, validAxiom);
        // then
        assertCorrectResult(wrongAxiom, validAxiom, saveAndReload());
    }

    @Test
    public void testShouldRoundTripInvalidEquivalentClasses() throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        // given
        OWLClass e1 = OWLFunctionalSyntaxFactory.Class(IRI.create("urn:tes#", "t1"));
        OWLClass e2 = OWLFunctionalSyntaxFactory.Class(IRI.create("urn:tes#", "t2"));
        OWLClass e3 = OWLFunctionalSyntaxFactory.Class(IRI.create("urn:tes#", "t3"));
        // given
        OWLAxiom wrongAxiom = OWLFunctionalSyntaxFactory.EquivalentClasses(e1);
        OWLAxiom validAxiom = OWLFunctionalSyntaxFactory.EquivalentClasses(e2, e3);
        // when
        o.add(wrongAxiom, validAxiom);
        OWLOntology reloaded = saveAndReload();
        // then
        Assertions.assertNotNull(reloaded);
        Assertions.assertTrue(reloaded.containsAxiom(validAxiom));
        Assertions.assertFalse(reloaded.containsAxiom(wrongAxiom));
        Assertions.assertEquals(1, reloaded.getLogicalAxiomCount());
    }

    @Test
    public void testShouldRoundTripInvalidEquivalentObjectProperties() throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        // given
        OWLObjectProperty e1 = OWLFunctionalSyntaxFactory.ObjectProperty(IRI.create("urn:tes#", "t1"));
        OWLObjectProperty e2 = OWLFunctionalSyntaxFactory.ObjectProperty(IRI.create("urn:tes#", "t2"));
        OWLObjectProperty e3 = OWLFunctionalSyntaxFactory.ObjectProperty(IRI.create("urn:tes#", "t3"));
        // given
        OWLAxiom wrongAxiom = OWLFunctionalSyntaxFactory.EquivalentObjectProperties(e1);
        OWLAxiom validAxiom = OWLFunctionalSyntaxFactory.EquivalentObjectProperties(e2, e3);
        // when
        o.add(wrongAxiom, validAxiom);
        OWLOntology reloaded = saveAndReload();
        // then
        Assertions.assertNotNull(reloaded);
        Assertions.assertTrue(reloaded.containsAxiom(validAxiom));
        Assertions.assertFalse(reloaded.containsAxiom(wrongAxiom));
        Assertions.assertEquals(1, reloaded.getLogicalAxiomCount());
    }

    @Test
    public void testShouldRoundTripInvalidEquivalentDataProperties() throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        // given
        OWLDataProperty e1 = OWLFunctionalSyntaxFactory.DataProperty(IRI.create("urn:tes#", "t1"));
        OWLDataProperty e2 = OWLFunctionalSyntaxFactory.DataProperty(IRI.create("urn:tes#", "t2"));
        OWLDataProperty e3 = OWLFunctionalSyntaxFactory.DataProperty(IRI.create("urn:tes#", "t3"));
        // given
        OWLAxiom wrongAxiom = OWLFunctionalSyntaxFactory.EquivalentDataProperties(e1);
        OWLAxiom validAxiom = OWLFunctionalSyntaxFactory.EquivalentDataProperties(e2, e3);
        // when
        o.add(wrongAxiom, validAxiom);
        OWLOntology reloaded = saveAndReload();
        // then
        Assertions.assertNotNull(reloaded);
        Assertions.assertTrue(reloaded.containsAxiom(validAxiom));
        Assertions.assertFalse(reloaded.containsAxiom(wrongAxiom));
        Assertions.assertEquals(1, reloaded.getLogicalAxiomCount());
    }
}
