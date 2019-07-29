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

import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.Iter;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static ru.avicomp.ontapi.internal.OWLComponent.*;

/**
 * Enum, that represents all public content meta-types of {@code OWLOntology}:
 * one for the ontology header ({@link #ANNOTATION}) and {@code 39} for each type of axiom.
 * The {@code OWLObject} corresponding to any of these constant is a container,
 * and may consist of {@code OWLObject}s described by the {@link OWLComponent} class.
 * For axioms there is a natural {@link AxiomType}'s order to provide a little bit faster iterating:
 * the declarations and widely used axioms go first, which is good for the data-factory and other caching.
 */
public enum OWLContentType {
    // a header annotation
    ANNOTATION(null, false, ANNOTATION_PROPERTY, LITERAL, ANONYMOUS_INDIVIDUAL, IRI) {
        @Override
        public boolean isAxiom() {
            return false;
        }

        @Override
        boolean hasAnnotations(OWLObject container) { // not used
            return ((HasAnnotations) container).annotations().findFirst().isPresent();
        }

        @Override
        ExtendedIterator<? extends ONTObject<? extends OWLObject>> read(OntGraphModel m,
                                                                        InternalObjectFactory f,
                                                                        InternalConfig c) {
            return ReadHelper.listOWLAnnotations(m.getID(), f);
        }

        @Override
        void write(OntGraphModel m, OWLObject v) {
            WriteHelper.addAnnotations(m.getID(), Stream.of((OWLAnnotation) v));
        }
    },
    DECLARATION(AxiomType.DECLARATION, true, ENTITY),
    EQUIVALENT_CLASSES(AxiomType.EQUIVALENT_CLASSES, true, CLASS_EXPRESSION),
    SUBCLASS_OF(AxiomType.SUBCLASS_OF, true, CLASS_EXPRESSION),
    DISJOINT_CLASSES(AxiomType.DISJOINT_CLASSES, false, CLASS_EXPRESSION),
    DISJOINT_UNION(AxiomType.DISJOINT_UNION, false, CLASS, CLASS_EXPRESSION),
    CLASS_ASSERTION(AxiomType.CLASS_ASSERTION, true, INDIVIDUAL, CLASS_EXPRESSION),
    SAME_INDIVIDUAL(AxiomType.SAME_INDIVIDUAL, false, INDIVIDUAL),
    DIFFERENT_INDIVIDUALS(AxiomType.DIFFERENT_INDIVIDUALS, false, INDIVIDUAL),
    OBJECT_PROPERTY_ASSERTION(AxiomType.OBJECT_PROPERTY_ASSERTION, true, NAMED_OBJECT_PROPERTY, INDIVIDUAL),
    NEGATIVE_OBJECT_PROPERTY_ASSERTION(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION, false, OBJECT_PROPERTY_EXPRESSION, INDIVIDUAL),
    DATA_PROPERTY_ASSERTION(AxiomType.DATA_PROPERTY_ASSERTION, true, DATATYPE_PROPERTY, LITERAL, INDIVIDUAL),
    NEGATIVE_DATA_PROPERTY_ASSERTION(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION, false, DATATYPE_PROPERTY, INDIVIDUAL, LITERAL),
    EQUIVALENT_OBJECT_PROPERTIES(AxiomType.EQUIVALENT_OBJECT_PROPERTIES, false, OBJECT_PROPERTY_EXPRESSION),
    SUB_OBJECT_PROPERTY(AxiomType.SUB_OBJECT_PROPERTY, true, OBJECT_PROPERTY_EXPRESSION),
    INVERSE_OBJECT_PROPERTIES(AxiomType.INVERSE_OBJECT_PROPERTIES, false, OBJECT_PROPERTY_EXPRESSION),
    FUNCTIONAL_OBJECT_PROPERTY(AxiomType.FUNCTIONAL_OBJECT_PROPERTY, true, OBJECT_PROPERTY_EXPRESSION),
    INVERSE_FUNCTIONAL_OBJECT_PROPERTY(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY, true, OBJECT_PROPERTY_EXPRESSION),
    SYMMETRIC_OBJECT_PROPERTY(AxiomType.SYMMETRIC_OBJECT_PROPERTY, true, OBJECT_PROPERTY_EXPRESSION),
    ASYMMETRIC_OBJECT_PROPERTY(AxiomType.ASYMMETRIC_OBJECT_PROPERTY, true, OBJECT_PROPERTY_EXPRESSION),
    TRANSITIVE_OBJECT_PROPERTY(AxiomType.TRANSITIVE_OBJECT_PROPERTY, true, OBJECT_PROPERTY_EXPRESSION),
    REFLEXIVE_OBJECT_PROPERTY(AxiomType.REFLEXIVE_OBJECT_PROPERTY, true, OBJECT_PROPERTY_EXPRESSION),
    IRREFLEXIVE_OBJECT_PROPERTY(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY, true, OBJECT_PROPERTY_EXPRESSION),
    OBJECT_PROPERTY_DOMAIN(AxiomType.OBJECT_PROPERTY_DOMAIN, true, OBJECT_PROPERTY_EXPRESSION, CLASS_EXPRESSION),
    OBJECT_PROPERTY_RANGE(AxiomType.OBJECT_PROPERTY_RANGE, true, OBJECT_PROPERTY_EXPRESSION, CLASS_EXPRESSION),
    DISJOINT_OBJECT_PROPERTIES(AxiomType.DISJOINT_OBJECT_PROPERTIES, false, OBJECT_PROPERTY_EXPRESSION),
    SUB_PROPERTY_CHAIN_OF(AxiomType.SUB_PROPERTY_CHAIN_OF, false, OBJECT_PROPERTY_EXPRESSION),
    EQUIVALENT_DATA_PROPERTIES(AxiomType.EQUIVALENT_DATA_PROPERTIES, false, DATATYPE_PROPERTY),
    SUB_DATA_PROPERTY(AxiomType.SUB_DATA_PROPERTY, true, DATATYPE_PROPERTY),
    FUNCTIONAL_DATA_PROPERTY(AxiomType.FUNCTIONAL_DATA_PROPERTY, true, DATATYPE_PROPERTY),
    DATA_PROPERTY_DOMAIN(AxiomType.DATA_PROPERTY_DOMAIN, true, DATATYPE_PROPERTY, CLASS_EXPRESSION),
    DATA_PROPERTY_RANGE(AxiomType.DATA_PROPERTY_RANGE, true, DATATYPE_PROPERTY, DATA_RANGE),
    DISJOINT_DATA_PROPERTIES(AxiomType.DISJOINT_DATA_PROPERTIES, false, DATATYPE_PROPERTY),
    HAS_KEY(AxiomType.HAS_KEY, false, CLASS_EXPRESSION, DATATYPE_PROPERTY, OBJECT_PROPERTY_EXPRESSION),
    SWRL_RULE(AxiomType.SWRL_RULE, false, SWRL_ATOM),
    ANNOTATION_ASSERTION(AxiomType.ANNOTATION_ASSERTION, true, ANNOTATION_PROPERTY, LITERAL, ANONYMOUS_INDIVIDUAL, IRI),
    SUB_ANNOTATION_PROPERTY_OF(AxiomType.SUB_ANNOTATION_PROPERTY_OF, true, ANNOTATION_PROPERTY),
    ANNOTATION_PROPERTY_RANGE(AxiomType.ANNOTATION_PROPERTY_RANGE, true, ANNOTATION_PROPERTY, IRI),
    ANNOTATION_PROPERTY_DOMAIN(AxiomType.ANNOTATION_PROPERTY_DOMAIN, true, ANNOTATION_PROPERTY, IRI),
    DATATYPE_DEFINITION(AxiomType.DATATYPE_DEFINITION, true, DATATYPE, DATA_RANGE),
    ;

