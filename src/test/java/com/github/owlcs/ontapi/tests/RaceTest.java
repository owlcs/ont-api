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

package com.github.owlcs.ontapi.tests;

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test for ontology edition racing:
 * one thread adds some axiom while another removes some axiom.
 * <p>
 * Created by @ssz on 04.07.2017.
 */
public class RaceTest {
    // constants for test tuning:
    private static final long TIMEOUT = 15_000; // ms
    private static final Logger LOGGER = LoggerFactory.getLogger(RaceTest.class);
    private static final boolean ADD_WITH_ANNOTATIONS = true;
    private static final int ADD_THREADS_NUM = 4;
    private static final int REMOVE_THREADS_NUM = 6;

    /**
     * Adds sub-class-of axioms in loop
     *
     * @param o     {@link Ontology}
     * @param ready {@link AtomicBoolean}
     */
    private static void add(Ontology o, AtomicBoolean ready) {
        OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();
        List<OWLAnnotation> annotations = ADD_WITH_ANNOTATIONS ?
                Stream.of(df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral("comm"), df.getRDFSLabel("lab")))
                        .collect(Collectors.toList()) : Collections.emptyList();
        while (ready.get()) {
            OWLClass c = df.getOWLClass(IRI.create("test", "clazz" + ThreadLocalRandom.current().nextInt()));
            OWLAxiom a = df.getOWLSubClassOfAxiom(c, df.getOWLThing(), annotations);
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("+ {}", a);
            o.add(a);
            long res = o.subClassAxiomsForSubClass(c).count();
            Assertions.assertTrue(res == 0 || res == 1);
        }
    }

    /**
     * Removes axioms in loop.
     *
     * @param o     {@link Ontology}
     * @param ready {@link AtomicBoolean}
     */
    private static void remove(Ontology o, AtomicBoolean ready) {
        while (ready.get()) {
            Stream<? extends OWLAxiom> axioms = ThreadLocalRandom.current().nextBoolean() ?
                    o.axioms() : o.generalClassAxioms();
            axioms.findFirst().ifPresent(a -> {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("- {}", a);
                o.remove(a);
            });
        }
    }

    private static Runnable toTask(Ontology o,
                                   AtomicBoolean process,
                                   BiConsumer<Ontology, AtomicBoolean> func) {
        return () -> {
            try {
                func.accept(o, process);
            } catch (Exception e) {
                process.set(false);
                throw e;
            }
        };
    }

    @Test
    public void testConcurrency() throws InterruptedException, ExecutionException {
        OntologyManager m = OntManagers.createConcurrentManager();
        m.getOntologyConfigurator().setAllowReadDeclarations(false);
        Ontology o = m.createOntology();
        AtomicBoolean process = new AtomicBoolean(true);
        int threads = ADD_THREADS_NUM + REMOVE_THREADS_NUM + 1;
        ScheduledExecutorService service = Executors.newScheduledThreadPool(threads);
        List<Future<?>> res = new ArrayList<>();
        LOGGER.debug("Start racing");
        for (int i = 0; i < ADD_THREADS_NUM; i++)
            res.add(service.submit(toTask(o, process, RaceTest::add)));
        for (int i = 0; i < REMOVE_THREADS_NUM; i++)
            res.add(service.submit(toTask(o, process, RaceTest::remove)));
        service.schedule(() -> process.set(false), TIMEOUT, TimeUnit.MILLISECONDS);
        service.shutdown();
        for (Future<?> f : res) {
            f.get();
        }
        LOGGER.debug("Fin.");
    }
}
