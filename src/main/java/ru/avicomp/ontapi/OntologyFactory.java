/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.utils.Models;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.Serializable;

/**
 * The factory to create and load ontologies to the manager.
 * It is the core of the system, an extended {@link OWLOntologyFactory}.
 * <p>
 * An implementation of this interface can be divided into two different parts:
 * {@link Builder Builder}, which is responsible for creating fresh ontologies,
 * and {@link OntologyLoader Loader}, which is responsible to load ontologies into a manager.
 * <p>
 * Created by @szuev
 */
@ParametersAreNonnullByDefault
public interface OntologyFactory extends OWLOntologyFactory, HasAdapter {

    /**
     * Provides a builder part of the factory.
     *
     * @return {@link Builder}, must not be {@code null}
     */
    Builder getBuilder();

    /**
     * Provides a loader part of the factory.
     *
     * @return {@link Loader}, must not be {@code null}
     */
    Loader getLoader();

    /**
     * Creates a fresh {@link OntologyModel Ontology Model} with the given ID inside the manager,
     * and associates it with a {@link OWLDocumentFormat Document Format} object.
     * Unlike the OWL-API default impl, which prefers {@code RDF/XML},
     * a <a href='https://www.w3.org/TR/turtle/'>Turtle</a> is chosen as a default format,
     * since it is more readable and more widely used in Jena-world.
     *
     * @param manager {@link OntologyManager} the ontology manager to set, not {@code null}
     * @param id      {@link OntologyID} the ID of the ontology to create, not {@code null}
     * @return {@link OntologyModel}
     * @throws OntApiException if something goes wrong
     * @since 1.3.0
     */
    default OntologyModel createOntology(OntologyManager manager, OntologyID id) throws OntApiException {
        OntologyModel res = getBuilder().createOntology(manager, id);
        OWLOntologyFactory.OWLOntologyCreationHandler handler = getAdapter().asHandler(manager);
        handler.ontologyCreated(res);
        OWLDocumentFormat format = OntFormat.TURTLE.createOwlFormat();
        Models.setNsPrefixes(res.asGraphModel(), format.asPrefixOWLDocumentFormat().getPrefixName2PrefixMap());
        handler.setOntologyFormat(res, format);
        return res;
    }

    /**
     * Reads a graph from the given document source and stores
     * it as a ready to use {@link OntologyModel Ontology Model} in the specified manager.
     * {@inheritDoc}
     *
     * @param manager {@link OntologyManager} manager the ontology manager to set, not {@code null}
     * @param source  {@link OWLOntologyDocumentSource} the document source that provides the means
     *                of getting a representation of a document, not {@code null}
     * @param config  {@link OntLoaderConfiguration} settings to manage loading process, not {@code null}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException if the ontology could not be created due to some I/O problem,
     *                                      broken source or incompatible state of manager
     * @throws OntApiException              if something else goes wrong
     * @since 1.3.0
     */
    default OntologyModel loadOntology(OntologyManager manager,
                                       OWLOntologyDocumentSource source,
                                       OntLoaderConfiguration config) throws OWLOntologyCreationException, OntApiException {
        return getLoader().loadOntology(getBuilder(), manager, source, config);
    }

    /**
     * Determines if the factory can create an ontology for the specified ontology document IRI.
     * It's a filter method, by default it is allowed to create ontology for any document IRI.
     *
     * @param iri {@link IRI} the document IRI
     * @return {@code true} if the factory is allowed create an ontology given the specified document IRI or
     * {@code false} if the factory cannot handle such IRI
     */
    @Override
    default boolean canCreateFromDocumentIRI(IRI iri) {
        return true;
    }

    /**
     * Determines if the factory can load an ontology for the specified input source.
     * It's a filter method, by default it is allowed to read ontology from any document source.
     *
     * @param source {@link OWLOntologyDocumentSource} the input source from which to load the ontology
     * @return {@code false} if the factory is not suitable for the specified document source or
     * {@code true} if it is allowed to try to handle that source
     */
    @Override
    default boolean canAttemptLoading(OWLOntologyDocumentSource source) {
        return true;
    }

    /**
     * Creates an (empty) ontology.
     * Notices that this method is not throwing checked {@link OWLOntologyCreationException} exception.
     * In most cases it does not work with resources, so there is no need in a checked exception,
     * RuntimeException is enough for all cases.
     *
     * @param manager     the ontology manager to set, must be {@link OntologyManager}, not {@code null}
     * @param id          {@link OWLOntologyID} the ID of the ontology to create, not {@code null}
     * @param documentIRI unused parameter
     * @param handler     unused parameter
     * @return {@link OntologyModel} the newly created ontology
     * @throws OntApiException if something goes wrong
     */
    @Override
    default OntologyModel createOWLOntology(OWLOntologyManager manager,
                                            OWLOntologyID id,
                                            IRI documentIRI,
                                            OWLOntologyCreationHandler handler) {
        Adapter adapter = getAdapter();
        return createOntology(adapter.asONT(manager), adapter.asONT(id));
    }

    /**
     * Reads a graph from the given document source and stores it as Graph based {@link OWLOntology} in the specified manager.
     *
     * @param manager the ontology manager to set, must be {@link OntologyManager}, not {@code null}
     * @param source  {@link OWLOntologyDocumentSource} the document source that provides
     *                the means of getting a representation of a document, not {@code null}
     * @param handler unused parameter
     * @param config  {@link OWLOntologyLoaderConfiguration} a configuration object which can be used
     *                to pass various options to th loader, not {@code null}
     * @return {@link OntologyModel} the newly created and loaded ontology
     * @throws OWLOntologyCreationException if the ontology could not be created due to some I/O problem,
     *                                      broken source or incompatible state of manager
     * @throws OntApiException              if something else goes wrong
     */
    @Override
    default OntologyModel loadOWLOntology(OWLOntologyManager manager,
                                          OWLOntologyDocumentSource source,
                                          OWLOntologyCreationHandler handler,
                                          OWLOntologyLoaderConfiguration config) throws OWLOntologyCreationException {
        Adapter adapter = getAdapter();
        return loadOntology(adapter.asONT(manager), source, adapter.asONT(config));
    }

    /**
     * A part of the factory, that is responsible for creating {@link Graph Graph}-based ontologies.
     * Notice that it produces standalone empty ontologies, which is not associated with any manager,
     * and therefore cannot be used until construction is finished.
     */
    interface Builder extends OntologyCreator, HasAdapter, OWLOntologyBuilder {

        @Override
        default OntologyModel createOWLOntology(OWLOntologyManager manager, OWLOntologyID id) {
            Adapter adapter = getAdapter();
            return createOntology(adapter.asONT(manager), adapter.asONT(id));
        }
    }

    /**
     * A part of the factory, that is responsible for reading ontologies from different document sources.
     */
    interface Loader extends OntologyLoader, HasAdapter, Serializable {
    }

}
