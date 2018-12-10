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

package ru.avicomp.owlapi.tests.profiles;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class ProfileNoELTestCase extends ProfileBase {

    private final String premise;

    public ProfileNoELTestCase(String premise) {
        this.premise = premise;
    }

    @Test
    public void testNoEL() {
        test(premise, false, true, true, true);
    }

    @Parameters
    public static List<String> getData() {
        return Arrays.asList(
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\" xmlns:ex=\"http://www.example.org#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"><owl:SymmetricProperty rdf:about=\"http://www.example.org#p\"/><rdf:Description rdf:about=\"http://www.example.org#x\"><ex:p rdf:resource=\"http://www.example.org#y\"/></rdf:Description></rdf:RDF>",
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\" xmlns:ex=\"http://www.example.org#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"><owl:SymmetricProperty rdf:about=\"http://www.example.org#p\"><rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#AsymmetricProperty\"/></owl:SymmetricProperty><rdf:Description rdf:about=\"http://www.example.org#x\"><ex:p rdf:resource=\"http://www.example.org#y\"/></rdf:Description></rdf:RDF>",
                "<rdf:RDF xml:base=\"http://example.org/\" xmlns=\"http://example.org/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><owl:Ontology/><owl:ObjectProperty rdf:about=\"parentOf\" /><owl:AsymmetricProperty rdf:about=\"parentOf\" /><rdf:Description rdf:about=\"Peter\"><parentOf rdf:resource=\"Stewie\" /></rdf:Description><rdf:Description rdf:about=\"Stewie\"><parentOf rdf:resource=\"Peter\" /></rdf:Description></rdf:RDF>",
                "Prefix( :=<http://example.org/> ) Ontology( Declaration( ObjectProperty( :parentOf ) ) AsymmetricObjectProperty( :parentOf ) ObjectPropertyAssertion( :parentOf :Peter :Stewie ) ObjectPropertyAssertion( :parentOf :Stewie :Peter ))",
                "<rdf:RDF xml:base=\"http://example.org/\" xmlns=\"http://example.org/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><owl:Ontology/><owl:ObjectProperty rdf:about=\"hasFather\" /><owl:ObjectProperty rdf:about=\"hasMother\" /><rdf:Description rdf:about=\"hasFather\"><owl:propertyDisjointWith rdf:resource=\"hasMother\" /></rdf:Description><rdf:Description rdf:about=\"Stewie\"><hasFather rdf:resource=\"Peter\" /></rdf:Description><rdf:Description rdf:about=\"Stewie\"><hasMother rdf:resource=\"Lois\" /></rdf:Description></rdf:RDF>",
                "Prefix( :=<http://example.org/> ) Ontology( Declaration( ObjectProperty( :hasFather ) ) Declaration( ObjectProperty( :hasMother ) ) DisjointObjectProperties( :hasFather :hasMother ) ObjectPropertyAssertion( :hasFather :Stewie :Peter ) ObjectPropertyAssertion( :hasMother :Stewie :Lois ))",
                "<rdf:RDF xml:base=\"http://example.org/\" xmlns=\"http://example.org/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><owl:Ontology/><owl:ObjectProperty rdf:about=\"hasFather\" /><owl:ObjectProperty rdf:about=\"hasMother\" /><owl:ObjectProperty rdf:about=\"hasChild\" /><owl:AllDisjointProperties><owl:members rdf:parseType=\"Collection\"><rdf:Description rdf:about=\"hasFather\" /><rdf:Description rdf:about=\"hasMother\" /><rdf:Description rdf:about=\"hasChild\" /></owl:members></owl:AllDisjointProperties><rdf:Description rdf:about=\"Stewie\"><hasFather rdf:resource=\"Peter\" /></rdf:Description><rdf:Description rdf:about=\"Stewie\"><hasMother rdf:resource=\"Lois\" /></rdf:Description><rdf:Description rdf:about=\"Stewie\"><hasChild rdf:resource=\"StewieJr\" /></rdf:Description></rdf:RDF>",
                "Prefix( :=<http://example.org/> ) Ontology( Declaration( ObjectProperty( :hasFather ) ) Declaration( ObjectProperty( :hasMother ) ) Declaration( ObjectProperty( :hasChild ) ) DisjointObjectProperties( :hasFather :hasMother :hasChild ) ObjectPropertyAssertion( :hasFather :Stewie :Peter ) ObjectPropertyAssertion( :hasMother :Stewie :Lois ) ObjectPropertyAssertion( :hasChild :Stewie :StewieJr ))",
                "<rdf:RDF xml:base=\"http://example.org/\" xmlns=\"http://example.org/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><owl:Ontology/><owl:DatatypeProperty rdf:about=\"hasName\" /><owl:DatatypeProperty rdf:about=\"hasAddress\" /><rdf:Description rdf:about=\"hasName\"><owl:propertyDisjointWith rdf:resource=\"hasAddress\" /></rdf:Description><rdf:Description rdf:about=\"Peter\"><hasName>Peter Griffin</hasName><hasAddress>Peter Griffin</hasAddress></rdf:Description></rdf:RDF>",
                "Prefix( :=<http://example.org/> ) Ontology( Declaration( DataProperty( :hasName ) ) Declaration( DataProperty( :hasAddress ) ) DisjointDataProperties( :hasName :hasAddress ) DataPropertyAssertion( :hasName :Peter \"Peter Griffin\" ) DataPropertyAssertion( :hasAddress :Peter \"Peter Griffin\" ))",
                "<rdf:RDF xml:base=\"http://example.org/\" xmlns=\"http://example.org/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><owl:Ontology/><owl:ObjectProperty rdf:about=\"marriedTo\" /><owl:IrreflexiveProperty rdf:about=\"marriedTo\" /><rdf:Description rdf:about=\"Peter\"><marriedTo rdf:resource=\"Peter\" /></rdf:Description></rdf:RDF>",
                "Prefix( :=<http://example.org/> ) Ontology( Declaration( ObjectProperty( :marriedTo ) ) IrreflexiveObjectProperty( :marriedTo ) ObjectPropertyAssertion( :marriedTo :Peter :Peter ))",
                "<rdf:RDF xml:base=\"http://example.org/\" xmlns=\"http://example.org/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><owl:Ontology/><owl:DatatypeProperty rdf:about=\"hasName\" /><owl:DatatypeProperty rdf:about=\"hasAddress\" /><owl:DatatypeProperty rdf:about=\"hasZip\" /><owl:AllDisjointProperties><owl:members rdf:parseType=\"Collection\"><rdf:Description rdf:about=\"hasName\" /><rdf:Description rdf:about=\"hasAddress\" /><rdf:Description rdf:about=\"hasZip\" /></owl:members></owl:AllDisjointProperties><rdf:Description rdf:about=\"Peter\"><hasName>Peter Griffin</hasName></rdf:Description><rdf:Description rdf:about=\"Peter_Griffin\"><hasAddress>Peter Griffin</hasAddress></rdf:Description><rdf:Description rdf:about=\"Petre\"><hasZip>Peter Griffin</hasZip></rdf:Description></rdf:RDF>",
                "Prefix( :=<http://example.org/> ) Ontology( Declaration( DataProperty( :hasName ) ) Declaration( DataProperty( :hasAddress ) ) Declaration( DataProperty( :hasZip ) ) DisjointDataProperties( :hasName :hasAddress :hasZip ) DataPropertyAssertion( :hasName :Peter \"Peter Griffin\" ) DataPropertyAssertion( :hasAddress :Peter_Griffin \"Peter Griffin\" ) DataPropertyAssertion( :hasZip :Petre \"Peter Griffin\" ))");
    }
}
