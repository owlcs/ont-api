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

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.CommonOntologies;
import com.github.owlcs.ontapi.OntManagers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by @szuev on 03.04.2018.
 */
public class SignatureTest {

    protected static OWLOntologyManager newManager() {
        return OntManagers.createManager();
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testClasses(TestData data) {
        data.doTest(T.CLASS);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testClassExpressions(TestData data) {
        data.doTest(T.CLASS_EXPRESSION);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testNamedIndividuals(TestData data) {
        data.doTest(T.NAMED_INDIVIDUAL);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testObjectProperties(TestData data) {
        data.doTest(T.OBJECT_PROPERTY);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testAnnotationProperties(TestData data) {
        data.doTest(T.ANNOTATION_PROPERTY);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testDataProperties(TestData data) {
        data.doTest(T.DATA_PROPERTY);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testDatatypes(TestData data) {
        data.doTest(T.DATATYPE);
    }

    enum TestData {
        PIZZA(CommonOntologies.PIZZA,
                T.CLASS.of(-18549559397L),
                T.CLASS_EXPRESSION.of(-42505386964L),
                T.NAMED_INDIVIDUAL.of(627705256L),
                T.DATATYPE.of(-638176551L),
                T.OBJECT_PROPERTY.of(833765171L),
                T.ANNOTATION_PROPERTY.of(2561927052L),
                T.DATA_PROPERTY.of()
        ),
        FAMILY(CommonOntologies.FAMILY,
                T.CLASS.of(-1268263574L),
                T.CLASS_EXPRESSION.of(-16978651096L),
                T.NAMED_INDIVIDUAL.of(85936112709L),
                T.DATATYPE.of(155000735L),
                T.OBJECT_PROPERTY.of(16114808725L),
                T.ANNOTATION_PROPERTY.of(1501132596L),
                T.DATA_PROPERTY.of(-253462624L)
        ),
        PEOPLE(CommonOntologies.PEOPLE,
                T.CLASS.of(-44946003502L),
                T.CLASS_EXPRESSION.of(-51187071185L),
                T.NAMED_INDIVIDUAL.of(-27921947437L),
                T.DATATYPE.of(2556627593L),
                T.OBJECT_PROPERTY.of(-3152099946L),
                T.ANNOTATION_PROPERTY.of(374445982L),
                T.DATA_PROPERTY.of()
        ),
        CAMERA(CommonOntologies.CAMERA,
                T.CLASS.of(8550118707L),
                T.CLASS_EXPRESSION.of(7152927621L),
                T.NAMED_INDIVIDUAL.of(-1151331346L),
                T.DATATYPE.of(2589442237L),
                T.OBJECT_PROPERTY.of(7453849661L),
                T.ANNOTATION_PROPERTY.of(1501132596L),
                T.DATA_PROPERTY.of(1045936326L)
        ),
        KOALA(CommonOntologies.KOALA,
                T.CLASS.of(4003478322L),
                T.CLASS_EXPRESSION.of(6612435426L),
                T.NAMED_INDIVIDUAL.of(11447501603L),
                T.DATATYPE.of(2556627593L),
                T.OBJECT_PROPERTY.of(1499233275L),
                T.ANNOTATION_PROPERTY.of(-1366328290L),
                T.DATA_PROPERTY.of(-1885133841L)
        ),
        TRAVEL(CommonOntologies.TRAVEL,
                T.CLASS.of(-12813239L),
                T.CLASS_EXPRESSION.of(5669053922L),
                T.NAMED_INDIVIDUAL.of(647180319L),
                T.DATATYPE.of(2492020304L),
                T.OBJECT_PROPERTY.of(451649769L),
                T.ANNOTATION_PROPERTY.of(2561927052L),
                T.DATA_PROPERTY.of(-3029448884L)
        ),
        WINE(CommonOntologies.WINE,
                T.CLASS.of(-7328739440L),
                T.CLASS_EXPRESSION.of(-12745569437L),
                T.NAMED_INDIVIDUAL.of(3479779986L),
                T.DATATYPE.of(-1614719226L),
                T.OBJECT_PROPERTY.of(2015421784L),
                T.ANNOTATION_PROPERTY.of(1366640788L),
                T.DATA_PROPERTY.of(-2003930395L)
        ),
        FOOD(CommonOntologies.FOOD,
                T.CLASS.of(-23236948086L),
                T.CLASS_EXPRESSION.of(-15980117497L),
                T.NAMED_INDIVIDUAL.of(-37834773654L),
                T.DATATYPE.of(799964985L),
                T.OBJECT_PROPERTY.of(-1744813981L),
                T.ANNOTATION_PROPERTY.of(1501132596L),
                T.DATA_PROPERTY.of()
        ),
        NCBITAXON_CUT(CommonOntologies.NCBITAXON_CUT,
                T.CLASS.of(-72461035056L),
                T.CLASS_EXPRESSION.of(2872387903L),
                T.NAMED_INDIVIDUAL.of(-75278820767L),
                T.DATATYPE.of(-638176551L),
                T.OBJECT_PROPERTY.of(-3871221171L),
                T.ANNOTATION_PROPERTY.of(7516115975L),
                T.DATA_PROPERTY.of(-795195587L)
        ),
        HP_CUT(CommonOntologies.HP_CUT,
                T.CLASS.of(4720902778L),
                T.CLASS_EXPRESSION.of(8426758568L),
                T.NAMED_INDIVIDUAL.of(),
                T.DATATYPE.of(-1448443492L),
                T.OBJECT_PROPERTY.of(-1116367413L),
                T.ANNOTATION_PROPERTY.of(6911265806L),
                T.DATA_PROPERTY.of()
        ),
        FAMILY_PEOPLE_UNION(CommonOntologies.FAMILY_PEOPLE_UNION,
                T.CLASS.of(-1213682231L),
                T.CLASS_EXPRESSION.of(2421052769L),
                T.NAMED_INDIVIDUAL.of(899752725L),
                T.DATATYPE.of(799964985L),
                T.OBJECT_PROPERTY.of(-163607138L),
                T.ANNOTATION_PROPERTY.of(1501132596L),
                T.DATA_PROPERTY.of(-1511256518L)
        ),
        ;
        private final CommonOntologies resource;
        private final Tester[] expectations;

        TestData(CommonOntologies data, Tester... expectations) {
            this.resource = data;
            this.expectations = expectations;
        }

        public Tester getTester(T type) {
            return Arrays.stream(expectations)
                    .filter(x -> Objects.equals(x.type, type.name()))
                    .findFirst().orElseThrow(IllegalArgumentException::new);
        }

        void doTest(T type) {
            getTester(type).testCounts(load(newManager()));
        }

        public OWLOntology load(OWLOntologyManager manager) {
            return resource.fetch(manager);
        }
    }

    enum T {
        CLASS_EXPRESSION {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont) {
                return ont.nestedClassExpressions();
            }
        },
        ANONYMOUS_INDIVIDUAL {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont) {
                return ont.anonymousIndividuals();
            }
        },
        NAMED_INDIVIDUAL {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont) {
                return ont.individualsInSignature();
            }
        },
        CLASS {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont) {
                return ont.classesInSignature();
            }
        },
        DATATYPE {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont) {
                return ont.datatypesInSignature();
            }
        },
        OBJECT_PROPERTY {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont) {
                return ont.objectPropertiesInSignature();
            }
        },
        DATA_PROPERTY {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont) {
                return ont.dataPropertiesInSignature();
            }
        },
        ANNOTATION_PROPERTY {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont) {
                return ont.annotationPropertiesInSignature();
            }
        },
        ;

        private Tester of() {
            return of(0);
        }

        private Tester of(long count) {
            return new Tester(this, count, this::listAxioms);
        }

        abstract Stream<? extends OWLObject> listAxioms(OWLOntology ont);
    }

    private static class Tester extends SearchTester {
        final Function<OWLOntology, Stream<? extends OWLObject>> listObjects;

        private Tester(T type, long count, Function<OWLOntology, Stream<? extends OWLObject>> listObjects) {
            super(type.name(), count);
            this.listObjects = listObjects;
        }

        void testCounts(OWLOntology ont) {
            long res = listObjects.apply(ont).mapToLong(SearchTester::toLong).sum();
            Assertions.assertEquals(count, res);
        }
    }
}
