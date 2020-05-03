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
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntNegativeAssertion;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLNegativeObjectPropertyAssertionAxiom} implementations.
 * Example:
 * <pre>{@code
 * [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :objProp; owl:targetIndividual :ind2 ] .
 * }</pre>
 * Created by szuev on 12.10.2016.
 */
public class NegativeObjectPropertyAssertionTranslator
        extends AbstractNegativePropertyAssertionTranslator<OWLNegativeObjectPropertyAssertionAxiom, OntNegativeAssertion.WithObjectProperty> {

    @Override
    OntNegativeAssertion.WithObjectProperty createNPA(OWLNegativeObjectPropertyAssertionAxiom axiom, OntModel model) {
        return WriteHelper.addObjectProperty(model, axiom.getProperty())
                .addNegativeAssertion(WriteHelper.addIndividual(model, axiom.getSubject()),
                        WriteHelper.addIndividual(model, axiom.getObject()));
    }

    @Override
    Class<OntNegativeAssertion.WithObjectProperty> getView() {
        return OntNegativeAssertion.WithObjectProperty.class;
    }

    @Override
    public ONTObject<OWLNegativeObjectPropertyAssertionAxiom> toAxiomImpl(OntStatement statement,
                                                                          ModelObjectFactory factory,
                                                                          AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLNegativeObjectPropertyAssertionAxiom> toAxiomWrap(OntStatement statement,
                                                                          InternalObjectFactory factory,
                                                                          AxiomsSettings config) {
        OntNegativeAssertion.WithObjectProperty npa = statement.getSubject(getView());
        ONTObject<? extends OWLIndividual> s = factory.getIndividual(npa.getSource());
        ONTObject<? extends OWLObjectPropertyExpression> p = factory.getProperty(npa.getProperty());
        ONTObject<? extends OWLIndividual> o = factory.getIndividual(npa.getTarget());
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLNegativeObjectPropertyAssertionAxiom res = factory.getOWLDataFactory()
                .getOWLNegativeObjectPropertyAssertionAxiom(p.getOWLObject(),
                        s.getOWLObject(), o.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, npa).append(annotations).append(s).append(p).append(o);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLNegativeObjectPropertyAssertionAxiomImpl
     */
    public static class AxiomImpl
            extends NegativeAssertionImpl<OntNegativeAssertion.WithObjectProperty, OWLNegativeObjectPropertyAssertionAxiom,
            OWLObjectPropertyExpression, OWLIndividual> implements OWLNegativeObjectPropertyAssertionAxiom {

        private static final BiFunction<Triple, Supplier<OntModel>, AxiomImpl> FACTORY = AxiomImpl::new;

        public AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link OWLNegativeObjectPropertyAssertionAxiom} that is also {@link ONTObject}.
         *
         * @param statement {@link OntStatement}, the source, not {@code null}
         * @param factory   {@link InternalObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            return WithAssertion.create(statement, FACTORY, SET_HASH_CODE, factory, config);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntModel> m) {
            super(s, p, o, m);
        }

        @Override
        public Class<OntNegativeAssertion.WithObjectProperty> getType() {
            return OntNegativeAssertion.WithObjectProperty.class;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Object fromObject(ONTObject o) {
            return fromIndividual((OWLIndividual) o.getOWLObject());
        }

        @Override
        public ONTObject<? extends OWLIndividual> toObject(Object o, ModelObjectFactory factory) {
            return toIndividual(o, factory);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Object fromPredicate(ONTObject o) {
            OWLObjectPropertyExpression ope = (OWLObjectPropertyExpression) o.getOWLObject();
            return ope.isOWLObjectProperty() ? ONTEntityImpl.getURI(ope.asOWLObjectProperty()) : o;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> toPredicate(Object p, ModelObjectFactory factory) {
            return p instanceof String ? factory.getObjectProperty((String) p) : (ONTObject<? extends OWLObjectPropertyExpression>) p;
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> fetchONTPredicate(OntStatement statement,
                                                                                  ModelObjectFactory factory) {
            return factory.getProperty(getResource(statement).getProperty());
        }

        @Override
        public ONTObject<? extends OWLIndividual> fetchONTObject(OntStatement statement,
                                                                 ModelObjectFactory factory) {
            return factory.getIndividual(getResource(statement).getTarget());
        }

        @Override
        public boolean containsAnonymousIndividuals() {
            Object[] content = getContent();
            return content[0] instanceof BlankNodeId && content[2] instanceof BlankNodeId;
        }

        @Override
        protected AxiomImpl makeCopy(ONTObject<OWLNegativeObjectPropertyAssertionAxiom> other) {
            return new AxiomImpl(subject, predicate, object, model) {
                @Override
                public Stream<Triple> triples() {
                    return Stream.concat(AxiomImpl.this.triples(), other.triples());
                }
            };
        }

        @FactoryAccessor
        @Override
        public OWLSubClassOfAxiom asOWLSubClassOfAxiom() {
            DataFactory df = getDataFactory();
            return df.getOWLSubClassOfAxiom(df.getOWLObjectOneOf(getFSubject()),
                    df.getOWLObjectComplementOf(df.getOWLObjectHasValue(getFPredicate(), getFObject())));
        }

        @FactoryAccessor
        @Override
        protected OWLNegativeObjectPropertyAssertionAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            DataFactory df = getDataFactory();
            return df.getOWLNegativeObjectPropertyAssertionAxiom(getFPredicate(), getFSubject(), getFObject(),
                    annotations);
        }

        @Override
        public boolean canContainDataProperties() {
            return false;
        }

        @Override
        public boolean canContainDatatypes() {
            return isAnnotated();
        }

        @Override
        public Set<OWLObjectProperty> getObjectPropertySet() {
            return createSet(getProperty().getNamedProperty());
        }

        @Override
        public boolean containsObjectProperty(OWLObjectProperty property) {
            return getProperty().getNamedProperty().equals(property);
        }

        @Override
        public Set<OWLNamedIndividual> getNamedIndividualSet() {
            Set<OWLNamedIndividual> res = createSortedSet();
            Object[] content = getContent();
            ModelObjectFactory factory = getObjectFactory();
            if (content[0] instanceof String) {
                res.add(toNamedIndividual((String) content[0], factory).getOWLObject());
            }
            if (content[2] instanceof String) {
                res.add(toNamedIndividual((String) content[2], factory).getOWLObject());
            }
            return res;
        }

        @Override
        public boolean containsNamedIndividual(OWLNamedIndividual individual) {
            Object[] content = getContent();
            String uri = null;
            return content[0] instanceof String
                    && content[0].equals(uri = ONTEntityImpl.getURI(individual))
                    || content[2] instanceof String
                    && content[2].equals(uri == null ? ONTEntityImpl.getURI(individual) : uri);
        }
    }
}