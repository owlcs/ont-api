/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
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

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.tests.TestFactory;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by @ssz on 15.10.2019.
 */
public class AxiomPropertiesTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(AxiomPropertiesTest.class);

    @Test
    public void testSubClassOf() {
        List<TestFactory.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.SUBCLASS_OF);
        testAxiom(data, OWLSubClassOfAxiom::isGCI);
    }

    @Test
    public void testPropertyChains() {
        List<TestFactory.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.SUB_PROPERTY_CHAIN_OF);
        testAxiom(data, OWLSubPropertyChainOfAxiom::isEncodingOfTransitiveProperty,
                OWLSubPropertyChainOfAxiom::getPropertyChain);
    }

    @Test
    public void testHasKey() {
        List<TestFactory.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.HAS_KEY);
        testAxiom(data, (OWLHasKeyAxiom a) -> a.dataPropertyExpressions().collect(Collectors.toSet()),
                (OWLHasKeyAxiom a) -> a.objectPropertyExpressions().collect(Collectors.toSet()));
    }

    @Test
    public void testEquivalentClasses() {
        List<TestFactory.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.EQUIVALENT_CLASSES);
        testSplitNaryAxioms(data, OWLEquivalentClassesAxiom::containsNamedEquivalentClass,
                OWLEquivalentClassesAxiom::containsOWLNothing, OWLEquivalentClassesAxiom::containsOWLThing);
    }

    @Test
    public void testSameIndividuals() {
        List<TestFactory.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.SAME_INDIVIDUAL);
        testSplitNaryAxioms(data, OWLSameIndividualAxiom::containsAnonymousIndividuals);
    }

    @Test
    public void testDisjointObjectProperties() {
        List<TestFactory.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.DISJOINT_OBJECT_PROPERTIES);
        testAxiom(data, (OWLNaryPropertyAxiom<?> a) -> a.properties().collect(Collectors.toList()));
    }

    @Test
    public void testEquivalentObjectProperties() {
        List<TestFactory.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.EQUIVALENT_OBJECT_PROPERTIES);
        testSplitNaryAxioms(data, (OWLNaryPropertyAxiom<?> a) -> a.properties().collect(Collectors.toList()));
    }

    @Test
    public void testDisjointDataProperties() {
        List<TestFactory.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.DISJOINT_DATA_PROPERTIES);
        testAxiom(data, (OWLNaryPropertyAxiom<?> a) -> a.properties().collect(Collectors.toList()));
    }

    @Test
    public void testEquivalentDataProperties() {
        List<TestFactory.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.EQUIVALENT_DATA_PROPERTIES);
        testSplitNaryAxioms(data, (OWLNaryPropertyAxiom<?> a) -> a.properties().collect(Collectors.toList()));
    }

    @Test
    public void testDataPropertyDomain() {
        List<TestFactory.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.DATA_PROPERTY_DOMAIN);
        testAxiom(data, (Function<OWLDataPropertyDomainAxiom, Object>) HasDomain::getDomain, HasProperty::getProperty);
    }

    @Test
    public void testObjectPropertyDomain() {
        List<TestFactory.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.OBJECT_PROPERTY_DOMAIN);
        testAxiom(data, (Function<OWLObjectPropertyDomainAxiom, Object>) HasDomain::getDomain, HasProperty::getProperty);
    }

    @Test
    public void testAnnotationPropertyDomain() {
        List<TestFactory.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.ANNOTATION_PROPERTY_DOMAIN);
        testAxiom(data, (Function<OWLAnnotationPropertyDomainAxiom, Object>) HasDomain::getDomain, HasProperty::getProperty);
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    private static <X extends OWLAxiom> void testAxiom(List<TestFactory.AxiomData> data,
                                                       Function<X, Object>... properties) {
        Assert.assertFalse(data.isEmpty());
        for (TestFactory.AxiomData a : data) {
            LOGGER.debug("Test properties for '{}'", a);
            X owl = (X) a.create(ObjectFactoryTestBase.OWL_DATA_FACTORY);
            X ont = (X) a.create(ObjectFactoryTestBase.ONT_DATA_FACTORY);
            X res = (X) CommonAxiomsTest.createONTObject(OntManagers.createONT(), owl);
            Assert.assertEquals(owl, res);
            for (Function<X, Object> property : properties) {
                Object expected = property.apply(owl);
                Assert.assertEquals(expected, property.apply(ont));
                Assert.assertEquals(expected, property.apply(res));
            }
        }
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    private static <X extends OWLNaryAxiom> void testSplitNaryAxioms(List<TestFactory.AxiomData> data,
                                                                     Function<X, Object>... properties) {
        Assert.assertFalse(data.isEmpty());
        for (TestFactory.AxiomData a : data) {
            LOGGER.debug("Test properties for '{}'", a);
            X base = (X) a.create(ObjectFactoryTestBase.OWL_DATA_FACTORY);
            Collection<X> owlList = ((X) a.create(ObjectFactoryTestBase.OWL_DATA_FACTORY)).asPairwiseAxioms();
            Collection<X> ontList = ((X) a.create(ObjectFactoryTestBase.ONT_DATA_FACTORY)).asPairwiseAxioms();
            Collection<X> resList = (Collection<X>) SplitNaryAxiomsTest.createONTAxioms(OntManagers.createONT(), base);
            for (X owl : owlList) {
                X ont = ontList.stream().filter(owl::equals)
                        .findFirst().orElseThrow(AssertionError::new);
                X res = resList.stream().filter(owl::equals)
                        .findFirst().orElseThrow(AssertionError::new);
                for (Function<X, Object> property : properties) {
                    Object expected = property.apply(owl);
                    Assert.assertEquals(expected, property.apply(ont));
                    Assert.assertEquals(expected, property.apply(res));
                }
            }
        }
    }
}
