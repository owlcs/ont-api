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

import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTClassImpl;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntProperty;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;

import java.util.function.Supplier;

/**
 * The base class for {@link ObjectPropertyDomainTranslator},
 * {@link DataPropertyDomainTranslator} and {@link AnnotationPropertyDomainTranslator} axioms.
 * All of them are based on a statement with {@code rdfs:domain} predicate.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public abstract class AbstractPropertyDomainTranslator<Axiom extends OWLAxiom & HasDomain<?> & HasProperty<?>,
        P extends OntProperty> extends AxiomTranslator<Axiom> {
    @Override
    public void write(Axiom axiom, OntModel model) {
        WriteHelper.writeTriple(model, axiom.getProperty(), RDFS.domain, axiom.getDomain(), axiom.annotationsAsList());
    }

    abstract Class<P> getView();

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, InternalConfig config) {
        return listByPredicate(model, RDFS.domain).filterKeep(s -> filter(s, config));
    }

    protected boolean filter(OntStatement statement, InternalConfig config) {
        return statement.getSubject().canAs(getView());
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return RDFS.domain.equals(statement.getPredicate()) && filter(statement, config);
    }

    /**
     * @param <A> either {@link OWLDataPropertyDomainAxiom}
     *            or {@link OWLObjectPropertyDomainAxiom} or {@link OWLAnnotationPropertyDomainAxiom}
     * @param <P> either {@link OWLAnnotationProperty}
     *            or {@link OWLDataPropertyExpression} or {@link OWLObjectPropertyExpression}
     * @param <D> either {@link OWLClassExpression} or {@link IRI}
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract static class DomainAxiomImpl<A extends OWLAxiom & HasProperty<P> & HasDomain<D>,
            P extends OWLPropertyExpression, D extends OWLObject>
            extends ONTAxiomImpl<A> implements WithTwoObjects<P, D> {

        protected DomainAxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        protected DomainAxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        public P getProperty() {
            return getONTSubject().getOWLObject();
        }

        public D getDomain() {
            return getONTObject().getOWLObject();
        }
    }

    /**
     * @param <A> either {@link OWLDataPropertyDomainAxiom} or {@link OWLObjectPropertyDomainAxiom}
     * @param <P> either {@link OWLDataPropertyExpression} or {@link OWLObjectPropertyExpression}
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract static class ClassDomainAxiomImpl<A extends OWLAxiom & HasProperty<P> & HasDomain<OWLClassExpression>,
            P extends OWLPropertyExpression> extends DomainAxiomImpl<A, P, OWLClassExpression> {

        protected ClassDomainAxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        protected ClassDomainAxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        @Override
        public final boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        @Override
        public ONTObject<? extends OWLClassExpression> getURIObject(ModelObjectFactory factory) {
            return ONTClassImpl.find(getObjectURI(), factory, model);
        }

        @Override
        public ONTObject<? extends OWLClassExpression> objectFromStatement(OntStatement statement,
                                                                           ModelObjectFactory factory) {
            return factory.getClass(statement.getObject(OntClass.class));
        }
    }
}
