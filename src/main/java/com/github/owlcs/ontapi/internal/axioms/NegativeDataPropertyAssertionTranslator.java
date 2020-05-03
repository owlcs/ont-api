/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.axioms;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntNegativeAssertion;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.owlapi.objects.OWLLiteralImpl;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLNegativeDataPropertyAssertionAxiom} implementations.
 * Example:
 * <pre>{@code
 * [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :dataProp; owl:targetValue "TEST"^^xsd:string ]
 * }</pre>
 * Created by szuev on 12.10.2016.
 */
public class NegativeDataPropertyAssertionTranslator
        extends AbstractNegativePropertyAssertionTranslator<OWLNegativeDataPropertyAssertionAxiom, OntNegativeAssertion.WithDataProperty> {

    @Override
    OntNegativeAssertion.WithDataProperty createNPA(OWLNegativeDataPropertyAssertionAxiom axiom, OntModel model) {
        return WriteHelper.addDataProperty(model, axiom.getProperty())
                .addNegativeAssertion(WriteHelper.addIndividual(model, axiom.getSubject()),
                        WriteHelper.addLiteral(model, axiom.getObject()));
    }

    @Override
    Class<OntNegativeAssertion.WithDataProperty> getView() {
        return OntNegativeAssertion.WithDataProperty.class;
    }

    @Override
    public ONTObject<OWLNegativeDataPropertyAssertionAxiom> toAxiomImpl(OntStatement statement,
                                                                        ModelObjectFactory factory,
                                                                        InternalConfig config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLNegativeDataPropertyAssertionAxiom> toAxiomWrap(OntStatement statement,
                                                                        InternalObjectFactory factory,
                                                                        InternalConfig config) {
        OntNegativeAssertion.WithDataProperty npa = statement.getSubject(getView());
        ONTObject<? extends OWLIndividual> s = factory.getIndividual(npa.getSource());
        ONTObject<OWLDataProperty> p = factory.getProperty(npa.getProperty());
        ONTObject<OWLLiteral> o = factory.getLiteral(npa.getTarget());
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLNegativeDataPropertyAssertionAxiom res = factory.getOWLDataFactory()
                .getOWLNegativeDataPropertyAssertionAxiom(p.getOWLObject(),
                        s.getOWLObject(), o.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, npa).append(annotations).append(s).append(p).append(o);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLNegativeDataPropertyAssertionAxiomImpl
     */
    public static class AxiomImpl
            extends NegativeAssertionImpl<OntNegativeAssertion.WithDataProperty, OWLNegativeDataPropertyAssertionAxiom,
            OWLDataPropertyExpression, OWLLiteral>
            implements OWLNegativeDataPropertyAssertionAxiom {

        private static final BiFunction<Triple, Supplier<OntModel>, AxiomImpl> FACTORY = AxiomImpl::new;

        public AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link OWLNegativeDataPropertyAssertionAxiom} that is also {@link ONTObject}.
         *
         * @param statement {@link OntStatement}, the source, not {@code null}
         * @param factory   {@link ModelObjectFactory}, not {@code null}
         * @param config    {@link InternalConfig}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       InternalConfig config) {
            return WithAssertion.create(statement, FACTORY, SET_HASH_CODE, factory, config);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntModel> m) {
            super(s, p, o, m);
        }

        @Override
        public Class<OntNegativeAssertion.WithDataProperty> getType() {
            return OntNegativeAssertion.WithDataProperty.class;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Object fromObject(ONTObject o) {
            return OWLLiteralImpl.asONT((OWLLiteral) o.getOWLObject()).getLiteralLabel();
        }

        @Override
        public ONTObject<? extends OWLLiteral> toObject(Object o, ModelObjectFactory factory) {
            return factory.getLiteral((LiteralLabel) o);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Object fromPredicate(ONTObject o) {
            OWLDataProperty property = (OWLDataProperty) o.getOWLObject();
            return ONTEntityImpl.getURI(property);
        }

        @Override
        public ONTObject<? extends OWLDataProperty> toPredicate(Object p, ModelObjectFactory factory) {
            return factory.getDataProperty((String) p);
        }

        @Override
        public ONTObject<? extends OWLDataProperty> fetchONTPredicate(OntStatement statement,
                                                                      ModelObjectFactory factory) {
            return factory.getProperty(getResource(statement).getProperty());
        }

        @Override
        public ONTObject<? extends OWLLiteral> fetchONTObject(OntStatement statement,
                                                              ModelObjectFactory factory) {
            return factory.getLiteral(getResource(statement).getTarget());
        }

        @Override
        public boolean containsAnonymousIndividuals() {
            return getContent()[0] instanceof BlankNodeId;
        }

        @FactoryAccessor
        @Override
        public OWLSubClassOfAxiom asOWLSubClassOfAxiom() {
            DataFactory df = getDataFactory();
            return df.getOWLSubClassOfAxiom(df.getOWLObjectOneOf(getFSubject()),
                    df.getOWLObjectComplementOf(df.getOWLDataHasValue(getFPredicate(), getFObject())));
        }

        @FactoryAccessor
        @Override
        protected OWLNegativeDataPropertyAssertionAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            DataFactory df = getDataFactory();
            return df.getOWLNegativeDataPropertyAssertionAxiom(getFPredicate(), getFSubject(), getFObject(),
                    annotations);
        }

        @Override
        protected AxiomImpl makeCopy(ONTObject<OWLNegativeDataPropertyAssertionAxiom> other) {
            return new AxiomImpl(subject, predicate, object, model) {
                @Override
                public Stream<Triple> triples() {
                    return Stream.concat(AxiomImpl.this.triples(), other.triples());
                }
            };
        }

        @Override
        public boolean canContainObjectProperties() {
            return false;
        }

        @Override
        public Set<OWLDataProperty> getDataPropertySet() {
            return createSet(getProperty().asOWLDataProperty());
        }

        @Override
        public boolean containsDataProperty(OWLDataProperty property) {
            return getProperty().equals(property);
        }

        @Override
        public Set<OWLNamedIndividual> getNamedIndividualSet() {
            Object[] content = getContent();
            if (content[0] instanceof String) {
                Set<OWLNamedIndividual> res = createSortedSet();
                res.add(toNamedIndividual((String) content[0], getObjectFactory()).getOWLObject());
                return res;
            }
            return createSet();
        }

        @Override
        public boolean containsNamedIndividual(OWLNamedIndividual individual) {
            Object[] content = getContent();
            return content[0] instanceof String && content[0].equals(ONTEntityImpl.getURI(individual));
        }
    }
}
