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

package com.github.owlcs.owlapi.tests.profiles;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.List;

@RunWith(Parameterized.class)
public class ProfileNoRLTestCase extends ProfileBase {

    private final String premise;

    public ProfileNoRLTestCase(String premise) {
        this.premise = premise;
    }

    @Test
    public void testNoRL() {
        test(premise, true, true, false, true);
    }

    @Parameters
    public static List<String> getData() {
        return Arrays.asList(
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" ><owl:Ontology /><owl:Class rdf:about=\"http://owl2.test/rules#C_Sub\"><rdfs:subClassOf><owl:Restriction><owl:someValuesFrom><owl:Class rdf:about=\"http://owl2.test/rules#C1\"/></owl:someValuesFrom><owl:onProperty><owl:ObjectProperty rdf:about=\"http://owl2.test/rules#p\"/></owl:onProperty></owl:Restriction></rdfs:subClassOf></owl:Class></rdf:RDF>",
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\"  xmlns:first=\"urn:test#\" xml:base=\"urn:test\"><owl:Ontology rdf:about=\"\" /><owl:Class rdf:about=\"urn:test#Car\"><owl:equivalentClass><owl:Class rdf:about=\"urn:test#Automobile\"/></owl:equivalentClass></owl:Class><first:Car rdf:about=\"urn:test#car\"><rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Thing\" /></first:Car><first:Automobile rdf:about=\"urn:test#auto\"><rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Thing\" /></first:Automobile></rdf:RDF>",
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xml:base=\"urn:test\"><owl:Ontology/><owl:Class rdf:about=\"http://www.w3.org/2002/07/owl#Thing\"><owl:equivalentClass rdf:resource=\"http://www.w3.org/2002/07/owl#Nothing\"/></owl:Class></rdf:RDF>",
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\"  xmlns:first=\"urn:test#\" xml:base=\"urn:test\"><owl:Ontology/><owl:Class rdf:about=\"urn:test#Car\"><owl:equivalentClass><owl:Class rdf:about=\"urn:test#Automobile\"/></owl:equivalentClass></owl:Class><first:Car rdf:about=\"urn:test#car\"><rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Thing\" /></first:Car><first:Automobile rdf:about=\"urn:test#auto\"><rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Thing\" /></first:Automobile></rdf:RDF>",
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\"  xmlns:first=\"urn:test#\" xml:base=\"urn:test\"><owl:Ontology/><owl:ObjectProperty rdf:about=\"urn:test#hasHead\"><owl:equivalentProperty><owl:ObjectProperty rdf:about=\"urn:test#hasLeader\"/></owl:equivalentProperty></owl:ObjectProperty><owl:Thing rdf:about=\"urn:test#X\"><first:hasLeader><owl:Thing rdf:about=\"urn:test#Y\"/></first:hasLeader></owl:Thing></rdf:RDF>",
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:first=\"urn:test#\" xml:base=\"urn:test\"><owl:Ontology/><owl:AnnotationProperty rdf:about=\"urn:test#prop\" /><owl:Thing rdf:about=\"urn:test#a\"><first:prop>foo</first:prop></owl:Thing></rdf:RDF>",
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:first=\"urn:test#\" xml:base=\"urn:test\"><owl:Ontology/><owl:Class rdf:about=\"urn:test#A\"><owl:disjointWith><owl:Class rdf:about=\"urn:test#B\"/></owl:disjointWith></owl:Class><first:A rdf:about=\"urn:test#a\"/><owl:Thing rdf:about=\"urn:test#a\"/><first:B rdf:about=\"urn:test#b\"/><owl:Thing rdf:about=\"urn:test#b\"/></rdf:RDF>");
    }
}
