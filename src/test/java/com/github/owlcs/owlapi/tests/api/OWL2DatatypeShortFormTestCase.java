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

package com.github.owlcs.owlapi.tests.api;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.semanticweb.owlapi.vocab.OWL2Datatype.*;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group, Date: 04/04/2014
 */
public class OWL2DatatypeShortFormTestCase {

    @Test
    public void testShouldReturnCorrectShortFormForXMLLiteral() {
        String shortForm = RDF_XML_LITERAL.getShortForm();
        Assertions.assertEquals("XMLLiteral", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForLiteral() {
        String shortForm = RDFS_LITERAL.getShortForm();
        Assertions.assertEquals("Literal", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForPlainLiteral() {
        String shortForm = RDF_PLAIN_LITERAL.getShortForm();
        Assertions.assertEquals("PlainLiteral", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForReal() {
        String shortForm = OWL_REAL.getShortForm();
        Assertions.assertEquals("real", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForRational() {
        String shortForm = OWL_RATIONAL.getShortForm();
        Assertions.assertEquals("rational", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForString() {
        String shortForm = XSD_STRING.getShortForm();
        Assertions.assertEquals("string", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForNormalizedString() {
        String shortForm = XSD_NORMALIZED_STRING.getShortForm();
        Assertions.assertEquals("normalizedString", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForToken() {
        String shortForm = XSD_TOKEN.getShortForm();
        Assertions.assertEquals("token", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForLanguage() {
        String shortForm = XSD_LANGUAGE.getShortForm();
        Assertions.assertEquals("language", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForName() {
        String shortForm = XSD_NAME.getShortForm();
        Assertions.assertEquals("Name", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForNCName() {
        String shortForm = XSD_NCNAME.getShortForm();
        Assertions.assertEquals("NCName", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForNMTOKEN() {
        String shortForm = XSD_NMTOKEN.getShortForm();
        Assertions.assertEquals("NMTOKEN", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForDecimal() {
        String shortForm = XSD_DECIMAL.getShortForm();
        Assertions.assertEquals("decimal", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForInteger() {
        String shortForm = XSD_INTEGER.getShortForm();
        Assertions.assertEquals("integer", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForNonNegativeInteger() {
        String shortForm = XSD_NON_NEGATIVE_INTEGER.getShortForm();
        Assertions.assertEquals("nonNegativeInteger", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForNonPositiveInteger() {
        String shortForm = XSD_NON_POSITIVE_INTEGER.getShortForm();
        Assertions.assertEquals("nonPositiveInteger", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForPositiveInteger() {
        String shortForm = XSD_POSITIVE_INTEGER.getShortForm();
        Assertions.assertEquals("positiveInteger", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForNegativeInteger() {
        String shortForm = XSD_NEGATIVE_INTEGER.getShortForm();
        Assertions.assertEquals("negativeInteger", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForLong() {
        String shortForm = XSD_LONG.getShortForm();
        Assertions.assertEquals("long", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForInt() {
        String shortForm = XSD_INT.getShortForm();
        Assertions.assertEquals("int", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForShort() {
        String shortForm = XSD_SHORT.getShortForm();
        Assertions.assertEquals("short", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForByte() {
        String shortForm = XSD_BYTE.getShortForm();
        Assertions.assertEquals("byte", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForUnsignedLong() {
        String shortForm = XSD_UNSIGNED_LONG.getShortForm();
        Assertions.assertEquals("unsignedLong", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForUnsignedInt() {
        String shortForm = XSD_UNSIGNED_INT.getShortForm();
        Assertions.assertEquals("unsignedInt", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForUnsignedShort() {
        String shortForm = XSD_UNSIGNED_SHORT.getShortForm();
        Assertions.assertEquals("unsignedShort", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForUnsignedByte() {
        String shortForm = XSD_UNSIGNED_BYTE.getShortForm();
        Assertions.assertEquals("unsignedByte", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForDouble() {
        String shortForm = XSD_DOUBLE.getShortForm();
        Assertions.assertEquals("double", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForFloat() {
        String shortForm = XSD_FLOAT.getShortForm();
        Assertions.assertEquals("float", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForBoolean() {
        String shortForm = XSD_BOOLEAN.getShortForm();
        Assertions.assertEquals("boolean", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForHexBinary() {
        String shortForm = XSD_HEX_BINARY.getShortForm();
        Assertions.assertEquals("hexBinary", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForBase64Binary() {
        String shortForm = XSD_BASE_64_BINARY.getShortForm();
        Assertions.assertEquals("base64Binary", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForAnyURI() {
        String shortForm = XSD_ANY_URI.getShortForm();
        Assertions.assertEquals("anyURI", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForDateTime() {
        String shortForm = XSD_DATE_TIME.getShortForm();
        Assertions.assertEquals("dateTime", shortForm);
    }

    @Test
    public void testShouldReturnCorrectShortFormForDateTimeStamp() {
        String shortForm = XSD_DATE_TIME_STAMP.getShortForm();
        Assertions.assertEquals("dateTimeStamp", shortForm);
    }
}
