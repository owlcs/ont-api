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

import org.apache.jena.graph.GraphListener;
import org.apache.jena.graph.Triple;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An auxiliary class-container (bucket) to provide
 * a common way for working with {@link OWLObject}s and {@link Triple}s all together.
 * It is logically based on the {@link ONTObject} container,
 * that is a wrapper around classic {@link OWLObject OWLObject}
 * but with a possibility to get all associated {@link Triple RDF triple}s.
 * This class is used by the {@link InternalModel Internal Model} cache as indivisible bucket per axiom/object type.
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

    // to use either Caffeine (if true) or LHM based cache,
    // both are synchronized, but Caffeine works faster in multi-thread, and LHM in single-thread environment.
    private final boolean parallel;

    /**
     * Constructs a bucket instance.
     *
     * @param loader   a {@code Supplier} to load object-triples pairs, not {@code null}
     * @param parallel if {@code true} use caffeine cache, otherwise LHM based cache
     */
    public ObjectTriplesMapImpl(Supplier<Iterator<ONTObject<X>>> loader, boolean parallel) {
        this.loader = Objects.requireNonNull(loader);
        this.map = InternalCache.createSoft(ObjectTriplesMapImpl::loadMap, this.parallel = parallel);
    }

    protected CachedMap loadMap() {
        this.hasNew = false;
        Iterator<ONTObject<X>> it = loader.get();
        Map<X, ONTObject<X>> res = createMap();
        while (it.hasNext()) {
            ONTObject<X> v = it.next();
            res.merge(v.getObject(), v, ONTObject::append);
        }
        return new CachedMap(res);
    }

    /**
     * Creates a fresh {@code Map} that is used in caches.
     *
     * @param <K> key type
     * @param <V> value type
     * @return {@link Map}
     */
    protected <K, V> Map<K, V> createMap() {
        // Iteration over LHM is a little bit faster than iteration over HashMap.
        // On the other hand LHM requires more memory -
        // but we don't care about memory since all important things contains in the very Graph
        // and this is just a temporary storage which is always free for GC.
        return new LinkedHashMap<>();
    }

    public CachedMap getMap() {
        return map.get(this);
    }

    @Override
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
        // OWL-API-impl also stores objects in ArrayList before creating a Stream
        // In our case it extremely speeds up axioms listing (even faster than in OWL-API-impl)
        return getMap().getObjectsList().stream();
    }

    @Override
    public Stream<Triple> triples() {
        return getMap().getTriplesMap().keySet().stream();
    }

    @Override
    public Stream<Triple> triples(X o) throws JenaException {
        return getMap().getObjectsMap().get(o).triples();
    }

    @Override
    public boolean contains(X o) {
        return getMap().getObjectsMap().containsKey(o);
    }

    @Override
    public boolean contains(X o, Triple t) {
        CachedMap m;
        if (isLoaded() && (m = getMap()).hasTriplesMap()) {
            Set<X> res = m.getTriplesMap().get(t);
            return res != null && res.contains(o);
        }
        return triples(o).anyMatch(t::equals);
    }

    @Override
    public boolean contains(Triple triple) {
        return getMap().getTriplesMap().containsKey(triple);
    }

    @Override
    public GraphListener addListener(X key) {
        return new Listener<>(this, key);
    }

    /**
     * Registers the given object-triple pair into the map.
     * Note that each object, in general, is associated with many triples, not just one.
     * If a set of associated triples is incomplete the method {@link #triples(OWLObject)}
     * may throw a {@link JenaException jena exception}.
     * WARNING: Must be called only from the {@link Listener listener}.
     *
     * @param key    {@link X} (axiom or annotation)
     * @param triple {@link Triple}
     * @see #addListener(OWLObject)
     */
    public void register(X key, Triple triple) {
        this.hasNew = true;
        CachedMap map = getMap();
        map.getObjectsMap().merge(key, new TripleSet<>(key, triple), (a, b) -> {
            if (a.isDefinitelyEmpty()) return b;
            return a.append(b);
        });
        // operation 'Add' must be as quick as possible
        // since it is used while reading documents in native OWL-API formats
        if (map.hasTriplesMap()) {
            map.getTriplesMap().computeIfAbsent(triple, t -> new HashSet<>()).add(key);
        }
        if (map.hasObjectsList()) {
            // for a given object operations 'add' are sequential and isolated by R/W lock upwards
            List<X> list = map.getObjectsList();
            if (list.isEmpty() || !key.equals(list.get(list.size() - 1))) {
                list.add(key);
            }
        }
    }

    /**
     * Unregisters the given object-triple pair from this map.
     * Both the object and the triple may still be present in the map after this operation.
     * Impl note: an {@link InternalModel} uses this method only while <b>adding</b> an object.
     * It seems now this method is almost unused by the system, although such a situation,
     * when removing triple happens on adding object may still exist,
     * it depends on Jena and other ONT-API places.
     * For deleting the method {@link #delete(OWLObject)} is used.
     * The operation may broke structure and, therefore,
     * the method {@link #triples(OWLObject)} may throw {@link JenaException Jena Exception} in this case.
     * WARNING: Must be called only the {@link Listener listener}.
     *
     * @param key    OWLObject (axiom or annotation)
     * @param triple {@link Triple}
     * @see #addListener(OWLObject)
     */
    public void unregister(X key, Triple triple) {
        if (!isLoaded()) return;
        CachedMap map = getMap();
        Map<X, ONTObject<X>> objectsCache = map.getObjectsMap();
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
        if (map.hasTriplesMap()) {
            Map<Triple, Set<X>> triplesCache = map.getTriplesMap();
            Optional.ofNullable(triplesCache.get(triple)).ifPresent(set -> {
                set.remove(key);
                if (set.isEmpty()) {
                    triplesCache.remove(triple);
                }
            });
        }
        if (map.hasObjectsList()) {
            List<X> list = map.getObjectsList();
            int index;
            if (!list.isEmpty() && key.equals(list.get(index = (list.size() - 1)))) {
                list.remove(index);
            }
        }
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
        ONTObject<X> res = map.getObjectsMap().remove(key);
        if (map.hasTriplesMap()) {
            Map<Triple, Set<X>> triplesCache = map.getTriplesMap();
            res.triples().forEach(t -> Optional.ofNullable(triplesCache.get(t)).ifPresent(set -> {
                set.remove(res.getObject());
                if (set.isEmpty()) {
                    triplesCache.remove(t);
                }
            }));
        }
        if (map.hasObjectsList()) {
            List<X> list = map.getObjectsList();
            // must be in the end of list:
            for (int i = list.size() - 1; i >= 0; i--) {
                if (key.equals(list.get(i))) {
                    list.remove(i);
                    break;
                }
            }
        }
    }

    @Override
    public void clear() {
        map.asCache().clear();
    }

    /**
     * An internal object-collection
     * that holds {@code Map} with {@link X OWLObject}-keys, a {@code Map} with {@link Triple}-keys
     * and a {@code List} with {@link X OWLObject}s to conduct fast iterating;
     * the last two implemented as {@link java.lang.ref.SoftReference} based caches.
     */
    protected class CachedMap {
        protected final Map<X, ONTObject<X>> objectsMap;
        protected final InternalCache.Loading<CachedMap, List<X>> objectsListCache;
        protected final InternalCache.Loading<CachedMap, Map<Triple, Set<X>>> triplesCache;

        protected CachedMap(Map<X, ONTObject<X>> objects) {
            this.objectsMap = Objects.requireNonNull(objects);
            this.objectsListCache = InternalCache.createSoft(CachedMap::loadObjects, parallel);
            this.triplesCache = InternalCache.createSoft(CachedMap::loadTriples, parallel);
        }

        protected long size() {
            return objectsMap.size();
        }

        protected Map<X, ONTObject<X>> getObjectsMap() {
            return objectsMap;
        }

        protected boolean hasObjectsList() {
            return !objectsListCache.asCache().isEmpty();
        }

        protected boolean hasTriplesMap() {
            return !triplesCache.asCache().isEmpty();
        }

        protected Map<Triple, Set<X>> getTriplesMap() {
            return triplesCache.get(this);
        }

        protected List<X> getObjectsList() {
            return objectsListCache.get(this);
        }

        protected Map<Triple, Set<X>> loadTriples() {
            Map<Triple, Set<X>> res = createMap();
            for (ONTObject<X> v : objectsMap.values()) {
                try {
                    v.triples().forEach(t -> res.computeIfAbsent(t, x -> new HashSet<>()).add(v.getObject()));
                } catch (JenaException ex) {
                    // object has wrong state: it is being registered or unregistered
                    // ignore exception
                }
            }
            return res;
        }

        protected List<X> loadObjects() {
            return new ArrayList<>(objectsMap.keySet());
        }
    }

    /**
     * An {@link ONTObject} which holds triples in memory.
     * Used in caches.
     * Note: it is mutable object while the base is immutable.
     *
     * @param <V> any subtype of {@link OWLObject}
     */
    public static class TripleSet<V extends OWLObject> extends ONTObject<V> {
        protected final Set<Triple> triples;

        protected TripleSet(V object, Triple t) {
            this(object);
            this.triples.add(t);
        }

        protected TripleSet(V object) { // empty
            this(object, new HashSet<>());
        }

        protected TripleSet(V object, Set<Triple> triples) {
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

    /**
     * A {@link GraphListenerBase Graph Listener} implementation
     * that monitors the triples addition and deletion for the specified {@link O object}.
     *
     * @param <O> a subtype of {@link OWLObject}
     */
    public static class Listener<O extends OWLObject> extends GraphListenerBase {
        protected final ObjectTriplesMapImpl<O> store;
        protected final O object;

        protected Listener(ObjectTriplesMapImpl<O> store, O object) {
            this.store = Objects.requireNonNull(store);
            this.object = Objects.requireNonNull(object);
        }

        @Override
        protected void addEvent(Triple t) {
            store.register(object, t);
        }

        @Override
        protected void deleteEvent(Triple t) {
            store.unregister(object, t);
        }
    }

}
