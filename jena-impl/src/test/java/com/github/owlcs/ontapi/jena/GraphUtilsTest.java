/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
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

import com.github.andrewoma.dexx.collection.Sets;
import com.github.owlcs.ontapi.jena.utils.Graphs;
import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.graph.impl.GraphBase;
import org.apache.jena.graph.impl.WrappedGraph;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.GraphWrapper;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To test {@link Graphs} utility class.
 * Created by @szz on 11.06.2019.
 */
@SuppressWarnings("deprecation")
public class GraphUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphUtilsTest.class);

    @Test
    public void testListBaseGraphs() {
        UnionGraph u = new UnionGraph(UnionGraphTest.createTestMemGraph("a"));
        u.addGraph(UnionGraphTest.createTestMemGraph("b"));
        u.addGraph(UnionGraphTest.createTestMemGraph("c"));
        UnionGraph g2 = new UnionGraph(UnionGraphTest.createTestMemGraph("d"));
        g2.addGraph(UnionGraphTest.createTestMemGraph("e"));
        u.addGraph(g2);
        u.addGraph(new WrappedGraph(UnionGraphTest.createTestMemGraph("x")));
        u.addGraph(new GraphWrapper(UnionGraphTest.createTestMemGraph("y")));

        Set<Graph> actual = Graphs.baseGraphs(u).peek(x -> LOGGER.debug("{}", x)).collect(Collectors.toSet());
        Assertions.assertEquals(7, actual.size());
        Assertions.assertEquals(flat(u).collect(Collectors.toSet()), actual);
    }

    private static Stream<Graph> flat(Graph graph) {
        if (graph == null) return Stream.empty();
        return Stream.concat(Stream.of(Graphs.getBase(graph)), Graphs.subGraphs(graph).flatMap(GraphUtilsTest::flat));
    }

    @Test
    public void testIsSized() {
        Assertions.assertTrue(Graphs.isSized(new GraphMem()));
        Assertions.assertTrue(Graphs.isSized(new UnionGraph(new GraphMem())));
        Assertions.assertTrue(Graphs.isSized(new UnionGraph(new GraphWrapper(new GraphMem()))));

        UnionGraph u1 = new UnionGraph(new GraphMem());
        u1.addGraph(u1);
        Assertions.assertFalse(Graphs.isSized(u1));

        Graph g = new GraphBase() {
            @Override
            protected ExtendedIterator<Triple> graphBaseFind(Triple tp) {
                throw new AssertionError();
            }
        };
        Assertions.assertFalse(Graphs.isSized(g));
    }

    @Test
    public void testIsDistinct() {
        Assertions.assertTrue(Graphs.isDistinct(new GraphMem()));
        Assertions.assertTrue(Graphs.isDistinct(new UnionGraph(new GraphMem())));
        Assertions.assertTrue(Graphs.isDistinct(new UnionGraph(new GraphWrapper(new GraphMem()))));

        UnionGraph u1 = new UnionGraph(new GraphMem(), false);
        Assertions.assertTrue(Graphs.isDistinct(u1));

        u1.addGraph(new GraphMem());
        Assertions.assertFalse(Graphs.isDistinct(u1));

        Graph g = new GraphBase() {
            @Override
            protected ExtendedIterator<Triple> graphBaseFind(Triple tp) {
                throw new AssertionError();
            }
        };
        Assertions.assertFalse(Graphs.isDistinct(g));
    }

    @Test
    public void testIsSame() {
        Graph g = Factory.createGraphMem();
        Assertions.assertTrue(Graphs.isSameBase(g, g));

        Graph a = new UnionGraph(new GraphWrapper(new GraphWrapper(g)));
        Assertions.assertTrue(Graphs.isSameBase(a, g));

        MultiUnion b = new MultiUnion();
        b.addGraph(g);
        b.addGraph(new GraphMem());
        Assertions.assertTrue(Graphs.isSameBase(a, b));

        UnionGraph c = new UnionGraph(new GraphWrapper(g));
        Assertions.assertTrue(Graphs.isSameBase(a, c));

        Assertions.assertFalse(Graphs.isSameBase(g, new GraphMem()));

        Graph d = new UnionGraph(new WrappedGraph(new WrappedGraph(g)));
        Assertions.assertFalse(Graphs.isSameBase(a, d));

        Assertions.assertFalse(Graphs.isSameBase(new UnionGraph(g), new UnionGraph(new GraphMem())));

        MultiUnion e = new MultiUnion();
        e.addGraph(new GraphMem());
        e.addGraph(g);
        Assertions.assertFalse(Graphs.isSameBase(b, e));
    }

    @Test
    public void testCollectPrefixes() {
        Graph a = new GraphMem();
        Graph b = new GraphMem();
        Graph c = new GraphMem();
        a.getPrefixMapping().setNsPrefix("a1", "x1").setNsPrefix("a2", "x2");
        b.getPrefixMapping().setNsPrefix("b1", "x3");
        c.getPrefixMapping().setNsPrefix("b2", "x4");

        Assertions.assertEquals(4, Graphs.collectPrefixes(Arrays.asList(a, b, c)).numPrefixes());
        Assertions.assertEquals(3, Graphs.collectPrefixes(Arrays.asList(a, b)).numPrefixes());
        Assertions.assertEquals(2, Graphs.collectPrefixes(Arrays.asList(b, c)).numPrefixes());
        Assertions.assertEquals(1, Graphs.collectPrefixes(Collections.singleton(b)).numPrefixes());

        try {
            Graphs.collectPrefixes(Sets.of(b, c)).setNsPrefix("X", "x");
            Assertions.fail();
        } catch (PrefixMapping.JenaLockedException j) {
            // expected
        }
    }
}
