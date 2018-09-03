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
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.*;
import java.util.stream.Stream;

/**
 * This is an enumeration of all entity (configurable-)factories.
 * <p>
 * Created by szuev on 03.11.2016.
 *
 * @see OntEntity
 */
public enum Entities implements Configurable<OntObjectFactory> {
    CLASS(OWL.Class, OntClassImpl.class) {
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

        @Override
        public Class<OntClass> getClassType() {
            return OntClass.class;
        }
    },
    DATATYPE(RDFS.Datatype, OntDatatypeImpl.class) {
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

        @Override
        public Class<OntDT> getClassType() {
            return OntDT.class;
        }
    },
    ANNOTATION_PROPERTY(OWL.AnnotationProperty, OntAPropertyImpl.class) {
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

        @Override
        public Class<OntNAP> getClassType() {
            return OntNAP.class;
        }
    },
    DATA_PROPERTY(OWL.DatatypeProperty, OntDPropertyImpl.class) {
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

        @Override
        public Class<OntNDP> getClassType() {
            return OntNDP.class;
        }
    },
    OBJECT_PROPERTY(OWL.ObjectProperty, OntOPEImpl.NamedPropertyImpl.class) {
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

        @Override
        public Class<OntNOP> getClassType() {
            return OntNOP.class;
        }
    },
    INDIVIDUAL(OWL.NamedIndividual, OntIndividualImpl.NamedImpl.class) {
        @Override
        public Class<OntIndividual.Named> getClassType() {
            return OntIndividual.Named.class;
        }
    };

    public static final BuiltIn.Vocabulary BUILTIN = BuiltIn.get();

    public static final Configurable<OntObjectFactory> ALL = OntObjectImpl.concatFactories(OntFinder.TYPED, values());

    private final Class<? extends OntObjectImpl> impl;
    private final Resource resourceType;
    private final Map<Mode, OntObjectFactory> registry = new HashMap<>();

    Entities(Resource resourceType, Class<? extends OntObjectImpl> impl) {
        this.impl = impl;
        this.resourceType = resourceType;
    }

    /**
     * Returns entity resource-type.
     *
     * @return {@link Resource}
     */
    public Resource getResourceType() {
        return resourceType;
    }

    /**
     * Returns entity class-type.
     *
     * @return {@link Class}
     */
    public abstract Class<? extends OntEntity> getClassType();

    Stream<Resource> bannedTypes(OntModelConfig.StdMode mode) {
        return Stream.empty();
    }

    Set<? extends Resource> builtInURIs() {
        return Collections.emptySet();
    }

    /**
     * Finds the entity by the resource-type.
     *
     * @param type {@link Resource}
     * @return {@link Optional} of {@link Entities}
     */
    public static Optional<Entities> find(Resource type) {
        for (Entities e : values()) {
            if (Objects.equals(e.resourceType, type)) return Optional.of(e);
        }
        return Optional.empty();
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
        OntFinder finder = new OntFinder.ByType(resourceType);

        OntFilter illegalPunningsFilter = OntFilter.TRUE.accumulate(bannedTypes(m)
                .map(OntFilter.HasType::new).map(OntFilter::negate).toArray(OntFilter[]::new));

        OntFilter filter = new OntFilter.OneOf(builtInURIs()).or(OntFilter.URI.and(new OntFilter.HasType(resourceType).and(illegalPunningsFilter)));
        //OntFilter filter = OntFilter.URI.and((new OntFilter.HasType(type).and(illegalPunningsFilter)).or(new OntFilter.OneOf(builtInURIs())));
        OntMaker maker = new OntMaker.WithType(impl, resourceType).restrict(illegalPunningsFilter);

        return new CommonOntObjectFactory(maker, finder, filter);
    }

}
