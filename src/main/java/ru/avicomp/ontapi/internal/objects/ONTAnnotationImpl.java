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
import org.apache.jena.graph.impl.LiteralLabel;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ModelObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.internal.ReadHelper;
import ru.avicomp.ontapi.jena.model.OntAnnotation;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.utils.OntModels;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import javax.annotation.Nonnull;
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
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public class ONTAnnotationImpl extends ONTStatementImpl implements OWLAnnotation, ONTObject<OWLAnnotation> {

    protected ONTAnnotationImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
        super(subject, predicate, object, m);
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
        res.content.put(res, res.collectContent(annotation, res.getObjectFactory()));
        return res;
    }

    @Override
    public OWLAnnotation getOWLObject() {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<OWLAnnotation> annotations() {
        List res = Arrays.asList(getContent());
        return (Stream<OWLAnnotation>) res.stream().skip(2);
    }

    @Override
    public List<OWLAnnotation> annotationsAsList() {
        return annotations().collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<ONTObject<? extends OWLObject>> objects() {
        List res = Arrays.asList(getContent());
        return (Stream<ONTObject<? extends OWLObject>>) res.stream();
    }

    @Override
    protected final Object[] collectContent() {
        return collectContent(asStatement(), getObjectFactory());
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
    @Override
    public boolean isAnnotated() {
        return getContent().length > 2;
    }

    @Override
    public boolean isDeprecatedIRIAnnotation() {
        return isDeprecated(predicate, object);
    }

    public static boolean isDeprecated(String predicate, Object value) {
        return OWL.deprecated.getURI().equals(predicate) && Models.TRUE.asNode().getLiteral().equals(value);
    }

    @Override
    public OWLAnnotation getAnnotatedAnnotation(@Nonnull Stream<OWLAnnotation> annotations) {
        return getAnnotatedAnnotation(annotations.collect(Collectors.toList()));
    }

    @Override
    public OWLAnnotation getAnnotatedAnnotation(@Nonnull Collection<OWLAnnotation> annotations) {
        return getDataFactory().getOWLAnnotation(getProperty(), getValue(), annotations);
    }

    /**
     * Gets the property that this annotation acts along.
     *
     * @return {@link ONTObject} of {@link OWLAnnotationProperty}
     */
    @SuppressWarnings("unchecked")
    public ONTObject<OWLAnnotationProperty> getONTAnnotationProperty() {
        return (ONTObject<OWLAnnotationProperty>) getContent()[0];
    }

    /**
     * Gets the annotation value, that can be either {@link OWLAnonymousIndividual}, {@link IRI} or {@link OWLLiteral}.
     *
     * @return {@link ONTObject} of {@link OWLAnnotationValue}
     */
    @SuppressWarnings("unchecked")
    public ONTObject<? extends OWLAnnotationValue> getONTAnnotationValue() {
        return (ONTObject<? extends OWLAnnotationValue>) getContent()[1];
    }

    /**
     * Collects the cache.
     *
     * @param root {@link OntStatement}, not {@code null}
     * @param of   {@link InternalObjectFactory}, not {@code null}
     * @return {@code Array} of {@code Object}s
     */
    @SuppressWarnings("unchecked")
    protected Object[] collectContent(OntStatement root, InternalObjectFactory of) {
        Set<ONTObject<OWLAnnotation>> sub = null;
        if (root.getSubject().getAs(OntAnnotation.class) != null || root.hasAnnotations()) {
            // OWL-API requires distinct and sorted Stream's and _List's_
            sub = createSortedSet(Comparator.comparing(ONTObject::getOWLObject));
            OntModels.listAnnotations(root).mapWith(of::getAnnotation).forEachRemaining(sub::add);
        }
        List res = new ArrayList(sub == null ? 2 : sub.size() + 2);
        res.add(collectONTAnnotationProperty(of));
        res.add(collectONTAnnotationValue(of));
        if (sub != null) {
            res.addAll(sub);
        }
        return res.toArray();
    }

    private ONTObject<OWLAnnotationProperty> collectONTAnnotationProperty(InternalObjectFactory of) {
        if (of instanceof ModelObjectFactory) {
            return ((ModelObjectFactory) of).getAnnotationProperty(predicate);
        }
        return getObjectFactory().getProperty(model.get().getAnnotationProperty(predicate));
    }

    private ONTObject<? extends OWLAnnotationValue> collectONTAnnotationValue(InternalObjectFactory of) {
        if (!(of instanceof ModelObjectFactory)) {
            return getObjectFactory().getValue(model.get().asRDFNode(getObjectNode()));
        }
        ModelObjectFactory f = (ModelObjectFactory) of;
        if (object instanceof BlankNodeId) {
            return f.getAnonymousIndividual((BlankNodeId) object);
        }
        if (object instanceof LiteralLabel) {
            return f.getLiteral((LiteralLabel) object);
        }
        if (object instanceof String) {
            return f.getIRI((String) object);
        }
        throw new OntApiException.IllegalState("Wrong object: " + object);
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
}
