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

import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.XMLUtils;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class XMLUtilsTestCase extends TestBase {

    private static final int CODE_POINT = 0xEFFFF;
    private static final String CODE_POINT_STRING = init();

    private static String init() {
        StringBuilder sb = new StringBuilder();
        sb.appendCodePoint(CODE_POINT);
        return sb.toString();
    }

    @Test
    public void testIsNCName() {
        Assertions.assertTrue(XMLUtils.isNCName(CODE_POINT_STRING + "abc" + CODE_POINT_STRING));
        Assertions.assertTrue(XMLUtils.isNCName(CODE_POINT_STRING + "abc123" + CODE_POINT_STRING));
        Assertions.assertFalse(XMLUtils.isNCName("123" + CODE_POINT_STRING));
        Assertions.assertFalse(XMLUtils.isNCName(CODE_POINT_STRING + ":a"));
        Assertions.assertFalse(XMLUtils.isNCName(""));
        Assertions.assertFalse(XMLUtils.isNCName(null));
    }

    @Test
    public void testIsQName() {
        Assertions.assertTrue(XMLUtils.isQName(CODE_POINT_STRING + "p1:abc" + CODE_POINT_STRING));
        Assertions.assertFalse(XMLUtils.isQName(CODE_POINT_STRING + "p1:2abc" + CODE_POINT_STRING));
        Assertions.assertFalse(XMLUtils.isQName("11" + CODE_POINT_STRING + ":abc" + CODE_POINT_STRING));
        Assertions.assertFalse(XMLUtils.isQName("ab:c%20d"));
    }

    @Test
    public void testEndsWithNCName() {
        Assertions.assertEquals("abc" + CODE_POINT_STRING, XMLUtils.getNCNameSuffix("1abc" + CODE_POINT_STRING));
        Assertions.assertTrue(XMLUtils.hasNCNameSuffix("1abc" + CODE_POINT_STRING));
        Assertions.assertNull(XMLUtils.getNCNameSuffix(CODE_POINT_STRING + "p1:123"));
        Assertions.assertFalse(XMLUtils.hasNCNameSuffix(CODE_POINT_STRING + "p1:123"));
        Assertions.assertEquals("ABC", XMLUtils.getNCNameSuffix("http://owlapi.sourceforge.net/ontology/ABC"));
        Assertions.assertEquals("ABC", XMLUtils.getNCNameSuffix("http://owlapi.sourceforge.net/ontology#ABC"));
        Assertions.assertEquals("ABC", XMLUtils.getNCNameSuffix("http://owlapi.sourceforge.net/ontology:ABC"));
    }

    @Test
    public void testParsesBNode() {
        Assertions.assertEquals("_:test", XMLUtils.getNCNamePrefix("_:test"));
        Assertions.assertNull(XMLUtils.getNCNameSuffix("_:test"));
    }

    /**
     * ONT-API comment:
     * It seems that OWL-API has incorrect behaviour in this testcase:
     * the {@link org.semanticweb.owlapi.vocab.SKOSVocabulary#INSCHEME} is an Object Property
     * (see also <a href='https://www.w3.org/TR/skos-reference/#schemes'>4.3. Class & Property Definitions</a>)
     * Moreover in the test RDF graph snippet below it enters as property in the object property assertion ("a1 PN a2"),
     * so it is incorrect to treat is as an annotation property.
     * The second difference with ONT-API behaviour is in fact that SKOS and DC are built-in vocabularies,
     * so there is no need in explicit declarations.
     *
     * @since 1.1.0: fix data due to changes in jena-3.4.0: Spaces in IRI are illegal. see {@link org.apache.jena.riot.tokens.TokenizerText#AllowSpacesInIRI}
     */
    @SuppressWarnings("JavadocReference")
    @Test
    public void testMissingTypes() {
        // given
        String input = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<rdf:RDF\n"
                + "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
                + "xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\"\n"
                // ONT-API comment: the correct DC NS is 'http://purl.org/dc/elements/1.1/', not 'http://purl.org/dc/elements/1.1#'
                + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n>\n"
                + "<skos:ConceptScheme rdf:about=\"http://www.thesaurus.gc.ca/#CoreSubjectThesaurus\">\n"
                + "<dc:title xml:lang=\"en\">Government of Canada Core Subject Thesaurus</dc:title>\n"
                + "<dc:creator xml:lang=\"en\">Government of Canada</dc:creator>\n"
                + "</skos:ConceptScheme>\n\n"
                + "<skos:Concept rdf:about=\"http://www.thesaurus.gc.ca/concept/#Abbreviations\">\n"
                + "<skos:prefLabel>Abbreviations</skos:prefLabel>\n"
                + "<skos:related rdf:resource=\"http://www.thesaurus.gc.ca/#Terminology\"/>\n"
                + "<skos:inScheme rdf:resource=\"http://www.thesaurus.gc.ca/#CoreSubjectThesaurus\"/>\n"
                + "<skos:prefLabel xml:lang=\"fr\">Abr&#233;viation</skos:prefLabel>\n"
                + "</skos:Concept>\n"
                + "<skos:Concept rdf:about=\"http://www.thesaurus.gc.ca/concept/#Aboriginal%20affairs\">\n"
                + "<skos:prefLabel>Aboriginal affairs</skos:prefLabel>\n"
                + "<skos:altLabel>Aboriginal issues</skos:altLabel>\n"
                + "<skos:related rdf:resource=\"http://www.thesaurus.gc.ca/#Aboriginal%20rights\"/>\n"
                // ONT-API (ver.1.1.0): see method comment above
                //+ "<skos:related rdf:resource=\"http://www.thesaurus.gc.ca/#Land claims\"/>\n"
                + "<skos:related rdf:resource=\"http://www.thesaurus.gc.ca/#Land%20claims\"/>\n"
                + "<skos:inScheme rdf:resource=\"http://www.thesaurus.gc.ca/#CoreSubjectThesaurus\"/>\n"
                + "<skos:prefLabel xml:lang=\"fr\">Affaires autochtones</skos:prefLabel>\n"
                + "</skos:Concept>\n\n"
                + "</rdf:RDF>";
        // when
        OWLOntology o = loadOntologyFromString(input, IRI.getNextDocumentIRI("testuriwithblankspace"), new RDFXMLDocumentFormat());
        com.github.owlcs.ontapi.utils.ReadWriteUtils.print(o);
        o.axioms().forEach(a -> LOGGER.debug(a.toString()));
        // then
        // ONT-API(15) - 7 AnnotationAssertion, 2 ObjectPropertyAssertion, 3 Declaration (owl:NamedIndividual), 3 ClassAssertion
        // OWL-API(15) - 12 AnnotationAssertion, 0 Declaration, 3 ClassAssertion

        if (!OWLManager.DEBUG_USE_OWL) {
            Map<AxiomType<? extends OWLAxiom>, Integer> expected = new HashMap<>();
            expected.put(AxiomType.OBJECT_PROPERTY_ASSERTION, 2);
            expected.put(AxiomType.ANNOTATION_ASSERTION, 7);
            expected.put(AxiomType.CLASS_ASSERTION, 3);
            expected.put(AxiomType.DECLARATION, 3);
            expected.forEach((t, i) -> Assertions.assertEquals(i.longValue(), o.axioms(t).count(), String.format("Should be %d %s.", i, t)));
        }
        Assertions.assertEquals(15, o.getAxiomCount());
    }
}
