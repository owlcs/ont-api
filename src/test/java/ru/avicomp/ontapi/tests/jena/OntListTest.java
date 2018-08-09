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

package ru.avicomp.ontapi.tests.jena;

import org.apache.jena.graph.Factory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.impl.OntListImpl;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by @szuev on 10.07.2018.
 */
public class OntListTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntListTest.class);

    @Test
    public void testCommonFunctionality1() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        OntNOP p1 = m.createOntEntity(OntNOP.class, "p1");
        OntNOP p2 = m.createOntEntity(OntNOP.class, "p2");
        OntNOP p3 = m.createOntEntity(OntNOP.class, "p3");
        p1.addSuperPropertyOf();
        check(m, 1, OntNOP.class);

        OntList<OntOPE> list = p2.createPropertyChain(Collections.emptyList());
        Assert.assertTrue(list.canAs(RDFList.class));
        RDFList r_list = list.as(RDFList.class);
        System.out.println(r_list.isEmpty() + " " + r_list.isValid());
        Assert.assertEquals(3, list.add(p3).add(p3).add(p1).members().count());
        Assert.assertEquals(3, list.members().count());
        Assert.assertEquals(3, list.as(RDFList.class).size());
        Assert.assertEquals(1, p2.listPropertyChains().count());
        Assert.assertEquals(0, p3.listPropertyChains().count());
        Assert.assertEquals(2, m.listObjectProperties().flatMap(OntOPE::listPropertyChains).count());
        check(m, 2, OntNOP.class);

        list.remove();
        Assert.assertEquals(2, list.members().count());
        Assert.assertEquals(2, list.as(RDFList.class).size());
        Assert.assertFalse(list.isEmpty());
        Assert.assertFalse(list.members().anyMatch(p -> p.equals(p1)));
        Assert.assertEquals(p3, list.last().orElseThrow(AssertionError::new));
        Assert.assertEquals(p3, list.first().orElseThrow(AssertionError::new));
        check(m, 2, OntNOP.class);

        Assert.assertEquals(1, (list = list.remove()).members().count());
        Assert.assertEquals(1, list.as(RDFList.class).size());
        Assert.assertFalse(list.isEmpty());
        check(m, 2, OntOPE.class);

        list = list.remove();
        Assert.assertEquals(0, list.members().count());
        Assert.assertEquals(0, list.as(RDFList.class).size());
        Assert.assertTrue(list.isEmpty());
        check(m, 2, OntOPE.class);

    }

    @Test
    public void testCommonFunctionality2() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        OntNOP p1 = m.createOntEntity(OntNOP.class, "p1");
        OntNOP p2 = m.createOntEntity(OntNOP.class, "p2");
        OntNOP p3 = m.createOntEntity(OntNOP.class, "p3");
        OntNOP p4 = m.createOntEntity(OntNOP.class, "p4");
        p1.createPropertyChain(Collections.singletonList(p2)).add(p3);
        check(m, 1, OntOPE.class);

        Assert.assertEquals(1, p1.listPropertyChains().count());
        OntList<OntOPE> list = p1.listPropertyChains().findFirst().orElseThrow(AssertionError::new);
        Assert.assertEquals(3, list.addFirst(p4).members().count());
        Assert.assertTrue(list.first().filter(p4::equals).isPresent());
        Assert.assertTrue(list.last().filter(p3::equals).isPresent());
        check(m, 1, OntOPE.class);

        Assert.assertEquals(1, p1.listPropertyChains().count());
        Assert.assertEquals(2, list.removeFirst().members().count());
        Assert.assertTrue(list.first().filter(p2::equals).isPresent());
        Assert.assertTrue(list.last().filter(p3::equals).isPresent());
        check(m, 1, OntNOP.class);

        Assert.assertTrue(list.removeFirst().removeFirst().isEmpty());
        check(m, 1, OntPE.class);
        Assert.assertEquals(1, list.addFirst(p4).members().count());
        Assert.assertTrue(list.first().filter(p4::equals).isPresent());
        Assert.assertEquals(1, p1.listPropertyChains().count());
        list = p1.listPropertyChains().findFirst().orElseThrow(AssertionError::new);
        Assert.assertEquals(1, list.members().count());
        Assert.assertTrue(list.last().filter(p4::equals).isPresent());
        check(m, 1, OntOPE.class);

        Assert.assertEquals(3, p1.listPropertyChains().findFirst().orElseThrow(AssertionError::new).addLast(p3).addFirst(p2).size());
        check(m, 1, OntNOP.class);
        list = p1.listPropertyChains().findFirst().orElseThrow(AssertionError::new);
        Assert.assertEquals(3, list.size());
        list.removeLast().removeLast();
        Assert.assertEquals(1, p1.listPropertyChains().findFirst().orElseThrow(AssertionError::new).size());
        Assert.assertEquals(1, list.members().count());

        list.clear();
        Assert.assertEquals(0, list.members().count());
        Assert.assertTrue(p1.listPropertyChains().findFirst().orElseThrow(AssertionError::new).isEmpty());
        Assert.assertEquals(0, list.members().count());
        Assert.assertEquals(3, list.addLast(p2).addFirst(p4).addFirst(p3).size());
        Assert.assertEquals(Arrays.asList(p3, p4, p2), list.as(RDFList.class).asJavaList());
    }

    @Test
    public void testGetAndClear1() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        OntNOP p1 = m.createOntEntity(OntNOP.class, "p1");
        OntNOP p2 = m.createOntEntity(OntNOP.class, "p2");
        OntNOP p3 = m.createOntEntity(OntNOP.class, "p3");
        OntNOP p4 = m.createOntEntity(OntNOP.class, "p4");

        OntList<OntOPE> list = p1.createPropertyChain(Arrays.asList(p2, p3)).add(p4);
        check(m, 1, OntOPE.class);

        Assert.assertEquals(3, list.get(0).size());
        Assert.assertEquals(2, list.get(1).size());
        Assert.assertEquals(1, list.get(2).size());
        Assert.assertEquals(0, list.get(3).size());
        try {
            OntList<OntOPE> n = list.get(4);
            Assert.fail("Found out of bound list: " + n);
        } catch (OntJenaException.IllegalArgument j) {
            LOGGER.debug("Expected: {}", j.getMessage());
        }

        Assert.assertTrue(list.get(2).clear().isEmpty());
        check(m, 1, OntOPE.class);
        Assert.assertEquals(2, list.size());
    }

    @Test
    public void testGetAndClear2() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        OntNOP p1 = m.createOntEntity(OntNOP.class, "p1");
        OntNOP p2 = m.createOntEntity(OntNOP.class, "p2");
        OntNOP p3 = m.createOntEntity(OntNOP.class, "p3");
        OntNOP p4 = m.createOntEntity(OntNOP.class, "p4");

        OntList<OntOPE> list = p1.createPropertyChain(Collections.emptyList()).add(p2).add(p3).add(p4);
        check(m, 1, OntOPE.class);
        Assert.assertEquals(2, list.get(2).addFirst(p2).get(1).addLast(p2).size());
        check(m, 1, OntOPE.class);
        // p2, p3, p2, p4, p2
        Assert.assertEquals(Arrays.asList(p2, p3, p2, p4, p2), list.as(RDFList.class).asJavaList());
        // link expired:
        p1.listPropertyChains().findFirst().orElseThrow(AssertionError::new).clear();
        try {
            list.size();
            Assert.fail("Possible to work with expired ont-list instance");
        } catch (OntJenaException.IllegalState j) {
            LOGGER.debug("Expected: {}", j.getMessage());
        }
    }

    @Test
    public void testMixedList() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        OntNOP p1 = m.createOntEntity(OntNOP.class, "p1");
        OntNOP p2 = m.createOntEntity(OntNOP.class, "p2");
        OntNOP p3 = m.createOntEntity(OntNOP.class, "p3");
        OntNOP p4 = m.createOntEntity(OntNOP.class, "p4");
        OntList<OntOPE> list = p1.createPropertyChain(Arrays.asList(p4, p3, p2));
        list.get(1).as(RDFList.class).replace(0, m.createTypedLiteral("Not a property"));
        check(m, 1, RDFNode.class);
        Assert.assertEquals(3, list.size());
        try {
            long c = list.members().count();
            Assert.fail("Possible to get members count for expired ont-list: " + c);
        } catch (OntJenaException.IllegalState j) {
            LOGGER.debug("Expected: {}", j.getMessage());
        }
        list = p1.listPropertyChains().findFirst().orElseThrow(AssertionError::new);
        Assert.assertEquals(3, list.size());
        Assert.assertEquals(2, list.members().count());
        Assert.assertEquals(3, list.addFirst(p3).members().count());
        Assert.assertEquals(4, list.size());
        Assert.assertEquals(2, list.get(1).members().count());
        Assert.assertEquals(p3, list.first().orElseThrow(AssertionError::new));
        Assert.assertEquals(p2, list.last().orElseThrow(AssertionError::new));
    }

    @Test
    public void testListAnnotations() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        OntNOP p1 = m.createOntEntity(OntNOP.class, "p1");
        OntNOP p2 = m.createOntEntity(OntNOP.class, "p2");
        OntNOP p3 = m.createOntEntity(OntNOP.class, "p3");
        OntNOP p4 = m.createOntEntity(OntNOP.class, "p4");
        OntNAP p5 = m.createOntEntity(OntNAP.class, "p5");
        Literal literal_x = m.createLiteral("x");
        Literal literal_y = m.createLiteral("y", "y");
        Literal literal_z = m.createTypedLiteral(2.2);
        // following checking does not really belong to this test (https://github.com/avicomp/ont-api/issues/24):
        Assert.assertEquals(XSD.xdouble.getURI(), literal_z.getDatatypeURI());

        p1.addSuperPropertyOf(p4, p4, p3, p2).addAnnotation(m.getRDFSLabel(), literal_x);
        debug(m);
        OntList<OntOPE> list = p1.listPropertyChains().findFirst().orElseThrow(AssertionError::new);
        Assert.assertEquals(literal_x, getSingleAnnotation(list).getLiteral());
        list.clear();
        debug(m);
        Assert.assertEquals(literal_x, getSingleAnnotation(list).getLiteral());
        Assert.assertEquals(0, list.size());
        list.addLast(p2).addFirst(p3);
        debug(m);
        list.last().filter(p2::equals).orElseThrow(AssertionError::new);
        list.first().filter(p3::equals).orElseThrow(AssertionError::new);
        Assert.assertEquals(literal_x, getSingleAnnotation(list).getLiteral());
        Assert.assertEquals(2, list.size());
        try {
            list.get(1).addAnnotation(m.getRDFSLabel(), literal_z);
            Assert.fail("Possible to annotate sub-lists");
        } catch (OntJenaException.Unsupported j) {
            LOGGER.debug("Expected: {}", j.getMessage());
        }

        getSingleAnnotation(list).addAnnotation(m.getRDFSLabel(), literal_y);
        list.removeFirst();
        debug(m);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(literal_x, getSingleAnnotation(list).getLiteral());
        Assert.assertEquals(literal_y, getSingleAnnotation(getSingleAnnotation(list)).getLiteral());
        list.addAnnotation(p5, literal_z);
        debug(m);
        Assert.assertEquals(2, list.getRoot().annotations().count());
        list.remove();
        debug(m);
        Assert.assertEquals(2, list.getRoot().annotations().count());
        list.getRoot().annotations()
                .filter(s -> p5.equals(s.getPredicate()) && literal_z.equals(s.getLiteral()))
                .findFirst().orElseThrow(AssertionError::new);
        list.getRoot().annotations()
                .filter(s -> RDFS.label.equals(s.getPredicate()) && literal_x.equals(s.getLiteral()))
                .findFirst().orElseThrow(AssertionError::new);
        Assert.assertTrue(list.clearAnnotations().isEmpty());
        Assert.assertEquals(6, m.statements().count());
    }

    @Test
    public void testListSpec() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        OntNOP p1 = m.createOntEntity(OntNOP.class, "p1");
        OntNOP p2 = m.createOntEntity(OntNOP.class, "p2");
        OntNOP p3 = m.createOntEntity(OntNOP.class, "p3");
        OntNOP p4 = m.createOntEntity(OntNOP.class, "p4");
        OntList<OntOPE> list = p1.createPropertyChain(Collections.emptyList());
        debug(m);
        Assert.assertEquals(0, list.spec().count());

        list.add(p2).add(p3).add(p4);
        Assert.assertEquals(6, list.spec().count());
        Set<Statement> set = Models.getAssociatedStatements(m.listStatements(null, OWL.propertyChainAxiom, (RDFNode) null)
                .mapWith(Statement::getObject).mapWith(RDFNode::asResource).toList().get(0));
        Assert.assertEquals(set, list.spec().collect(Collectors.toSet()));

        list.addComment("The list", "xx").addAnnotation(m.getRDFSLabel(), "test");
        debug(m);
        Assert.assertEquals(6, list.spec().count());

        // check that spec elements cannot be annotated
        try {
            list.spec().skip(3).limit(1)
                    .findFirst().orElseThrow(AssertionError::new).addAnnotation(m.getRDFSComment(), "Is it possible?");
            Assert.fail("Possible to annotate some rdf:List statement");
        } catch (OntJenaException j) {
            LOGGER.debug("Expected: {}", j.getMessage());
        }

        list.clear();
        debug(m);
        Assert.assertEquals(0, list.spec().count());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTypedList() {
        OntGraphModelImpl m = new OntGraphModelImpl(Factory.createDefaultGraph(), OntModelConfig.ONT_PERSONALITY_LAX);
        m.setNsPrefixes(OntModelFactory.STANDARD);
        Resource a = m.createResource("A");
        Resource b = m.createResource("B");
        Literal c = m.createLiteral("C");
        Resource d = m.createResource("D");
        Literal e = m.createLiteral("E");

        OntObject s = m.createResource("list").as(OntObject.class);
        Property p = m.createProperty("of");
        OntList list = OntListImpl.create(m, s, p, RDF.List, RDFNode.class, Arrays.asList(a, b, c, d, e).iterator());
        ReadWriteUtils.print(m);
        Assert.assertEquals(RDF.List, ((Optional<Resource>) list.type()).orElseThrow(AssertionError::new));
        Assert.assertEquals(16, m.size());
        Assert.assertEquals(15, list.spec().count());
        Assert.assertEquals(16, list.content().count());
        Assert.assertEquals(5, list.members().count());

        OntList<Resource> tmp1 = OntListImpl.asOntList(list.as(RDFList.class), m, s, p, RDF.List, Resource.class);
        Assert.assertEquals(RDF.List, tmp1.type().orElseThrow(AssertionError::new));
        Assert.assertEquals(15, tmp1.spec().count());
        Assert.assertEquals(3, tmp1.members().count());
        OntList<Literal> tmp2 = OntListImpl.asOntList(list.as(RDFList.class), m, s, p, RDF.List, Literal.class);
        Assert.assertEquals(RDF.List, tmp2.type().orElseThrow(AssertionError::new));
        Assert.assertEquals(15, tmp2.spec().count());
        Assert.assertEquals(2, tmp2.members().count());

        list.removeLast().removeFirst();
        ReadWriteUtils.print(m);
        Assert.assertEquals(10, m.size());
        Assert.assertEquals(9, list.spec().count());

        list.addLast(m.createResource("X")).addFirst(m.createLiteral("Y"));
        ReadWriteUtils.print(m);
        Assert.assertEquals(15, list.spec().count());
        Assert.assertEquals(Arrays.asList("Y", "B", "C", "D", "X"),
                list.members().map(String::valueOf).collect(Collectors.toList()));

        Assert.assertEquals(2, list.get(2).removeFirst()
                .addFirst(m.createResource("Z")).get(1)
                .removeFirst().addLast(m.createLiteral("F")).members().peek(x -> LOGGER.debug("member={}", x)).count());
        ReadWriteUtils.print(m);
        Assert.assertEquals(Arrays.asList("Y", "B", "Z", "X", "F"), list.members().map(String::valueOf).collect(Collectors.toList()));
        Assert.assertEquals(16, m.size());
        Assert.assertEquals(15, list.spec().count());

        list.clear();
        Assert.assertTrue(list.isEmpty());
        Assert.assertEquals(0, list.size());
        Assert.assertEquals(1, m.size());
        ReadWriteUtils.print(m);

        OntList<Resource> empty = OntListImpl.create(m, m.createResource("empty").as(OntObject.class), p, RDF.List,
                Resource.class,
                Collections.emptyIterator());
        Assert.assertTrue(empty.isEmpty());
        Assert.assertEquals(0, empty.size());
        Assert.assertEquals(2, m.size());

        ReadWriteUtils.print(m);
    }

    @Test
    public void testPropertyChain() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        OntNOP p1 = m.createOntEntity(OntNOP.class, "p1");
        OntNOP p2 = m.createOntEntity(OntNOP.class, "p2");
        OntNOP p3 = m.createOntEntity(OntNOP.class, "p3");
        OntNOP p4 = m.createOntEntity(OntNOP.class, "p4");
        p1.addSuperPropertyOf(p2, p3);
        p1.addSuperPropertyOf(p3, p3, p4);
        p1.addSuperPropertyOf(p4, p4);
        debug(m);
        Assert.assertEquals(3, m.listObjectProperties().flatMap(OntOPE::listPropertyChains).count());
        OntList<OntOPE> p334 = p1.listPropertyChains()
                .filter(c -> c.first().filter(p3::equals).isPresent())
                .findFirst()
                .orElseThrow(AssertionError::new);
        Assert.assertEquals(Arrays.asList(p3, p3, p4), p334.members().collect(Collectors.toList()));
        OntList<OntOPE> p23 = p1.listPropertyChains()
                .filter(c -> c.last().filter(p3::equals).isPresent())
                .findFirst()
                .orElseThrow(AssertionError::new);
        Assert.assertEquals(Arrays.asList(p2, p3), p23.members().collect(Collectors.toList()));
        p334.addAnnotation(m.getRDFSComment(), m.createLiteral("p3, p3, p4"));
        p23.addAnnotation(m.getRDFSComment(), m.createLiteral("p2, p3"));
        debug(m);
        Assert.assertEquals(2, m.statements(null, RDF.type, OWL.Axiom).count());
        p1.removePropertyChain(p334);
        debug(m);
        Assert.assertEquals(2, m.listObjectProperties().flatMap(OntOPE::listPropertyChains).count());
        Assert.assertEquals(1, m.statements(null, RDF.type, OWL.Axiom).count());
        p1.clearPropertyChains();
        debug(m);
        Assert.assertEquals(4, m.size());
    }

    @Test
    public void testDisjointUnion() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        OntClass clazz = m.createOntEntity(OntClass.class, "c");
        OntCE ce1, ce3, ce4;
        OntCE ce2 = m.createComplementOf(ce1 = m.createOntEntity(OntClass.class, "c1"));
        OntCE ce5 = m.createUnionOf(Arrays.asList(ce3 = m.createOntEntity(OntClass.class, "c3"), ce4 = m.createOntEntity(OntClass.class, "c4")));
        Assert.assertEquals(2, clazz.addDisjointUnionOf(ce2, ce3).getObject().as(RDFList.class).size());
        Assert.assertEquals(2, clazz.addDisjointUnionOf(ce3, ce3, ce4).getObject().as(RDFList.class).size());
        Assert.assertEquals(3, clazz.addDisjointUnionOf(ce4, ce4, ce5, ce1, ce1).getObject().as(RDFList.class).size());
        debug(m);
        Assert.assertEquals(3, clazz.listDisjointUnions().count());
        Assert.assertEquals(3, m.listClasses().flatMap(OntClass::listDisjointUnions).count());

        OntList<OntCE> d23 = clazz.listDisjointUnions()
                .filter(c -> c.first().filter(ce2::equals).isPresent())
                .findFirst()
                .orElseThrow(AssertionError::new);
        OntList<OntCE> d34 = clazz.listDisjointUnions()
                .filter(c -> c.last().filter(ce4::equals).isPresent())
                .findFirst()
                .orElseThrow(AssertionError::new);
        OntList<OntCE> d451 = clazz.listDisjointUnions()
                .filter(c -> c.last().filter(ce1::equals).isPresent())
                .findFirst()
                .orElseThrow(AssertionError::new);
        Assert.assertEquals(Arrays.asList(ce2, ce3), d23.members().collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(ce3, ce4), d34.members().collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(ce4, ce5, ce1), d451.members().collect(Collectors.toList()));

        d451.addLabel("ce4, ce5, ce1");
        d23.addLabel("ce2, ce3");
        debug(m);
        Assert.assertEquals(2, m.statements(null, RDF.type, OWL.Axiom).count());
        clazz.removeDisjointUnion(d451);

        debug(m);
        Assert.assertEquals(2, m.listClasses().flatMap(OntClass::listDisjointUnions).count());
        Assert.assertEquals(1, m.statements(null, RDF.type, OWL.Axiom).count());
        clazz.clearDisjointUnions();
        debug(m);
        Assert.assertEquals(12, m.size());
    }

    @Test
    public void testHasKey() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        OntClass clazz = m.createOntEntity(OntClass.class, "c");
        OntNOP p1 = m.createOntEntity(OntNOP.class, "p1");
        OntNOP p2 = m.createOntEntity(OntNOP.class, "p2");
        OntNDP p3 = m.createOntEntity(OntNDP.class, "p3");
        OntNDP p4 = m.createOntEntity(OntNDP.class, "p4");
        OntOPE p5 = m.createOntEntity(OntNOP.class, "p5").createInverse();

        Assert.assertEquals(2, clazz.addHasKey(p2, p3).getObject().as(RDFList.class).size());
        Assert.assertEquals(2, clazz.addHasKey(p3, p3, p4).getObject().as(RDFList.class).size());
        Assert.assertEquals(3, clazz.addHasKey(p4, p4, p5, p1, p1).getObject().as(RDFList.class).size());
        debug(m);
        Assert.assertEquals(3, clazz.listHasKeys().count());
        Assert.assertEquals(3, m.listClasses().flatMap(OntClass::listHasKeys).count());

        OntList<OntDOP> h23 = clazz.listHasKeys()
                .filter(c -> c.first().filter(p2::equals).isPresent())
                .findFirst()
                .orElseThrow(AssertionError::new);
        OntList<OntDOP> h34 = clazz.listHasKeys()
                .filter(c -> c.last().filter(p4::equals).isPresent())
                .findFirst()
                .orElseThrow(AssertionError::new);
        OntList<OntDOP> h451 = clazz.listHasKeys()
                .filter(c -> c.last().filter(p1::equals).isPresent())
                .findFirst()
                .orElseThrow(AssertionError::new);
        Assert.assertEquals(Arrays.asList(p2, p3), h23.members().collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(p3, p4), h34.members().collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(p4, p5, p1), h451.members().collect(Collectors.toList()));

        h451.addComment("p4, p5, p1");
        h23.addComment("p2, p3");
        debug(m);
        Assert.assertEquals(2, m.statements(null, RDF.type, OWL.Axiom).count());
        clazz.removeHasKey(h451);

        debug(m);
        Assert.assertEquals(2, m.listClasses().flatMap(OntClass::listHasKeys).count());
        Assert.assertEquals(1, m.statements(null, RDF.type, OWL.Axiom).count());
        clazz.clearHasKeys();
        debug(m);
        Assert.assertEquals(7, m.size());
    }

    @Test
    public void testDisjointPropertiesOntList() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);
        OntNOP p1 = m.createOntEntity(OntNOP.class, "p1");
        OntNOP p2 = m.createOntEntity(OntNOP.class, "p2");
        OntNOP p3 = m.createOntEntity(OntNOP.class, "p3");
        OntNOP p4 = m.createOntEntity(OntNOP.class, "p4");

        OntNDP p5 = m.createOntEntity(OntNDP.class, "p5");
        OntNDP p6 = m.createOntEntity(OntNDP.class, "p6");
        OntNDP p7 = m.createOntEntity(OntNDP.class, "p7");

        OntDisjoint.ObjectProperties d1 = m.createDisjointObjectProperties(Arrays.asList(p1, p2));
        OntDisjoint.DataProperties d2 = m.createDisjointDataProperties(Arrays.asList(p5, p7));
        ReadWriteUtils.print(m);
        Assert.assertEquals(2, m.ontObjects(OntDisjoint.class).count());
        d1.getList().addFirst(p3).addFirst(p4);
        d2.getList().get(1).addFirst(p6);

        ReadWriteUtils.print(m);
        Assert.assertEquals(Arrays.asList(p4, p3, p1, p2), d1.members().collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList(p5, p6, p7), m.ontObjects(OntDisjoint.DataProperties.class)
                .findFirst().orElseThrow(AssertionError::new).members().collect(Collectors.toList()));
    }

    @Test
    public void testDisjointClassIndividualsOntList() {
        OntGraphModel m = OntModelFactory.createModel();
        m.setNsPrefixes(OntModelFactory.STANDARD);

        OntCE ce1 = m.createOntEntity(OntClass.class, "c1");
        OntCE ce3 = m.createHasSelf(m.createOntEntity(OntNOP.class, "p1"));
        OntCE ce2 = m.createDataHasValue(m.createOntEntity(OntNDP.class, "p2"), m.createLiteral("2"));

        OntDisjoint.Classes d1 = m.createDisjointClasses(Arrays.asList(m.getOWLNothing(), ce1, ce3));
        OntDisjoint.Individuals d2 = m.createDifferentIndividuals(Arrays.asList(ce2.createIndividual(), ce3.createIndividual("I")));

        ReadWriteUtils.print(m);
        Assert.assertEquals(2, m.statements(null, OWL.members, null).count());
        Assert.assertEquals(2, m.ontObjects(OntDisjoint.class).count());

        Assert.assertEquals(1, d1.getList().get(1).removeFirst().members().peek(s -> LOGGER.debug("{}", s)).count());
        Assert.assertEquals(2, d1.members().peek(s -> LOGGER.debug("{}", s)).count());
        Assert.assertEquals(1, d2.getList().removeFirst().members().peek(s -> LOGGER.debug("{}", s)).count());
        ReadWriteUtils.print(m);
        Assert.assertEquals(1, m.ontObjects(OntDisjoint.Individuals.class)
                .findFirst().orElseThrow(AssertionError::new).members().count());
    }

    private static OntStatement getSingleAnnotation(OntList<?> list) {
        return getSingleAnnotation(list.getRoot());
    }

    private static OntStatement getSingleAnnotation(OntStatement s) {
        List<OntStatement> res = s.annotations().collect(Collectors.toList());
        Assert.assertEquals(1, res.size());
        return res.get(0);
    }

    private static void check(OntGraphModel m, int numLists, Class<? extends RDFNode> type) {
        debug(m);
        Assert.assertFalse(m.contains(null, RDF.type, RDF.List));
        Assert.assertEquals(numLists, m.statements(null, null, RDF.nil).count());
        m.statements(null, RDF.first, null).map(Statement::getObject).forEach(n -> Assert.assertTrue(n.canAs(type)));
        m.statements(null, RDF.rest, null)
                .map(Statement::getObject)
                .forEach(n -> Assert.assertTrue(RDF.nil.equals(n) ||
                        (n.isAnon() && m.statements().map(OntStatement::getSubject).anyMatch(n::equals))));
    }

    private static void debug(OntGraphModel m) {
        ReadWriteUtils.print(m);
        LOGGER.debug("====");
        m.statements().map(Models::toString).forEach(LOGGER::debug);
    }
}
