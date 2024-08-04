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

import com.github.owlcs.ontapi.OntApiException;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.utils.Iterators;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.HasAnnotations;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.github.owlcs.ontapi.internal.OWLComponentType.ANNOTATION_PROPERTY;
import static com.github.owlcs.ontapi.internal.OWLComponentType.ANONYMOUS_INDIVIDUAL;
import static com.github.owlcs.ontapi.internal.OWLComponentType.CLASS;
import static com.github.owlcs.ontapi.internal.OWLComponentType.CLASS_EXPRESSION;
import static com.github.owlcs.ontapi.internal.OWLComponentType.DATATYPE;
import static com.github.owlcs.ontapi.internal.OWLComponentType.DATATYPE_PROPERTY;
import static com.github.owlcs.ontapi.internal.OWLComponentType.DATA_RANGE;
import static com.github.owlcs.ontapi.internal.OWLComponentType.ENTITY;
import static com.github.owlcs.ontapi.internal.OWLComponentType.INDIVIDUAL;
import static com.github.owlcs.ontapi.internal.OWLComponentType.IRI;
import static com.github.owlcs.ontapi.internal.OWLComponentType.LITERAL;
import static com.github.owlcs.ontapi.internal.OWLComponentType.NAMED_OBJECT_PROPERTY;
import static com.github.owlcs.ontapi.internal.OWLComponentType.OBJECT_PROPERTY_EXPRESSION;
import static com.github.owlcs.ontapi.internal.OWLComponentType.SWRL_ATOM;

/**
 * Enum, that represents all public content meta-types of {@code OWLOntology}:
 * one for the ontology header ({@link #ANNOTATION}) and {@code 39} for each type of axiom.
 * The {@code OWLObject} corresponding to any of these constant is a container,
 * and may consist of {@code OWLObject}s described by the {@link OWLComponentType} enum-class.
 * For axioms there is a natural {@link AxiomType}'s order to provide a little faster iterating:
 * the declarations and widely used axioms go first, which is good for the data-factory and other caching mechanisms.
 */
