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
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.jena.impl.conf.PersonalityBuilder;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.XSD;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by @ssz on 31.01.2019.
 */
public class OntBuiltinsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntBuiltinsTest.class);


    private static void assertBuiltins(OntGraphModel m, Class<? extends OntEntity> type, Collection<Resource> expected) {
        List<? extends Resource> actual = m.ontBuiltins(type)
                .peek(x -> LOGGER.debug("Builtin:{}", x)).collect(Collectors.toList());
        Assert.assertEquals(expected.size(), actual.size());
        expected.forEach(x -> Assert.assertTrue(actual.contains(x)));
    }

    @Test
    public void testBuiltInsGeneralFunctionality() {
        OntGraphModel m = OntModelFactory.createModel();
        Assert.assertEquals(0, m.getOWLBottomObjectProperty().spec().count());
        Assert.assertEquals(0, m.getOWLBottomObjectProperty().statements().count());
        Assert.assertFalse(m.getOWLTopObjectProperty().isLocal());
        Assert.assertNull(m.getOWLTopDataProperty().getRoot());
        Assert.assertEquals(0, m.getOWLNothing().types().count());
        Assert.assertEquals(0, m.getRDFSLabel().content().count());
    }

    @Test
    public void testPizzaBuiltins() {
        OntGraphModel m = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("ontapi/pizza.ttl").getGraph());
        assertBuiltins(m, OntClass.class, Collections.singletonList(OWL.Thing));
        assertBuiltins(m, OntDT.class, Arrays.asList(RDF.langString, XSD.xstring));
    }

    @Test
    public void testFamilyBuiltins() {
        OntGraphModel m = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("ontapi/family.ttl").getGraph());
        assertBuiltins(m, OntClass.class, Collections.emptySet());
        assertBuiltins(m, OntDT.class, Arrays.asList(XSD.xstring, XSD.integer));
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
        m1.createOntClass("b").addSuperClass(m1.getOntClass("B")).addDisjointUnion(m1.getOntClass("D"));
        ReadWriteUtils.print(m1);
        OntGraphModel m2 = OntModelFactory.createModel(Factory.createGraphMem(), personality)
                .setNsPrefixes(OntModelFactory.STANDARD).setID("m2").getModel().addImport(m1);
        m2.getOntClass("b").addEquivalentClass(m2.getOntClass("C"));
        ReadWriteUtils.print(m2);

        Assert.assertEquals(3, m2.ontBuiltins(OntClass.class).peek(x -> LOGGER.debug("1) Builtin: {}", x)).count());
        Assert.assertEquals(2, m1.ontBuiltins(OntClass.class).peek(x -> LOGGER.debug("2) Builtin: {}", x)).count());
        Assert.assertEquals(1, m2.ontBuiltins(OntClass.class, true).peek(x -> LOGGER.debug("3) Builtin: {}", x)).count());
        Assert.assertNotNull(m1.getOntClass("A"));
        Assert.assertNotNull(m2.getOntClass("A"));
        Assert.assertNull(m1.getOntClass("X"));
        Assert.assertNull(m2.getOWLThing());

        OntGraphModel m3 = OntModelFactory.createModel(Factory.createGraphMem(), personality)
                .setNsPrefixes(OntModelFactory.STANDARD).addImport(m2);
        Assert.assertEquals(0, m3.ontBuiltins(OntClass.class, true).count());
        m3.createDisjointClasses(Arrays.asList(m1.getOntClass("B"),
                m1.getOntClass("E")));
        m3.createObjectMaxCardinality(m3.createObjectProperty("p"), 12, m2.getOntClass("F"));
        ReadWriteUtils.print(m3);
        Assert.assertEquals(5, m3.ontBuiltins(OntClass.class).peek(x -> LOGGER.debug("4) Builtin: {}", x)).count());
    }

    @Test
    public void testBuiltinClassesStandardPersonality() {
        OntGraphModel m1 = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        Assert.assertNotNull(m1.getOWLThing());
        OntNOP p = m1.createObjectProperty("p");
        Assert.assertEquals(0, m1.ontBuiltins(OntClass.class).count());
        // unqualified personality
        m1.createObjectCardinality(p, 21, null);
        ReadWriteUtils.print(m1);
        Assert.assertEquals(1, m1.ontBuiltins(OntClass.class).peek(x -> LOGGER.debug("1) Builtin: {}", x)).count());
        Assert.assertNotNull(m1.getOWLThing());

        OntGraphModel m2 = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        Assert.assertEquals(OWL.Thing, m2.getOWLThing().addHasKey());
        Assert.assertEquals(1, m2.ontBuiltins(OntClass.class).peek(x -> LOGGER.debug("2) Builtin: {}", x)).count());
    }


    @Test
    public void testMatchOWLAPIOption() {
        OntPersonality from = OntModelConfig.ONT_PERSONALITY_LAX;
        OntPersonality.Builtins test = new OntPersonality.Builtins() {
            @Override
            public boolean matchOWLAPI() {
                return false;
            }

            @Override
            public Set<Node> get(Class<? extends OntObject> type) throws OntJenaException {
                return from.getBuiltins().get(type);
            }
        };
        OntPersonality personality = PersonalityBuilder.from(from).setBuiltins(test).build();

        Graph g = Factory.createGraphMem();
        g.getPrefixMapping().setNsPrefixes(OntModelFactory.STANDARD);
        OntGraphModel m1 = OntModelFactory.createModel(g, from);
        OntClass c = m1.createOntClass("C");
        OntNOP op = m1.createObjectProperty("OP");
        OntNDP dp = m1.createDataProperty("DP");

        OntCE.ObjectCardinality r1 = m1.createObjectCardinality(op, 12, c);
        OntCE.DataMinCardinality r2 = m1.createDataMinCardinality(dp, 1, null);
        Assert.assertTrue(r1.isQualified());
        Assert.assertFalse(r2.isQualified());

        m1.createHasSelf(op);

        ReadWriteUtils.print(m1);
        assertBuiltins(m1, OntClass.class, Collections.emptySet());
        assertBuiltins(m1, OntDT.class, Collections.singleton(RDFS.Literal));

        LOGGER.debug("----------");
        OntGraphModel m2 = OntModelFactory.createModel(g, personality);
        assertBuiltins(m2, OntClass.class, Collections.emptySet());
        assertBuiltins(m2, OntDT.class, Arrays.asList(RDFS.Literal, XSD.nonNegativeInteger, XSD.xboolean));
    }
}
