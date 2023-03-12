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

package com.github.owlcs.ontapi.internal.axioms;

import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.jena.model.OntModel;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import java.util.function.Supplier;

/**
 * A common super-type for all property assertion axiom translators.
 * There are 3 assertion axioms:
 * <ul>
 * <li>annotation assertion ({@code s A t})</li>
 * <li>object property assertion ({@code a1 PN a2})</li>
 * <li>data property assertion {@code a R v}</li>
 * </ul>
 * Where:
 * <ul>
 * <li>{@code s} - IRI or anonymous individual</li>
 * <li>{@code t} - IRI, anonymous individual, or literal</li>
 * <li>{@code v} - literal</li>
 * <li>{@code a} - individual</li>
 * <li>{@code A} - annotation property</li>
 * <li>{@code R} - data property</li>
 * <li>{@code PN} - named object property expression</li>
 * </ul>
 * <p>
 * Created by @ssz on 26.05.2019.
 *
 * @param <P> either annotation, data or object property
 * @param <A> corresponding property assertion axiom
 */
public abstract class AbstractPropertyAssertionTranslator<P extends OWLPropertyExpression,
        A extends OWLAxiom & HasProperty<P>> extends AbstractSimpleTranslator<A> {

    @Override
    boolean testSearchTriple(Triple t) {
        return true;
    }

    /**
     * A base for annotation and positive property assertion axioms.
     *
     * @param <A> - assertion axiom, subtype of {@link OWLAxiom}
     * @param <S> - subject, subtype of {@link OWLObject}
     * @param <P> - predicate, subtype of {@link OWLObject}
     * @param <O> - object, subtype of {@link OWLObject}
     */
    @SuppressWarnings("WeakerAccess")
    protected static abstract class AssertionImpl<A extends OWLAxiom,
            S extends OWLObject, P extends OWLObject, O extends OWLObject>
            extends ONTAxiomImpl<A> implements WithAssertion<S, P, O> {

        protected AssertionImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        @Override
        public S getSubject() {
            return getONTSubject().getOWLObject();
        }

        @Override
        public O getValue() {
            return getONTObject().getOWLObject();
        }

        public O getObject() {
            return getValue();
        }

        @Override
        public P getProperty() {
            return getONTPredicate().getOWLObject();
        }

        @Override
        protected final boolean sameContent(ONTStatementImpl other) {
            return false;
        }

        @Override
        public final boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public final boolean canContainClassExpressions() {
            return false;
        }

        protected final ONTObject<? extends OWLIndividual> findByURIOrBlankId(Object id, ModelObjectFactory factory) {
            return id instanceof String ? factory.getNamedIndividual((String) id) : factory.getAnonymousIndividual((BlankNodeId) id);
        }
    }
}
