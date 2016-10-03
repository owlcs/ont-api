package ru.avicomp.ontapi;

import java.util.concurrent.locks.ReadWriteLock;

import org.semanticweb.owlapi.OWLAPIParsersModule;
import org.semanticweb.owlapi.OWLAPIServiceLoaderModule;
import org.semanticweb.owlapi.annotations.OwlapiModule;
import org.semanticweb.owlapi.model.*;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import uk.ac.manchester.cs.owl.owlapi.*;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NonConcurrentDelegate;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NonConcurrentOWLOntologyBuilder;

/**
 * TODO: copy-past from {@link org.semanticweb.owlapi.apibinding.OWLManager}
 * Created by @szuev on 27.09.2016.
 */
public class OntManagerFactory implements OWLOntologyManagerFactory {

    /**
     * Creates an OWL ontology manager that is configured with standard parsers,
     * storeres etc.
     *
     * @return The new manager.
     */
    public static OWLOntologyManager createOWLOntologyManager() {
        Injector injector = createInjector();
        OWLOntologyManager instance = injector.getInstance(OWLOntologyManager.class);
        injector.injectMembers(instance);
        return instance;
    }

    private static Injector createInjector() {
        return Guice.createInjector(new OntologyAPIImplModule(), new OWLAPIParsersModule(),
                new OWLAPIServiceLoaderModule());
    }

    @Override
    public OWLOntologyManager get() {
        return createOWLOntologyManager();
    }

    /**
     * TODO:
     * copy-past from {@link OWLAPIImplModule}
     * Created by @szuev on 27.09.2016.
     */
    @OwlapiModule
    public static class OntologyAPIImplModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(ReadWriteLock.class).to(NoOpReadWriteLock.class).asEagerSingleton();
            bind(boolean.class).annotatedWith(CompressionEnabled.class).toInstance(Boolean.FALSE);
            bind(OWLDataFactory.class).to(OWLDataFactoryImpl.class).asEagerSingleton();
            bind(OWLOntologyManager.class).to(OntManager.class).asEagerSingleton();
            bind(OWLOntologyManager.class).annotatedWith(NonConcurrentDelegate.class).to(OntManager.class)
                    .asEagerSingleton();
            //bind(OWLOntologyBuilder.class).to(ConcurrentOWLOntologyBuilder.class);
            bind(OWLOntologyBuilder.class).to(NonConcurrentOWLOntologyBuilder.class);
            bind(OWLOntologyBuilder.class).annotatedWith(NonConcurrentDelegate.class).to(
                    NonConcurrentOWLOntologyBuilder.class);
            install(new FactoryModuleBuilder().implement(OWLOntology.class, OntologyModel.class).build(OWLOntologyImplementationFactory.class));
            multibind(OWLOntologyFactory.class, OWLOntologyFactoryImpl.class);
        }

        @SafeVarargs
        private final <T> Multibinder<T> multibind(Class<T> type, Class<? extends T>... implementations) {
            Multibinder<T> binder = Multibinder.newSetBinder(binder(), type);
            for (Class<? extends T> i : implementations) {
                binder.addBinding().to(i);
            }
            return binder;
        }
    }
}