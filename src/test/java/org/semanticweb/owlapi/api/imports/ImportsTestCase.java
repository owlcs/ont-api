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
package org.semanticweb.owlapi.api.imports;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.semanticweb.owlapi.api.baseclasses.TestBase;
import org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.Profiles;
import org.semanticweb.owlapi.util.AutoIRIMapper;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import ru.avicomp.owlapi.OWLManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asList;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health
 *         Informatics Group
 * @since 3.1.0
 */

@SuppressWarnings("javadoc")
public class ImportsTestCase extends TestBase {

    @Test
    public void testImportsClosureUpdate() throws OWLOntologyCreationException {
        IRI aIRI = OWLFunctionalSyntaxFactory.IRI("http://a.com", "");
        OWLOntology ontA = getOWLOntology(aIRI);
        IRI bIRI = OWLFunctionalSyntaxFactory.IRI("http://b.com", "");
        OWLOntology ontB = getOWLOntology(bIRI);
        ontA.applyChange(new AddImport(ontA, df.getOWLImportsDeclaration(bIRI)));
        Assert.assertEquals(2, m.importsClosure(ontA).count());
        m.removeOntology(ontB);
        Assert.assertEquals(1, m.importsClosure(ontA).count());
        getOWLOntology(bIRI);
        Assert.assertEquals(2, m.importsClosure(ontA).count());
    }

    @Test
    public void shouldLoad() throws Exception {
        File importsBothNameAndVersion = folder.newFile("tempimportsNameAndVersion.owl");
        File importsBothNameAndOther = folder.newFile("tempimportsNameAndOther.owl");
        File ontologyByName = folder.newFile("tempmain.owl");
        File ontologyByVersion = folder.newFile("tempversion.owl");
        File ontologyByOtherPath = folder.newFile("tempother.owl");
        OWLOntology ontology = getOWLOntology(new OWLOntologyID(Optional.of(IRI.create(ontologyByName)), Optional.of(IRI
                .create(ontologyByVersion))));
        ontology.saveOntology(IRI.create(ontologyByName));
        ontology.saveOntology(IRI.create(ontologyByVersion));
        ontology.saveOntology(IRI.create(ontologyByOtherPath));
        OWLOntology ontology1 = m1.createOntology(IRI.create(importsBothNameAndVersion));
        OWLOntology ontology2 = m1.createOntology(IRI.create(importsBothNameAndOther));
        List<AddImport> changes = new ArrayList<>();
        changes.add(new AddImport(ontology1, df.getOWLImportsDeclaration(IRI.create(ontologyByName))));
        changes.add(new AddImport(ontology1, df.getOWLImportsDeclaration(IRI.create(ontologyByVersion))));
        changes.add(new AddImport(ontology2, df.getOWLImportsDeclaration(IRI.create(ontologyByName))));
        changes.add(new AddImport(ontology2, df.getOWLImportsDeclaration(IRI.create(ontologyByOtherPath))));
        ontology1.applyChanges(changes);
        ontology2.applyChanges(changes);
        ontology1.saveOntology(IRI.create(importsBothNameAndVersion));
        ontology2.saveOntology(IRI.create(importsBothNameAndOther));
        // when
        OWLOntology o1 = m.loadOntology(IRI.create(importsBothNameAndVersion));
        OWLOntology o2 = m1.loadOntology(IRI.create(importsBothNameAndOther));
        // then
        Assert.assertNotNull(o1);
        Assert.assertNotNull(o2);
    }

    @Test
    public void shouldNotLoadWrong() throws OWLOntologyCreationException {
        m.createOntology(IRI.create("urn:test#", "test"));
        StringDocumentSource documentSource = new StringDocumentSource("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:owl =\"http://www.w3.org/2002/07/owl#\">\n"
                + "    <owl:Ontology><owl:imports rdf:resource=\"urn:test#test\"/></owl:Ontology></rdf:RDF>");
        OWLOntology o = m.loadOntologyFromOntologyDocument(documentSource);
        Assert.assertTrue(o.getOntologyID().toString(), o.isAnonymous());
        Assert.assertFalse(o.getOntologyID().getDefaultDocumentIRI().isPresent());
    }

    @Test
    public void testManualImports() throws OWLOntologyCreationException {
        OWLOntology baseOnt = getOWLOntology(OWLFunctionalSyntaxFactory.IRI("http://semanticweb.org/ontologies/", "base"));
        IRI importedIRI = OWLFunctionalSyntaxFactory.IRI("http://semanticweb.org/ontologies/", "imported");
        OWLOntology importedOnt = getOWLOntology(importedIRI);
        Set<OWLOntology> preImportsClosureCache = OWLAPIStreamUtils.asUnorderedSet(baseOnt.importsClosure());
        Assert.assertTrue(preImportsClosureCache.contains(baseOnt));
        Assert.assertFalse(preImportsClosureCache.contains(importedOnt));
        baseOnt.applyChange(new AddImport(baseOnt, df.getOWLImportsDeclaration(importedIRI)));
        Set<OWLOntology> postImportsClosureCache = OWLAPIStreamUtils.asUnorderedSet(baseOnt.importsClosure());
        Assert.assertTrue(postImportsClosureCache.contains(baseOnt));
        Assert.assertTrue(postImportsClosureCache.contains(importedOnt));
    }

