package ru.avicomp.ontapi.internal;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyWriterConfiguration;

import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.config.OntWriterConfiguration;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * This is an internal object to provide access to the {@link Config} with access to the OWL-API containers with settings.
 * <p>
 * Created by @szuev on 06.04.2017.
 */
public interface ConfigProvider {
    Config DEFAULT = new Dummy();

    ConfigProvider.Config getConfig();

    /**
     * The config.
     * It may content reference to the manager as well,
     * but default implementation ({@link #DEFAULT}) is not intended to work with such things.
     *
     * @see OWLDataFactory
     * @see org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration
     * @see OWLOntologyWriterConfiguration
     * @see OntLoaderConfiguration
     * Created by @szuev on 05.04.2017.
     */
    interface Config {

        OWLDataFactory dataFactory();

        OntLoaderConfiguration loaderConfig();

        OntWriterConfiguration writerConfig();

    }

    class Dummy implements Config {
        private static final OWLDataFactory DATA_FACTORY = new OWLDataFactoryImpl();
        private static final OntConfig GLOBAL_CONFIG = new OntConfig();
        private static final OntLoaderConfiguration LOADER_CONFIGURATION = GLOBAL_CONFIG.buildLoaderConfiguration();
        private static final OntWriterConfiguration WRITER_CONFIGURATION = GLOBAL_CONFIG.buildWriterConfiguration();

        @Override
        public OWLDataFactory dataFactory() {
            return DATA_FACTORY;
        }

        @Override
        public OntLoaderConfiguration loaderConfig() {
            return LOADER_CONFIGURATION;
        }

        @Override
        public OntWriterConfiguration writerConfig() {
            return WRITER_CONFIGURATION;
        }

        public OntConfig globalConfig() {
            return GLOBAL_CONFIG;
        }
    }
}
