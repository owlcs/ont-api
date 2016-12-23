package org.semanticweb.owlapi.rio;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLStorerFactory;
import org.semanticweb.owlapi.util.PriorityCollection;

import ru.avicomp.ontapi.OntManagerFactory;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
@SuppressWarnings("javadoc")
public class OWLOntologyStorerFactoryRegistryTestCase {

    private static final int EXPECTED_STORERS = 20;

    @Test
    public void setUp() {
        PriorityCollection<OWLStorerFactory> ontologyStorers = OntManagerFactory.createONTManager().getOntologyStorers();
        assertEquals(EXPECTED_STORERS, ontologyStorers.size());
    }
}
