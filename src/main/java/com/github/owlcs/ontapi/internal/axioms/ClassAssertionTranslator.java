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
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.ONTWrapperImpl;
import com.github.owlcs.ontapi.internal.OntModelSupport;
import com.github.owlcs.ontapi.internal.WriteHelper;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTAnnotationImpl;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.internal.objects.WithContent;
import com.github.owlcs.ontapi.internal.objects.WithoutAnnotations;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontapi.impl.OntGraphModelImpl;
import org.apache.jena.ontapi.model.OntClass;
import org.apache.jena.ontapi.model.OntIndividual;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntObject;
import org.apache.jena.ontapi.model.OntStatement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLClassAssertionAxiom} implementations.
 * The main statement is {@code a rdf:type C},
 * where {@code a} - named or anonymous OWL individual and {@code C} - any class expression.
 * Example:
 * <pre>{@code pizza:France rdf:type owl:NamedIndividual, pizza:Country, owl:Thing.}</pre>
 * <p>
 * Created by @ssz on 28.09.2016.
 *
 * @see OntModel#individuals()
 * @see OntGraphModelImpl#listIndividuals()
 */
@SuppressWarnings("WeakerAccess")
public class ClassAssertionTranslator extends AbstractSimpleTranslator<OWLClassAssertionAxiom> {

    @Override
    public void write(OWLClassAssertionAxiom axiom, OntModel model) {
        OntClass ce = WriteHelper.addClassExpression(model, axiom.getClassExpression());
        if (!ce.canAsAssertionClass()) {
            throw new OntApiException.Unsupported(
                    axiom + " cannot be added: prohibited by the profile " + OntModelSupport.profileName(model)
            );
        }
        OWLIndividual individual = axiom.getIndividual();
        OntObject subject = individual.isAnonymous() ?
                WriteHelper.toResource(individual).inModel(model).as(OntObject.class) :
                WriteHelper.addIndividual(model, individual);
        OntStatement statement = subject.addStatement(RDF.type, ce);
        WriteHelper.addAnnotations(statement, axiom.annotationsAsList());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        Set<String> forbidden = getSystemResources(model);
        return model.getBaseGraph().find(Node.ANY, RDF.Nodes.type, Node.ANY)
                .filterDrop(t -> t.getObject().isURI() && forbidden.contains(t.getObject().getURI()))
                .mapWith(model::asStatement)
                .filterKeep(this::filter);
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return statement.isDeclaration() && filter(statement);
    }

    public boolean filter(OntStatement statement) {
        // first class then individual,
        // since anonymous individual has more sophisticated and time-consuming checking
        return statement.getObject().canAs(OntClass.class) &&
                statement.getObject(OntClass.class).canAsAssertionClass() &&
                statement.getSubject().canAs(OntIndividual.class);
    }

