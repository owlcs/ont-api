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
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.shared.ClosedException;
import org.apache.jena.shared.DeleteDeniedException;
import org.apache.jena.shared.PrefixMapping;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.utils.UnmodifiableGraph;

/**
 * Created by @ssz on 21.10.2018.
 */
public class UnionGraphTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnionGraphTest.class);

    @Test
    public void testWrapUnmodified() {
        Triple a = Triple.create(NodeFactory.createURI("a"), RDF.Nodes.type, OWL.Class.asNode());
        Triple b = Triple.create(NodeFactory.createURI("b"), RDF.Nodes.type, OWL.Class.asNode());

        Graph base = Factory.createDefaultGraph();
        base.getPrefixMapping().setNsPrefixes(OntModelFactory.STANDARD);
        base.add(a);
        Graph unmodified = new UnmodifiableGraph(base);
        Assert.assertEquals(1, unmodified.find().toSet().size());
        Assert.assertEquals(4, unmodified.getPrefixMapping().numPrefixes());

        UnionGraph u = new UnionGraph(unmodified);
        Assert.assertEquals(4, u.getPrefixMapping().numPrefixes());

        try {
            u.getPrefixMapping().setNsPrefix("x", "http://x#");
            Assert.fail("Possible to add prefix");
        } catch (PrefixMapping.JenaLockedException lj) {
            LOGGER.debug("Expected: '{}'", lj.getMessage());
        }

        Assert.assertEquals(4, u.getPrefixMapping().numPrefixes());
        try {
            u.add(b);
            Assert.fail("Possible to add triple");
        } catch (AddDeniedException aj) {
            LOGGER.debug("Expected: '{}'", aj.getMessage());
        }
        try {
            u.delete(a);
            Assert.fail("Possible to delete triple");
        } catch (DeleteDeniedException dj) {
            LOGGER.debug("Expected: '{}'", dj.getMessage());
        }
        Assert.assertEquals(1, unmodified.find().toSet().size());

        base.add(b);
        base.getPrefixMapping().setNsPrefix("x", "http://x#").setNsPrefix("y", "http://y#");
        Assert.assertEquals(2, u.find().toSet().size());
        Assert.assertEquals(6, u.getPrefixMapping().numPrefixes());
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
            Assert.fail("Possible to add a sub-graph");
        } catch (ClosedException ce) {
            LOGGER.debug("Expected: '{}'", ce.getMessage());
        }
        try {
            b.removeGraph(c);
            Assert.fail("Possible to remove a sub-graph");
        } catch (ClosedException ce) {
            LOGGER.debug("Expected: '{}'", ce.getMessage());
        }
        Assert.assertNotNull(a.addGraph(d));
        LOGGER.debug("1) Tree:\n{}", Graphs.importsTreeAsString(a));
        Assert.assertEquals(4, a.listBaseGraphs().toList().size());

        Assert.assertNotNull(a.removeGraph(b));
        LOGGER.debug("2) Tree:\n{}", Graphs.importsTreeAsString(a));
        Assert.assertEquals(2, a.listBaseGraphs().toList().size());
    }

    private void assertClosed(UnionGraph g, boolean expectedClosed) {
        if (expectedClosed) {
            Assert.assertTrue(g.isClosed());
            Assert.assertTrue(g.getBaseGraph().isClosed());
            return;
        }
        Assert.assertFalse(g.isClosed());
        Assert.assertFalse(g.getBaseGraph().isClosed());
    }
}