    @Test(expected = UnloadableImportException.class)
    public void shouldThrowExceptionWithDefaultImportsconfig() throws OWLOntologyCreationException {
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

    /**
     * ONT-API: This is a borderline case.
     * The original ontologies (subject-amy.ttl, subject-bob.ttl and subject-sue.ttl) did NOT contain prefix 'owl'
     * until I added it.
     * But they had (and have) resources in short form which use this prefix (e.g. 'owl:Ontology', 'owl:imports' etc).
     * So they were NOT correct ontologies until now.
     * And jena parser collapsed on them with the fully correct error ('[line: 9, col: 77] Undefined prefix: owl').
     * In case of failure with jena the current implementation of ONT-API {@link ru.avicomp.ontapi.OntologyFactory}
     * tries the original ('pure') OWL-API parser (the mixed loading).
     * But in this case there is no trimming with transformation mechanism
     * ({@link ru.avicomp.ontapi.transforms.Transform}).
     * Only the anonymous base ontology (core.ttl) and the top-level ontology (subject.ttl) were transformed.
     * But it seems it was not enough for correct working of the logic embedded in the testcase
     * (the declarations for NamedIndividuals occurred to be missed and
     * instead object and data property assertions there were annotation assertions).
     * So the conclusion is this: if you have corrupted ontology then you may encounter some problems.
     * No one knows what exactly since we use OWL-API native implementations as last attempt to load
     * which is not strict to the syntax in most owl-api-parsers.
     *
     * @throws OWLOntologyCreationException
     */
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
        ru.avicomp.ontapi.utils.ReadWriteUtils.print(top);
        OWLOntology bob = m.getOntology(bobsOntologyName);
        Assert.assertNotNull("Can't find Bob.", bob);
        ru.avicomp.ontapi.utils.ReadWriteUtils.print(bob);
        Assert.assertEquals("Unexpected ontologies count.", 4, m.ontologies().count());
        Assert.assertTrue("Individuals about Bob are missing...", top.containsEntityInSignature(bobsIndividual, Imports.INCLUDED));
    }

    /**
     * Tests to see if the method which obtains the imports closure behaves
     * correctly.
     */
    @Test
    public void testImportsClosure() {
        OWLOntology ontA = getOWLOntology();
        OWLOntology ontB = getOWLOntology();
        Assert.assertTrue(OWLAPIStreamUtils.contains(ontA.importsClosure(), ontA));
        OWLImportsDeclaration importsDeclaration = OWLFunctionalSyntaxFactory.ImportsDeclaration(get(ontB.getOntologyID().getOntologyIRI()));
        ontA.applyChange(new AddImport(ontA, importsDeclaration));
        Assert.assertTrue(OWLAPIStreamUtils.contains(ontA.importsClosure(), ontB));
        ontA.applyChange(new RemoveImport(ontA, importsDeclaration));
        Assert.assertFalse(OWLAPIStreamUtils.contains(ontA.importsClosure(), ontB));
        ontA.applyChange(new AddImport(ontA, importsDeclaration));
        Assert.assertTrue(OWLAPIStreamUtils.contains(ontA.importsClosure(), ontB));
        ontB.getOWLOntologyManager().removeOntology(ontB);
        Assert.assertFalse(OWLAPIStreamUtils.contains(ontA.importsClosure(), ontB));
    }

    @Test
    public void shouldRemapImport() throws OWLOntologyCreationException {
        String input = "<?xml version=\"1.0\"?>\n"
                + "<Ontology  ontologyIRI=\"http://protege.org/ontologies/TestFunnyPizzaImport.owl\">\n"
                + "    <Import>http://test.org/TestPizzaImport.owl</Import>\n" + "</Ontology>";
        // the explicit type for ONT-API, otherwise this(^^^) data would be considered as RDF/XML,
        // and the corresponding(valid) graph will have the strange URI's
        StringDocumentSource source = new StringDocumentSource(input, IRI.getNextDocumentIRI("string:ontology"),
                ru.avicomp.ontapi.OntFormat.OWL_XML.createOwlFormat(), ru.avicomp.ontapi.OntFormat.OWL_XML.getID());
        IRI testImport = IRI.create("http://test.org/", "TestPizzaImport.owl");
        IRI remap = IRI.create("urn:test:", "mockImport");
        OWLOntologyIRIMapper mock = Mockito.mock(OWLOntologyIRIMapper.class);
        Mockito.when(mock.getDocumentIRI(Matchers.eq(testImport))).thenReturn(remap);
        m.getIRIMappers().set(mock);
        m.createOntology(remap);
        OWLOntology o = m.loadOntologyFromOntologyDocument(source);
        ru.avicomp.ontapi.utils.ReadWriteUtils.print(o);
        Assert.assertEquals(1, o.importsDeclarations().count());
        Mockito.verify(mock).getDocumentIRI(testImport);
    }

