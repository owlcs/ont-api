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

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.BaseOntologyModel;
import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OWLAdapter;
import com.github.owlcs.ontapi.OntGraphDocumentSource;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.testutils.MiscTestUtils;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.ontapi.testutils.OntIRI;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.UnionGraph;
import org.apache.jena.ontapi.model.OntID;
import org.apache.jena.ontapi.model.OntIndividual;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.utils.Graphs;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.parameters.Imports;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To test behaviour with {@code owl:imports} within {@link OntologyManager}.
 * <p>
 * Created by @ssz on 08.10.2016.
 */
public class ImportsOntModelTest extends OntModelTestBase {

    private static void testGraphModelCycleImports(OntologyManager m) {
        OntModel a = m.createGraphModel("a");
        OntModel b = m.createGraphModel("b");
        OntModel c = m.createGraphModel("c");

        a.addImport(b);
        Assertions.assertEquals(1, ((UnionGraph) (a.getGraph())).subGraphs().count());
        b.addImport(a);
        Assertions.assertEquals(1, ((UnionGraph) (a.getGraph())).subGraphs().count());
        b.addImport(c);
        Assertions.assertEquals(2, ((UnionGraph) (b.getGraph())).subGraphs().count());

        Assertions.assertEquals(1, a.imports().count());
        Assertions.assertEquals(2, b.imports().count());
        Assertions.assertEquals(0, c.imports().count());

        a.createOntClass("A");
        Assertions.assertEquals(1, a.ontEntities().count());
        Assertions.assertEquals(1, b.ontEntities().count());
        Assertions.assertEquals(0, c.ontEntities().count());

        b.createOntClass("B");
        Assertions.assertEquals(2, a.ontEntities().count());
        Assertions.assertEquals(2, b.ontEntities().count());
        Assertions.assertEquals(0, c.ontEntities().count());

        c.createOntClass("C");
        Assertions.assertEquals(3, a.ontEntities().count());
        Assertions.assertEquals(3, b.ontEntities().count());
        Assertions.assertEquals(1, c.ontEntities().count());

        assertOntologyAxioms(m, a.getID().getURI(), 3);
        assertOntologyAxioms(m, b.getID().getURI(), 3);
        assertOntologyAxioms(m, c.getID().getURI(), 1);
    }

    private static void assertOntologyAxioms(OntologyManager m, String ontURI, int expectedAxiomsCount) {
        Ontology o = m.getOntology(IRI.create(ontURI));
        Assertions.assertNotNull(o);
        Assertions.assertEquals(expectedAxiomsCount, o.axioms(Imports.INCLUDED).count());
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

        Ontology a = m.loadOntology(a_iri);
        Assertions.assertEquals(2, m.ontologies().count());
        Ontology b = m.getOntology(b_iri);
        Assertions.assertNotNull(b);

        a.add(df.getOWLDeclarationAxiom(df.getOWLClass("http://X")));
        Assertions.assertEquals(3, a.axioms(Imports.INCLUDED).count());
        Assertions.assertEquals(3, b.axioms(Imports.INCLUDED).count());
    }

    private static OWLOntologyDocumentSource createSource(IRI ont, IRI imports) {
        return OntGraphDocumentSource.of(createGraph(ont.getIRIString(),
                imports.getIRIString(), ont.getIRIString() + "#C"));
    }

    private static Graph createGraph(String ontologyURI, String importURI, String classURI) {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        m.setID(ontologyURI).addImport(importURI);
        m.createOntClass(classURI);
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
        Assertions.assertEquals(expected, ont_a.axioms(Imports.INCLUDED).collect(Collectors.toSet()));
        Assertions.assertEquals(expected, ont_b.axioms(Imports.INCLUDED).collect(Collectors.toSet()));
    }

