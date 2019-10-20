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

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.*;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.OntModels;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.XSD;
import ru.avicomp.ontapi.transforms.GraphTransformers;
import ru.avicomp.ontapi.transforms.OWLRecursiveTransform;
import ru.avicomp.ontapi.transforms.Transform;
import ru.avicomp.ontapi.transforms.TransformException;
import ru.avicomp.ontapi.utils.*;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To test loading mechanisms from {@link OntologyFactoryImpl}
 * including different {@link ru.avicomp.ontapi.config.LoadSettings lading settings}.
 * <p>
 * Created by @szuev on 16.01.2018.
 */
@SuppressWarnings("JavaDoc")
public class LoadFactoryManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFactoryManagerTest.class);

    private static void checkForMissedImportsTest(OntologyModel b) {
        checkForMissedImportsTest((OWLOntology) b);
        Assert.assertEquals(1, b.asGraphModel().imports().count());
    }

    private static void checkForMissedImportsTest(OWLOntology b) {
        Assert.assertEquals(1, b.imports().count());
        Assert.assertEquals(1, b.axioms(Imports.EXCLUDED)
                .filter(a -> AxiomType.DECLARATION.equals(a.getAxiomType())).count());
        Assert.assertEquals(2, b.axioms(Imports.INCLUDED)
                .filter(a -> AxiomType.DECLARATION.equals(a.getAxiomType())).count());
    }

    private static void loadLoopedOntologyFamily(OWLOntologyManager m) throws Exception {
        IRI amyIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-amy");
        IRI sueIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-sue");
        IRI bobIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-bob");
        IRI coreIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/core");

        IRI amyFile = IRI.create(ReadWriteUtils.getResourceURI("owlapi/importNoOntology/subject-amy.ttl"));
        IRI sueFile = IRI.create(ReadWriteUtils.getResourceURI("owlapi/importNoOntology/subject-sue.ttl"));
        IRI bobFile = IRI.create(ReadWriteUtils.getResourceURI("owlapi/importNoOntology/subject-bob.ttl"));
        IRI coreFile = IRI.create(ReadWriteUtils.getResourceURI("ontapi/core.ttl"));

        m.getIRIMappers().add(FileMap.create(amyIRI, amyFile));
        m.getIRIMappers().add(FileMap.create(bobIRI, bobFile));
        m.getIRIMappers().add(FileMap.create(sueIRI, sueFile));
        m.getIRIMappers().add(FileMap.create(coreIRI, coreFile));
        m.getIRIMappers().forEach(x -> LOGGER.debug("{}", x));

        LOGGER.debug("-================-");
        OWLOntology bob = m.loadOntology(bobIRI);
        ReadWriteUtils.print(bob);
        LOGGER.debug("[ONT]");
        m.ontologies().forEach(x -> LOGGER.debug("{}", x));
    }

    private static String getOWLComment(OWLOntology o) {
        return o.annotations().map(OWLAnnotation::getValue)
                .map(OWLAnnotationValue::asLiteral)
                .map(x -> x.orElseThrow(() -> new AssertionError("Empty comment")))
                .map(OWLLiteral::getLiteral)
                .findFirst().orElseThrow(() -> new AssertionError("No comment."));
    }

    private static OntologyManager createManagerWithOWLAPIOntologyFactory() {
        DataFactory df = OntManagers.DEFAULT_PROFILE.dataFactory();
        OntologyCreator builder = OntManagers.DEFAULT_PROFILE.createOntologyBuilder();
        OntologyFactory of = new OWLFactoryWrapper().asOntologyFactory(builder);
        return OntManagers.DEFAULT_PROFILE.createManager(df, of, null);
    }

    @Test
    public void testOntologyAlreadyExistsException() throws Exception {
        Path p = ReadWriteUtils.getResourcePath("ontapi", "pizza.ttl");
        OWLOntologyDocumentSource src = new FileDocumentSource(p.toFile(), OntFormat.TURTLE.createOwlFormat());
        OntologyManager m = OntManagers.createONT();
        m.loadOntologyFromOntologyDocument(src);
        Assert.assertEquals(1, m.ontologies().count());
        try {
            m.loadOntologyFromOntologyDocument(src); // in OWL-API-impl (5.1.9) there is no exception
            Assert.fail("Possible to load the same ontology twice");
        } catch (OWLOntologyAlreadyExistsException oae) {
            LOGGER.debug("Expected: '{}'", oae.getMessage());
        }
        Assert.assertEquals(1, m.ontologies().count());
    }

    @Test
    public void testEmptyOntologyDefaultPrefixes() {
        OWLDocumentFormat f = OntManagers.createONT().createOntology().getFormat();
        Assert.assertNotNull(f);
        Map<String, String> map = f.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap();
        Assert.assertEquals(4, map.size());
        Assert.assertEquals(XSD.NS, map.get("xsd:"));
        Assert.assertEquals(RDFS.getURI(), map.get("rdfs:"));
        Assert.assertEquals(RDF.getURI(), map.get("rdf:"));
        Assert.assertEquals(OWL.NS, map.get("owl:"));
    }

    @Test
    public void testPrefixesRoundTrips() throws Exception {
        URI uri = ReadWriteUtils.getResourcePath("ontapi", "foaf.rdf").toUri();
        Path p = Paths.get(uri);
        OWLOntologyManager m = OntManagers.createONT();
        OWLOntologyDocumentSource src = new FileDocumentSource(p.toFile(), OntFormat.RDF_XML.createOwlFormat());
        OWLOntology o = m.loadOntologyFromOntologyDocument(src);
        OWLDocumentFormat f = m.getOntologyFormat(o);
        Assert.assertNotNull(f);
        Assert.assertEquals(5, f.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().size());
        OWLDocumentFormat f1 = OntFormat.TURTLE.createOwlFormat();
        // 4 default:
        Assert.assertEquals(4, f1.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().size());

        // modify:
        f1.asPrefixOWLDocumentFormat().clear();
        f1.asPrefixOWLDocumentFormat().setPrefix("owl", "http://www.w3.org/2002/07/owl#");
        f1.asPrefixOWLDocumentFormat().setPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
        f1.asPrefixOWLDocumentFormat().setPrefix("a", "http://xmlns.com/foaf/0.1/");
        f1.asPrefixOWLDocumentFormat().setPrefix("b", "http://www.w3.org/2003/06/sw-vocab-status/ns#");
        f1.asPrefixOWLDocumentFormat().setPrefix("c", "http://schema.org/");
        f1.asPrefixOWLDocumentFormat().setPrefix("d", "http://purl.org/dc/elements/1.1/");
        f1.asPrefixOWLDocumentFormat().setPrefix("e", "http://www.w3.org/2003/01/geo/wgs84_pos#");
        f1.asPrefixOWLDocumentFormat().setPrefix("f", "http://www.w3.org/2000/10/swap/pim/contact#");
        f1.asPrefixOWLDocumentFormat().setPrefix("g", "http://purl.org/dc/terms/");
        f1.asPrefixOWLDocumentFormat().setPrefix("h", "http://xmlns.com/wot/0.1/");
        f1.asPrefixOWLDocumentFormat().setPrefix("i", "http://www.w3.org/2004/02/skos/core#");

        // round-trip turtle
        String txt1 = ReadWriteUtils.toString(o, f1);
        LOGGER.debug(txt1);
        int c = StringUtils.countMatches(txt1, "@prefix");
        Assert.assertEquals(11, c);
        OWLOntologyDocumentSource src1 = new StringInputStreamDocumentSource(txt1, f1);
        OWLDocumentFormat f2 = OntManagers.createConcurrentONT().loadOntologyFromOntologyDocument(src1).getFormat();
        Assert.assertNotNull(f2);
        Assert.assertEquals(c, f2.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().size());

        // check original not changed
        OWLDocumentFormat f3 = m.getOntologyFormat(o);
        Assert.assertNotNull(f3);
        Assert.assertEquals(5, f3.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().size());

        // round-trip manchester:
        OWLDocumentFormat f4 = OntFormat.MANCHESTER_SYNTAX.createOwlFormat();
        // 4 default:
        Assert.assertEquals(4, f4.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().size());
        f4.asPrefixOWLDocumentFormat().setPrefixManager(f1.asPrefixOWLDocumentFormat());
        Assert.assertEquals(11, f4.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().size());
        String txt4 = ReadWriteUtils.toString(o, f4);
        LOGGER.debug(txt4);
        OWLOntologyDocumentSource src4 = new StringInputStreamDocumentSource(txt1, f1);
        OWLDocumentFormat f5 = OntManagers.createConcurrentONT().loadOntologyFromOntologyDocument(src4).getFormat();
        Assert.assertNotNull(f5);
        Assert.assertTrue(f2.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().entrySet()
                .containsAll(f1.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().entrySet()));
    }

    /**
     * For <a href='https://github.com/avicomp/ont-api/issues/47'>issue#47</a>
     *
     * @throws OWLOntologyCreationException
     */
    @Test
    public void testLoadUnmodifiableGraph() throws OWLOntologyCreationException {
        OntologyManager m = OntManagers.createONT();
        OntGraphModel b = OntModelFactory.createModel().setID("http://b").getModel();
        OntGraphModel a = OntModelFactory.createModel().setID("http://a").getModel().addImport(b);

        String str = ReadWriteUtils.toString(a, OntFormat.TURTLE);
        LOGGER.debug("{}", str);

        UnmodifiableGraph g = new UnmodifiableGraph(b.getGraph());
        m.addOntology(g);

        Assert.assertTrue(m.models().findFirst()
                .orElseThrow(AssertionError::new).getBaseGraph() instanceof UnmodifiableGraph);
        m.loadOntologyFromOntologyDocument(ReadWriteUtils.getStringDocumentSource(str, OntFormat.TURTLE));
        Assert.assertEquals(2, m.ontologies().count());
        Assert.assertNotNull(m.getGraphModel("http://b"));
        Assert.assertNotNull(m.getGraphModel("http://a"));
    }

    /**
     * For <a href='https://github.com/avicomp/ont-api/issues/4'>issue#4</a>
     *
     * @throws OWLOntologyCreationException
     */
    @Test
    public void testLoadWrongDuplicate() throws OWLOntologyCreationException {
        IRI a = IRI.create(ReadWriteUtils.getResourceURI("ontapi/load-test-a.owl"));
        IRI b = IRI.create(ReadWriteUtils.getResourceURI("ontapi/load-test-b.ttl"));

        OWLOntologyManager m = OntManagers.createONT();
        OWLOntology o = m.loadOntologyFromOntologyDocument(a);
        Assert.assertEquals(1, m.ontologies().count());
        Assert.assertNotNull(o.getOWLOntologyManager());
        String comment = getOWLComment(o);
        LOGGER.debug("Ontology comment '{}'", comment);

        try {
            m.loadOntologyFromOntologyDocument(b);
        } catch (UnparsableOntologyException e) {
            LOGGER.debug("Exception: {}", e.getMessage().substring(0, Math.min(100, e.getMessage().length())));
        }
        // Note: the different with OWL-API (5.1.4) : no ontologies inside manager. Believe it is a bug of OWL-API.
        Assert.assertEquals("Wrong count", 1, m.ontologies().count());
        Assert.assertNotNull("No manager", o.getOWLOntologyManager());
        Assert.assertSame(o, m.ontologies().findFirst().orElseThrow(AssertionError::new));
        Assert.assertEquals(comment, getOWLComment(o));
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

        manager.loadOntologyFromOntologyDocument(new FileDocumentSource(map.get(a).toFile(),
                OntFormat.MANCHESTER_SYNTAX.createOwlFormat()));
        Assert.assertEquals("Should be one ontology inside manager", 1, manager.ontologies().count());
        manager.loadOntologyFromOntologyDocument(new FileDocumentSource(map.get(b).toFile(),
                OntFormat.MANCHESTER_SYNTAX.createOwlFormat()));
        Assert.assertEquals("Wrong num of onts", 4, manager.ontologies().count());
    }

    @Test(expected = OntologyFactoryImpl.OWLTransformException.class)
    public void tesLoadWrongRDFSyntax() throws OWLOntologyCreationException {
        // wrong []-List
        OntManagers.createONT().loadOntology(IRI.create(ReadWriteUtils.getResourceURI("ontapi/wrong.rdf")));
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
        IRI iri = IRI.create(ReadWriteUtils.getResourceURI("ontapi/recursive-graph.ttl"));
        LOGGER.debug("The file: {}", iri);
        OntologyManager m = OntManagers.createONT();
        GraphTransformers.Store store = m.getOntologyConfigurator().getGraphTransformers();
        if (!store.contains(OWLRecursiveTransform.class.getName())) {
            m.getOntologyConfigurator().setGraphTransformers(store.addFirst(OWLRecursiveTransform::new));
        }
        OntologyModel o = m.loadOntology(iri);
        ReadWriteUtils.print(o.asGraphModel());
        o.axioms().forEach(a -> LOGGER.debug("{}", a));
        Assert.assertEquals("Wrong axioms count", 5, o.getAxiomCount());
        Assert.assertEquals(1, o.axioms(AxiomType.SUBCLASS_OF).count());
    }

    @Test
    public void testNoTransformsForNativeOWLAPIFormats() throws Exception {
        OWLOntologyDocumentSource src = ReadWriteUtils.getFileDocumentSource("/owlapi/primer.owlxml.xml", OntFormat.OWL_XML);
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setGraphTransformers(new GraphTransformers.Store().add(g -> new Transform(g) {
            @Override
            public void perform() throws TransformException {
                Assert.fail("No transforms are expected.");
            }
        }));
        Assert.assertNotNull(m.loadOntologyFromOntologyDocument(src));
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
        IRI wrongFile = IRI.create(ReadWriteUtils.getResourceURI("ontapi/wrong-core.ttl"));

        m.getIRIMappers().add(FileMap.create(amyIRI, amyFile));
        m.getIRIMappers().add(FileMap.create(sueIRI, sueFile));
        m.getIRIMappers().add(FileMap.create(coreIRI, wrongFile));
        m.getIRIMappers().forEach(x -> LOGGER.debug("{}", x));

        LOGGER.debug("-================-");
        try {
            Assert.fail("No exception while loading " + m.loadOntology(coreIRI));
        } catch (OWLRuntimeException e) {
            if (e instanceof UnloadableImportException) {
                LOGGER.debug("Exception", e);
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
        OWLOntologyIRIMapper mapSp = new SimpleIRIMapper(sp,
                IRI.create(ReadWriteUtils.getResourcePath("etc", "sp.ttl").toFile()));
        OWLOntologyIRIMapper mapSpin = new SimpleIRIMapper(spin,
                IRI.create(ReadWriteUtils.getResourcePath("etc", "spin.ttl").toFile()));

        LOGGER.debug("1) Test load some web ontology for a case when only file scheme is allowed.");
        OntologyManager m1 = OntManagers.createONT();
        OntLoaderConfiguration conf = m1.getOntologyLoaderConfiguration()
                .setSupportedSchemes(Stream.of(OntConfig.DefaultScheme.FILE).collect(Collectors.toList()));
        m1.setOntologyLoaderConfiguration(conf);
        try {
            Assert.fail("No exception while loading " + m1.loadOntology(sp));
        } catch (OWLOntologyCreationException e) {
            if (e instanceof OntologyFactoryImpl.ConfigMismatchException) {
                LOGGER.debug("Exception", e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }

        LOGGER.debug("2) Add mapping and try to load again.");
        m1.getIRIMappers().add(mapSp);
        m1.loadOntology(sp);
        Assert.assertEquals("Should be single ontology inside", 1, m1.ontologies().count());

        LOGGER.debug("3) Load new web-ontology which depends on this existing one.");
        try {
            Assert.fail("No exception while loading " + m1.loadOntology(spin));
        } catch (OWLOntologyCreationException e) {
            if (e instanceof OntologyFactoryImpl.ConfigMismatchException) {
                LOGGER.debug("Exception", e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }
        Assert.assertEquals("Should be single ontology inside", 1, m1.ontologies().count());

        LOGGER.debug("4) Try to load new web-ontology with file mapping which depends on some other web-ontology.");
        OntologyManager m2 = OntManagers.createONT();
        m2.setOntologyLoaderConfiguration(conf);
        m2.getIRIMappers().add(mapSpin);
        try {
            Assert.fail("No exception while loading " + m2.loadOntology(spin));
        } catch (OWLRuntimeException e) {
            if (e instanceof UnloadableImportException) {
                LOGGER.debug("Exception", e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }
        Assert.assertEquals("Manager should be empty", 0, m2.ontologies().count());

        LOGGER.debug("5) Set ignore broken imports and try to load again.");
        m2.setOntologyLoaderConfiguration(conf.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
        m2.loadOntology(spin);
        Assert.assertEquals("Should be only single ontology inside.", 1, m2.ontologies().count());

        LOGGER.debug("6) Set ignore some import and load ontology with dependencies.");
        OntologyManager m3 = OntManagers.createONT();
        m3.getIRIMappers().add(mapSp);
        m3.getIRIMappers().add(mapSpin);
        m3.setOntologyLoaderConfiguration(conf
                .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.THROW_EXCEPTION).addIgnoredImport(sp));
        m3.loadOntology(spin);
        Assert.assertEquals("Should be only single ontology inside.", 1, m3.ontologies().count());

        LOGGER.debug("7) Default way to load.");
        OntologyManager m4 = OntManagers.createONT();
        m4.getIRIMappers().add(mapSp);
        m4.getIRIMappers().add(mapSpin);
        m4.loadOntology(spin);
        Assert.assertEquals("Should be two ontologies inside.", 2, m4.ontologies().count());

        LOGGER.debug("8) Test loading with MissingOntologyHeaderStrategy = true/false");
        OWLOntologyManager m5 = OntManagers.createONT();
        Assert.assertEquals("Incorrect default settings", MissingOntologyHeaderStrategy.INCLUDE_GRAPH,
                m5.getOntologyLoaderConfiguration().getMissingOntologyHeaderStrategy());
        loadLoopedOntologyFamily(m5);
        Assert.assertEquals("Wrong ontologies count.", 3, m5.ontologies().count());
        OWLOntologyManager m6 = OntManagers.createONT();
        m6.setOntologyLoaderConfiguration(m6.getOntologyLoaderConfiguration()
                .setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy.IMPORT_GRAPH));
        loadLoopedOntologyFamily(m6);
        Assert.assertEquals("Wrong ontologies.", 4, m6.ontologies().count());
        // todo: it would be nice to validate the result ontologies
    }

    @Test
    public void testMissedImports() throws OWLOntologyCreationException {
        // create data:
        OntologyManager m = OntManagers.createONT();
        OntologyModel a = m.createOntology(IRI.create("urn:a"));
        OntologyModel b = m.createOntology(IRI.create("urn:b"));
        a.asGraphModel().createOntClass("A");
        b.asGraphModel().createOntClass("B");
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
                m3.getOntologyLoaderConfiguration()
                        .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
        m3.loadOntologyFromOntologyDocument(new StringDocumentSource(sA));
        checkForMissedImportsTest(b3);

        // reverse through graph
        OntologyManager m4 = OntManagers.createONT();
        OntologyModel b4 = m4.addOntology(b.asGraphModel().getBaseGraph(),
                m4.getOntologyLoaderConfiguration()
                        .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
        m4.addOntology(a.asGraphModel().getBaseGraph());
        checkForMissedImportsTest(b4);
    }

    @Test
    public void testDocumentSourceMapping() throws OWLOntologyCreationException {
        final String a_uri = "urn:a";
        final String b_uri = "urn:b";
        // create data:
        OntGraphModel a = OntModelFactory.createModel();
        a.setID(a_uri);
        a.setNsPrefixes(OntModelFactory.STANDARD);
        OntGraphModel b = OntModelFactory.createModel();
        b.setID(b_uri);
        b.setNsPrefixes(OntModelFactory.STANDARD);
        a.createOntClass("urn:a#A");
        b.createOntClass("urn:b#B");
        b.addImport(a);
        Map<String, String> data = new HashMap<>();
        data.put("store://a", ReadWriteUtils.toString(a, OntFormat.TURTLE));
        data.put("store://b", ReadWriteUtils.toString(b, OntFormat.TURTLE));

        data.forEach((iri, txt) -> LOGGER.debug("Document iri: <{}>\nData:\n{}", iri, txt));

        OWLOntologyIRIMapper iriMapper = iri -> {
            switch (iri.toString()) {
                case a_uri:
                    return IRI.create("store://a");
                case b_uri:
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
        m.getDocumentSourceMappers().add(docMapper);
        OntologyModel o = m.loadOntology(IRI.create(b_uri));
        Assert.assertNotNull(o);
        Assert.assertEquals(2, m.ontologies().count());
    }

    @Test(expected = OntologyFactoryImpl.ConfigMismatchException.class)
    public void testDisableWebAccess() throws OWLOntologyCreationException {
        IRI iri = IRI.create("http://spinrdf.org/sp");
        OntologyManager m = OntManagers.createONT();
        m.loadOntologyFromOntologyDocument(new IRIDocumentSource(iri),
                m.getOntologyLoaderConfiguration().disableWebAccess());
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

    @Test
    public void testAddRemoveOntologyFactory() throws OWLOntologyCreationException {
        OntologyManager manager = OntManagers.createConcurrentONT();
        Assert.assertEquals(1, manager.getOntologyFactories().size());
        manager.getOntologyFactories().clear();
        Assert.assertTrue(manager.getOntologyFactories().isEmpty());

        OWLOntologyFactory owlFactory = new OWLFactoryWrapper.FactoryImpl((m, i) ->
                new OntManagers.OWLAPIImplProfile().createOWLOntologyImpl(m, i));
        try {
            manager.getOntologyFactories().add(owlFactory);
            Assert.fail("Must fail");
        } catch (OntApiException e) {
            LOGGER.debug(e.getMessage());
        }
        Assert.assertEquals(0, manager.getOntologyFactories().size());

        String comment = "Generated by test";
        OntologyFactory.Builder builder = new OntologyBuilderImpl() {
            @Override
            public Graph createGraph() {
                return OntModelFactory.createModel().setID(null).addComment(comment).getModel().getBaseGraph();
            }
        };
        OntologyFactory ontFactory = new OntManagers.ONTAPIProfile().createOntologyFactory(builder);
        Assert.assertNotNull(ontFactory);
        manager.getOntologyFactories().add(ontFactory);
        Assert.assertEquals(1, manager.getOntologyFactories().size());

        String uri1 = "http://test1.com";
        OntologyModel o1 = manager.createOntology(IRI.create(uri1));
        Assert.assertNotNull(o1);
        ReadWriteUtils.print(o1);
        Assert.assertEquals(uri1, o1.getOntologyID().getOntologyIRI()
                .map(IRI::getIRIString).orElseThrow(AssertionError::new));
        Assert.assertEquals(comment, getOWLComment(o1));

        OntologyModel o2 = manager.loadOntology(IRI.create(ReadWriteUtils.getResourceURI("/ontapi/test1.ttl")));
        Assert.assertNotNull(o2);
        ReadWriteUtils.print(o2);
        Assert.assertEquals("http://test.test/complex", o2.getOntologyID().getOntologyIRI()
                .map(IRI::getIRIString).orElseThrow(AssertionError::new));
        Assert.assertEquals("http://test.test/complex/version-iri/1.0", o2.getOntologyID().getVersionIRI()
                .map(IRI::getIRIString).orElseThrow(AssertionError::new));
        Assert.assertEquals(comment, getOWLComment(o2));
    }

    @Test
    public void testNativeTurtleOWLParser() throws OWLOntologyCreationException {
        OntConfig conf = new OntConfig()
                .addIgnoredImport(IRI.create("http://spinrdf.org/sp"))
                .addIgnoredImport(IRI.create("http://spinrdf.org/spin"))
                .disableWebAccess();
        Assert.assertFalse(conf.buildLoaderConfiguration().isUseOWLParsersToLoad());

        OWLOntologyDocumentSource source = new IRIDocumentSource(IRI.create(ReadWriteUtils.getResourceURI("/etc/spl.spin.ttl")));

        // Load using Jena turtle reader:
        OntologyManager m1 = OntManagers.createONT();
        OntologyModel o1 = m1.loadOntologyFromOntologyDocument(source, conf.buildLoaderConfiguration());
        Assert.assertEquals(1, m1.ontologies().count());
        Assert.assertEquals(0, o1.asGraphModel().imports().count());
        // check all []-lists are valid:
        List<RDFList> lists = o1.asGraphModel()
                .statements(null, SP.where, null)
                .map(Statement::getObject).map(o -> o.as(RDFList.class)).collect(Collectors.toList());
        Assert.assertEquals(40, lists.size());
        Assert.assertTrue(lists.stream().allMatch(RDFList::isValid));

        // Load using OWL-API Turtle Parser
        OntologyManager m2 = OntManagers.createONT();
        OntologyModel o2 = m2.loadOntologyFromOntologyDocument(source,
                conf.buildLoaderConfiguration().setUseOWLParsersToLoad(true));
        Assert.assertEquals(1, m2.ontologies().count());
        Assert.assertEquals(0, o2.asGraphModel().imports().count());
        ReadWriteUtils.print(o2);
        // Due to buggy OWL-API Parser behaviour there is no []-lists at all!:
        Assert.assertTrue(o2.asGraphModel().statements(null, null, null)
                .map(Statement::getObject).noneMatch(l -> l.canAs(RDFList.class)));
    }

    @Test
    public void testLoadWithDisabledProcessImports() throws OWLOntologyCreationException {
        OntologyManager m = OntManagers.createONT();
        String uri_a = "urn:a";
        String uri_b = "urn:b";
        String prefixes = "@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n" +
                "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .";

        String txt1 = String.format("%s[ a owl:Ontology; owl:imports  <%s>, <%s> ].", prefixes, uri_a, uri_b);
        OWLOntologyDocumentSource src1 = ReadWriteUtils.getStringDocumentSource(txt1, OntFormat.TURTLE);
        Assert.assertTrue(m.getOntologyConfigurator().isProcessImports());
        Assert.assertTrue(m.getOntologyLoaderConfiguration().isProcessImports());
        OntologyModel o1 = m.loadOntologyFromOntologyDocument(src1,
                m.getOntologyLoaderConfiguration().setProcessImports(false));
        ReadWriteUtils.print(o1);

        Assert.assertTrue(m.getOntologyConfigurator().isProcessImports());
        Assert.assertTrue(m.getOntologyLoaderConfiguration().isProcessImports());

        Assert.assertEquals(1, m.ontologies().count());
        Assert.assertEquals(2, o1.importsDeclarations().count());
        Assert.assertEquals(0, o1.directImports().count());

        String txt2 = String.format("%s <%s> a owl:Ontology; owl:imports <%s> .", prefixes, uri_a, uri_b);
        OWLOntologyDocumentSource src2 = ReadWriteUtils.getStringDocumentSource(txt2, OntFormat.TURTLE);
        OntologyModel o2 = m.loadOntologyFromOntologyDocument(src2,
                m.getOntologyLoaderConfiguration().setProcessImports(false));
        ReadWriteUtils.print(o2);

        Assert.assertEquals(2, m.ontologies().count());
        Assert.assertEquals(2, o1.importsDeclarations().count());
        Assert.assertEquals(1, o2.importsDeclarations().count());
        Assert.assertEquals(1, o1.directImports().count());
        Assert.assertEquals(0, o2.directImports().count());

        String txt3 = String.format("%s <%s> a owl:Ontology .", prefixes, uri_b);
        OWLOntologyDocumentSource src3 = ReadWriteUtils.getStringDocumentSource(txt3, OntFormat.TURTLE);
        OntologyModel o3 = m.loadOntologyFromOntologyDocument(src3,
                m.getOntologyLoaderConfiguration().setProcessImports(false));
        ReadWriteUtils.print(o3);

        Assert.assertEquals(3, m.ontologies().count());
        Assert.assertEquals(2, o1.imports().count());
        Assert.assertEquals(1, o2.imports().count());
        Assert.assertEquals(0, o3.directImports().count());
        Assert.assertEquals(0, o3.imports().count());
    }

    @Test
    public void testControlUnionGraphs() throws OWLOntologyCreationException {
        class MyUnion extends UnionGraph {
            MyUnion(Graph base) {
                super(base);
            }
        }
        @ParametersAreNonnullByDefault
        OntologyFactory.Builder builder = new OntologyBuilderImpl() {

            @Override
            public UnionGraph createUnionGraph(Graph g, OntLoaderConfiguration c) {
                return new MyUnion(g);
            }
        };
        OntologyFactory factory = new OntManagers.ONTAPIProfile().createOntologyFactory(builder);

        OntologyManager m = OntManagers.createONT();
        m.getOntologyFactories().clear();
        Assert.assertEquals(0, m.getOntologyFactories().size());
        m.getOntologyFactories().add(factory);

        String uri_a = "urn:a";
        String uri_b = "urn:b";
        String uri_c = "urn:c";
        String prefixes = "@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
                "@prefix owl:   <http://www.w3.org/2002/07/owl#> .\n" +
                "@prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .\n" +
                "@prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .";
        String txt_a = String.format("%s <%s> a owl:Ontology; owl:imports <%s> .", prefixes, uri_a, uri_b);
        String txt_b = String.format("%s <%s> a owl:Ontology .", prefixes, uri_b);

        OWLOntologyDocumentSource src_a = ReadWriteUtils.getStringDocumentSource(txt_a, OntFormat.TURTLE);
        OWLOntologyDocumentSource src_b = ReadWriteUtils.getStringDocumentSource(txt_b, OntFormat.TURTLE);

        OntologyModel b = m.loadOntologyFromOntologyDocument(src_b);
        OntologyModel a = m.loadOntologyFromOntologyDocument(src_a);
        OntologyModel c = m.createOntology(IRI.create(uri_c));

        m.applyChange(new AddImport(c, m.getOWLDataFactory().getOWLImportsDeclaration(IRI.create(uri_a))));

        Assert.assertTrue(a.asGraphModel().getGraph() instanceof MyUnion);
        Assert.assertTrue(b.asGraphModel().getGraph() instanceof MyUnion);
        Assert.assertTrue(c.asGraphModel().getGraph() instanceof MyUnion);

        Assert.assertEquals(3, OntModels.importsClosure(c.asGraphModel())
                .peek(x -> Assert.assertTrue(x.getGraph() instanceof MyUnion)).count());
        Assert.assertEquals(2, OntModels.importsClosure(a.asGraphModel())
                .peek(x -> Assert.assertTrue(x.getGraph() instanceof MyUnion)).count());
        Assert.assertEquals(1, OntModels.importsClosure(b.asGraphModel())
                .peek(x -> Assert.assertTrue(x.getGraph() instanceof MyUnion)).count());
    }

    @Test
    public void testLoadConfigurationWWithUseOWLParsersOption() throws OWLOntologyCreationException {
        OWLOntologyDocumentSource src = ReadWriteUtils.getFileDocumentSource("/ontapi/test2.owl", OntFormat.OWL_XML);
        OntologyManager m = OntManagers.createONT();
        OntLoaderConfiguration conf = m.getOntologyLoaderConfiguration().setUseOWLParsersToLoad(true);
        OntologyModel o = m.loadOntologyFromOntologyDocument(src, conf);
        OWLAdapter adapter = OWLAdapter.get();
        OntLoaderConfiguration conf2 = adapter.asModelConfig(adapter.asBaseModel(o).getConfig()).getLoaderConfig();
        Assert.assertSame(conf, conf2);
    }

    @Test
    public void testLoadConfigurationWithOWLAPIFactory() throws OWLOntologyCreationException {
        OWLOntologyDocumentSource src = ReadWriteUtils.getFileDocumentSource("/ontapi/test2.omn", OntFormat.MANCHESTER_SYNTAX);
        OntologyManager m = createManagerWithOWLAPIOntologyFactory();
        OWLParserFactory parser = OWLLangRegistry.getLang(OWLLangRegistry.LangKey.MANCHESTERSYNTAX.getKey())
                .orElseThrow(AssertionError::new).getParserFactory();
        m.getOntologyParsers().add(parser);
        Assert.assertEquals(1, m.getOntologyFactories().size());
        Assert.assertEquals(1, m.getOntologyParsers().size());
        Assert.assertEquals(0, m.getOntologyStorers().size());
        OntLoaderConfiguration conf = m.getOntologyLoaderConfiguration().setUseOWLParsersToLoad(true);
        OntologyModel o = m.loadOntologyFromOntologyDocument(src, conf);
        OWLAdapter adapter = OWLAdapter.get();
        OntLoaderConfiguration conf2 = adapter.asModelConfig(adapter.asBaseModel(o).getConfig()).getLoaderConfig();
        Assert.assertSame(conf, conf2);
    }

    @Test
    public void testErrorWhenNoParserFound() {
        OWLOntologyDocumentSource src = ReadWriteUtils.getFileDocumentSource("/ontapi/test2.fss", OntFormat.FUNCTIONAL_SYNTAX);
        OntologyManager m = createManagerWithOWLAPIOntologyFactory();
        Assert.assertEquals(1, m.getOntologyFactories().size());
        Assert.assertEquals(0, m.getOntologyParsers().size());
        Assert.assertEquals(0, m.getOntologyStorers().size());

        try {
            m.loadOntologyFromOntologyDocument(src);
            Assert.fail("Possible to load");
        } catch (UnparsableOntologyException e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }
}
