package ru.avicomp.ontapi.tests;

import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyManagerImpl;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.OntologyModelImpl;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * to test core ({@link ru.avicomp.ontapi.OntManagerFactory})
 * <p>
 * Created by szuev on 22.12.2016.
 */
public class ManagerTest {

    @Test
    public void test() throws OWLOntologyCreationException {
        final IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI("test1.ttl"));
        final OWLOntologyID id = OntIRI.create("http://dummy").toOwlOntologyID();

        Assert.assertNotSame("The same manager", OntManagerFactory.createONTManager(), OntManagerFactory.createONTManager());
        Assert.assertNotSame("The same concurrent manager", OntManagerFactory.createONTConcurrentManager(), OntManagerFactory.createONTConcurrentManager());

        OntologyManagerImpl m1 = (OntologyManagerImpl) OntManagerFactory.createONTManager();
        Assert.assertFalse("Concurrent", m1.isConcurrent());

        OntologyModel ont1 = m1.loadOntology(fileIRI);
        OntologyModel ont2 = m1.createOntology(id);
        Assert.assertEquals("Incorrect num of ontologies", 2, m1.ontologies().count());
        Stream.of(ont1, ont2).forEach(o -> {
            Assert.assertEquals("Incorrect impl", OntologyModelImpl.class, ont1.getClass());
            Assert.assertNotEquals("Incorrect impl", OntologyModelImpl.Concurrent.class, ont1.getClass());
        });

        OntologyManagerImpl m2 = (OntologyManagerImpl) OntManagerFactory.createONTConcurrentManager();
        Assert.assertTrue("Not Concurrent", m2.isConcurrent());
        OntologyModel ont3 = m2.loadOntology(fileIRI);
        OntologyModel ont4 = m2.createOntology(id);
        Assert.assertEquals("Incorrect num of ontologies", 2, m2.ontologies().count());
        Stream.of(ont3, ont4).forEach(o -> {
            Assert.assertNotEquals("Incorrect impl", OntologyModelImpl.class, ont3.getClass());
            Assert.assertEquals("Incorrect impl", OntologyModelImpl.Concurrent.class, ont3.getClass());
        });

    }
}
