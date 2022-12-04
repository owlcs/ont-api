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

package com.github.owlcs.owlapi.tests.api.annotations;

import com.github.owlcs.owlapi.tests.api.baseclasses.AbstractRoundTrippingTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RioTurtleDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

public class RoundTripOWLXMLToRioTurtleTestCase extends AbstractRoundTrippingTestCase {

    //@formatter:off
    private static final String original = "<?xml version=\"1.0\"?>\n" +
            "<Ontology xmlns=\"http://www.w3.org/2002/07/owl#\"\n" +
            "     xml:base=\"http://www.derivo.de/ontologies/examples/nested_annotations\"\n" +
            "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n" +
            "     xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"\n" +
            "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n" +
            "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n" +
            "     ontologyIRI=\"http://www.derivo.de/ontologies/examples/nested_annotations\">\n" +
            "    <Prefix name=\"owl\" IRI=\"http://www.w3.org/2002/07/owl#\"/>\n" +
            "    <Prefix name=\"rdf\" IRI=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>\n" +
            "    <Prefix name=\"xml\" IRI=\"http://www.w3.org/XML/1998/namespace\"/>\n" +
            "    <Prefix name=\"xsd\" IRI=\"http://www.w3.org/2001/XMLSchema#\"/>\n" +
            "    <Prefix name=\"rdfs\" IRI=\"http://www.w3.org/2000/01/rdf-schema#\"/>\n" +
            "    <Declaration>\n" +
            "        <NamedIndividual IRI=\"#b\"/>\n" +
            "    </Declaration>\n" +
            "    <Declaration>\n" +
            "        <NamedIndividual IRI=\"#c\"/>\n" +
            "    </Declaration>\n" +
            "    <Declaration>\n" +
            "        <NamedIndividual IRI=\"#a\"/>\n" +
            "    </Declaration>\n" +
            "    <Declaration>\n" +
            "        <ObjectProperty IRI=\"#r\"/>\n" +
            "    </Declaration>\n" +
            "    <Declaration>\n" +
            "        <AnnotationProperty abbreviatedIRI=\"rdfs:commment\"/>\n" +
            "    </Declaration>\n" +
            "    <ObjectPropertyAssertion>\n" +
            "        <Annotation>\n" +
            "            <Annotation>\n" +
            "                <AnnotationProperty abbreviatedIRI=\"rdfs:commment\"/>\n" +
            "                <Literal datatypeIRI=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral\">comment for one</Literal>\n" +
            "            </Annotation>\n" +
            "            <AnnotationProperty abbreviatedIRI=\"rdfs:label\"/>\n" +
            "            <Literal datatypeIRI=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral\">one</Literal>\n" +
            "        </Annotation>\n" +
            "        <Annotation>\n" +
            "            <Annotation>\n" +
            "                <AnnotationProperty abbreviatedIRI=\"rdfs:commment\"/>\n" +
            "                <Literal datatypeIRI=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral\">comment for two</Literal>\n" +
            "            </Annotation>\n" +
            "            <AnnotationProperty abbreviatedIRI=\"rdfs:label\"/>\n" +
            "            <Literal datatypeIRI=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral\">two</Literal>\n" +
            "        </Annotation>\n" +
            "        <ObjectProperty IRI=\"#r\"/>\n" +
            "        <NamedIndividual IRI=\"#a\"/>\n" +
            "        <NamedIndividual IRI=\"#b\"/>\n" +
            "    </ObjectPropertyAssertion>\n" +
            "    <ObjectPropertyAssertion>\n" +
            "        <Annotation>\n" +
            "            <Annotation>\n" +
            "                <AnnotationProperty abbreviatedIRI=\"rdfs:commment\"/>\n" +
            "                <Literal datatypeIRI=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral\">comment for three</Literal>\n" +
            "            </Annotation>\n" +
            "            <AnnotationProperty abbreviatedIRI=\"rdfs:label\"/>\n" +
            "            <Literal datatypeIRI=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral\">three</Literal>\n" +
            "        </Annotation>\n" +
            "        <ObjectProperty IRI=\"#r\"/>\n" +
            "        <NamedIndividual IRI=\"#b\"/>\n" +
            "        <NamedIndividual IRI=\"#c\"/>\n" +
            "    </ObjectPropertyAssertion>\n" +
            "</Ontology>";

    //@formatter:on
    @Override
    protected OWLOntology createOntology() {
        try {
            return m.loadOntologyFromOntologyDocument(new StringDocumentSource(original));
        } catch (OWLOntologyCreationException e) {
            return Assertions.fail(e);
        }
    }

    @Test
    public void testShouldRoundTripThroughOWLXML() throws Exception {
        OWLOntology ontology = loadOntologyFromString(original);
        StringDocumentTarget targetOWLXML = new StringDocumentTarget();
        ontology.saveOntology(new OWLXMLDocumentFormat(), targetOWLXML);
        OWLOntology o1 = loadOntologyFromString(targetOWLXML, new OWLXMLDocumentFormat());
        equal(ontology, o1);
    }

    @Test
    public void testShouldRoundTripThroughOWLXMLOrTurtle() throws Exception {
        OWLOntology ontology = loadOntologyFromString(original);
        OWLOntology o1 = roundTrip(ontology, new RioTurtleDocumentFormat());
        equal(ontology, o1);
        OWLOntology o2 = roundTrip(o1, new OWLXMLDocumentFormat());
        equal(o2, o1);
    }

    @Test
    public void testShouldRoundTripThroughOWLXMLToTurtle() throws Exception {
        OWLOntology ontology = loadOntologyFromString(original);
        StringDocumentTarget targetTTL = new StringDocumentTarget();
        ontology.saveOntology(new TurtleDocumentFormat(), targetTTL);
        StringDocumentTarget targetTTLFromTTL = new StringDocumentTarget();
        ontology.saveOntology(new TurtleDocumentFormat(), targetTTLFromTTL);
        Assertions.assertEquals(targetTTL.toString(), targetTTLFromTTL.toString());
    }

    @Test
    public void testShouldRoundTripThroughOWLXMLToRioTurtle() throws Exception {
        OWLOntology ontology = loadOntologyFromString(original);
        StringDocumentTarget target1 = new StringDocumentTarget();
        ontology.saveOntology(new RioTurtleDocumentFormat(), target1);
        StringDocumentTarget target2 = new StringDocumentTarget();
        ontology.saveOntology(new RioTurtleDocumentFormat(), target2);
        Assertions.assertEquals(target1.toString().replaceAll("_:genid[0-9]+", "_:genid"),
                target2.toString().replaceAll("_:genid[0-9]+", "_:genid"));
    }
}
