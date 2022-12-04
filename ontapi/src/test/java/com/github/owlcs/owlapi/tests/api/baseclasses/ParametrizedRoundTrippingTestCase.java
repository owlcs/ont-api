/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.owlapi.tests.api.baseclasses;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.stream.Stream;

/**
 * Created by @ssz on 11.10.2020.
 */
public abstract class ParametrizedRoundTrippingTestCase extends TestBase {

    public static Stream<OWLOntology> data() {
        throw new UnsupportedOperationException();
    }

    private AbstractRoundTrippingTestCase getTestCore(OWLOntology ont) {
        return new AbstractRoundTrippingTestCase() {
            @Override
            protected OWLOntology createOntology() {
                return ont;
            }
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testRDFXML(OWLOntology ont) throws Exception {
        getTestCore(ont).testRDFXML();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testRDFJSON(OWLOntology ont) throws Exception {
        getTestCore(ont).testRDFJSON();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testOWLXML(OWLOntology ont) throws Exception {
        getTestCore(ont).testOWLXML();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testFunctionalSyntax(OWLOntology ont) throws Exception {
        getTestCore(ont).testFunctionalSyntax();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testTurtle(OWLOntology ont) throws Exception {
        getTestCore(ont).testTurtle();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testManchesterOWLSyntax(OWLOntology ont) throws Exception {
        getTestCore(ont).testManchesterOWLSyntax();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testTrig(OWLOntology ont) throws Exception {
        getTestCore(ont).testTrig();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testJSONLD(OWLOntology ont) throws Exception {
        getTestCore(ont).testJSONLD();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testNTriples(OWLOntology ont) throws Exception {
        getTestCore(ont).testNTriples();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testNQuads(OWLOntology ont) throws Exception {
        getTestCore(ont).testNQuads();
    }

    @ParameterizedTest
    @MethodSource("data")
    public void roundTripRDFXMLAndFunctionalShouldBeSame(OWLOntology ont) throws Exception {
        getTestCore(ont).testRoundTripRDFXMLAndFunctionalShouldBeSame();
    }
}
