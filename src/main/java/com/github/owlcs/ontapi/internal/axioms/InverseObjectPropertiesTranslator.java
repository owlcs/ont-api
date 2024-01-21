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
import com.github.owlcs.ontapi.internal.WriteHelper;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.sszuev.jena.ontapi.model.OntModel;
import com.github.sszuev.jena.ontapi.model.OntObjectProperty;
import com.github.sszuev.jena.ontapi.model.OntStatement;
import com.github.sszuev.jena.ontapi.utils.Iterators;
import com.github.sszuev.jena.ontapi.vocabulary.OWL;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectInverseOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A translator that provides {@link OWLInverseObjectPropertiesAxiom} implementations.
 * Do not confuse with {@link OWLObjectInverseOf OWLObjectInverseOf} {@code OWLObject}.
 * <p>
 * Example:
 * <pre>{@code
 * pizza:hasBase owl:inverseOf pizza:isBaseOf ;
 * }</pre>
 * <p>
 * Created by @ssz on 30.09.2016.
 */
public class InverseObjectPropertiesTranslator extends AbstractSimpleTranslator<OWLInverseObjectPropertiesAxiom> {

    @Override
    public void write(OWLInverseObjectPropertiesAxiom axiom, OntModel model) {
        WriteHelper.writeTriple(model, axiom.getFirstProperty(), OWL.inverseOf, axiom.getSecondProperty(),
                axiom.annotationsAsList());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        // NOTE as a precaution: the first (commented) way is not correct
        // since it includes anonymous object property expressions (based on owl:inverseOf),
        // which might be treated as separated axioms, but OWL-API doesn't think so.
        /*return model.statements(null, OWL.inverseOf, null)
                .filter(OntStatement::isLocal)
                .filter(s -> s.getSubject().canAs(OntOPE.class))
                .filter(s -> s.getObject().canAs(OntOPE.class));*/
        return listByPredicate(model, OWL.inverseOf) // skip {@code _:x owl:inverseOf PN}
                .filterDrop(s -> s.getSubject().isAnon() && s.getObject().isURIResource());
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        if (!OWL.inverseOf.equals(statement.getPredicate())) return false;
        // skip {@code _:x owl:inverseOf PN} (inverse object property expression):
        if (statement.getSubject().isAnon() && statement.getObject().isURIResource()) return false;
        return statement.getSubject().canAs(OntObjectProperty.class) && statement.getObject().canAs(OntObjectProperty.class);
    }

    @Override
    public ONTObject<OWLInverseObjectPropertiesAxiom> toAxiomImpl(OntStatement statement,
                                                                  ModelObjectFactory factory,
                                                                  AxiomsSettings config) {
        return AxiomImpl.create(statement, factory, config);
    }

    @Override
    public ONTObject<OWLInverseObjectPropertiesAxiom> toAxiomWrap(OntStatement statement,
                                                                  ONTObjectFactory factory,
                                                                  AxiomsSettings config) {
        ONTObject<? extends OWLObjectPropertyExpression> f = factory.getProperty(statement.getSubject(OntObjectProperty.class));
        ONTObject<? extends OWLObjectPropertyExpression> s = factory.getProperty(statement.getObject(OntObjectProperty.class));
        Collection<ONTObject<OWLAnnotation>> annotations = factory.getAnnotations(statement, config);
        OWLInverseObjectPropertiesAxiom res = factory.getOWLDataFactory()
                .getOWLInverseObjectPropertiesAxiom(f.getOWLObject(), s.getOWLObject(), TranslateHelper.toSet(annotations));
        return ONTWrapperImpl.create(res, statement).append(annotations).append(f).append(s);
    }

