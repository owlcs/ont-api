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
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.OntBaseModel;
import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.config.CacheSettings;
import com.github.owlcs.ontapi.config.OntConfig;
import com.github.owlcs.ontapi.config.OntLoaderConfiguration;
import com.github.owlcs.ontapi.config.OntSettings;
import com.github.owlcs.ontapi.internal.CacheObjectFactory;
import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.InternalGraphModelImpl;
import com.github.owlcs.ontapi.internal.InternalObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.OWLComponentType;
import com.github.owlcs.ontapi.internal.OWLTopObjectType;
import com.github.owlcs.ontapi.internal.ObjectMap;
import com.github.owlcs.ontapi.internal.SearchModel;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.WrappedGraph;
import org.apache.jena.ontapi.impl.OntGraphModelImpl;
import org.apache.jena.riot.Lang;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by @ssz on 04.03.2019.
 *
 * @see CacheSettings
 */
public class CacheConfigTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheConfigTest.class);

    static InternalGraphModelImpl getBase(Ontology o) {
        return ((InternalGraphModelImpl) ((OntBaseModel) o).getGraphModel());
    }

    private static void testConfigureIRICacheSize(OntologyManager m) {
        int def = Prop.IRI_CACHE_SIZE.getInt();
        OntConfig c1 = m.getOntologyConfigurator();
        Assertions.assertNotNull(c1);
        Assertions.assertEquals(def, c1.getManagerIRIsCacheSize());
        Assertions.assertEquals(def, m.getOntologyConfigurator().getManagerIRIsCacheSize());

        OntConfig c2 = new OntConfig() {
            @Override
            protected OntConfig setManagerIRIsCacheSize(int size) {
                return super.setManagerIRIsCacheSize(size);
            }
        }.setManagerIRIsCacheSize(1);
        Assertions.assertEquals(1, c2.getManagerIRIsCacheSize());
        m.setOntologyConfigurator(c2);
        Assertions.assertEquals(1, m.getOntologyConfigurator().getManagerIRIsCacheSize());
    }

    private static InternalCache<?, ?> getInternalCache(CacheObjectFactory of,
                                                        Class<? extends OWLEntity> type) throws Exception {
        return getPrivateField(of, InternalCache.Loading.class, type).asCache();
    }

    private static InternalCache.Loading<?, ?> getInternalCache(InternalGraphModelImpl m,
                                                                Class<? extends Enum<?>> type) throws Exception {
        return getPrivateField(m, InternalCache.Loading.class, type);
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private static <X> X getPrivateField(Object container,
                                         Class<X> type,
                                         Class<?>... genericTypes) throws Exception {
        Field res = find(container.getClass(), Class::getDeclaredFields, f -> {
            String name = f.getGenericType().getTypeName();
            return name.contains(type.getName()) && Arrays.stream(genericTypes).map(Class::getName).allMatch(name::contains);
        }).orElseThrow(AssertionError::new);
        res.setAccessible(true);
        return (X) res.get(container);
    }

    private static <X> Optional<X> find(Class<?> classType, Function<Class<?>, X[]> getFields, Predicate<X> select) {
        List<Class<?>> types = new ArrayList<>();
        Set<Class<?>> seen = new HashSet<>();
        types.add(classType);
        while (!types.isEmpty()) {
            Class<?> type = types.remove(0);
            if (!seen.add(type)) {
                continue;
            }
            Class<?> superType = type.getSuperclass();
            if (superType != null) {
                types.add(superType);
            }
            Optional<X> res = Arrays.stream(getFields.apply(type))
                    .filter(select)
                    .findFirst();
            if (res.isPresent()) return res;
        }
        return Optional.empty();
    }

    private static void testLoadManchesterString(OntologyManager m) throws OWLOntologyCreationException {
        String input = """
                Prefix: o: <urn:test#>
                Ontology: <urn:test>
                AnnotationProperty: o:bob
                Annotations:
                rdfs:label "bob-label"@en""";
        OWLOntologyDocumentSource source = OWLIOUtils.getStringDocumentSource(input, OntFormat.MANCHESTER_SYNTAX);
        Ontology o = m.loadOntologyFromOntologyDocument(source);
        Assertions.assertNotNull(o);
        Assertions.assertEquals(2, o.axioms().count());
    }

    @Test
    public void testConfigureManagerIRICacheSize() {
        testConfigureIRICacheSize(OntManagers.createManager());
        testConfigureIRICacheSize(OntManagers.createConcurrentManager());
    }

    @Test
    public void testNodesCacheSize() throws Exception {
        Assertions.assertEquals(Prop.NODES_CACHE_SIZE.getInt(), new OntConfig().getLoadNodesCacheSize());
        OntologyManager m = OntManagers.createManager();
        Assertions.assertNotNull(m.getOntologyConfigurator().setLoadNodesCacheSize(-123));
        Assertions.assertEquals(-123, m.getOntologyLoaderConfiguration().getLoadNodesCacheSize());
        // cache is disabled, try to load model
        Ontology o = m.loadOntologyFromOntologyDocument(OWLIOUtils.getFileDocumentSource("/ontapi/pizza.ttl",
                OntFormat.TURTLE));
        Assertions.assertNotNull(o);
        Assertions.assertEquals(945, o.axioms().count());

        OntGraphModelImpl m1 = getBase(o).getSearchModel();
        Assertions.assertInstanceOf(InternalGraphModelImpl.class, m1);

        m.setOntologyLoaderConfiguration(m.getOntologyLoaderConfiguration().setLoadNodesCacheSize(10_000));
        OntGraphModelImpl m2 = getBase(o).getSearchModel();
        Assertions.assertInstanceOf(SearchModel.class, m2);
    }

    @Test
    public void testObjectsCacheSize() throws Exception {
        long axioms = 945;
        OntologyManager m = OntManagers.createManager();
        Assertions.assertEquals(Prop.OBJECTS_CACHE_SIZE.getInt(), m.getOntologyConfigurator().getLoadObjectsCacheSize());
        OntLoaderConfiguration conf = new OntConfig().buildLoaderConfiguration().setLoadObjectsCacheSize(-1);
        Assertions.assertEquals(-1, conf.getLoadObjectsCacheSize());
        m.setOntologyLoaderConfiguration(conf);
        Ontology o = m.loadOntologyFromOntologyDocument(OWLIOUtils.getFileDocumentSource("/ontapi/pizza.ttl",
                OntFormat.TURTLE));
        Assertions.assertNotNull(o);
        Assertions.assertEquals(axioms, o.axioms().count());
        ONTObjectFactory of1 = getBase(o).getObjectFactory();
        Assertions.assertInstanceOf(InternalObjectFactory.class, of1);
        Assertions.assertFalse(of1 instanceof CacheObjectFactory);

        int size1 = 52;
        m.setOntologyLoaderConfiguration(conf.setLoadObjectsCacheSize(size1));
        Assertions.assertEquals(axioms, o.axioms().count());
        ONTObjectFactory of2 = getBase(o).getObjectFactory();
        Assertions.assertInstanceOf(CacheObjectFactory.class, of2);
        CacheObjectFactory cof1 = (CacheObjectFactory) of2;

        Assertions.assertEquals(size1, getInternalCache(cof1, OWLClass.class).size());
        Assertions.assertEquals(2, getInternalCache(cof1, OWLDatatype.class).size());
        Assertions.assertEquals(5, getInternalCache(cof1, OWLNamedIndividual.class).size());
        Assertions.assertEquals(2, getInternalCache(cof1, OWLAnnotationProperty.class).size());
        Assertions.assertEquals(0, getInternalCache(cof1, OWLDataProperty.class).size());
        Assertions.assertEquals(8, getInternalCache(cof1, OWLObjectProperty.class).size());

        int size2 = 2;
        m.setOntologyLoaderConfiguration(conf.setLoadObjectsCacheSize(size2));
        Assertions.assertEquals(axioms, o.axioms().count());
        ONTObjectFactory of3 = getBase(o).getObjectFactory();
        Assertions.assertInstanceOf(CacheObjectFactory.class, of3);
        CacheObjectFactory cof2 = (CacheObjectFactory) of3;

        Assertions.assertEquals(size2, getInternalCache(cof2, OWLClass.class).size());
        Assertions.assertEquals(2, getInternalCache(cof2, OWLDatatype.class).size());
        Assertions.assertEquals(size2, getInternalCache(cof2, OWLNamedIndividual.class).size());
        Assertions.assertEquals(size2, getInternalCache(cof2, OWLAnnotationProperty.class).size());
        Assertions.assertEquals(0, getInternalCache(cof2, OWLDataProperty.class).size());
        Assertions.assertEquals(size2, getInternalCache(cof2, OWLObjectProperty.class).size());
    }

    @Test
    public void testContentCacheOption() {
        Graph g = OWLIOUtils.loadResourceAsModel("/ontapi/pizza.ttl", Lang.TURTLE).getGraph();

        long axioms1 = 945;
        OntologyManager m1 = OntManagers.createManager();
        DataFactory df = m1.getOWLDataFactory();
        Assertions.assertEquals(CacheSettings.CACHE_ALL, Prop.CONTENT_CACHE_LEVEL.getInt());
        Assertions.assertTrue(m1.getOntologyConfigurator().useContentCache());
        LogFindGraph g1 = new LogFindGraph(g);
        Ontology o1 = m1.addOntology(g1);
        Assertions.assertEquals(axioms1, o1.axioms().count());
        int count1 = g1.getFindPatterns().size();
        LOGGER.debug("1) Find invocation count: {}", count1);
        // cached:
        Assertions.assertEquals(axioms1, o1.axioms().count());
        Assertions.assertEquals(count1, g1.getFindPatterns().size());
        OWLAxiom axiom = df.getOWLSubClassOfAxiom(df.getOWLClass("A"), df.getOWLClass("B"));
        o1.add(axiom);

        // no cache model:
        long axioms2 = 948;
        OntologyManager m2 = OntManagers.createManager();
        OntLoaderConfiguration conf = m2.getOntologyLoaderConfiguration()
                .setModelCacheLevel(CacheSettings.CACHE_ALL, false);
        Assertions.assertFalse(conf.useContentCache());
        m2.setOntologyLoaderConfiguration(conf);
        LogFindGraph g2 = new LogFindGraph(g);
        Ontology o2 = m2.addOntology(g2);
        Assertions.assertEquals(axioms2, o2.axioms().count());
        int count2_1 = g2.getFindPatterns().size();
        LOGGER.debug("2) Find invocation count: {}", count2_1);

        Assertions.assertEquals(axioms2, o2.axioms().count());
        int count2_2 = g2.getFindPatterns().size();
        Assertions.assertTrue(count2_2 > count2_1);

        Assertions.assertEquals(axioms2, o2.axioms().count());
        Assertions.assertEquals(2 * count2_2 - count2_1, g2.getFindPatterns().size());

        int size = g.size();
        try {
            o2.add(df.getOWLSubClassOfAxiom(df.getOWLClass("C"), df.getOWLClass("D")));
            Assertions.fail("Possible to add axiom");
        } catch (OntApiException.ModificationDenied e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        }
        Assertions.assertEquals(size, g.size());

        try {
            o2.remove(axiom);
            Assertions.fail("Possible to delete axiom");
        } catch (OntApiException.ModificationDenied e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        }
        Assertions.assertEquals(size, g.size());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testContentCacheInternal() throws Exception {
        int axioms = 945;
        OWLAdapter adapter = OWLAdapter.get();
        OWLOntologyDocumentSource src = OWLIOUtils.getFileDocumentSource("/ontapi/pizza.ttl", OntFormat.TURTLE);

        OntologyManager m1 = OntManagers.createManager();
        Ontology o1 = m1.loadOntologyFromOntologyDocument(src,
                m1.getOntologyLoaderConfiguration().setModelCacheLevel(CacheSettings.CACHE_CONTENT, true));
        InternalGraphModelImpl im1 = (InternalGraphModelImpl) adapter.asBaseModel(o1).getGraphModel();
        InternalCache.Loading c1 = getInternalCache(im1, OWLTopObjectType.class);
        Assertions.assertNotNull(c1);
        Map map1 = (Map) c1.get(im1);
        Assertions.assertNotNull(map1);
        OWLTopObjectType.all().forEach(k -> {
            ObjectMap v = (ObjectMap) map1.get(k);
            Assertions.assertNotNull(v);
            Assertions.assertFalse(v.isLoaded(), "Loaded: " + k);
        });
        // load axioms:
        Assertions.assertEquals(axioms, o1.getAxiomCount());
        OWLTopObjectType.all().forEach(k -> {
            ObjectMap v = (ObjectMap) map1.get(k);
            Assertions.assertNotNull(v);
            if (OWLTopObjectType.ANNOTATION.equals(k)) {
                Assertions.assertFalse(v.isLoaded(), "Loaded: " + k);
            } else {
                Assertions.assertTrue(v.isLoaded(), "Not loaded: " + k);
            }
        });

        OntologyManager m2 = OntManagers.createManager();
        Ontology o2 = m2.loadOntologyFromOntologyDocument(src,
                m2.getOntologyLoaderConfiguration().setModelCacheLevel(CacheSettings.CACHE_CONTENT, false));
        InternalGraphModelImpl im2 = getBase(o2);
        InternalCache.Loading c2 = getInternalCache(im2, OWLTopObjectType.class);
        Assertions.assertNotNull(c2);
        Map map2 = (Map) c2.get(im2);
        Assertions.assertNotNull(map2);
        // load axioms:
        Assertions.assertEquals(axioms, o2.getAxiomCount());
        OWLTopObjectType.all().forEach(k -> {
            ObjectMap v = (ObjectMap) map2.get(k);
            Assertions.assertNotNull(v);
            Assertions.assertFalse(v.isLoaded(), "Loaded: " + k);
        });
    }

    @Test
    public void testComponentCacheOption() {
        Graph g = OWLIOUtils.loadResourceAsModel("/ontapi/pizza.ttl", Lang.TURTLE).getGraph();

        long signature1 = 118;
        OntologyManager m1 = OntManagers.createManager();
        DataFactory df = m1.getOWLDataFactory();
        Assertions.assertEquals(CacheSettings.CACHE_ALL, Prop.CONTENT_CACHE_LEVEL.getInt());
        Assertions.assertTrue(m1.getOntologyConfigurator().useComponentCache());

        LogFindGraph g1 = new LogFindGraph(g);
        Ontology o1 = m1.addOntology(g1);
        Assertions.assertEquals(signature1, o1.signature().count());
        int count1 = g1.getFindPatterns().size();
        LOGGER.debug("1) Find invocation count: {}", count1);
        // cached:
        Assertions.assertEquals(signature1, o1.signature().count());
        Assertions.assertEquals(count1, g1.getFindPatterns().size());
        OWLAxiom axiom = df.getOWLSubClassOfAxiom(df.getOWLClass("A"), df.getOWLClass("B"));
        o1.add(axiom);
        Assertions.assertEquals(signature1 + 2, o1.signature().count());

        // no cache model:
        long signature2 = signature1 + 2;
        OntologyManager m2 = OntManagers.createManager();
        OntLoaderConfiguration conf = m2.getOntologyLoaderConfiguration()
                .setModelCacheLevel(CacheSettings.CACHE_COMPONENT, false);
        Assertions.assertFalse(conf.useComponentCache());
        Assertions.assertTrue(conf.useContentCache());
        LogFindGraph g2 = new LogFindGraph(g);
        Ontology o2 = m2.addOntology(g2, conf);
        Assertions.assertEquals(signature2, o2.signature().distinct().count());
        int count2_1 = g2.getFindPatterns().size();
        LOGGER.debug("2) Find invocation count: {}", count2_1);

        Assertions.assertEquals(signature2, o2.signature().distinct().count());
        int count2_2 = g2.getFindPatterns().size();
        LOGGER.debug("3) Find invocation count: {}", count2_2);
        Assertions.assertTrue(count2_1 < count2_2);
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
        OWLOntologyDocumentSource src = OWLIOUtils.getFileDocumentSource("/ontapi/pizza.ttl", OntFormat.TURTLE);

        OntologyManager m1 = OntManagers.createManager();
        Ontology o1 = m1.loadOntologyFromOntologyDocument(src,
                m1.getOntologyLoaderConfiguration().setModelCacheLevel(CacheSettings.CACHE_COMPONENT, true));
        InternalGraphModelImpl im1 = getBase(o1);
        InternalCache.Loading c1 = getInternalCache(im1, OWLComponentType.class);
        Assertions.assertNotNull(c1);
        Map map1 = (Map) c1.get(im1);
        Assertions.assertNotNull(map1);
        keys.forEach(k -> {
            ObjectMap v = (ObjectMap) map1.get(k);
            Assertions.assertNotNull(v);
            Assertions.assertFalse(v.isLoaded(), "Loaded " + k);
        });
        // load signature:
        Assertions.assertEquals(signature, o1.signature().count());
        keys.forEach(k -> {
            ObjectMap v = (ObjectMap) map1.get(k);
            Assertions.assertNotNull(v);
            if (OWLComponentType.ANONYMOUS_INDIVIDUAL.equals(k)) {
                Assertions.assertFalse(v.isLoaded(), "Loaded " + k);
            } else {
                Assertions.assertTrue(v.isLoaded(), "Not loaded: " + k);
            }
        });

        OntologyManager m2 = OntManagers.createManager();
        Ontology o2 = m2.loadOntologyFromOntologyDocument(src,
                m2.getOntologyLoaderConfiguration().setModelCacheLevel(CacheSettings.CACHE_COMPONENT, false));
        InternalGraphModelImpl im2 = (InternalGraphModelImpl) adapter.asBaseModel(o2).getGraphModel();
        InternalCache.Loading c2 = getInternalCache(im2, OWLComponentType.class);
        Assertions.assertNotNull(c2);
        Map map2 = (Map) c2.get(im2);
        Assertions.assertNotNull(map2);
        // load signature:
        Assertions.assertEquals(signature, o2.signature().distinct().count());
        keys.forEach(k -> {
            ObjectMap v = (ObjectMap) map2.get(k);
            Assertions.assertNotNull(v);
            Assertions.assertFalse(v.isLoaded(), "Loaded " + k);
        });
    }

    @Test
    public void testContentCacheLevels() {
        OntConfig c = new OntConfig();
        c.setModelCacheLevel(CacheSettings.CACHE_ITERATOR);
        Assertions.assertFalse(c.useComponentCache());
        Assertions.assertTrue(c.useIteratorCache());
        Assertions.assertFalse(c.useContentCache());

        c.setModelCacheLevel(CacheSettings.CACHE_COMPONENT);
        Assertions.assertTrue(c.useComponentCache());
        Assertions.assertFalse(c.useIteratorCache());
        Assertions.assertFalse(c.useContentCache());

        c.setModelCacheLevel(CacheSettings.CACHE_CONTENT);
        Assertions.assertFalse(c.useComponentCache());
        Assertions.assertFalse(c.useIteratorCache());
        Assertions.assertTrue(c.useContentCache());

        c.setModelCacheLevel(CacheSettings.CACHE_ALL, false);
        Assertions.assertFalse(c.useComponentCache());
        Assertions.assertFalse(c.useIteratorCache());
        Assertions.assertFalse(c.useContentCache());

        c.setModelCacheLevel(CacheSettings.CACHE_ALL, true);
        Assertions.assertTrue(c.useComponentCache());
        Assertions.assertTrue(c.useIteratorCache());
        Assertions.assertTrue(c.useContentCache());
    }

    @Test
    public void testNoIteratorAndComponentCache() throws OWLOntologyCreationException {
        OWLOntologyDocumentSource s = OWLIOUtils.getFileDocumentSource("/ontapi/pizza.ttl", OntFormat.TURTLE);

        long axioms = 945;
        OntologyManager m = OntManagers.createManager();
        DataFactory df = m.getOWLDataFactory();
        m.getOntologyConfigurator().setModelCacheLevel(CacheSettings.CACHE_CONTENT);

        Ontology o = m.loadOntologyFromOntologyDocument(s);
        Assertions.assertEquals(axioms, o.getAxiomCount());
        OWLClass c = df.getOWLClass("C");
        OWLAxiom a = df.getOWLSubClassOfAxiom(c, df.getOWLNothing(), Collections.singletonList(df.getRDFSLabel("x1")));
        o.add(a);
        Assertions.assertEquals(axioms + 1, o.getAxiomCount());
        o.clearCache();
        Assertions.assertEquals(axioms + 2, o.getAxiomCount());

        o.remove(a);
        Assertions.assertEquals(axioms + 1, o.axioms().count());
        o.remove(df.getOWLDeclarationAxiom(c));
        Assertions.assertEquals(axioms, o.getAxiomCount());
    }

    @Test
    public void testLoadNativeOWLFormatWhenContentCacheIsDisabled() throws OWLOntologyCreationException {
        OntologyManager m = OntManagers.createManager();
        m.getOntologyConfigurator().setModelCacheLevel(CacheSettings.CACHE_CONTENT, false);
        testLoadManchesterString(m);
    }

    @Test
    public void testLoadNativeOWLFormatWhenAllCachesAreDisabled() throws OWLOntologyCreationException {
        OntologyManager m = OntManagers.createManager();
        m.getOntologyConfigurator().setModelCacheLevel(0);
        testLoadManchesterString(m);
    }

    @Test
    public void testLoadBrokenTurtleUsingOWLAPIWithNoCache() throws OWLOntologyCreationException {
        // The following TTL is wrong: it has unexpected dot at [line: 18, col: 15]
        // Since Jena fails as expected, ONT-API uses the native OWL-API Turtle Parser as alternative,
        // which is more tolerant.
        // It calls remove axiom operations, to test which this test-case is intended.
        String wrong = """
                @prefix : <urn:fm2#> .
                @prefix owl: <http://www.w3.org/2002/07/owl#> .
                @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                @prefix xml: <http://www.w3.org/XML/1998/namespace> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                @prefix prov: <urn:prov#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @base <urn:fm2> .
                <http://www.ida.org/fm2.owl> rdf:type owl:Ontology.
                :prov rdf:type owl:AnnotationProperty .
                :Manage rdf:type owl:Class ; rdfs:subClassOf :ManagementType .
                [ rdf:type owl:Axiom ;
                  owl:annotatedSource :Manage ;
                  owl:annotatedTarget :ManagementType ;
                  owl:annotatedProperty rdfs:subClassOf ;
                  :prov [
                 prov:gen :FMDomain ;
                 prov:att :DM .
                 \
                ]
                 ] \
                .
                :ManagementType rdf:type owl:Class .
                :DM rdf:type owl:NamedIndividual , prov:Person .
                :FMDomain rdf:type owl:NamedIndividual , prov:Activity ; prov:ass :DM .""";

        OntologyManager m = OntManagers.createManager();
        m.getOntologyConfigurator().setModelCacheLevel(0);

        OWLOntologyDocumentSource source = OWLIOUtils.getStringDocumentSource(wrong, OntFormat.TURTLE);
        Ontology o = m.loadOntologyFromOntologyDocument(source);
        OWLIOUtils.print(o);
        Assertions.assertEquals(16, o.axioms().count());
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
