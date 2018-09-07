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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.*;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;

import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Not a test.
 * Comparing listing different things.
 * Note: currently for private usage, since dependent on local resources.
 * <p>
 * Created by @ssz on 02.09.2018.
 */
@Ignore
public class ListPerformanceTester {
    private static final PrintStream out = System.out;
    private static Level log4jLevel = Logger.getRootLogger().getLevel();

    @BeforeClass
    public static void before() {
        Logger.getRootLogger().setLevel(Level.OFF);
    }

    @AfterClass
    public static void after() {
        Logger.getRootLogger().setLevel(log4jLevel);
    }

    @Test
    public void comparePizzaLoadAxioms() {
        compareListAxioms("/ontapi/pizza.ttl", OntFormat.TURTLE, 1000, 945); // 0.56
    }

    @Test
    public void compareFamilyLoadAxioms() {
        compareListAxioms("/ontapi/family.ttl", OntFormat.TURTLE, 800, 2845); // 0.60
    }

    @Test
    public void comparePsychologyLoadAxioms() {
        compareListAxioms("/ontapi/psychology.rdf", OntFormat.RDF_XML, 50, 38872); // 0.27
    }

    @Test
    public void comparePizzaLoadClasses() {
        compareListClasses("/ontapi/pizza.ttl", OntFormat.TURTLE, 1000, 100); // 1.65
    }

    @Test
    public void compareFamilyLoadClasses() {
        compareListClasses("/ontapi/family.ttl", OntFormat.TURTLE, 1000, 58); // 1.18
    }

    @Test
    public void comparePsychologyLoadClasses() {
        // difference in built-in classes:
        compareListClasses("/ontapi/psychology.rdf", OntFormat.RDF_XML, 100, 6038, 6037); // 0.63
    }

    @Test
    public void compareGalenLoadAxioms() {
        compareListAxioms("/ontapi/galen.rdf", OntFormat.RDF_XML, 10, 96463); // 0.27
    }

    @Test
    public void compareHPLoadAxioms() {
        compareListAxioms("/ontapi/hp-no-imports.fss", OntFormat.FUNCTIONAL_SYNTAX, 20, 143855); // 0.11
    }

    @Test
    public void compareTTOLoadAxioms() {
        // the difference in axiom lists is due to split-annotation-axioms functionality,
        // turn it on and the number will be the same:
        compareListAxioms("/ontapi/tto.rdf", OntFormat.RDF_XML, 10, 336294, 336291); // 0.32
    }

    @Test
    public void compareGalenLoadClasses() {
        compareListClasses("/ontapi/galen.rdf", OntFormat.RDF_XML, 10, 23142, 23141); // 0.64
    }

    @Test
    public void compareHPLoadClasses() {
        compareListClasses("/ontapi/hp-no-imports.fss", OntFormat.FUNCTIONAL_SYNTAX, 20, 15984); // 0.16
    }

    @Test
    public void compareTTORDFLoadClasses() {
        compareListClasses("/ontapi/tto.rdf", OntFormat.RDF_XML, 10, 38705); // 0.58
    }

    @Test
    public void compareTTOTurtleLoadClasses() {
        compareListClasses("/ontapi/tto.ttl", OntFormat.TURTLE, 10, 38705); // 1.37
    }

    private void compareListAxioms(String file, OntFormat format, int iter, int expectedAxioms) {
        compareListAxioms(file, format, iter, expectedAxioms, expectedAxioms);
    }

    private void compareListAxioms(String file, OntFormat format, int iter, int expectedAxiomsOWL, int expectedAxiomsONT) {
        out.println(compare(iter,
                () -> testListAxioms(file, format, expectedAxiomsOWL, OntManagers.createOWL()),
                () -> testListAxioms(file, format, expectedAxiomsONT, OntManagers.createONT()),
                OWLAPICaches::clearAll) > 1 ? "Good" : "Bad");
    }

    private void compareListClasses(String file, OntFormat format, int iter,
                                    int expectedClasses) {
        compareListClasses(file, format, iter, expectedClasses, expectedClasses);
    }

    private void compareListClasses(String file, OntFormat format, int iter,
                                    int expectedSignatureClasses,
                                    int expectedListClasses) {
        Action ont = () -> testListONTClasses(file, format, expectedListClasses, OntManagers.createONT());
        Action owl = () -> testListOWLClasses(file, format, expectedSignatureClasses, OntManagers.createOWL());
        out.println(compare(iter,
                owl,
                ont,
                OWLAPICaches::clearAll) > 1 ? "Good" : "Bad"); // 0.61
    }

    private static void testListAxioms(String file, OntFormat format, int count, OWLOntologyManager manager) {
        Assert.assertEquals(manager instanceof OntologyManager ? "ONT" : "OWL", count, load(manager, file, format).axioms().count());
    }

    private static void testListOWLClasses(String file, OntFormat format, int count, OWLOntologyManager manager) {
        Assert.assertEquals(count, load(manager, file, format).classesInSignature().count());
    }

    private static void testListONTClasses(String file, OntFormat format, int count, OntologyManager manager) {
        Assert.assertEquals(count, ((OntologyModel) load(manager, file, format)).asGraphModel().listClasses().count());
    }

    public static double compare(int numberOfIterations, Action firstFunc, Action secondFunc, Action after) {
        class P {
            private long first, second;
        }
        int step = numberOfIterations > 10 ? numberOfIterations / 10 : 1;
        List<P> res = IntStream.rangeClosed(1, numberOfIterations).mapToObj(j -> {
            if (j % step == 0) {
                out.println("Iter #" + j);
            }
            P r = new P();
            r.first = calcNanos(firstFunc);
            r.second = calcNanos(secondFunc);
            after.perform();
            return r;
        }).collect(Collectors.toList());

        double first = res.stream().mapToLong(x -> x.first).average().orElse(-1.0);
        double second = res.stream().mapToLong(x -> x.second).average().orElse(-1.0);
        double r = first / second;
        out.println("\nFirst = " + first + ";\t\tSecond = " + second + ";\t\tDiff (first/second) = " + r);
        return r;
    }

    @FunctionalInterface
    public interface Action {
        Action NO_OP = () -> {
        };

        void perform();
    }

    private static long calcNanos(Action tester) {
        Instant s = Instant.now();
        tester.perform();
        Instant e = Instant.now();
        return Duration.between(s, e).toNanos();
    }

    public static OWLOntology load(OWLOntologyManager manager, String file, OntFormat lang) {
        try {
            Path path = Paths.get(ListPerformanceTester.class.getResource(file).toURI());
            OWLOntologyDocumentSource source = new FileDocumentSource(path.toFile(), lang.createOwlFormat());
            return manager.loadOntologyFromOntologyDocument(source);
        } catch (URISyntaxException | OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }
}
