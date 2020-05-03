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
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A translator that provides {@link OWLSubDataPropertyOfAxiom} implementations.
 * See also {@link AbstractSubPropertyTranslator}.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class SubDataPropertyOfTranslator extends AbstractSubPropertyTranslator<OWLSubDataPropertyOfAxiom, OntDataProperty> {

    @Override
    OWLPropertyExpression getSubProperty(OWLSubDataPropertyOfAxiom axiom) {
        return axiom.getSubProperty();
    }

    @Override
    OWLPropertyExpression getSuperProperty(OWLSubDataPropertyOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    Class<OntDataProperty> getView() {
        return OntDataProperty.class;
    }

    @Override
    public ONTObject<OWLSubDataPropertyOfAxiom> toAxiomImpl(OntStatement statement,
                                                            ModelObjectFactory factory,
                                                            AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLSubDataPropertyOfAxiom> toAxiomWrap(OntStatement statement,
                                                            ONTObjectFactory factory,
                                                            AxiomsSettings config) {
        ONTObject<OWLDataProperty> sub = factory.getProperty(statement.getSubject(OntDataProperty.class));
        ONTObject<OWLDataProperty> sup = factory.getProperty(statement.getObject().as(OntDataProperty.class));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLSubDataPropertyOfAxiom res = factory.getOWLDataFactory()
                .getOWLSubDataPropertyOfAxiom(sub.getOWLObject(), sup.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(sub).append(sup);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLSubDataPropertyOfAxiomImpl
     */
    public abstract static class AxiomImpl
            extends SubPropertyAxiomImpl<OWLSubDataPropertyOfAxiom, OWLDataPropertyExpression>
            implements OWLSubDataPropertyOfAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link ONTObject} container that is also {@link OWLSubDataPropertyOfAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link ONTObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            return WithTwoObjects.create(statement,
                    SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public ONTObject<? extends OWLDataPropertyExpression> subjectFromStatement(OntStatement statement,
                                                                                   ModelObjectFactory factory) {
            return factory.getProperty(statement.getSubject(OntDataProperty.class));
        }

        @Override
        public ONTObject<? extends OWLDataPropertyExpression> objectFromStatement(OntStatement statement,
                                                                                  ModelObjectFactory factory) {
            return factory.getProperty(statement.getObject(OntDataProperty.class));
        }

        @Override
        public ONTObject<? extends OWLDataPropertyExpression> findByURI(String uri, ModelObjectFactory factory) {
            return factory.getDataProperty(uri);
        }

        @FactoryAccessor
        @Override
        protected OWLSubDataPropertyOfAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLSubDataPropertyOfAxiom(eraseModel(getSubProperty()),
                    eraseModel(getSuperProperty()), annotations);
        }

        @Override
        public boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        @Override
        public boolean canContainObjectProperties() {
            return false;
        }

        /**
         * An {@link OWLSubDataPropertyOfAxiom} that has no annotations.
         */
        protected static class SimpleImpl extends AxiomImpl implements UnarySimple<OWLDataPropertyExpression> {

            private static final BiFunction<Triple, Supplier<OntModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLDataProperty> getDataPropertySet() {
                return (Set<OWLDataProperty>) getOWLComponentsAsSet();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLEntity> getSignatureSet() {
                return (Set<OWLEntity>) getOWLComponentsAsSet();
            }

            @Override
            public boolean containsDataProperty(OWLDataProperty property) {
                return hasURIResource(ONTEntityImpl.getURI(property));
            }
        }

        /**
         * An {@link OWLSubDataPropertyOfAxiom} that has annotations.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl
                implements UnaryWithContent<ComplexImpl, OWLDataPropertyExpression> {

            private static final BiFunction<Triple, Supplier<OntModel>, ComplexImpl> FACTORY = ComplexImpl::new;
            protected final InternalCache.Loading<ComplexImpl, Object[]> content;

            public ComplexImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
                this.content = createContentCache();
            }

            @Override
            public InternalCache.Loading<ComplexImpl, Object[]> getContentCache() {
                return content;
            }
        }
    }
}
