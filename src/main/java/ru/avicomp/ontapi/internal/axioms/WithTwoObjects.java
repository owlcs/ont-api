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

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.internal.HasObjectFactory;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.internal.objects.ONTComposite;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * A technical interface that describes an axiom based on a single (main) triple with constant predicate.
 * Just to avoid copy-pasting.
 * Created by @ssz on 30.09.2019.
 *
 * @param <S> - any subtype of {@link OWLObject} (the type of triple's subject)
 * @param <O> - any subtype of {@link OWLObject} (the type of triple's object)
 * @since 1.4.3
 */
interface WithTwoObjects<S extends OWLObject, O extends OWLObject>
        extends ONTComposite, HasObjectFactory {

    /**
     * Finds the {@link ONTObject} that matches the triple's subject using the given factory.
     *
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @return an {@link ONTObject}
     */
    ONTObject<? extends S> findONTSubject(InternalObjectFactory factory);

    /**
     * Finds the {@link ONTObject} that matches the triple's object using the given factory.
     *
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @return an {@link ONTObject}
     */
    ONTObject<? extends O> findONTObject(InternalObjectFactory factory);

    /**
     * Answers {@code true} iff the subject is an URI resource.
     *
     * @return boolean
     */
    boolean hasURIObject();

    /**
     * Answers {@code true} iff the object is an URI resource.
     *
     * @return boolean
     */
    boolean hasURISubject();

    /**
     * Gets the subject from the base triple of this axiom.
     *
     * @return {@link ONTObject} with {@link S}
     */
    default ONTObject<? extends S> getONTSubject() {
        return findONTSubject(getObjectFactory());
    }

    /**
     * Gets the object from the base triple of this axiom.
     *
     * @return {@link ONTObject} with {@link O}
     */
    default ONTObject<? extends O> getONTObject() {
        return findONTObject(getObjectFactory());
    }

    @Override
    default Stream<ONTObject<? extends OWLObject>> objects() {
        InternalObjectFactory factory = getObjectFactory();
        return Stream.of(findONTSubject(factory), findONTObject(factory));
    }

    /**
     * For this class it is assumed that content contains only b-nodes and annotations, named objects are not cached.
     *
     * @param <A> - any subtype of {@link OWLAxiom}
     * @param <S> - any subtype of {@link OWLObject} (the type of triple's subject)
     * @param <O> - any subtype of {@link OWLObject} (the type of triple's object)
     */
    interface WithContent<A extends OWLAxiom, S extends OWLObject, O extends OWLObject>
            extends WithTwoObjects<S, O>, ru.avicomp.ontapi.internal.objects.WithContent<A> {

        /**
         * Finds the {@link ONTObject} that matches the triple's subject using the given factory and content array.
         *
         * @param content an {@code Array} with content
         * @param factory {@link InternalObjectFactory}, not {@code null}
         * @return an {@link ONTObject}
         */
        ONTObject<? extends S> findONTSubject(Object[] content, InternalObjectFactory factory);

        /**
         * Finds the {@link ONTObject} that matches the triple's object using the given factory and content array.
         *
         * @param content an {@code Array} with content
         * @param factory {@link InternalObjectFactory}, not {@code null}
         * @return an {@link ONTObject}
         */
        ONTObject<? extends O> findONTObject(Object[] content, InternalObjectFactory factory);

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
                if (hasURIObject()) {
                    InternalObjectFactory factory = getObjectFactory();
                    objects = Stream.of(findONTSubject(content, factory), findONTObject(content, factory));
                } else {
                    objects = Stream.of(findONTSubject(content, getObjectFactory()));
                }
            } else if (hasURIObject()) {
                objects = Stream.of(findONTObject(content, getObjectFactory()));
            }
            res = objects != null ? Stream.concat(objects, res) : res;
            return (Stream<ONTObject<? extends OWLObject>>) res;
        }
    }
}