    private static void oneMoreImportsTest(OntologyManager m) {
        String a_uri = "http://a";
        String b_uri = "http://b";
        String c_uri = "http://c";
        OWLDataFactory df = m.getOWLDataFactory();
        Ontology a = m.createOntology(IRI.create(a_uri));
        Ontology b = m.createOntology(IRI.create(b_uri));
        Ontology c = m.createOntology(IRI.create(c_uri));
        c.asGraphModel().createOntClass(c_uri + "#c-1");

        a.add(df.getOWLDeclarationAxiom(df.getOWLClass(IRI.create(a_uri + "#a-1"))));
        Assertions.assertEquals(1, a.axioms().count());

        a.asGraphModel().createOntClass(a_uri + "#a-2");
        Assertions.assertEquals(2, a.axioms().count());

        a.asGraphModel().addImport(b.asGraphModel());
        b.add(df.getOWLDeclarationAxiom(df.getOWLClass(IRI.create(b_uri + "#b-1"))));
        b.asGraphModel().createOntClass(b_uri + "#b-2");
        Assertions.assertEquals(4, a.axioms(Imports.INCLUDED).count());
        Assertions.assertEquals(4, a.asGraphModel().classes().count());

        a.asGraphModel().imports().findFirst().orElseThrow(AssertionError::new)
                .createOntClass(b_uri + "#b-3");
        a.imports().findFirst().orElseThrow(AssertionError::new)
                .add(df.getOWLDeclarationAxiom(df.getOWLClass(IRI.create(b_uri + "#b-4"))));
        Assertions.assertEquals(6, a.axioms(Imports.INCLUDED).count());
        Assertions.assertEquals(6, a.asGraphModel().classes().count());

        OntModelFactory.createModel(a.asGraphModel().getGraph()).createOntClass(a_uri + "#a-3");
        OntModelFactory.createModel(a.asGraphModel().imports().findFirst()
                .orElseThrow(AssertionError::new).getGraph()).createOntClass(b_uri + "#b-5");
        Assertions.assertEquals(8, a.axioms(Imports.INCLUDED).count());
        Assertions.assertEquals(8, a.asGraphModel().classes().count());

        Optional.ofNullable(m.getOntology(IRI.create("http://b")))
                .orElseThrow(AssertionError::new).asGraphModel().createOntClass(b_uri + "#b-6");
        Optional.ofNullable(m.getOntology(IRI.create("http://b")))
                .orElseThrow(AssertionError::new)
                .add(df.getOWLDeclarationAxiom(df.getOWLClass(IRI.create(b_uri + "#b-7"))));
        Assertions.assertEquals(10, a.axioms(Imports.INCLUDED).count());
        Assertions.assertEquals(10, a.asGraphModel().classes().count());

        OntModelFactory.createModel(a.asGraphModel().getGraph()).addImport(c.asGraphModel());
        Assertions.assertEquals(11, a.axioms(Imports.INCLUDED).count());
        Assertions.assertEquals(11, a.asGraphModel().classes().count());
        OntModelFactory.createModel(a.asGraphModel().getGraph()).removeImport(c.asGraphModel());
        Assertions.assertEquals(10, a.axioms(Imports.INCLUDED).count());
        Assertions.assertEquals(10, a.asGraphModel().classes().count());

        OntModelFactory.createModel(a.asGraphModel().getGraph()).addImport(m.getGraphModel("http://c"));
        Assertions.assertEquals(11, a.axioms(Imports.INCLUDED).count());
        Assertions.assertEquals(11, a.asGraphModel().classes().count());
        OntModelFactory.createModel(a.asGraphModel().getGraph()).removeImport("http://c");
        Assertions.assertEquals(10, a.axioms(Imports.INCLUDED).count());
        Assertions.assertEquals(10, a.asGraphModel().classes().count());
    }

    private static void assertDeclarationInModels(OntModel mustHave,
                                                  OntModel mustNotHave,
                                                  Resource subject,
                                                  Resource type) {
        assertHasDeclaration(mustHave, subject, type);
        assertHasNoDeclaration(mustNotHave, subject, type);
    }

    private static void assertHasDeclaration(OntModel model, Resource subject, Resource object) {
        Triple t = createDeclaration(subject, object);
        Assertions.assertTrue(model.getBaseGraph().contains(t), "Can't find the triple " + t);
    }

    private static void assertHasNoDeclaration(OntModel model, Resource subject, Resource object) {
        Triple t = createDeclaration(subject, object);
        Assertions.assertFalse(model.getBaseGraph().contains(t), "There is the triple " + t);
    }

    private static Triple createDeclaration(Resource r, RDFNode o) {
        return Triple.create(r.asNode(), RDF.type.asNode(), o.asNode());
    }

