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

package ru.avicomp.ontapi.internal;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntApiException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Axiom Graph Translator accessor.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public abstract class AxiomParserProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AxiomParserProvider.class);

    public static Map<AxiomType, AxiomTranslator<? extends OWLAxiom>> getParsers() {
        return ParserHolder.PARSERS;
    }

    /**
     * Returns the {@link AxiomTranslator Axiom Translator} for the specified class-type.
     *
     * @param type {@link Class}, not null
     * @param <A>  subclass of {@link OWLAxiom}
     * @return {@link AxiomTranslator} of the type of {@link A}
     */
    public static <A extends OWLAxiom> AxiomTranslator<A> get(Class<A> type) {
        return get(AxiomType.getTypeForClass(type));
    }

    /**
     * Finds the {@link AxiomTranslator Axiom Translator} for the given {@link OWLAxiom OWL Axiom}.
     *
     * @param axiom axiom, not null
     * @param <A>   subclass of {@link OWLAxiom}
     * @return {@link AxiomTranslator} of the type of {@link A}
     */
    @SuppressWarnings("unchecked")
    public static <A extends OWLAxiom> AxiomTranslator<A> get(A axiom) {
        return (AxiomTranslator<A>) get(OntApiException.notNull(axiom, "Null axiom.").getAxiomType());
    }

    /**
     * Returns the {@link AxiomTranslator Axiom Translator} for the specified {@link AxiomType Axiom Type}.
     *
     * @param type {@link AxiomType}, not null
     * @param <A>  subclass of {@link OWLAxiom}
     * @return {@link AxiomTranslator} of the type of {@link A}
     */
    @SuppressWarnings("unchecked")
    public static <A extends OWLAxiom> AxiomTranslator<A> get(AxiomType<A> type) {
        return OntApiException.notNull((AxiomTranslator<A>) getParsers().get(OntApiException.notNull(type, "Null axiom type")),
                "Can't find parser for axiom " + type.getActualClass());
    }

    @SuppressWarnings("unchecked")
    static <A extends OWLAxiom> AxiomTranslator<A> getByType(AxiomType<? extends OWLAxiom> type) {
        return (AxiomTranslator<A>) get(type);
    }

    private static class ParserHolder {

        // 39 axiom types:
        private static final List<Class<? extends AxiomTranslator>> TRANSLATORS = Arrays.asList(
                ru.avicomp.ontapi.internal.DataPropertyDomainTranslator.class,
                ru.avicomp.ontapi.internal.SameIndividualTranslator.class,
                ru.avicomp.ontapi.internal.SubObjectPropertyOfTranslator.class,
                ru.avicomp.ontapi.internal.AsymmetricObjectPropertyTranslator.class,
                ru.avicomp.ontapi.internal.FunctionalObjectPropertyTranslator.class,
                ru.avicomp.ontapi.internal.AnnotationAssertionTranslator.class,
                ru.avicomp.ontapi.internal.DisjointUnionTranslator.class,
                ru.avicomp.ontapi.internal.SWRLRuleTranslator.class,
                ru.avicomp.ontapi.internal.EquivalentClassesTranslator.class,
                ru.avicomp.ontapi.internal.AnnotationPropertyRangeTranslator.class,
                ru.avicomp.ontapi.internal.DatatypeDefinitionTranslator.class,
                ru.avicomp.ontapi.internal.DisjointObjectPropertiesTranslator.class,
                ru.avicomp.ontapi.internal.InverseFunctionalObjectPropertyTranslator.class,
                ru.avicomp.ontapi.internal.DataPropertyAssertionTranslator.class,
                ru.avicomp.ontapi.internal.InverseObjectPropertiesTranslator.class,
                ru.avicomp.ontapi.internal.ReflexiveObjectPropertyTranslator.class,
                ru.avicomp.ontapi.internal.DifferentIndividualsTranslator.class,
                ru.avicomp.ontapi.internal.FunctionalDataPropertyTranslator.class,
                ru.avicomp.ontapi.internal.DataPropertyRangeTranslator.class,
                ru.avicomp.ontapi.internal.EquivalentObjectPropertiesTranslator.class,
                ru.avicomp.ontapi.internal.ObjectPropertyRangeTranslator.class,
                ru.avicomp.ontapi.internal.NegativeDataPropertyAssertionTranslator.class,
                ru.avicomp.ontapi.internal.SubPropertyChainOfTranslator.class,
                ru.avicomp.ontapi.internal.AnnotationPropertyDomainTranslator.class,
                ru.avicomp.ontapi.internal.TransitiveObjectPropertyTranslator.class,
                ru.avicomp.ontapi.internal.EquivalentDataPropertiesTranslator.class,
                ru.avicomp.ontapi.internal.DisjointDataPropertiesTranslator.class,
                ru.avicomp.ontapi.internal.ObjectPropertyDomainTranslator.class,
                ru.avicomp.ontapi.internal.SubAnnotationPropertyOfTranslator.class,
                ru.avicomp.ontapi.internal.SubClassOfTranslator.class,
                ru.avicomp.ontapi.internal.DisjointClassesTranslator.class,
                ru.avicomp.ontapi.internal.SymmetricObjectPropertyTranslator.class,
                ru.avicomp.ontapi.internal.SubDataPropertyOfTranslator.class,
                ru.avicomp.ontapi.internal.DeclarationTranslator.class,
                ru.avicomp.ontapi.internal.ObjectPropertyAssertionTranslator.class,
                ru.avicomp.ontapi.internal.ClassAssertionTranslator.class,
                ru.avicomp.ontapi.internal.HasKeyTranslator.class,
                ru.avicomp.ontapi.internal.IrreflexiveObjectPropertyTranslator.class,
                ru.avicomp.ontapi.internal.NegativeObjectPropertyAssertionTranslator.class
        );
        private static final Map<AxiomType, AxiomTranslator<? extends OWLAxiom>> PARSERS = init();

        static {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.trace("There are following axiom-parsers (" + PARSERS.size() + "): ");
                PARSERS.forEach((t, p) -> LOGGER.trace("{} ::: {}", t, p.getClass()));
            }
        }

        @SuppressWarnings("unchecked")
        private static Map<AxiomType, AxiomTranslator<? extends OWLAxiom>> init() {
            Map<AxiomType, AxiomTranslator<? extends OWLAxiom>> res = new HashMap<>();
            // to be sure that list of types are exactly the same in ont-api as in owl-api:
            AxiomType.AXIOM_TYPES.forEach(type -> {
                Class<? extends AxiomTranslator> parserClass = findParserClass(type);
                try {
                    res.put(type, parserClass.newInstance());
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new OntApiException("Can't instance parser for type: " + type, e);
                }
            });
            return res;
        }

        private static Class<? extends AxiomTranslator> findParserClass(AxiomType<? extends OWLAxiom> type) {
            return ParserHolder.TRANSLATORS.stream()
                    .filter(p -> isRelatedToAxiom(p, type.getActualClass()))
                    .findFirst()
                    .orElseThrow(() -> new OntApiException("Can't find parser class for type '" + type + "'"));
        }

        private static boolean isRelatedToAxiom(Class<? extends AxiomTranslator> parserClass,
                                                Class<? extends OWLAxiom> actualClass) {
            ParameterizedType type = ((ParameterizedType) parserClass.getGenericSuperclass());
            if (type == null) return false;
            for (Type t : type.getActualTypeArguments()) {
                if (actualClass.getName().equals(t.getTypeName())) return true;
            }
            return false;
        }
    }


}
