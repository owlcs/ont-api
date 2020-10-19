/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
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

import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by vincent on 20.08.15.
 */
public class DeclareAnnotatedEntitiesTestCase extends TestBase {

    @Test
    public void testShouldDeclareAllDatatypes() throws Exception {
        String in = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE Ontology [\n"
                + "    <!ENTITY xsd \"http://www.w3.org/2001/XMLSchema#\" >\n"
                + "    <!ENTITY xml \"http://www.w3.org/XML/1998/namespace\" >\n"
                + "    <!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\" >\n"
                + "    <!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" >\n]>\n"
                + "<Ontology xmlns=\"http://www.w3.org/2002/07/owl#\"\n"
                + "     xml:base=\"http://www.semanticweb.org/owlapi-datatypes\"\n"
                + "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
                + "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n"
                + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
                + "     xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"\n"
                + "     ontologyIRI=\"http://www.semanticweb.org/owlapi-datatypes\">\n"
                + "    <Prefix name=\"rdf\" IRI=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>\n"
                + "    <Prefix name=\"rdfs\" IRI=\"http://www.w3.org/2000/01/rdf-schema#\"/>\n"
                + "    <Prefix name=\"xsd\" IRI=\"http://www.w3.org/2001/XMLSchema#\"/>\n"
                + "    <Prefix name=\"owl\" IRI=\"http://www.w3.org/2002/07/owl#\"/>\n"
                + "    <Declaration>\n        <Datatype IRI=\"#myDatatype\"/>\n    </Declaration>\n"
                + "    <Declaration>\n        <Datatype IRI=\"#myDatatype2\"/>\n    </Declaration>\n"
                + "    <AnnotationAssertion>\n"
                + "        <AnnotationProperty abbreviatedIRI=\"rdfs:comment\"/>\n"
                + "        <IRI>#myDatatype2</IRI>\n"
                + "        <Literal datatypeIRI=\"&rdf;PlainLiteral\">myDatatype2 has a comment. " +
                "It causes the all serializers except the OWLXML serializer to omit the declaration of myDatatype2 " +
                "when saving a new ontology created from the annotation axioms. " +
                "Interesting is that they do not omit the declaration of myDatatype that has no comment.</Literal>\n"
                + "    </AnnotationAssertion>\n"
                + "    <AnnotationAssertion>\n"
                + "        <AnnotationProperty abbreviatedIRI=\"rdfs:comment\"/>\n"
                + "        <AbbreviatedIRI>owl:Thing</AbbreviatedIRI>\n"
                + "        <Literal datatypeIRI=\"http://www.semanticweb.org/owlapi-datatypes#myDatatype\">comment " +
                "with datatype myDatatype</Literal>\n"
                + "    </AnnotationAssertion>\n"
                + "    <AnnotationAssertion>\n"
                + "        <AnnotationProperty abbreviatedIRI=\"rdfs:comment\"/>\n"
                + "        <AbbreviatedIRI>owl:Thing</AbbreviatedIRI>\n"
                + "        <Literal datatypeIRI=\"http://www.semanticweb.org/owlapi-datatypes#myDatatype2\">comment " +
                "with datatype myDatatype2</Literal>\n"
                + "    </AnnotationAssertion>\n"
                + "</Ontology>";
        OWLOntology ontology = loadOntologyFromString(in);
        Set<OWLDeclarationAxiom> declarations = ontology.axioms(AxiomType.DECLARATION).collect(Collectors.toSet());
        Set<OWLAnnotationAssertionAxiom> annotationAssertionAxioms = ontology.axioms(AxiomType.ANNOTATION_ASSERTION)
                .collect(Collectors.toSet());
        OWLOntology ontology2 = m1.createOntology();
        ontology2.add(annotationAssertionAxioms);
        OWLOntology o3 = roundTrip(ontology2, new RDFXMLDocumentFormat());
        Set<OWLDeclarationAxiom> reloadedDeclarations = o3.axioms(AxiomType.DECLARATION).collect(Collectors.toSet());
        Assertions.assertEquals(declarations.toString(), reloadedDeclarations.toString());
    }
}
