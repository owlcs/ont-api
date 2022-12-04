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
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.objects.ONTAnnotationImpl;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTEntityImpl;
import com.github.owlcs.ontapi.internal.objects.ONTObjectImpl;
import com.github.owlcs.ontapi.internal.objects.WithContent;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iterators;
import com.github.owlcs.ontapi.owlapi.OWLObjectImpl;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An abstraction for axioms that has []-lists in its definitions
 * (e.g. {@link org.semanticweb.owlapi.model.OWLDisjointClassesAxiom},
 * {@link org.semanticweb.owlapi.model.OWLHasKeyAxiom}, etc).
 * Created by @ssz on 14.10.2019.
 *
 * @param <A> - a subtype of {@link OWLAxiom}
 * @param <E> - a list's member subtype ({@link OWLObject})
 * @since 2.0.0
 */
@SuppressWarnings("rawtypes")
interface WithList<A extends OWLAxiom, E extends OWLObject> extends WithTriple, WithContent<A> {

    /**
     * Extracts and lists all members from the statement.
     *
     * @param statement {@link OntStatement}, not {@code null}
     * @param factory   {@link ModelObjectFactory} to create {@link ONTObject}s, not {@code null}
     * @return an {@link ExtendedIterator} of {@link ONTObject}s with {@link E}s
     */
    ExtendedIterator<ONTObject<? extends E>> listONTComponents(OntStatement statement, ModelObjectFactory factory);

    /**
     * Lists all components and annotations of this axiom.
     *
     * @param factory {@link ModelObjectFactory}, not {@code null}
     * @return a {@code Stream} of {@link ONTObject}s that wrap either {@link E}s or {@link OWLAnnotation}s
     */
    Stream<ONTObject<? extends OWLObject>> objects(ModelObjectFactory factory);

    /**
     * Restores the cached object to the {@link ONTObject} instance.
     *
     * @param x       - cached item, not {@code null}
     * @param factory {@link ModelObjectFactory}, not {@code null}
     * @return {@link ONTObject}
     * @see WithList#toContentItem(ONTObject)
     */
    ONTObject fromContentItem(Object x, ModelObjectFactory factory);

    @Override
    default boolean isAnnotated() {
        return ONTAnnotationImpl.hasAnnotations(getContent());
    }

    @SuppressWarnings("unchecked")
    @Override
    default Stream<OWLAnnotation> annotations() {
        Stream res = Arrays.stream(getContent()).map(WithList::toOWLAnnotation).filter(Objects::nonNull);
        return (Stream<OWLAnnotation>) res;
    }

    @Override
    default List<OWLAnnotation> annotationsAsList() {
        return annotations().collect(Collectors.toList());
    }

    /**
     * Makes a content item from the object.
     *
     * @param x {@link ONTObject} to cache, not {@code null}
     * @return a content item
     * @see WithList#fromContentItem(Object, ModelObjectFactory)
     */
    default Object toContentItem(ONTObject x) {
        return x instanceof OWLEntity ? ONTEntityImpl.getURI((OWLEntity) x) : x;
    }

    /**
     * Restores an annotation from the cached item.
     *
     * @param x - cached item, not {@code null}
     * @return {@link OWLAnnotation}
     * @see WithList#fromContentItem(Object, ModelObjectFactory)
     */
    static OWLAnnotation toOWLAnnotation(Object x) {
        if (x instanceof OWLAnnotation) {
            return (OWLAnnotation) x;
        }
        if (x instanceof ONTObject) {
            OWLObject res = ((ONTObject) x).getOWLObject();
            if (res instanceof OWLAnnotation)
                return (OWLAnnotation) res;
        }
        return null;
    }

    /**
     * For axioms that have both subject and []-list: {@code s predicate []}.
     * There are three such axioms:
     * <ul>
     * <li>A property chain inclusion: {@code P owl:propertyChainAxiom (P1 ... Pn)}</li>
     * <li>HasKey axiom: {@code C owl:hasKey (P1 ... Pm R1 ... Rn)}</li>
     * <li>Disjoint union: {@code CN owl:disjointUnionOf (C1 ... Cn)}</li>
     * </ul>
     *
     * @param <A> - subtype of {@link OWLAxiom}
     * @param <S> - subtype of {@link OWLObject}
     * @param <E> - subtype of {@link OWLObject}
     */
    interface WithSubject<A extends OWLAxiom, S extends OWLObject, E extends OWLObject> extends WithList<A, E> {

