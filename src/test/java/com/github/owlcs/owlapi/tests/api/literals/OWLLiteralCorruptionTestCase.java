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
package com.github.owlcs.owlapi.tests.api.literals;

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.util.Optional;


@SuppressWarnings({"javadoc"})
public class OWLLiteralCorruptionTestCase extends TestBase {

    @Test
    public void testShouldRoundTripLiteral() {
        String testString;
        StringBuilder sb = new StringBuilder();
        int count = 17;
        while (count-- > 0) {
            sb.append("200 µLiters + character above U+0FFFF = ");
            sb.appendCodePoint(0x10192); // happens to be "ROMAN SEMUNCIA SIGN"
            sb.append('\n');
        }
        testString = sb.toString();
        OWLLiteral literal = OWLFunctionalSyntaxFactory.Literal(testString);
        Assertions.assertEquals(literal.getLiteral(), testString, "Out = in ? false");
    }

    /**
     * This test shows the difference in XML serialization between OWL-API (something custom) and ONT-API (Jena)
     * OWL-API writes xml-literals as xml-injection (no any changing in the structure, no trimming and quoting):
     * <pre>
     * <owl:NamedIndividual rdf:about="urn:test#i">
     * <test:p rdf:parseType="Literal"><div xmlns="http://www.w3.org/1999/xhtml"><h3>[unknown]</h3><p>(describe NameGroup "[unknown]")</p></div></test:p>
     * </owl:NamedIndividual>
     * </pre>
     * While jena escapes inner xml-code:
     * <pre>
     * <owl:NamedIndividual rdf:about="urn:test#i">
     * <j.0:p rdf:datatype="http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral"
     * >&lt;div xmlns='http://www.w3.org/1999/xhtml'&gt;&lt;h3&gt;[unknown]&lt;/h3&gt;&lt;p&gt;(describe NameGroup "[unknown]")&lt;/p&gt;&lt;/div&gt;</j.0:p>
     * </owl:NamedIndividual>
     * </pre>
     * As result if you use OWL-API after reloading ontology you will NOT find the same literal as specified.
     * But there is no such problem in case of ONT-API.
     *
     * @throws Exception
     */
    @Test
    public void testShouldRoundTripXMLLiteral() throws Exception {
        String literal = "<div xmlns=\"http://www.w3.org/1999/xhtml\"><h3>[unknown]</h3><p>(describe NameGroup \"[unknown]\")</p></div>";
        OWLOntology o = getOWLOntology();
        OWLDataProperty p = df.getOWLDataProperty(IRI.create("urn:test#", "p"));
        OWLLiteral l = df.getOWLLiteral(literal, OWL2Datatype.RDF_XML_LITERAL);
        OWLNamedIndividual i = df.getOWLNamedIndividual(IRI.create("urn:test#", "i"));
        o.add(df.getOWLDataPropertyAssertionAxiom(p, i, l));
        String txt = saveOntology(o, OntFormat.RDF_XML.createOwlFormat()).toString();
        LOGGER.debug(txt);
        OWLOntology res = loadOntologyFromString(txt);
        OWLDataPropertyAssertionAxiom axiom = res.axioms(AxiomType.DATA_PROPERTY_ASSERTION)
                .findFirst().orElseThrow(() -> new AssertionError("Can't find data-property assertion"));

        if (OWLManager.DEBUG_USE_OWL) {
            LOGGER.warn("'{}' != '{}'", literal, axiom.getObject().getLiteral());
            Assertions.assertTrue(txt.contains(literal), "Can't find literal");
        } else {
            Assertions.assertEquals(literal, axiom.getObject().getLiteral(), "Incorrect literal '" + literal + "'");
        }
    }

