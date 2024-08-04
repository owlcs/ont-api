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

import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.AxiomTranslator;
import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.ONTWrapperImpl;
import com.github.owlcs.ontapi.internal.WriteHelper;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontapi.model.OntClass;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntStatement;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLSubClassOfAxiom} implementations.
 * Examples:
 * <pre>{@code
 * pizza:JalapenoPepperTopping
 *         rdfs:subClassOf   pizza:PepperTopping ;
 *         rdfs:subClassOf   [ a                   owl:Restriction ;
 *                             owl:onProperty      pizza:hasSpiciness ;
 *                             owl:someValuesFrom  pizza:Hot
 *                           ] .
 * }</pre>
 * <p>
 * Created by @ssz on 28.09.2016.
 *
 * @see <a href='https://www.w3.org/TR/owl-syntax/#Subclass_Axioms'>9.1.1 Subclass Axioms</a>
 */
public class SubClassOfTranslator extends AxiomTranslator<OWLSubClassOfAxiom> {

    @Override
    public void write(OWLSubClassOfAxiom axiom, OntModel model) {
        WriteHelper.writeTriple(model, axiom.getSubClass(), RDFS.subClassOf, axiom.getSuperClass(),
                axiom.annotationsAsList());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        return listByPredicate(model, RDFS.subClassOf).filterKeep(this::filter);
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return statement.getPredicate().equals(RDFS.subClassOf) && filter(statement);
    }

    public boolean filter(Statement s) {
        return s.getSubject().canAs(OntClass.class) && s.getObject().canAs(OntClass.class);
    }

    @Override
    public ONTObject<OWLSubClassOfAxiom> toAxiomImpl(OntStatement statement,
                                                     ModelObjectFactory factory,
                                                     AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLSubClassOfAxiom> toAxiomWrap(OntStatement statement,
                                                     ONTObjectFactory factory,
                                                     AxiomsSettings config) {
        ONTObject<? extends OWLClassExpression> sub = factory.getClass(statement.getSubject(OntClass.class));
        ONTObject<? extends OWLClassExpression> sup = factory.getClass(statement.getObject().as(OntClass.class));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLSubClassOfAxiom res = factory.getOWLDataFactory()
                .getOWLSubClassOfAxiom(sub.getOWLObject(), sup.getOWLObject(), TranslateHelper.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(sub).append(sup);
    }

    @Override
    protected final Collection<Triple> getSearchTriples(OWLSubClassOfAxiom axiom) {
        Triple res = getSearchTriple(axiom);
        return TranslateHelper.isGoodSearchTriple(res) ? Collections.singleton(res) : Collections.emptySet();
    }

    private Triple getSearchTriple(OWLSubClassOfAxiom axiom) {
        if (axiom instanceof WithTriple) {
            return TranslateHelper.toSearchTriple(((WithTriple) axiom).asTriple());
        }
        Node subject = TranslateHelper.getSearchNodeOrANY(axiom.getSubClass());
        Node object = TranslateHelper.getSearchNodeOrANY(axiom.getSuperClass());
        return Triple.create(subject, RDFS.subClassOf.asNode(), object);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.SubClassOfAxiomImpl
     */
    public abstract static class AxiomImpl extends ONTAxiomImpl<OWLSubClassOfAxiom>
            implements WithTwoObjects.Unary<OWLClassExpression>, OWLSubClassOfAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        protected AxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLSubClassOfAxiom}.
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

        @FactoryAccessor
        @Override
        protected OWLSubClassOfAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLSubClassOfAxiom(eraseModel(getSubClass()),
                    eraseModel(getSuperClass()), annotations);
        }

        @Override
        public OWLClassExpression getSubClass() {
            return getONTSubject().getOWLObject();
        }

        @Override
        public OWLClassExpression getSuperClass() {
            return getONTObject().getOWLObject();
        }

        @Override
        public ONTObject<? extends OWLClassExpression> findByURI(String uri, ModelObjectFactory factory) {
            return factory.getClass(uri);
        }

        @Override
        public ONTObject<? extends OWLClassExpression> subjectFromStatement(OntStatement statement,
                                                                            ModelObjectFactory factory) {
            return factory.getClass(statement.getSubject(OntClass.class));
        }

        @Override
        public ONTObject<? extends OWLClassExpression> objectFromStatement(OntStatement statement,
                                                                           ModelObjectFactory factory) {
            return factory.getClass(statement.getObject(OntClass.class));
        }

        @Override
        public boolean isGCI() {
            return getSubClass().isAnonymous();
        }

        @Override
        public boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        /**
         * An {@link OWLSubClassOfAxiom} that has named classes as subject and object and has no annotations.
         */
        public static class SimpleImpl extends AxiomImpl implements UnarySimple<OWLClassExpression> {

            private static final BiFunction<Triple, Supplier<OntModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return false;
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
            public boolean canContainAnnotationProperties() {
                return false;
            }

            @Override
            public boolean canContainAnonymousIndividuals() {
                return false;
            }
        }

        /**
         * An {@link OWLSubClassOfAxiom}
         * that either has annotations or anonymous class expressions in subject or object positions.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl implements UnaryWithContent<ComplexImpl, OWLClassExpression> {

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
                if (hasURISubject() && !sameSubject(other)) {
                    return false;
                }
                if (hasURIObject() && !sameObject(other)) {
                    return false;
                }
                return other instanceof ComplexImpl && Arrays.equals(getContent(), ((ComplexImpl) other).getContent());
            }

            @Override
            protected boolean sameAs(ONTStatementImpl other) {
                if (notSame(other)) {
                    return false;
                }
                // no #sameTriple(), since it can contain b-nodes
                return sameContent(other);
            }

            @Override
            public ONTObject<OWLSubClassOfAxiom> merge(ONTObject<OWLSubClassOfAxiom> other) {
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
