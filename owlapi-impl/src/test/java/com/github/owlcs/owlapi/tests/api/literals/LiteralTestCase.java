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

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class LiteralTestCase extends TestBase {

    @Test
    public void testHasLangMethod() {
        OWLLiteral literalWithLang = OWLFunctionalSyntaxFactory.Literal("abc", "en");
        Assertions.assertTrue(literalWithLang.hasLang());
        OWLLiteral literalWithoutLang = OWLFunctionalSyntaxFactory.Literal("abc", "");
        Assertions.assertFalse(literalWithoutLang.hasLang());
    }

    @Test
    public void testGetLangMethod() {
        OWLLiteral literalWithLang = OWLFunctionalSyntaxFactory.Literal("abc", "en");
        Assertions.assertEquals("en", literalWithLang.getLang());
        OWLLiteral literalWithoutLang = OWLFunctionalSyntaxFactory.Literal("abc", "");
        Assertions.assertEquals("", literalWithoutLang.getLang());
    }

    @Test
    public void testNormalisation() {
        OWLLiteral literalWithLang = OWLFunctionalSyntaxFactory.Literal("abc", "EN");
        Assertions.assertEquals("en", literalWithLang.getLang());
        Assertions.assertTrue(literalWithLang.hasLang("EN"));
    }

    @Test
    public void testPlainLiteralWithLang() {
        OWLLiteral literalWithLang = OWLFunctionalSyntaxFactory.Literal("abc", "en");
        Assertions.assertFalse(literalWithLang.getDatatype().getIRI().isPlainLiteral());
        Assertions.assertFalse(literalWithLang.isRDFPlainLiteral());
        Assertions.assertTrue(literalWithLang.hasLang());
        Assertions.assertEquals("en", literalWithLang.getLang());
        Assertions.assertEquals(literalWithLang.getDatatype(), OWL2Datatype.RDF_LANG_STRING.getDatatype(df));
    }

    @Test
    public void testPlainLiteralWithEmbeddedLang() {
        OWLLiteral literal = OWLFunctionalSyntaxFactory.Literal("abc@en", OWLFunctionalSyntaxFactory.PlainLiteral());
        Assertions.assertTrue(literal.hasLang());
        Assertions.assertFalse(literal.isRDFPlainLiteral());
        Assertions.assertEquals("en", literal.getLang());
        Assertions.assertEquals("abc", literal.getLiteral());
        Assertions.assertEquals(literal.getDatatype(), OWL2Datatype.RDF_LANG_STRING.getDatatype(df));
    }

    @SuppressWarnings("unused") // todo?
    public void tesPlainLiteralWithEmbeddedEmptyLang() {
        OWLLiteral literal = OWLFunctionalSyntaxFactory.Literal("abc@", OWLFunctionalSyntaxFactory.PlainLiteral());
        Assertions.assertFalse(literal.hasLang());
        Assertions.assertFalse(literal.isRDFPlainLiteral());
        Assertions.assertEquals("", literal.getLang());
        Assertions.assertEquals("abc", literal.getLiteral());
        Assertions.assertEquals(literal.getDatatype(), OWL2Datatype.XSD_STRING.getDatatype(df));
    }

    @SuppressWarnings("unused") // todo?
    public void tesPlainLiteralWithDoubleSep() {
        OWLLiteral literal = OWLFunctionalSyntaxFactory.Literal("abc@@en", OWLFunctionalSyntaxFactory.PlainLiteral());
        Assertions.assertTrue(literal.hasLang());
        Assertions.assertFalse(literal.isRDFPlainLiteral());
        Assertions.assertEquals("en", literal.getLang());
        Assertions.assertEquals("abc@", literal.getLiteral());
        Assertions.assertEquals(literal.getDatatype(), OWL2Datatype.RDF_LANG_STRING.getDatatype(df));
    }

    @Test
    public void testBoolean() {
        OWLLiteral literal = OWLFunctionalSyntaxFactory.Literal(true);
        Assertions.assertTrue(literal.isBoolean());
        Assertions.assertTrue(literal.parseBoolean());
        OWLLiteral trueLiteral = OWLFunctionalSyntaxFactory.Literal("true", OWL2Datatype.XSD_BOOLEAN);
        Assertions.assertTrue(trueLiteral.isBoolean());
        Assertions.assertTrue(trueLiteral.parseBoolean());
        OWLLiteral falseLiteral = OWLFunctionalSyntaxFactory.Literal("false", OWL2Datatype.XSD_BOOLEAN);
        Assertions.assertTrue(falseLiteral.isBoolean());
        Assertions.assertFalse(falseLiteral.parseBoolean());
        OWLLiteral oneLiteral = OWLFunctionalSyntaxFactory.Literal("1", OWL2Datatype.XSD_BOOLEAN);
        Assertions.assertTrue(oneLiteral.isBoolean());
        Assertions.assertTrue(oneLiteral.parseBoolean());
        OWLLiteral zeroLiteral = OWLFunctionalSyntaxFactory.Literal("0", OWL2Datatype.XSD_BOOLEAN);
        Assertions.assertTrue(zeroLiteral.isBoolean());
        Assertions.assertFalse(zeroLiteral.parseBoolean());
    }

    @Test
    public void testBuiltInDatatypes() {
        OWL2Datatype dt = OWL2Datatype.getDatatype(OWLRDFVocabulary.RDF_PLAIN_LITERAL);
        Assertions.assertNotNull(dt, "object should not be null");
        dt = OWL2Datatype.getDatatype(OWLRDFVocabulary.RDFS_LITERAL);
        Assertions.assertNotNull(dt, "object should not be null");
        OWLDatatype datatype = df.getOWLDatatype(OWLRDFVocabulary.RDFS_LITERAL);
        Assertions.assertNotNull(datatype, "object should not be null");
        OWL2Datatype test = datatype.getBuiltInDatatype();
        Assertions.assertEquals(test, dt);
    }

    @Test
    public void testFailure() {
        for (IRI type : OWL2Datatype.getDatatypeIRIs()) {
            OWLDatatype datatype = df.getOWLDatatype(type);
            if (!datatype.isBuiltIn()) {
                continue;
            }
            OWL2Datatype builtInDatatype = datatype.getBuiltInDatatype();
            Assertions.assertNotNull(builtInDatatype, "object should not be null");
        }
    }

    @Test
    public void testShouldStoreTagsCorrectly() throws Exception {
        String in = "See more at <a href=\"http://abc.com\">abc</a>";
        OWLOntology o = getOWLOntology();
        OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(OWLFunctionalSyntaxFactory.createIndividual().getIRI(),
                df.getRDFSComment(in));
        o.add(ax);
        OWLOntology o1 = roundTrip(o, new RDFXMLDocumentFormat());
        Assertions.assertTrue(o1.containsAxiom(ax));
        equal(o, o1);
    }
}
