package ru.avicomp.ontapi.tests;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.TestUtils;

/**
 * base and utility class for graph-tests
 * <p>
 * Created by @szuev on 02.10.2016.
 */
abstract class GraphTestBase {
    static final Logger LOGGER = Logger.getLogger(GraphTestBase.class);

    static void debug(OntologyModel ontology) {
        LOGGER.info("DEBUG:");
        LOGGER.debug("OWL: ");
        ReadWriteUtils.print(ontology, OntFormat.TTL_RDF);
        LOGGER.debug("Axioms:");
        ontology.axioms().forEach(LOGGER::debug);
        LOGGER.debug("Jena: ");
        ReadWriteUtils.print(ontology.asGraphModel(), OntFormat.TTL_RDF);
    }

    Stream<OWLAxiom> filterAxioms(OntologyModel ontology, AxiomType... excluded) {
        if (excluded.length == 0) return ontology.axioms();
        List<AxiomType> types = Stream.of(excluded).collect(Collectors.toList());
        return ontology.axioms().filter(axiom -> !types.contains(axiom.getAxiomType()));
    }

    void checkAxioms(OntologyModel original, AxiomType... excluded) {
        LOGGER.info("Load ontology to another manager from jena graph.");
        OntologyManager manager = OntManagerFactory.createONTManager();
        OntologyModel result = TestUtils.loadOntologyFromIOStream(manager, original.asGraphModel(), null);
        LOGGER.info("All axioms:");
        result.axioms().forEach(LOGGER::info);
        Map<AxiomType, List<OWLAxiom>> expected = TestUtils.toMap(filterAxioms(original, excluded));
        Map<AxiomType, List<OWLAxiom>> actual = TestUtils.toMap(filterAxioms(result, excluded));
        LOGGER.info("Expected axioms:");
        expected.forEach((t, list) -> LOGGER.debug(String.format("[%s]:::%s", t, list)));
        LOGGER.info("Actual axioms:");
        actual.forEach((t, list) -> LOGGER.debug(String.format("[%s]:::%s", t, list)));
        TestUtils.compareAxioms(expected, actual);
    }
}
