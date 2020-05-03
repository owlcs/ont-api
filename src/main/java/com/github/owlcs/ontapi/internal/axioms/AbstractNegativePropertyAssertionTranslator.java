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

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntNegativeAssertion;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.owlapi.objects.OWLAnonymousIndividualImpl;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.*;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An abstraction for data and object negative property assertion.
 * Sub-classes:
 * {@link NegativeDataPropertyAssertionTranslator}
 * {@link NegativeObjectPropertyAssertionTranslator}
 * <p>
 * Created by szuev on 29.11.2016.
 */
public abstract class AbstractNegativePropertyAssertionTranslator<Axiom extends OWLPropertyAssertionAxiom<?, ?>,
        NPA extends OntNegativeAssertion<?, ?>> extends AxiomTranslator<Axiom> {

    abstract NPA createNPA(Axiom axiom, OntModel model);

    abstract Class<NPA> getView();

    @Override
    public void write(Axiom axiom, OntModel model) {
        WriteHelper.addAnnotations(createNPA(axiom, model), axiom.annotationsAsList());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        return listByPredicateAndObject(model, RDF.type, OWL.NegativePropertyAssertion)
                .mapWith(s -> {
                    NPA res = s.getSubject().getAs(getView());
                    return res != null ? res.getMainStatement() : null;
                }).filterDrop(Objects::isNull);
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return statement.getObject().equals(OWL.NegativePropertyAssertion)
                && statement.isDeclaration()
                && statement.getSubject().canAs(getView());
    }

    /**
     * A base for data or object negative assertions
     *
     * @param <R> - either {@link OntNegativeAssertion.WithObjectProperty} or {@link OntNegativeAssertion.WithDataProperty}
     * @param <A> - either {@link OWLNegativeObjectPropertyAssertionAxiom}
     *            or {@link OWLNegativeDataPropertyAssertionAxiom}, that matches {@link R}
     * @param <P> - either {@link OWLObjectPropertyExpression} or {@link OWLDataProperty}
     * @param <O> - either {@link OWLIndividual} or {@link OWLLiteral}
     */
    @SuppressWarnings({"WeakerAccess", "rawtypes"})
    protected static abstract class NegativeAssertionImpl<R extends OntNegativeAssertion<?, ?>,
            A extends OWLPropertyAssertionAxiom<?, ?>,
            P extends OWLPropertyExpression, O extends OWLObject> extends ONTAxiomImpl<A>
            implements WithAssertion.Complex<NegativeAssertionImpl<?, ?, ?, ?>, OWLIndividual, P, O> {

        protected final InternalCache.Loading<NegativeAssertionImpl<?, ?, ?, ?>, Object[]> content;

        protected NegativeAssertionImpl(Triple t, Supplier<OntModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected NegativeAssertionImpl(Object s, String p, Object o, Supplier<OntModel> m) {
            super(s, p, o, m);
            this.content = createContentCache();
        }

        @Override
        public InternalCache.Loading<NegativeAssertionImpl<?, ?, ?, ?>, Object[]> getContentCache() {
            return content;
        }

        @Override
        protected boolean sameContent(ONTStatementImpl other) {
            return other instanceof NegativeAssertionImpl<?, ?, ?, ?>
                    && Arrays.equals(getContent(), ((NegativeAssertionImpl<?, ?, ?, ?>) other).getContent());
        }

        /**
         * Gets the {@link OntNegativeAssertion} type (object or data).
         *
         * @return {@code Class} - a type of {@link R}
         */
        protected abstract Class<R> getType();

        @Override
        public OntStatement asStatement() {
            return asResource().getMainStatement();
        }

        /**
         * Represents this axioms as ONT-API Jena Resource.
         *
         * @return {@link R}
         */
        public final R asResource() {
            return getPersonalityModel().getNodeAs(getSubjectNode(), getType());
        }

        /**
         * Extracts a {@link R}-resource from the {@code statement}
         *
         * @param statement {@link OntStatement} - the source, not {@code null}
         * @return {@link R}
         */
        protected final R getResource(OntStatement statement) {
            return statement.getSubject(getType());
        }

        @Override
        public Stream<Triple> triples() {
            return Stream.concat(asResource().spec().map(FrontsTriple::asTriple),
                    objects().flatMap(ONTObject::triples));
        }

        @Override
        public ONTObject<? extends OWLIndividual> fetchONTSubject(OntStatement statement,
                                                                  ModelObjectFactory factory) {
            return factory.getIndividual(getResource(statement).getSource());
        }

        @Override
        public OWLIndividual getSubject() {
            return getONTSubject().getOWLObject();
        }

        public O getObject() {
            return getONTObject().getOWLObject();
        }

        @Override
        public P getProperty() {
            return getONTPredicate().getOWLObject();
        }

        @Override
        public Object fromSubject(ONTObject o) {
            return fromIndividual((OWLIndividual) o.getOWLObject());
        }

        @Override
        public ONTObject<? extends OWLIndividual> toSubject(Object s, ModelObjectFactory factory) {
            return toIndividual(s, factory);
        }

        protected ONTObject<? extends OWLIndividual> toIndividual(Object s, ModelObjectFactory factory) {
            return s instanceof String ?
                    toNamedIndividual((String) s, factory) :
                    toAnonymousIndividual((BlankNodeId) s, factory);
        }

        protected ONTObject<OWLNamedIndividual> toNamedIndividual(String uri, ModelObjectFactory factory) {
            return factory.getNamedIndividual(uri);
        }

        protected ONTObject<OWLAnonymousIndividual> toAnonymousIndividual(BlankNodeId id, ModelObjectFactory factory) {
            return factory.getAnonymousIndividual(id);
        }

        protected Object fromIndividual(OWLIndividual i) {
            if (i.isOWLNamedIndividual()) {
                return ONTEntityImpl.getURI(i.asOWLNamedIndividual());
            }
            return OWLAnonymousIndividualImpl.asONT(i.asOWLAnonymousIndividual()).getBlankNodeId();
        }

        @SuppressWarnings("unchecked")
        @Override
        public final ONTObject<A> merge(ONTObject<A> other) {
            if (this == other) {
                return this;
            }
            if (other instanceof NegativeAssertionImpl && sameTriple((NegativeAssertionImpl) other)) {
                return this;
            }
            NegativeAssertionImpl res = makeCopy(other);
            if (hasContent()) {
                res.putContent(getContent());
            }
            res.hashCode = hashCode;
            return res;
        }

        /**
         * Creates a copy of this axiom with additional triples from the specified axiom (that must equal to this).
         *
         * @param other {@link ONTObject} to get triples
         * @return {@link NegativeAssertionImpl}
         */
        protected abstract NegativeAssertionImpl makeCopy(ONTObject<A> other);

        @Override
        public final boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public final boolean canContainClassExpressions() {
            return false;
        }

        @Override
        public final boolean canContainAnnotationProperties() {
            return isAnnotated();
        }
    }
}
