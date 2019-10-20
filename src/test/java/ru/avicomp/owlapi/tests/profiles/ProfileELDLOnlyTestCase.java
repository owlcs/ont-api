/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.owlapi.tests.profiles;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class ProfileELDLOnlyTestCase extends ProfileBase {

    private final String premise;

    public ProfileELDLOnlyTestCase(String premise) {
        this.premise = premise;
    }

    @Test
    public void testELDLONLY() {
        test(premise, true, false, false, true);
    }

    @Parameters
    public static List<String> getData() {
        return Arrays.asList(
                "<rdf:RDF xml:base=\"http://example.org/\" xmlns=\"http://example.org/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><owl:Ontology/><owl:DatatypeProperty rdf:about=\"hasSSN\" /><rdf:Description rdf:about=\"http://www.w3.org/2002/07/owl#Thing\"><owl:hasKey rdf:parseType=\"Collection\"><rdf:Description rdf:about=\"hasSSN\" /></owl:hasKey></rdf:Description><rdf:Description rdf:about=\"Peter\"><hasSSN>123-45-6789</hasSSN></rdf:Description><rdf:Description rdf:about=\"Peter_Griffin\"><hasSSN>123-45-6789</hasSSN></rdf:Description></rdf:RDF>",
                "Prefix( owl:=<http://www.w3.org/2002/07/owl#> )\nPrefix( :=<http://example.org/> )\nOntology( Declaration( DataProperty( :hasSSN ) ) HasKey( owl:Thing () ( :hasSSN ) ) DataPropertyAssertion( :hasSSN :Peter \"123-45-6789\" ) DataPropertyAssertion( :hasSSN :Peter_Griffin \"123-45-6789\" ))",
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\"  xmlns:first=\"urn:test#\" xml:base=\"urn:test\"><owl:Ontology/><owl:ObjectProperty rdf:about=\"urn:test#op\"/><rdf:Description rdf:about=\"urn:test#a\"><rdf:type><owl:Restriction><owl:onProperty rdf:resource=\"urn:test#op\"/><owl:someValuesFrom rdf:resource=\"http://www.w3.org/2002/07/owl#Nothing\" /></owl:Restriction></rdf:type></rdf:Description><rdf:Description rdf:about=\"urn:test#b\"><rdf:type><owl:Restriction><owl:onProperty rdf:resource=\"urn:test#op\"/><owl:someValuesFrom rdf:resource=\"http://www.w3.org/2002/07/owl#Nothing\" /></owl:Restriction></rdf:type></rdf:Description></rdf:RDF>",
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xml:base=\"urn:test\"><owl:Ontology/><owl:ObjectProperty rdf:about=\"urn:test#op\"/><rdf:Description rdf:about=\"urn:test#a\"><rdf:type><owl:Restriction rdf:nodeID=\"r\"><owl:onProperty rdf:resource=\"urn:test#op\"/><owl:someValuesFrom rdf:resource=\"http://www.w3.org/2002/07/owl#Nothing\" /></owl:Restriction></rdf:type></rdf:Description><rdf:Description rdf:about=\"urn:test#b\"><rdf:type rdf:nodeID=\"r\"/></rdf:Description></rdf:RDF>",
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xml:base=\"urn:test\"><owl:Ontology/><owl:Class rdf:about=\"http://www.w3.org/2002/07/owl#Thing\"><owl:oneOf rdf:parseType=\"Collection\"><owl:Thing rdf:about=\"urn:test#s\"/></owl:oneOf></owl:Class></rdf:RDF>",
                "<rdf:RDF xml:base=\"http://example.org/\" xmlns=\"http://example.org/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><owl:Ontology/><owl:ObjectProperty rdf:about=\"likes\" /><rdf:Description rdf:about=\"Peter\"><rdf:type><owl:Restriction><owl:onProperty rdf:resource=\"likes\" /><owl:hasSelf rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</owl:hasSelf></owl:Restriction></rdf:type></rdf:Description></rdf:RDF>",
                "Prefix( :=<http://example.org/> ) Ontology(\n Declaration( ObjectProperty( :likes ) ) ClassAssertion( ObjectHasSelf( :likes ) :Peter ))",
                "<rdf:RDF xml:base=\"http://example.org/\" xmlns=\"http://example.org/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><owl:Ontology/><owl:DatatypeProperty rdf:about=\"hasSSN\" /><rdf:Description rdf:about=\"http://www.w3.org/2002/07/owl#Thing\"><owl:hasKey rdf:parseType=\"Collection\"><rdf:Description rdf:about=\"hasSSN\" /></owl:hasKey></rdf:Description><rdf:Description rdf:about=\"Peter\"><hasSSN>123-45-6789</hasSSN></rdf:Description><rdf:Description rdf:about=\"Peter_Griffin\"><hasSSN>123-45-6789</hasSSN></rdf:Description><rdf:Description rdf:about=\"Peter\"><owl:differentFrom rdf:resource=\"Peter_Griffin\" /></rdf:Description></rdf:RDF>",
                "Prefix( owl:=<http://www.w3.org/2002/07/owl#> ) Prefix( :=<http://example.org/> ) Ontology( Declaration( DataProperty( :hasSSN ) ) HasKey( owl:Thing () ( :hasSSN ) ) DataPropertyAssertion( :hasSSN :Peter \"123-45-6789\" ) DataPropertyAssertion( :hasSSN :Peter_Griffin \"123-45-6789\" ) DifferentIndividuals( :Peter :Peter_Griffin ))",
                "<rdf:RDF xml:base=\"http://example.org/\" xmlns=\"http://example.org/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><owl:Ontology/><owl:Class rdf:about=\"Person\" /><owl:Class rdf:about=\"Man\" /><owl:DatatypeProperty rdf:about=\"hasSSN\" /><owl:ObjectProperty rdf:about=\"marriedTo\" /><rdf:Description rdf:about=\"Person\"><owl:hasKey rdf:parseType=\"Collection\"><rdf:Description rdf:about=\"hasSSN\" /></owl:hasKey></rdf:Description><rdf:Description rdf:about=\"Peter\"><hasSSN>123-45-6789</hasSSN><rdf:type rdf:resource=\"Person\" /></rdf:Description><rdf:Description rdf:about=\"Lois\"><rdf:type><owl:Restriction><owl:onProperty rdf:resource=\"marriedTo\" /><owl:someValuesFrom><owl:Class><owl:intersectionOf rdf:parseType=\"Collection\"><rdf:Description rdf:about=\"Man\" /><owl:Restriction><owl:onProperty rdf:resource=\"hasSSN\" /><owl:hasValue>123-45-6789</owl:hasValue></owl:Restriction></owl:intersectionOf></owl:Class></owl:someValuesFrom></owl:Restriction></rdf:type></rdf:Description></rdf:RDF>");
    }
}
