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
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLObjectPropertyDomainAxiom} implementations.
 * A Property Domain Axiom is a statement with predicate {@link org.apache.jena.vocabulary.RDFS#domain rdfs:domain}.
 * <p>
 * Created by @szuev on 29.09.2016.
 */
public class ObjectPropertyDomainTranslator
        extends AbstractPropertyDomainTranslator<OWLObjectPropertyDomainAxiom, OntObjectProperty> {

    @Override
    Class<OntObjectProperty> getView() {
        return OntObjectProperty.class;
    }

    @Override
    public boolean filter(OntStatement statement, AxiomsSettings config) {
        return super.filter(statement, config) && statement.getObject().canAs(OntClass.class);
    }

    @Override
    public ONTObject<OWLObjectPropertyDomainAxiom> toAxiomImpl(OntStatement statement,
                                                               ModelObjectFactory factory,
                                                               AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLObjectPropertyDomainAxiom> toAxiomWrap(OntStatement statement,
                                                               ONTObjectFactory factory,
                                                               AxiomsSettings config) {
        ONTObject<? extends OWLObjectPropertyExpression> p = factory.getProperty(statement.getSubject(getView()));
        ONTObject<? extends OWLClassExpression> ce = factory.getClass(statement.getObject(OntClass.class));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLObjectPropertyDomainAxiom res = factory.getOWLDataFactory()
                .getOWLObjectPropertyDomainAxiom(p.getOWLObject(), ce.getOWLObject(), TranslateHelper.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(p).append(ce);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLObjectPropertyDomainAxiomImpl
     */
    public abstract static class AxiomImpl
            extends ClassDomainAxiomImpl<OWLObjectPropertyDomainAxiom, OWLObjectPropertyExpression>
            implements OWLObjectPropertyDomainAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        protected AxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        /**
         * Creates an {@link ONTObject} container that is also {@link OWLObjectPropertyDomainAxiom}.
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
        public ONTObject<? extends OWLObjectPropertyExpression> getURISubject(ModelObjectFactory factory) {
            return factory.getObjectProperty(getSubjectURI());
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> subjectFromStatement(OntStatement statement,
                                                                                     ModelObjectFactory factory) {
            return factory.getProperty(statement.getSubject(OntObjectProperty.class));
        }

        @FactoryAccessor
        @Override
        protected OWLObjectPropertyDomainAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLObjectPropertyDomainAxiom(eraseModel(getProperty()),
                    eraseModel(getDomain()), annotations);
        }

        @FactoryAccessor
        @Override
        public OWLSubClassOfAxiom asOWLSubClassOfAxiom() {
            DataFactory df = getDataFactory();
            return df.getOWLSubClassOfAxiom(df.getOWLObjectSomeValuesFrom(eraseModel(getProperty()), df.getOWLThing()),
                    eraseModel(getDomain()));
        }

        /**
         * An {@link OWLObjectPropertyDomainAxiom}
         * that has named object property and class expressions and has no annotations.
         */
        public static class SimpleImpl extends AxiomImpl
                implements Simple<OWLObjectPropertyExpression, OWLClassExpression> {

            private static final BiFunction<Triple, Supplier<OntModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            @Override
            public Set<OWLObjectProperty> getObjectPropertySet() {
                return createSet(getONTSubject().getOWLObject().asOWLObjectProperty());
            }

            @Override
            public Set<OWLClass> getNamedClassSet() {
                return createSet(getONTObject().getOWLObject().asOWLClass());
            }

            @Override
            public Set<OWLClassExpression> getClassExpressionSet() {
                return createSet(getONTObject().getOWLObject());
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLEntity> getSignatureSet() {
                return (Set<OWLEntity>) getOWLComponentsAsSet();
            }

            @Override
            public boolean containsObjectProperty(OWLObjectProperty property) {
                return getSubjectURI().equals(ONTEntityImpl.getURI(property));
            }

            @Override
            public boolean containsNamedClass(OWLClass clazz) {
                return getObjectURI().equals(ONTEntityImpl.getURI(clazz));
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return false;
            }

            @Override
            public boolean canContainDatatypes() {
                return false;
            }

            @Override
            public boolean canContainAnonymousIndividuals() {
                return false;
            }

            @Override
            public boolean canContainNamedIndividuals() {
                return false;
            }

            @Override
            public boolean canContainDataProperties() {
                return false;
            }
        }

        /**
         * An {@link OWLObjectPropertyDomainAxiom}
         * that either has annotations
         * or anonymous object/class expressions in main triple's subject/object positions respectively.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl
                implements Complex<ComplexImpl, OWLObjectPropertyExpression, OWLClassExpression> {

            private static final BiFunction<Triple, Supplier<OntModel>, ComplexImpl> FACTORY = ComplexImpl::new;

            protected final InternalCache.Loading<ComplexImpl, Object[]> content;

            public ComplexImpl(Triple t, Supplier<OntModel> m) {
                this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
            }

            protected ComplexImpl(Object s, String p, Object o, Supplier<OntModel> m) {
                super(s, p, o, m);
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

            @Override
            public ONTObject<OWLObjectPropertyDomainAxiom> merge(ONTObject<OWLObjectPropertyDomainAxiom> other) {
                if (this == other) {
                    return this;
                }
                if (other instanceof AxiomImpl && sameTriple((AxiomImpl) other)) {
                    return this;
                }
                ComplexImpl res = new ComplexImpl(subject, predicate, object, model) {
                    @Override
                    public Stream<Triple> triples() {
                        return Stream.concat(ComplexImpl.this.triples(), other.triples());
                    }
                };
                if (hasContent()) {
                    res.putContent(getContent());
                }
                res.hashCode = hashCode;
                return res;
            }
        }
    }
}
