package ru.avicomp.ontapi.tests;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.ontology.OntModel;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.TestUtils;

/**
 * for testing pizza, foaf, googrelations ontologies.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public abstract class LoadTestBase {
    private static final Logger LOGGER = Logger.getLogger(LoadTestBase.class);

    public abstract String getFileName();

    public abstract long getTotalNumberOfAxioms();

    @Test
    public void test() {
        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI(getFileName()));
        LOGGER.info("The file " + fileIRI);

        OWLOntologyManager manager = OntManagerFactory.createOWLOntologyManager();
        OntologyModel ontology = TestUtils.load(manager, fileIRI);
        OWLOntologyID id = ontology.getOntologyID();
        IRI iri = id.getOntologyIRI().orElse(null);
        Assert.assertNotNull("Null ont-iri " + id, iri);

        Assert.assertEquals("Incorrect count of axioms", getTotalNumberOfAxioms(), ontology.getAxiomCount());
        OntModel ontModel = ontology.asGraphModel();
        String ontIRI = iri.getIRIString();
        ontModel.setNsPrefix("", ontIRI + "#");
        ReadWriteUtils.print(ontModel, OntFormat.TTL_RDF);
        Assert.assertNotNull("Null jena ontology ", ontModel.getOntology(ontIRI));

        String copyOntIRI = ontIRI + ".copy";
        OntModel copyOntModel = TestUtils.copyOntModel(ontModel, copyOntIRI);

        OntologyModel copyOntology = TestUtils.loadOntologyFromIOStream(manager, copyOntModel, convertFormat());
        long ontologiesCount = manager.ontologies().count();
        LOGGER.debug("Number of ontologies inside manager: " + ontologiesCount);
        Assert.assertTrue("Incorrect number of ontologies inside manager (" + ontologiesCount + ")", ontologiesCount >= 2);
        LOGGER.debug("Total number of axioms: " + copyOntology.getAxiomCount());
        testAxioms(ontology, copyOntology);
    }

    private void testAxioms(OntologyModel origin, OntologyModel test) {
        long numberOfNamedIndividuals = origin.individualsInSignature().count();
        List<String> errors = new ArrayList<>();
        AxiomType.AXIOM_TYPES.forEach(t -> {
            long expected = origin.axioms(t).count();
            long actual = test.axioms(t).count();
            if (AxiomType.DECLARATION.equals(t)) {
                // don't know why, but sometimes (pizza.ttl) it takes into account NamedIndividuals, but sometimes not (goodrelations.rdf)
                // perhaps it is due to different initial format.
                if (OntFormat.XML_RDF.equals(convertFormat())) {
                    actual -= numberOfNamedIndividuals;
                }
                return;
            }
            if (actual == expected) return;
            errors.add(String.format("Incorrect count of axioms(%s). Expected: %d. Actual: %d\n", t, expected, actual));
        });
        Assert.assertTrue(String.valueOf(errors), errors.isEmpty());
    }

    public OntFormat convertFormat() {
        return OntFormat.TTL_RDF;
    }
}
