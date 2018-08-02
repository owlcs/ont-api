/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.tests.model;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntID;
import ru.avicomp.ontapi.jena.model.OntIndividual;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.TestUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * to test behaviour with owl:imports
 * <p>
 * Created by @szuev on 08.10.2016.
 */
public class ImportsOntModelTest extends OntModelTestBase {

    @Test
    public void testMixedAddImports() {
        OntIRI iri = OntIRI.create("http://test.test/add-import/1");
        OntologyModel owl = TestUtils.createModel(iri);
        OntologyManager manager = owl.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        OntGraphModel jena = owl.asGraphModel();
        int importsCount = 4;
        OntID jenaOnt = jena.setID(iri.getIRIString());
        Assert.assertNotNull(jenaOnt);
        LOGGER.debug("Add imports.");
        OntIRI import1 = OntIRI.create("http://dummy-imports.com/first");
        OntIRI import2 = OntIRI.create("http://dummy-imports.com/second");
        OntIRI import3 = OntIRI.create(ReadWriteUtils.getResourceURI("ontapi/foaf.rdf"));
        OntIRI import4 = OntIRI.create(ReadWriteUtils.getResourceURI("ontapi/pizza.ttl"));
        manager.applyChange(new AddImport(owl, factory.getOWLImportsDeclaration(import1)));
        jena.getID().addImport(import2.getIRIString());
        manager.applyChange(new AddImport(owl, factory.getOWLImportsDeclaration(import3)));
        jena.getID().addImport(import4.getIRIString());

        debug(owl);

        Assert.assertEquals("OWL: incorrect imported ontology count.", 0, owl.imports().count());
        Assert.assertEquals("OWL: incorrect imports count.", importsCount, owl.importsDeclarations().count());
        Assert.assertEquals("Jena: incorrect imports count.", importsCount, jena.listStatements(iri.toResource(), OWL.imports, (RDFNode) null).toList().size());

        LOGGER.debug("Remove imports.");
        jena.getID().removeImport(import4.getIRIString());
        manager.applyChange(new RemoveImport(owl, factory.getOWLImportsDeclaration(import1)));
        debug(owl);
        importsCount = 2;
        Assert.assertEquals("OWL: incorrect imports count after removing.", importsCount, owl.importsDeclarations().count());
        Assert.assertEquals("Jena: incorrect imports count after removing.", importsCount, jena.getID().imports().count());

        debug(owl);
    }

