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

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.tests.ModelData;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.*;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by @szuev on 03.04.2018.
 */
@RunWith(Parameterized.class)
public class SignatureTest {

    private final TestData data;

    public SignatureTest(TestData data) {
        this.data = data;
    }

    @Parameterized.Parameters(name = "{0}")
    public static TestData[] getData() {
        return TestData.values();
    }

    private static OWLOntologyManager newManager() {
        return OntManagers.createONT();
    }

    @Test
    public void testClasses() {
        data.doTest(T.CLASS, HasClassesInSignature::classesInSignature);
    }

    @Test
    public void testNamedIndividuals() {
        data.doTest(T.NAMED_INDIVIDUAL, HasIndividualsInSignature::individualsInSignature);
    }

    enum TestData {
        PIZZA(ModelData.PIZZA,
                T.CLASS.of(-18549559397L),
                T.NAMED_INDIVIDUAL.of(627705256L)
        ),
        FAMILY(ModelData.FAMILY,
                T.CLASS.of(-1268263574L),
                T.NAMED_INDIVIDUAL.of(85936112709L)
        ),
        PEOPLE(ModelData.PEOPLE,
                T.CLASS.of(-44946003502L),
                T.NAMED_INDIVIDUAL.of(-27921947437L)
        ),
        CAMERA(ModelData.CAMERA,
                T.CLASS.of(8550118707L),
                T.NAMED_INDIVIDUAL.of(-1151331346L)
        ),
        KOALA(ModelData.KOALA,
                T.CLASS.of(4003478322L),
                T.NAMED_INDIVIDUAL.of(11447501603L)
        ),
        TRAVEL(ModelData.TRAVEL,
                T.CLASS.of(-12813239L),
                T.NAMED_INDIVIDUAL.of(647180319L)
        ),
        WINE(ModelData.WINE,
                T.CLASS.of(-7328739440L),
                T.NAMED_INDIVIDUAL.of(3479779986L)
        ),
        FOOD(ModelData.FOOD,
                T.CLASS.of(-23236948086L),
                T.NAMED_INDIVIDUAL.of(-37834773654L)
        ),
        NCBITAXON_CUT(ModelData.NCBITAXON_CUT,
                T.CLASS.of(-72461035056L),
                T.NAMED_INDIVIDUAL.of(-75278820767L)
        ),
        HP_CUT(ModelData.HP_CUT,
                T.CLASS.of(4720902778L),
                T.NAMED_INDIVIDUAL.of()
        ),
        ;
        private final ModelData resource;
        private final Tester[] expectations;

        TestData(ModelData data, Tester... expectations) {
            this.resource = data;
            this.expectations = expectations;
        }

        public Tester getTester(T type) {
            return Arrays.stream(expectations)
                    .filter(x -> Objects.equals(x.type, type))
                    .findFirst().orElseThrow(IllegalArgumentException::new);
        }

        void doTest(T type, Function<OWLOntology, Stream<? extends OWLPrimitive>> getEntities) {
            OWLOntology ont = load(newManager());
            getTester(type).testCounts(ont, getEntities);
        }

        public OWLOntology load(OWLOntologyManager manager) {
            return resource.load(manager);
        }
    }

    enum T {
        ANONYMOUS_INDIVIDUAL, NAMED_INDIVIDUAL, CLASS, DATATYPE, OBJECT_PROPERTY, DATA_PROPERTY, ANNOTATION_PROPERTY,
        ;

        private Tester of() {
            return of(0);
        }

        private Tester of(long count) {
            return new Tester(this, count);
        }
    }

    private static class Tester {
        private final long count;
        private final T type;

        private Tester(T type, long count) {
            this.type = type;
            this.count = count;
        }

        private long calc(OWLObject ax) {
            return ax.hashCode();
        }

        void testCounts(OWLOntology ont, Function<OWLOntology, Stream<? extends OWLPrimitive>> getSignature) {
            long res = getSignature.apply(ont).mapToLong(this::calc).sum();
            Assert.assertEquals(count, res);
        }
    }
}
