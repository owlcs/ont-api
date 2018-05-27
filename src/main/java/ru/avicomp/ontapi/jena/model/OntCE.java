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
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Common interface for any Class Expressions (both named and anonymous).
 * See for example <a href='https://www.w3.org/TR/owl2-quick-reference/'>2.1 Class Expressions</a>
 * <p>
 * Created by szuev on 01.11.2016.
 * @see OntClass
 */
public interface OntCE extends OntObject {

    /**
     * Creates an anonymous individual which is of this class type.
     *
     * @return {@link OntIndividual.Anonymous}
     * @see OntIndividual#attachClass(OntCE)
     */
    OntIndividual.Anonymous createIndividual();

    /**
     * Creates a named individual which is of this class type.
     *
     * @param uri, String not null
     * @return {@link OntIndividual.Named}
     * @see OntIndividual#attachClass(OntCE)
     */
    OntIndividual.Named createIndividual(String uri);

    /**
     * Lists all key properties.
     * I.e. returns all object- and datatype- properties which belong to
     * the {@code C owl:hasKey (P1 ... Pm R1 ... Rn)} statements,
     * where {@code C} - this class expression, {@code P} is a property expression, and {@code R} is a data(-type) property.
     *
     * @return distinct Stream of {@link OntOPE}s and {@link OntNDP}s.
     */
    Stream<OntPE> hasKey();

    /**
     * Creates an {@code owl:hasKey} statement.
     *
     * @param objectProperties the collection of {@link OntOPE}s/
     * @param dataProperties   the collection of {@link OntNDP}s.
     * @return {@link OntStatement}
     */
    OntStatement addHasKey(Collection<OntOPE> objectProperties, Collection<OntNDP> dataProperties);

    /**
     * Removes all key properties.
     * I.e. removes all statements with their content from a {@code owl:hasKey} axiom.
     */
    void removeHasKey();

    /*
     * ============================
     * all known Class Expressions:
     * ============================
     */

    interface ObjectSomeValuesFrom extends ComponentRestrictionCE<OntCE, OntOPE> {
    }

    interface DataSomeValuesFrom extends ComponentRestrictionCE<OntDR, OntNDP> {
    }

    interface ObjectAllValuesFrom extends ComponentRestrictionCE<OntCE, OntOPE> {
    }

    interface DataAllValuesFrom extends ComponentRestrictionCE<OntDR, OntNDP> {
    }

    interface ObjectHasValue extends ComponentRestrictionCE<OntIndividual, OntOPE> {
    }

    interface DataHasValue extends ComponentRestrictionCE<Literal, OntNDP> {
    }

    interface ObjectMinCardinality extends CardinalityRestrictionCE<OntCE, OntOPE> {
    }

    interface DataMinCardinality extends CardinalityRestrictionCE<OntDR, OntNDP> {
    }

    interface ObjectMaxCardinality extends CardinalityRestrictionCE<OntCE, OntOPE> {
    }

    interface DataMaxCardinality extends CardinalityRestrictionCE<OntDR, OntNDP> {
    }

    interface ObjectCardinality extends CardinalityRestrictionCE<OntCE, OntOPE> {
    }

    interface DataCardinality extends CardinalityRestrictionCE<OntDR, OntNDP> {
    }

    interface HasSelf extends RestrictionCE, ONProperty<OntOPE> {
    }

    interface UnionOf extends ComponentsCE<OntCE> {
    }

    interface OneOf extends ComponentsCE<OntIndividual> {
    }

    interface IntersectionOf extends ComponentsCE<OntCE> {
    }

    interface ComplementOf extends OntCE, Value<OntCE> {
    }

    interface NaryDataAllValuesFrom extends NaryRestrictionCE<OntDR, OntNDP> {
    }

    interface NaryDataSomeValuesFrom extends NaryRestrictionCE<OntDR, OntNDP> {
    }

    /*
     * ======================================
     * Technical interfaces for abstract CEs:
     * ======================================
     */

    interface ONProperty<P extends OntPE> {
        P getOnProperty();

        void setOnProperty(P p);
    }

    interface ONProperties<P extends OntPE> {
        Stream<P> onProperties();

        void setOnProperties(Collection<P> properties);
    }

    interface Components<O extends OntObject> {
        Stream<O> components();

        void setComponents(Collection<O> components);
    }

    interface Value<O extends RDFNode> {
        O getValue();

        void setValue(O value);
    }

    interface Cardinality {
        int getCardinality();

        void setCardinality(int cardinality);

