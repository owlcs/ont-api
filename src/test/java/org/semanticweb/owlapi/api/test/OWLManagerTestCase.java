package org.semanticweb.owlapi.api.test;

import java.lang.reflect.Field;
import java.util.concurrent.locks.ReadWriteLock;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyImpl;


/**
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 10/04/15
 */
@ru.avicomp.ontapi.utils.ModifiedForONTApi
@SuppressWarnings("javadoc")
public class OWLManagerTestCase {

    private OWLOntologyManager manager;
    private OWLOntology ontology;

    @Before
    public void setUp() throws Exception {
        manager = TestBase.DEBUG_USE_OWL ? ru.avicomp.ontapi.OntManagers.createConcurrentOWL() : ru.avicomp.ontapi.OntManagers.createConcurrentONT();
        ontology = manager.createOntology();
    }

    @Test
    public void shouldCreateOntologyWithCorrectManager() {
        Assert.assertThat(ontology.getOWLOntologyManager(), CoreMatchers.is(manager));
    }

    @Test
    public void shouldCreateConcurrentOntologyByDefault() {
        if (TestBase.DEBUG_USE_OWL) {
            Assert.assertThat(ontology, CoreMatchers.is(CoreMatchers.instanceOf(ConcurrentOWLOntologyImpl.class)));
        } else {
            Assert.assertThat(ontology, CoreMatchers.is(CoreMatchers.instanceOf(ru.avicomp.ontapi.OntologyModelImpl.Concurrent.class)));
        }
    }

    @Test
    public void shouldShareReadWriteLock() throws Exception {
        // Nasty, but not sure of another way to do this without exposing it in
        // the interface
        Object managerReadLock, managerWriteLock;
        if (TestBase.DEBUG_USE_OWL) {
            Field ontologyManagerField = OWLOntologyManagerImpl.class.getDeclaredField("readLock");
            ontologyManagerField.setAccessible(true);
            managerReadLock = ontologyManagerField.get(manager);
            ontologyManagerField = OWLOntologyManagerImpl.class.getDeclaredField("writeLock");
            ontologyManagerField.setAccessible(true);
            managerWriteLock = ontologyManagerField.get(manager);
        } else {
            Field ontologyManagerField = ru.avicomp.ontapi.OntologyManagerImpl.class.getDeclaredField("lock");
            ontologyManagerField.setAccessible(true);
            managerReadLock = ((ReadWriteLock) ontologyManagerField.get(manager)).readLock();
            managerWriteLock = ((ReadWriteLock) ontologyManagerField.get(manager)).writeLock();
        }

        Field ontologyLockField = ConcurrentOWLOntologyImpl.class.getDeclaredField("readLock");
        ontologyLockField.setAccessible(true);
        Object ontologyReadLock = ontologyLockField.get(ontology);
        ontologyLockField = ConcurrentOWLOntologyImpl.class.getDeclaredField("writeLock");
        ontologyLockField.setAccessible(true);
        Object ontologyWriteLock = ontologyLockField.get(ontology);

        Assert.assertThat(ontologyReadLock, CoreMatchers.is(managerReadLock));
        Assert.assertThat(ontologyWriteLock, CoreMatchers.is(managerWriteLock));
    }

}
