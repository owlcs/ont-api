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

package com.github.owlcs.owlapi.tests.api.syntax.rdf;

import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by ses on 3/10/14.
 */
public class UndeclaredAnnotationTestCase extends TestBase {

    @Test
    public void testRDFXMLUsingUndeclaredAnnotationProperty() throws OWLOntologyCreationException {
        String input = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE rdf:RDF [\n "
                + "<!ENTITY ns \"http://example.com/ns#\" >\n "
                + "<!ENTITY owl \"http://www.w3.org/2002/07/owl#\" >\n "
                + "<!ENTITY xsd \"http://www.w3.org/2001/XMLSchema#\" >\n "
                + "<!ENTITY xml \"http://www.w3.org/XML/1998/namespace\" >\n "
                + "<!ENTITY rdfs \"http://www.w3.org/2000/01/rdf-schema#\" >\n "
                + "<!ENTITY rdf \"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" >\n ]>\n"
                + "<rdf:RDF xmlns=\"http://www.org/\" "
                + "xml:base=\"http://www.org/\" "
                + "xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" "
                + "xmlns:ns=\"http://example.com/ns#\" "
                + "xmlns:owl=\"http://www.w3.org/2002/07/owl#\" "
                + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\" "
                + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
                + "xmlns:xml=\"http://www.w3.org/XML/1998/namespace\">\n"
                + "    <owl:Ontology rdf:about=\"http://www.org/\"/>\n"
                + "    <rdf:Description rdf:about=\"&ns;test\"><ns:rel><rdf:Description ns:pred =\"Not visible\"/></ns:rel></rdf:Description>\n"
                + "</rdf:RDF>";
        OWLOntology oo = loadOntologyFromString(input);
        RDFXMLDocumentFormat format = (RDFXMLDocumentFormat) oo.getFormat();
        Assertions.assertNotNull(format);
        Assertions.assertTrue(format.getOntologyLoaderMetaData().isPresent());
        Assertions.assertEquals(0, format.getOntologyLoaderMetaData().get().getUnparsedTriples()
                .count());
        Set<OWLAnnotationAssertionAxiom> annotationAxioms = oo.axioms(AxiomType.ANNOTATION_ASSERTION).collect(Collectors.toSet());
        Assertions.assertEquals(2, annotationAxioms.size());
        OWLAnnotationProperty p = df.getOWLAnnotationProperty("http://example.com/ns#", "rel");
        OWLAnnotationProperty pp = df.getOWLAnnotationProperty("http://example.com/ns#", "pred");
        Set<OWLAnonymousIndividual> anonymousIndividualSet = oo.anonymousIndividuals().collect(Collectors.toSet());
        Assertions.assertEquals(1, anonymousIndividualSet.size());
        @Nonnull OWLAnonymousIndividual anonymousIndividual = anonymousIndividualSet.iterator().next();
        OWLAnnotationAssertionAxiom relAx = df.getOWLAnnotationAssertionAxiom(p, IRI.create(
                "http://example.com/ns#", "test"), anonymousIndividual);
        OWLLiteral notVisible = df.getOWLLiteral("Not visible", "");
        OWLAnnotationAssertionAxiom predAx = df.getOWLAnnotationAssertionAxiom(pp, anonymousIndividual, notVisible);
        Assertions.assertTrue(annotationAxioms.contains(relAx));
        Assertions.assertTrue(annotationAxioms.contains(predAx));
    }

    @Test
    public void testTurtleUsingUndeclaredAnnotationProperty() throws OWLOntologyCreationException {
        String input = "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + "        @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n"
                + "        @prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + "        @prefix ex: <http://www.example.org/> .\n" + "        [] rdfs:label \"Visible\" ;\n"
                + "           ex:pred ex:Visible ;\n" + "           ex:pred \"Not visible\" .\n"
                + "        ex:subj rdfs:label \"Visible\" .\n" + "        ex:subj ex:pred \"Visible\" .";
        OWLOntology o = loadOntologyFromString(input);
        OWLAnnotationProperty pred = df.getOWLAnnotationProperty("http://www.example.org/", "pred");
        AtomicInteger countLabels = new AtomicInteger();
        AtomicInteger countPreds = new AtomicInteger();
        AtomicInteger countBNodeAnnotations = new AtomicInteger();
        o.axioms(AxiomType.ANNOTATION_ASSERTION).forEach(oa -> {
            if (oa.getProperty().equals(df.getRDFSLabel())) {
                countLabels.incrementAndGet();
            }
            if (oa.getProperty().equals(pred)) {
                countPreds.incrementAndGet();
            }
            if (oa.getSubject() instanceof OWLAnonymousIndividual) {
                countBNodeAnnotations.incrementAndGet();
            }
        });
        Assertions.assertEquals(3, countPreds.intValue());
        Assertions.assertEquals(2, countLabels.intValue());
        Assertions.assertEquals(3, countBNodeAnnotations.intValue());
    }

    @Test
    public void shouldThrowAnExceptionOnError1AndStrictParsing() throws OWLOntologyCreationException {
        String input = " @prefix : <http://www.example.com#> .\n" + " @prefix owl: <http://www.w3.org/2002/07/owl#> .\n"
                + " @prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
                + " @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"
                + " @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\n" + "<urn:test:testonotlogy> a owl:Ontology ."
                + " :subject rdf:type owl:Class ;\n" + "   rdfs:subClassOf [ rdf:type owl:Restriction ;\n"
                + "                owl:onProperty :unknownproperty;\n"
                + "                owl:minCardinality \"0\"^^xsd:nonNegativeInteger\n" + "   ] .";
        OWLOntology o = loadOntologyWithConfig(new StringDocumentSource(input), new OWLOntologyLoaderConfiguration()
                .setStrict(true));
        Assertions.assertEquals(0, o.getLogicalAxiomCount());
    }
}