    public static final List<OWLContentType> AXIOMS = all().skip(1).collect(Iter.toUnmodifiableList());
    public static final List<OWLContentType> LOGICAL = AXIOMS.stream().filter(x -> x.type.isLogical()).collect(Iter.toUnmodifiableList());

    private final AxiomType<OWLAxiom> type;
    private final boolean distinct;
    private final Set<OWLComponent> components;

    @SuppressWarnings("unchecked")
    OWLContentType(AxiomType<? extends OWLAxiom> type, boolean distinct, OWLComponent... types) {
        this.type = (AxiomType<OWLAxiom>) type;
        this.distinct = distinct;
        this.components = OWLComponent.toSet(types);
    }

    /**
     * Returns a {@link OWLContentType} by the {@link AxiomType}.
     *
     * @param type {@link AxiomType}, not {@code null}
     * @return {@link OWLContentType}, not {@code null}
     */
    public static OWLContentType get(AxiomType<?> type) throws IndexOutOfBoundsException {
        return values()[type.getIndex() + 1];
    }

    /**
     * Returns a {@link OWLContentType} to which the specified object corresponds.
     *
     * @param o {@link OWLObject}, a content-container, not {@code nul;}
     * @return {@link OWLContentType}, not {@code null}
     */
    static OWLContentType get(OWLObject o) {
        if (o instanceof OWLAnnotation) {
            return ANNOTATION;
        }
        if (o instanceof OWLAxiom) {
            return get(((OWLAxiom) o).getAxiomType());
        }
        throw new OntApiException.IllegalArgument();
    }

