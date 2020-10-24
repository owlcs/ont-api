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
import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.jena.model.OntDisjoint;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import org.apache.jena.graph.BlankNodeId;
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
 * A translator that provides {@link OWLDifferentIndividualsAxiom} implementations.
 * See {@link AbstractTwoWayNaryTranslator}.
 * Syntax:
 * <ul>
 * <li>{@code a1 owl:differentFrom a2.}</li>
 * <li><pre>{@code _:x rdf:type owl:AllDifferent.
 * _:x owl:members (a1 ... an). }</pre></li>
 * </ul>
 * <p>
 * Created by @szuev on 29.09.2016.
 */
public class DifferentIndividualsTranslator
        extends AbstractTwoWayNaryTranslator<OWLDifferentIndividualsAxiom, OWLIndividual, OntIndividual> {

    private static final Property PREDICATE = OWL.differentFrom;

    @Override
    Property getPredicate() {
        return PREDICATE;
    }

    @Override
    Class<OntIndividual> getView() {
        return OntIndividual.class;
    }

    @Override
    Triple getONTSearchTriple(OWLDifferentIndividualsAxiom axiom) {
        if (axiom instanceof WithTriple) { // both named and anonymous individuals
            Triple t = ((WithTriple) axiom).asTriple();
            return PREDICATE.asNode().equals(t.getPredicate()) ? t : null;
        }
        return null;
    }

    @Override
    Resource getMembersType() {
        return OWL.AllDifferent;
    }

    @Override
    Property getMembersPredicate() {
        return OWL.distinctMembers;
    }

    @Override
    Class<OntDisjoint.Individuals> getDisjointView() {
        return OntDisjoint.Individuals.class;
    }

    @Override
    public ONTObject<OWLDifferentIndividualsAxiom> toAxiomImpl(OntStatement statement,
                                                               ModelObjectFactory factory,
                                                               AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLDifferentIndividualsAxiom> toAxiomWrap(OntStatement statement,
                                                               ONTObjectFactory factory,
                                                               AxiomsSettings config) {
        return makeAxiom(statement, factory.getAnnotations(statement, config),
                factory::getIndividual,
                (members, annotations) -> factory.getOWLDataFactory()
                        .getOWLDifferentIndividualsAxiom(TranslateHelper.toSet(members), TranslateHelper.toSet(annotations)));
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.OWLDifferentIndividualsAxiomImpl
     */
    public abstract static class AxiomImpl extends IndividualNaryAxiomImpl<OWLDifferentIndividualsAxiom>
            implements OWLDifferentIndividualsAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntModel> m) {
            super(s, p, o, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLDifferentIndividualsAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link ONTObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            if (PREDICATE.equals(statement.getPredicate())) {
                return WithManyObjects.create(statement,
                        SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
            }
            return WithManyObjects.create(statement, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLIndividual>> listONTComponents(OntStatement statement,
                                                                                      ModelObjectFactory factory) {
            if (PREDICATE.equals(statement.getPredicate())) {
                return super.listONTComponents(statement, factory);
            }
            return OntModels.listMembers(statement.getSubject(OntDisjoint.Individuals.class).getList())
                    .mapWith(factory::getIndividual);
        }

        @FactoryAccessor
        @Override
        protected OWLDifferentIndividualsAxiom createAxiom(Collection<OWLIndividual> members,
                                                           Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLDifferentIndividualsAxiom(members,
                    annotations == null ? NO_ANNOTATIONS : annotations);
        }

        @FactoryAccessor
        @Override
        protected OWLSubClassOfAxiom createSubClassOf(OWLIndividual a, OWLIndividual b) {
            DataFactory df = getDataFactory();
            return df.getOWLSubClassOfAxiom(df.getOWLObjectOneOf(a), df.getOWLObjectOneOf(b).getObjectComplementOf());
        }

        /**
         * An {@link OWLDifferentIndividualsAxiom}
         * that is based on a single unannotated triple {@code s owl:differentFrom o},
         * where {@code s} and {@code o} are named classes (URI {@link Resource}s).
         */
        protected static class SimpleImpl extends AxiomImpl implements Simple<OWLIndividual> {

            private static final BiFunction<Triple, Supplier<OntModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            protected SimpleImpl(Object s, String p, Object o, Supplier<OntModel> m) {
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
            protected AxiomImpl makeCopyWith(ONTObject<OWLDifferentIndividualsAxiom> other) {
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
            public Set<OWLNamedIndividual> getNamedIndividualSet() {
                return (Set<OWLNamedIndividual>) getOWLComponentsAsSet();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLEntity> getSignatureSet() {
                return (Set<OWLEntity>) getOWLComponentsAsSet();
            }

            @Override
            public boolean containsNamedIndividual(OWLNamedIndividual individual) {
                return hasURIResource(ONTEntityImpl.getURI(individual));
            }

            @Override
            public boolean canContainAnonymousIndividuals() {
                return false;
            }

            @Override
            public boolean containsAnonymousIndividuals() {
                return false;
            }
        }

        /**
         * An {@link OWLDifferentIndividualsAxiom}
         * that either has annotations or anonymous class expressions in subject or object positions,
         * or it is based on {@link OntDisjoint.Individuals} resource.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl implements Complex<ComplexImpl, OWLIndividual> {

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
            protected long count() {
                return isSimple() ? 2 : members().count();
            }

            boolean isSimple() {
                return predicate.equals(PREDICATE.getURI());
            }

            @Override
            public boolean containsAnonymousIndividuals() {
                if (isSimple()) {
                    return subject instanceof BlankNodeId || object instanceof BlankNodeId;
                }
                return individuals().anyMatch(OWLIndividual::isAnonymous);
            }

            protected OntDisjoint.Individuals asResource() {
                return getPersonalityModel().getNodeAs(getSubjectNode(), OntDisjoint.Individuals.class);
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
                return asResource().getMainStatement();
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
            protected ComplexImpl makeCopyWith(ONTObject<OWLDifferentIndividualsAxiom> other) {
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
