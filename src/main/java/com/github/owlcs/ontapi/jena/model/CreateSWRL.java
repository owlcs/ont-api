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

package com.github.owlcs.ontapi.jena.model;

import org.apache.jena.rdf.model.Resource;

import java.util.Collection;

/**
 * A technical interface to generate SWRL Objects (Variable, Atoms, Imp).
 * <p>
 * Created by @szz on 14.05.2019.
 *
 * @since 1.4.0
 */
interface CreateSWRL {

    /**
     * Creates a SWRL Variable,
     * that is an URI resource with a type {@link com.github.owlcs.ontapi.jena.vocabulary.SWRL#Variable swrl:Variable}.
     *
     * @param uri String, not {@code null}
     * @return {@link OntSWRL.Variable}
     */
    OntSWRL.Variable createSWRLVariable(String uri);

    /**
     * Creates a BuiltIn Atom.
     * An input predicate can be taken from {@link com.github.owlcs.ontapi.jena.vocabulary.SWRLB SWRL Builins Vocabulary}.
     * Turtle syntax:
     * <pre>{@code
     * 	_:x rdf:type swrl:BuiltinAtom .
     * _:x swrl:arguments ( d1 ... d2 ) .
     * _:x swrl:builtin U .
     * }</pre>
     *
     * @param predicate an <b>URI</b>, {@link Resource}, not {@code null}
     * @param arguments {@code Collection} of {@link OntSWRL.DArg}s
     * @return {@link OntSWRL.Atom.BuiltIn}
     * @see com.github.owlcs.ontapi.jena.vocabulary.SWRLB
     */
    OntSWRL.Atom.BuiltIn createBuiltInSWRLAtom(Resource predicate, Collection<OntSWRL.DArg> arguments);

    /**
     * Creates a Class Atom.
     * Turtle syntax:
     * <pre>{@code
     * _:x rdf:type swrl:ClassAtom .
     * _:x swrl:argument1 i .
     * _:x swrl:classPredicate C .
     * }</pre>
     *
     * @param clazz {@link OntCE}, not {@code null}
     * @param arg   {@link OntSWRL.IArg} (either {@link OntIndividual} or {@link OntSWRL.Variable}), not {@code null}
     * @return {@link OntSWRL.Atom.OntClass}
     */
    OntSWRL.Atom.OntClass createClassSWRLAtom(OntCE clazz, OntSWRL.IArg arg);

    /**
     * Creates a Data Range Atom.
     * Turtle syntax:
     * <pre>{@code
     * _:x rdf:type swrl:DataRangeAtom .
     * _:x swrl:argument1 d .
     * _:x swrl:dataRange D .
     * }</pre>
     *
     * @param range {@link OntDR}, not {@code null}
     * @param arg   {@link OntSWRL.DArg} (either {@link OntSWRL.Variable}
     *              or {@link org.apache.jena.rdf.model.Literal}), not {@code null}
     * @return {@link OntSWRL.Atom.DataRange}
     */
    OntSWRL.Atom.DataRange createDataRangeSWRLAtom(OntDR range, OntSWRL.DArg arg);

    /**
     * Creates a Data Property Atom.
     * Turtle syntax:
     * <pre>{@code
     * _:x rdf:type swrl:DatavaluedPropertyAtom .
     * _:x swrl:argument1 i .
     * _:x swrl:argument2 d .
     * _:x swrl:propertyPredicate R .
     * }</pre>
     *
     * @param property {@link OntNDP}, not {@code null}
     * @param first    {@link OntSWRL.IArg} (either {@link OntIndividual} or {@link OntSWRL.Variable}), not {@code null}
     * @param second   {@link OntSWRL.DArg} (either {@link OntSWRL.Variable}
     *                 or {@link org.apache.jena.rdf.model.Literal}), not {@code null}
     * @return {@link OntSWRL.Atom.DataProperty}
     */
    OntSWRL.Atom.DataProperty createDataPropertySWRLAtom(OntNDP property, OntSWRL.IArg first, OntSWRL.DArg second);

    /**
     * Creates an Object Property Atom.
     * Turtle syntax:
     * <pre>{@code
     * _:x rdf:type swrl:IndividualPropertyAtom .
     * _:x swrl:argument1 i1 .
     * _:x swrl:argument2 i2 .
     * _:x swrl:propertyPredicate P .
     * }</pre>
     *
     * @param property {@link OntOPE}, not {@code null}
     * @param first    {@link OntSWRL.IArg} (either {@link OntIndividual} or {@link OntSWRL.Variable}), not {@code null}
     * @param second   {@link OntSWRL.IArg} (either {@link OntIndividual} or {@link OntSWRL.Variable}), not {@code null}
     * @return {@link OntSWRL.Atom.ObjectProperty}
     */
    OntSWRL.Atom.ObjectProperty createObjectPropertySWRLAtom(OntOPE property, OntSWRL.IArg first, OntSWRL.IArg second);

    /**
     * Creates a Different Individuals Atom.
     * Turtle syntax:
     * <pre>{@code
     * _:x rdf:type swrl:DifferentIndividualsAtom .
     * _:x swrl:argument1 i1 .
     * _:x swrl:argument2 i2 .
     * }</pre>
     *
     * @param first  {@link OntSWRL.IArg} (either {@link OntIndividual} or {@link OntSWRL.Variable}), not {@code null}
     * @param second {@link OntSWRL.IArg} (either {@link OntIndividual} or {@link OntSWRL.Variable}), not {@code null}
     * @return {@link OntSWRL.Atom.DifferentIndividuals}
     */
    OntSWRL.Atom.DifferentIndividuals createDifferentIndividualsSWRLAtom(OntSWRL.IArg first, OntSWRL.IArg second);

    /**
     * Creates a Same Individuals Atom.
     * Turtle syntax:
     * <pre>{@code
     * _:x rdf:type swrl:SameIndividualAtom .
     * _:x swrl:argument1 i1 .
     * _:x swrl:argument2 i2 .
     * }</pre>
     *
     * @param first  {@link OntSWRL.IArg} (either {@link OntIndividual} or {@link OntSWRL.Variable}), not {@code null}
     * @param second {@link OntSWRL.IArg} (either {@link OntIndividual} or {@link OntSWRL.Variable}), not {@code null}
     * @return {@link OntSWRL.Atom.SameIndividuals}
     */
    OntSWRL.Atom.SameIndividuals createSameIndividualsSWRLAtom(OntSWRL.IArg first, OntSWRL.IArg second);

    /**
     * Creates a SWRL Rule.
     * A rule consists of a head and a body. Both the head and the body consist of a conjunction of Atoms.
     * In RDF, instead of a regular []-list, a typed version of []-lis is used,
     * where {@code rdf:type} is {@link com.github.owlcs.ontapi.jena.vocabulary.SWRL#AtomList swrl:AtomList}.
     * Turtle syntax:
     * <pre>{@code
     * _:x rdf:type swrl:Impl .
     * _:x swrl:body (swrl:AtomList: A1 ... An ) .
     * _:x swrl:head (swrl:AtomList: A1 ... Am ) .
     * }</pre>
     *
     * @param head {@code Collection} of {@link OntSWRL.Atom}s
     * @param body {@code Collection} of {@link OntSWRL.Atom}s
     * @return {@link OntSWRL.Imp}
     */
    OntSWRL.Imp createSWRLImp(Collection<OntSWRL.Atom<?>> head,
                              Collection<OntSWRL.Atom<?>> body);
}
