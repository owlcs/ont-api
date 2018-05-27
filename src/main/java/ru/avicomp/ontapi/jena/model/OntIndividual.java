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

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.stream.Stream;

/**
 * For named and anonymous individuals
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
     * Removes owl:sameAs statement for the specified object.
     *
     * @param other {@link OntIndividual}
     */
    default void removeSameAs(OntIndividual other) {
        remove(OWL.sameAs, other);
    }

    /**
     * Returns all differen individuals, the pattern to search for is "a1 owl:differentFrom a2"
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
     * Removes different individual statement.
     *
     * @param other {@link OntIndividual}
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
     * An interface for Named Individual.
     * <p>
     * Created by szuev on 01.11.2016.
     */
    interface Named extends OntIndividual, OntEntity {
    }

    /**
     * An interface for Anonymous Individuals.
     * The anonymous individual is a blank node ("_:a") which satisfies one of the following conditions:
     * <ul>
     * <li>it has a class declaration (i.e. there is a triple "_:a rdf:type C", where C is a class expression)</li>
     * <li>it is a subject or an object in a statement with predicate owl:sameAs or owl:differentFrom</li>
     * <li>it is contained in a rdf:List with predicate owl:distinctMembers or owl:members in a blank node with rdf:type owl:AllDifferent</li>
     * <li>it is contained in a rdf:List with predicate owl:oneOf in a blank node with rdf:type owl:Class</li>
     * <li>it is a part of owl:NegativePropertyAssertion section with predicates owl:sourceIndividual or owl:targetIndividual</li>
     * <li>it is an object with predicate owl:hasValue inside "_:x rdf:type owl:Restriction" (Object Property Restriction)</li>
     * <li>it is a subject or an object in a statement where predicate is an uri-resource with rdf:type owl:AnnotationProperty (i.e. annotation property assertion "s A t")</li>
     * <li>it is a subject in a triple which corresponds data property assertion "_:a R v" (where "R" is a datatype property, "v" is a literal)</li>
     * <li>it is a subject or an object in a triple which corresponds object property assertion "_:a1 PN _:a2" (where PN is a named object property)</li>
     * </ul>
     * <p>
     * Created by szuev on 10.11.2016.
     */
    interface Anonymous extends OntIndividual {
    }
}
