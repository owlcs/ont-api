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
package com.github.owlcs.owlapi.tests.api;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.search.EntitySearcher;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class PropertyCharacteristicsAccessorsTestCase extends TestBase {

    @Test
    public void testTransitive() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        Assertions.assertFalse(EntitySearcher.isTransitive(prop, ont));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.TransitiveObjectProperty(prop);
        ont.add(ax);
        Assertions.assertTrue(EntitySearcher.isTransitive(prop, ont));
    }

    @Test
    public void testSymmetric() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        Assertions.assertFalse(EntitySearcher.isSymmetric(prop, ont));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.SymmetricObjectProperty(prop);
        ont.add(ax);
        Assertions.assertTrue(EntitySearcher.isSymmetric(prop, ont));
    }

    @Test
    public void testAsymmetric() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        Assertions.assertFalse(EntitySearcher.isAsymmetric(prop, ont));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.AsymmetricObjectProperty(prop);
        ont.add(ax);
        Assertions.assertTrue(EntitySearcher.isAsymmetric(prop, ont));
    }

    @Test
    public void testReflexive() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        Assertions.assertFalse(EntitySearcher.isReflexive(prop, ont));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.ReflexiveObjectProperty(prop);
        ont.add(ax);
        Assertions.assertTrue(EntitySearcher.isReflexive(prop, ont));
    }

    @Test
    public void testIrreflexive() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        Assertions.assertFalse(EntitySearcher.isIrreflexive(prop, ont));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.IrreflexiveObjectProperty(prop);
        ont.add(ax);
        Assertions.assertTrue(EntitySearcher.isIrreflexive(prop, ont));
    }

    @Test
    public void testFunctional() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        Assertions.assertFalse(EntitySearcher.isFunctional(prop, ont));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.FunctionalObjectProperty(prop);
        ont.add(ax);
        Assertions.assertTrue(EntitySearcher.isFunctional(prop, ont));
    }

    @Test
    public void testInverseFunctional() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty prop = OWLFunctionalSyntaxFactory.ObjectProperty(iri("prop"));
        Assertions.assertFalse(EntitySearcher.isInverseFunctional(prop, ont));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.InverseFunctionalObjectProperty(prop);
        ont.add(ax);
        Assertions.assertTrue(EntitySearcher.isInverseFunctional(prop, ont));
    }

    @Test
    public void testFunctionalDataProperty() {
        OWLOntology ont = getOWLOntology();
        OWLDataProperty prop = OWLFunctionalSyntaxFactory.DataProperty(iri("prop"));
        Assertions.assertFalse(EntitySearcher.isFunctional(prop, ont));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.FunctionalDataProperty(prop);
        ont.add(ax);
        Assertions.assertTrue(EntitySearcher.isFunctional(prop, ont));
    }
}
