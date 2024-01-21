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

package com.github.owlcs.ontapi.tests.managers;

import com.github.owlcs.TempDirectory;
import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.OntologyModelImpl;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.sszuev.jena.ontapi.OntJenaException;
import com.github.sszuev.jena.ontapi.OntModelFactory;
import com.github.sszuev.jena.ontapi.model.OntClass;
import com.github.sszuev.jena.ontapi.model.OntModel;
import com.github.sszuev.jena.ontapi.vocabulary.RDF;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * For testing miscellaneous general functionality related to manager and load settings.
 * <p>
 * Created by @ssz on 23.01.2018.
 */
public class MiscOntologyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiscOntologyTest.class);

    private static Graph makeGraphWithRecursion() {
        Model m = OntModelFactory.createDefaultModel().setNsPrefixes(OntModelFactory.STANDARD);
        Resource anon = m.createResource().addProperty(RDF.type, OWL.Class);
        anon.addProperty(OWL.complementOf, anon);
        return m.getGraph();
    }

    @Test
    public void testImportsOnConcurrentManager() {
        Class<? extends OWLOntology> expected = OntologyModelImpl.Concurrent.class;
        OntologyManager m = OntManagers.createConcurrentManager();
        DataFactory df = m.getOWLDataFactory();
        Ontology a = m.createOntology(IRI.create("A"));
        Ontology b = m.createOntology(IRI.create("B"));
        Ontology c = m.createOntology(IRI.create("C"));
        Ontology d = m.createOntology(IRI.create("D"));
        a.asGraphModel().addImport(b.asGraphModel());
        m.applyChange(new AddImport(a,
                df.getOWLImportsDeclaration(c.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new))));
        b.asGraphModel().addImport(d.asGraphModel());
        OWLIOUtils.print(a);
        OWLIOUtils.print(b);

        Assertions.assertEquals(3, a.imports()
                .peek(x -> Assertions.assertTrue(expected.isInstance(x))).count());
        Assertions.assertEquals(2, a.directImports()
                .peek(x -> Assertions.assertTrue(expected.isInstance(x))).count());
        Assertions.assertEquals(4, a.importsClosure()
                .peek(x -> Assertions.assertTrue(expected.isInstance(x))).count());
    }

    @Test
    public void testReadRecursiveGraph() {
        // not a StackOverflowError
        Assertions.assertThrows(OntApiException.class, () -> {
            IRI iri = IRI.create(OWLIOUtils.getResourceURI("/ontapi/recursive-graph.ttl"));
            LOGGER.debug("The file: {}", iri);
            OntologyManager m = OntManagers.createManager();
            m.getOntologyConfigurator().setPerformTransformation(false);
            Ontology o = m.loadOntology(iri);
            OWLIOUtils.print(o.asGraphModel());
            o.axioms().forEach(a -> LOGGER.debug("{}", a));
        });
    }

    @Test
    public void testRecursionOnComplementOf() {
        // not a StackOverflowError
        Assertions.assertThrows(OntJenaException.class, () -> {
            Graph g = makeGraphWithRecursion();
            OntModel o = OntModelFactory.createModel(g);
            OWLIOUtils.print(o);
            List<OntClass> ces = o.ontObjects(OntClass.class).collect(Collectors.toList());
            ces.forEach(x -> LOGGER.error("{}", x));
        });
    }

    @Test
    public void testRecursionOnComplementOfWithIgnoreReadAxiomsErrors() {
        OntologyManager m = OntManagers.createManager();
        m.getOntologyConfigurator().setIgnoreAxiomsReadErrors(true);
        Graph g = makeGraphWithRecursion();
        OntModel o = m.addOntology(g).asGraphModel();
        OWLIOUtils.print(o);
        Assertions.assertEquals(0, o.ontObjects(OntClass.ComplementOf.class).count());
        Assertions.assertEquals(0, o.ontObjects(OntClass.class).count());
    }

    @Test
    public void testPrefixesOnReload() throws IOException, OWLOntologyStorageException, OWLOntologyCreationException {
        LOGGER.debug("Create");
        String prefName = "argh";
        OntModel model = OntModelFactory.createModel();
        model.setID("http://test.com/ont").setVersionIRI("http://test.com/ver/1.0");
        LOGGER.debug("{} has been created", model);
        model.setNsPrefix(prefName, model.getID().getURI() + "#");
        String clazz = model.createOntClass(model.getNsPrefixURI("argh") + "TheClass").getURI();
        LOGGER.debug("Class {} has been added", clazz);
        LOGGER.debug("\n{}", OWLIOUtils.asString(model, OntFormat.TURTLE));

        LOGGER.debug("Add to manager");
        OntologyManager manager1 = OntManagers.createManager();
        Ontology ontology1 = manager1.addOntology(model.getGraph());
        Assertions.assertEquals(1, ontology1.getAxiomCount());
        OWLDocumentFormat f = manager1.getOntologyFormat(ontology1);
        Assertions.assertEquals(OntFormat.TURTLE.createOwlFormat(), f);
        Assertions.assertEquals(model.getNsPrefixURI(prefName),
                f != null ? f.asPrefixOWLDocumentFormat().getPrefix(prefName + ":") : null, "Wrong prefix");

        LOGGER.debug("Save");
        Path file = TempDirectory.createFile(getClass().getName() + ".", ".rdf");
        IRI iri = IRI.create(file.toUri());
        LOGGER.debug("File to save {}", file);
        OWLDocumentFormat format = OntFormat.RDF_XML.createOwlFormat();
        format.asPrefixOWLDocumentFormat().setPrefixManager(Objects.requireNonNull(f).asPrefixOWLDocumentFormat());
        manager1.saveOntology(ontology1, format, iri);

        LOGGER.debug("Reload");
        OWLOntologyDocumentSource source = new FileDocumentSource(file.toFile(), OntFormat.RDF_XML.createOwlFormat());
        OntologyManager manager2 = OntManagers.createManager();
        Ontology ontology2 = manager2.loadOntologyFromOntologyDocument(source);

        Files.delete(file);
        Assertions.assertFalse(Files.exists(file));

        Assertions.assertEquals(1, ontology2.getAxiomCount());
        OWLDocumentFormat format2 = manager2.getOntologyFormat(ontology2);
        LOGGER.debug("\n{}", OWLIOUtils.asString(ontology2, format2));
        Assertions.assertEquals(OntFormat.RDF_XML.createOwlFormat(), format2);
        Assertions.assertEquals(model.getNsPrefixURI(prefName),
                format2 != null ? format2.asPrefixOWLDocumentFormat().getPrefix(prefName + ":") : null, "Wrong prefix");
    }

}
