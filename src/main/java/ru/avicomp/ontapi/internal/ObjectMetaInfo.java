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

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.utils.Iter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Enum, that represents all public component-types of {@code OWLOntology}: ontology header and {@code 39} axiom types.
 * For axioms there is a natural {@link AxiomType}'s order to provide a little bit faster iterating:
 * the declarations and widely used axioms go first, which is good for the data-factory caching.
 */
public enum ObjectMetaInfo {
    HEADER(null, false) {
        @Override
        public boolean isAxiom() {
            return false;
        }
    },
    DECLARATION(AxiomType.DECLARATION),
    EQUIVALENT_CLASSES(AxiomType.EQUIVALENT_CLASSES),
    SUBCLASS_OF(AxiomType.SUBCLASS_OF),
    DISJOINT_CLASSES(AxiomType.DISJOINT_CLASSES, false),
    DISJOINT_UNION(AxiomType.DISJOINT_UNION, false),
    CLASS_ASSERTION(AxiomType.CLASS_ASSERTION),
    SAME_INDIVIDUAL(AxiomType.SAME_INDIVIDUAL, false),
    DIFFERENT_INDIVIDUALS(AxiomType.DIFFERENT_INDIVIDUALS, false),
    OBJECT_PROPERTY_ASSERTION(AxiomType.OBJECT_PROPERTY_ASSERTION),
    NEGATIVE_OBJECT_PROPERTY_ASSERTION(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION, false),
    DATA_PROPERTY_ASSERTION(AxiomType.DATA_PROPERTY_ASSERTION),
    NEGATIVE_DATA_PROPERTY_ASSERTION(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION, false),
    EQUIVALENT_OBJECT_PROPERTIES(AxiomType.EQUIVALENT_OBJECT_PROPERTIES, false),
    SUB_OBJECT_PROPERTY(AxiomType.SUB_OBJECT_PROPERTY),
    INVERSE_OBJECT_PROPERTIES(AxiomType.INVERSE_OBJECT_PROPERTIES, false),
    FUNCTIONAL_OBJECT_PROPERTY(AxiomType.FUNCTIONAL_OBJECT_PROPERTY),
    INVERSE_FUNCTIONAL_OBJECT_PROPERTY(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY),
    SYMMETRIC_OBJECT_PROPERTY(AxiomType.SYMMETRIC_OBJECT_PROPERTY),
    ASYMMETRIC_OBJECT_PROPERTY(AxiomType.ASYMMETRIC_OBJECT_PROPERTY),
    TRANSITIVE_OBJECT_PROPERTY(AxiomType.TRANSITIVE_OBJECT_PROPERTY),
    REFLEXIVE_OBJECT_PROPERTY(AxiomType.REFLEXIVE_OBJECT_PROPERTY),
    IRREFLEXIVE_OBJECT_PROPERTY(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY),
    OBJECT_PROPERTY_DOMAIN(AxiomType.OBJECT_PROPERTY_DOMAIN),
    OBJECT_PROPERTY_RANGE(AxiomType.OBJECT_PROPERTY_RANGE),
    DISJOINT_OBJECT_PROPERTIES(AxiomType.DISJOINT_OBJECT_PROPERTIES, false),
    SUB_PROPERTY_CHAIN_OF(AxiomType.SUB_PROPERTY_CHAIN_OF, false),
    EQUIVALENT_DATA_PROPERTIES(AxiomType.EQUIVALENT_DATA_PROPERTIES, false),
    SUB_DATA_PROPERTY(AxiomType.SUB_DATA_PROPERTY),
    FUNCTIONAL_DATA_PROPERTY(AxiomType.FUNCTIONAL_DATA_PROPERTY),
    DATA_PROPERTY_DOMAIN(AxiomType.DATA_PROPERTY_DOMAIN),
    DATA_PROPERTY_RANGE(AxiomType.DATA_PROPERTY_RANGE),
    DISJOINT_DATA_PROPERTIES(AxiomType.DISJOINT_DATA_PROPERTIES, false),
    HAS_KEY(AxiomType.HAS_KEY, false),
    SWRL_RULE(AxiomType.SWRL_RULE, false),
    ANNOTATION_ASSERTION(AxiomType.ANNOTATION_ASSERTION),
    SUB_ANNOTATION_PROPERTY_OF(AxiomType.SUB_ANNOTATION_PROPERTY_OF),
    ANNOTATION_PROPERTY_RANGE(AxiomType.ANNOTATION_PROPERTY_RANGE),
    ANNOTATION_PROPERTY_DOMAIN(AxiomType.ANNOTATION_PROPERTY_DOMAIN),
    DATATYPE_DEFINITION(AxiomType.DATATYPE_DEFINITION),
    ;