    @Test
    public void testOWLAddImports() {
        OntIRI baseIRI = OntIRI.create("http://test.test/add-import/base");
        OntologyManager manager = OntManagers.createConcurrentONT();
        OWLDataFactory factory = manager.getOWLDataFactory();

        OntologyModel base = manager.createOntology(baseIRI);

        OntIRI classIRI1 = baseIRI.addFragment("Class-1");
        OntIRI classIRI2 = baseIRI.addFragment("Class-2");
        OntIRI objPropIRI = baseIRI.addFragment("obj-prop-1");
        OntIRI dataPropIRI = baseIRI.addFragment("data-prop-1");
        OntIRI annPropIRI = baseIRI.addFragment("ann-prop-1");
        OntIRI dataTypeIRI = baseIRI.addFragment("data-type-1");

        OWLClass class1 = factory.getOWLClass(classIRI1);
        OWLClass class2 = factory.getOWLClass(classIRI2);
        OWLObjectProperty objProperty = factory.getOWLObjectProperty(objPropIRI);
        OWLDataProperty dataProperty = factory.getOWLDataProperty(dataPropIRI);
        OWLAnnotationProperty annProperty = factory.getOWLAnnotationProperty(annPropIRI);
        OWLDatatype dataType = factory.getOWLDatatype(dataTypeIRI);

        List<OWLAxiom> baseAxioms = new ArrayList<>();
        baseAxioms.add(factory.getOWLDeclarationAxiom(objProperty));
        baseAxioms.add(factory.getOWLDeclarationAxiom(dataProperty));
        baseAxioms.add(factory.getOWLDeclarationAxiom(annProperty));
        baseAxioms.add(factory.getOWLDeclarationAxiom(class1));
        baseAxioms.add(factory.getOWLDeclarationAxiom(class2));
        baseAxioms.add(factory.getOWLDeclarationAxiom(dataType));

        LOGGER.debug("Apply axioms to the base ontology {}", baseIRI);
        baseAxioms.forEach(axiom -> base.applyChanges(new AddAxiom(base, axiom)));

        debug(base);

        LOGGER.debug("Add import {}", baseIRI);
        OntIRI childIRI = OntIRI.create("http://test.test/add-import/child");
        OntologyModel child = manager.createOntology(childIRI);
        child.applyChanges(new AddImport(child, factory.getOWLImportsDeclaration(baseIRI)));

        Assert.assertEquals("Incorrect imports count", 1, child.imports().count());

        OWLDatatypeRestriction dataRange1 = factory.getOWLDatatypeMinMaxInclusiveRestriction(1, 2.3);

        OWLNamedIndividual individual1 = factory.getOWLNamedIndividual(childIRI.addFragment("Individual-1"));
        OWLNamedIndividual individual2 = factory.getOWLNamedIndividual(childIRI.addFragment("Individual-2"));
        OWLClassExpression ce1 = factory.getOWLObjectUnionOf(class1, class2);
        List<OWLAxiom> axioms = new ArrayList<>();
        axioms.add(factory.getOWLDeclarationAxiom(individual1));
        axioms.add(factory.getOWLDeclarationAxiom(individual2));
        axioms.add(factory.getOWLInverseFunctionalObjectPropertyAxiom(objProperty));
        axioms.add(factory.getOWLDataPropertyRangeAxiom(dataProperty, dataRange1));
        axioms.add(factory.getOWLClassAssertionAxiom(class1, individual1));
        axioms.add(factory.getOWLClassAssertionAxiom(ce1, individual2));
        axioms.add(factory.getOWLAnnotationPropertyDomainAxiom(annProperty, class1.getIRI()));

        LOGGER.debug("Apply axioms to the subsidiary ontology {}", child);
        axioms.forEach(axiom -> child.applyChanges(new AddAxiom(child, axiom)));

        debug(child);

        LOGGER.debug("Check triplets presence.");
        assertDeclarationInModels(base.asGraphModel(), child.asGraphModel(), classIRI1.toResource(), OWL.Class);
        assertDeclarationInModels(base.asGraphModel(), child.asGraphModel(), classIRI2.toResource(), OWL.Class);
        assertDeclarationInModels(base.asGraphModel(), child.asGraphModel(), objPropIRI.toResource(), OWL.ObjectProperty);
        assertDeclarationInModels(base.asGraphModel(), child.asGraphModel(), dataPropIRI.toResource(), OWL.DatatypeProperty);
        assertDeclarationInModels(base.asGraphModel(), child.asGraphModel(), annPropIRI.toResource(), OWL.AnnotationProperty);
        assertDeclarationInModels(base.asGraphModel(), child.asGraphModel(), dataTypeIRI.toResource(), RDFS.Datatype);

        LOGGER.debug("Reload models.");
        OntologyManager newManager = OntManagers.createONT();
        OntologyModel newBase = ReadWriteUtils.convertJenaToONT(newManager, base.asGraphModel());
        OntologyModel newChild = ReadWriteUtils.convertJenaToONT(newManager, child.asGraphModel());

        Assert.assertEquals("Incorrect imports count", 1, newChild.imports().count());
        Assert.assertEquals("Should be the same number of statements",
                child.asGraphModel().listStatements().toList().size(),
                newChild.asGraphModel().listStatements().toList().size());
        TestUtils.compareAxioms(base.axioms(), newBase.axioms());

        LOGGER.debug("Check axioms after reload:");
        LOGGER.debug("Origin ont");
        child.axioms().map(String::valueOf).forEach(LOGGER::debug);
        LOGGER.debug("Reloaded ont");
        newChild.axioms().map(String::valueOf).forEach(LOGGER::debug);
        TestUtils.compareAxioms(child.axioms(), newChild.axioms());

        LOGGER.debug("Remove import test");
        child.applyChanges(new RemoveImport(child, factory.getOWLImportsDeclaration(baseIRI)));
        debug(child);
        assertHasDeclaration(child.asGraphModel(), classIRI1.toResource(), OWL.Class);
        assertHasDeclaration(child.asGraphModel(), classIRI2.toResource(), OWL.Class);
        assertHasDeclaration(child.asGraphModel(), objPropIRI.toResource(), OWL.ObjectProperty);
        assertHasDeclaration(child.asGraphModel(), dataPropIRI.toResource(), OWL.DatatypeProperty);
        assertHasDeclaration(child.asGraphModel(), annPropIRI.toResource(), OWL.AnnotationProperty);
        assertHasNoDeclaration(child.asGraphModel(), dataTypeIRI.toResource(), RDFS.Datatype);
    }

