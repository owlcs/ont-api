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
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An auxiliary class-container to provide
 * a common way for working with {@link OWLObject}s and {@link Triple}s all together.
 * It is logically based on the {@link ONTObject} container,
 * which is a wrapper around {@link OWLObject OWLObject} with the reference to get all associated {@link Triple RDF triple}s.
 * This class is used by the {@link InternalModel internal model} cache as indivisible bucket.
 *
 * @param <O> Component type: a subtype of {@link OWLAxiom} or {@link OWLAnnotation}
 */
@SuppressWarnings("WeakerAccess")
public class ObjectTriplesMapImpl<O extends OWLObject> implements ObjectTriplesMap<O> {
    protected final Class<O> type;
    protected final Map<O, ONTObject<O>> map;
    protected final InternalCache.Loading<O, Set<Triple>> triples;

    // a state flag that responds whether some axioms have been manually added to this map
    // the dangerous of manual added axioms is that the same information can be represented in different ways.
    protected boolean manualAdded;

    /**
     * @param type     {@code Class}
     * @param map      {@code Map}
     * @param parallel boolean to control internal triples cache
     */
    public ObjectTriplesMapImpl(Class<O> type, Map<O, ONTObject<O>> map, boolean parallel) {
        this.type = Objects.requireNonNull(type);
        this.map = Objects.requireNonNull(map);
        this.triples = InternalCache.createSoft(parallel).asLoading(this::loadTripleSet);
    }

    @Override
    public Class<O> type() {
        return type;
    }

    @Nonnull
    private Set<Triple> loadTripleSet(O key) {
        return find(key).map(s -> s.triples()
                .collect(Collectors.toSet())).orElse(Collections.emptySet());
    }

    /**
     * Adds the object-triple pair to this map.
     * If there is no triple-container for the specified object, or it is empty, or it is in-memory,
     * then a triple will be added to the inner set, otherwise appended to existing stream.
     *
     * @param key    OWLObject (axiom or annotation)
     * @param triple {@link Triple}
     */
    @Override
    public void add(O key, Triple triple) {
        ONTObject<O> res = find(key).map(o -> o.isEmpty() ? new TripleSet<>(o) : o).orElseGet(() -> new TripleSet<>(key));
        map.put(key, res.add(triple));
        fromCache(key).ifPresent(set -> set.add(triple));
        this.manualAdded = true;
    }

    /**
     * Removes the object-triple pair from the map.
     *
     * @param key    OWLObject (axiom or annotation)
     * @param triple {@link Triple}
     */
    @Override
    public void remove(O key, Triple triple) {
        find(key).ifPresent(o -> map.put(o.getObject(), o.delete(triple)));
        fromCache(key).ifPresent(set -> set.remove(triple));
    }

    /**
     * Removes the given object and all associated triples.
     *
     * @param key OWLObject (axiom or annotation)
     */
    @Override
    public void remove(O key) {
        triples.asCache().remove(key);
        map.remove(key);
    }

    protected Optional<Set<Triple>> fromCache(O key) {
        return Optional.ofNullable(get(key));
    }

    private Optional<ONTObject<O>> find(O key) {
        return Optional.ofNullable(map.get(key));
    }

    @Override
    public Set<Triple> get(O key) {
        return triples.get(key);
    }

    @Override
    public boolean contains(O o) {
        return map.containsKey(o);
    }

    @Override
    public Stream<O> objects() {
        return map.keySet().stream();
    }

    @Override
    public long size() {
        return map.size();
    }

    @Override
    public boolean hasNew() {
        return manualAdded;
    }

    /**
     * An {@link ONTObject} which holds triples in memory.
     * Used in caches.
     * Note: it is mutable object while the base is immutable.
     *
     * @param <V>
     */
    private class TripleSet<V extends O> extends ONTObject<V> {
        private final Set<Triple> triples;

        protected TripleSet(V object) { // empty
            this(object, new HashSet<>());
        }

        protected TripleSet(ONTObject<V> object) {
            this(object.getObject(), object.triples().collect(Collectors.toCollection(HashSet::new)));
        }

        private TripleSet(V object, Set<Triple> triples) {
            super(object);
            this.triples = triples;
        }

        @Override
        public Stream<Triple> triples() {
            return triples.stream();
        }

        @Override
        protected boolean isEmpty() {
            return triples.isEmpty();
        }

        @Override
        public ONTObject<V> add(Triple triple) {
            triples.add(triple);
            return this;
        }

        @Override
        public ONTObject<V> delete(Triple triple) {
            triples.remove(triple);
            return this;
        }
    }
}
