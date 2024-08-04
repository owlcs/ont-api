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

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OWLAdapter;
import com.github.owlcs.ontapi.OWLFactoryWrapper;
import com.github.owlcs.ontapi.OWLLangRegistry;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyBuilderImpl;
import com.github.owlcs.ontapi.OntologyCreator;
import com.github.owlcs.ontapi.OntologyFactory;
import com.github.owlcs.ontapi.OntologyFactoryImpl;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.config.OntConfig;
import com.github.owlcs.ontapi.config.OntLoaderConfiguration;
import com.github.owlcs.ontapi.testutils.FileMap;
import com.github.owlcs.ontapi.testutils.MiscTestUtils;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.ontapi.testutils.SP;
import com.github.owlcs.ontapi.testutils.StringInputStreamDocumentSource;
import com.github.owlcs.ontapi.transforms.GraphTransformers;
import com.github.owlcs.ontapi.transforms.OWLRecursiveTransform;
import com.github.owlcs.ontapi.transforms.Transform;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.UnionGraph;
import org.apache.jena.ontapi.impl.UnionGraphImpl;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.graph.GraphReadOnly;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSourceBase;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.MissingOntologyHeaderStrategy;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFactory;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLRuntimeException;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To test loading mechanisms from {@link OntologyFactoryImpl}
 * including different {@link com.github.owlcs.ontapi.config.LoadSettings lading settings}.
 * <p>
 * Created by @ssz on 16.01.2018.
 */
