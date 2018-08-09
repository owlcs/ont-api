/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.stream.Stream;

/**
 * For SWRL addition.
 * <p>
 * Created by @szuev on 02.11.2016.
 *
 * @see ru.avicomp.ontapi.jena.vocabulary.SWRL
 * @see <a href='https://www.w3.org/Submission/SWRL'>specification</a>
 */
public interface OntSWRL extends OntObject {

    interface Imp extends OntSWRL {

        /**
         * Gets the head ONT-List.
         * The list <b>is</b> typed: each list item has the type {@link ru.avicomp.ontapi.jena.vocabulary.SWRL#AtomList swrl:AtomList}.
         *
         * @return {@link OntList} of {@link Atom}
         * @since 1.2.1
         */
        OntList<Atom> getHeadList();

        /**
         * Gets the body ONT-List.
         * The list <b>is</b> typed: each list item has the type {@link ru.avicomp.ontapi.jena.vocabulary.SWRL#AtomList swrl:AtomList}.
         *
         * @return {@link OntList} of {@link Atom}
         * @since 1.2.1
         */
        OntList<Atom> getBodyList();

        default Stream<Atom> head() {
            return getHeadList().members().distinct();
        }

        default Stream<Atom> body() {
            return getBodyList().members().distinct();
        }
    }

    interface Variable extends OntSWRL {
    }

    /**
     * It is not SWRL Object, but just a plain {@link OntObject}.
     * An interface that represents either {@link org.apache.jena.rdf.model.Literal}, {@link Variable} or {@link OntIndividual}.
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

    interface Atom<P extends RDFNode> extends OntSWRL {

        /**
         * Returns the atom predicate, which can be one of the following:
         * {@link OntDR}, {@link OntOPE}, {@link OntNDP}, {@link OntCE}, URI-{@link Resource}, {@link Property}.
         *
         * @return RDFNode
         */
        P getPredicate();

        interface BuiltIn extends Atom<Resource> {
            /**
             * Gets the arguments typed ONT-List, the list <b>is not</b> typed,
             * i.e. there are no {@code _:x rdf:type rdf:List} for list items.
             *
             * @return {@link OntList} of {@link DArg}
             * @since 1.2.1
             */
            OntList<DArg> getArgList();

            default Stream<DArg> arguments() {
                return getArgList().members();
            }
        }

        interface OntClass extends Unary<OntCE, IArg> {
        }

        interface DataRange extends Unary<OntDR, DArg> {
        }

        interface DataProperty extends Binary<OntNDP, IArg, DArg> {
        }

        interface ObjectProperty extends Binary<OntOPE, IArg, IArg> {
        }

        interface DifferentIndividuals extends Binary<Property, IArg, IArg> {
        }

        interface SameIndividuals extends Binary<Property, IArg, IArg> {
        }

        interface Binary<P extends Resource, F extends Arg, S extends Arg> extends Atom<P> {
            F getFirstArg();

            S getSecondArg();
        }

        interface Unary<P extends Resource, A extends Arg> extends Atom<P> {
            A getArg();
        }

    }
}
