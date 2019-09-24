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

import java.util.*;
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
    public abstract static class AxiomImpl extends ONTAxiomImpl<OWLSubClassOfAxiom> implements OWLSubClassOfAxiom {

        protected AxiomImpl(Triple t, Supplier<OntGraphModel> m) {
            super(t, m);
        }

        /**
         * Creates an {@link ONTObject} container, which is {@link OWLSubClassOfAxiom},
         * for the given {@link OntStatement}.
         *
         * Impl notes:
         * If there is no sub-annotations and subject and object are URI-{@link org.apache.jena.rdf.model.Resource}s,
         * then a simplified instance of {@link Simple} is returned.
         * Otherwise the instance is {@link Complex} with a cache inside.
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
            AxiomImpl res;
            Object[] content = Complex.collectContent(statement, factory, config);
            if (content == EMPTY) {
                res = new Simple(statement.asTriple(), model);
            } else {
                res = WithContent.addContent(new Complex(statement.asTriple(), model), content);
            }
            res.hashCode = collectHashCode(res, factory, content);
            return res;
        }

        protected static int collectHashCode(AxiomImpl axiom, InternalObjectFactory factory, Object[] content) {
            int res = axiom.hashIndex();
            int index = 0;
            res = OWLObject.hashIteration(res, (axiom.hasURISubject()
                    ? axiom.findONTSubClass(factory)
                    : content[index++]).hashCode());
            res = OWLObject.hashIteration(res, (axiom.hasURIObject()
                    ? axiom.findONTSuperClass(factory)
                    : content[index++]).hashCode());
            return OWLObject.hashIteration(res, hashCode(content, index));
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            InternalObjectFactory factory = getObjectFactory();
            return Stream.of(findONTSubClass(factory), findONTSuperClass(factory));
        }

        @FactoryAccessor
        @Override
        protected OWLSubClassOfAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLSubClassOfAxiom(eraseModel(getSubClass()),
                    eraseModel(getSuperClass()), annotations);
        }

        @Override
        public OWLClassExpression getSubClass() {
            return getONTSubClass().getOWLObject();
        }

        @Override
        public OWLClassExpression getSuperClass() {
            return getONTSuperClass().getOWLObject();
        }

        /**
         * Gets the subclass in this axiom.
         *
         * @return {@link ONTObject} with {@link OWLClassExpression (Sub) Class Expression}
         */
        public abstract ONTObject<? extends OWLClassExpression> getONTSubClass();

        /**
         * Gets the superclass in this axiom.
         *
         * @return {@link ONTObject} with {@link OWLClassExpression (Super) Class Expression}
         */
        public abstract ONTObject<? extends OWLClassExpression> getONTSuperClass();

        protected ONTObject<? extends OWLClassExpression> findONTSubClass(InternalObjectFactory factory) {
            return ONTClassImpl.find((String) subject, factory, model);
        }

        protected ONTObject<? extends OWLClassExpression> findONTSuperClass(InternalObjectFactory factory) {
            return ONTClassImpl.find((String) object, factory, model);
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
        public static class Simple extends AxiomImpl {

            protected Simple(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return false;
            }

            @Override
            public ONTObject<? extends OWLClassExpression> getONTSubClass() {
                return findONTSubClass(getObjectFactory());
            }

            @Override
            public ONTObject<? extends OWLClassExpression> getONTSuperClass() {
                return findONTSuperClass(getObjectFactory());
            }

            @SuppressWarnings("unchecked")
            private Set getOWLClasses() {
                Set res = createSortedSet();
                InternalObjectFactory factory = getObjectFactory();
                res.add(findONTSubClass(factory).getOWLObject());
                res.add(findONTSuperClass(factory).getOWLObject());
                return res;
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLClass> getNamedClassSet() {
                return (Set<OWLClass>) getOWLClasses();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLClassExpression> getClassExpressionSet() {
                return (Set<OWLClassExpression>) getOWLClasses();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLEntity> getSignatureSet() {
                return (Set<OWLEntity>) getOWLClasses();
            }

            @Override
            public boolean containsEntity(OWLEntity entity) {
                if (!entity.isOWLClass()) {
                    return false;
                }
                InternalObjectFactory factory = getObjectFactory();
                if (findONTSubClass(factory).equals(entity)) {
                    return true;
                }
                return findONTSuperClass(factory).equals(entity);
            }

            @Override
            public Set<OWLDatatype> getDatatypeSet() {
                return createSet();
            }

            @Override
            public Set<OWLNamedIndividual> getNamedIndividualSet() {
                return createSet();
            }

            @Override
            public Set<OWLDataProperty> getDataPropertySet() {
                return createSet();
            }

            @Override
            public Set<OWLObjectProperty> getObjectPropertySet() {
                return createSet();
            }

            @Override
            public Set<OWLAnnotationProperty> getAnnotationPropertySet() {
                return createSet();
            }

            @Override
            public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
                return createSet();
            }
        }

        /**
         * An {@link OWLSubClassOfAxiom}
         * that either has annotations or anonymous class expressions in subject or object positions.
         * It has a public constructor since it is more generic then {@link Simple}.
         */
        public static class Complex extends AxiomImpl implements WithContent<Complex> {
            protected final InternalCache.Loading<Complex, Object[]> content;

            public Complex(Triple t, Supplier<OntGraphModel> m) {
                super(t, m);
                this.content = createContentCache();
            }

            protected static Object[] collectContent(OntStatement statement,
                                                     InternalObjectFactory factory,
                                                     InternalConfig config) {
                Collection annotations = collectAnnotations(statement, factory, config);
                int size = annotations.size();
                Object subject = null;
                if (!statement.getSubject().isURIResource()) {
                    size++;
                    subject = factory.getClass(statement.getSubject(OntCE.class));
                }
                Object object = null;
                if (!statement.getObject().isURIResource()) {
                    size++;
                    object = factory.getClass(statement.getObject(OntCE.class));
                }
                if (size == 0) {
                    return EMPTY;
                }
                Object[] res = new Object[size];
                int index = 0;
                if (subject != null) {
                    res[index++] = subject;
                }
                if (object != null) {
                    res[index++] = object;
                }
                for (Object a : annotations) {
                    res[index++] = a;
                }
                return res;
            }

            @Override
            public boolean isAnnotated() {
                return hasAnnotations(getContent());
            }

            @SuppressWarnings("unchecked")
            @Override
            public Stream<OWLAnnotation> annotations() {
                Object[] content = getContent();
                if (!hasAnnotations(content)) {
                    return Stream.empty();
                }
                Stream res = Arrays.stream(content, getAnnotationStartIndex(), content.length);
                return (Stream<OWLAnnotation>) res;
            }

            @SuppressWarnings("unchecked")
            @Override
            public List<OWLAnnotation> annotationsAsList() {
                Object[] content = getContent();
                if (!hasAnnotations(content)) {
                    return Collections.emptyList();
                }
                List res = Arrays.asList(Arrays.copyOfRange(content, getAnnotationStartIndex(), content.length));
                return (List<OWLAnnotation>) Collections.unmodifiableList(res);
            }

            @SuppressWarnings("unchecked")
            @Override
            public Stream<ONTObject<? extends OWLObject>> objects() {
                Object[] content = getContent();
                Stream res = Arrays.stream(content);
                if (hasURISubject()) {
                    if (hasURIObject()) {
                        res = Stream.concat(super.objects(), res);
                    } else {
                        res = Stream.concat(Stream.of(findONTSubClass(content, getObjectFactory())), res);
                    }
                } else if (hasURIObject()) {
                    res = Stream.concat(Stream.of(findONTSuperClass(content, getObjectFactory())), res);
                }
                return (Stream<ONTObject<? extends OWLObject>>) res;
            }

            private int getAnnotationStartIndex() {
                int res = 0;
                if (!hasURISubject()) {
                    res++;
                }
                if (!hasURIObject()) {
                    res++;
                }
                return res;
            }

            @Override
            public Object[] collectContent() {
                return collectContent(asStatement(), getObjectFactory(), getConfig());
            }

            @Override
            public ONTObject<? extends OWLClassExpression> getONTSubClass() {
                return findONTSubClass(getContent(), getObjectFactory());
            }

            @Override
            public ONTObject<? extends OWLClassExpression> getONTSuperClass() {
                return findONTSuperClass(getContent(), getObjectFactory());
            }

            @SuppressWarnings("unchecked")
            protected ONTObject<? extends OWLClassExpression> findONTSubClass(Object[] content,
                                                                              InternalObjectFactory factory) {
                if (hasURISubject()) {
                    return findONTSubClass(factory);
                }
                return (ONTObject<? extends OWLClassExpression>) content[0];
            }

            @SuppressWarnings("unchecked")
            protected ONTObject<? extends OWLClassExpression> findONTSuperClass(Object[] content,
                                                                                InternalObjectFactory factory) {
                if (hasURIObject()) {
                    return findONTSuperClass(factory);
                }
                return (ONTObject<? extends OWLClassExpression>) content[hasURISubject() ? 0 : 1];
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                if (hasURISubject() && !sameSubject(other)) {
                    return false;
                }
                if (hasURIObject() && !sameObject(other)) {
                    return false;
                }
                return other instanceof Complex && Arrays.equals(getContent(), ((Complex) other).getContent());
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
            public InternalCache.Loading<Complex, Object[]> getContentCache() {
                return content;
            }
        }
    }
}
