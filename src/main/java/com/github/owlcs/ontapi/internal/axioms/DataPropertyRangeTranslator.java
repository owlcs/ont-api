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

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.objects.*;
import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntDataRange;
import com.github.owlcs.ontapi.jena.model.OntModel;
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
 * A translator that provides {@link OWLDataPropertyRangeAxiom} implementations.
 * A Property Range Axiom is a statement with the predicate {@link org.apache.jena.vocabulary.RDFS#range rdfs:range}.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class DataPropertyRangeTranslator extends AbstractPropertyRangeTranslator<OWLDataPropertyRangeAxiom, OntDataProperty> {

    @Override
    Class<OntDataProperty> getView() {
        return OntDataProperty.class;
    }

    protected boolean filter(OntStatement statement, InternalConfig config) {
        return super.filter(statement, config) && statement.getObject().canAs(OntDataRange.class);
    }

    @Override
    public ONTObject<OWLDataPropertyRangeAxiom> toAxiomImpl(OntStatement statement,
                                                            ModelObjectFactory factory,
                                                            InternalConfig config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLDataPropertyRangeAxiom> toAxiomWrap(OntStatement statement,
                                                            InternalObjectFactory factory,
                                                            InternalConfig config) {
        ONTObject<OWLDataProperty> p = factory.getProperty(statement.getSubject(getView()));
        ONTObject<? extends OWLDataRange> d = factory.getDatatype(statement.getObject(OntDataRange.class));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLDataPropertyRangeAxiom res = factory.getOWLDataFactory()
                .getOWLDataPropertyRangeAxiom(p.getOWLObject(), d.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(p).append(d);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLDataPropertyRangeAxiomImpl
     */
    public abstract static class AxiomImpl
            extends RangeAxiomImpl<OWLDataPropertyRangeAxiom, OWLDataPropertyExpression, OWLDataRange>
            implements OWLDataPropertyRangeAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        protected AxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        /**
         * Creates an {@link ONTObject} container that is also {@link OWLDataPropertyRangeAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link InternalObjectFactory}, not {@code null}
         * @param config    {@link InternalConfig}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       InternalConfig config) {
            return WithTwoObjects.create(statement,
                    SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public ONTObject<? extends OWLDataPropertyExpression> getURISubject(ModelObjectFactory factory) {
            return ONTDataPropertyImpl.find(getSubjectURI(), factory, model);
        }

        @Override
        public ONTObject<? extends OWLDataPropertyExpression> subjectFromStatement(OntStatement statement,
                                                                                   ModelObjectFactory factory) {
            return factory.getProperty(statement.getSubject(OntDataProperty.class));
        }

        @Override
        public ONTObject<? extends OWLDataRange> getURIObject(ModelObjectFactory factory) {
            return ONTDatatypeImpl.find(getObjectURI(), factory, model);
        }

        @Override
        public ONTObject<? extends OWLDataRange> objectFromStatement(OntStatement statement,
                                                                     ModelObjectFactory factory) {
            return factory.getDatatype(statement.getObject(OntDataRange.class));
        }

        @FactoryAccessor
        @Override
        protected OWLDataPropertyRangeAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLDataPropertyRangeAxiom(eraseModel(getProperty()),
                    eraseModel(getRange()), annotations);
        }

        @FactoryAccessor
        @Override
        public OWLSubClassOfAxiom asOWLSubClassOfAxiom() {
            DataFactory df = getDataFactory();
            return df.getOWLSubClassOfAxiom(df.getOWLThing(),
                    df.getOWLDataAllValuesFrom(eraseModel(getProperty()), eraseModel(getRange())));
        }

        @Override
        public final boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public final boolean canContainClassExpressions() {
            return false;
        }

        @Override
        public final boolean canContainNamedIndividuals() {
            return false;
        }

        @Override
        public final boolean canContainObjectProperties() {
            return false;
        }

        @Override
        public final boolean canContainAnonymousIndividuals() {
            return isAnnotated();
        }

        @Override
        public final boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        /**
         * An {@link OWLDataPropertyRangeAxiom}
         * that has named data range expression (i.e. {@link OWLDatatype OWL Datatype}) as object
         * and has no annotations.
         */
        public static class SimpleImpl extends AxiomImpl implements Simple<OWLDataPropertyExpression, OWLDataRange> {

            private static final BiFunction<Triple, Supplier<OntModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            @Override
            public Set<OWLDataProperty> getDataPropertySet() {
                return createSet(getONTSubject().getOWLObject().asOWLDataProperty());
            }

            @Override
            public Set<OWLDatatype> getDatatypeSet() {
                return createSet(getONTObject().getOWLObject().asOWLDatatype());
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLEntity> getSignatureSet() {
                return (Set<OWLEntity>) getOWLComponentsAsSet();
            }

            @Override
            public boolean containsDataProperty(OWLDataProperty property) {
                return getSubjectURI().equals(ONTEntityImpl.getURI(property));
            }

            @Override
            public boolean containsDatatype(OWLDatatype datatype) {
                return getObjectURI().equals(ONTEntityImpl.getURI(datatype));
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return false;
            }
        }

        /**
         * An {@link OWLDataPropertyRangeAxiom}
         * that either has annotations or anonymous data range expression as domain (in main triple's object position).
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl
                implements Complex<ComplexImpl, OWLDataPropertyExpression, OWLDataRange> {

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
            protected boolean sameAs(ONTStatementImpl other) {
                if (notSame(other)) {
                    return false;
                }
                if (!sameSubject(other)) {
                    return false;
                }
                return sameContent(other);
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof ComplexImpl && Arrays.equals(getContent(), ((ComplexImpl) other).getContent());
            }

            @Override
            public ONTObject<OWLDataPropertyRangeAxiom> merge(ONTObject<OWLDataPropertyRangeAxiom> other) {
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
