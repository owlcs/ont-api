/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
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

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.config.CacheSettings;
import com.github.owlcs.ontapi.config.OntConfig;
import com.github.owlcs.ontapi.config.OntLoaderConfiguration;

import java.util.EnumMap;
import java.util.Objects;

/**
 * A container with various configuration settings
 * to manage mappings of the structural representation to RDF and vice versa.
 * <p>
 * Created by @ssz on 05.04.2017.
 */
public interface InternalConfig extends CacheSettings, AxiomsSettings {

    InternalConfig DEFAULT = createFrom(new OntConfig().buildLoaderConfiguration());

    /**
     * Answers whether the behaviour should be concurrent oriented.
     *
     * @return {@code true} if parallel mode is enabled
     */
    default boolean concurrent() {
        return false;
    }

    /**
     * Gets a fixed state of this config as immutable instance.
     *
     * @return immutable {@link InternalConfig}
     */
    default Snapshot snapshot() {
        return new Snapshot(this);
    }

    /**
     * Snapshot config implementation.
     */
    class Snapshot implements InternalConfig {
        private final EnumMap<Key, Object> map = new EnumMap<>(Key.class);
        private final boolean parallel;

        Snapshot(InternalConfig delegate) {
            parallel = Objects.requireNonNull(delegate, "Null config").concurrent();
            map.put(Key.LOAD_ANNOTATIONS, delegate.isLoadAnnotationAxioms());
            map.put(Key.ALLOW_DECLARATION_BULK_ANNOTATIONS, delegate.isAllowBulkAnnotationAssertions());
            map.put(Key.IGNORE_ANNOTATION_OVERLAPS, delegate.isIgnoreAnnotationAxiomOverlaps());
            map.put(Key.ALLOW_DECLARATIONS, delegate.isAllowReadDeclarations());
            map.put(Key.SPLIT_AXIOM_ANNOTATIONS, delegate.isSplitAxiomAnnotations());
            map.put(Key.IGNORE_READ_ERRORS, delegate.isIgnoreAxiomsReadErrors());
            map.put(Key.CACHE_NODES_SIZE, delegate.getLoadNodesCacheSize());
            map.put(Key.CACHE_OBJECTS_SIZE, delegate.getLoadObjectsCacheSize());
            map.put(Key.CONTENT_CACHE_LEVEL, delegate.getModelCacheLevel());
            map.put(Key.READ_ONT_OBJECTS, delegate.isReadONTObjects());
        }

        @SuppressWarnings("unchecked")
        private <X> X get(Key k) {
            return (X) map.get(k);
        }

        @Override
        public boolean isLoadAnnotationAxioms() {
            return get(Key.LOAD_ANNOTATIONS);
        }

        @Override
        public boolean isAllowBulkAnnotationAssertions() {
            return get(Key.ALLOW_DECLARATION_BULK_ANNOTATIONS);
        }

        @Override
        public boolean isIgnoreAnnotationAxiomOverlaps() {
            return get(Key.IGNORE_ANNOTATION_OVERLAPS);
        }

        @Override
        public boolean isAllowReadDeclarations() {
            return get(Key.ALLOW_DECLARATIONS);
        }

        @Override
        public boolean isSplitAxiomAnnotations() {
            return get(Key.SPLIT_AXIOM_ANNOTATIONS);
        }

        @Override
        public boolean isIgnoreAxiomsReadErrors() {
            return get(Key.IGNORE_READ_ERRORS);
        }

        @Override
        public boolean isReadONTObjects() {
            return get(Key.READ_ONT_OBJECTS);
        }

        @Override
        public int getLoadNodesCacheSize() {
            return get(Key.CACHE_NODES_SIZE);
        }

        @Override
        public int getLoadObjectsCacheSize() {
            return get(Key.CACHE_OBJECTS_SIZE);
        }

        @Override
        public int getModelCacheLevel() {
            return get(Key.CONTENT_CACHE_LEVEL);
        }

        @Override
        public boolean concurrent() {
            return parallel;
        }

        @Override
        public Snapshot snapshot() {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return map.equals(((Snapshot) o).map);
        }

        @Override
        public int hashCode() {
            return Objects.hash(map);
        }

        private enum Key {
            LOAD_ANNOTATIONS,
            ALLOW_DECLARATION_BULK_ANNOTATIONS,
            IGNORE_ANNOTATION_OVERLAPS,
            ALLOW_DECLARATIONS,
            SPLIT_AXIOM_ANNOTATIONS,
            IGNORE_READ_ERRORS,
            READ_ONT_OBJECTS,
            CACHE_NODES_SIZE,
            CACHE_OBJECTS_SIZE,
            CONTENT_CACHE_LEVEL,
        }
    }

    /**
     * Creates a {@code InternalConfig} implementation from the given {@code OntLoaderConfiguration}.
     *
     * @param conf {@link OntLoaderConfiguration}
     * @return {@link InternalConfig} which is backed by the {@link OntLoaderConfiguration}
     */
    static InternalConfig createFrom(OntLoaderConfiguration conf) {
        return new InternalConfig() {
            @Override
            public boolean isLoadAnnotationAxioms() {
                return conf.isLoadAnnotationAxioms();
            }

            @Override
            public boolean isAllowBulkAnnotationAssertions() {
                return conf.isAllowBulkAnnotationAssertions();
            }

            @Override
            public boolean isIgnoreAnnotationAxiomOverlaps() {
                return conf.isIgnoreAnnotationAxiomOverlaps();
            }

            @Override
            public boolean isAllowReadDeclarations() {
                return conf.isAllowReadDeclarations();
            }

            @Override
            public boolean isSplitAxiomAnnotations() {
                return conf.isSplitAxiomAnnotations();
            }

            @Override
            public boolean isIgnoreAxiomsReadErrors() {
                return conf.isIgnoreAxiomsReadErrors();
            }

            @Override
            public boolean isReadONTObjects() {
                return conf.isReadONTObjects();
            }

            @Override
            public int getLoadNodesCacheSize() {
                return conf.getLoadNodesCacheSize();
            }

            @Override
            public int getLoadObjectsCacheSize() {
                return conf.getLoadObjectsCacheSize();
            }

            @Override
            public int getModelCacheLevel() {
                return conf.getModelCacheLevel();
            }

        };
    }
}
