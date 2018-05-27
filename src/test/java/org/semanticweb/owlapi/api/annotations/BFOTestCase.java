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

package org.semanticweb.owlapi.api.annotations;

import org.junit.Test;
import org.semanticweb.owlapi.api.baseclasses.TestBase;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParserFactory;

@SuppressWarnings("javadoc")
public class BFOTestCase extends TestBase {

    @Test
    public void shouldparseBFO() throws OWLOntologyCreationException {
        m.getOntologyParsers().set(new RDFXMLParserFactory());
        loadOntologyFromString("<?xml version=\"1.0\"?>\n"
                + "<rdf:RDF xmlns=\"http://purl.obolibrary.org/obo/bfo.owl#\"\n"
                + "     xml:base=\"http://purl.obolibrary.org/obo/bfo.owl\"\n"
                + "     xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "     xmlns:obo=\"http://purl.obolibrary.org/obo/\"\n"
                + "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
                + "     xmlns:foaf=\"http://xmlns.com/foaf/0.1/\"\n"
                + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
                + "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n"
                + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n"
                + "    <owl:Ontology rdf:about=\"http://purl.obolibrary.org/obo/bfo.owl\"/>\n"
                + "\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/BFO_0000179\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/BFO_0000180\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://xmlns.com/foaf/0.1/homepage\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.org/dc/elements/1.1/member\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0010000\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0000115\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0000601\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://www.w3.org/2000/01/rdf-schema#seeAlso\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0000116\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0000602\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.org/dc/elements/1.1/contributor\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0000412\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0000232\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0000119\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://www.w3.org/2000/01/rdf-schema#isDefinedBy\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0000117\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0000118\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0000600\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0000111\"/>\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://purl.obolibrary.org/obo/IAO_0000112\"/>\n"
                + "    \n" + "\n" + "    <owl:Axiom>\n"
                + "        <owl:annotatedTarget rdf:resource=\"http://purl.obolibrary.org/obo/BFO_0000110\"/><!-- has continuant part at all times -->\n"
                + "        <owl:annotatedSource rdf:resource=\"http://purl.obolibrary.org/obo/BFO_0000186\"/><!-- part of continuant at all times that whole exists -->\n"
                + "        <!-- has axiom label --><obo:IAO_0010000 rdf:resource=\"http://purl.obolibrary.org/obo/bfo/axiom/0000602\"/>\n"
                + "        <owl:annotatedProperty rdf:resource=\"http://www.w3.org/2002/07/owl#inverseOf\"/>\n"
                + "    </owl:Axiom>\n" + "    <owl:Axiom>\n"
                + "        <owl:annotatedTarget rdf:resource=\"http://purl.obolibrary.org/obo/BFO_0000177\"/><!-- part of continuant at all times -->\n"
                + "        <owl:annotatedSource rdf:resource=\"http://purl.obolibrary.org/obo/BFO_0000187\"/><!-- has continuant part at all times that part exists -->\n"
                + "        <!-- has axiom label --><obo:IAO_0010000 rdf:resource=\"http://purl.obolibrary.org/obo/bfo/axiom/0000601\"/>\n"
                + "        <owl:annotatedProperty rdf:resource=\"http://www.w3.org/2002/07/owl#inverseOf\"/>\n"
                + "    </owl:Axiom>\n" + "    <owl:Axiom>\n"
                + "        <owl:annotatedTarget rdf:resource=\"http://purl.obolibrary.org/obo/BFO_0000110\"/><!-- has continuant part at all times -->\n"
                + "        <owl:annotatedSource rdf:resource=\"http://purl.obolibrary.org/obo/BFO_0000186\"/><!-- part of continuant at all times that whole exists -->\n"
                + "        <!-- has axiom label --><obo:IAO_0010000 rdf:resource=\"http://purl.obolibrary.org/obo/bfo/axiom/0000602\"/>\n"
                + "        <owl:annotatedProperty rdf:resource=\"http://www.w3.org/2002/07/owl#inverseOf\"/>\n"
                + "    </owl:Axiom>\n" + "    <owl:Axiom>\n"
                + "        <owl:annotatedTarget>This is a binary version of a ternary time-indexed, instance level, relation. Unlike the rest of the temporalized relations which temporally quantify over existence of the subject of the relation, this relation temporally quantifies over the existence of the object of the relation. The relation is provided tentatively, to assess whether the GO needs such a relation. It is inverse of &apos;part of continuant at all times&apos;</owl:annotatedTarget>\n"
                + "        <owl:annotatedSource rdf:resource=\"http://purl.obolibrary.org/obo/BFO_0000187\"/><!-- has continuant part at all times that part exists -->\n"
                + "        <owl:annotatedProperty rdf:resource=\"http://purl.obolibrary.org/obo/IAO_0000116\"/><!-- editor note -->\n"
                + "        <!-- has axiom label --><obo:IAO_0010000 rdf:resource=\"http://purl.obolibrary.org/obo/bfo/axiom/0000600\"/>\n"
                + "    </owl:Axiom>\n" + "    <owl:Axiom>\n"
                + "        <owl:annotatedTarget rdf:resource=\"http://purl.obolibrary.org/obo/BFO_0000177\"/><!-- part of continuant at all times -->\n"
                + "        <owl:annotatedSource rdf:resource=\"http://purl.obolibrary.org/obo/BFO_0000187\"/><!-- has continuant part at all times that part exists -->\n"
                + "        <!-- has axiom label --><obo:IAO_0010000 rdf:resource=\"http://purl.obolibrary.org/obo/bfo/axiom/0000601\"/>\n"
                + "        <owl:annotatedProperty rdf:resource=\"http://www.w3.org/2002/07/owl#inverseOf\"/>\n"
                + "    </owl:Axiom>\n" + "</rdf:RDF>");
    }
}
