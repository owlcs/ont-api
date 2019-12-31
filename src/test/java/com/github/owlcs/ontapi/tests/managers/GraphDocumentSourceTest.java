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

package com.github.owlcs.ontapi.tests.managers;

import com.github.owlcs.ontapi.*;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.UnionGraph;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.transforms.GraphTransformers;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.Union;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * To test {@link OntGraphDocumentSource}.
 * Created by @ssz on 23.09.2018.
 */
public class GraphDocumentSourceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphDocumentSourceTest.class);

    @Test
    public void testCommonValidateOGDS() {
        OntModel m = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("/ontapi/pizza.ttl").getGraph());
        OntGraphDocumentSource s = OntGraphDocumentSource.wrap(m.getGraph());
        URI u1 = s.getDocumentIRI().toURI();
        Assert.assertFalse(s.hasAlredyFailedOnStreams());
        Assert.assertFalse(s.hasAlredyFailedOnIRIResolution());
        InputStream a = s.getInputStream().orElseThrow(AssertionError::new);
        InputStream b = s.getInputStream().orElseThrow(AssertionError::new);
        Assert.assertNotSame(a, b);
        Assert.assertFalse(s.hasAlredyFailedOnStreams());
        Assert.assertFalse(s.hasAlredyFailedOnIRIResolution());
        Assert.assertEquals(u1, s.getDocumentIRI().toURI());
        Assert.assertEquals(OntFormat.TURTLE, s.getOntFormat());
        Assert.assertTrue(s.getFormat().orElseThrow(AssertionError::new) instanceof TurtleDocumentFormat);
    }

    @Test
    public void testOntGraphDocumentSourceInOWL() throws OWLOntologyCreationException {
        IRI pizza = IRI.create(MiscOntologyTest.class.getResource("/ontapi/pizza.ttl"));
        LOGGER.debug("File: {}", pizza);
        Ontology ont = OntManagers.createONT().loadOntology(pizza);
        OWLOntologyDocumentSource src = OntGraphDocumentSource.wrap(ont.asGraphModel().getBaseGraph());
        URI uri = src.getDocumentIRI().toURI();
        LOGGER.debug("Load using pipes from: {}", uri);
        OWLOntology owl = OntManagers.createOWL().loadOntologyFromOntologyDocument(src);
        Set<OWLAxiom> ontAxioms = ont.axioms().collect(Collectors.toSet());
        Set<OWLAxiom> owlAxioms = owl.axioms().collect(Collectors.toSet());
        LOGGER.debug("OWL Axioms Count={}, ONT Axioms Count={}", owlAxioms.size(), ontAxioms.size());
        Assert.assertEquals(ontAxioms, owlAxioms);
    }

    @Test
    public void testOntGraphDocumentSourceInONT() throws OWLOntologyCreationException {
        List<String> iris = Arrays.asList("a", "b", "c");
        OntologyManager m = OntManagers.createONT();
        m.createGraphModel(iris.get(0));
        m.createGraphModel(iris.get(1));
        OntModel c = OntModelFactory.createModel();
        c.setID(iris.get(2)).addImport(iris.get(0)).addImport(iris.get(1));
        ReadWriteUtils.print(c);
        OntGraphDocumentSource src = OntGraphDocumentSource.wrap(c.getGraph());
        LOGGER.debug("Load graph from: {}", src.getDocumentIRI().toURI());
        m.loadOntologyFromOntologyDocument(src);
        Assert.assertEquals(3, m.ontologies().count());
        iris.forEach(i -> Assert.assertNotNull(m.getGraphModel(i)));
    }

    @Test
    public void testUnsupportedFormatInOGDS() throws IOException {
        OntModel m = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("/ontapi/pizza.ttl").getGraph());
        String uri = m.getID().getURI();
        OntGraphDocumentSource unsupported = new OntGraphDocumentSource() {
            @Override
            public Graph getGraph() {
                return m.getGraph();
            }

            @Override
            public OntFormat getOntFormat() {
                return OntFormat.TSV;
            }
        };
        testBrokenOGDS(uri, unsupported);
    }

    @Test
    public void testBrokenOntGraphDocumentSource() throws IOException {
        String key = "TEST";
        testBrokenOGDS(String.format("unknown: '%s'", key), new OntGraphDocumentSource() {
            @Override
            public Graph getGraph() {
                return new GraphMem() {

                    @Override
                    public ExtendedIterator<Triple> graphBaseFind(Triple m) {
                        throw new IllegalArgumentException(key);
                    }
                };
            }
        });
    }

    private void testBrokenOGDS(String graphName, OntGraphDocumentSource ogds) throws IOException {
        Assert.assertFalse(ogds.hasAlredyFailedOnStreams());
        IOException expected = null;
        try (InputStream is1 = ogds.getInputStream().orElseThrow(AssertionError::new)) {
            int x = is1.read();
            LOGGER.debug("Close: {}, bytes: {}", is1, x);
        } catch (IOException e) {
            expected = e;
        }
        Assert.assertNotNull(expected);
        LOGGER.debug("Message: {}", expected.getMessage());
        Assert.assertTrue("Unexpected message: '" + expected.getMessage() + "'",
                expected.getMessage().contains(graphName));
        Assert.assertNotNull(expected.getCause());
        LOGGER.debug("Cause: {}", expected.getCause().getMessage());
        Assert.assertTrue("No fail?", ogds.hasAlredyFailedOnStreams());
        InputStream is2 = ogds.getInputStream().orElseThrow(AssertionError::new);
        try {
            is2.close();
        } catch (Exception e) {
            LOGGER.debug("Exception: {}", e.getMessage());
        }
        is2.close();
    }

    @Test
    public void testAddCompositeGraph() {
        OntModel m1 = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        m1.setID("http://xxxxxx");
        m1.createOntClass("x");

        Model m2 = ModelFactory.createDefaultModel();
        m2.createResource().addProperty(RDFS.comment, "XXX");

        Union u = new Union(m1.getBaseGraph(), m2.getGraph());

        ReadWriteUtils.print(ModelFactory.createModelForGraph(u));

        OntologyManager manager = OntManagers.createONT();
        Ontology o = manager.addOntology(u);
        Assert.assertEquals(1, manager.ontologies().peek(x -> LOGGER.debug("Ontology: {}", x)).count());
        Assert.assertNotNull(manager.getOntology(new ID(m1.getID())));

        // class declaration and annotation property assertion with anonymous individual
        Assert.assertEquals(2, o.axioms().peek(x -> LOGGER.debug("AXIOM: {}", x)).count());
        Assert.assertEquals(1, o.asGraphModel().ontObjects(OntIndividual.class)
                .peek(x -> LOGGER.debug("Individual: {}", x)).count());

        Assert.assertTrue(o.asGraphModel().getBaseGraph() instanceof Union);
        Assert.assertTrue(o.asGraphModel().getGraph() instanceof UnionGraph);

        // serialization should fail:
        try (ObjectOutputStream stream = new ObjectOutputStream(new ByteArrayOutputStream())) {
            stream.writeObject(manager);
            Assert.fail("Possible to serialize");
        } catch (OntApiException e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        } catch (IOException e) {
            throw new AssertionError("Unexpected io-error", e);
        }
    }

    @Test
    public void testDisableTransforms() throws OWLOntologyCreationException {
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setGraphTransformers(new GraphTransformers()
                .addLast(g -> {
                    throw new IllegalStateException("TEST");
                }));

        OntModel g = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("/ontapi/pizza.ttl").getGraph());
        OntGraphDocumentSource s1 = OntGraphDocumentSource.wrap(g.getBaseGraph());
        try {
            m.loadOntologyFromOntologyDocument(s1);
            Assert.fail("No transforms are running");
        } catch (IllegalStateException e) {
            LOGGER.debug("Expected: {}", e.getMessage());
            Assert.assertEquals(0, m.ontologies().count());
        }
        OntGraphDocumentSource s2 = new OntGraphDocumentSource() {
            @Override
            public Graph getGraph() {
                return g.getBaseGraph();
            }

            @Override
            public boolean withTransforms() {
                return false;
            }
        };
        Ontology o = m.loadOntologyFromOntologyDocument(s2);
        Assert.assertNotNull(o);
        Assert.assertEquals(1, m.ontologies().count());
    }
}