    private static void testGraphModelImports(OntologyManager m) {
        OntModel a = m.createGraphModel("a");
        OntModel b = m.createGraphModel("b");
        OntModel c = m.createGraphModel("c");

        a.addImport(b);
        Assertions.assertEquals(1, a.imports().count());
        Assertions.assertFalse(a.hasImport(c));
        Assertions.assertTrue(a.hasImport(b.getID().getURI()));
        Assertions.assertTrue(a.hasImport(b));
        b = a.imports().findFirst().orElseThrow(AssertionError::new);
        Assertions.assertTrue(a.hasImport(b));

        c.addImport(b);
        Assertions.assertEquals(1, c.imports().count());
        Assertions.assertTrue(c.hasImport(b.getID().getURI()));
        Assertions.assertTrue(c.hasImport(b));

        a.removeImport(b);
        Assertions.assertEquals(0, a.imports().count());
        Assertions.assertFalse(a.hasImport(b.getID().getURI()));
        Assertions.assertFalse(a.hasImport(b));

        a.addImport(m.getGraphModel("b"));
        Assertions.assertEquals(1, a.imports().count());
        Assertions.assertTrue(a.hasImport(b.getID().getURI()));
        Assertions.assertTrue(a.hasImport(b));

        c.removeImport("b");
        Assertions.assertEquals(0, c.imports().count());
        Assertions.assertFalse(c.hasImport(b.getID().getURI()));
        Assertions.assertFalse(c.hasImport(b));

        OntModel x = m.createGraphModel("x");
        OntModel y = m.createGraphModel("y");
        OntModel z = m.createGraphModel("z");

        m.getGraphModel("x").addImport(y).addImport(z);
        Assertions.assertEquals(2, x.imports().count());
        Assertions.assertEquals(2, m.getGraphModel("x").imports().count());
    }

    private static void baseModelImportsTest(OntologyManager m) {
        OWLAdapter ad = OWLAdapter.get();
        IRI iri_a = IRI.create("A");
        IRI iri_b = IRI.create("B");

        OWLDataFactory d = m.getOWLDataFactory();
        Ontology a = m.createOntology(iri_a);
        Ontology b = m.createOntology(iri_b);

        m.applyChange(new AddImport(a, d.getOWLImportsDeclaration(iri_b)));
        Assertions.assertTrue(a.isEmpty());
        Assertions.assertTrue(b.isEmpty());
        Assertions.assertEquals(1, a.imports().count());
        Assertions.assertEquals(0, b.imports().count());
        Assertions.assertEquals(1, ((UnionGraph) (a.asGraphModel().getGraph())).subGraphs().count());
        Assertions.assertEquals(0, ((UnionGraph) (b.asGraphModel().getGraph())).subGraphs().count());
        Assertions.assertEquals(1, ((UnionGraph) (ad.asBaseModel(a).getBaseGraphModel().getGraph())).subGraphs().count());
        Assertions.assertEquals(Stream.of(a, b)
                        .map(ad::asBaseModel)
                        .map(BaseOntologyModel::getBaseGraphModel)
                        .map(OntModel::getBaseGraph).collect(Collectors.toSet()),
                Graphs.dataGraphs(ad.asBaseModel(a).getBaseGraphModel().getGraph()).collect(Collectors.toSet()));

        m.applyChange(new RemoveImport(a, d.getOWLImportsDeclaration(iri_b)));
        Assertions.assertEquals(0, a.imports().count());
        Assertions.assertEquals(0, b.imports().count());
        Assertions.assertEquals(0, ((UnionGraph) (a.asGraphModel().getGraph())).subGraphs().count());
        Assertions.assertEquals(0, ((UnionGraph) (b.asGraphModel().getGraph())).subGraphs().count());
        Assertions.assertEquals(0, ((UnionGraph) (ad.asBaseModel(a).getBaseGraphModel().getGraph())).subGraphs().count());

    }

    @Test
    public void testGraphModelImports() {
        testGraphModelImports(OntManagers.createManager());
    }

    @Test
    public void testConcurrentGraphModelImports() {
        testGraphModelImports(OntManagers.createConcurrentManager());
    }

    @Test
    public void testGraphModelCycleImports() {
        testGraphModelCycleImports(OntManagers.createManager());
    }

    @Test
    public void testGraphModelCycleImportsWithConcurrent() {
        testGraphModelCycleImports(OntManagers.createConcurrentManager());
    }

    @Test
    public void testMutualImportsWhileLoad() throws Exception {
        testMutualImportsWhileLoading(OntManagers.createManager());
    }

    @Test
    public void testMutualImportsWhileLoadWithConcurrentManager() throws Exception {
        testMutualImportsWhileLoading(OntManagers.createConcurrentManager());
    }

    @Test
    public void testMutualImportsWhileCreate() throws Exception {
        testMutualImportsWhileCreation(OntManagers.createManager());
    }

    @Test
    public void testMutualImportsWhileCreateWithConcurrent() throws Exception {
        testMutualImportsWhileCreation(OntManagers.createConcurrentManager());
    }

