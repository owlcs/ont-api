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
 *
 */

package ru.avicomp.ontapi;

import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * The factory to create and load ontologies.
 * It is the core of the system, an extended {@link OWLOntologyFactory}.
 */
public interface OntologyFactory extends OWLOntologyFactory {

    @Override
    OntologyModel createOWLOntology(@Nonnull OWLOntologyManager manager,
                                    @Nonnull OWLOntologyID id,
                                    @Nonnull IRI documentIRI,
                                    @Nonnull OWLOntologyCreationHandler handler);

    @Override
    OntologyModel loadOWLOntology(@Nonnull OWLOntologyManager manager,
                                  @Nonnull OWLOntologyDocumentSource source,
                                  @Nonnull OWLOntologyCreationHandler handler,
                                  @Nonnull OWLOntologyLoaderConfiguration config) throws OWLOntologyCreationException;

    /**
     * A interface to load model from any source.
     * Currently there are two main implementations:
     * - the decorator of pure OWL-API factory-loader, i.e. <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl</a>
     * - the jena-based factory-loader.
     * <p>
     * Note: there are only three input parameters passed to the single method ({@link OWLOntologyDocumentSource},
     * {@link OntologyManager} and {@link OWLOntologyLoaderConfiguration}), while
     * {@link OWLOntologyFactory#loadOWLOntology} requires four.
     * The single instance of {@link OntologyManager} is an {@link OWLOntologyManager} as well as {@link OWLOntologyCreationHandler}.
     * And this is also true for <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl</a>.
     * Therefore, the {@link OWLOntologyCreationHandler} can be considered just as part of internal (OWL-API) implementation
     * and so there is no need in this parameter in our case.
     *
     * @see OntologyFactoryImpl.OWLLoaderImpl
     * @see OntologyFactoryImpl.ONTLoaderImpl
     */
    interface Loader extends Serializable {

        /**
         * The base method to load ontology model ({@link OntologyModel}) to the manager ({@link OntologyManager}).
         * if the result model contains imports they should come as models also.
         *
         * @param source  {@link OWLOntologyDocumentSource} the source (iri, file iri, stream or who knows what)
         * @param manager {@link OntologyManager}, the manager
         * @param conf    {@link OntLoaderConfiguration}, the load settings
         * @return {@link OntologyModel} the result model inside the manager.
         * @throws OWLOntologyCreationException if something wrong.
         */
        OntologyModel load(OWLOntologyDocumentSource source, OntologyManager manager, OntLoaderConfiguration conf) throws OWLOntologyCreationException;

    }

    /**
     * @see OWLOntologyBuilder
     */
    interface Builder extends OWLOntologyBuilder {

        /**
         * Creates a detached ontology, which is related to the specified manager.
         * Does not change the manager state, although the result ontology will have a reference to it.
         *
         * @param manager {@link OWLOntologyManager}, not null
         * @param id      {@link OWLOntologyID}, not null
         * @return {@link OntologyModel}, new instance reflecting manager settings.
         */
        @Override
        OntologyModel createOWLOntology(@Nonnull OWLOntologyManager manager, @Nonnull OWLOntologyID id);

        /**
         * Creates a new ontology inside the specified manager.
         *
         * @param manager {@link OntologyManager}, not null
         * @param id      {@link OWLOntologyID}, not null
         * @return {@link OntologyModel}, new instance inside manager.
         */
        OntologyModel create(@Nonnull OntologyManager manager, @Nonnull OWLOntologyID id);
    }
}
