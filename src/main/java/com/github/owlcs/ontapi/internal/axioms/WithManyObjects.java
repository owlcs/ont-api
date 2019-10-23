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
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTObjectImpl;
import com.github.owlcs.ontapi.internal.objects.WithContent;
import com.github.owlcs.ontapi.internal.objects.WithoutAnnotations;
import com.github.owlcs.ontapi.jena.model.OntGraphModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.owlapi.OWLObjectImpl;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A technical interface that describes a n-ary axiom,
 * which may be presented as a triple (arity is {@code 2}) or as
 * []-list based section (e.g. {@link com.github.owlcs.ontapi.jena.vocabulary.OWL#AllDisjointClasses owl:AllDisjointClasses}).
 * Note: for internal usage only, it is just to avoid copy-pasting.
 * <p>
 * Created by @ssz on 02.10.2019.
 *
 * @param <E> - any subtype of {@link OWLObject} (the type of axiom components)
 * @since 2.0.0
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
     * Lists all components and annotations of this axiom.
     *
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @return a {@code Stream} of {@link ONTObject}s that wrap either {@link E}s or {@link OWLAnnotation}s
     */
    Stream<ONTObject<? extends OWLObject>> objects(InternalObjectFactory factory);

    /**
     * Gets all components (as {@link ONTObject}s) in the form of sorted {@code Set}.
     *
     * @param statement {@link OntStatement}, the source, not {@code null}
     * @param factory   {@link InternalObjectFactory}, not {@code null}
     * @return a {@code Set} of {@link ONTObject} with type {@link E}
     */
    default Set<ONTObject<? extends E>> fetchONTComponents(OntStatement statement, InternalObjectFactory factory) {
        return Iter.addAll(listONTComponents(statement, factory), ONTObjectImpl.createContentSet());
    }

    /**
     * Sorts and lists all components of this axiom.
     *
     * @return a sorted {@code Stream} of {@link ONTObject}s that wrap {@link E}s
     */
    default Stream<ONTObject<? extends E>> sorted() {
        return sorted(getObjectFactory());
    }

    /**
     * Lists all characteristic (i.e. without annotations) components of this axiom.
     *
     * @return an unsorted {@code Stream} of {@link ONTObject}s that wrap {@link E}s
     */
    default Stream<ONTObject<? extends E>> members() {
        return members(getObjectFactory());
    }

    @Override
    default Stream<ONTObject<? extends OWLObject>> objects() {
        return objects(getObjectFactory());
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
     * Creates an {@link ONTObject} container for the given {@link OntStatement};
     * the returned object is also {@link R}.
     * Impl notes:
     * If there is no sub-annotations,
     * and the subject and object are URI-{@link org.apache.jena.rdf.model.Resource}s, which correspond operands,
     * then a simplified instance of {@link Simple} is returned, for this the factory {@code simple} is used.
     * Otherwise the instance is {@link Complex}, it is created by the factory {@code complex} and has a cache inside.
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
    static <R extends ONTObject & WithManyObjects> R create(OntStatement statement,
                                                            Supplier<OntGraphModel> model,
                                                            BiFunction<Triple, Supplier<OntGraphModel>, ? extends R> simple,
                                                            BiFunction<Triple, Supplier<OntGraphModel>, ? extends R> complex,
                                                            ObjIntConsumer<OWLAxiom> setHash,
                                                            InternalObjectFactory factory,
                                                            InternalConfig config) {
        R c = complex.apply(statement.asTriple(), model);
        Object[] content = Complex.initContent((Complex) c, statement, setHash, true, factory, config);
        if (content != null) {
            ((WithContent<?>) c).putContent(content);
            return c;
        }
        R s = simple.apply(statement.asTriple(), model);
        setHash.accept(s, c.hashCode());
        return s;
    }

    /**
     * Creates an {@link ONTObject} container for the given {@link OntStatement};
     * the returned object is also {@link R}.
     * This method is intended to produce {@code n-ary} axioms
     * that are mapped from {@link com.github.owlcs.ontapi.jena.model.OntDisjoint} list-based anonymous resources.
     *
     * @param statement {@link OntStatement}, the source to parse, not {@code null}
     * @param model     {@link OntGraphModel}-provider, not {@code null}
     * @param maker     factory (as {@link BiFunction}) to provide {@link Complex} instance, not {@code null}
     * @param setHash   {@code ObjIntConsumer<OWLAxiom>}, facility to assign {@code hashCode}, not {@code null}
     * @param factory   {@link InternalObjectFactory} (singleton), not {@code null}
     * @param config    {@link InternalConfig} (singleton), not {@code null}
     * @param <R>       the desired {@link OWLAxiom axiom}-type
     * @return {@link R}
     */
    static <R extends ONTObject & Complex> R create(OntStatement statement,
                                                    Supplier<OntGraphModel> model,
                                                    BiFunction<Triple, Supplier<OntGraphModel>, ? extends R> maker,
                                                    ObjIntConsumer<OWLAxiom> setHash,
                                                    InternalObjectFactory factory,
                                                    InternalConfig config) {
        R res = maker.apply(statement.asTriple(), model);
        res.putContent(Complex.initContent(res, statement, setHash, false, factory, config));
        return res;
    }

    /**
     * Represents the simplest case of unannotated axiom with arity {@code 2},
     * that corresponds a single triple consisting of URI nodes.
     *
     * @param <E> - any subtype of {@link OWLObject} (the type of axiom components)
     */
    interface Simple<E extends OWLObject> extends WithManyObjects<E>, WithoutAnnotations {
        @Override
        default boolean isAnnotated() {
            return false;
        }

        @SuppressWarnings({"unchecked", "RedundantCast"})
        @Override
        default Stream<ONTObject<? extends E>> members(InternalObjectFactory factory) {
            return (Stream<ONTObject<? extends E>>) ((Stream) objects(getObjectFactory()));
        }

        default Stream<ONTObject<? extends OWLObject>> objects(InternalObjectFactory factory) {
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
    interface Complex<A extends OWLAxiom, E extends OWLObject> extends WithManyObjects<E>, WithList<A, E> {

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
         * @return an {@code Array} with content or {@code null} if no content is needed
         */
        @SuppressWarnings("unchecked")
        static Object[] initContent(Complex axiom,
                                    OntStatement statement,
                                    ObjIntConsumer<OWLAxiom> setHash,
                                    boolean simplify,
                                    InternalObjectFactory factory,
                                    InternalConfig config) {
            Collection annotations = ONTAxiomImpl.collectAnnotations(statement, factory, config);
            Set<ONTObject> components = axiom.fetchONTComponents(statement, factory);
            Object[] res = new Object[components.size() + annotations.size()];
            int index = 0;
            int h = 1;
            for (ONTObject c : components) {
                res[index++] = axiom.toContentItem(c);
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
                    return null;
                }
                if (res.length == 2 && res[0] instanceof String && res[1] instanceof String) { // 's p o'
                    return null;
                }
            }
            return res;
        }

        @Override
        default Object[] collectContent() {
            OntStatement statement = asStatement();
            InternalObjectFactory factory = getObjectFactory();
            List<Object> res = new ArrayList<>(2);
            fetchONTComponents(statement, factory).forEach(c -> res.add(toContentItem(c)));
            res.addAll(ONTAxiomImpl.collectAnnotations(statement, factory, getConfig()));
            return res.toArray();
        }

        @Override
        default ONTObject fromContentItem(Object x, InternalObjectFactory factory) {
            return x instanceof String ? findByURI((String) x, factory) : (ONTObject) x;
        }

        @SuppressWarnings("unchecked")
        @Override
        default Stream<ONTObject<? extends OWLObject>> objects(InternalObjectFactory factory) {
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
                    .filter(x -> WithList.toOWLAnnotation(x) == null);
            return (Stream<ONTObject<? extends E>>) res;
        }

        @Override
        default Set<? extends OWLObject> getOWLComponentsAsSet(InternalObjectFactory factory) {
            Set<OWLObject> res = OWLObjectImpl.createSortedSet();
            Arrays.stream(getContent())
                    .map(x -> fromContentItem(x, factory))
                    .filter(x -> WithList.toOWLAnnotation(x) == null)
                    .forEach(x -> res.add(x.getOWLObject()));
            return res;
        }
    }

}
