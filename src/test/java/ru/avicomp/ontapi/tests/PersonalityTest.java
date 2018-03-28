/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.tests;

import org.apache.jena.shared.PrefixMapping;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by @szuev on 27.03.2018.
 */
public class PersonalityTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalityTest.class);

    @Test
    public void testClassDatatypePunn() {
        String ns = "http://ex.com#";
        OntGraphModel m = OntModelFactory.createModel(OntModelFactory.createDefaultGraph(), OntModelConfig.ONT_PERSONALITY_LAX);
        m.setNsPrefixes(PrefixMapping.Standard);
        OntCE c1 = m.createOntEntity(OntClass.class, ns + "class1");
        OntCE c2 = m.createOntEntity(OntClass.class, ns + "class2");
        OntIndividual i1 = c1.createIndividual(ns + "indi1");
        OntIndividual i2 = m.createComplementOf(c1).createIndividual(ns + "indi2");
        OntIndividual i3 = c2.createIndividual(ns + "indi3");
        m.createDifferentIndividuals(Arrays.asList(i1, i2, i3));
        ReadWriteUtils.print(m);
        Assert.assertEquals(0, m.listDatatypes().count());
        Assert.assertEquals(2, m.listClasses().count());
        Assert.assertEquals(1, m.ontObjects(OntDisjoint.Individuals.class).count());
        Assert.assertEquals(3, m.ontObjects(OntCE.class).count());
        LOGGER.info("===================");

        // add punn:
        m.createOntEntity(OntDT.class, ns + "class1");
        OntGraphModel m2 = OntModelFactory.createModel(m.getBaseGraph(), OntModelConfig.ONT_PERSONALITY_STRICT);

        try {
            m2.createOntEntity(OntDT.class, ns + "class2");
            Assert.fail("Possible to add punn");
        } catch (OntJenaException e) {
            LOGGER.debug(e.getMessage());
        }

        ReadWriteUtils.print(m2);
        Assert.assertEquals(0, m2.listDatatypes().count());
        Assert.assertEquals(1, m2.listClasses().count());
        Assert.assertEquals(1, m2.ontObjects(OntDisjoint.Individuals.class).count());
        List<OntCE> ces = m2.ontObjects(OntCE.class).collect(Collectors.toList());
        Assert.assertEquals("Wrong ces list: " + ces, 1, ces.size());
        LOGGER.info("===================");

        OntGraphModel m3 = OntModelFactory.createModel(m2.getBaseGraph(), OntModelConfig.ONT_PERSONALITY_LAX);
        Assert.assertEquals(1, m3.listDatatypes().count());
        Assert.assertEquals(2, m3.listClasses().count());
        Assert.assertEquals(1, m3.ontObjects(OntDisjoint.Individuals.class).count());
        Assert.assertEquals(3, m3.ontObjects(OntCE.class).count());
    }

    @Test
    public void testPropertyPunn() {
        String ns = "http://ex.com#";
        OntGraphModel m = OntModelFactory.createModel(OntModelFactory.createDefaultGraph(), OntModelConfig.ONT_PERSONALITY_LAX);
        m.setNsPrefixes(PrefixMapping.Standard);
        OntCE c1 = m.createOntEntity(OntClass.class, ns + "class1");
        OntNOP p1 = m.createOntEntity(OntNOP.class, ns + "prop1");
        OntOPE p2 = m.createOntEntity(OntNOP.class, ns + "prop2").createInverse();

        OntIndividual i1 = c1.createIndividual(ns + "indi1");
        OntIndividual i2 = m.createComplementOf(c1).createIndividual(ns + "indi2");
        OntIndividual i3 = m.createObjectSomeValuesFrom(p1, c1).createIndividual(ns + "indi3");
        OntIndividual i4 = m.createObjectCardinality(p2, 1, c1).createIndividual(ns + "indi4");
        m.createDifferentIndividuals(Arrays.asList(i1, i2, i3, i4));
        ReadWriteUtils.print(m);
        Assert.assertEquals(0, m.listDataProperties().count());
        Assert.assertEquals(0, m.listAnnotationProperties().count());
        Assert.assertEquals(2, m.listObjectProperties().count());
        Assert.assertEquals(1, m.listClasses().count());
        Assert.assertEquals(4, m.ontObjects(OntIndividual.class).count());
        Assert.assertEquals(4, m.ontObjects(OntCE.class).count());
        LOGGER.info("===================");

        // add punns:
        m.createOntEntity(OntNDP.class, ns + "prop1");
        m.createOntEntity(OntNAP.class, ns + "prop2");
        OntGraphModel m2 = OntModelFactory.createModel(m.getBaseGraph(), OntModelConfig.ONT_PERSONALITY_STRICT);

        try {
            m2.createOntEntity(OntNDP.class, ns + "prop2");
            Assert.fail("Possible to add punn");
        } catch (OntJenaException e) {
            LOGGER.debug(e.getMessage());
        }

        ReadWriteUtils.print(m2);
        Assert.assertEquals(0, m2.listObjectProperties().count());
        Assert.assertEquals(0, m2.listDataProperties().count());
        Assert.assertEquals(0, m2.listAnnotationProperties().count());
        List<OntCE> ces = m2.ontObjects(OntCE.class).collect(Collectors.toList());
        // no ObjectSomeValuesFrom, no ObjectCardinality
        Assert.assertEquals("Wrong ces list: " + ces, 2, ces.size());
        LOGGER.info("===================");

        OntGraphModel m3 = OntModelFactory.createModel(m2.getBaseGraph(), OntModelConfig.ONT_PERSONALITY_LAX);
        Assert.assertEquals(1, m3.listDataProperties().count());
        Assert.assertEquals(1, m3.listAnnotationProperties().count());
        Assert.assertEquals(2, m3.listObjectProperties().count());
        Assert.assertEquals(1, m3.ontObjects(OntDisjoint.Individuals.class).count());
        Assert.assertEquals(4, m3.ontObjects(OntCE.class).count());
    }
}
