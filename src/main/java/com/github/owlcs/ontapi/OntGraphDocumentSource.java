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

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;

import java.io.InputStream;
import java.io.Reader;
import java.util.Optional;

/**
 * This is an extended {@link OWLOntologyDocumentSource} that allows to pass any graph into the manager as is.
 * <p>
 * Here, the default implementations of the {@link #getInputStream()} and {@link #getReader()} methods throw an error,
 * these methods are not used by the ONT-API internals,
 * instead, the method {@link #getGraph()} (which provides a direct link to the graph) is used.
 * To control transformations there is the method {@link #withTransforms()}.
 */
public interface OntGraphDocumentSource extends OWLOntologyDocumentSource {

    /**
     * Returns the {@link Graph Jena RDF Graph} to be wrapped as an ontology into the manager.
     *
     * @return {@link Graph}
     */
    Graph getGraph();

    /**
     * Answers whether the graph transformation is allowed.
     * If the method returns {@code true} then the {@link #getGraph() graph}
     * can be put in order by the internal transformations' mechanism, depending on config settings.
     * If the method returns {@code false} then no transformations will be performed
     * and the graph go to the manager as is, without any changes, regardless the config settings.
     *
     * @return {@code boolean}, {@code true} if the graph transformation for the source is allowed
     * @see com.github.owlcs.ontapi.config.LoadSettings#isPerformTransformation()
     */
    default boolean withTransforms() {
        return true;
    }

    @Override
    default Optional<Reader> getReader() {
        throw new UnsupportedOperationException("Inappropriate use of graph source.");
    }

    @Override
    default Optional<InputStream> getInputStream() {
        throw new UnsupportedOperationException("Inappropriate use of graph source.");
    }
}
