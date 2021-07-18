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

import java.util.Map;
import java.util.Objects;

/**
 * Axiom Graph Translator accessor.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public abstract class AxiomParserProvider {

    private static final Map<AxiomType<?>, AxiomTranslator<? extends OWLAxiom>> MAP = Map.ofEntries(
            Map.entry(AxiomType.DATA_PROPERTY_DOMAIN, new DataPropertyDomainTranslator()),
            Map.entry(AxiomType.SAME_INDIVIDUAL, new SameIndividualTranslator()),
            Map.entry(AxiomType.SUB_OBJECT_PROPERTY, new SubObjectPropertyOfTranslator()),
            Map.entry(AxiomType.ASYMMETRIC_OBJECT_PROPERTY, new AsymmetricObjectPropertyTranslator()),
            Map.entry(AxiomType.FUNCTIONAL_OBJECT_PROPERTY, new FunctionalObjectPropertyTranslator()),
            Map.entry(AxiomType.ANNOTATION_ASSERTION, new AnnotationAssertionTranslator()),
            Map.entry(AxiomType.DISJOINT_UNION, new DisjointUnionTranslator()),
            Map.entry(AxiomType.SWRL_RULE, new SWRLRuleTranslator()),
            Map.entry(AxiomType.EQUIVALENT_CLASSES, new EquivalentClassesTranslator()),
            Map.entry(AxiomType.ANNOTATION_PROPERTY_RANGE, new AnnotationPropertyRangeTranslator()),
            Map.entry(AxiomType.DATATYPE_DEFINITION, new DatatypeDefinitionTranslator()),
            Map.entry(AxiomType.DISJOINT_OBJECT_PROPERTIES, new DisjointObjectPropertiesTranslator()),
            Map.entry(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY, new InverseFunctionalObjectPropertyTranslator()),
            Map.entry(AxiomType.DATA_PROPERTY_ASSERTION, new DataPropertyAssertionTranslator()),
            Map.entry(AxiomType.INVERSE_OBJECT_PROPERTIES, new InverseObjectPropertiesTranslator()),
            Map.entry(AxiomType.REFLEXIVE_OBJECT_PROPERTY, new ReflexiveObjectPropertyTranslator()),
            Map.entry(AxiomType.DIFFERENT_INDIVIDUALS, new DifferentIndividualsTranslator()),
            Map.entry(AxiomType.FUNCTIONAL_DATA_PROPERTY, new FunctionalDataPropertyTranslator()),
            Map.entry(AxiomType.DATA_PROPERTY_RANGE, new DataPropertyRangeTranslator()),
            Map.entry(AxiomType.EQUIVALENT_OBJECT_PROPERTIES, new EquivalentObjectPropertiesTranslator()),
            Map.entry(AxiomType.OBJECT_PROPERTY_RANGE, new ObjectPropertyRangeTranslator()),
            Map.entry(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION, new NegativeDataPropertyAssertionTranslator()),
            Map.entry(AxiomType.SUB_PROPERTY_CHAIN_OF, new SubPropertyChainOfTranslator()),
            Map.entry(AxiomType.ANNOTATION_PROPERTY_DOMAIN, new AnnotationPropertyDomainTranslator()),
            Map.entry(AxiomType.TRANSITIVE_OBJECT_PROPERTY, new TransitiveObjectPropertyTranslator()),
            Map.entry(AxiomType.EQUIVALENT_DATA_PROPERTIES, new EquivalentDataPropertiesTranslator()),
            Map.entry(AxiomType.DISJOINT_DATA_PROPERTIES, new DisjointDataPropertiesTranslator()),
            Map.entry(AxiomType.OBJECT_PROPERTY_DOMAIN, new ObjectPropertyDomainTranslator()),
            Map.entry(AxiomType.SUB_ANNOTATION_PROPERTY_OF, new SubAnnotationPropertyOfTranslator()),
            Map.entry(AxiomType.SUBCLASS_OF, new SubClassOfTranslator()),
            Map.entry(AxiomType.DISJOINT_CLASSES, new DisjointClassesTranslator()),
            Map.entry(AxiomType.SYMMETRIC_OBJECT_PROPERTY, new SymmetricObjectPropertyTranslator()),
            Map.entry(AxiomType.SUB_DATA_PROPERTY, new SubDataPropertyOfTranslator()),
            Map.entry(AxiomType.DECLARATION, new DeclarationTranslator()),
            Map.entry(AxiomType.OBJECT_PROPERTY_ASSERTION, new ObjectPropertyAssertionTranslator()),
            Map.entry(AxiomType.CLASS_ASSERTION, new ClassAssertionTranslator()),
            Map.entry(AxiomType.HAS_KEY, new HasKeyTranslator()),
            Map.entry(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY, new IrreflexiveObjectPropertyTranslator()),
            Map.entry(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION, new NegativeObjectPropertyAssertionTranslator())
    );

    /**
     * Returns the parsers {@code Map}.
     *
     * @return the {@code Map} with {@link AxiomType}-keys that contains {@link AxiomTranslator}s
     */
    public static Map<AxiomType<?>, AxiomTranslator<? extends OWLAxiom>> getParsers() {
        return MAP;
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