    @Override
    Triple createSearchTriple(OWLInverseObjectPropertiesAxiom axiom) {
        Node subject = TranslateHelper.getSearchNode(axiom.getFirstProperty());
        if (subject == null) return null;
        Node object = TranslateHelper.getSearchNode(axiom.getSecondProperty());
        if (object == null) return null;
        return Triple.create(subject, OWL.inverseOf.asNode(), object);
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.axioms.InverseObjectPropertiesAxiomImpl
     */
    @ParametersAreNonnullByDefault
    public static abstract class AxiomImpl
            extends ONTAxiomImpl<OWLInverseObjectPropertiesAxiom>
            implements WithManyObjects<OWLObjectPropertyExpression>, OWLInverseObjectPropertiesAxiom {

        protected AxiomImpl(Triple t, Supplier<OntModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected AxiomImpl(Object s, String p, Object o, Supplier<OntModel> m) {
            super(s, p, o, m);
        }

        /**
         * Creates an {@link ONTObject} container, that is also {@link OWLInverseObjectPropertiesAxiom}.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @param factory   {@link ModelObjectFactory}, not {@code null}
         * @param config    {@link AxiomsSettings}, not {@code null}
         * @return {@link AxiomImpl}
         */
        public static AxiomImpl create(OntStatement statement,
                                       ModelObjectFactory factory,
                                       AxiomsSettings config) {
            return WithManyObjects.create(statement,
                    SimpleImpl.FACTORY, ComplexImpl.FACTORY, SET_HASH_CODE, factory, config);
        }

        public abstract ONTObject<? extends OWLObjectPropertyExpression> getFirstONTProperty();

        public abstract ONTObject<? extends OWLObjectPropertyExpression> getSecondONTProperty();

        @Override
        public OWLObjectPropertyExpression getFirstProperty() {
            return getFirstONTProperty().getOWLObject();
        }

        @Override
        public OWLObjectPropertyExpression getSecondProperty() {
            return getSecondONTProperty().getOWLObject();
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> findByURI(String uri, ModelObjectFactory factory) {
            return factory.getObjectProperty(uri);
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLObjectPropertyExpression>> listONTComponents(OntStatement statement,
                                                                                                    ModelObjectFactory factory) {
            return Iterators.of(factory.getProperty(statement.getSubject(OntObjectProperty.class)),
                    factory.getProperty(statement.getObject(OntObjectProperty.class)));
        }

        @Override
        public Stream<OWLObjectPropertyExpression> properties() {
            return sorted().map(ONTObject::getOWLObject);
        }

        @Override
        public Set<OWLObjectPropertyExpression> getPropertiesMinus(OWLObjectPropertyExpression property) {
            return getSetMinus(property);
        }

        @FactoryAccessor
        @Override
        public Collection<OWLInverseObjectPropertiesAxiom> asPairwiseAxioms() {
            return createSet(eraseModel());
        }

        @FactoryAccessor
        @Override
        public Collection<OWLInverseObjectPropertiesAxiom> splitToAnnotatedPairs() {
            return createSet(eraseModel());
        }

        @FactoryAccessor
        @Override
        public Collection<OWLSubObjectPropertyOfAxiom> asSubObjectPropertyOfAxioms() {
            OWLObjectPropertyExpression first = eraseModel(getFirstProperty());
            OWLObjectPropertyExpression second = eraseModel(getSecondProperty());
            Set<OWLSubObjectPropertyOfAxiom> res = new HashSet<>();
            DataFactory df = getDataFactory();
            res.add(df.getOWLSubObjectPropertyOfAxiom(first, second.getInverseProperty()));
            res.add(df.getOWLSubObjectPropertyOfAxiom(second, first.getInverseProperty()));
            return res;
        }

        @FactoryAccessor
        @Override
        protected OWLInverseObjectPropertiesAxiom createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return getDataFactory().getOWLInverseObjectPropertiesAxiom(eraseModel(getFirstProperty()),
                    eraseModel(getSecondProperty()), annotations);
        }

        @Override
        public boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        @Override
        public boolean canContainDatatypes() {
            return isAnnotated();
        }

        @Override
        public boolean canContainAnonymousIndividuals() {
            return isAnnotated();
        }

        @Override
        public boolean canContainNamedClasses() {
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
        public boolean canContainClassExpressions() {
            return false;
        }

        @Override
        public AxiomImpl merge(ONTObject<OWLInverseObjectPropertiesAxiom> other) {
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

        /**
         * Creates an instance of {@link AxiomImpl}
         * with additional triples getting from the specified {@code other} object.
         * The returned instance must be equivalent to this instance.
         *
         * @param other {@link ONTObject} with {@link OWLInverseObjectPropertiesAxiom}, not {@code null}
         * @return {@link AxiomImpl} - a fresh instance that equals to this
         */
        abstract AxiomImpl makeCopyWith(ONTObject<OWLInverseObjectPropertiesAxiom> other);

        /**
         * An {@link OWLInverseObjectPropertiesAxiom}
         * that has named object properties as subject and object and has no annotations.
         */
        protected static class SimpleImpl extends AxiomImpl implements Simple<OWLObjectPropertyExpression> {

            private static final BiFunction<Triple, Supplier<OntModel>, SimpleImpl> FACTORY = SimpleImpl::new;

            protected SimpleImpl(Triple t, Supplier<OntModel> m) {
                super(t, m);
            }

            protected SimpleImpl(Object s, String p, Object o, Supplier<OntModel> m) {
                super(s, p, o, m);
            }

            @Override
            public ONTObject<? extends OWLObjectPropertyExpression> getFirstONTProperty() {
                return findByURI((String) subject, getObjectFactory());
            }

            @Override
            public ONTObject<? extends OWLObjectPropertyExpression> getSecondONTProperty() {
                return findByURI((String) object, getObjectFactory());
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                // triple is checked above in trace
                return other instanceof SimpleImpl
                        && subject.equals(((AxiomImpl) other).getObjectURI())
                        && object.equals(((AxiomImpl) other).getSubjectURI());
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLObjectProperty> getObjectPropertySet() {
                return (Set<OWLObjectProperty>) getOWLComponentsAsSet();
            }

            @SuppressWarnings("unchecked")
            @Override
            public Set<OWLEntity> getSignatureSet() {
                return (Set<OWLEntity>) getOWLComponentsAsSet();
            }

            @Override
            public boolean containsObjectProperty(OWLObjectProperty property) {
                return hasURIResource(ONTEntityImpl.getURI(property));
            }

            @Override
            AxiomImpl makeCopyWith(ONTObject<OWLInverseObjectPropertiesAxiom> other) {
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
        }

        /**
         * An {@link OWLInverseObjectPropertiesAxiom}
         * that either has annotations or anonymous object expressions ({@link OWLObjectInverseOf})
         * in subject or object positions.
         * It has a public constructor since it is more generic then {@link SimpleImpl}.
         */
        protected static class ComplexImpl extends AxiomImpl implements Complex<ComplexImpl, OWLObjectPropertyExpression> {

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

            @SuppressWarnings("unchecked")
            @Override
            public ONTObject<? extends OWLObjectPropertyExpression> getFirstONTProperty() {
                if (subject instanceof String) {
                    return findByURI((String) subject, getObjectFactory());
                }
                Object[] content = getContent();
                return (ONTObject<? extends OWLObjectPropertyExpression>) content[0];
            }

            @SuppressWarnings("unchecked")
            @Override
            public ONTObject<? extends OWLObjectPropertyExpression> getSecondONTProperty() {
                if (object instanceof String) {
                    return findByURI((String) object, getObjectFactory());
                }
                Object[] content = getContent();
                return (ONTObject<? extends OWLObjectPropertyExpression>) content[content.length == 1 ? 0 : 1];
            }

            @Override
            protected boolean sameContent(ONTStatementImpl other) {
                return other instanceof ComplexImpl && Arrays.equals(getContent(), ((ComplexImpl) other).getContent());
            }

            @Override
            ComplexImpl makeCopyWith(ONTObject<OWLInverseObjectPropertiesAxiom> other) {
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
