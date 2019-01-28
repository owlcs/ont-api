/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
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

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import ru.avicomp.ontapi.*;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.impl.UnionModel;
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
import java.util.stream.Stream;

/**
 * To test behaviour with {@code owl:imports} within {@link OntologyManager}.
 * <p>
 * Created by @szuev on 08.10.2016.
 */
public class ImportsOntModelTest extends OntModelTestBase {

    private static void testGraphModelCycleImports(OntologyManager m) {
        OntGraphModel a = m.createGraphModel("a");
        OntGraphModel b = m.createGraphModel("b");
        OntGraphModel c = m.createGraphModel("c");

        a.addImport(b);
        Assert.assertEquals(2, UnionModel.asUnionGraph(a.getGraph()).listBaseGraphs().toList().size());
        b.addImport(a);
        Assert.assertEquals(2, UnionModel.asUnionGraph(a.getGraph()).listBaseGraphs().toList().size());
        b.addImport(c);
        Assert.assertEquals(3, UnionModel.asUnionGraph(a.getGraph()).listBaseGraphs().toList().size());

        Assert.assertEquals(1, a.imports().count());
        Assert.assertEquals(2, b.imports().count());
        Assert.assertEquals(0, c.imports().count());

        a.createOntEntity(OntClass.class, "A");
        Assert.assertEquals(1, a.ontEntities().count());
        Assert.assertEquals(1, b.ontEntities().count());
        Assert.assertEquals(0, c.ontEntities().count());

        b.createOntEntity(OntClass.class, "B");
        Assert.assertEquals(2, a.ontEntities().count());
        Assert.assertEquals(2, b.ontEntities().count());
        Assert.assertEquals(0, c.ontEntities().count());

        c.createOntEntity(OntClass.class, "C");
        Assert.assertEquals(3, a.ontEntities().peek(x -> LOGGER.debug("Entity: {}", x)).count());
        Assert.assertEquals(3, b.ontEntities().count());
        Assert.assertEquals(1, c.ontEntities().count());

        assertOntologyAxioms(m, a.getID().getURI(), 3);
        assertOntologyAxioms(m, b.getID().getURI(), 3);
        assertOntologyAxioms(m, c.getID().getURI(), 1);
    }

    private static void assertOntologyAxioms(OntologyManager m, String ontURI, int expectedAxiomsCount) {
        OntologyModel o = m.getOntology(IRI.create(ontURI));
        Assert.assertNotNull(o);
        Assert.assertEquals(expectedAxiomsCount, o.axioms(Imports.INCLUDED).count());
    }

    private static void testMutualImportsWhileLoading(OntologyManager m) throws Exception {
        IRI a_iri = IRI.create("http://a");
        IRI b_iri = IRI.create("http://b");
        OntologyManager.DocumentSourceMapping source = id -> {
            if (id.matchOntology(a_iri)) {
                return createSource(a_iri, b_iri);
            }
            if (id.matchOntology(b_iri)) {
                return createSource(b_iri, a_iri);
            }
            return null;
        };

        DataFactory df = m.getOWLDataFactory();
        m.getDocumentSourceMappers().add(source);

        OntologyModel a = m.loadOntology(a_iri);
        Assert.assertEquals(2, m.ontologies().count());
        OntologyModel b = m.getOntology(b_iri);
        Assert.assertNotNull(b);

        a.add(df.getOWLDeclarationAxiom(df.getOWLClass("http://X")));
        Assert.assertEquals(3, a.axioms(Imports.INCLUDED).peek(x -> LOGGER.debug("{}:::Axiom: {}", a_iri, x)).count());
        Assert.assertEquals(3, b.axioms(Imports.INCLUDED).peek(x -> LOGGER.debug("{}:::Axiom: {}", b_iri, x)).count());
    }

    private static OWLOntologyDocumentSource createSource(IRI ont, IRI imports) {
        return OntGraphDocumentSource.wrap(createGraph(ont.getIRIString(),
                imports.getIRIString(), ont.getIRIString() + "#C"));
    }

    private static Graph createGraph(String ontologyURI, String importURI, String classURI) {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        m.setID(ontologyURI).addImport(importURI);
        m.createOntEntity(OntClass.class, classURI);
        return m.getBaseGraph();
    }

    private static void testMutualImportsWhileCreation(OWLOntologyManager m) throws Exception {
        OWLDataFactory df = m.getOWLDataFactory();
        IRI a = IRI.create("http://a");
        IRI b = IRI.create("http://b");
        OWLOntology ont_a = m.createOntology(a);
        OWLOntology ont_b = m.createOntology(b);

        m.applyChange(new AddImport(ont_b, df.getOWLImportsDeclaration(a)));
        m.applyChange(new AddImport(ont_a, df.getOWLImportsDeclaration(b)));

        OWLAxiom ax_a = df.getOWLDeclarationAxiom(df.getOWLClass("A"));
        OWLAxiom ax_b = df.getOWLDeclarationAxiom(df.getOWLClass("B"));
        ont_a.add(ax_a);
        ont_b.add(ax_b);

        ont_a.axioms(Imports.INCLUDED).forEach(x -> LOGGER.debug("{}", x));
        Set<OWLAxiom> expected = Stream.of(ax_a, ax_b).collect(Collectors.toSet());
        Assert.assertEquals(expected, ont_a.axioms(Imports.INCLUDED).collect(Collectors.toSet()));
        Assert.assertEquals(expected, ont_b.axioms(Imports.INCLUDED).collect(Collectors.toSet()));
    }

