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

package com.github.owlcs.ontapi.tests.jena;

import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphStatisticsHandler;
import org.apache.jena.graph.Node;
import org.apache.jena.shared.PrefixMapping;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.owlcs.ontapi.NoOpReadWriteLock;
import com.github.owlcs.ontapi.internal.AxiomParserProvider;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.RWLockedGraph;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * To test {@link RWLockedGraph}.
 * Created by @ssz on 05.04.2019.
 */
public class RWLockedGraphTest {

    private static final long TIMEOUT = 10_000; // ms
    private static final Logger LOGGER = LoggerFactory.getLogger(RWLockedGraphTest.class);

    private static final int THREADS_NUM_1 = 3;
    private static final int THREADS_NUM_2 = 3;

    private static List<AxiomType<? extends OWLAxiom>> EXCLUDED_TYPES = Arrays.asList(AxiomType.DECLARATION,
            AxiomType.ANNOTATION_ASSERTION, AxiomType.SUBCLASS_OF);
    private static Set<AxiomType<? extends OWLAxiom>> CONSIDERED_TYPES = AxiomType.AXIOM_TYPES.stream()
            .filter(x -> !EXCLUDED_TYPES.contains(x)).collect(Iter.toUnmodifiableSet()); // 453

    private static void testRace(OntGraphModel m) throws ExecutionException, InterruptedException {
        AtomicBoolean process = new AtomicBoolean(true);
        int threads = THREADS_NUM_1 + THREADS_NUM_2 + 1;
        ScheduledExecutorService service = Executors.newScheduledThreadPool(threads);
        List<Future<?>> res = new ArrayList<>();
        LOGGER.debug("Start racing");
        for (int i = 0; i < THREADS_NUM_1; i++)
            res.add(service.submit(toTask(m, process, RWLockedGraphTest::listAxiomsAndModifyClasses)));
        for (int i = 0; i < THREADS_NUM_2; i++)
            res.add(service.submit(toTask(m, process, RWLockedGraphTest::modifyClassesAndListClasses)));
        service.schedule(() -> process.set(false), TIMEOUT, TimeUnit.MILLISECONDS);
        service.shutdown();
        for (Future<?> f : res) {
            f.get();
        }
        LOGGER.debug("Fin.");
    }

    private static Runnable toTask(OntGraphModel m, AtomicBoolean ready, Consumer<OntGraphModel> action) {
        return () -> {
            while (ready.get()) {
                try {
                    action.accept(m);
                } catch (Exception e) {
                    ready.set(false);
                    throw e;
                }
            }
        };
    }

    private static void listAxiomsAndModifyClasses(OntGraphModel m) {
        LOGGER.debug("[{}]:::listAxiomsAndModifyClasses", Thread.currentThread().getName());
        Assert.assertEquals(453, listAxioms(CONSIDERED_TYPES, m).count());
        int num = 10;
        for (int i = 0; i < num; i++) {
            m.createOntClass("C" + i).addComment("X" + i);
        }
        for (int i = 0; i < num; i++) {
            OntClass c = m.getOntClass("C" + i);
            if (c != null) {
                m.removeOntObject(c);
            }
        }
    }

    private static void modifyClassesAndListClasses(OntGraphModel m) {
        LOGGER.debug("[{}]:::modifyClassesAndListClasses", Thread.currentThread().getName());
        String name = Thread.currentThread().getName();
        int num = 10;
        for (int i = 0; i < num; i++) {
            m.createOntClass(name + "a" + i).addSuperClass(m.createComplementOf(m.createOntClass(name + "b" + i)));
        }
        for (int i = 0; i < num; i++) {
            m.getOntClass(name + "a" + i).listProperties().toSet().forEach(m::remove);
            m.removeOntObject(m.getOntClass(name + "b" + i));
        }
        long count = m.statements(null, RDF.type, OWL.Class).filter(x -> x.getSubject().isURIResource()).count();
        Assert.assertTrue("Count: " + count, count >= 100);
    }

    private static Stream<OWLAxiom> listAxioms(Collection<AxiomType<? extends OWLAxiom>> types, OntGraphModel m) {
        return types.stream()
                .map(AxiomParserProvider::get)
                .flatMap(x -> x.axioms(m)).map(ONTObject::getOWLObject);
    }

    private static Graph loadPizza() {
        return ReadWriteUtils.loadResourceTTLFile("/ontapi/pizza.ttl").getGraph();
    }

    @Test
    public void testRaceModifyAndList() throws Exception {
        Graph g = loadPizza();
        Graph gg = new RWLockedGraph(g, new ReentrantReadWriteLock());
        OntGraphModel m = OntModelFactory.createModel(gg);
        Instant s = Instant.now();
        testRace(m);
        Instant e = Instant.now();
        LOGGER.debug("Duration: {}", Duration.between(s, e));
    }

    @Test
    public void testConcurrentPrefixes() throws ExecutionException, InterruptedException {
        PrefixMapping pm = new RWLockedGraph(Factory.createGraphMem(), new ReentrantReadWriteLock()).getPrefixMapping();
        ExecutorService service = Executors.newScheduledThreadPool(3);
        List<Future<?>> res = new ArrayList<>();
        for (int i = 0; i < THREADS_NUM_1; i++) {
            res.add(service.submit(() -> {
                String pref = Thread.currentThread().getName();
                pm.setNsPrefix(pref + "1", "a");
                pm.setNsPrefix(pref + "2", "b");
                Assert.assertTrue(pm.numPrefixes() >= 2);
                pm.removeNsPrefix(pref + "1");
                Assert.assertTrue(pm.numPrefixes() >= 1);
                pm.removeNsPrefix(pref + "2");
                Assert.assertTrue(pm.numPrefixes() >= 0);
            }));
        }
        AtomicInteger index = new AtomicInteger();
        for (int i = 0; i < THREADS_NUM_2; i++) {
            res.add(service.submit(() -> {
                String pref = "p" + index.incrementAndGet();
                pm.setNsPrefix(pref, "a");
                pm.getNsPrefixMap();
            }));
        }
        service.shutdown();
        for (Future<?> f : res) {
            f.get();
        }
        LOGGER.debug("{}", pm);
        Assert.assertFalse(pm.hasNoMappings());
        Assert.assertEquals(THREADS_NUM_2, pm.numPrefixes());
    }

    @Test
    public void testGraphStatisticHandler() {
        RWLockedGraph g = new RWLockedGraph(loadPizza(), NoOpReadWriteLock.NO_OP_RW_LOCK);
        GraphStatisticsHandler gsh = g.getStatisticsHandler();
        int size = g.size();
        LOGGER.debug("Total size: {}", size);
        Assert.assertEquals(size, g.get().getStatisticsHandler().getStatistic(Node.ANY, Node.ANY, Node.ANY));
        Assert.assertEquals(size, gsh.getStatistic(Node.ANY, Node.ANY, Node.ANY));
    }
}
