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

import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.internal.InternalConfig;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.internal.objects.*;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.owlapi.OWLObjectImpl;

import java.util.*;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A technical interface that describes a n-ary axiom,
 * which may be presented as a triple (arity is {@code 2}) or as
 * []-list based section (e.g. {@link ru.avicomp.ontapi.jena.vocabulary.OWL#AllDisjointClasses owl:AllDisjointClasses}).
 * Note: for internal usage only, it is just to avoid copy-pasting.
 * <p>
 * Created by @ssz on 02.10.2019.
 *
 * @param <E> - any subtype of {@link OWLObject} (the type of axiom components)
 * @since 1.4.3
 */
interface WithManyObjects<E extends OWLObject> extends WithTriple {

    /**
     * Gets the {@link ONTObject}-wrapper from the factory.
     *
     * @param uri     String, an entity URI, not {@code null}
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @return {@link ONTObject} of {@link E}
     */
    ONTObject<? extends E> findByURI(String uri, InternalObjectFactory factory);

    /**
     * Extracts and lists all elements from the given statement.
     *
     * @param statement {@link OntStatement}, the source, not {@code null}
     * @param factory   {@link InternalObjectFactory}, not {@code null}
     * @return an {@link ExtendedIterator} of {@link ONTObject} with type {@link E}
     */
    ExtendedIterator<ONTObject<? extends E>> listONTComponents(OntStatement statement, InternalObjectFactory factory);

    /**
     * Gets all components (as {@link ONTObject}s) in the form of sorted {@code Set}.
     *
     * @param statement {@link OntStatement}, the source, not {@code null}
     * @param factory   {@link InternalObjectFactory}, not {@code null}
     * @return a {@code Set} of {@link ONTObject} with type {@link E}
     */
    default Set<ONTObject<? extends E>> getONTComponents(OntStatement statement, InternalObjectFactory factory) {
        return Iter.addAll(listONTComponents(statement, factory), ONTObjectImpl.createContentSet());
    }

    /**
     * Returns a sorted and distinct {@code Stream} over all components (annotations are not included).
     *
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @return a {@code Stream} of {@link ONTObject}s that wrap {@link E}s
     */
    Stream<ONTObject<? extends E>> sorted(InternalObjectFactory factory);

    /**
     * Lists all components of this axiom.
     *
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @return a {@code Stream} of {@link ONTObject}s that wrap {@link E}s
     */
    Stream<ONTObject<? extends E>> members(InternalObjectFactory factory);

    /**
     * Sorts and lists all components of this axiom.
     *
     * @return a sorted {@code Stream} of {@link ONTObject}s that wrap {@link E}s
     */
    default Stream<ONTObject<? extends E>> sorted() {
        return sorted(getObjectFactory());
    }

    /**
     * Lists all components of this axiom.
     *
     * @return an unsorted {@code Stream} of {@link ONTObject}s that wrap {@link E}s
     */
    default Stream<ONTObject<? extends E>> members() {
        return members(getObjectFactory());
    }

    /**
     * Gets all components as a sorted {@code Set} with exclusion of the specified.
     *
     * @param excludes an {@code Array} of {@link E}s, not {@code null}
     * @return a {@link Set} of {@link E}s
     */
    @SuppressWarnings("unchecked")
    default Set<E> getSetMinus(E... excludes) {
        return sorted().map(ONTObject::getOWLObject)
                .filter(negationPredicate(excludes))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Creates a {@code Predicate} that allows everything with exclusion of specified elements.
     *
     * @param excludes an {@code Array} of {@link X}-elements to exclude, not {@code null}
     * @param <X>      - anything
     * @return a {@link Predicate} for {@link X}
     */
    @SafeVarargs
    static <X> Predicate<X> negationPredicate(X... excludes) {
        if (excludes.length == 0) {
            return x -> true;
        }
        Set<X> set = new HashSet<>(Arrays.asList(excludes));
        return x -> !set.contains(x);
    }

    /**
     * Represents the simplest case of unannotated axiom with arity {@code 2}, that corresponds a single triple.
     *
     * @param <E> - any subtype of {@link OWLObject} (the type of axiom components)
     */
    interface Simple<E extends OWLObject> extends WithManyObjects<E>, WithoutAnnotations {
        @Override
        default boolean isAnnotated() {
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        default Stream<ONTObject<? extends OWLObject>> objects() {
            return (Stream<ONTObject<? extends OWLObject>>) objects(getObjectFactory());
        }

        @SuppressWarnings("unchecked")
        @Override
        default Stream<ONTObject<? extends E>> members(InternalObjectFactory factory) {
            return (Stream<ONTObject<? extends E>>) objects(getObjectFactory());
        }

        default Stream objects(InternalObjectFactory factory) {
            return Stream.of(findByURI(getSubjectURI(), factory), findByURI(getObjectURI(), factory));
        }

        @Override
        default Stream<ONTObject<? extends E>> sorted(InternalObjectFactory factory) {
            Set<ONTObject<? extends E>> res = ONTObjectImpl.createContentSet();
            res.add(findByURI(getSubjectURI(), factory));
            res.add(findByURI(getObjectURI(), factory));
            return res.stream();
        }

        @Override
        default Set<? extends OWLObject> getOWLComponentsAsSet(InternalObjectFactory factory) {
            Set<OWLObject> res = OWLObjectImpl.createSortedSet();
            res.add(findByURI(getSubjectURI(), factory).getOWLObject());
            res.add(findByURI(getObjectURI(), factory).getOWLObject());
            return res;
        }
    }

    /**
     * Represents the axiom which cannot be present in simplified form (i.e. as {@link Simple}).
     * It has annotations or b-nodes as components, or does not correspond a single triple.
     *
     * @param <A> - any subtype of {@link OWLAxiom} which is implemented by the instance of this interface
     * @param <E> - any subtype of {@link OWLObject} (the type of axiom components)
     */
    interface WithSortedContent<A extends OWLAxiom, E extends OWLObject> extends WithManyObjects<E>, WithContent<A> {

        /**
         * Calculates the content and {@code hashCode} simultaneously.
         * Such a way was chosen for performance sake.
         *
         * @param axiom     - a {@link WithTwoObjects} instance, the axiom, not {@code null}
         * @param statement - a {@link OntStatement}, the source statement, not {@code null}
         * @param setHash   - a {@code ObjIntConsumer<OWLAxiom>}, facility to assign {@code hashCode}, not {@code null}
         * @param simplify  - boolean, if {@code true}, and the given statement is simple
         *                  (no annotations, uri subject and object), an empty array is returned
         * @param factory   - a {@link InternalObjectFactory} singleton, not {@code null}
         * @param config    - a {@link InternalConfig} singleton, not {@code null}
         * @return an {@code Array} with content
         */
        @SuppressWarnings("unchecked")
        static Object[] initContent(WithManyObjects axiom,
                                    OntStatement statement,
                                    ObjIntConsumer<OWLAxiom> setHash,
                                    boolean simplify,
                                    InternalObjectFactory factory,
                                    InternalConfig config) {
            Collection annotations = ONTAxiomImpl.collectAnnotations(statement, factory, config);
            Set<OWLObject> components = axiom.getONTComponents(statement, factory);
            Object[] res = new Object[components.size() + annotations.size()];
            int index = 0;
            int h = 1;
            for (OWLObject c : components) {
                res[index++] = toContentItem(c);
                h = WithContent.hashIteration(h, c.hashCode());
            }
            int hash = OWLObject.hashIteration(axiom.hashIndex(), h);
            h = 1;
            for (Object a : annotations) {
                res[index++] = a;
                h = WithContent.hashIteration(h, a.hashCode());
            }
            setHash.accept(axiom, OWLObject.hashIteration(hash, h));
            if (simplify && annotations.isEmpty()) {
                if (res.length == 1 && res[0] instanceof String) { // symmetric triple 's p s'
                    return ONTStatementImpl.EMPTY;
                }
                if (res.length == 2 && res[0] instanceof String && res[1] instanceof String) { // 's p o'
                    return ONTStatementImpl.EMPTY;
                }
            }
            return res;
        }

        static Object toContentItem(Object o) {
            return o instanceof OWLEntity ? ONTEntityImpl.getURI((OWLEntity) o) : o;
        }

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

        @Override
        default Object[] collectContent() {
            OntStatement statement = asStatement();
            InternalObjectFactory factory = getObjectFactory();
            List<ONTObject> res = new ArrayList<>(2);
            res.addAll(getONTComponents(statement, factory));
            res.addAll(ONTAxiomImpl.collectAnnotations(statement, factory, getConfig()));
            if (res.isEmpty()) {
                return ONTStatementImpl.EMPTY;
            }
            return res.toArray();
        }

        default ONTObject fromContentItem(Object o, InternalObjectFactory factory) {
            return o instanceof String ? findByURI((String) o, factory) : (ONTObject) o;
        }

        @SuppressWarnings("unchecked")
        @Override
        default Stream<ONTObject<? extends OWLObject>> objects() {
            InternalObjectFactory factory = getObjectFactory();
            Stream res = Arrays.stream(getContent());
            return (Stream<ONTObject<? extends OWLObject>>) res.map(x -> fromContentItem(x, factory));
        }

        @Override
        default Stream<ONTObject<? extends E>> members(InternalObjectFactory factory) {
            return sorted(factory);
        }

        @SuppressWarnings("unchecked")
        @Override
        default Stream<ONTObject<? extends E>> sorted(InternalObjectFactory factory) {
            Stream res = Arrays.stream(getContent())
                    .map(x -> fromContentItem(x, factory))
                    .filter(x -> toOWLAnnotation(x) == null);
            return (Stream<ONTObject<? extends E>>) res;
        }

        @Override
        default Set<? extends OWLObject> getOWLComponentsAsSet(InternalObjectFactory factory) {
            Set<OWLObject> res = OWLObjectImpl.createSortedSet();
            Arrays.stream(getContent())
                    .map(x -> fromContentItem(x, factory))
                    .filter(x -> toOWLAnnotation(x) == null)
                    .forEach(x -> res.add(x.getOWLObject()));
            return res;
        }

        @Override
        default boolean isAnnotated() {
            return ONTAnnotationImpl.hasAnnotations(getContent());
        }

        @SuppressWarnings("unchecked")
        @Override
        default Stream<OWLAnnotation> annotations() {
            Stream res = Arrays.stream(getContent()).map(WithSortedContent::toOWLAnnotation).filter(Objects::nonNull);
            return (Stream<OWLAnnotation>) res;
        }

        @Override
        default List<OWLAnnotation> annotationsAsList() {
            return annotations().collect(Collectors.toList());
        }
    }

}