    @Test
    public void testCommonImportsBehaviour() {
        OntologyManager m = OntManagers.createONT();
        String a_uri = "A";
        String b_uri = "B";
        OntologyModel a_owl = m.createOntology(IRI.create(a_uri));
        OntologyModel b_owl = m.createOntology(IRI.create(b_uri));

        OntGraphModel a = m.getGraphModel(a_uri);
        Assert.assertNotNull(a);
        OntGraphModel b = m.getGraphModel(b_uri);
        Assert.assertNotNull(b);
        a.addImport(b);

        Assert.assertTrue(a_owl.imports().anyMatch(o -> Objects.equals(o, b_owl)));

        LOGGER.debug("Add class and associated individual");
        OntIndividual i = b.createOntEntity(OntClass.class, "class").createIndividual("individual");
        b_owl.axioms().forEach(x -> LOGGER.debug("{}", x));

        Set<OWLAxiom> b_axioms_1 = b_owl.axioms().collect(Collectors.toSet());
        Set<OWLAxiom> a_axioms_1 = a_owl.axioms(Imports.INCLUDED).collect(Collectors.toSet());
        Assert.assertEquals(3, b_axioms_1.size());
        Assert.assertEquals(0, a_owl.getAxiomCount());
        Assert.assertEquals(b_axioms_1, a_axioms_1);

        LOGGER.debug("Remove individual (class assertion + declaration)");
        b.removeOntObject(i);
        b_owl.axioms().forEach(x -> LOGGER.debug("{}", x));

        Set<OWLAxiom> b_axioms_2 = b_owl.axioms().collect(Collectors.toSet());
        Set<OWLAxiom> a_axioms_2 = a_owl.axioms(Imports.INCLUDED).collect(Collectors.toSet());
        Assert.assertEquals("Wrong axioms list: " + b_axioms_2, 1, b_axioms_2.size());
        Assert.assertEquals("Expected no axioms in parent.", 0, a_owl.getAxiomCount());
        Assert.assertEquals(b_axioms_2, a_axioms_2);
    }

    @Test
    public void testConcurrentImportsBehaviour() {
        OntologyManager m = OntManagers.createConcurrentONT();
        OntologyModel a = m.createOntology(IRI.create("a"));
        OntologyModel b = m.createOntology(IRI.create("b"));
        a.asGraphModel().addImport(b.asGraphModel());
        Assert.assertEquals(1, a.imports().count());
        Assert.assertEquals(0, b.imports().count());
        Assert.assertEquals(1, a.asGraphModel().imports().count());
        Assert.assertEquals(0, b.asGraphModel().imports().count());
        a.asGraphModel().createOntEntity(OntClass.class, "A-C");
        b.asGraphModel().createOntEntity(OntClass.class, "B-C");
        Assert.assertEquals(2, a.signature(Imports.INCLUDED).count());
        Assert.assertEquals(2, a.asGraphModel().listClasses().count());

        Assert.assertEquals(0, a.asGraphModel().removeImport(b.asGraphModel()).imports().count());
        Assert.assertEquals(0, a.imports().count());
        Assert.assertEquals(0, a.asGraphModel().imports().count());
        Assert.assertEquals(1, a.signature(Imports.INCLUDED).count());
        Assert.assertEquals(1, a.asGraphModel().listClasses().count());
    }

    @Test
    public void testCommonDifferentImportsStrategies() {
        oneMoreImportsTest(OntManagers.createONT());
    }

    @Test
    public void testConcurrentDifferentImportsStrategies() {
        oneMoreImportsTest(OntManagers.createConcurrentONT());
    }

