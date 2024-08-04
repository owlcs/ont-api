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

package com.github.owlcs.ontapi.tests.internal;

import com.github.owlcs.ontapi.TestManagers;
import com.github.owlcs.ontapi.internal.objects.ModelObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLHasValueRestriction;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLNaryAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiomSetShortCut;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiomShortCut;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

/**
 * To test {@link ModelObject}
 * Created by @ssz on 24.09.2019.
 */
public class ModelObjectTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelObjectTest.class);

    @Test
    public void testObjectAsSomeValuesFrom() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLObjectHasValue in = df.getOWLObjectHasValue(df.getOWLObjectProperty("P"), df.getOWLNamedIndividual("I"));
        testAsSomeValuesFrom(in);
    }

    @Test
    public void testDataAsSomeValuesFrom() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLDataHasValue in = df.getOWLDataHasValue(df.getOWLDataProperty("P"), df.getOWLLiteral("I"));
        testAsSomeValuesFrom(in);
    }

    @Test
    public void testObjectAsIntersectionOfMinMax() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLObjectExactCardinality in = df.getOWLObjectExactCardinality(12,
                df.getOWLObjectProperty("P"), df.getOWLClass("X"));
        OWLObjectExactCardinality res = (OWLObjectExactCardinality) ClassExpressionTest.createONTObject(in);
        Assertions.assertInstanceOf(ModelObject.class, res);
        OWLClassExpression c = res.asIntersectionOfMinMax();
        ObjectFactoryTestBase.testObjectHasNoModelReference(c);
    }

    @Test
    public void testDataAsIntersectionOfMinMax() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLDataExactCardinality in = df.getOWLDataExactCardinality(12,
                df.getOWLDataProperty("P"), df.getOWLDatatype("X"));
        OWLDataExactCardinality res = (OWLDataExactCardinality) ClassExpressionTest.createONTObject(in);
        Assertions.assertInstanceOf(ModelObject.class, res);
        OWLClassExpression c = res.asIntersectionOfMinMax();
        ObjectFactoryTestBase.testObjectHasNoModelReference(c);
    }

    @Test
    public void testObjectAsObjectUnionOf() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLObjectOneOf in = df.getOWLObjectOneOf(df.getOWLNamedIndividual("1"), df.getOWLNamedIndividual("2"));
        OWLObjectOneOf res = (OWLObjectOneOf) ClassExpressionTest.createONTObject(in);
        Assertions.assertInstanceOf(ModelObject.class, res);
        OWLClassExpression c = res.asObjectUnionOf();
        ObjectFactoryTestBase.testObjectHasNoModelReference(c);
    }

    @Test
    public void testAssertionGetOWLAnnotation() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLAnnotation expected = df.getRDFSComment("x");
        OWLAnnotationAssertionAxiom ax = df.getOWLAnnotationAssertionAxiom(IRI.create("subject"), expected);
        OWLAnnotationAssertionAxiom res = (OWLAnnotationAssertionAxiom) CommonAxiomsTest
                .createONTObject(TestManagers.createONTManager(), ax);
        Assertions.assertInstanceOf(ModelObject.class, res);
        OWLAnnotation actual = res.getAnnotation();
        Assertions.assertEquals(expected, actual);
        ObjectFactoryTestBase.testObjectHasNoModelReference(actual);
    }

    @Test
    public void testEquivalentClassesEraseModelMethods() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLEquivalentClassesAxiom expected = df.getOWLEquivalentClassesAxiom(df.getOWLClass("X"), df.getOWLClass("Y"),
                Collections.singleton(df.getRDFSComment("x")));
        testSubClassShortCutNaryAxiom(expected);
    }

    @Test
    public void testDisjointClassesEraseModelMethods() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLDisjointClassesAxiom expected = df.getOWLDisjointClassesAxiom(Arrays.asList(df.getOWLClass("X"),
                df.getOWLClass("Y"), df.getOWLClass("Z"), df.getOWLObjectOneOf(df.getOWLNamedIndividual("I"))),
                Collections.singleton(df.getRDFSComment("x")));
        testSubClassShortCutNaryAxiom(expected);
    }

    @Test
    public void testInversePropertiesAxiomEraseModelMethods() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLInverseObjectPropertiesAxiom expected = df.getOWLInverseObjectPropertiesAxiom(df.getOWLObjectProperty("X"),
                df.getOWLObjectProperty("Y"),
                Collections.singleton(df.getRDFSComment("x")));
        OWLInverseObjectPropertiesAxiom actual = testNaryAxiom(expected);
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

    @Test
    public void testDisjointUnionAxiomEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLDisjointUnionAxiom(df.getOWLClass("X"),
                Arrays.asList(df.getOWLObjectOneOf(df.getOWLAnonymousIndividual("_:b33"),
                        df.getOWLNamedIndividual("I")), df.getOWLObjectComplementOf(df.getOWLNothing())),
                Arrays.asList(df.getRDFSComment("x"), df.getRDFSLabel("y"))),
                OWLDisjointUnionAxiom::getOWLDisjointClassesAxiom, OWLDisjointUnionAxiom::getOWLEquivalentClassesAxiom);
    }

    @Test
    public void testDifferentIndividualsEraseModelMethods() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLDifferentIndividualsAxiom expected =
                df.getOWLDifferentIndividualsAxiom(Arrays.asList(df.getOWLAnonymousIndividual("_:b0"),
                        df.getOWLNamedIndividual("Y"), df.getOWLNamedIndividual("Z")),
                        Collections.singleton(df.getRDFSComment("x")));
        testSubClassShortCutNaryAxiom(expected);
    }

    @Test
    public void testSameIndividualsEraseModelMethods() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLSameIndividualAxiom expected = df.getOWLSameIndividualAxiom(df.getOWLNamedIndividual("X"), df.getOWLNamedIndividual("Y"),
                Collections.singleton(df.getRDFSComment("x")));
        testSubClassShortCutNaryAxiom(expected);
    }

    @Test
    public void testDisjointObjectPropertiesEraseModelMethods() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLDisjointObjectPropertiesAxiom expected = df.getOWLDisjointObjectPropertiesAxiom(Arrays.asList(df.getOWLObjectProperty("X"),
                df.getOWLObjectProperty("Y"), df.getOWLObjectProperty("Z"), df.getOWLObjectInverseOf(df.getOWLObjectProperty("W"))),
                Collections.singleton(df.getRDFSLabel("lab")));
        testNaryAxiom(expected);
    }

    @Test
    public void testEquivalentObjectPropertiesAxiomEraseModelMethods() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLEquivalentObjectPropertiesAxiom expected = df.getOWLEquivalentObjectPropertiesAxiom(df.getOWLObjectProperty("X"),
                df.getOWLObjectProperty("Y"),
                Collections.singleton(df.getRDFSComment("x")));
        OWLEquivalentObjectPropertiesAxiom actual = testNaryAxiom(expected);
        testNarySplitMethod(expected, actual, x -> ((OWLEquivalentObjectPropertiesAxiom) x).asSubObjectPropertyOfAxioms());
    }

    @Test
    public void testDisjointDataPropertiesEraseModelMethods() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLDisjointDataPropertiesAxiom expected = df.getOWLDisjointDataPropertiesAxiom(Arrays.asList(df.getOWLDataProperty("X"),
                df.getOWLDataProperty("Y"), df.getOWLDataProperty("Z")), Collections.singleton(df.getRDFSLabel("lab")));
        testNaryAxiom(expected);
    }

    @Test
    public void testEquivalentDataPropertiesAxiomEraseModelMethods() {
        OWLDataFactory df = ObjectFactoryTestBase.ONT_DATA_FACTORY;
        OWLEquivalentDataPropertiesAxiom expected = df.getOWLEquivalentDataPropertiesAxiom(Arrays.asList(df.getOWLDataProperty("X"),
                df.getOWLDataProperty("Y")),
                Arrays.asList(df.getRDFSComment("x"), df.getRDFSComment("y")));
        OWLEquivalentDataPropertiesAxiom actual = testNaryAxiom(expected);
        testNarySplitMethod(expected, actual, x -> ((OWLEquivalentDataPropertiesAxiom) x).asSubDataPropertyOfAxioms());
    }

    @Test
    public void testDataPropertyDomainEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLDataPropertyDomainAxiom(df.getOWLDataProperty("X"),
                df.getOWLObjectOneOf(df.getOWLAnonymousIndividual("_:b33"), df.getOWLNamedIndividual("Y")),
                Arrays.asList(df.getRDFSComment("x"), df.getRDFSLabel("y"))));
    }

    @Test
    public void testObjectPropertyDomainEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLObjectPropertyDomainAxiom(df.getOWLObjectInverseOf(df.getOWLObjectProperty("X")),
                df.getOWLObjectOneOf(df.getOWLAnonymousIndividual("_:b33"), df.getOWLNamedIndividual("Y")),
                Arrays.asList(df.getRDFSComment("x"), df.getRDFSLabel("y"))));
    }

    @Test
    public void testDataPropertyRangeEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLDataPropertyRangeAxiom(df.getOWLDataProperty("X"),
                df.getOWLDataIntersectionOf(df.getStringOWLDatatype(), df.getIntegerOWLDatatype()),
                Arrays.asList(df.getRDFSComment("x"), df.getRDFSLabel("y"))));
    }

    @Test
    public void testObjectPropertyRangeEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLObjectPropertyRangeAxiom(df.getOWLObjectProperty("X"),
                df.getOWLObjectUnionOf(df.getOWLClass("C"), df.getOWLThing()),
                Arrays.asList(df.getRDFSComment("x"), df.getRDFSLabel("y"))));
    }

    @Test
    public void testClassAssertionEraseModelMethods() {
        testUnaryPropAxiom(df -> df.getOWLClassAssertionAxiom(df.getOWLObjectUnionOf(df.getOWLNothing(), df.getOWLClass("C")),
                df.getOWLNamedIndividual("I"), Arrays.asList(df.getRDFSComment("x"), df.getRDFSLabel("y"))));
    }

    @Test
    public void testSWRLRuleEraseModelMethods() {
        CommonAxiomsTest.getAxiomData(AxiomType.SWRL_RULE)
                .forEach(t -> testUnaryPropAxiom(df -> (SWRLRule) t.create(df), SWRLRule::getSimplified));
    }

    @SuppressWarnings("unchecked")
    private <X extends OWLNaryAxiom<?> & OWLSubClassOfAxiomSetShortCut> void testSubClassShortCutNaryAxiom(X expected) {
        X actual = testNaryAxiom(expected);
        testNarySplitMethod(expected, actual, x -> ((X) x).asOWLSubClassOfAxioms());
    }

    @SuppressWarnings("unchecked")
    private <X extends OWLNaryAxiom<?>> X testNaryAxiom(X expected) {
        Collection<? extends OWLAxiom> res = SplitNaryAxiomsTest.createONTAxioms(TestManagers.createONTManager(), expected);
        Assertions.assertEquals(1, res.size());
        X actual = (X) res.iterator().next();
        Assertions.assertInstanceOf(ModelObject.class, actual);
        Assertions.assertEquals(expected, actual);

        testNarySplitMethod(expected, actual, OWLNaryAxiom::asPairwiseAxioms);
        testNarySplitMethod(expected, actual, OWLNaryAxiom::splitToAnnotatedPairs);
        return actual;
    }

    private void testNarySplitMethod(OWLNaryAxiom<?> expected,
                                     OWLNaryAxiom<?> actual,
                                     Function<OWLNaryAxiom<?>, Collection<? extends OWLAxiom>> get) {
        Collection<? extends OWLAxiom> expectedAxioms = get.apply(expected);
        Collection<? extends OWLAxiom> actualAxioms = get.apply(actual);
        Assertions.assertEquals(expectedAxioms, actualAxioms);
        actualAxioms.forEach(ObjectFactoryTestBase::testObjectHasNoModelReference);
    }

    private <X extends OWLSubClassOfAxiomShortCut & OWLAxiom> void testUnaryPropAxiom(Function<OWLDataFactory, X> factory) {
        testUnaryPropAxiom(factory, OWLSubClassOfAxiomShortCut::asOWLSubClassOfAxiom);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    private static <X extends OWLAxiom> void testUnaryPropAxiom(Function<OWLDataFactory, X> factory,
                                                                Function<X, Object>... properties) {
        X ont = factory.apply(ObjectFactoryTestBase.ONT_DATA_FACTORY);
        X owl = factory.apply(ObjectFactoryTestBase.OWL_DATA_FACTORY);
        LOGGER.debug("Test factory properties for '{}'", owl);
        Assertions.assertEquals(owl, ont);
        Assertions.assertNotEquals(0, properties.length);
        for (Function<X, Object> property : properties) {
            Object expected = property.apply(owl);
            LOGGER.debug("Test factory property '{}'", expected);
            Assertions.assertEquals(expected, property.apply(ont));
            OWLObject res = CommonAxiomsTest.createONTObject(TestManagers.createONTManager(), ont);
            Assertions.assertInstanceOf(ModelObject.class, res);
            X actual = (X) res;
            Assertions.assertEquals(owl, actual);
            Object test = property.apply(actual);
            Assertions.assertEquals(expected, test);
            if (test instanceof OWLAxiom) {
                ObjectFactoryTestBase.testObjectHasNoModelReference((OWLObject) test);
            } else if (test instanceof Collection) {
                Collection<OWLAxiom> list = (Collection<OWLAxiom>) test;
                Assertions.assertFalse(list.isEmpty());
                list.forEach(ObjectFactoryTestBase::testObjectHasNoModelReference);
            }
        }
    }

    private void testAsSomeValuesFrom(OWLHasValueRestriction<?> in) {
        OWLHasValueRestriction<?> res = (OWLHasValueRestriction<?>) ClassExpressionTest.createONTObject(in);
        Assertions.assertInstanceOf(ModelObject.class, res);
        OWLClassExpression c = res.asSomeValuesFrom();
        ObjectFactoryTestBase.testObjectHasNoModelReference(c);
    }
}
