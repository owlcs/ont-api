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

package ru.avicomp.ontapi.tests;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test for ontology edition racing:
 * one thread adds some axiom while another removes some axiom.
 * <p>
 * Created by @szuev on 04.07.2017.
 */
public class RaceTest {
    // constants for test tuning:
    private static final long TIMEOUT = 15_000; // 15s
    private static final Logger LOGGER = LoggerFactory.getLogger(RaceTest.class);
    private static final PrintStream OUT = LOGGER.isDebugEnabled() ? System.out : ReadWriteUtils.NULL_OUT;
    private static final boolean ADD_WITH_ANNOTATIONS = true;
    private static final int ADD_THREADS_NUM = 6;
    private static final int REMOVE_THREADS_NUM = 4;

    @Test
    public void test() throws InterruptedException, ExecutionException {
        OntologyManager m = OntManagers.createConcurrentONT();
        m.getOntologyConfigurator().setAllowReadDeclarations(false);
        OntologyModel o = m.createOntology();
        AtomicBoolean flag = new AtomicBoolean(true);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ExecutorService service = Executors.newFixedThreadPool(ADD_THREADS_NUM + REMOVE_THREADS_NUM);
        List<Future<?>> res = new ArrayList<>();
        LOGGER.debug("Start racing");
        for (int i = 0; i < ADD_THREADS_NUM; i++)
            res.add(service.submit(() -> add(o, random, flag)));
        for (int i = 0; i < REMOVE_THREADS_NUM; i++)
            res.add(service.submit(() -> remove(o, random, flag)));
        service.shutdown();
        Thread.sleep(TIMEOUT);
        flag.set(false);
        for (Future<?> f : res) {
            f.get();
        }
        LOGGER.debug("Fin.");
    }

    /**
     * Adds sub-class-of axioms in loop
     *
     * @param o      {@link OntologyModel}
     * @param random {@link ThreadLocalRandom}
     * @param ready  {@link AtomicBoolean}
     */
    private static void add(OntologyModel o, ThreadLocalRandom random, AtomicBoolean ready) {
        OWLDataFactory df = o.getOWLOntologyManager().getOWLDataFactory();
        List<OWLAnnotation> annotations = ADD_WITH_ANNOTATIONS ?
                Stream.of(df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral("comm"), df.getRDFSLabel("lab"))).collect(Collectors.toList()) :
                Collections.emptyList();
        while (ready.get()) {
            OWLClass c = df.getOWLClass(IRI.create("test", "clazz" + random.nextInt()));
            OWLAxiom a = df.getOWLSubClassOfAxiom(c, df.getOWLThing(), annotations);
            OUT.println("+ " + a);
            o.add(a);
            long l = o.subClassAxiomsForSubClass(c).count();
            Assert.assertTrue(l == 0 || l == 1);
        }
    }

    /**
     * Removes axioms in loop.
     *
     * @param o      {@link OntologyModel}
     * @param random {@link ThreadLocalRandom}
     * @param ready  {@link AtomicBoolean}
     */
    private static void remove(OntologyModel o, ThreadLocalRandom random, AtomicBoolean ready) {
        while (ready.get()) {
            Stream<? extends OWLAxiom> axioms = random.nextBoolean() ? o.axioms() : o.generalClassAxioms();
            axioms.findFirst().ifPresent(a -> {
                OUT.println("- " + a);
                o.remove(a);
            });
        }
    }
}
