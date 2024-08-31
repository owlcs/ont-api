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
import org.apache.jena.ontapi.model.OntIndividual;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntStatement;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLSameIndividualAxiom} implementations.
 * <p>
 * Example:
 * <pre>{@code
 * :individual1 owl:sameAs :individual2, :individual3 .
 * }</pre>
 * <p>
 * Created @ssz on 13.10.2016.
 *
 * @see OWLSameIndividualAxiom
 */
public class SameIndividualTranslator
        extends AbstractNaryTranslator<OWLSameIndividualAxiom, OWLIndividual, OntIndividual> {

    private static final Property PREDICATE = OWL.sameAs;

    @Override
    OntModelControls control() {
        return OntModelControls.USE_OWL_INDIVIDUAL_SAME_AS_FEATURE;
    }

    @Override
    public Property getPredicate() {
        return PREDICATE;
    }

    @Override
    Class<OntIndividual> getView() {
        return OntIndividual.class;
    }

    @Override
    Triple getONTSearchTriple(OWLSameIndividualAxiom axiom) {
        // both named and anonymous individuals
        return axiom instanceof WithTriple ? ((WithTriple) axiom).asTriple() : null;
    }

    @Override
    public ONTObject<OWLSameIndividualAxiom> toAxiomImpl(OntStatement statement,
                                                         ModelObjectFactory factory,
                                                         AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLSameIndividualAxiom> toAxiomWrap(OntStatement statement,
                                                         ONTObjectFactory factory,
                                                         AxiomsSettings config) {
        ONTObject<? extends OWLIndividual> a = factory.getIndividual(statement.getSubject(getView()));
        ONTObject<? extends OWLIndividual> b = factory.getIndividual(statement.getObject().as(getView()));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLSameIndividualAxiom res = factory.getOWLDataFactory()
                .getOWLSameIndividualAxiom(a.getOWLObject(), b.getOWLObject(), TranslateHelper.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(a).append(b);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.SameIndividualAxiomImpl
     */
    public abstract static class AxiomImpl extends IndividualNaryAxiomImpl<OWLSameIndividualAxiom>
            implements OWLSameIndividualAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntModel> m) {
            super(s, p, o, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLSameIndividualAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link ONTObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            return WithManyObjects.create(statement,
                    SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        protected final long count() {
            return 2;
        }

        @Override
        public boolean containsAnonymousIndividuals() {
            return individuals().anyMatch(OWLIndividual::isAnonymous);
        }

        @FactoryAccessor
        @Override
        protected OWLSameIndividualAxiom createAxiom(Collection<OWLIndividual> members,
                                                     Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLSameIndividualAxiom(members,
                    annotations == null ? NO_ANNOTATIONS : annotations);
        }

        @FactoryAccessor
        @Override
        protected OWLSubClassOfAxiom createSubClassOf(OWLIndividual a, OWLIndividual b) {
            DataFactory df = getDataFactory();
            return df.getOWLSubClassOfAxiom(df.getOWLObjectOneOf(a), df.getOWLObjectOneOf(b));
        }

        @FactoryAccessor
        @Override
        public Collection<OWLSubClassOfAxiom> asOWLSubClassOfAxioms() { // OWL-API-impl returns a Set here
            return fromPairs((a, b) -> createSubClassOf(eraseModel(a), eraseModel(b))).collect(Collectors.toSet());
        }

        /**
         * An {@link OWLSameIndividualAxiom} that has named classes as subject and object and has no annotations.
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
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof SimpleImpl && isReverseTriple((SimpleImpl) other);
            }

            @Override
            protected AxiomImpl makeCopyWith(ONTObject<OWLSameIndividualAxiom> other) {
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

            @Override
            public boolean containsAnonymousIndividuals() {
                return false;
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
        }

        /**
         * An {@link OWLSameIndividualAxiom}
         * that either has annotations or anonymous class expressions in subject or object positions.
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
            public InternalCache.Loading<ComplexImpl, Object[]> getContentCache() {
                return content;
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof ComplexImpl && Arrays.equals(getContent(), ((ComplexImpl) other).getContent());
            }

            @Override
            protected ComplexImpl makeCopyWith(ONTObject<OWLSameIndividualAxiom> other) {
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
