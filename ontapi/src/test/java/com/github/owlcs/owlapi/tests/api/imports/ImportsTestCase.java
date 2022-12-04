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
package com.github.owlcs.owlapi.tests.api.imports;

import com.github.owlcs.TempDirectory;
import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.Profiles;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
@ExtendWith(TempDirectory.class)
public class ImportsTestCase extends TestBase {

    @Test
    public void testImportsClosureUpdate() throws OWLOntologyCreationException {
        IRI aIRI = IRI.create("http://a.com", "");
        OWLOntology ontA = getOWLOntology(aIRI);
        IRI bIRI = IRI.create("http://b.com", "");
        OWLOntology ontB = getOWLOntology(bIRI);
        ontA.applyChange(new AddImport(ontA, df.getOWLImportsDeclaration(bIRI)));
        Assertions.assertEquals(2, m.importsClosure(ontA).count());
        m.removeOntology(ontB);
        Assertions.assertEquals(1, m.importsClosure(ontA).count());
        getOWLOntology(bIRI);
        Assertions.assertEquals(2, m.importsClosure(ontA).count());
    }

    @Test
    public void testShouldLoad() throws Exception {
        IRI importsBothNameAndVersion = IRI.create(TempDirectory.createFile("tempImportsNameAndVersion.", ".owl").toFile());
        IRI importsBothNameAndOther = IRI.create(TempDirectory.createFile("tempImportsNameAndOther.", ".owl").toFile());
        IRI ontologyByName = IRI.create(TempDirectory.createFile("tempMain.", ".owl").toFile());
        IRI ontologyByVersion = IRI.create(TempDirectory.createFile("tempVersion.", ".owl").toFile());
        IRI ontologyByOtherPath = IRI.create(TempDirectory.createFile("tempOther.", ".owl").toFile());
        OWLOntology ontology = getOWLOntology(new OWLOntologyID(ontologyByName, ontologyByVersion));
        ontology.saveOntology(ontologyByName);
        ontology.saveOntology(ontologyByVersion);
        ontology.saveOntology(ontologyByOtherPath);
        OWLOntology ontology1 = m1.createOntology(importsBothNameAndVersion);
        OWLOntology ontology2 = m1.createOntology(importsBothNameAndOther);
        List<AddImport> changes = new ArrayList<>();
        changes.add(new AddImport(ontology1, df.getOWLImportsDeclaration(ontologyByName)));
        changes.add(new AddImport(ontology1, df.getOWLImportsDeclaration(ontologyByVersion)));
        changes.add(new AddImport(ontology2, df.getOWLImportsDeclaration(ontologyByName)));
        changes.add(new AddImport(ontology2, df.getOWLImportsDeclaration(ontologyByOtherPath)));
        ontology1.applyChanges(changes);
        ontology2.applyChanges(changes);
        ontology1.saveOntology(importsBothNameAndVersion);
        ontology2.saveOntology(importsBothNameAndOther);
        // when
        OWLOntology o1 = m.loadOntology(importsBothNameAndVersion);
        OWLOntology o2 = m1.loadOntology(importsBothNameAndOther);
        // then
        Assertions.assertNotNull(o1);
        Assertions.assertNotNull(o2);
    }

    @Test
    public void testShouldNotLoadWrong() throws OWLOntologyCreationException {
        m.createOntology(IRI.create("urn:test#", "test"));
        StringDocumentSource documentSource = new StringDocumentSource("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" " +
                "xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" " +
                "xmlns:owl =\"http://www.w3.org/2002/07/owl#\">\n"
                + "    <owl:Ontology><owl:imports rdf:resource=\"urn:test#test\"/></owl:Ontology></rdf:RDF>");
        OWLOntology o = m.loadOntologyFromOntologyDocument(documentSource);
        Assertions.assertTrue(o.isAnonymous(), o.getOntologyID().toString());
        Assertions.assertFalse(o.getOntologyID().getDefaultDocumentIRI().isPresent());
    }

