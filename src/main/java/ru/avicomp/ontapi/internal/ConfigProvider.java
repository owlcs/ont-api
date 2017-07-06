/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

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

        /**
         * Returns data-factory reference.
         *
         * @return {@link OWLDataFactory}
         */
        OWLDataFactory dataFactory();

        /**
         * Returns loader-configuration settings.
         * @return {@link OntLoaderConfiguration}
         */
        OntLoaderConfiguration loaderConfig();

        /**
         * Returns writer-configuration settings.
         * @return {@link OntWriterConfiguration}
         */
        OntWriterConfiguration writerConfig();

        /**
         * Answers whether the behaviour should be concurrent oriented.
         *
         * @return true if parallel mode is enabled.
         */
        default boolean parallel() {
            return false;
        }

    }

    /**
     * Default (dummy) implementation of {@link Config}.
     */
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
