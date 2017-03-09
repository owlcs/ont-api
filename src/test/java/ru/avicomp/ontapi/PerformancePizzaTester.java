package ru.avicomp.ontapi;

import java.util.concurrent.TimeUnit;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.base.Stopwatch;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * Test performance on an example of the <a href='file:/test/resources/pizza.ttl'>pizza</a> ontology.
 *
 * Created by @szuev on 16.12.2016.
 */
public class PerformancePizzaTester {
    private static final Logger LOGGER = Logger.getLogger(PerformancePizzaTester.class);

    public static void main(String... strings) {
        final String fileName = "pizza.ttl";
        final int axiomCount = 945; // if this is non-positive number, then the loading&checking axioms will be skipped (but OWL-API loads them always)
        final int num = 50;
        final int innerNum = 1;
        final boolean debugTestPureJena = false; // if true, then don't use manager
        final boolean callGC = true;
        OWLOntologyDocumentSource source = new IRIDocumentSource(IRI.create(ReadWriteUtils.getResourceURI(fileName)),
                OntFormat.TURTLE.createOwlFormat(), null);

        // init:
        Assert.assertEquals("[ONT]Incorrect axiom count", axiomCount, loadONT(source).getAxiomCount());
        Assert.assertEquals("[OWL]Incorrect axiom count", axiomCount, loadOWL(source).getAxiomCount());
        System.gc();

        Level level = Logger.getRootLogger().getLevel();
        float owlAverage, ontAverage;
        try {
            Logger.getRootLogger().setLevel(Level.OFF);
            owlAverage = doTest(num, () -> testOWL(source, axiomCount, innerNum), "OWL", callGC);
            System.err.println("=============");
            ontAverage = doTest(num, () -> testONT(source, axiomCount, debugTestPureJena, innerNum), "ONT", callGC);
        } finally {
            Logger.getRootLogger().setLevel(level);
        }

        LOGGER.info("ONT = " + ontAverage);
        LOGGER.info("OWL = " + owlAverage);
        float diff = ontAverage / owlAverage;
        LOGGER.info("ONT/OWL = " + diff);
        Assert.assertTrue("ONT-API should not be slower (" + diff + ")", diff <= 1);
    }

    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    public static float doTest(final int num, final Tester tester, String tip, boolean doGCAfterIter) {
        String txt = tip == null ? String.valueOf(tester) : tip;
        System.err.println("Test " + tip + " (" + num + ")");
        int step = num / 50;
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (int i = 0; i < num; i++) {
            if (i % step == 0) {
                System.err.println("[" + txt + "]Iter #" + i);
            }
            tester.test();
            if (doGCAfterIter) {
                System.gc();
            }
        }
        stopwatch.stop();
        return stopwatch.elapsed(TimeUnit.MILLISECONDS) / num;
    }

    public interface Tester {
        void test();
    }

    private static void testOWL(OWLOntologyDocumentSource file, int axiomCount, int num) {
        for (int j = 0; j < num; j++) {
            OWLOntology o = loadOWL(file);
            if (axiomCount > 0) {
                Assert.assertEquals(axiomCount, o.getAxiomCount());
            }
        }
    }

    private static void testONT(OWLOntologyDocumentSource file, int axiomCount, boolean testPureJena, int num) {
        for (int j = 0; j < num; j++) {
            if (testPureJena) {
                // load without manager
                Graph g = GraphFactory.createDefaultGraph();
                RDFDataMgr.read(g, file.getDocumentIRI().getIRIString(), Lang.TURTLE);
                OntInternalModel i = new OntInternalModel(g, OntModelConfig.ONT_PERSONALITY_LAX);
                if (axiomCount > 0)
                    Assert.assertEquals(axiomCount, i.getAxioms().size());
                continue;
            }
            // whole cycle of loading:
            OntologyModel o = loadONT(file);
            if (axiomCount > 0) {
                Assert.assertEquals(axiomCount, o.getAxiomCount());
            }
        }
    }

    public static OntologyModel loadONT(OWLOntologyDocumentSource file) {
        LOGGER.info("[ONT]Load " + file.getDocumentIRI());
        OntologyManager m = OntManagers.createONT();
        OntConfig.LoaderConfiguration conf = m.getOntologyLoaderConfiguration()
                .setPersonality(OntModelConfig.ONT_PERSONALITY_LAX)
                .setPerformTransformation(false);
        m.setOntologyLoaderConfiguration(conf);
        try {
            return (OntologyModel) m.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }

    public static OWLOntology loadOWL(OWLOntologyDocumentSource file) {
        LOGGER.info("[OWL]Load " + file.getDocumentIRI());
        OWLOntologyManager m = OntManagers.createOWL();
        try {
            return m.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }
}