    @Test
    public void testManualImports() throws OWLOntologyCreationException {
        OWLOntology baseOnt = getOWLOntology(IRI.create("http://semanticweb.org/ontologies/", "base"));
        IRI importedIRI = IRI.create("http://semanticweb.org/ontologies/", "imported");
        OWLOntology importedOnt = getOWLOntology(importedIRI);
        Set<OWLOntology> preImportsClosureCache = OWLAPIStreamUtils.asUnorderedSet(baseOnt.importsClosure());
        Assertions.assertTrue(preImportsClosureCache.contains(baseOnt));
        Assertions.assertFalse(preImportsClosureCache.contains(importedOnt));
        baseOnt.applyChange(new AddImport(baseOnt, df.getOWLImportsDeclaration(importedIRI)));
        Set<OWLOntology> postImportsClosureCache = OWLAPIStreamUtils.asUnorderedSet(baseOnt.importsClosure());
        Assertions.assertTrue(postImportsClosureCache.contains(baseOnt));
        Assertions.assertTrue(postImportsClosureCache.contains(importedOnt));
    }

    @Test
    public void testShouldThrowExceptionWithDefaultImportsConfig() {
        Assertions.assertThrows(UnloadableImportException.class, this::shouldThrowExceptionWithDefaultImportsConfig);
    }

    private void shouldThrowExceptionWithDefaultImportsConfig() throws OWLOntologyCreationException {
        String input = "<?xml version=\"1.0\"?>\n" + "<rdf:RDF xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
                + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
                + "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n"
                + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "    <owl:Ontology rdf:about=\"http://www.semanticweb.org/fake/ontologies/2012/8/1\">\n"
                + "        <owl:imports rdf:resource=\"http://localhost:1\"/>\n" + "    </owl:Ontology>\n" + "</rdf:RDF>";
        loadOntologyFromString(input);
    }

    @Test
    public void testImports() throws OWLOntologyCreationException {
        m.getIRIMappers().add(new AutoIRIMapper(new File(RESOURCES, "imports"), true));
        m.loadOntologyFromOntologyDocument(new File(RESOURCES, "/imports/D.owl"));
    }

    @Test
    public void testCyclicImports() throws OWLOntologyCreationException {
        m.getIRIMappers().add(new AutoIRIMapper(new File(RESOURCES, "importscyclic"), true));
        m.loadOntologyFromOntologyDocument(new File(RESOURCES, "/importscyclic/D.owl"));
    }

    @Test
    public void testCyclicImports2() throws OWLOntologyCreationException {
        m.getIRIMappers().add(new AutoIRIMapper(new File(RESOURCES, "importscyclic"), true));
        m.loadOntologyFromOntologyDocument(IRI.create(new File(RESOURCES, "importscyclic/D.owl")));
    }

    @Test
    public void testTurtleGraphImport() throws OWLOntologyCreationException {
        File ontologyDirectory = new File(RESOURCES, "importNoOntology");
        String ns = "http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/";
        IRI bobsOntologyName = IRI.create(ns, "subject-bob");
        OWLNamedIndividual bobsIndividual = df.getOWLNamedIndividual(ns + "subject-bob#", "subjectOnImmunosuppressantA2");
        m.getIRIMappers().add(new SimpleIRIMapper(IRI.create(ns, "subject-amy"), IRI.create(new File(ontologyDirectory, "subject-amy.ttl"))));
        m.getIRIMappers().add(new SimpleIRIMapper(bobsOntologyName, IRI.create(new File(ontologyDirectory, "subject-bob.ttl"))));
        m.getIRIMappers().add(new SimpleIRIMapper(IRI.create(ns, "subject-sue"), IRI.create(new File(ontologyDirectory, "subject-sue.ttl"))));
        m.getIRIMappers().add(new SimpleIRIMapper(IRI.create("http://www.w3.org/2013/12/FDA-TA/", "core"), IRI.create(new File(ontologyDirectory, "core.ttl"))));
        OWLOntology top = m.loadOntologyFromOntologyDocument(new File(ontologyDirectory, "subjects.ttl"));
        OWLIOUtils.print(top);
        OWLOntology bob = m.getOntology(bobsOntologyName);
        Assertions.assertNotNull(bob, "Can't find Bob.");
        OWLIOUtils.print(bob);
        Assertions.assertEquals(4, m.ontologies().count(), "Unexpected ontologies count.");
        Assertions.assertTrue(top.containsEntityInSignature(bobsIndividual, Imports.INCLUDED), "Individuals about Bob are missing...");
    }

