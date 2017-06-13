/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.semanticweb.owlapi.api.test.annotations;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.*;

import com.google.common.collect.Sets;

import static org.junit.Assert.assertEquals;
import static org.semanticweb.owlapi.apibinding.OWLFunctionalSyntaxFactory.*;

@ru.avicomp.ontapi.utils.ModifiedForONTApi
@SuppressWarnings("javadoc")
public class AnnotatetAnnotationsTestCase extends TestBase {

    @Test
    public void shouldRoundtripMultipleNestedAnnotationsdebug() throws OWLOntologyCreationException {
        String ns = "urn:n:a#";
        Set<OWLAxiom> axioms = Sets.newHashSet(df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(ns, "r"),
                df.getOWLNamedIndividual(ns, "a"), df.getOWLNamedIndividual(ns, "b"), Arrays.asList(df.getOWLAnnotation(df
                        .getRDFSLabel(), df.getOWLLiteral(1), df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral(3))), df
                        .getOWLAnnotation(df.getRDFSLabel(), df.getOWLLiteral(2), df.getOWLAnnotation(df.getRDFSComment(),
                                df.getOWLLiteral(4))))));
        String input = "<?xml version=\"1.0\"?>\n"
                + "<rdf:RDF xmlns=\"urn:t:o#\" xml:base=\"urn:t:o\"\n xmlns:ann=\"urn:n:a#\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:owl=\"http://www.w3.org/2002/07/owl#\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\" xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n"
                + "    <owl:Ontology rdf:about=\"urn:t:o\"/>\n" + "    <owl:ObjectProperty rdf:about=\"urn:n:a#r\"/>\n"
                + "    <owl:NamedIndividual rdf:about=\"urn:n:a#a\"><ann:r rdf:resource=\"urn:n:a#b\"/></owl:NamedIndividual>\n"
                + "    <owl:Annotation>\n" + "        <owl:annotatedSource>\n"
                + "            <owl:Axiom rdf:nodeID=\"_:genid1\">\n"
                + "                <owl:annotatedSource rdf:resource=\"urn:n:a#a\"/><owl:annotatedProperty rdf:resource=\"urn:n:a#r\"/><owl:annotatedTarget rdf:resource=\"urn:n:a#b\"/>\n"
                + "                <rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#integer\">1</rdfs:label><rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#integer\">2</rdfs:label>\n"
                + "            </owl:Axiom>\n" + "        </owl:annotatedSource>\n"
                + "        <owl:annotatedProperty rdf:resource=\"http://www.w3.org/2000/01/rdf-schema#label\"/><owl:annotatedTarget rdf:datatype=\"http://www.w3.org/2001/XMLSchema#integer\">1</owl:annotatedTarget>\n"
                + "        <rdfs:comment rdf:datatype=\"http://www.w3.org/2001/XMLSchema#integer\">3</rdfs:comment></owl:Annotation>\n"
                + "    <owl:Annotation>\n" + "        <owl:annotatedSource>\n"
                + "            <owl:Axiom rdf:nodeID=\"_:genid1\">\n"
                + "                <owl:annotatedSource rdf:resource=\"urn:n:a#a\"/><owl:annotatedProperty rdf:resource=\"urn:n:a#r\"/><owl:annotatedTarget rdf:resource=\"urn:n:a#b\"/>\n"
                + "                <rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#integer\">1</rdfs:label><rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#integer\">2</rdfs:label>\n"
                + "            </owl:Axiom>\n" + "        </owl:annotatedSource>\n"
                + "        <owl:annotatedProperty rdf:resource=\"http://www.w3.org/2000/01/rdf-schema#label\"/><owl:annotatedTarget rdf:datatype=\"http://www.w3.org/2001/XMLSchema#integer\">2</owl:annotatedTarget>\n"
                + "        <rdfs:comment rdf:datatype=\"http://www.w3.org/2001/XMLSchema#integer\">4</rdfs:comment></owl:Annotation>\n"
                + "    <owl:NamedIndividual rdf:about=\"urn:n:a#b\"/></rdf:RDF>";
        OWLOntology ont = loadOntologyFromString(input);
        // due to smart OWL-API (OWLLiteralImpl.hash != OWLLiteralImplInteger.hash) we can't compare Sets, change to Lists:
        // assertEquals(axioms, OWLAPIStreamUtils.asUnorderedSet(ont.logicalAxioms()));
        assertEquals("Incorrect axioms", axioms.stream().sorted().collect(Collectors.toList()), ont.logicalAxioms().sorted().collect(Collectors.toList()));
    }

    @Test
    public void shouldLoadAnnotatedannotationsCorrectly() throws OWLOntologyCreationException,
            OWLOntologyStorageException {
        IRI subject = IRI.create("http://purl.obolibrary.org/obo/", "UBERON_0000033");
        String input = "<?xml version=\"1.0\"?>\n" + "<rdf:RDF xmlns=\"http://example.com#\"\n"
                + "     xml:base=\"http://example.com\"\n" + "     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
                + "     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
                + "     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n"
                + "     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
                + "     xmlns:oboInOwl=\"http://www.geneontology.org/formats/oboInOwl#\">\n"
                + "    <owl:Ontology rdf:about=\"http://example.com\"/>\n" + "\n"
                + "    <owl:AnnotationProperty rdf:about=\"http://www.geneontology.org/formats/oboInOwl#source\"/>\n" + "\n"
                + "    <owl:Class rdf:about=\"http://purl.obolibrary.org/obo/UBERON_0000033\">\n"
                + "        <rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">head</rdfs:label>\n"
                + "        <oboInOwl:hasDbXref rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">UMLS:C0018670</oboInOwl:hasDbXref>\n"
                + "    </owl:Class>\n" + "    <owl:Axiom>\n"
                + "        <oboInOwl:source rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">NIFSTD:birnlex_1230</oboInOwl:source>\n"
                + "        <owl:annotatedTarget rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">UMLS:C0018670</owl:annotatedTarget>\n"
                + "        <owl:annotatedSource rdf:resource=\"http://purl.obolibrary.org/obo/UBERON_0000033\"/>\n"
                + "        <owl:annotatedProperty rdf:resource=\"http://www.geneontology.org/formats/oboInOwl#hasDbXref\"/>\n"
                + "    </owl:Axiom>\n" + "    <owl:Axiom>\n"
                + "        <owl:annotatedTarget rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">UMLS:C0018670</owl:annotatedTarget>\n"
                + "        <oboInOwl:source rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">ncithesaurus:Head</oboInOwl:source>\n"
                + "        <owl:annotatedSource rdf:resource=\"http://purl.obolibrary.org/obo/UBERON_0000033\"/>\n"
                + "        <owl:annotatedProperty rdf:resource=\"http://www.geneontology.org/formats/oboInOwl#hasDbXref\"/>\n"
                + "    </owl:Axiom>\n" + "</rdf:RDF>";
        OWLOntology testcase = loadOntologyFromString(input);
        long before = testcase.annotationAssertionAxioms(subject).count();
        OWLOntology result = roundTrip(testcase);
        long after = result.annotationAssertionAxioms(subject).count();
        assertEquals(before, after);
    }

    @Test
    public void shouldRecognizeAnnotationsOnAxiomsWithDifferentannotationsAsDistinct() {
        OWLAnnotationProperty p = AnnotationProperty(iri("p"));
        OWLAnnotationSubject i = iri("i");
        OWLLiteral v = Literal("value");
        OWLLiteral ann1 = Literal("value1");
        OWLLiteral ann2 = Literal("value2");
        OWLAnnotationAssertionAxiom ax1 = AnnotationAssertion(p, i, v, singleton(Annotation(RDFSLabel(), ann1)));
        OWLAnnotationAssertionAxiom ax2 = AnnotationAssertion(p, i, v, singleton(Annotation(RDFSLabel(), ann2)));
        Set<OWLAnnotationAssertionAxiom> set = new TreeSet<>();
        set.add(ax1);
        set.add(ax2);
        assertEquals(2, set.size());
    }
}
