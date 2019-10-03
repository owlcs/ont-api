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
import org.apache.jena.rdf.model.Property;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.internal.objects.*;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.owlapi.axioms.OWLEquivalentClassesAxiomImpl;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class {@link AbstractNaryTranslator}
 * Example of ttl:
 * <pre>{@code
 *  pizza:SpicyTopping owl:equivalentClass [ a owl:Class; owl:intersectionOf ( pizza:PizzaTopping [a owl:Restriction; owl:onProperty pizza:hasSpiciness; owl:someValuesFrom pizza:Hot] )] ;
 * }
 * </pre>
 * <p>
 * Created by @szuev on 29.09.2016.
 *
 * @see OWLEquivalentClassesAxiom
 */
public class EquivalentClassesTranslator extends AbstractNaryTranslator<OWLEquivalentClassesAxiom, OWLClassExpression, OntCE> {

    @Override
    public Property getPredicate() {
        return OWL.equivalentClass;
    }

    @Override
    Class<OntCE> getView() {
        return OntCE.class;
    }

    @Override
    public ONTObject<OWLEquivalentClassesAxiom> toAxiom(OntStatement statement,
                                                        Supplier<OntGraphModel> model,
                                                        InternalObjectFactory factory,
                                                        InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLEquivalentClassesAxiom> toAxiom(OntStatement statement,
                                                        InternalObjectFactory reader,
                                                        InternalConfig config) {
        ONTObject<? extends OWLClassExpression> a = reader.getClass(statement.getSubject(getView()));
        ONTObject<? extends OWLClassExpression> b = reader.getClass(statement.getObject().as(getView()));
        Collection<ONTObject<OWLAnnotation>> annotations = reader.getAnnotations(statement, config);
        OWLEquivalentClassesAxiom res = reader.getOWLDataFactory()
                .getOWLEquivalentClassesAxiom(a.getOWLObject(), b.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(a).append(b);
    }

    /**
     * Although the {@link OWLNaryAxiom} interface allows axioms with any arity,
     * an instance of this implementation has exactly {@code 2} operands, no more, no less.
     *
     * @see OWLEquivalentClassesAxiomImpl
     */
    @SuppressWarnings("WeakerAccess")
    @ParametersAreNonnullByDefault
    public abstract static class AxiomImpl extends ONTAxiomImpl<OWLEquivalentClassesAxiom>
            implements WithManyObjects<OWLClassExpression>, WithMerge<ONTObject<OWLEquivalentClassesAxiom>>,
            OWLEquivalentClassesAxiom {

        /**
         * Creates an {@link ONTObject} container, which is {@link OWLEquivalentClassesAxiom},
         * using the given {@link OntStatement} as a source.
         * <p>
         * Impl notes:
         * If there is no sub-annotations and subject and object are URI-{@link org.apache.jena.rdf.model.Resource}s,
         * then a simplified instance of {@link SimpleImpl} is returned.
         * Otherwise the instance is {@link ComplexImpl} with a cache inside.
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
            SimpleImpl s = new SimpleImpl(statement.asTriple(), model);
            Object[] content = WithSortedContent.initContent(s, statement, SET_HASH_CODE, true, factory, config);
            if (content == EMPTY) {
                return s;
            }
            ComplexImpl c = new ComplexImpl(statement.asTriple(), model);
            c.setHashCode(s.hashCode);
            c.putContent(content);
            return c;
        }

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
            super(s, p, o, m);
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLClassExpression>> listONTComponents(OntStatement statement,
                                                                                           InternalObjectFactory factory) {
            return Iter.of(factory.getClass(statement.getSubject(OntCE.class)),
                    factory.getClass(statement.getObject(OntCE.class)));
        }

        @Override
        public ONTObject<? extends OWLClassExpression> findByURI(String uri, InternalObjectFactory factory) {
            return ONTClassImpl.find(uri, factory, model);
        }

        @Override
        public Stream<OWLClassExpression> classExpressions() {
            return sorted().map(ONTObject::getOWLObject);
        }

        @Override
        public Stream<OWLClass> namedClasses() {
            return classExpressions().filter(OWLEquivalentClassesAxiomImpl::isNamed)
                    .map(OWLClassExpression::asOWLClass);
        }

        @Override
        public Set<OWLClassExpression> getClassExpressionsMinus(OWLClassExpression... exclude) {
            Set<OWLClassExpression> set = new HashSet<>(Arrays.asList(exclude));
            return classExpressions().filter(x -> !set.contains(x))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        @Override
        public boolean containsNamedEquivalentClass() {
            return members().map(ONTObject::getOWLObject).anyMatch(OWLEquivalentClassesAxiomImpl::isNamed);
        }

        @Override
        public boolean containsOWLNothing() {
            return members().map(ONTObject::getOWLObject).anyMatch(OWLClassExpression::isOWLNothing);
        }

        @Override
        public boolean containsOWLThing() {
            return members().map(ONTObject::getOWLObject).anyMatch(OWLClassExpression::isOWLThing);
        }

        @Override
        public boolean contains(OWLClassExpression ce) {
            return members().map(ONTObject::getOWLObject).anyMatch(ce::equals);
        }

        @FactoryAccessor
        @Override
        public Collection<OWLEquivalentClassesAxiom> asPairwiseAxioms() {
            return createSet(eraseModel());
        }

        @FactoryAccessor
        @Override
        public Collection<OWLEquivalentClassesAxiom> splitToAnnotatedPairs() {
            return createSet(eraseModel());
        }

        @FactoryAccessor
        @Override
        public Collection<OWLSubClassOfAxiom> asOWLSubClassOfAxioms() {
            return walkAllPairwise((a, b) -> getDataFactory().getOWLSubClassOfAxiom(a, b));
        }

        @FactoryAccessor
        @Override
        protected OWLEquivalentClassesAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLEquivalentClassesAxiom(members()
                    .map(x -> eraseModel(x.getOWLObject())).collect(Collectors.toList()), annotations);
        }

        @Override
        public AxiomImpl merge(ONTObject<OWLEquivalentClassesAxiom> other) {
            if (this == other) {
                return this;
            }
            if (other instanceof AxiomImpl && sameTriple((AxiomImpl) other)) {
                return this;
            }
            AxiomImpl res = makeCopyWith(other);
            res.hashCode = hashCode;
            return res;
        }

        @Override
        public boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        /**
         * Creates an instance of {@link AxiomImpl}
         * with additional triples getting from the specified {@code other} object.
         * The returned instance must be equivalent to this instance.
         *
         * @param other {@link ONTObject} with {@link OWLEquivalentClassesAxiom}, not {@code null}
         * @return {@link AxiomImpl} - a fresh instance that equals to this
         */
        abstract AxiomImpl makeCopyWith(ONTObject<OWLEquivalentClassesAxiom> other);

        /**
         * An {@link OWLEquivalentClassesAxiom} that has named classes as subject and object and has no annotations.
         */
        protected static class SimpleImpl extends AxiomImpl implements Simple<OWLClassExpression> {

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            protected SimpleImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
                super(s, p, o, m);
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                // triple is checked above in trace
                return other instanceof AxiomImpl
                        && subject.equals(((AxiomImpl) other).getObjectURI())
                        && object.equals(((AxiomImpl) other).getSubjectURI());
            }

            @Override
            AxiomImpl makeCopyWith(ONTObject<OWLEquivalentClassesAxiom> other) {
                if (other instanceof SimpleImpl) {
                    Triple t = ((SimpleImpl) other).asTriple();
                    return new SimpleImpl(subject, predicate, object, model) {

                        @Override
                        public Stream<Triple> triples() {
                            return Stream.concat(super.triples(), Stream.of(t));
                        }
                    };
                }
                return new SimpleImpl(subject, predicate, object, model) {
                    @Override
                    public Stream<Triple> triples() {
                        return Stream.concat(super.triples(), other.triples());
                    }
                };
            }

            @Override
            public Set<OWLClass> getNamedClassSet() {
                return sorted().map(x -> x.getOWLObject().asOWLClass()).collect(Collectors.toSet());
            }

            @Override
            public Set<OWLClassExpression> getClassExpressionSet() {
                return sorted().map(x -> x.getOWLObject().asOWLClass()).collect(Collectors.toSet());
            }

            @Override
            public Set<OWLEntity> getSignatureSet() {
                return sorted().map(x -> x.getOWLObject().asOWLClass()).collect(Collectors.toSet());
            }

            @Override
            public boolean containsEntity(OWLEntity entity) {
                if (!entity.isOWLClass()) {
                    return false;
                }
                String uri = ONTEntityImpl.getURI(entity);
                return subject.equals(uri) || object.equals(uri);
            }

            @Override
            public boolean canContainDatatypes() {
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

            @Override
            public boolean canContainObjectProperties() {
                return false;
            }

            @Override
            public boolean canContainAnnotationProperties() {
                return false;
            }

            @Override
            public boolean canContainAnonymousIndividuals() {
                return false;
            }

        }

        /**
         * An {@link OWLEquivalentClassesAxiom}
         * that either has annotations or anonymous class expressions in subject or object positions.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl
                implements WithManyObjects.WithSortedContent<ComplexImpl, OWLClassExpression> {
            protected final InternalCache.Loading<ComplexImpl, Object[]> content;

            public ComplexImpl(Triple t, Supplier<OntGraphModel> m) {
                this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
            }

            protected ComplexImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
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
            AxiomImpl makeCopyWith(ONTObject<OWLEquivalentClassesAxiom> other) {
                ComplexImpl res = new ComplexImpl(subject, predicate, object, model) {
                    @Override
                    public Stream<Triple> triples() {
                        return Stream.concat(super.triples(), other.triples());
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
