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

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;
import com.github.owlcs.ontapi.jena.model.OntNDP;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLEquivalentDataPropertiesAxiom} implementations.
 * Example:
 * <pre>{@code
 *      gr:description rdf:type owl:DatatypeProperty ;  owl:equivalentProperty <http://schema.org/description> ;
 * }</pre>
 * Created by @szuev on 01.10.2016.
 */
public class EquivalentDataPropertiesTranslator
        extends AbstractNaryTranslator<OWLEquivalentDataPropertiesAxiom, OWLDataPropertyExpression, OntNDP> {

    @Override
    Property getPredicate() {
        return OWL.equivalentProperty;
    }

    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    public ONTObject<OWLEquivalentDataPropertiesAxiom> toAxiomImpl(OntStatement statement,
                                                                   Supplier<OntGraphModel> model,
                                                                   InternalObjectFactory factory,
                                                                   InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLEquivalentDataPropertiesAxiom> toAxiomWrap(OntStatement statement,
                                                                   InternalObjectFactory factory,
                                                                   InternalConfig config) {
        ONTObject<OWLDataProperty> a = factory.getProperty(statement.getSubject(getView()));
        ONTObject<OWLDataProperty> b = factory.getProperty(statement.getObject().as(getView()));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLEquivalentDataPropertiesAxiom res = factory.getOWLDataFactory()
                .getOWLEquivalentDataPropertiesAxiom(a.getOWLObject(), b.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(a).append(b);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLEquivalentDataPropertiesAxiomImpl
     */
    @SuppressWarnings("WeakerAccess")
    public abstract static class AxiomImpl extends DataPropertyNaryAxiomImpl<OWLEquivalentDataPropertiesAxiom>
            implements OWLEquivalentDataPropertiesAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
            super(s, p, o, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLEquivalentDataPropertiesAxiom}.
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
            return WithManyObjects.create(statement, model,
                    SimpleImpl.FACTORY, WithAnnotationsImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        protected final long count() {
            return 2;
        }

        @FactoryAccessor
        @Override
        protected OWLEquivalentDataPropertiesAxiom createAxiom(Collection<OWLDataPropertyExpression> members,
                                                               Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLEquivalentDataPropertiesAxiom(members,
                    annotations == null ? NO_ANNOTATIONS : annotations);
        }

        protected OWLSubDataPropertyOfAxiom createSubPropertyOf(OWLDataPropertyExpression a,
                                                                OWLDataPropertyExpression b) {
            DataFactory df = getDataFactory();
            return df.getOWLSubDataPropertyOfAxiom(a, b);
        }

        @FactoryAccessor
        @Override
        public Collection<OWLSubDataPropertyOfAxiom> asSubDataPropertyOfAxioms() {
            return walkAllPairwise((a, b) -> createSubPropertyOf(eraseModel(a), eraseModel(b)));
        }

        /**
         * An {@link OWLEquivalentDataPropertiesAxiom} that has no annotations.
         */
        protected static class SimpleImpl extends AxiomImpl implements Simple<OWLDataPropertyExpression> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            protected SimpleImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
                super(s, p, o, m);
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof SimpleImpl && isReverseTriple((SimpleImpl) other);
            }

            @Override
            protected AxiomImpl makeCopyWith(ONTObject<OWLEquivalentDataPropertiesAxiom> other) {
                if (other instanceof SimpleImpl) {
                    Triple t = ((SimpleImpl) other).asTriple();
                    return new SimpleImpl(subject, predicate, object, model) {

                        @Override
                        public Stream<Triple> triples() {
                            return Stream.concat(SimpleImpl.this.triples(), Stream.of(t));
                        }
                    };
                }
                return new SimpleImpl(subject, predicate, object, model) {
                    @Override
                    public Stream<Triple> triples() {
                        return Stream.concat(SimpleImpl.this.triples(), other.triples());
                    }
                };
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
         * An {@link OWLEquivalentDataPropertiesAxiom} that has annotations.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class WithAnnotationsImpl extends AxiomImpl
                implements Complex<WithAnnotationsImpl, OWLDataPropertyExpression> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, WithAnnotationsImpl> FACTORY =
                    WithAnnotationsImpl::new;
            protected final InternalCache.Loading<WithAnnotationsImpl, Object[]> content;

            public WithAnnotationsImpl(Triple t, Supplier<OntGraphModel> m) {
                this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
            }

            protected WithAnnotationsImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
                super(s, p, o, m);
                this.content = createContentCache();
            }

            @Override
            public InternalCache.Loading<WithAnnotationsImpl, Object[]> getContentCache() {
                return content;
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof WithAnnotationsImpl && Arrays.equals(getContent(), ((WithAnnotationsImpl) other).getContent());
            }

            @Override
            public boolean isAnnotated() {
                return true;
            }

            @Override
            protected WithAnnotationsImpl makeCopyWith(ONTObject<OWLEquivalentDataPropertiesAxiom> other) {
                WithAnnotationsImpl res = new WithAnnotationsImpl(subject, predicate, object, model) {
                    @Override
                    public Stream<Triple> triples() {
                        return Stream.concat(WithAnnotationsImpl.this.triples(), other.triples());
                    }
                };
                if (hasContent()) {
                    res.putContent(getContent());
                }
                return res;
            }
        }
    }
}
