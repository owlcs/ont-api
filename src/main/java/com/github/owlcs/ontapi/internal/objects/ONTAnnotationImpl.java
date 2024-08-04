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

package com.github.owlcs.ontapi.internal.objects;

import com.github.owlcs.ontapi.BlankNodeId;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.internal.ReadHelper;
import com.github.owlcs.ontapi.internal.WithMerge;
import com.github.owlcs.ontapi.owlapi.objects.AnnotationImpl;
import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.ontapi.model.OntAnnotation;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntStatement;
import org.apache.jena.ontapi.utils.OntModels;
import org.apache.jena.ontapi.utils.StdModels;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An {@link OWLAnnotation} implementation that is also an instance of {@link ONTObject}.
 * <p>
 * Created by @ssz on 17.08.2019.
 *
 * @see ReadHelper#getAnnotation(OntStatement, ONTObjectFactory)
 * @see AnnotationImpl
 * @since 2.0.0
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTAnnotationImpl extends ONTStatementImpl
        implements OWLAnnotation, ModelObject<OWLAnnotation>, WithMerge<ONTObject<OWLAnnotation>> {

    protected ONTAnnotationImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
        super(subject, predicate, object, m);
    }

    /**
     * Wraps the given annotation ({@link OntStatement}) as {@link OWLAnnotation} and {@link ONTObject}.
     * <p>
     * Impl notes:
     * If the annotation does not contain sub-annotations,
     * then a simplified instance of {@link SimpleImpl} is returned.
     * Otherwise, the instance is {@link WithAnnotationsImpl} with a cache inside.
     *
     * @param statement {@link OntStatement}, must be annotation (i.e. {@link OntStatement#isAnnotationAssertion()}
     *                  must be {@code true}), not {@code null}
     * @param factory   {@link ModelObjectFactory}, not {@code null}
     * @param model     a provider of non-null {@link OntModel}, not {@code null}
     * @return {@link ONTAnnotationImpl}
     */
    public static ONTAnnotationImpl create(OntStatement statement,
                                           ModelObjectFactory factory,
                                           Supplier<OntModel> model) {
        Collection<?> annotations = collectAnnotations(statement, factory);
        if (annotations.isEmpty()) {
            SimpleImpl res = new SimpleImpl(statement.asTriple(), model);
            res.hashCode = collectHashCode(res, factory, 1);
        }
        Object[] content = new Object[annotations.size()];
        WithAnnotationsImpl res = new WithAnnotationsImpl(statement.asTriple(), model);
        int hash = 1, index = 0;
        for (Object a : annotations) {
            content[index++] = a;
            hash = WithContent.hashIteration(hash, a.hashCode());
        }
        res.hashCode = collectHashCode(res, factory, hash);
        res.putContent(content);
        return res;
    }

    /**
     * Collects the has-code of {@code res} according to OWL-API specification.
     *
     * @param res             {@link ONTAnnotationImpl}, not {@code null}
     * @param factory         {@link ModelObjectFactory}, not {@code null}
     * @param contentHashCode int, a {@code hashCode} of sub-annotations
     * @return int
     */
    private static int collectHashCode(ONTAnnotationImpl res,
                                       ModelObjectFactory factory,
                                       int contentHashCode) {
        int hash = OWLObject.hashIteration(res.hashIndex(), res.findONTAnnotationProperty(factory).hashCode());
        hash = OWLObject.hashIteration(hash, res.findONTAnnotationValue(factory).hashCode());
        return OWLObject.hashIteration(hash, contentHashCode);
    }

    /**
     * Collects all annotations for the given root {@link OntStatement},
     * that expected to be an annotation assertion (i.e. {@link OntStatement#isAnnotationAssertion()} must return {@code true}).
     *
     * @param root    {@link OntStatement} the root annotation statement or plain annotation assertion, not {@code null}
     * @param factory {@link ONTObjectFactory} to retrieve {@link ONTObject}s, not {@code null}
     * @return a sorted nonnull distinct {@code Collection} {@code Collection}
     * of {@link ONTObject}s with {@link OWLAnnotation}s (can be empty if no annotations)
     * @see ONTAxiomImpl#collectAnnotations(OntStatement, ONTObjectFactory, com.github.owlcs.ontapi.config.AxiomsSettings)
     */
    protected static Collection<ONTObject<OWLAnnotation>> collectAnnotations(OntStatement root,
                                                                             ONTObjectFactory factory) {
        Map<OWLAnnotation, ONTObject<OWLAnnotation>> res = new TreeMap<>();
        OntModels.listAnnotations(root).mapWith(factory::getAnnotation).forEachRemaining(x -> WithMerge.add(res, x));
        return res.values();
    }

    /**
     * Answers {@code true} if the given pair is a part of the triple {@code x owl:deprecated "true"^^xsd:boolean}.
     *
     * @param predicate predicate from SPO, not {@code null}
     * @param value     object from SPO, not {@code null}
     * @return boolean
     */
    public static boolean isDeprecated(String predicate, Object value) {
        return OWL.deprecated.getURI().equals(predicate) && StdModels.TRUE.asNode().getLiteral().equals(value);
    }

    /**
     * Extracts the {@link OWLAnnotationSubject} from the given statement, returning an {@link ONTObject}-wrapper.
     * Note: despite the public modifier, this method is for internal usage only.
     * The specified statement must be an annotation assertion.
     *
     * @param statement {@link ONTStatementImpl} - must be an annotation assertion, not {@code null}
     * @param factory   {@link ModelObjectFactory}, not {@code null}
     * @return {@link ONTObject}
     */
    public static ONTObject<? extends OWLAnnotationSubject> findONTSubject(ONTStatementImpl statement,
                                                                           ModelObjectFactory factory) {
        if (statement.subject instanceof String) {
            return factory.getIRI((String) statement.subject);
        }
        if (statement.subject instanceof BlankNodeId) {
            return factory.getAnonymousIndividual((BlankNodeId) statement.subject);
        }
        throw new OntApiException.IllegalState("Wrong subject: " + statement.subject);
    }

    /**
     * Extracts the {@link OWLAnnotationProperty} from the given statement
     * returning an {@link ONTObject}-wrapper for it.
     * Note: despite the public modifier, this method is for internal usage only.
     * The specified statement must be an annotation assertion.
     *
     * @param statement {@link ONTStatementImpl} - must be an annotation assertion, not {@code null}
     * @param factory   {@link ModelObjectFactory}, not {@code null}
     * @return {@link ONTObject}
     */
    public static ONTObject<OWLAnnotationProperty> findONTPredicate(ONTStatementImpl statement,
                                                                    ModelObjectFactory factory) {
        return factory.getAnnotationProperty(statement.predicate);
    }

    /**
     * Extracts the {@link OWLAnnotationValue} from the given statement, returning an {@link ONTObject}-wrapper.
     * Note: despite the public modifier, this method is for internal usage only.
     * The specified statement must be an annotation assertion.
     *
     * @param statement {@link ONTStatementImpl} - must be an annotation assertion, not {@code null}
     * @param factory   {@link ModelObjectFactory}, not {@code null}
     * @return {@link ONTObject}
     */
    public static ONTObject<? extends OWLAnnotationValue> findONTObject(ONTStatementImpl statement,
                                                                        ModelObjectFactory factory) {
        if (statement.object instanceof BlankNodeId) {
            return factory.getAnonymousIndividual((BlankNodeId) statement.object);
        }
        if (statement.object instanceof LiteralLabel) {
            return factory.getLiteral((LiteralLabel) statement.object);
        }
        if (statement.object instanceof String) {
            return factory.getIRI((String) statement.object);
        }
        throw new OntApiException.IllegalState("Wrong object: " + statement.object);
    }

    /**
     * Creates a {@code Stream} for the object's content in unsafe manner.
     * Note: despite the public modifier, this method is for internal usage only.
     * The content must contain only annotations.
     *
     * @param content an {@code Array} with {@link OWLAnnotation}s
     * @return a {@code Stream} of {@link OWLAnnotation}s
     */
    @SuppressWarnings("unchecked")
    public static Stream<OWLAnnotation> contentAsStream(Object[] content) {
        Stream<?> res = Arrays.stream(content);
        return (Stream<OWLAnnotation>) res;
    }

    /**
     * Creates a {@code List} for the object's content in unsafe manner.
     * Note: despite the public modifier, this method is for internal usage only.
     * The content must contain only annotations.
     *
     * @param content an {@code Array} with {@link OWLAnnotation}s
     * @return a {@code List} of {@link OWLAnnotation}s
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<OWLAnnotation> contentAsList(Object[] content) {
        List res = Arrays.asList(content);
        return (List<OWLAnnotation>) Collections.unmodifiableList(res);
    }

    /**
     * Creates a {@code Stream} for the object's content in unsafe manner.
     * Note: despite the public modifier, this method is for internal usage only.
     * The content must contain only annotations beginning with the specified index.
     *
     * @param content   an {@code Array} with {@link OWLAnnotation}s
     * @param fromIndex a positive int less than array length - annotation's stating position
     * @return a {@code Stream} of {@link OWLAnnotation}s
     */
    @SuppressWarnings("unchecked")
    public static Stream<OWLAnnotation> contentAsStream(Object[] content, int fromIndex) {
        if (!ONTAnnotationImpl.hasAnnotations(content)) {
            return Stream.empty();
        }
        Stream<?> res = Arrays.stream(content, fromIndex, content.length);
        return (Stream<OWLAnnotation>) res;
    }

    /**
     * Creates a {@code List} for the object's content in unsafe manner.
     * Note: despite the public modifier, this method is for internal usage only.
     * The content must contain only annotations beginning with the specified index.
     *
     * @param content   an {@code Array} with {@link OWLAnnotation}s
     * @param fromIndex a positive int less than array length - annotation's stating position
     * @return a {@code List} of {@link OWLAnnotation}s
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<OWLAnnotation> contentAsList(Object[] content, int fromIndex) {
        if (!ONTAnnotationImpl.hasAnnotations(content)) {
            return Collections.emptyList();
        }
        List res = Arrays.asList(Arrays.copyOfRange(content, fromIndex, content.length));
        return (List<OWLAnnotation>) Collections.unmodifiableList(res);
    }

    /**
     * Answers {@code true} if the given array contains {@link OWLAnnotation} at its end.
     *
     * @param content an {@code Array}, not {@code null}
     * @return boolean
     */
    public static boolean hasAnnotations(Object[] content) {
        return content[content.length - 1] instanceof OWLAnnotation;
    }

    @Override
    public OWLAnnotation getOWLObject() {
        return this;
    }

    @Override
    public Stream<Triple> triples() {
        OntStatement root = asStatement();
        Stream<Triple> res = Stream.concat(Stream.of(root.asTriple()), objects().flatMap(ONTObject::triples));
        OntAnnotation a = root.getSubject().getAs(OntAnnotation.class);
        if (a != null) {
            res = Stream.concat(res, a.spec().map(FrontsTriple::asTriple));
        }
        return res;
    }

    @Override
    public Stream<ONTObject<? extends OWLObject>> objects() {
        return Stream.of(getONTAnnotationProperty(), getONTAnnotationValue());
    }

    @Override
    public boolean isDeprecatedIRIAnnotation() {
        return isDeprecated(predicate, object);
    }

    @Override
    public OWLAnnotationProperty getProperty() {
        return getONTAnnotationProperty().getOWLObject();
    }

    @Override
    public OWLAnnotationValue getValue() {
        return getONTAnnotationValue().getOWLObject();
    }

    @FactoryAccessor
    @Override
    public OWLAnnotation getAnnotatedAnnotation(Stream<OWLAnnotation> annotations) {
        return createAnnotation(appendAnnotations(annotations.iterator()));
    }

    @FactoryAccessor
    @Override
    public OWLAnnotation getAnnotatedAnnotation(Collection<OWLAnnotation> annotations) {
        return createAnnotation(appendAnnotations(annotations.iterator()));
    }

    @FactoryAccessor
    protected OWLAnnotation createAnnotation(Collection<OWLAnnotation> annotations) {
        return getDataFactory().getOWLAnnotation(eraseModel(getProperty()), eraseModel(getValue()), annotations);
    }

    /**
     * Gets the property that this annotation acts along.
     *
     * @return {@link ONTObject} of {@link OWLAnnotationProperty}
     */
    public ONTObject<OWLAnnotationProperty> getONTAnnotationProperty() {
        return findONTAnnotationProperty(getObjectFactory());
    }

    /**
     * Gets the annotation value, that can be either {@link OWLAnonymousIndividual}, {@link IRI} or {@link OWLLiteral}.
     *
     * @return {@link ONTObject} of {@link OWLAnnotationValue}
     */
    public ONTObject<? extends OWLAnnotationValue> getONTAnnotationValue() {
        return findONTAnnotationValue(getObjectFactory());
    }

    protected ONTObject<OWLAnnotationProperty> findONTAnnotationProperty(ModelObjectFactory factory) {
        return findONTPredicate(this, factory);
    }

    protected ONTObject<? extends OWLAnnotationValue> findONTAnnotationValue(ModelObjectFactory factory) {
        return findONTObject(this, factory);
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
    public boolean canContainObjectProperties() {
        return false;
    }

    @Override
    public boolean canContainClassExpressions() {
        return false;
    }

    @Override
    public ONTAnnotationImpl merge(ONTObject<OWLAnnotation> other) {
        if (this == other) {
            return this;
        }
        if (other instanceof ONTAnnotationImpl && sameTriple((ONTAnnotationImpl) other)) {
            return this;
        }
        ONTAnnotationImpl res = makeCopyWith(other);
        res.hashCode = hashCode;
        return res;
    }

    /**
     * Creates an instance of {@link ONTAnnotationImpl}
     * with additional triples getting from the specified {@code other} object.
     * The returned instance must be equivalent to this one.
     *
     * @param other {@link ONTObject} with {@link OWLAnnotation}, not {@code null}
     * @return {@link ONTAnnotationImpl}
     */
    protected abstract ONTAnnotationImpl makeCopyWith(ONTObject<OWLAnnotation> other);

    /**
     * An {@link OWLAnnotation} that has no sub-annotations.
     */
    public static class SimpleImpl extends ONTAnnotationImpl implements WithoutAnnotations {

        protected SimpleImpl(Triple t, Supplier<OntModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected SimpleImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        @Override
        protected boolean sameContent(ONTStatementImpl other) {
            return !other.isAnnotated() && predicate.equals(other.predicate) && object.equals(other.object);
        }

        @Override
        protected boolean sameAs(ONTStatementImpl other) {
            if (notSame(other)) {
                return false;
            }
            return sameContent(other);
        }

        @Override
        protected SimpleImpl makeCopyWith(ONTObject<OWLAnnotation> other) {
            return new SimpleImpl(subject, predicate, object, model) {

                @Override
                public Stream<Triple> triples() {
                    return Stream.concat(SimpleImpl.this.triples(), other.triples());
                }
            };
        }

        @Override
        public boolean containsDatatype(OWLDatatype datatype) {
            return object instanceof LiteralLabel
                    && getONTAnnotationValue().getOWLObject().containsEntityInSignature(datatype);
        }

        @Override
        public boolean containsAnnotationProperty(OWLAnnotationProperty property) {
            return getONTAnnotationProperty().getOWLObject().equals(property);
        }

        @Override
        public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
            return object instanceof BlankNodeId ? createSet(retrieveAnonymousIndividual()) : createSet();
        }

        @Override
        public Set<OWLAnnotationProperty> getAnnotationPropertySet() {
            return createSet(getProperty());
        }

        @Override
        public Set<OWLDatatype> getDatatypeSet() {
            return object instanceof LiteralLabel ? createSet(retrieveDatatype()) : createSet();
        }

        private OWLDatatype retrieveDatatype() {
            return getValue().asLiteral().orElseThrow(OntApiException.IllegalState::new).getDatatype();
        }

        private OWLAnonymousIndividual retrieveAnonymousIndividual() {
            return getValue().asAnonymousIndividual().orElseThrow(OntApiException.IllegalState::new);
        }

        @Override
        public Set<OWLEntity> getSignatureSet() {
            Set<OWLEntity> res = createSortedSet();
            res.add(getProperty());
            if (object instanceof LiteralLabel) {
                res.add(retrieveDatatype());
            }
            return res;
        }

        @Override
        public OWLAnnotation eraseModel() {
            return getDataFactory().getOWLAnnotation(eraseModel(getProperty()), eraseModel(getValue()));
        }
    }

    /**
     * An {@link OWLAnnotation} that has sub-annotations.
     * It has a public constructor since it is more generic then {@link SimpleImpl}.
     */
    public static class WithAnnotationsImpl extends ONTAnnotationImpl implements WithContent<WithAnnotationsImpl> {
        protected final InternalCache.Loading<WithAnnotationsImpl, Object[]> content;

        public WithAnnotationsImpl(Triple t, Supplier<OntModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected WithAnnotationsImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
            this.content = createContentCache();
        }

        @Override
        protected boolean sameContent(ONTStatementImpl other) {
            return other instanceof WithAnnotationsImpl
                    && predicate.equals(other.predicate) && object.equals(other.object)
                    && Arrays.equals(getContent(), ((WithAnnotationsImpl) other).getContent());
        }

        @SuppressWarnings("unchecked")
        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            Stream<?> res = Stream.concat(super.objects(), annotations());
            return (Stream<ONTObject<? extends OWLObject>>) res;
        }

        @Override
        public Object[] collectContent() {
            return collectAnnotations(asStatement(), getObjectFactory()).toArray();
        }

        @Override
        public InternalCache.Loading<WithAnnotationsImpl, Object[]> getContentCache() {
            return content;
        }

        @Override
        public boolean isAnnotated() {
            return true;
        }

        @Override
        public Stream<OWLAnnotation> annotations() {
            return contentAsStream(getContent());
        }

        @Override
        public List<OWLAnnotation> annotationsAsList() {
            return contentAsList(getContent());
        }

        @Override
        public OWLAnnotation eraseModel() {
            return getDataFactory().getOWLAnnotation(eraseModel(getProperty()), eraseModel(getValue()),
                    factoryAnnotations());
        }

        @Override
        protected WithAnnotationsImpl makeCopyWith(ONTObject<OWLAnnotation> other) {
            WithAnnotationsImpl res = new WithAnnotationsImpl(subject, predicate, object, model) {

                @Override
                public Stream<Triple> triples() {
                    return Stream.concat(WithAnnotationsImpl.this.triples(), other.triples());
                }
            };
            if (hasContent()) {
                res.putContent(getContent());
            }
            return res;
        }
    }
}
