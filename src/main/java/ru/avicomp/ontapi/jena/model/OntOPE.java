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
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A common interface for any Ontology <b>O</b>bject <b>P</b>roperty <b>E</b>xpression.
 * In OWL2 there are two types of object property expressions:
 * named object property (entity) and InverseOf anonymous property expression.
 * Range values for this property expression are restricted to individuals
 * (as distinct from datatype valued {@link OntNDP properties}).
 * <p>
 * Created by @szuev on 08.11.2016.
 */
public interface OntOPE extends OntDOP {

    /**
     * {@inheritDoc}
     * Note: a {@code PropertyChain} is not included into consideration:
     * even this property is a member of some chain ({@code P owl:propertyChainAxiom ( P1 ... Pn )},
     * where {@code Pj} is this property), it does not mean it has the same super property ({@code P}).
     *
     * @return <b>distinct</b> {@code Stream} of object property expressions
     */
    Stream<OntOPE> listSuperProperties(boolean direct);

    /**
     * {@inheritDoc}
     * Note: a {@code PropertyChain} is not included into consideration,
     * even this property is a super property of some chain ({@code P owl:propertyChainAxiom ( P1 ... Pn )},
     * where {@code P} is this property), each of chain members is not considered as sub property of this property.
     *
     * @return <b>distinct</b> {@code Stream} of object property expressions
     */
    Stream<OntOPE> listSubProperties(boolean direct);

    /**
     * Adds a negative property assertion ontology object.
     *
     * @param source {@link OntIndividual}
     * @param target {@link OntIndividual}
     * @return {@link OntNPA.ObjectAssertion}
     * @see OntNDP#addNegativeAssertion(OntIndividual, Literal)
     */
    OntNPA.ObjectAssertion addNegativeAssertion(OntIndividual source, OntIndividual target);

    /**
     * Creates a property chain as {@link OntList ontology []-list} of {@link OntOPE Object Property Expression}s
     * that is attached to this Object Property Expression
     * using the predicate {@link OWL#propertyChainAxiom owl:propertyChainAxiom}.
     * The resulting rdf-list will consist of all the elements of the specified collection
     * in the same order with the possibility of duplication.
     * Note: {@code null}s in collection will cause {@link OntJenaException.IllegalArgument exception}.
     * For additional information about {@code PropertyChain} logical construction see
     * <a href='https://www.w3.org/TR/owl2-syntax/#Object_Subproperties'>9.2.1 Object Subproperties</a> specification.
     *
     * @param properties {@link Collection} (preferably {@link List}) of {@link OntOPE object property expression}s
     * @return {@link OntList} of {@link OntOPE}s
     * @since 1.3.0
     */
    OntList<OntOPE> createPropertyChain(Collection<OntOPE> properties);

    /**
     * Lists all property chain {@link OntList ontology list}s that are attached to this Object Property Expression
     * on predicate {@link OWL#propertyChainAxiom owl:propertyChainAxiom}.
     *
     * @return Stream of {@link OntList}s with parameter-type {@code OntOPE}
     * @since 1.3.0
     */
    Stream<OntList<OntOPE>> listPropertyChains();

    /**
     * Deletes the given property chain list including its annotations
     * with predicate {@link OWL#propertyChainAxiom owl:propertyChainAxiom} for this resource from its associated model.
     *
     * @param list {@link RDFNode} can be {@link OntList} or {@link RDFList}
     * @throws OntJenaException if the list is not found
     * @since 1.3.0
     */
    void removePropertyChain(RDFNode list) throws OntJenaException;

    /**
     * Returns all associated negative object property assertions.
     *
     * @return Stream of {@link OntNPA.ObjectAssertion}s
     * @see OntNDP#negativeAssertions()
     */
    default Stream<OntNPA.ObjectAssertion> negativeAssertions() {
        return getModel().ontObjects(OntNPA.ObjectAssertion.class).filter(a -> OntOPE.this.equals(a.getProperty()));
    }

