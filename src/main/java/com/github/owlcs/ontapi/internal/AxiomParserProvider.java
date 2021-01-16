/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2021, owl.cs group.
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

import com.github.owlcs.ontapi.internal.axioms.*;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Axiom Graph Translator accessor.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public abstract class AxiomParserProvider {

    private static final Map<AxiomType<?>, AxiomTranslator<? extends OWLAxiom>> MAP =
            new HashMap<AxiomType<?>, AxiomTranslator<? extends OWLAxiom>>() {
                {
                    add(AxiomType.DATA_PROPERTY_DOMAIN, new DataPropertyDomainTranslator());
                    add(AxiomType.SAME_INDIVIDUAL, new SameIndividualTranslator());
                    add(AxiomType.SUB_OBJECT_PROPERTY, new SubObjectPropertyOfTranslator());
                    add(AxiomType.ASYMMETRIC_OBJECT_PROPERTY, new AsymmetricObjectPropertyTranslator());
                    add(AxiomType.FUNCTIONAL_OBJECT_PROPERTY, new FunctionalObjectPropertyTranslator());
                    add(AxiomType.ANNOTATION_ASSERTION, new AnnotationAssertionTranslator());
                    add(AxiomType.DISJOINT_UNION, new DisjointUnionTranslator());
                    add(AxiomType.SWRL_RULE, new SWRLRuleTranslator());
                    add(AxiomType.EQUIVALENT_CLASSES, new EquivalentClassesTranslator());
                    add(AxiomType.ANNOTATION_PROPERTY_RANGE, new AnnotationPropertyRangeTranslator());
                    add(AxiomType.DATATYPE_DEFINITION, new DatatypeDefinitionTranslator());
                    add(AxiomType.DISJOINT_OBJECT_PROPERTIES, new DisjointObjectPropertiesTranslator());
                    add(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY, new InverseFunctionalObjectPropertyTranslator());
                    add(AxiomType.DATA_PROPERTY_ASSERTION, new DataPropertyAssertionTranslator());
                    add(AxiomType.INVERSE_OBJECT_PROPERTIES, new InverseObjectPropertiesTranslator());
                    add(AxiomType.REFLEXIVE_OBJECT_PROPERTY, new ReflexiveObjectPropertyTranslator());
                    add(AxiomType.DIFFERENT_INDIVIDUALS, new DifferentIndividualsTranslator());
                    add(AxiomType.FUNCTIONAL_DATA_PROPERTY, new FunctionalDataPropertyTranslator());
                    add(AxiomType.DATA_PROPERTY_RANGE, new DataPropertyRangeTranslator());
                    add(AxiomType.EQUIVALENT_OBJECT_PROPERTIES, new EquivalentObjectPropertiesTranslator());
                    add(AxiomType.OBJECT_PROPERTY_RANGE, new ObjectPropertyRangeTranslator());
                    add(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION, new NegativeDataPropertyAssertionTranslator());
                    add(AxiomType.SUB_PROPERTY_CHAIN_OF, new SubPropertyChainOfTranslator());
                    add(AxiomType.ANNOTATION_PROPERTY_DOMAIN, new AnnotationPropertyDomainTranslator());
                    add(AxiomType.TRANSITIVE_OBJECT_PROPERTY, new TransitiveObjectPropertyTranslator());
                    add(AxiomType.EQUIVALENT_DATA_PROPERTIES, new EquivalentDataPropertiesTranslator());
                    add(AxiomType.DISJOINT_DATA_PROPERTIES, new DisjointDataPropertiesTranslator());
                    add(AxiomType.OBJECT_PROPERTY_DOMAIN, new ObjectPropertyDomainTranslator());
                    add(AxiomType.SUB_ANNOTATION_PROPERTY_OF, new SubAnnotationPropertyOfTranslator());
                    add(AxiomType.SUBCLASS_OF, new SubClassOfTranslator());
                    add(AxiomType.DISJOINT_CLASSES, new DisjointClassesTranslator());
                    add(AxiomType.SYMMETRIC_OBJECT_PROPERTY, new SymmetricObjectPropertyTranslator());
                    add(AxiomType.SUB_DATA_PROPERTY, new SubDataPropertyOfTranslator());
                    add(AxiomType.DECLARATION, new DeclarationTranslator());
                    add(AxiomType.OBJECT_PROPERTY_ASSERTION, new ObjectPropertyAssertionTranslator());
                    add(AxiomType.CLASS_ASSERTION, new ClassAssertionTranslator());
                    add(AxiomType.HAS_KEY, new HasKeyTranslator());
                    add(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY, new IrreflexiveObjectPropertyTranslator());
                    add(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION, new NegativeObjectPropertyAssertionTranslator());

                }

                private <X extends OWLAxiom> void add(AxiomType<X> key, AxiomTranslator<X> value) {
                    put(key, value);
                }
            };

    /**
     * Returns the parsers {@code Map}.
     *
     * @return the {@code Map} with {@link AxiomType}-keys that contains {@link AxiomTranslator}s
     */
    public static Map<AxiomType<?>, AxiomTranslator<? extends OWLAxiom>> getParsers() {
        return Collections.unmodifiableMap(MAP);
    }

    /**
     * Returns the {@link AxiomTranslator Axiom Translator} for the specified class-type.
     *
     * @param type {@link Class}, not null
     * @param <A>  subclass of {@link OWLAxiom}
     * @return {@link AxiomTranslator} of the type of {@link A}
     * @deprecated use {@link AxiomTranslator#get(AxiomType)}
     */
    @Deprecated
    public static <A extends OWLAxiom> AxiomTranslator<A> get(Class<A> type) {
        return getTranslator(AxiomType.getTypeForClass(type));
    }

    /**
     * Finds the {@link AxiomTranslator Axiom Translator} for the given {@link OWLAxiom OWL Axiom}.
     *
     * @param axiom axiom, not null
     * @param <A>   subclass of {@link OWLAxiom}
     * @return {@link AxiomTranslator} of the type of {@link A}
     * @deprecated use {@link AxiomTranslator#get(AxiomType)}
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static <A extends OWLAxiom> AxiomTranslator<A> get(A axiom) {
        return (AxiomTranslator<A>) getTranslator(Objects.requireNonNull(axiom, "Null axiom.").getAxiomType());
    }

    /**
     * Returns the {@link AxiomTranslator Axiom Translator} for the specified {@link AxiomType Axiom Type}.
     *
     * @param type {@link AxiomType}, not null
     * @param <A>  subclass of {@link OWLAxiom}
     * @return {@link AxiomTranslator} of the type of {@link A}
     * @deprecated use {@link AxiomTranslator#get(AxiomType)}
     */
    @Deprecated
    public static <A extends OWLAxiom> AxiomTranslator<A> get(AxiomType<A> type) {
        return getTranslator(type);
    }

    @SuppressWarnings("unchecked")
    static <A extends OWLAxiom> AxiomTranslator<A> getTranslator(AxiomType<A> type) {
        return Objects.requireNonNull((AxiomTranslator<A>) MAP.get(Objects.requireNonNull(type, "Null axiom type")),
                "Can't find parser for " + type);
    }

}
