/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
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
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.search.Searcher;

/**
 * @author Matthew Horridge, The University Of Manchester, Information  Management Group
 */
public class ObjectPropertyTestCase extends TestBase {

    @Test
    public void testInverseInverseSimplification() {
        OWLObjectProperty p = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLObjectPropertyExpression inv = p.getInverseProperty();
        OWLObjectPropertyExpression inv2 = inv.getInverseProperty();
        Assertions.assertEquals(p, inv2);
    }

    @Test
    public void testInverseInverseInverseSimplification() {
        OWLObjectProperty p = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLObjectPropertyExpression inv = p.getInverseProperty();
        OWLObjectPropertyExpression inv2 = inv.getInverseProperty();
        OWLObjectPropertyExpression inv3 = inv2.getInverseProperty();
        Assertions.assertEquals(inv, inv3);
    }

    @Test
    public void testInverse() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLObjectProperty propQ = OWLFunctionalSyntaxFactory.ObjectProperty(iri("q"));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.InverseObjectProperties(propP, propQ);
        ont.getOWLOntologyManager().addAxiom(ont, ax);
        Assertions.assertTrue(Searcher.inverse(ont.inverseObjectPropertyAxioms(propP), propP).anyMatch(propQ::equals));
        Assertions.assertFalse(Searcher.inverse(ont.inverseObjectPropertyAxioms(propP), propP).anyMatch(propP::equals));
    }

    @Test
    public void testInverseSelf() {
        OWLOntology ont = getOWLOntology();
        OWLObjectProperty propP = OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
        OWLAxiom ax = OWLFunctionalSyntaxFactory.InverseObjectProperties(propP, propP);
        ont.getOWLOntologyManager().addAxiom(ont, ax);
        Assertions.assertTrue(Searcher.inverse(ont.inverseObjectPropertyAxioms(propP), propP).anyMatch(propP::equals));
    }

    @Test
    public void testCompareRoleChains() {
        OWLObjectPropertyExpression p = df.getOWLObjectProperty("_:", "p");
        OWLObjectPropertyExpression q = df.getOWLObjectProperty("_:", "q");
        OWLObjectPropertyExpression r = df.getOWLObjectProperty("_:", "r");
        OWLSubPropertyChainOfAxiom ax1 = df.getOWLSubPropertyChainOfAxiom(Lists.newArrayList(p, q), r);
        OWLSubPropertyChainOfAxiom ax2 = df.getOWLSubPropertyChainOfAxiom(Lists.newArrayList(p, p), r);
        Assertions.assertNotEquals(ax1, ax2);
        int comparisonResult = ax1.compareTo(ax2);
        Assertions.assertNotEquals(0, comparisonResult, "role chain comparision:\n " + ax1 + " should not compare to\n " + ax2 + " as 0\n");
    }
}
