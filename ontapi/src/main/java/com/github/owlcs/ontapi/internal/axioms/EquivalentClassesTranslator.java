/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
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

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.owlapi.axioms.EquivalentClassesAxiomImpl;
import org.apache.jena.graph.Node;
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
 * A translator that provides {@link OWLEquivalentClassesAxiom} implementations.
 * <p>
 * Example:
 * <pre>{@code
 * :Spiciness  a                owl:Class ;
 *         owl:equivalentClass  [ a            owl:Class ;
 *                                owl:unionOf  ( :Hot :Medium :Mild )
 *                              ] .
 * }</pre>
 * <p>
 * Created by @szuev on 29.09.2016.
 */
public class EquivalentClassesTranslator extends AbstractNaryTranslator<OWLEquivalentClassesAxiom, OWLClassExpression, OntClass> {

    @Override
    public Property getPredicate() {
        return OWL.equivalentClass;
    }

    @Override
    Class<OntClass> getView() {
        return OntClass.class;
    }

    @Override
    public ONTObject<OWLEquivalentClassesAxiom> toAxiomImpl(OntStatement statement,
                                                            ModelObjectFactory factory,
                                                            AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLEquivalentClassesAxiom> toAxiomWrap(OntStatement statement,
                                                            ONTObjectFactory factory,
                                                            AxiomsSettings config) {
        ONTObject<? extends OWLClassExpression> a = factory.getClass(statement.getSubject(getView()));
        ONTObject<? extends OWLClassExpression> b = factory.getClass(statement.getObject().as(getView()));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLEquivalentClassesAxiom res = factory.getOWLDataFactory()
                .getOWLEquivalentClassesAxiom(a.getOWLObject(), b.getOWLObject(), TranslateHelper.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(a).append(b);
    }

    @Override
    Triple getONTSearchTriple(OWLEquivalentClassesAxiom axiom) {
        Triple res = super.getONTSearchTriple(axiom);
        return res != null && TranslateHelper.isGoodSearchTriple(res) ? res : null;
    }

    @Override
    Triple createSearchTriple(OWLObject subject, OWLObject object) {
        Node left = TranslateHelper.getSearchNodeOrANY(subject);
        Node right = TranslateHelper.getSearchNodeOrANY(object);
        Triple res = Triple.create(left, getPredicate().asNode(), right);
        return TranslateHelper.isGoodSearchTriple(res) ? res : null;
    }

    /**
     * Although the {@link OWLNaryAxiom} interface allows axioms with any arity,
     * an instance of this implementation has exactly {@code 2} operands, no more, no less.
     *
     * @see EquivalentClassesAxiomImpl
     */
    public abstract static class AxiomImpl extends ClassNaryAxiomImpl<OWLEquivalentClassesAxiom>
            implements OWLEquivalentClassesAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntModel> m) {
            super(s, p, o, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLEquivalentClassesAxiom}.
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
        public Stream<OWLClass> namedClasses() {
            return classExpressions().filter(EquivalentClassesAxiomImpl::isNamed)
                    .map(OWLClassExpression::asOWLClass);
        }

        @Override
        public boolean containsNamedEquivalentClass() {
            return members().map(ONTObject::getOWLObject).anyMatch(EquivalentClassesAxiomImpl::isNamed);
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

            private static final BiFunction<Triple, Supplier<OntModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            protected SimpleImpl(Object s, String p, Object o, Supplier<OntModel> m) {
                super(s, p, o, m);
            }

            private static boolean isNonBuiltin(String uri) {
                return !uri.equals(OWL.Thing.getURI()) && !uri.equals(OWL.Nothing.getURI());
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof SimpleImpl && isReverseTriple((SimpleImpl) other);
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

            @Override
            public boolean containsNamedEquivalentClass() {
                return isNonBuiltin((String) subject) || isNonBuiltin((String) object);
            }

            @Override
            public boolean containsOWLNothing() {
                return hasURIResource(OWL.Nothing);
            }

            @Override
            public boolean containsOWLThing() {
                return hasURIResource(OWL.Thing);
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
                return hasURIResource(ONTEntityImpl.getURI(clazz));
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
