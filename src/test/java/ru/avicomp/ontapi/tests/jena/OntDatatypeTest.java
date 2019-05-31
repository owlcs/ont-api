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

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.model.OntDR;
import ru.avicomp.ontapi.jena.model.OntDT;
import ru.avicomp.ontapi.jena.model.OntFR;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.XSD;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * All test-cases related to {@link OntDR}.
 * <p>
 * Created by @ssz on 11.05.2019.
 */
public class OntDatatypeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntDatatypeTest.class);

    @Test
    public void testListDataRanges() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntFR f1 = m.createFacetRestriction(OntFR.MaxExclusive.class, m.createTypedLiteral(12));
        OntFR f2 = m.createFacetRestriction(OntFR.Pattern.class, m.createTypedLiteral("\\d+"));
        OntFR f3 = m.createFacetRestriction(OntFR.LangRange.class, m.createTypedLiteral("^r.*"));

        OntDT d1 = m.getDatatype(XSD.xstring);
        OntDR d2 = m.createComplementOfDataRange(d1);
        OntDR d3 = m.createRestrictionDataRange(d1, f1, f2, f3);

        ReadWriteUtils.print(m);
        Assert.assertEquals(3, m.ontObjects(OntFR.class).count());
        Assert.assertEquals(2, m.ontObjects(OntDR.class).count());
        Assert.assertEquals(1, m.ontObjects(OntDR.ComponentsDR.class).count());
        Assert.assertEquals(d2, m.ontObjects(OntDR.class).filter(s -> s.canAs(OntDR.ComplementOf.class))
                .findFirst().orElseThrow(AssertionError::new));
        Assert.assertEquals(d3, m.ontObjects(OntDR.class).filter(s -> s.canAs(OntDR.Restriction.class))
                .findFirst().orElseThrow(AssertionError::new));

        Assert.assertEquals(XSD.xstring, d3.as(OntDR.Restriction.class).getValue());
        Assert.assertEquals(12, d3.spec().peek(s -> LOGGER.debug("{}", Models.toString(s))).count());
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
    public void testDataRangeComponents() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDT dt1 = m.createDatatype("DT1");
        OntDT dt2 = m.createDatatype("DT2");
        OntDT dt3 = m.createDatatype("DT3");
        OntDT dt4 = m.createDatatype("DT4");
        Literal l1 = dt1.createLiteral("L1");
        Literal l2 = dt1.createLiteral("L2");
        Literal l3 = m.createTypedLiteral(3);
        Literal l4 = m.createTypedLiteral(4);
        OntFR fr1 = m.createFacetRestriction(OntFR.MaxExclusive.class, l3);
        OntFR fr2 = m.createFacetRestriction(OntFR.MaxInclusive.class, l4);
        OntFR fr3 = m.createFacetRestriction(OntFR.TotalDigits.class, l4);

        List<Literal> list1 = Arrays.asList(l1, l2);
        OntDR.OneOf dr1 = m.createOneOfDataRange(list1);
        Assert.assertEquals(list1, dr1.getList().members().collect(Collectors.toList()));
        Assert.assertSame(dr1, dr1.setComponents(l2, l3));
        Assert.assertEquals(Arrays.asList(l2, l3), dr1.getList().members().collect(Collectors.toList()));

        OntDR.IntersectionOf dr2 = m.createIntersectionOfDataRange(dt2, dt3, dt4);
        Assert.assertEquals(3, dr2.getList().members().count());
        Assert.assertTrue(dr2.setComponents().getList().isEmpty());

        OntDR.Restriction dr3 = m.createRestrictionDataRange(dt3, fr1, fr2);
        Assert.assertEquals(3, dr3.setComponents(Arrays.asList(fr3, fr1, fr2)).getList().members().count());

        ReadWriteUtils.print(m);

        Set<RDFNode> expected = new HashSet<>(Arrays.asList(l2, l3, fr3, fr1, fr2));
        Set<RDFNode> actual = m.ontObjects(OntDR.ComponentsDR.class)
                .map(x -> x.getList())
                .map(x -> x.as(RDFList.class))
                .map(RDFList::asJavaList)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testDataRangeValues() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDT d1 = m.getDatatype(XSD.xstring);
        OntDT d2 = m.createDatatype("x");

        OntDR.ComplementOf dr1 = m.createComplementOfDataRange(d2);
        Assert.assertEquals(d2, dr1.getValue());
        Assert.assertSame(dr1, dr1.setValue(d1));
        Assert.assertEquals(d1, dr1.getValue());

        OntDR.Restriction dr2 = m.createRestrictionDataRange(d1, Collections.emptySet());
        Assert.assertEquals(d1, dr2.getValue());
        Assert.assertSame(dr2, dr2.setValue(d2));
        Assert.assertEquals(d2, dr2.getValue());
    }

    @Test
    public void testFacetRestriction() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDT d1 = m.getDatatype(XSD.xstring);
        OntDT d2 = m.getDatatype(XSD.positiveInteger);

        OntDR.Restriction dr = m.createRestrictionDataRange(d1);
        Assert.assertTrue(dr.getList().isEmpty());
        dr.addFacet(OntFR.Pattern.class, d1.createLiteral(".*")).addFacet(OntFR.Length.class, d2.createLiteral(21));

        Assert.assertEquals(2, dr.getList().size());
        Assert.assertEquals(9, m.size());
        List<OntDT> actual = dr.getList().members()
                .map(OntFR::getValue)
                .map(m::getDatatype).collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(d1, d2), actual);
    }

}
