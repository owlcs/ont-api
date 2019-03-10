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
import org.apache.jena.shared.JenaException;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An auxiliary class-container to provide
 * a common way for working with {@link OWLObject}s and {@link Triple}s all together.
 * It is logically based on the {@link ONTObject} container,
 * which is a wrapper around {@link OWLObject OWLObject}
 * with the reference to get all associated {@link Triple RDF triple}s.
 * This class is used by the {@link InternalModel Internal Model} cache as indivisible bucket.
 * <p>
 * Created by @ssz on 09.03.2019.
 *
 * @param <X> any subtype of {@link OWLObject} (in system either {@link OWLAxiom} or {@link OWLAnnotation})
 */
@SuppressWarnings("WeakerAccess")
public class ObjectTriplesMapImpl<X extends OWLObject> implements ObjectTriplesMap<X> {

    // objects provider:
    private final Supplier<Iterator<ONTObject<X>>> loader;
    // soft reference:
    private final InternalCache.Loading<ObjectTriplesMapImpl<X>, CachedMap> map;

    // a state flag that responds whether some axioms have been manually added to this map
    // the dangerous of manual added axioms is that the same information can be represented in different ways.
    private volatile boolean hasNew;

    public ObjectTriplesMapImpl(Supplier<Iterator<ONTObject<X>>> loader) {
        this.loader = Objects.requireNonNull(loader);
        this.map = InternalCache.createSoft(ObjectTriplesMapImpl::loadMap, true);
    }

    protected CachedMap loadMap() {
        this.hasNew = false;
        Iterator<ONTObject<X>> it = loader.get();
        Map<X, ONTObject<X>> res = new HashMap<>();
        while (it.hasNext()) {
            ONTObject<X> v = it.next();
            res.merge(v.getObject(), v, ONTObject::append);
        }
        return new CachedMap(res, null);
    }

    public CachedMap getMap() {
        return map.get(this);
    }

    public boolean isLoaded() {
        return !map.asCache().isEmpty();
    }

    @Override
    public void load() {
        getMap();
    }

    @Override
    public boolean hasNew() {
        return isLoaded() && hasNew;
    }

    @Override
    public Stream<X> objects() {
        return getMap().getObjects().keySet().stream();
    }

    @Override
    public Stream<Triple> triples() {
        return getMap().getTriples().keySet().stream();
    }

    @Override
    public Stream<Triple> triples(X o) throws JenaException {
        return getMap().getObjects().get(o).triples();
    }

    @Override
    public boolean contains(X o) {
        return getMap().getObjects().containsKey(o);
    }

    @Override
    public boolean contains(X o, Triple t) {
        CachedMap m;
        if (isLoaded() && (m = getMap()).hasTriplesMap()) {
            Set<X> res = m.getTriples().get(t);
            return res != null && res.contains(o);
        }
        return triples(o).anyMatch(t::equals);
    }

    @Override
    public boolean contains(Triple triple) {
        return getMap().getTriples().containsKey(triple);
    }

    /**
     * Adds the object-triple pair to this map.
     * If there is no triple-container for the specified object or it is in-memory,
     * then a triple will be added to the inner set, otherwise appended to existing stream.
     * <p>
     * WARNING: Must be called only from listener.
     *
     * @param key    {@link X} (axiom or annotation)
     * @param triple {@link Triple}
     */
    @Override
    public void register(X key, Triple triple) {
        this.hasNew = true;
        CachedMap map = getMap();
        map.getObjects().merge(key, new TripleSet<>(key, triple), (a, b) -> {
            if (a.isDefinitelyEmpty()) return b;
            return a.append(b);
        });
        map.getTriples().computeIfAbsent(triple, t -> new HashSet<>()).add(key);
    }

    /**
     * Removes the object-triple pair from the map.
     * <p>
     * WARNING: Must be called only from listener.
     * Note (1): the operation may broke structure and, therefore,
     * the method {@link #triples(OWLObject)} may throw {@link JenaException} in this case.
     * Note (2): seems now this method is unused by the system.
     *
     * @param key    OWLObject (axiom or annotation)
     * @param triple {@link Triple}
     */
    @Override
    public void unregister(X key, Triple triple) {
        if (!isLoaded()) return;
        CachedMap map = getMap();
        Map<X, ONTObject<X>> objectsCache = map.getObjects();
        Optional.ofNullable(objectsCache.get(key)).ifPresent(v -> {
            ONTObject<X> x = v.delete(triple);
            objectsCache.put(x.getObject(), x);
            try {
                if (x.isDefinitelyEmpty() || x.triples().count() == 0) {
                    objectsCache.remove(x.getObject());
                }
            } catch (JenaException e) {
                // incomplete object
            }
        });
        if (!map.hasTriplesMap()) return;
        Map<Triple, Set<X>> triplesCache = map.getTriples();
        Optional.ofNullable(triplesCache.get(triple)).ifPresent(set -> {
            set.remove(key);
            if (set.isEmpty()) {
                triplesCache.remove(triple);
            }
        });
    }

    /**
     * Deletes the given object and all its associated triples.
     *
     * @param key {@link X} (axiom or annotation)
     */
    @Override
    public void delete(X key) {
        if (!isLoaded()) return;
        CachedMap map = getMap();
        ONTObject<X> res = map.getObjects().remove(key);
        if (!map.hasTriplesMap()) return;
        Map<Triple, Set<X>> triplesCache = map.getTriples();
        res.triples().forEach(t -> Optional.ofNullable(triplesCache.get(t)).ifPresent(set -> {
            set.remove(res.getObject());
            if (set.isEmpty()) {
                triplesCache.remove(t);
            }
        }));
    }

    @Override
    public void clear() {
        map.asCache().clear();
    }

    protected class CachedMap {
        private final Map<X, ONTObject<X>> objectsCache;
        private final InternalCache.Loading<CachedMap, Map<Triple, Set<X>>> triplesCache;

        CachedMap(Map<X, ONTObject<X>> objectsCache, Map<Triple, Set<X>> triplesCache) {
            this.objectsCache = Objects.requireNonNull(objectsCache);
            this.triplesCache = InternalCache.createSoft(CachedMap::loadMap, true);
            if (triplesCache != null) {
                this.triplesCache.asCache().put(this, triplesCache);
            }
        }

        long size() {
            return objectsCache.size();
        }

        Map<X, ONTObject<X>> getObjects() {
            return objectsCache;
        }

        boolean hasTriplesMap() {
            return !triplesCache.asCache().isEmpty();
        }

        Map<Triple, Set<X>> getTriples() {
            return triplesCache.get(this);
        }

        private Map<Triple, Set<X>> loadMap() {
            Map<Triple, Set<X>> res = new HashMap<>();
            for (ONTObject<X> v : objectsCache.values()) {
                try {
                    v.triples().forEach(t -> res.computeIfAbsent(t, x -> new HashSet<>()).add(v.getObject()));
                } catch (JenaException ex) {
                    // object has wrong state: it is being registered or unregistered
                    // ignore exception
                }
            }
            return res;
        }
    }


    /**
     * An {@link ONTObject} which holds triples in memory.
     * Used in caches.
     * Note: it is mutable object while the base is immutable.
     *
     * @param <V>
     */
    private class TripleSet<V extends X> extends ONTObject<V> {
        private final Set<Triple> triples;

        TripleSet(V object, Triple t) {
            this(object);
            this.triples.add(t);
        }

        TripleSet(V object) { // empty
            this(object, new HashSet<>());
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
        protected boolean isDefinitelyEmpty() {
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
