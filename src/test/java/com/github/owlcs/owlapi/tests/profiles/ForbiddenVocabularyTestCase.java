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
package com.github.owlcs.owlapi.tests.profiles;

import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWL2RLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.OWLProfileViolation;
import org.semanticweb.owlapi.profiles.violations.UseOfReservedVocabularyForAnnotationPropertyIRI;
import org.semanticweb.owlapi.util.CollectionFactory;
import org.semanticweb.owlapi.util.OWLObjectPropertyManager;

import java.util.Arrays;
import java.util.List;

public class ForbiddenVocabularyTestCase extends TestBase {

    private static final String input1 = """
            <?xml version="1.0"?>
            <rdf:RDF xmlns="http://purl.org/net/social-reality#"
                 xml:base="http://purl.org/net/social-reality"
                 xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                 xmlns:owl="http://www.w3.org/2002/07/owl#"
                 xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                 xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <owl:Ontology rdf:about="http://purl.org/net/social-reality"/>
                <owl:ObjectProperty rdf:about="http://purl.org/net/social-reality#context">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/social-reality#counts-as">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/social-reality#has_OR">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                    <owl:propertyChainAxiom rdf:parseType="Collection">
                        <rdf:Description>
                            <owl:inverseOf rdf:resource="http://purl.org/net/social-reality#context"/>
                        </rdf:Description>
                        <rdf:Description rdf:about="http://purl.org/net/social-reality#is_OR"/>
                        <rdf:Description>
                            <owl:inverseOf rdf:resource="http://purl.org/net/social-reality#counts-as"/>
                        </rdf:Description>
                    </owl:propertyChainAxiom>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/social-reality#is_OR">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                <owl:Class rdf:about="http://purl.org/net/social-reality#BF"/>
                <owl:Class rdf:about="http://purl.org/net/social-reality#C"/>
                <owl:Class rdf:about="http://purl.org/net/social-reality#OR">
                    <owl:equivalentClass>
                        <owl:Class>
                            <owl:intersectionOf rdf:parseType="Collection">
                                <owl:Restriction>
                                    <owl:onProperty rdf:resource="http://purl.org/net/social-reality#context"/>
                                    <owl:someValuesFrom rdf:resource="http://purl.org/net/social-reality#C"/>
                                </owl:Restriction>
                                <owl:Restriction>
                                    <owl:onProperty>
                                        <rdf:Description>
                                            <owl:inverseOf rdf:resource="http://purl.org/net/social-reality#counts-as"/>
                                        </rdf:Description>
                                    </owl:onProperty>
                                    <owl:someValuesFrom rdf:resource="http://purl.org/net/social-reality#BF"/>
                                </owl:Restriction>
                            </owl:intersectionOf>
                        </owl:Class>
                    </owl:equivalentClass>
                    <owl:equivalentClass>
                        <owl:Restriction>
                            <owl:onProperty rdf:resource="http://purl.org/net/social-reality#is_OR"/>
                            <owl:hasSelf rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">true</owl:hasSelf>
                        </owl:Restriction>
                    </owl:equivalentClass>
                    <rdfs:subClassOf>
                        <owl:Class>
                            <owl:intersectionOf rdf:parseType="Collection">
                                <owl:Restriction>
                                    <owl:onProperty rdf:resource="http://purl.org/net/social-reality#context"/>
                                    <owl:cardinality rdf:datatype="http://www.w3.org/2001/XMLSchema#nonNegativeInteger">1</owl:cardinality>
                                </owl:Restriction>
                                <owl:Restriction>
                                    <owl:onProperty>
                                        <rdf:Description>
                                            <owl:inverseOf rdf:resource="http://purl.org/net/social-reality#counts-as"/>
                                        </rdf:Description>
                                    </owl:onProperty>
                                    <owl:cardinality rdf:datatype="http://www.w3.org/2001/XMLSchema#nonNegativeInteger">1</owl:cardinality>
                                </owl:Restriction>
                            </owl:intersectionOf>
                        </owl:Class>
                    </rdfs:subClassOf>
                </owl:Class>
                <owl:Class rdf:about="http://www.w3.org/2002/07/owl#Thing"/>
            </rdf:RDF>""";
    private static final String input2 = """
            <?xml version="1.0"?>
            <rdf:RDF xmlns="http://purl.org/net/roles#"
                 xml:base="http://purl.org/net/roles"
                 xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                 xmlns:owl="http://www.w3.org/2002/07/owl#"
                 xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                 xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                <owl:Ontology rdf:about="http://purl.org/net/roles"/>
                <owl:ObjectProperty rdf:about="http://purl.org/net/roles#has_F">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                    <owl:propertyChainAxiom rdf:parseType="Collection">
                        <rdf:Description rdf:about="http://purl.org/net/roles#is_Ac"/>
                        <rdf:Description>
                            <owl:inverseOf rdf:resource="http://purl.org/net/social-reality#context"/>
                        </rdf:Description>
                        <rdf:Description rdf:about="http://purl.org/net/roles#is_F"/>
                        <rdf:Description>
                            <owl:inverseOf rdf:resource="http://purl.org/net/roles#plays"/>
                        </rdf:Description>
                        <rdf:Description rdf:about="http://purl.org/net/roles#is_Ar"/>
                    </owl:propertyChainAxiom>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/roles#has_R">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                    <owl:propertyChainAxiom rdf:parseType="Collection">
                        <rdf:Description>
                            <owl:inverseOf rdf:resource="http://purl.org/net/social-reality#context"/>
                        </rdf:Description>
                        <rdf:Description rdf:about="http://purl.org/net/roles#is_R"/>
                        <rdf:Description>
                            <owl:inverseOf rdf:resource="http://purl.org/net/roles#plays"/>
                        </rdf:Description>
                    </owl:propertyChainAxiom>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/roles#has_TR">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                    <owl:propertyChainAxiom rdf:parseType="Collection">
                        <rdf:Description rdf:about="http://purl.org/net/roles#is_Ac"/>
                        <rdf:Description>
                            <owl:inverseOf rdf:resource="http://purl.org/net/social-reality#context"/>
                        </rdf:Description>
                        <rdf:Description rdf:about="http://purl.org/net/roles#is_TR"/>
                        <rdf:Description>
                            <owl:inverseOf rdf:resource="http://purl.org/net/roles#plays"/>
                        </rdf:Description>
                        <rdf:Description rdf:about="http://purl.org/net/roles#is_Ag"/>
                    </owl:propertyChainAxiom>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/roles#is_Ac">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/roles#is_Ag">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/roles#is_Ar">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/roles#is_F">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/roles#is_R">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/roles#is_TR">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/roles#plays">
                    <rdfs:subPropertyOf rdf:resource="http://purl.org/net/social-reality#counts-as"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/social-reality#context">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/social-reality#counts-as">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/social-reality#has_OR">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                    <owl:propertyChainAxiom rdf:parseType="Collection">
                        <rdf:Description>
                            <owl:inverseOf rdf:resource="http://purl.org/net/social-reality#context"/>
                        </rdf:Description>
                        <rdf:Description rdf:about="http://purl.org/net/social-reality#is_OR"/>
                        <rdf:Description>
                            <owl:inverseOf rdf:resource="http://purl.org/net/social-reality#counts-as"/>
                        </rdf:Description>
                    </owl:propertyChainAxiom>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://purl.org/net/social-reality#is_OR">
                    <rdfs:subPropertyOf rdf:resource="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                </owl:ObjectProperty>
                <owl:ObjectProperty rdf:about="http://www.w3.org/2002/07/owl#topObjectProperty"/>
                <owl:Class rdf:about="http://purl.org/net/roles#Ac">
                    <owl:equivalentClass>
                        <owl:Restriction>
                            <owl:onProperty rdf:resource="http://purl.org/net/roles#is_Ac"/>
                            <owl:hasSelf rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">true</owl:hasSelf>
                        </owl:Restriction>
                    </owl:equivalentClass>
                    <rdfs:subClassOf rdf:resource="http://www.w3.org/2002/07/owl#Thing"/>
                </owl:Class>
                <owl:Class rdf:about="http://purl.org/net/roles#Ag">
                    <owl:equivalentClass>
                        <owl:Restriction>
                            <owl:onProperty rdf:resource="http://purl.org/net/roles#is_Ag"/>
                            <owl:hasSelf rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">true</owl:hasSelf>
                        </owl:Restriction>
                    </owl:equivalentClass>
                    <rdfs:subClassOf rdf:resource="http://www.w3.org/2002/07/owl#Thing"/>
                </owl:Class>
                <owl:Class rdf:about="http://purl.org/net/roles#Ar">
                    <owl:equivalentClass>
                        <owl:Restriction>
                            <owl:onProperty rdf:resource="http://purl.org/net/roles#is_Ar"/>
                            <owl:hasSelf rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">true</owl:hasSelf>
                        </owl:Restriction>
                    </owl:equivalentClass>
                </owl:Class>
                <owl:Class rdf:about="http://purl.org/net/roles#F">
                    <owl:equivalentClass>
                        <owl:Class>
                            <owl:intersectionOf rdf:parseType="Collection">
                                <owl:Restriction>
                                    <owl:onProperty rdf:resource="http://purl.org/net/social-reality#context"/>
                                    <owl:someValuesFrom rdf:resource="http://purl.org/net/roles#Ac"/>
                                </owl:Restriction>
                                <owl:Restriction>
                                    <owl:onProperty>
                                        <rdf:Description>
                                            <owl:inverseOf rdf:resource="http://purl.org/net/roles#plays"/>
                                        </rdf:Description>
                                    </owl:onProperty>
                                    <owl:someValuesFrom rdf:resource="http://purl.org/net/roles#Ar"/>
                                </owl:Restriction>
                            </owl:intersectionOf>
                        </owl:Class>
                    </owl:equivalentClass>
                    <owl:equivalentClass>
                        <owl:Restriction>
                            <owl:onProperty rdf:resource="http://purl.org/net/roles#is_F"/>
                            <owl:hasSelf rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">true</owl:hasSelf>
                        </owl:Restriction>
                    </owl:equivalentClass>
                    <rdfs:subClassOf rdf:resource="http://purl.org/net/social-reality#OR"/>
                </owl:Class>
                <owl:Class rdf:about="http://purl.org/net/roles#R">
                    <owl:equivalentClass>
                        <owl:Restriction>
                            <owl:onProperty>
                                <rdf:Description>
                                    <owl:inverseOf rdf:resource="http://purl.org/net/roles#plays"/>
                                </rdf:Description>
                            </owl:onProperty>
                            <owl:someValuesFrom rdf:resource="http://purl.org/net/social-reality#BF"/>
                        </owl:Restriction>
                    </owl:equivalentClass>
                    <owl:equivalentClass>
                        <owl:Restriction>
                            <owl:onProperty rdf:resource="http://purl.org/net/roles#is_R"/>
                            <owl:hasSelf rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">true</owl:hasSelf>
                        </owl:Restriction>
                    </owl:equivalentClass>
                    <rdfs:subClassOf rdf:resource="http://purl.org/net/social-reality#OR"/>
                </owl:Class>
                <owl:Class rdf:about="http://purl.org/net/roles#TR">
                    <owl:equivalentClass>
                        <owl:Class>
                            <owl:intersectionOf rdf:parseType="Collection">
                                <owl:Restriction>
                                    <owl:onProperty rdf:resource="http://purl.org/net/social-reality#context"/>
                                    <owl:someValuesFrom rdf:resource="http://purl.org/net/roles#Ac"/>
                                </owl:Restriction>
                                <owl:Restriction>
                                    <owl:onProperty>
                                        <rdf:Description>
                                            <owl:inverseOf rdf:resource="http://purl.org/net/roles#plays"/>
                                        </rdf:Description>
                                    </owl:onProperty>
                                    <owl:someValuesFrom rdf:resource="http://purl.org/net/roles#Ag"/>
                                </owl:Restriction>
                            </owl:intersectionOf>
                        </owl:Class>
                    </owl:equivalentClass>
                    <owl:equivalentClass>
                        <owl:Restriction>
                            <owl:onProperty rdf:resource="http://purl.org/net/roles#is_TR"/>
                            <owl:hasSelf rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">true</owl:hasSelf>
                        </owl:Restriction>
                    </owl:equivalentClass>
                    <rdfs:subClassOf rdf:resource="http://purl.org/net/social-reality#OR"/>
                </owl:Class>
                <owl:Class rdf:about="http://purl.org/net/social-reality#BF"/>
                <owl:Class rdf:about="http://purl.org/net/social-reality#C"/>
                <owl:Class rdf:about="http://purl.org/net/social-reality#OR">
                    <owl:equivalentClass>
                        <owl:Class>
                            <owl:intersectionOf rdf:parseType="Collection">
                                <owl:Restriction>
                                    <owl:onProperty rdf:resource="http://purl.org/net/social-reality#context"/>
                                    <owl:someValuesFrom rdf:resource="http://purl.org/net/social-reality#C"/>
                                </owl:Restriction>
                                <owl:Restriction>
                                    <owl:onProperty>
                                        <rdf:Description>
                                            <owl:inverseOf rdf:resource="http://purl.org/net/social-reality#counts-as"/>
                                        </rdf:Description>
                                    </owl:onProperty>
                                    <owl:someValuesFrom rdf:resource="http://purl.org/net/social-reality#BF"/>
                                </owl:Restriction>
                            </owl:intersectionOf>
                        </owl:Class>
                    </owl:equivalentClass>
                    <owl:equivalentClass>
                        <owl:Restriction>
                            <owl:onProperty rdf:resource="http://purl.org/net/social-reality#is_OR"/>
                            <owl:hasSelf rdf:datatype="http://www.w3.org/2001/XMLSchema#boolean">true</owl:hasSelf>
                        </owl:Restriction>
                    </owl:equivalentClass>
                    <rdfs:subClassOf>
                        <owl:Class>
                            <owl:intersectionOf rdf:parseType="Collection">
                                <owl:Restriction>
                                    <owl:onProperty rdf:resource="http://purl.org/net/social-reality#context"/>
                                    <owl:cardinality rdf:datatype="http://www.w3.org/2001/XMLSchema#nonNegativeInteger">1</owl:cardinality>
                                </owl:Restriction>
                                <owl:Restriction>
                                    <owl:onProperty>
                                        <rdf:Description>
                                            <owl:inverseOf rdf:resource="http://purl.org/net/social-reality#counts-as"/>
                                        </rdf:Description>
                                    </owl:onProperty>
                                    <owl:cardinality rdf:datatype="http://www.w3.org/2001/XMLSchema#nonNegativeInteger">1</owl:cardinality>
                                </owl:Restriction>
                            </owl:intersectionOf>
                        </owl:Class>
                    </rdfs:subClassOf>
                </owl:Class>
                <owl:Class rdf:about="http://www.w3.org/2002/07/owl#Thing"/>
            </rdf:RDF>""";

