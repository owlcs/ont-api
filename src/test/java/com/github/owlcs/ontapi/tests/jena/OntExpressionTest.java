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

package com.github.owlcs.ontapi.tests.jena;

import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * To test {@link OntClass class expression}s mostly.
 * <p>
 * Created by @ssz on 08.05.2019.
 */
public class OntExpressionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntExpressionTest.class);

    @Test
    public void testCreateCardinalityRestrictions() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass c = m.createOntClass("C");
        OntObjectProperty op = m.createObjectProperty("OP");
        OntDataProperty dp = m.createDataProperty("DP");

        OntClass.ObjectCardinality r1 = m.createObjectCardinality(op, 12, c);
        OntClass.DataMinCardinality r2 = m.createDataMinCardinality(dp, 1, null);
        OntClass.DataMaxCardinality r3 = m.createDataMaxCardinality(dp, 2, m.getRDFSLiteral());
        OntClass.ObjectMinCardinality r4 = m.createObjectMinCardinality(op, 12, m.getOWLThing());
        OntClass.CardinalityRestrictionCE<?, ?> r5 = m.createDataCardinality(dp, 0, m.getDatatype(XSD.xstring));
        ReadWriteUtils.print(m);

        Assertions.assertTrue(r1.isQualified());
        Assertions.assertFalse(r2.isQualified());
        Assertions.assertFalse(r3.isQualified());
        Assertions.assertFalse(r4.isQualified());
        Assertions.assertTrue(r5.isQualified());
        long size = m.size();

        try {
            m.createObjectMaxCardinality(op, -12, c);
            Assertions.fail("Possible to create restriction with negative cardinality.");
        } catch (OntJenaException.IllegalArgument e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        }
        Assertions.assertEquals(size, m.size());
    }

    @Test
    public void testListClassHierarchy() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass a = m.createOntClass("A");
        OntClass b = m.createOntClass("B");
        OntClass c = m.createOntClass("C");
        OntClass d = m.createOntClass("D");
        OntClass e = m.createOntClass("E");
        e.addSuperClass(d);
        a.addSuperClass(b).addSuperClass(c);
        b.addSuperClass(m.createObjectComplementOf(b)).addSuperClass(d);
        ReadWriteUtils.print(m);

        Assertions.assertEquals(2, a.superClasses(true)
                .peek(x -> LOGGER.debug("{} has direct super class: {}", a, x)).count());
        Assertions.assertEquals(4, a.superClasses(false)
                .peek(x -> LOGGER.debug("{} has super class: {}", a, x)).count());

        Assertions.assertEquals(2, d.subClasses(true)
                .peek(x -> LOGGER.debug("{} has direct sub class: {}", d, x)).count());
        Assertions.assertEquals(3, d.subClasses(false)
                .peek(x -> LOGGER.debug("{} has sub class: {}", d, x)).count());
    }

    @Test
    public void testClassExpressionSubClassOf() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass a = m.createOntClass("A");
        OntClass b = m.createOntClass("B");
        OntClass c = m.createOntClass("C");
        Assertions.assertNotNull(a.addSubClassOfStatement(b));
        Assertions.assertSame(a, a.addSuperClass(c).addSuperClass(m.getOWLThing()).removeSuperClass(b));
        Assertions.assertEquals(2, a.superClasses().count());
        Assertions.assertSame(a, a.removeSuperClass(null));
        Assertions.assertEquals(3, m.size());
    }

    @Test
    public void testClassExpressionDisjointWith() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass a = m.createOntClass("A");
        OntClass b = m.createOntClass("B");
        OntClass c = m.createOntClass("C");
        Assertions.assertNotNull(a.addDisjointWithStatement(b));
        Assertions.assertSame(a, a.addDisjointClass(c).addDisjointClass(m.getOWLThing()).removeDisjointClass(b));
        Assertions.assertEquals(2, a.disjointClasses().count());
        Assertions.assertSame(a, a.removeDisjointClass(null));
        Assertions.assertEquals(3, m.size());
    }

    @Test
    public void testClassExpressionEquivalentClass() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass.Named a = m.createOntClass("A");
        OntClass.Named b = m.createOntClass("B");
        OntClass.Named c = m.createOntClass("C");
        Assertions.assertNotNull(a.addEquivalentClassStatement(b));
        Assertions.assertSame(a, a.addEquivalentClass(c).addEquivalentClass(m.getOWLThing()).removeEquivalentClass(b));
        Assertions.assertEquals(2, a.equivalentClasses().count());
        Assertions.assertSame(a, a.removeEquivalentClass(null));
        Assertions.assertEquals(3, m.size());
    }

    @Test
    public void testHasKeys() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty o1 = m.createObjectProperty("O1");
        OntObjectProperty o2 = m.createObjectProperty("O2");
        OntDataProperty d1 = m.createDataProperty("D1");
        OntDataProperty d2 = m.createDataProperty("D2");
        OntClass.Named c = m.getOWLThing();
        Assertions.assertNotNull(c.addHasKeyStatement());
        Assertions.assertSame(c, c.addHasKey());
        Assertions.assertEquals(1, c.hasKeys().count());

        Assertions.assertEquals(0, c.fromHasKey().count());
        Assertions.assertSame(c, c.addHasKey(o1, d1).addHasKey(Arrays.asList(o1, o2), Collections.singletonList(d2)));
        Assertions.assertEquals(3, c.hasKeys().count());
        Assertions.assertEquals(4, c.fromHasKey().count());
        Assertions.assertSame(c, c.clearHasKeys());
        Assertions.assertEquals(4, m.size());
    }

    @Test
    public void testComponentRestrictionValues() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty po1 = m.createObjectProperty("PO1");
        OntObjectProperty po2 = m.createObjectProperty("PO2");
        OntDataProperty pd1 = m.createDataProperty("PD1");
        OntDataProperty pd2 = m.createDataProperty("PD2");
        OntDataRange.Named dt1 = m.createDatatype("DT1");
        OntDataRange.Named dt2 = m.createDatatype("DT2");
        OntClass.Named c1 = m.createOntClass("C1");
        OntClass.Named c2 = m.createOntClass("C2");
        OntIndividual i1 = c1.createIndividual();
        OntIndividual i2 = c2.createIndividual("I2");
        Literal l1 = dt1.createLiteral("L1");
        Literal l2 = dt1.createLiteral("L2");

        OntClass.DataSomeValuesFrom r1 = m.createDataSomeValuesFrom(pd1, dt1);
        Assertions.assertEquals(dt1, r1.getValue());
        Assertions.assertSame(r1, r1.setValue(dt2));
        Assertions.assertEquals(dt2, r1.getValue());

        OntClass.ObjectMinCardinality r2 = m.createObjectMinCardinality(po1, 1, c1);
        Assertions.assertEquals(c1, r2.getValue());
        Assertions.assertSame(r2, r2.setValue(c2));
        Assertions.assertEquals(c2, r2.getValue());

        OntClass.ObjectHasValue r3 = m.createObjectHasValue(po2, i1);
        Assertions.assertEquals(i1, r3.getValue());
        Assertions.assertSame(r3, r3.setValue(i2));
        Assertions.assertEquals(i2, r3.getValue());

        OntClass.DataHasValue r4 = m.createDataHasValue(pd2, l1);
        Assertions.assertEquals(l1, r4.getValue());
        Assertions.assertSame(r4, r4.setValue(l2));
        Assertions.assertEquals(l2, r4.getValue());

        Set<RDFNode> expected = new HashSet<>(Arrays.asList(dt2, c2, i2, l2));
        Set<RDFNode> actual = m.ontObjects(OntClass.ComponentRestrictionCE.class)
                .map(x -> x.getValue()).collect(Collectors.toSet());
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testExpressionSetWrongComponents() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataRange.Named dt1 = m.createDatatype("DT1");
        OntDataRange.Named dt2 = m.createDatatype("DT2");
        OntDataRange.Named dt3 = m.createDatatype("DT3");
        OntDataRange.Named dt4 = m.createDatatype("DT4");

        OntDataRange.UnionOf u = m.createDataUnionOf(dt1, dt2, dt3);
        try {
            u.setComponents(u, dt4);
            Assertions.fail("Possible to set itself inside a []-list");
        } catch (OntJenaException e) {
            LOGGER.debug("Expected: {}", e.getMessage());
        }
        Assertions.assertEquals(3, u.getList().size());
    }

    @Test
    public void testClassExpressionComponents() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass c1 = m.createOntClass("C1");
        OntClass c2 = m.createOntClass("C2");
        OntClass c3 = m.createOntClass("C3");
        OntClass c4 = m.createOntClass("C4");
        OntIndividual i1 = c1.createIndividual();
        OntIndividual i2 = c2.createIndividual("I2");
        OntIndividual i3 = c1.createIndividual();
        OntIndividual i4 = c4.createIndividual("I4");

        List<OntIndividual> list1 = Arrays.asList(i1, i2, i3);
        OntClass.OneOf e1 = m.createObjectOneOf(list1);
        Assertions.assertEquals(list1, e1.getList().members().collect(Collectors.toList()));
        Assertions.assertSame(e1, e1.setComponents(i1, i4));
        Assertions.assertEquals(Arrays.asList(i1, i4), e1.getList().members().collect(Collectors.toList()));

        List<OntClass> list2 = Arrays.asList(c3, c4);
        OntClass.UnionOf e2 = m.createObjectUnionOf(list2);
        Assertions.assertEquals(2, e2.getList().members().count());
        Assertions.assertTrue(e2.setComponents().getList().isEmpty());

        OntClass.IntersectionOf e3 = m.createObjectIntersectionOf(list2);
        Assertions.assertEquals(3, e3.setComponents(Arrays.asList(c1, c2, m.getOWLThing())).getList().members().count());

        Set<RDFNode> expected = new HashSet<>(Arrays.asList(i1, i4, c1, c2, m.getOWLThing()));
        Set<RDFNode> actual = m.ontObjects(OntClass.ComponentsCE.class)
                .map(x -> x.getList())
                .map(x -> x.as(RDFList.class))
                .map(RDFList::asJavaList)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testRestrictionOnProperties() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass.Named c1 = m.createOntClass("C1");
        OntDataRange.Named dt1 = m.createDatatype("DT1");
        OntDataProperty dp1 = m.createDataProperty("DP1");
        OntDataProperty dp2 = m.createDataProperty("DP2");
        OntObjectProperty.Named op1 = m.createObjectProperty("OP1");
        OntObjectProperty op2 = m.createObjectProperty("OP2");

        OntClass.DataAllValuesFrom r1 = m.createDataAllValuesFrom(dp1, dt1);
        Assertions.assertEquals(dp1, r1.getProperty());
        Assertions.assertSame(r1, r1.setProperty(dp2));
        Assertions.assertEquals(dp2, r1.getProperty());

        OntClass.ObjectMaxCardinality r2 = m.createObjectMaxCardinality(op1, 2, c1);
        Assertions.assertEquals(op1, r2.getProperty());
        Assertions.assertSame(r2, r2.setProperty(op2));
        Assertions.assertEquals(op2, r2.getProperty());

        OntClass.HasSelf r3 = m.createHasSelf(op2);
        Assertions.assertEquals(op2, r3.getProperty());
        Assertions.assertSame(r3, r3.setProperty(op1));
        Assertions.assertEquals(op1, r3.getProperty());

        Set<OntRealProperty> actual = new HashSet<>(Arrays.asList(dp2, op2, op1));
        Set<OntRealProperty> expected = m.ontObjects(OntClass.UnaryRestrictionCE.class)
                .map(x -> x.getProperty()).collect(Collectors.toSet());
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testRestrictionCardinality() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataProperty dp1 = m.createDataProperty("DP1");
        OntObjectProperty op2 = m.createObjectProperty("OP2");

        OntClass.DataMinCardinality r1 = m.createDataMinCardinality(dp1, 5, null);
        Assertions.assertEquals(5, r1.getCardinality());
        Assertions.assertSame(r1, r1.setCardinality(6));
        Assertions.assertEquals(6, r1.getCardinality());

        OntClass.ObjectCardinality r2 = m.createObjectCardinality(op2, 2, m.createOntClass("C1"));
        Assertions.assertEquals(2, r2.getCardinality());
        Assertions.assertSame(r2, r2.setCardinality(3));
        Assertions.assertEquals(3, r2.getCardinality());

        long expected = 6 + 3;
        long actual = m.ontObjects(OntClass.CardinalityRestrictionCE.class).mapToLong(x -> x.getCardinality()).sum();
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testChangeCardinalityQualification() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataProperty dp1 = m.createDataProperty("DP1");
        OntObjectProperty.Named op2 = m.createObjectProperty("OP2");
        OntClass.Named c1 = m.createOntClass("C1");
        OntDataRange.Named d1 = m.getDatatype(XSD.xstring);
        Literal v = m.getDatatype(XSD.nonNegativeInteger).createLiteral(2);

        OntClass.DataCardinality r1 = m.createDataCardinality(dp1, v.getInt(), d1);
        Assertions.assertEquals(d1, r1.getValue());
        Assertions.assertFalse(m.containsResource(OWL.cardinality));
        Assertions.assertTrue(m.contains(null, OWL.qualifiedCardinality, v));
        Assertions.assertSame(r1, r1.setValue(null));
        Assertions.assertEquals(RDFS.Literal, r1.getValue());
        Assertions.assertFalse(m.containsResource(OWL.qualifiedCardinality));
        Assertions.assertTrue(m.contains(null, OWL.cardinality, v));

        OntClass.ObjectMinCardinality r2 = m.createObjectMinCardinality(op2, v.getInt(), null);
        Assertions.assertEquals(OWL.Thing, r2.getValue());
        Assertions.assertFalse(m.containsResource(OWL.minQualifiedCardinality));
        Assertions.assertTrue(m.contains(null, OWL.minCardinality, v));
        Assertions.assertEquals(c1, r2.setValue(c1).getValue());
        Assertions.assertFalse(m.containsResource(OWL.minCardinality));
        Assertions.assertTrue(m.contains(null, OWL.minQualifiedCardinality, v));
    }

    @Test
    public void testNaryDataRestrictions() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataProperty dp1 = m.createDataProperty("DP1");
        OntDataProperty dp2 = m.createDataProperty("DP2");
        OntDataRange.Named d1 = m.getDatatype(XSD.xstring);
        OntDataRange d2 = m.createDatatype("x");

        OntClass.NaryDataAllValuesFrom r1 = m.createDataAllValuesFrom(Collections.singleton(dp1), d1);
        long s = m.size();
        Assertions.assertSame(r1, r1.setValue(d2));
        Assertions.assertEquals(d2, r1.getValue());
        Assertions.assertEquals(dp1, r1.getProperty());
        Assertions.assertEquals(s, m.size());
        Assertions.assertFalse(m.contains(null, OWL.someValuesFrom, (RDFNode) null));
        Assertions.assertTrue(m.contains(null, OWL.allValuesFrom, (RDFNode) null));

        try {
            m.createDataAllValuesFrom(Arrays.asList(dp1, dp2), d1);
            Assertions.fail("Possible to create wrong n-ary restriction");
        } catch (OntJenaException e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        }
        Assertions.assertEquals(s, m.size());

        OntClass.NaryDataSomeValuesFrom r2 = m.createDataSomeValuesFrom(Collections.singleton(dp2), d1);
        Assertions.assertEquals(s = s + 5, m.size());
        Assertions.assertTrue(m.contains(null, OWL.someValuesFrom, (RDFNode) null));
        Assertions.assertTrue(m.contains(null, OWL.allValuesFrom, (RDFNode) null));

        try {
            r2.setComponents(dp1, dp2);
            Assertions.fail("Possible to set more than one properties");
        } catch (OntJenaException e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        }
        Assertions.assertEquals(s, m.size());
        Assertions.assertEquals(dp2, r2.getProperty());
        Assertions.assertEquals(dp1, r2.setProperty(dp1).getProperty());
    }

    @Test
    public void testDisjointUnion() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass.Named c1 = m.createOntClass("C1");
        OntClass.Named c2 = m.createOntClass("C2");
        OntClass.Named c3 = m.createOntClass("C3");
        OntClass c4 = m.createObjectComplementOf(c3);
        long s = m.size();
        OntClass.Named c0 = m.getOWLThing();
        Assertions.assertNotNull(c0.addDisjointUnionOfStatement());
        Assertions.assertSame(c0, c0.addDisjointUnion());
        Assertions.assertEquals(1, c0.disjointUnions().count());

        Assertions.assertEquals(0, c0.fromDisjointUnionOf().count());
        Assertions.assertSame(c0, c0.addDisjointUnion(c1, c3).addDisjointUnion(Arrays.asList(c1, c2, c4)));
        Assertions.assertEquals(3, c0.disjointUnions().count());
        Assertions.assertEquals(4, c0.fromDisjointUnionOf().count());

        Assertions.assertSame(c0, c0.removeDisjointUnion(RDF.nil));
        Assertions.assertEquals(2, c0.disjointUnions().count());
        Assertions.assertEquals(4, c0.fromDisjointUnionOf().count());

        Assertions.assertSame(c0, c0.clearDisjointUnions());
        Assertions.assertEquals(s, m.size());
    }
}
