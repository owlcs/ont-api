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

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.InternalObjectFactory;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.jena.model.OntList;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLSubPropertyChainOfAxiom} implementations.
 * Example in turtle:
 * <pre>{@code
 * owl:topObjectProperty owl:propertyChainAxiom ( :ob-prop-1 :ob-prop-2 ) .
 * }</pre>
 * <p>
 * Created by @szuev on 18.10.2016.
 *
 * @see <a href='https://www.w3.org/TR/owl2-syntax/#Object_Subproperties'>9.2.1 Object Subproperties</a>
 */
public class SubPropertyChainOfTranslator
        extends AbstractListBasedTranslator<OWLSubPropertyChainOfAxiom, OntObjectProperty,
        OWLObjectPropertyExpression, OntObjectProperty, OWLObjectPropertyExpression> {
    @Override
    OWLObject getSubject(OWLSubPropertyChainOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    Property getPredicate() {
        return OWL.propertyChainAxiom;
    }

    @Override
    Collection<? extends OWLObject> getObjects(OWLSubPropertyChainOfAxiom axiom) {
        return axiom.getPropertyChain();
    }

    @Override
    Class<OntObjectProperty> getView() {
        return OntObjectProperty.class;
    }

    @Override
    public ONTObject<OWLSubPropertyChainOfAxiom> toAxiomImpl(OntStatement statement,
                                                             ModelObjectFactory factory,
                                                             AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLSubPropertyChainOfAxiom> toAxiomWrap(OntStatement statement,
                                                             InternalObjectFactory factory,
                                                             AxiomsSettings config) {
        return makeAxiom(statement,
                factory::getProperty,
                OntObjectProperty::findPropertyChain,
                factory::getProperty,
                Collectors.toList(),
                (s, m) -> factory.getOWLDataFactory().getOWLSubPropertyChainOfAxiom(m.stream()
                                .map(ONTObject::getOWLObject).collect(Collectors.toList()), s.getOWLObject(),
                        ONTObject.toSet(factory.getAnnotations(statement, config))));
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLSubPropertyChainAxiomImpl
     */
    @SuppressWarnings("WeakerAccess")
    public static class AxiomImpl
            extends WithListImpl<OWLSubPropertyChainOfAxiom, OntObjectProperty>
            implements WithList.Sequent<OWLSubPropertyChainOfAxiom,
            OWLObjectPropertyExpression, OWLObjectPropertyExpression>, OWLSubPropertyChainOfAxiom {

        private static final BiFunction<Triple, Supplier<OntModel>, AxiomImpl> FACTORY = AxiomImpl::new;

        public AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        protected AxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        @Override
        protected OntList<OntObjectProperty> findList(OntStatement statement) {
            return statement.getSubject(OntObjectProperty.class).findPropertyChain(statement.getObject(RDFList.class))
                    .orElseThrow(() -> new OntApiException.IllegalState("Can't find []-list in " + statement));
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLObjectPropertyExpression>> listONTComponents(OntStatement statement,
                                                                                                    ModelObjectFactory factory) {
            return OntModels.listMembers(findList(statement)).mapWith(factory::getProperty);
        }

        /**
         * Creates an {@link ONTObject} container that is also {@link  OWLSubPropertyChainOfAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link InternalObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            return WithList.Sequent.create(statement, FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public List<OWLObjectPropertyExpression> getPropertyChain() {
            return members().map(ONTObject::getOWLObject).collect(Collectors.toList());
        }

        @Override
        public OWLObjectPropertyExpression getSuperProperty() {
            return getONTSubject().getOWLObject();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public ONTObject fromContentItem(Object x, ModelObjectFactory factory) {
            return x instanceof String ? findPropertyByURI((String) x, factory) : (ONTObject) x;
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> findSubjectByURI(String uri,
                                                                                 ModelObjectFactory factory) {
            return findPropertyByURI(uri, factory);
        }

        private ONTObject<OWLObjectProperty> findPropertyByURI(String uri, ModelObjectFactory factory) {
            return factory.getObjectProperty(uri);
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> fetchONTSubject(OntStatement statement,
                                                                                ModelObjectFactory factory) {
            return factory.getProperty(statement.getSubject(OntObjectProperty.class));
        }

        @FactoryAccessor
        @Override
        protected OWLSubPropertyChainOfAxiom createAnnotatedAxiom(Object[] content,
                                                                  ModelObjectFactory factory,
                                                                  Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLSubPropertyChainOfAxiom(members(content, factory)
                            .map(x -> eraseModel(x.getOWLObject())).collect(Collectors.toList()),
                    eraseModel(findONTSubject(content[0], factory).getOWLObject()), annotations);
        }

        @Override
        public boolean isEncodingOfTransitiveProperty() {
            return isEncodingOfTransitiveProperty(getContent(), getObjectFactory());
        }

        protected boolean isEncodingOfTransitiveProperty(Object[] content, ModelObjectFactory factory) {
            if (content.length < 3) {
                return false;
            }
            List<ONTObject<?>> members = members(content, factory).collect(Collectors.toList());
            if (members.size() != 2)
                return false;
            ONTObject<?> subject = findONTSubject(content[0], factory);
            return subject.equals(members.get(0)) && subject.equals(members.get(1));
        }

        @Override
        public AxiomImpl makeCopy(ONTObject<OWLSubPropertyChainOfAxiom> other) {
            return new AxiomImpl(subject, predicate, object, model) {
                @Override
                public Stream<Triple> triples() {
                    return Stream.concat(AxiomImpl.this.triples(), other.triples());
                }
            };
        }

        @Override
        public final boolean canContainDatatypes() {
            return isAnnotated();
        }

        @Override
        public final boolean canContainAnonymousIndividuals() {
            return isAnnotated();
        }

        @Override
        public final boolean canContainDataProperties() {
            return false;
        }

        @Override
        public final boolean canContainClassExpressions() {
            return false;
        }

        @Override
        public final boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public final boolean canContainNamedIndividuals() {
            return false;
        }
    }
}
