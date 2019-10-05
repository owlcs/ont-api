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
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.internal.objects.*;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.OntModels;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

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
 * Created by @szuev on 28.09.2016.
 *
 * @see <a href='https://www.w3.org/TR/owl-syntax/#Subclass_Axioms'>9.1.1 Subclass Axioms</a>
 */
@SuppressWarnings("WeakerAccess")
public class SubClassOfTranslator extends AxiomTranslator<OWLSubClassOfAxiom> {

    @Override
    public void write(OWLSubClassOfAxiom axiom, OntGraphModel model) {
        WriteHelper.writeTriple(model, axiom.getSubClass(), RDFS.subClassOf, axiom.getSuperClass(), axiom.annotations());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        return OntModels.listLocalStatements(model, null, RDFS.subClassOf, null).filterKeep(this::filter);
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return statement.getPredicate().equals(RDFS.subClassOf) && filter(statement);
    }

    public boolean filter(Statement s) {
        return s.getSubject().canAs(OntCE.class) && s.getObject().canAs(OntCE.class);
    }

    @Override
    public ONTObject<OWLSubClassOfAxiom> toAxiom(OntStatement statement,
                                                 Supplier<OntGraphModel> model,
                                                 InternalObjectFactory factory,
                                                 InternalConfig config) {
        return AxiomImpl.create(statement, model, factory, config);
    }

    @Override
    public ONTObject<OWLSubClassOfAxiom> toAxiom(OntStatement statement,
                                                 InternalObjectFactory factory,
                                                 InternalConfig config) {
        ONTObject<? extends OWLClassExpression> sub = factory.getClass(statement.getSubject(OntCE.class));
        ONTObject<? extends OWLClassExpression> sup = factory.getClass(statement.getObject().as(OntCE.class));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLSubClassOfAxiom res = factory.getOWLDataFactory()
                .getOWLSubClassOfAxiom(sub.getOWLObject(), sup.getOWLObject(), ONTObject.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(sub).append(sup);
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.axioms.OWLSubClassOfAxiomImpl
     */
    public abstract static class AxiomImpl extends ONTAxiomImpl<OWLSubClassOfAxiom>
            implements WithTwoObjects.Unary<OWLClassExpression>, OWLSubClassOfAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link ONTObject} container, which is {@link OWLSubClassOfAxiom},
         * for the given {@link OntStatement}.
         *
         * Impl notes:
         * If there is no sub-annotations and subject and object are URI-{@link org.apache.jena.rdf.model.Resource}s,
         * then a simplified instance of {@link SimpleImpl} is returned.
         * Otherwise the instance is {@link ComplexImpl} with a cache inside.
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
            SimpleImpl s = new SimpleImpl(statement.asTriple(), model);
            Object[] content = WithPartialContent.initContent(s, statement, SET_HASH_CODE, factory, config);
            if (content == EMPTY) {
                return s;
            }
            ComplexImpl c = new ComplexImpl(statement.asTriple(), model);
            c.setHashCode(s.hashCode);
            c.putContent(content);
            return c;
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
        public ONTObject<? extends OWLClassExpression> findByURI(String uri, InternalObjectFactory factory) {
            return ONTClassImpl.find(uri, factory, model);
        }

        @Override
        public ONTObject<? extends OWLClassExpression> fetchONTSubject(OntStatement statement,
                                                                       InternalObjectFactory factory) {
            return factory.getClass(statement.getSubject(OntCE.class));
        }

        @Override
        public ONTObject<? extends OWLClassExpression> fetchONTObject(OntStatement statement,
                                                                      InternalObjectFactory factory) {
            return factory.getClass(statement.getObject(OntCE.class));
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

            protected SimpleImpl(Triple t, Supplier<OntGraphModel> m) {
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
         * An {@link OWLSubClassOfAxiom}
         * that either has annotations or anonymous class expressions in subject or object positions.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl
                implements UnaryWithContent<ComplexImpl, OWLClassExpression> {

            protected final InternalCache.Loading<ComplexImpl, Object[]> content;

            public ComplexImpl(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
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
        }
    }
}
