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
package com.github.owlcs.owlapi.tests.api.literals;

import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class LiteralTestCase extends TestBase {

    @Test
    public void testHasLangMethod() {
        OWLLiteral literalWithLang = OWLFunctionalSyntaxFactory.Literal("abc", "en");
        Assert.assertTrue(literalWithLang.hasLang());
        OWLLiteral literalWithoutLang = OWLFunctionalSyntaxFactory.Literal("abc", "");
        Assert.assertFalse(literalWithoutLang.hasLang());
    }

    @Test
    public void testGetLangMethod() {
        OWLLiteral literalWithLang = OWLFunctionalSyntaxFactory.Literal("abc", "en");
        Assert.assertEquals("en", literalWithLang.getLang());
        OWLLiteral literalWithoutLang = OWLFunctionalSyntaxFactory.Literal("abc", "");
        Assert.assertEquals("", literalWithoutLang.getLang());
    }

    @Test
    public void testNormalisation() {
        OWLLiteral literalWithLang = OWLFunctionalSyntaxFactory.Literal("abc", "EN");
        Assert.assertEquals("en", literalWithLang.getLang());
        Assert.assertTrue(literalWithLang.hasLang("EN"));
    }

    @Test
    public void testPlainLiteralWithLang() {
        OWLLiteral literalWithLang = OWLFunctionalSyntaxFactory.Literal("abc", "en");
        Assert.assertFalse(literalWithLang.getDatatype().getIRI().isPlainLiteral());
        Assert.assertFalse(literalWithLang.isRDFPlainLiteral());
        Assert.assertTrue(literalWithLang.hasLang());
        Assert.assertEquals("en", literalWithLang.getLang());
        Assert.assertEquals(literalWithLang.getDatatype(), OWL2Datatype.RDF_LANG_STRING.getDatatype(df));
    }

    @Test
    public void testPlainLiteralWithEmbeddedLang() {
        OWLLiteral literal = OWLFunctionalSyntaxFactory.Literal("abc@en", OWLFunctionalSyntaxFactory.PlainLiteral());
        Assert.assertTrue(literal.hasLang());
        Assert.assertFalse(literal.isRDFPlainLiteral());
        Assert.assertEquals("en", literal.getLang());
        Assert.assertEquals("abc", literal.getLiteral());
        Assert.assertEquals(literal.getDatatype(), OWL2Datatype.RDF_LANG_STRING.getDatatype(df));
    }

    @SuppressWarnings("unused") // todo?
    public void tesPlainLiteralWithEmbeddedEmptyLang() {
        OWLLiteral literal = OWLFunctionalSyntaxFactory.Literal("abc@", OWLFunctionalSyntaxFactory.PlainLiteral());
        Assert.assertFalse(literal.hasLang());
        Assert.assertFalse(literal.isRDFPlainLiteral());
        Assert.assertEquals("", literal.getLang());
        Assert.assertEquals("abc", literal.getLiteral());
        Assert.assertEquals(literal.getDatatype(), OWL2Datatype.XSD_STRING.getDatatype(df));
    }

    @SuppressWarnings("unused") // todo?
    public void tesPlainLiteralWithDoubleSep() {
        OWLLiteral literal = OWLFunctionalSyntaxFactory.Literal("abc@@en", OWLFunctionalSyntaxFactory.PlainLiteral());
        Assert.assertTrue(literal.hasLang());
        Assert.assertFalse(literal.isRDFPlainLiteral());
        Assert.assertEquals("en", literal.getLang());
        Assert.assertEquals("abc@", literal.getLiteral());
        Assert.assertEquals(literal.getDatatype(), OWL2Datatype.RDF_LANG_STRING.getDatatype(df));
    }

    @Test
    public void testBoolean() {
        OWLLiteral literal = OWLFunctionalSyntaxFactory.Literal(true);
        Assert.assertTrue(literal.isBoolean());
        Assert.assertTrue(literal.parseBoolean());
        OWLLiteral trueLiteral = OWLFunctionalSyntaxFactory.Literal("true", OWL2Datatype.XSD_BOOLEAN);
        Assert.assertTrue(trueLiteral.isBoolean());
        Assert.assertTrue(trueLiteral.parseBoolean());
        OWLLiteral falseLiteral = OWLFunctionalSyntaxFactory.Literal("false", OWL2Datatype.XSD_BOOLEAN);
        Assert.assertTrue(falseLiteral.isBoolean());
        Assert.assertFalse(falseLiteral.parseBoolean());
        OWLLiteral oneLiteral = OWLFunctionalSyntaxFactory.Literal("1", OWL2Datatype.XSD_BOOLEAN);
        Assert.assertTrue(oneLiteral.isBoolean());
        Assert.assertTrue(oneLiteral.parseBoolean());
        OWLLiteral zeroLiteral = OWLFunctionalSyntaxFactory.Literal("0", OWL2Datatype.XSD_BOOLEAN);
        Assert.assertTrue(zeroLiteral.isBoolean());
        Assert.assertFalse(zeroLiteral.parseBoolean());
    }

    @Test
    public void testBuiltInDatatypes() {
        OWL2Datatype dt = OWL2Datatype.getDatatype(OWLRDFVocabulary.RDF_PLAIN_LITERAL);
        Assert.assertNotNull("object should not be null", dt);
        dt = OWL2Datatype.getDatatype(OWLRDFVocabulary.RDFS_LITERAL);
        Assert.assertNotNull("object should not be null", dt);
        OWLDatatype datatype = df.getOWLDatatype(OWLRDFVocabulary.RDFS_LITERAL);
        Assert.assertNotNull("object should not be null", datatype);
        OWL2Datatype test = datatype.getBuiltInDatatype();
        Assert.assertEquals(test, dt);
    }

    @Test
    public void testFailure() {
        for (IRI type : OWL2Datatype.getDatatypeIRIs()) {
            OWLDatatype datatype = df.getOWLDatatype(type);
            if (!datatype.isBuiltIn()) {
                continue;
            }
            OWL2Datatype builtInDatatype = datatype.getBuiltInDatatype();
            Assert.assertNotNull("object should not be null", builtInDatatype);
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
        Assert.assertTrue(o1.containsAxiom(ax));
        equal(o, o1);
    }
}
