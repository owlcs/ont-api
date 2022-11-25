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

package com.github.owlcs.ontapi.tests.jena;

import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

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
        Assertions.assertEquals(9, m.ontObjects(OntNamedProperty.class).count());
        Assertions.assertEquals(11, m.ontObjects(OntProperty.class).count());
        Assertions.assertEquals(9, m.ontObjects(OntRealProperty.class).count());
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

        Assertions.assertEquals(1, da.superProperties(true)
                .peek(x -> LOGGER.debug("{} has direct data super property: {}", da, x)).count());
        Assertions.assertEquals(2, da.superProperties(false)
                .peek(x -> LOGGER.debug("{} has data super property: {}", da, x)).count());

        Assertions.assertEquals(1, iob.subProperties(true)
                .peek(x -> LOGGER.debug("{} has direct object sub property: {}", iob, x)).count());
        Assertions.assertEquals(1, iob.subProperties(false)
                .peek(x -> LOGGER.debug("{} has object sub property: {}", iob, x)).count());
        Assertions.assertEquals(2, oa.subProperties(false)
                .peek(x -> LOGGER.debug("{} has object sub property: {}", oa, x)).count());

        Assertions.assertEquals(1, ac.superProperties(true)
                .peek(x -> LOGGER.debug("{} has direct annotation super property: {}", ac, x)).count());
        Assertions.assertEquals(1, ac.subProperties(true)
                .peek(x -> LOGGER.debug("{} has direct annotation sub property: {}", ac, x)).count());
        Assertions.assertEquals(3, ac.superProperties(false)
                .peek(x -> LOGGER.debug("{} has annotation super property: {}", ac, x)).count());
        Assertions.assertEquals(3, m.getRDFSComment().subProperties(false)
                .peek(x -> LOGGER.debug("{} has annotation sub property: {}", m.getRDFSComment(), x)).count());
    }

    @Test
    public void testAnnotationPropertyDomainsAndRanges() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntAnnotationProperty p = m.createAnnotationProperty("A");
        Assertions.assertNotNull(p.addRangeStatement(m.getRDFSComment()));
        Assertions.assertNotNull(p.addDomainStatement(m.getRDFSComment()));
        Assertions.assertSame(p, p.addDomain(m.getOWLThing()).addRange(m.getOWLNothing()).addDomain(m.getRDFSLabel()));
        Assertions.assertEquals(2, p.ranges().count());
        Assertions.assertEquals(3, p.domains().count());

        Assertions.assertSame(p, p.removeDomain(m.getOWLThing()).removeRange(m.getRDFSComment()));
        Assertions.assertEquals(1, p.ranges().count());
        Assertions.assertEquals(2, p.domains().count());
    }

    @Test
    public void testAnnotationSuperProperties() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntAnnotationProperty p = m.createAnnotationProperty("A");
        Assertions.assertNotNull(p.addSubPropertyOfStatement(m.getRDFSComment()));
        Assertions.assertSame(p, p.addSuperProperty(m.getRDFSLabel())
                .addSuperProperty(m.getAnnotationProperty(RDFS.seeAlso)));
        Assertions.assertEquals(3, p.superProperties().count());

        Assertions.assertSame(p, p.removeSuperProperty(m.getOWLThing()).removeSuperProperty(m.getRDFSComment()));
        Assertions.assertEquals(2, p.superProperties().count());
        p.removeSuperProperty(null);
        Assertions.assertEquals(0, p.superProperties().count());
    }

    @Test
    public void testIndirectDomains() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD).setNsPrefix("", "http://ex.com#");
        OntObjectProperty hasDog = m.createObjectProperty(m.expandPrefix(":hasDog"));
        OntDataProperty hasName = m.createDataProperty(m.expandPrefix(":hasName"));
        OntClass animal = m.createOntClass(m.expandPrefix(":Animal"));
        OntClass primate = m.createOntClass(m.expandPrefix(":Primate"));
        OntClass person = m.createOntClass(m.expandPrefix(":Person"));
        primate.addSuperClass(animal);
        person.addSuperClass(primate);
        hasName.addDomain(person);
        hasDog.addDomain(person);

        Assertions.assertEquals(Set.of(person, primate, animal), hasDog.domains(false).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(person, primate, animal), hasName.domains(false).collect(Collectors.toSet()));
    }

    @Test
    public void testDeclaringClasses() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD).setNsPrefix("", "http://ex.com#");

        OntClass c1 = m.createOntClass(":C1");
        OntClass c2 = m.createOntClass(":C2");
        OntClass c3 = m.createOntClass(":C3");
        OntClass c4 = m.createOntClass(":C4");
        OntClass c5 = m.getOWLThing();
        OntClass c6 = m.getOWLNothing();

        OntObjectProperty p1 = m.createObjectProperty(":p1");
        OntObjectProperty p2 = m.createObjectProperty(":p2");
        OntObjectProperty p3 = m.createObjectProperty(":p3");
        OntObjectProperty p4 = m.createObjectProperty(":p4");
        OntDataProperty p5 = m.createDataProperty(":p5");
        OntDataProperty p6 = m.createDataProperty(":p6");
        OntDataProperty p7 = m.createDataProperty(":p7");
        OntObjectProperty p8 = m.getOWLTopObjectProperty();
        OntDataProperty p9 = m.getOWLBottomDataProperty();
        OntObjectProperty p10 = m.getOWLBottomObjectProperty();

        p1.addSuperProperty(p2);
        p2.addSuperProperty(p3);
        p5.addSuperProperty(p6);

        c1.addSuperClass(c2);
        c2.addSuperClass(c3);
        c1.addSuperClass(c4);

        p1.addDomain(c1);
        p2.addDomain(c2);
        p4.addDomain(c4);
        p6.addDomain(c3);
        p7.addDomain(c1);
        p8.addDomain(c5);
        p9.addDomain(c6);

        Assertions.assertEquals(Set.of(c1), p1.declaringClasses(true).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(c1), p1.declaringClasses(false).collect(Collectors.toSet()));

        Assertions.assertEquals(Set.of(c2), p2.declaringClasses(true).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(c1, c2), p2.declaringClasses(false).collect(Collectors.toSet()));

        Assertions.assertEquals(Set.of(c3, c4), p3.declaringClasses(true).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(c1, c2, c3, c4), p3.declaringClasses(false).collect(Collectors.toSet()));

        Assertions.assertEquals(Set.of(c4), p4.declaringClasses(true).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(c1, c4), p4.declaringClasses(false).collect(Collectors.toSet()));

        Assertions.assertEquals(Set.of(c3, c4), p5.declaringClasses(true).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(c1, c2, c3, c4), p5.declaringClasses(false).collect(Collectors.toSet()));

        Assertions.assertEquals(Set.of(c3), p6.declaringClasses(true).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(c1, c2, c3), p6.declaringClasses(false).collect(Collectors.toSet()));

        Assertions.assertEquals(Set.of(c1), p7.declaringClasses(true).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(c1), p7.declaringClasses(false).collect(Collectors.toSet()));

        Assertions.assertEquals(Set.of(c3, c4), p8.declaringClasses(true).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(c1, c2, c3, c4), p8.declaringClasses(false).collect(Collectors.toSet()));

        Assertions.assertEquals(Set.of(c3, c4), p9.declaringClasses(true).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(c1, c2, c3, c4), p9.declaringClasses(false).collect(Collectors.toSet()));

        Assertions.assertEquals(Set.of(c3, c4), p10.declaringClasses(true).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of(c1, c2, c3, c4), p10.declaringClasses(false).collect(Collectors.toSet()));
    }
}
