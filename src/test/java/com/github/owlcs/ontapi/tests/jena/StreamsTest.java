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

package com.github.owlcs.ontapi.tests.jena;

import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.UnionGraph;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by @ssz on 10.07.2019.
 */
public class StreamsTest {

    private static final Map<Integer, String> SPLITERATOR_CONSTANTS = getSpliteratorConstants();

    private static void assertTrueConstant(Stream<?> s, int expected) {
        int actual = getCharacteristics(s);
        Assert.assertTrue("Expected: " + SPLITERATOR_CONSTANTS.get(expected) + ", but found: " + actual,
                hasCharacteristics(actual, expected));
    }

    private static void assertFalseConstant(Stream<?> s, int expected) {
        int actual = getCharacteristics(s);
        Assert.assertFalse("Stream should not have " + SPLITERATOR_CONSTANTS.get(expected),
                hasCharacteristics(actual, expected));
    }

    private static int getCharacteristics(Stream<?> s) {
        return s.spliterator().characteristics();
    }

    private static boolean hasCharacteristics(int actual, int expected) {
        return (actual & expected) == expected;
    }

    private static Map<Integer, String> getSpliteratorConstants() {
        return directFields(Spliterator.class, int.class)
                .collect(Collectors.toMap(f -> getValue(f, Integer.class), Field::getName));
    }

    @SuppressWarnings("SameParameterValue")
    private static Stream<Field> directFields(Class<?> vocabulary, Class<?> type) {
        return Arrays.stream(vocabulary.getDeclaredFields()).
                filter(field -> Modifier.isPublic(field.getModifiers())).
                filter(field -> Modifier.isStatic(field.getModifiers())).
                filter(field -> type.equals(field.getType()));
    }

    @SuppressWarnings("SameParameterValue")
    private static <T> T getValue(Field field, Class<T> type) {
        try {
            return type.cast(field.get(null));
        } catch (IllegalAccessException e) {
            throw new OntJenaException(e);
        }
    }

    @Test
    public void testSetBasedMethods() {
        OntGraphModel m = OntModelFactory.createModel();
        OntClass a = m.createOntClass("C1");
        OntIndividual i = a.addSuperClass(m.createOntClass("C2").addSuperClass(m.getOWLThing()))
                .createIndividual("I");
        OntNDP p = m.createDataProperty("D1")
                .addSuperProperty(m.createDataProperty("D2").addSuperProperty(m.getOWLBottomDataProperty()));

        Supplier<Stream<?>> s1 = () -> a.superClasses(false);
        Supplier<Stream<?>> s2 = () -> a.subClasses(false);
        Supplier<Stream<?>> s3 = () -> a.subClasses(true);
        Supplier<Stream<?>> s4 = () -> a.superClasses(true);

        Supplier<Stream<?>> s5 = () -> i.classes(false);
        Supplier<Stream<?>> s6 = () -> i.classes(true);

        Supplier<Stream<?>> s7 = () -> p.superProperties(false);
        Supplier<Stream<?>> s8 = () -> p.subProperties(false);
        Supplier<Stream<?>> s9 = () -> p.superProperties(true);
        Supplier<Stream<?>> s10 = () -> p.subProperties(true);

        Supplier<Stream<?>> s11 = p::content;

        Stream.of(s1, s2, s3, s4, s5, s6, s7, s8, s9, s10, s11).forEach(s -> {
            assertTrueConstant(s.get(), Spliterator.NONNULL);
            assertTrueConstant(s.get(), Spliterator.DISTINCT);
            assertTrueConstant(s.get(), Spliterator.IMMUTABLE);
        });
    }

    @Test
    public void testObjectsMethods() {
        OntGraphModel m = OntModelFactory.createModel();
        OntObject o = m.getOWLThing();

        Supplier<Stream<?>> s1 = () -> o.objects(RDFS.comment, OntNAP.class);
        Supplier<Stream<?>> s2 = () -> o.objects(RDFS.comment);
        Supplier<Stream<?>> s3 = o::spec;
        Supplier<Stream<?>> s4 = o::annotations;
        Supplier<Stream<?>> s5 = o::statements;
        Supplier<Stream<?>> s6 = () -> o.statements(RDFS.seeAlso);

        Stream.of(s1, s2, s3, s4, s5, s6).forEach(s -> {
            assertTrueConstant(s.get(), Spliterator.NONNULL);
            assertTrueConstant(s.get(), Spliterator.DISTINCT);
            assertFalseConstant(s.get(), Spliterator.IMMUTABLE);
        });
    }