    @Test
    public void shouldFindViolation() throws Exception {
        String input = "<rdf:RDF " +
                "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" " +
                "xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" " +
                "xmlns:owl=\"http://www.w3.org/2002/07/owl#\" >" +
                "<owl:Ontology rdf:about=\"\"/>\n" +
                "<owl:Class rdf:about=\"http://phenomebrowser.net/cellphenotype.owl#C3PO:000000015\">" +
                "<rdf:Description rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">Any.</rdf:Description>" +
                "</owl:Class>" +
                "</rdf:RDF>";
        OWLOntologyManager m = setupManager();
        OWLOntology o = m.loadOntologyFromOntologyDocument(new StringDocumentSource(input));
        OWLIOUtils.print(o);
        OWL2DLProfile p = new OWL2DLProfile();
        OWLProfileReport checkOntology = p.checkOntology(o);
        Assertions.assertEquals(2, checkOntology.getViolations().size());
        OWLProfileViolation v1 = checkOntology.getViolations().get(0);
        OWLProfileViolation v2 = checkOntology.getViolations().get(0);
        Assertions.assertInstanceOf(UseOfReservedVocabularyForAnnotationPropertyIRI.class, v1);
        Assertions.assertInstanceOf(UseOfReservedVocabularyForAnnotationPropertyIRI.class, v2);
    }