    private void oneMoreImportsTest(OntologyManager m) {
        String a_uri = "http://a";
        String b_uri = "http://b";
        String c_uri = "http://c";
        OWLDataFactory df = m.getOWLDataFactory();
        OntologyModel a = m.createOntology(IRI.create(a_uri));
        OntologyModel b = m.createOntology(IRI.create(b_uri));
        OntologyModel c = m.createOntology(IRI.create(c_uri));
        c.asGraphModel().createOntEntity(OntClass.class, c_uri + "#c-1");

        a.add(df.getOWLDeclarationAxiom(df.getOWLClass(IRI.create(a_uri + "#a-1"))));
        Assert.assertEquals(1, a.axioms().count());

        a.asGraphModel().createOntEntity(OntClass.class, a_uri + "#a-2");
        Assert.assertEquals(2, a.axioms().count());

        a.asGraphModel().addImport(b.asGraphModel());
        b.add(df.getOWLDeclarationAxiom(df.getOWLClass(IRI.create(b_uri + "#b-1"))));
        b.asGraphModel().createOntEntity(OntClass.class, b_uri + "#b-2");
        Assert.assertEquals(4, a.axioms(Imports.INCLUDED).count());
        Assert.assertEquals(4, a.asGraphModel().listClasses().count());

        a.asGraphModel().imports().findFirst().orElseThrow(AssertionError::new).createOntEntity(OntClass.class, b_uri + "#b-3");
        a.imports().findFirst().orElseThrow(AssertionError::new).add(df.getOWLDeclarationAxiom(df.getOWLClass(IRI.create(b_uri + "#b-4"))));
        Assert.assertEquals(6, a.axioms(Imports.INCLUDED).count());
        Assert.assertEquals(6, a.asGraphModel().listClasses().count());

        OntModelFactory.createModel(a.asGraphModel().getGraph()).createOntEntity(OntClass.class, a_uri + "#a-3");
        OntModelFactory.createModel(a.asGraphModel().imports().findFirst()
                .orElseThrow(AssertionError::new).getGraph()).createOntEntity(OntClass.class, b_uri + "#b-5");
        Assert.assertEquals(8, a.axioms(Imports.INCLUDED).count());
        Assert.assertEquals(8, a.asGraphModel().listClasses().count());

        Optional.ofNullable(m.getOntology(IRI.create("http://b")))
                .orElseThrow(AssertionError::new).asGraphModel().createOntEntity(OntClass.class, b_uri + "#b-6");
        Optional.ofNullable(m.getOntology(IRI.create("http://b")))
                .orElseThrow(AssertionError::new).add(df.getOWLDeclarationAxiom(df.getOWLClass(IRI.create(b_uri + "#b-7"))));
        Assert.assertEquals(10, a.axioms(Imports.INCLUDED).count());
        Assert.assertEquals(10, a.asGraphModel().listClasses().count());

        OntModelFactory.createModel(a.asGraphModel().getGraph()).addImport(c.asGraphModel());
        Assert.assertEquals(11, a.axioms(Imports.INCLUDED).count());
        Assert.assertEquals(11, a.asGraphModel().listClasses().count());
        OntModelFactory.createModel(a.asGraphModel().getGraph()).removeImport(c.asGraphModel());
        Assert.assertEquals(10, a.axioms(Imports.INCLUDED).count());
        Assert.assertEquals(10, a.asGraphModel().listClasses().count());

        OntModelFactory.createModel(a.asGraphModel().getGraph()).addImport(m.getGraphModel("http://c"));
        Assert.assertEquals(11, a.axioms(Imports.INCLUDED).count());
        Assert.assertEquals(11, a.asGraphModel().listClasses().count());
        OntModelFactory.createModel(a.asGraphModel().getGraph()).removeImport(m.getGraphModel("http://c"));
        Assert.assertEquals(10, a.axioms(Imports.INCLUDED).count());
        Assert.assertEquals(10, a.asGraphModel().listClasses().count());
    }

    private static void assertDeclarationInModels(OntGraphModel base, OntGraphModel child, Resource subject, Resource type) {
        assertHasDeclaration(base, subject, type);
        assertHasNoDeclaration(child, subject, type);
    }

    private static void assertHasDeclaration(OntGraphModel model, Resource subject, Resource object) {
        Triple t = createDeclaration(subject, object);
        Assert.assertTrue("Can't find the triple " + t, model.getBaseGraph().contains(t));
    }

    private static void assertHasNoDeclaration(OntGraphModel model, Resource subject, Resource object) {
        Triple t = createDeclaration(subject, object);
        Assert.assertFalse("There is the triple " + t, model.getBaseGraph().contains(t));
    }

    private static Triple createDeclaration(Resource r, RDFNode o) {
        return Triple.create(r.asNode(), RDF.type.asNode(), o.asNode());
    }
}
