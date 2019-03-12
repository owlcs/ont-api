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
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Enum, that encapsulates {@link AxiomType}s.
 * It has a natural {@link AxiomType}'s order to provide a little bit faster iterating:
 * the declarations and widely used axioms go first, which is good for the data-factory cache.
 */
public enum AxiomKey {
    DECLARATION(AxiomType.DECLARATION),
    EQUIVALENT_CLASSES(AxiomType.EQUIVALENT_CLASSES),
    SUBCLASS_OF(AxiomType.SUBCLASS_OF),
    DISJOINT_CLASSES(AxiomType.DISJOINT_CLASSES),
    DISJOINT_UNION(AxiomType.DISJOINT_UNION),
    CLASS_ASSERTION(AxiomType.CLASS_ASSERTION),
    SAME_INDIVIDUAL(AxiomType.SAME_INDIVIDUAL),
    DIFFERENT_INDIVIDUALS(AxiomType.DIFFERENT_INDIVIDUALS),
    OBJECT_PROPERTY_ASSERTION(AxiomType.OBJECT_PROPERTY_ASSERTION),
    NEGATIVE_OBJECT_PROPERTY_ASSERTION(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION),
    DATA_PROPERTY_ASSERTION(AxiomType.DATA_PROPERTY_ASSERTION),
    NEGATIVE_DATA_PROPERTY_ASSERTION(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION),
    EQUIVALENT_OBJECT_PROPERTIES(AxiomType.EQUIVALENT_OBJECT_PROPERTIES),
    SUB_OBJECT_PROPERTY(AxiomType.SUB_OBJECT_PROPERTY),
    INVERSE_OBJECT_PROPERTIES(AxiomType.INVERSE_OBJECT_PROPERTIES),
    FUNCTIONAL_OBJECT_PROPERTY(AxiomType.FUNCTIONAL_OBJECT_PROPERTY),
    INVERSE_FUNCTIONAL_OBJECT_PROPERTY(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY),
    SYMMETRIC_OBJECT_PROPERTY(AxiomType.SYMMETRIC_OBJECT_PROPERTY),
    ASYMMETRIC_OBJECT_PROPERTY(AxiomType.ASYMMETRIC_OBJECT_PROPERTY),
    TRANSITIVE_OBJECT_PROPERTY(AxiomType.TRANSITIVE_OBJECT_PROPERTY),
    REFLEXIVE_OBJECT_PROPERTY(AxiomType.REFLEXIVE_OBJECT_PROPERTY),
    IRREFLEXIVE_OBJECT_PROPERTY(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY),
    OBJECT_PROPERTY_DOMAIN(AxiomType.OBJECT_PROPERTY_DOMAIN),
    OBJECT_PROPERTY_RANGE(AxiomType.OBJECT_PROPERTY_RANGE),
    DISJOINT_OBJECT_PROPERTIES(AxiomType.DISJOINT_OBJECT_PROPERTIES),
    SUB_PROPERTY_CHAIN_OF(AxiomType.SUB_PROPERTY_CHAIN_OF),
    EQUIVALENT_DATA_PROPERTIES(AxiomType.EQUIVALENT_DATA_PROPERTIES),
    SUB_DATA_PROPERTY(AxiomType.SUB_DATA_PROPERTY),
    FUNCTIONAL_DATA_PROPERTY(AxiomType.FUNCTIONAL_DATA_PROPERTY),
    DATA_PROPERTY_DOMAIN(AxiomType.DATA_PROPERTY_DOMAIN),
    DATA_PROPERTY_RANGE(AxiomType.DATA_PROPERTY_RANGE),
    DISJOINT_DATA_PROPERTIES(AxiomType.DISJOINT_DATA_PROPERTIES),
    HAS_KEY(AxiomType.HAS_KEY),
    SWRL_RULE(AxiomType.SWRL_RULE),
    ANNOTATION_ASSERTION(AxiomType.ANNOTATION_ASSERTION),
    SUB_ANNOTATION_PROPERTY_OF(AxiomType.SUB_ANNOTATION_PROPERTY_OF),
    ANNOTATION_PROPERTY_RANGE(AxiomType.ANNOTATION_PROPERTY_RANGE),
    ANNOTATION_PROPERTY_DOMAIN(AxiomType.ANNOTATION_PROPERTY_DOMAIN),
    DATATYPE_DEFINITION(AxiomType.DATATYPE_DEFINITION),
    ;

    public static final List<AxiomKey> LOGICAL = list().filter(x -> x.type.isLogical()).collect(Iter.toUnmodifiableList());

    private AxiomType<? extends OWLAxiom> type;

    AxiomKey(AxiomType<? extends OWLAxiom> type) {
        this.type = type;
    }

    /**
     * Returns a {@link AxiomKey} by the {@link AxiomType}.
     *
     * @param type {@link AxiomType}, not {@code null}
     * @return {@link AxiomKey}, not {@code null}
     */
    public static AxiomKey get(AxiomType<?> type) throws IndexOutOfBoundsException {
        return values()[type.getIndex()];
    }

    /**
     * Returns a {@link AxiomKey} by the {@link OWLAxiom}s {@code Class}-type.
     *
     * @param type {@link OWLAxiom} actual class-type
     * @return {@link AxiomKey}, not {@code null}
     * @see AxiomType#getActualClass()
     */
    public static AxiomKey get(Class<? extends OWLAxiom> type) {
        for (AxiomKey t : values()) {
            if (type == t.getAxiomClass()) return t;
        }
        throw new OntApiException.IllegalState();
    }

    public AxiomType<? extends OWLAxiom> getAxiomType() {
        return type;
    }

    @SuppressWarnings("unchecked")
    Class<OWLAxiom> getAxiomClass() {
        return (Class<OWLAxiom>) type.getActualClass();
    }

    public static Stream<AxiomKey> list() {
        return Arrays.stream(values());
    }

    public static Stream<AxiomKey> list(Iterable<AxiomType<?>> types) {
        return StreamSupport.stream(types.spliterator(), false).map(AxiomKey::get);
    }
}
