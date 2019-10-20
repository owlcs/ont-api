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

package ru.avicomp.ontapi.config;

/**
 * A common interface to control cache settings.
 * <p>
 * Created by @szz on 05.03.2019.
 *
 * @param <R> config, either {@link OntConfig} (this instance) or {@link OntLoaderConfiguration} (a copied instance)
 * @since 1.4.0
 */
interface CacheControl<R> extends CacheSettings {

    /**
     * Sets a new maximum nodes cache size to the specified positive number
     * or disables nodes caching at all in case of non-positive number.
     *
     * @param size int
     * @return {@link R}
     * @see CacheSettings#getLoadNodesCacheSize()
     * @see OntSettings#ONT_API_LOAD_CONF_CACHE_NODES
     */
    R setLoadNodesCacheSize(int size);

    /**
     * Sets a new maximum objects cache size to the specified positive number
     * or disables objects caching at all in case of non-positive number.
     *
     * @param size int
     * @return {@link R}
     * @see CacheSettings#getLoadObjectsCacheSize()
     * @see OntSettings#ONT_API_LOAD_CONF_CACHE_OBJECTS
     */
    R setLoadObjectsCacheSize(int size);

    /**
     * Sets the model content cache level to the specified integer value.
     * The number {@code 0} means disabling all model's caches.
     * <p>
     * The content cache consists of several levels:
     * <ul>
     *     <li>{@link CacheSettings#CACHE_ITERATOR}</li>
     *     <li>{@link CacheSettings#CACHE_COMPONENT}</li>
     *     <li>{@link CacheSettings#CACHE_CONTENT}</li>
     *     <li>{@link CacheSettings#CACHE_ALL}</li>
     * </ul>
     *
     * @param level int, a non-negative number, preferably power of {@code 2}
     * @return {@link R}
     * @throws IllegalArgumentException in case the input is a negative number
     * @see CacheSettings#getModelCacheLevel()
     * @see OntSettings#ONT_API_LOAD_CONF_CACHE_MODEL
     */
    R setModelCacheLevel(int level);

    /**
     * Turns on/off the content cache use.
     * Other cache settings are untouched,
     * which means if there is a component cache enabled, it will remain enabled,
     * if its constant do not equal to the given.
     *
     * @param constant a non-negative int number
     * @param b        {@code true} to turn on, {@code false} to turn off
     * @return {@link R}
     * @see CacheSettings#CACHE_CONTENT
     * @see CacheSettings#CACHE_COMPONENT
     * @see CacheSettings#CACHE_ITERATOR
     * @see CacheSettings#CACHE_ALL
     */
    default R setModelCacheLevel(int constant, boolean b) {
        int current = getModelCacheLevel();
        return setModelCacheLevel(b ? current | constant : current & ~constant);
    }

    /**
     * Sets the model content cache level to the specified integer value.
     *
     * @param level non-negative integer
     * @return {@link R}
     * @deprecated since 1.4.2: use {@link #setModelCacheLevel(int)}
     */
    @Deprecated
    default R setContentCacheLevel(int level) {
        return setModelCacheLevel(level);
    }

    /**
     * Turns on/off the content cache.
     *
     * @param b boolean
     * @return {@link R}
     * @see CacheSettings#useContentCache()
     * @deprecated since 1.4.2: bad naming, use {@link #setModelCacheLevel(int, boolean)}
     */
    @Deprecated
    default R setUseContentCache(boolean b) {
        return setModelCacheLevel(CACHE_ALL, b);
    }

}
