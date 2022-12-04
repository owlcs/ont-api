/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi;

import com.github.owlcs.ontapi.config.OntLoaderConfiguration;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;

/**
 * An interface intended to process OWL document sources
 * and produce {@link Ontology} instances inside a {@link OntologyManager} instance.
 *
 * @since 1.4.1
 */
public interface OntologyLoader {

    /**
     * This method loads an {@link Ontology ontology model} from
     * the given {@link OWLOntologyDocumentSource document source}
     * into the {@link OntologyManager manarer} according to the {@link OntLoaderConfiguration configuration} settings.
     * If the document source corresponds an ontology that has imports,
     * these all imports are also treated as sources, and subsequently populated in the manager as ontologies.
     * To handle correctly an IRI document sources,
     * the manager's {@link OWLOntologyIRIMapper IRI Mapper}
     * and {@link OntologyManager.DocumentSourceMapping Graph Mapper} are used.
     * In case of any error the manager state should not be change.
     *
     * @param builder {@link OntologyCreator} to create {@link Ontology} instance with all its parts
     *                ({@link com.github.owlcs.ontapi.jena.UnionGraph},
     *                {@link org.apache.jena.graph.Graph}), cannot be {@code null}
     * @param source  {@link OWLOntologyDocumentSource} the source (iri, file iri, stream, graph or whatever else),
     *                cannot be {@code null}
     * @param manager {@link OntologyManager}, the manager, cannot be {@code null}
     * @param conf    {@link OntLoaderConfiguration}, the load settings configuration, cannot be {@code null}
     * @return {@link Ontology} the resulting ontology model, which must be within the manager
     * @throws OWLOntologyCreationException if something is wrong
     * @throws OntApiException              if something is very wrong
     * @see OWLOntologyIRIMapper
     * @see OntologyManager.DocumentSourceMapping
     */
    Ontology loadOntology(OntologyCreator builder,
                          OntologyManager manager,
                          OWLOntologyDocumentSource source,
                          OntLoaderConfiguration conf) throws OWLOntologyCreationException;

}
