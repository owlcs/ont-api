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

import com.github.owlcs.ontapi.TestDataCollection;
import com.github.owlcs.ontapi.TestManagers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.HasDomain;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.HasRange;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLNaryAxiom;
import org.semanticweb.owlapi.model.OWLNaryPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.SWRLRule;
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
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.SUBCLASS_OF);
        testAxiom(data, OWLSubClassOfAxiom::isGCI);
    }

    @Test
    public void testPropertyChains() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.SUB_PROPERTY_CHAIN_OF);
        testAxiom(data, OWLSubPropertyChainOfAxiom::isEncodingOfTransitiveProperty,
                OWLSubPropertyChainOfAxiom::getPropertyChain);
    }

    @Test
    public void testHasKey() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.HAS_KEY);
        testAxiom(data, (OWLHasKeyAxiom a) -> a.dataPropertyExpressions().collect(Collectors.toSet()),
                (OWLHasKeyAxiom a) -> a.objectPropertyExpressions().collect(Collectors.toSet()));
    }

    @Test
    public void testEquivalentClasses() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.EQUIVALENT_CLASSES);
        testSplitNaryAxioms(data, OWLEquivalentClassesAxiom::containsNamedEquivalentClass,
                OWLEquivalentClassesAxiom::containsOWLNothing, OWLEquivalentClassesAxiom::containsOWLThing);
    }

    @Test
    public void testSameIndividuals() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.SAME_INDIVIDUAL);
        testSplitNaryAxioms(data, OWLSameIndividualAxiom::containsAnonymousIndividuals);
    }

    @Test
    public void testDisjointObjectProperties() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.DISJOINT_OBJECT_PROPERTIES);
        testAxiom(data, (OWLNaryPropertyAxiom<?> a) -> a.properties().collect(Collectors.toList()));
    }

    @Test
    public void testEquivalentObjectProperties() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.EQUIVALENT_OBJECT_PROPERTIES);
        testSplitNaryAxioms(data, (OWLNaryPropertyAxiom<?> a) -> a.properties().collect(Collectors.toList()));
    }

    @Test
    public void testDisjointDataProperties() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.DISJOINT_DATA_PROPERTIES);
        testAxiom(data, (OWLNaryPropertyAxiom<?> a) -> a.properties().collect(Collectors.toList()));
    }

    @Test
    public void testEquivalentDataProperties() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.EQUIVALENT_DATA_PROPERTIES);
        testSplitNaryAxioms(data, (OWLNaryPropertyAxiom<?> a) -> a.properties().collect(Collectors.toList()));
    }

    @Test
    public void testDataPropertyDomain() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.DATA_PROPERTY_DOMAIN);
        testAxiom(data, (Function<OWLDataPropertyDomainAxiom, Object>) HasDomain::getDomain, HasProperty::getProperty);
    }

    @Test
    public void testObjectPropertyDomain() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.OBJECT_PROPERTY_DOMAIN);
        testAxiom(data, (Function<OWLObjectPropertyDomainAxiom, Object>) HasDomain::getDomain, HasProperty::getProperty);
    }

    @Test
    public void testAnnotationPropertyDomain() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.ANNOTATION_PROPERTY_DOMAIN);
        testAxiom(data, (Function<OWLAnnotationPropertyDomainAxiom, Object>) HasDomain::getDomain, HasProperty::getProperty);
    }

    @Test
    public void testAnnotationPropertyRange() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.ANNOTATION_PROPERTY_RANGE);
        testAxiom(data, (Function<OWLAnnotationPropertyRangeAxiom, Object>) HasRange::getRange, HasProperty::getProperty);
    }

    @Test
    public void testDataPropertyRange() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.DATA_PROPERTY_RANGE);
        testAxiom(data, (Function<OWLDataPropertyRangeAxiom, Object>) HasRange::getRange, HasProperty::getProperty);
    }

    @Test
    public void testObjectPropertyRange() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.OBJECT_PROPERTY_RANGE);
        testAxiom(data, (Function<OWLObjectPropertyRangeAxiom, Object>) HasRange::getRange, HasProperty::getProperty);
    }

    @Test
    public void testDatatypeDefinition() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.DATATYPE_DEFINITION);
        testAxiom(data, OWLDatatypeDefinitionAxiom::getDatatype, OWLDatatypeDefinitionAxiom::getDataRange);
    }

    @Test
    public void testClassAssertion() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.CLASS_ASSERTION);
        testAxiom(data, OWLClassAssertionAxiom::getClassExpression, OWLClassAssertionAxiom::getIndividual);
    }

    @Test
    public void testSWRLRule() {
        List<TestDataCollection.AxiomData> data = CommonAxiomsTest.getAxiomData(AxiomType.SWRL_RULE);
        testAxiom(data
                , SWRLRule::bodyList
                , SWRLRule::headList
                , SWRLRule::containsAnonymousClassExpressions
                , x -> x.body().collect(Collectors.toList())
                , x -> x.head().collect(Collectors.toList())
                , x -> x.variables().collect(Collectors.toList())
                , x -> x.classAtomPredicates().collect(Collectors.toList())
        );
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    private static <X extends OWLAxiom> void testAxiom(List<TestDataCollection.AxiomData> data,
                                                       Function<X, Object>... properties) {
        Assertions.assertFalse(data.isEmpty());
        for (TestDataCollection.AxiomData a : data) {
            LOGGER.debug("Test properties for '{}'", a);
            X owl = (X) a.create(ObjectFactoryTestBase.OWL_DATA_FACTORY);
            X ont = (X) a.create(ObjectFactoryTestBase.ONT_DATA_FACTORY);
            X res = (X) CommonAxiomsTest.createONTObject(TestManagers.createONTManager(), owl);
            Assertions.assertEquals(owl, res);
            for (Function<X, Object> property : properties) {
                Object expected = property.apply(owl);
                Assertions.assertEquals(expected, property.apply(ont));
                Assertions.assertEquals(expected, property.apply(res));
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SafeVarargs
    private static <X extends OWLNaryAxiom> void testSplitNaryAxioms(List<TestDataCollection.AxiomData> data,
                                                                     Function<X, Object>... properties) {
        Assertions.assertFalse(data.isEmpty());
        for (TestDataCollection.AxiomData a : data) {
            LOGGER.debug("Test properties for '{}'", a);
            X base = (X) a.create(ObjectFactoryTestBase.OWL_DATA_FACTORY);
            Collection<X> owlList = ((X) a.create(ObjectFactoryTestBase.OWL_DATA_FACTORY)).asPairwiseAxioms();
            Collection<X> ontList = ((X) a.create(ObjectFactoryTestBase.ONT_DATA_FACTORY)).asPairwiseAxioms();
            Collection<X> resList = (Collection<X>) SplitNaryAxiomsTest.createONTAxioms(TestManagers.createONTManager(), base);
            for (X owl : owlList) {
                X ont = ontList.stream().filter(owl::equals)
                        .findFirst().orElseThrow(AssertionError::new);
                X res = resList.stream().filter(owl::equals)
                        .findFirst().orElseThrow(AssertionError::new);
                for (Function<X, Object> property : properties) {
                    Object expected = property.apply(owl);
                    Assertions.assertEquals(expected, property.apply(ont));
                    Assertions.assertEquals(expected, property.apply(res));
                }
            }
        }
    }
}
