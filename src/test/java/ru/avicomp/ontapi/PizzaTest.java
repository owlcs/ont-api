package ru.avicomp.ontapi;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.utils.ReadWriteUtils;

/**
 * Created by @szuev on 28.09.2016.
 */
public class PizzaTest {
    private static final Logger LOGGER = Logger.getLogger(PizzaTest.class);

    @Test
    public void test() throws OWLOntologyCreationException {
        IRI iri = IRI.create(ReadWriteUtils.getResourceURI("pizza.ttl"));
        LOGGER.debug("The file " + iri);
        OWLOntologyManager manager = OntManagerFactory.createOWLOntologyManager();
        OWLOntology owl = manager.loadOntology(iri);
        Assert.assertEquals("incorrect class " + owl.getClass(), OntologyModel.class, owl.getClass());
        OntologyModel ontology = (OntologyModel) owl;
        ReadWriteUtils.print(ontology.getOntModel(), OntFormat.TTL_RDF);
    }
}
