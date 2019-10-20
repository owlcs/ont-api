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

package ru.avicomp.owlapi.tests.api.syntax;

import org.junit.Test;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import ru.avicomp.owlapi.tests.api.baseclasses.TestBase;

@SuppressWarnings("javadoc")
public class ParserBanningTestCase extends TestBase {

    @Test(expected = OWLOntologyCreationException.class)
    public void shouldFailWithBanningOfTriX() throws OWLOntologyCreationException {
        // This ontology is malformed RDF/XML but does not fail under a regular
        // parsing because the
        // TriX parser does not throw an exception reading it (although it does
        // not recognise any axioms)
        // This test ensures that TriX can be banned from parsing
        String in = "<?xml version=\"1.0\"?>\n"
                + "<rdf:RDF xmlns=\"http://www.semanticweb.org/ontologies/ontologies/2016/2/untitled-ontology-199#\"\n"
                + "     xml:base=\"http://www.semanticweb.org/ontologies/ontologies/2016/2/untitled-ontology-199\"\n"
                + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
                + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
                + "     xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"\n"
                + "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n"
                + "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n"
                + "    <owl:Ontology rdf:about=\"http://www.semanticweb.org/ontologies/ontologies/2016/2/untitled-ontology-199\"/>\n"
                + "    <owl:Class rdf:about=\"http://ontologies.owl/A\">\n"
                + "        <rdfs:comment rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">See more at <a href=\"http://abc.com\">abc</a></rdfs:comment>\n"
                + "    </owl:Class>\n" + "    <owl:Class rdf:about=\"http://ontologies.owl/B\">\n"
                + "        <rdfs:comment rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">a regular comment</rdfs:comment>\n"
                + "    </owl:Class>\n" + "    <owl:Class rdf:about=\"http://ontologies.owl/C\">\n"
                + "        <rdfs:comment rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">another regular comment</rdfs:comment>\n"
                + "    </owl:Class>\n" + "</rdf:RDF>";
        OWLOntologyManager manager = setupManager();
        // org.semanticweb.owlapi.rio.RioBinaryRdfParserFactory
        // org.semanticweb.owlapi.rio.RioJsonLDParserFactory
        // org.semanticweb.owlapi.rio.RioJsonParserFactory
        // org.semanticweb.owlapi.rio.RioN3ParserFactory
        // org.semanticweb.owlapi.rio.RioNQuadsParserFactory
        // org.semanticweb.owlapi.rio.RioNTriplesParserFactory
        // org.semanticweb.owlapi.rio.RioRDFaParserFactory
        // org.semanticweb.owlapi.rio.RioRDFXMLParserFactory
        // org.semanticweb.owlapi.rio.RioTrigParserFactory
        // org.semanticweb.owlapi.rio.RioTrixParserFactory
        // org.semanticweb.owlapi.rio.RioTurtleParserFactory
        String name = "org.semanticweb.owlapi.rio.RioTrixParserFactory";
        manager.getOntologyConfigurator().withBannedParsers(name);
        OWLOntology o = manager.loadOntologyFromOntologyDocument(new StringDocumentSource(in));
    }
}
