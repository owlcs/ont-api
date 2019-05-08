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

package ru.avicomp.ontapi.tests.jena;

import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.XSD;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.Arrays;
import java.util.Collections;

/**
 * To test class and data-range expressions: {@link OntCE}, {@link OntDR} and all their descendants.
 * <p>
 * Created by @ssz on 08.05.2019.
 */
public class OntExpressionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntExpressionTest.class);

    @Test
    public void testCreateExpressions() {
        String uri = "http://test.com/graph/3";
        String ns = uri + "#";

        OntGraphModel m = OntModelFactory.createModel()
                .setNsPrefix("test", ns)
                .setNsPrefixes(OntModelFactory.STANDARD)
                .setID(uri)
                .getModel();

        OntNDP ndp1 = m.createDataProperty(ns + "dataProperty1");
        OntDT dt1 = m.createOntEntity(OntDT.class, ns + "dataType1");
        dt1.addEquivalentClass(m.getDatatype(XSD.dateTime));

        OntDT dt2 = m.createOntEntity(OntDT.class, ns + "dataType2");

        OntFR fr1 = m.createFacetRestriction(OntFR.MaxExclusive.class, ResourceFactory.createTypedLiteral(12));
        OntFR fr2 = m.createFacetRestriction(OntFR.LangRange.class, ResourceFactory.createTypedLiteral("\\d+"));

        OntDR dr1 = m.createRestrictionDataRange(dt1, Arrays.asList(fr1, fr2));

        OntCE ce1 = m.createDataSomeValuesFrom(ndp1, dr1);

        OntDR dr2 = m.createIntersectionOfDataRange(Arrays.asList(dt1, dt2));
        OntIndividual i1 = ce1.createIndividual(ns + "individual1");
        OntCE ce2 = m.createDataMaxCardinality(ndp1, 343434, dr2);
        i1.attachClass(ce2);
        i1.attachClass(m.createOntClass(ns + "Class1"));

        OntIndividual i2 = ce2.createIndividual();
        i2.addStatement(ndp1, ResourceFactory.createPlainLiteral("individual value"));

        ReadWriteUtils.print(m);
        Assert.assertEquals("Incorrect count of individuals", 2, m.ontObjects(OntIndividual.class).count());
        Assert.assertEquals("Incorrect count of class expressions", 3, m.ontObjects(OntCE.class).count());
        Assert.assertEquals("Incorrect count of restrictions", 2, m.ontObjects(OntCE.RestrictionCE.class).count());
        Assert.assertEquals("Incorrect count of cardinality restrictions", 1,
                m.ontObjects(OntCE.CardinalityRestrictionCE.class).count());
        Assert.assertEquals("Incorrect count of datatype entities", 2, m.ontObjects(OntDT.class).count());
        Assert.assertEquals("Incorrect count of data properties", 1, m.ontObjects(OntNDP.class).count());
        Assert.assertEquals("Incorrect count of facet restrictions", 2, m.ontObjects(OntFR.class).count());
        Assert.assertEquals("Incorrect count of data ranges", 4, m.ontObjects(OntDR.class).count());
        Assert.assertEquals("Incorrect count of entities", 5, m.ontObjects(OntEntity.class).count());
    }

    @Test
    public void testDataRanges() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntFR f1 = m.createFacetRestriction(OntFR.MaxExclusive.class, m.createTypedLiteral(12));
        OntFR f2 = m.createFacetRestriction(OntFR.Pattern.class, m.createTypedLiteral("\\d+"));
        OntFR f3 = m.createFacetRestriction(OntFR.LangRange.class, m.createTypedLiteral("^r.*"));

        OntDT d1 = m.getDatatype(XSD.xstring);
        OntDR d2 = m.createComplementOfDataRange(d1);
        OntDR d3 = m.createRestrictionDataRange(d1, Arrays.asList(f1, f2, f3));

        ReadWriteUtils.print(m);
        Assert.assertEquals(3, m.ontObjects(OntFR.class).count());
        Assert.assertEquals(2, m.ontObjects(OntDR.class).count());
        Assert.assertEquals(1, m.ontObjects(OntDR.ComponentsDR.class).count());
        Assert.assertEquals(d2, m.ontObjects(OntDR.class).filter(s -> s.canAs(OntDR.ComplementOf.class))
                .findFirst().orElseThrow(AssertionError::new));
        Assert.assertEquals(d3, m.ontObjects(OntDR.class).filter(s -> s.canAs(OntDR.Restriction.class))
                .findFirst().orElseThrow(AssertionError::new));

        Assert.assertEquals(XSD.xstring, d3.as(OntDR.Restriction.class).getDatatype());
        Assert.assertEquals(12, d3.spec().peek(s -> LOGGER.debug("{}", Models.toString(s))).count());
    }

    @Test
    public void testCreateCardinalityRestrictions() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass c = m.createOntClass("C");
        OntNOP op = m.createObjectProperty("OP");
        OntNDP dp = m.createDataProperty("DP");

        OntCE.ObjectCardinality r1 = m.createObjectCardinality(op, 12, c);
        OntCE.DataMinCardinality r2 = m.createDataMinCardinality(dp, 1, null);
        OntCE.DataMaxCardinality r3 = m.createDataMaxCardinality(dp, 2, m.getRDFSLiteral());
        OntCE.ObjectMinCardinality r4 = m.createObjectMinCardinality(op, 12, m.getOWLThing());
        OntCE.CardinalityRestrictionCE r5 = m.createDataCardinality(dp, 0, m.getDatatype(XSD.xstring));
        ReadWriteUtils.print(m);

        Assert.assertTrue(r1.isQualified());
        Assert.assertFalse(r2.isQualified());
        Assert.assertFalse(r3.isQualified());
        Assert.assertFalse(r4.isQualified());
        Assert.assertTrue(r5.isQualified());
        long size = m.size();

        try {
            m.createObjectMaxCardinality(op, -12, c);
            Assert.fail("Possible to create restriction with negative cardinality.");
        } catch (OntJenaException.IllegalArgument e) {
            LOGGER.debug("Expected: '{}'", e.getMessage());
        }
        Assert.assertEquals(size, m.size());
    }

    @Test
    public void testListClassHierarchy() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass a = m.createOntClass("A");
        OntClass b = m.createOntClass("B");
        OntClass c = m.createOntClass("C");
        OntClass d = m.createOntClass("D");
        OntClass e = m.createOntClass("E");
        e.addSuperClass(d);
        a.addSuperClass(b).addSuperClass(c);
        b.addSuperClass(m.createComplementOf(b)).addSuperClass(d);
        ReadWriteUtils.print(m);

        Assert.assertEquals(2, a.listSuperClasses(true)
                .peek(x -> LOGGER.debug("{} has direct super class: {}", a, x)).count());
        Assert.assertEquals(4, a.listSuperClasses(false)
                .peek(x -> LOGGER.debug("{} has super class: {}", a, x)).count());

        Assert.assertEquals(2, d.listSubClasses(true)
                .peek(x -> LOGGER.debug("{} has direct sub class: {}", d, x)).count());
        Assert.assertEquals(3, d.listSubClasses(false)
                .peek(x -> LOGGER.debug("{} has sub class: {}", d, x)).count());
    }

    @Test
    public void testClassExpressionSubClassOf() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass a = m.createOntClass("A");
        OntClass b = m.createOntClass("B");
        OntClass c = m.createOntClass("C");
        Assert.assertNotNull(a.addSubClassOfStatement(b));
        Assert.assertSame(a, a.addSuperClass(c).addSuperClass(m.getOWLThing()).removeSuperClass(b));
        Assert.assertEquals(2, a.subClassOf().count());
        Assert.assertSame(a, a.removeSuperClass(null));
        Assert.assertEquals(3, m.size());
    }

    @Test
    public void testClassExpressionDisjointWith() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass a = m.createOntClass("A");
        OntClass b = m.createOntClass("B");
        OntClass c = m.createOntClass("C");
        Assert.assertNotNull(a.addDisjointWithStatement(b));
        Assert.assertSame(a, a.addDisjointClass(c).addDisjointClass(m.getOWLThing()).removeDisjointClass(b));
        Assert.assertEquals(2, a.disjointWith().count());
        Assert.assertSame(a, a.removeDisjointClass(null));
        Assert.assertEquals(3, m.size());
    }

    @Test
    public void testClassExpressionEquivalentClass() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass a = m.createOntClass("A");
        OntClass b = m.createOntClass("B");
        OntClass c = m.createOntClass("C");
        Assert.assertNotNull(a.addEquivalentClassStatement(b));
        Assert.assertSame(a, a.addEquivalentClass(c).addEquivalentClass(m.getOWLThing()).removeEquivalentClass(b));
        Assert.assertEquals(2, a.equivalentClass().count());
        Assert.assertSame(a, a.removeEquivalentClass(null));
        Assert.assertEquals(3, m.size());
    }

    @Test
    public void testDatatypeEquivalentClass() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDT a = m.createDatatype("A");
        OntDT b = m.createDatatype("B");
        OntDT c = m.createDatatype("C");
        Assert.assertNotNull(a.addEquivalentClassStatement(b));
        Assert.assertSame(a, a.addEquivalentClass(c).addEquivalentClass(m.getRDFSLiteral()).removeEquivalentClass(b));
        Assert.assertEquals(2, a.equivalentClass().count());
        Assert.assertSame(a, a.removeEquivalentClass(null));
        Assert.assertEquals(3, m.size());
    }

    @Test
    public void testHasKeys() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntNOP o1 = m.createObjectProperty("O1");
        OntNOP o2 = m.createObjectProperty("O2");
        OntNDP d1 = m.createDataProperty("D1");
        OntNDP d2 = m.createDataProperty("D2");
        OntClass c = m.getOWLThing();
        Assert.assertNotNull(c.addHasKeyStatement());
        Assert.assertSame(c, c.addHasKey());
        Assert.assertEquals(1, c.listHasKeys().count());

        Assert.assertEquals(0, c.fromHasKey().count());
        Assert.assertSame(c, c.addHasKey(o1, d1).addHasKey(Arrays.asList(o1, o2), Collections.singletonList(d2)));
        Assert.assertEquals(3, c.listHasKeys().count());
        Assert.assertEquals(4, c.fromHasKey().count());
        Assert.assertSame(c, c.clearHasKeys());
        Assert.assertEquals(4, m.size());
    }
}
