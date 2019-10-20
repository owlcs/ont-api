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

package ru.avicomp.ontapi.jena.impl.conf;

import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A {@link Vocabulary} abstraction and a holder for some particular vocabulary {@link OntPersonality}'s implementations.
 * Each of the impl has a simple in-memory cache to speed-up,
 * since it is important to have a quick access to desired types.
 * <p>
 * Created by @ssz on 18.01.2019.
 *
 * @since 1.4.0
 */
abstract class VocabularyImpl<T extends Resource> implements Vocabulary<T> {
    private final Map<Class<? extends T>, Set<Node>> map;

    VocabularyImpl(Map<Class<? extends T>, Set<Node>> map) {
        this.map = Objects.requireNonNull(map);
    }

    @Override
    public Set<Node> get(Class<? extends T> key) throws OntJenaException {
        Set<Node> res = map.get(OntJenaException.notNull(key, "Null key"));
        if (res == null) {
            throw new OntJenaException.Unsupported("Unsupported class-type " + key);
        }
        if (res.isEmpty()) {
            return Collections.emptySet();
        }
        return res;
    }

    static class EntitiesImpl extends VocabularyImpl<OntObject> implements OntPersonality.Builtins, OntPersonality.Punnings {
        private Set<Node> classes;
        private Set<Node> datatypes;
        private Set<Node> objectProperties;
        private Set<Node> datatypeProperties;
        private Set<Node> annotationProperties;
        private Set<Node> allProperties;
        private Set<Node> individuals;

        EntitiesImpl(Map<Class<? extends OntObject>, Set<Node>> map) {
            super(map);
        }

        @Override
        public Set<Node> getClasses() {
            return classes == null ? classes = get(OntClass.class) : classes;
        }

        @Override
        public Set<Node> getDatatypes() {
            return datatypes == null ? datatypes = get(OntDT.class) : datatypes;
        }

        @Override
        public Set<Node> getObjectProperties() {
            return objectProperties == null ? objectProperties = get(OntNOP.class) : objectProperties;
        }

        @Override
        public Set<Node> getDatatypeProperties() {
            return datatypeProperties == null ? datatypeProperties = get(OntNDP.class) : datatypeProperties;
        }

        @Override
        public Set<Node> getAnnotationProperties() {
            return annotationProperties == null ? annotationProperties = get(OntNAP.class) : annotationProperties;
        }

        @Override
        public Set<Node> getIndividuals() {
            return individuals == null ? individuals = get(OntIndividual.Named.class) : individuals;
        }

        @Override
        public Set<Node> getProperties() {
            if (allProperties != null) return allProperties;
            return allProperties = Stream.of(getObjectProperties(),
                    getAnnotationProperties(),
                    getDatatypeProperties())
                    .flatMap(Collection::stream).collect(Iter.toUnmodifiableSet());
        }
    }

    static class ReservedIml extends VocabularyImpl<Resource> implements OntPersonality.Reserved {
        private Set<Node> resources;
        private Set<Node> properties;
        private Map<String, Set<Node>> nodes = new HashMap<>();

        ReservedIml(Map<Class<? extends Resource>, Set<Node>> map) {
            super(map);
        }

        @Override
        public Set<Node> get(String key, Supplier<Set<Node>> loader) {
            return nodes.computeIfAbsent(key, k -> loader.get());
        }

        @Override
        public Set<Node> getResources() {
            return resources == null ? resources = get(Resource.class) : resources;
        }

        @Override
        public Set<Node> getProperties() {
            return properties == null ? properties = get(Property.class) : properties;
        }
    }
}