    @Test
    public void testGenIdGalenFragment() throws OWLOntologyCreationException {
        String test = """
                <?xml version="1.0"?>
                <rdf:RDF\s
                     xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                     xmlns:owl="http://www.w3.org/2002/07/owl#"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                     xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <owl:Ontology rdf:about="http://www.co-ode.org/ontologies/galen"/>
                <owl:ObjectProperty rdf:about="http://www.co-ode.org/ontologies/galen#hasQuantity"/>
                <owl:Class rdf:about="http://www.co-ode.org/ontologies/galen#test">
                <rdfs:subClassOf><owl:Restriction>
                <owl:onProperty rdf:resource="http://www.co-ode.org/ontologies/galen#hasQuantity"/>
                <owl:someValuesFrom><owl:Class rdf:about="http://www.co-ode.org/ontologies/galen#anotherTest"/></owl:someValuesFrom>
                </owl:Restriction></rdfs:subClassOf></owl:Class></rdf:RDF>""";
        OWLOntology o = loadOntologyFromString(test);
        OWL2DLProfile profile = new OWL2DLProfile();
        OWLProfileReport report = profile.checkOntology(o);
        Assertions.assertTrue(report.isInProfile());
    }

    @Test
    public void testOWLEL() throws OWLOntologyCreationException {
        String onto = """
                <?xml version="1.0"?>
                <!DOCTYPE rdf:RDF [
                <!ENTITY owl "http://www.w3.org/2002/07/owl#" >
                <!ENTITY rdfs "http://www.w3.org/2000/01/rdf-schema#" >
                <!ENTITY rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#" >
                ]>
                <rdf:RDF xmlns="http://xmlns.com/foaf/0.1/"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:owl="http://www.w3.org/2002/07/owl#">
                <owl:Ontology rdf:about="http://ex.com"/>
                <rdf:Property rdf:about="http://ex.com#p1">
                <rdf:type rdf:resource="http://www.w3.org/2002/07/owl#DatatypeProperty"/>
                </rdf:Property>
                <rdf:Property rdf:about="http://ex.com#p2">
                <rdf:type rdf:resource="http://www.w3.org/2002/07/owl#DatatypeProperty"/>
                <rdfs:subPropertyOf rdf:resource="http://ex.com#p1"/>
                </rdf:Property>
                </rdf:RDF>""";
        OWLOntology o = loadOntologyFromString(onto);
        OWL2RLProfile p = new OWL2RLProfile();
        OWLProfileReport report = p.checkOntology(o);
        Assertions.assertTrue(report.getViolations().isEmpty());
    }

