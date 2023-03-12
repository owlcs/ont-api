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
package com.github.owlcs.owlapi.tests.api.fileroundtrip;

import org.semanticweb.owlapi.model.OWLOntology;

import java.util.stream.Stream;

/**
 * @author Matthew Horridge, The University Of Manchester, Information Management Group
 */
public class FileRoundTripTestCase extends AbstractFileRoundTrippingTestCase {

    public static Stream<OWLOntology> data() {
        return files().map(AbstractFileRoundTrippingTestCase::createOntology);
    }

    public static Stream<String> files() {
        return Stream.of(
                "AnnotatedPropertyAssertions.rdf",          // 1
                "ComplexSubProperty.rdf",                   // 2
                "DataAllValuesFrom.rdf",                    // 3
                "cardinalitywithwhitespace.owl",            // 4
                "DataComplementOf.rdf",                     // 5
                "DataHasValue.rdf",                         // 6
                "DataIntersectionOf.rdf",                   // 7
                "DataMaxCardinality.rdf",                   // 8
                "DataMinCardinality.rdf",                   // 9
                "DataOneOf.rdf",                            // 10
                "DataSomeValuesFrom.rdf",                   // 11
                "DataUnionOf.rdf",                          // 12
                "DatatypeRestriction.rdf",                  // 13
                "TestDeclarations.rdf",                     // 14
                "Deprecated.rdf",                           // 15
                "DisjointClasses.rdf",                      // 16
                "HasKey.rdf",                               // 17
                "InverseOf.rdf",                            // 18
                "ObjectAllValuesFrom.rdf",                  // 19
                "ObjectCardinality.rdf",                    // 20
                "ObjectComplementOf.rdf",                   // 21
                "ObjectHasSelf.rdf",                        // 22
                "ObjectHasValue.rdf",                       // 23
                "ObjectIntersectionOf.rdf",                 // 24
                "ObjectMaxCardinality.rdf",                 // 25
                "ObjectMaxQualifiedCardinality.rdf",        // 26
                "ObjectMinCardinality.rdf",                 // 27
                "ObjectMinQualifiedCardinality.rdf",        // 28
                "ObjectOneOf.rdf",                          // 29
                "ObjectQualifiedCardinality.rdf",           // 30
                "ObjectSomeValuesFrom.rdf",                 // 31
                "ObjectUnionOf.rdf",                        // 32
                "primer.functionalsyntax.txt",              // 33
                "primer.owlxml.xml",                        // 34
                "primer.rdfxml.xml",                        // 35
                "RDFSClass.rdf",                            // 36
                "koala.owl",                                // 37
                "SubClassOf.rdf",                           // 38
                "TestParser06.rdf",                         // 39
                "TestParser07.rdf",                         // 40
                "TestParser10.rdf",                         // 41
                "annotatedpropertychain.ttl.rdf",           // 42
                "UntypedSubClassOf.rdf",                    // 43
                "SubClassOfUntypedOWLClass.rdf",            // 44
                "SubClassOfUntypedSomeValuesFrom.rdf",      // 45
                "XMLLiteral.rdf");                          // 46
    }
}