        /**
         * Determines if this restriction is qualified.
         * Qualified cardinality restrictions are defined to be cardinality restrictions
         * that have fillers which aren't TOP (owl:Thing or rdfs:Literal).
         * An object restriction is unqualified if it has a filler that is owl:Thing.
         * A data restriction is unqualified if it has a filler which is the top data type (rdfs:Literal).
         *
         * @return {@code true} if this restriction is qualified, or {@code false} if this restriction is unqualified.
         */
        boolean isQualified();
    }

    /*
     * ============================
     * Interfaces for Abstract CEs:
     * ============================
     */

    interface ComponentsCE<O extends OntObject> extends OntCE, Components<O> {
    }

    interface RestrictionCE extends OntCE {
    }

    interface ComponentRestrictionCE<O extends RDFNode, P extends OntPE> extends RestrictionCE, ONProperty<P>, Value<O> {
    }

    interface CardinalityRestrictionCE<O extends OntObject, P extends OntPE> extends Cardinality, ComponentRestrictionCE<O, P> {
    }

    interface NaryRestrictionCE<O extends OntObject, P extends OntPE> extends RestrictionCE, ONProperties<P>, Value<O> {
    }

    /*
     * =======================
     * Default common methods:
     * =======================
     */

    /**
     * Lists all individuals,
     * i.e. subjects from class-assertion statements {@code a rdf:type C}.
     *
     * @return Stream of {@link OntIndividual}s
     */
    default Stream<OntIndividual> individuals() {
        return getModel().statements(null, RDF.type, this)
                .map(OntStatement::getSubject).map(s -> s.as(OntIndividual.class));
    }

    /**
     * Lists all properties attached to the class in a {@code rdfs:domain} statement.
     * The property is considered as attached if
     * it and the class expression are both included in property domain axiom description:
     * <ul>
     * <li>{@code R rdfs:domain C} - {@code R} is a data property {@code C} - this class expression</li>
     * <li>{@code P rdfs:domain C} - {@code P} is an object property expression, {@code C} - this class expression</li>
     * <li>{@code A rdfs:domain U} - {@code A} is annotation property, {@code U} is IRI, this class expression</li>
     * </ul>
     *
     * @return Stream of {@link OntPE}s
     * @see OntPE#domain()
     */
    default Stream<OntPE> properties() {
        return getModel().statements(null, RDFS.domain, this)
                .map(OntStatement::getSubject)
                .filter(s -> s.canAs(OntPE.class))
                .map(s -> s.as(OntPE.class));
    }

    /**
     * Returns all super classes.
     *
     * @return Stream of {@link OntCE}s.
     */
    default Stream<OntCE> subClassOf() {
        return objects(RDFS.subClassOf, OntCE.class);
    }

    /**
     * Adds a super class.
     *
     * @param superClass {@link OntCE}
     * @return {@link OntStatement}
     */
    default OntStatement addSubClassOf(OntCE superClass) {
        return addStatement(RDFS.subClassOf, superClass);
    }

    /**
     * Removes a super class.
     *
     * @param superClass {@link OntCE}
     */
    default void removeSubClassOf(OntCE superClass) {
        remove(RDFS.subClassOf, superClass);
    }

    /**
     * Returns all disjoint classes.
     * The statement patter to search for is {@code C1 owl:disjointWith C2}.
     *
     * @return Stream of {@link OntCE}s
     * @see OntDisjoint.Classes
     */
    default Stream<OntCE> disjointWith() {
        return objects(OWL.disjointWith, OntCE.class);
    }

    /**
     * Adds a disjoint class.
     *
     * @param other {@link OntCE}
     * @return {@link OntStatement}
     * @see OntDisjoint.Classes
     */
    default OntStatement addDisjointWith(OntCE other) {
        return addStatement(OWL.disjointWith, other);
    }

    /**
     * Removes a disjoint class.
     *
     * @param other {@link OntCE}
     * @see OntDisjoint.Classes
     */
    default void removeDisjointWith(OntCE other) {
        remove(OWL.disjointWith, other);
    }

    /**
     * Lists all equivalent classes.
     *
     * @return Stream of {@link OntCE}s
     * @see OntDT#equivalentClass()
     */
    default Stream<OntCE> equivalentClass() {
        return objects(OWL.equivalentClass, OntCE.class);
    }

    /**
     * Adds new equivalent class.
     *
     * @param other {@link OntCE}
     * @return {@link OntStatement}
     * @see OntDT#addEquivalentClass(OntDR)
     */
    default OntStatement addEquivalentClass(OntCE other) {
        return addStatement(OWL.equivalentClass, other);
    }

    /**
     * Removes an equivalent class.
     *
     * @param other {@link OntCE}
     * @see OntDT#removeEquivalentClass(OntDR)
     */
    default void removeEquivalentClass(OntCE other) {
        remove(OWL.equivalentClass, other);
    }

}

