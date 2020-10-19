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
package com.github.owlcs.owlapi.tests.api.baseclasses;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;

/**
 * This test case creates a small ontology and tests the getters in the reasoner
 * interface. The test ontology isn't designed to test the correctness of
 * reasoning results, rather it is designed to test the reasoner returns the
 * results in the form required by the OWL API reasoner interface.
 *
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class ReasonerTestCase extends TestBase {

    private final OWLReasonerFactory reasonerFactory = new StructuralReasonerFactory();
    private OWLReasoner reasoner;

    private OWLOntology createOntology() {
        OWLOntology o = getOWLOntology();
        OWLClass clsA = getClsA();
        OWLClass clsB = getClsB();
        OWLClass clsC = getClsC();
        OWLClass clsD = getClsD();
        OWLClass clsE = getClsE();
        OWLClass clsF = getClsF();
        OWLClass clsG = getClsG();
        OWLClass clsK = getClsK();
        OWLObjectPropertyExpression propP = getPropP();
        OWLObjectPropertyExpression propQ = getPropQ();
        OWLObjectPropertyExpression propR = getPropR();
        OWLObjectPropertyExpression propS = getPropS();
        o.add(OWLFunctionalSyntaxFactory.SubClassOf(clsG, OWLFunctionalSyntaxFactory.OWLThing()),
                OWLFunctionalSyntaxFactory.SubClassOf(OWLFunctionalSyntaxFactory.OWLThing(), clsG),
                OWLFunctionalSyntaxFactory.EquivalentClasses(clsA, clsB),
                OWLFunctionalSyntaxFactory.SubClassOf(clsC, clsB),
                OWLFunctionalSyntaxFactory.SubClassOf(clsD, clsA),
                OWLFunctionalSyntaxFactory.SubClassOf(clsD, clsF),
                OWLFunctionalSyntaxFactory.SubClassOf(clsF, clsD),
                OWLFunctionalSyntaxFactory.SubClassOf(clsE, clsC),
                OWLFunctionalSyntaxFactory.SubClassOf(clsK, clsD),
                OWLFunctionalSyntaxFactory.EquivalentClasses(clsK, OWLFunctionalSyntaxFactory.OWLNothing()),
                OWLFunctionalSyntaxFactory.EquivalentObjectProperties(propP, propQ),
                OWLFunctionalSyntaxFactory.SubObjectPropertyOf(propP, propR),
                OWLFunctionalSyntaxFactory.InverseObjectProperties(propR, propS));
        return o;
    }

    private static OWLObjectProperty getPropS() {
        return OWLFunctionalSyntaxFactory.ObjectProperty(iri("s"));
    }

    private static OWLObjectProperty getPropR() {
        return OWLFunctionalSyntaxFactory.ObjectProperty(iri("r"));
    }

    private static OWLObjectProperty getPropQ() {
        return OWLFunctionalSyntaxFactory.ObjectProperty(iri("q"));
    }

    private static OWLObjectProperty getPropP() {
        return OWLFunctionalSyntaxFactory.ObjectProperty(iri("p"));
    }

    private static OWLClass getClsK() {
        return OWLFunctionalSyntaxFactory.Class(iri("K"));
    }

    private static OWLClass getClsG() {
        return OWLFunctionalSyntaxFactory.Class(iri("G"));
    }

    private static OWLClass getClsF() {
        return OWLFunctionalSyntaxFactory.Class(iri("F"));
    }

    private static OWLClass getClsE() {
        return OWLFunctionalSyntaxFactory.Class(iri("E"));
    }

    private static OWLClass getClsD() {
        return OWLFunctionalSyntaxFactory.Class(iri("D"));
    }

    private static OWLClass getClsC() {
        return OWLFunctionalSyntaxFactory.Class(iri("C"));
    }

    private static OWLClass getClsB() {
        return OWLFunctionalSyntaxFactory.Class(iri("B"));
    }

    private static OWLClass getClsA() {
        return OWLFunctionalSyntaxFactory.Class(iri("A"));
    }

    @Before
    public void setUpOntoAndReasoner() {
        reasoner = reasonerFactory.createReasoner(createOntology());
    }

    @After
    public void tearDown() {
        reasoner.dispose();
    }

    @Test
    public void testGetName() {
        Assert.assertNotNull("name should not be null", reasoner.getReasonerName());
    }

    @Test
    public void testGetVersion() {
        Assert.assertNotNull("version should not be null", reasoner.getReasonerVersion());
    }

    @Test
    public void testGetTopClassNode() {
        Node<OWLClass> node = reasoner.getTopClassNode();
        Assert.assertTrue(node.isTopNode());
        Assert.assertFalse(node.isBottomNode());
        Assert.assertTrue(node.contains(OWLFunctionalSyntaxFactory.OWLThing()));
        Assert.assertTrue(node.contains(getClsG()));
        Assert.assertEquals(2, node.getSize());
        Assert.assertEquals(2, node.entities().count());
        Assert.assertEquals(1, node.getEntitiesMinusTop().size());
        Assert.assertTrue(node.getEntitiesMinusTop().contains(getClsG()));
    }

    @Test
    public void testGetBottomClassNode() {
        Node<OWLClass> node = reasoner.getBottomClassNode();
        Assert.assertTrue(node.isBottomNode());
        Assert.assertFalse(node.isTopNode());
        Assert.assertTrue(node.contains(OWLFunctionalSyntaxFactory.OWLNothing()));
        Assert.assertTrue(node.contains(getClsK()));
        Assert.assertEquals(2, node.getSize());
        Assert.assertEquals(2, node.entities().count());
        Assert.assertEquals(1, node.getEntitiesMinusBottom().size());
        Assert.assertTrue(node.getEntitiesMinusBottom().contains(getClsK()));
    }

    @Test
    public void testGetEquivalentClasses() {
        Node<OWLClass> nTop = reasoner.getEquivalentClasses(OWLFunctionalSyntaxFactory.OWLThing());
        Assert.assertNotNull("object should not be null", nTop);
        Assert.assertEquals(2, nTop.getSize());
        Assert.assertTrue(nTop.contains(OWLFunctionalSyntaxFactory.OWLThing()));
        Assert.assertTrue(nTop.contains(getClsG()));
        Node<OWLClass> nG = reasoner.getEquivalentClasses(getClsG());
        Assert.assertNotNull("object should not be null", nG);
        Assert.assertEquals(2, nG.getSize());
        Assert.assertTrue(nG.contains(OWLFunctionalSyntaxFactory.OWLThing()));
        Assert.assertTrue(nG.contains(getClsG()));
        Assert.assertEquals(nTop, nG);
        Node<OWLClass> nA = reasoner.getEquivalentClasses(getClsA());
        Assert.assertNotNull("object should not be null", nA);
        Assert.assertEquals(2, nA.getSize());
        Assert.assertTrue(nA.contains(getClsA()));
        Assert.assertTrue(nA.contains(getClsB()));
        Node<OWLClass> nB = reasoner.getEquivalentClasses(getClsB());
        Assert.assertNotNull("object should not be null", nB);
        Assert.assertEquals(2, nB.getSize());
        Assert.assertTrue(nB.contains(getClsA()));
        Assert.assertTrue(nB.contains(getClsB()));
        Assert.assertEquals("object should not be null", nA, nB);
        Node<OWLClass> nC = reasoner.getEquivalentClasses(getClsC());
        Assert.assertNotNull("object should not be null", nC);
        Assert.assertEquals(1, nC.getSize());
        Assert.assertTrue(nC.contains(getClsC()));
        Assert.assertEquals(nC.getRepresentativeElement(), getClsC());
        Node<OWLClass> nE = reasoner.getEquivalentClasses(getClsE());
        Assert.assertNotNull("object should not be null", nE);
        Assert.assertEquals(1, nE.getSize());
        Assert.assertTrue(nE.contains(getClsE()));
        Assert.assertEquals(nE.getRepresentativeElement(), getClsE());
        Node<OWLClass> nD = reasoner.getEquivalentClasses(getClsD());
        Assert.assertNotNull("object should not be null", nD);
        Assert.assertEquals(2, nD.getSize());
        Assert.assertTrue(nD.contains(getClsD()));
        Assert.assertTrue(nD.contains(getClsF()));
        Node<OWLClass> nF = reasoner.getEquivalentClasses(getClsF());
        Assert.assertNotNull("object should not be null", nF);
        Assert.assertEquals(2, nF.getSize());
        Assert.assertTrue(nF.contains(getClsD()));
        Assert.assertTrue(nF.contains(getClsF()));
        Assert.assertEquals(nD, nF);
        Node<OWLClass> nBot = reasoner.getEquivalentClasses(OWLFunctionalSyntaxFactory.OWLNothing());
        Assert.assertNotNull("object should not be null", nBot);
        Assert.assertEquals(2, nBot.getSize());
        Assert.assertTrue(nBot.contains(OWLFunctionalSyntaxFactory.OWLNothing()));
        Assert.assertTrue(nBot.contains(getClsK()));
        Node<OWLClass> nK = reasoner.getEquivalentClasses(getClsK());
        Assert.assertNotNull("object should not be null", nK);
        Assert.assertEquals(2, nK.getSize());
        Assert.assertTrue(nBot.contains(OWLFunctionalSyntaxFactory.OWLNothing()));
        Assert.assertTrue(nBot.contains(getClsK()));
        Assert.assertEquals(nBot, nK);
    }

    @Test
    public void testGetSuperClassesDirect() {
        NodeSet<OWLClass> nsSupTop = reasoner.getSuperClasses(OWLFunctionalSyntaxFactory.OWLThing(), true);
        Assert.assertNotNull("object should not be null", nsSupTop);
        Assert.assertTrue(nsSupTop.isEmpty());
        NodeSet<OWLClass> nsSupG = reasoner.getSuperClasses(getClsG(), true);
        Assert.assertNotNull("object should not be null", nsSupG);
        Assert.assertTrue(nsSupG.isEmpty());
        NodeSet<OWLClass> nsSupA = reasoner.getSuperClasses(getClsA(), true);
        Assert.assertNotNull("object should not be null", nsSupA);
        Assert.assertFalse(nsSupA.isEmpty());
        Assert.assertEquals(1, nsSupA.nodes().count());
        Assert.assertTrue(nsSupA.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        Assert.assertTrue(nsSupA.containsEntity(getClsG()));
        Assert.assertTrue(nsSupA.isTopSingleton());
        NodeSet<OWLClass> nsSupB = reasoner.getSuperClasses(getClsB(), true);
        Assert.assertNotNull("object should not be null", nsSupB);
        Assert.assertEquals(1, nsSupB.nodes().count());
        Assert.assertTrue(nsSupB.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        Assert.assertTrue(nsSupB.containsEntity(getClsG()));
        Assert.assertTrue(nsSupB.isTopSingleton());
        NodeSet<OWLClass> nsSupC = reasoner.getSuperClasses(getClsC(), true);
        Assert.assertNotNull("object should not be null", nsSupC);
        Assert.assertEquals(1, nsSupC.nodes().count());
        Assert.assertTrue(nsSupC.containsEntity(getClsA()));
        Assert.assertTrue(nsSupC.containsEntity(getClsB()));
        NodeSet<OWLClass> nsSupE = reasoner.getSuperClasses(getClsE(), true);
        Assert.assertNotNull("object should not be null", nsSupE);
        Assert.assertEquals(1, nsSupE.nodes().count());
        Assert.assertTrue(nsSupE.containsEntity(getClsC()));
        NodeSet<OWLClass> nsSupD = reasoner.getSuperClasses(getClsD(), true);
        Assert.assertNotNull("object should not be null", nsSupD);
        Assert.assertEquals(1, nsSupD.nodes().count());
        Assert.assertTrue(nsSupD.containsEntity(getClsA()));
        Assert.assertTrue(nsSupD.containsEntity(getClsB()));
        NodeSet<OWLClass> nsSupF = reasoner.getSuperClasses(getClsF(), true);
        Assert.assertNotNull("object should not be null", nsSupF);
        Assert.assertEquals(1, nsSupF.nodes().count());
        Assert.assertTrue(nsSupF.containsEntity(getClsA()));
        Assert.assertTrue(nsSupF.containsEntity(getClsB()));
        NodeSet<OWLClass> nsSupK = reasoner.getSuperClasses(getClsK(), true);
        Assert.assertNotNull("object should not be null", nsSupK);
        Assert.assertEquals(2, nsSupK.nodes().count());
        Assert.assertTrue(nsSupK.containsEntity(getClsE()));
        Assert.assertTrue(nsSupK.containsEntity(getClsD()));
        Assert.assertTrue(nsSupK.containsEntity(getClsF()));
        NodeSet<OWLClass> nsSupBot = reasoner.getSuperClasses(OWLFunctionalSyntaxFactory.OWLNothing(), true);
        Assert.assertNotNull("object should not be null", nsSupBot);
        Assert.assertEquals(2, nsSupBot.nodes().count());
        Assert.assertTrue(nsSupBot.containsEntity(getClsE()));
        Assert.assertTrue(nsSupBot.containsEntity(getClsD()));
        Assert.assertTrue(nsSupBot.containsEntity(getClsF()));
    }

    @Test
    public void testGetSuperClasses() {
        NodeSet<OWLClass> nsSupTop = reasoner.getSuperClasses(OWLFunctionalSyntaxFactory.OWLThing(), false);
        Assert.assertNotNull("object should not be null", nsSupTop);
        Assert.assertTrue(nsSupTop.isEmpty());
        NodeSet<OWLClass> nsSupG = reasoner.getSuperClasses(getClsG(), false);
        Assert.assertNotNull("object should not be null", nsSupG);
        Assert.assertTrue(nsSupG.isEmpty());
        NodeSet<OWLClass> nsSupA = reasoner.getSuperClasses(getClsA(), false);
        Assert.assertNotNull("object should not be null", nsSupA);
        Assert.assertFalse(nsSupA.isEmpty());
        Assert.assertEquals(1, nsSupA.nodes().count());
        Assert.assertTrue(nsSupA.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        Assert.assertTrue(nsSupA.containsEntity(getClsG()));
        Assert.assertTrue(nsSupA.isTopSingleton());
        NodeSet<OWLClass> nsSupB = reasoner.getSuperClasses(getClsB(), false);
        Assert.assertNotNull("object should not be null", nsSupB);
        Assert.assertEquals(1, nsSupB.nodes().count());
        Assert.assertTrue(nsSupB.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        Assert.assertTrue(nsSupB.containsEntity(getClsG()));
        Assert.assertTrue(nsSupB.isTopSingleton());
        NodeSet<OWLClass> nsSupC = reasoner.getSuperClasses(getClsC(), false);
        Assert.assertNotNull("object should not be null", nsSupC);
        Assert.assertEquals(2, nsSupC.nodes().count());
        Assert.assertTrue(nsSupC.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        Assert.assertTrue(nsSupC.containsEntity(getClsG()));
        Assert.assertTrue(nsSupC.containsEntity(getClsA()));
        Assert.assertTrue(nsSupC.containsEntity(getClsB()));
        NodeSet<OWLClass> nsSupE = reasoner.getSuperClasses(getClsE(), false);
        Assert.assertNotNull("object should not be null", nsSupE);
        Assert.assertEquals(3, nsSupE.nodes().count());
        Assert.assertTrue(nsSupE.containsEntity(getClsC()));
        Assert.assertTrue(nsSupE.containsEntity(getClsA()));
        Assert.assertTrue(nsSupE.containsEntity(getClsB()));
        Assert.assertTrue(nsSupE.containsEntity(getClsG()));
        Assert.assertTrue(nsSupE.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        NodeSet<OWLClass> nsSupD = reasoner.getSuperClasses(getClsD(), false);
        Assert.assertNotNull("object should not be null", nsSupD);
        Assert.assertEquals(2, nsSupD.nodes().count());
        Assert.assertTrue(nsSupD.containsEntity(getClsA()));
        Assert.assertTrue(nsSupD.containsEntity(getClsB()));
        Assert.assertTrue(nsSupD.containsEntity(getClsG()));
        Assert.assertTrue(nsSupD.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        NodeSet<OWLClass> nsSupF = reasoner.getSuperClasses(getClsF(), false);
        Assert.assertNotNull("object should not be null", nsSupF);
        Assert.assertEquals(2, nsSupF.nodes().count());
        Assert.assertTrue(nsSupF.containsEntity(getClsA()));
        Assert.assertTrue(nsSupF.containsEntity(getClsB()));
        Assert.assertTrue(nsSupF.containsEntity(getClsG()));
        Assert.assertTrue(nsSupF.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        NodeSet<OWLClass> nsSupK = reasoner.getSuperClasses(getClsK(), false);
        Assert.assertNotNull("object should not be null", nsSupK);
        Assert.assertEquals(5, nsSupK.nodes().count());
        Assert.assertTrue(nsSupK.containsEntity(getClsE()));
        Assert.assertTrue(nsSupK.containsEntity(getClsD()));
        Assert.assertTrue(nsSupK.containsEntity(getClsF()));
        Assert.assertTrue(nsSupK.containsEntity(getClsC()));
        Assert.assertTrue(nsSupK.containsEntity(getClsA()));
        Assert.assertTrue(nsSupK.containsEntity(getClsB()));
        Assert.assertTrue(nsSupK.containsEntity(getClsG()));
        Assert.assertTrue(nsSupK.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        NodeSet<OWLClass> nsSupBot = reasoner.getSuperClasses(OWLFunctionalSyntaxFactory.OWLNothing(), false);
        Assert.assertNotNull("object should not be null", nsSupBot);
        Assert.assertEquals(5, nsSupBot.nodes().count());
        Assert.assertTrue(nsSupBot.containsEntity(getClsE()));
        Assert.assertTrue(nsSupBot.containsEntity(getClsD()));
        Assert.assertTrue(nsSupBot.containsEntity(getClsF()));
        Assert.assertTrue(nsSupBot.containsEntity(getClsC()));
        Assert.assertTrue(nsSupBot.containsEntity(getClsA()));
        Assert.assertTrue(nsSupBot.containsEntity(getClsB()));
        Assert.assertTrue(nsSupBot.containsEntity(getClsG()));
        Assert.assertTrue(nsSupBot.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
    }

    @Test
    public void testGetSubClassesDirect() {
        NodeSet<OWLClass> nsSubTop = reasoner.getSubClasses(OWLFunctionalSyntaxFactory.OWLThing(), true);
        Assert.assertNotNull("object should not be null", nsSubTop);
        Assert.assertEquals(1, nsSubTop.nodes().count());
        Assert.assertTrue(nsSubTop.containsEntity(getClsA()));
        Assert.assertTrue(nsSubTop.containsEntity(getClsB()));
        NodeSet<OWLClass> nsSubG = reasoner.getSubClasses(getClsG(), true);
        Assert.assertNotNull("object should not be null", nsSubG);
        Assert.assertEquals(1, nsSubG.nodes().count());
        Assert.assertTrue(nsSubG.containsEntity(getClsA()));
        Assert.assertTrue(nsSubG.containsEntity(getClsB()));
        NodeSet<OWLClass> nsSubA = reasoner.getSubClasses(getClsA(), true);
        Assert.assertNotNull("object should not be null", nsSubA);
        Assert.assertFalse(nsSubG.isEmpty());
        Assert.assertEquals(2, nsSubA.nodes().count());
        Assert.assertTrue(nsSubA.containsEntity(getClsC()));
        Assert.assertTrue(nsSubA.containsEntity(getClsD()));
        Assert.assertTrue(nsSubA.containsEntity(getClsF()));
        NodeSet<OWLClass> nsSubB = reasoner.getSubClasses(getClsB(), true);
        Assert.assertNotNull("object should not be null", nsSubB);
        Assert.assertEquals(2, nsSubB.nodes().count());
        Assert.assertTrue(nsSubB.containsEntity(getClsC()));
        Assert.assertTrue(nsSubB.containsEntity(getClsD()));
        Assert.assertTrue(nsSubB.containsEntity(getClsF()));
        NodeSet<OWLClass> nsSubC = reasoner.getSubClasses(getClsC(), true);
        Assert.assertNotNull("object should not be null", nsSubC);
        Assert.assertEquals(1, nsSubC.nodes().count());
        Assert.assertTrue(nsSubC.containsEntity(getClsE()));
        NodeSet<OWLClass> nsSubE = reasoner.getSubClasses(getClsE(), true);
        Assert.assertNotNull("object should not be null", nsSubE);
        Assert.assertEquals(1, nsSubE.nodes().count());
        Assert.assertTrue(nsSubE.containsEntity(getClsK()));
        Assert.assertTrue(nsSubE.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubD = reasoner.getSubClasses(getClsD(), true);
        Assert.assertNotNull("object should not be null", nsSubD);
        Assert.assertEquals(1, nsSubD.nodes().count());
        Assert.assertTrue(nsSubD.containsEntity(getClsK()));
        Assert.assertTrue(nsSubD.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubF = reasoner.getSubClasses(getClsF(), true);
        Assert.assertNotNull("object should not be null", nsSubF);
        Assert.assertEquals(1, nsSubF.nodes().count());
        Assert.assertTrue(nsSubF.containsEntity(getClsK()));
        Assert.assertTrue(nsSubF.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubK = reasoner.getSubClasses(getClsK(), true);
        Assert.assertNotNull("object should not be null", nsSubK);
        Assert.assertTrue(nsSubK.isEmpty());
        NodeSet<OWLClass> nsSubBot = reasoner.getSubClasses(OWLFunctionalSyntaxFactory.OWLNothing(), true);
        Assert.assertNotNull("object should not be null", nsSubBot);
        Assert.assertTrue(nsSubBot.isEmpty());
    }

    @Test
    public void testGetSubClasses() {
        NodeSet<OWLClass> nsSubTop = reasoner.getSubClasses(OWLFunctionalSyntaxFactory.OWLThing(), false);
        Assert.assertNotNull("object should not be null", nsSubTop);
        Assert.assertEquals(5, nsSubTop.nodes().count());
        Assert.assertTrue(nsSubTop.containsEntity(getClsA()));
        Assert.assertTrue(nsSubTop.containsEntity(getClsB()));
        Assert.assertTrue(nsSubTop.containsEntity(getClsC()));
        Assert.assertTrue(nsSubTop.containsEntity(getClsD()));
        Assert.assertTrue(nsSubTop.containsEntity(getClsF()));
        Assert.assertTrue(nsSubTop.containsEntity(getClsE()));
        Assert.assertTrue(nsSubTop.containsEntity(getClsK()));
        Assert.assertTrue(nsSubTop.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubG = reasoner.getSubClasses(getClsG(), false);
        Assert.assertNotNull("object should not be null", nsSubG);
        Assert.assertEquals(5, nsSubG.nodes().count());
        Assert.assertTrue(nsSubG.containsEntity(getClsA()));
        Assert.assertTrue(nsSubG.containsEntity(getClsB()));
        Assert.assertTrue(nsSubG.containsEntity(getClsC()));
        Assert.assertTrue(nsSubG.containsEntity(getClsD()));
        Assert.assertTrue(nsSubG.containsEntity(getClsF()));
        Assert.assertTrue(nsSubG.containsEntity(getClsE()));
        Assert.assertTrue(nsSubG.containsEntity(getClsK()));
        Assert.assertTrue(nsSubG.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubA = reasoner.getSubClasses(getClsA(), false);
        Assert.assertNotNull("object should not be null", nsSubA);
        Assert.assertFalse(nsSubG.isEmpty());
        Assert.assertEquals(4, nsSubA.nodes().count());
        Assert.assertTrue(nsSubA.containsEntity(getClsC()));
        Assert.assertTrue(nsSubA.containsEntity(getClsD()));
        Assert.assertTrue(nsSubA.containsEntity(getClsF()));
        Assert.assertTrue(nsSubA.containsEntity(getClsE()));
        Assert.assertTrue(nsSubA.containsEntity(getClsK()));
        Assert.assertTrue(nsSubA.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubB = reasoner.getSubClasses(getClsB(), false);
        Assert.assertNotNull("object should not be null", nsSubB);
        Assert.assertEquals(4, nsSubB.nodes().count());
        Assert.assertTrue(nsSubB.containsEntity(getClsC()));
        Assert.assertTrue(nsSubB.containsEntity(getClsD()));
        Assert.assertTrue(nsSubB.containsEntity(getClsF()));
        Assert.assertTrue(nsSubB.containsEntity(getClsE()));
        Assert.assertTrue(nsSubB.containsEntity(getClsK()));
        Assert.assertTrue(nsSubB.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubC = reasoner.getSubClasses(getClsC(), false);
        Assert.assertNotNull("object should not be null", nsSubC);
        Assert.assertEquals(2, nsSubC.nodes().count());
        Assert.assertTrue(nsSubC.containsEntity(getClsE()));
        Assert.assertTrue(nsSubC.containsEntity(getClsK()));
        Assert.assertTrue(nsSubC.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubE = reasoner.getSubClasses(getClsE(), false);
        Assert.assertNotNull("object should not be null", nsSubE);
        Assert.assertEquals(1, nsSubE.nodes().count());
        Assert.assertTrue(nsSubE.containsEntity(getClsK()));
        Assert.assertTrue(nsSubE.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubD = reasoner.getSubClasses(getClsD(), false);
        Assert.assertNotNull("object should not be null", nsSubD);
        Assert.assertEquals(1, nsSubD.nodes().count());
        Assert.assertTrue(nsSubD.containsEntity(getClsK()));
        Assert.assertTrue(nsSubD.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubF = reasoner.getSubClasses(getClsF(), false);
        Assert.assertNotNull("object should not be null", nsSubF);
        Assert.assertEquals(1, nsSubF.nodes().count());
        Assert.assertTrue(nsSubF.containsEntity(getClsK()));
        Assert.assertTrue(nsSubF.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubK = reasoner.getSubClasses(getClsK(), false);
        Assert.assertNotNull("object should not be null", nsSubK);
        Assert.assertTrue(nsSubK.isEmpty());
        NodeSet<OWLClass> nsSubBot = reasoner.getSubClasses(OWLFunctionalSyntaxFactory.OWLNothing(), false);
        Assert.assertNotNull("object should not be null", nsSubBot);
        Assert.assertTrue(nsSubBot.isEmpty());
    }

    @Test
    public void testIsSatisfiable() {
        Assert.assertTrue(reasoner.isSatisfiable(OWLFunctionalSyntaxFactory.OWLThing()));
        Assert.assertTrue(reasoner.isSatisfiable(getClsG()));
        Assert.assertTrue(reasoner.isSatisfiable(getClsA()));
        Assert.assertTrue(reasoner.isSatisfiable(getClsB()));
        Assert.assertTrue(reasoner.isSatisfiable(getClsC()));
        Assert.assertTrue(reasoner.isSatisfiable(getClsD()));
        Assert.assertTrue(reasoner.isSatisfiable(getClsE()));
        Assert.assertFalse(reasoner.isSatisfiable(OWLFunctionalSyntaxFactory.OWLNothing()));
        Assert.assertFalse(reasoner.isSatisfiable(getClsK()));
    }

    @Test
    public void testComputeClassHierarchy() {
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        Assert.assertTrue(reasoner.isPrecomputed(InferenceType.CLASS_HIERARCHY));
    }

    @Test
    public void testGetTopObjectPropertyNode() {
        Node<OWLObjectPropertyExpression> node = reasoner.getTopObjectPropertyNode();
        Assert.assertNotNull("object should not be null", node);
        Assert.assertTrue(node.isTopNode());
    }

    @Test
    public void testGetBottomObjectPropertyNode() {
        Node<OWLObjectPropertyExpression> node = reasoner.getBottomObjectPropertyNode();
        Assert.assertNotNull("object should not be null", node);
        Assert.assertTrue(node.isBottomNode());
    }

    @Test
    public void testGetSubObjectPropertiesDirect() {
        NodeSet<OWLObjectPropertyExpression> nsSubTop = reasoner.getSubObjectProperties(df.getOWLTopObjectProperty(),
                true);
        Assert.assertNotNull("object should not be null", nsSubTop);
        Assert.assertEquals(2, nsSubTop.nodes().count());
        Assert.assertTrue(nsSubTop.containsEntity(getPropR()));
        Assert.assertTrue(nsSubTop.containsEntity(getPropS()));
        Assert.assertTrue(nsSubTop.containsEntity(getPropR().getInverseProperty()));
        Assert.assertTrue(nsSubTop.containsEntity(getPropS().getInverseProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubR = reasoner.getSubObjectProperties(getPropR(), true);
        Assert.assertNotNull("object should not be null", nsSubR);
        Assert.assertEquals(1, nsSubR.nodes().count());
        Assert.assertTrue(nsSubR.containsEntity(getPropP()));
        Assert.assertTrue(nsSubR.containsEntity(getPropQ()));
        NodeSet<OWLObjectPropertyExpression> nsSubRMinus = reasoner
                .getSubObjectProperties(getPropR().getInverseProperty(), true);
        Assert.assertNotNull("object should not be null", nsSubRMinus);
        Assert.assertEquals(1, nsSubRMinus.nodes().count());
        Assert.assertTrue(nsSubRMinus.containsEntity(getPropP().getInverseProperty()));
        Assert.assertTrue(nsSubRMinus.containsEntity(getPropQ().getInverseProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubSMinus = reasoner
                .getSubObjectProperties(getPropS().getInverseProperty(), true);
        Assert.assertNotNull("object should not be null", nsSubSMinus);
        Assert.assertEquals(1, nsSubSMinus.nodes().count());
        Assert.assertTrue(nsSubSMinus.containsEntity(getPropP()));
        Assert.assertTrue(nsSubSMinus.containsEntity(getPropQ()));
        NodeSet<OWLObjectPropertyExpression> nsSubS = reasoner.getSubObjectProperties(getPropS(), true);
        Assert.assertNotNull("object should not be null", nsSubS);
        Assert.assertEquals(1, nsSubS.nodes().count());
        Assert.assertTrue(nsSubS.containsEntity(getPropP().getInverseProperty()));
        Assert.assertTrue(nsSubS.containsEntity(getPropQ().getInverseProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubP = reasoner.getSubObjectProperties(getPropP(), true);
        Assert.assertNotNull("object should not be null", nsSubP);
        Assert.assertEquals(1, nsSubP.nodes().count());
        Assert.assertTrue(nsSubP.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubQ = reasoner.getSubObjectProperties(getPropQ(), true);
        Assert.assertNotNull("object should not be null", nsSubQ);
        Assert.assertEquals(1, nsSubQ.nodes().count());
        Assert.assertTrue(nsSubQ.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubPMinus = reasoner
                .getSubObjectProperties(getPropP().getInverseProperty(), true);
        Assert.assertNotNull("object should not be null", nsSubPMinus);
        Assert.assertEquals(1, nsSubPMinus.nodes().count());
        Assert.assertTrue(nsSubPMinus.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubQMinus = reasoner
                .getSubObjectProperties(getPropQ().getInverseProperty(), true);
        Assert.assertNotNull("object should not be null", nsSubQMinus);
        Assert.assertEquals(1, nsSubQMinus.nodes().count());
        Assert.assertTrue(nsSubQMinus.containsEntity(df.getOWLBottomObjectProperty()));
    }

    @Test
    public void testGetSubObjectProperties() {
        NodeSet<OWLObjectPropertyExpression> nsSubTop = reasoner.getSubObjectProperties(df.getOWLTopObjectProperty(),
                false);
        Assert.assertNotNull("object should not be null", nsSubTop);
        Assert.assertEquals(5, nsSubTop.nodes().count());
        Assert.assertTrue(nsSubTop.containsEntity(getPropR()));
        Assert.assertTrue(nsSubTop.containsEntity(getPropS()));
        Assert.assertTrue(nsSubTop.containsEntity(getPropP()));
        Assert.assertTrue(nsSubTop.containsEntity(getPropQ()));
        Assert.assertTrue(nsSubTop.containsEntity(getPropR().getInverseProperty()));
        Assert.assertTrue(nsSubTop.containsEntity(getPropR().getInverseProperty()));
        Assert.assertTrue(nsSubTop.containsEntity(getPropP().getInverseProperty()));
        Assert.assertTrue(nsSubTop.containsEntity(getPropQ().getInverseProperty()));
        Assert.assertTrue(nsSubTop.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubR = reasoner.getSubObjectProperties(getPropR(), false);
        Assert.assertNotNull("object should not be null", nsSubR);
        Assert.assertEquals(2, nsSubR.nodes().count());
        Assert.assertTrue(nsSubR.containsEntity(getPropP()));
        Assert.assertTrue(nsSubR.containsEntity(getPropQ()));
        Assert.assertTrue(nsSubR.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubRMinus = reasoner
                .getSubObjectProperties(getPropR().getInverseProperty(), false);
        Assert.assertNotNull("object should not be null", nsSubRMinus);
        Assert.assertEquals(2, nsSubRMinus.nodes().count());
        Assert.assertTrue(nsSubRMinus.containsEntity(getPropP().getInverseProperty()));
        Assert.assertTrue(nsSubRMinus.containsEntity(getPropQ().getInverseProperty()));
        Assert.assertTrue(nsSubRMinus.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubSMinus = reasoner
                .getSubObjectProperties(getPropS().getInverseProperty(), false);
        Assert.assertNotNull("object should not be null", nsSubSMinus);
        Assert.assertEquals(2, nsSubSMinus.nodes().count());
        Assert.assertTrue(nsSubRMinus.containsEntity(getPropP().getInverseProperty()));
        Assert.assertTrue(nsSubRMinus.containsEntity(getPropQ().getInverseProperty()));
        Assert.assertTrue(nsSubRMinus.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubS = reasoner.getSubObjectProperties(getPropS(), false);
        Assert.assertNotNull("object should not be null", nsSubS);
        Assert.assertEquals(2, nsSubS.nodes().count());
        Assert.assertTrue(nsSubS.containsEntity(getPropP().getInverseProperty()));
        Assert.assertTrue(nsSubS.containsEntity(getPropQ().getInverseProperty()));
        Assert.assertTrue(nsSubS.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubP = reasoner.getSubObjectProperties(getPropP(), false);
        Assert.assertNotNull("object should not be null", nsSubP);
        Assert.assertEquals(1, nsSubP.nodes().count());
        Assert.assertTrue(nsSubP.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubQ = reasoner.getSubObjectProperties(getPropQ(), false);
        Assert.assertNotNull("object should not be null", nsSubQ);
        Assert.assertEquals(1, nsSubQ.nodes().count());
        Assert.assertTrue(nsSubQ.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubPMinus = reasoner
                .getSubObjectProperties(getPropP().getInverseProperty(), false);
        Assert.assertNotNull("object should not be null", nsSubPMinus);
        Assert.assertEquals(1, nsSubPMinus.nodes().count());
        Assert.assertTrue(nsSubPMinus.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubQMinus = reasoner
                .getSubObjectProperties(getPropQ().getInverseProperty(), false);
        Assert.assertNotNull("object should not be null", nsSubQMinus);
        Assert.assertEquals(1, nsSubQMinus.nodes().count());
        Assert.assertTrue(nsSubQMinus.containsEntity(df.getOWLBottomObjectProperty()));
    }
}