    public static final List<ObjectMetaInfo> AXIOMS = all().skip(1).collect(Iter.toUnmodifiableList());
    public static final List<ObjectMetaInfo> LOGICAL = AXIOMS.stream().filter(x -> x.type.isLogical()).collect(Iter.toUnmodifiableList());

    private final AxiomType<? extends OWLAxiom> type;
    private final boolean distinct;

    ObjectMetaInfo(AxiomType<? extends OWLAxiom> type) {
        this(type, true);
    }

    ObjectMetaInfo(AxiomType<? extends OWLAxiom> type, boolean distinct) {
        this.type = type;
        this.distinct = distinct;
    }

    /**
     * Returns a {@link ObjectMetaInfo} by the {@link AxiomType}.
     *
     * @param type {@link AxiomType}, not {@code null}
     * @return {@link ObjectMetaInfo}, not {@code null}
     */
    public static ObjectMetaInfo get(AxiomType<?> type) throws IndexOutOfBoundsException {
        return values()[type.getIndex() + 1];
    }

    /**
     * Returns a {@link ObjectMetaInfo} by the {@link OWLAxiom}s {@code Class}-type.
     *
     * @param type {@link OWLAxiom} actual class-type
     * @return {@link ObjectMetaInfo}, not {@code null}
     * @see AxiomType#getActualClass()
     */
    public static ObjectMetaInfo get(Class<? extends OWLAxiom> type) {
        ObjectMetaInfo[] array = values();
        for (int i = 1; i < array.length; i++) {
            ObjectMetaInfo res = array[i];
            if (type == res.type.getActualClass()) return res;
        }
        throw new OntApiException.IllegalState();
    }

    /**
     * Lists all values as {@code Stream}.
     *
     * @return {@code Stream} of {@link ObjectMetaInfo}s
     */
    public static Stream<ObjectMetaInfo> all() {
        return Arrays.stream(values());
    }

    /**
     * Lists all {@link OWLAxiom}s {@link ObjectMetaInfo Meta-info}s.
     *
     * @return {@code Stream} of {@link ObjectMetaInfo}s
     */
    public static Stream<ObjectMetaInfo> axioms() {
        return AXIOMS.stream();
    }

    public static Stream<ObjectMetaInfo> logical() {
        return LOGICAL.stream();
    }

    /**
     * Lists all {@link OWLAxiom}s {@link ObjectMetaInfo Meta-info}s for the specified axioms {@code types}.
     *
     * @param types {@link Iterable}, not {@code null}
     * @return {@code Stream} of {@link ObjectMetaInfo}s
     */
    public static Stream<ObjectMetaInfo> axioms(Iterable<AxiomType<?>> types) {
        Stream<AxiomType<?>> res;
        if (types instanceof Collection) {
            res = ((Collection<AxiomType<?>>) types).stream();
        } else {
            res = StreamSupport.stream(types.spliterator(), false);
        }
        return res.map(ObjectMetaInfo::get);
    }

    /**
     * Answers {@code true} iff it is meta-info for an axiom-type, not for {@link #HEADER header}.
     *
     * @return boolean
     */
    public boolean isAxiom() {
        return true;
    }

    /**
     * Answers {@code true} if and only if
     * there can be only one unique object of this enum-type,
     * which means that there is only one statement in the graph, to which that object corresponds.
     * Returns {@code false}, if an object of the enum-type can be derived from different RDF statements.
     * <p>
     * Several examples when the method returns {@code false}:
     * <ul>
     * <li>{@link #DIFFERENT_INDIVIDUALS}: the same axiom {@code DifferentIndividuals(<A> <B>)} can be derived
     * form three statements: {@code a1 owl:differentFrom a2}, {@code a2 owl:differentFrom a1}
     * and {@code _:x rdf:type owl:AllDifferent . _:x owl:members ( a1 a2 ). }</li>
     * <li>{@link #DISJOINT_UNION}: it is possible to have different rdf-lists with the same content,
     * therefore a graph may contain different but similar statements that result the same axiom:
     * {@code C1 owl:disjointUnionOf ( C2 ) . C1 owl:disjointUnionOf ( C2 ) . }</li>
     * <li>{@link #HEADER}: bulk annotation is a b-node, RDF graph can contain any number of b-nodes
     * with the same content (annotation property and annotation value)</li>
     * </ul>
     *
     * @return boolean
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Answers a {@link AxiomType} for this enum or {@code null} if it is {@link #HEADER header}.
     *
     * @return {@link AxiomType} or {@code null}
     */
    @SuppressWarnings("unchecked")
    public AxiomType<OWLAxiom> getAxiomType() {
        return (AxiomType<OWLAxiom>) type;
    }

}
