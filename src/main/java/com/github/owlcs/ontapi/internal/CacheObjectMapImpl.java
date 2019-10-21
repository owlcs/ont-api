/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
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

import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An auxiliary class-container (bucket) to provide
 * a common way for working with {@link OWLObject}s and {@link Triple}s all together.
 * It is logically based on the {@link ONTObject} container,
 * that is a wrapper around classic {@link OWLObject OWL-API Object}
 * but with a possibility to get all associated {@link Triple Jena RDF Triple}s.
 * In the {@link InternalModel Internal Model}
 * instances of this class are used as indivisible buckets per axiom/object type.
 * <p>
 * Created by @ssz on 09.03.2019.
 *
 * @param <X> any subtype of {@link OWLObject} (in the system it is either {@link OWLAxiom} or {@link OWLAnnotation})
 */
@SuppressWarnings("WeakerAccess")
public class CacheObjectMapImpl<X extends OWLObject> implements ObjectMap<X> {

    // objects provider:
    private final Supplier<Iterator<ONTObject<X>>> loader;
    // soft reference:
    private final InternalCache.Loading<CacheObjectMapImpl<X>, CachedMap<X, ONTObject<X>>> map;

    // a state flag that responds whether some axioms have been manually added to this map
    // the dangerous of manual added axioms is that the same information can be represented in different ways.
    private volatile boolean hasNew;

    // to control cache loading:
    // if true, then checking for duplicates and merging is performed,
    // otherwise possible duplicates will be overwritten
    private final boolean withMerge;

    // to use either Caffeine (if true) or LHM based cache,
    // both are synchronized, but Caffeine works faster in multi-thread, and LHM in single-thread environment.
    private final boolean parallel;
    // to control key-iteration
    private final boolean fastIterator;

    @SuppressWarnings("unused")
    public CacheObjectMapImpl(Supplier<Iterator<ONTObject<X>>> loader, boolean parallel) {
        this(loader, true, parallel, true);
    }

    /**
     * Constructs a bucket instance.
     *
     * @param loader       a {@code Supplier} to load object-triples pairs, not {@code null}
     * @param withMerge    if {@code true} merging is performed while loading cache,
     *                     otherwise the source is assumed to be distinct
     * @param parallel     if {@code true} use caffeine cache, otherwise LHM based cache
     * @param fastIterator if {@code true} use Array-based cache to speedup iteration over {@link X}-keys
     */
    public CacheObjectMapImpl(Supplier<Iterator<ONTObject<X>>> loader,
                              boolean withMerge,
                              boolean parallel,
                              boolean fastIterator) {
        this.loader = Objects.requireNonNull(loader);
        this.withMerge = withMerge;
        this.parallel = parallel;
        this.fastIterator = fastIterator;
        this.map = InternalCache.createSoftSingleton(CacheObjectMapImpl::loadMap);
    }

    /**
     * Loads the cache into memory using {@link #loader}.
     *
     * @return {@link CachedMap}
     */
    protected CachedMap<X, ONTObject<X>> loadMap() {
        this.hasNew = false;
        Iterator<ONTObject<X>> it = loader.get();
        Map<X, ONTObject<X>> res = createMap();
        if (withMerge) {
            while (it.hasNext()) {
                WithMerge.add(res, it.next());
            }
            return CachedMap.create(res, WithMerge.getMerger(), parallel);
        }
        while (it.hasNext()) {
            ONTObject<X> v = it.next();
            res.put(v.getOWLObject(), v);
        }
        return CachedMap.create(res, null, parallel);
    }

    /**
     * Creates a fresh {@code Map} that is used in caches.
     *
     * @param <K> key type
     * @param <V> value type
     * @return {@link Map}
     */
    protected <K, V> Map<K, V> createMap() {
        if (parallel && !fastIterator) {
            // use ConcurrentMap to ensure the objects list will not be broken by some mutation
            return new ConcurrentHashMap<>();
        }
        // Iteration over LHM is a little bit faster than iteration over HashMap.
        // On the other hand LHM requires more memory -
        // but memory consumption doesn't really bother me since all important things contains in the very Graph
        // and this is just a temporary storage which is always free for GC.
        return new LinkedHashMap<>();
    }

    protected CachedMap<X, ONTObject<X>> getMap() {
        return map.get(this);
    }

