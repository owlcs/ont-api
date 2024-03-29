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
package com.github.owlcs.owlapi.tests.api.syntax.rdfxml;

import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 */
public class RDFParserTestCase extends TestBase {

    @Test
    public void testOWLAPI() throws Exception {
        parseFiles("/owlapi/");
    }

    @SuppressWarnings("SameParameterValue")
    private void parseFiles(String base) throws URISyntaxException, OWLOntologyCreationException {
        URL url = getClass().getResource(base.startsWith("/owlapi/") ? base : "/owlapi/" + base);
        File file = new File(Objects.requireNonNull(url).toURI());
        for (File testSuiteFolder : Objects.requireNonNull(file.listFiles())) {
            if (!testSuiteFolder.isDirectory()) {
                continue;
            }
            for (File ontologyFile : Objects.requireNonNull(testSuiteFolder.listFiles())) {
                if (!ontologyFile.getName().endsWith(".rdf") && !ontologyFile.getName().endsWith(".owlapi")) {
                    continue;
                }
                OWLOntology ont = m.loadOntologyFromOntologyDocument(ontologyFile);
                m.removeOntology(ont);
            }
        }
    }

    @Test
    public void shouldParseDataProperty() throws OWLOntologyCreationException {
        String in = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<!DOCTYPE rdf:RDF [\n"
                + "    <!ENTITY b 'http://www.loa-cnr.it/ontologies/ExtendedDnS.owl#'>\n"
                + "    <!ENTITY k 'http://www.loa-cnr.it/ontologies/Plans.owl#'>\n"
                + "    <!ENTITY owl 'http://www.w3.org/2002/07/owl#'>\n"
                + "    <!ENTITY xsd 'http://www.w3.org/2001/XMLSchema#'>\n" + "]>\n" + "\n" + "<rdf:RDF\n"
                + "    xml:base=\"http://www.loa-cnr.it/ontologies/DLP_397.owl\"\n"
                + "    xmlns:b=\"http://www.loa-cnr.it/ontologies/ExtendedDnS.owl#\"\n"
                + "    xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
                + "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
                + "    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n" + "\n" + "<owl:Ontology rdf:about=\"\"/>\n"
                + "\n" + "<owl:Class rdf:about=\"&b;task\">\n"
                + "    <rdfs:comment rdf:datatype=\"&xsd;string\">A course used to sequence activities or other controllable perdurants (some states, processes), usually within methods. They must be defined by a method, but can be *used* by other kinds of descriptions. They are desire targets of some role played by an agent. Tasks can be complex, and ordered according to an abstract succession relation. Tasks can relate to ground activities or decision making; the last kind deals with typical flowchart content. A task is different both from a flowchart node, and from an action or action type.Tasks can be considered shortcuts for plans, since at least one role played by an agent has a desire attitude towards them (possibly different from the one that puts the task into action). In principle, tasks could be transformed into explicit plans.</rdfs:comment>\n"
                + "</owl:Class>\n" + "\n" + "<owl:DatatypeProperty rdf:about=\"&k;iteration-cardinality\">\n"
                + "    <rdfs:comment rdf:datatype=\"&xsd;string\">iteration cardinality can be used to state in a task how many times an action should be repeated</rdfs:comment>\n"
                + "    <rdfs:domain rdf:resource=\"&b;task\"/>\n" + "    <rdfs:range rdf:resource=\"&xsd;integer\"/>\n"
                + "</owl:DatatypeProperty>\n" + "<owl:Datatype rdf:about=\"&xsd;decimal\"/>\n"
                + "<owl:Datatype rdf:about=\"&xsd;integer\"/>\n" + "<owl:Datatype rdf:about=\"&xsd;string\"/>\n"
                + "</rdf:RDF>";
        OWLOntology o = loadOntologyFromString(in);
        Assertions.assertFalse(o.containsObjectPropertyInSignature(IRI.create("http://www.loa-cnr.it/ontologies/Plans.owl#",
                "iteration-cardinality")));
    }

    @Test
    public void shouldLoadSubPropertiesAsObjectProperties() throws OWLOntologyCreationException {
        String in = "<rdf:RDF xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:marc-fields=\"http://bibframe.org/marc/\" xmlns:rda-fields=\"http://bibframe.org/rda/\" xmlns:bf-abstract=\"http://bibframe.org/model-abstract/\" xmlns:bframe=\"http://bibframe.org/vocab/frame\" xmlns:bfmain=\"http://bibframe.org/vocab/main\" xml:base=\"http://bibframe.org/vocab/\">\n"
                + "  <owl:Ontology rdf:about=\"\">\n" + "  </owl:Ontology>\n"
                + "  <rdfs:Class rdf:about=\"http://bibframe.org/vocab/Resource\">\n" + "  </rdfs:Class>\n"
                + "  <rdf:Property rdf:about=\"http://bibframe.org/vocab/relatedTo\">\n"
                + "    <rdfs:domain rdf:resource=\"http://bibframe.org/vocab/Resource\"/>\n"
                + "    <rdfs:range rdf:resource=\"http://bibframe.org/vocab/Resource\"/>\n" + "  </rdf:Property>\n"
                + "  <rdf:Property rdf:about=\"http://bibframe.org/vocab/partOf\">\n"
                + "    <rdfs:subPropertyOf rdf:resource=\"http://bibframe.org/vocab/relatedTo\"/>\n" + "  </rdf:Property>\n"
                + "</rdf:RDF>";
        OWLOntology o = loadOntologyFromString(in);
        Assertions.assertEquals(0, o.axioms(AxiomType.SUB_ANNOTATION_PROPERTY_OF).count());
        Assertions.assertEquals(1, o.axioms(AxiomType.SUB_OBJECT_PROPERTY).count());
    }
}
