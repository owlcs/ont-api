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

package com.github.owlcs.owlapi.tests.api.syntax.rdf;

import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.io.ReaderDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;

public class BOMSafeInputStreamAndParseTestCase extends TestBase {

    public static Collection<String> data() {
        return Arrays.asList(
                "<Ontology xml:base=\"" + IRI.getNextDocumentIRI("http://www.example.org/ISA14#o")
                        + "\" ontologyIRI=\"http://www.example.org/ISA14#\"> <Declaration><Class IRI=\"Researcher\"/></Declaration></Ontology>",
                "Ontology: <" + IRI.getNextDocumentIRI("http://www.example.org/ISA14#o")
                        + ">\nClass: <http://www.example.org/ISA14#Researcher>",
                "Ontology(<" + IRI.getNextDocumentIRI("http://www.example.org/ISA14#o")
                        + ">\nDeclaration(Class(<http://www.example.org/ISA14#Researcher>)))",
                "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n<"
                        + IRI.getNextDocumentIRI("http://www.example.org/ISA14#o")
                        + "> rdf:type owl:Ontology .\n<http://www.example.org/ISA14#Researcher> rdf:type owl:Class .",
                "<rdf:RDF xml:base=\"" + IRI.getNextDocumentIRI("http://www.example.org/ISA14#o")
                        + "\" xmlns:owl =\"http://www.w3.org/2002/07/owl#\" xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" ><owl:Ontology rdf:about=\"#\" /><owl:Class rdf:about=\"http://www.example.org/ISA14#Researcher\"/></rdf:RDF>");
    }

    private static InputStream in(int[] b, String s) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i : b) {
            out.write(i);
        }
        out.write(s.getBytes());
        byte[] byteArray = out.toByteArray();
        return new ByteArrayInputStream(byteArray);
    }

    // Bytes Encoding Form
    // 00 00 FE FF | UTF-32, big-endian
    // FF FE 00 00 | UTF-32, little-endian
    // FE FF |UTF-16, big-endian
    // FF FE |UTF-16, little-endian
    // EF BB BF |UTF-8
    @ParameterizedTest
    @MethodSource("data")
    public void testBOMError32big(String input) throws OWLOntologyCreationException, IOException {
        int[] b = new int[]{0x00, 0x00, 0xFE, 0xFF};
        m.loadOntologyFromOntologyDocument(in(b, input));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testBOMError32small(String input) throws OWLOntologyCreationException, IOException {
        int[] b = new int[]{0xFF, 0xFE, 0x00, 0x00};
        m.loadOntologyFromOntologyDocument(in(b, input));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testBOMError16big(String input) throws OWLOntologyCreationException, IOException {
        int[] b = new int[]{0xFF, 0xFE};
        m.loadOntologyFromOntologyDocument(in(b, input));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testBOMError16small(String input) throws OWLOntologyCreationException, IOException {
        int[] b = new int[]{0xFF, 0xFE};
        m.loadOntologyFromOntologyDocument(in(b, input));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testBOMError8(String input) throws OWLOntologyCreationException, IOException {
        int[] b = new int[]{0xEF, 0xBB, 0xBF};
        m.loadOntologyFromOntologyDocument(in(b, input));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testBOMError32bigReader(String input) throws OWLOntologyCreationException, IOException {
        int[] b = new int[]{0x00, 0x00, 0xFE, 0xFF};
        m.loadOntologyFromOntologyDocument(new ReaderDocumentSource(new InputStreamReader(in(b, input))));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testBOMError32Reader(String input) throws OWLOntologyCreationException, IOException {
        int[] b = new int[]{0xFF, 0xFE, 0x00, 0x00};
        m.loadOntologyFromOntologyDocument(new ReaderDocumentSource(new InputStreamReader(in(b, input))));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testBOMError16Reader(String input) throws OWLOntologyCreationException, IOException {
        int[] b = new int[]{0xFF, 0xFE};
        m.loadOntologyFromOntologyDocument(new ReaderDocumentSource(new InputStreamReader(in(b, input))));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testBOMError16smallReader(String input) throws OWLOntologyCreationException, IOException {
        int[] b = new int[]{0xFF, 0xFE};
        m.loadOntologyFromOntologyDocument(new ReaderDocumentSource(new InputStreamReader(in(b, input))));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testBOMError8Reader(String input) throws OWLOntologyCreationException, IOException {
        int[] b = new int[]{0xEF, 0xBB, 0xBF};
        m.loadOntologyFromOntologyDocument(new ReaderDocumentSource(new InputStreamReader(in(b, input))));
    }
}