    @Test
    public void testImportByVersionIRI() {
        IRI bIRI = IRI.create("http://b");
        IRI aIRI = IRI.create("http://a");
        IRI ver1 = IRI.create("http://ver/1.0");
        IRI ver2 = IRI.create("http://ver/2.0");

        OntologyManager m = OntManagers.createManager();
        OWLDataFactory df = m.getOWLDataFactory();
        Ontology a = m.createOntology(new OWLOntologyID(aIRI, ver1));
        Ontology b = m.createOntology(bIRI);

        // add owl:imports for 'a' inside 'b':
        m.applyChange(new AddImport(b, df.getOWLImportsDeclaration(ver1)));

        // check OWL-API declaration:
        List<OWLImportsDeclaration> dec = b.importsDeclarations().toList();
        Assertions.assertEquals(1, dec.size());
        Assertions.assertEquals(ver1, dec.get(0).getIRI());
        // check graph references and graph imports:
        OntModel g = b.asGraphModel();
        Assertions.assertEquals(1, g.imports().count());
        Assertions.assertEquals(ver1, g.getID().imports().findFirst().map(IRI::create).orElseThrow(AssertionError::new));

        Assertions.assertSame(a, m.getImportedOntology(ver1));
        // should found ontology by its iri:
        Assertions.assertSame(a, m.getImportedOntology(aIRI));
        Assertions.assertSame(b, m.getImportedOntology(bIRI));

        // what if in manager there is one more ontology with the same iri but different version iri ?
        Ontology c = m.createOntology(new OWLOntologyID(aIRI, ver2));
        Assertions.assertSame(a, m.getImportedOntology(ver1));
        Assertions.assertSame(c, m.getImportedOntology(ver2));
        Assertions.assertSame(b, m.getImportedOntology(bIRI));
        // should not be found by its iri, since it is not primary for series:
        Assertions.assertNull(m.getImportedOntology(aIRI));
    }

    @Test
    public void testMixedAddImports() {
        OntIRI iri = OntIRI.create("http://test.test/add-import/1");
        Ontology owl = MiscTestUtils.createModel(iri);
        OntologyManager manager = owl.getOWLOntologyManager();
        OWLDataFactory factory = manager.getOWLDataFactory();
        OntModel jena = owl.asGraphModel();
        int importsCount = 4;
        OntID jenaOnt = jena.setID(iri.getIRIString());
        Assertions.assertNotNull(jenaOnt);
        LOGGER.debug("Add imports.");
        OntIRI import1 = OntIRI.create("http://dummy-imports.com/first");
        OntIRI import2 = OntIRI.create("http://dummy-imports.com/second");
        OntIRI import3 = OntIRI.create(OWLIOUtils.getResourceURI("/ontapi/foaf.rdf"));
        OntIRI import4 = OntIRI.create(OWLIOUtils.getResourceURI("/ontapi/pizza.ttl"));
        manager.applyChange(new AddImport(owl, factory.getOWLImportsDeclaration(import1)));
        jena.getID().addImport(import2.getIRIString());
        manager.applyChange(new AddImport(owl, factory.getOWLImportsDeclaration(import3)));
        jena.getID().addImport(import4.getIRIString());

        debug(owl);

        Assertions.assertEquals(0, owl.imports().count());
        Assertions.assertEquals(importsCount, owl.importsDeclarations().count());
        Assertions.assertEquals(importsCount,
                jena.listStatements(iri.toResource(), OWL.imports, (RDFNode) null).toList().size());

        LOGGER.debug("Remove imports.");
        jena.getID().removeImport(import4.getIRIString());
        manager.applyChange(new RemoveImport(owl, factory.getOWLImportsDeclaration(import1)));
        debug(owl);
        importsCount = 2;
        Assertions.assertEquals(importsCount,
                owl.importsDeclarations().count());
        Assertions.assertEquals(importsCount,
                jena.getID().imports().count());

        debug(owl);
    }

