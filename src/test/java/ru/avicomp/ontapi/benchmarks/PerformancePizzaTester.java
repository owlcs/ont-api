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

import com.google.common.base.Stopwatch;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.concurrent.TimeUnit;

/**
 * Not a test: only for manual running!
 * Test performance on an example of the <a href='file:/test/resources/pizza.ttl'>pizza</a> ontology.
 * <p>
 * Created by @szuev on 16.12.2016.
 */
@Ignore
@SuppressWarnings("ConstantConditions")
public class PerformancePizzaTester {
    private static final Logger LOGGER = Logger.getLogger(PerformancePizzaTester.class);

    private static final String fileName = "pizza.ttl";
    // if this is non-positive number, then the loading&checking axioms will be skipped for ONT-API (OWL-API loads them always):
    private static final int axiomCount = 945;
    private static final int num = 150;
    private static final int innerNum = 1;
    // if true, show also pure jena loading:
    private static final boolean debugTestPureJena = false;
    private static final boolean callGC = false;

    @Test
    public void testCalculateAverage() {
        OWLOntologyDocumentSource source = new IRIDocumentSource(IRI.create(ReadWriteUtils.getResourceURI(fileName)),
                OntFormat.TURTLE.createOwlFormat(), null);

        // init:
        int ont = loadONT(source).getAxiomCount();
        int owl = loadOWL(source).getAxiomCount();
        if (axiomCount > 0) {
            Assert.assertEquals("[ONT]Incorrect axiom count", axiomCount, ont);
            Assert.assertEquals("[OWL]Incorrect axiom count", axiomCount, owl);
        }
        if (callGC)
            System.gc();

        Level level = Logger.getRootLogger().getLevel();
        float owlAverage, ontAverage, jenaAverage;
        try {
            Logger.getRootLogger().setLevel(Level.OFF);
            owlAverage = doTest(num, () -> testOWL(source, axiomCount, innerNum), "OWL", callGC);
            System.err.println("=============");
            ontAverage = doTest(num, () -> testONT(source, axiomCount, innerNum), "ONT", callGC);
            if (debugTestPureJena) {
                System.err.println("=============");
                jenaAverage = doTest(num, () -> testJena(source, innerNum), "JENA", callGC);
            }
        } finally {
            Logger.getRootLogger().setLevel(level);
        }

        LOGGER.info("ONT = " + ontAverage);
        LOGGER.info("OWL = " + owlAverage);
        if (debugTestPureJena) {
            LOGGER.info("JENA = " + jenaAverage);
        }
        float diff = ontAverage / owlAverage;
        LOGGER.info("ONT/OWL = " + diff);
        Assert.assertTrue("ONT-API should not be slower (" + diff + ")", diff <= 1);
    }

    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    public static float doTest(final int num, final Tester tester, String tip, boolean doGCAfterIter) {
        String txt = tip == null ? String.valueOf(tester) : tip;
        System.err.println("Test " + tip + " (" + num + ")");
        int step = num / 50;
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (int i = 0; i < num; i++) {
            if (i % step == 0) {
                System.err.println("[" + txt + "]Iter #" + i);
            }
            tester.test();
            if (doGCAfterIter) {
                System.gc();
            }
        }
        stopwatch.stop();
        return stopwatch.elapsed(TimeUnit.MILLISECONDS) / num;
    }

    public interface Tester {
        void test();
    }

    private static void testOWL(OWLOntologyDocumentSource file, int axiomCount, int num) {
        for (int j = 0; j < num; j++) {
            OWLOntology o = loadOWL(file);
            if (axiomCount > 0) {
                Assert.assertEquals(axiomCount, o.getAxiomCount());
            }
        }
    }

    private static void testONT(OWLOntologyDocumentSource file, int axiomCount, int num) {
        for (int j = 0; j < num; j++) {
            // whole cycle of loading:
            OntologyModel o = loadONT(file);
            if (axiomCount > 0) {
                Assert.assertEquals(axiomCount, o.getAxiomCount());
            }
        }
    }

    private static void testJena(OWLOntologyDocumentSource file, int num) {
        for (int j = 0; j < num; j++) {
            RDFDataMgr.read(GraphFactory.createDefaultGraph(), file.getDocumentIRI().getIRIString(), Lang.TURTLE);
        }
    }

    public static OntologyModel loadONT(OWLOntologyDocumentSource file) {
        OWLAPICaches.clearAll();
        LOGGER.info("[ONT]Load " + file.getDocumentIRI());
        OntologyManager m = OntManagers.createONT();
        OntLoaderConfiguration conf = m.getOntologyLoaderConfiguration()
                .setPersonality(OntModelConfig.ONT_PERSONALITY_LAX)
                .setPerformTransformation(false);
        m.setOntologyLoaderConfiguration(conf);
        try {
            return m.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }

    public static OWLOntology loadOWL(OWLOntologyDocumentSource file) {
        OWLAPICaches.clearAll();
        LOGGER.info("[OWL]Load " + file.getDocumentIRI());
        OWLOntologyManager m = OntManagers.createOWL();
        try {
            return m.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }
}