    /**
     * Tests to see if the method which obtains the imports closure behaves correctly.
     */
    @Test
    public void testImportsClosure() {
        OWLOntology ontA = getOWLOntology();
        OWLOntology ontB = getOWLOntology();
        Assertions.assertTrue(OWLAPIStreamUtils.contains(ontA.importsClosure(), ontA));
        IRI iriB = ontB.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new);
        OWLImportsDeclaration importsDeclaration = df.getOWLImportsDeclaration(iriB);
        ontA.applyChange(new AddImport(ontA, importsDeclaration));
        Assertions.assertTrue(OWLAPIStreamUtils.contains(ontA.importsClosure(), ontB));
        ontA.applyChange(new RemoveImport(ontA, importsDeclaration));
        Assertions.assertFalse(OWLAPIStreamUtils.contains(ontA.importsClosure(), ontB));
        ontA.applyChange(new AddImport(ontA, importsDeclaration));
        Assertions.assertTrue(OWLAPIStreamUtils.contains(ontA.importsClosure(), ontB));
        ontB.getOWLOntologyManager().removeOntology(ontB);
        Assertions.assertFalse(OWLAPIStreamUtils.contains(ontA.importsClosure(), ontB));
    }

    @Test
    public void tetsShouldRemapImport() throws OWLOntologyCreationException {
        String input = "<?xml version=\"1.0\"?>\n"
                + "<Ontology  ontologyIRI=\"http://protege.org/ontologies/TestFunnyPizzaImport.owl\">\n"
                + "    <Import>http://test.org/TestPizzaImport.owl</Import>\n" + "</Ontology>";
        // the explicit type for ONT-API, otherwise this(^^^) data would be considered as RDF/XML,
        // and the corresponding(valid) graph will have the strange URI's
        StringDocumentSource source = new StringDocumentSource(input, IRI.getNextDocumentIRI("string:ontology"),
                OntFormat.OWL_XML.createOwlFormat(), OntFormat.OWL_XML.getID());
        IRI testImport = IRI.create("http://test.org/", "TestPizzaImport.owl");
        IRI remap = IRI.create("urn:test:", "mockImport");
        OWLOntologyIRIMapper mock = Mockito.mock(OWLOntologyIRIMapper.class);
        Mockito.when(mock.getDocumentIRI(Mockito.eq(testImport))).thenReturn(remap);
        m.getIRIMappers().set(mock);
        m.createOntology(remap);
        OWLOntology o = m.loadOntologyFromOntologyDocument(source);
        OWLIOUtils.print(o);
        Assertions.assertEquals(1, o.importsDeclarations().count());
        Mockito.verify(mock).getDocumentIRI(testImport);
    }

    @Test
    public void testShouldRemapImportRdfXML() throws OWLOntologyCreationException {
        String input = "<?xml version=\"1.0\"?>\n"
                + "<rdf:RDF xmlns=\"urn:test#\"\n"
                + "     xml:base=\"urn:test\"\n"
                + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
                + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "    <owl:Ontology rdf:about=\"urn:test\">\n"
                + "        <owl:imports rdf:resource=\"http://test.org/TestPizzaImport.owl\"/>\n"
                + "    </owl:Ontology>\n"
                + "</rdf:RDF>";
        IRI testImport = IRI.create("http://test.org/", "TestPizzaImport.owl");
        IRI remap = IRI.create("urn:test:", "mockImport");
        StringDocumentSource source = new StringDocumentSource(input);
        OWLOntologyIRIMapper mock = Mockito.mock(OWLOntologyIRIMapper.class);
        Mockito.when(mock.getDocumentIRI(Mockito.eq(testImport))).thenReturn(remap);
        m.getIRIMappers().set(mock);
        m.createOntology(remap);
        OWLOntology o = m.loadOntologyFromOntologyDocument(source);
        Assertions.assertEquals(1, o.importsDeclarations().count());
        Mockito.verify(mock).getDocumentIRI(testImport);
    }

    @Test
    public void testImportOntologyByLocation() throws Exception {
        Path f = TempDirectory.createFile("a.", ".owl");
        OWLOntology o = createOntologyFile(IRI.create("http://a.com", ""), f);
        // have to load an ontology for it to get a document IRI
        OWLOntology a = m.loadOntologyFromOntologyDocument(f.toFile());
        Assertions.assertEquals(o, a);
        IRI locA = m.getOntologyDocumentIRI(a);
        IRI bIRI = IRI.create("http://b.com", "");
        OWLOntology b = getOWLOntology(bIRI);
        // import from the document location of a.owl (rather than the
        // ontology IRI)
        b.applyChange(new AddImport(b, df.getOWLImportsDeclaration(locA)));
        Assertions.assertEquals(1, b.importsDeclarations().count());
        Assertions.assertEquals(1, b.imports().count());
    }

    @Test
    public void testShouldNotCreateIllegalPunning() throws OWLOntologyCreationException {
        m.getIRIMappers().add(new AutoIRIMapper(new File(RESOURCES, "importscyclic"), true));
        OWLOntology o = m.loadOntologyFromOntologyDocument(IRI.create(new File(RESOURCES, "importscyclic/relaMath.owl")));
        OWLProfileReport checkOntology = Profiles.OWL2_DL.checkOntology(o);
        Assertions.assertTrue(checkOntology.isInProfile(), checkOntology.toString());
        o.directImports().forEach(ont -> Assertions.assertEquals(0, ont.annotationPropertiesInSignature().count()));
    }

    private OWLOntology createOntologyFile(IRI iri, Path f) throws Exception {
        OWLOntology a = m1.createOntology(iri);
        try (OutputStream out = new FileOutputStream(f.toFile())) {
            a.saveOntology(out);
        }
        return a;
    }

    @Test // from OWL-API-5.1.5
    public void testImportsWhenRemovingAndReloading() throws Exception {
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        AutoIRIMapper mapper = new AutoIRIMapper(new File(RESOURCES, "imports"), true);
        man.getIRIMappers().add(mapper);
        String name = "/owlapi/imports/thesubont.omn";
        OWLOntology root = man.loadOntologyFromOntologyDocument(Objects.requireNonNull(getClass().getResourceAsStream(name)));
        Assertions.assertEquals(1, root.imports().count());
        for (OWLOntology ontology : man.ontologies().collect(Collectors.toList())) {
            man.removeOntology(ontology);
        }
        Assertions.assertEquals(0, man.ontologies().count());
        root = man.loadOntologyFromOntologyDocument(Objects.requireNonNull(getClass().getResourceAsStream(name)));
        Assertions.assertEquals(1, root.imports().count());
    }

    @Test // from OWL-API-5.1.5
    public void testAutoIRIMapperShouldNotBeConfusedByPrefixes() {
        AutoIRIMapper mapper = new AutoIRIMapper(new File(RESOURCES, "imports"), true);
        Assertions.assertTrue(mapper.getOntologyIRIs().contains(IRI.create("http://owlapitestontologies.com/thesubont")));
    }

    @Test // from OWL-API-5.1.5
    public void testAutoIRIMapperShouldRecogniseRdfAboutInOwlOntology() {
        AutoIRIMapper mapper = new AutoIRIMapper(new File(RESOURCES, "imports"), true);
        Assertions.assertTrue(mapper.getOntologyIRIs().contains(IRI.create("http://test.org/compleximports/A.owl")));
    }

}