    private static void oneMoreImportsTest(OntologyManager m) {
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

        a.asGraphModel().imports().findFirst().orElseThrow(AssertionError::new)
                .createOntEntity(OntClass.class, b_uri + "#b-3");
        a.imports().findFirst().orElseThrow(AssertionError::new)
                .add(df.getOWLDeclarationAxiom(df.getOWLClass(IRI.create(b_uri + "#b-4"))));
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
                .orElseThrow(AssertionError::new)
                .add(df.getOWLDeclarationAxiom(df.getOWLClass(IRI.create(b_uri + "#b-7"))));
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
        OntModelFactory.createModel(a.asGraphModel().getGraph()).removeImport("http://c");
        Assert.assertEquals(10, a.axioms(Imports.INCLUDED).count());
        Assert.assertEquals(10, a.asGraphModel().listClasses().count());
    }

    private static void assertDeclarationInModels(OntGraphModel mustHave,
                                                  OntGraphModel mustNotHave,
                                                  Resource subject,
                                                  Resource type) {
        assertHasDeclaration(mustHave, subject, type);
        assertHasNoDeclaration(mustNotHave, subject, type);
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

    private static void testGraphModelImports(OntologyManager m) {
        OntGraphModel a = m.createGraphModel("a");
        OntGraphModel b = m.createGraphModel("b");
        OntGraphModel c = m.createGraphModel("c");

        a.addImport(b);
        Assert.assertEquals(1, a.imports().count());
        Assert.assertFalse(a.hasImport(c));
        Assert.assertTrue(a.hasImport(b.getID().getURI()));
        Assert.assertTrue(a.hasImport(b));
        b = a.imports().findFirst().orElseThrow(AssertionError::new);
        Assert.assertTrue(a.hasImport(b));

        c.addImport(b);
        Assert.assertEquals(1, c.imports().count());
        Assert.assertTrue(c.hasImport(b.getID().getURI()));
        Assert.assertTrue(c.hasImport(b));

        a.removeImport(b);
        Assert.assertEquals(0, a.imports().count());
        Assert.assertFalse(a.hasImport(b.getID().getURI()));
        Assert.assertFalse(a.hasImport(b));

        a.addImport(m.getGraphModel("b"));
        Assert.assertEquals(1, a.imports().count());
        Assert.assertTrue(a.hasImport(b.getID().getURI()));
        Assert.assertTrue(a.hasImport(b));

        c.removeImport("b");
        Assert.assertEquals(0, c.imports().count());
        Assert.assertFalse(c.hasImport(b.getID().getURI()));
        Assert.assertFalse(c.hasImport(b));

        OntGraphModel x = m.createGraphModel("x");
        OntGraphModel y = m.createGraphModel("y");
        OntGraphModel z = m.createGraphModel("z");

        m.getGraphModel("x").addImport(y).addImport(z);
        Assert.assertEquals(2, x.imports().count());
        Assert.assertEquals(2, m.getGraphModel("x").imports().count());
    }

    @Test
    public void testGraphModelImports() {
        testGraphModelImports(OntManagers.createONT());
    }

    @Test
    public void testConcurrentGraphModelImports() {
        testGraphModelImports(OntManagers.createConcurrentONT());
    }

    @Test
    public void testGraphModelCycleImports() {
        testGraphModelCycleImports(OntManagers.createONT());
    }

    @Test
    public void testGraphModelCycleImportsWithConcurrent() {
        testGraphModelCycleImports(OntManagers.createConcurrentONT());
    }

    @Test
    public void testMutualImportsWhileLoad() throws Exception {
        testMutualImportsWhileLoading(OntManagers.createONT());
    }

    @Test
    public void testMutualImportsWhileLoadWithConcurrentManager() throws Exception {
        testMutualImportsWhileLoading(OntManagers.createConcurrentONT());
    }

    @Test
    public void testMutualImportsWhileCreate() throws Exception {
        testMutualImportsWhileCreation(OntManagers.createONT());
    }

    @Test
    public void testMutualImportsWhileCreateWithConcurrent() throws Exception {
        testMutualImportsWhileCreation(OntManagers.createConcurrentONT());
    }

    @Test
    public void testImportByVersionIRI() {
        IRI bIRI = IRI.create("http://b");
        IRI aIRI = IRI.create("http://a");
        IRI ver1 = IRI.create("http://ver/1.0");
        IRI ver2 = IRI.create("http://ver/2.0");

        OntologyManager m = OntManagers.createONT();
        OWLDataFactory df = m.getOWLDataFactory();
        OntologyModel a = m.createOntology(new OWLOntologyID(aIRI, ver1));
        OntologyModel b = m.createOntology(bIRI);

        // add owl:imports for 'a' inside 'b':
        m.applyChange(new AddImport(b, df.getOWLImportsDeclaration(ver1)));

        // check OWL-API declaration:
        List<OWLImportsDeclaration> dec = b.importsDeclarations().collect(Collectors.toList());
        Assert.assertEquals(1, dec.size());
        Assert.assertEquals(ver1, dec.get(0).getIRI());
        // check graph references and graph imports:
        OntGraphModel g = b.asGraphModel();
        Assert.assertEquals(1, g.imports().count());
        Assert.assertEquals(ver1, g.getID().imports().findFirst().map(IRI::create).orElseThrow(AssertionError::new));

        Assert.assertSame(a, m.getImportedOntology(ver1));
        // should found ontology by its iri:
        Assert.assertSame(a, m.getImportedOntology(aIRI));
        Assert.assertSame(b, m.getImportedOntology(bIRI));

        // what if in manager there is one more ontology with the same iri but different version iri ?
        OntologyModel c = m.createOntology(new OWLOntologyID(aIRI, ver2));
        Assert.assertSame(a, m.getImportedOntology(ver1));
        Assert.assertSame(c, m.getImportedOntology(ver2));
        Assert.assertSame(b, m.getImportedOntology(bIRI));
        // should not be found by its iri, since it is not primary for series:
        Assert.assertNull(m.getImportedOntology(aIRI));
    }

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
        Assert.assertEquals("Jena: incorrect imports count.", importsCount,
                jena.listStatements(iri.toResource(), OWL.imports, (RDFNode) null).toList().size());

        LOGGER.debug("Remove imports.");
        jena.getID().removeImport(import4.getIRIString());
        manager.applyChange(new RemoveImport(owl, factory.getOWLImportsDeclaration(import1)));
        debug(owl);
        importsCount = 2;
        Assert.assertEquals("OWL: incorrect imports count after removing.", importsCount,
                owl.importsDeclarations().count());
        Assert.assertEquals("Jena: incorrect imports count after removing.", importsCount,
                jena.getID().imports().count());

        debug(owl);
    }

