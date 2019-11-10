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

import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.InternalConfig;
import com.github.owlcs.ontapi.internal.InternalObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.jena.model.OntDisjoint;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;
import com.github.owlcs.ontapi.jena.model.OntNDP;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLDisjointDataPropertiesAxiom} implementations.
 * Examples:
 * <ul>
 * <li>{@code  :objProperty1 owl:propertyDisjointWith :objProperty2}</li>
 * <li>{@code [ rdf:type owl:AllDisjointProperties; owl:members ( :objProperty1 :objProperty2 :objProperty3 ) ]}</li>
 * </ul>
 * <p>
 * Created by szuev on 12.10.2016.
 */
public class DisjointDataPropertiesTranslator
        extends AbstractTwoWayNaryTranslator<OWLDisjointDataPropertiesAxiom, OWLDataPropertyExpression, OntNDP> {

    private static final Property PREDICATE = OWL.propertyDisjointWith;

    @Override
    Property getPredicate() {
        return PREDICATE;
    }

    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    Resource getMembersType() {
        return OWL.AllDisjointProperties;
    }

    @Override
    Property getMembersPredicate() {
        return OWL.members;
    }

    @Override
    Class<OntDisjoint.DataProperties> getDisjointView() {
        return OntDisjoint.DataProperties.class;
    }

    @Override
    public ONTObject<OWLDisjointDataPropertiesAxiom> toAxiom(OntStatement statement,
                                                             Supplier<OntGraphModel> model,
                                                             InternalObjectFactory factory,
                                                             InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLDisjointDataPropertiesAxiom> toAxiom(OntStatement statement,
                                                             InternalObjectFactory factory,
                                                             InternalConfig config) {
        return makeAxiom(statement, factory.getAnnotations(statement, config),
                factory::getProperty,
                (members, annotations) -> factory.getOWLDataFactory()
                        .getOWLDisjointDataPropertiesAxiom(ONTObject.toSet(members), ONTObject.toSet(annotations)));
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLDisjointDataPropertiesAxiomImpl
     */
    public abstract static class AxiomImpl extends DataPropertyNaryAxiomImpl<OWLDisjointDataPropertiesAxiom>
            implements OWLDisjointDataPropertiesAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
            super(s, p, o, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLDisjointDataPropertiesAxiom}.
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
            if (PREDICATE.equals(statement.getPredicate())) {
                return WithManyObjects.create(statement, model,
                        SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
            }
            return WithManyObjects.create(statement, model, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLDataPropertyExpression>> listONTComponents(OntStatement statement,
                                                                                                  InternalObjectFactory factory) {
            if (PREDICATE.equals(statement.getPredicate())) {
                return super.listONTComponents(statement, factory);
            }
            return OntModels.listMembers(statement.getSubject(OntDisjoint.DataProperties.class).getList())
                    .mapWith(factory::getProperty);
        }

        @FactoryAccessor
        @Override
        protected OWLDisjointDataPropertiesAxiom createAxiom(Collection<OWLDataPropertyExpression> members,
                                                             Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLDisjointDataPropertiesAxiom(members,
                    annotations == null ? NO_ANNOTATIONS : annotations);
        }

        /**
         * An {@link OWLDisjointDataPropertiesAxiom}
         * that is based on a single unannotated triple {@code s owl:propertyDisjointWith o},
         * where {@code s} and {@code o} are data properties (URI {@link Resource}s).
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
            protected long count() {
                return 2;
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof SimpleImpl && isReverseTriple((SimpleImpl) other);
            }

            @Override
            protected AxiomImpl makeCopyWith(ONTObject<OWLDisjointDataPropertiesAxiom> other) {
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
         * An {@link OWLDisjointDataPropertiesAxiom}
         * that either has annotations or it is based on {@link OntDisjoint.DataProperties} resource.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl implements Complex<ComplexImpl, OWLDataPropertyExpression> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, ComplexImpl> FACTORY = ComplexImpl::new;
            protected final InternalCache.Loading<ComplexImpl, Object[]> content;

            public ComplexImpl(Triple t, Supplier<OntGraphModel> m) {
                this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
            }

            protected ComplexImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
                super(s, p, o, m);
                this.content = createContentCache();
            }

            @Override
            protected long count() {
                return isSimple() ? 2 : members().count();
            }

            boolean isSimple() {
                return predicate.equals(PREDICATE.getURI());
            }

            protected OntDisjoint.DataProperties asResource() {
                return getPersonalityModel().getNodeAs(getSubjectNode(), OntDisjoint.DataProperties.class);
            }

            @Override
            public Stream<Triple> triples() {
                if (isSimple()) {
                    return super.triples();
                }
                return Stream.concat(asResource().spec().map(FrontsTriple::asTriple),
                        objects().flatMap(ONTObject::triples));
            }

            @Override
            public OntStatement asStatement() {
                if (isSimple()) {
                    return super.asStatement();
                }
                return asResource().getRoot();
            }

            @Override
            public InternalCache.Loading<ComplexImpl, Object[]> getContentCache() {
                return content;
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return testSameContent(other);
            }

            @Override
            protected ComplexImpl makeCopyWith(ONTObject<OWLDisjointDataPropertiesAxiom> other) {
                ComplexImpl res = new ComplexImpl(subject, predicate, object, model) {
                    @Override
                    public Stream<Triple> triples() {
                        return Stream.concat(ComplexImpl.this.triples(), other.triples());
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
