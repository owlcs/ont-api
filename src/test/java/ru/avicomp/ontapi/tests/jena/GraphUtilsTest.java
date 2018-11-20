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

import org.apache.jena.graph.Graph;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.utils.SpinModels;
import ru.avicomp.ontapi.utils.UnmodifiableGraph;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * To test {@link Graphs} utility.
 *
 * Created by @szuev on 06.04.2018.
 */
public class GraphUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GraphUtilsTest.class);

    @Test
    public void testLoadSpinTree() {
        Map<String, Graph> graphs = loadSpinGraphs();
        Assert.assertEquals(10, graphs.size());
        UnionGraph g = Graphs.toUnion(graphs.get(SpinModels.SPINMAPL.uri()), graphs.values());
        LOGGER.debug("\n{}", Graphs.toTurtleString(g));
        String tree = Graphs.importsTreeAsString(g);
        LOGGER.debug("----------\n{}", tree);
        Assert.assertEquals(27, tree.split("\n").length);
    }

    @Test
    public void testUnionMakeGraph() {
        UnionGraph a = new UnionGraph(createNamedGraph("a"));
        Graph b = createNamedGraph("b");
        a.addGraph(b);
        UnionGraph c = new UnionGraph(createNamedGraph("c"));
        a.addGraph(c);
        UnionGraph d = new UnionGraph(createNamedGraph("d"));
        c.addGraph(d);
        String tree = Graphs.importsTreeAsString(a);
        LOGGER.debug("----------\n{}", tree);
        Assert.assertEquals(4, tree.split("\n").length);
        d.addGraph(b);
        tree = Graphs.importsTreeAsString(a);
        LOGGER.debug("----------\n{}", tree);
        Assert.assertEquals(5, tree.split("\n").length);
        // recursion:
        d.addGraph(c);
        tree = Graphs.importsTreeAsString(a);
        LOGGER.debug("----------\n{}", tree);
        Assert.assertEquals(6, tree.split("\n").length);

        Graph h = createNamedGraph("H");
        c.addGraph(h);
        a.removeGraph(b);
        a.addGraph(b = new UnionGraph(b));
        ((UnionGraph) b).addGraph(h);
        tree = Graphs.importsTreeAsString(a);
        LOGGER.debug("----------\n{}", tree);
        Assert.assertEquals(8, tree.split("\n").length);

        // remove recursion:
        d.removeGraph(c);
        tree = Graphs.importsTreeAsString(a);
        LOGGER.debug("----------\n{}", tree);
        Assert.assertEquals(7, tree.split("\n").length);
    }

    public static Graph createNamedGraph(String uri) {
        OntGraphModel m = OntModelFactory.createModel();
        m.setID(uri);
        return m.getBaseGraph();
    }

    public static Map<String, Graph> loadSpinGraphs() throws UncheckedIOException {
        Map<String, Graph> res = new HashMap<>();
        for (SpinModels f : SpinModels.values()) {
            Graph g = new GraphMem();
            try (InputStream in = GraphUtilsTest.class.getResourceAsStream(f.file())) {
                RDFDataMgr.read(g, in, null, Lang.TURTLE);
            } catch (IOException e) {
                throw new UncheckedIOException("Can't load " + f.file(), e);
            }
            LOGGER.debug("Graph {} is loaded, size: {}", f.uri(), g.size());
            res.put(f.uri(), new UnmodifiableGraph(g));
        }
        return Collections.unmodifiableMap(res);
    }

}