    @Disabled("should be fixed in Jena-5.2.0") // TODO: enable
    @Test
    public void testShouldFailOnMalformedXMLLiteral() throws Exception {
        String literal = "<ncicp:ComplexDefinition>" +
                "<ncicp:def-definition>" +
                "A form of cancer that begins in melanocytes (cells that make the pigment melanin). " +
                "It may begin in a mole (skin melanoma), but can also begin in other pigmented tissues, " +
                "such as in the eye or in the intestines." +
                "</ncicp:def-definition>" +
                "<ncicp:def-source>NCI-GLOSS</ncicp:def-source>" +
                "</ncicp:ComplexDefinition>";
        if (OWLManager.DEBUG_USE_OWL) {
            String actual = Assertions.assertThrows(OWLOntologyStorageException.class,
                    () -> shouldFailOnMalformedXMLLiteral(literal)).getMessage();
            Assertions.assertTrue(actual.contains(literal));
            Assertions.assertTrue(actual.contains("XML literal is not self contained"));
            return;
        }
        shouldFailOnMalformedXMLLiteral(literal);
    }

    private void shouldFailOnMalformedXMLLiteral(String literal) throws Exception {
        OWLOntology o = m.createOntology();
        OWLDataProperty p = df.getOWLDataProperty(IRI.create("urn:test#", "p"));
        OWLLiteral l = df.getOWLLiteral(literal, OWL2Datatype.RDF_XML_LITERAL);
        OWLNamedIndividual i = df.getOWLNamedIndividual(IRI.create("urn:test#", "i"));
        o.add(df.getOWLDataPropertyAssertionAxiom(p, i, l));
        // OWL-API stops working on the next line:
        String txt = saveOntology(o, OntFormat.RDF_XML.createOwlFormat()).toString();
        // ONT-API checking:
        LOGGER.debug(txt);
        OWLOntology res = loadOntologyFromString(txt);
        OWLDataPropertyAssertionAxiom axiom = res.axioms(AxiomType.DATA_PROPERTY_ASSERTION)
                .findFirst().orElseThrow(() -> new AssertionError("Can't find data-property assertion"));
        Assertions.assertEquals(literal, axiom.getObject().getLiteral(), "Incorrect literal");
    }

    @Test
    public void testShouldAcceptXMLLiteralWithDatatype() throws OWLOntologyStorageException {
        // A bug in OWLAPI means some incorrect XMLLiterals might have been
        // produced.
        // They should be understood in input and saved correctly on roundtrip
        String wrong = "rdf:datatype=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral\"";
        String correct = "rdf:parseType=\"Literal\"";
        String preamble = """
                <?xml version="1.0"?>
                <rdf:RDF xmlns="http://www.w3.org/2002/07/owl#"
                     xml:base="http://www.w3.org/2002/07/owl"
                     xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                     xmlns:owl="http://www.w3.org/2002/07/owl#"
                     xmlns:xml="http://www.w3.org/XML/1998/namespace"
                     xmlns:protege="http://protege.stanford.edu/"
                     xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                     xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
                    <Ontology/>
                    <AnnotationProperty rdf:about="http://protege.stanford.edu/code"/>
                    <Class rdf:about="http://protege.stanford.edu/A">
                        <rdfs:subClassOf rdf:resource="http://www.w3.org/2002/07/owl#Thing"/>
                        <protege:code\s""";
        String closure = """
                ><test>xxx</test></protege:code>
                    </Class>
                </rdf:RDF>""";
        String in1 = preamble + wrong + closure;
        String in2 = preamble + correct + closure;
        LOGGER.debug(in1);
        LOGGER.debug(in2);
        OWLOntology o1 = loadOntologyFromString(in1, IRI.generateDocumentIRI(), new RDFXMLDocumentFormat());
        OWLOntology o2 = loadOntologyFromString(in2, IRI.generateDocumentIRI(), new RDFXMLDocumentFormat());

        OWLLiteral l1 = findFirstAnnotationAssertionLiteral(o1);
        OWLLiteral l2 = findFirstAnnotationAssertionLiteral(o2);
        if (OWLManager.DEBUG_USE_OWL) {
            Assertions.assertEquals(l1, l2, "OWL-API expects identical literals");
        } else {
            // I don't understand what's going on here, but I trust Jena.
            // The second literal has 'xmlns' in additional.
            // Since we can't easily fix Jena, lets think 'it is by design'
            // TODO: check it!
            Assertions.assertEquals("<test>xxx</test>", l1.getLiteral());
            Assertions.assertEquals("<test xmlns=\"http://www.w3.org/2002/07/owl#\">xxx</test>", l2.getLiteral());
        }
        String res1 = saveOntology(o1, new RDFXMLDocumentFormat()).toString();
        String res2 = saveOntology(o2, new RDFXMLDocumentFormat()).toString();
        LOGGER.debug(res1);
        LOGGER.debug(res2);
        Assertions.assertTrue(res1.contains(correct));
        Assertions.assertTrue(res2.contains(correct));
    }

