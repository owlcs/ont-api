/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
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
import com.github.sszuev.jena.ontapi.UnionGraph;
import com.github.sszuev.jena.ontapi.impl.UnionGraphImpl;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphMemFactory;

import java.util.Objects;

/**
 * An interface to create {@link Ontology} instances.
 * Created by @ssz on 03.06.2019.
 *
 * @since 1.4.1
 */
public interface OntologyCreator {

    /**
     * Creates a new detached ontology with a given ID and configuration,
     * which is associated to the specified manager.
     * Does not change the manager state,
     * although the returned ontology must have a reference to the given manager,
     * i.e. the method {@link Ontology#getOWLOntologyManager()} must return the passed manager instance.
     *
     * @param id      {@link ID}, not {@code null}
     * @param manager {@link OntologyManager}, not {@code null}
     * @param config  {@link OntLoaderConfiguration} the config, not {@code null}
     * @return {@link Ontology} new instance reflecting manager settings
     */
    Ontology createOntology(ID id, OntologyManager manager, OntLoaderConfiguration config);

    /**
     * Creates a new detached ontology model which wraps the specified {@link Graph graph}.
     * The method must not change the manager state,
     * although the result ontology must have a reference to it.
     *
     * @param graph   {@link Graph} the graph, not {@code null}
     * @param manager {@link OntologyManager} manager, not {@code null}
     * @param config  {@link OntLoaderConfiguration} the config, not {@code null}
     * @return {@link Ontology} new instance reflecting manager settings
     * @since 1.3.0
     */
    Ontology createOntology(Graph graph, OntologyManager manager, OntLoaderConfiguration config);

    /**
     * Creates a fresh empty {@link Graph RDF Graph} instance.
     * Any ontology in ONT-API relies on a graph and just serves as a facade for it.
     * A {@code Graph} is a primary thing and a holder for raw data.
     * <p>
     * By default, the method offers a {@code GraphMem},
     * which demonstrates great performance for relatively small data.
     *
     * @return {@link Graph Jena Graph}
     * @see OntologyCreator#createUnionGraph(Graph, OntLoaderConfiguration)
     * @since 1.3.0
     */
    default Graph createDataGraph() {
        return GraphMemFactory.createDefaultGraph();
    }

    /**
     * Wraps the specified {@code graph} as an {@link UnionGraph Union Graph},
     * that maintains an ontology {@code owl:imports} hierarchical structure.
     *
     * @param graph  {@link Graph} to set as a base (root), not {@code null}
     * @param config {@link OntLoaderConfiguration} the settings to control creation of the hierarchy container graph
     * @return {@link UnionGraph}, a graph instance containing the {@code graph} as a base graph,
     * which is responsible for a structure hierarchy;
     * @see OntologyCreator#createDataGraph()
     * @since 1.4.2
     */
    default UnionGraph createUnionGraph(Graph graph, OntLoaderConfiguration config) {
        // TODO: parameter 'distinct' must be configurable
        return new UnionGraphImpl(Objects.requireNonNull(graph), true);
    }

}
