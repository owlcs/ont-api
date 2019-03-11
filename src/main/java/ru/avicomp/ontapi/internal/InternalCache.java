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
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The simplest common Cache Adapter interface for internal use.
 * <p>
 * Also, it is a factory to produce various {@link InternalCache} implementations,
 * and the only place in system to access to a particular external caches.
 * Currently it includes only two kind of implementations: a LRU {@code LinkedHashMap} based cache,
 * which has the best performance in a single thread environment,
 * and a {@link java.util.concurrent.ConcurrentHashMap} based {@link Cache Caffeine Cache},
 * which has good benchmarks both in multi-thread and single-thread environments.
 * <p>
 * In general case, an implementation of this cache-adapter may not guarantee full thread safety.
 * This is optimization for conditions in which this cache is used: the upper-system uses R/W lock for any accessors,
 * and, therefore, the data on which the cache should rely does not change in the process of reading,
 * whatever single- or multi- thread environment is used it.
 * This fact allows to make some read operations to be simpler and a little bit faster,
 * then it would be with direct use particular caches.
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
     * Discards any cached value for the {@code key}.
     *
     * @param key {@link K}
     */
    void remove(K key);

    /**
     * Discards all entries in the cache.
     */
    void clear();

    /**
     * Answers {@code true} if the cache is empty.
     *
     * @return boolean
     */
    boolean isEmpty();

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
     * Creates a fake empty {@code InternalCache}, that does not contain anything.
     * Can be used to debug and to disable caching according to configuration settings.
     *
     * @param <K> the type of keys maintained by the return cache
     * @param <V> the type of mapped values
     * @return {@link InternalCache}
     */
    static <K, V> InternalCache<K, V> createEmpty() {
        return new InternalCache<K, V>() {
            @Override
            public void put(K key, V value) {
                // nothing
            }

            @Override
            public V get(K key) {
                return null;
            }

            @Override
            public void remove(K key) {
                // nothing
            }

            @Override
            public void clear() {
                // nothing
            }

            @Override
            public boolean isEmpty() {
                // contains everything:
                return false;
            }

            @Override
            public V get(K key, Function<? super K, ? extends V> mappingFunction) {
                return Objects.requireNonNull(mappingFunction).apply(key);
            }
        };
    }

    /**
     * Creates a bounded LRU cache,
     * that wraps either {@link Cache Caffeine} or simple {@link LinkedHashMap} based cache.
     *
     * @param caffeine boolean factor, if {@code true} a caffeine cache will be created,
     *                 otherwise - a LHM based cache
     * @param size     int the maximum size of the cache
     * @param <K>      the type of keys maintained by the return cache
     * @param <V>      the type of mapped values
     * @return {@link InternalCache}
     */
    static <K, V> InternalCache<K, V> createBounded(boolean caffeine, long size) {
        if (caffeine) {
            return new CaffeineWrapper<>(Caffeine.newBuilder().maximumSize(size).build());
        }
        return new MapWrapper<>(new LinkedHashMap<K, V>((int) size, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > size;
            }
        });
    }

    /**
     * Creates a bounded LRU loading cache,
     * that wraps either {@link Cache Caffeine} or simple {@link LinkedHashMap} based cache.
     *
     * @param loader   a {@link Function}-loaded to obtain a value if it absence in the cache
     * @param caffeine boolean factor, if {@code true} a caffeine cache will be created,
     *                 otherwise - a LHM based cache
     * @param size     int the maximum size of the cache
     * @param <K>      the type of keys maintained by the return cache
     * @param <V>      the type of mapped values
     * @return {@link Loading}
     */
    static <K, V> Loading<K, V> createBounded(Function<? super K, ? extends V> loader,
                                              boolean caffeine,
                                              long size) {
        InternalCache<K, V> res = caffeine ?
                new CaffeineWrapper<>(Caffeine.newBuilder().maximumSize(size).build(loader::apply), loader) :
                new MapWrapper<>(new LinkedHashMap<K, V>((int) size, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                        return size() > size;
                    }
                });
        return res.asLoading(loader);
    }

    /**
     * Creates an unbounded LRU cache with soft reference values,
     * that wraps either {@link Cache Caffeine} or simple {@link LinkedHashMap} based cache.
     *
     * @param caffeine boolean factor, if {@code true} a caffeine cache will be created,
     *                 otherwise - a LHM based cache
     * @param <K>      the type of keys maintained by the return cache
     * @param <V>      the type of mapped values
     * @return {@link InternalCache}
     */
    static <K, V> InternalCache<K, V> createSoft(boolean caffeine) {
        if (caffeine) {
            return new CaffeineWrapper<>(Caffeine.newBuilder().softValues().build());
        }
        return new SoftMapWrapper<>(new LinkedHashMap<>(128, 0.75f, true));
    }

    /**
     * Creates an unbounded LRU loading cache with soft reference values,
     * that wraps either {@link Cache Caffeine} or simple {@link LinkedHashMap} based cache.
     *
     * @param loader   a {@link Function}-loaded to obtain a value if it absence in the cache
     * @param caffeine boolean factor, if {@code true} a caffeine cache will be created,
     *                 otherwise - a LHM based cache
     * @param <K>      the type of keys maintained by the return cache
     * @param <V>      the type of mapped values
     * @return {@link Loading}
     */
    static <K, V> Loading<K, V> createSoft(Function<? super K, ? extends V> loader,
                                           boolean caffeine) {
        InternalCache<K, V> res = caffeine ?
                new CaffeineWrapper<>(Caffeine.newBuilder().softValues().build(loader::apply), loader) :
                new SoftMapWrapper<>(new LinkedHashMap<>(128, 0.75f, true));
        return res.asLoading(loader);
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
         * Returns the value associated with the {@code key} in this cache,
         * obtaining that value from internal factory if necessary.
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

    /**
     * A {@code InternalCache} implementation that wraps a {@code Map} with {@link SoftReference} values.
     * It is partially synchronized: only read operations are not thread safe.
     *
     * @param <K> the type of keys maintained by this cache
     * @param <V> the type of mapped values
     */
    @SuppressWarnings("WeakerAccess")
    class SoftMapWrapper<K, V> implements InternalCache<K, V> {
        protected final Map<K, SoftReference<V>> map;

        protected SoftMapWrapper(Map<K, SoftReference<V>> map) {
            this.map = Objects.requireNonNull(map);
        }

        @Override
        public void put(K key, V value) {
            synchronized (map) {
                map.put(key, new SoftReference<>(value));
            }
        }

        @Override
        public V get(K key) {
            SoftReference<V> res = map.get(key);
            return res == null ? null : res.get();
        }

        @Override
        public void remove(K key) {
            synchronized (map) {
                map.remove(key);
            }
        }

        @Override
        public void clear() {
            synchronized (map) {
                map.clear();
            }
        }

        @Override
        public boolean isEmpty() {
            if (map.isEmpty()) return true;
            synchronized (map) {
                if (map.isEmpty()) return true;
                for (K k : map.keySet()) {
                    if (get(k) != null) return false;
                }
                return true;
            }
        }

        @Override
        public V get(K key, Function<? super K, ? extends V> mappingFunction) {
            Objects.requireNonNull(mappingFunction);
            V res;
            if ((res = get(key)) != null) {
                return res;
            }
            synchronized (map) {
                if ((res = get(key)) != null) {
                    return res;
                }
                if ((res = mappingFunction.apply(key)) != null) {
                    put(key, res);
                }
            }
            return res;
        }
    }

    /**
     * A {@code InternalCache} implementations that wraps the standard {@code Map}.
     * It is partially synchronized: only read operations are not safe.
     *
     * @param <K> the type of keys maintained by this cache
     * @param <V> the type of mapped values
     */
    @SuppressWarnings("WeakerAccess")
    class MapWrapper<K, V> implements InternalCache<K, V> {
        protected final Map<K, V> map;

        protected MapWrapper(Map<K, V> map) {
            this.map = map;
        }

        @Override
        public void put(K key, V value) {
            synchronized (map) {
                map.put(key, value);
            }
        }

        @Override
        public V get(K key) {
            return map.get(key);
        }

        @Override
        public void remove(K key) {
            synchronized (map) {
                map.remove(key);
            }
        }

        @Override
        public void clear() {
            synchronized (map) {
                map.clear();
            }
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public V get(K key, Function<? super K, ? extends V> mappingFunction) {
            Objects.requireNonNull(mappingFunction);
            V res;
            if ((res = get(key)) != null) {
                return res;
            }
            synchronized (map) {
                if ((res = get(key)) != null) {
                    return res;
                }
                if ((res = mappingFunction.apply(key)) != null) {
                    put(key, res);
                }
            }
            return res;
        }
    }

    /**
     * A {@code InternalCache} implementations that wraps the {@link Cache Caffeine Cache}.
     *
     * @param <K> the type of keys maintained by this cache
     * @param <V> the type of mapped values
     */
    @SuppressWarnings("WeakerAccess")
    class CaffeineWrapper<K, V> implements InternalCache<K, V> {
        protected final Cache<K, V> cache;
        protected final Function<? super K, ? extends V> embeddedLoader;

        protected CaffeineWrapper(LoadingCache<K, V> cache, Function<? super K, ? extends V> loader) {
            this.cache = Objects.requireNonNull(cache);
            this.embeddedLoader = Objects.requireNonNull(loader);
        }

        protected CaffeineWrapper(Cache<K, V> cache) {
            this.cache = Objects.requireNonNull(cache);
            this.embeddedLoader = null;
        }

        @Override
        public void put(K key, V value) {
            cache.put(key, value);
        }

        @Override
        public V get(K key) {
            return cache.getIfPresent(key);
        }

        @Override
        public void remove(K key) {
            cache.invalidate(key);
        }

        @Override
        public void clear() {
            cache.invalidateAll();
        }

        @Override
        public boolean isEmpty() {
            return cache.asMap().isEmpty();
        }

        @Override
        public V get(K key, Function<? super K, ? extends V> mappingFunction) {
            return cache.get(key, mappingFunction);
        }

        @Override
        public <Key extends K, Value extends V> Loading<Key, Value> asLoading(Function<? super Key, ? extends Value> loader) {
            Objects.requireNonNull(loader);
            @SuppressWarnings("unchecked")
            InternalCache<Key, Value> self = (InternalCache<Key, Value>) this;
            if (cache instanceof LoadingCache && this.embeddedLoader.equals(loader)) {
                return new Loading<Key, Value>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public Value get(Key key) {
                        return (Value) ((LoadingCache<K, V>) cache).get(key);
                    }

                    @Override
                    public InternalCache<Key, Value> asCache() {
                        return self;
                    }
                };
            }
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
    }
}
