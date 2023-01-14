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

import com.github.owlcs.ontapi.CommonOntologies;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.OntologyManagerImpl;
import com.github.owlcs.ontapi.OntologyModelImpl;
import com.github.owlcs.ontapi.RWLockedGraph;
import com.github.owlcs.ontapi.config.OntLoaderConfiguration;
import com.github.owlcs.ontapi.internal.AxiomTranslator;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.UnionGraph;
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntEntity;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.utils.Graphs;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.ontapi.testutils.OntIRI;
import com.github.owlcs.ontapi.testutils.SpinModels;
import com.github.owlcs.ontapi.transforms.GraphTransformers;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AsOWLClass;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To test core ({@link OntManagers}) (+ serialization)
 * <p>
 * Created by szuev on 22.12.2016.
 */
@SuppressWarnings("WeakerAccess")
public class CommonManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonManagerTest.class);

    private static void fixAfterSerialization(OWLOntologyManager origin, OWLOntologyManager copy) {
        if (copy instanceof OntologyManager) {
            return;
        }
        // OWL-API 5.0.5:
        copy.setOntologyWriterConfiguration(origin.getOntologyWriterConfiguration());
    }

    private static void debugManager(OWLOntologyManager m) {
        m.ontologies().forEach(o -> {
            LOGGER.debug("<{}>:\n", o.getOntologyID());
            OWLIOUtils.print(o);
        });
    }

    public static void compareManagersTest(OWLOntologyManager expected, OWLOntologyManager actual) {
        Assertions.assertEquals(expected.ontologies().count(), actual.ontologies().count());
        actual.ontologies().forEach(test -> {
            OWLOntologyID id = test.getOntologyID();
            LOGGER.debug("Test <{}>", id);
            OWLOntology origin = expected.getOntology(id);
            Assertions.assertNotNull(origin, "Can't find init ontology with id " + id);
            AxiomType.AXIOM_TYPES.forEach(t -> {
                Set<OWLAxiom> expectedAxiom = origin.axioms(t).collect(Collectors.toSet());
                Set<OWLAxiom> actualAxiom = test.axioms(t).collect(Collectors.toSet());
                Assertions.assertEquals(expectedAxiom, actualAxiom,
                        String.format("Incorrect axioms for type <%s> and %s (expected=%d, actual=%d)",
                                t, id, expectedAxiom.size(), actualAxiom.size()));
            });

            Set<OWLImportsDeclaration> expectedImports = origin.importsDeclarations().collect(Collectors.toSet());
            Set<OWLImportsDeclaration> actualImports = test.importsDeclarations().collect(Collectors.toSet());
            Assertions.assertEquals(expectedImports, actualImports, "Incorrect imports for " + id);
            OWLDocumentFormat expectedFormat = origin.getFormat();
            OWLDocumentFormat actualFormat = test.getFormat();
            Assertions.assertEquals(expectedFormat, actualFormat, "Incorrect formats for " + id);
            Map<String, String> expectedPrefixes = expectedFormat != null &&
                    expectedFormat.isPrefixOWLDocumentFormat() ?
                    expectedFormat.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap() : null;
            Map<String, String> actualPrefixes = actualFormat != null && actualFormat.isPrefixOWLDocumentFormat() ?
                    actualFormat.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap() : null;
            Assertions.assertEquals(expectedPrefixes, actualPrefixes, "Incorrect prefixes for " + id);
            checkCompareEntities(origin, test);
        });
    }

    public static void checkCompareEntities(OWLOntology expectedOnt, OWLOntology actualOnt) {
        for (Imports i : Imports.values()) {
            Set<OWLEntity> actualEntities = actualOnt.signature(i).collect(Collectors.toSet());
            Set<OWLEntity> expectedEntities = expectedOnt.signature(i).collect(Collectors.toSet());
            LOGGER.debug("OWL entities: {}", actualEntities);
            Assertions.assertEquals(expectedEntities, actualEntities);
        }
        if (actualOnt instanceof Ontology && expectedOnt instanceof Ontology) {  // ont
            OntModel a = ((Ontology) actualOnt).asGraphModel();
            OntModel e = ((Ontology) expectedOnt).asGraphModel();
            List<OntEntity> actualEntities = a.ontEntities().collect(Collectors.toList());
            List<OntEntity> expectedEntities = e.ontEntities().collect(Collectors.toList());
            LOGGER.debug("ONT entities: {}", actualEntities);
            Assertions.assertEquals(expectedEntities, actualEntities);
        }
    }

    public static Stream<Arguments> managersData() {
        return Stream.of(getManagerData("createManager", false)
                , getManagerData("createConcurrentManager", true)
                , getManagerData("createDirectManager", false));
    }

    private static Arguments getManagerData(String method, boolean isConcurrent) {
        return Arguments.of(new Supplier<OntologyManager>() {
            @Override
            public String toString() {
                return "OntManagers::" + method;
            }

            @Override
            public OntologyManager get() {
                return createOntologyManager(method);
            }
        }, isConcurrent);
    }

    private static OntologyManager createOntologyManager(String method) {
        try {
            return (OntologyManager) OntManagers.class.getMethod(method).invoke(null);
        } catch (Exception e) {
            return Assertions.fail("Can't invoke '" + method + "'", e);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("managersData")
    public void testBasicsFunctionality(Supplier<OntologyManager> factory, boolean isConcurrent) throws Exception {
        final IRI fileIRI = IRI.create(OWLIOUtils.getResourceURI("/ontapi/test1.ttl"));
        final OWLOntologyID id = OntIRI.create("http://dummy").toOwlOntologyID();

        Assertions.assertNotSame(factory.get(), factory.get());

        OntologyManagerImpl m1 = (OntologyManagerImpl) factory.get();
        Assertions.assertEquals(isConcurrent, m1.isConcurrent());

        Ontology ont1 = m1.loadOntology(fileIRI);
        Ontology ont2 = m1.createOntology(id);
        Assertions.assertEquals(2, m1.ontologies().count());
        Stream.of(ont1, ont2).forEach(o -> {
            Class<?> t = o.getClass();
            if (isConcurrent) {
                Assertions.assertNotEquals(OntologyModelImpl.class, t);
                Assertions.assertEquals(OntologyModelImpl.Concurrent.class, t);
            } else {
                Assertions.assertEquals(OntologyModelImpl.class, t);
                Assertions.assertNotEquals(OntologyModelImpl.Concurrent.class, t);
            }
        });
    }

    @Test
    public void testSetLoaderConfigs() {
        OntologyManager m1 = OntManagers.createManager();
        OntologyManager m2 = OntManagers.createManager();
        OntLoaderConfiguration conf1 = m1.getOntologyLoaderConfiguration();
        conf1.setPersonality(OntModelConfig.ONT_PERSONALITY_LAX);
        OntLoaderConfiguration conf2 = m2.getOntologyLoaderConfiguration();
        conf2.setPersonality(OntModelConfig.ONT_PERSONALITY_STRICT);
        Assertions.assertEquals(conf1, conf2);
        Assertions.assertEquals(conf1.getPersonality(), conf2.getPersonality());
        m1.setOntologyLoaderConfiguration(conf1.setPersonality(OntModelConfig.ONT_PERSONALITY_LAX));
        m2.setOntologyLoaderConfiguration(conf1.setPersonality(OntModelConfig.ONT_PERSONALITY_STRICT));
        Assertions.assertNotEquals(m1.getOntologyLoaderConfiguration().getPersonality(),
                m2.getOntologyLoaderConfiguration().getPersonality());

        boolean doTransformation = !conf1.isPerformTransformation();
        m1.getOntologyLoaderConfiguration().setPerformTransformation(doTransformation);
        Assertions.assertNotEquals(doTransformation,
                m1.getOntologyLoaderConfiguration().isPerformTransformation());
        m1.setOntologyLoaderConfiguration(conf2.setPerformTransformation(doTransformation));
        Assertions.assertEquals(doTransformation, m1.getOntologyLoaderConfiguration().isPerformTransformation());

        GraphTransformers store = new GraphTransformers().addLast(g -> Stream.empty());
        OntLoaderConfiguration conf3 = m1.getOntologyLoaderConfiguration().setGraphTransformers(store);
        Assertions.assertNotEquals(store, m1.getOntologyLoaderConfiguration().getGraphTransformers());
        m1.setOntologyLoaderConfiguration(conf3);
        Assertions.assertEquals(store, m1.getOntologyLoaderConfiguration().getGraphTransformers());
    }

    @Test
    public void testConcurrentManagerValidateStructure() throws Exception {
        OWLOntologyManager m = OntManagers.createConcurrentManager();
        OWLOntology o1 = m.createOntology();
        OWLOntology o2 = m.loadOntology(IRI.create(OWLIOUtils.getResourceURI("/ontapi/test1.ttl")));
        Assertions.assertEquals(2, m.ontologies().count());

        ReadWriteLock managerLock = ((OntologyManagerImpl) m).getLock();
        Assertions.assertNotNull(managerLock);
        checkConcurrentOntologyLock(o1, managerLock);
        checkConcurrentOntologyLock(o2, managerLock);
    }

    @Test
    public void testConcurrentManagerModifyAxioms() throws Exception {
        OWLOntologyManager m = OntManagers.createConcurrentManager();
        OWLOntology o1 = m.createOntology();
        OWLOntology o2 = m.loadOntology(IRI.create(OWLIOUtils.getResourceURI("/ontapi/test1.ttl")));
        Assertions.assertEquals(2, m.ontologies().count());

        o1.axioms().forEach(a -> LOGGER.debug("{}", a));
        ((Ontology) o1).asGraphModel().createOntClass("urn:c1").createIndividual("urn:i");
        OWLDataFactory df = m.getOWLDataFactory();
        o1.add(df.getOWLAnnotationAssertionAxiom(df.getOWLAnnotationProperty(IRI.create("urn:ap")),
                IRI.create("urn:c1"), df.getOWLLiteral("test", "e")));
        Assertions.assertEquals(4, o1.getAxiomCount());
        ((Ontology) o1).clearCache();
        Assertions.assertEquals(5, o1.getAxiomCount());

        o2.axioms().forEach(a -> LOGGER.debug("{}", a));
        Assertions.assertEquals(12, o2.getAxiomCount());
        ((Ontology) o2).clearCache();
        Assertions.assertEquals(12, o2.getAxiomCount());
    }

    private void checkConcurrentOntologyLock(OWLOntology o, ReadWriteLock managerLock) {
        Assertions.assertTrue(o instanceof OntologyModelImpl.Concurrent);
        Assertions.assertEquals(managerLock, ((OntologyModelImpl.Concurrent) o).getLock());
        Assertions.assertTrue(((Ontology) o).asGraphModel().getBaseGraph() instanceof RWLockedGraph);
        Assertions.assertEquals(managerLock, ((RWLockedGraph) ((Ontology) o).asGraphModel().getBaseGraph()).lock());
    }

    @ParameterizedTest
    @ValueSource(strings = {"createManager", "createConcurrentManager"})
    public void testSerialization(String method) throws Exception {
        OntologyManager origin = createOntologyManager(method);

        setUpManager(origin);
        debugManager(origin);

        LOGGER.debug("|====================|");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(out);
        stream.writeObject(origin);
        stream.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream inStream = new ObjectInputStream(in);
        OWLOntologyManager copy = (OWLOntologyManager) inStream.readObject();

        Assertions.assertEquals(((OntologyManagerImpl) origin).isConcurrent(), ((OntologyManagerImpl) copy).isConcurrent());

        fixAfterSerialization(origin, copy);
        debugManager(copy);
        compareManagersTest(origin, copy);

        editManagerTest(origin, (OntologyManager) copy);
    }

    private void setUpManager(OWLOntologyManager m) throws OWLOntologyCreationException {
        OWLDataFactory f = m.getOWLDataFactory();

        OWLOntology a1 = m.createOntology();
        a1.add(f.getOWLDeclarationAxiom(f.getOWLClass(IRI.create("urn:iri.com#Class1"))));
        OWLOntology a2 = m.createOntology();
        a2.add(f.getOWLDeclarationAxiom(f.getOWLClass(IRI.create("urn:iri.com#Class2"))));

        OWLOntology i1 = m.createOntology(IRI.create("urn:iri.com#1"));
        i1.add(f.getOWLDeclarationAxiom(f.getOWLClass(IRI.create("urn:iri.com#Class3"))));
        OWLOntology i2 = m.createOntology(IRI.create("urn:iri.com#2"));
        i2.add(f.getOWLDeclarationAxiom(f.getOWLClass(IRI.create("urn:iri.com#Class4"))));
        OWLOntology i3 = m.createOntology(IRI.create("urn:iri.com#3"));
        i3.add(f.getOWLDeclarationAxiom(f.getOWLClass(IRI.create("urn:iri.com#Class5"))));

        i1.applyChange(new AddImport(i1, f.getOWLImportsDeclaration(i2.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new))));
        i2.applyChange(new AddImport(i2, f.getOWLImportsDeclaration(i3.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new))));
        i1.applyChange(new AddImport(i1, f.getOWLImportsDeclaration(IRI.create("urn:some.import"))));
        a1.applyChange(new AddImport(a1, f.getOWLImportsDeclaration(i1.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new))));

        Objects.requireNonNull(i2.getFormat()).asPrefixOWLDocumentFormat().setPrefix("test", "urn:iri.com");
    }

    @SuppressWarnings("LambdaBodyCanBeCodeBlock")
    private void editManagerTest(OntologyManager origin, OntologyManager copy) {
        String uri = "urn:iri.com#1";
        OntModel o1 = origin.getGraphModel(uri);
        OntModel o2 = copy.getGraphModel(uri);

        List<OntClass.Named> classes1 = o1.classes().collect(Collectors.toList());
        // create two new classes inside original manager (in two models)
        o1.createOntClass("http://some/new#Class1");
        origin.getGraphModel("urn:iri.com#3").createOntClass("http://some/new#Class2");
        List<OntClass.Named> classes2 = o2.classes().collect(Collectors.toList());
        // check that in the second (copied) manager there is no changes:
        Assertions.assertEquals(classes1, classes2);

        // create two new classes inside copied manager
        Set<OntClass.Named> classes3 = o2.classes().collect(Collectors.toSet());
        OntClass.Named cl3 = o2.createOntClass("http://some/new#Class3");
        OntClass.Named cl4 = copy.getGraphModel("urn:iri.com#3").createOntClass("http://some/new#Class4");
        List<OntClass.Named> newClasses = Arrays.asList(cl3, cl4);
        classes3.addAll(newClasses);
        Set<OntClass.Named> classes4 = o2.classes().collect(Collectors.toSet());
        Assertions.assertEquals(classes3, classes4);
        newClasses.forEach(c -> Assertions.assertFalse(o1.containsResource(c), "Found " + c + " inside original ontology"));
        Ontology ont = copy.getOntology(IRI.create(uri));
        Assertions.assertNotNull(ont);
        ONTObjectFactory df = AxiomTranslator.getObjectFactory(o2);
        List<OWLClass> newOWLClasses = newClasses.stream()
                .map(df::getClass)
                .map(ONTObject::getOWLObject)
                .map(AsOWLClass::asOWLClass).collect(Collectors.toList());
        LOGGER.debug("OWL-Classes: {}", newOWLClasses);
        //noinspection deprecation
        newOWLClasses.forEach(c -> Assertions.assertTrue(ont.containsReference(c, Imports.INCLUDED),
                "Can't find " + c + " inside copied ontology"));
    }

    @Test
    public void testPassingJenaOntModel() throws Exception {
        LOGGER.debug("Build MultiUnion graph using jena OntModel");
        OntModelSpec spec = OntModelSpec.OWL_DL_MEM;
        FileManager jenaFileManager = spec.getDocumentManager().getFileManager();
        SpinModels.addMappings(jenaFileManager);
        org.apache.jena.ontology.OntModel jenaModel = ModelFactory.createOntologyModel(spec);
        jenaModel.read(SpinModels.SPINMAPL.getIRI().getIRIString(), "ttl");

        LOGGER.debug("Load spin-rdf ontology family using file-iri-mappings");
        OntologyManager m1 = OntManagers.createManager();
        SpinModels.addMappings(m1);
        m1.loadOntologyFromOntologyDocument(SpinModels.SPINMAPL.getIRI());
        long expected = m1.ontologies().count();

        LOGGER.debug("Pass ready composite graph to the manager as-is");
        OntologyManager m2 = OntManagers.createManager();
        m2.addOntology(jenaModel.getGraph());
        long actual = m2.ontologies().count();

        Assertions.assertEquals(expected, actual);

        LOGGER.debug("Add several additional ontologies");
        m2.addOntology(OntModelFactory.createDefaultGraph());
        OntModel o2 = OntModelFactory.createModel();
        o2.setID("http://example.org/test");
        m2.addOntology(o2.getGraph());
        Assertions.assertEquals(expected + 2, m2.ontologies().count());
    }

    @ParameterizedTest
    @MethodSource("managersData")
    public void testPassingOntGraphModel(Supplier<OntologyManager> factory, boolean concurrency) {
        testPassingOntGraphModel(factory.get(), o -> checkOntology(o, concurrency));
    }

    @Test
    public void testDirectManagerReadAxioms() {
        CommonOntologies data = CommonOntologies.PEOPLE;
        long count = 412;
        long distinctCount = 409;

        Ontology o = (Ontology) data.fetch(OntManagers.createDirectManager());

        List<OWLAxiom> list = o.axioms().collect(Collectors.toList());
        Assertions.assertEquals(count, list.size());
        Assertions.assertEquals(distinctCount, new HashSet<>(list).size());
        Assertions.assertEquals(count, o.axioms().count());
        Assertions.assertEquals(distinctCount, o.axioms().distinct().count());
    }

    @Test
    public void testDirectManagerModifyAxioms() {
        CommonOntologies data = CommonOntologies.PEOPLE;
        String classURI = data.getNS() + "person";

        Ontology o = (Ontology) data.fetch(OntManagers.createDirectManager());

        OWLClass clazz = OntManagers.getDataFactory().getOWLClass(classURI);
        OWLDeclarationAxiom d = o.declarationAxioms(clazz).findFirst().orElseThrow(AssertionError::new);
        Assertions.assertTrue(o.containsAxiom(d));

        Assertions.assertThrows(OntApiException.ModificationDenied.class, () -> o.remove(d));

        OntClass c = o.asGraphModel().getOntClass(classURI);
        o.asGraphModel().removeOntObject(c);

        Assertions.assertFalse(o.containsAxiom(d));
        Assertions.assertFalse(o.declarationAxioms(clazz).findFirst().isPresent());

        Assertions.assertThrows(OntApiException.ModificationDenied.class, () -> o.add(d));

        o.asGraphModel().createOntClass(classURI);
        Assertions.assertTrue(o.containsAxiom(d));
        Assertions.assertEquals(d, o.declarationAxioms(clazz).findFirst().orElseThrow(AssertionError::new));
    }

    private void testPassingOntGraphModel(OntologyManager m, Consumer<OWLOntology> tester) {
        OntModel a = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD).setID("a").getModel();
        OntModel b = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD).setID("b").getModel();
        OntModel c = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD).setID("c").getModel();
        OntModel d = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD).setID("d").getModel();
        a.addImport(b.addImport(c).addImport(d));

        m.addOntology(a.getGraph());
        Assertions.assertEquals(4, m.ontologies().peek(tester).count());
    }

    private void checkOntology(OWLOntology ont, boolean isConcurrent) {
        OntModel m = ((Ontology) ont).asGraphModel();
        LOGGER.debug("Test {}", m);
        Assertions.assertNotNull(m);
        if (isConcurrent) {
            Assertions.assertTrue(m.getBaseGraph() instanceof RWLockedGraph);
            Assertions.assertTrue(Graphs.isGraphMem(((RWLockedGraph) m.getBaseGraph()).get()));
        } else {
            Assertions.assertTrue(Graphs.isGraphMem(m.getBaseGraph()));
        }
        Assertions.assertTrue(m.getGraph() instanceof UnionGraph);
        Assertions.assertEquals(m.getID().imports().count(), m.imports().count());
        UnionGraph u = (UnionGraph) m.getGraph();
        u.getUnderlying().listGraphs().forEachRemaining(g -> {
            if (g instanceof UnionGraph) {
                Assertions.assertTrue(Graphs.isGraphMem(((UnionGraph) g).getBaseGraph()));
            } else {
                Assertions.assertTrue(Graphs.isGraphMem(g));
            }
        });
        u.listBaseGraphs().forEachRemaining(g -> Assertions.assertFalse(g instanceof UnionGraph));
    }
}