    /**
     * Returns all associated negative object property assertions for the specified source individual.
     *
     * @param source {@link OntIndividual}
     * @return Stream of {@link OntNPA.ObjectAssertion}s
     * @see OntNDP#negativeAssertions(OntIndividual)
     */

    default Stream<OntNPA.ObjectAssertion> negativeAssertions(OntIndividual source) {
        return negativeAssertions()
                .filter(a -> a.getSource().equals(source));
    }

    /**
     * Finds a {@code PropertyChain} logical construction
     * attached to this property by the specified rdf-node in the form of {@link OntList}.
     *
     * @param list {@link RDFNode}
     * @return Optional around {@link OntList} of {@link OntOPE object property expression}s
     * @since 1.3.0
     */
    default Optional<OntList<OntOPE>> findPropertyChain(RDFNode list) {
        try (Stream<OntList<OntOPE>> res = listPropertyChains().filter(r -> Objects.equals(r, list))) {
            return res.findFirst();
        }
    }

    /**
     * Deletes all property chain lists including their annotations
     * with predicate {@link OWL#propertyChainAxiom owl:propertyChainAxiom} for this resource from its associated model.
     *
     * @since 1.3.0
     */
    default void clearPropertyChains() {
        listPropertyChains().collect(Collectors.toSet()).forEach(this::removePropertyChain);
    }

    /**
     * Lists all members from the right part of statement {@code P owl:propertyChainAxiom ( P1 ... Pn )},
     * where {@code P} is this property.
     * Note: the result ignores repetitions, e.g. for
     * {@code SubObjectPropertyOf( ObjectPropertyChain( :hasParent :hasParent ) :hasGrandparent )},
     * it returns only {@code :hasParent} property.
     *
     * @return <b>distinct</b> Stream of all super {@link OntOPE object properties},
     * possible empty in case of nil-list or if there is no property-chains at all
     * @see #listPropertyChains()
     * @since 1.4.0
     */
    default Stream<OntOPE> fromPropertyChain() {
        return listPropertyChains().flatMap(OntList::members).distinct();
    }

    /**
     * Creates a property chain {@link OntList ontology list}
     * and returns statement {@code P owl:propertyChainAxiom ( P1 ... Pn )} to allow the addition of annotations.
     * About RDF Graph annotation specification see, for example,
     * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>.
     *
     * @param properties Array of {@link OntOPE}s without {@code null}s
     * @return {@link OntStatement}
     * @see #createPropertyChain(Collection)
     * @since 1.3.0
     */
    default OntStatement addSuperPropertyOf(OntOPE... properties) {
        return createPropertyChain(Arrays.asList(properties)).getRoot();
    }

    /**
     * Adds new sub-property-of chain.
     *
     * @param properties Collection of {@link OntOPE}s
     * @return the {@link OntStatement} ({@code _:this owl:propertyChainAxiom ( ... )})
     * @see #createPropertyChain(Collection)
     * @see #addSuperPropertyOf(OntOPE...)
     * @see #createPropertyChain(Collection)
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#a_SubObjectPropertyOfChain'>9.2.1 Object Subproperties</a>
     */
    default OntStatement addSuperPropertyOf(Collection<OntOPE> properties) {
        return createPropertyChain(new ArrayList<>(properties)).getRoot();
    }

    /**
     * Returns all ranges.
     * The statement pattern is {@code P rdfs:range C}, where {@code P} is this object property,
     * and {@code C} is one of the return class expressions.
     *
     * @return Stream of {@link OntCE}s
     */
    @Override
    default Stream<OntCE> range() {
        return objects(RDFS.range, OntCE.class);
    }

    /**
     * Adds a property range (i.e. {@code P rdfs:range C} statement).
     *
     * @param range {@link OntCE}, not {@code null}
     * @return {@link OntStatement} to allow processing annotations
     * @see #addRange(OntCE)
     */
    default OntStatement addRangeStatement(OntCE range) {
        return addStatement(RDFS.range, range);
    }

