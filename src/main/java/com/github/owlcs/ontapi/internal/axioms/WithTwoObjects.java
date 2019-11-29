/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
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

import com.github.owlcs.ontapi.internal.InternalConfig;
import com.github.owlcs.ontapi.internal.InternalObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.objects.ONTAnnotationImpl;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.WithContent;
import com.github.owlcs.ontapi.internal.objects.WithoutAnnotations;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.owlapi.OWLObjectImpl;
import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A technical interface that describes an axiom based on a single (main) triple with constant predicate.
 * Such an axiom has two operands ({@link S subject} and {@link O object})
 * and corresponds to the triple pattern {@code S predicate O},
 * where {@code predicate} is a fixed constant from OWL, RDF or RDFS vocabularies
 * (e.g. {@link org.apache.jena.vocabulary.RDFS#subClassOf}).
 * <p>
 * Note: for internal usage only, it is just to avoid copy-pasting.
 * <p>
 * Created by @ssz on 30.09.2019.
 *
 * @param <S> - any subtype of {@link OWLObject} (the type of triple's subject)
 * @param <O> - any subtype of {@link OWLObject} (the type of triple's object)
 * @since 2.0.0
 */
@SuppressWarnings("rawtypes")
interface WithTwoObjects<S extends OWLObject, O extends OWLObject> extends WithTriple {

    /**
     * Finds the {@link ONTObject} that matches the triple's subject-uri using the given factory.
     *
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @return an {@link ONTObject}
     * @throws ClassCastException in case the subject is not an URI resource
     */
    ONTObject<? extends S> getURISubject(InternalObjectFactory factory);

    /**
     * Finds the {@link ONTObject} that matches the triple's object-uri using the given factory.
     *
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @return an {@link ONTObject}
     * @throws ClassCastException in case the object is not an URI resource
     */
    ONTObject<? extends O> getURIObject(InternalObjectFactory factory);

    /**
     * Picks the {@link ONTObject} that matches the subject of the given {@code statement} using the {@code factory}.
     *
     * @param statement {@link OntStatement}, the source to parse, not {@code null}
     * @param factory   {@link InternalObjectFactory} to fetch an {@link ONTObject}, not {@code null}
     * @return an {@link ONTObject} that can be seen as wrapper of {@link S}
     */
    ONTObject<? extends S> subjectFromStatement(OntStatement statement, InternalObjectFactory factory);

    /**
     * Picks the {@link ONTObject} that matches the object of the given {@code statement} using the {@code factory}.
     *
     * @param statement {@link OntStatement}, the source to parse, not {@code null}
     * @param factory   {@link InternalObjectFactory} to fetch an {@link ONTObject}, not {@code null}
     * @return an {@link ONTObject} that can be seen as wrapper of {@link O}
     */
    ONTObject<? extends O> objectFromStatement(OntStatement statement, InternalObjectFactory factory);

    /**
     * Finds the {@link ONTObject} that matches the subject using the {@code factory}.
     *
     * @param factory {@link InternalObjectFactory} to fetch an {@link ONTObject}, not {@code null}
     * @return an {@link ONTObject} that can be seen as wrapper of {@link S}
     */
    ONTObject<? extends S> getONTSubject(InternalObjectFactory factory);

    /**
     * Finds the {@link ONTObject} that matches the object using the {@code factory}.
     *
     * @param factory {@link InternalObjectFactory} to fetch an {@link ONTObject}, not {@code null}
     * @return an {@link ONTObject} that can be seen as wrapper of {@link O}
     */
    ONTObject<? extends O> getONTObject(InternalObjectFactory factory);

    /**
     * Gets the subject from the base triple of this axiom.
     *
     * @return {@link ONTObject} with {@link S}
     */
    default ONTObject<? extends S> getONTSubject() {
        return getONTSubject(getObjectFactory());
    }

    /**
     * Gets the object from the base triple of this axiom.
     *
     * @return {@link ONTObject} with {@link O}
     */
    default ONTObject<? extends O> getONTObject() {
        return getONTObject(getObjectFactory());
    }

    default Set<? extends OWLObject> getOWLComponentsAsSet(InternalObjectFactory factory) {
        Set<OWLObject> res = OWLObjectImpl.createSortedSet();
        res.add(getONTSubject(factory).getOWLObject());
        res.add(getONTObject(factory).getOWLObject());
        return res;
    }

    /**
     * Creates an {@link ONTObject} container for the given {@link OntStatement};
     * the returned object is also {@link R}.
     * Impl notes:
     * If there is no sub-annotations and subject and object are URI-{@link org.apache.jena.rdf.model.Resource}s,
     * then a simplified instance of {@link Simple} is returned, for this the factory {@code simple} is used.
     * Otherwise the instance is created by the factory {@code complex} and has a cache inside
     * (its type must be {@link Complex}).
     * Note: this is an auxiliary method as shortcut to reduce copy-pasting, it is for internal usage only.
     *
     * @param statement {@link OntStatement}, the source to parse, not {@code null}
     * @param model     {@link OntGraphModel}-provider, not {@code null}
     * @param simple    factory (as {@link BiFunction}) to provide {@link Simple} instance, not {@code null}
     * @param complex   factory (as {@link BiFunction}) to provide {@link Complex} instance, not {@code null}
     * @param setHash   {@code ObjIntConsumer<OWLAxiom>}, facility to assign {@code hashCode}, not {@code null}
     * @param factory   {@link InternalObjectFactory} (singleton), not {@code null}
     * @param config    {@link InternalConfig} (singleton), not {@code null}
     * @param <R>       the desired {@link OWLAxiom axiom}-type
     * @return {@link R}
     */
    static <R extends ONTObject & WithTwoObjects> R create(OntStatement statement,
                                                           Supplier<OntGraphModel> model,
                                                           BiFunction<Triple, Supplier<OntGraphModel>, ? extends R> simple,
                                                           BiFunction<Triple, Supplier<OntGraphModel>, ? extends R> complex,
                                                           ObjIntConsumer<OWLAxiom> setHash,
                                                           InternalObjectFactory factory,
                                                           InternalConfig config) {
        R s = simple.apply(statement.asTriple(), model);
        Object[] content = Complex.initContent(s, statement, setHash, factory, config);
        if (content == null) {
            return s;
        }
        R c = complex.apply(statement.asTriple(), model);
        setHash.accept(c, s.hashCode());
        ((WithContent<?>) c).putContent(content);
        return c;
    }
    /**
     * Represents the simplest case when the axiom has no annotations
     * and the subject and object of its main triple are URI resources.
     *
     * @param <S> - any subtype of {@link OWLObject} (the type of triple's subject)
     * @param <O> - any subtype of {@link OWLObject} (the type of triple's object)
     */
    interface Simple<S extends OWLObject, O extends OWLObject> extends WithTwoObjects<S, O>, WithoutAnnotations {
        @Override
        default boolean isAnnotated() {
            return false;
        }

        @Override
        default Stream<ONTObject<? extends OWLObject>> objects() {
            InternalObjectFactory factory = getObjectFactory();
            return Stream.of(getONTSubject(factory), getONTObject(factory));
        }

        @Override
        default ONTObject<? extends S> getONTSubject(InternalObjectFactory factory) {
            return getURISubject(factory);
        }

        @Override
        default ONTObject<? extends O> getONTObject(InternalObjectFactory factory) {
            return getURIObject(factory);
        }
    }

    /**
     * For a class that implements this interface it is assumed
     * that the content contains only b-nodes and annotations,
     * named objects (which correspond to the main triple) are not cached.
     *
     * @param <A> - any subtype of {@link OWLAxiom} which is implemented by the instance of this interface
     * @param <S> - any subtype of {@link OWLObject} (the type of main triple's subject)
     * @param <O> - any subtype of {@link OWLObject} (the type of main triple's object)
     */
    interface Complex<A extends OWLAxiom, S extends OWLObject, O extends OWLObject>
            extends WithTwoObjects<S, O>, WithContent<A> {

        /**
         * Calculates the content and {@code hashCode} simultaneously.
         * Such a way was chosen for performance sake.
         *
         * @param axiom     - a {@link WithTwoObjects} instance, the axiom, not {@code null}
         * @param statement - a {@link OntStatement}, the source statement, not {@code null}
         * @param setHash   - a {@code ObjIntConsumer<OWLAxiom>}, facility to assign {@code hashCode}, not {@code null}
         * @param factory   - a {@link InternalObjectFactory} singleton, not {@code null}
         * @param config    - a {@link InternalConfig} singleton, not {@code null}
         * @return an {@code Array} with content or {@code null} if no content is needed
         */
        static Object[] initContent(WithTwoObjects axiom,
                                    OntStatement statement,
                                    ObjIntConsumer<OWLAxiom> setHash,
                                    InternalObjectFactory factory,
                                    InternalConfig config) {
            Collection annotations = ONTAxiomImpl.collectAnnotations(statement, factory, config);
            int size = annotations.size();
            Object subject = null;
            int hash = axiom.hashIndex();
            if (statement.getSubject().isURIResource()) {
                hash = OWLObject.hashIteration(hash, axiom.getURISubject(factory).hashCode());
            } else {
                size++;
                subject = axiom.subjectFromStatement(statement, factory);
                hash = OWLObject.hashIteration(hash, subject.hashCode());
            }
            Object object = null;
            if (statement.getObject().isURIResource()) {
                hash = OWLObject.hashIteration(hash, axiom.getURIObject(factory).hashCode());
            } else {
                size++;
                object = axiom.objectFromStatement(statement, factory);
                hash = OWLObject.hashIteration(hash, object.hashCode());
            }
            if (size == 0) {
                setHash.accept(axiom, OWLObject.hashIteration(hash, 1));
                return null;
            }
            int h = 1;
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
                h = WithContent.hashIteration(h, a.hashCode());
            }
            setHash.accept(axiom, OWLObject.hashIteration(hash, h));
            return res;
        }

        @Override
        default Object[] collectContent() {
            OntStatement statement = asStatement();
            InternalObjectFactory factory = getObjectFactory();
            List<ONTObject> res = new ArrayList<>(2);
            if (!statement.getSubject().isURIResource()) {
                res.add(subjectFromStatement(statement, factory));
            }
            if (!statement.getObject().isURIResource()) {
                res.add(objectFromStatement(statement, factory));
            }
            res.addAll(ONTAxiomImpl.collectAnnotations(statement, factory, getConfig()));
            return res.toArray();
        }

        @Override
        default ONTObject<? extends S> getONTSubject(InternalObjectFactory factory) {
            return findONTSubject(getContent(), factory);
        }

        @Override
        default ONTObject<? extends O> getONTObject(InternalObjectFactory factory) {
            return findONTObject(getContent(), factory);
        }

        @SuppressWarnings("unchecked")
        default ONTObject<? extends S> findONTSubject(Object[] content, InternalObjectFactory factory) {
            return hasURISubject() ? getURISubject(factory) : (ONTObject<? extends S>) content[0];
        }

        @SuppressWarnings("unchecked")
        default ONTObject<? extends O> findONTObject(Object[] content, InternalObjectFactory factory) {
            return hasURIObject() ? getURIObject(factory)
                    : (ONTObject<? extends O>) content[hasURISubject() ? 0 : 1];
        }

        /**
         * Returns {@code 0}, {@code 1} or {@code 2} depending on base triple:
         * if the subject and object are URI resources then the {@code 0} is returned and
         * if the subject and object are blank-nodes then the {@code 2} is returned.
         *
         * @return int
         */
        default int getAnnotationStartIndex() {
            return hasURISubject() ? hasURIObject() ? 0 : 1 : hasURIObject() ? 1 : 2;
        }

        @Override
        @SuppressWarnings("unchecked")
        default Stream<ONTObject<? extends OWLObject>> objects() {
            Object[] content = getContent();
            Stream objects = null;
            Stream res = Arrays.stream(content);
            if (hasURISubject()) {
                InternalObjectFactory factory = getObjectFactory();
                objects = hasURIObject() ?
                        Stream.of(getURISubject(factory), getURIObject(factory)) :
                        Stream.of(getURISubject(factory));
            } else if (hasURIObject()) {
                objects = Stream.of(getURIObject(getObjectFactory()));
            }
            res = objects != null ? Stream.concat(objects, res) : res;
            return (Stream<ONTObject<? extends OWLObject>>) res;
        }

        @Override
        default Set<? extends OWLObject> getOWLComponentsAsSet(InternalObjectFactory factory) {
            Object[] content = getContent();
            Set<OWLObject> res = OWLObjectImpl.createSortedSet();
            res.add(findONTSubject(content, factory).getOWLObject());
            res.add(findONTObject(content, factory).getOWLObject());
            return res;
        }

        @Override
        default boolean isAnnotated() {
            return ONTAnnotationImpl.hasAnnotations(getContent());
        }

        @Override
        default Stream<OWLAnnotation> annotations() {
            return ONTAnnotationImpl.contentAsStream(getContent(), getAnnotationStartIndex());
        }

        @Override
        default List<OWLAnnotation> annotationsAsList() {
            return ONTAnnotationImpl.contentAsList(getContent(), getAnnotationStartIndex());
        }
    }

    /**
     * For axioms with main triple that has the subject and object of the same type.
     *
     * @param <X> - any subtype of {@link OWLObject} (the type of triple's subject)
     */
    interface Unary<X extends OWLObject> extends WithTwoObjects<X, X> {

        ONTObject<? extends X> findByURI(String uri, InternalObjectFactory factory);

        @Override
        default ONTObject<? extends X> getURISubject(InternalObjectFactory factory) {
            return findByURI(getSubjectURI(), factory);
        }

        @Override
        default ONTObject<? extends X> getURIObject(InternalObjectFactory factory) {
            return findByURI(getObjectURI(), factory);
        }
    }

    interface UnarySimple<X extends OWLObject> extends Unary<X>, Simple<X, X> {
    }

    interface UnaryWithContent<A extends OWLAxiom, X extends OWLObject> extends Unary<X>, Complex<A, X, X> {
    }
}
