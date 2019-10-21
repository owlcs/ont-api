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

package com.github.owlcs.owlapi.tests.api.swrl;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asList;
import static org.semanticweb.owlapi.util.OWLAPIStreamUtils.asUnorderedSet;
import static com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory.Class;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics
 *         Research Group, Date: 04/04/2014
 */
@SuppressWarnings({"javadoc", "null"})
public class SWRLAtomOrderingRoundTripTestCase extends TestBase {

    private final
    @Nonnull
    Set<SWRLAtom> body = new LinkedHashSet<>();
    private final
    @Nonnull
    Set<SWRLAtom> head = new LinkedHashSet<>();
    private
    @Nonnull
    SWRLRule rule;

    @Before
    public void setUpPrefixes() {
        PrefixManager pm = new DefaultPrefixManager(null, null, "http://stuff.com/A/");
        OWLClass clsA = Class("A", pm);
        OWLClass clsB = Class("B", pm);
        OWLClass clsC = Class("C", pm);
        OWLClass clsD = Class("D", pm);
        OWLClass clsE = Class("E", pm);
        SWRLVariable varA = df.getSWRLVariable("http://other.com/A/", "VarA");
        SWRLVariable varB = df.getSWRLVariable("http://other.com/A/", "VarA");
        SWRLVariable varC = df.getSWRLVariable("http://other.com/A/", "VarA");
        body.add(df.getSWRLClassAtom(clsC, varA));
        body.add(df.getSWRLClassAtom(clsB, varB));
        body.add(df.getSWRLClassAtom(clsA, varC));
        head.add(df.getSWRLClassAtom(clsE, varA));
        head.add(df.getSWRLClassAtom(clsD, varA));
        rule = df.getSWRLRule(body, head);
    }

