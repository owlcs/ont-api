package ru.avicomp.ontapi.tests;

import org.apache.log4j.Logger;

import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

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
}
