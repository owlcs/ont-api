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
package com.github.owlcs.owlapi.tests.api.individuals;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.AbstractRoundTrippingTestCase;
import com.github.owlcs.owlapi.tests.api.baseclasses.AxiomsRoundTrippingBase;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.rdf.rdfxml.parser.OWLRDFXMLParserException;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParser;

/**
 * @author Matthew Horridge, The University of Manchester, Information Management Group
 */
public class RelativeURITestCase extends AbstractRoundTrippingTestCase {

    @Override
    protected OWLOntology createOntology() {
        OWLClass clazz = OWLFunctionalSyntaxFactory.Class(IRI.create(IRI.getNextDocumentIRI(URI_BASE) + "/", "Office"));
        return AxiomsRoundTrippingBase.createOntology(OWLManager.createOWLOntologyManager(),
                () -> Sets.newHashSet(OWLFunctionalSyntaxFactory.Declaration(clazz)));
    }

    @Test
    public void testShouldThrowMeaningfulException() {
        String actual = Assertions.assertThrows(OWLRDFXMLParserException.class, this::shouldThrowMeaningfulException).getMessage();
        Assertions.assertTrue(actual.contains("[line=1:column=378] IRI 'http://example.com/#1#2' " +
                "cannot be resolved against current base IRI "));
    }

    private void shouldThrowMeaningfulException() {
        // on Java 6 for Mac the following assertion does not work: the root
        // exception does not have a message.
        // expectedException
        // .expectMessage(" reason is: Illegal character in fragment at index
        // 21: http://example.com/#1#2");
        String rdfContent = "" + "<rdf:RDF" + "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
                + "    xmlns:owl=\"http://www.w3.org/2002/07/owl#\"" + "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\""
                + "    xmlns=\"http://example.org/rdfxmlparserbug#\""
                + "    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">"
                + "  <owl:Ontology rdf:about=\"http://example.org/rdfxmlparserbug\"/>"
                + "  <owl:Thing rdf:about=\"http://example.com/#1#2\">"
                + "    <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#NamedIndividual\"/>" + "  </owl:Thing>"
                + "</rdf:RDF>";
        OWLOntology ontology = getOWLOntology();
        RDFXMLParser parser = new RDFXMLParser();
        parser.parse(new StringDocumentSource(rdfContent), ontology, config);
    }

}
