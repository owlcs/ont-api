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

package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.stream.Stream;

/**
 * Interface for named and anonymous individuals.
 * <p>
 * Created by @szuev on 02.11.2016.
 */
public interface OntIndividual extends OntObject {

    /**
     * Adds a type (class expression) to this individual.
     *
     * @param clazz {@link OntCE}
     * @return {@link OntStatement}
     */
    OntStatement attachClass(OntCE clazz);

    /**
     * Removes class assertion for the specified class expression.
     *
     * @param clazz {@link OntCE}
     * @throws ru.avicomp.ontapi.jena.OntJenaException in case it is anonymous individual and there is no more class-assertions.
     */
    void detachClass(OntCE clazz);

    /**
     * Answers a {@code Stream} over the class expressions to which this individual belongs,
     * including super-classes if the flag {@code direct} is {@code false}.
     * If the flag {@code direct} is {@code true}, then only direct types are returned,
     * and the method is almost equivalent to the method {@link #classes()}.
     * See also {@link OntCE#listSuperClasses(boolean)}.
     *
     * @param direct if {@code true}, only answers those {@link OntCE}s that are direct types of this individual,
     *               not the super-classes of the class etc
     * @return <b>distinct</b> {@code Stream} of {@link OntCE class expressions}
     * @see #classes()
     * @see OntCE#listSuperClasses(boolean)
     * @since 1.4.0
     */
    Stream<OntCE> listClasses(boolean direct);

    /**
     * Returns all direct class types.
     *
     * @return Stream of {@link OntCE}s
     */
    default Stream<OntCE> classes() {
        return objects(RDF.type, OntCE.class);
    }

    /**
     * Returns all same individuals. The pattern to search for is "ai owl:sameAs aj"
     *
     * @return Stream of {@link OntIndividual}s.
     */
    default Stream<OntIndividual> sameAs() {
        return objects(OWL.sameAs, OntIndividual.class);
    }

    /**
     * Adds same individual reference.
     *
     * @param other {@link OntIndividual}
     * @return {@link OntStatement}
     */
    default OntStatement addSameAs(OntIndividual other) {
        return addStatement(OWL.sameAs, other);
    }

    /**
     * Removes a {@code owl:sameAs} statement for the specified object.
     *
     * @param other {@link OntIndividual}, or {@code null} to remove all same individuals
     */
    default void removeSameAs(OntIndividual other) {
        remove(OWL.sameAs, other);
    }

    /**
     * Lists all different individuals.
     * The pattern to search for is {@code a1 owl:differentFrom a2},
     * where {@code a1} is this {@link OntIndividual} and {@code a2} is one of the returned {@link OntIndividual}.
     *
     * @return Stream of {@link OntIndividual}s
     * @see OntDisjoint.Individuals
     */
    default Stream<OntIndividual> differentFrom() {
        return objects(OWL.differentFrom, OntIndividual.class);
    }

    /**
     * Adds different individual.
     *
     * @param other {@link OntIndividual}
     * @return {@link OntStatement}
     * @see OntDisjoint.Individuals
     */
    default OntStatement addDifferentFrom(OntIndividual other) {
        return addStatement(OWL.differentFrom, other);
    }

    /**
     * Removes a different individual statement for the specified individual.
     *
     * @param other {@link OntIndividual} or {@code null} to remove all different individuals
     * @see OntDisjoint.Individuals
     */
    default void removeDifferentFrom(OntIndividual other) {
        remove(OWL.differentFrom, other);
    }

    /**
     * Adds annotation assertion {@code AnnotationAssertion(A s t)}.
     * In general case it is {@code s A t}, where {@code s} is IRI or anonymous individual,
     * {@code A} - annotation property, and {@code t} - IRI, anonymous individual, or literal.
     *
     * @param property {@link OntNAP}
     * @param value    {@link RDFNode} (IRI, anonymous individual, or literal)
     * @return this individual to allow cascading calls
     * @see #addAnnotation(OntNAP, RDFNode)
     */
    default OntIndividual addAssertion(OntNAP property, RDFNode value) {
        addProperty(property, value);
        return this;
    }

    /**
     * Adds a positive data property assertion {@code a R v}.
     *
     * @param property {@link OntNDP}
     * @param value    {@link Literal}
     * @return this individual to allow cascading calls
     */
    default OntIndividual addAssertion(OntNDP property, Literal value) {
        addProperty(property, value);
        return this;
    }

