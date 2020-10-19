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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.semanticweb.owlapi.vocab.OWL2Datatype.*;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group, Date: 04/04/2014
 */
public class OWL2DatatypePrefixedNameTestCase {

    @Test
    public void testShouldReturnCorrectPrefixNameForXMLLiteral() {
        String prefixedName = RDF_XML_LITERAL.getPrefixedName();
        Assertions.assertEquals("rdf:XMLLiteral", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForLiteral() {
        String prefixedName = RDFS_LITERAL.getPrefixedName();
        Assertions.assertEquals("rdfs:Literal", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForPlainLiteral() {
        String prefixedName = RDF_PLAIN_LITERAL.getPrefixedName();
        Assertions.assertEquals("rdf:PlainLiteral", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForReal() {
        String prefixedName = OWL_REAL.getPrefixedName();
        Assertions.assertEquals("owl:real", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForRational() {
        String prefixedName = OWL_RATIONAL.getPrefixedName();
        Assertions.assertEquals("owl:rational", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForString() {
        String prefixedName = XSD_STRING.getPrefixedName();
        Assertions.assertEquals("xsd:string", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForNormalizedString() {
        String prefixedName = XSD_NORMALIZED_STRING.getPrefixedName();
        Assertions.assertEquals("xsd:normalizedString", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForToken() {
        String prefixedName = XSD_TOKEN.getPrefixedName();
        Assertions.assertEquals("xsd:token", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForLanguage() {
        String prefixedName = XSD_LANGUAGE.getPrefixedName();
        Assertions.assertEquals("xsd:language", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForName() {
        String prefixedName = XSD_NAME.getPrefixedName();
        Assertions.assertEquals("xsd:Name", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForNCName() {
        String prefixedName = XSD_NCNAME.getPrefixedName();
        Assertions.assertEquals("xsd:NCName", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForNMTOKEN() {
        String prefixedName = XSD_NMTOKEN.getPrefixedName();
        Assertions.assertEquals("xsd:NMTOKEN", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForDecimal() {
        String prefixedName = XSD_DECIMAL.getPrefixedName();
        Assertions.assertEquals("xsd:decimal", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForInteger() {
        String prefixedName = XSD_INTEGER.getPrefixedName();
        Assertions.assertEquals("xsd:integer", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForNonNegativeInteger() {
        String prefixedName = XSD_NON_NEGATIVE_INTEGER.getPrefixedName();
        Assertions.assertEquals("xsd:nonNegativeInteger", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForNonPositiveInteger() {
        String prefixedName = XSD_NON_POSITIVE_INTEGER.getPrefixedName();
        Assertions.assertEquals("xsd:nonPositiveInteger", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForPositiveInteger() {
        String prefixedName = XSD_POSITIVE_INTEGER.getPrefixedName();
        Assertions.assertEquals("xsd:positiveInteger", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForNegativeInteger() {
        String prefixedName = XSD_NEGATIVE_INTEGER.getPrefixedName();
        Assertions.assertEquals("xsd:negativeInteger", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForLong() {
        String prefixedName = XSD_LONG.getPrefixedName();
        Assertions.assertEquals("xsd:long", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForInt() {
        String prefixedName = XSD_INT.getPrefixedName();
        Assertions.assertEquals("xsd:int", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForShort() {
        String prefixedName = XSD_SHORT.getPrefixedName();
        Assertions.assertEquals("xsd:short", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForByte() {
        String prefixedName = XSD_BYTE.getPrefixedName();
        Assertions.assertEquals("xsd:byte", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForUnsignedLong() {
        String prefixedName = XSD_UNSIGNED_LONG.getPrefixedName();
        Assertions.assertEquals("xsd:unsignedLong", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForUnsignedInt() {
        String prefixedName = XSD_UNSIGNED_INT.getPrefixedName();
        Assertions.assertEquals("xsd:unsignedInt", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForUnsignedShort() {
        String prefixedName = XSD_UNSIGNED_SHORT.getPrefixedName();
        Assertions.assertEquals("xsd:unsignedShort", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForUnsignedByte() {
        String prefixedName = XSD_UNSIGNED_BYTE.getPrefixedName();
        Assertions.assertEquals("xsd:unsignedByte", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForDouble() {
        String prefixedName = XSD_DOUBLE.getPrefixedName();
        Assertions.assertEquals("xsd:double", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForFloat() {
        String prefixedName = XSD_FLOAT.getPrefixedName();
        Assertions.assertEquals("xsd:float", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForBoolean() {
        String prefixedName = XSD_BOOLEAN.getPrefixedName();
        Assertions.assertEquals("xsd:boolean", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForHexBinary() {
        String prefixedName = XSD_HEX_BINARY.getPrefixedName();
        Assertions.assertEquals("xsd:hexBinary", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForBase64Binary() {
        String prefixedName = XSD_BASE_64_BINARY.getPrefixedName();
        Assertions.assertEquals("xsd:base64Binary", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForAnyURI() {
        String prefixedName = XSD_ANY_URI.getPrefixedName();
        Assertions.assertEquals("xsd:anyURI", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForDateTime() {
        String prefixedName = XSD_DATE_TIME.getPrefixedName();
        Assertions.assertEquals("xsd:dateTime", prefixedName);
    }

    @Test
    public void testShouldReturnCorrectPrefixNameForDateTimeStamp() {
        String prefixedName = XSD_DATE_TIME_STAMP.getPrefixedName();
        Assertions.assertEquals("xsd:dateTimeStamp", prefixedName);
    }
}
