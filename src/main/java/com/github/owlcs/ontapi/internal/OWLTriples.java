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

package com.github.owlcs.ontapi.internal;

import com.github.sszuev.jena.ontapi.impl.GraphListenerBase;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.CollectionGraph;
import org.semanticweb.owlapi.model.OWLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * An {@link ONTObject} implementation that holds triples in memory.
 * Used while adding the content container ({@code OWLAxiom} or {@code OWLAnnotation})
 * into the {@link InternalGraphModel} cache.
 *
 * @param <V> any subtype of {@link OWLObject}
 */
@SuppressWarnings("WeakerAccess")
public class OWLTriples<V extends OWLObject> extends ONTWrapperImpl<V> {
    protected final Set<Triple> triples;

    protected OWLTriples(V object, Set<Triple> triples) {
        super(object);
        this.triples = triples;
    }

    @Override
    public Stream<Triple> triples() {
        return triples.stream();
    }

    @Override
    public Graph toGraph() {
        return new CollectionGraph(triples);
    }

    @Override
    public boolean isDefinitelyEmpty() {
        return triples.isEmpty();
    }

    /**
     * Creates a listener capable of producing this wrapper.
     *
     * @return {@link Listener}
     */
    public static Listener createListener() {
        return new Listener();
    }

    /**
     * A {@link org.apache.jena.graph.GraphListener Graph Listener} implementation
     * that monitors the {@code Graph} mutation while adding {@link OWLObject} into the cache-map.
     */
    public static class Listener extends GraphListenerBase {
        private static final Logger LOGGER = LoggerFactory.getLogger(Listener.class);
        protected final Set<Triple> triples = new HashSet<>();

        @Override
        protected void addTripleEvent(Graph g, Triple t) {
            triples.add(t);
        }

        @Override
        protected void deleteTripleEvent(Graph g, Triple t) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Suspicious deleting: {}", t);
            }
            triples.remove(t);
        }

        /**
         * Returns all added triples.
         *
         * @return a {@code Set} of {@link Triple}s
         */
        public Set<Triple> getTriples() {
            return Collections.unmodifiableSet(triples);
        }

        /**
         * Makes a {@link OWLTriples}-container, that contains the specified object and all collected triples.
         *
         * @param key {@link X} the {@link OWLObject}, not {@code null}
         * @param <X> any subtype of {@link OWLObject}
         * @return {@link OWLTriples}
         */
        public <X extends OWLObject> OWLTriples<X> toObject(X key) {
            return new OWLTriples<>(key, triples);
        }
    }
}
