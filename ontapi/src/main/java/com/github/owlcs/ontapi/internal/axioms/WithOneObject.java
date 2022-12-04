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
import com.github.owlcs.ontapi.internal.objects.WithContent;
import com.github.owlcs.ontapi.internal.objects.WithoutAnnotations;
import com.github.owlcs.ontapi.jena.model.OntModel;
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
 * A technical interface that describes an axiom based on a single (main) triple with constant predicate and object.
 * Such an axiom has only one operand ({@link S subject})
 * and corresponds to the triple pattern {@code S predicate object},
 * where {@code predicate} and {@code object} are fixed constants from OWL, RDF or RDFS vocabularies.
 * Example: {@code R rdf:type owl:FunctionalProperty}.
 * Created by @ssz on 05.10.2019.
 *
 * @param <S> - any subtype of {@link OWLObject} (the type of triple's subject)
 * @since 2.0.0
 */
@SuppressWarnings("rawtypes")
interface WithOneObject<S extends OWLObject> extends WithTriple {

    ONTObject<? extends S> findURISubject(ModelObjectFactory factory);

    ONTObject<? extends S> fetchONTSubject(OntStatement statement, ModelObjectFactory factory);

    ONTObject<? extends S> findONTValue(ModelObjectFactory factory);

    default ONTObject<? extends S> getONTValue() {
        return findONTValue(getObjectFactory());
    }

    @Override
    default Set<? extends OWLObject> getOWLComponentsAsSet(ModelObjectFactory factory) {
        Set<OWLObject> res = OWLObjectImpl.createSortedSet();
        res.add(findONTValue(factory).getOWLObject());
        return res;
    }

    /**
     * Creates an {@link ONTObject} container for the given {@link OntStatement};
     * the returned object is also {@link R}.
     * Impl notes:
     * If there is no sub-annotations and subject is a URI-{@link org.apache.jena.rdf.model.Resource},
     * then a simplified instance of {@link Simple} is returned, and for this the factory {@code simple} is used.
     * Otherwise, the instance is {@link Complex}, created by the factory {@code complex} and has a cache inside.
     * Note: this is an auxiliary method as shortcut to reduce copy-pasting, it is for internal usage only.
     *
     * @param <R>       the desired {@link OWLAxiom axiom}-type
     * @param statement {@link OntStatement}, the source to parse, not {@code null}
     * @param simple    factory (as {@link BiFunction}) to provide {@link Simple} instance, not {@code null}
     * @param complex   factory (as {@link BiFunction}) to provide {@link Complex} instance, not {@code null}
     * @param setHash   {@code ObjIntConsumer<OWLAxiom>}, facility to assign {@code hashCode}, not {@code null}
     * @param factory   {@link ModelObjectFactory} (singleton), not {@code null}
     * @param config    {@link AxiomsSettings} (singleton), not {@code null}
     * @return {@link R}
     */
    static <R extends ONTObject & WithOneObject> R create(OntStatement statement,
                                                          BiFunction<Triple, Supplier<OntModel>, ? extends R> simple,
                                                          BiFunction<Triple, Supplier<OntModel>, ? extends R> complex,
                                                          ObjIntConsumer<OWLAxiom> setHash,
                                                          ModelObjectFactory factory,
                                                          AxiomsSettings config) {
        R s = simple.apply(statement.asTriple(), factory.model());
        Object[] content = Complex.initContent(s, statement, setHash, factory, config);
        if (content == null) {
            return s;
        }
        R c = complex.apply(statement.asTriple(), factory.model());
        setHash.accept(c, s.hashCode());
        ((WithContent<?>) c).putContent(content);
        return c;
    }

    /**
     * Represents the simplest case when the axiom has no annotations and the triple's subject is URI resources.
     *
     * @param <X> - any subtype of {@link OWLObject} (the type of triple's subject)
     */
    interface Simple<X extends OWLObject> extends WithOneObject<X>, WithoutAnnotations {

        @Override
        default ONTObject<? extends X> findONTValue(ModelObjectFactory factory) {
            return findURISubject(factory);
        }

        @Override
        default Stream<ONTObject<? extends OWLObject>> objects() {
            return Stream.of(findURISubject(getObjectFactory()));
        }

        @Override
        default boolean isAnnotated() {
            return false;
        }
    }

    /**
     * For a class that implements this interface it is assumed
     * that the content contains only b-nodes and annotations,
     * named objects (which correspond to the main triple) are not cached.
     *
     * @param <A> - any subtype of {@link OWLAxiom} which is implemented by the instance of this interface
     * @param <X> - any subtype of {@link OWLObject} (the type of main triple's subject)
     */
    interface Complex<A extends OWLAxiom, X extends OWLObject> extends WithOneObject<X>, WithContent<A> {

        /**
         * Calculates the content and {@code hashCode} simultaneously.
         * Such a way was chosen for performance’s sake.
         *
         * @param axiom     - a {@link WithOneObject} instance, the axiom, not {@code null}
         * @param statement - a {@link OntStatement}, the source statement, not {@code null}
         * @param setHash   - a {@code ObjIntConsumer<OWLAxiom>}, facility to assign {@code hashCode}, not {@code null}
         * @param factory   - a {@link ModelObjectFactory} singleton, not {@code null}
         * @param config    - a {@link AxiomsSettings} singleton, not {@code null}
         * @return an {@code Array} with content or {@code null} if no content is needed
         */
        static Object[] initContent(WithOneObject axiom,
                                    OntStatement statement,
                                    ObjIntConsumer<OWLAxiom> setHash,
                                    ModelObjectFactory factory,
                                    AxiomsSettings config) {
            Collection annotations = ONTAxiomImpl.collectAnnotations(statement, factory, config);
            ONTObject value = axiom.fetchONTSubject(statement, factory);
            int hash = OWLObject.hashIteration(axiom.hashIndex(), value.hashCode());
            Object[] res;
            int index = 0;
            if (statement.getSubject().isURIResource()) {
                res = new Object[annotations.size()];
            } else {
                res = new Object[annotations.size() + 1];
                res[index++] = value;
            }
            int h = 1;
            for (Object a : annotations) {
                res[index++] = a;
                h = WithContent.hashIteration(h, a.hashCode());
            }
            setHash.accept(axiom, OWLObject.hashIteration(hash, h));
            return res.length == 0 ? null : res;
        }

        @Override
        default Object[] collectContent() {
            OntStatement statement = asStatement();
            ModelObjectFactory factory = getObjectFactory();
            List<ONTObject> res = new ArrayList<>(1);
            ONTObject value = fetchONTSubject(statement, factory);
            if (!statement.getSubject().isURIResource()) {
                res.add(value);
            }
            res.addAll(ONTAxiomImpl.collectAnnotations(statement, factory, getConfig()));
            return res.toArray();
        }

        @SuppressWarnings("unchecked")
        @Override
        default ONTObject<? extends X> findONTValue(ModelObjectFactory factory) {
            return hasURISubject() ? findURISubject(factory) : (ONTObject<? extends X>) getContent()[0];
        }

        @Override
        @SuppressWarnings("unchecked")
        default Stream<ONTObject<? extends OWLObject>> objects() {
            Object[] content = getContent();
            Stream res = Arrays.stream(content);
            if (hasURISubject()) {
                ModelObjectFactory factory = getObjectFactory();
                res = Stream.concat(Stream.of(findURISubject(factory)), res);
            }
            return (Stream<ONTObject<? extends OWLObject>>) res;
        }

        @Override
        default boolean isAnnotated() {
            return ONTAnnotationImpl.hasAnnotations(getContent());
        }

        @Override
        default Stream<OWLAnnotation> annotations() {
            return ONTAnnotationImpl.contentAsStream(getContent(), hasURISubject() ? 0 : 1);
        }

        @Override
        default List<OWLAnnotation> annotationsAsList() {
            return ONTAnnotationImpl.contentAsList(getContent(), hasURISubject() ? 0 : 1);
        }
    }

}
