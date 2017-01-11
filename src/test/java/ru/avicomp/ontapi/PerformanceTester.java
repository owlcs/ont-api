package ru.avicomp.ontapi;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.google.common.base.Stopwatch;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * todo
 * Created by @szuev on 16.12.2016.
 */
public class PerformanceTester {
    private static final Logger LOGGER = Logger.getLogger(PerformanceTester.class);

    public static void main(String... s) {
        final String fileName = "pizza.ttl";
        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI(fileName));
        load(fileIRI, OntFormat.TTL_RDF);
        System.gc();

        final int num = 50;
        final int innerNum = 1;
        final boolean loadAxioms = true;
        final boolean testPureJena = true;
        Stopwatch stopwatch;

        stopwatch = Stopwatch.createStarted();
        for (int i = 0; i < num; i++) {
            LOGGER.info("[ONT]Iter #" + i);
            testONT(fileIRI, loadAxioms, testPureJena, innerNum);
            System.gc();
        }
        stopwatch.stop();
        float ontAverage = stopwatch.elapsed(TimeUnit.MILLISECONDS) / num;

        stopwatch = Stopwatch.createStarted();
        for (int i = 0; i < num; i++) {
            LOGGER.info("[OWL]Iter #" + i);
            testOWL(fileIRI, loadAxioms, innerNum);
            System.gc();
        }
        stopwatch.stop();
        float owlAverage = stopwatch.elapsed(TimeUnit.MILLISECONDS) / num;


        LOGGER.info("ONT = " + ontAverage);
        LOGGER.info("OWL = " + owlAverage);
        LOGGER.info("ONT/OWL = " + ontAverage / owlAverage);
    }

    private static void testOWL(IRI file, boolean loadAxioms, int num) {
        for (int j = 0; j < num; j++) {
            OWLOntology o = load(file);
            if (loadAxioms)
                o.axioms().collect(Collectors.toSet());
        }
    }

    private static void testONT(IRI file, boolean loadAxioms, boolean testPureJena, int num) {
        for (int j = 0; j < num; j++) {
            if (testPureJena) { // no graph-converter tuning here
                Graph g = GraphFactory.createDefaultGraph();
                RDFDataMgr.read(g, file.getIRIString(), Lang.TURTLE);
                OntInternalModel i = new OntInternalModel(g);
                i.getAxioms();
                continue;
            }
            OntologyModel o = load(file, OntFormat.TTL_RDF);
            if (loadAxioms)
                o.axioms().collect(Collectors.toSet());
        }
    }

    public static OntologyModel load(IRI file, OntFormat format) {
        LOGGER.info("[ONT]Load " + file + "[" + format + "]");
        OntologyManager m = OntManagerFactory.createONTManager();
        try {
            return (OntologyModel) m.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }

    }

    public static OWLOntology load(IRI file) {
        LOGGER.info("[OWL]Load " + file);
        OWLOntologyManager m = OntManagerFactory.createOWLManager();
        try {
            return m.loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }
}