    @Test
    public void shouldCauseViolationsWithUseOfPropertyInChain() throws OWLOntologyCreationException {
        OWLOntology o = m.createOntology();
        // SubObjectPropertyOf( ObjectPropertyChain( a:hasFather a:hasBrother )
        // a:hasUncle ) The brother of someone's father is that person's uncle.
        // SubObjectPropertyOf( ObjectPropertyChain( a:hasChild a:hasUncle )
        // a:hasBrother ) The uncle of someone's child is that person's brother.
        OWLObjectProperty father = df.getOWLObjectProperty("urn:test:", "hasFather");
        OWLObjectProperty brother = df.getOWLObjectProperty("urn:test:", "hasBrother");
        OWLObjectProperty child = df.getOWLObjectProperty("urn:test:", "hasChild");
        OWLObjectProperty uncle = df.getOWLObjectProperty("urn:test:", "hasUncle");
        o.getOWLOntologyManager().addAxiom(o, df.getOWLDeclarationAxiom(father));
        o.getOWLOntologyManager().addAxiom(o, df.getOWLDeclarationAxiom(brother));
        o.getOWLOntologyManager().addAxiom(o, df.getOWLDeclarationAxiom(child));
        o.getOWLOntologyManager().addAxiom(o, df.getOWLDeclarationAxiom(uncle));
        OWLSubPropertyChainOfAxiom brokenAxiom1 = df.getOWLSubPropertyChainOfAxiom(Arrays.asList(father, brother),
                uncle);
        OWLSubPropertyChainOfAxiom brokenAxiom2 = df.getOWLSubPropertyChainOfAxiom(Arrays.asList(child, uncle),
                brother);
        OWLObjectPropertyManager manager = new OWLObjectPropertyManager(o);
        o.getOWLOntologyManager().addAxiom(o, brokenAxiom1);
        o.getOWLOntologyManager().addAxiom(o, brokenAxiom2);
        Assertions.assertTrue(manager.isLessThan(brother, uncle));
        Assertions.assertTrue(manager.isLessThan(uncle, brother));
        Assertions.assertTrue(manager.isLessThan(brother, brother));
        Assertions.assertTrue(manager.isLessThan(uncle, uncle));
        OWL2DLProfile profile = new OWL2DLProfile();
        List<OWLProfileViolation> violations = profile.checkOntology(o).getViolations();
        Assertions.assertFalse(violations.isEmpty());
        for (OWLProfileViolation v : violations) {
            Assertions.assertTrue(brokenAxiom1.equals(v.getAxiom()) || brokenAxiom2.equals(v.getAxiom()));
        }
    }