    private static OWLLiteral findFirstAnnotationAssertionLiteral(OWLOntology o) {
        return o.axioms(AxiomType.ANNOTATION_ASSERTION).findFirst()
                .map(OWLAnnotationAssertionAxiom::getValue)
                .map(OWLAnnotationValue::asLiteral)
                .filter(Optional::isPresent)
                .map(Optional::get).orElseThrow(AssertionError::new);
    }

    @Test
    public void testShouldRoundtripPaddedLiterals() throws OWLOntologyCreationException, OWLOntologyStorageException {
        String in = """
                Prefix(:=<urn:test#>)
                Prefix(a:=<urn:test#>)
                Prefix(rdfs:=<http://www.w3.org/2000/01/rdf-schema#>)
                Prefix(owl2xml:=<http://www.w3.org/2006/12/owl2-xml#>)
                Prefix(test:=<urn:test#>)
                Prefix(owl:=<http://www.w3.org/2002/07/owl#>)
                Prefix(xsd:=<http://www.w3.org/2001/XMLSchema#>)
                Prefix(rdf:=<http://www.w3.org/1999/02/22-rdf-syntax-ns#>)
                Ontology(<urn:test>
                DataPropertyAssertion(:dp :c "1"^^xsd:integer) DataPropertyAssertion(:dp :c "01"^^xsd:integer) \
                DataPropertyAssertion(:dp :c "1"^^xsd:short))""";
        OWLOntology o = loadOntologyFromString(new StringDocumentSource(in, IRI.create("urn:test#", "test"), new FunctionalSyntaxDocumentFormat(), null));
        OWLOntology o2 = roundTrip(o, new FunctionalSyntaxDocumentFormat());
        equal(o, o2);
        OWLDataProperty p = df.getOWLDataProperty(IRI.create("urn:test#", "dp"));
        OWLNamedIndividual i = df.getOWLNamedIndividual(IRI.create("urn:test#", "c"));
        Assertions.assertTrue(o.containsAxiom(df.getOWLDataPropertyAssertionAxiom(p, i, df.getOWLLiteral("01", df.getIntegerOWLDatatype()))));
        Assertions.assertTrue(o.containsAxiom(df.getOWLDataPropertyAssertionAxiom(p, i, df.getOWLLiteral("1", df.getIntegerOWLDatatype()))));
        Assertions.assertTrue(o.containsAxiom(df.getOWLDataPropertyAssertionAxiom(p, i, df.getOWLLiteral("1", OWL2Datatype.XSD_SHORT.getDatatype(df)))));
    }

    @Test
    public void testShouldNotFindPaddedLiteralsEqualToNonPadded() {
        Assertions.assertNotEquals(df.getOWLLiteral("01", df.getIntegerOWLDatatype()), df.getOWLLiteral("1", df.getIntegerOWLDatatype()));
        Assertions.assertNotEquals(df.getOWLLiteral("1", df.getIntegerOWLDatatype()), df.getOWLLiteral("01", df.getIntegerOWLDatatype()));
    }
}
