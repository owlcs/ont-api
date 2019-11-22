/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
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
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTAnnotationPropertyImpl;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;
import com.github.owlcs.ontapi.jena.model.OntNAP;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A translator that provides {@link OWLSubAnnotationPropertyOfAxiom} implementations.
 * See {@link AbstractSubPropertyTranslator}.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class SubAnnotationPropertyOfTranslator
        extends AbstractSubPropertyTranslator<OWLSubAnnotationPropertyOfAxiom, OntNAP> {

    @Override
    OWLPropertyExpression getSubProperty(OWLSubAnnotationPropertyOfAxiom axiom) {
        return axiom.getSubProperty();
    }

    @Override
    OWLPropertyExpression getSuperProperty(OWLSubAnnotationPropertyOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    Class<OntNAP> getView() {
        return OntNAP.class;
    }

    /**
     * Returns {@link OntStatement}s defining the {@link OWLSubAnnotationPropertyOfAxiom} axiom.
     *
     * @param model  {@link OntGraphModel}
     * @param config {@link InternalConfig}
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     */
    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        if (!config.isLoadAnnotationAxioms()) return NullIterator.instance();
        return super.listStatements(model, config);
    }

    @Override
    protected boolean filter(OntStatement statement, InternalConfig config) {
        return super.filter(statement, config)
                && ReadHelper.testAnnotationAxiomOverlaps(statement, config,
                AxiomType.SUB_OBJECT_PROPERTY, AxiomType.SUB_DATA_PROPERTY);
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return config.isLoadAnnotationAxioms() && super.testStatement(statement, config);
    }

    @Override
    public ONTObject<OWLSubAnnotationPropertyOfAxiom> toAxiomImpl(OntStatement statement,
                                                                  Supplier<OntGraphModel> model,
                                                                  InternalObjectFactory factory,
                                                                  InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLSubAnnotationPropertyOfAxiom> toAxiomWrap(OntStatement statement,
                                                                  InternalObjectFactory factory,
                                                                  InternalConfig config) {
        ONTObject<OWLAnnotationProperty> sub = factory.getProperty(statement.getSubject(OntNAP.class));
        ONTObject<OWLAnnotationProperty> sup = factory.getProperty(statement.getObject().as(OntNAP.class));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLSubAnnotationPropertyOfAxiom res = factory.getOWLDataFactory()
                .getOWLSubAnnotationPropertyOfAxiom(sub.getOWLObject(), sup.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(sub).append(sup);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLSubAnnotationPropertyOfAxiomImpl
     */
    public abstract static class AxiomImpl
            extends SubPropertyAxiomImpl<OWLSubAnnotationPropertyOfAxiom, OWLAnnotationProperty>
            implements OWLSubAnnotationPropertyOfAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link ONTObject} container that is also {@link OWLSubAnnotationPropertyOfAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param model     {@link OntGraphModel} provider, not {@code null}
         * @param factory   {@link InternalObjectFactory}, not {@code null}
         * @param config    {@link InternalConfig}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       Supplier<OntGraphModel> model,
                                       InternalObjectFactory factory,
                                       InternalConfig config) {
            return WithTwoObjects.create(statement, model,
                    SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public ONTObject<? extends OWLAnnotationProperty> subjectFromStatement(OntStatement statement,
                                                                               InternalObjectFactory factory) {
            return factory.getProperty(statement.getSubject(OntNAP.class));
        }

        @Override
        public ONTObject<? extends OWLAnnotationProperty> objectFromStatement(OntStatement statement,
                                                                              InternalObjectFactory factory) {
            return factory.getProperty(statement.getObject(OntNAP.class));
        }

        @Override
        public ONTObject<? extends OWLAnnotationProperty> findByURI(String uri, InternalObjectFactory factory) {
            return ONTAnnotationPropertyImpl.find(uri, factory, model);
        }

        @FactoryAccessor
        @Override
        protected OWLSubAnnotationPropertyOfAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLSubAnnotationPropertyOfAxiom(eraseModel(getSubProperty()),
                    eraseModel(getSuperProperty()), annotations);
        }

        @Override
        public boolean canContainDataProperties() {
            return false;
        }

        @Override
        public boolean canContainObjectProperties() {
            return false;
        }

        /**
         * An {@link OWLSubAnnotationPropertyOfAxiom}
         * that has named object properties as subject and object and has no annotations.
         */
        protected static class SimpleImpl extends AxiomImpl implements UnarySimple<OWLAnnotationProperty> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLAnnotationProperty> getAnnotationPropertySet() {
                return (Set<OWLAnnotationProperty>) getOWLComponentsAsSet();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLEntity> getSignatureSet() {
                return (Set<OWLEntity>) getOWLComponentsAsSet();
            }

            @Override
            public boolean containsAnnotationProperty(OWLAnnotationProperty property) {
                return hasURIResource(ONTEntityImpl.getURI(property));
            }
        }

        /**
         * An {@link OWLSubAnnotationPropertyOfAxiom} that has annotations.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl
                implements UnaryWithContent<ComplexImpl, OWLAnnotationProperty> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, ComplexImpl> FACTORY = ComplexImpl::new;
            protected final InternalCache.Loading<ComplexImpl, Object[]> content;

            public ComplexImpl(Triple t, Supplier<OntGraphModel> m) {
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
