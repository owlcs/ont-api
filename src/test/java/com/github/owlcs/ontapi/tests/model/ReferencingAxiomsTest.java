/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.*;
import com.github.owlcs.ontapi.utils.FileMap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.PriorityCollection;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by @ssz on 08.03.2020.
 */
@RunWith(Parameterized.class)
public class ReferencingAxiomsTest {
    private final TestData data;

    public ReferencingAxiomsTest(TestData data) {
        this.data = data;
    }

    @Parameterized.Parameters(name = "{0}")
    public static TestData[] getData() {
        return TestData.values();
    }

    @Test
    public void testSearchByClass() {
        data.doTest(T.CLS, HasClassesInSignature::classesInSignature);
    }

    @Test
    public void testSearchByLiteral() {
        OWLOntology ont = data.load(newManager());
        Set<OWLLiteral> literals = ont.axioms().flatMap(x -> OwlObjects.objects(OWLLiteral.class, x))
                .collect(Collectors.toSet());
        data.getTester(T.LTL).testCounts(ont, x -> literals.stream());
    }

    @Test
    public void testSearchByIRI() {
        OWLOntology ont = data.load(newManager());
        Set<IRI> iris = ont.signature().map(HasIRI::getIRI).collect(Collectors.toSet());
        data.getTester(T.IRI).testCounts(ont, x -> iris.stream());
    }

    @Test
    public void testSearchByAnonymousIndividuals() {
        data.doTest(T.ANI, HasAnonymousIndividuals::anonymousIndividuals);
    }

    @Test
    public void testSearchByNamedIndividuals() {
        data.doTest(T.NMI, HasIndividualsInSignature::individualsInSignature);
    }

    @Test
    public void testSearchByDatatypes() {
        data.doTest(T.DTD, HasDatatypesInSignature::datatypesInSignature);
    }

    @Test
    public void testSearchByObjectProperty() {
        data.doTest(T.OBP, HasObjectPropertiesInSignature::objectPropertiesInSignature);
    }

    @Test
    public void testSearchByDatatypeProperty() {
        data.doTest(T.DTP, HasDatatypesInSignature::datatypesInSignature);
    }

    @Test
    public void testSearchByAnnotationProperty() {
        data.doTest(T.ANP, HasAnnotationPropertiesInSignature::annotationPropertiesInSignature);
    }

    private static OWLOntologyManager newManager() {
        return OntManagers.createONT();
    }

    enum TestData {
        PIZZA("/ontapi/pizza.ttl", T.CLS.of(1577, 795), T.LTL.of(120), T.IRI.of(2199, 945), T.ANI.of(),
                T.NMI.of(31, 23), T.DTD.of(120), T.OBP.of(231, 223), T.DTP.of(120), T.ANP.of(120)),
        FAMILY("/ontapi/family.ttl", T.CLS.of(342, 236), T.LTL.of(521), T.IRI.of(7543, 2845), T.ANI.of(),
                T.NMI.of(4214, 2372), T.DTD.of(530), T.OBP.of(1891, 1709), T.DTP.of(530), T.ANP.of(4)),
        PEOPLE("/ontapi/people.ttl", T.CLS.of(235, 151), T.LTL.of(201, 196), T.IRI.of(999, 409), T.ANI.of(12, 11),
                T.NMI.of(91, 62), T.DTD.of(197, 196), T.OBP.of(76, 65), T.DTP.of(197, 196), T.ANP.of(206, 201)),
        CAMERA("/ontapi/camera.ttl", T.CLS.of(60, 47), T.LTL.of(0), T.IRI.of(130, 77), T.ANI.of(),
                T.NMI.of(7, 6), T.DTD.of(8), T.OBP.of(27, 25), T.DTP.of(8), T.ANP.of()),
        KOALA("/ontapi/koala.ttl", T.CLS.of(82, 59), T.LTL.of(6), T.IRI.of(144, 76), T.ANI.of(),
                T.NMI.of(18, 17), T.DTD.of(7), T.OBP.of(24, 23), T.DTP.of(7), T.ANP.of(3)),
        TRAVEL("/ontapi/travel.ttl", T.CLS.of(167, 115), T.LTL.of(12), T.IRI.of(321, 177), T.ANI.of(12),
                T.NMI.of(61, 51), T.DTD.of(16), T.OBP.of(41, 36), T.DTP.of(16), T.ANP.of(12)),
        WINE("/ontapi/wine.ttl", T.CLS.of(576, 462), T.LTL.of(4), T.IRI.of(2127, 911), T.ANI.of(),
                T.NMI.of(1080, 744), T.DTD.of(5), T.OBP.of(456, 449), T.DTP.of(5), T.ANP.of(3)) {
            @Override
            String getName() {
                return "http://www.w3.org/TR/2003/PR-owl-guide-20031209/wine";
            }

            @Override
            public OWLOntology load(OWLOntologyManager manager) {
                return load(manager, FOOD, this);
            }
        },
        FOOD("/ontapi/food.ttl", T.CLS.of(415, 284), T.LTL.of(), T.IRI.of(772, 361), T.ANI.of(),
                T.NMI.of(189, 175), T.DTD.of(), T.OBP.of(168, 107), T.DTP.of(), T.ANP.of()) {
            @Override
            String getName() {
                return "http://www.w3.org/TR/2003/PR-owl-guide-20031209/food";
            }

            @Override
            public OWLOntology load(OWLOntologyManager manager) {
                return load(manager, WINE, this);
            }
        };
        private final Path file;
        private final OntFormat format;
        private final Tester[] expectations;

