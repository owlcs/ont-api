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
import com.github.owlcs.ontapi.jena.utils.Iterators;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.jena.vocabulary.SWRL;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.ontapi.testutils.SpinModels;
import com.github.owlcs.ontapi.transforms.GraphTransformers;
import com.github.owlcs.ontapi.transforms.OWLCommonTransform;
import com.github.owlcs.ontapi.transforms.OWLIDTransform;
import com.github.owlcs.ontapi.transforms.SWRLTransform;
import com.github.owlcs.ontapi.transforms.Transform;
import com.github.owlcs.ontapi.transforms.TransformException;
import com.github.owlcs.ontapi.transforms.TransformationModel;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.util.FileManager;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.io.OWLOntologyLoaderMetaData;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.HasIRI;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * to test {@link GraphTransformers}
 * <p>
 * Created by @szuev on 30.10.2016.
 */
public class GraphTransformersTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphTransformersTest.class);

    private static final Set<IRI> ADDITIONAL_BUILT_IN_ENTITIES = Stream.of(
            RDF.List, RDFS.Resource, RDF.Property, RDFS.Class, OWL.Ontology)
            .map(Resource::getURI)
            .map(IRI::create)
            .collect(Collectors.toUnmodifiableSet());

    @Test
    public void testLoadSpinLibraryWithoutTransforms() throws Exception {
        OntologyManager m = OntManagers.createManager();
        // Setup spin manager:
        GraphTransformers transformers = m.getOntologyConfigurator().getGraphTransformers()
                .setFilter(g -> {
                    String uri = Graphs.getOntologyIRI(g);
                    return uri == null || Arrays.stream(SpinModels.values()).map(SpinModels::uri).noneMatch(uri::equals);
                });
        m.getOntologyConfigurator().setGraphTransformers(transformers)
                .disableWebAccess();
        SpinModels.addMappings(m);
        //noinspection deprecation
        SpinModels.addMappings(FileManager.get());
        Ontology spin = m.loadOntology(SpinModels.SPINMAPL.getIRI());
        Assertions.assertEquals(10, m.ontologies().count());
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
            Assertions.assertEquals(0, d.getGuessedDeclarations().size(), "Wrong guessed declarations count for " + uri);
        });
        Assertions.assertNotNull(m.getOntology(SpinModels.SPL.getIRI()));

        Assertions.assertEquals(1098, spin.axioms(Imports.INCLUDED).count());
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
        Assertions.assertNotNull(o);
        Assertions.assertEquals(count, o.asGraphModel().localStatements().count());
    }

    @Test // todo: old test - seems to be irrelevant now. delete or fix
    public void testSPSignature() throws Exception {
        // global transforms:
        GraphTransformers.get().addLast(g -> {
            LOGGER.debug("Finish transformation ({}).", Graphs.getName(g));
            return Stream.empty();
        });

        OWLOntologyManager manager = OntManagers.createOWLAPIImplManager();
        OWLOntologyManager testManager = OntManagers.createOWLAPIImplManager();

        OntModel jenaSP = OntModelFactory.createModel(
                GraphTransformers.convert(OWLIOUtils.loadResourceAsModel("/etc/sp.ttl", Lang.TURTLE).getGraph()),
                OntModelConfig.ONT_PERSONALITY_LAX);
        OWLOntology owlSP = load(manager, "/etc/sp.ttl");
        LOGGER.debug("SP(Jena): ");
        OWLIOUtils.print(jenaSP);
        signatureTest(owlSP, jenaSP);
        OWLOntology testSP = testManager.loadOntologyFromOntologyDocument(OWLIOUtils.asInputStream(jenaSP, OntFormat.TURTLE));
        LOGGER.debug("SP signature:");
        testSP.signature().forEach(entity -> LOGGER.debug(String.format("%s(%s)", entity, entity.getEntityType())));

        // WARNING:
        // There is a difference in behaviour between ONT-API and OWL-API,
        // Example: spin:violationDetail is ObjectProperty and spin:labelTemplate is DataProperty due to rdfs:range.
        // But OWL-API treats them as AnnotationProperty only.
        // spin:Modules is treated as NamedIndividual by OWL-API and as Class by ONT-API.
        UnionGraph spinGraph = new UnionGraph(OWLIOUtils.loadResourceAsModel("/etc/spin.ttl", Lang.TURTLE).getGraph());
        spinGraph.addGraph(jenaSP.getBaseGraph());
        OntModel jenaSPIN = OntModelFactory.createModel(GraphTransformers.convert(spinGraph));
        OWLOntology owlSPIN = load(manager, "/etc/spin.ttl");
        LOGGER.debug("SPIN(Jena): ");
        OWLIOUtils.print(jenaSPIN);
        LOGGER.debug("SPIN(OWL): ");
        OWLIOUtils.print(owlSPIN);

        //testSignature(owlSPIN, jenaSPIN);
        OWLOntology testSPIN = testManager.loadOntologyFromOntologyDocument(OWLIOUtils.asInputStream(jenaSPIN, OntFormat.TURTLE));
        LOGGER.debug("SPIN signature:");
        testSPIN.signature().forEach(entity -> LOGGER.debug(String.format("%s(%s)", entity, entity.getEntityType())));
        LOGGER.debug("Origin SPIN signature:");
        owlSPIN.signature().forEach(e -> LOGGER.debug(String.format("%s(%s)", e, e.getEntityType())));

        UnionGraph splGraph = new UnionGraph(OWLIOUtils.loadResourceAsModel("/etc/spl.spin.ttl", Lang.TURTLE).getGraph());
        splGraph.addGraph(jenaSPIN.getBaseGraph());
        OntModel jenaSPL = OntModelFactory.createModel(GraphTransformers.convert(splGraph));
        LOGGER.debug("SPL-SPIN(Jena): ");
        OWLIOUtils.print(jenaSPL);
        LOGGER.debug("SPL-SPIN(Jena) All entities: ");
        jenaSPL.ontEntities().map(String::valueOf).forEach(LOGGER::debug);
    }

    @Test
    public void testSWRLVocabulary() throws Exception {
        IRI iri = IRI.create("http://www.w3.org/2003/11/swrl");
        IRI file = IRI.create(OWLIOUtils.getResourceURI("/ontapi/swrl.owl.rdf"));
        OntologyManager m = OntManagers.createManager();
        m.getOntologyConfigurator().setPersonality(OntModelConfig.ONT_PERSONALITY_LAX);
        Ontology o = m.loadOntology(file);
        Assertions.assertTrue(m.contains(iri));

        OWLIOUtils.print(o);
        o.axioms().map(String::valueOf).forEach(LOGGER::debug);

        Assertions.assertNull(o.asGraphModel().getOntClass(RDFS.Literal));
        Assertions.assertEquals(1, o.asGraphModel().ontObjects(OntClass.DataAllValuesFrom.class).count());
        Assertions.assertNotNull(o.asGraphModel().getDataProperty(SWRL.argument2), SWRL.argument2 + " should be data property");
        Assertions.assertNotNull(o.asGraphModel().getObjectProperty(SWRL.argument2), SWRL.argument2 + " should be object property");
    }

    @Test
    public void testGeneralFunctionality() {
        int num = 6; //5;
        Transform first = Transform.Factory.create(OWLIDTransform.class);
        Transform last = Transform.Factory.create(SWRLTransform.class); //OWLDeclarationTransform.class;

        GraphTransformers store = GraphTransformers.get();
        Assertions.assertSame(store, GraphTransformers.get());
        Assertions.assertEquals(num, store.transforms().peek(x -> LOGGER.debug("Store:::{}", x.id())).count());
        Assertions.assertTrue(store.get(first.id()).isPresent());
        List<String> ids = store.transforms().map(Transform::id).collect(Collectors.toList());
        Assertions.assertEquals(first.id(), ids.get(0));
        Assertions.assertEquals(last.id(), ids.get(ids.size() - 1));
        Transform maker = Transform.Factory.create("a", g -> new TransformationModel(g) {
            @Override
            public void perform() throws TransformException {
                getQueryModel().createResource("some", RDFS.Datatype);
            }
        });
        GraphTransformers store1 = store.insertAfter(first.id(), maker);
        store1.transforms().map(Transform::id).forEach(x -> LOGGER.debug("Store1:::{}", x));
        Assertions.assertNotEquals(store, store1);
        Assertions.assertEquals(num, store.transforms().count());
        List<String> ids1 = store1.transforms().map(Transform::id).collect(Collectors.toList());
        Assertions.assertEquals(num + 1, ids1.size());
        Assertions.assertEquals("a", ids1.get(1));

        GraphTransformers store2 = store1.removeFirst();
        Assertions.assertNotEquals(store1, store2);
        Assertions.assertEquals(num + 1, store1.transforms().count());
        Assertions.assertEquals(num, store2.transforms().count());

        GraphTransformers store3 = store1.remove(OWLCommonTransform.class.getSimpleName());
        Assertions.assertNotEquals(store1, store3);
        Assertions.assertEquals(num + 1, store1.transforms().count());
        Assertions.assertEquals(num, store3.transforms().peek(x -> LOGGER.debug("Store3:::{}", x.id())).count());

        GraphTransformers store4 = store3.removeLast().removeLast().removeLast()
                .addLast(g -> {
                    Model m = ModelFactory.createModelForGraph(g);
                    Resource s = Iterators.asStream(m
                                    .listResourcesWithProperty(RDF.type, OWL.Ontology))
                            .findFirst()
                            .orElseThrow(AssertionError::new);
                    s.addProperty(OWL.versionIRI, m.createResource("http://ver"));
                    return Stream.empty();
                })
                .setFilter(Graph::isEmpty);

        Assertions.assertEquals(num - 2, store4.transforms().peek(s -> LOGGER.debug("Store4::::{}", s.id())).count());
        Model m1 = ModelFactory.createDefaultModel();
        OntModel m2 = OntModelFactory.createModel().setID("http://x").getModel();
        store4.transform(m2.getGraph());
        store4.transform(m1.getGraph());

        // test m1:
        OWLIOUtils.print(m1);
        Assertions.assertEquals(3, m1.listStatements().toList().size());
        Assertions.assertTrue(m1.contains(m1.getResource("some"), RDF.type, RDFS.Datatype));
        Assertions.assertTrue(m1.contains(null, OWL.versionIRI, m1.getResource("http://ver")));
        Assertions.assertEquals(1, m1.listResourcesWithProperty(RDF.type, OWL.Ontology).filterKeep(RDFNode::isAnon).toSet().size());

        // test m2 (should be unchanged):
        OWLIOUtils.print(m2);
        Assertions.assertEquals("http://x", m2.getID().getURI());
        Assertions.assertEquals(1, m2.statements().count());
    }

    @Test
    public void testTransformsOnLoad() throws OWLOntologyCreationException {
        List<String> iris = Arrays.asList("http://a", "http://b", "http://c", "http://d");
        OntologyManager m = OntManagers.createManager();

        Set<String> processed = new HashSet<>();
        GraphTransformers st = m.getOntologyConfigurator().getGraphTransformers()
                .addFirst(new Transform() {
                    @Override
                    public Stream<Triple> apply(Graph g) {
                        String n = Graphs.getName(g);
                        LOGGER.debug("Test {} on {}", id(), n);
                        Assertions.assertTrue(processed.add(n), "Already processed: " + n);
                        return Stream.empty();
                    }

                    @Override
                    public String id() {
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
        String c_txt = OWLIOUtils.asString(c, OntFormat.TURTLE);
        OntModel d = OntModelFactory.createModel();
        d.setID(iris.get(3)).addImport(c.getID().getURI());
        String d_txt = OWLIOUtils.asString(d, OntFormat.TURTLE);

        m.loadOntologyFromOntologyDocument(OWLIOUtils.getStringDocumentSource(c_txt, OntFormat.TURTLE));
        Assertions.assertEquals(3, m.ontologies().count());

        m.loadOntologyFromOntologyDocument(OWLIOUtils.getStringDocumentSource(d_txt, OntFormat.TURTLE));
        Assertions.assertEquals(4, m.ontologies().count());

        iris.forEach(i -> Assertions.assertNotNull(m.getGraphModel(i)));
    }

    @SuppressWarnings("unused")
    private static void signatureTest(OWLOntology owl, OntModel jena) {
        List<String> expectedClasses = owlToList(owl.classesInSignature(Imports.INCLUDED));
        List<String> actualClasses = jenaToList(jena.classes());
        Assertions.assertTrue(actualClasses.containsAll(expectedClasses));

        List<String> expectedAnnotationProperties = owlToList(owl.annotationPropertiesInSignature(Imports.INCLUDED));//, RDFS.comment, RDFS.label, OWL2.deprecated, OWL.versionInfo);
        List<String> actualAnnotationProperties = jenaToList(jena.annotationProperties());
        List<String> expectedDataProperties = owlToList(owl.dataPropertiesInSignature(Imports.INCLUDED));
        List<String> actualDataProperties = jenaToList(jena.dataProperties());
        List<String> expectedObjectProperties = owlToList(owl.objectPropertiesInSignature(Imports.INCLUDED));
        List<String> actualObjectProperties = jenaToList(jena.objectProperties());
        LOGGER.debug("Actual AnnotationProperties: " + actualAnnotationProperties);
        LOGGER.debug("Actual ObjectProperties: " + actualObjectProperties);
        LOGGER.debug("Actual DataProperties: " + actualDataProperties);

        Assertions.assertEquals(expectedAnnotationProperties, actualAnnotationProperties);

        List<String> expectedDatatypes = owlToList(owl.datatypesInSignature(Imports.INCLUDED));
        List<String> actualDatatypes = jenaToList(jena.datatypes());
        Assertions.assertEquals(expectedDatatypes, actualDatatypes);

        List<String> expectedIndividuals = owlToList(owl.individualsInSignature(Imports.INCLUDED));
        List<String> actualIndividuals = jenaToList(jena.namedIndividuals());
        Assertions.assertEquals(expectedIndividuals, actualIndividuals);
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

    private static OWLOntology load(OWLOntologyManager manager, String resource) throws Exception {
        return manager.loadOntology(IRI.create(OWLIOUtils.getResourceURI(resource)));
    }
}
