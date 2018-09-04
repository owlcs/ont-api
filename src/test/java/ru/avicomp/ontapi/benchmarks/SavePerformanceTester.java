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

import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Not a test.
 * Comparing saving in different formats.
 * Note: currently for private usage, since dependent on local resources.
 * <p>
 * Created by @ssz on 02.09.2018.
 */
@Ignore
public class SavePerformanceTester {
    private static final Logger LOGGER = LoggerFactory.getLogger(SavePerformanceTester.class);

    private OutputStream out = ReadWriteUtils.NULL_OUT; //System.out;
    private static List<Double> owl = new ArrayList<>();
    private static List<Double> ont = new ArrayList<>();

    @Test
    public void testPS_OWL_API() throws Exception { // 5 MB
        owl.add(loadSaveTest(OntManagers.createOWL(), SavePerformanceTester.class.getResource("/ontapi/psychology.rdf").toURI(), out));
    }

    @Test
    public void testPS_ONT_API() throws Exception { // 5 MB
        ont.add(loadSaveTest(OntManagers.createONT(), SavePerformanceTester.class.getResource("/ontapi/psychology.rdf").toURI(), out));
    }

    @Test
    public void testHP_OWL_API() throws Exception { // 24 MB
        owl.add(loadSaveTest(OntManagers.createOWL(), SavePerformanceTester.class.getResource("/ontapi/hp-no-imports.fss").toURI(), out));
    }

    @Test
    public void testHP_ONT_API() throws Exception { // 24 MB
        ont.add(loadSaveTest(OntManagers.createONT(), SavePerformanceTester.class.getResource("/ontapi/hp-no-imports.fss").toURI(), out));
    }

    @Test
    public void testGalen_OWL_API() throws Exception { // 21 MB
        owl.add(loadSaveTest(OntManagers.createOWL(), SavePerformanceTester.class.getResource("/ontapi/galen.rdf").toURI(), out));
    }

    @Test
    public void testGalen_ONT_API() throws Exception { // 21 MB
        ont.add(loadSaveTest(OntManagers.createONT(), SavePerformanceTester.class.getResource("/ontapi/galen.rdf").toURI(), out));
    }

    @Test
    public void testTTO_OWL_API() throws Exception {
        owl.add(loadSaveTest(OntManagers.createOWL(), SavePerformanceTester.class.getResource("/ontapi/tto.rdf").toURI(), out));
    }

    @Test
    public void testTTO_ONT_API() throws Exception { // 68mb
        ont.add(loadSaveTest(OntManagers.createONT(), SavePerformanceTester.class.getResource("/ontapi/tto.rdf").toURI(), out));
    }

    @AfterClass
    public static void after() {
        double OWL = owl.stream().mapToDouble(d -> d).sum();
        double ONT = ont.stream().mapToDouble(d -> d).sum();
        LOGGER.info("TOTAL :: OWL={} ONT={}, DIFF={}", print(OWL), print(ONT), print(ONT / OWL));
    }

    private static double loadSaveTest(OWLOntologyManager m, URI uri, OutputStream out) throws Exception {
        Path file = Paths.get(uri);
        Instant S = Instant.now();

        LOGGER.info("Start loading <{}>", file);
        Instant sL = Instant.now();
        OWLOntology o = m.loadOntologyFromOntologyDocument(file.toFile());
        String dL = now(sL);
        LOGGER.info("Finish loading <{}>", file);

        Instant sA = Instant.now();
        long axioms = o.axioms().count();
        String dA = now(sA);
        LOGGER.info("Load: {} min. Read Axioms: {}({} min)", dL, axioms, dA);

        Instant sT = Instant.now();
        o.saveOntology(OntFormat.TURTLE.createOwlFormat(), out);
        String dT = now(sT);
        LOGGER.info("Load: {} min. Print(Turtle): {} min", dL, dT);

        Instant sM = Instant.now();
        o.saveOntology(OntFormat.MANCHESTER_SYNTAX.createOwlFormat(), out);
        String dM = now(sM);
        LOGGER.info("Load: {} min. Print(Turtle): {} min. Save(Manchester): {} min", dL, dT, dM);

        Instant sF = Instant.now();
        o.saveOntology(OntFormat.FUNCTIONAL_SYNTAX.createOwlFormat(), out);
        String dF = now(sF);
        LOGGER.info("Load: {} min. Turtle: {} min. Manchester: {} min. Functional: {}", dL, dT, dM, dF);

        Instant sO = Instant.now();
        o.saveOntology(OntFormat.OWL_XML.createOwlFormat(), out);
        String dO = now(sO);
        LOGGER.info("Load: {} min. Turtle: {}. Manchester: {}. Functional: {}. OWL/XML: {}.", dL, dT, dM, dF, dO);

        Instant sR = Instant.now();
        o.saveOntology(OntFormat.RDF_XML.createOwlFormat(), out);
        String dR = now(sR);

        double res = minutes(S, Instant.now());
        String D = print(res);

        String txt = String.format("%s|%s", m instanceof OntologyManager ? "ONT-API" : "OWL-API", file.getFileName().toString().toUpperCase());
        LOGGER.info("[{}]Load: {} min. Read: {}. Turtle: {}. Manchester: {}. Functional: {}. OWL/XML: {}. RDF/XML: {}. TOTAL: {} min.",
                txt, dL, dA, dT, dM, dF, dO, dR, D);
        return res;
    }

    public static String now(Instant s) {
        return print(minutes(s, Instant.now()));
    }

    public static String print(double minutes) {
        return String.format(Locale.ENGLISH, "%.2f", minutes);
    }

    public static double minutes(Instant s, Instant e) {
        return Duration.between(s, e).toMillis() / 60_000d;
    }

}
