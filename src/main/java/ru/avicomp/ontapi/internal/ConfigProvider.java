/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
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

import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;

/**
 * This is an internal object to provide access to the {@link Config},
 * which is a mixed collection of different settings intended to glue the OWL-API into Jena interface.
 * <p>
 * Created by @szuev on 06.04.2017.
 */
public interface ConfigProvider { // todo: rename to Configurable(Model) ? InternalConfigHolder ?
    Config DEFAULT_CONFIG = new Dummy();
    InternalDataFactory DEFAULT_DATA_FACTORY = new NoCacheDataFactory(DEFAULT_CONFIG);

    Config getConfig();

    /**
     * A container with various configuration settings
     * to manage mappings of the structural representation to RDF and vice versa.
     * <p>
     * Created by @szuev on 05.04.2017.
     *
     * @see DataFactory
     * @see OntPersonality
     */
    interface Config {

        /**
         * Returns the reference to the OWL data-factory.
         * TODO: it seems that this method is superfluous here.
         *
         * @return {@link DataFactory}
         */
        DataFactory dataFactory();

        /**
         * Returns an {@link OntPersonality Ontology Personality},
         * a class-mapping that is responsible for jena-polymorphism.
         * Using some of the ONT-personalities it is possible to hide illegal punnings.
         *
         * @return {@link OntPersonality}
         * @see ru.avicomp.ontapi.jena.impl.conf.OntModelConfig
         */
        OntPersonality getPersonality();

        /**
         * Answers whether or not annotation axioms (instances of {@code OWLAnnotationAxiom}) should be loaded.
         * If {@code true} Annotation Property Domain, Property Range, Assertion and SubAnnotationPropertyOf axioms are skipped.
         * @return boolean
         */
        boolean isLoadAnnotationAxioms();

        /**
         * Answers whether bulk-annotations is allowed in declaration axioms or
         * they should go separately as annotation assertion axioms.
         *
         * @return boolean
         */
        boolean isAllowBulkAnnotationAssertions();

        /**
         * Answers whether the Range, Domain and SubClassOf axioms should be separated
         * in case there is a punning with annotation property and some other property (data or object).
         *
         * @return boolean
         */
        boolean isIgnoreAnnotationAxiomOverlaps();

        /**
         * Answers whether the declaration axioms should be allowed.
         * In OWL-API declarations are not always mandatory.
         *
         * @return boolean
         */
        boolean isAllowReadDeclarations();

        /**
         * Answers whether the different bulk annotations for the same axiom should go as different axioms.
         *
         * @return boolean
         */
        boolean isSplitAxiomAnnotations();

        /**
         * Answers whether errors that arise when parsing axioms from a graph should be ignored.
         *
         * @return boolean
         */
        boolean isIgnoreAxiomsReadErrors();

        /**
         * Answers whether the behaviour should be concurrent oriented.
         *
         * @return {@code true} if parallel mode is enabled
         */
        default boolean parallel() {
            return false;
        }

    }

    /**
     * Dummy implementation of the {@link Config}.
     */
    class Dummy implements Config {
        private static final DataFactory DATA_FACTORY = OntManagers.getDataFactory();
        private static final OntLoaderConfiguration LOADER_CONFIGURATION = new OntConfig().buildLoaderConfiguration();

        @Override
        public OntPersonality getPersonality() {
            return LOADER_CONFIGURATION.getPersonality();
        }

        @Override
        public boolean isLoadAnnotationAxioms() {
            return LOADER_CONFIGURATION.isLoadAnnotationAxioms();
        }

        @Override
        public boolean isAllowBulkAnnotationAssertions() {
            return LOADER_CONFIGURATION.isAllowBulkAnnotationAssertions();
        }

        @Override
        public boolean isIgnoreAnnotationAxiomOverlaps() {
            return LOADER_CONFIGURATION.isIgnoreAnnotationAxiomOverlaps();
        }

        @Override
        public boolean isAllowReadDeclarations() {
            return LOADER_CONFIGURATION.isAllowReadDeclarations();
        }

        @Override
        public boolean isSplitAxiomAnnotations() {
            return LOADER_CONFIGURATION.isSplitAxiomAnnotations();
        }

        @Override
        public boolean isIgnoreAxiomsReadErrors() {
            return LOADER_CONFIGURATION.isIgnoreAxiomsReadErrors();
        }

        @Override
        public DataFactory dataFactory() {
            return DATA_FACTORY;
        }

    }
}
