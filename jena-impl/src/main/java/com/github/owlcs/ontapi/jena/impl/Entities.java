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

package com.github.owlcs.ontapi.jena.impl;

import com.github.owlcs.ontapi.jena.impl.conf.ObjectFactory;
import com.github.owlcs.ontapi.jena.impl.conf.OntFilter;
import com.github.owlcs.ontapi.jena.impl.conf.OntFinder;
import com.github.owlcs.ontapi.jena.impl.conf.OntMaker;
import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality;
import com.github.owlcs.ontapi.jena.impl.conf.Vocabulary;
import com.github.owlcs.ontapi.jena.model.OntAnnotationProperty;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntDataRange;
import com.github.owlcs.ontapi.jena.model.OntEntity;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * This is an enumeration of all entity (configurable-)factories.
 * <p>
 * Created @ssz on 03.11.2016.
 *
 * @see OntEntity
 */
public enum Entities {
    CLASS(OWL.Class, OntClass.Named.class, OntClassImpl.class, Vocabulary.Entities::getClasses),
    DATATYPE(RDFS.Datatype, OntDataRange.Named.class, OntDatatypeImpl.class, Vocabulary.Entities::getDatatypes),
    ANNOTATION_PROPERTY(OWL.AnnotationProperty, OntAnnotationProperty.class, OntAPropertyImpl.class, Vocabulary.Entities::getAnnotationProperties),
    DATA_PROPERTY(OWL.DatatypeProperty, OntDataProperty.class, OntDPropertyImpl.class, Vocabulary.Entities::getDatatypeProperties),
    OBJECT_PROPERTY(OWL.ObjectProperty, OntObjectProperty.Named.class, OntOPEImpl.NamedPropertyImpl.class, Vocabulary.Entities::getObjectProperties),
    INDIVIDUAL(OWL.NamedIndividual, OntIndividual.Named.class, OntIndividualImpl.NamedImpl.class, Vocabulary.Entities::getIndividuals) {
        @Override
        OntFilter createPrimaryFilter() {
            return (n, g) -> n.isURI() && filterType(n, g);
        }

        private boolean filterType(Node n, EnhGraph g) {
            if (builtInURIs(g).contains(n)) { // just in case
                return true;
            }
            Set<Node> forbidden = bannedTypes(g);
            List<Node> candidates = new ArrayList<>();
            boolean hasDeclaration = false;
            ExtendedIterator<Triple> it = g.asGraph().find(n, RDF.Nodes.type, Node.ANY);
            try {
                while (it.hasNext()) {
                    Node type = it.next().getObject();
                    if (forbidden.contains(type)) {
                        return false;
                    }
                    if (resourceType.asNode().equals(type)) {
                        hasDeclaration = true;
                    } else {
                        candidates.add(type);
                    }
                }
            } finally {
                it.close();
            }
            if (hasDeclaration) {
                return true;
            }
            // In general, owl:NamedIndividual declaration is optional
            for (Node c : candidates) {
                if (PersonalityModel.canAs(OntClass.class, c, g)) return true;
            }
            return false;
        }
    },
    ;
    private static final OntFinder ENTITY_FINDER = Factories.createFinder(e -> e.getResourceType().asNode(), values());
    public static final ObjectFactory ALL = Factories.createFrom(ENTITY_FINDER,
            Arrays.stream(values()).map(Entities::getActualType));

    final Class<? extends OntObjectImpl> impl;
    final Class<? extends OntEntity> classType;
    final Resource resourceType;
    final Function<Vocabulary.Entities, Set<Node>> extractNodeSet;

    /**
     * Creates an entity enum.
     * @param resourceType {@link Resource}-type
     * @param classType    class-type of the corresponding {@link OntEntity}
     * @param impl         class-implementation
     * @param extractNodeSet to retrieve {@link Node}s
     */
    Entities(Resource resourceType,
             Class<? extends OntEntity> classType,
             Class<? extends OntObjectImpl> impl,
             Function<Vocabulary.Entities, Set<Node>> extractNodeSet) {
        this.classType = classType;
        this.resourceType = resourceType;
        this.impl = impl;
        this.extractNodeSet = extractNodeSet;
    }

    /**
     * Returns entity class-type.
     *
     * @return {@link Class}, one of {@link OntEntity}
     */
    public Class<? extends OntEntity> getActualType() {
        return classType;
    }

    /**
     * Returns entity resource-type.
     *
     * @return {@link Resource}
     */
    public Resource getResourceType() {
        return resourceType;
    }

    private OntPersonality personality(EnhGraph g) {
        return PersonalityModel.asPersonalityModel(g).getOntPersonality();
    }

    /**
     * Answers a {@code Set} of URI Nodes that this entity cannot have as {@code rdf:type}.
     *
     * @param g {@link EnhGraph}
     * @return Set of {@link Node}s
     */
    Set<Node> bannedTypes(EnhGraph g) {
        return extractNodeSet.apply(personality(g).getPunnings());
    }

    /**
     * Answers a {@code Set} of URI Nodes
     * that can be treated as this entity even there is no any {@code rdf:type} declarations.
     *
     * @param g {@link EnhGraph}
     * @return Set of {@link Node}s
     */
    Set<Node> builtInURIs(EnhGraph g) {
        return extractNodeSet.apply(personality(g).getBuiltins());
    }

    /**
     * Creates a factory for the entity.
     *
     * @return {@link ObjectFactory}
     */
    public ObjectFactory createFactory() {
        OntFinder finder = new OntFinder.ByType(resourceType);
        OntFilter filter = createPrimaryFilter();
        OntMaker maker = new OntMaker.WithType(impl, resourceType).restrict(createIllegalPunningsFilter());
        return Factories.createCommon(classType, maker, finder, filter);
    }

    OntFilter createIllegalPunningsFilter() {
        return (n, eg) -> {
            Graph g = eg.asGraph();
            for (Node t : bannedTypes(eg)) {
                if (g.contains(n, RDF.Nodes.type, t)) return false;
            }
            return true;
        };
    }

    OntFilter createPrimaryFilter() {
        OntFilter builtInEntity = (n, g) -> builtInURIs(g).contains(n);
        OntFilter modelEntity = new OntFilter.HasType(resourceType).and(createIllegalPunningsFilter());
        OntFilter entity = modelEntity.or(builtInEntity);
        return OntFilter.URI.and(entity);
    }

    /**
     * Finds the entity by the resource-type.
     *
     * @param type {@link Resource}
     * @return {@link Optional} of {@link Entities}
     */
    public static Optional<Entities> find(Resource type) {
        return find(type.asNode());
    }

    /**
     * Finds the entity by the node-type.
     *
     * @param type {@link Node}, not {@code null}
     * @return {@link Optional} of {@link Entities}
     */
    public static Optional<Entities> find(Node type) {
        for (Entities e : values()) {
            if (Objects.equals(e.getResourceType().asNode(), type)) return Optional.of(e);
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