    @Test
    public void testAddImportsWithControl() {
        OntIRI baseIRI = OntIRI.create("http://test.test/add-import/base");
        OntologyManager manager = OntManagers.createConcurrentManager();
        manager.getOntologyConfigurator().setControlImports(true);

        OWLDataFactory factory = manager.getOWLDataFactory();

        Ontology base = manager.createOntology(baseIRI);

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
        Ontology child = manager.createOntology(childIRI);
        child.applyChanges(new AddImport(child, factory.getOWLImportsDeclaration(baseIRI)));

        Assertions.assertEquals(1, child.imports().count());

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
        OntologyManager newManager = OntManagers.createManager();
        Ontology newBase = OWLIOUtils.convertJenaToONT(newManager, base.asGraphModel());
        Ontology newChild = OWLIOUtils.convertJenaToONT(newManager, child.asGraphModel());

        Assertions.assertEquals(1, newChild.imports().count());
        Assertions.assertEquals(child.asGraphModel().listStatements().toList().size(),
                newChild.asGraphModel().listStatements().toList().size());
        MiscTestUtils.compareAxioms(base.axioms(), newBase.axioms());

        LOGGER.debug("Check axioms after reload:");
        LOGGER.debug("Origin ont");
        child.axioms().map(String::valueOf).forEach(LOGGER::debug);
        LOGGER.debug("Reloaded ont");
        newChild.axioms().map(String::valueOf).forEach(LOGGER::debug);
        MiscTestUtils.compareAxioms(child.axioms(), newChild.axioms());

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
        OntologyManager m = OntManagers.createManager();
        String a_uri = "A";
        String b_uri = "B";
        Ontology a_owl = m.createOntology(IRI.create(a_uri));
        Ontology b_owl = m.createOntology(IRI.create(b_uri));

        OntModel a = m.getGraphModel(a_uri);
        Assertions.assertNotNull(a);
        OntModel b = m.getGraphModel(b_uri);
        Assertions.assertNotNull(b);
        a.addImport(b);

        Assertions.assertTrue(a_owl.imports().anyMatch(o -> Objects.equals(o, b_owl)));

        LOGGER.debug("Add class and associated individual");
        OntIndividual i = b.createOntClass("class").createIndividual("individual");
        b_owl.axioms().forEach(x -> LOGGER.debug("{}", x));

        Set<OWLAxiom> b_axioms_1 = b_owl.axioms().collect(Collectors.toSet());
        Set<OWLAxiom> a_axioms_1 = a_owl.axioms(Imports.INCLUDED).collect(Collectors.toSet());
        Assertions.assertEquals(3, b_axioms_1.size());
        Assertions.assertEquals(0, a_owl.getAxiomCount());
        Assertions.assertEquals(b_axioms_1, a_axioms_1);

        LOGGER.debug("Remove individual (class assertion + declaration)");
        b.removeOntObject(i);
        b_owl.axioms().forEach(x -> LOGGER.debug("{}", x));

        Set<OWLAxiom> b_axioms_2 = b_owl.axioms().collect(Collectors.toSet());
        Set<OWLAxiom> a_axioms_2 = a_owl.axioms(Imports.INCLUDED).collect(Collectors.toSet());
        Assertions.assertEquals(1, b_axioms_2.size());
        Assertions.assertEquals(0, a_owl.getAxiomCount());
        Assertions.assertEquals(b_axioms_2, a_axioms_2);
    }

    @Test
    public void testConcurrentImportsBehaviour() {
        OntologyManager m = OntManagers.createConcurrentManager();
        Ontology a = m.createOntology(IRI.create("a"));
        Ontology b = m.createOntology(IRI.create("b"));
        a.asGraphModel().addImport(b.asGraphModel());
        Assertions.assertEquals(1, a.imports().count());
        Assertions.assertEquals(0, b.imports().count());
        Assertions.assertEquals(1, a.asGraphModel().imports().count());
        Assertions.assertEquals(0, b.asGraphModel().imports().count());
        a.asGraphModel().createOntClass("A-C");
        b.asGraphModel().createOntClass("B-C");
        Assertions.assertEquals(2, a.signature(Imports.INCLUDED).count());
        Assertions.assertEquals(2, a.asGraphModel().classes().count());

        Assertions.assertEquals(0, a.asGraphModel().removeImport("b").imports().count());
        Assertions.assertEquals(0, a.imports().count());
        Assertions.assertEquals(0, a.asGraphModel().imports().count());
        Assertions.assertEquals(1, a.signature(Imports.INCLUDED).count());
        Assertions.assertEquals(1, a.asGraphModel().classes().count());
    }

    @Test
    public void testCommonDifferentImportsStrategies() {
        oneMoreImportsTest(OntManagers.createManager());
    }

    @Test
    public void testConcurrentDifferentImportsStrategies() {
        oneMoreImportsTest(OntManagers.createConcurrentManager());
    }

    @Test
    public void testBaseModelImportsCommon() {
        baseModelImportsTest(OntManagers.createManager());
    }

    @Test
    public void testBaseModelImportsConcurrent() {
        baseModelImportsTest(OntManagers.createConcurrentManager());
    }
}
