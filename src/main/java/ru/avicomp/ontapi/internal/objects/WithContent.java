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

package ru.avicomp.ontapi.internal.objects;

import ru.avicomp.ontapi.internal.InternalCache;

/**
 * A technical interface that describes a object that has content cache,
 * which can be restored from a {@link org.apache.jena.graph.Graph}.
 * For internal usage only.
 * <p>
 * Created by @ssz on 15.09.2019.
 *
 * @see InternalCache
 * @since 1.4.3
 */
public interface WithContent<X> {

    /**
     * Collects the object's content cache in the form of {@code Array}.
     * Such a form of cache was chosen
     * since it allows to reduces memory consumption and, at the same time, provides fast access.
     *
     * @return {@code Array} of {@code Object}s
     */
    Object[] collectContent();

    /**
     * Gets the content cache.
     *
     * @return {@code Array} of {@code Object}s
     */
    Object[] getContent();

    /**
     * Associates the given {@code Array} with this object's content
     *
     * @param content an {@code Array}
     */
    void putContent(Object[] content);

    /**
     * Answers {@code true} if the object contains no content.
     *
     * @return boolean
     */
    boolean hasContent();

    /**
     * Clears the content.
     */
    void clearContent();

    /**
     * Creates a content-container, which is used as a cache for different {@code ONTObject} parts.
     *
     * @return {@link InternalCache.Loading}
     */
    default InternalCache.Loading<X, Object[]> createContent() {
        return InternalCache.createSoftSingleton(x -> collectContent());
    }

    /**
     * Puts a new content inside the {@code object}.
     *
     * @param object  an {@code Array} with new content
     * @param content a new {@code Array} to cache
     * @param <X>     subtype of {@link WithContent}
     * @return the same {@code object} as specified
     */
    static <X extends WithContent> X addContent(X object, Object[] content) {
        object.putContent(content);
        return object;
    }
}
