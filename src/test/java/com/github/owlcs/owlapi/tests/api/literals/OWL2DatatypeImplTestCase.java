/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2021, owl.cs group.
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

import com.github.owlcs.ontapi.owlapi.objects.entity.DatatypeImpl;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.DataRangeType;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group
 */
public class OWL2DatatypeImplTestCase extends TestBase {

    private OWLDatatype plainLiteral;

    @BeforeEach
    public void setUpLiteral() {
        plainLiteral = OWL2Datatype.RDF_PLAIN_LITERAL.getDatatype(df);
    }

    @Test
    public void testGetBuiltInDatatype() {
        Assertions.assertEquals(OWL2Datatype.RDF_PLAIN_LITERAL, plainLiteral.getBuiltInDatatype());
    }

    @Test
    public void testIsString() {
        Assertions.assertFalse(plainLiteral.isString());
        Assertions.assertTrue(OWL2Datatype.XSD_STRING.getDatatype(df).isString());
    }

    @Test
    public void testIsInteger() {
        Assertions.assertFalse(plainLiteral.isInteger());
        Assertions.assertTrue(OWL2Datatype.XSD_INTEGER.getDatatype(df).isInteger());
    }

    @Test
    public void testIsFloat() {
        Assertions.assertFalse(plainLiteral.isFloat());
        Assertions.assertTrue(OWL2Datatype.XSD_FLOAT.getDatatype(df).isFloat());
    }

    @Test
    public void testIsDouble() {
        Assertions.assertFalse(plainLiteral.isDouble());
        Assertions.assertTrue(OWL2Datatype.XSD_DOUBLE.getDatatype(df).isDouble());
    }

    @Test
    public void testIsBoolean() {
        Assertions.assertFalse(plainLiteral.isBoolean());
        Assertions.assertTrue(OWL2Datatype.XSD_BOOLEAN.getDatatype(df).isBoolean());
    }

    @Test
    public void testIsRDFPlainLiteral() {
        Assertions.assertTrue(plainLiteral.isRDFPlainLiteral());
        Assertions.assertFalse(OWL2Datatype.XSD_STRING.getDatatype(df).isRDFPlainLiteral());
    }

    @Test
    public void testIsDatatype() {
        Assertions.assertTrue(plainLiteral.isOWLDatatype());
    }

    @Test
    public void testAsOWLDatatype() {
        Assertions.assertEquals(plainLiteral, plainLiteral.asOWLDatatype());
    }

    @Test
    public void testIsTopDatatype() {
        Assertions.assertTrue(OWL2Datatype.RDFS_LITERAL.getDatatype(df).isTopDatatype());
        Assertions.assertFalse(plainLiteral.isTopDatatype());
    }

    @Test
    public void testGetDataRangeType() {
        Assertions.assertEquals(DataRangeType.DATATYPE, plainLiteral.getDataRangeType());
    }

    @Test
    public void testGetEntityType() {
        Assertions.assertEquals(EntityType.DATATYPE, plainLiteral.getEntityType());
    }

    @Test
    public void testIsType() {
        Assertions.assertTrue(plainLiteral.isType(EntityType.DATATYPE));
        Assertions.assertFalse(plainLiteral.isType(EntityType.CLASS));
        Assertions.assertFalse(plainLiteral.isType(EntityType.OBJECT_PROPERTY));
        Assertions.assertFalse(plainLiteral.isType(EntityType.DATA_PROPERTY));
        Assertions.assertFalse(plainLiteral.isType(EntityType.ANNOTATION_PROPERTY));
        Assertions.assertFalse(plainLiteral.isType(EntityType.NAMED_INDIVIDUAL));
    }

    @Test
    public void testIsBuiltIn() {
        Assertions.assertTrue(plainLiteral.isBuiltIn());
    }

    @Test
    public void testIsOWLClass() {
        Assertions.assertFalse(plainLiteral.isOWLClass());
    }

    @Test
    public void testAsOWLClass() {
        Assertions.assertThrows(RuntimeException.class, () -> plainLiteral.asOWLClass());
    }

    @Test
    public void testIsOWLObjectProperty() {
        Assertions.assertFalse(plainLiteral.isOWLObjectProperty());
    }

