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

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The simplest common cache-adapter interface.
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
     * Returns the current size of cache.
     *
     * @return long, the number of keys in this cache (not necessary key-value pairs)
     */
    long size();

    /**
     * Returns the value associated with the {@code key} in this cache, obtaining that value from the
     * {@code mappingFunction} if necessary. This method provides a simple substitute for the
     * conventional "if cached, return; otherwise create, cache and return" pattern.
     *
     * @param key             the key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with the specified key, or null if
     * the computed value is null
     */
    default V get(K key, Supplier<? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V res;
        if ((res = get(key)) != null) {
            return res;
        }
        if ((res = mappingFunction.get()) != null) {
            put(key, res);
            return res;
        }
        return res;
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
        };
    }


}