    @Test
    public void testAddImportsWithControl() {
        OntIRI baseIRI = OntIRI.create("http://test.test/add-import/base");
        OntologyManager manager = OntManagers.createConcurrentONT();
        manager.getOntologyConfigurator().setControlImports(true);

        OWLDataFactory factory = manager.getOWLDataFactory();

        OntologyModel base = manager.createOntology(baseIRI);

        OntIRI classIRI1 = baseIRI.addFragment("Class-1");
        OntIRI classIRI2 = baseIRI.addFragment("Class-2");
        OntIRI opIRI = baseIRI.addFragment("obj-prop-1");
        OntIRI dpIRI = baseIRI.addFragment("data-prop-1");
        OntIRI apIRI = baseIRI.addFragment("ann-prop-1");
        OntIRI dtIRI = baseIRI.addFragment("data-type-1");

        OWLClass class1 = factory.getOWLClass(classIRI1);
        OWLClass class2 = factory.getOWLClass(classIRI2);
        OWLObjectProperty objProperty = factory.getOWLObjectProperty(opIRI);
        OWLDataProperty dataProperty = factory.getOWLDataProperty(dpIRI);
        OWLAnnotationProperty annProperty = factory.getOWLAnnotationProperty(apIRI);
        OWLDatatype dataType = factory.getOWLDatatype(dtIRI);

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
        assertDeclarationInModels(base.asGraphModel(), child.asGraphModel(), opIRI.toResource(), OWL.ObjectProperty);
        assertDeclarationInModels(base.asGraphModel(), child.asGraphModel(), dpIRI.toResource(), OWL.DatatypeProperty);
        assertDeclarationInModels(base.asGraphModel(), child.asGraphModel(), apIRI.toResource(), OWL.AnnotationProperty);
        assertDeclarationInModels(base.asGraphModel(), child.asGraphModel(), dtIRI.toResource(), RDFS.Datatype);

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
        assertHasDeclaration(child.asGraphModel(), opIRI.toResource(), OWL.ObjectProperty);
        assertHasDeclaration(child.asGraphModel(), dpIRI.toResource(), OWL.DatatypeProperty);
        assertHasDeclaration(child.asGraphModel(), apIRI.toResource(), OWL.AnnotationProperty);
        assertHasNoDeclaration(child.asGraphModel(), dtIRI.toResource(), RDFS.Datatype);
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

        Assert.assertEquals(0, a.asGraphModel().removeImport("b").imports().count());
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
}
