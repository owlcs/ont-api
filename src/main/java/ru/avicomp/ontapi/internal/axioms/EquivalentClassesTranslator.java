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
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.internal.objects.FactoryAccessor;
import ru.avicomp.ontapi.internal.objects.ONTEntityImpl;
import ru.avicomp.ontapi.internal.objects.ONTStatementImpl;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.owlapi.axioms.OWLEquivalentClassesAxiomImpl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLEquivalentClassesAxiom} implementations.
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
                                                        InternalObjectFactory factory,
                                                        InternalConfig config) {
        ONTObject<? extends OWLClassExpression> a = factory.getClass(statement.getSubject(getView()));
        ONTObject<? extends OWLClassExpression> b = factory.getClass(statement.getObject().as(getView()));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLEquivalentClassesAxiom res = factory.getOWLDataFactory()
                .getOWLEquivalentClassesAxiom(a.getOWLObject(), b.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(a).append(b);
    }

    /**
     * Although the {@link OWLNaryAxiom} interface allows axioms with any arity,
     * an instance of this implementation has exactly {@code 2} operands, no more, no less.
     *
     * @see OWLEquivalentClassesAxiomImpl
     */
    public abstract static class AxiomImpl extends ClassNaryAxiomImpl<OWLEquivalentClassesAxiom>
            implements OWLEquivalentClassesAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
            super(s, p, o, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLEquivalentClassesAxiom}.
         *
         * @param statement  {@link OntStatement}, not {@code null}
         * @param model  {@link OntGraphModel} provider, not {@code null}
         * @param factory {@link InternalObjectFactory}, not {@code null}
         * @param config  {@link InternalConfig}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       Supplier<OntGraphModel> model,
                                       InternalObjectFactory factory,
                                       InternalConfig config) {
            return WithManyObjects.create(statement, model,
                    SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        @Override
        protected final long count() {
            return 2;
        }

        @Override
        public Stream<OWLClass> namedClasses() {
            return classExpressions().filter(OWLEquivalentClassesAxiomImpl::isNamed)
                    .map(OWLClassExpression::asOWLClass);
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

        @FactoryAccessor
        @Override
        protected OWLEquivalentClassesAxiom createAxiom(Collection<OWLClassExpression> members,
                                                        Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLEquivalentClassesAxiom(members,
                    annotations == null ? NO_ANNOTATIONS : annotations);
        }

        @FactoryAccessor
        @Override
        protected OWLSubClassOfAxiom createSubClassOf(OWLClassExpression a, OWLClassExpression b) {
            return getDataFactory().getOWLSubClassOfAxiom(a, b);
        }

        /**
         * An {@link OWLEquivalentClassesAxiom} that has named classes as subject and object and has no annotations.
         */
        protected static class SimpleImpl extends AxiomImpl implements Simple<OWLClassExpression> {

            private static final BiFunction<Triple, Supplier<OntGraphModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            protected SimpleImpl(Object s, String p, Object o, Supplier<OntGraphModel> m) {
                super(s, p, o, m);
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                // triple is checked above in trace
                return other instanceof SimpleImpl
                        && subject.equals(((AxiomImpl) other).getObjectURI())
                        && object.equals(((AxiomImpl) other).getSubjectURI());
            }

            @Override
            protected AxiomImpl makeCopyWith(ONTObject<OWLEquivalentClassesAxiom> other) {
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
            public Set<OWLClass> getNamedClassSet() {
                return (Set<OWLClass>) getOWLComponentsAsSet();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLClassExpression> getClassExpressionSet() {
                return (Set<OWLClassExpression>) getOWLComponentsAsSet();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLEntity> getSignatureSet() {
                return (Set<OWLEntity>) getOWLComponentsAsSet();
            }

            @Override
            public boolean containsNamedClass(OWLClass clazz) {
                String uri = ONTEntityImpl.getURI(clazz);
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
                implements Complex<ComplexImpl, OWLClassExpression> {

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
            public InternalCache.Loading<ComplexImpl, Object[]> getContentCache() {
                return content;
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof ComplexImpl && Arrays.equals(getContent(), ((ComplexImpl) other).getContent());
            }

            @Override
            protected ComplexImpl makeCopyWith(ONTObject<OWLEquivalentClassesAxiom> other) {
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
