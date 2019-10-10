/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.tests.internal;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.internal.objects.ModelObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

/**
 * To test {@link ModelObject}
 * Created by @ssz on 24.09.2019.
 */
public class ModelObjectTest {

    @Test
    public void testObjectAsSomeValuesFrom() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLHasValueRestriction in = df.getOWLObjectHasValue(df.getOWLObjectProperty("P"), df.getOWLNamedIndividual("I"));
        testAsSomeValuesFrom(in);
    }

    @Test
    public void testDataAsSomeValuesFrom() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLHasValueRestriction in = df.getOWLDataHasValue(df.getOWLDataProperty("P"), df.getOWLLiteral("I"));
        testAsSomeValuesFrom(in);
    }

    @Test
    public void testObjectAsIntersectionOfMinMax() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLObjectExactCardinality in = df.getOWLObjectExactCardinality(12, df.getOWLObjectProperty("P"), df.getOWLClass("X"));
        OWLObjectExactCardinality res = (OWLObjectExactCardinality) ClassExpressionTest.createONTObject(in);
        Assert.assertTrue(res instanceof ModelObject);
        OWLClassExpression c = res.asIntersectionOfMinMax();
        ObjectFactoryTestBase.testObjectHasNoModelReference(c);
    }

    @Test
    public void testDataAsIntersectionOfMinMax() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLDataExactCardinality in = df.getOWLDataExactCardinality(12, df.getOWLDataProperty("P"), df.getOWLDatatype("X"));
        OWLDataExactCardinality res = (OWLDataExactCardinality) ClassExpressionTest.createONTObject(in);
        Assert.assertTrue(res instanceof ModelObject);
        OWLClassExpression c = res.asIntersectionOfMinMax();
        ObjectFactoryTestBase.testObjectHasNoModelReference(c);
    }

    @Test
    public void testObjectAsObjectUnionOf() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLObjectOneOf in = df.getOWLObjectOneOf(df.getOWLNamedIndividual("1"), df.getOWLNamedIndividual("2"));
        OWLObjectOneOf res = (OWLObjectOneOf) ClassExpressionTest.createONTObject(in);
        Assert.assertTrue(res instanceof ModelObject);
        OWLClassExpression c = res.asObjectUnionOf();
        ObjectFactoryTestBase.testObjectHasNoModelReference(c);
    }

    @Test
    public void testAssertionGetOWLAnnotation() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLAnnotation expected = df.getRDFSComment("x");
        OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(IRI.create("subject"), expected);
        OWLAnnotationAssertionAxiom res = (OWLAnnotationAssertionAxiom) CommonAxiomsTest.createONTObject(OntManagers.createONT(), ax);
        Assert.assertTrue(res instanceof ModelObject);
        OWLAnnotation actual = res.getAnnotation();
        Assert.assertEquals(expected, actual);
        ObjectFactoryTestBase.testObjectHasNoModelReference(actual);
    }

    private void testAsSomeValuesFrom(OWLHasValueRestriction in) {
        OWLHasValueRestriction res = (OWLHasValueRestriction) ClassExpressionTest.createONTObject(in);
        Assert.assertTrue(res instanceof ModelObject);
        OWLClassExpression c = res.asSomeValuesFrom();
        ObjectFactoryTestBase.testObjectHasNoModelReference(c);
    }

    @Test
    public void testEquivalentClassesEraseModelMethods() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLEquivalentClassesAxiom expected = df.getOWLEquivalentClassesAxiom(df.getOWLClass("X"), df.getOWLClass("Y"),
                Collections.singleton(df.getRDFSComment("x")));
        Collection<? extends OWLAxiom> res = NaryAxiomsTest.createONTAxioms(OntManagers.createONT(), expected);
        Assert.assertEquals(1, res.size());
        OWLEquivalentClassesAxiom actual = (OWLEquivalentClassesAxiom) res.iterator().next();
        Assert.assertTrue(actual instanceof ModelObject);
        Assert.assertEquals(expected, actual);

        testNarySplitMethod(expected, actual, OWLNaryAxiom::asPairwiseAxioms);
        testNarySplitMethod(expected, actual, OWLNaryAxiom::splitToAnnotatedPairs);
        testNarySplitMethod(expected, actual, x -> ((OWLEquivalentClassesAxiom) x).asOWLSubClassOfAxioms());
    }

    @Test
    public void testInversePropertiesAxiomEraseModelMethods() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLInverseObjectPropertiesAxiom expected = df.getOWLInverseObjectPropertiesAxiom(df.getOWLObjectProperty("X"),
                df.getOWLObjectProperty("Y"),
                Collections.singleton(df.getRDFSComment("x")));
        Collection<? extends OWLAxiom> res = NaryAxiomsTest.createONTAxioms(OntManagers.createONT(), expected);
        Assert.assertEquals(1, res.size());
        OWLInverseObjectPropertiesAxiom actual = (OWLInverseObjectPropertiesAxiom) res.iterator().next();
        Assert.assertTrue(actual instanceof ModelObject);
        Assert.assertEquals(expected, actual);

        testNarySplitMethod(expected, actual, OWLNaryAxiom::asPairwiseAxioms);
        testNarySplitMethod(expected, actual, OWLNaryAxiom::splitToAnnotatedPairs);
        testNarySplitMethod(expected, actual, x -> ((OWLInverseObjectPropertiesAxiom) x).asSubObjectPropertyOfAxioms());
    }

    @Test
    public void testFunctionalObjectAxiomEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLFunctionalObjectPropertyAxiom(df.getOWLObjectProperty("X"),
                Collections.singleton(df.getRDFSComment("x"))));
    }

    @Test
    public void testFunctionalDataAxiomEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLFunctionalDataPropertyAxiom(df.getOWLDataProperty("X")));
    }

    @Test
    public void testInverseFunctionalObjectAxiomEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLInverseFunctionalObjectPropertyAxiom(df.getOWLObjectProperty("X"),
                Collections.singleton(df.getRDFSComment("x"))));
    }

    @Test
    public void testReflexiveObjectPropertyEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLReflexiveObjectPropertyAxiom(df.getOWLObjectProperty("X"),
                Arrays.asList(df.getRDFSComment("x"), df.getRDFSLabel("y"))));
    }

    @Test
    public void testIrreflexiveObjectPropertyEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLIrreflexiveObjectPropertyAxiom(df.getOWLObjectProperty("X"),
                Arrays.asList(df.getRDFSComment("x"), df.getRDFSLabel("y"))));
    }

    @Test
    public void testSymmetricObjectPropertyEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLSymmetricObjectPropertyAxiom(df.getOWLObjectProperty("X"),
                Arrays.asList(df.getRDFSComment("x"), df.getRDFSLabel("y"))),
                OWLSymmetricObjectPropertyAxiom::asSubPropertyAxioms);
    }

    @Test
    public void testObjectPropertyAssertionAxiomEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty("X"),
                df.getOWLAnonymousIndividual("_:b33"), df.getOWLNamedIndividual("I"),
                Arrays.asList(df.getRDFSComment("x"), df.getRDFSLabel("y"))));
    }

    @Test
    public void testDataPropertyAssertionAxiomEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty("X"),
                df.getOWLNamedIndividual("I"), df.getOWLLiteral(true),
                Collections.singletonList(df.getRDFSComment("x"))));
    }

    @Test
    public void testNegativeObjectPropertyAssertionAxiomEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLNegativeObjectPropertyAssertionAxiom(
                df.getOWLObjectInverseOf(df.getOWLObjectProperty("X")),
                df.getOWLNamedIndividual("I"), df.getOWLAnonymousIndividual("_:b33"),
                Arrays.asList(df.getRDFSComment("x"), df.getRDFSLabel("y"))));
    }

    @Test
    public void testNegativeDataPropertyAssertionAxiomEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLNegativeDataPropertyAssertionAxiom(df.getOWLDataProperty("X"),
                df.getOWLAnonymousIndividual("_:b33"), df.getOWLLiteral(true),
                Arrays.asList(df.getRDFSComment("x"), df.getRDFSLabel("y"))));
    }

    private <X extends OWLSubClassOfAxiomShortCut & OWLAxiom> void testUnaryPropAxiom(Function<OWLDataFactory, X> factory) {
        testUnaryPropAxiom(factory, OWLSubClassOfAxiomShortCut::asOWLSubClassOfAxiom);
    }

    @SuppressWarnings("unchecked")
    private <X extends OWLAxiom> void testUnaryPropAxiom(Function<OWLDataFactory, X> factory,
                                                         Function<X, Object> get) {
        X ont = factory.apply(ObjectFactoryTestBase.ONT_DATA_FACTORY);
        X owl = factory.apply(ObjectFactoryTestBase.OWL_DATA_FACTORY);
        Assert.assertEquals(owl, ont);
        Assert.assertEquals(get.apply(owl), get.apply(ont));
        OWLObject res = CommonAxiomsTest.createONTObject(OntManagers.createONT(), ont);
        Assert.assertTrue(res instanceof ModelObject);
        X actual = (X) res;
        Assert.assertEquals(owl, actual);
        Object test = get.apply(actual);
        Assert.assertEquals(get.apply(owl), test);
        if (test instanceof OWLAxiom) {
            ObjectFactoryTestBase.testObjectHasNoModelReference((OWLObject) test);
        } else if (test instanceof Collection) {
            Collection<OWLAxiom> list = (Collection<OWLAxiom>) test;
            Assert.assertFalse(list.isEmpty());
            list.forEach(ObjectFactoryTestBase::testObjectHasNoModelReference);
        }
    }

    private void testNarySplitMethod(OWLNaryAxiom expected,
                                     OWLNaryAxiom actual,
                                     Function<OWLNaryAxiom, Collection<? extends OWLAxiom>> get) {
        Collection<? extends OWLAxiom> expectedAxioms = get.apply(expected);
        Collection<? extends OWLAxiom> actualAxioms = get.apply(actual);
        Assert.assertEquals(expectedAxioms, actualAxioms);
        actualAxioms.forEach(ObjectFactoryTestBase::testObjectHasNoModelReference);
    }

}
