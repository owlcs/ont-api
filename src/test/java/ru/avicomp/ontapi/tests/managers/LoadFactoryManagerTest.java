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

package ru.avicomp.ontapi.tests.managers;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSourceBase;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.*;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.transforms.OWLRecursiveTransform;
import ru.avicomp.ontapi.utils.FileMap;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To test loading mechanisms from {@link OntologyFactoryImpl}
 * <p>
 * Created by @szuev on 16.01.2018.
 */
@SuppressWarnings("JavaDoc")
public class LoadFactoryManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFactoryManagerTest.class);

    /**
     * for <a href='https://github.com/avicomp/ont-api/issues/4'>issue#4</a>
     *
     * @throws OWLOntologyCreationException
     */
    @Test
    public void testLoadWrongDuplicate() throws OWLOntologyCreationException {
        IRI a = IRI.create(ReadWriteUtils.getResourceURI("load-test-a.owl"));
        IRI b = IRI.create(ReadWriteUtils.getResourceURI("load-test-b.ttl"));

        OWLOntologyManager m = OntManagers.createONT();
        OWLOntology o = m.loadOntologyFromOntologyDocument(a);
        Assert.assertEquals(1, m.ontologies().count());
        Assert.assertNotNull(o.getOWLOntologyManager());
        String comment = getComment(o);
        LOGGER.debug("Ontology comment '{}'", comment);

        try {
            m.loadOntologyFromOntologyDocument(b);
        } catch (UnparsableOntologyException e) {
            LOGGER.info("Exception: {}", e.getMessage().substring(0, Math.min(100, e.getMessage().length())));
        }
        // Note: the different with OWL-API (5.1.4) : no ontologies inside manager. Believe it is a bug of OWL-API.
        Assert.assertEquals("Wrong count", 1, m.ontologies().count());
        Assert.assertNotNull("No manager", o.getOWLOntologyManager());
        Assert.assertSame(o, m.ontologies().findFirst().orElseThrow(AssertionError::new));
        Assert.assertEquals(comment, getComment(o));
    }


    @Test
    public void testLoadNotJenaHierarchy() throws Exception {
        String a = "http://spinrdf.org/sp";
        String b = "http://spinrdf.org/spif";
        Map<String, Path> map = new HashMap<>();
        map.put(a, ReadWriteUtils.getResourcePath("omn", "sp.omn"));
        map.put(b, ReadWriteUtils.getResourcePath("omn", "spif.omn"));
        map.put("http://spinrdf.org/spin", ReadWriteUtils.getResourcePath("omn", "spin.omn"));
        map.put("http://spinrdf.org/spl", ReadWriteUtils.getResourcePath("omn", "spl.spin.omn"));

        OntologyManager manager = OntManagers.createONT();
        for (String uri : map.keySet()) {
            manager.getIRIMappers().add(new SimpleIRIMapper(IRI.create(uri), IRI.create(map.get(uri).toUri())));
        }
        manager.getOntologyConfigurator()
                .setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE))
                .setPerformTransformation(false);

        manager.loadOntologyFromOntologyDocument(new FileDocumentSource(map.get(a).toFile(), OntFormat.MANCHESTER_SYNTAX.createOwlFormat()));
        Assert.assertEquals("Should be one ontology inside manager", 1, manager.ontologies().count());
        manager.loadOntologyFromOntologyDocument(new FileDocumentSource(map.get(b).toFile(), OntFormat.MANCHESTER_SYNTAX.createOwlFormat()));
        Assert.assertEquals("Wrong num of onts", 4, manager.ontologies().count());
    }

    @Test(expected = OntologyFactoryImpl.OWLTransformException.class)
    public void tesLoadWrongRDFSyntax() throws OWLOntologyCreationException {
        // wrong []-List
        OntManagers.createONT().loadOntology(IRI.create(ReadWriteUtils.getResourceURI("wrong.rdf")));
    }

    @Test(expected = UnloadableImportException.class)
    public void testLoadNotJenaHierarchyWithDisabledWeb() throws Exception {
        Path path = ReadWriteUtils.getResourcePath("/owlapi/obo", "annotated_import.obo");
        LOGGER.debug("File {}", path);
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE));
        OWLOntologyID id = m.loadOntology(IRI.create(path.toUri())).getOntologyID();
        LOGGER.error("The ontology {} is loaded.", id);
    }

    @Test
    public void testLoadRecursiveGraphWithTransform() throws OWLOntologyCreationException {
        IRI iri = IRI.create(ReadWriteUtils.getResourceURI("recursive-graph.ttl"));
        LOGGER.debug("The file: {}", iri);
        OntologyManager m = OntManagers.createONT();
        if (!m.getOntologyConfigurator().getGraphTransformers().contains(OWLRecursiveTransform.class.getName())) {
            m.getOntologyConfigurator().setGraphTransformers(m.getOntologyConfigurator()
                    .getGraphTransformers().addFirst(OWLRecursiveTransform::new));
        }
        OntologyModel o = m.loadOntology(iri);
        ReadWriteUtils.print(o.asGraphModel());
        o.axioms().forEach(a -> LOGGER.debug("{}", a));
        Assert.assertEquals("Wrong axioms count", 5, o.getAxiomCount());
        Assert.assertEquals(1, o.axioms(AxiomType.SUBCLASS_OF).count());
    }

    /**
     * Moved from {@link CommonManagerTest}
     *
     * @throws Exception
     */
    @Test
    public void testLoadCorruptedOntology() throws Exception {
        OWLOntologyManager m = OntManagers.createONT();

        IRI amyIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-amy");
        IRI sueIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-sue");
        IRI coreIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/core");

        IRI amyFile = IRI.create(ReadWriteUtils.getResourceURI("owlapi/importNoOntology/subject-amy.ttl"));
        IRI sueFile = IRI.create(ReadWriteUtils.getResourceURI("owlapi/importNoOntology/subject-sue.ttl"));
        IRI wrongFile = IRI.create(ReadWriteUtils.getResourceURI("wrong-core.ttl"));

        m.getIRIMappers().add(FileMap.create(amyIRI, amyFile));
        m.getIRIMappers().add(FileMap.create(sueIRI, sueFile));
        m.getIRIMappers().add(FileMap.create(coreIRI, wrongFile));
        m.getIRIMappers().forEach(x -> LOGGER.info("{}", x));

        LOGGER.info("-================-");
        try {
            Assert.fail("No exception while loading " + m.loadOntology(coreIRI));
        } catch (OWLRuntimeException e) {
            if (e instanceof UnloadableImportException) {
                LOGGER.info("Exception", e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }
        Assert.assertEquals("There are some ontologies inside manager", 0, m.ontologies().count());
    }

    /**
     * Moved from {@link CommonManagerTest}
     *
     * @throws Exception
     */
    @Test
    public void testLoadDifferentStrategies() throws Exception {
        IRI sp = IRI.create("http://spinrdf.org/sp");
        IRI spin = IRI.create("http://spinrdf.org/spin");
        OWLOntologyIRIMapper mapSp = new SimpleIRIMapper(sp, IRI.create(ReadWriteUtils.getResourcePath("etc", "sp.ttl").toFile()));
        OWLOntologyIRIMapper mapSpin = new SimpleIRIMapper(spin, IRI.create(ReadWriteUtils.getResourcePath("etc", "spin.ttl").toFile()));

        LOGGER.info("1) Test load some web ontology for a case when only file scheme is allowed.");
        OntologyManager m1 = OntManagers.createONT();
        OntLoaderConfiguration conf = m1.getOntologyLoaderConfiguration().setSupportedSchemes(Stream.of(OntConfig.DefaultScheme.FILE).collect(Collectors.toList()));
        m1.setOntologyLoaderConfiguration(conf);
        try {
            Assert.fail("No exception while loading " + m1.loadOntology(sp));
        } catch (OWLOntologyCreationException e) {
            if (e instanceof OntologyFactoryImpl.ConfigMismatchException) {
                LOGGER.info("Exception", e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }

        LOGGER.info("2) Add mapping and try to load again.");
        m1.getIRIMappers().add(mapSp);
        m1.loadOntology(sp);
        Assert.assertEquals("Should be single ontology inside", 1, m1.ontologies().count());

        LOGGER.info("3) Load new web-ontology which depends on this existing one.");
        try {
            Assert.fail("No exception while loading " + m1.loadOntology(spin));
        } catch (OWLOntologyCreationException e) {
            if (e instanceof OntologyFactoryImpl.ConfigMismatchException) {
                LOGGER.info("Exception", e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }
        Assert.assertEquals("Should be single ontology inside", 1, m1.ontologies().count());

        LOGGER.info("4) Try to load new web-ontology with file mapping which depends on some other web-ontology.");
        OntologyManager m2 = OntManagers.createONT();
        m2.setOntologyLoaderConfiguration(conf);
        m2.getIRIMappers().add(mapSpin);
        try {
            Assert.fail("No exception while loading " + m2.loadOntology(spin));
        } catch (OWLRuntimeException e) {
            if (e instanceof UnloadableImportException) {
                LOGGER.info("Exception", e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }
        Assert.assertEquals("Manager should be empty", 0, m2.ontologies().count());

        LOGGER.info("5) Set ignore broken imports and try to load again.");
        m2.setOntologyLoaderConfiguration(conf.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
        m2.loadOntology(spin);
        Assert.assertEquals("Should be only single ontology inside.", 1, m2.ontologies().count());

        LOGGER.info("6) Set ignore some import and load ontology with dependencies.");
        OntologyManager m3 = OntManagers.createONT();
        m3.getIRIMappers().add(mapSp);
        m3.getIRIMappers().add(mapSpin);
        m3.setOntologyLoaderConfiguration(conf.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.THROW_EXCEPTION).addIgnoredImport(sp));
        m3.loadOntology(spin);
        Assert.assertEquals("Should be only single ontology inside.", 1, m3.ontologies().count());

        LOGGER.info("7) Default way to load.");
        OntologyManager m4 = OntManagers.createONT();
        m4.getIRIMappers().add(mapSp);
        m4.getIRIMappers().add(mapSpin);
        m4.loadOntology(spin);
        Assert.assertEquals("Should be two ontologies inside.", 2, m4.ontologies().count());

        LOGGER.info("8) Test loading with MissingOntologyHeaderStrategy = true/false");
        OWLOntologyManager m5 = OntManagers.createONT();
        Assert.assertEquals("Incorrect default settings", MissingOntologyHeaderStrategy.INCLUDE_GRAPH, m5.getOntologyLoaderConfiguration().getMissingOntologyHeaderStrategy());
        loadLoopedOntologyFamily(m5);
        Assert.assertEquals("Wrong ontologies count.", 3, m5.ontologies().count());
        OWLOntologyManager m6 = OntManagers.createONT();
        m6.setOntologyLoaderConfiguration(m6.getOntologyLoaderConfiguration().setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy.IMPORT_GRAPH));
        loadLoopedOntologyFamily(m6);
        Assert.assertEquals("Wrong ontologies.", 4, m6.ontologies().count());
        // todo: it would be nice to validate the result ontologie
    }

    @Test
    public void testMissedImports() throws OWLOntologyCreationException {
        // create data:
        OntologyManager m = OntManagers.createONT();
        OntologyModel a = m.createOntology(IRI.create("urn:a"));
        OntologyModel b = m.createOntology(IRI.create("urn:b"));
        a.asGraphModel().createOntEntity(OntClass.class, "A");
        b.asGraphModel().createOntEntity(OntClass.class, "B");
        b.asGraphModel().addImport(a.asGraphModel());
        // check data:
        checkForMissedImportsTest(b);
        String sA = ReadWriteUtils.toString(a, OntFormat.TURTLE);
        String sB = ReadWriteUtils.toString(b, OntFormat.TURTLE);
        // direct:
        OntologyManager m2 = OntManagers.createONT();
        m2.loadOntologyFromOntologyDocument(new StringDocumentSource(sA));
        OntologyModel b2 = m2.loadOntologyFromOntologyDocument(new StringDocumentSource(sB));
        checkForMissedImportsTest(b2);

        // reverse through stream:
        OntologyManager m3 = OntManagers.createONT();
        OntologyModel b3 = m3.loadOntologyFromOntologyDocument(new StringDocumentSource(sB),
                m3.getOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
        m3.loadOntologyFromOntologyDocument(new StringDocumentSource(sA));
        checkForMissedImportsTest(b3);

        // reverse through graph
        OntologyManager m4 = OntManagers.createONT();
        OntologyModel b4 = m4.addOntology(b.asGraphModel().getBaseGraph(),
                m4.getOntologyLoaderConfiguration().setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
        m4.addOntology(a.asGraphModel().getBaseGraph());
        checkForMissedImportsTest(b4);
    }

    @Test
    public void testDocumentSourceMapping() throws OWLOntologyCreationException {
        // create data:
        OntGraphModel a = OntModelFactory.createModel();
        a.setID("urn:a");
        a.setNsPrefixes(OntModelFactory.STANDARD);
        OntGraphModel b = OntModelFactory.createModel();
        b.setID("urn:b");
        b.setNsPrefixes(OntModelFactory.STANDARD);
        a.createOntEntity(OntClass.class, "urn:a#A");
        b.createOntEntity(OntClass.class, "urn:b#B");
        b.addImport(a);
        Map<String, String> data = new HashMap<>();
        data.put("store://a", ReadWriteUtils.toString(a, OntFormat.TURTLE));
        data.put("store://b", ReadWriteUtils.toString(b, OntFormat.TURTLE));

        data.forEach((iri, txt) -> LOGGER.debug("Document iri: <{}>\nData:\n{}", iri, txt));

        OWLOntologyIRIMapper iriMapper = iri -> {
            switch (iri.toString()) {
                case "urn:a":
                    return IRI.create("store://a");
                case "urn:b":
                    return IRI.create("store://b");
            }
            return null;
        };
        OntologyManager.DocumentSourceMapping docMapper = id -> id.getOntologyIRI()
                .map(iriMapper::getDocumentIRI)
                .map(doc -> new OWLOntologyDocumentSourceBase(doc, OntFormat.TURTLE.createOwlFormat(), null) {

                    @Override
                    public Optional<InputStream> getInputStream() { // every time create a new InputStream
                        return Optional.of(ReadWriteUtils.toInputStream(data.get(doc.getIRIString())));
                    }
                })
                .orElse(null);

        OntologyManager m = OntManagers.createONT();
        //m.getOntologyConfigurator().setSupportedSchemes(Collections.singletonList(() -> "store"));
        //m.setIRIMappers(Collections.singleton(iriMapper));
        m.addDocumentSourceMapper(docMapper);
        OntologyModel o = m.loadOntologyFromOntologyDocument(docMapper.map(new OWLOntologyID(IRI.create("urn:b"))));
        Assert.assertNotNull(o);
        Assert.assertEquals(2, m.ontologies().count());
    }

    @Test
    public void testAddGraphWithVersionIRI() {
        OntGraphModel a = OntModelFactory.createModel();
        OntGraphModel b = OntModelFactory.createModel();
        b.setID("http://b").setVersionIRI("http://ver1");
        a.addImport(b);

        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().disableWebAccess();
        m.addOntology(a.getGraph());
        Assert.assertEquals(2, m.ontologies().count());
    }

    private static void checkForMissedImportsTest(OntologyModel b) {
        checkForMissedImportsTest((OWLOntology) b);
        Assert.assertEquals(1, b.asGraphModel().imports().count());
    }

    private static void checkForMissedImportsTest(OWLOntology b) {
        Assert.assertEquals(1, b.imports().count());
        Assert.assertEquals(1, b.axioms(Imports.EXCLUDED).filter(a -> AxiomType.DECLARATION.equals(a.getAxiomType())).count());
        Assert.assertEquals(2, b.axioms(Imports.INCLUDED).filter(a -> AxiomType.DECLARATION.equals(a.getAxiomType())).count());
    }

    private static void loadLoopedOntologyFamily(OWLOntologyManager m) throws Exception {
        IRI amyIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-amy");
        IRI sueIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-sue");
        IRI bobIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-bob");
        IRI coreIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/core");

        IRI amyFile = IRI.create(ReadWriteUtils.getResourceURI("owlapi/importNoOntology/subject-amy.ttl"));
        IRI sueFile = IRI.create(ReadWriteUtils.getResourceURI("owlapi/importNoOntology/subject-sue.ttl"));
        IRI bobFile = IRI.create(ReadWriteUtils.getResourceURI("owlapi/importNoOntology/subject-bob.ttl"));
        IRI coreFile = IRI.create(ReadWriteUtils.getResourceURI("core.ttl"));

        m.getIRIMappers().add(FileMap.create(amyIRI, amyFile));
        m.getIRIMappers().add(FileMap.create(bobIRI, bobFile));
        m.getIRIMappers().add(FileMap.create(sueIRI, sueFile));
        m.getIRIMappers().add(FileMap.create(coreIRI, coreFile));
        m.getIRIMappers().forEach(x -> LOGGER.info("{}", x));

        LOGGER.info("-================-");
        OWLOntology bob = m.loadOntology(bobIRI);
        ReadWriteUtils.print(bob);
        LOGGER.debug("[ONT]");
        m.ontologies().forEach(x -> LOGGER.info("{}", x));
    }

    private static String getComment(OWLOntology o) {
        return o.annotations().map(OWLAnnotation::getValue)
                .map(OWLAnnotationValue::asLiteral)
                .map(x -> x.orElseThrow(() -> new AssertionError("Empty comment")))
                .map(OWLLiteral::getLiteral)
                .findFirst().orElseThrow(() -> new AssertionError("No comment."));
    }

}