    @Test
    public void shouldRemapImportRdfXML() throws OWLOntologyCreationException {
        String input = "<?xml version=\"1.0\"?>\n" + "<rdf:RDF xmlns=\"urn:test#\"\n" + "     xml:base=\"urn:test\"\n"
                + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
                + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "    <owl:Ontology rdf:about=\"urn:test\">\n"
                + "        <owl:imports rdf:resource=\"http://test.org/TestPizzaImport.owl\"/>\n" + "    </owl:Ontology>\n"
                + "</rdf:RDF>";
        IRI testImport = IRI.create("http://test.org/", "TestPizzaImport.owl");
        IRI remap = IRI.create("urn:test:", "mockImport");
        StringDocumentSource source = new StringDocumentSource(input);
        OWLOntologyIRIMapper mock = Mockito.mock(OWLOntologyIRIMapper.class);
        Mockito.when(mock.getDocumentIRI(Matchers.eq(testImport))).thenReturn(remap);
        m.getIRIMappers().set(mock);
        m.createOntology(remap);
        OWLOntology o = m.loadOntologyFromOntologyDocument(source);
        Assert.assertEquals(1, o.importsDeclarations().count());
        Mockito.verify(mock).getDocumentIRI(testImport);
    }

    @Test
    public void testImportOntologyByLocation() throws Exception {
        File f = folder.newFile("a.owl");
        createOntologyFile(OWLFunctionalSyntaxFactory.IRI("http://a.com", ""), f);
        // have to load an ontology for it to get a document IRI
        OWLOntology a = m.loadOntologyFromOntologyDocument(f);
        IRI locA = m.getOntologyDocumentIRI(a);
        IRI bIRI = OWLFunctionalSyntaxFactory.IRI("http://b.com", "");
        OWLOntology b = getOWLOntology(bIRI);
        // import from the document location of a.owl (rather than the
        // ontology IRI)
        b.applyChange(new AddImport(b, df.getOWLImportsDeclaration(locA)));
        Assert.assertEquals(1, b.importsDeclarations().count());
        Assert.assertEquals(1, b.imports().count());
    }

    @Test
    public void shouldNotCreateIllegalPunning() throws OWLOntologyCreationException {
        m.getIRIMappers().add(new AutoIRIMapper(new File(RESOURCES, "importscyclic"), true));
        OWLOntology o = m.loadOntologyFromOntologyDocument(IRI.create(new File(RESOURCES,
                "importscyclic/relaMath.owl")));
        OWLProfileReport checkOntology = Profiles.OWL2_DL.checkOntology(o);
        Assert.assertTrue(checkOntology.toString(), checkOntology.isInProfile());
        o.directImports().forEach(ont -> Assert.assertEquals(0, ont.annotationPropertiesInSignature().count()));
    }

    private OWLOntology createOntologyFile(IRI iri, File f) throws Exception {
        OWLOntology a = m1.createOntology(iri);
        try (OutputStream out = new FileOutputStream(f)) {
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
        OWLOntology root = man.loadOntologyFromOntologyDocument(getClass().getResourceAsStream(name));
        assertEquals(1, root.imports().count());
        for (OWLOntology ontology : asList(man.ontologies())) {
            man.removeOntology(ontology);
        }
        assertEquals(0, man.ontologies().count());
        root = man.loadOntologyFromOntologyDocument(getClass().getResourceAsStream(name));
        assertEquals(1, root.imports().count());
    }

    /* // TODO: disabled till update to jena 3.8.0 + (where xerces is excluded)
    @Test // from OWL-API-5.1.5
    public void testAutoIRIMapperShouldNotBeConfusedByPrefixes() {
        AutoIRIMapper mapper = new AutoIRIMapper(new File(RESOURCES, "imports"), true);
        assertTrue(mapper.getOntologyIRIs()
                .contains(IRI.create("http://owlapitestontologies.com/thesubont")));
    }
    */

    @Test // from OWL-API-5.1.5
    public void testAutoIRIMapperShouldRecogniseRdfAboutInOwlOntology() {
        AutoIRIMapper mapper = new AutoIRIMapper(new File(RESOURCES, "imports"), true);
        assertTrue(mapper.getOntologyIRIs().contains(IRI.create("http://test.org/compleximports/A.owl")));
    }


}
