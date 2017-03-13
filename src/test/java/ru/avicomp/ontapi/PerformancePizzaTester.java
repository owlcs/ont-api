package ru.avicomp.ontapi;

import java.util.concurrent.TimeUnit;

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
 * <p>
 * Created by @szuev on 16.12.2016.
 */
@SuppressWarnings("ConstantConditions")
public class PerformancePizzaTester {
    private static final Logger LOGGER = Logger.getLogger(PerformancePizzaTester.class);

    private static final String fileName = "pizza.ttl";
    // if this is non-positive number, then the loading&checking axioms will be skipped for ONT-API (OWL-API loads them always):
    private static final int axiomCount = 945;
    private static final int num = 150;
    private static final int innerNum = 1;
    // if true, show also pure jena loading:
    private static final boolean debugTestPureJena = false;
    private static final boolean callGC = false;

    public static void main(String... strings) {
        OWLOntologyDocumentSource source = new IRIDocumentSource(IRI.create(ReadWriteUtils.getResourceURI(fileName)),
                OntFormat.TURTLE.createOwlFormat(), null);

        // init:
        int ont = loadONT(source).getAxiomCount();
        int owl = loadOWL(source).getAxiomCount();
        if (axiomCount > 0) {
            Assert.assertEquals("[ONT]Incorrect axiom count", axiomCount, ont);
            Assert.assertEquals("[OWL]Incorrect axiom count", axiomCount, owl);
        }
        if (callGC)
            System.gc();

        Level level = Logger.getRootLogger().getLevel();
        float owlAverage, ontAverage, jenaAverage;
        try {
            Logger.getRootLogger().setLevel(Level.OFF);
            owlAverage = doTest(num, () -> testOWL(source, axiomCount, innerNum), "OWL", callGC);
            System.err.println("=============");
            ontAverage = doTest(num, () -> testONT(source, axiomCount, innerNum), "ONT", callGC);
            if (debugTestPureJena) {
                System.err.println("=============");
                jenaAverage = doTest(num, () -> testJena(source, innerNum), "JENA", callGC);
            }
        } finally {
            Logger.getRootLogger().setLevel(level);
        }

        LOGGER.info("ONT = " + ontAverage);
        LOGGER.info("OWL = " + owlAverage);
        if (debugTestPureJena) {
            LOGGER.info("JENA = " + jenaAverage);
        }
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

    private static void testONT(OWLOntologyDocumentSource file, int axiomCount, int num) {
        for (int j = 0; j < num; j++) {
            // whole cycle of loading:
            OntologyModel o = loadONT(file);
            if (axiomCount > 0) {
                Assert.assertEquals(axiomCount, o.getAxiomCount());
            }
        }
    }

    private static void testJena(OWLOntologyDocumentSource file, int num) {
        for (int j = 0; j < num; j++) {
            RDFDataMgr.read(GraphFactory.createDefaultGraph(), file.getDocumentIRI().getIRIString(), Lang.TURTLE);
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