public enum OWLTopObjectType {
    // an ontology header annotations
    ANNOTATION(null, false, ANNOTATION_PROPERTY, LITERAL, ANONYMOUS_INDIVIDUAL, IRI) {
        @Override
        public ObjectsSearcher<OWLObject> getSearcher() {
            return (m, f, c) -> BaseSearcher.cast(ReadHelper.listOWLAnnotations(m.getID(), f));
        }

        @Override
        public boolean isAxiom() {
            return false;
        }

        @Override
        boolean hasAnnotations(OWLObject container) { // not used
            return ((HasAnnotations) container).annotations().findFirst().isPresent();
        }

        @Override
        void write(OntModel m, OWLObject v) {
            WriteHelper.addAnnotations(m.getID(), Collections.singleton(((OWLAnnotation) v)));
        }
    },
    // axioms:
    DECLARATION(AxiomType.DECLARATION, true, ENTITY),
    EQUIVALENT_CLASSES(AxiomType.EQUIVALENT_CLASSES, CLASS_EXPRESSION),
    SUBCLASS_OF(AxiomType.SUBCLASS_OF, CLASS_EXPRESSION),
    DISJOINT_CLASSES(AxiomType.DISJOINT_CLASSES, CLASS_EXPRESSION),
    DISJOINT_UNION(AxiomType.DISJOINT_UNION, CLASS, CLASS_EXPRESSION),
    CLASS_ASSERTION(AxiomType.CLASS_ASSERTION, INDIVIDUAL, CLASS_EXPRESSION),
    SAME_INDIVIDUAL(AxiomType.SAME_INDIVIDUAL, INDIVIDUAL),
    DIFFERENT_INDIVIDUALS(AxiomType.DIFFERENT_INDIVIDUALS, INDIVIDUAL),
    OBJECT_PROPERTY_ASSERTION(AxiomType.OBJECT_PROPERTY_ASSERTION, true, NAMED_OBJECT_PROPERTY, INDIVIDUAL),
    NEGATIVE_OBJECT_PROPERTY_ASSERTION(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION, OBJECT_PROPERTY_EXPRESSION, INDIVIDUAL),
    DATA_PROPERTY_ASSERTION(AxiomType.DATA_PROPERTY_ASSERTION, true, DATATYPE_PROPERTY, LITERAL, INDIVIDUAL),
    NEGATIVE_DATA_PROPERTY_ASSERTION(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION, DATATYPE_PROPERTY, INDIVIDUAL, LITERAL),
    EQUIVALENT_OBJECT_PROPERTIES(AxiomType.EQUIVALENT_OBJECT_PROPERTIES, OBJECT_PROPERTY_EXPRESSION),
    SUB_OBJECT_PROPERTY(AxiomType.SUB_OBJECT_PROPERTY, OBJECT_PROPERTY_EXPRESSION),
    INVERSE_OBJECT_PROPERTIES(AxiomType.INVERSE_OBJECT_PROPERTIES, OBJECT_PROPERTY_EXPRESSION),
    FUNCTIONAL_OBJECT_PROPERTY(AxiomType.FUNCTIONAL_OBJECT_PROPERTY, OBJECT_PROPERTY_EXPRESSION),
    INVERSE_FUNCTIONAL_OBJECT_PROPERTY(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY, OBJECT_PROPERTY_EXPRESSION),
    SYMMETRIC_OBJECT_PROPERTY(AxiomType.SYMMETRIC_OBJECT_PROPERTY, OBJECT_PROPERTY_EXPRESSION),
    ASYMMETRIC_OBJECT_PROPERTY(AxiomType.ASYMMETRIC_OBJECT_PROPERTY, OBJECT_PROPERTY_EXPRESSION),
    TRANSITIVE_OBJECT_PROPERTY(AxiomType.TRANSITIVE_OBJECT_PROPERTY, OBJECT_PROPERTY_EXPRESSION),
    REFLEXIVE_OBJECT_PROPERTY(AxiomType.REFLEXIVE_OBJECT_PROPERTY, OBJECT_PROPERTY_EXPRESSION),
    IRREFLEXIVE_OBJECT_PROPERTY(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY, OBJECT_PROPERTY_EXPRESSION),
    OBJECT_PROPERTY_DOMAIN(AxiomType.OBJECT_PROPERTY_DOMAIN, OBJECT_PROPERTY_EXPRESSION, CLASS_EXPRESSION),
    OBJECT_PROPERTY_RANGE(AxiomType.OBJECT_PROPERTY_RANGE, OBJECT_PROPERTY_EXPRESSION, CLASS_EXPRESSION),
    DISJOINT_OBJECT_PROPERTIES(AxiomType.DISJOINT_OBJECT_PROPERTIES, OBJECT_PROPERTY_EXPRESSION),
    SUB_PROPERTY_CHAIN_OF(AxiomType.SUB_PROPERTY_CHAIN_OF, OBJECT_PROPERTY_EXPRESSION),
    EQUIVALENT_DATA_PROPERTIES(AxiomType.EQUIVALENT_DATA_PROPERTIES, DATATYPE_PROPERTY),
    SUB_DATA_PROPERTY(AxiomType.SUB_DATA_PROPERTY, true, DATATYPE_PROPERTY),
    FUNCTIONAL_DATA_PROPERTY(AxiomType.FUNCTIONAL_DATA_PROPERTY, true, DATATYPE_PROPERTY),
    DATA_PROPERTY_DOMAIN(AxiomType.DATA_PROPERTY_DOMAIN, DATATYPE_PROPERTY, CLASS_EXPRESSION),
    DATA_PROPERTY_RANGE(AxiomType.DATA_PROPERTY_RANGE, DATATYPE_PROPERTY, DATA_RANGE),
    DISJOINT_DATA_PROPERTIES(AxiomType.DISJOINT_DATA_PROPERTIES, DATATYPE_PROPERTY),
    HAS_KEY(AxiomType.HAS_KEY, CLASS_EXPRESSION, DATATYPE_PROPERTY, OBJECT_PROPERTY_EXPRESSION),
    SWRL_RULE(AxiomType.SWRL_RULE, SWRL_ATOM),
    ANNOTATION_ASSERTION(AxiomType.ANNOTATION_ASSERTION, true, ANNOTATION_PROPERTY, LITERAL, ANONYMOUS_INDIVIDUAL, IRI),
    SUB_ANNOTATION_PROPERTY_OF(AxiomType.SUB_ANNOTATION_PROPERTY_OF, true, ANNOTATION_PROPERTY),
    ANNOTATION_PROPERTY_RANGE(AxiomType.ANNOTATION_PROPERTY_RANGE, true, ANNOTATION_PROPERTY, IRI),
    ANNOTATION_PROPERTY_DOMAIN(AxiomType.ANNOTATION_PROPERTY_DOMAIN, true, ANNOTATION_PROPERTY, IRI),
    DATATYPE_DEFINITION(AxiomType.DATATYPE_DEFINITION, DATATYPE, DATA_RANGE),
    ;

