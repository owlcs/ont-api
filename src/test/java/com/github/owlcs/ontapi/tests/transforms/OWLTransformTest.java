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

package com.github.owlcs.ontapi.tests.transforms;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyLoaderMetaData;
import org.semanticweb.owlapi.io.RDFOntologyHeaderStatus;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.OntologyModel;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import com.github.owlcs.ontapi.transforms.GraphTransformers;
import com.github.owlcs.ontapi.transforms.Transform;
import com.github.owlcs.ontapi.transforms.TransformException;
import com.github.owlcs.ontapi.transforms.vocabulary.DEPRECATED;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import com.github.owlcs.ontapi.utils.TestUtils;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Created by @szuev on 01.04.2018.
 */
public class OWLTransformTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OWLTransformTest.class);

    @Test
    public void testCustomDatatypes() throws Exception {
        String txt = "<http://dbpedia.org/resource/Belgium>\t" +
                "<http://dbpedia.org/ontology/PopulatedPlace/populationDensity>\t" +
                "\"363.6\"^^<http://dbpedia.org/datatype/inhabitantsPerSquareKilometre> .\n" +
                "<http://dbpedia.org/resource/Belgium>\t" +
                "<http://dbpedia.org/ontology/PopulatedPlace/populationDensity>\t" +
                "\"363.58468065625044\"^^<http://dbpedia.org/datatype/inhabitantsPerSquareKilometre> .\n" +
                "<http://dbpedia.org/resource/London>\t" +
                "<http://dbpedia.org/ontology/PopulatedPlace/populationDensity>\t" +
                "\"5518.0\"^^<http://dbpedia.org/datatype/inhabitantsPerSquareKilometre> .";

        OWLOntologyDocumentSource src = ReadWriteUtils.getStringDocumentSource(txt, OntFormat.TURTLE);

        OntologyModel o = OntManagers.createONT().loadOntologyFromOntologyDocument(src);
        ReadWriteUtils.print(o);
        o.saveOntology(OntFormat.FUNCTIONAL_SYNTAX.createOwlFormat(), ReadWriteUtils.NULL_OUT);
        Assert.assertEquals(6, o.asGraphModel().size());
        Assert.assertEquals(5, o.axioms().peek(x -> LOGGER.debug("Axiom: {}", x)).count());
    }

    @Test
    public void testNCBITAXONTransform() throws OWLOntologyCreationException {
        OWLOntologyDocumentSource src = ReadWriteUtils.getFileDocumentSource("/ontapi/NCBITAXON-CUT.ttl", OntFormat.TURTLE);
        OWLOntologyManager m = OntManagers.createONT();
        OWLOntology o = m.loadOntologyFromOntologyDocument(src);

        TestUtils.assertAxiom(o, AxiomType.TRANSITIVE_OBJECT_PROPERTY, 3);
        TestUtils.assertAxiom(o, AxiomType.FUNCTIONAL_OBJECT_PROPERTY, 1);
        TestUtils.assertAxiom(o, AxiomType.SYMMETRIC_OBJECT_PROPERTY, 4);
        TestUtils.assertAxiom(o, AxiomType.INVERSE_OBJECT_PROPERTIES, 4);

        TestUtils.assertAxiom(o, AxiomType.OBJECT_PROPERTY_RANGE, 6);
        TestUtils.assertAxiom(o, AxiomType.OBJECT_PROPERTY_DOMAIN, 5);
        TestUtils.assertAxiom(o, AxiomType.SUB_OBJECT_PROPERTY, 15);
        TestUtils.assertAxiom(o, AxiomType.SUB_ANNOTATION_PROPERTY_OF, 9);

        TestUtils.assertAxiom(o, AxiomType.DATA_PROPERTY_ASSERTION, 2913);
        TestUtils.assertAxiom(o, AxiomType.ANNOTATION_ASSERTION, 3139);

        TestUtils.assertAxiom(o, AxiomType.SUBCLASS_OF, 3);
        TestUtils.assertAxiom(o, AxiomType.DISJOINT_CLASSES, 3);
        // 1011 without named individuals:
        TestUtils.assertAxiom(o, AxiomType.DECLARATION, 1984);
    }

    @Test
    public void testOWL11OntologyWithTransform() throws Exception {
        OWLOntologyDocumentSource src = ReadWriteUtils.getFileDocumentSource("/owlapi/owl11/family/family.owl",
                OntFormat.RDF_XML);
        LOGGER.debug("Source: {}", src);
        OWLOntologyManager m = OntManagers.createONT();
        OWLOntology o = m.loadOntologyFromOntologyDocument(src);

        ReadWriteUtils.print(o);

        TestUtils.assertAxiom(o, AxiomType.DECLARATION, 43);
        TestUtils.assertAxiom(o, AxiomType.EQUIVALENT_CLASSES, 11);
        TestUtils.assertAxiom(o, AxiomType.SUBCLASS_OF, 8);
        TestUtils.assertAxiom(o, AxiomType.DISJOINT_UNION, 1);
        TestUtils.assertAxiom(o, AxiomType.CLASS_ASSERTION, 13);
        TestUtils.assertAxiom(o, AxiomType.OBJECT_PROPERTY_ASSERTION, 13);
        TestUtils.assertAxiom(o, AxiomType.DATA_PROPERTY_ASSERTION, 5);
        TestUtils.assertAxiom(o, AxiomType.SUB_OBJECT_PROPERTY, 7);
        TestUtils.assertAxiom(o, AxiomType.INVERSE_OBJECT_PROPERTIES, 3);
        TestUtils.assertAxiom(o, AxiomType.SYMMETRIC_OBJECT_PROPERTY, 1);
        TestUtils.assertAxiom(o, AxiomType.IRREFLEXIVE_OBJECT_PROPERTY, 2);
        TestUtils.assertAxiom(o, AxiomType.FUNCTIONAL_OBJECT_PROPERTY, 2);
        TestUtils.assertAxiom(o, AxiomType.OBJECT_PROPERTY_DOMAIN, 2);
        TestUtils.assertAxiom(o, AxiomType.OBJECT_PROPERTY_RANGE, 9);
        TestUtils.assertAxiom(o, AxiomType.FUNCTIONAL_DATA_PROPERTY, 1);
        TestUtils.assertAxiom(o, AxiomType.DATA_PROPERTY_RANGE, 1);
        TestUtils.assertAxiom(o, AxiomType.ANNOTATION_ASSERTION, 0);
        TestUtils.assertAxiom(o, AxiomType.SUB_PROPERTY_CHAIN_OF, 2);

        Assert.assertEquals(132, o.getAxiomCount());
    }

    @Test
    public void testOWL11OntologyWithoutTransform() throws Exception {
        OWLOntologyDocumentSource src = ReadWriteUtils.getFileDocumentSource("/owlapi/owl11/family/family.owl",
                OntFormat.RDF_XML);
        LOGGER.debug("Source: {}", src);
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setPerformTransformation(false);
        OntologyModel o = m.loadOntologyFromOntologyDocument(src);

        ReadWriteUtils.print(o);

        TestUtils.assertAxiom(o, AxiomType.DECLARATION, 29);
        TestUtils.assertAxiom(o, AxiomType.EQUIVALENT_CLASSES, 5);
        TestUtils.assertAxiom(o, AxiomType.SUBCLASS_OF, 2);
        TestUtils.assertAxiom(o, AxiomType.DISJOINT_UNION, 1);
        TestUtils.assertAxiom(o, AxiomType.CLASS_ASSERTION, 13);
        TestUtils.assertAxiom(o, AxiomType.OBJECT_PROPERTY_ASSERTION, 13);
        TestUtils.assertAxiom(o, AxiomType.DATA_PROPERTY_ASSERTION, 5);
        TestUtils.assertAxiom(o, AxiomType.SUB_OBJECT_PROPERTY, 7);
        TestUtils.assertAxiom(o, AxiomType.INVERSE_OBJECT_PROPERTIES, 3);
        TestUtils.assertAxiom(o, AxiomType.SYMMETRIC_OBJECT_PROPERTY, 1);
        TestUtils.assertAxiom(o, AxiomType.IRREFLEXIVE_OBJECT_PROPERTY, 2);
        TestUtils.assertAxiom(o, AxiomType.FUNCTIONAL_OBJECT_PROPERTY, 2);
        TestUtils.assertAxiom(o, AxiomType.OBJECT_PROPERTY_DOMAIN, 2);
        TestUtils.assertAxiom(o, AxiomType.OBJECT_PROPERTY_RANGE, 9);
        TestUtils.assertAxiom(o, AxiomType.FUNCTIONAL_DATA_PROPERTY, 1);
        TestUtils.assertAxiom(o, AxiomType.DATA_PROPERTY_RANGE, 1);
        TestUtils.assertAxiom(o, AxiomType.ANNOTATION_ASSERTION, 0);
        TestUtils.assertAxiom(o, AxiomType.SUB_PROPERTY_CHAIN_OF, 0);

        Assert.assertEquals(96, o.getAxiomCount());
    }

    @Test
    public void testTransformOnAmbiguousRestriction() throws OWLOntologyCreationException {
        String s = "@prefix ex:    <http://www.example.org#> .\n" +
                "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n" +
                "@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .\n" +
                "\n" +
                "ex:c    owl:unionOf  ( ex:x ex:y ) .";
        OntologyModel o = OntManagers.createONT().loadOntologyFromOntologyDocument(new StringDocumentSource(s));
        ReadWriteUtils.print(o);
        Assert.assertEquals(3, o.axioms(AxiomType.DECLARATION).peek(x -> LOGGER.debug("DE: {}", x)).count());
        Assert.assertEquals(1, o.axioms(AxiomType.EQUIVALENT_CLASSES).peek(x -> LOGGER.debug("EC: {}", x)).count());
    }

    @Test
    public void testTransformDeprecatedOWLIRIs() {
        Model m = ModelFactory.createDefaultModel().setNsPrefixes(OntModelFactory.STANDARD);
        Resource c = m.createResource("C1").addProperty(DEPRECATED.OWL.declaredAs, OWL.Class);
        Resource i = m.createResource("I1", c);
        Resource p = m.createResource("P")
                .addProperty(DEPRECATED.OWL.declaredAs, DEPRECATED.OWL.DataProperty)
                .addProperty(DEPRECATED.OWL.dataPropertyRange, XSD.xstring)
                .addProperty(DEPRECATED.OWL.dataPropertyDomain, c)
                .addProperty(DEPRECATED.OWL.equivalentDataProperty, OWL.bottomDataProperty);

        m.createResource(DEPRECATED.OWL.NegativeDataPropertyAssertion)
                .addProperty(DEPRECATED.RDF.subject, i)
                .addProperty(DEPRECATED.RDF.predicate, p)
                .addProperty(DEPRECATED.RDF.object, "v");
        ReadWriteUtils.print(m);

        OntologyManager manager = OntManagers.createONT();
        OntologyModel o = manager.addOntology(m.getGraph(), manager.getOntologyLoaderConfiguration());
        ReadWriteUtils.print(o);
        TestUtils.assertAxiom(o, AxiomType.DECLARATION, 3);
        TestUtils.assertAxiom(o, AxiomType.CLASS_ASSERTION, 1);
        TestUtils.assertAxiom(o, AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION, 1);
        TestUtils.assertAxiom(o, AxiomType.DATA_PROPERTY_DOMAIN, 1);
        TestUtils.assertAxiom(o, AxiomType.DATA_PROPERTY_RANGE, 1);
        TestUtils.assertAxiom(o, AxiomType.EQUIVALENT_DATA_PROPERTIES, 1);
        Assert.assertEquals(8, o.axioms().count());
    }

    @Test
    public void testParseZeroHeader() throws OWLOntologyCreationException {
        Model m = ModelFactory.createDefaultModel().setNsPrefixes(OntModelFactory.STANDARD);
        m.createResource("http://class").addProperty(RDF.type, OWL.Class);
        String txt = ReadWriteUtils.toString(m, OntFormat.TURTLE);

        OntologyManager manager = OntManagers.createONT();
        OntologyModel o = manager.loadOntologyFromOntologyDocument(ReadWriteUtils.getStringDocumentSource(txt, OntFormat.TURTLE));
        OWLOntologyLoaderMetaData meta = manager.getNonnullOntologyFormat(o)
                .getOntologyLoaderMetaData().orElseThrow(AssertionError::new);
        print(meta);
        Assert.assertEquals(RDFOntologyHeaderStatus.PARSED_ZERO_HEADERS, meta.getHeaderState());
        Assert.assertEquals(0, meta.getUnparsedTriples().count());
        Assert.assertEquals(1, meta.getGuessedDeclarations().size());
        Assert.assertEquals(2, meta.getTripleCount());
    }

    @Test
    public void testParseMultipleHeaderAndGuessedDeclarations() throws OWLOntologyCreationException {
        String ontIRI = "http://o";
        String verIRI = "http://v";
        Model m = ModelFactory.createDefaultModel().setNsPrefixes(OntModelFactory.STANDARD);
        m.createResource("http://class2").addProperty(RDFS.subClassOf, m.createResource("http://class1", OWL.Class));
        m.createResource().addProperty(RDF.type, OWL.Ontology);
        m.createResource("http://ont1").addProperty(RDF.type, OWL.Ontology);
        m.createResource(ontIRI).addProperty(RDF.type, OWL.Ontology)
                .addProperty(OWL.versionIRI, m.createResource(verIRI));
        String txt = ReadWriteUtils.toString(m, OntFormat.TURTLE);

        LOGGER.debug("Original RDF:\n{}", txt);

        OntologyManager manager = OntManagers.createONT();
        OntologyModel o = manager.loadOntologyFromOntologyDocument(ReadWriteUtils.getStringDocumentSource(txt, OntFormat.TURTLE));
        ReadWriteUtils.print(o);
        Assert.assertEquals(ontIRI, o.getOntologyID().getOntologyIRI().map(IRI::getIRIString).orElseThrow(AssertionError::new));
        Assert.assertEquals(verIRI, o.getOntologyID().getVersionIRI().map(IRI::getIRIString).orElseThrow(AssertionError::new));

        OWLOntologyLoaderMetaData meta = manager.getNonnullOntologyFormat(o)
                .getOntologyLoaderMetaData().orElseThrow(AssertionError::new);
        print(meta);

        Assert.assertEquals(RDFOntologyHeaderStatus.PARSED_MULTIPLE_HEADERS, meta.getHeaderState());
        Assert.assertEquals(0, meta.getUnparsedTriples().count());
        Assert.assertEquals(1, meta.getGuessedDeclarations().size());
        Assert.assertEquals(o.asGraphModel().size(), meta.getTripleCount());
    }

    @Test
    public void testUnparsableTriples() throws OWLOntologyCreationException {
        class Empty extends Transform {
            private final Triple[] unparseable;

            private Empty(Graph graph, Triple... triples) {
                super(graph);
                unparseable = triples;
            }

            @Override
            public void perform() throws TransformException {
                // nothing
            }

            @Override
            public Stream<Triple> uncertainTriples() {
                return Arrays.stream(unparseable);
            }
        }
        Triple t1 = Triple.create(NodeFactory.createBlankNode(), NodeFactory.createURI("a"),
                NodeFactory.createLiteral("v1"));
        Triple t2 = Triple.create(NodeFactory.createURI("b"), NodeFactory.createURI("c"),
                NodeFactory.createLiteral("v2"));
        OntologyManager manager = OntManagers.createONT();
        GraphTransformers.Store transformers = manager.getOntologyConfigurator().getGraphTransformers()
                .add(g -> new Empty(g, t1)).add(g -> new Empty(g, t2));
        manager.getOntologyConfigurator().setGraphTransformers(transformers);

        OWLOntologyDocumentSource src = ReadWriteUtils.getFileDocumentSource("/ontapi/pizza.ttl", OntFormat.TURTLE);
        OntologyModel o = manager.loadOntologyFromOntologyDocument(src);
        ReadWriteUtils.print(o);

        OWLOntologyLoaderMetaData meta = manager.getNonnullOntologyFormat(o)
                .getOntologyLoaderMetaData().orElseThrow(AssertionError::new);
        print(meta);

        Assert.assertEquals(RDFOntologyHeaderStatus.PARSED_ONE_HEADER, meta.getHeaderState());
        Assert.assertEquals(2, meta.getUnparsedTriples().count());
        Assert.assertEquals(0, meta.getGuessedDeclarations().size());
        Assert.assertEquals(o.asGraphModel().size(), meta.getTripleCount());
    }

    private static void print(OWLOntologyLoaderMetaData meta) {
        meta.getGuessedDeclarations().asMap().forEach((x, y) -> LOGGER.debug("Guessed: {} => {}", x, y));
        meta.getUnparsedTriples().forEach(t -> LOGGER.debug("Unparsed: {}", t));
    }
}
