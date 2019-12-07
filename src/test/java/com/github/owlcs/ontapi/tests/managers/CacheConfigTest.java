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
import com.github.owlcs.ontapi.config.CacheSettings;
import com.github.owlcs.ontapi.config.OntConfig;
import com.github.owlcs.ontapi.config.OntLoaderConfiguration;
import com.github.owlcs.ontapi.config.OntSettings;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.jena.impl.OntGraphModelImpl;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.WrappedGraph;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by @ssz on 04.03.2019.
 *
 * @see com.github.owlcs.ontapi.config.CacheSettings
 */
public class CacheConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfigTest.class);

    private static void testConfigureIRICacheSize(OntologyManager m) {
        int def = Prop.IRI_CACHE_SIZE.getInt();
        OntConfig c1 = m.getOntologyConfigurator();
        Assert.assertNotNull(c1);
        Assert.assertEquals(def, c1.getManagerIRIsCacheSize());
        Assert.assertEquals(def, m.getOntologyConfigurator().getManagerIRIsCacheSize());

        OntConfig c2 = new OntConfig() {
            @Override
            protected OntConfig setManagerIRIsCacheSize(int size) {
                return super.setManagerIRIsCacheSize(size);
            }
        }.setManagerIRIsCacheSize(1);
        Assert.assertEquals(1, c2.getManagerIRIsCacheSize());
        m.setOntologyConfigurator(c2);
        Assert.assertEquals(1, m.getOntologyConfigurator().getManagerIRIsCacheSize());
    }

    private static InternalCache<?, ?> getInternalCache(CacheObjectFactory of,
                                                        Class<? extends OWLEntity> type) throws Exception {
        return getPrivateField(of, InternalCache.Loading.class, type).asCache();
    }

    private static InternalCache.Loading<?, ?> getInternalCache(InternalModel m,
                                                                Class<? extends Enum<?>> type) throws Exception {
        return getPrivateField(m, InternalCache.Loading.class, type);
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private static <X> X getPrivateField(Object container,
                                         Class<X> type,
                                         Class<?>... genericTypes) throws Exception {
        Field res = null;
        for (Field f : container.getClass().getDeclaredFields()) {
            String name = f.getGenericType().getTypeName();
            if (!name.contains(type.getName()))
                continue;
            if (Arrays.stream(genericTypes).map(Class::getName).allMatch(name::contains)) {
                res = f;
                break;
            }
        }
        Assert.assertNotNull(res);
        res.setAccessible(true);
        return (X) res.get(container);
    }

    private static void testLoadManchesterString(OntologyManager m) throws OWLOntologyCreationException {
        String input = "Prefix: o: <urn:test#>\n" +
                "Ontology: <urn:test>\n" +
                "AnnotationProperty: o:bob\n" +
                "Annotations:\n" +
                "rdfs:label \"bob-label\"@en";
        OWLOntologyDocumentSource source = ReadWriteUtils.getStringDocumentSource(input, OntFormat.MANCHESTER_SYNTAX);
        Ontology o = m.loadOntologyFromOntologyDocument(source);
        Assert.assertNotNull(o);
        Assert.assertEquals(2, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
    }

    @Test
    public void testConfigureManagerIRICacheSize() {
        testConfigureIRICacheSize(OntManagers.createONT());
        testConfigureIRICacheSize(OntManagers.createConcurrentONT());
    }

    @Test
    public void testNodesCacheSize() throws Exception {
        Assert.assertEquals(Prop.NODES_CACHE_SIZE.getInt(), new OntConfig().getLoadNodesCacheSize());
        OntologyManager m = OntManagers.createONT();
        Assert.assertNotNull(m.getOntologyConfigurator().setLoadNodesCacheSize(-123));
        Assert.assertEquals(-123, m.getOntologyLoaderConfiguration().getLoadNodesCacheSize());
        // cache is disabled, try to load model
        Ontology o = m.loadOntologyFromOntologyDocument(ReadWriteUtils.getFileDocumentSource("/ontapi/pizza.ttl",
                OntFormat.TURTLE));
        Assert.assertNotNull(o);
        Assert.assertEquals(945, o.axioms().count());

        OntGraphModelImpl m1 = ((BaseModel) o).getBase().getSearchModel();
        Assert.assertTrue(m1 instanceof InternalModel);

        m.setOntologyLoaderConfiguration(m.getOntologyLoaderConfiguration().setLoadNodesCacheSize(10_000));
        OntGraphModelImpl m2 = ((BaseModel) o).getBase().getSearchModel();
        Assert.assertTrue(m2 instanceof SearchModel);
    }

    @Test
    public void testObjectsCacheSize() throws Exception {
        long axioms = 945;
        OntologyManager m = OntManagers.createONT();
        Assert.assertEquals(Prop.OBJECTS_CACHE_SIZE.getInt(), m.getOntologyConfigurator().getLoadObjectsCacheSize());
        OntLoaderConfiguration conf = new OntConfig().buildLoaderConfiguration().setLoadObjectsCacheSize(-1);
        Assert.assertEquals(-1, conf.getLoadObjectsCacheSize());
        m.setOntologyLoaderConfiguration(conf);
        Ontology o = m.loadOntologyFromOntologyDocument(ReadWriteUtils.getFileDocumentSource("/ontapi/pizza.ttl",
                OntFormat.TURTLE));
        Assert.assertNotNull(o);
        Assert.assertEquals(axioms, o.axioms().count());
        InternalObjectFactory of1 = ((BaseModel) o).getBase().getObjectFactory();
        Assert.assertTrue(of1 instanceof ModelObjectFactory);
        Assert.assertFalse(of1 instanceof CacheObjectFactory);

        int size1 = 52;
        m.setOntologyLoaderConfiguration(conf.setLoadObjectsCacheSize(size1));
        Assert.assertEquals(axioms, o.axioms().count());
        InternalObjectFactory of2 = ((BaseModel) o).getBase().getObjectFactory();
        Assert.assertTrue(of2 instanceof CacheObjectFactory);
        CacheObjectFactory cof1 = (CacheObjectFactory) of2;

        Assert.assertEquals(size1, getInternalCache(cof1, OWLClass.class).size());
        Assert.assertEquals(2, getInternalCache(cof1, OWLDatatype.class).size());
        Assert.assertEquals(5, getInternalCache(cof1, OWLNamedIndividual.class).size());
        Assert.assertEquals(2, getInternalCache(cof1, OWLAnnotationProperty.class).size());
        Assert.assertEquals(0, getInternalCache(cof1, OWLDataProperty.class).size());
        Assert.assertEquals(8, getInternalCache(cof1, OWLObjectProperty.class).size());

        int size2 = 2;
        m.setOntologyLoaderConfiguration(conf.setLoadObjectsCacheSize(size2));
        Assert.assertEquals(axioms, o.axioms().count());
        InternalObjectFactory of3 = ((BaseModel) o).getBase().getObjectFactory();
        Assert.assertTrue(of3 instanceof CacheObjectFactory);
        CacheObjectFactory cof2 = (CacheObjectFactory) of3;

        Assert.assertEquals(size2, getInternalCache(cof2, OWLClass.class).size());
        Assert.assertEquals(2, getInternalCache(cof2, OWLDatatype.class).size());
        Assert.assertEquals(size2, getInternalCache(cof2, OWLNamedIndividual.class).size());
        Assert.assertEquals(size2, getInternalCache(cof2, OWLAnnotationProperty.class).size());
        Assert.assertEquals(0, getInternalCache(cof2, OWLDataProperty.class).size());
        Assert.assertEquals(size2, getInternalCache(cof2, OWLObjectProperty.class).size());
    }

    @Test
    public void testContentCacheOption() {
        Graph g = ReadWriteUtils.loadResourceTTLFile("/ontapi/pizza.ttl").getGraph();

        long axioms1 = 945;
        OntologyManager m1 = OntManagers.createONT();
        DataFactory df = m1.getOWLDataFactory();
        Assert.assertEquals(CacheSettings.CACHE_ALL, Prop.CONTENT_CACHE_LEVEL.getInt());
        Assert.assertTrue(m1.getOntologyConfigurator().useContentCache());
        LogFindGraph g1 = new LogFindGraph(g);
        Ontology o1 = m1.addOntology(g1);
        Assert.assertEquals(axioms1, o1.axioms().count());
        int count1 = g1.getFindPatterns().size();
        LOGGER.debug("1) Find invocation count: {}", count1);
        // cached:
        Assert.assertEquals(axioms1, o1.axioms().count());
        Assert.assertEquals(count1, g1.getFindPatterns().size());
        OWLAxiom axiom = df.getOWLSubClassOfAxiom(df.getOWLClass("A"), df.getOWLClass("B"));
        o1.add(axiom);

        // no cache model:
        long axioms2 = 948;
        OntologyManager m2 = OntManagers.createONT();
        OntLoaderConfiguration conf = m2.getOntologyLoaderConfiguration()
                .setModelCacheLevel(CacheSettings.CACHE_ALL, false);
        Assert.assertFalse(conf.useContentCache());
        m2.setOntologyLoaderConfiguration(conf);
        LogFindGraph g2 = new LogFindGraph(g);
        Ontology o2 = m2.addOntology(g2);
        Assert.assertEquals(axioms2, o2.axioms().count());
        int count2_1 = g2.getFindPatterns().size();
        LOGGER.debug("2) Find invocation count: {}", count2_1);

        Assert.assertEquals(axioms2, o2.axioms().count());
        int count2_2 = g2.getFindPatterns().size();
        Assert.assertTrue(count2_2 > count2_1);

        Assert.assertEquals(axioms2, o2.axioms().count());
        Assert.assertEquals(2 * count2_2 - count2_1, g2.getFindPatterns().size());

        int size = g.size();
        try {
            o2.add(df.getOWLSubClassOfAxiom(df.getOWLClass("C"), df.getOWLClass("D")));
            Assert.fail("Possible to add axiom");
        } catch (OntologyModelImpl.ModificationDeniedException e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        }
        Assert.assertEquals(size, g.size());

        try {
            o2.remove(axiom);
            Assert.fail("Possible to delete axiom");
        } catch (OntologyModelImpl.ModificationDeniedException e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        }
        Assert.assertEquals(size, g.size());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testContentCacheInternal() throws Exception {
        int axioms = 945;
        OWLAdapter adapter = OWLAdapter.get();
        OWLOntologyDocumentSource src = ReadWriteUtils.getFileDocumentSource("/ontapi/pizza.ttl", OntFormat.TURTLE);

        OntologyManager m1 = OntManagers.createONT();
        Ontology o1 = m1.loadOntologyFromOntologyDocument(src,
                m1.getOntologyLoaderConfiguration().setModelCacheLevel(CacheSettings.CACHE_CONTENT, true));
        InternalModel im1 = adapter.asBaseModel(o1).getBase();
        InternalCache.Loading c1 = getInternalCache(im1, OWLContentType.class);
        Assert.assertNotNull(c1);
        Map map1 = (Map) c1.get(im1);
        Assert.assertNotNull(map1);
        OWLContentType.all().forEach(k -> {
            ObjectMap v = (ObjectMap) map1.get(k);
            Assert.assertNotNull(v);
            Assert.assertFalse("Loaded: " + k, v.isLoaded());
        });
        // load axioms:
        Assert.assertEquals(axioms, o1.getAxiomCount());
        OWLContentType.all().forEach(k -> {
            ObjectMap v = (ObjectMap) map1.get(k);
            Assert.assertNotNull(v);
            if (OWLContentType.ANNOTATION.equals(k)) {
                Assert.assertFalse("Loaded: " + k, v.isLoaded());
            } else {
                Assert.assertTrue("Not loaded: " + k, v.isLoaded());
            }
        });

        OntologyManager m2 = OntManagers.createONT();
        Ontology o2 = m2.loadOntologyFromOntologyDocument(src,
                m2.getOntologyLoaderConfiguration().setModelCacheLevel(CacheSettings.CACHE_CONTENT, false));
        InternalModel im2 = adapter.asBaseModel(o2).getBase();
        InternalCache.Loading c2 = getInternalCache(im2, OWLContentType.class);
        Assert.assertNotNull(c2);
        Map map2 = (Map) c2.get(im2);
        Assert.assertNotNull(map2);
        // load axioms:
        Assert.assertEquals(axioms, o2.getAxiomCount());
        OWLContentType.all().forEach(k -> {
            ObjectMap v = (ObjectMap) map2.get(k);
            Assert.assertNotNull(v);
            Assert.assertFalse("Loaded: " + k, v.isLoaded());
        });
    }

    @Test
    public void testComponentCacheOption() {
        Graph g = ReadWriteUtils.loadResourceTTLFile("/ontapi/pizza.ttl").getGraph();

        long signature1 = 118;
        OntologyManager m1 = OntManagers.createONT();
        DataFactory df = m1.getOWLDataFactory();
        Assert.assertEquals(CacheSettings.CACHE_ALL, Prop.CONTENT_CACHE_LEVEL.getInt());
        Assert.assertTrue(m1.getOntologyConfigurator().useComponentCache());

        LogFindGraph g1 = new LogFindGraph(g);
        Ontology o1 = m1.addOntology(g1);
        Assert.assertEquals(signature1, o1.signature().count());
        int count1 = g1.getFindPatterns().size();
        LOGGER.debug("1) Find invocation count: {}", count1);
        // cached:
        Assert.assertEquals(signature1, o1.signature().count());
        Assert.assertEquals(count1, g1.getFindPatterns().size());
        OWLAxiom axiom = df.getOWLSubClassOfAxiom(df.getOWLClass("A"), df.getOWLClass("B"));
        o1.add(axiom);
        Assert.assertEquals(signature1 + 2, o1.signature().count());

        // no cache model:
        long signature2 = signature1 + 2;
        OntologyManager m2 = OntManagers.createONT();
        OntLoaderConfiguration conf = m2.getOntologyLoaderConfiguration()
                .setModelCacheLevel(CacheSettings.CACHE_COMPONENT, false);
        Assert.assertFalse(conf.useComponentCache());
        Assert.assertTrue(conf.useContentCache());
        LogFindGraph g2 = new LogFindGraph(g);
        Ontology o2 = m2.addOntology(g2, conf);
        Assert.assertEquals(signature2, o2.signature().distinct().count());
        int count2_1 = g2.getFindPatterns().size();
        LOGGER.debug("2) Find invocation count: {}", count2_1);

        Assert.assertEquals(signature2, o2.signature().distinct().count());
        int count2_2 = g2.getFindPatterns().size();
        LOGGER.debug("3) Find invocation count: {}", count2_2);
        // currently components cache uses content cache in any case -> no read operations, count is same
        Assert.assertEquals(count2_1, count2_2);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testComponentCacheInternal() throws Exception {
        long signature = 118;
        OWLAdapter adapter = OWLAdapter.get();
        List<OWLComponentType> keys = Arrays.asList(OWLComponentType.CLASS
                , OWLComponentType.DATATYPE
                , OWLComponentType.ANNOTATION_PROPERTY
                , OWLComponentType.DATATYPE_PROPERTY
                , OWLComponentType.NAMED_OBJECT_PROPERTY
                , OWLComponentType.NAMED_INDIVIDUAL
                , OWLComponentType.ANONYMOUS_INDIVIDUAL);
        OWLOntologyDocumentSource src = ReadWriteUtils.getFileDocumentSource("/ontapi/pizza.ttl", OntFormat.TURTLE);

        OntologyManager m1 = OntManagers.createONT();
        Ontology o1 = m1.loadOntologyFromOntologyDocument(src,
                m1.getOntologyLoaderConfiguration().setModelCacheLevel(CacheSettings.CACHE_COMPONENT, true));
        InternalModel im1 = adapter.asBaseModel(o1).getBase();
        InternalCache.Loading c1 = getInternalCache(im1, OWLComponentType.class);
        Assert.assertNotNull(c1);
        Map map1 = (Map) c1.get(im1);
        Assert.assertNotNull(map1);
        keys.forEach(k -> {
            ObjectMap v = (ObjectMap) map1.get(k);
            Assert.assertNotNull(v);
            Assert.assertFalse("Loaded " + k, v.isLoaded());
        });
        // load signature:
        Assert.assertEquals(signature, o1.signature().count());
        keys.forEach(k -> {
            ObjectMap v = (ObjectMap) map1.get(k);
            Assert.assertNotNull(v);
            if (OWLComponentType.ANONYMOUS_INDIVIDUAL.equals(k)) {
                Assert.assertFalse("Loaded " + k, v.isLoaded());
            } else {
                Assert.assertTrue("Not loaded: " + k, v.isLoaded());
            }
        });

        OntologyManager m2 = OntManagers.createONT();
        Ontology o2 = m2.loadOntologyFromOntologyDocument(src,
                m2.getOntologyLoaderConfiguration().setModelCacheLevel(CacheSettings.CACHE_COMPONENT, false));
        InternalModel im2 = adapter.asBaseModel(o2).getBase();
        InternalCache.Loading c2 = getInternalCache(im2, OWLComponentType.class);
        Assert.assertNotNull(c2);
        Map map2 = (Map) c2.get(im2);
        Assert.assertNotNull(map2);
        // load signature:
        Assert.assertEquals(signature, o2.signature().distinct().count());
        keys.forEach(k -> {
            ObjectMap v = (ObjectMap) map2.get(k);
            Assert.assertNotNull(v);
            Assert.assertFalse("Loaded " + k, v.isLoaded());
        });
    }

    @Test
    public void testContentCacheLevels() {
        OntConfig c = new OntConfig();
        c.setModelCacheLevel(CacheSettings.CACHE_ITERATOR);
        Assert.assertFalse(c.useComponentCache());
        Assert.assertTrue(c.useIteratorCache());
        Assert.assertFalse(c.useContentCache());

        c.setModelCacheLevel(CacheSettings.CACHE_COMPONENT);
        Assert.assertTrue(c.useComponentCache());
        Assert.assertFalse(c.useIteratorCache());
        Assert.assertFalse(c.useContentCache());

        c.setModelCacheLevel(CacheSettings.CACHE_CONTENT);
        Assert.assertFalse(c.useComponentCache());
        Assert.assertFalse(c.useIteratorCache());
        Assert.assertTrue(c.useContentCache());

        c.setModelCacheLevel(CacheSettings.CACHE_ALL, false);
        Assert.assertFalse(c.useComponentCache());
        Assert.assertFalse(c.useIteratorCache());
        Assert.assertFalse(c.useContentCache());

        c.setModelCacheLevel(CacheSettings.CACHE_ALL, true);
        Assert.assertTrue(c.useComponentCache());
        Assert.assertTrue(c.useIteratorCache());
        Assert.assertTrue(c.useContentCache());
    }

    @Test
    public void testNoIteratorAndComponentCache() throws OWLOntologyCreationException {
        OWLOntologyDocumentSource s = ReadWriteUtils.getFileDocumentSource("/ontapi/pizza.ttl", OntFormat.TURTLE);

        long axioms = 945;
        OntologyManager m = OntManagers.createONT();
        DataFactory df = m.getOWLDataFactory();
        m.getOntologyConfigurator().setModelCacheLevel(CacheSettings.CACHE_CONTENT);

        Ontology o = m.loadOntologyFromOntologyDocument(s);
        Assert.assertEquals(axioms, o.getAxiomCount());
        OWLClass c = df.getOWLClass("C");
        OWLAxiom a = df.getOWLSubClassOfAxiom(c, df.getOWLNothing(), Collections.singletonList(df.getRDFSLabel("x1")));
        o.add(a);
        Assert.assertEquals(axioms + 1, o.getAxiomCount());
        o.clearCache();
        Assert.assertEquals(axioms + 2, o.getAxiomCount());

        o.remove(a);
        Assert.assertEquals(axioms + 1, o.axioms().count());
        o.remove(df.getOWLDeclarationAxiom(c));
        Assert.assertEquals(axioms, o.getAxiomCount());
    }

    @Test
    public void testLoadNativeOWLFormatWhenContentCacheIsDisabled() throws OWLOntologyCreationException {
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setModelCacheLevel(CacheSettings.CACHE_CONTENT, false);
        testLoadManchesterString(m);
    }

    @Test
    public void testLoadNativeOWLFormatWhenAllCachesAreDisabled() throws OWLOntologyCreationException {
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setModelCacheLevel(0);
        testLoadManchesterString(m);
    }

    @Test
    public void testLoadBrokenTurtleUsingOWLAPIWithNoCache() throws OWLOntologyCreationException {
        // The following TTL is wrong: it has unexpected dot at [line: 18, col: 15]
        // Since Jena fails as expected, ONT-API uses the native OWL-API Turtle Parser as alternative,
        // which is more tolerant.
        // It calls remove axiom operations, to test which this test-case is intended.
        String wrong = "@prefix : <urn:fm2#> .\n"
                + "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + "@prefix xml: <http://www.w3.org/XML/1998/namespace> .\n"
                + "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                + "@prefix prov: <urn:prov#> .\n"
                + "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "@base <urn:fm2> .\n"
                + "<http://www.ida.org/fm2.owl> rdf:type owl:Ontology.\n"
                + ":prov rdf:type owl:AnnotationProperty .\n"
                + ":Manage rdf:type owl:Class ; rdfs:subClassOf :ManagementType .\n"
                + "[ rdf:type owl:Axiom ;\n"
                + "  owl:annotatedSource :Manage ;\n"
                + "  owl:annotatedTarget :ManagementType ;\n"
                + "  owl:annotatedProperty rdfs:subClassOf ;\n"
                + "  :prov [\n"
                + " prov:gen :FMDomain ;\n"
                + " prov:att :DM .\n "
                + "]\n ] "
                + ".\n"
                + ":ManagementType rdf:type owl:Class .\n"
                + ":DM rdf:type owl:NamedIndividual , prov:Person .\n"
                + ":FMDomain rdf:type owl:NamedIndividual , prov:Activity ; prov:ass :DM .";

        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setModelCacheLevel(0);

        OWLOntologyDocumentSource source = ReadWriteUtils.getStringDocumentSource(wrong, OntFormat.TURTLE);
        Ontology o = m.loadOntologyFromOntologyDocument(source);
        ReadWriteUtils.print(o);
        Assert.assertEquals(16, o.axioms().peek(x -> LOGGER.debug("{}", x)).count());
    }

    enum Prop {
        IRI_CACHE_SIZE(OntSettings.ONT_API_MANAGER_CACHE_IRIS.key() + ".integer"),
        NODES_CACHE_SIZE(OntSettings.ONT_API_LOAD_CONF_CACHE_NODES.key() + ".integer"),
        OBJECTS_CACHE_SIZE(OntSettings.ONT_API_LOAD_CONF_CACHE_OBJECTS.key() + ".integer"),
        CONTENT_CACHE_LEVEL(OntSettings.ONT_API_LOAD_CONF_CACHE_MODEL.key() + ".integer");
        private final String key;

        Prop(String key) {
            this.key = key;
        }

        int getInt() {
            return Integer.parseInt(get());
        }

        private String get() {
            return Objects.requireNonNull(OntSettings.PROPERTIES.getProperty(key), "Null " + key);
        }

    }

    private static class LogFindGraph extends WrappedGraph {
        private final List<Triple> track = new ArrayList<>();

        LogFindGraph(Graph base) {
            super(Objects.requireNonNull(base));
        }

        @Override
        public ExtendedIterator<Triple> find(Triple m) {
            track.add(m);
            return super.find(m);
        }

        @Override
        public ExtendedIterator<Triple> find(Node s, Node p, Node o) {
            track.add(Triple.createMatch(s, p, o));
            return super.find(s, p, o);
        }

        List<Triple> getFindPatterns() {
            return track;
        }
    }
}