    @Override
    public boolean isLoaded() {
        return !map.isEmpty();
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
    public Stream<X> keys() {
        if (fastIterator) {
            // OWL-API-impl also stores all objects in ArrayList before creating a Stream
            // In our case it extremely speeds up axioms listing (even faster than in OWL-API-impl)
            return getMap().getKeys().stream();
        }
        return getMap().asMap().keySet().stream();
    }

    @Override
    public Stream<ONTObject<X>> values() {
        return getMap().asMap().values().stream();
    }

    @Override
    public long count() {
        return getMap().asMap().size();
    }

    /**
     * {@inheritDoc}
     * It is used directly by OWL-API interfaces.
     *
     * @param o {@link X} key-object, not {@code null}
     * @return boolean
     */
    @Override
    public boolean contains(X o) {
        return getMap().contains(o);
    }

    /**
     * Deletes the given object and all its associated triples.
     *
     * @param key {@link X} (axiom or annotation)
     */
    @Override
    public void remove(X key) {
        if (!isLoaded()) return;
        getMap().remove(key);
    }

    @Override
    public void add(ONTObject<X> value) {
        getMap().put(value.getOWLObject(), value);
        hasNew = true;
    }

    @Override
    public ONTObject<X> get(X key) {
        return getMap().get(key);
    }

    @Override
    public void clear() {
        map.clear();
    }

    /**
     * An internal map-object that holds true-{@code Map} with {@link K}-keys and {@link V}-values.
     * It has the dedicated cache implemented as {@link java.lang.ref.SoftReference}
     * for the map keys to provide fast iteration, this is the only difference with the standard map.
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     */
    public static class CachedMap<K, V> {
        protected final Map<K, V> map;
        protected final BiFunction<V, V, V> merger;
        protected final InternalCache.Loading<CachedMap, List<K>> keys;

        protected CachedMap(Map<K, V> objects,
                            InternalCache.Loading<CachedMap, List<K>> cache,
                            BiFunction<V, V, V> merger) {
            this.map = Objects.requireNonNull(objects);
            this.keys = Objects.requireNonNull(cache);
            this.merger = merger;
        }

        /**
         * Creates a ready-to-use {@code CacheMap} instance.
         *
         * @param map      {@code Map} to wrap
         * @param merger   a Function to perform merge operations
         * @param parallel a boolean flag that is used to create {@link InternalCache} instance
         * @param <K>      the type of keys maintained by this map
         * @param <V>      the type of mapped values
         * @return {@link CachedMap} instance
         */
        public static <K, V> CachedMap<K, V> create(Map<K, V> map,
                                                    BiFunction<V, V, V> merger,
                                                    boolean parallel) {
            InternalCache.Loading<CachedMap, List<K>> keys = InternalCache.createSoftSingleton(m -> {
                if (!parallel) {
                    return new ArrayList<>(map.keySet());
                }
                // R/W lock does not guarantee thread-safety in multithreading,
                // since iterator go beyond a locked-block where it has been initialized,
                // but (I believe), R/W locking reduces the List's mutation costs
                return new CopyOnWriteArrayList<>(map.keySet());
            });
            return new CachedMap<>(map, keys, merger);
        }

        /**
         * Returns the number of key-value mappings in this map.
         *
         * @return long
         */
        public long size() {
            return map.size();
        }

        /**
         * Represents this map as a true java {@code Map}
         *
         * @return {@link Map}
         */
        public Map<K, V> asMap() {
            return map;
        }

        /**
         * Represents all keys as a {@code List}.
         * Note: when calling it will collect the cache if it is absent,
         * so, for big collections, some delay is expected.
         *
         * @return {@code List} of {@link K}s
         */
        public List<K> getKeys() {
            return keys.get(this);
        }

        /**
         * Removes the mapping for a key from this map if it is present.
         *
         * @param key {@link K}
         */
        public void remove(K key) {
            if (map.remove(key) == null) {
                return;
            }
            if (keys.isEmpty()) {
                return;
            }
            List<K> list = keys.get(this);
            // must be in the end of the list:
            for (int i = list.size() - 1; i >= 0; i--) {
                if (key.equals(list.get(i))) {
                    list.remove(i);
                    break;
                }
            }
        }

        /**
         * Associates the specified value with the specified key in this map.
         *
         * @param key   key with which the specified value is to be associated
         * @param value value to be associated with the specified key
         */
        public void put(K key, V value) {
            if (merger != null) {
                map.merge(key, value, merger);
            } else {
                map.put(key, value);
            }
            if (keys.isEmpty()) {
                return;
            }
            List<K> list = keys.get(this);
            if (map.size() - 1 != list.size()) {
                return;
            }
            list.add(key);
        }

        /**
         * Returns {@code true} if this map contains a mapping for the specified {@code key}.
         *
         * @param key key whose presence in this map is to be tested
         * @return boolean
         */
        public boolean contains(K key) {
            return map.containsKey(key);
        }

        /**
         * Returns the value to which the specified key is mapped,
         * or {@code null} if this map contains no mapping for the key.
         *
         * @param key the key whose associated value is to be returned
         * @return the value to which the specified key is mapped,
         * or {@code null} if this map contains no mapping for the key
         */
        public V get(K key) {
            return map.get(key);
        }
    }

}
