package ru.avicomp.ontapi.tests;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import ru.avicomp.ontapi.OntManagerFactory;
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

    static void debug(OWLOntology ontology) {
        LOGGER.info("DEBUG:");
        LOGGER.debug("OWL: ");
        ReadWriteUtils.print(ontology, OntFormat.TTL_RDF);
        LOGGER.debug("Axioms:");
        ontology.axioms().forEach(LOGGER::debug);
        if (ontology instanceof OntologyModel) {
            LOGGER.debug("Jena: ");
            ReadWriteUtils.print(((OntologyModel) ontology).asGraphModel(), OntFormat.TTL_RDF);
        }
    }

    Stream<OWLAxiom> filterAxioms(OWLOntology ontology, AxiomType... excluded) {
        if (excluded.length == 0) return ontology.axioms();
        List<AxiomType> types = Stream.of(excluded).collect(Collectors.toList());
        return ontology.axioms().filter(axiom -> !types.contains(axiom.getAxiomType()));
    }

    void checkAxioms(OntologyModel original, AxiomType... excluded) {
        LOGGER.info("Load ontology to another manager from jena graph.");
        OWLOntologyManager manager = OntManagerFactory.createOWLManager();
        OWLOntology result = ReadWriteUtils.loadOWLOntologyFromIOStream(manager, original.asGraphModel(), null);
        LOGGER.info("All (actual) axioms from reloaded ontology:");
        result.axioms().forEach(LOGGER::info);
        Map<AxiomType, List<OWLAxiom>> expected = TestUtils.toMap(filterAxioms(original, excluded));
        Map<AxiomType, List<OWLAxiom>> actual = TestUtils.toMap(filterAxioms(result, excluded));
        TestUtils.compareAxioms(expected, actual);
    }
}
