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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.impl.conf.*;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * This is an enumeration of all entity (configurable-)factories.
 * <p>
 * Created by szuev on 03.11.2016.
 *
 * @see OntEntity
 */
public enum Entities {
    CLASS(OWL.Class, OntClass.class, OntClassImpl.class) {
        @Override
        Set<Node> bannedTypes(EnhGraph g) {
            return punnings(g).getClasses();
        }

        @Override
        Set<Node> builtInURIs(EnhGraph g) {
            return builtins(g).getClasses();
        }
    },
    DATATYPE(RDFS.Datatype, OntDT.class, OntDatatypeImpl.class) {
        @Override
        Set<Node> bannedTypes(EnhGraph g) {
            return punnings(g).getDatatypes();
        }

        @Override
        Set<Node> builtInURIs(EnhGraph g) {
            return builtins(g).getDatatypes();
        }
    },
    ANNOTATION_PROPERTY(OWL.AnnotationProperty, OntNAP.class, OntAPropertyImpl.class) {
        @Override
        Set<Node> bannedTypes(EnhGraph g) {
            return punnings(g).getAnnotationProperties();
        }

        @Override
        Set<Node> builtInURIs(EnhGraph g) {
            return builtins(g).getAnnotationProperties();
        }
    },
    DATA_PROPERTY(OWL.DatatypeProperty, OntNDP.class, OntDPropertyImpl.class) {
        @Override
        Set<Node> bannedTypes(EnhGraph g) {
            return punnings(g).getDatatypeProperties();
        }

        @Override
        Set<Node> builtInURIs(EnhGraph g) {
            return builtins(g).getDatatypeProperties();
        }
    },
    OBJECT_PROPERTY(OWL.ObjectProperty, OntNOP.class, OntOPEImpl.NamedPropertyImpl.class) {
        @Override
        Set<Node> bannedTypes(EnhGraph g) {
            return punnings(g).getObjectProperties();
        }

        @Override
        Set<Node> builtInURIs(EnhGraph g) {
            return builtins(g).getObjectProperties();
        }
    },
    INDIVIDUAL(OWL.NamedIndividual, OntIndividual.Named.class, OntIndividualImpl.NamedImpl.class) {
        @Override
        Set<Node> bannedTypes(EnhGraph g) {
            return punnings(g).getIndividuals();
        }

        @Override
        Set<Node> builtInURIs(EnhGraph g) {
            return builtins(g).getIndividuals();
        }
    },
    ;
    private static final OntFinder ENTITY_FINDER = Factories.createFinder(e -> e.getResourceType().asNode(), values());
    public static final ObjectFactory ALL = Factories.createFrom(ENTITY_FINDER,
            Arrays.stream(values()).map(Entities::getActualType));

    private final Class<? extends OntObjectImpl> impl;
    private final Class<? extends OntEntity> classType;
    private final Resource resourceType;

    /**
     * @param resourceType {@link Resource}-type
     * @param classType    class-type of the corresponding {@link OntEntity}
     * @param impl         class-implementation
     */
    Entities(Resource resourceType, Class<? extends OntEntity> classType, Class<? extends OntObjectImpl> impl) {
        this.classType = classType;
        this.resourceType = resourceType;
        this.impl = impl;
    }

    /**
     * Returns entity class-type.
     *
     * @return {@link Class}
     */
    public Class<? extends OntEntity> getActualType() {
        return classType;
    }

    OntPersonality.Builtins builtins(EnhGraph g) {
        return PersonalityModel.asPersonalityModel(g).getOntPersonality().getBuiltins();
    }

    OntPersonality.Punnings punnings(EnhGraph g) {
        return PersonalityModel.asPersonalityModel(g).getOntPersonality().getPunnings();
    }

    /**
     * Answers a {@code Set} of URI Nodes that this entity cannot have as {@code rdf:type}.
     *
     * @param g {@link EnhGraph}
     * @return Set of {@link Node}s
     */
    abstract Set<Node> bannedTypes(EnhGraph g);

    /**
     * Answers a {@code Set} of URI Nodes
     * that can be treated as this entity even there is no any {@code rdf:type} declarations.
     *
     * @param g {@link EnhGraph}
     * @return Set of {@link Node}s
     */
    abstract Set<Node> builtInURIs(EnhGraph g);

    /**
     * Returns entity resource-type.
     *
     * @return {@link Resource}
     */
    public Resource getResourceType() {
        return resourceType;
    }

    /**
     * Creates a factory for the entity.
     *
     * @return {@link ObjectFactory}
     */
    public ObjectFactory createFactory() {
        OntFinder finder = new OntFinder.ByType(resourceType);

        OntFilter illegalPunningsFilter = (n, eg) -> {
            Graph g = eg.asGraph();
            for (Node t : bannedTypes(eg)) {
                if (g.contains(n, RDF.Nodes.type, t)) return false;
            }
            return true;
        };
        OntFilter builtInEntity = (n, g) -> builtInURIs(g).contains(n);

        OntFilter modelEntity = new OntFilter.HasType(resourceType).and(illegalPunningsFilter);

        OntFilter filter = OntFilter.URI.and(modelEntity.or(builtInEntity));
        OntMaker maker = new OntMaker.WithType(impl, resourceType).restrict(illegalPunningsFilter);
        return Factories.createCommon(maker, finder, filter);
    }

    /**
     * Finds the entity by the resource-type.
     *
     * @param type {@link Resource}
     * @return {@link Optional} of {@link Entities}
     */
    public static Optional<Entities> find(Resource type) {
        for (Entities e : values()) {
            if (Objects.equals(e.getResourceType(), type)) return Optional.of(e);
        }
        return Optional.empty();
    }

    /**
     * Finds the entity by the class-type.
     *
     * @param type {@link Class}
     * @return {@link Optional} of {@link Entities}
     */
    public static Optional<Entities> find(Class<? extends OntEntity> type) {
        for (Entities e : values()) {
            if (Objects.equals(e.getActualType(), type)) return Optional.of(e);
        }
        return Optional.empty();
    }

}
