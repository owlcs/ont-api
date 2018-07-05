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

package ru.avicomp.ontapi;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.OWLOntologyID;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.OntModelFactory;

/**
 * An implementation of {@link OntologyFactory.Builder} - a technical factory to create standalone ontology instances.
 * This should be the only way to create {@link OntologyModel} instances.
 */
@SuppressWarnings("WeakerAccess")
public class OntologyBuilderImpl implements OntologyFactory.Builder {

    @Override
    public OntologyModel createOntology(OntologyManager manager, OWLOntologyID id) {
        OntologyManagerImpl m = OWLAdapter.get().asIMPL(manager);
        OntologyModelImpl res = createOntologyImpl(createGraph(), m, m.getOntologyLoaderConfiguration());
        res.setOntologyID(id);
        return withLock(res, m);
    }

    @Override
    public OntologyModel createOntology(Graph graph, OntologyManager manager, OntLoaderConfiguration config) {
        OntologyManagerImpl m = OWLAdapter.get().asIMPL(manager);
        return withLock(createOntologyImpl(graph, m, config), m);
    }

    public OntologyModelImpl createOntologyImpl(Graph graph, OntologyManagerImpl manager, OntLoaderConfiguration config) {
        OntologyManagerImpl.ModelConfig modelConfig = manager.createModelConfig();
        if (config != null)
            modelConfig.setLoaderConf(config);
        return new OntologyModelImpl(graph, modelConfig);
    }

    public OntologyModel withLock(OntologyModelImpl ont, OntologyManagerImpl manager) {
        if (!manager.isConcurrent()) return ont;
        return new OntologyModelImpl.Concurrent(ont, manager.getLock());
    }

    @Override
    public Graph createGraph() {
        return OntModelFactory.createDefaultGraph();
    }
}
