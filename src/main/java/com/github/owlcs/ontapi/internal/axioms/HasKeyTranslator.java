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

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.OntModelSupport;
import com.github.owlcs.ontapi.internal.WriteHelper;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontapi.OntModelControls;
import org.apache.jena.ontapi.model.OntClass;
import org.apache.jena.ontapi.model.OntList;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntObject;
import org.apache.jena.ontapi.model.OntRelationalProperty;
import org.apache.jena.ontapi.model.OntStatement;
import org.apache.jena.ontapi.utils.OntModels;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLHasKeyAxiom} implementations.
 * Example in turtle:
 * <pre>{@code
 * :MyClass1 owl:hasKey ( :ob-prop-1 ) .
 * }</pre>
 * <p>
 * Created by @ssz on 17.10.2016.
 *
 * @see <a href='https://www.w3.org/TR/owl2-syntax/#Keys'>9.5 Keys</a>
 */
public class HasKeyTranslator
        extends AbstractListBasedTranslator<OWLHasKeyAxiom, OntClass, OWLClassExpression, OntRelationalProperty, OWLPropertyExpression> {
    @Override
    OWLObject getSubject(OWLHasKeyAxiom axiom) {
        return axiom.getClassExpression();
    }

    @Override
    Property getPredicate() {
        return OWL.hasKey;
    }

    @Override
    Collection<? extends OWLObject> getObjects(OWLHasKeyAxiom axiom) {
        return axiom.getOperandsAsList();
    }

    @Override
    Class<OntClass> getView() {
        return OntClass.class;
    }

    @Override
    OntModelControls control() {
        return OntModelControls.USE_OWL2_CLASS_HAS_KEY_FEATURE;
    }

    @Override
    public void write(OWLHasKeyAxiom axiom, OntModel model) {
        if (!isAxiomSupported(model)) {
            throw new OntApiException.Unsupported(
                    axiom + " cannot be added: prohibited by the profile " + OntModelSupport.profileName(model)
            );
        }
        OntClass s = (OntClass) WriteHelper.addRDFNode(model, getSubject(axiom)).as(OntObject.class);
        if (!s.canAsSubClass()) {
            throw new OntApiException.Unsupported(
                    axiom + " cannot be added: prohibited by the profile " + OntModelSupport.profileName(model)
            );
        }
        WriteHelper.addAnnotations(
                s.addStatement(getPredicate(), WriteHelper.addRDFList(model, getObjects(axiom))),
                axiom.annotationsAsList()
        );
    }

    @Override
    protected boolean filter(OntStatement statement) {
        if (!isAxiomSupported(statement.getModel())) {
            return false;
        }
        return statement.getSubject().canAs(getView())
                && statement.getSubject(getView()).canAsSubClass()
                && statement.getObject().canAs(RDFList.class);
    }

    @Override
    public ONTObject<OWLHasKeyAxiom> toAxiomImpl(OntStatement statement,
                                                 ModelObjectFactory factory,
                                                 AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLHasKeyAxiom> toAxiomWrap(OntStatement statement,
                                                 ONTObjectFactory factory,
                                                 AxiomsSettings config) {
        return makeAxiom(statement, factory::getClass, OntClass::findHasKey, factory::getProperty, Collectors.toSet(),
                (s, m) -> factory.getOWLDataFactory().getOWLHasKeyAxiom(s.getOWLObject(),
                        TranslateHelper.toSet(m),
                        TranslateHelper.toSet(factory.getAnnotations(statement, config))));
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.HasKeyAxiomImpl
     */
    public static class AxiomImpl
            extends WithListImpl<OWLHasKeyAxiom, OntRelationalProperty>
            implements WithList.Sorted<OWLHasKeyAxiom, OWLClassExpression, OWLPropertyExpression>, OWLHasKeyAxiom {

        private static final BiFunction<Triple, Supplier<OntModel>, AxiomImpl> FACTORY = AxiomImpl::new;

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        protected AxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        /**
         * Creates an {@link ONTObject} container that is also {@link  OWLHasKeyAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link ONTObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            return Sorted.create(statement, FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        protected OntList<OntRelationalProperty> findList(OntStatement statement) {
            return statement.getSubject(OntClass.class).findHasKey(statement.getObject(RDFList.class))
                    .orElseThrow(() -> new OntApiException.IllegalState("Can't find []-list in " + statement));
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLPropertyExpression>> listONTComponents(OntStatement statement,
                                                                                              ModelObjectFactory factory) {
            return OntModels.listMembers(findList(statement)).mapWith(factory::getProperty);
        }

        @Override
        public OWLClassExpression getClassExpression() {
            return getONTSubject().getOWLObject();
        }

        @Override
        public Stream<OWLPropertyExpression> propertyExpressions() {
            return operands();
        }

        @Override
        public Stream<OWLPropertyExpression> operands() {
            return members().map(ONTObject::getOWLObject);
        }

        /**
         * {@inheritDoc}
         * Since the concrete property type is unknown, the item is cached as is.
         */
        @SuppressWarnings("rawtypes")
        @Override
        public ONTObject fromContentItem(Object x, ModelObjectFactory factory) {
            return (ONTObject) x;
        }

        /**
         * {@inheritDoc}
         * Since the concrete property type is unknown, the item is cached as is.
         */
        @SuppressWarnings("rawtypes")
        @Override
        public Object toContentItem(ONTObject x) {
            return x;
        }

        @Override
        public ONTObject<? extends OWLClassExpression> findSubjectByURI(String uri, ModelObjectFactory factory) {
            return factory.getClass(uri);
        }

        @Override
        public ONTObject<? extends OWLClassExpression> fetchONTSubject(OntStatement statement,
                                                                       ModelObjectFactory factory) {
            return factory.getClass(statement.getSubject(OntClass.class));
        }

        @Override
        protected AxiomImpl makeCopy(ONTObject<OWLHasKeyAxiom> other) {
            return new AxiomImpl(subject, predicate, object, model) {
                @Override
                public Stream<Triple> triples() {
                    return Stream.concat(AxiomImpl.this.triples(), other.triples());
                }
            };
        }

        @FactoryAccessor
        @Override
        protected OWLHasKeyAxiom createAnnotatedAxiom(Object[] content,
                                                      ModelObjectFactory factory,
                                                      Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLHasKeyAxiom(eraseModel(findONTSubject(content[0], factory).getOWLObject()),
                    members(content, factory).map(x -> eraseModel(x.getOWLObject())).collect(Collectors.toList()),
                    annotations);
        }
    }

}