        ONTObject<? extends S> findSubjectByURI(String uri, ModelObjectFactory factory);

        ONTObject<? extends S> fetchONTSubject(OntStatement statement, ModelObjectFactory factory);

        default ONTObject<? extends S> getONTSubject() {
            return findONTSubject(getObjectFactory());
        }

        default ONTObject<? extends S> findONTSubject(ModelObjectFactory factory) {
            return findONTSubject(getContent()[0], factory);
        }

        @Override
        default Stream<ONTObject<? extends OWLObject>> objects() {
            return objects(getObjectFactory());
        }

        @Override
        default Stream<ONTObject<? extends OWLObject>> objects(ModelObjectFactory factory) {
            return objects(getContent(), factory);
        }

        @SuppressWarnings("unchecked")
        default Stream<ONTObject<? extends OWLObject>> objects(Object[] content, ModelObjectFactory factory) {
            Object s = findONTSubject(content[0], factory);
            Stream res = Stream.concat(Stream.of(s), Arrays.stream(content).skip(1));
            return (Stream<ONTObject<? extends OWLObject>>) res.map(x -> fromContentItem(x, factory));
        }

        @SuppressWarnings("unchecked")
        default ONTObject<? extends S> findONTSubject(Object content, ModelObjectFactory factory) {
            return content instanceof String ?
                    findSubjectByURI((String) content, factory) : (ONTObject<? extends S>) content;
        }

        default Stream<ONTObject<? extends E>> members() {
            return members(getContent(), getObjectFactory());
        }

        @SuppressWarnings("unchecked")
        default Stream<ONTObject<? extends E>> members(Object[] content, ModelObjectFactory factory) {
            Stream res = Arrays.stream(content).skip(1)
                    .map(x -> fromContentItem(x, factory))
                    .filter(x -> WithList.toOWLAnnotation(x) == null);
            return (Stream<ONTObject<? extends E>>) res;
        }

        @Override
        default Set<? extends OWLObject> getOWLComponentsAsSet(ModelObjectFactory factory) {
            Set<OWLObject> res = OWLObjectImpl.createSortedSet();
            objects(getContent(), factory)
                    .map(ONTObject::getOWLObject)
                    .filter(x -> !(x instanceof OWLAnnotation)).forEach(res::add);
            return res;
        }
    }

    /**
     * A {@link WithSubject} axiom that has fixed order determining by {@code rdf:List}.
     * The order of members in this axiom is important.
     *
     * @param <A> - subtype of {@link OWLAxiom}
     * @param <S> - subtype of {@link OWLObject}
     * @param <E> - subtype of {@link OWLObject}
     */
    interface Sequent<A extends OWLAxiom, S extends OWLObject, E extends OWLObject> extends WithSubject<A, S, E> {

        /**
         * Creates an {@link ONTObject} container for the given {@link OntStatement};
         * the returned object is also {@link R}.
         *
         * @param <R>       the desired {@link OWLAxiom axiom}-type which is also {@link Sequent}
         * @param statement {@link OntStatement}, the source to parse, not {@code null}
         * @param getAxiom  factory (as {@link BiFunction}) to provide {@link R} instance, not {@code null}
         * @param setHash   {@code ObjIntConsumer<OWLAxiom>}, facility to assign {@code hashCode}, not {@code null}
         * @param factory   {@link ModelObjectFactory} (singleton), not {@code null}
         * @param config    {@link AxiomsSettings} (singleton), not {@code null}
         * @return {@link R}
         */
        @SuppressWarnings("unchecked")
        static <R extends ONTObject & Sequent> R create(OntStatement statement,
                                                        BiFunction<Triple, Supplier<OntModel>, ? extends R> getAxiom,
                                                        ObjIntConsumer<OWLAxiom> setHash,
                                                        ModelObjectFactory factory,
                                                        AxiomsSettings config) {
            R res = getAxiom.apply(statement.asTriple(), factory.model());
            List content = new ArrayList();
            ONTObject<? extends OWLObject> s = res.fetchONTSubject(statement, factory);
            content.add(res.toContentItem(s));
            Iterator<ONTObject<? extends OWLObject>> it = res.listONTComponents(statement, factory);
            int h = 1;
            while (it.hasNext()) {
                ONTObject e = it.next();
                h = WithContent.hashIteration(h, e.hashCode());
                content.add(res.toContentItem(e));
            }
            int hash = OWLObject.hashIteration(res.hashIndex(), h);
            hash = OWLObject.hashIteration(hash, s.hashCode());
            h = 1;
            for (Object a : ONTAxiomImpl.collectAnnotations(statement, factory, config)) {
                content.add(a);
                h = WithContent.hashIteration(h, a.hashCode());
            }
            hash = OWLObject.hashIteration(hash, h);
            setHash.accept(res, hash);
            res.putContent(content.toArray());
            return res;
        }