    @Test
    public void individualsShouldNotGetSWRLVariableTypes() throws OWLOntologyStorageException {
        String in = "<rdf:RDF xmlns=\"urn:test#\" xml:base=\"urn:test\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" xmlns:swrlb=\"http://www.w3.org/2003/11/swrlb#\" xmlns:swrl=\"http://www.w3.org/2003/11/swrl#\" xmlns:protege=\"urn:test#\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n"
                + "    <owl:Ontology rdf:about=\"urn:test\"/>\n"
                + "    <owl:ObjectProperty rdf:about=\"urn:test#drives\"/>\n"
                + "    <owl:ObjectProperty rdf:about=\"urn:test#hasDriver\"/>\n"
                + "    <owl:NamedIndividual rdf:about=\"urn:test#i61\"/>\n"
                + "    <owl:NamedIndividual rdf:about=\"urn:test#i62\"/>\n" + "    <rdf:Description>\n"
                + "        <rdf:type rdf:resource=\"http://www.w3.org/2003/11/swrl#Imp\"/>\n"
                + "        <swrl:body rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#nil\"/>\n"
                + "        <swrl:head>\n" + "            <rdf:Description>\n"
                + "                <rdf:type rdf:resource=\"http://www.w3.org/2003/11/swrl#AtomList\"/>\n"
                + "                <rdf:first>\n" + "                    <rdf:Description>\n"
                + "                        <rdf:type rdf:resource=\"http://www.w3.org/2003/11/swrl#IndividualPropertyAtom\"/>\n"
                + "                        <swrl:argument1 rdf:resource=\"urn:test#i61\"/>\n"
                + "                        <swrl:argument2 rdf:resource=\"urn:test#i62\"/>\n"
                + "                        <swrl:propertyPredicate rdf:resource=\"urn:test#drives\"/>\n"
                + "                    </rdf:Description>\n" + "                </rdf:first>\n"
                + "                <rdf:rest rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#nil\"/>\n"
                + "            </rdf:Description>\n" + "        </swrl:head>\n" + "    </rdf:Description>\n"
                + "    <rdf:Description>\n" + "        <rdf:type rdf:resource=\"http://www.w3.org/2003/11/swrl#Imp\"/>\n"
                + "        <rdfs:comment rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">:i62, :i61</rdfs:comment>\n"
                + "        <swrl:body>\n" + "            <rdf:Description>\n"
                + "                <rdf:type rdf:resource=\"http://www.w3.org/2003/11/swrl#AtomList\"/>\n"
                + "                <rdf:first>\n" + "                    <rdf:Description>\n"
                + "                        <rdf:type rdf:resource=\"http://www.w3.org/2003/11/swrl#IndividualPropertyAtom\"/>\n"
                + "                        <swrl:argument1 rdf:resource=\"urn:test#i62\"/>\n"
                + "                        <swrl:argument2 rdf:resource=\"urn:test#i61\"/>\n"
                + "                        <swrl:propertyPredicate rdf:resource=\"urn:test#hasDriver\"/>\n"
                + "                    </rdf:Description>\n" + "                </rdf:first>\n"
                + "                <rdf:rest rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#nil\"/>\n"
                + "            </rdf:Description>\n" + "        </swrl:body>\n" + "        <swrl:head>\n"
                + "            <rdf:Description>\n"
                + "                <rdf:type rdf:resource=\"http://www.w3.org/2003/11/swrl#AtomList\"/>\n"
                + "                <rdf:first>\n" + "                    <rdf:Description>\n"
                + "                        <rdf:type rdf:resource=\"http://www.w3.org/2003/11/swrl#BuiltinAtom\"/>\n"
                + "                        <swrl:arguments rdf:parseType=\"Collection\">\n"
                + "                            <rdf:Description rdf:about=\"urn:test#i62\"/>\n"
                + "                            <rdf:Description rdf:about=\"urn:test#i61\"/>\n"
                + "                        </swrl:arguments>\n"
                + "                        <swrl:builtin rdf:resource=\"http://sqwrl.stanford.edu/ontologies/built-ins/3.4/sqwrl.owl#select\"/>\n"
                + "                    </rdf:Description>\n" + "                </rdf:first>\n"
                + "                <rdf:rest rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#nil\"/>\n"
                + "            </rdf:Description>\n" + "        </swrl:head>\n" + "    </rdf:Description>\n" + "</rdf:RDF>";
        OWLOntology o = loadOntologyFromString(in, IRI.create("urn:test#", "test"), new RDFXMLDocumentFormat());
        String string = saveOntology(o).toString();
        assertFalse(string, string.contains("<rdf:type rdf:resource=\"http://www.w3.org/2003/11/swrl#Variable\"/>"));
    }

    @Test
    public void shouldPreserveOrderingInRDFXMLRoundTrip() throws Exception {
        roundTrip(new RDFXMLDocumentFormat());
    }

    private void roundTrip(OWLDocumentFormat ontologyFormat) throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        OWLOntology ont = getOWLOntology();
        ont.add(rule);
        StringDocumentTarget documentTarget = new StringDocumentTarget();
        ont.saveOntology(ontologyFormat, documentTarget);
        OWLOntology ont2 = loadOntologyFromString(documentTarget);
        Set<SWRLRule> rules = asUnorderedSet(ont2.axioms(AxiomType.SWRL_RULE));
        assertTrue(rules.size() == 1);
        SWRLRule parsedRule = rules.iterator().next();
        assertThat(parsedRule, is(equalTo(rule)));
        List<SWRLAtom> originalBody = new ArrayList<>(body);
        List<SWRLAtom> parsedBody = asList(parsedRule.body());
        assertThat(parsedBody, is(equalTo(originalBody)));
        List<SWRLAtom> originalHead = new ArrayList<>(head);
        List<SWRLAtom> parsedHead = asList(parsedRule.head());
        assertThat(originalHead, is(equalTo(parsedHead)));
    }

    @Test
    public void shouldPreserveOrderingInTurtleRoundTrip() throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        roundTrip(new TurtleDocumentFormat());
    }

    @Test
    public void shouldPreserveOrderingInManchesterSyntaxRoundTrip() throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        roundTrip(new ManchesterSyntaxDocumentFormat());
    }

    @Test
    public void shouldPreserveOrderingInOWLXMLRoundTrip() throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        roundTrip(new OWLXMLDocumentFormat());
    }
}