    /**
     * Returns a {@link OWLContentType} by the {@link OWLAxiom}s {@code Class}-type.
     *
     * @param type {@link OWLAxiom} actual class-type
     * @return {@link OWLContentType}, not {@code null}
     * @see AxiomType#getActualClass()
     */
    public static OWLContentType get(Class<? extends OWLAxiom> type) {
        OWLContentType[] array = values();
        for (int i = 1; i < array.length; i++) {
            OWLContentType res = array[i];
            if (type == res.type.getActualClass()) return res;
        }
        throw new OntApiException.IllegalState();
    }

    /**
     * Lists all values as {@code Stream}.
     *
     * @return {@code Stream} of {@link OWLContentType}s
     */
    public static Stream<OWLContentType> all() {
        return Arrays.stream(values());
    }

    /**
     * Answers an iterator over all {@code 40} content types.
     *
     * @return {@link ExtendedIterator} of {@link OWLContentType}s
     */
    static ExtendedIterator<OWLContentType> iterator() {
        return Iter.of(values());
    }

    /**
     * Lists all {@link OWLAxiom}s {@link OWLContentType Meta-info}s.
     *
     * @return {@code Stream} of {@link OWLContentType}s
     */
    public static Stream<OWLContentType> axioms() {
        return AXIOMS.stream();
    }

    public static Stream<OWLContentType> logical() {
        return LOGICAL.stream();
    }

    /**
     * Lists all {@link OWLAxiom}s {@link OWLContentType Meta-info}s for the specified axioms {@code types}.
     *
     * @param types {@link Iterable}, not {@code null}
     * @return {@code Stream} of {@link OWLContentType}s
     */
    public static Stream<OWLContentType> axioms(Iterable<AxiomType<?>> types) {
        Stream<AxiomType<?>> res;
        if (types instanceof Collection) {
            res = ((Collection<AxiomType<?>>) types).stream();
        } else {
            res = StreamSupport.stream(types.spliterator(), false);
        }
        return res.map(OWLContentType::get);
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
     * or ontology header {@link org.semanticweb.owlapi.model.OWLAnnotation}.
     *
     * @param container {@link OWLObject}, not {@code null}
     * @return boolean
     */
    boolean hasAnnotations(OWLObject container) {
        return ((OWLAxiom) container).isAnnotated();
    }

    /**
     * Answers {@code true} if and only if
     * there can be only one unique content object of this enum-type,
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
     * <li>{@link #ANNOTATION}: bulk annotation is a b-node, RDF graph can contain any number of b-nodes
     * with the same content (annotation property and annotation value)</li>
     * </ul>
     *
     * @return boolean
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
     * @param container {@link OWLComponent} - the type, not {@code null}
     * @return boolean
     */
    public boolean hasComponent(OWLComponent container) {
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
     * Reads content-objects from the graph.
     *
     * @param m {@link OntGraphModel ONT-API Jena Model}, to search over
     * @param f {@link InternalObjectFactory} to construct OWL-API Objects (wrapped as {@link ONTObject})
     * @param c {@link InternalConfig} to control process
     * @return {@link ExtendedIterator} over all content objects, found in modelr for this type
     */
    ExtendedIterator<? extends ONTObject<? extends OWLObject>> read(OntGraphModel m,
                                                                    InternalObjectFactory f,
                                                                    InternalConfig c) {
        return getTranslator().listAxioms(m, f, c);
    }

    /**
     * Writes the content-object into the graph.
     *
     * @param m     {@link OntGraphModel ONT-API Jena Model}, to modify
     * @param value {@link OWLObject} - either {@link OWLAxiom} or {@link OWLAnnotation}
     */
    @SuppressWarnings("unchecked")
    void write(OntGraphModel m, OWLObject value) {
        ((AxiomTranslator<OWLAxiom>) getTranslator()).write((OWLAxiom) value, m);
    }

    /**
     * Provides a translator - the facility to read/write {@link OWLAxiom} in/from a graph.
     *
     * @return {@link AxiomTranslator}
     */
    AxiomTranslator<? extends OWLAxiom> getTranslator() {
        return AxiomParserProvider.get(type);
    }

}