    @Test
    public void testSimpleModelStreams() {
        OntGraphModel m = OntModelFactory.createModel();

        assertTrueConstant(m.statements(), Spliterator.NONNULL);
        assertTrueConstant(m.statements(), Spliterator.SIZED);
        assertTrueConstant(m.statements(), Spliterator.DISTINCT);
        assertFalseConstant(m.statements(), Spliterator.IMMUTABLE);
        assertFalseConstant(m.statements(), Spliterator.ORDERED);

        assertTrueConstant(m.localStatements(), Spliterator.NONNULL);
        assertTrueConstant(m.localStatements(), Spliterator.SIZED);
        assertTrueConstant(m.localStatements(), Spliterator.DISTINCT);
        assertFalseConstant(m.localStatements(), Spliterator.IMMUTABLE);
        assertFalseConstant(m.localStatements(), Spliterator.ORDERED);

        Supplier<Stream<?>> s1 = () -> m.statements(null, RDF.type, OWL.Class);
        Supplier<Stream<?>> s2 = () -> m.localStatements(null, RDF.type, OWL.Class);
        Stream.of(s1, s2).forEach(s -> {
            assertTrueConstant(s.get(), Spliterator.NONNULL);
            assertFalseConstant(s.get(), Spliterator.SIZED);
            assertTrueConstant(s.get(), Spliterator.DISTINCT);
            assertFalseConstant(s.get(), Spliterator.IMMUTABLE);
            assertFalseConstant(s.get(), Spliterator.ORDERED);
        });
    }

    @Test
    public void testNonSizedModelStreams() {
        OntGraphModel m = OntModelFactory.createModel().addImport(OntModelFactory.createModel().setID("base").getModel());
        assertTrueConstant(m.localStatements(), Spliterator.SIZED);
        assertFalseConstant(m.statements(), Spliterator.SIZED);
        assertFalseConstant(m.statements(null, RDF.type, OWL.Class), Spliterator.SIZED);
        assertFalseConstant(m.localStatements(null, RDF.type, OWL.Class), Spliterator.SIZED);
    }

    @Test
    public void testNonDistinctModelStreams() {
        String ns = "http://ex#";
        UnionGraph g = new UnionGraph(new GraphMem(), null, null, false);
        OntGraphModel a = OntModelFactory.createModel(g).setNsPrefixes(OntModelFactory.STANDARD).setNsPrefix("x", ns);
        OntGraphModel b = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD).setNsPrefix("x", ns);
        a.setID(ns + "a");
        b.setID(ns + "b");
        a.addImport(b);
        b.createOntClass(a.createOntClass(ns + "C").getURI());

        assertTrueConstant(a.localStatements(), Spliterator.DISTINCT);
        assertFalseConstant(a.statements(), Spliterator.DISTINCT);
        assertFalseConstant(a.statements(null, RDF.type, OWL.Class), Spliterator.DISTINCT);
        assertTrueConstant(a.localStatements(null, RDF.type, OWL.Class), Spliterator.DISTINCT);

        assertFalseConstant(a.ontObjects(OntClass.class), Spliterator.DISTINCT);

        Assert.assertEquals(2, a.classes().count());
        Assert.assertEquals(2, a.ontObjects(OntClass.class).count());
        Assert.assertEquals(2, a.ontEntities().count());
        Assert.assertEquals(2, a.statements(null, RDF.type, OWL.Class).count());

        Assert.assertEquals(1, a.classes().distinct().count());
        Assert.assertEquals(1, a.ontObjects(OntClass.class).distinct().count());
        Assert.assertEquals(1, a.ontEntities().distinct().count());
        Assert.assertEquals(1, a.statements(null, RDF.type, OWL.Class).distinct().count());
    }
}
