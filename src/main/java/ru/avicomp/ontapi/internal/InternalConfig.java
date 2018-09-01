/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
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

import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;

import java.util.EnumMap;

/**
 * A container with various configuration settings
 * to manage mappings of the structural representation to RDF and vice versa.
 * <p>
 * Created by @szuev on 05.04.2017.
 */
public interface InternalConfig {

    InternalConfig DEFAULT = createFrom(new OntConfig().buildLoaderConfiguration());

    /**
     * Answers whether or not annotation axioms (instances of {@code OWLAnnotationAxiom}) should be loaded.
     * If {@code true} Annotation Property Domain, Property Range, Assertion and SubAnnotationPropertyOf axioms are skipped.
     *
     * @return boolean
     */
    boolean isLoadAnnotationAxioms();

    /**
     * Answers whether bulk-annotations is allowed in declaration axioms or
     * they should go separately as annotation assertion axioms.
     *
     * @return boolean
     */
    boolean isAllowBulkAnnotationAssertions();

    /**
     * Answers whether the Range, Domain and SubClassOf axioms should be separated
     * in case there is a punning with annotation property and some other property (data or object).
     *
     * @return boolean
     */
    boolean isIgnoreAnnotationAxiomOverlaps();

    /**
     * Answers whether the declaration axioms should be allowed.
     * In OWL-API declarations are not always mandatory.
     *
     * @return boolean
     */
    boolean isAllowReadDeclarations();

    /**
     * Answers whether the different bulk annotations for the same axiom should go as different axioms.
     *
     * @return boolean
     */
    boolean isSplitAxiomAnnotations();

    /**
     * Answers whether errors that arise when parsing axioms from a graph should be ignored.
     *
     * @return boolean
     */
    boolean isIgnoreAxiomsReadErrors();

    /**
     * Answers whether the behaviour should be concurrent oriented.
     *
     * @return {@code true} if parallel mode is enabled
     */
    default boolean parallel() {
        return false;
    }

    /**
     * Gets a fixed state of this config as immutable instance.
     *
     * @return {@link InternalConfig}
     */
    default InternalConfig snapshot() {
        return new Snapshot(this);
    }

    /**
     * Snapshot config implementation.
     */
    class Snapshot implements InternalConfig {
        private final EnumMap<Snapshot.Key, Boolean> map = new EnumMap<>(Snapshot.Key.class);

        Snapshot(InternalConfig delegate) {
            map.put(Snapshot.Key.LOAD_ANNOTATIONS, delegate.isLoadAnnotationAxioms());
            map.put(Snapshot.Key.ALLOW_DECLARATION_BULK_ANNOTATIONS, delegate.isAllowBulkAnnotationAssertions());
            map.put(Snapshot.Key.IGNORE_ANNOTATION_OVERLAPS, delegate.isIgnoreAnnotationAxiomOverlaps());
            map.put(Snapshot.Key.ALLOW_DECLARATIONS, delegate.isAllowReadDeclarations());
            map.put(Snapshot.Key.SPLIT_AXIOM_ANNOTATIONS, delegate.isSplitAxiomAnnotations());
            map.put(Snapshot.Key.IGNORE_READ_ERRORS, delegate.isIgnoreAxiomsReadErrors());
        }

        @Override
        public boolean isLoadAnnotationAxioms() {
            return map.get(Snapshot.Key.LOAD_ANNOTATIONS);
        }

        @Override
        public boolean isAllowBulkAnnotationAssertions() {
            return map.get(Snapshot.Key.ALLOW_DECLARATION_BULK_ANNOTATIONS);
        }

        @Override
        public boolean isIgnoreAnnotationAxiomOverlaps() {
            return map.get(Snapshot.Key.IGNORE_ANNOTATION_OVERLAPS);
        }

        @Override
        public boolean isAllowReadDeclarations() {
            return map.get(Snapshot.Key.ALLOW_DECLARATIONS);
        }

        @Override
        public boolean isSplitAxiomAnnotations() {
            return map.get(Snapshot.Key.SPLIT_AXIOM_ANNOTATIONS);
        }

        @Override
        public boolean isIgnoreAxiomsReadErrors() {
            return map.get(Snapshot.Key.IGNORE_READ_ERRORS);
        }

        private enum Key {
            LOAD_ANNOTATIONS,
            ALLOW_DECLARATION_BULK_ANNOTATIONS,
            IGNORE_ANNOTATION_OVERLAPS,
            ALLOW_DECLARATIONS,
            SPLIT_AXIOM_ANNOTATIONS,
            IGNORE_READ_ERRORS,
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
        };
    }
}
