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

import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.OntDataRange;
import com.github.owlcs.ontapi.jena.model.OntFacetRestriction;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.utils.Models;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * All test-cases related to {@link OntDataRange}.
 * <p>
 * Created by @ssz on 11.05.2019.
 */
public class OntDatatypeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntDatatypeTest.class);

    @Test
    public void testListDataRanges() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntFacetRestriction f1 = m.createFacetRestriction(OntFacetRestriction.MaxExclusive.class, m.createTypedLiteral(12));
        OntFacetRestriction f2 = m.createFacetRestriction(OntFacetRestriction.Pattern.class, m.createTypedLiteral("\\d+"));
        OntFacetRestriction f3 = m.createFacetRestriction(OntFacetRestriction.LangRange.class, m.createTypedLiteral("^r.*"));

        OntDataRange d1 = m.getDatatype(XSD.xstring);
        OntDataRange d2 = m.createDataComplementOf(d1);
        OntDataRange d3 = m.createDataRestriction(d1.asNamed(), f1, f2, f3);

        ReadWriteUtils.print(m);
        Assertions.assertEquals(3, m.ontObjects(OntFacetRestriction.class).count());
        Assertions.assertEquals(2, m.ontObjects(OntDataRange.class).count());
        Assertions.assertEquals(1, m.ontObjects(OntDataRange.ComponentsDR.class).count());
        Assertions.assertEquals(d2, m.ontObjects(OntDataRange.class).filter(s -> s.canAs(OntDataRange.ComplementOf.class))
                .findFirst().orElseThrow(AssertionError::new));
        Assertions.assertEquals(d3, m.ontObjects(OntDataRange.class).filter(s -> s.canAs(OntDataRange.Restriction.class))
                .findFirst().orElseThrow(AssertionError::new));

        Assertions.assertEquals(XSD.xstring, d3.as(OntDataRange.Restriction.class).getValue());
        Assertions.assertEquals(12, d3.spec().peek(s -> LOGGER.debug("{}", Models.toString(s))).count());
    }

    @Test
    public void testDatatypeEquivalentClass() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataRange a = m.createDatatype("A");
        OntDataRange b = m.createDatatype("B");
        OntDataRange c = m.createDatatype("C");
        Assertions.assertNotNull(a.asNamed().addEquivalentClassStatement(b));
        Assertions.assertSame(a, a.asNamed()
                .addEquivalentClass(c)
                .addEquivalentClass(m.getRDFSLiteral()).removeEquivalentClass(b));
        Assertions.assertEquals(2, a.asNamed().equivalentClasses().count());
        Assertions.assertSame(a, a.asNamed().removeEquivalentClass(null));
        Assertions.assertEquals(3, m.size());
    }

    @Test
    public void testDataRangeComponents() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataRange.Named dt1 = m.createDatatype("DT1");
        OntDataRange.Named dt2 = m.createDatatype("DT2");
        OntDataRange.Named dt3 = m.createDatatype("DT3");
        OntDataRange.Named dt4 = m.createDatatype("DT4");
        Literal l1 = dt1.createLiteral("L1");
        Literal l2 = dt1.createLiteral("L2");
        Literal l3 = m.createTypedLiteral(3);
        Literal l4 = m.createTypedLiteral(4);
        OntFacetRestriction fr1 = m.createFacetRestriction(OntFacetRestriction.MaxExclusive.class, l3);
        OntFacetRestriction fr2 = m.createFacetRestriction(OntFacetRestriction.MaxInclusive.class, l4);
        OntFacetRestriction fr3 = m.createFacetRestriction(OntFacetRestriction.TotalDigits.class, l4);

        List<Literal> list1 = Arrays.asList(l1, l2);
        OntDataRange.OneOf dr1 = m.createDataOneOf(list1);
        Assertions.assertEquals(list1, dr1.getList().members().collect(Collectors.toList()));
        Assertions.assertSame(dr1, dr1.setComponents(l2, l3));
        Assertions.assertEquals(Arrays.asList(l2, l3), dr1.getList().members().collect(Collectors.toList()));

        OntDataRange.IntersectionOf dr2 = m.createDataIntersectionOf(dt2, dt3, dt4);
        Assertions.assertEquals(3, dr2.getList().members().count());
        Assertions.assertTrue(dr2.setComponents().getList().isEmpty());

        OntDataRange.Restriction dr3 = m.createDataRestriction(dt3, fr1, fr2);
        Assertions.assertEquals(3, dr3.setComponents(Arrays.asList(fr3, fr1, fr2)).getList().members().count());

        ReadWriteUtils.print(m);

        Set<RDFNode> expected = new HashSet<>(Arrays.asList(l2, l3, fr3, fr1, fr2));
        Set<RDFNode> actual = m.ontObjects(OntDataRange.ComponentsDR.class)
                .map(x -> x.getList())
                .map(x -> x.as(RDFList.class))
                .map(RDFList::asJavaList)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void testDataRangeValues() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataRange.Named d1 = m.getDatatype(XSD.xstring);
        OntDataRange.Named d2 = m.createDatatype("x");

        OntDataRange.ComplementOf dr1 = m.createDataComplementOf(d2);
        Assertions.assertEquals(d2, dr1.getValue());
        Assertions.assertSame(dr1, dr1.setValue(d1));
        Assertions.assertEquals(d1, dr1.getValue());

        OntDataRange.Restriction dr2 = m.createDataRestriction(d1, Collections.emptySet());
        Assertions.assertEquals(d1, dr2.getValue());
        Assertions.assertSame(dr2, dr2.setValue(d2));
        Assertions.assertEquals(d2, dr2.getValue());
    }

    @Test
    public void testFacetRestriction() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataRange d1 = m.getDatatype(XSD.xstring);
        OntDataRange d2 = m.getDatatype(XSD.positiveInteger);

        OntDataRange.Restriction dr = m.createDataRestriction(d1.asNamed());
        Assertions.assertTrue(dr.getList().isEmpty());
        dr.addFacet(OntFacetRestriction.Pattern.class, d1.asNamed().createLiteral(".*"))
                .addFacet(OntFacetRestriction.Length.class, d2.asNamed().createLiteral(21));

        Assertions.assertEquals(2, dr.getList().size());
        Assertions.assertEquals(9, m.size());
        List<OntDataRange.Named> actual = dr.getList().members()
                .map(OntFacetRestriction::getValue)
                .map(m::getDatatype).collect(Collectors.toList());
        Assertions.assertEquals(Arrays.asList(d1, d2), actual);
    }

}
