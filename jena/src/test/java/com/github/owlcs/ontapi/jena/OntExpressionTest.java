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

package com.github.owlcs.ontapi.jena;

import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * To test {@link OntClass class expression}s mostly.
 * <p>
 * Created by @ssz on 08.05.2019.
 */
public class OntExpressionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntExpressionTest.class);

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
