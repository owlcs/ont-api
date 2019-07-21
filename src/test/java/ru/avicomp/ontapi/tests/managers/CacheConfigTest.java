/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
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

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.WrappedGraph;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.*;
import ru.avicomp.ontapi.config.CacheSettings;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.config.OntSettings;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by @ssz on 04.03.2019.
 *
 * @see ru.avicomp.ontapi.config.CacheSettings
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
        OntologyModel o = m.loadOntologyFromOntologyDocument(ReadWriteUtils.getFileDocumentSource("/ontapi/pizza.ttl",
                OntFormat.TURTLE));
        Assert.assertNotNull(o);
        Assert.assertEquals(945, o.axioms().count());

        OntGraphModelImpl m1 = ((InternalModelHolder) o).getBase().getSearchModel();
        Assert.assertTrue(m1 instanceof InternalModel);

        m.setOntologyLoaderConfiguration(m.getOntologyLoaderConfiguration().setLoadNodesCacheSize(10_000));
        OntGraphModelImpl m2 = ((InternalModelHolder) o).getBase().getSearchModel();
        Assert.assertTrue(m2 instanceof SearchModel);
    }

    @Test
    public void testObjectsCacheSize() throws Exception {
        OntologyManager m = OntManagers.createONT();
        Assert.assertEquals(Prop.OBJECTS_CACHE_SIZE.getInt(), m.getOntologyConfigurator().getLoadObjectsCacheSize());
        OntLoaderConfiguration conf = new OntConfig().buildLoaderConfiguration().setLoadObjectsCacheSize(-1);
        Assert.assertEquals(-1, conf.getLoadObjectsCacheSize());
        m.setOntologyLoaderConfiguration(conf);
        OntologyModel o = m.loadOntologyFromOntologyDocument(ReadWriteUtils.getFileDocumentSource("/ontapi/pizza.ttl",
                OntFormat.TURTLE));
        Assert.assertNotNull(o);
        Assert.assertEquals(945, o.axioms().count());
        InternalObjectFactory of1 = ((InternalModelHolder) o).getBase().getObjectFactory();
        Assert.assertTrue(of1 instanceof NoCacheObjectFactory);
        Assert.assertFalse(of1 instanceof CacheObjectFactory);

        m.setOntologyLoaderConfiguration(conf.setLoadObjectsCacheSize(10_000));
        Assert.assertEquals(945, o.axioms().count());
        InternalObjectFactory of2 = ((InternalModelHolder) o).getBase().getObjectFactory();
        Assert.assertTrue(of2 instanceof CacheObjectFactory);
    }

    @Test
    public void testContentCacheOption() {
        Graph g = ReadWriteUtils.loadResourceTTLFile("/ontapi/pizza.ttl").getGraph();

        long axioms1 = 945;
        OntologyManager m1 = OntManagers.createONT();
        DataFactory df = m1.getOWLDataFactory();
        Assert.assertEquals(CacheSettings.CONTENT_CACHE_LEVEL_ALL, Prop.CONTENT_CACHE_LEVEL.getInt());
        Assert.assertTrue(m1.getOntologyConfigurator().isContentCacheEnabled());
        LogFindGraph g1 = new LogFindGraph(g);
        OntologyModel o1 = m1.addOntology(g1);
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
        OntLoaderConfiguration conf = m2.getOntologyLoaderConfiguration().setUseContentCache(false);
        Assert.assertFalse(conf.isContentCacheEnabled());
        m2.setOntologyLoaderConfiguration(conf);
        LogFindGraph g2 = new LogFindGraph(g);
        OntologyModel o2 = m2.addOntology(g2);
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
        } catch (DirectObjectTripleMapImpl.ModificationDeniedException e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        }
        Assert.assertEquals(size, g.size());

        try {
            o2.remove(axiom);
            Assert.fail("Possible to delete axiom");
        } catch (DirectObjectTripleMapImpl.ModificationDeniedException e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        }
        Assert.assertEquals(size, g.size());
    }

    @Test
    public void testContentCacheLevels() {
        OntConfig c = new OntConfig();
        c.setContentCacheLevel(CacheSettings.CONTENT_CACHE_LEVEL_FAST_ITERATOR);
        Assert.assertFalse(c.useTriplesContentCache());
        Assert.assertTrue(c.useIteratorContentCache());
        Assert.assertTrue(c.isContentCacheEnabled());

        c.setContentCacheLevel(CacheSettings.CONTENT_CACHE_LEVEL_TRIPLE_STORE);
        Assert.assertTrue(c.useTriplesContentCache());
        Assert.assertFalse(c.useIteratorContentCache());
        Assert.assertTrue(c.isContentCacheEnabled());

        c.setContentCacheLevel(1);
        Assert.assertFalse(c.useTriplesContentCache());
        Assert.assertFalse(c.useIteratorContentCache());
        Assert.assertTrue(c.isContentCacheEnabled());

        c.setUseContentCache(false);
        Assert.assertFalse(c.useTriplesContentCache());
        Assert.assertFalse(c.useIteratorContentCache());
        Assert.assertFalse(c.isContentCacheEnabled());

        c.setUseContentCache(true);
        Assert.assertTrue(c.useTriplesContentCache());
        Assert.assertTrue(c.useIteratorContentCache());
        Assert.assertTrue(c.isContentCacheEnabled());
    }

    @Test
    public void testNoCacheContentOptimization() throws OWLOntologyCreationException {
        OWLOntologyDocumentSource s = ReadWriteUtils.getFileDocumentSource("/ontapi/pizza.ttl", OntFormat.TURTLE);

        long axioms = 945;
        OntologyManager m = OntManagers.createONT();
        DataFactory df = m.getOWLDataFactory();
        m.getOntologyConfigurator().setContentCacheLevel(1);

        OntologyModel o = m.loadOntologyFromOntologyDocument(s);
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


    enum Prop {
        IRI_CACHE_SIZE(OntSettings.ONT_API_MANAGER_CACHE_IRIS.key() + ".integer"),
        NODES_CACHE_SIZE(OntSettings.ONT_API_LOAD_CONF_CACHE_NODES.key() + ".integer"),
        OBJECTS_CACHE_SIZE(OntSettings.ONT_API_LOAD_CONF_CACHE_OBJECTS.key() + ".integer"),
        CONTENT_CACHE_LEVEL(OntSettings.ONT_API_LOAD_CONF_CACHE_CONTENT.key() + ".integer");
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
