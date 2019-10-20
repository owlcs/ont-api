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

package ru.avicomp.ontapi.tests.managers;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.*;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * For testing miscellaneous general functionality related to manager and load settings.
 * <p>
 * Created by @szuev on 23.01.2018.
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
        OntologyManager m = OntManagers.createConcurrentONT();
        DataFactory df = m.getOWLDataFactory();
        OntologyModel a = m.createOntology(IRI.create("A"));
        OntologyModel b = m.createOntology(IRI.create("B"));
        OntologyModel c = m.createOntology(IRI.create("C"));
        OntologyModel d = m.createOntology(IRI.create("D"));
        a.asGraphModel().addImport(b.asGraphModel());
        m.applyChange(new AddImport(a,
                df.getOWLImportsDeclaration(c.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new))));
        b.asGraphModel().addImport(d.asGraphModel());
        ReadWriteUtils.print(a);
        ReadWriteUtils.print(b);

        Assert.assertEquals(3, a.imports()
                .peek(x -> Assert.assertTrue(expected.isInstance(x))).count());
        Assert.assertEquals(2, a.directImports()
                .peek(x -> Assert.assertTrue(expected.isInstance(x))).count());
        Assert.assertEquals(4, a.importsClosure()
                .peek(x -> Assert.assertTrue(expected.isInstance(x))).count());
    }

    @Test(expected = OntApiException.class) // not a StackOverflowError
    public void testReadRecursiveGraph() throws OWLOntologyCreationException {
        IRI iri = IRI.create(ReadWriteUtils.getResourceURI("ontapi/recursive-graph.ttl"));
        LOGGER.debug("The file: {}", iri);
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setPerformTransformation(false);
        OntologyModel o = m.loadOntology(iri);
        ReadWriteUtils.print(o.asGraphModel());
        o.axioms().forEach(a -> LOGGER.debug("{}", a));
    }

    @Test(expected = OntJenaException.class) // not a StackOverflowError
    public void testRecursionOnComplementOf() {
        Graph g = makeGraphWithRecursion();
        OntGraphModel o = OntModelFactory.createModel(g);
        ReadWriteUtils.print(o);
        List<OntCE> ces = o.ontObjects(OntCE.class).collect(Collectors.toList());
        ces.forEach(x -> LOGGER.error("{}", x));
    }

    @Test
    public void testRecursionOnComplementOfWithIgnoreReadAxiomsErrors() {
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setIgnoreAxiomsReadErrors(true);
        Graph g = makeGraphWithRecursion();
        OntGraphModel o = m.addOntology(g).asGraphModel();
        ReadWriteUtils.print(o);
        Assert.assertEquals(0, o.ontObjects(OntCE.ComplementOf.class).count());
        Assert.assertEquals(0, o.ontObjects(OntCE.class).count());
    }

    @Test
    public void testPrefixesOnReload() throws IOException, OWLOntologyStorageException, OWLOntologyCreationException {
        LOGGER.debug("Create");
        String prefName = "argh";
        OntGraphModel model = OntModelFactory.createModel();
        model.setID("http://test.com/ont").setVersionIRI("http://test.com/ver/1.0");
        LOGGER.debug("{} has been created", model);
        model.setNsPrefix(prefName, model.getID().getURI() + "#");
        String clazz = model.createOntClass(model.getNsPrefixURI("argh") + "TheClass").getURI();
        LOGGER.debug("Class {} has been added", clazz);
        LOGGER.debug("\n{}", ReadWriteUtils.toString(model, OntFormat.TURTLE));

        LOGGER.debug("Add to manager");
        OntologyManager manager1 = OntManagers.createONT();
        OntologyModel ontology1 = manager1.addOntology(model.getGraph());
        Assert.assertEquals(1, ontology1.getAxiomCount());
        OWLDocumentFormat format1 = manager1.getOntologyFormat(ontology1);
        Assert.assertEquals(OntFormat.TURTLE.createOwlFormat(), format1);
        //noinspection ConstantConditions
        Assert.assertEquals("Wrong prefix", model.getNsPrefixURI(prefName), format1.asPrefixOWLDocumentFormat().getPrefix(prefName + ":"));

        LOGGER.debug("Save");
        Path file = Files.createTempFile(getClass().getName() + ".", ".rdf");
        IRI iri = IRI.create(file.toUri());
        LOGGER.debug("File to save {}", file);
        OWLDocumentFormat format = OntFormat.RDF_XML.createOwlFormat();
        format.asPrefixOWLDocumentFormat().setPrefixManager(format1.asPrefixOWLDocumentFormat());
        manager1.saveOntology(ontology1, format, iri);

        LOGGER.debug("Reload");
        OWLOntologyDocumentSource source = new FileDocumentSource(file.toFile(), OntFormat.RDF_XML.createOwlFormat());
        OntologyManager manager2 = OntManagers.createONT();
        OntologyModel ontology2 = manager2.loadOntologyFromOntologyDocument(source);

        Assert.assertEquals(1, ontology2.getAxiomCount());
        OWLDocumentFormat format2 = manager2.getOntologyFormat(ontology2);
        LOGGER.debug("\n{}", ReadWriteUtils.toString(ontology2, format2));
        Assert.assertEquals(OntFormat.RDF_XML.createOwlFormat(), format2);
        //noinspection ConstantConditions
        Assert.assertEquals("Wrong prefix", model.getNsPrefixURI(prefName), format2.asPrefixOWLDocumentFormat().getPrefix(prefName + ":"));
    }

}
