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

package com.github.owlcs.ontapi.jena.model;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * A base for SWRL addition.
 * <p>
 * Created by @szuev on 02.11.2016.
 *
 * @see com.github.owlcs.ontapi.jena.vocabulary.SWRL
 * @see <a href='https://www.w3.org/Submission/SWRL'>specification</a>
 */
public interface OntSWRL extends OntObject {

    /**
     * @see OntModel#createSWRLImp(Collection, Collection)
     */
    @SuppressWarnings("rawtypes")
    interface Imp extends OntSWRL {

        /**
         * Gets the head ONT-List.
         * The list <b>is</b> typed:
         * each of its items has the type {@link com.github.owlcs.ontapi.jena.vocabulary.SWRL#AtomList swrl:AtomList}.
         *
         * @return {@link OntList} of {@link Atom}
         */
        OntList<Atom> getHeadList();

        /**
         * Gets the body ONT-List.
         * The list <b>is</b> typed:
         * each of its items has the type {@link com.github.owlcs.ontapi.jena.vocabulary.SWRL#AtomList swrl:AtomList}.
         *
         * @return {@link OntList} of {@link Atom}
         */
        OntList<Atom> getBodyList();

        default Stream<Atom> head() {
            return getHeadList().members().distinct();
        }

        default Stream<Atom> body() {
            return getBodyList().members().distinct();
        }
    }

    /**
     * Represents {@link com.github.owlcs.ontapi.jena.vocabulary.SWRL#Builtin} entity.
     * Must be a URI {@link Resource}.
     */
    interface Builtin extends OntSWRL {
    }

    /**
     * Represents {@link com.github.owlcs.ontapi.jena.vocabulary.SWRL#Variable} entity.
     *
     * @see OntModel#createSWRLVariable(String)
     */
    interface Variable extends OntSWRL, DArg, IArg {
    }

    /**
     * It is not a SWRL Object, but just a plain {@link OntObject}.
     * An interface that represents either {@link org.apache.jena.rdf.model.Literal},
     * {@link Variable} or {@link OntIndividual}.
     */
    interface Arg extends OntObject {
    }

    /**
     * An interface that represents either {@link org.apache.jena.rdf.model.Literal} or {@link Variable}.
     */
    interface DArg extends Arg {
    }

    /**
     * An interface that represents either {@link OntIndividual} or {@link Variable}.
     */
    interface IArg extends Arg {
    }

    /**
     * A base abstraction for SWRL-Atom.
     *
     * @param <P> subtype of {@link OntObject}
     */
    interface Atom<P extends OntObject> extends OntSWRL {

        /**
         * Returns the atom predicate, which can be one of the following:
         * {@link OntDataRange}, {@link OntObjectProperty}, {@link OntDataProperty}, {@link OntClass}, URI-{@link Resource}, {@link Property}.
         *
         * @return RDFNode
         */
        P getPredicate();

        /**
         * Lists all arguments from this {@code Atom}.
         *
         * @return Stream of {@link Arg}s
         */
        Stream<? extends Arg> arguments();

        /**
         * @see OntModel#createBuiltInSWRLAtom(Resource, Collection)
         */
        interface WithBuiltin extends Atom<Builtin> {
            /**
             * Gets the argument's ONT-List.
             * Note that the returned list is <b>not</b> expected to be typed,
             * i.e. there is neither {@code _:x rdf:type rdf:List}
             * or {@code _:x rdf:type swrl:AtomList} statements for each its items.
             *
             * @return {@link OntList} of {@link DArg}s
             */
            OntList<DArg> getArgList();

            @Override
            default Stream<DArg> arguments() {
                return getArgList().members();
            }
        }

        /**
         * @see OntModel#createClassSWRLAtom(OntClass, IArg)
         */
        interface WithClass extends Unary<OntClass, IArg> {
        }

        /**
         * @see OntModel#createDataRangeSWRLAtom(OntDataRange, DArg)
         */
        interface WithDataRange extends Unary<OntDataRange, DArg> {
        }

        /**
         * @see OntModel#createDataPropertySWRLAtom(OntDataProperty, IArg, DArg)
         */
        interface WithDataProperty extends Binary<OntDataProperty, IArg, DArg> {
        }

        /**
         * @see OntModel#createObjectPropertySWRLAtom(OntObjectProperty, IArg, IArg)
         */
        interface WithObjectProperty extends Binary<OntObjectProperty, IArg, IArg> {
        }

        /**
         * @see CreateSWRL#createDifferentIndividualsSWRLAtom(IArg, IArg)
         */
        interface WithDifferentIndividuals extends Binary<OntObjectProperty.Named, IArg, IArg> {
        }

        /**
         * @see CreateSWRL#createSameIndividualsSWRLAtom(IArg, IArg)
         */
        interface WithSameIndividuals extends Binary<OntObjectProperty.Named, IArg, IArg> {
        }

        /**
         * A binary atom abstraction.
         *
         * @param <P> the predicate - either {@link Arg} or {@link OntRealProperty}
         * @param <F> {@link Arg} the first argument
         * @param <S> {@link Arg} the second argument
         */
        interface Binary<P extends OntObject, F extends Arg, S extends Arg> extends Atom<P> {
            F getFirstArg();

            S getSecondArg();

            @Override
            default Stream<Arg> arguments() {
                return Stream.of(getFirstArg(), getSecondArg());
            }
        }

        /**
         * A unary atom abstraction.
         *
         * @param <P> the predicate, either {@link OntClass} or {@link OntDataRange}
         * @param <A> {@link Arg}, the argument
         */
        interface Unary<P extends OntObject, A extends Arg> extends Atom<P> {
            A getArg();

            @Override
            default Stream<A> arguments() {
                return Stream.of(getArg());
            }
        }
    }
}
