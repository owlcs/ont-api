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
import ru.avicomp.ontapi.internal.InternalCache;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.owlapi.OWLObjectImpl;
import ru.avicomp.ontapi.tests.TestFactory;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by @ssz on 13.08.2019.
 */
@SuppressWarnings("WeakerAccess")
abstract class ObjectFactoryTest extends TestFactory {
    private static final OWLDataFactory ONT_DATA_FACTORY = OntManagers.getDataFactory();
    private static final OWLDataFactory OWL_DATA_FACTORY = OntManagers.createOWLProfile().dataFactory();

    private static final String TEST_NS = "http://" + UUID.randomUUID() + "#";

    protected final Data data;

    ObjectFactoryTest(Data data) {
        this.data = data;
    }

    abstract OWLObject fromModel();

    @Test
    public void testCompare() {
        OWLObject ont = data.create(ONT_DATA_FACTORY);
        OWLObject owl = data.create(OWL_DATA_FACTORY);
        OWLObject test = fromModel();
        Assert.assertTrue(test instanceof ONTObject);
        Assert.assertTrue(ont.getClass().getName().startsWith("ru.avicomp.ontapi.owlapi"));
        Assert.assertTrue(owl.getClass().getName().startsWith("uk.ac.manchester.cs.owl.owlapi"));

        compare(owl, test);
        compare(ont, test);
        validate(owl, test);
        testContains(owl, test);

        Class<? extends OWLObjectImpl> frame = getCacheFrameType();
        if (frame != null)
            testInternalReset(frame, owl, test);
    }

    Class<? extends OWLObjectImpl> getCacheFrameType() {
        return null;
    }

    void testInternalReset(Class<? extends OWLObjectImpl> frame, OWLObject expected, OWLObject test) {
        Assert.assertTrue(frame.isInstance(test));
        InternalCache.Loading cache = getContentCache(frame, test);
        Assert.assertFalse(cache.isEmpty());
        cache.clear();
        Assert.assertTrue(cache.isEmpty());
        compare(expected, test);
        Assert.assertFalse(cache.isEmpty());
        validate(expected, test);
    }

    private static InternalCache.Loading getContentCache(Class<? extends OWLObjectImpl> type, OWLObject inst) {

        try {
            Field f = type.getDeclaredField("content");
            f.setAccessible(true);
            return (InternalCache.Loading) f.get(inst);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    void compare(OWLObject expected, OWLObject actual) {
        data.testCompare(expected, actual);
    }

    void validate(OWLObject expected, OWLObject actual) {
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

    void validate(OWLObject expected,
                  OWLObject actual,
                  String msg,
                  Function<OWLObject, Stream<? extends OWLObject>> get) {
        List<? extends OWLObject> expectedList = get.apply(expected).collect(Collectors.toList());
        List<? extends OWLObject> actualList = get.apply(actual).collect(Collectors.toList());
        Assert.assertEquals("Wrong " + msg + ":", expectedList, actualList);
    }

    void testContains(OWLObject expected, OWLObject actual) {
        expected.signature().forEach(x -> Assert.assertTrue(actual.containsEntityInSignature(x)));
        Assert.assertFalse(actual.containsEntityInSignature(OWL_DATA_FACTORY.getOWLClass(TEST_NS, "C")));
        Assert.assertFalse(actual.containsEntityInSignature(OWL_DATA_FACTORY.getOWLDatatype(TEST_NS, "D")));
        Assert.assertFalse(actual.containsEntityInSignature(OWL_DATA_FACTORY.getOWLNamedIndividual(TEST_NS, "I")));
        Assert.assertFalse(actual.containsEntityInSignature(OWL_DATA_FACTORY.getOWLObjectProperty(TEST_NS, "P")));
        Assert.assertFalse(actual.containsEntityInSignature(OWL_DATA_FACTORY.getOWLDataProperty(TEST_NS, "P")));
        Assert.assertFalse(actual.containsEntityInSignature(OWL_DATA_FACTORY.getOWLAnnotationProperty(TEST_NS, "P")));
    }

}
