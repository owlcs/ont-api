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
import com.github.owlcs.ontapi.jena.UnionGraph;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.utils.Graphs;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.utils.UnmodifiableGraph;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Factory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.shared.ClosedException;
import org.apache.jena.shared.DeleteDeniedException;
import org.apache.jena.shared.PrefixMapping;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;

/**
 * To test {@link UnionGraph}.
 * <p>
 * Created by @ssz on 21.10.2018.
 */
@SuppressWarnings("WeakerAccess")
public class UnionGraphTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnionGraphTest.class);

    static Graph createNamedGraph(String uri) {
        OntModel m = OntModelFactory.createModel();
        m.setID(uri);
        return m.getBaseGraph();
    }

    @SuppressWarnings("deprecation")
    static Graph createTestMemGraph(String name) {
        return new GraphMem() {
            @Override
            public String toString() {
                return String.format("[%s]", name);
            }
        };
    }

    private static void assertClosed(UnionGraph g, boolean expectedClosed) {
        if (expectedClosed) {
            Assertions.assertTrue(g.isClosed());
            Assertions.assertTrue(g.getBaseGraph().isClosed());
            return;
        }
        Assertions.assertFalse(g.isClosed());
        Assertions.assertFalse(g.getBaseGraph().isClosed());
    }

    @Test
    public void testAddRemoveSubGraphs() {
        UnionGraph a = new UnionGraph(createNamedGraph("a"));
        Graph b = createNamedGraph("b");
        a.addGraph(b);
        UnionGraph c = new UnionGraph(createNamedGraph("c"));
        a.addGraph(c);
        UnionGraph d = new UnionGraph(createNamedGraph("d"));
        c.addGraph(d);
        String tree = Graphs.importsTreeAsString(a);
        LOGGER.debug("----------\n{}", tree);
        Assertions.assertEquals(4, tree.split("\n").length);
        d.addGraph(b);
        tree = Graphs.importsTreeAsString(a);
        LOGGER.debug("----------\n{}", tree);
        Assertions.assertEquals(5, tree.split("\n").length);
        // recursion:
        d.addGraph(c);
        tree = Graphs.importsTreeAsString(a);
        LOGGER.debug("----------\n{}", tree);
        Assertions.assertEquals(6, tree.split("\n").length);

        Graph h = createNamedGraph("H");
        c.addGraph(h);
        a.removeGraph(b);
        a.addGraph(b = new UnionGraph(b));
        ((UnionGraph) b).addGraph(h);
        tree = Graphs.importsTreeAsString(a);
        LOGGER.debug("----------\n{}", tree);
        Assertions.assertEquals(8, tree.split("\n").length);

        // remove recursion:
        d.removeGraph(c);
        tree = Graphs.importsTreeAsString(a);
        LOGGER.debug("----------\n{}", tree);
        Assertions.assertEquals(7, tree.split("\n").length);
    }

    @Test
    public void testWrapAsUnmodified() {
        Triple a = Triple.create(NodeFactory.createURI("a"), RDF.Nodes.type, OWL.Class.asNode());
        Triple b = Triple.create(NodeFactory.createURI("b"), RDF.Nodes.type, OWL.Class.asNode());

        Graph base = Factory.createDefaultGraph();
        base.getPrefixMapping().setNsPrefixes(OntModelFactory.STANDARD);
        base.add(a);
        Graph unmodified = new UnmodifiableGraph(base);
        Assertions.assertEquals(1, unmodified.find().toSet().size());
        Assertions.assertEquals(4, unmodified.getPrefixMapping().numPrefixes());

        UnionGraph u = new UnionGraph(unmodified);
        Assertions.assertEquals(4, u.getPrefixMapping().numPrefixes());

        try {
            u.getPrefixMapping().setNsPrefix("x", "http://x#");
            Assertions.fail("Possible to add prefix");
        } catch (PrefixMapping.JenaLockedException lj) {
            LOGGER.debug("Expected: '{}'", lj.getMessage());
        }

        Assertions.assertEquals(4, u.getPrefixMapping().numPrefixes());
        try {
            u.add(b);
            Assertions.fail("Possible to add triple");
        } catch (AddDeniedException aj) {
            LOGGER.debug("Expected: '{}'", aj.getMessage());
        }
        try {
            u.delete(a);
            Assertions.fail("Possible to delete triple");
        } catch (DeleteDeniedException dj) {
            LOGGER.debug("Expected: '{}'", dj.getMessage());
        }
        Assertions.assertEquals(1, unmodified.find().toSet().size());

        base.add(b);
        base.getPrefixMapping().setNsPrefix("x", "http://x#").setNsPrefix("y", "http://y#");
        Assertions.assertEquals(2, u.find().toSet().size());
        Assertions.assertEquals(6, u.getPrefixMapping().numPrefixes());
    }

    @Test
    public void testCloseRecursiveGraph() {
        UnionGraph a = new UnionGraph(Factory.createGraphMem());
        UnionGraph b = new UnionGraph(Factory.createGraphMem());
        UnionGraph c = new UnionGraph(Factory.createGraphMem());
        UnionGraph d = new UnionGraph(Factory.createGraphMem());
        UnionGraph e = new UnionGraph(Factory.createGraphMem());
        assertClosed(a, false);
        assertClosed(b, false);
        assertClosed(c, false);
        assertClosed(d, false);
        assertClosed(e, false);

        c.addGraph(a);
        b.addGraph(c);
        c.addGraph(b).addGraph(d).addGraph(e);
        a.addGraph(c);
        LOGGER.debug("Tree:\n{}", Graphs.importsTreeAsString(a));

        c.close();
        assertClosed(a, true);
        assertClosed(b, true);
        assertClosed(c, true);
        assertClosed(d, true);
        assertClosed(e, true);
    }

    @Test
    public void testCloseHierarchyGraph() {
        UnionGraph a = new UnionGraph(Factory.createGraphMem());
        UnionGraph b = new UnionGraph(Factory.createGraphMem());
        UnionGraph c = new UnionGraph(Factory.createGraphMem());
        assertClosed(a, false);
        assertClosed(b, false);
        assertClosed(c, false);

        a.addGraph(b.addGraph(c));

        b.close();
        assertClosed(b, true);
        assertClosed(c, true);
        assertClosed(a, false);

        UnionGraph d = new UnionGraph(Factory.createGraphMem());
        try {
            b.addGraph(d);
            Assertions.fail("Possible to add a sub-graph");
        } catch (ClosedException ce) {
            LOGGER.debug("Expected: '{}'", ce.getMessage());
        }
        try {
            b.removeGraph(c);
            Assertions.fail("Possible to remove a sub-graph");
        } catch (ClosedException ce) {
            LOGGER.debug("Expected: '{}'", ce.getMessage());
        }
        Assertions.assertNotNull(a.addGraph(d));
        LOGGER.debug("1) Tree:\n{}", Graphs.importsTreeAsString(a));
        Assertions.assertEquals(4, a.listBaseGraphs().toList().size());

        Assertions.assertNotNull(a.removeGraph(b));
        LOGGER.debug("2) Tree:\n{}", Graphs.importsTreeAsString(a));
        Assertions.assertEquals(2, a.listBaseGraphs().toList().size());
    }

    @Test
    public void testDependsOn() {
        Graph g1 = Factory.createGraphMem();
        Graph g2 = Factory.createGraphMem();
        UnionGraph a = new UnionGraph(g1);
        Assertions.assertTrue(a.dependsOn(a));
        Assertions.assertTrue(a.dependsOn(g1));
        Assertions.assertFalse(g1.dependsOn(a));
        Assertions.assertFalse(a.dependsOn(Factory.createGraphMem()));

        UnionGraph b = new UnionGraph(g1);
        UnionGraph c = new UnionGraph(Factory.createGraphMem());
        a.addGraph(b.addGraph(c));
        Assertions.assertEquals(2, a.listBaseGraphs().toList().size());
        String tree = Graphs.importsTreeAsString(a);
        LOGGER.debug("1) Tree:\n{}", tree);
        Assertions.assertEquals(3, tree.split("\n").length);
        Assertions.assertEquals(0, StringUtils.countMatches(tree, Graphs.RECURSIVE_GRAPH_IDENTIFIER));

        Assertions.assertTrue(a.dependsOn(b));
        Assertions.assertTrue(a.dependsOn(c));
        Assertions.assertTrue(a.dependsOn(c.getBaseGraph()));
        Assertions.assertFalse(a.dependsOn(g2));

        UnionGraph d = new UnionGraph(createNamedGraph("d"));
        c.addGraph(d);
        // recursion:
        d.addGraph(a);
        Assertions.assertEquals(3, a.listBaseGraphs().toList().size());
        LOGGER.debug("2) Tree:\n{}", (tree = Graphs.importsTreeAsString(a)));
        Assertions.assertEquals(5, tree.split("\n").length);
        Assertions.assertEquals(4, StringUtils.countMatches(tree, Graphs.NULL_ONTOLOGY_IDENTIFIER));
        Assertions.assertEquals(1, StringUtils.countMatches(tree, Graphs.RECURSIVE_GRAPH_IDENTIFIER));

        Assertions.assertTrue(a.dependsOn(b));
        Assertions.assertTrue(a.dependsOn(c));
        Assertions.assertTrue(a.dependsOn(d));
        Assertions.assertTrue(c.dependsOn(d));
        Assertions.assertTrue(d.dependsOn(c));
        Assertions.assertTrue(d.dependsOn(a));
        Assertions.assertFalse(a.dependsOn(g2));
    }

    @Test
    public void testListBaseGraphs1() {
        Graph a = createTestMemGraph("a");
        Graph b = createTestMemGraph("b");
        Graph c = createTestMemGraph("c");
        UnionGraph u1 = new UnionGraph(a);
        UnionGraph u2 = new UnionGraph(b);
        UnionGraph u3 = new UnionGraph(c);
        u1.addGraph(u1);
        u1.addGraph(u2);
        u1.addGraph(u3);
        u1.addGraph(b);
        Assertions.assertEquals(new HashSet<>(Arrays.asList(a, b, c)), u1.listBaseGraphs().toSet());
    }

    @Test
    public void testListBaseGraphs2() {
        Graph a = createTestMemGraph("a");
        Graph b = createTestMemGraph("b");
        Graph c = createTestMemGraph("c");
        Graph d = createTestMemGraph("d");
        UnionGraph u1 = new UnionGraph(new UnionGraph(a).addGraph(d));
        UnionGraph u2 = new UnionGraph(b);
        UnionGraph u3 = new UnionGraph(new UnionGraph(c));
        u1.addGraph(u1);
        u1.addGraph(u2);
        u1.addGraph(u3);
        u1.addGraph(b);
        Assertions.assertEquals(new HashSet<>(Arrays.asList(a, b, c, d)), u1.listBaseGraphs().toSet());
    }
}
