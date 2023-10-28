package com.github.owlcs.ontapi.tests.managers;

import com.github.owlcs.ontapi.OntGraphUtils;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;

public class OntGraphUtilsTest {

    @Test
    public void testWriteGraphOWLOntologyDocumentTargetClose() throws Exception {
        // see #53
        Graph graph = GraphFactory.createGraphMem();

        AtomicBoolean writerIsClosed = new AtomicBoolean(false);
        Writer writer = OWLIOUtils.nullWriter(() -> writerIsClosed.set(true));

        OntGraphUtils.writeGraph(graph, Lang.TURTLE, OWLIOUtils.newOWLOntologyDocumentTarget(null, writer));
        // ensure the writer is closed
        Assertions.assertTrue(writerIsClosed.get());

        AtomicBoolean outputStreamIsClosed = new AtomicBoolean(false);
        OutputStream outputStream = OWLIOUtils.nullOutputStream(() -> outputStreamIsClosed.set(true));

        OntGraphUtils.writeGraph(graph, Lang.TURTLE, OWLIOUtils.newOWLOntologyDocumentTarget(outputStream, null));
        // ensure the output-stream is NOT closed
        Assertions.assertFalse(outputStreamIsClosed.get());
    }
}
