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
package com.github.owlcs.owlapi.tests.api.syntax;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.util.Optional;

public class FunctionalSyntaxCommentTestCase extends TestBase {

    @Test
    public void testShouldParseCommentAndSkipIt() throws OWLOntologyCreationException {
        String input = "Prefix(xsd:=<http://www.w3.org/2001/XMLSchema#>)\n"
                + "Prefix(owl:=<http://www.w3.org/2002/07/owl#>)\n"
                + "Prefix(xml:=<http://www.w3.org/XML/1998/namespace>)\n"
                + "Prefix(rdf:=<http://www.w3.org/1999/02/22-rdf-syntax-ns#>)\n"
                + "Prefix(rdfs:=<http://www.w3.org/2000/01/rdf-schema#>)\n"
                + "Prefix(skos:=<http://www.w3.org/2004/02/skos/core#>)\n\n"
                + "Ontology(<file:test.owl>\n" + "Declaration(Class(<urn:test.owl#ContactInformation>))\n"
                + "#Test comment\n" + "Declaration(DataProperty(<urn:test.owl#city>))\n"
                + "SubClassOf(<urn:test.owl#ContactInformation> DataMaxCardinality(1 <urn:test.owl#city> xsd:string))\n"
                + ')';
        OWLOntology o = loadOntologyFromString(input);
        OWLAxiom ax1 = OWLFunctionalSyntaxFactory.Declaration(OWLFunctionalSyntaxFactory.DataProperty(IRI.create("urn:test.owl#", "city")));
        OWLAxiom ax2 = OWLFunctionalSyntaxFactory.SubClassOf(OWLFunctionalSyntaxFactory.Class(IRI.create("urn:test.owl#", "ContactInformation")), OWLFunctionalSyntaxFactory.DataMaxCardinality(1, OWLFunctionalSyntaxFactory.DataProperty(
                IRI.create("urn:test.owl#", "city")), OWLFunctionalSyntaxFactory.Datatype(OWL2Datatype.XSD_STRING.getIRI())));
        OWLAxiom ax3 = OWLFunctionalSyntaxFactory.Declaration(OWLFunctionalSyntaxFactory.Class(IRI.create("urn:test.owl#", "ContactInformation")));
        Assertions.assertTrue(o.containsAxiom(ax1));
        Assertions.assertTrue(o.containsAxiom(ax2));
        Assertions.assertTrue(o.containsAxiom(ax3));
    }

    @Test
    public void testShouldParseCardinalityRestrictionWithMoreThanOneDigitRange() throws OWLOntologyCreationException {
        String in = "Prefix(:=<urn:test#>)" + "Prefix(a:=<urn:test#>)"
                + "Prefix(rdfs:=<http://www.w3.org/2000/01/rdf-schema#>)"
                + "Prefix(owl2xml:=<http://www.w3.org/2006/12/owl2-xml#>)" + "Prefix(test:=<urn:test#>)"
                + "Prefix(owl:=<http://www.w3.org/2002/07/owl#>)" + "Prefix(xsd:=<http://www.w3.org/2001/XMLSchema#>)"
                + "Prefix(rdf:=<http://www.w3.org/1999/02/22-rdf-syntax-ns#>)\n" + "Ontology(<urn:test>\n"
                + "Declaration(NamedIndividual(test:a)) \n" + "    Declaration(Class(test:A)) \n"
                + "    Declaration(DataProperty(test:dp)) \n"
                + "    SubClassOf( test:A DataMinCardinality( 257 test:dp rdfs:Literal ) ) \n"
                + "    SubClassOf( test:A DataAllValuesFrom( test:dp xsd:byte ) ) \n"
                + "    ClassAssertion( test:A test:a ))";
        OWLOntology o = loadOntologyFromString(new StringDocumentSource(in));
        OWLClass a = df.getOWLClass(IRI.create("urn:test#", "A"));
        OWLDataProperty p = df.getOWLDataProperty(IRI.create("urn:test#", "dp"));
        Assertions.assertTrue(o.containsAxiom(df.getOWLSubClassOfAxiom(a, df.getOWLDataMinCardinality(257, p,
                OWL2Datatype.RDFS_LITERAL.getDatatype(df)))));
    }

    @Test
    public void testConvertGetLoadedOntology() throws Exception {
        String input = "Prefix(:=<http://www.example.org/#>)\nOntology(<http://example.org/>\nSubClassOf(:a :b) )";
        OWLOntology origOnt = loadOntologyFromString(input);
        Assertions.assertNotNull(origOnt);
        OWLOntologyManager manager = origOnt.getOWLOntologyManager();
        Assertions.assertEquals(1, manager.ontologies().count());
        Assertions.assertFalse(origOnt.getOntologyID().getVersionIRI().isPresent());
        Assertions.assertTrue(origOnt.getAxiomCount() > 0);
        Optional<IRI> ontologyIRI = origOnt.getOntologyID().getOntologyIRI();
        Assertions.assertTrue(ontologyIRI.isPresent());
        OWLOntology newOnt = manager.getOntology(ontologyIRI.orElseThrow(AssertionError::new));
        Assertions.assertNotNull(newOnt);
    }
}
