/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
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
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.objects.ModelObject;
import com.github.owlcs.ontapi.tests.TestFactory.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.model.HasAnnotationPropertiesInSignature;
import org.semanticweb.owlapi.model.HasAnonymousIndividuals;
import org.semanticweb.owlapi.model.HasClassesInSignature;
import org.semanticweb.owlapi.model.HasDataPropertiesInSignature;
import org.semanticweb.owlapi.model.HasDatatypesInSignature;
import org.semanticweb.owlapi.model.HasIndividualsInSignature;
import org.semanticweb.owlapi.model.HasObjectPropertiesInSignature;
import org.semanticweb.owlapi.model.HasSignature;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by @ssz on 13.08.2019.
 */
@SuppressWarnings("WeakerAccess")
abstract class ObjectFactoryTestBase {
    static final Logger LOGGER = LoggerFactory.getLogger(ObjectFactoryTestBase.class);

    static final OWLDataFactory ONT_DATA_FACTORY = OntManagers.getDataFactory();
    static final OWLDataFactory OWL_DATA_FACTORY = new OntManagers.OWLAPIImplProfile().createDataFactory();

    private static final String TEST_NS = "http://" + UUID.randomUUID() + "#";

    public static List<? extends Data> getData() {
        throw new UnsupportedOperationException();
    }

    static Stream<? extends OWLObject> components(OWLObject object) {
        return Stream.of(object.signature(), object.nestedClassExpressions(), object.anonymousIndividuals())
                .flatMap(Function.identity());
    }

    static void testObjectHasNoModelReference(OWLObject test) {
        Assertions.assertFalse(test instanceof ONTObject);
        components(test).forEach(x -> Assertions.assertFalse(x instanceof ONTObject));
    }

    static boolean isOWL(OWLObject obj) {
        return isInPackage(obj, "uk.ac.manchester.cs.owl.owlapi");
    }

    static boolean isONT(OWLObject obj) {
        return isInPackage(obj, "com.github.owlcs.ontapi.owlapi");
    }

    private static boolean isInPackage(Object obj, String packageFullName) {
        return obj.getClass().getName().startsWith(packageFullName);
    }

    abstract OWLObject fromModel(Data data);

    @ParameterizedTest
    @MethodSource("getData")
    public void testONTObject(Data data) {
        OWLObject ont = data.create(ONT_DATA_FACTORY);
        OWLObject owl = data.create(OWL_DATA_FACTORY);
        OWLObject test = fromModel(data);
        Assertions.assertTrue(test instanceof ModelObject);
        Assertions.assertTrue(isONT(ont));
        Assertions.assertTrue(isOWL(owl));

        testONTObject(data, owl, ont, test);
    }

    final void testONTObject(Data data, OWLObject sample, OWLObject fromFactory, OWLObject fromModel) {
        testCompare(data, sample, fromModel);
        testCompare(data, fromFactory, fromModel);

        testComponents(data, sample, fromModel);
        testBooleanProperties(data, sample, fromModel);
        testEraseModel(data, sample, fromModel);
        testContent(data, sample, fromModel);
    }

    void testContent(Data data, OWLObject sample, OWLObject test) {
    }

    void testCompare(Data data, OWLObject expected, OWLObject actual) {
        LOGGER.debug("Test compare for '{}'", data);
        data.testCompare(expected, actual);
    }

    void testEraseModel(Data data, OWLObject sample, OWLObject actual) {
        LOGGER.debug("Test erase model for '{}'", data);
        OWLObject factoryObject = ((ModelObject<?>) actual).eraseModel();
        Assertions.assertEquals(sample, factoryObject);
        testObjectHasNoModelReference(factoryObject);
    }

    void testComponents(Data data, OWLObject expected, OWLObject actual) {
        LOGGER.debug("Test signature for '{}'", data);
        validate(expected, actual, "signature", HasSignature::signature);
        validate(expected, actual, "classes", HasClassesInSignature::classesInSignature);
        validate(expected, actual, "datatypes", HasDatatypesInSignature::datatypesInSignature);
        validate(expected, actual, "named individuals", HasIndividualsInSignature::individualsInSignature);
        validate(expected, actual, "anonymous individuals", HasAnonymousIndividuals::anonymousIndividuals);
        validate(expected, actual, "nested class expressions", OWLObject::nestedClassExpressions);
        validate(expected, actual, "annotation properties",
                HasAnnotationPropertiesInSignature::annotationPropertiesInSignature);
        validate(expected, actual, "datatype properties", HasDataPropertiesInSignature::dataPropertiesInSignature);
        validate(expected, actual, "object properties", HasObjectPropertiesInSignature::objectPropertiesInSignature);
    }

    <X extends OWLObject> void validate(X expected,
                                        X actual,
                                        String msg,
                                        Function<X, Stream<? extends OWLObject>> get) {
        List<? extends OWLObject> expectedList = get.apply(expected).collect(Collectors.toList());
        List<? extends OWLObject> actualList = get.apply(actual).collect(Collectors.toList());
        Assertions.assertEquals(expectedList, actualList, "Wrong " + msg + ":");
    }

    void testBooleanProperties(Data data, OWLObject expected, OWLObject actual) {
        LOGGER.debug("Test contains for '{}'", data);
        expected.signature().forEach(x -> Assertions.assertTrue(actual.containsEntityInSignature(x)));
        Assertions.assertFalse(actual.containsEntityInSignature(OWL_DATA_FACTORY.getOWLClass(TEST_NS, "C")));
        Assertions.assertFalse(actual.containsEntityInSignature(OWL_DATA_FACTORY.getOWLDatatype(TEST_NS, "D")));
        Assertions.assertFalse(actual.containsEntityInSignature(OWL_DATA_FACTORY.getOWLNamedIndividual(TEST_NS, "I")));
        Assertions.assertFalse(actual.containsEntityInSignature(OWL_DATA_FACTORY.getOWLObjectProperty(TEST_NS, "P")));
        Assertions.assertFalse(actual.containsEntityInSignature(OWL_DATA_FACTORY.getOWLDataProperty(TEST_NS, "P")));
        Assertions.assertFalse(actual.containsEntityInSignature(OWL_DATA_FACTORY.getOWLAnnotationProperty(TEST_NS, "P")));
    }

}
