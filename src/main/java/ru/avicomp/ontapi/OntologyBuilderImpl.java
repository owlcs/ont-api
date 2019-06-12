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
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.UnionGraph;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * An implementation of {@link OntologyFactory.Builder} - a technical factory to create standalone ontology instances.
 * This should be the only way to create {@link OntologyModel} instances.
 */
@SuppressWarnings("WeakerAccess")
@ParametersAreNonnullByDefault
public class OntologyBuilderImpl implements OntologyFactory.Builder {

    @Override
    public OWLAdapter getAdapter() {
        return OWLAdapter.get();
    }

    @Override
    public OntologyModel createOntology(OntologyManager manager, OntologyID id) {
        OntologyManagerImpl m = getAdapter().asIMPL(manager);
        OntologyModelImpl res = createOntologyImpl(createGraph(), m, m.getOntologyLoaderConfiguration());
        res.setOntologyID(id);
        return withLock(res, m.getLock());
    }

    @Override
    public OntologyModel createOntology(Graph graph, OntologyManager manager, OntLoaderConfiguration config) {
        OntologyManagerImpl m = getAdapter().asIMPL(manager);
        return withLock(createOntologyImpl(graph, m, config), m.getLock());
    }

    /**
     * Creates a {@link OntologyModelImpl Default Ontology Implementation} instance from the given components.
     *
     * @param graph   {@link Graph} obtained from {@link #createGraph()}, must not be {@code null}
     * @param manager {@link OntologyManagerImpl}, must not be {@code null}
     * @param config  {@link OntLoaderConfiguration}, the loading configuration, must not be {@code null}
     * @return a fresh {@link OntologyManagerImpl}
     */
    public OntologyModelImpl createOntologyImpl(Graph graph,
                                                OntologyManagerImpl manager,
                                                OntLoaderConfiguration config) {
        ModelConfig modelConfig = manager.createModelConfig();
        modelConfig.setLoaderConf(config);
        return new OntologyModelImpl(wrap(graph), modelConfig);
    }

    /**
     * Wraps the given {@code ont} as a concurrent R/W locked view impl, if it is needed.
     * @param ont {@link OntologyModelImpl}, not {@code null}
     * @param lock {@link ReadWriteLock}, possible {@code null}
     * @return {@link OntologyModel} instance
     */
    public OntologyModel withLock(OntologyModelImpl ont, ReadWriteLock lock) {
        if (!NoOpReadWriteLock.isConcurrent(lock)) return ont;
        return new OntologyModelImpl.Concurrent(ont, lock);
    }

    /**
     * Creates an {@link org.apache.jena.mem.GraphMem in-memory graph}.
     *
     * @return Graph
     */
    @Override
    public Graph createGraph() {
        return OntModelFactory.createDefaultGraph();
    }

    /**
     * Wraps the given graph as {@link UnionGraph} if it is required.
     * A graph is obtained from this builder with help of the method {@link #createGraph()},
     * or it is coming from {@link ru.avicomp.ontapi.OntologyFactory.Loader Loader},
     * and in the last case it is already {@link UnionGraph} and the method returns the same instance as specified.
     *
     * @param g {@link Graph}, not {@code null}
     * @return {@link UnionGraph}
     */
    public UnionGraph wrap(Graph g) {
        return g instanceof UnionGraph ? (UnionGraph) g : createUnionGraph(g);
    }
}