@SuppressWarnings("JavaDoc")
public class LoadFactoryManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFactoryManagerTest.class);

    private static void checkForMissedImportsTest(Ontology b) {
        checkForMissedImportsTest((OWLOntology) b);
        Assertions.assertEquals(1, b.asGraphModel().imports().count());
    }

    private static void checkForMissedImportsTest(OWLOntology b) {
        Assertions.assertEquals(1, b.imports().count());
        Assertions.assertEquals(1, b.axioms(Imports.EXCLUDED)
                .filter(a -> AxiomType.DECLARATION.equals(a.getAxiomType())).count());
        Assertions.assertEquals(2, b.axioms(Imports.INCLUDED)
                .filter(a -> AxiomType.DECLARATION.equals(a.getAxiomType())).count());
    }

    private static void loadLoopedOntologyFamily(OWLOntologyManager m) throws Exception {
        IRI amyIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-amy");
        IRI sueIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-sue");
        IRI bobIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-bob");
        IRI coreIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/core");

        IRI amyFile = IRI.create(OWLIOUtils.getResourceURI("/owlapi/importNoOntology/subject-amy.ttl"));
        IRI sueFile = IRI.create(OWLIOUtils.getResourceURI("/owlapi/importNoOntology/subject-sue.ttl"));
        IRI bobFile = IRI.create(OWLIOUtils.getResourceURI("/owlapi/importNoOntology/subject-bob.ttl"));
        IRI coreFile = IRI.create(OWLIOUtils.getResourceURI("/ontapi/core.ttl"));

        m.getIRIMappers().add(FileMap.create(amyIRI, amyFile));
        m.getIRIMappers().add(FileMap.create(bobIRI, bobFile));
        m.getIRIMappers().add(FileMap.create(sueIRI, sueFile));
        m.getIRIMappers().add(FileMap.create(coreIRI, coreFile));
        m.getIRIMappers().forEach(x -> LOGGER.debug("{}", x));

        LOGGER.debug("-================-");
        OWLOntology bob = m.loadOntology(bobIRI);
        OWLIOUtils.print(bob);
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
        DataFactory df = OntManagers.DEFAULT_PROFILE.createDataFactory();
        OntologyCreator builder = OntManagers.DEFAULT_PROFILE.createOntologyBuilder();
        OntologyFactory of = new OWLFactoryWrapper().asOntologyFactory(builder);
        return OntManagers.DEFAULT_PROFILE.createManager(df, of, null);
    }

    @Test
    public void testOntologyAlreadyExistsException() throws Exception {
        Path p = OWLIOUtils.getResourcePath("/ontapi/pizza.ttl");
        OWLOntologyDocumentSource src = new FileDocumentSource(p.toFile(), OntFormat.TURTLE.createOwlFormat());
        OntologyManager m = OntManagers.createManager();
        m.loadOntologyFromOntologyDocument(src);
        Assertions.assertEquals(1, m.ontologies().count());
        try {
            m.loadOntologyFromOntologyDocument(src); // in OWL-API-impl (5.1.9) there is no exception
            Assertions.fail("Possible to load the same ontology twice");
        } catch (OWLOntologyAlreadyExistsException oae) {
            LOGGER.debug("Expected: '{}'", oae.getMessage());
        }
        Assertions.assertEquals(1, m.ontologies().count());
    }

    @Test
    public void testEmptyOntologyDefaultPrefixes() {
        OWLDocumentFormat f = OntManagers.createManager().createOntology().getFormat();
        Assertions.assertNotNull(f);
        Map<String, String> map = f.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap();
        Assertions.assertEquals(4, map.size());
        Assertions.assertEquals(XSD.NS, map.get("xsd:"));
        Assertions.assertEquals(RDFS.getURI(), map.get("rdfs:"));
        Assertions.assertEquals(RDF.getURI(), map.get("rdf:"));
        Assertions.assertEquals(OWL.NS, map.get("owl:"));
    }

    @Test
    public void testPrefixesRoundTrips() throws Exception {
        URI uri = OWLIOUtils.getResourcePath("/ontapi/foaf.rdf").toUri();
        Path p = Paths.get(uri);
        OWLOntologyManager m = OntManagers.createManager();
        OWLOntologyDocumentSource src = new FileDocumentSource(p.toFile(), OntFormat.RDF_XML.createOwlFormat());
        OWLOntology o = m.loadOntologyFromOntologyDocument(src);
        OWLDocumentFormat f = m.getOntologyFormat(o);
        Assertions.assertNotNull(f);
        Assertions.assertEquals(5, f.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().size());
        OWLDocumentFormat f1 = OntFormat.TURTLE.createOwlFormat();
        // 4 default:
        Assertions.assertEquals(4, f1.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().size());

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
        String txt1 = OWLIOUtils.asString(o, f1);
        LOGGER.debug(txt1);
        int c = StringUtils.countMatches(txt1, "PREFIX");
        Assertions.assertEquals(11, c);
        OWLOntologyDocumentSource src1 = new StringInputStreamDocumentSource(txt1, f1);
        OWLDocumentFormat f2 = OntManagers.createConcurrentManager().loadOntologyFromOntologyDocument(src1).getFormat();
        Assertions.assertNotNull(f2);
        Assertions.assertEquals(c, f2.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().size());

        // check original not changed
        OWLDocumentFormat f3 = m.getOntologyFormat(o);
        Assertions.assertNotNull(f3);
        Assertions.assertEquals(5, f3.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().size());

        // round-trip manchester:
        OWLDocumentFormat f4 = OntFormat.MANCHESTER_SYNTAX.createOwlFormat();
        // 4 default:
        Assertions.assertEquals(4, f4.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().size());
        f4.asPrefixOWLDocumentFormat().setPrefixManager(f1.asPrefixOWLDocumentFormat());
        Assertions.assertEquals(11, f4.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().size());
        String txt4 = OWLIOUtils.asString(o, f4);
        LOGGER.debug(txt4);
        OWLOntologyDocumentSource src4 = new StringInputStreamDocumentSource(txt1, f1);
        OWLDocumentFormat f5 = OntManagers.createConcurrentManager().loadOntologyFromOntologyDocument(src4).getFormat();
        Assertions.assertNotNull(f5);
        Assertions.assertTrue(f2.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().entrySet()
                .containsAll(f1.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap().entrySet()));
    }

    @Test
    public void testLoadUnmodifiableGraph() throws OWLOntologyCreationException {
        OntologyManager m = OntManagers.createManager();
        OntModel b = OntModelFactory.createModel().setID("http://b").getModel();
        OntModel a = OntModelFactory.createModel().setID("http://a").getModel().addImport(b);

        String str = OWLIOUtils.asString(a, OntFormat.TURTLE);
        LOGGER.debug("{}", str);

        GraphReadOnly g = new GraphReadOnly(b.getGraph());
        m.addOntology(g);

        Assertions.assertInstanceOf(GraphReadOnly.class, m.models().findFirst()
                .orElseThrow(AssertionError::new).getBaseGraph());
        m.loadOntologyFromOntologyDocument(OWLIOUtils.getStringDocumentSource(str, OntFormat.TURTLE));
        Assertions.assertEquals(2, m.ontologies().count());
        Assertions.assertNotNull(m.getGraphModel("http://b"));
        Assertions.assertNotNull(m.getGraphModel("http://a"));
    }

    @Test
    public void testLoadWrongDuplicate() throws OWLOntologyCreationException {
        IRI a = IRI.create(OWLIOUtils.getResourceURI("/ontapi/load-test-a.owl"));
        IRI b = IRI.create(OWLIOUtils.getResourceURI("/ontapi/load-test-b.ttl"));

        OWLOntologyManager m = OntManagers.createManager();
        OWLOntology o = m.loadOntologyFromOntologyDocument(a);
        Assertions.assertEquals(1, m.ontologies().count());
        Assertions.assertNotNull(o.getOWLOntologyManager());
        String comment = getOWLComment(o);
        LOGGER.debug("Ontology comment '{}'", comment);

        try {
            m.loadOntologyFromOntologyDocument(b);
        } catch (UnparsableOntologyException e) {
            LOGGER.debug("Exception: {}", e.getMessage().substring(0, Math.min(100, e.getMessage().length())));
        }
        // Note: the different with OWL-API (5.1.4) : no ontologies inside manager. Believe it is a bug of OWL-API.
        Assertions.assertEquals(1, m.ontologies().count());
        Assertions.assertNotNull(o.getOWLOntologyManager());
        Assertions.assertSame(o, m.ontologies().findFirst().orElseThrow(AssertionError::new));
        Assertions.assertEquals(comment, getOWLComment(o));
    }

    @Test
    public void testLoadNotJenaHierarchy() throws Exception {
        String a = "http://spinrdf.org/sp";
        String b = "http://spinrdf.org/spif";
        Map<String, Path> map = new HashMap<>();
        map.put(a, OWLIOUtils.getResourcePath("/ontapi/omn/sp.omn"));
        map.put(b, OWLIOUtils.getResourcePath("/ontapi/omn/spif.omn"));
        map.put("http://spinrdf.org/spin", OWLIOUtils.getResourcePath("/ontapi/omn/spin.omn"));
        map.put("http://spinrdf.org/spl", OWLIOUtils.getResourcePath("/ontapi/omn/spl.spin.omn"));

        OntologyManager manager = OntManagers.createManager();
        for (String uri : map.keySet()) {
            manager.getIRIMappers().add(new SimpleIRIMapper(IRI.create(uri), IRI.create(map.get(uri).toUri())));
        }
        manager.getOntologyConfigurator()
                .setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE))
                .setPerformTransformation(false);

        manager.loadOntologyFromOntologyDocument(new FileDocumentSource(map.get(a).toFile(),
                OntFormat.MANCHESTER_SYNTAX.createOwlFormat()));
        Assertions.assertEquals(1, manager.ontologies().count());
        manager.loadOntologyFromOntologyDocument(new FileDocumentSource(map.get(b).toFile(),
                OntFormat.MANCHESTER_SYNTAX.createOwlFormat()));
        Assertions.assertEquals(4, manager.ontologies().count());
    }

    @Test
    public void tesLoadWrongRDFSyntax() {
        // wrong []-List
        Assertions.assertThrows(OntologyFactoryImpl.OWLTransformException.class,
                () -> OntManagers.createManager().loadOntology(IRI.create(OWLIOUtils.getResourceURI("/ontapi/wrong.rdf"))));
    }

    @Test
    public void testLoadNotJenaHierarchyWithDisabledWeb() {
        Assertions.assertThrows(UnloadableImportException.class, () -> {
            Path path = OWLIOUtils.getResourcePath("/owlapi/obo/annotated_import.obo");
            LOGGER.debug("File {}", path);
            OntologyManager m = OntManagers.createManager();
            m.getOntologyConfigurator().setSupportedSchemes(Collections.singletonList(OntConfig.DefaultScheme.FILE));
            OWLOntologyID id = m.loadOntology(IRI.create(path.toUri())).getOntologyID();
            LOGGER.error("The ontology {} is loaded.", id);
        });
    }

    @Test
    public void testLoadRecursiveGraphWithTransform() throws OWLOntologyCreationException {
        IRI iri = IRI.create(OWLIOUtils.getResourceURI("/ontapi/recursive-graph.ttl"));
        LOGGER.debug("The file: {}", iri);
        OntologyManager m = OntManagers.createManager();
        GraphTransformers store = m.getOntologyConfigurator().getGraphTransformers();
        if (store.get(OWLRecursiveTransform.class.getName()).isEmpty()) {
            m.getOntologyConfigurator().setGraphTransformers(store.addFirst(Transform.Factory.create(OWLRecursiveTransform.class)));
        }
        Ontology o = m.loadOntology(iri);
        OWLIOUtils.print(o.asGraphModel());
        o.axioms().forEach(a -> LOGGER.debug("{}", a));
        Assertions.assertEquals(5, o.getAxiomCount(), "Wrong axioms count");
        Assertions.assertEquals(1, o.axioms(AxiomType.SUBCLASS_OF).count());
    }

    @Test
    public void testNoTransformsForNativeOWLAPIFormats() throws Exception {
        OWLOntologyDocumentSource src = OWLIOUtils.getFileDocumentSource("/owlapi/primer.owlxml.xml", OntFormat.OWL_XML);
        OntologyManager m = OntManagers.createManager();
        m.getOntologyConfigurator().setGraphTransformers(new GraphTransformers().addLast(g -> {
            Assertions.fail("No transforms are expected.");
            return null;
        }));
        Assertions.assertNotNull(m.loadOntologyFromOntologyDocument(src));
    }

    /**
     * Moved from {@link CommonManagerTest}
     *
     * @throws Exception
     */
    @Test
    public void testLoadCorruptedOntology() throws Exception {
        OWLOntologyManager m = OntManagers.createManager();

        IRI amyIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-amy");
        IRI sueIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/tests/RenalTransplantation/subject-sue");
        IRI coreIRI = IRI.create("http://www.w3.org/2013/12/FDA-TA/core");

        IRI amyFile = IRI.create(OWLIOUtils.getResourceURI("/owlapi/importNoOntology/subject-amy.ttl"));
        IRI sueFile = IRI.create(OWLIOUtils.getResourceURI("/owlapi/importNoOntology/subject-sue.ttl"));
        IRI wrongFile = IRI.create(OWLIOUtils.getResourceURI("/ontapi/wrong-core.ttl"));

        m.getIRIMappers().add(FileMap.create(amyIRI, amyFile));
        m.getIRIMappers().add(FileMap.create(sueIRI, sueFile));
        m.getIRIMappers().add(FileMap.create(coreIRI, wrongFile));
        m.getIRIMappers().forEach(x -> LOGGER.debug("{}", x));

        LOGGER.debug("-================-");
        try {
            Assertions.fail("No exception while loading " + m.loadOntology(coreIRI));
        } catch (OWLRuntimeException e) {
            if (e instanceof UnloadableImportException) {
                LOGGER.debug("Exception", e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }
        Assertions.assertEquals(0, m.ontologies().count(), "There are some ontologies inside manager");
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
                IRI.create(OWLIOUtils.getResourcePath("/etc/sp.ttl").toFile()));
        OWLOntologyIRIMapper mapSpin = new SimpleIRIMapper(spin,
                IRI.create(OWLIOUtils.getResourcePath("/etc/spin.ttl").toFile()));

        LOGGER.debug("1) Test load some web ontology for a case when only file scheme is allowed.");
        OntologyManager m1 = OntManagers.createManager();
        OntLoaderConfiguration conf = m1.getOntologyLoaderConfiguration()
                .setSupportedSchemes(Stream.of(OntConfig.DefaultScheme.FILE).collect(Collectors.toList()));
        m1.setOntologyLoaderConfiguration(conf);
        try {
            Assertions.fail("No exception while loading " + m1.loadOntology(sp));
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
        Assertions.assertEquals(1, m1.ontologies().count(), "Should be single ontology inside");

        LOGGER.debug("3) Load new web-ontology which depends on this existing one.");
        try {
            Assertions.fail("No exception while loading " + m1.loadOntology(spin));
        } catch (OWLOntologyCreationException e) {
            if (e instanceof OntologyFactoryImpl.ConfigMismatchException) {
                LOGGER.debug("Exception", e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }
        Assertions.assertEquals(1, m1.ontologies().count(), "Should be single ontology inside");

        LOGGER.debug("4) Try to load new web-ontology with file mapping which depends on some other web-ontology.");
        OntologyManager m2 = OntManagers.createManager();
        m2.setOntologyLoaderConfiguration(conf);
        m2.getIRIMappers().add(mapSpin);
        try {
            Assertions.fail("No exception while loading " + m2.loadOntology(spin));
        } catch (OWLRuntimeException e) {
            if (e instanceof UnloadableImportException) {
                LOGGER.debug("Exception", e);
            } else {
                throw new AssertionError("Incorrect exception", e);
            }
        }
        Assertions.assertEquals(0, m2.ontologies().count(), "Manager should be empty");

        LOGGER.debug("5) Set ignore broken imports and try to load again.");
        m2.setOntologyLoaderConfiguration(conf.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
        m2.loadOntology(spin);
        Assertions.assertEquals(1, m2.ontologies().count(), "Should be only single ontology inside");

        LOGGER.debug("6) Set ignore some import and load ontology with dependencies.");
        OntologyManager m3 = OntManagers.createManager();
        m3.getIRIMappers().add(mapSp);
        m3.getIRIMappers().add(mapSpin);
        m3.setOntologyLoaderConfiguration(conf
                .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.THROW_EXCEPTION).addIgnoredImport(sp));
        m3.loadOntology(spin);
        Assertions.assertEquals(1, m3.ontologies().count(), "Should be only single ontology inside");

        LOGGER.debug("7) Default way to load.");
        OntologyManager m4 = OntManagers.createManager();
        m4.getIRIMappers().add(mapSp);
        m4.getIRIMappers().add(mapSpin);
        m4.loadOntology(spin);
        Assertions.assertEquals(2, m4.ontologies().count());

        LOGGER.debug("8) Test loading with MissingOntologyHeaderStrategy = true/false");
        OWLOntologyManager m5 = OntManagers.createManager();
        Assertions.assertEquals(MissingOntologyHeaderStrategy.INCLUDE_GRAPH,
                m5.getOntologyLoaderConfiguration().getMissingOntologyHeaderStrategy());
        loadLoopedOntologyFamily(m5);
        Assertions.assertEquals(3, m5.ontologies().count());
        OWLOntologyManager m6 = OntManagers.createManager();
        m6.setOntologyLoaderConfiguration(m6.getOntologyLoaderConfiguration()
                .setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy.IMPORT_GRAPH));
        loadLoopedOntologyFamily(m6);
        Assertions.assertEquals(4, m6.ontologies().count());
        // todo: it would be nice to validate the result ontologies
    }

    @Test
    public void testMissedImports() throws OWLOntologyCreationException {
        // create data:
        OntologyManager m = OntManagers.createManager();
        Ontology a = m.createOntology(IRI.create("urn:a"));
        Ontology b = m.createOntology(IRI.create("urn:b"));
        a.asGraphModel().createOntClass("A");
        b.asGraphModel().createOntClass("B");
        b.asGraphModel().addImport(a.asGraphModel());
        // check data:
        checkForMissedImportsTest(b);
        String sA = OWLIOUtils.asString(a, OntFormat.TURTLE);
        String sB = OWLIOUtils.asString(b, OntFormat.TURTLE);
        // direct:
        OntologyManager m2 = OntManagers.createManager();
        m2.loadOntologyFromOntologyDocument(new StringDocumentSource(sA));
        Ontology b2 = m2.loadOntologyFromOntologyDocument(new StringDocumentSource(sB));
        checkForMissedImportsTest(b2);

        // reverse through stream:
        OntologyManager m3 = OntManagers.createManager();
        Ontology b3 = m3.loadOntologyFromOntologyDocument(new StringDocumentSource(sB),
                m3.getOntologyLoaderConfiguration()
                        .setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT));
        m3.loadOntologyFromOntologyDocument(new StringDocumentSource(sA));
        checkForMissedImportsTest(b3);

        // reverse through graph
        OntologyManager m4 = OntManagers.createManager();
        Ontology b4 = m4.addOntology(b.asGraphModel().getBaseGraph(),
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
        OntModel a = OntModelFactory.createModel();
        a.setID(a_uri);
        a.setNsPrefixes(OntModelFactory.STANDARD);
        OntModel b = OntModelFactory.createModel();
        b.setID(b_uri);
        b.setNsPrefixes(OntModelFactory.STANDARD);
        a.createOntClass("urn:a#A");
        b.createOntClass("urn:b#B");
        b.addImport(a);
        Map<String, String> data = new HashMap<>();
        data.put("store://a", OWLIOUtils.asString(a, OntFormat.TURTLE));
        data.put("store://b", OWLIOUtils.asString(b, OntFormat.TURTLE));

        data.forEach((iri, txt) -> LOGGER.debug("Document iri: <{}>\nData:\n{}", iri, txt));

        OWLOntologyIRIMapper iriMapper = iri -> switch (iri.toString()) {
            case a_uri -> IRI.create("store://a");
            case b_uri -> IRI.create("store://b");
            default -> null;
        };
        OntologyManager.DocumentSourceMapping docMapper = id -> id.getOntologyIRI()
                .map(iriMapper::getDocumentIRI)
                .map(doc -> new OWLOntologyDocumentSourceBase(doc, OntFormat.TURTLE.createOwlFormat(), null) {

                    @Override
                    public Optional<InputStream> getInputStream() { // every time create a new InputStream
                        return Optional.of(OWLIOUtils.asInputStream(data.get(doc.getIRIString())));
                    }
                })
                .orElse(null);

        OntologyManager m = OntManagers.createManager();
        m.getDocumentSourceMappers().add(docMapper);
        Ontology o = m.loadOntology(IRI.create(b_uri));
        Assertions.assertNotNull(o);
        Assertions.assertEquals(2, m.ontologies().count());
    }

    @Test
    public void testDisableWebAccess() {
        Assertions.assertThrows(OntologyFactoryImpl.ConfigMismatchException.class, () -> {
            IRI iri = IRI.create("http://spinrdf.org/sp");
            OntologyManager m = OntManagers.createManager();
            m.loadOntologyFromOntologyDocument(new IRIDocumentSource(iri),
                    m.getOntologyLoaderConfiguration().disableWebAccess());
        });
    }

    @Test
    public void testAddGraphWithVersionIRI() {
        OntModel a = OntModelFactory.createModel();
        OntModel b = OntModelFactory.createModel();
        b.setID("http://b").setVersionIRI("http://ver1");
        a.addImport(b);

        OntologyManager m = OntManagers.createManager();
        m.getOntologyConfigurator().disableWebAccess();
        m.addOntology(a.getGraph());
        Assertions.assertEquals(2, m.ontologies().count());
    }

    @Test
    public void testAddRemoveOntologyFactory() throws OWLOntologyCreationException {
        OntologyManager manager = OntManagers.createConcurrentManager();
        Assertions.assertEquals(1, manager.getOntologyFactories().size());
        manager.getOntologyFactories().clear();
        Assertions.assertTrue(manager.getOntologyFactories().isEmpty());

        OWLOntologyFactory owlFactory = new OWLFactoryWrapper.FactoryImpl((m, i) ->
                new OntManagers.OWLAPIImplProfile().createOWLOntologyImpl(m, i));
        try {
            manager.getOntologyFactories().add(owlFactory);
            Assertions.fail("Must fail");
        } catch (OntApiException e) {
            LOGGER.debug(e.getMessage());
        }
        Assertions.assertEquals(0, manager.getOntologyFactories().size());

        String comment = "Generated by test";
        OntologyFactory.Builder builder = new OntologyBuilderImpl() {
            @Override
            public Graph createDataGraph() {
                return OntModelFactory.createModel().setID(null).addComment(comment).getModel().getBaseGraph();
            }
        };
        OntologyFactory ontFactory = new OntManagers.ONTAPIProfile().createOntologyFactory(builder);
        Assertions.assertNotNull(ontFactory);
        manager.getOntologyFactories().add(ontFactory);
        Assertions.assertEquals(1, manager.getOntologyFactories().size());

        String uri1 = "http://test1.com";
        Ontology o1 = manager.createOntology(IRI.create(uri1));
        Assertions.assertNotNull(o1);
        OWLIOUtils.print(o1);
        Assertions.assertEquals(uri1, o1.getOntologyID().getOntologyIRI()
                .map(IRI::getIRIString).orElseThrow(AssertionError::new));
        Assertions.assertEquals(comment, getOWLComment(o1));

        Ontology o2 = manager.loadOntology(IRI.create(OWLIOUtils.getResourceURI("/ontapi/test1.ttl")));
        Assertions.assertNotNull(o2);
        OWLIOUtils.print(o2);
        Assertions.assertEquals("http://test.test/complex", o2.getOntologyID().getOntologyIRI()
                .map(IRI::getIRIString).orElseThrow(AssertionError::new));
        Assertions.assertEquals("http://test.test/complex/version-iri/1.0", o2.getOntologyID().getVersionIRI()
                .map(IRI::getIRIString).orElseThrow(AssertionError::new));
        Assertions.assertEquals(comment, getOWLComment(o2));
    }

    @Test
    public void testNativeTurtleOWLParser() throws OWLOntologyCreationException {
        OntConfig conf = new OntConfig()
                .addIgnoredImport(IRI.create("http://spinrdf.org/sp"))
                .addIgnoredImport(IRI.create("http://spinrdf.org/spin"))
                .disableWebAccess();
        Assertions.assertFalse(conf.buildLoaderConfiguration().isUseOWLParsersToLoad());

        OWLOntologyDocumentSource source = new IRIDocumentSource(IRI.create(OWLIOUtils.getResourceURI("/etc/spl.spin.ttl")));

        // Load using Jena turtle reader:
        OntologyManager m1 = OntManagers.createManager();
        Ontology o1 = m1.loadOntologyFromOntologyDocument(source, conf.buildLoaderConfiguration());
        Assertions.assertEquals(1, m1.ontologies().count());
        Assertions.assertEquals(0, o1.asGraphModel().imports().count());
        // check all []-lists are valid:
        List<RDFList> lists = o1.asGraphModel()
                .statements(null, SP.where, null)
                .map(Statement::getObject).map(o -> o.as(RDFList.class)).toList();
        Assertions.assertEquals(40, lists.size());
        Assertions.assertTrue(lists.stream().allMatch(RDFList::isValid));

        // Load using OWL-API Turtle Parser
        OntologyManager m2 = OntManagers.createManager();
        Ontology o2 = m2.loadOntologyFromOntologyDocument(source,
                conf.buildLoaderConfiguration().setUseOWLParsersToLoad(true));
        Assertions.assertEquals(1, m2.ontologies().count());
        Assertions.assertEquals(0, o2.asGraphModel().imports().count());
        OWLIOUtils.print(o2);
        // Due to buggy OWL-API Parser behaviour there is no []-lists at all!:
        Assertions.assertTrue(o2.asGraphModel().statements(null, null, null)
                .map(Statement::getObject).noneMatch(l -> l.canAs(RDFList.class)));
    }

    @Test
    public void testLoadWithDisabledProcessImports() throws OWLOntologyCreationException {
        OntologyManager m = OntManagers.createManager();
        String uri_a = "urn:a";
        String uri_b = "urn:b";
        String prefixes = """
                @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix owl:   <http://www.w3.org/2002/07/owl#> .
                @prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
                @prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .""";

        String txt1 = String.format("%s[ a owl:Ontology; owl:imports  <%s>, <%s> ].", prefixes, uri_a, uri_b);
        OWLOntologyDocumentSource src1 = OWLIOUtils.getStringDocumentSource(txt1, OntFormat.TURTLE);
        Assertions.assertTrue(m.getOntologyConfigurator().isProcessImports());
        Assertions.assertTrue(m.getOntologyLoaderConfiguration().isProcessImports());
        Ontology o1 = m.loadOntologyFromOntologyDocument(src1,
                m.getOntologyLoaderConfiguration().setProcessImports(false));
        OWLIOUtils.print(o1);

        Assertions.assertTrue(m.getOntologyConfigurator().isProcessImports());
        Assertions.assertTrue(m.getOntologyLoaderConfiguration().isProcessImports());

        Assertions.assertEquals(1, m.ontologies().count());
        Assertions.assertEquals(2, o1.importsDeclarations().count());
        Assertions.assertEquals(0, o1.directImports().count());

        String txt2 = String.format("%s <%s> a owl:Ontology; owl:imports <%s> .", prefixes, uri_a, uri_b);
        OWLOntologyDocumentSource src2 = OWLIOUtils.getStringDocumentSource(txt2, OntFormat.TURTLE);
        Ontology o2 = m.loadOntologyFromOntologyDocument(src2,
                m.getOntologyLoaderConfiguration().setProcessImports(false));
        OWLIOUtils.print(o2);

        Assertions.assertEquals(2, m.ontologies().count());
        Assertions.assertEquals(2, o1.importsDeclarations().count());
        Assertions.assertEquals(1, o2.importsDeclarations().count());
        Assertions.assertEquals(1, o1.directImports().count());
        Assertions.assertEquals(0, o2.directImports().count());

        String txt3 = String.format("%s <%s> a owl:Ontology .", prefixes, uri_b);
        OWLOntologyDocumentSource src3 = OWLIOUtils.getStringDocumentSource(txt3, OntFormat.TURTLE);
        Ontology o3 = m.loadOntologyFromOntologyDocument(src3,
                m.getOntologyLoaderConfiguration().setProcessImports(false));
        OWLIOUtils.print(o3);

        Assertions.assertEquals(3, m.ontologies().count());
        Assertions.assertEquals(2, o1.imports().count());
        Assertions.assertEquals(1, o2.imports().count());
        Assertions.assertEquals(0, o3.directImports().count());
        Assertions.assertEquals(0, o3.imports().count());
    }

    @Test
    public void testControlUnionGraphs() throws OWLOntologyCreationException {
        class MyUnion extends UnionGraphImpl {
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

        OntologyManager m = OntManagers.createManager();
        m.getOntologyFactories().clear();
        Assertions.assertEquals(0, m.getOntologyFactories().size());
        m.getOntologyFactories().add(factory);

        String uri_a = "urn:a";
        String uri_b = "urn:b";
        String uri_c = "urn:c";
        String prefixes = """
                @prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix owl:   <http://www.w3.org/2002/07/owl#> .
                @prefix xsd:   <http://www.w3.org/2001/XMLSchema#> .
                @prefix rdfs:  <http://www.w3.org/2000/01/rdf-schema#> .""";
        String txt_a = String.format("%s <%s> a owl:Ontology; owl:imports <%s> .", prefixes, uri_a, uri_b);
        String txt_b = String.format("%s <%s> a owl:Ontology .", prefixes, uri_b);

        OWLOntologyDocumentSource src_a = OWLIOUtils.getStringDocumentSource(txt_a, OntFormat.TURTLE);
        OWLOntologyDocumentSource src_b = OWLIOUtils.getStringDocumentSource(txt_b, OntFormat.TURTLE);

        Ontology b = m.loadOntologyFromOntologyDocument(src_b);
        Ontology a = m.loadOntologyFromOntologyDocument(src_a);
        Ontology c = m.createOntology(IRI.create(uri_c));

        m.applyChange(new AddImport(c, m.getOWLDataFactory().getOWLImportsDeclaration(IRI.create(uri_a))));

        Assertions.assertInstanceOf(MyUnion.class, a.asGraphModel().getGraph());
        Assertions.assertInstanceOf(MyUnion.class, b.asGraphModel().getGraph());
        Assertions.assertInstanceOf(MyUnion.class, c.asGraphModel().getGraph());

        Assertions.assertEquals(3, MiscTestUtils.importsClosure(c.asGraphModel()).count());
        Assertions.assertEquals(2, MiscTestUtils.importsClosure(a.asGraphModel()).count());
        Assertions.assertEquals(1, MiscTestUtils.importsClosure(b.asGraphModel()).count());
        MiscTestUtils.importsClosure(c.asGraphModel())
                .forEach(x -> Assertions.assertInstanceOf(MyUnion.class, x.getGraph()));
        MiscTestUtils.importsClosure(a.asGraphModel())
                .forEach(x -> Assertions.assertInstanceOf(MyUnion.class, x.getGraph()));
        MiscTestUtils.importsClosure(b.asGraphModel())
                .forEach(x -> Assertions.assertInstanceOf(MyUnion.class, x.getGraph()));
    }

    @Test
    public void testLoadConfigurationWWithUseOWLParsersOption() throws OWLOntologyCreationException {
        OWLOntologyDocumentSource src = OWLIOUtils.getFileDocumentSource("/ontapi/test2.owl", OntFormat.OWL_XML);
        OntologyManager m = OntManagers.createManager();
        OntLoaderConfiguration conf = m.getOntologyLoaderConfiguration().setUseOWLParsersToLoad(true);
        Ontology o = m.loadOntologyFromOntologyDocument(src, conf);
        OWLAdapter adapter = OWLAdapter.get();
        OntLoaderConfiguration conf2 = adapter.asModelConfig(adapter.asBaseModel(o).getConfig()).getLoaderConfig();
        Assertions.assertSame(conf, conf2);
    }

    @Test
    public void testLoadConfigurationWithOWLAPIFactory() throws OWLOntologyCreationException {
        OWLOntologyDocumentSource src = OWLIOUtils.getFileDocumentSource("/ontapi/test2.omn", OntFormat.MANCHESTER_SYNTAX);
        OntologyManager m = createManagerWithOWLAPIOntologyFactory();
        OWLParserFactory parser = OWLLangRegistry.getLang(OWLLangRegistry.LangKey.MANCHESTERSYNTAX.getKey())
                .orElseThrow(AssertionError::new).getParserFactory();
        m.getOntologyParsers().add(parser);
        Assertions.assertEquals(1, m.getOntologyFactories().size());
        Assertions.assertEquals(1, m.getOntologyParsers().size());
        Assertions.assertEquals(0, m.getOntologyStorers().size());
        OntLoaderConfiguration conf = m.getOntologyLoaderConfiguration().setUseOWLParsersToLoad(true);
        Ontology o = m.loadOntologyFromOntologyDocument(src, conf);
        OWLAdapter adapter = OWLAdapter.get();
        OntLoaderConfiguration conf2 = adapter.asModelConfig(adapter.asBaseModel(o).getConfig()).getLoaderConfig();
        Assertions.assertSame(conf, conf2);
    }

    @Test
    public void testErrorWhenNoParserFound() {
        OWLOntologyDocumentSource src = OWLIOUtils.getFileDocumentSource("/ontapi/test2.fss", OntFormat.FUNCTIONAL_SYNTAX);
        OntologyManager m = createManagerWithOWLAPIOntologyFactory();
        Assertions.assertEquals(1, m.getOntologyFactories().size());
        Assertions.assertEquals(0, m.getOntologyParsers().size());
        Assertions.assertEquals(0, m.getOntologyStorers().size());

        try {
            m.loadOntologyFromOntologyDocument(src);
            Assertions.fail("Possible to load");
        } catch (UnparsableOntologyException e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }
}
