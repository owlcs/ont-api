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

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.UnionGraph;
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntEntity;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.utils.Graphs;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.jena.vocabulary.SWRL;
import com.github.owlcs.ontapi.transforms.*;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import com.github.owlcs.ontapi.utils.SP;
import com.github.owlcs.ontapi.utils.SpinModels;
import com.github.owlcs.ontapi.utils.SpinTransform;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDFS;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.OWLOntologyLoaderMetaData;
import org.semanticweb.owlapi.io.RDFOntologyHeaderStatus;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * to test {@link GraphTransformers}
 * <p>
 * Created by @szuev on 30.10.2016.
 */
public class GraphTransformersTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphTransformersTest.class);

    private static final Set<IRI> ADDITIONAL_BUILT_IN_ENTITIES = Stream.of(RDF.List, RDFS.Resource, RDF.Property, RDFS.Class, OWL.Ontology)
            .map(Resource::getURI)
            .map(IRI::create)
            .collect(Iter.toUnmodifiableSet());

    @Test
    public void testLoadSpinLibraryWithTransformation() throws Exception {
        int axiomsCountSPINMAPL = 902; //895;//902;//856;
        int axiomsCountTotal = 7701; //7685; //7796; //7625;

        OntologyManager m = OntManagers.createONT();
        // Setup spin manager:
        m.getOntologyConfigurator()
                .setGraphTransformers(GraphTransformers.getTransformers().addFirst(SpinTransform::new))
                .setPersonality(SpinModels.ONT_SPIN_PERSONALITY)
                .disableWebAccess();
        SpinModels.addMappings(m);
        SpinModels.addMappings(FileManager.get());

        IRI iri = SpinModels.SPINMAPL.getIRI();
        Ontology spinmapl = m.loadOntology(iri);
        String actualTree = Graphs.importsTreeAsString(spinmapl.asGraphModel().getGraph());
        LOGGER.debug("Tree:\n{}", actualTree);
        Assert.assertEquals(27, actualTree.split("\n").length);

        Assert.assertEquals(10, m.ontologies().count());
        m.ontologies().forEach(o -> {
            IRI uri = o.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new);
            OWLDocumentFormat f = Optional.ofNullable(m.getOntologyFormat(o)).orElseThrow(AssertionError::new);
            OWLOntologyLoaderMetaData d = f.getOntologyLoaderMetaData().orElseThrow(AssertionError::new);
            LOGGER.debug("Ontology: {}. MetaData: {}, {}, {}, {}",
                    o,
                    d.getHeaderState(),
                    d.getTripleCount(),
                    d.getUnparsedTriples().count(),
                    d.getGuessedDeclarations().size());
            Assert.assertEquals(RDFOntologyHeaderStatus.PARSED_ONE_HEADER, d.getHeaderState());
            if (SpinModels.SMF_BASE.getIRI().equals(uri)) {
                return;
            }
            Assert.assertNotEquals("Wrong guessed declarations count for " + uri, 0, d.getGuessedDeclarations().size());
        });

        Ontology spl = m.getOntology(SpinModels.SPL.getIRI());
        Assert.assertNotNull("Can't find SPL", spl);

        //String splAsString = ReadWriteUtils.toString(spl.asGraphModel(), OntFormat.TURTLE);
        //LOGGER.debug(splAsString);

        Assert.assertEquals("Incorrect spinmapl axioms count", axiomsCountSPINMAPL, spinmapl.getAxiomCount());
        Assert.assertEquals("Incorrect total axioms count", axiomsCountTotal, spinmapl.axioms(Imports.INCLUDED).count());

        OWLAnnotationProperty spText = m.getOWLDataFactory().getOWLAnnotationProperty(IRI.create(SP.text.getURI()));
        OWLAnnotationAssertionAxiom axiom = spl.axioms(AxiomType.ANNOTATION_ASSERTION).filter(a -> Objects.equals(a.getProperty(), spText))
                .findAny().orElseThrow(() -> new AssertionError("Can't find any sp:text annotation assertion"));
        Optional<OWLLiteral> literal = axiom.getValue().asLiteral();
        Optional<OWLAnonymousIndividual> individual = axiom.getSubject().asAnonymousIndividual();
        Assert.assertTrue("No literal", literal.isPresent());
        Assert.assertTrue("No individual", individual.isPresent());
        LOGGER.debug("Axioms related to query <{}>", literal.get().getLiteral().replace("\n", " "));
        spl.referencingAxioms(individual.get()).map(String::valueOf).forEach(LOGGER::debug);
    }

    @Test
    public void testLoadSpinLibraryWithoutTransforms() throws Exception {
        OntologyManager m = OntManagers.createONT();
        // Setup spin manager:
        GraphTransformers.Store transformers = m.getOntologyConfigurator().getGraphTransformers()
                .setFilter(g -> {
                    String uri = Graphs.getURI(g);
                    return uri == null || Arrays.stream(SpinModels.values()).map(SpinModels::uri).noneMatch(uri::equals);
                });
        m.getOntologyConfigurator().setGraphTransformers(transformers)
                .disableWebAccess();
        SpinModels.addMappings(m);
        SpinModels.addMappings(FileManager.get());
        Ontology spin = m.loadOntology(SpinModels.SPINMAPL.getIRI());
        Assert.assertEquals(10, m.ontologies().count());
        m.ontologies().forEach(o -> {
            IRI uri = o.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new);
            OWLDocumentFormat f = Optional.ofNullable(m.getOntologyFormat(o)).orElseThrow(AssertionError::new);
            OWLOntologyLoaderMetaData d = f.getOntologyLoaderMetaData().orElseThrow(AssertionError::new);
            LOGGER.debug("Ontology: {}. MetaData: {}, {}, {}, {}",
                    o,
                    d.getHeaderState(),
                    d.getTripleCount(),
                    d.getUnparsedTriples().count(),
                    d.getGuessedDeclarations().size());
            Assert.assertEquals("Wrong guessed declarations count for " + uri, 0, d.getGuessedDeclarations().size());
        });
        Assert.assertNotNull(m.getOntology(SpinModels.SPL.getIRI()));

        Assert.assertEquals(1098, spin.axioms(Imports.INCLUDED).count());
        spin.axioms(Imports.INCLUDED)
                .collect(Collectors.groupingBy(OWLAxiom::getAxiomType))
                .forEach((type, axioms) -> {
                    LOGGER.debug("AXIOMS[{}]::{}", type, axioms.size());
                    if (!AxiomType.ANNOTATION_ASSERTION.equals(type))
                        axioms.forEach(x -> LOGGER.debug("AXIOM::{}", x));
                });
        assertGraphSize(m, SpinModels.SPINMAPL, 1922);
        assertGraphSize(m, SpinModels.SMF, 1223);
        assertGraphSize(m, SpinModels.SP, 523);
        assertGraphSize(m, SpinModels.SPL, 3636);
        assertGraphSize(m, SpinModels.SPIN, 375);
        assertGraphSize(m, SpinModels.SPIF, 959);
        assertGraphSize(m, SpinModels.SPINMAP, 802);
    }

    private static void assertGraphSize(OntologyManager m, SpinModels ont, long count) {
        Ontology o = m.getOntology(ont.getIRI());
        Assert.assertNotNull(o);
        Assert.assertEquals(count, o.asGraphModel().localStatements().count());
    }

    @Test // todo: old test - seems to be irrelevant now. delete or fix
    public void testSPSignature() throws Exception {
        // global transforms:
        GraphTransformers.getTransformers().add(g -> new Transform(g) {
            @Override
            public void perform() {
                LOGGER.debug("Finish transformation ({}).", Graphs.getName(g));
            }
        });

        OWLOntologyManager manager = OntManagers.createOWL();
        OWLOntologyManager testManager = OntManagers.createOWL();

        OntModel jenaSP = OntModelFactory.createModel(
                GraphTransformers.convert(ReadWriteUtils.loadResourceTTLFile("etc/sp.ttl").getGraph()),
                OntModelConfig.ONT_PERSONALITY_LAX);
        OWLOntology owlSP = load(manager, "etc/sp.ttl");
        LOGGER.debug("SP(Jena): ");
        ReadWriteUtils.print(jenaSP);
        //LOGGER.info("SP(OWL): ");
        //ReadWriteUtils.print(owlSP);
        signatureTest(owlSP, jenaSP);
        OWLOntology testSP = testManager.loadOntologyFromOntologyDocument(ReadWriteUtils.toInputStream(jenaSP, OntFormat.TURTLE));
        LOGGER.debug("SP signature:");
        testSP.signature().forEach(entity -> LOGGER.debug(String.format("%s(%s)", entity, entity.getEntityType())));

        // WARNING:
        // There is a difference in behaviour between ONT-API and OWL-API,
        // Example: spin:violationDetail is ObjectProperty and spin:labelTemplate is DataProperty due to rdfs:range.
        // But OWL-API treats them as AnnotationProperty only.
        // spin:Modules is treated as NamedIndividual by OWL-API and as Class by ONT-API.
        UnionGraph spinGraph = new UnionGraph(ReadWriteUtils.loadResourceTTLFile("etc/spin.ttl").getGraph());
        spinGraph.addGraph(jenaSP.getBaseGraph());
        OntModel jenaSPIN = OntModelFactory.createModel(GraphTransformers.convert(spinGraph));
        OWLOntology owlSPIN = load(manager, "etc/spin.ttl");
        LOGGER.debug("SPIN(Jena): ");
        ReadWriteUtils.print(jenaSPIN);
        LOGGER.debug("SPIN(OWL): ");
        ReadWriteUtils.print(owlSPIN);

        //testSignature(owlSPIN, jenaSPIN);
        OWLOntology testSPIN = testManager.loadOntologyFromOntologyDocument(ReadWriteUtils.toInputStream(jenaSPIN, OntFormat.TURTLE));
        LOGGER.debug("SPIN signature:");
        testSPIN.signature().forEach(entity -> LOGGER.debug(String.format("%s(%s)", entity, entity.getEntityType())));
        LOGGER.debug("Origin SPIN signature:");
        owlSPIN.signature().forEach(e -> LOGGER.debug(String.format("%s(%s)", e, e.getEntityType())));

        UnionGraph splGraph = new UnionGraph(ReadWriteUtils.loadResourceTTLFile("etc/spl.spin.ttl").getGraph());
        splGraph.addGraph(jenaSPIN.getBaseGraph());
        OntModel jenaSPL = OntModelFactory.createModel(GraphTransformers.convert(splGraph));
        LOGGER.debug("SPL-SPIN(Jena): ");
        ReadWriteUtils.print(jenaSPL);
        LOGGER.debug("SPL-SPIN(Jena) All entities: ");
        jenaSPL.ontEntities().map(String::valueOf).forEach(LOGGER::debug);
    }

    @Test
    public void testSWRLVocabulary() throws Exception {
        IRI iri = IRI.create("http://www.w3.org/2003/11/swrl");
        IRI file = IRI.create(ReadWriteUtils.getResourceURI("ontapi/swrl.owl.rdf"));
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setPersonality(OntModelConfig.ONT_PERSONALITY_LAX);
        Ontology o = m.loadOntology(file);
        Assert.assertTrue("No ontology", m.contains(iri));

        ReadWriteUtils.print(o);
        o.axioms().map(String::valueOf).forEach(LOGGER::debug);

        Assert.assertNull("rdfs:Literal should not be class", o.asGraphModel().getOntClass(RDFS.Literal));
        Assert.assertEquals("Should be DataAllValuesFrom", 1, o.asGraphModel().ontObjects(OntClass.DataAllValuesFrom.class).count());
        Assert.assertNotNull(SWRL.argument2 + " should be data property", o.asGraphModel().getDataProperty(SWRL.argument2));
        Assert.assertNotNull(SWRL.argument2 + " should be object property", o.asGraphModel().getObjectProperty(SWRL.argument2));
    }

    @Test
    public void testGeneralFunctionality() {
        int num = 6; //5;
        Class<? extends Transform> first = OWLIDTransform.class;
        Class<? extends Transform> last = SWRLTransform.class; //OWLDeclarationTransform.class;

        GraphTransformers.Store store = GraphTransformers.getTransformers();
        Assert.assertSame(store, GraphTransformers.getTransformers());
        Assert.assertEquals(num, store.ids().peek(x -> LOGGER.debug("Store:::{}", x)).count());
        Assert.assertTrue(store.get(first.getName()).isPresent());
        List<String> ids = store.ids().collect(Collectors.toList());
        Assert.assertEquals(first.getName(), ids.get(0));
        Assert.assertEquals(last.getName(), ids.get(ids.size() - 1));
        GraphTransformers.Maker maker = GraphTransformers.Maker.create("a", g -> new Transform(g) {
            @Override
            public void perform() throws TransformException {
                getQueryModel().createResource("some", RDFS.Datatype);
            }

            @Override
            public String name() {
                return "Test #1";
            }
        });
        GraphTransformers.Store store1 = store.insertAfter(first.getName(), maker);
        store1.ids().forEach(x -> LOGGER.debug("Store1:::{}", x));
        Assert.assertNotEquals(store, store1);
        Assert.assertEquals(num, store.ids().count());
        List<String> ids1 = store1.ids().collect(Collectors.toList());
        Assert.assertEquals(num + 1, ids1.size());
        Assert.assertEquals("a", ids1.get(1));

        GraphTransformers.Store store2 = store1.removeFirst();
        Assert.assertNotEquals(store1, store2);
        Assert.assertEquals(num + 1, store1.ids().count());
        Assert.assertEquals(num, store2.ids().count());

        GraphTransformers.Store store3 = store1.remove(OWLCommonTransform.class.getName());
        Assert.assertNotEquals(store1, store3);
        Assert.assertEquals(num + 1, store1.ids().count());
        Assert.assertEquals(num, store3.ids().peek(x -> LOGGER.debug("Store3:::{}", x)).count());

        GraphTransformers.Store store4 = store3.removeLast().removeLast().removeLast()
                .addLast(GraphTransformers.Maker.create("b", graph -> true, g -> {
                    Model m = ModelFactory.createModelForGraph(g);
                    Resource s = Iter.asStream(m
                            .listResourcesWithProperty(RDF.type, OWL.Ontology))
                            .findFirst()
                            .orElseThrow(AssertionError::new);
                    s.addProperty(OWL.versionIRI, m.createResource("http://ver"));
                })).setFilter(Graph::isEmpty);

        Assert.assertEquals(num - 2, store4.ids().peek(s -> LOGGER.debug("Store4::::{}", s)).count());
        Model m1 = ModelFactory.createDefaultModel();
        OntModel m2 = OntModelFactory.createModel().setID("http://x").getModel();
        store4.transform(m2.getGraph());
        store4.transform(m1.getGraph());

        // test m1:
        ReadWriteUtils.print(m1);
        Assert.assertEquals(3, m1.listStatements().toList().size());
        Assert.assertTrue(m1.contains(m1.getResource("some"), RDF.type, RDFS.Datatype));
        Assert.assertTrue(m1.contains(null, OWL.versionIRI, m1.getResource("http://ver")));
        Assert.assertEquals(1, m1.listResourcesWithProperty(RDF.type, OWL.Ontology).filterKeep(RDFNode::isAnon).toSet().size());

        // test m2 (should be unchanged):
        ReadWriteUtils.print(m2);
        Assert.assertEquals("http://x", m2.getID().getURI());
        Assert.assertEquals(1, m2.statements().count());
    }

    @Test
    public void testTransformsOnLoad() throws OWLOntologyCreationException {
        List<String> iris = Arrays.asList("http://a", "http://b", "http://c", "http://d");
        OntologyManager m = OntManagers.createONT();

        Set<String> processed = new HashSet<>();
        GraphTransformers.Store st = m.getOntologyConfigurator().getGraphTransformers().addFirst(g -> new Transform(g) {
            @Override
            public void perform() throws TransformException {
                String graph = Graphs.getName(g);
                LOGGER.debug("Test {} on {}", name(), graph);
                Assert.assertTrue("Already processed: " + graph, processed.add(graph));
            }

            @Override
            public String name() {
                return "Test Checker";
            }
        });
        m.getOntologyConfigurator().setGraphTransformers(st);

        iris.stream().limit(2).map(IRI::create).forEach(iri -> {
            LOGGER.debug("Create {}", iri);
            processed.add(Graphs.getName(m.createOntology(iri).asGraphModel().getGraph()));
        });

        OntModel c = OntModelFactory.createModel();
        c.setID(iris.get(2)).addImport(iris.get(0)).addImport(iris.get(1));
        String c_txt = ReadWriteUtils.toString(c, OntFormat.TURTLE);
        OntModel d = OntModelFactory.createModel();
        d.setID(iris.get(3)).addImport(c.getID().getURI());
        String d_txt = ReadWriteUtils.toString(d, OntFormat.TURTLE);

        m.loadOntologyFromOntologyDocument(ReadWriteUtils.getStringDocumentSource(c_txt, OntFormat.TURTLE));
        Assert.assertEquals(3, m.ontologies().count());

        m.loadOntologyFromOntologyDocument(ReadWriteUtils.getStringDocumentSource(d_txt, OntFormat.TURTLE));
        Assert.assertEquals(4, m.ontologies().count());

        iris.forEach(i -> Assert.assertNotNull(m.getGraphModel(i)));
    }

    private static void signatureTest(OWLOntology owl, OntModel jena) {
        List<String> expectedClasses = owlToList(owl.classesInSignature(Imports.INCLUDED));
        List<String> actualClasses = jenaToList(jena.classes());
        Assert.assertTrue("Classes", actualClasses.containsAll(expectedClasses));

        List<String> expectedAnnotationProperties = owlToList(owl.annotationPropertiesInSignature(Imports.INCLUDED));//, RDFS.comment, RDFS.label, OWL2.deprecated, OWL.versionInfo);
        List<String> actualAnnotationProperties = jenaToList(jena.annotationProperties());
        List<String> expectedDataProperties = owlToList(owl.dataPropertiesInSignature(Imports.INCLUDED));
        List<String> actualDataProperties = jenaToList(jena.dataProperties());
        List<String> expectedObjectProperties = owlToList(owl.objectPropertiesInSignature(Imports.INCLUDED));
        List<String> actualObjectProperties = jenaToList(jena.objectProperties());
        LOGGER.debug("Actual AnnotationProperties: " + actualAnnotationProperties);
        LOGGER.debug("Actual ObjectProperties: " + actualObjectProperties);
        LOGGER.debug("Actual DataProperties: " + actualDataProperties);

        Assert.assertThat("AnnotationProperties", actualAnnotationProperties, IsEqual.equalTo(expectedAnnotationProperties));
        //Assert.assertThat("DataProperties", actualDataProperties, IsEqual.equalTo(expectedDataProperties));
        //Assert.assertThat("ObjectProperties", actualObjectProperties, IsEqual.equalTo(expectedObjectProperties));

        List<String> expectedDatatypes = owlToList(owl.datatypesInSignature(Imports.INCLUDED));
        List<String> actualDatatypes = jenaToList(jena.datatypes());
        Assert.assertThat("Datatypes", actualDatatypes, IsEqual.equalTo(expectedDatatypes));

        List<String> expectedIndividuals = owlToList(owl.individualsInSignature(Imports.INCLUDED));
        List<String> actualIndividuals = jenaToList(jena.namedIndividuals());
        Assert.assertThat("Individuals", actualIndividuals, IsEqual.equalTo(expectedIndividuals));
    }

    private static boolean isNotBuiltIn(OWLEntity entity) {
        return !entity.isBuiltIn() && !ADDITIONAL_BUILT_IN_ENTITIES.contains(entity.getIRI());
    }

    private static Stream<String> owlToStream(Stream<? extends OWLEntity> entities) {
        return entities.filter(GraphTransformersTest::isNotBuiltIn).distinct().map(HasIRI::getIRI).map(IRI::getIRIString).sorted();
    }

    private static List<String> owlToList(Stream<? extends OWLEntity> entities) {
        return owlToStream(entities).collect(Collectors.toList());
    }

    private static Stream<String> jenaToStream(Stream<? extends OntEntity> entities) {
        return entities.map(Resource::getURI).sorted();
    }

    private static List<String> jenaToList(Stream<? extends OntEntity> entities) {
        return jenaToStream(entities).sorted().collect(Collectors.toList());
    }

    private static OWLOntology load(OWLOntologyManager manager, String file) throws Exception {
        return manager.loadOntology(IRI.create(ReadWriteUtils.getResourceURI(file)));
    }
}