    @Test
    public void testAsOWLObjectProperty() {
        Assertions.assertThrows(RuntimeException.class, () -> plainLiteral.asOWLObjectProperty());
    }

    @Test
    public void testIsOWLDataProperty() {
        Assertions.assertFalse(plainLiteral.isOWLDataProperty());
    }

    @Test
    public void testAsOWLDataProperty() {
        Assertions.assertThrows(RuntimeException.class, () -> plainLiteral.asOWLDataProperty());
    }

    @Test
    public void testIsOWLAnnotationProperty() {
        Assertions.assertFalse(plainLiteral.isOWLAnnotationProperty());
    }

    @Test
    public void testAsOWLAnnotationProperty() {
        Assertions.assertThrows(RuntimeException.class, () -> plainLiteral.asOWLAnnotationProperty());
    }

    @Test
    public void testIsOWLNamedIndividual() {
        Assertions.assertFalse(plainLiteral.isOWLNamedIndividual());
    }

    @Test
    public void testAsOWLNamedIndividual() {
        Assertions.assertThrows(RuntimeException.class, () -> plainLiteral.asOWLNamedIndividual());
    }

    @Test
    public void toStringID() {
        Assertions.assertNotNull(plainLiteral.toStringID());
        Assertions.assertEquals(OWL2Datatype.RDF_PLAIN_LITERAL.getIRI().toString(), plainLiteral.toStringID());
    }

    @Test
    public void testGetIRI() {
        Assertions.assertNotNull(plainLiteral.getIRI());
        Assertions.assertEquals(OWL2Datatype.RDF_PLAIN_LITERAL.getIRI(), plainLiteral.getIRI());
    }

    @Test
    public void shouldEquals() {
        Assertions.assertEquals(plainLiteral, plainLiteral);
        Assertions.assertEquals(plainLiteral, OWL2Datatype.RDF_PLAIN_LITERAL.getDatatype(df));
        Assertions.assertNotSame(plainLiteral, OWL2Datatype.XSD_STRING.getDatatype(df));
    }

    @Test
    public void testGetSignature() {
        Assertions.assertEquals(plainLiteral.signature().collect(Collectors.toSet()), Collections.singleton(plainLiteral));
    }

    @Test
    public void testGetAnonymousIndividuals() {
        Assertions.assertEquals(0L, plainLiteral.anonymousIndividuals().count());
    }

    @Test
    public void testGetClassesInSignature() {
        Assertions.assertEquals(0, plainLiteral.classesInSignature().count());
    }

    @Test
    public void testGetObjectPropertiesInSignature() {
        Assertions.assertEquals(0, plainLiteral.objectPropertiesInSignature().count());
    }

    @Test
    public void testGetDataPropertiesInSignature() {
        Assertions.assertEquals(0, plainLiteral.dataPropertiesInSignature().count());
    }

    @Test
    public void testGetIndividualsInSignature() {
        Assertions.assertEquals(0, plainLiteral.individualsInSignature().count());
    }

    @Test
    public void testGetNestedClassExpressions() {
        Assertions.assertEquals(0, plainLiteral.nestedClassExpressions().count());
    }

    @Test
    public void testIsTopEntity() {
        Assertions.assertTrue(OWL2Datatype.RDFS_LITERAL.getDatatype(df).isTopDatatype());
        Assertions.assertFalse(OWL2Datatype.RDF_PLAIN_LITERAL.getDatatype(df).isTopDatatype());
    }

    @Test
    public void testIsBottomEntity() {
        Assertions.assertFalse(plainLiteral.isBottomEntity());
    }

    @Test
    public void testContains() {
        IRI iri = OWL2Datatype.XSD_BYTE.getIRI();
        Set<OWLDatatype> datatypes = new HashSet<>();
        DatatypeImpl dtImpl = new DatatypeImpl(iri);
        OWLDatatype dt2Impl = OWL2Datatype.XSD_BYTE.getDatatype(df);
        Assertions.assertEquals(dtImpl, dt2Impl);
        datatypes.add(dt2Impl);
        Assertions.assertTrue(datatypes.contains(dtImpl));
        Assertions.assertEquals(dt2Impl.hashCode(), dtImpl.hashCode());
    }
}