    /**
     * Adds a positive object property assertion {@code a1 PN a2}.
     *
     * @param property {@link OntNOP} named object property
     * @param value    {@link OntIndividual} other individual
     * @return this individual to allow cascading calls
     */
    default OntIndividual addAssertion(OntNOP property, OntIndividual value) {
        addProperty(property, value);
        return this;
    }

    /**
     * Adds a negative object property assertion.
     * <pre>
     * Functional syntax: {@code NegativeObjectPropertyAssertion(P a1 a2)}
     * RDF Syntax:
     * {@code
     * _:x rdf:type owl:NegativePropertyAssertion .
     * _:x owl:sourceIndividual a1 .
     * _:x owl:assertionProperty P .
     * _:x owl:targetIndividual a2 .
     * }
     * </pre>
     *
     * @param property {@link OntOPE}
     * @param value    {@link OntIndividual} other individual
     * @return this individual to allow cascading calls
     */
    default OntIndividual addNegativeAssertion(OntOPE property, OntIndividual value) {
        property.addNegativeAssertion(this, value);
        return this;
    }

    /**
     * Adds a negative data property assertion.
     * <pre>
     * Functional syntax: {@code NegativeDataPropertyAssertion(R a v)}
     * RDF Syntax:
     * {@code
     * _:x rdf:type owl:NegativePropertyAssertion.
     * _:x owl:sourceIndividual a .
     * _:x owl:assertionProperty R .
     * _:x owl:targetValue v.
     * }
     * </pre>
     *
     * @param property {@link OntNDP}
     * @param value    {@link Literal}
     * @return this individual to allow cascading calls
     */
    default OntIndividual addNegativeAssertion(OntNDP property, Literal value) {
        property.addNegativeAssertion(this, value);
        return this;
    }

    /**
     * Lists all positive assertions.
     *
     * @return Stream of {@link OntStatement}s
     */
    default Stream<OntStatement> positiveAssertions() {
        return statements().filter(s -> s.getPredicate().canAs(OntPE.class));
    }

    /**
     * Lists all negative property assertions.
     *
     * @return Stream of {@link OntNPA negative property assertions}
     */
    default Stream<OntNPA> negativeAssertions() {
        return getModel().ontObjects(OntNPA.class).filter(s -> OntIndividual.this.equals(s.getSource()));
    }

    /**
     * An interface for <b>Named</b> Individual which is an {@link OWL Entity OntEntity}.
     * <p>
     * Created by szuev on 01.11.2016.
     */
    interface Named extends OntIndividual, OntEntity {
    }

    /**
     * An interface for Anonymous Individuals.
     * The anonymous individual is a blank node ({@code _:a}) which satisfies one of the following conditions:
     * <ul>
     * <li>it has a class declaration (i.e. there is a triple {@code _:a rdf:type C}, where {@code C} is a {@link OntCE class expression})</li>
     * <li>it is a subject or an object in a statement with predicate {@link OWL#sameAs owl:sameAs} or {@link OWL#differentFrom owl:differentFrom}</li>
     * <li>it is contained in a {@code rdf:List} with predicate {@code owl:distinctMembers} or {@code owl:members}
     * in a blank node with {@code rdf:type = owl:AllDifferent}, see {@link OntDisjoint.Individuals}</li>
     * <li>it is contained in a {@code rdf:List} with predicate {@code owl:oneOf} in a blank node with {@code rdf:type = owl:Class},
     * see {@link OntCE.OneOf}</li>
     * <li>it is a part of {@link OntNPA owl:NegativePropertyAssertion} section with predicates
     * {@link OWL#sourceIndividual owl:sourceIndividual} or {@link OWL#targetIndividual owl:targetIndividual}</li>
     * <li>it is an object with predicate {@code owl:hasValue} inside {@code _:x rdf:type owl:Restriction}
     * (see {@link OntCE.ObjectHasValue Object Property HasValue Restriction})</li>
     * <li>it is a subject or an object in a statement where predicate is an uri-resource with {@code rdf:type = owl:AnnotationProperty}
     * (i.e. {@link OntNAP annotation property} assertion {@code s A t})</li>
     * <li>it is a subject in a triple which corresponds data property assertion {@code _:a R v}
     * (where {@code R} is a {@link OntNDP datatype property}, {@code v} is a {@link Literal literal})</li>
     * <li>it is a subject or an object in a triple which corresponds object property assertion {@code _:a1 PN _:a2}
     * (where {@code PN} is a {@link OntNOP named object property}, and {@code _:ai} are individuals)</li>
     * </ul>
     * <p>
     * Created by szuev on 10.11.2016.
     */
    interface Anonymous extends OntIndividual {
    }
}