    /**
     * Adds a statement with the {@link RDFS#range} as predicate
     * and the specified {@link OntCE class expression} as an object.
     *
     * @param range {@link OntCE}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addRangeStatement(OntCE)
     */
    default OntOPE addRange(OntCE range) {
        addRangeStatement(range);
        return this;
    }

    /**
     * Adds a statement with the {@link RDFS#domain} as predicate
     * and the specified {@link OntCE class expression} as an object.
     *
     * @param domain {@link OntCE}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addDomainStatement(OntCE)
     */
    default OntOPE addDomain(OntCE domain) {
        addDomainStatement(domain);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    default OntOPE removeDomain(Resource domain) {
        remove(RDFS.domain, domain);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    default OntOPE removeRange(Resource range) {
        remove(RDFS.range, range);
        return this;
    }

    /**
     * Lists all direct super properties, the pattern is {@code P1 rdfs:subPropertyOf P2}.
     *
     * @return Stream of {@link OntOPE}s
     * @see #addSubPropertyOf(OntOPE)
     * @see OntPE#removeSubPropertyOf(Resource)
     * @see #listSuperProperties(boolean)
     */
    @Override
    default Stream<OntOPE> subPropertyOf() {
        return objects(RDFS.subPropertyOf, OntOPE.class);
    }

    /**
     * Add a super-property of this property (i.e. {@code _:this rdfs:subPropertyOf @superProperty} statement).
     *
     * @param superProperty {@link OntOPE}
     * @return {@link OntStatement}
     */
    default OntStatement addSubPropertyOf(OntOPE superProperty) {
        return addStatement(RDFS.subPropertyOf, superProperty);
    }

    /**
     * Returns disjoint properties (statement: {@code P1 owl:propertyDisjointWith P2}).
     *
     * @return Stream of {@link OntOPE}s
     * @see OntNDP#disjointWith()
     * @see OntDisjoint.ObjectProperties
     */
    default Stream<OntOPE> disjointWith() {
        return objects(OWL.propertyDisjointWith, OntOPE.class);
    }

    /**
     * Adds a disjoint object property (i.e. {@code _:this owl:propertyDisjointWith @other} statement).
     *
     * @param other {@link OntOPE}
     * @return {@link OntStatement}
     * @see OntNDP#addDisjointWith(OntNDP)
     * @see OntDisjoint.ObjectProperties
     */
    default OntStatement addDisjointWith(OntOPE other) {
        return addStatement(OWL.propertyDisjointWith, other);
    }

    /**
     * Clears all {@code P1 owl:propertyDisjointWith P2} statements
     * for the specified object property (subject, {@code P1}).
     *
     * @param other {@link OntOPE}
     * @see OntNDP#removeDisjointWith(OntNDP)
     * @see OntDisjoint.ObjectProperties
     */
    default void removeDisjointWith(OntOPE other) {
        remove(OWL.propertyDisjointWith, other);
    }

    /**
     * Returns all equivalent object properties
     * (i.e. {@code Pi owl:equivalentProperty Pj}, where {@code Pi} - this property).
     *
     * @return Stream of {@link OntOPE}s.
     * @see OntNDP#equivalentProperty()
     */
    default Stream<OntOPE> equivalentProperty() {
        return objects(OWL.equivalentProperty, OntOPE.class);
    }

    /**
     * Adds new {@link OWL#equivalentProperty owl:equivalentProperty} statement for this and the given properties.
     *
     * @param other {@link OntOPE}
     * @return {@link OntStatement}
     * @see OntNDP#addEquivalentProperty(OntNDP)
     */
    default OntStatement addEquivalentProperty(OntOPE other) {
        return addStatement(OWL.equivalentProperty, other);
    }

    /**
     * Removes all equivalent-property statements for this and the specified object properties.
     *
     * @param other {@link OntOPE}
     * @see OntNDP#removeEquivalentProperty(OntNDP)
     */
    default void removeEquivalentProperty(OntOPE other) {
        remove(OWL.equivalentProperty, other);
    }

    /**
     * Gets the <b>first</b> object property
     * from the right part of the statements {@code _:x owl:inverseOf PN} or {@code P1 owl:inverseOf P2}.
     * What exactly is the first statement is defined at the level of graph; in general it is unpredictable.
     *
     * @return {@link OntOPE} or {@code null}
     * @see #inverseOf()
     */
    default OntOPE getInverseOf() {
        try (Stream<OntOPE> res = inverseOf()) {
            return res.findFirst().orElse(null);
        }
    }

    /**
     * Lists all object properties from the right part of statement {@code _:this owl:inverseOf P}.
     *
     * @return Stream of {@link OntOPE}s.
     */
    default Stream<OntOPE> inverseOf() {
        return objects(OWL.inverseOf, OntOPE.class);
    }

    /**
     * Adds new inverse-of statement.
     *
     * @param other {@link OntOPE}
     * @return {@link OntStatement}
     */
    default OntStatement addInverseOf(OntOPE other) {
        return addStatement(OWL.inverseOf, other);
    }

    /**
     * Removes the statement with predicate {@code owl:inverseOf} and the given object property as object.
     *
     * @param other {@link OntOPE}
     */
    default void removeInverseOf(OntOPE other) {
        remove(OWL.inverseOf, other);
    }

    /**
     * @return true iff it is an inverse functional property
     */
    default boolean isInverseFunctional() {
        return hasType(OWL.InverseFunctionalProperty);
    }

    /**
     * To add or remove {@code P rdf:type owl:InverseFunctionalProperty} statement.
     *
     * @param inverseFunctional true if should be inverse functional
     */
    void setInverseFunctional(boolean inverseFunctional);

    /**
     * @return true iff it is a transitive property
     */
    default boolean isTransitive() {
        return hasType(OWL.TransitiveProperty);
    }

    /**
     * To add or remove {@code P rdf:type owl:TransitiveProperty} statement.
     *
     * @param transitive true if should be transitive
     */
    void setTransitive(boolean transitive);

    /**
     * @return true iff it is a symmetric property
     */
    default boolean isSymmetric() {
        return hasType(OWL.SymmetricProperty);
    }

    /**
     * To add or remove {@code P rdf:type owl:SymmetricProperty} statement.
     *
     * @param symmetric true if should be symmetric
     */
    void setSymmetric(boolean symmetric);

    /**
     * @return true iff it is an asymmetric property
     */
    default boolean isAsymmetric() {
        return hasType(OWL.AsymmetricProperty);
    }

    /**
     * To add or remove {@code P rdf:type owl:AsymmetricProperty} statement.
     *
     * @param asymmetric true if should be asymmetric
     */
    void setAsymmetric(boolean asymmetric);

    /**
     * @return true iff it is a reflexive property
     */
    default boolean isReflexive() {
        return hasType(OWL.ReflexiveProperty);
    }

    /**
     * To add or remove {@code P rdf:type owl:ReflexiveProperty} statement.
     *
     * @param reflexive true if should be reflexive
     */
    void setReflexive(boolean reflexive);

    /**
     * @return true iff it is an irreflexive property
     */
    default boolean isIrreflexive() {
        return hasType(OWL.IrreflexiveProperty);
    }

    /**
     * To add or remove {@code P rdf:type owl:IrreflexiveProperty} statement.
     *
     * @param irreflexive true if should be irreflexive
     */
    void setIrreflexive(boolean irreflexive);

    /**
     * Represents a <a href="http://www.w3.org/TR/owl2-syntax/#Inverse_Object_Properties">ObjectInverseOf</a>.
     * Anonymous triple {@code _:x owl:inverseOf PN} which is also object property expression.
     */
    interface Inverse extends OntOPE {
        OntNOP getDirect();
    }
}