    @Test
    public void shouldNotCauseViolations() throws OWLOntologyCreationException {
        OWLOntology o = m.createOntology();
        OWLObjectProperty father = df.getOWLObjectProperty("urn:test:", "hasFather");
        OWLObjectProperty brother = df.getOWLObjectProperty("urn:test:", "hasBrother");
        OWLObjectProperty child = df.getOWLObjectProperty("urn:test:", "hasChild");
        OWLObjectProperty uncle = df.getOWLObjectProperty("urn:test:", "hasUncle");
        o.getOWLOntologyManager().addAxiom(o, df.getOWLDeclarationAxiom(father));
        o.getOWLOntologyManager().addAxiom(o, df.getOWLDeclarationAxiom(brother));
        o.getOWLOntologyManager().addAxiom(o, df.getOWLDeclarationAxiom(child));
        o.getOWLOntologyManager().addAxiom(o, df.getOWLDeclarationAxiom(uncle));
        OWLSubPropertyChainOfAxiom brokenAxiom1 = df.getOWLSubPropertyChainOfAxiom(CollectionFactory.list(father,
                brother), uncle);
        OWLObjectPropertyManager manager = new OWLObjectPropertyManager(o);
        o.getOWLOntologyManager().addAxiom(o, brokenAxiom1);
        Assertions.assertTrue(manager.isLessThan(brother, uncle));
        OWL2DLProfile profile = new OWL2DLProfile();
        List<OWLProfileViolation> violations = profile.checkOntology(o).getViolations();
        Assertions.assertTrue(violations.isEmpty());
    }

    @Test
    public void shouldNotCauseViolationsInput1() throws OWLOntologyCreationException {
        OWLOntology o = loadOntologyFromString(input1);
        OWL2DLProfile profile = new OWL2DLProfile();
        List<OWLProfileViolation> violations = profile.checkOntology(o).getViolations();
        Assertions.assertTrue(violations.isEmpty());
    }

    @Test
    public void shouldNotCauseViolationsInput2() throws OWLOntologyCreationException {
        OWLOntology o = loadOntologyFromString(input2);
        OWL2DLProfile profile = new OWL2DLProfile();
        List<OWLProfileViolation> violations = profile.checkOntology(o).getViolations();
        Assertions.assertTrue(violations.isEmpty());
    }
}