        @Override
        default Object[] collectContent() {
            OntStatement statement = asStatement();
            ModelObjectFactory factory = getObjectFactory();
            AxiomsSettings config = getConfig();
            List<Object> content = new ArrayList<>();
            content.add(toContentItem(fetchONTSubject(statement, factory)));
            Iterator<ONTObject<? extends E>> it = listONTComponents(statement, factory);
            it.forEachRemaining(x -> content.add(toContentItem(x)));
            content.addAll(ONTAxiomImpl.collectAnnotations(statement, factory, config));
            return content.toArray();
        }
    }

    /**
     * A {@link WithSubject} axiom whose components order does not determine by underlying {@code rdf:List},
     * all items are stored as sorted {@code Collection}.
     *
     * @param <A> - subtype of {@link OWLAxiom}
     * @param <S> - subtype of {@link OWLObject}
     * @param <E> - subtype of {@link OWLObject}
     */
    interface Sorted<A extends OWLAxiom, S extends OWLObject, E extends OWLObject> extends WithSubject<A, S, E> {

        /**
         * Creates an {@link ONTObject} container for the given {@link OntStatement};
         * the returned object is also {@link R}.
         *
         * @param <R>       the desired {@link OWLAxiom axiom}-type which is also {@link Sorted}
         * @param statement {@link OntStatement}, the source to parse, not {@code null}
         * @param getAxiom  factory (as {@link BiFunction}) to provide {@link R} instance, not {@code null}
         * @param setHash   {@code ObjIntConsumer<OWLAxiom>}, facility to assign {@code hashCode}, not {@code null}
         * @param factory   {@link ModelObjectFactory} (singleton), not {@code null}
         * @param config    {@link AxiomsSettings} (singleton), not {@code null}
         * @return {@link R}
         */
        @SuppressWarnings("unchecked")
        static <R extends ONTObject & Sorted> R create(OntStatement statement,
                                                       BiFunction<Triple, Supplier<OntModel>, ? extends R> getAxiom,
                                                       ObjIntConsumer<OWLAxiom> setHash,
                                                       ModelObjectFactory factory,
                                                       AxiomsSettings config) {
            R res = getAxiom.apply(statement.asTriple(), factory.model());
            ONTObject<? extends OWLObject> s = res.fetchONTSubject(statement, factory);
            int hash = OWLObject.hashIteration(res.hashIndex(), s.hashCode());
            Set<ONTObject<? extends OWLObject>> components = res.fetchONTComponents(statement, factory);
            Collection<ONTObject<OWLAnnotation>> annotations = ONTAxiomImpl.collectAnnotations(statement, factory, config);
            Object[] content = new Object[1 + components.size() + annotations.size()];
            int index = 0;
            content[index++] = res.toContentItem(s);
            int h = 1;
            for (ONTObject x : components) {
                h = WithContent.hashIteration(h, x.hashCode());
                content[index++] = res.toContentItem(x);
            }
            hash = OWLObject.hashIteration(hash, h);
            h = 1;
            for (Object a : annotations) {
                h = WithContent.hashIteration(h, a.hashCode());
                content[index++] = a;
            }
            hash = OWLObject.hashIteration(hash, h);
            setHash.accept(res, hash);
            res.putContent(content);
            return res;
        }

        default Set<ONTObject<? extends E>> fetchONTComponents(OntStatement statement, ModelObjectFactory factory) {
            return Iterators.addAll(listONTComponents(statement, factory), ONTObjectImpl.createContentSet());
        }

        @Override
        default Object[] collectContent() {
            OntStatement statement = asStatement();
            ModelObjectFactory factory = getObjectFactory();
            AxiomsSettings config = getConfig();
            List<Object> content = new ArrayList<>();
            content.add(toContentItem(fetchONTSubject(statement, factory)));
            for (ONTObject x : fetchONTComponents(statement, factory)) {
                content.add(toContentItem(x));
            }
            content.addAll(ONTAxiomImpl.collectAnnotations(statement, factory, config));
            return content.toArray();
        }
    }
}
