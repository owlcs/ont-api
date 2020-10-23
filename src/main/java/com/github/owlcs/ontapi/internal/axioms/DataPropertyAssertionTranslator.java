/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
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
import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A translator that provides {@link OWLDataPropertyAssertionAxiom} implementations.
 * <p>
 * The pattern is {@code a R v}, where:
 * <ul>
 * <li>{@code a} - individual (named or anonymous)</li>
 * <li>{@code R} - data property</li>
 * <li>{@code v} - literal</li>
 * </ul>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class DataPropertyAssertionTranslator
        extends AbstractPropertyAssertionTranslator<OWLDataPropertyExpression, OWLDataPropertyAssertionAxiom> {

    @Override
    public void write(OWLDataPropertyAssertionAxiom axiom, OntModel model) {
        WriteHelper.writeAssertionTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getObject(),
                axiom.annotationsAsList());
    }

    /**
     * Lists positive data property assertions: the rule {@code a R v}.
     * See <a href='https://www.w3.org/TR/owl2-quick-reference/'>Assertions</a>
     *
     * @param model  {@link OntModel} the model
     * @param config {@link AxiomsSettings}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     */
    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        return listStatements(model).filterKeep(s -> testStatement(s, config));
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return statement.getObject().isLiteral()
                && statement.getPredicate().canAs(OntDataProperty.class)
                && statement.getSubject().canAs(OntIndividual.class);
    }

    @Override
    public ONTObject<OWLDataPropertyAssertionAxiom> toAxiomImpl(OntStatement statement,
                                                                ModelObjectFactory factory,
                                                                AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLDataPropertyAssertionAxiom> toAxiomWrap(OntStatement statement,
                                                                ONTObjectFactory factory,
                                                                AxiomsSettings config) {
        ONTObject<? extends OWLIndividual> i = factory.getIndividual(statement.getSubject(OntIndividual.class));
        ONTObject<OWLDataProperty> p = factory.getProperty(statement.getPredicate().as(OntDataProperty.class));
        ONTObject<OWLLiteral> literal = factory.getLiteral(statement.getLiteral());
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLDataPropertyAssertionAxiom res = factory.getOWLDataFactory()
                .getOWLDataPropertyAssertionAxiom(p.getOWLObject(), i.getOWLObject(), literal.getOWLObject(),
                        ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(i).append(p).append(literal);
    }

    @Override
    Triple createSearchTriple(OWLDataPropertyAssertionAxiom axiom) {
        Node subject = WriteHelper.getSearchNode(axiom.getSubject());
        if (subject == null) return null;
        Node property = WriteHelper.toNode(axiom.getProperty().asOWLDataProperty());
        Node object = WriteHelper.toNode(axiom.getObject());
        return Triple.create(subject, property, object);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLDataPropertyAssertionAxiomImpl
     */
    @SuppressWarnings("WeakerAccess")
    public static abstract class AxiomImpl
            extends AssertionImpl<OWLDataPropertyAssertionAxiom,
            OWLIndividual, OWLDataPropertyExpression, OWLLiteral>
            implements OWLDataPropertyAssertionAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link OWLDataPropertyAssertionAxiom} that is also {@link ONTObject}.
         *
         * @param statement {@link OntStatement}, the source, not {@code null}
         * @param factory   {@link ModelObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            return WithAssertion.create(statement,
                    SimpleImpl.FACTORY, WithAnnotationsImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public ONTObject<? extends OWLIndividual> findONTSubject(ModelObjectFactory factory) {
            return findByURIOrBlankId(subject, factory);
        }

        @Override
        public ONTObject<? extends OWLLiteral> findONTObject(ModelObjectFactory factory) {
            return factory.getLiteral((LiteralLabel) object);
        }

        @Override
        public ONTObject<? extends OWLDataProperty> findONTPredicate(ModelObjectFactory factory) {
            return findONTProperty(factory);
        }

        public ONTObject<OWLDataProperty> findONTProperty(ModelObjectFactory factory) {
            return factory.getDataProperty(predicate);
        }

        @FactoryAccessor
        @Override
        protected OWLDataPropertyAssertionAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLDataPropertyAssertionAxiom(getFPredicate(), getFSubject(),
                    getFObject(), annotations);
        }

        @FactoryAccessor
        @Override
        public OWLSubClassOfAxiom asOWLSubClassOfAxiom() {
            DataFactory df = getDataFactory();
            return df.getOWLSubClassOfAxiom(df.getOWLObjectOneOf(getFSubject()),
                    df.getOWLDataHasValue(getFPredicate(), getFObject()));
        }

        @Override
        public boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        @Override
        public boolean canContainAnonymousIndividuals() {
            return isAnnotated();
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
            return predicate.equals(ONTEntityImpl.getURI(property));
        }

        @Override
        public Set<OWLNamedIndividual> getNamedIndividualSet() {
            return hasURISubject() ? createSet(getSubject().asOWLNamedIndividual()) : createSet();
        }

        /**
         * An {@link OWLDataPropertyAssertionAxiom} that has no sub-annotations.
         */
        public static class SimpleImpl extends AxiomImpl
                implements Simple<OWLIndividual, OWLDataPropertyExpression, OWLLiteral> {

            private static final BiFunction<Triple, Supplier<OntModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            @Override
            public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
                return hasURISubject() ? createSet() : createSet(getSubject().asOWLAnonymousIndividual());
            }

            @Override
            public Set<OWLDatatype> getDatatypeSet() {
                return createSet(getONTObject().getOWLObject().getDatatype());
            }

            @Override
            public Set<OWLEntity> getSignatureSet() {
                Set<OWLEntity> res = createSortedSet();
                ModelObjectFactory factory = getObjectFactory();
                res.add(findONTProperty(factory).getOWLObject());
                if (hasURISubject()) {
                    res.add(findONTSubject(factory).getOWLObject().asOWLNamedIndividual());
                }
                res.add(findONTObject(factory).getOWLObject().getDatatype());
                return res;
            }

            @Override
            public boolean containsNamedIndividual(OWLNamedIndividual individual) {
                return getSubject().equals(individual);
            }
        }

        /**
         * An {@link OWLDataPropertyAssertionAxiom} that has sub-annotations.
         * This class has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class WithAnnotationsImpl extends AxiomImpl
                implements WithAnnotations<WithAnnotationsImpl, OWLIndividual, OWLDataPropertyExpression, OWLLiteral> {

            private static final BiFunction<Triple, Supplier<OntModel>, WithAnnotationsImpl> FACTORY =
                    WithAnnotationsImpl::new;
            protected final InternalCache.Loading<WithAnnotationsImpl, Object[]> content;

            public WithAnnotationsImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
                this.content = createContentCache();
            }

            @Override
            public InternalCache.Loading<WithAnnotationsImpl, Object[]> getContentCache() {
                return content;
            }
        }
    }
}