    @Override
    public ONTObject<OWLClassAssertionAxiom> toAxiomImpl(OntStatement statement,
                                                         ModelObjectFactory factory,
                                                         AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLClassAssertionAxiom> toAxiomWrap(OntStatement statement,
                                                         ONTObjectFactory factory,
                                                         AxiomsSettings config) {
        ONTObject<? extends OWLIndividual> i = factory.getIndividual(statement.getSubject(OntIndividual.class));
        ONTObject<? extends OWLClassExpression> ce = factory.getClass(statement.getObject(OntClass.class));

        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLClassAssertionAxiom res = factory.getOWLDataFactory()
                .getOWLClassAssertionAxiom(ce.getOWLObject(), i.getOWLObject(), TranslateHelper.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(i).append(ce);
    }

    @Override
    boolean testSearchTriple(Triple t) {
        return t.getObject().isURI();
    }

    @Override
    Triple createSearchTriple(OWLClassAssertionAxiom axiom) {
        Node object = TranslateHelper.getSearchNode(axiom.getClassExpression());
        if (object == null) return null;
        Node subject = TranslateHelper.getSearchNode(axiom.getIndividual());
        if (subject == null) return null;
        return Triple.create(subject, RDF.type.asNode(), object);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.ClassAssertionAxiomImpl
     */
    public abstract static class AxiomImpl extends ONTAxiomImpl<OWLClassAssertionAxiom>
            implements OWLClassAssertionAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            super(t, m);
        }

        protected AxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        /**
         * Creates an {@link ONTObject} container that is also {@link OWLClassAssertionAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link ONTObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            AxiomImpl s = new SimpleImpl(statement.asTriple(), factory.model());
            Object[] content = ComplexImpl.initContent(s, statement, factory, config);
            if (content == null) return s;
            ComplexImpl c = new ComplexImpl(statement.asTriple(), factory.model());
            c.hashCode = s.hashCode;
            c.putContent(content);
            return c;
        }

        @Override
        public OWLIndividual getIndividual() {
            return getONTSubject(getObjectFactory()).getOWLObject();
        }

        @Override
        public OWLClassExpression getClassExpression() {
            return getONTObject(getObjectFactory()).getOWLObject();
        }

        public ONTObject<? extends OWLIndividual> getONTSubject(ModelObjectFactory factory) {
            return hasURISubject() ?
                    factory.getNamedIndividual(getSubjectURI()) :
                    factory.getAnonymousIndividual(getSubjectBlankNodeId());
        }

        public ONTObject<? extends OWLIndividual> getONTSubject() {
            return getONTSubject(getObjectFactory());
        }

        public ONTObject<? extends OWLClassExpression> getONTObject() {
            return getONTObject(getObjectFactory());
        }

        public abstract ONTObject<? extends OWLClassExpression> getONTObject(ModelObjectFactory factory);

        @FactoryAccessor
        @Override
        protected OWLClassAssertionAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLClassAssertionAxiom(eraseModel(getClassExpression()),
                    eraseModel(getIndividual()), annotations);
        }

        @FactoryAccessor
        @Override
        public OWLSubClassOfAxiom asOWLSubClassOfAxiom() {
            DataFactory df = getDataFactory();
            return df.getOWLSubClassOfAxiom(df.getOWLObjectOneOf(eraseModel(getIndividual())),
                    eraseModel(getClassExpression()));
        }

        @Override
        public final boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        /**
         * An {@link OWLClassAssertionAxiom} with named class expressions and without annotations.
         */
        public static class SimpleImpl extends AxiomImpl implements WithoutAnnotations {

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            @Override
            public boolean isAnnotated() {
                return false;
            }

            @Override
            public ONTObject<? extends OWLClassExpression> getONTObject(ModelObjectFactory factory) {
                return factory.getClass(getObjectURI());
            }

            @Override
            public Set<OWLNamedIndividual> getNamedIndividualSet() {
                return hasURISubject() ? createSet(getONTSubject().getOWLObject().asOWLNamedIndividual()) : createSet();
            }

            @Override
            public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
                return hasURISubject() ? createSet() :
                        createSet(getONTSubject().getOWLObject().asOWLAnonymousIndividual());
            }

            @Override
            public Set<OWLClass> getNamedClassSet() {
                return createSet(getONTObject().getOWLObject().asOWLClass());
            }

            @Override
            public Set<OWLClassExpression> getClassExpressionSet() {
                return createSet(getONTObject().getOWLObject());
            }

            @Override
            public Set<OWLEntity> getSignatureSet() {
                ModelObjectFactory f = getObjectFactory();
                Set<OWLEntity> res = createSortedSet();
                res.add(getONTObject(f).getOWLObject().asOWLClass());
                if (hasURISubject()) {
                    res.add(getONTSubject(f).getOWLObject().asOWLNamedIndividual());
                }
                return res;
            }

            @Override
            public Stream<ONTObject<? extends OWLObject>> objects() {
                ModelObjectFactory f = getObjectFactory();
                return Stream.of(getONTSubject(f), getONTObject(f));
            }

            @Override
            public boolean containsNamedIndividual(OWLNamedIndividual individual) {
                return hasURISubject() && getSubjectURI().equals(ONTEntityImpl.getURI(individual));
            }

            @Override
            public boolean containsNamedClass(OWLClass clazz) {
                return getObjectURI().equals(ONTEntityImpl.getURI(clazz));
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return false;
            }

            @Override
            public boolean canContainDatatypes() {
                return false;
            }

            @Override
            public boolean canContainAnonymousIndividuals() {
                return false;
            }

            @Override
            public boolean canContainObjectProperties() {
                return false;
            }

            @Override
            public boolean canContainDataProperties() {
                return false;
            }
        }

        /**
         * An {@link OWLClassAssertionAxiom} that either has annotations
         * or contains anonymous class expressions in the main triple's object position.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        public static class ComplexImpl extends AxiomImpl implements WithContent<ComplexImpl> {

            protected final InternalCache.Loading<ComplexImpl, Object[]> content;

            public ComplexImpl(Triple t, Supplier<OntModel> m) {
                this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
            }

            protected ComplexImpl(Object s, String p, Object o, Supplier<OntModel> m) {
                super(s, p, o, m);
                this.content = createContentCache();
            }

            static Object[] initContent(AxiomImpl axiom,
                                        OntStatement statement,
                                        ModelObjectFactory factory,
                                        AxiomsSettings config) {
                Collection<?> annotations = ONTAxiomImpl.collectAnnotations(statement, factory, config);
                int size = annotations.size();
                int hash = OWLObject.hashIteration(axiom.hashIndex(), axiom.getONTSubject(factory).hashCode());
                Object object = null;
                if (statement.getObject().isURIResource()) {
                    hash = OWLObject.hashIteration(hash,
                            factory.getClass(statement.getObject().asNode().getURI()).hashCode());
                } else {
                    size++;
                    object = factory.getClass(statement.getObject(OntClass.class));
                    hash = OWLObject.hashIteration(hash, object.hashCode());
                }
                if (size == 0) {
                    axiom.hashCode = OWLObject.hashIteration(hash, 1);
                    return null;
                }
                int h = 1;
                Object[] res = new Object[size];
                int index = 0;
                if (object != null) {
                    res[index++] = object;
                }
                for (Object a : annotations) {
                    res[index++] = a;
                    h = WithContent.hashIteration(h, a.hashCode());
                }
                axiom.hashCode = OWLObject.hashIteration(hash, h);
                return res;
            }

            @Override
            public Object[] collectContent() {
                OntStatement statement = asStatement();
                ONTObjectFactory factory = getObjectFactory();
                List<ONTObject<?>> res = new ArrayList<>(1);
                if (!statement.getObject().isURIResource()) {
                    res.add(factory.getClass(statement.getObject(OntClass.class)));
                }
                res.addAll(ONTAxiomImpl.collectAnnotations(statement, factory, getConfig()));
                return res.toArray();
            }

            @Override
            public InternalCache.Loading<ComplexImpl, Object[]> getContentCache() {
                return content;
            }

            @Override
            public ONTObject<? extends OWLClassExpression> getONTObject(ModelObjectFactory factory) {
                return getONTObject(getContent(), factory);
            }

            @SuppressWarnings("unchecked")
            protected ONTObject<? extends OWLClassExpression> getONTObject(Object[] content, ModelObjectFactory factory) {
                return hasURIObject() ? factory.getClass(getObjectURI()) : (ONTObject<? extends OWLClassExpression>) content[0];
            }

            @SuppressWarnings("unchecked")
            @Override
            public Stream<ONTObject<? extends OWLObject>> objects() {
                Object[] content = getContent();
                ModelObjectFactory f = getObjectFactory();
                Stream<?> res = Arrays.stream(content);
                Stream<?> objects;
                if (hasURIObject()) {
                    objects = Stream.of(getONTSubject(f), getONTObject(content, f));
                } else {
                    objects = Stream.of(getONTSubject(f));
                }
                res = Stream.concat(objects, res);
                return (Stream<ONTObject<? extends OWLObject>>) res;
            }

            @Override
            public boolean isAnnotated() {
                int content = getContent().length;
                return content > (hasURIObject() ? 0 : 1);
            }

            @Override
            public Stream<OWLAnnotation> annotations() {
                return ONTAnnotationImpl.contentAsStream(getContent(), hasURIObject() ? 0 : 1);
            }

            @Override
            public List<OWLAnnotation> annotationsAsList() {
                return ONTAnnotationImpl.contentAsList(getContent(), hasURIObject() ? 0 : 1);
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof ComplexImpl && Arrays.equals(getContent(), ((ComplexImpl) other).getContent());
            }

            @Override
            public ONTObject<OWLClassAssertionAxiom> merge(ONTObject<OWLClassAssertionAxiom> other) {
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