        TestData(String file, Tester... expectations) {
            this(file, OntFormat.TURTLE, expectations);
        }

        TestData(String file, OntFormat format, Tester... expectations) {
            try {
                this.file = Paths.get(TestData.class.getResource(file).toURI()).toRealPath();
            } catch (IOException | URISyntaxException e) {
                throw new ExceptionInInitializerError(e);
            }
            this.format = format;
            this.expectations = expectations;
        }

        public Tester getTester(T type) {
            return Arrays.stream(expectations)
                    .filter(x -> Objects.equals(x.type, type))
                    .findFirst().orElseThrow(IllegalArgumentException::new);
        }

        void doTest(T type, Function<OWLOntology, Stream<? extends OWLPrimitive>> getPrimitives) {
            OWLOntology ont = load(newManager());
            getTester(type).testCounts(ont, getPrimitives);
        }

        public OWLOntology load(OWLOntologyManager manager) {
            try {
                // no transform
                OWLOntologyLoaderConfiguration conf = OWLAdapter.get()
                        .asONT(manager.getOntologyLoaderConfiguration())
                        .setPerformTransformation(false);
                return manager.loadOntologyFromOntologyDocument(getDocumentSource(), conf);
            } catch (OWLOntologyCreationException e) {
                throw new AssertionError(e);
            }
        }

        static OWLOntology load(OWLOntologyManager manager, TestData... data) {
            OWLOntology res = null;
            OWLOntologyLoaderConfiguration conf = manager.getOntologyLoaderConfiguration();
            if (!(manager instanceof OntologyManager)) { // OWL-API
                conf = conf.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
                manager.setOntologyLoaderConfiguration(conf);
                PriorityCollection<OWLOntologyIRIMapper> maps = manager.getIRIMappers();
                Arrays.stream(data)
                        .map(d -> FileMap.create(IRI.create(d.getName()), d.getDocumentSource().getDocumentIRI()))
                        .forEach(maps::add);
                try {
                    res = manager.loadOntology(IRI.create(data[data.length - 1].getName()));
                } catch (OWLOntologyCreationException e) {
                    throw new AssertionError(e);
                }
            } else { // ONT-API
                conf = OWLAdapter.get().asONT(conf).setProcessImports(false).setPerformTransformation(false);
                for (TestData d : data) {
                    try {
                        res = manager.loadOntologyFromOntologyDocument(d.getDocumentSource(), conf);
                    } catch (OWLOntologyCreationException e) {
                        throw new AssertionError(e);
                    }
                }
            }
            Assert.assertEquals(data.length, manager.ontologies().count());
            return res;
        }

        public OWLOntologyDocumentSource getDocumentSource() {
            return new FileDocumentSource(file.toFile(), getDocumentFormat());
        }

        public OWLDocumentFormat getDocumentFormat() {
            return format.createOwlFormat();
        }

        String getName() {
            return name();
        }

    }

    enum T {
        CLS,
        LTL,
        IRI,
        ANI,
        NMI,
        DTD,
        OBP,
        DTP,
        ANP,
        ;

        private Tester of(long sum, long distinct) {
            return new Tester(this, sum, distinct);
        }

        private Tester of(long count) {
            return of(count, count);
        }

        private Tester of() {
            return of(0, 0);
        }
    }

    private static class Tester {
        private final long distinct;
        private final long sum;
        private final T type;

        private Tester(T type, long sum, long distinct) {
            this.type = type;
            this.sum = sum;
            this.distinct = distinct;
        }

        long distinctCount(OWLOntology ont, Stream<? extends OWLPrimitive> stream) {
            return referencingAxioms(ont, stream).distinct().count();
        }

        long nonDistinctCount(OWLOntology ont, Stream<? extends OWLPrimitive> stream) {
            return referencingAxioms(ont, stream).count();
        }

        Stream<? extends OWLAxiom> referencingAxioms(OWLOntology ont, Stream<? extends OWLPrimitive> stream) {
            return stream.flatMap(ont::referencingAxioms);
        }

        void testCounts(OWLOntology ont, Function<OWLOntology, Stream<? extends OWLPrimitive>> getPrimitives) {
            long d = distinctCount(ont, getPrimitives.apply(ont));
            long s = nonDistinctCount(ont, getPrimitives.apply(ont));
            String msg = "(" + s + ", " + d + ")";
            Assert.assertEquals(msg, distinct, d);
            Assert.assertEquals(msg, sum, s);
        }
    }

}
