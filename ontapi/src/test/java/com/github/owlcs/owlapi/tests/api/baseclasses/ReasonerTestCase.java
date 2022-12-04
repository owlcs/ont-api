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
package com.github.owlcs.owlapi.tests.api.baseclasses;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
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

    @BeforeEach
    public void setUpOntoAndReasoner() {
        reasoner = reasonerFactory.createReasoner(createOntology());
    }

    @AfterEach
    public void tearDown() {
        reasoner.dispose();
    }

    @Test
    public void testGetName() {
        Assertions.assertNotNull(reasoner.getReasonerName());
    }

    @Test
    public void testGetVersion() {
        Assertions.assertNotNull(reasoner.getReasonerVersion());
    }

    @Test
    public void testGetTopClassNode() {
        Node<OWLClass> node = reasoner.getTopClassNode();
        Assertions.assertTrue(node.isTopNode());
        Assertions.assertFalse(node.isBottomNode());
        Assertions.assertTrue(node.contains(OWLFunctionalSyntaxFactory.OWLThing()));
        Assertions.assertTrue(node.contains(getClsG()));
        Assertions.assertEquals(2, node.getSize());
        Assertions.assertEquals(2, node.entities().count());
        Assertions.assertEquals(1, node.getEntitiesMinusTop().size());
        Assertions.assertTrue(node.getEntitiesMinusTop().contains(getClsG()));
    }

    @Test
    public void testGetBottomClassNode() {
        Node<OWLClass> node = reasoner.getBottomClassNode();
        Assertions.assertTrue(node.isBottomNode());
        Assertions.assertFalse(node.isTopNode());
        Assertions.assertTrue(node.contains(OWLFunctionalSyntaxFactory.OWLNothing()));
        Assertions.assertTrue(node.contains(getClsK()));
        Assertions.assertEquals(2, node.getSize());
        Assertions.assertEquals(2, node.entities().count());
        Assertions.assertEquals(1, node.getEntitiesMinusBottom().size());
        Assertions.assertTrue(node.getEntitiesMinusBottom().contains(getClsK()));
    }

    @Test
    public void testGetEquivalentClasses() {
        Node<OWLClass> nTop = reasoner.getEquivalentClasses(OWLFunctionalSyntaxFactory.OWLThing());
        Assertions.assertNotNull(nTop);
        Assertions.assertEquals(2, nTop.getSize());
        Assertions.assertTrue(nTop.contains(OWLFunctionalSyntaxFactory.OWLThing()));
        Assertions.assertTrue(nTop.contains(getClsG()));
        Node<OWLClass> nG = reasoner.getEquivalentClasses(getClsG());
        Assertions.assertNotNull(nG);
        Assertions.assertEquals(2, nG.getSize());
        Assertions.assertTrue(nG.contains(OWLFunctionalSyntaxFactory.OWLThing()));
        Assertions.assertTrue(nG.contains(getClsG()));
        Assertions.assertEquals(nTop, nG);
        Node<OWLClass> nA = reasoner.getEquivalentClasses(getClsA());
        Assertions.assertNotNull(nA);
        Assertions.assertEquals(2, nA.getSize());
        Assertions.assertTrue(nA.contains(getClsA()));
        Assertions.assertTrue(nA.contains(getClsB()));
        Node<OWLClass> nB = reasoner.getEquivalentClasses(getClsB());
        Assertions.assertNotNull(nB);
        Assertions.assertEquals(2, nB.getSize());
        Assertions.assertTrue(nB.contains(getClsA()));
        Assertions.assertTrue(nB.contains(getClsB()));
        Assertions.assertEquals(nA, nB);
        Node<OWLClass> nC = reasoner.getEquivalentClasses(getClsC());
        Assertions.assertNotNull(nC);
        Assertions.assertEquals(1, nC.getSize());
        Assertions.assertTrue(nC.contains(getClsC()));
        Assertions.assertEquals(nC.getRepresentativeElement(), getClsC());
        Node<OWLClass> nE = reasoner.getEquivalentClasses(getClsE());
        Assertions.assertNotNull(nE);
        Assertions.assertEquals(1, nE.getSize());
        Assertions.assertTrue(nE.contains(getClsE()));
        Assertions.assertEquals(nE.getRepresentativeElement(), getClsE());
        Node<OWLClass> nD = reasoner.getEquivalentClasses(getClsD());
        Assertions.assertNotNull(nD);
        Assertions.assertEquals(2, nD.getSize());
        Assertions.assertTrue(nD.contains(getClsD()));
        Assertions.assertTrue(nD.contains(getClsF()));
        Node<OWLClass> nF = reasoner.getEquivalentClasses(getClsF());
        Assertions.assertNotNull(nF);
        Assertions.assertEquals(2, nF.getSize());
        Assertions.assertTrue(nF.contains(getClsD()));
        Assertions.assertTrue(nF.contains(getClsF()));
        Assertions.assertEquals(nD, nF);
        Node<OWLClass> nBot = reasoner.getEquivalentClasses(OWLFunctionalSyntaxFactory.OWLNothing());
        Assertions.assertNotNull(nBot);
        Assertions.assertEquals(2, nBot.getSize());
        Assertions.assertTrue(nBot.contains(OWLFunctionalSyntaxFactory.OWLNothing()));
        Assertions.assertTrue(nBot.contains(getClsK()));
        Node<OWLClass> nK = reasoner.getEquivalentClasses(getClsK());
        Assertions.assertNotNull(nK);
        Assertions.assertEquals(2, nK.getSize());
        Assertions.assertTrue(nBot.contains(OWLFunctionalSyntaxFactory.OWLNothing()));
        Assertions.assertTrue(nBot.contains(getClsK()));
        Assertions.assertEquals(nBot, nK);
    }

    @Test
    public void testGetSuperClassesDirect() {
        NodeSet<OWLClass> nsSupTop = reasoner.getSuperClasses(OWLFunctionalSyntaxFactory.OWLThing(), true);
        Assertions.assertNotNull(nsSupTop);
        Assertions.assertTrue(nsSupTop.isEmpty());
        NodeSet<OWLClass> nsSupG = reasoner.getSuperClasses(getClsG(), true);
        Assertions.assertNotNull(nsSupG);
        Assertions.assertTrue(nsSupG.isEmpty());
        NodeSet<OWLClass> nsSupA = reasoner.getSuperClasses(getClsA(), true);
        Assertions.assertNotNull(nsSupA);
        Assertions.assertFalse(nsSupA.isEmpty());
        Assertions.assertEquals(1, nsSupA.nodes().count());
        Assertions.assertTrue(nsSupA.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        Assertions.assertTrue(nsSupA.containsEntity(getClsG()));
        Assertions.assertTrue(nsSupA.isTopSingleton());
        NodeSet<OWLClass> nsSupB = reasoner.getSuperClasses(getClsB(), true);
        Assertions.assertNotNull(nsSupB);
        Assertions.assertEquals(1, nsSupB.nodes().count());
        Assertions.assertTrue(nsSupB.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        Assertions.assertTrue(nsSupB.containsEntity(getClsG()));
        Assertions.assertTrue(nsSupB.isTopSingleton());
        NodeSet<OWLClass> nsSupC = reasoner.getSuperClasses(getClsC(), true);
        Assertions.assertNotNull(nsSupC);
        Assertions.assertEquals(1, nsSupC.nodes().count());
        Assertions.assertTrue(nsSupC.containsEntity(getClsA()));
        Assertions.assertTrue(nsSupC.containsEntity(getClsB()));
        NodeSet<OWLClass> nsSupE = reasoner.getSuperClasses(getClsE(), true);
        Assertions.assertNotNull(nsSupE);
        Assertions.assertEquals(1, nsSupE.nodes().count());
        Assertions.assertTrue(nsSupE.containsEntity(getClsC()));
        NodeSet<OWLClass> nsSupD = reasoner.getSuperClasses(getClsD(), true);
        Assertions.assertNotNull(nsSupD);
        Assertions.assertEquals(1, nsSupD.nodes().count());
        Assertions.assertTrue(nsSupD.containsEntity(getClsA()));
        Assertions.assertTrue(nsSupD.containsEntity(getClsB()));
        NodeSet<OWLClass> nsSupF = reasoner.getSuperClasses(getClsF(), true);
        Assertions.assertNotNull(nsSupF);
        Assertions.assertEquals(1, nsSupF.nodes().count());
        Assertions.assertTrue(nsSupF.containsEntity(getClsA()));
        Assertions.assertTrue(nsSupF.containsEntity(getClsB()));
        NodeSet<OWLClass> nsSupK = reasoner.getSuperClasses(getClsK(), true);
        Assertions.assertNotNull(nsSupK);
        Assertions.assertEquals(2, nsSupK.nodes().count());
        Assertions.assertTrue(nsSupK.containsEntity(getClsE()));
        Assertions.assertTrue(nsSupK.containsEntity(getClsD()));
        Assertions.assertTrue(nsSupK.containsEntity(getClsF()));
        NodeSet<OWLClass> nsSupBot = reasoner.getSuperClasses(OWLFunctionalSyntaxFactory.OWLNothing(), true);
        Assertions.assertNotNull(nsSupBot);
        Assertions.assertEquals(2, nsSupBot.nodes().count());
        Assertions.assertTrue(nsSupBot.containsEntity(getClsE()));
        Assertions.assertTrue(nsSupBot.containsEntity(getClsD()));
        Assertions.assertTrue(nsSupBot.containsEntity(getClsF()));
    }

    @Test
    public void testGetSuperClasses() {
        NodeSet<OWLClass> nsSupTop = reasoner.getSuperClasses(OWLFunctionalSyntaxFactory.OWLThing(), false);
        Assertions.assertNotNull(nsSupTop);
        Assertions.assertTrue(nsSupTop.isEmpty());
        NodeSet<OWLClass> nsSupG = reasoner.getSuperClasses(getClsG(), false);
        Assertions.assertNotNull(nsSupG);
        Assertions.assertTrue(nsSupG.isEmpty());
        NodeSet<OWLClass> nsSupA = reasoner.getSuperClasses(getClsA(), false);
        Assertions.assertNotNull(nsSupA);
        Assertions.assertFalse(nsSupA.isEmpty());
        Assertions.assertEquals(1, nsSupA.nodes().count());
        Assertions.assertTrue(nsSupA.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        Assertions.assertTrue(nsSupA.containsEntity(getClsG()));
        Assertions.assertTrue(nsSupA.isTopSingleton());
        NodeSet<OWLClass> nsSupB = reasoner.getSuperClasses(getClsB(), false);
        Assertions.assertNotNull(nsSupB);
        Assertions.assertEquals(1, nsSupB.nodes().count());
        Assertions.assertTrue(nsSupB.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        Assertions.assertTrue(nsSupB.containsEntity(getClsG()));
        Assertions.assertTrue(nsSupB.isTopSingleton());
        NodeSet<OWLClass> nsSupC = reasoner.getSuperClasses(getClsC(), false);
        Assertions.assertNotNull(nsSupC);
        Assertions.assertEquals(2, nsSupC.nodes().count());
        Assertions.assertTrue(nsSupC.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        Assertions.assertTrue(nsSupC.containsEntity(getClsG()));
        Assertions.assertTrue(nsSupC.containsEntity(getClsA()));
        Assertions.assertTrue(nsSupC.containsEntity(getClsB()));
        NodeSet<OWLClass> nsSupE = reasoner.getSuperClasses(getClsE(), false);
        Assertions.assertNotNull(nsSupE);
        Assertions.assertEquals(3, nsSupE.nodes().count());
        Assertions.assertTrue(nsSupE.containsEntity(getClsC()));
        Assertions.assertTrue(nsSupE.containsEntity(getClsA()));
        Assertions.assertTrue(nsSupE.containsEntity(getClsB()));
        Assertions.assertTrue(nsSupE.containsEntity(getClsG()));
        Assertions.assertTrue(nsSupE.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        NodeSet<OWLClass> nsSupD = reasoner.getSuperClasses(getClsD(), false);
        Assertions.assertNotNull(nsSupD);
        Assertions.assertEquals(2, nsSupD.nodes().count());
        Assertions.assertTrue(nsSupD.containsEntity(getClsA()));
        Assertions.assertTrue(nsSupD.containsEntity(getClsB()));
        Assertions.assertTrue(nsSupD.containsEntity(getClsG()));
        Assertions.assertTrue(nsSupD.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        NodeSet<OWLClass> nsSupF = reasoner.getSuperClasses(getClsF(), false);
        Assertions.assertNotNull(nsSupF);
        Assertions.assertEquals(2, nsSupF.nodes().count());
        Assertions.assertTrue(nsSupF.containsEntity(getClsA()));
        Assertions.assertTrue(nsSupF.containsEntity(getClsB()));
        Assertions.assertTrue(nsSupF.containsEntity(getClsG()));
        Assertions.assertTrue(nsSupF.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        NodeSet<OWLClass> nsSupK = reasoner.getSuperClasses(getClsK(), false);
        Assertions.assertNotNull(nsSupK);
        Assertions.assertEquals(5, nsSupK.nodes().count());
        Assertions.assertTrue(nsSupK.containsEntity(getClsE()));
        Assertions.assertTrue(nsSupK.containsEntity(getClsD()));
        Assertions.assertTrue(nsSupK.containsEntity(getClsF()));
        Assertions.assertTrue(nsSupK.containsEntity(getClsC()));
        Assertions.assertTrue(nsSupK.containsEntity(getClsA()));
        Assertions.assertTrue(nsSupK.containsEntity(getClsB()));
        Assertions.assertTrue(nsSupK.containsEntity(getClsG()));
        Assertions.assertTrue(nsSupK.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
        NodeSet<OWLClass> nsSupBot = reasoner.getSuperClasses(OWLFunctionalSyntaxFactory.OWLNothing(), false);
        Assertions.assertNotNull(nsSupBot);
        Assertions.assertEquals(5, nsSupBot.nodes().count());
        Assertions.assertTrue(nsSupBot.containsEntity(getClsE()));
        Assertions.assertTrue(nsSupBot.containsEntity(getClsD()));
        Assertions.assertTrue(nsSupBot.containsEntity(getClsF()));
        Assertions.assertTrue(nsSupBot.containsEntity(getClsC()));
        Assertions.assertTrue(nsSupBot.containsEntity(getClsA()));
        Assertions.assertTrue(nsSupBot.containsEntity(getClsB()));
        Assertions.assertTrue(nsSupBot.containsEntity(getClsG()));
        Assertions.assertTrue(nsSupBot.containsEntity(OWLFunctionalSyntaxFactory.OWLThing()));
    }

    @Test
    public void testGetSubClassesDirect() {
        NodeSet<OWLClass> nsSubTop = reasoner.getSubClasses(OWLFunctionalSyntaxFactory.OWLThing(), true);
        Assertions.assertNotNull(nsSubTop);
        Assertions.assertEquals(1, nsSubTop.nodes().count());
        Assertions.assertTrue(nsSubTop.containsEntity(getClsA()));
        Assertions.assertTrue(nsSubTop.containsEntity(getClsB()));
        NodeSet<OWLClass> nsSubG = reasoner.getSubClasses(getClsG(), true);
        Assertions.assertNotNull(nsSubG);
        Assertions.assertEquals(1, nsSubG.nodes().count());
        Assertions.assertTrue(nsSubG.containsEntity(getClsA()));
        Assertions.assertTrue(nsSubG.containsEntity(getClsB()));
        NodeSet<OWLClass> nsSubA = reasoner.getSubClasses(getClsA(), true);
        Assertions.assertNotNull(nsSubA);
        Assertions.assertFalse(nsSubG.isEmpty());
        Assertions.assertEquals(2, nsSubA.nodes().count());
        Assertions.assertTrue(nsSubA.containsEntity(getClsC()));
        Assertions.assertTrue(nsSubA.containsEntity(getClsD()));
        Assertions.assertTrue(nsSubA.containsEntity(getClsF()));
        NodeSet<OWLClass> nsSubB = reasoner.getSubClasses(getClsB(), true);
        Assertions.assertNotNull(nsSubB);
        Assertions.assertEquals(2, nsSubB.nodes().count());
        Assertions.assertTrue(nsSubB.containsEntity(getClsC()));
        Assertions.assertTrue(nsSubB.containsEntity(getClsD()));
        Assertions.assertTrue(nsSubB.containsEntity(getClsF()));
        NodeSet<OWLClass> nsSubC = reasoner.getSubClasses(getClsC(), true);
        Assertions.assertNotNull(nsSubC);
        Assertions.assertEquals(1, nsSubC.nodes().count());
        Assertions.assertTrue(nsSubC.containsEntity(getClsE()));
        NodeSet<OWLClass> nsSubE = reasoner.getSubClasses(getClsE(), true);
        Assertions.assertNotNull(nsSubE);
        Assertions.assertEquals(1, nsSubE.nodes().count());
        Assertions.assertTrue(nsSubE.containsEntity(getClsK()));
        Assertions.assertTrue(nsSubE.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubD = reasoner.getSubClasses(getClsD(), true);
        Assertions.assertNotNull(nsSubD);
        Assertions.assertEquals(1, nsSubD.nodes().count());
        Assertions.assertTrue(nsSubD.containsEntity(getClsK()));
        Assertions.assertTrue(nsSubD.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubF = reasoner.getSubClasses(getClsF(), true);
        Assertions.assertNotNull(nsSubF);
        Assertions.assertEquals(1, nsSubF.nodes().count());
        Assertions.assertTrue(nsSubF.containsEntity(getClsK()));
        Assertions.assertTrue(nsSubF.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubK = reasoner.getSubClasses(getClsK(), true);
        Assertions.assertNotNull(nsSubK);
        Assertions.assertTrue(nsSubK.isEmpty());
        NodeSet<OWLClass> nsSubBot = reasoner.getSubClasses(OWLFunctionalSyntaxFactory.OWLNothing(), true);
        Assertions.assertNotNull(nsSubBot);
        Assertions.assertTrue(nsSubBot.isEmpty());
    }

    @Test
    public void testGetSubClasses() {
        NodeSet<OWLClass> nsSubTop = reasoner.getSubClasses(OWLFunctionalSyntaxFactory.OWLThing(), false);
        Assertions.assertNotNull(nsSubTop);
        Assertions.assertEquals(5, nsSubTop.nodes().count());
        Assertions.assertTrue(nsSubTop.containsEntity(getClsA()));
        Assertions.assertTrue(nsSubTop.containsEntity(getClsB()));
        Assertions.assertTrue(nsSubTop.containsEntity(getClsC()));
        Assertions.assertTrue(nsSubTop.containsEntity(getClsD()));
        Assertions.assertTrue(nsSubTop.containsEntity(getClsF()));
        Assertions.assertTrue(nsSubTop.containsEntity(getClsE()));
        Assertions.assertTrue(nsSubTop.containsEntity(getClsK()));
        Assertions.assertTrue(nsSubTop.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubG = reasoner.getSubClasses(getClsG(), false);
        Assertions.assertNotNull(nsSubG);
        Assertions.assertEquals(5, nsSubG.nodes().count());
        Assertions.assertTrue(nsSubG.containsEntity(getClsA()));
        Assertions.assertTrue(nsSubG.containsEntity(getClsB()));
        Assertions.assertTrue(nsSubG.containsEntity(getClsC()));
        Assertions.assertTrue(nsSubG.containsEntity(getClsD()));
        Assertions.assertTrue(nsSubG.containsEntity(getClsF()));
        Assertions.assertTrue(nsSubG.containsEntity(getClsE()));
        Assertions.assertTrue(nsSubG.containsEntity(getClsK()));
        Assertions.assertTrue(nsSubG.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubA = reasoner.getSubClasses(getClsA(), false);
        Assertions.assertNotNull(nsSubA);
        Assertions.assertFalse(nsSubG.isEmpty());
        Assertions.assertEquals(4, nsSubA.nodes().count());
        Assertions.assertTrue(nsSubA.containsEntity(getClsC()));
        Assertions.assertTrue(nsSubA.containsEntity(getClsD()));
        Assertions.assertTrue(nsSubA.containsEntity(getClsF()));
        Assertions.assertTrue(nsSubA.containsEntity(getClsE()));
        Assertions.assertTrue(nsSubA.containsEntity(getClsK()));
        Assertions.assertTrue(nsSubA.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubB = reasoner.getSubClasses(getClsB(), false);
        Assertions.assertNotNull(nsSubB);
        Assertions.assertEquals(4, nsSubB.nodes().count());
        Assertions.assertTrue(nsSubB.containsEntity(getClsC()));
        Assertions.assertTrue(nsSubB.containsEntity(getClsD()));
        Assertions.assertTrue(nsSubB.containsEntity(getClsF()));
        Assertions.assertTrue(nsSubB.containsEntity(getClsE()));
        Assertions.assertTrue(nsSubB.containsEntity(getClsK()));
        Assertions.assertTrue(nsSubB.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubC = reasoner.getSubClasses(getClsC(), false);
        Assertions.assertNotNull(nsSubC);
        Assertions.assertEquals(2, nsSubC.nodes().count());
        Assertions.assertTrue(nsSubC.containsEntity(getClsE()));
        Assertions.assertTrue(nsSubC.containsEntity(getClsK()));
        Assertions.assertTrue(nsSubC.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubE = reasoner.getSubClasses(getClsE(), false);
        Assertions.assertNotNull(nsSubE);
        Assertions.assertEquals(1, nsSubE.nodes().count());
        Assertions.assertTrue(nsSubE.containsEntity(getClsK()));
        Assertions.assertTrue(nsSubE.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubD = reasoner.getSubClasses(getClsD(), false);
        Assertions.assertNotNull(nsSubD);
        Assertions.assertEquals(1, nsSubD.nodes().count());
        Assertions.assertTrue(nsSubD.containsEntity(getClsK()));
        Assertions.assertTrue(nsSubD.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubF = reasoner.getSubClasses(getClsF(), false);
        Assertions.assertNotNull(nsSubF);
        Assertions.assertEquals(1, nsSubF.nodes().count());
        Assertions.assertTrue(nsSubF.containsEntity(getClsK()));
        Assertions.assertTrue(nsSubF.containsEntity(OWLFunctionalSyntaxFactory.OWLNothing()));
        NodeSet<OWLClass> nsSubK = reasoner.getSubClasses(getClsK(), false);
        Assertions.assertNotNull(nsSubK);
        Assertions.assertTrue(nsSubK.isEmpty());
        NodeSet<OWLClass> nsSubBot = reasoner.getSubClasses(OWLFunctionalSyntaxFactory.OWLNothing(), false);
        Assertions.assertNotNull(nsSubBot);
        Assertions.assertTrue(nsSubBot.isEmpty());
    }

    @Test
    public void testIsSatisfiable() {
        Assertions.assertTrue(reasoner.isSatisfiable(OWLFunctionalSyntaxFactory.OWLThing()));
        Assertions.assertTrue(reasoner.isSatisfiable(getClsG()));
        Assertions.assertTrue(reasoner.isSatisfiable(getClsA()));
        Assertions.assertTrue(reasoner.isSatisfiable(getClsB()));
        Assertions.assertTrue(reasoner.isSatisfiable(getClsC()));
        Assertions.assertTrue(reasoner.isSatisfiable(getClsD()));
        Assertions.assertTrue(reasoner.isSatisfiable(getClsE()));
        Assertions.assertFalse(reasoner.isSatisfiable(OWLFunctionalSyntaxFactory.OWLNothing()));
        Assertions.assertFalse(reasoner.isSatisfiable(getClsK()));
    }

    @Test
    public void testComputeClassHierarchy() {
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        Assertions.assertTrue(reasoner.isPrecomputed(InferenceType.CLASS_HIERARCHY));
    }

    @Test
    public void testGetTopObjectPropertyNode() {
        Node<OWLObjectPropertyExpression> node = reasoner.getTopObjectPropertyNode();
        Assertions.assertNotNull(node);
        Assertions.assertTrue(node.isTopNode());
    }

    @Test
    public void testGetBottomObjectPropertyNode() {
        Node<OWLObjectPropertyExpression> node = reasoner.getBottomObjectPropertyNode();
        Assertions.assertNotNull(node);
        Assertions.assertTrue(node.isBottomNode());
    }

    @Test
    public void testGetSubObjectPropertiesDirect() {
        NodeSet<OWLObjectPropertyExpression> nsSubTop = reasoner.getSubObjectProperties(df.getOWLTopObjectProperty(),
                true);
        Assertions.assertNotNull(nsSubTop);
        Assertions.assertEquals(2, nsSubTop.nodes().count());
        Assertions.assertTrue(nsSubTop.containsEntity(getPropR()));
        Assertions.assertTrue(nsSubTop.containsEntity(getPropS()));
        Assertions.assertTrue(nsSubTop.containsEntity(getPropR().getInverseProperty()));
        Assertions.assertTrue(nsSubTop.containsEntity(getPropS().getInverseProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubR = reasoner.getSubObjectProperties(getPropR(), true);
        Assertions.assertNotNull(nsSubR);
        Assertions.assertEquals(1, nsSubR.nodes().count());
        Assertions.assertTrue(nsSubR.containsEntity(getPropP()));
        Assertions.assertTrue(nsSubR.containsEntity(getPropQ()));
        NodeSet<OWLObjectPropertyExpression> nsSubRMinus = reasoner
                .getSubObjectProperties(getPropR().getInverseProperty(), true);
        Assertions.assertNotNull(nsSubRMinus);
        Assertions.assertEquals(1, nsSubRMinus.nodes().count());
        Assertions.assertTrue(nsSubRMinus.containsEntity(getPropP().getInverseProperty()));
        Assertions.assertTrue(nsSubRMinus.containsEntity(getPropQ().getInverseProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubSMinus = reasoner
                .getSubObjectProperties(getPropS().getInverseProperty(), true);
        Assertions.assertNotNull(nsSubSMinus);
        Assertions.assertEquals(1, nsSubSMinus.nodes().count());
        Assertions.assertTrue(nsSubSMinus.containsEntity(getPropP()));
        Assertions.assertTrue(nsSubSMinus.containsEntity(getPropQ()));
        NodeSet<OWLObjectPropertyExpression> nsSubS = reasoner.getSubObjectProperties(getPropS(), true);
        Assertions.assertNotNull(nsSubS);
        Assertions.assertEquals(1, nsSubS.nodes().count());
        Assertions.assertTrue(nsSubS.containsEntity(getPropP().getInverseProperty()));
        Assertions.assertTrue(nsSubS.containsEntity(getPropQ().getInverseProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubP = reasoner.getSubObjectProperties(getPropP(), true);
        Assertions.assertNotNull(nsSubP);
        Assertions.assertEquals(1, nsSubP.nodes().count());
        Assertions.assertTrue(nsSubP.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubQ = reasoner.getSubObjectProperties(getPropQ(), true);
        Assertions.assertNotNull(nsSubQ);
        Assertions.assertEquals(1, nsSubQ.nodes().count());
        Assertions.assertTrue(nsSubQ.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubPMinus = reasoner
                .getSubObjectProperties(getPropP().getInverseProperty(), true);
        Assertions.assertNotNull(nsSubPMinus);
        Assertions.assertEquals(1, nsSubPMinus.nodes().count());
        Assertions.assertTrue(nsSubPMinus.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubQMinus = reasoner
                .getSubObjectProperties(getPropQ().getInverseProperty(), true);
        Assertions.assertNotNull(nsSubQMinus);
        Assertions.assertEquals(1, nsSubQMinus.nodes().count());
        Assertions.assertTrue(nsSubQMinus.containsEntity(df.getOWLBottomObjectProperty()));
    }

    @Test
    public void testGetSubObjectProperties() {
        NodeSet<OWLObjectPropertyExpression> nsSubTop = reasoner.getSubObjectProperties(df.getOWLTopObjectProperty(),
                false);
        Assertions.assertNotNull(nsSubTop);
        Assertions.assertEquals(5, nsSubTop.nodes().count());
        Assertions.assertTrue(nsSubTop.containsEntity(getPropR()));
        Assertions.assertTrue(nsSubTop.containsEntity(getPropS()));
        Assertions.assertTrue(nsSubTop.containsEntity(getPropP()));
        Assertions.assertTrue(nsSubTop.containsEntity(getPropQ()));
        Assertions.assertTrue(nsSubTop.containsEntity(getPropR().getInverseProperty()));
        Assertions.assertTrue(nsSubTop.containsEntity(getPropR().getInverseProperty()));
        Assertions.assertTrue(nsSubTop.containsEntity(getPropP().getInverseProperty()));
        Assertions.assertTrue(nsSubTop.containsEntity(getPropQ().getInverseProperty()));
        Assertions.assertTrue(nsSubTop.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubR = reasoner.getSubObjectProperties(getPropR(), false);
        Assertions.assertNotNull(nsSubR);
        Assertions.assertEquals(2, nsSubR.nodes().count());
        Assertions.assertTrue(nsSubR.containsEntity(getPropP()));
        Assertions.assertTrue(nsSubR.containsEntity(getPropQ()));
        Assertions.assertTrue(nsSubR.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubRMinus = reasoner
                .getSubObjectProperties(getPropR().getInverseProperty(), false);
        Assertions.assertNotNull(nsSubRMinus);
        Assertions.assertEquals(2, nsSubRMinus.nodes().count());
        Assertions.assertTrue(nsSubRMinus.containsEntity(getPropP().getInverseProperty()));
        Assertions.assertTrue(nsSubRMinus.containsEntity(getPropQ().getInverseProperty()));
        Assertions.assertTrue(nsSubRMinus.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubSMinus = reasoner
                .getSubObjectProperties(getPropS().getInverseProperty(), false);
        Assertions.assertNotNull(nsSubSMinus);
        Assertions.assertEquals(2, nsSubSMinus.nodes().count());
        Assertions.assertTrue(nsSubRMinus.containsEntity(getPropP().getInverseProperty()));
        Assertions.assertTrue(nsSubRMinus.containsEntity(getPropQ().getInverseProperty()));
        Assertions.assertTrue(nsSubRMinus.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubS = reasoner.getSubObjectProperties(getPropS(), false);
        Assertions.assertNotNull(nsSubS);
        Assertions.assertEquals(2, nsSubS.nodes().count());
        Assertions.assertTrue(nsSubS.containsEntity(getPropP().getInverseProperty()));
        Assertions.assertTrue(nsSubS.containsEntity(getPropQ().getInverseProperty()));
        Assertions.assertTrue(nsSubS.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubP = reasoner.getSubObjectProperties(getPropP(), false);
        Assertions.assertNotNull(nsSubP);
        Assertions.assertEquals(1, nsSubP.nodes().count());
        Assertions.assertTrue(nsSubP.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubQ = reasoner.getSubObjectProperties(getPropQ(), false);
        Assertions.assertNotNull(nsSubQ);
        Assertions.assertEquals(1, nsSubQ.nodes().count());
        Assertions.assertTrue(nsSubQ.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubPMinus = reasoner
                .getSubObjectProperties(getPropP().getInverseProperty(), false);
        Assertions.assertNotNull(nsSubPMinus);
        Assertions.assertEquals(1, nsSubPMinus.nodes().count());
        Assertions.assertTrue(nsSubPMinus.containsEntity(df.getOWLBottomObjectProperty()));
        NodeSet<OWLObjectPropertyExpression> nsSubQMinus = reasoner
                .getSubObjectProperties(getPropQ().getInverseProperty(), false);
        Assertions.assertNotNull(nsSubQMinus);
        Assertions.assertEquals(1, nsSubQMinus.nodes().count());
        Assertions.assertTrue(nsSubQMinus.containsEntity(df.getOWLBottomObjectProperty()));
    }
}
