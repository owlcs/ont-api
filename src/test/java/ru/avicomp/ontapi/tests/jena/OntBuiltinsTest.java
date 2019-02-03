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

import org.apache.jena.graph.Factory;
import org.apache.jena.graph.NodeFactory;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.jena.impl.conf.PersonalityBuilder;
import ru.avicomp.ontapi.jena.model.OntClass;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNOP;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * Created by @ssz on 31.01.2019.
 */
public class OntBuiltinsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntBuiltinsTest.class);

    @Test
    public void testPizzaBuiltins() {
        OntGraphModel m = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("ontapi/pizza.ttl").getGraph());
        Assert.assertEquals(1, m.ontBuiltins(OntClass.class).peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertEquals(OWL.Thing, m.ontBuiltins(OntClass.class, true).findFirst().orElseThrow(AssertionError::new));
    }

    @Test
    public void testBuiltinClassesCustomPersonality() {
        OntPersonality.Builtins test = type -> {
            if (type != OntClass.class) return Collections.emptySet();
            return Stream.of("A", "B", "C", "D", "E", "F").map(NodeFactory::createURI).collect(Iter.toUnmodifiableSet());
        };
        OntPersonality personality = PersonalityBuilder.from(OntModelConfig.ONT_PERSONALITY_LAX).setBuiltins(test).build();
        OntGraphModel m1 = OntModelFactory.createModel(Factory.createGraphMem(), personality)
                .setNsPrefixes(OntModelFactory.STANDARD).setID("m1").getModel();
        OntClass c = m1.createOntEntity(OntClass.class, "b");
        c.addSubClassOf(m1.getOntEntity(OntClass.class, "B"));
        c.addDisjointUnionOf(m1.getOntEntity(OntClass.class, "D"));
        ReadWriteUtils.print(m1);
        OntGraphModel m2 = OntModelFactory.createModel(Factory.createGraphMem(), personality)
                .setNsPrefixes(OntModelFactory.STANDARD).setID("m2").getModel().addImport(m1);
        m2.getOntEntity(OntClass.class, "b").addEquivalentClass(m2.getOntEntity(OntClass.class, "C"));
        ReadWriteUtils.print(m2);

        Assert.assertEquals(3, m2.ontBuiltins(OntClass.class).peek(x -> LOGGER.debug("1) Builtin: {}", x)).count());
        Assert.assertEquals(2, m1.ontBuiltins(OntClass.class).peek(x -> LOGGER.debug("2) Builtin: {}", x)).count());
        Assert.assertEquals(1, m2.ontBuiltins(OntClass.class, true).peek(x -> LOGGER.debug("3) Builtin: {}", x)).count());
        Assert.assertNotNull(m1.getOntEntity(OntClass.class, "A"));
        Assert.assertNotNull(m2.getOntEntity(OntClass.class, "A"));
        Assert.assertNull(m1.getOntEntity(OntClass.class, "X"));
        Assert.assertNull(m2.getOWLThing());

        OntGraphModel m3 = OntModelFactory.createModel(Factory.createGraphMem(), personality)
                .setNsPrefixes(OntModelFactory.STANDARD).addImport(m2);
        Assert.assertEquals(0, m3.ontBuiltins(OntClass.class, true).count());
        m3.createDisjointClasses(Arrays.asList(m1.getOntEntity(OntClass.class, "B"),
                m1.getOntEntity(OntClass.class, "E")));
        m3.createObjectMaxCardinality(m3.createOntEntity(OntNOP.class, "p"), 12, m2.getOntEntity(OntClass.class, "F"));
        ReadWriteUtils.print(m3);
        Assert.assertEquals(5, m3.ontBuiltins(OntClass.class).peek(x -> LOGGER.debug("4) Builtin: {}", x)).count());
    }

    @Test
    public void testBuiltinClassesStandardPersonality() {
        OntGraphModel m1 = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        Assert.assertNotNull(m1.getOWLThing());
        OntNOP p = m1.createOntEntity(OntNOP.class, "p");
        Assert.assertEquals(0, m1.ontBuiltins(OntClass.class).count());
        // unqualified personality
        m1.createObjectCardinality(p, 21, null);
        ReadWriteUtils.print(m1);
        Assert.assertEquals(1, m1.ontBuiltins(OntClass.class).peek(x -> LOGGER.debug("1) Builtin: {}", x)).count());
        Assert.assertNotNull(m1.getOWLThing());

        OntGraphModel m2 = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        m2.getOWLThing().addHasKey();
        Assert.assertEquals(1, m2.ontBuiltins(OntClass.class).peek(x -> LOGGER.debug("2) Builtin: {}", x)).count());
    }
}