    private static final Set<OWLTopObjectType> ALL_AXIOMS;
    private static final Set<OWLTopObjectType> LOGICAL_AXIOMS;

    static {
        Set<OWLTopObjectType> axioms = EnumSet.noneOf(OWLTopObjectType.class);
        Set<OWLTopObjectType> logical = EnumSet.noneOf(OWLTopObjectType.class);
        OWLTopObjectType[] array = values();
        for (int i = 1; i < array.length; i++) {
            OWLTopObjectType x = array[i];
            axioms.add(x);
            if (x.type.isLogical()) {
                logical.add(x);
            }
        }
        ALL_AXIOMS = axioms;
        LOGICAL_AXIOMS = logical;
    }

    private final AxiomType<OWLAxiom> type;
    private final boolean distinct;
    private final Set<OWLComponentType> components;

    OWLTopObjectType(AxiomType<? extends OWLAxiom> type, OWLComponentType... types) {
        this(type, false, types);
    }

    /**
     * @param type     {@link AxiomType} or {@code null} if ontology header
     * @param distinct see {@link #isDistinct()}
     * @param types    an {@code Array} of top-level components
     */
    @SuppressWarnings("unchecked")
    OWLTopObjectType(AxiomType<? extends OWLAxiom> type, boolean distinct, OWLComponentType... types) {
        this.type = (AxiomType<OWLAxiom>) type;
        this.distinct = distinct;
        this.components = OWLComponentType.toSet(types);
    }

    /**
     * Returns a {@link OWLTopObjectType} by the {@link AxiomType}.
     *
     * @param type {@link AxiomType}, not {@code null}
     * @return {@link OWLTopObjectType}, not {@code null}
     */
    public static OWLTopObjectType get(AxiomType<?> type) throws IndexOutOfBoundsException {
        return values()[type.getIndex() + 1];
    }

    /**
     * Returns a {@link OWLTopObjectType} by the {@link OWLAxiom}s {@code Class}-type.
     *
     * @param type {@link OWLAxiom} actual class-type
     * @return {@link OWLTopObjectType}, not {@code null}
     * @see AxiomType#getActualClass()
     */
    public static OWLTopObjectType get(Class<? extends OWLAxiom> type) {
        OWLTopObjectType[] array = values();
        for (int i = 1; i < array.length; i++) {
            OWLTopObjectType res = array[i];
            if (type == res.type.getActualClass()) return res;
        }
        throw new OntApiException.IllegalState();
    }

    /**
     * Lists all values as {@code Stream}.
     *
     * @return {@code Stream} of {@link OWLTopObjectType}s
     */
    public static Stream<OWLTopObjectType> all() {
        return Arrays.stream(values());
    }

    /**
     * Answers an iterator over all {@code 40} content types.
     *
     * @return {@link ExtendedIterator} of {@link OWLTopObjectType}s
     */
    static ExtendedIterator<OWLTopObjectType> listAll() {
        return Iterators.of(values());
    }

    /**
     * Lists all {@link OWLAxiom}s {@link OWLTopObjectType Meta-info}s.
     *
     * @return {@code Stream} of {@link OWLTopObjectType}s
     */
    public static Stream<OWLTopObjectType> axioms() {
        return ALL_AXIOMS.stream();
    }

    /**
     * List all logical axiom types.
     *
     * @return {@code Stream} of {@link OWLTopObjectType}s
     */
    public static Stream<OWLTopObjectType> logical() {
        return LOGICAL_AXIOMS.stream();
    }

