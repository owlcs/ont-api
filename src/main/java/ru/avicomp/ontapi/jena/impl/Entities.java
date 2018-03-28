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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.*;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * This is an enumeration of all entity (configurable-)factories.
 * <p>
 * Created by szuev on 03.11.2016.
 */
public enum Entities implements Configurable<OntObjectFactory> {
    CLASS(OntClassImpl.class, OWL.Class) {
        @Override
        Stream<Resource> bannedTypes(OntModelConfig.StdMode mode) {
            switch (mode) {
                case MEDIUM:
                case STRICT:
                    return Stream.of(RDFS.Datatype);
                default:
                    return Stream.empty();
            }
        }

        @Override
        Set<Resource> builtInURIs() {
            return BUILTIN.classes();
        }
    },
    DATATYPE(OntDatatypeImpl.class, RDFS.Datatype) {
        @Override
        Stream<Resource> bannedTypes(OntModelConfig.StdMode mode) {
            switch (mode) {
                case MEDIUM:
                case STRICT:
                    return Stream.of(OWL.Class);
                default:
                    return Stream.empty();
            }
        }

        @Override
        Set<Resource> builtInURIs() {
            return BUILTIN.datatypes();
        }
    },
    ANNOTATION_PROPERTY(OntAPropertyImpl.class, OWL.AnnotationProperty) {
        @Override
        Stream<Resource> bannedTypes(OntModelConfig.StdMode mode) {
            switch (mode) {
                case STRICT:
                    return Stream.of(OWL.ObjectProperty, OWL.DatatypeProperty);
                default:
                    return Stream.empty();
            }
        }

        @Override
        Set<Property> builtInURIs() {
            return BUILTIN.annotationProperties();
        }
    },
    DATA_PROPERTY(OntDPropertyImpl.class, OWL.DatatypeProperty) {
        @Override
        Stream<Resource> bannedTypes(OntModelConfig.StdMode mode) {
            switch (mode) {
                case STRICT:
                    return Stream.of(OWL.ObjectProperty, OWL.AnnotationProperty);
                case MEDIUM:
                    return Stream.of(OWL.ObjectProperty);
                default:
                    return Stream.empty();
            }
        }

        @Override
        Set<Property> builtInURIs() {
            return BUILTIN.datatypeProperties();
        }
    },
    OBJECT_PROPERTY(OntOPEImpl.NamedPropertyImpl.class, OWL.ObjectProperty) {
        @Override
        Stream<Resource> bannedTypes(OntModelConfig.StdMode mode) {
            switch (mode) {
                case STRICT:
                    return Stream.of(OWL.DatatypeProperty, OWL.AnnotationProperty);
                case MEDIUM:
                    return Stream.of(OWL.DatatypeProperty);
                default:
                    return Stream.empty();
            }
        }

        @Override
        Set<Property> builtInURIs() {
            return BUILTIN.objectProperties();
        }
    },
    INDIVIDUAL(OntIndividualImpl.NamedImpl.class, OWL.NamedIndividual);

    public static final BuiltIn.Vocabulary BUILTIN = BuiltIn.get();

    public static final Configurable<OntObjectFactory> ALL = OntObjectImpl.concatFactories(OntFinder.TYPED, values());

    private final Class<? extends OntObjectImpl> impl;
    private final Resource type;
    private final Map<Mode, OntObjectFactory> registry = new HashMap<>();

    Entities(Class<? extends OntObjectImpl> impl, Resource type) {
        this.impl = impl;
        this.type = type;
    }

    public Resource type() {
        return type;
    }

    Stream<Resource> bannedTypes(OntModelConfig.StdMode mode) {
        return Stream.empty();
    }

    Set<? extends Resource> builtInURIs() {
        return Collections.emptySet();
    }

    /**
     * Registers a custom entity factory.
     *
     * @param key     {@link Configurable.Mode} key, not null.
     * @param factory {@link OntObjectFactory}, the factory for selected entity, not null.
     */
    public void register(Mode key, OntObjectFactory factory) {
        registry.put(OntJenaException.notNull(key, "Null mode-key"), OntJenaException.notNull(factory, "Null factory-value"));
    }

    /**
     * Unregisters a custom factory.
     *
     * @param key {@link Configurable.Mode} key, not null.
     * @return a {@link OntObjectFactory} previously associated with the {@code key}
     */
    public OntObjectFactory unregister(Mode key) {
        return registry.remove(key);
    }

    /**
     * Returns all custom factories keys
     *
     * @return Set of {@link Mode}s.
     */
    public Set<Mode> keys() {
        return registry.keySet();
    }

    @Override
    public OntObjectFactory select(Mode m) {
        return registry.getOrDefault(m, makeDefault(m));
    }

    private OntObjectFactory makeDefault(Mode mode) {
        OntModelConfig.StdMode m = mode instanceof OntModelConfig.StdMode ? (OntModelConfig.StdMode) mode : OntModelConfig.StdMode.LAX;
        OntFinder finder = new OntFinder.ByType(type);

        OntFilter illegalPunningsFilter = OntFilter.TRUE.accumulate(bannedTypes(m)
                .map(OntFilter.HasType::new).map(OntFilter::negate).toArray(OntFilter[]::new));

        OntFilter filter = OntFilter.URI.and((new OntFilter.HasType(type).and(illegalPunningsFilter)).or(new OntFilter.OneOf(builtInURIs())));
        OntMaker maker = new OntMaker.WithType(impl, type).restrict(illegalPunningsFilter);

        return new CommonOntObjectFactory(maker, finder, filter);
    }

}
