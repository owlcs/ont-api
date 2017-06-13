/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * The (Named) Datatype Property.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntNDP extends OntPE, OntEntity, Property {

    /**
     * Adds negative data property assertion
     *
     * @param source {@link OntIndividual}, the source
     * @param target {@link Literal}, the target
     * @return {@link OntNPA.DataAssertion}
     * @see OntOPE#addNegativeAssertion(OntIndividual, OntIndividual)
     */
    OntNPA.DataAssertion addNegativeAssertion(OntIndividual source, Literal target);

    /**
     * Creates or removes "R rdf:type owl:FunctionalProperty" statement
     *
     * @param functional if true makes this data property as functional
     * @see OntOPE#setFunctional(boolean)
     */
    void setFunctional(boolean functional);

    /**
     * Returns all associated negative data property assertions
     *
     * @return Stream of {@link OntNPA.DataAssertion}s.
     * @see OntOPE#negativeAssertions()
     */
    default Stream<OntNPA.DataAssertion> negativeAssertions() {
        return getModel().ontObjects(OntNPA.DataAssertion.class).filter(a -> OntNDP.this.equals(a.getProperty()));
    }

    /**
     * Returns all associated negative data property assertions for the specified source individual.
     *
     * @param source {@link OntIndividual}
     * @return Stream of {@link OntNPA.DataAssertion}s.
     * @see OntOPE#negativeAssertions(OntIndividual)
     */
    default Stream<OntNPA.DataAssertion> negativeAssertions(OntIndividual source) {
        return negativeAssertions()
                .filter(a -> a.getSource().equals(source));
    }

    /**
     * Answers iff this data-property is functional
     *
     * @return true if functional
     * @see OntOPE#isFunctional()
     */
    default boolean isFunctional() {
        return hasType(OWL.FunctionalProperty);
    }

    /**
     * Returns domain class expressions (statement "R rdfs:domain C").
     *
     * @return Stream of {@link OntCE}s.
     */
    @Override
    default Stream<OntCE> domain() {
        return objects(RDFS.domain, OntCE.class);
    }

    /**
     * Adds statement "R rdfs:domain C"
     *
     * @param domain {@link OntCE}
     * @return {@link OntStatement}
     */
    default OntStatement addDomain(OntCE domain) {
        return addStatement(RDFS.domain, domain);
    }

    /**
     * Returns ranges (statement pattern: "R rdfs:range D")
     *
     * @return Stream of {@link OntDR}s
     */
    @Override
    default Stream<OntDR> range() {
        return objects(RDFS.range, OntDR.class);
    }

    /**
     * Adds statement "R rdfs:range D"
     *
     * @param range {@link OntDR}
     * @return {@link OntStatement}
     */
    default OntStatement addRange(OntDR range) {
        return addStatement(RDFS.range, range);
    }

    /**
     * Returns all super properties, the pattern is "R1 rdfs:subPropertyOf R2"
     *
     * @return Stream of {@link OntNDP}s
     * @see #addSubPropertyOf(OntNDP)
     * @see OntPE#removeSubPropertyOf(Resource)
     */
    @Override
    default Stream<OntNDP> subPropertyOf() {
        return objects(RDFS.subPropertyOf, OntNDP.class);
    }

    /**
     * Adds super property.
     *
     * @param superProperty {@link OntNDP}
     * @return {@link OntStatement}
     */
    default OntStatement addSubPropertyOf(OntNDP superProperty) {
        return addStatement(RDFS.subPropertyOf, superProperty);
    }

    /**
     * Returns disjoint properties (statement: "R1 owl:propertyDisjointWith R2").
     *
     * @return Stream of {@link OntNDP}s
     * @see OntOPE#disjointWith()
     * @see OntDisjoint.DataProperties
     */
    default Stream<OntNDP> disjointWith() {
        return objects(OWL.propertyDisjointWith, OntNDP.class);
    }

    /**
     * Adds disjoint data property.
     *
     * @param other {@link OntNDP}
     * @return {@link OntStatement}
     * @see OntOPE#addDisjointWith(OntOPE)
     * @see OntDisjoint.DataProperties
     */
    default OntStatement addDisjointWith(OntNDP other) {
        return addStatement(OWL.propertyDisjointWith, other);
    }

    /**
     * Clears all "R1 owl:propertyDisjointWith R2" statements for the specified data property.
     *
     * @param other {@link OntNDP}
     * @see OntOPE#removeDisjointWith(OntOPE)
     * @see OntDisjoint.DataProperties
     */
    default void removeDisjointWith(OntNDP other) {
        remove(OWL.propertyDisjointWith, other);
    }

    /**
     * Returns all equivalent data properties ("Ri owl:equivalentProperty Rj")
     *
     * @return Stream of {@link OntNDP}s.
     * @see OntOPE#equivalentProperty()
     */
    default Stream<OntNDP> equivalentProperty() {
        return objects(OWL.equivalentProperty, OntNDP.class);
    }

    /**
     * Adds new owl:equivalentProperty statement.
     *
     * @param other {@link OntNDP}
     * @return {@link OntStatement}
     * @see OntOPE#addEquivalentProperty(OntOPE)
     */
    default OntStatement addEquivalentProperty(OntNDP other) {
        return addStatement(OWL.equivalentProperty, other);
    }

    /**
     * Removes all equivalent-property statements for the specified data property.
     *
     * @param other {@link OntNDP}
     * @see OntOPE#removeEquivalentProperty(OntOPE)
     */
    default void removeEquivalentProperty(OntNDP other) {
        remove(OWL.equivalentProperty, other);
    }

    /**
     * @see Property#isProperty()
     */
    @Override
    default boolean isProperty() {
        return true;
    }

    /**
     * @see Property#getOrdinal()
     */
    @Override
    default int getOrdinal() {
        return as(Property.class).getOrdinal();
    }
}