    /**
     * Lists all {@link OWLAxiom}s {@link OWLTopObjectType Meta-info}s for the specified axioms {@code types}.
     *
     * @param types {@link Iterable}, not {@code null}
     * @return {@code Stream} of {@link OWLTopObjectType}s
     */
    public static Stream<OWLTopObjectType> axioms(Iterable<AxiomType<?>> types) {
        Stream<AxiomType<?>> res;
        if (types instanceof Collection) {
            res = ((Collection<AxiomType<?>>) types).stream();
        } else {
            res = StreamSupport.stream(types.spliterator(), false);
        }
        return res.map(OWLTopObjectType::get);
    }

    /**
     * Answers {@code true} iff it is meta-info for an axiom-type, not for {@link #ANNOTATION header}.
     *
     * @return boolean
     */
    public boolean isAxiom() {
        return true;
    }

    /**
     * Answers {@code true} if the given OWL object has annotations.
     * The OWL object must be either {@link OWLAxiom}
     * or ontology header {@link OWLAnnotation}.
     *
     * @param container {@link OWLObject}, not {@code null}
     * @return boolean
     */
    boolean hasAnnotations(OWLObject container) {
        return ((OWLAxiom) container).isAnnotated();
    }

    /**
     * Answers {@code true} iff
     * an object of this type can be derived from one and only one main triple,
     * and returns {@code false}, if several main triples can correspond to the same object.
     * Usually the method returns {@code false}, only few types allow mapping to be distinct.
     * An example of distinct type is {@link #DECLARATION}:
     * any triple {@code x rdf:type y} uniquely corresponds to the entity {@code x} of the type {@code y}.
     * Below are examples of non-distinct types:
     * <ul>
     * <li>{@link #DIFFERENT_INDIVIDUALS}: the same axiom {@code DifferentIndividuals(<A> <B>)} can be derived
     * form three statements: {@code a1 owl:differentFrom a2}, {@code a2 owl:differentFrom a1}
     * and {@code _:x rdf:type owl:AllDifferent . _:x owl:members ( a1 a2 ). }</li>
     * <li>{@link #DISJOINT_UNION}: it is possible to have different rdf-lists with the same content,
     * therefore a graph may contain different but similar statements that result the same axiom:
     * {@code C1 owl:disjointUnionOf ( C2 ) . C1 owl:disjointUnionOf ( C2 ) . }</li>
     * <li>{@link #ANNOTATION}: bulk annotation is a b-node, RDF graph can contain any number of b-nodes
     * with the same content (annotation property and annotation value)</li>
     * <li>{@link #FUNCTIONAL_OBJECT_PROPERTY}:
     * the axiom {@code FunctionalObjectProperty(ObjectInverseOf(P))} can be derived from
     * the any statements of the form {@code _:bi rdf:type owl:FunctionalProperty . _:bi owl:inverseOf P .}</li>
     * </ul>
     *
     * @return boolean
     * @see WithMerge
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Answers {@code true} if any {@code OWLObject}-container of this meta type
     * may contain the {@code OWLObject}-component of the specified component-type.
     * Note: axioms annotations are not taking into consideration,
     * in other words the real intersection by component possibility is more wide.
     *
     * @param container {@link OWLComponentType} - the type, not {@code null}
     * @return boolean
     */
    public boolean hasComponent(OWLComponentType container) {
        return components.contains(container);
    }

    /**
     * Answers a {@link AxiomType} for this enum or {@code null} if it is {@link #ANNOTATION header}.
     *
     * @return {@link AxiomType} or {@code null}
     */
    public AxiomType<OWLAxiom> getAxiomType() {
        return type;
    }

    /**
     * Writes the content-object into the graph.
     *
     * @param m     {@link OntModel ONT-API Jena Model}, to modify
     * @param value {@link OWLObject} - either {@link OWLAxiom} or {@link OWLAnnotation}, to write
     */
    void write(OntModel m, OWLObject value) {
        getTranslator().write((OWLAxiom) value, m);
    }

    /**
     * Provides a translator - the facility to read/write {@link OWLAxiom} in/from a graph.
     *
     * @return {@link AxiomTranslator}
     */
    AxiomTranslator<OWLAxiom> getTranslator() {
        return AxiomTranslator.get(type);
    }

    /**
     * Returns a searcher - the facility to list/find/test {@link OWLObject} in a graph.
     *
     * @return {@link ObjectsSearcher} for raw {@link OWLObject}s of this type
     */
    public ObjectsSearcher<OWLObject> getSearcher() {
        return BaseSearcher.cast(getTranslator());
    }
}
