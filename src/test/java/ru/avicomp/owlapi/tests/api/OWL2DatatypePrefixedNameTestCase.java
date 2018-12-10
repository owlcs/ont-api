/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.owlapi.tests.api;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.semanticweb.owlapi.vocab.OWL2Datatype.*;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics
 *         Research Group, Date: 04/04/2014
 */
@SuppressWarnings("javadoc")
public class OWL2DatatypePrefixedNameTestCase {

    @Test
    public void shouldReturnCorrectPrefixNameForXMLLiteral() {
        String prefixedName = RDF_XML_LITERAL.getPrefixedName();
        assertThat(prefixedName, is(equalTo("rdf:XMLLiteral")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForLiteral() {
        String prefixedName = RDFS_LITERAL.getPrefixedName();
        assertThat(prefixedName, is(equalTo("rdfs:Literal")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForPlainLiteral() {
        String prefixedName = RDF_PLAIN_LITERAL.getPrefixedName();
        assertThat(prefixedName, is(equalTo("rdf:PlainLiteral")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForreal() {
        String prefixedName = OWL_REAL.getPrefixedName();
        assertThat(prefixedName, is(equalTo("owl:real")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForrational() {
        String prefixedName = OWL_RATIONAL.getPrefixedName();
        assertThat(prefixedName, is(equalTo("owl:rational")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForstring() {
        String prefixedName = XSD_STRING.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:string")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameFornormalizedString() {
        String prefixedName = XSD_NORMALIZED_STRING.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:normalizedString")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameFortoken() {
        String prefixedName = XSD_TOKEN.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:token")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForlanguage() {
        String prefixedName = XSD_LANGUAGE.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:language")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForName() {
        String prefixedName = XSD_NAME.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:Name")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForNCName() {
        String prefixedName = XSD_NCNAME.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:NCName")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForNMTOKEN() {
        String prefixedName = XSD_NMTOKEN.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:NMTOKEN")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameFordecimal() {
        String prefixedName = XSD_DECIMAL.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:decimal")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForinteger() {
        String prefixedName = XSD_INTEGER.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:integer")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameFornonNegativeInteger() {
        String prefixedName = XSD_NON_NEGATIVE_INTEGER.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:nonNegativeInteger")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameFornonPositiveInteger() {
        String prefixedName = XSD_NON_POSITIVE_INTEGER.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:nonPositiveInteger")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForpositiveInteger() {
        String prefixedName = XSD_POSITIVE_INTEGER.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:positiveInteger")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameFornegativeInteger() {
        String prefixedName = XSD_NEGATIVE_INTEGER.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:negativeInteger")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForlong() {
        String prefixedName = XSD_LONG.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:long")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForint() {
        String prefixedName = XSD_INT.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:int")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForshort() {
        String prefixedName = XSD_SHORT.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:short")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForbyte() {
        String prefixedName = XSD_BYTE.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:byte")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForunsignedLong() {
        String prefixedName = XSD_UNSIGNED_LONG.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:unsignedLong")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForunsignedInt() {
        String prefixedName = XSD_UNSIGNED_INT.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:unsignedInt")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForunsignedShort() {
        String prefixedName = XSD_UNSIGNED_SHORT.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:unsignedShort")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForunsignedByte() {
        String prefixedName = XSD_UNSIGNED_BYTE.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:unsignedByte")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameFordouble() {
        String prefixedName = XSD_DOUBLE.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:double")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForfloat() {
        String prefixedName = XSD_FLOAT.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:float")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForboolean() {
        String prefixedName = XSD_BOOLEAN.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:boolean")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForhexBinary() {
        String prefixedName = XSD_HEX_BINARY.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:hexBinary")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForbase64Binary() {
        String prefixedName = XSD_BASE_64_BINARY.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:base64Binary")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameForanyURI() {
        String prefixedName = XSD_ANY_URI.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:anyURI")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameFordateTime() {
        String prefixedName = XSD_DATE_TIME.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:dateTime")));
    }

    @Test
    public void shouldReturnCorrectPrefixNameFordateTimeStamp() {
        String prefixedName = XSD_DATE_TIME_STAMP.getPrefixedName();
        assertThat(prefixedName, is(equalTo("xsd:dateTimeStamp")));
    }
}
