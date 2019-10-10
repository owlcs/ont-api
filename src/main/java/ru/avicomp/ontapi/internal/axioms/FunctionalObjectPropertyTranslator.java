/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.internal.axioms;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.internal.objects.FactoryAccessor;
import ru.avicomp.ontapi.internal.objects.ONTEntityImpl;
import ru.avicomp.ontapi.internal.objects.ONTStatementImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A translator that provides {@link OWLFunctionalObjectPropertyAxiom} implementations.
 * Example:
 * <pre>{@code
 * pizza:isBaseOf rdf:type owl:ObjectProperty , owl:FunctionalProperty .
 * }</pre>
 * <p>
 * Created by @szuev on 29.09.2016.
 */
public class FunctionalObjectPropertyTranslator
        extends AbstractPropertyTypeTranslator<OWLFunctionalObjectPropertyAxiom, OntOPE> {

    @Override
    Resource getType() {
        return OWL.FunctionalProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    public ONTObject<OWLFunctionalObjectPropertyAxiom> toAxiom(OntStatement statement,
                                                               Supplier<OntGraphModel> model,
                                                               InternalObjectFactory factory,
                                                               InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLFunctionalObjectPropertyAxiom> toAxiom(OntStatement statement,
                                                               InternalObjectFactory factory,
                                                               InternalConfig config) {
        ONTObject<? extends OWLObjectPropertyExpression> p = factory.getProperty(getSubject(statement));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLFunctionalObjectPropertyAxiom res = factory.getOWLDataFactory()
                .getOWLFunctionalObjectPropertyAxiom(p.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(p);
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.axioms.OWLFunctionalObjectPropertyAxiomImpl
     */
    @SuppressWarnings("WeakerAccess")
    public static abstract class AxiomImpl extends ObjectAxiomImpl<OWLFunctionalObjectPropertyAxiom>
            implements OWLFunctionalObjectPropertyAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLFunctionalObjectPropertyAxiom}.
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
            return WithOneObject.create(statement, model,
                    SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @FactoryAccessor
        @Override
        public OWLSubClassOfAxiom asOWLSubClassOfAxiom() {
            DataFactory df = getDataFactory();
            OWLClass thing = df.getOWLThing();
            return df.getOWLSubClassOfAxiom(thing, df.getOWLObjectMaxCardinality(1, eraseModel(getProperty()), thing));
        }

        @FactoryAccessor
        @Override
        protected OWLFunctionalObjectPropertyAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLFunctionalObjectPropertyAxiom(eraseModel(getProperty()), annotations);
        }

        /**
         * An {@link OWLFunctionalObjectPropertyAxiom}
         * that has a named object property as subject and has no annotations.
         */
        public static class SimpleImpl extends AxiomImpl implements Simple<OWLObjectPropertyExpression> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return false;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLObjectProperty> getObjectPropertySet() {
                return (Set<OWLObjectProperty>) getOWLComponentsAsSet();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLEntity> getSignatureSet() {
                return (Set<OWLEntity>) getOWLComponentsAsSet();
            }

            @Override
            public boolean containsObjectProperty(OWLObjectProperty property) {
                return subject.equals(ONTEntityImpl.getURI(property));
            }
        }

        /**
         * An {@link OWLFunctionalObjectPropertyAxiom}
         * that either has annotations or an anonymous object property expression (inverse object property)
         * in the main triple's subject position.
         * It has a public constructor since this class is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl implements Complex<ComplexImpl, OWLObjectPropertyExpression> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, ComplexImpl> FACTORY = ComplexImpl::new;
            protected final InternalCache.Loading<AxiomImpl.ComplexImpl, Object[]> content;

            protected ComplexImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
                this.content = createContentCache();
            }

            @Override
            public InternalCache.Loading<ComplexImpl, Object[]> getContentCache() {
                return content;
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof ComplexImpl && Arrays.equals(getContent(), ((ComplexImpl) other).getContent());
            }
        }
    }
}
