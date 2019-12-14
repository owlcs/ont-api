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

package com.github.owlcs.ontapi.tests.transforms;

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.utils.Graphs;
import com.github.owlcs.ontapi.transforms.OWLRecursiveTransform;
import com.github.owlcs.ontapi.transforms.Transform;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by @szuev on 30.01.2018.
 */
@Ignore //todo: temporary ignored: right now this functionality is not used by the ONT-API and buggy
@RunWith(Parameterized.class)
public class RecursionTransformTest {
    private final TestData data;

    public RecursionTransformTest(TestData data) {
        this.data = data;
    }

    /**
     * TODO: although right now this transformer is disabled, so it is doesn't matter, but sometimes (very rare) there is StackOverflowError:
     * java.lang.StackOverflowError
     * at java.util.stream.Streams$StreamBuilderImpl.forEachRemaining(Streams.java:419)
     * at java.util.stream.Streams$ConcatSpliterator.forEachRemaining(Streams.java:742)
     * at java.util.stream.ReferencePipeline$Head.forEach(ReferencePipeline.java:580)
     * at java.util.stream.ReferencePipeline$7$1.accept(ReferencePipeline.java:270)
     */
    @Test
    public void testOWLTrasform() throws IOException {
        OntModel m = OntModelFactory.createModel();
        try (InputStream in = Files.newInputStream(data.file)) {
            m.read(in, null, data.format.getID());
        }
        ReadWriteUtils.print(m);
        Graph g = m.getGraph();
        Transform t = new OWLRecursiveTransform(g);
        TestListener l = new TestListener();
        Graphs.getBase(g).getEventManager().register(l);
        t.perform();
        Assert.assertEquals("Wrong add triples count", data.addCount, l.add.size());
        Assert.assertEquals("Wrong delete triples count", data.deleteCount, l.delete.size());
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<TestData> getData() {
        return Arrays.asList(TestData.of("ontapi/recursive-graph.ttl", 1), TestData.of("/etc/spl.spin.ttl", 0), TestData.of("ontapi/test-rec.ttl", 0), TestData.of("ontapi/test-long.ttl", 0));
    }

    private static class TestData {
        private final Path file;
        private final OntFormat format;
        private final int deleteCount, addCount;

        private TestData(Path file, OntFormat format, int deleteCount, int addCount) {
            this.file = file;
            this.format = format;
            this.deleteCount = deleteCount;
            this.addCount = addCount;
        }

        public static TestData of(String file, int deleteCount) {
            return new TestData(Paths.get(ReadWriteUtils.getResourceURI(file)), OntFormat.TURTLE, deleteCount, 0);
        }

        @Override
        public String toString() {
            return file.getFileName().toString();
        }
    }

    private static class TestListener extends GraphListenerBase {
        private Set<Triple> add = new HashSet<>();
        private Set<Triple> delete = new HashSet<>();

        @Override
        protected void addEvent(Triple t) {
            add.add(t);
        }

        @Override
        protected void deleteEvent(Triple t) {
            delete.add(t);
        }
    }
}
