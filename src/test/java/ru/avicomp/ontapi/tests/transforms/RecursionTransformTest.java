package ru.avicomp.ontapi.tests.transforms;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.transforms.OWLRecursiveTransform;
import ru.avicomp.ontapi.transforms.Transform;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

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
@RunWith(Parameterized.class)
public class RecursionTransformTest {
    private final TestData data;

    public RecursionTransformTest(TestData data) {
        this.data = data;
    }

    @Test
    public void testOWLTrasform() throws IOException {
        OntGraphModel m = OntModelFactory.createModel();
        try (InputStream in = Files.newInputStream(data.file)) {
            m.read(in, null, data.format.getID());
        }
        m.write(System.out, "ttl");
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
        return Arrays.asList(TestData.of("recursive-graph.ttl", 1), TestData.of("/etc/spl.spin.ttl", 0), TestData.of("test-rec.ttl", 0), TestData.of("test-long.ttl", 0));
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
