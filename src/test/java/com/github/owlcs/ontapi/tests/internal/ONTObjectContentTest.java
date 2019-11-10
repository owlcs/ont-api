/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.internal;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.OntologyModel;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Created by @szz on 12.09.2019.
 */
public class ONTObjectContentTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ONTObjectContentTest.class);

    @SuppressWarnings("unchecked")
    @Test
    public void testAnnotatedSubClassOf() {
        OntologyModel o = OntManagers.createONT().createOntology();
        OntGraphModel g = o.asGraphModel();
        g.createOntClass("X").addSubClassOfStatement(g.createOntClass("Y")).annotate(g.getRDFSComment(), "XY");
        ReadWriteUtils.print(g);

        Assert.assertEquals(3, o.getAxiomCount());
        OWLSubClassOfAxiom owl = o.axioms(AxiomType.SUBCLASS_OF).findFirst().orElseThrow(AssertionError::new);
        Assert.assertTrue(owl.isAnnotated());

        ONTObject<OWLSubClassOfAxiom> ont = (ONTObject<OWLSubClassOfAxiom>) owl;
        Model res = ModelFactory.createModelForGraph(ont.toGraph()).setNsPrefixes(OntModelFactory.STANDARD);
        Assert.assertEquals(8, res.size());

        o.remove(ont.getOWLObject());
        Assert.assertEquals(3, g.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAnnotatedDeclaration() {
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setLoadAnnotationAxioms(false);
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();
        g.createOntClass("X").addComment("X");
        ReadWriteUtils.print(g);

        Assert.assertEquals(1, o.getAxiomCount());
        OWLDeclarationAxiom owl = o.axioms(AxiomType.DECLARATION).findFirst().orElseThrow(AssertionError::new);
        Assert.assertTrue(owl.isAnnotated());

        ONTObject<OWLDeclarationAxiom> ont = (ONTObject<OWLDeclarationAxiom>) owl;
        Model res = ModelFactory.createModelForGraph(ont.toGraph()).setNsPrefixes(OntModelFactory.STANDARD);
        Assert.assertEquals(2, res.size());

        o.remove(ont.getOWLObject());
        Assert.assertEquals(1, g.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMergedHeaderAnnotations() {
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setLoadAnnotationAxioms(false);
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();
        // two identical annotations, but one is assertion, and the second one is bulk
        g.getID().addComment("x");
        g.asStatement(g.getID().getRoot().asTriple()).annotate(g.getRDFSComment(), "x");
        ReadWriteUtils.print(g);

        // in OWL-view must be one (merged) annotation:
        List<OWLAnnotation> owl = o.annotationsAsList();
        Assert.assertEquals(1, owl.size());

        ONTObject<OWLAnnotation> ont = (ONTObject<OWLAnnotation>) owl.get(0);
        Model res = ModelFactory.createModelForGraph(ont.toGraph()).setNsPrefixes(OntModelFactory.STANDARD);
        ReadWriteUtils.print(res);

        m.applyChange(new RemoveOntologyAnnotation(o, ont.getOWLObject()));
        Assert.assertEquals(1, g.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMergeEquivalentClasses() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntClass x = g.createOntClass("X");
        OntClass y = g.createOntClass("Y");
        OntClass z = g.createOntClass("Z");
        x.addEquivalentClass(y.addEquivalentClass(x)).addEquivalentClass(z);
        ReadWriteUtils.print(g);

        Assert.assertEquals(5, o.axioms().count());

        DataFactory df = m.getOWLDataFactory();
        OWLEquivalentClassesAxiom xz = o.axioms(AxiomType.EQUIVALENT_CLASSES)
                .filter(a -> a.contains(df.getOWLClass(z.getURI()))).findFirst().orElseThrow(AssertionError::new);
        OWLEquivalentClassesAxiom xy = o.axioms(AxiomType.EQUIVALENT_CLASSES)
                .filter(a -> a.contains(df.getOWLClass(y.getURI()))).findFirst().orElseThrow(AssertionError::new);
        Assert.assertTrue(xy.containsEntityInSignature(df.getOWLClass(x.getURI())));

        ONTObject<OWLEquivalentClassesAxiom> xzOnt = (ONTObject<OWLEquivalentClassesAxiom>) xz;
        ONTObject<OWLEquivalentClassesAxiom> xyOnt = (ONTObject<OWLEquivalentClassesAxiom>) xy;

        Assert.assertEquals(3, xzOnt.triples().count());

        // can't test carefully, since no method to get value (merged axiom), only keys are available:
        Assert.assertEquals(3, xyOnt.triples().count());
        // but can delete axiom with all its triples
        o.remove(xyOnt.getOWLObject());

        ReadWriteUtils.print(g);
        Assert.assertEquals(4, o.axioms().count());
        Assert.assertEquals(1, o.axioms(AxiomType.EQUIVALENT_CLASSES).count());
        // header + "<X> owl:equivalentClass <Z>" + 3 declarations
        Assert.assertEquals(5, g.size());
        Assert.assertEquals(1, g.statements(null, OWL.equivalentClass, null).count());
    }

    @Test
    public void testMergeSubClassOf() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntClass x = g.createOntClass("X");
        OntClass y = g.createOntClass("Y");
        x.addSuperClass(g.createComplementOf(y));
        x.addSuperClass(g.createComplementOf(y));
        ReadWriteUtils.print(g);

        Assert.assertEquals(3, o.axioms().count());
        OWLSubClassOfAxiom a = o.axioms(AxiomType.SUBCLASS_OF).findFirst().orElseThrow(AssertionError::new);

        o.remove(a);

        o.clearCache();
        Assert.assertEquals(2, o.axioms().count());
        Assert.assertEquals(3, g.size());
    }

    @Test
    public void testMergeInverseObjectProperties() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntOPE x = g.createObjectProperty("X");
        OntOPE y = g.createObjectProperty("Y");
        x.addInverseProperty(y.addInverseProperty(x));

        // header + 2 declarations + 2 owl:inverseOf
        Assert.assertEquals(5, g.size());

        Assert.assertEquals(3, o.axioms().count());

        OWLInverseObjectPropertiesAxiom a = o.axioms(AxiomType.INVERSE_OBJECT_PROPERTIES)
                .findFirst().orElseThrow(AssertionError::new);
        o.remove(a);

        Assert.assertEquals(2, o.axioms().count());
        // header + 2 declarations
        Assert.assertEquals(3, g.size());
    }

    @Test
    public void testMergeSubObjectPropertyOf() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntNOP x = g.createObjectProperty("X");
        OntNOP y = g.createObjectProperty("Y");
        createInverse(x).addSuperProperty(createInverse(y));
        createInverse(x).addSuperProperty(createInverse(y));
        ReadWriteUtils.print(g);

        Assert.assertEquals(3, o.axioms().count());
        OWLSubObjectPropertyOfAxiom a = o.axioms(AxiomType.SUB_OBJECT_PROPERTY).findFirst().orElseThrow(AssertionError::new);

        o.remove(a);

        o.clearCache();
        Assert.assertEquals(2, o.axioms().count());
        Assert.assertEquals(3, g.size());
    }

    @Test
    public void testMergeNegativeObjectPropertyAssertion() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntOPE op = g.createObjectProperty("OP");
        OntNAP ap = g.createAnnotationProperty("AP");
        OntIndividual i1 = g.createIndividual("I1");
        OntIndividual i2 = g.createIndividual("I2");

        // 10 + 10 triples
        op.addNegativeAssertion(i1, i2).addAnnotation(ap, "comm1", "x").addAnnotation(g.getRDFSComment(), "comm2");
        op.addNegativeAssertion(i1, i2).addAnnotation(ap, "comm1", "x").addAnnotation(g.getRDFSComment(), "comm2");
        // 4 triples
        op.addNegativeAssertion(i1, i2);
        ReadWriteUtils.print(g);
        Assert.assertEquals(29, g.size());

        Assert.assertEquals(2, o.axioms(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION).count());
        OWLAxiom a1 = o.axioms(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION).filter(x -> !x.isAnnotated())
                .findFirst().orElseThrow(AssertionError::new);
        OWLAxiom a2 = o.axioms(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION).filter(OWLAxiom::isAnnotated)
                .findFirst().orElseThrow(AssertionError::new);
        // 4 from OntNPA + 3 declarations
        Assert.assertEquals(7, ((ONTObject) a1).triples().count());

        o.remove(a2);
        Assert.assertEquals(9, g.size());

        o.remove(a1);
        Assert.assertEquals(5, g.size());
    }

    @Test
    public void testMergeDisjointClasses() {
        simpleTestMergeDisjointAxioms(OntGraphModel::createOntClass,
                (x, y) -> x.addDisjointClass(y.addDisjointClass(x)).getModel().createDisjointClasses(x, y),
                AxiomType.DISJOINT_CLASSES);
    }

    @Test
    public void testMergePropertyChains() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntNOP x = g.createObjectProperty("P");
        OntNOP y = g.createObjectProperty("Y");
        OntNOP z = g.createObjectProperty("Z");

        createInverse(x).addPropertyChain(y, z).addPropertyChain(y, z);
        createInverse(x).addPropertyChain(y, z);
        ReadWriteUtils.print(g);
        Assert.assertEquals(21, g.size());

        Assert.assertEquals(4, o.axioms().count());
        OWLSubPropertyChainOfAxiom a = o.axioms(AxiomType.SUB_PROPERTY_CHAIN_OF)
                .findFirst().orElseThrow(AssertionError::new);

        o.remove(a);

        ReadWriteUtils.print(g);
        o.clearCache();
        Assert.assertEquals(3, o.axioms().count());
        Assert.assertEquals(4, g.size());
    }

    @Test
    public void testMergeHasKeys() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntClass c = g.createOntClass("C");
        OntNOP x = g.createObjectProperty("P");
        OntNOP y = g.createObjectProperty("Y");
        OntNDP z = g.createDataProperty("Z");

        c.addHasKey(x, y, z).addHasKey(x, z, y, x);
        ReadWriteUtils.print(g);
        Assert.assertEquals(19, g.size());

        Assert.assertEquals(5, o.axioms().count());
        OWLHasKeyAxiom a = o.axioms(AxiomType.HAS_KEY)
                .findFirst().orElseThrow(AssertionError::new);

        o.remove(a);

        ReadWriteUtils.print(g);
        o.clearCache();
        Assert.assertEquals(4, o.axioms().count());
        Assert.assertEquals(5, g.size());
    }

    @Test
    public void testMergeDisjointUnion() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntClass c1 = g.createOntClass("C1");
        OntClass c2 = g.createOntClass("C2");
        OntClass c3 = g.createOntClass("C3");

        c1.addDisjointUnion(g.createComplementOf(c2), c3).addDisjointUnion(c3, g.createComplementOf(c2));
        ReadWriteUtils.print(g);
        Assert.assertEquals(18, g.size());

        Assert.assertEquals(4, o.axioms().count());
        OWLDisjointUnionAxiom a = o.axioms(AxiomType.DISJOINT_UNION)
                .findFirst().orElseThrow(AssertionError::new);

        o.remove(a);

        ReadWriteUtils.print(g);
        o.clearCache();
        Assert.assertEquals(3, o.axioms().count());
        Assert.assertEquals(4, g.size());
    }

    @Test
    public void testMergeDifferentIndividuals() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntIndividual x = g.getOWLThing().createIndividual("X");
        OntIndividual y = g.getOWLThing().createIndividual("Y");
        x.addDifferentIndividual(y.addDifferentIndividual(x));
        g.createDifferentIndividuals(x, y);
        g.createDifferentIndividuals(y, x);
        ReadWriteUtils.print(g);

        Assert.assertEquals(19, g.size());
        Assert.assertEquals(5, o.axioms().count());
        OWLDifferentIndividualsAxiom a = o.axioms(AxiomType.DIFFERENT_INDIVIDUALS)
                .findFirst().orElseThrow(AssertionError::new);

        o.remove(a);

        o.clearCache();
        Assert.assertEquals(4, o.axioms().count());
        Assert.assertEquals(5, g.size());
    }

    @Test
    public void testMergeSameIndividuals() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntIndividual x = g.getOWLThing().createIndividual("X");
        OntIndividual y = g.getOWLThing().createIndividual("Y");
        x.addSameIndividual(y.addSameIndividual(x));
        ReadWriteUtils.print(g);

        Assert.assertEquals(7, g.size());
        Assert.assertEquals(5, o.axioms().peek(a -> LOGGER.debug("A: {}", a)).count());
        OWLSameIndividualAxiom a = o.axioms(AxiomType.SAME_INDIVIDUAL)
                .findFirst().orElseThrow(AssertionError::new);

        o.remove(a);
        ReadWriteUtils.print(g);

        o.clearCache();
        Assert.assertEquals(4, o.axioms().count());
        Assert.assertEquals(5, g.size());
    }

    @Test
    public void testMergeDisjointObjectProperties() {
        simpleTestMergeDisjointAxioms(OntGraphModel::createObjectProperty,
                (x, y) -> x.addDisjointProperty(y.addDisjointProperty(x)).getModel().createDisjointObjectProperties(x, y),
                AxiomType.DISJOINT_OBJECT_PROPERTIES);
    }

    @Test
    public void testMergeEquivalentObjectProperties() {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        OntNOP x = g.createObjectProperty("X");
        OntNOP y = g.createObjectProperty("Y");
        x.addEquivalentPropertyStatement(y).addAnnotation(g.getRDFSComment(), "x");
        y.addEquivalentPropertyStatement(x).addAnnotation(g.getRDFSComment(), "x");
        ReadWriteUtils.print(g);

        Assert.assertEquals(15, g.size());
        Assert.assertEquals(3, o.axioms().peek(a -> LOGGER.debug("A: {}", a)).count());
        OWLEquivalentObjectPropertiesAxiom a = o.axioms(AxiomType.EQUIVALENT_OBJECT_PROPERTIES)
                .findFirst().orElseThrow(AssertionError::new);

        o.remove(a);
        ReadWriteUtils.print(g);

        o.clearCache();
        Assert.assertEquals(2, o.axioms().count());
        Assert.assertEquals(3, g.size());
    }

    @Test
    public void testMergeDisjointDataProperties() {
        simpleTestMergeDisjointAxioms(OntGraphModel::createDataProperty,
                (x, y) -> x.addDisjointProperty(y.addDisjointProperty(x)).getModel().createDisjointDataProperties(x, y),
                AxiomType.DISJOINT_DATA_PROPERTIES);
    }

    private <X extends OntObject, Y extends OWLNaryAxiom> void simpleTestMergeDisjointAxioms(BiFunction<OntGraphModel, String, X> addDeclaration,
                                                                                             BiConsumer<X, X> addDisjoints,
                                                                                             AxiomType<Y> type) {
        OntologyManager m = OntManagers.createONT();
        OntologyModel o = m.createOntology();
        OntGraphModel g = o.asGraphModel();

        X x = addDeclaration.apply(g, "X");
        X y = addDeclaration.apply(g, "Y");
        addDisjoints.accept(x, y);
        ReadWriteUtils.print(g);

        Assert.assertEquals(3, o.axioms().count());
        Y a = o.axioms(type).findFirst().orElseThrow(AssertionError::new);

        o.remove(a);
        Assert.assertEquals(2, o.axioms().count());
        Assert.assertEquals(3, g.size());
    }

    private static OntOPE createInverse(OntNOP p) {
        return p.getModel().createResource().addProperty(OWL.inverseOf, p).as(OntOPE.class);
    }
}
