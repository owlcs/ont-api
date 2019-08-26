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

package ru.avicomp.ontapi.internal.objects;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.jena.model.OntAnnotation;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.utils.OntModels;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An {@link OWLAnnotation} implementation that is also an instance of {@link ONTObject}.
 * Created by @ssz on 17.08.2019.
 *
 * @see ReadHelper#getAnnotation(OntStatement, InternalObjectFactory)
 * @see ru.avicomp.ontapi.owlapi.objects.OWLAnnotationImpl
 * @see ONTExpressionImpl
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public class ONTAnnotationImpl extends ONTStatementImpl implements OWLAnnotation, ONTObject<OWLAnnotation> {
    private static final ONTAnnotationImpl[] NO_SUB_ANNOTATIONS = new ONTAnnotationImpl[]{};

    protected final InternalCache.Loading<ONTAnnotationImpl, Object[]> content;

    protected ONTAnnotationImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
        super(subject, predicate, object, m);
        this.content = InternalCache.createSoftSingleton(x -> collectContent());
    }

    /**
     * Wraps the given annotation ({@link OntStatement}) as {@link OWLAnnotation} and {@link ONTObject}.
     *
     * @param annotation {@link OntStatement}, must be annotation (i.e. {@link OntStatement#isAnnotation()}
     *                   must be {@code true}), not {@code null}
     * @param model      a provider of non-null {@link OntGraphModel}, not {@code null}
     * @return {@link ONTAnnotationImpl}
     */
    public static ONTAnnotationImpl create(OntStatement annotation, Supplier<OntGraphModel> model) {
        ONTAnnotationImpl res = new ONTAnnotationImpl(fromNode(annotation.getSubject()),
                annotation.getPredicate().getURI(), fromNode(annotation.getObject()), model);
        if (annotation.getSubject().getAs(OntAnnotation.class) != null || annotation.hasAnnotations()) {
            res.content.put(res, res.collectContent(annotation, res.getObjectFactory()));
        } else {
            res.content.put(res, NO_SUB_ANNOTATIONS);
        }
        return res;
    }

    /**
     * Collects the cache.
     *
     * @param root {@link OntStatement}, not {@code null}
     * @param of   {@link InternalObjectFactory}, not {@code null}
     * @return {@code Array} of {@code Object}s
     */
    protected Object[] collectContent(OntStatement root, InternalObjectFactory of) {
        // OWL-API requires distinct and sorted Stream's and _List's_
        Set<ONTObject<OWLAnnotation>> res = createSortedSet(Comparator.comparing(ONTObject::getOWLObject));
        OntModels.listAnnotations(root).mapWith(of::getAnnotation).forEachRemaining(res::add);
        return res.toArray();
    }

    /**
     * Collects the cache.
     *
     * @return {@code Array} of {@code Object}s
     * @see #collectContent(OntStatement, InternalObjectFactory)
     * @see ONTExpressionImpl#collectContent()
     */
    protected final Object[] collectContent() {
        return collectContent(asStatement(), getObjectFactory());
    }

    /**
     * Gets the content from cache.
     *
     * @return {@code Array} of {@code Object}s
     * @see ONTExpressionImpl#getContent()
     */
    protected Object[] getContent() {
        return content.get(this);
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
    public Stream<OWLAnnotation> annotations() {
        return annotationsAsList().stream();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<OWLAnnotation> annotationsAsList() {
        List res = Arrays.asList(getContent());
        return (List<OWLAnnotation>) res;
    }

    /**
     * Lists all components in the form of {@code Stream}.
     * Neither this object or component objects are not included in result: it content only top-level direct components.
     *
     * @return {@code Stream} of {@link ONTObject}s
     * @see ONTAnnotationImpl#listComponents()
     * @see ONTExpressionImpl#objects()
     */
    public final Stream<ONTObject<? extends OWLObject>> objects() {
        return Iter.asStream(listComponents(), Spliterator.NONNULL | Spliterator.DISTINCT | Spliterator.ORDERED);
    }

    /**
     * Lists all components in the form of {@code Iterator}.
     * Neither this object or component objects are not included in result: it content only top-level direct components.
     *
     * @return {@link ExtendedIterator} of {@link ONTObject}s
     * @see HasComponents#components()
     * @see HasOperands#operands()
     * @see ONTExpressionImpl#listComponents()
     */
    public ExtendedIterator<ONTObject<? extends OWLObject>> listComponents() {
        InternalObjectFactory f = getObjectFactory();
        return Iter.concat(Iter.of(getONTAnnotationProperty(f),
                getONTAnnotationValue(f)), Iter.create(() -> getONTAnnotations().iterator()));
    }

    /**
     * Answers a sorted distinct {@code List} of sub-annotations.
     *
     * @return a {@code List} of {@link ONTAnnotationImpl}
     */
    @SuppressWarnings("unchecked")
    public List<ONTAnnotationImpl> getONTAnnotations() {
        List res = Arrays.asList(getContent());
        return (List<ONTAnnotationImpl>) res;
    }

    /**
     * Gets the property that this annotation acts along.
     *
     * @return {@link ONTObject} of {@link OWLAnnotationProperty}
     */
    public ONTObject<OWLAnnotationProperty> getONTAnnotationProperty() {
        return getONTAnnotationProperty(getObjectFactory());
    }

    /**
     * Gets the annotation value, that can be either {@link OWLAnonymousIndividual}, {@link IRI} or {@link OWLLiteral}.
     *
     * @return {@link ONTObject} of {@link OWLAnnotationValue}
     */
    public ONTObject<? extends OWLAnnotationValue> getONTAnnotationValue() {
        return getONTAnnotationValue(getObjectFactory());
    }

    protected ONTObject<OWLAnnotationProperty> getONTAnnotationProperty(InternalObjectFactory of) {
        if (of instanceof ModelObjectFactory) {
            return ((ModelObjectFactory) of).getAnnotationProperty(predicate);
        }
        return getObjectFactory().getProperty(model.get().getAnnotationProperty(predicate));
    }

    protected ONTObject<? extends OWLAnnotationValue> getONTAnnotationValue(InternalObjectFactory of) {
        if (!(of instanceof ModelObjectFactory)) {
            return getObjectFactory().getValue(model.get().asRDFNode(getObjectNode()));
        }
        ModelObjectFactory f = (ModelObjectFactory) of;
        if (object instanceof BlankNodeId)
            return f.getAnonymousIndividual((BlankNodeId) object);
        if (object instanceof LiteralLabel) {
            return f.getLiteral((LiteralLabel) object);
        }
        if (object instanceof String) {
            return f.getIRI((String) object);
        }
        throw new OntApiException.IllegalState("Wrong object: " + object);
    }

    @Override
    public OWLAnnotationProperty getProperty() {
        return getONTAnnotationProperty().getOWLObject();
    }

    @Override
    public OWLAnnotationValue getValue() {
        return getONTAnnotationValue().getOWLObject();
    }

    /**
     * Answers {@code true} if this annotation has sub-annotations.
     *
     * @return boolean
     * @see OWLAxiom#isAnnotated()
     */
    @SuppressWarnings("unused")
    public boolean hasAnnotations() {
        return NO_SUB_ANNOTATIONS != getContent();
    }

    @Override
    public boolean isDeprecatedIRIAnnotation() {
        return OWL.deprecated.getURI().equals(predicate) && Models.TRUE.asNode().getLiteral().equals(object);
    }

    @Override
    public OWLAnnotation getAnnotatedAnnotation(@Nonnull Collection<OWLAnnotation> annotations) {
        InternalObjectFactory f = getObjectFactory();
        return f.getOWLDataFactory().getOWLAnnotation(getONTAnnotationProperty(f).getOWLObject(),
                getONTAnnotationValue(f).getOWLObject(), annotations);
    }

    @Override
    public OWLAnnotation getAnnotatedAnnotation(@Nonnull Stream<OWLAnnotation> annotations) {
        return getAnnotatedAnnotation(annotations.collect(Collectors.toList()));
    }

    @Override
    public boolean containsEntityInSignature(@Nullable OWLEntity entity) {
        if (entity == null) return false;
        return (entity.isOWLAnnotationProperty() || entity.isOWLDatatype()) && super.containsEntityInSignature(entity);
    }

    @Override
    protected Set<OWLClass> getNamedClassSet() {
        return createSet();
    }

    @Override
    protected Set<OWLNamedIndividual> getNamedIndividualSet() {
        return createSet();
    }

    @Override
    protected Set<OWLDataProperty> getDataPropertySet() {
        return createSet();
    }

    @Override
    protected Set<OWLObjectProperty> getObjectPropertySet() {
        return createSet();
    }

    @Override
    protected Set<OWLClassExpression> getClassExpressionSet() {
        return createSet();
    }
}
