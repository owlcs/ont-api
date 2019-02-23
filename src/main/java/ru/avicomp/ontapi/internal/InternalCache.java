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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The simplest common cache-adapter interface.
 * <p>
 * This interface is also used as a factory and as the only place to access to a particular caches.
 * Currently, it includes two implementations: a LRU {@code LinkedHashMap} based cache,
 * which has the best performance in a single tread environment,
 * and a {@link java.util.concurrent.ConcurrentHashMap} based {@link Cache Caffeine Cache},
 * which has good benchmarks both in multi-thread and single-thread environments.
 * <p>
 * In general, this cache-wrapper is not thread-safe.
 * But the upper-system uses R/W lock for any accessors,
 * and, therefore, the data on which this cache relies does not change in the process of reading,
 * whatever single- or multi- thread environment is used it.
 * <p>
 * Created by @ssz on 18.02.2019.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 * @since 1.4.0
 */
public interface InternalCache<K, V> {

    /**
     * Associates the {@code value} with the {@code key} in this cache. If the cache previously
     * contained a value associated with the {@code key}, the old value is replaced by the new {@code value}.
     *
     * @param key   {@link K}, the key with which the specified value is to be associated
     * @param value {@link V}, value to be associated with the specified key
     */
    void put(K key, V value);

    /**
     * Returns the value associated with the {@code key} in this cache, or {@code null} if there is no
     * cached value for the {@code key}.
     *
     * @param key {@link K} the key whose associated value is to be returned
     * @return {@link V} the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key
     */
    V get(K key);

    /**
     * Returns the current number of cached keys.
     *
     * @return long, the number of keys in this cache (not necessary key-value pairs)
     */
    long size();

    /**
     * Discards all entries in the cache.
     */
    void clear();

    /**
     * Returns the value associated with the {@code key} in this cache,
     * obtaining that value from the {@code mappingFunction} if necessary.
     * This method provides a simple substitute for the conventional
     * "if cached, return; otherwise create, cache and return" pattern.
     *
     * @param key             the key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with the specified key,
     * or {@code null} if the computed value is {@code null}
     */
    default V get(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V res;
        if ((res = get(key)) != null) {
            return res;
        }
        if ((res = mappingFunction.apply(key)) != null) {
            put(key, res);
            return res;
        }
        return res;
    }

    /**
     * Represents this cache as {@link Loading Loading Cache}.
     *
     * @param loader  {@link Function} the function to compute a value if it absence in the cache
     * @param <Key>   the type of keys maintained by the return cache
     * @param <Value> the type of mapped values
     * @return {@link Loading}
     */
    default <Key extends K, Value extends V> Loading<Key, Value> asLoading(Function<? super Key, ? extends Value> loader) {
        Objects.requireNonNull(loader);
        @SuppressWarnings("unchecked")
        InternalCache<Key, Value> self = (InternalCache<Key, Value>) this;
        return new Loading<Key, Value>() {
            @Override
            public Value get(Key key) {
                return self.get(key, loader);
            }

            @Override
            public InternalCache<Key, Value> asCache() {
                return self;
            }
        };
    }

    /**
     * Creates a bounded LRU cache,
     * that wraps either {@link Cache Caffeine} or simple {@link LinkedHashMap} based cache.
     *
     * @param parallel boolean factor, if {@code true} a caffeine cache will be created, otherwise - LHM based cache
     * @param size     int the maximum size of the cache
     * @param <K>      the type of keys maintained by the return cache
     * @param <V>      the type of mapped values
     * @return {@link InternalCache}
     */
    static <K, V> InternalCache<K, V> createBounded(boolean parallel, long size) {
        if (parallel) {
            return fromCaffeine(Caffeine.newBuilder().maximumSize(size).build());
        }
        return fromMap(new LinkedHashMap<K, V>((int) size, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > size;
            }
        });
    }

    /**
     * Wraps the given {@code Map} as {@code InternalCache}.
     *
     * @param map {@link Map}
     * @param <K> the type of keys maintained by the return cache
     * @param <V> the type of mapped values
     * @return {@link InternalCache}
     */
    static <K, V> InternalCache<K, V> fromMap(Map<K, V> map) {
        Objects.requireNonNull(map);
        return new InternalCache<K, V>() {
            @Override
            public void put(K key, V value) {
                map.put(key, value);
            }

            @Override
            public V get(K key) {
                return map.get(key);
            }

            @Override
            public long size() {
                return map.size();
            }

            @Override
            public void clear() {
                map.clear();
            }
        };
    }

    /**
     * Wraps the given Caffeine {@code Cache} as {@code InternalCache}.
     * Note: the impl does not override {@link Cache#get(Object, Function)} method for performance reasons.
     *
     * @param cache {@link Cache}
     * @param <K>   the type of keys maintained by the return cache
     * @param <V>   the type of mapped values
     * @return {@link InternalCache}
     */
    static <K, V> InternalCache<K, V> fromCaffeine(Cache<K, V> cache) {
        Objects.requireNonNull(cache);
        return new InternalCache<K, V>() {
            @Override
            public void put(K key, V value) {
                cache.put(key, value);
            }

            @Override
            public V get(K key) {
                return cache.getIfPresent(key);
            }

            @Override
            public long size() {
                return cache.asMap().size();
            }

            @Override
            public void clear() {
                cache.invalidateAll();
            }
        };
    }

    /**
     * Loading cache.
     * Values are automatically loaded by the cache,
     * and are stored in the cache until either evicted or manually invalidated.
     *
     * @param <K> the type of keys maintained by this cache
     * @param <V> the type of mapped values
     */
    interface Loading<K, V> {
        /**
         * Returns the value associated with the {@code key} in this cache, obtaining that value from internal factory.
         *
         * @param key {@link K} key with which the specified value is to be associated
         * @return {@link V} the current (existing or computed) value associated with the specified key,
         * or {@code null} if the computed value is {@code null}
         */
        V get(K key);

        /**
         * Answers a {@link InternalCache} view of this cache.
         *
         * @return {@link InternalCache}
         */
        InternalCache<K, V> asCache();
    }

}
