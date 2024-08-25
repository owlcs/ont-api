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

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.ONTWrapperImpl;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontapi.OntModelControls;
import org.apache.jena.ontapi.model.OntDataProperty;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntStatement;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A translator that provides {@link OWLFunctionalDataPropertyAxiom} implementations.
 * Example:
 * <pre>{@code
 * foaf:gender rdf:type owl:DatatypeProperty , owl:FunctionalProperty ;
 * }</pre>
 * <p>
 * Created by @ssz on 30.09.2016.
 */
public class FunctionalDataPropertyTranslator
        extends AbstractPropertyTypeTranslator<OWLFunctionalDataPropertyAxiom, OntDataProperty> {

    @Override
    Resource getType() {
        return OWL.FunctionalProperty;
    }

    @Override
    Class<OntDataProperty> getView() {
        return OntDataProperty.class;
    }

    @Override
    OntModelControls control() {
        return OntModelControls.USE_OWL_DATA_PROPERTY_FUNCTIONAL_FEATURE;
    }

    @Override
    public ONTObject<OWLFunctionalDataPropertyAxiom> toAxiomImpl(OntStatement statement,
                                                                 ModelObjectFactory factory,
                                                                 AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLFunctionalDataPropertyAxiom> toAxiomWrap(OntStatement statement,
                                                                 ONTObjectFactory factory,
                                                                 AxiomsSettings config) {
        ONTObject<OWLDataProperty> p = factory.getProperty(getSubject(statement));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLFunctionalDataPropertyAxiom res = factory.getOWLDataFactory()
                .getOWLFunctionalDataPropertyAxiom(p.getOWLObject(), TranslateHelper.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(p);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.FunctionalDataPropertyAxiomImpl
     */
    public static abstract class AxiomImpl
            extends UnaryAxiomImpl<OWLFunctionalDataPropertyAxiom, OWLDataPropertyExpression>
            implements OWLFunctionalDataPropertyAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLFunctionalDataPropertyAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link ONTObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            return WithOneObject.create(statement,
                    SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        protected final boolean sameContent(ONTStatementImpl other) {
            return false;
        }

        @FactoryAccessor
        @Override
        public OWLSubClassOfAxiom asOWLSubClassOfAxiom() {
            DataFactory df = getDataFactory();
            return df.getOWLSubClassOfAxiom(df.getOWLThing(),
                    df.getOWLDataMaxCardinality(1, eraseModel(getProperty()), df.getTopDatatype()));
        }

        @FactoryAccessor
        @Override
        protected OWLFunctionalDataPropertyAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLFunctionalDataPropertyAxiom(eraseModel(getProperty()), annotations);
        }

        @Override
        public ONTObject<? extends OWLDataPropertyExpression> findURISubject(ModelObjectFactory factory) {
            return factory.getDataProperty((String) subject);
        }

        @Override
        public ONTObject<? extends OWLDataPropertyExpression> fetchONTSubject(OntStatement statement,
                                                                              ModelObjectFactory factory) {
            return factory.getProperty(statement.getSubject(OntDataProperty.class));
        }

        @Override
        public boolean canContainObjectProperties() {
            return false;
        }

        /**
         * An {@link OWLFunctionalDataPropertyAxiom}
         * that has a named object property as subject and has no annotations.
         */
        public static class SimpleImpl extends AxiomImpl implements Simple<OWLDataPropertyExpression> {

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
                return subject.equals(ONTEntityImpl.getURI(property));
            }
        }

        /**
         * An {@link OWLFunctionalDataPropertyAxiom}
         * that either has annotations or an anonymous object property expression (inverse object property)
         * in the main triple's subject position.
         * It has a public constructor since this class is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl implements Complex<ComplexImpl, OWLDataPropertyExpression> {

            private static final BiFunction<Triple, Supplier<OntModel>, ComplexImpl> FACTORY = ComplexImpl::new;
            protected final InternalCache.Loading<ComplexImpl, Object[]> content;

            protected ComplexImpl(Triple t, Supplier<OntModel> m) {
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
