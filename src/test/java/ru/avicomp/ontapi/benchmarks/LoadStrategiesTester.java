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

package ru.avicomp.ontapi.benchmarks;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.*;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Not a test: only for manual running!
 * Created by @szuev on 23.02.2018.
 */
@Ignore
@RunWith(Parameterized.class)
public class LoadStrategiesTester {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadStrategiesTester.class);

    private static List<TestData> ontologies = Arrays.asList(
            /*new TestData( // put to 'test/resources' directory and uncomment
                    "test-ontology",
                    LoadStrategiesTester.class.getResource("/ontolog.rdf.xml"), OntFormat.RDF_XML, "http://coim/intellect/ontolog",
                    7464, 1002, true),*/
            new TestData(
                    "pizza",
                    LoadStrategiesTester.class.getResource("/ontapi/pizza.ttl"), OntFormat.TURTLE, "http://www.co-ode.org/ontologies/pizza/pizza.owl",
                    945, 100, true),
            new TestData(
                    "teleost",
                    toURL("https://data.bioontology.org/ontologies/TTO/download?apikey=8b5b7825-538d-40e0-9e9e-5ab9274a9aeb&download_format=rdf"), OntFormat.RDF_XML, null, // ~50 MB
                    375007, 38705, false),
            new TestData("psychology",
                    toURL("https://data.bioontology.org/ontologies/APAONTO/download?apikey=8b5b7825-538d-40e0-9e9e-5ab9274a9aeb&download_format=rdf"), OntFormat.RDF_XML, null, // ~5 MB
                    38872, 6037, false),
            new TestData(
                    "galen",
                    toURL("https://data.bioontology.org/ontologies/GALEN/download?apikey=8b5b7825-538d-40e0-9e9e-5ab9274a9aeb&download_format=rdf"), OntFormat.RDF_XML, null, // ~20 MB
                    96463, 23141, false)
    );

    private static Map<MethodName, Map<String, List<Double>>> res = new EnumMap<>(MethodName.class);
    private final TestData data;
    private final Strategy type;

    public LoadStrategiesTester(TestData data, Strategy type) {
        this.data = data;
        this.type = type;
    }

    @BeforeClass
    public static void download() throws Exception {
        for (TestData d : ontologies) {
            if (d.hasLocation()) continue;
            Path dst = ReadWriteUtils.getOutPath(d.dataName() + "." + d.format().getExt());
            if (Files.exists(dst)) {
                LOGGER.debug("File {} already downloaded", dst);
            } else {
                LOGGER.info("Download {} => {}", d.source(), dst);
                OntologyManager m = OntManagers.createONT();
                m.getOntologyConfigurator().setPerformTransformation(false);
                // should be fast: downloading without axioms collection.
                OntologyModel o = m.loadOntologyFromOntologyDocument(createSource(d.source(), d.format()));
                o.saveOntology(IRI.create(dst.toFile()));
                Assert.assertTrue(Files.exists(dst));
            }
            d.setLocation(dst.toUri().toURL());
        }
    }

    @Before
    public void before() {
        LOGGER.debug("Clear OWL-API caches");
        OWLAPICaches.clearAll();
    }

    @Parameterized.Parameters(name = "{1}-{0}")
    public static Object[][] data() {
        return ontologies.stream()
                .flatMap(d -> strategies(d.format(), d.withExperimental()).map(s -> new Object[]{d, s}))
                .toArray(Object[][]::new);
    }

    @Test
    public void testLoadAxioms() {
        int axioms = data.axiomsCount();
        Assume.assumeTrue("Skipped. Data: " + data, axioms > 0);
        OWLOntologyManager m = type.create();
        Instant s = Instant.now();
        OWLOntology o = type.load(m, data.location());
        // validate data:
        String name = getOntologyName(data, o);
        // axioms: load(in case of ONT-API) or just get(in case of OWL-API)
        LOGGER.info("Counting axioms for <{}>", name);
        int count = o.getAxiomCount();
        LOGGER.info("Axioms count: {}", count);
        Instant e = Instant.now();

        finalAction(MethodName.AXIOMS, type, s, e);
        assertEquals(isONT(o), "Wrong axioms count", axioms, count);
    }

    @Test
    public void testLoadClasses() {
        int classes = data.classesCount();
        Assume.assumeTrue("Skipped. Data: " + data, classes > 0);
        OWLOntologyManager m = type.create();
        Instant s = Instant.now();
        OWLOntology o = type.load(m, data.location());
        String name = getOntologyName(data, o);
        LOGGER.info("Counting classes for <{}>", name);
        long count = isONT(o) ? ((OntologyModel) o).asGraphModel().listClasses().count() : o.classesInSignature().count();
        LOGGER.info("Classes count: {}", count);
        Instant e = Instant.now();

        finalAction(MethodName.CLASSES, type, s, e);
        assertEquals(isONT(o), "Wrong classes count", classes, count);
    }

    @AfterClass
    public static void info() {
        res.forEach((name, map) -> {
            LOGGER.info("Method={}", name);
            Map<String, Double> averages = map.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().mapToDouble(v -> v).average().orElse(-1)));
            averages.forEach((strategy, average) -> LOGGER.info("[{}][{}]:::{}",
                    name, strategy, average));
            ratio(name, averages, StandardStrategy.ONT_LIGHT, StandardStrategy.OWLAPI_COMMON);
        });
    }

    private static void ratio(MethodName name, Map<String, Double> averages, StandardStrategy left, StandardStrategy right) {
        LOGGER.info("[{}][{}/{}]={}", name, left, right, averages.get(left.name()) / averages.get(right.name()));
    }

    private static void finalAction(MethodName method, Strategy str, Instant s, Instant e) {
        double d = duration(s, e);
        LOGGER.info("Seconds {}", d);
        res.computeIfAbsent(method, k -> new HashMap<>()).computeIfAbsent(str.toString(), k -> new ArrayList<>()).add(d);
    }

    private static double duration(Instant s, Instant e) {
        Duration d = Duration.between(s, e);
        return d.get(ChronoUnit.SECONDS) + d.get(ChronoUnit.NANOS) / 1_000_000_000.0;
    }

    private static String getOntologyName(TestData data, OWLOntology o) {
        if (!data.isAnon()) {
            IRI iri = o.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new);
            Assert.assertEquals(data.ontologyIRI(), iri.toString());
            return iri.toString();
        } else {
            Assert.assertTrue(o.isAnonymous());
            return data.dataName();
        }
    }

    private static boolean isONT(OWLOntology o) {
        return o instanceof OntologyModel;
    }

    private static void assertEquals(boolean doThrow, String message, long expected, long actual) {
        if (doThrow) {
            Assert.assertEquals(message, expected, actual);
            return;
        }
        if (expected == actual) return;
        LOGGER.error(message + ":\n Expected :" + expected + "\n Actual   :" + actual);
    }

    private enum StandardStrategy {
        OWLAPI_CONCURRENT {
            @Override
            public OWLOntologyManager create() {
                return OntManagers.createConcurrentOWL();
            }
        },
        OWLAPI_COMMON {
            @Override
            public OWLOntologyManager create() {
                return OntManagers.createOWL();
            }
        },

        ONT_CONCURRENT {
            @Override
            public OWLOntologyManager create() {
                return OntManagers.createConcurrentONT();
            }
        },
        ONT_COMMON {
            @Override
            OWLOntologyManager create() {
                return OntManagers.createONT();
            }
        },
        ONT_LIGHT {
            @Override
            public OWLOntologyManager create() {
                OntologyManager m = OntManagers.createONT();
                m.getOntologyConfigurator()
                        .setPersonality(OntModelConfig.ONT_PERSONALITY_LAX)
                        .setPerformTransformation(false);
                return m;
            }
        },;

        abstract OWLOntologyManager create();


    }

    public static Stream<Strategy> strategies(OntFormat format, boolean withExperimental) {
        Stream<Strategy> res = Arrays.stream(StandardStrategy.values()).map(t -> new Strategy() {
            @Override
            public OWLOntologyManager create() {
                return t.create();
            }

            @Override
            public OWLOntology load(OWLOntologyManager manager, URL file) {
                return LoadStrategiesTester.load(manager, file, format);
            }

            @Override
            public String toString() {
                return t.name();
            }
        });
        if (!withExperimental) return res;
        Stream<Strategy> b = format.owlLangs().filter(OWLLangRegistry.OWLLang::isReadable).map(l -> new Strategy() {
            @Override
            public OWLOntologyManager create() {
                return OntManagers.createONT();
            }

            @Override
            public OWLOntology load(OWLOntologyManager manager, URL file) {
                return LoadStrategiesTester.load(manager, file, l);
            }

            @Override
            public String toString() {
                return "ONT_THROUGH_OWLAPI-" + l.getType().getSimpleName();
            }
        });
        return Stream.concat(res, b);
    }

    public static OWLOntology load(OWLOntologyManager manager, URL file, OntFormat format) {
        try {
            OWLOntologyDocumentSource source = createSource(file, format);
            LOGGER.debug("Source: {}", source.getDocumentIRI());
            return manager.loadOntologyFromOntologyDocument(source);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }

    public static OWLOntologyDocumentSource createSource(URL url, OntFormat format) {
        Assert.assertNotNull("No source", url);
        URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return new FileDocumentSource(Paths.get(uri).toFile(), format.createOwlFormat());
        }
        return new IRIDocumentSource(IRI.create(uri), format.createOwlFormat(), null);
    }

    public static URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Non-canonical way to load ontology (using owl-api parsers)
     *
     * @param manager {@link OWLOntologyManager}
     * @param file    {@link URL}
     * @param lang    {@link ru.avicomp.ontapi.OWLLangRegistry.OWLLang}
     * @return {@link OWLOntology}
     */
    public static OWLOntology load(OWLOntologyManager manager, URL file, OWLLangRegistry.OWLLang lang) {
        int num = (int) manager.ontologies().count();
        LOGGER.info("LANG: {}", lang);
        Assert.assertNotNull(lang);
        OWLOntology res;
        try {
            res = manager.createOntology();
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
        OWLParserFactory factory = lang.getParserFactory();
        // add axioms one by one:
        factory.createParser().parse(IRI.create(file), res);
        Assert.assertEquals(num + 1, manager.ontologies().count());
        return res;
    }

    private enum MethodName {
        AXIOMS,
        CLASSES,
    }

    interface Strategy {
        OWLOntologyManager create();

        OWLOntology load(OWLOntologyManager manager, URL file);
    }

    private static class TestData {
        private final String name;
        private URL location;
        private final URL source;
        private final OntFormat format;
        private final String iri;
        private final int axiomsCount, classesCount;
        private boolean includeExperimentalStrategies;

        private TestData(String name, URL source, OntFormat format, String iri, int axiomsCount, int classesCount, boolean withExperimental) {
            this.name = name;
            this.source = source;
            this.format = format;
            this.iri = iri;
            this.axiomsCount = axiomsCount;
            this.classesCount = classesCount;
            this.includeExperimentalStrategies = withExperimental;
        }


        String dataName() {
            return name;
        }

        @Override
        public String toString() {
            return "{" + name.toUpperCase() + "}";
        }

        boolean withExperimental() {
            return includeExperimentalStrategies;
        }

        boolean hasLocation() {
            return location != null;
        }

        URL source() {
            return source;
        }

        URL location() {
            return location == null ? source : location;
        }

        OntFormat format() {
            return format;
        }

        String ontologyIRI() {
            return iri;
        }

        boolean isAnon() {
            return iri == null;
        }

        void setLocation(URL url) {
            this.location = url;
        }

        int axiomsCount() {
            return axiomsCount;
        }

        int classesCount() {
            return classesCount;
        }

    }

}
