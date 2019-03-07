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

package ru.avicomp.ontapi.internal;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Created by @ssz on 07.03.2019.
 */
public interface ObjectTriplesMap<O extends OWLObject> {

    Class<O> type();

    boolean hasNew();

    boolean contains(O o);

    Stream<O> objects();

    Set<Triple> get(O o);

    void add(O key, Triple triple);

    void remove(O key, Triple triple);

    void remove(O key);

    long size();

    default boolean contains(Triple triple) {
        return objects().anyMatch(o -> get(o).contains(triple));
    }

    /**
     * Creates a graph listener that handles adding/removing axioms and header annotations
     * through top-level OWL-API {@link org.semanticweb.owlapi.model.OWLOntology} interface.
     *
     * @param map {@link ObjectTriplesMap}
     * @param obj {@link OWLObject}
     * @param <X> either {@link OWLAnnotation} or {@link OWLAxiom}
     * @return {@link Listener} new instance
     */
    static <X extends OWLObject> Listener<X> createListener(ObjectTriplesMap<X> map, X obj) {
        return new Listener<>(map, obj);
    }

    /**
     * The listener that monitors the addition and deletion of axioms and header annotations.
     *
     * @param <O> {@link OWLAxiom} in our case.
     */
    class Listener<O extends OWLObject> extends GraphListenerBase {
        private final ObjectTriplesMap<O> store;
        private final O object;

        Listener(ObjectTriplesMap<O> store, O object) {
            this.store = store;
            this.object = object;
        }

        @Override
        protected void addEvent(Triple t) {
            store.add(object, t);
        }

        @Override
        protected void deleteEvent(Triple t) {
            store.remove(object, t);
        }
    }
}
