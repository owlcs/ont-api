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

import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * To test properties:
 * {@link OntNamedProperty}, {@link OntProperty} ({@link OntObjectProperty}, {@link OntObjectProperty.Named}, {@link OntDataProperty}, {@link OntRealProperty}, {@link OntAnnotationProperty}).
 * <p>
 * Created by @ssz on 08.05.2019.
 */
public class OntPropertyTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntPropertyTest.class);

    @Test
    public void testCreateProperties() {
        String ns = "http://test.com/graph/7#";

        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD).setNsPrefix("test", ns);
        OntAnnotationProperty a1 = m.createAnnotationProperty(ns + "a-p-1");
        OntAnnotationProperty a2 = m.createAnnotationProperty(ns + "a-p-2");
        m.createObjectProperty(ns + "o-p-1");
        m.createObjectProperty(ns + "o-p-2").createInverse();
        m.createObjectProperty(ns + "o-p-3").createInverse().addComment("Anonymous property expression");
        m.createObjectProperty(ns + "o-p-4")
                .addInverseOfStatement(m.createObjectProperty(ns + "o-p-5"))
                .annotate(a1, m.createLiteral("inverse statement, not inverse-property"));
        m.createDataProperty(ns + "d-p-1");
        m.createDataProperty(ns + "d-p-2").addAnnotation(a2, m.createLiteral("data-property"));

        ReadWriteUtils.print(m);
        OntModelTest.simplePropertiesValidation(m);
        Assert.assertEquals(9, m.ontObjects(OntNamedProperty.class).count());
        Assert.assertEquals(11, m.ontObjects(OntProperty.class).count());
        Assert.assertEquals(9, m.ontObjects(OntRealProperty.class).count());
    }

    @Test
    public void testListPropertyHierarchy() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataProperty da = m.createDataProperty("dA");
        OntDataProperty db = m.createDataProperty("dB");

        OntObjectProperty.Named oa = m.createObjectProperty("oA");
        OntObjectProperty.Inverse iob = m.createObjectProperty("oB").createInverse();
        OntObjectProperty.Named oc = m.createObjectProperty("oC");

        OntAnnotationProperty aa = m.createAnnotationProperty("aA");
        OntAnnotationProperty ab = m.createAnnotationProperty("aB");
        OntAnnotationProperty ac = m.createAnnotationProperty("aC");

        da.addSuperProperty(db);
        db.addSuperProperty(m.getOWLBottomDataProperty());

        oc.addSuperProperty(iob);
        iob.addSuperProperty(oa);

        aa.addSuperProperty(ab);
        ab.addSuperProperty(ac).addSuperProperty(m.getRDFSComment());
        ac.addSuperProperty(aa);

        ReadWriteUtils.print(m);

        Assert.assertEquals(1, da.superProperties(true)
                .peek(x -> LOGGER.debug("{} has direct data super property: {}", da, x)).count());
        Assert.assertEquals(2, da.superProperties(false)
                .peek(x -> LOGGER.debug("{} has data super property: {}", da, x)).count());

        Assert.assertEquals(1, iob.subProperties(true)
                .peek(x -> LOGGER.debug("{} has direct object sub property: {}", iob, x)).count());
        Assert.assertEquals(1, iob.subProperties(false)
                .peek(x -> LOGGER.debug("{} has object sub property: {}", iob, x)).count());
        Assert.assertEquals(2, oa.subProperties(false)
                .peek(x -> LOGGER.debug("{} has object sub property: {}", oa, x)).count());

        Assert.assertEquals(1, ac.superProperties(true)
                .peek(x -> LOGGER.debug("{} has direct annotation super property: {}", ac, x)).count());
        Assert.assertEquals(1, ac.subProperties(true)
                .peek(x -> LOGGER.debug("{} has direct annotation sub property: {}", ac, x)).count());
        Assert.assertEquals(3, ac.superProperties(false)
                .peek(x -> LOGGER.debug("{} has annotation super property: {}", ac, x)).count());
        Assert.assertEquals(3, m.getRDFSComment().subProperties(false)
                .peek(x -> LOGGER.debug("{} has annotation sub property: {}", m.getRDFSComment(), x)).count());
    }

    @Test
    public void testAnnotationPropertyDomainsAndRanges() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntAnnotationProperty p = m.createAnnotationProperty("A");
        Assert.assertNotNull(p.addRangeStatement(m.getRDFSComment()));
        Assert.assertNotNull(p.addDomainStatement(m.getRDFSComment()));
        Assert.assertSame(p, p.addDomain(m.getOWLThing()).addRange(m.getOWLNothing()).addDomain(m.getRDFSLabel()));
        Assert.assertEquals(2, p.ranges().count());
        Assert.assertEquals(3, p.domains().count());

        Assert.assertSame(p, p.removeDomain(m.getOWLThing()).removeRange(m.getRDFSComment()));
        Assert.assertEquals(1, p.ranges().count());
        Assert.assertEquals(2, p.domains().count());
    }

    @Test
    public void testDataPropertyDomainsAndRanges() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass.Named c = m.createOntClass("C");
        OntDataRange.Named d = m.getDatatype(XSD.xstring);
        OntDataProperty p = m.createDataProperty("D");
        Assert.assertNotNull(p.addRangeStatement(m.getRDFSLiteral()));
        Assert.assertNotNull(p.addDomainStatement(m.getOWLNothing()));
        Assert.assertSame(p, p.addDomain(m.getOWLThing()).addRange(d).addDomain(c));
        Assert.assertEquals(2, p.ranges().count());
        Assert.assertEquals(3, p.domains().count());

        Assert.assertSame(p, p.removeDomain(m.getOWLThing()).removeRange(d));
        Assert.assertEquals(1, p.ranges().count());
        Assert.assertEquals(2, p.domains().count());

        p.removeRange(null).removeDomain(null);
        Assert.assertEquals(2, m.size());
    }

    @Test
    public void testObjectPropertyDomainsAndRanges() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass.Named c = m.createOntClass("C");
        OntObjectProperty.Named p = m.createObjectProperty("O");
        Assert.assertNotNull(p.addRangeStatement(m.getOWLThing()));
        Assert.assertNotNull(p.addDomainStatement(m.getOWLNothing()));
        Assert.assertSame(p, p.addDomain(m.getOWLThing()).addRange(m.getOWLNothing()).addDomain(c));
        Assert.assertEquals(2, p.ranges().count());
        Assert.assertEquals(3, p.domains().count());

        Assert.assertSame(p, p.removeDomain(m.getOWLThing()).removeRange(m.getOWLNothing()));
        Assert.assertEquals(1, p.ranges().count());
        Assert.assertEquals(2, p.domains().count());

        p.removeRange(null).removeDomain(null);
        Assert.assertEquals(2, m.size());
    }

    @Test
    public void testAnnotationSuperProperties() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntAnnotationProperty p = m.createAnnotationProperty("A");
        Assert.assertNotNull(p.addSubPropertyOfStatement(m.getRDFSComment()));
        Assert.assertSame(p, p.addSuperProperty(m.getRDFSLabel())
                .addSuperProperty(m.getAnnotationProperty(RDFS.seeAlso)));
        Assert.assertEquals(3, p.superProperties().count());

        Assert.assertSame(p, p.removeSuperProperty(m.getOWLThing()).removeSuperProperty(m.getRDFSComment()));
        Assert.assertEquals(2, p.superProperties().count());
        p.removeSuperProperty(null);
        Assert.assertEquals(0, p.superProperties().count());
    }

    @Test
    public void testDataSuperProperties() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataProperty p1 = m.createDataProperty("D");
        OntDataProperty p2 = m.createDataProperty("P");
        Assert.assertNotNull(p1.addSubPropertyOfStatement(m.getOWLBottomDataProperty()));
        Assert.assertSame(p1, p1.addSuperProperty(m.getOWLTopDataProperty())
                .addSuperProperty(p2));
        Assert.assertEquals(3, p1.superProperties().count());

        Assert.assertSame(p1, p1.removeSuperProperty(m.getOWLThing()).removeSuperProperty(m.getOWLTopDataProperty()));
        Assert.assertEquals(2, p1.superProperties().count());
        p1.removeSuperProperty(null);
        Assert.assertEquals(0, p1.superProperties().count());
    }

    @Test
    public void testObjectSuperProperties() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty.Named p1 = m.createObjectProperty("O");
        OntObjectProperty.Named p2 = m.createObjectProperty("P");
        Assert.assertNotNull(p1.addSubPropertyOfStatement(m.getOWLBottomObjectProperty()));
        Assert.assertSame(p1, p1.addSuperProperty(m.getOWLTopObjectProperty())
                .addSuperProperty(p2));
        Assert.assertEquals(3, p1.superProperties().count());

        Assert.assertSame(p1, p1.removeSuperProperty(m.getOWLThing()).removeSuperProperty(m.getOWLTopObjectProperty()));
        Assert.assertEquals(2, p1.superProperties().count());
        p1.removeSuperProperty(null);
        Assert.assertEquals(0, p1.superProperties().count());
    }

    @Test
    public void testDataPropertyAdditionalDeclarations() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataProperty p = m.createDataProperty("P");
        Assert.assertNotNull(p.addFunctionalDeclaration());
        Assert.assertTrue(p.isFunctional());
        Assert.assertSame(p, p.setFunctional(false));
        Assert.assertFalse(p.isFunctional());
    }

    @Test
    public void testObjectPropertyAdditionalDeclarations() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty.Named p = m.createObjectProperty("P");
        Assert.assertNotNull(p.addFunctionalDeclaration().getSubject(OntObjectProperty.class)
                .addInverseFunctionalDeclaration().getSubject(OntObjectProperty.class)
                .addAsymmetricDeclaration().getSubject(OntObjectProperty.class)
                .addSymmetricDeclaration().getSubject(OntObjectProperty.class)
                .addReflexiveDeclaration().getSubject(OntObjectProperty.class)
                .addIrreflexiveDeclaration().getSubject(OntObjectProperty.class)
                .addTransitiveDeclaration().getSubject(OntObjectProperty.class));

        Assert.assertTrue(p.isFunctional());
        Assert.assertTrue(p.isInverseFunctional());
        Assert.assertTrue(p.isSymmetric());
        Assert.assertTrue(p.isAsymmetric());
        Assert.assertTrue(p.isReflexive());
        Assert.assertTrue(p.isIrreflexive());
        Assert.assertTrue(p.isTransitive());

        Assert.assertSame(p, p.setFunctional(false)
                .setInverseFunctional(false)
                .setAsymmetric(false)
                .setSymmetric(false)
                .setIrreflexive(false)
                .setReflexive(false)
                .setTransitive(false));
        Assert.assertEquals(1, m.size());

        Assert.assertSame(p, p.setFunctional(true)
                .setInverseFunctional(true)
                .setAsymmetric(true)
                .setSymmetric(true)
                .setIrreflexive(true)
                .setReflexive(true)
                .setTransitive(true));
        Assert.assertEquals(8, m.size());
    }

    @Test
    public void testPropertyChains() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty.Named p = m.createObjectProperty("P");
        OntObjectProperty.Named p1 = m.createObjectProperty("P1");
        OntObjectProperty.Named p2 = m.createObjectProperty("P2");
        Assert.assertNotNull(p.addPropertyChainAxiomStatement());
        Assert.assertSame(p, p.addPropertyChain());
        Assert.assertEquals(0, p.fromPropertyChain().count());
        Assert.assertSame(p, p.addPropertyChain(Arrays.asList(p1, p1)).addPropertyChain(Arrays.asList(p2, p2)));
        Assert.assertEquals(2, p.fromPropertyChain().count());
    }

    @Test
    public void testObjectPropertyInverseOf() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty.Named a = m.createObjectProperty("A");
        OntObjectProperty.Named b = m.createObjectProperty("B");
        OntObjectProperty.Named c = m.createObjectProperty("C");
        Assert.assertNotNull(a.addInverseOfStatement(b));
        Assert.assertEquals(b, a.findInverseProperty().orElseThrow(AssertionError::new));
        Assert.assertEquals(1, a.inverseProperties().count());
        Assert.assertSame(c, c.addInverseProperty(b).addInverseProperty(a));
        Assert.assertEquals(2, c.inverseProperties().count());
        Assert.assertSame(c, c.removeInverseProperty(c).removeInverseProperty(b));
        Assert.assertEquals(1, c.inverseProperties().count());
        Assert.assertSame(a, a.removeInverseProperty(null));
        Assert.assertEquals(4, m.size());
    }

    @Test
    public void testDataPropertyEquivalentProperties() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataProperty a = m.createDataProperty("A");
        OntDataProperty b = m.createDataProperty("B");
        OntDataProperty c = m.createDataProperty("C");
        Assert.assertNotNull(a.addEquivalentPropertyStatement(b));
        Assert.assertSame(a, a.addEquivalentProperty(c).addEquivalentProperty(m.getOWLBottomDataProperty()));
        Assert.assertEquals(3, a.equivalentProperties().count());
        Assert.assertSame(a, a.removeEquivalentProperty(b).removeEquivalentProperty(m.getRDFSComment()));
        Assert.assertEquals(2, a.equivalentProperties().count());
        Assert.assertSame(a, a.removeEquivalentProperty(null));
        Assert.assertEquals(3, m.size());
    }

    @Test
    public void testObjectPropertyEquivalentProperties() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty.Named a = m.createObjectProperty("A");
        OntObjectProperty.Named b = m.createObjectProperty("B");
        OntObjectProperty.Named c = m.createObjectProperty("C");
        Assert.assertNotNull(a.addEquivalentPropertyStatement(b));
        Assert.assertSame(a, a.addEquivalentProperty(c).addEquivalentProperty(m.getOWLTopObjectProperty()));
        Assert.assertEquals(3, a.equivalentProperties().count());
        Assert.assertSame(a, a.removeEquivalentProperty(b).removeEquivalentProperty(m.getOWLThing()));
        Assert.assertEquals(2, a.equivalentProperties().count());
        Assert.assertSame(a, a.removeEquivalentProperty(null));
        Assert.assertEquals(3, m.size());
    }

    @Test
    public void testDataPropertyDisjointProperties() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntDataProperty a = m.createDataProperty("A");
        OntDataProperty b = m.createDataProperty("B");
        OntDataProperty c = m.createDataProperty("C");
        Assert.assertNotNull(a.addPropertyDisjointWithStatement(b));
        Assert.assertSame(a, a.addDisjointProperty(c).addDisjointProperty(m.getOWLBottomDataProperty()));
        Assert.assertEquals(3, a.disjointProperties().count());
        Assert.assertSame(a, a.removeDisjointProperty(b).removeDisjointProperty(m.getRDFSComment()));
        Assert.assertEquals(2, a.disjointProperties().count());
        Assert.assertSame(a, a.removeDisjointProperty(null));
        Assert.assertEquals(3, m.size());
    }

    @Test
    public void testObjectPropertyDisjointProperties() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntObjectProperty.Named a = m.createObjectProperty("A");
        OntObjectProperty.Named b = m.createObjectProperty("B");
        OntObjectProperty.Named c = m.createObjectProperty("C");
        Assert.assertNotNull(a.addPropertyDisjointWithStatement(b));
        Assert.assertSame(a, a.addDisjointProperty(c).addDisjointProperty(m.getOWLTopObjectProperty()));
        Assert.assertEquals(3, a.disjointProperties().count());
        Assert.assertSame(a, a.removeDisjointProperty(b).removeDisjointProperty(m.getOWLThing()));
        Assert.assertEquals(2, a.disjointProperties().count());
        Assert.assertSame(a, a.removeDisjointProperty(null));
        Assert.assertEquals(3, m.size());
    }
}
