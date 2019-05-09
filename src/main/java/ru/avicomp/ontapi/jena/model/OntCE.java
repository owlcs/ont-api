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
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A common interface for any <b>C</b>lass <b>E</b>xpressions (both named and anonymous).
 * Examples of rdf-patterns see <a href='https://www.w3.org/TR/owl2-quick-reference/'>here</a>.
 * <p>
 * Created by szuev on 01.11.2016.
 *
 * @see OntClass
 * @see <a href='https://www.w3.org/TR/owl2-quick-reference/'>2.1 Class Expressions</a>
 * @see <a href='https://www.w3.org/TR/owl2-syntax/#Class_Expressions'>8 Class Expressions</a>
 */
public interface OntCE extends OntObject {

    /**
     * Creates an anonymous individual which is of this class-expression type.
     *
     * @return {@link OntIndividual.Anonymous}
     * @see OntIndividual#attachClass(OntCE)
     * @see #individuals()
     */
    OntIndividual.Anonymous createIndividual();

    /**
     * Creates a named individual which is of this class type.
     *
     * @param uri, String, not {@code null}
     * @return {@link OntIndividual.Named}
     * @see OntIndividual#attachClass(OntCE)
     * @see #individuals()
     */
    OntIndividual.Named createIndividual(String uri);

    /**
     * Answers a {@code Stream} over the class-expressions
     * for which this class expression is declared to be sub-class.
     * The return {@code Stream} is distinct and this instance is not included into it.
     * <p>
     * The flag {@code direct} allows some selectivity over the classes that appear in the {@code Stream}.
     * If it is {@code true} only direct sub-classes are returned,
     * and the method is equivalent to the method {@link #subClassOf()}
     * with except of some boundary cases (e.g. {@code <A> rdfs:subClassOf <A>}).
     * If it is {@code false}, the method returns all super classes recursively.
     * Consider the following scenario:
     * <pre>{@code
     *   :A rdfs:subClassOf :B .
     *   :B rdfs:subClassOf :C .
     * }</pre>
     * If the flag {@code direct} is {@code true},
     * the listing super classes for the class {@code A} will return only {@code B}.
     * And otherwise, if the flag {@code direct} is {@code false}, it will return {@code B} and also {@code C}.
     *
     * @param direct boolean: if {@code true}, only answers the directly adjacent classes in the super-class relation,
     *               otherwise answers all super-classes found in the {@code Graph} recursively
     * @return <b>distinct</b> {@code Stream} of super {@link OntCE class expression}s
     * @see #subClassOf()
     * @see #listSubClasses(boolean)
     * @since 1.4.0
     */
    Stream<OntCE> listSuperClasses(boolean direct);

    /**
     * Answer a {@code Stream} over all of the class expressions
     * that are declared to be sub-classes of this class expression.
     * The return {@code Stream} is distinct and this instance is not included into it.
     * The flag {@code direct} allows some selectivity over the classes that appear in the {@code Stream}.
     * Consider the following scenario:
     * <pre>{@code
     *   :B rdfs:subClassOf :A .
     *   :C rdfs:subClassOf :B .
     * }</pre>
     * If the flag {@code direct} is {@code true},
     * the listing sub classes for the class {@code A} will return only {@code B}.
     * And otherwise, if the flag {@code direct} is {@code false}, it will return {@code B} and also {@code C}.
     *
     * @param direct boolean: if {@code true}, only answers the directly adjacent classes in the sub-class relation,
     *               otherwise answers all sub-classes found in the {@code Graph} recursively
     * @return <b>distinct</b> {@code Stream} of sub {@link OntCE class expression}s
     * @see #listSuperClasses(boolean)
     * @since 1.4.0
     */
    Stream<OntCE> listSubClasses(boolean direct);

    /**
     * Creates a {@code HasKey} logical construction as {@link OntList ontology []-list}
     * of {@link OntDOP Object or Data Property Expression}s
     * that is attached to this Class Expression using the predicate {@link OWL#hasKey owl:hasKey}.
     * The resulting rdf-list will consist of all the elements of the specified collection
     * in the same order but with exclusion of duplicates.
     * Note: {@code null}s in collection will cause {@link OntJenaException.IllegalArgument exception}.
     * For additional information about {@code HasKey} logical construction see
     * <a href='https://www.w3.org/TR/owl2-syntax/#Keys'>9.5 Keys</a> specification.
     *
     * @param objectProperties {@link Collection} (preferably {@link Set})of {@link OntOPE object property expression}s
     * @param dataProperties   {@link Collection} (preferably {@link Set})of {@link OntNDP data property expression}s
     * @return {@link OntList} of {@link OntDOP}s
     * @since 1.3.0
     */
    OntList<OntDOP> createHasKey(Collection<OntOPE> objectProperties, Collection<OntNDP> dataProperties);

    /**
     * Creates a {@code HasKey} logical construction as {@link OntList ontology list}
     * and returns the statement {@code C owl:hasKey ( P1 ... Pm R1 ... Rn )}
     * to allow the subsequent addition of annotations.
     * About RDF Graph annotation specification see, for example,
     * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>.
     *
     * @param properties Array of {@link OntDOP}s without {@code null}s
     * @return {@link OntStatement} with a possibility to annotate
     * @see #addHasKeyStatement(Collection, Collection)
     * @see #addHasKey(OntDOP...)
     * @see #removeHasKey(RDFNode)
     * @see #clearHasKeys()
     * @since 1.4.0
     */
    OntStatement addHasKeyStatement(OntDOP... properties);

    /**
     * Lists all {@code HasKey} {@link OntList ontology []-list}s
     * that are attached to this class expression on predicate {@link OWL#hasKey owl:hasKey}.
     *
     * @return Stream of {@link OntList}s with parameter-type {@code OntDOP}
     * @since 1.3.0
     */
    Stream<OntList<OntDOP>> listHasKeys();

    /**
     * Deletes the given {@code HasKey} list including its annotations
     * with predicate {@link OWL#hasKey owl:hasKey} for this resource from its associated model.
     *
     * @param list {@link RDFNode} can be {@link OntList} or {@link RDFList}
     * @return <b>this</b> instance to allow cascading calls
     * @throws OntJenaException if the list is not found
     * @since 1.3.0
     */
    OntCE removeHasKey(RDFNode list);

    /**
     * Lists all individuals,
     * i.e. subjects from class-assertion statements {@code a rdf:type C}, where {@code C} is this class expression.
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
     * Lists all super classes for this class expression.
     * The search pattern is {@code C rdfs:subClassOf Ci},
     * where {@code C} is this instance, and {@code Ci} is one of the returned.
     *
     * @return Stream of {@link OntCE}s
     * @see #listSuperClasses(boolean)
     */
    default Stream<OntCE> subClassOf() {
        return objects(RDFS.subClassOf, OntCE.class);
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
     * Lists all equivalent classes.
     *
     * @return Stream of {@link OntCE}s
     * @see OntDT#equivalentClass()
     */
    default Stream<OntCE> equivalentClass() {
        return objects(OWL.equivalentClass, OntCE.class);
    }

    /**
     * Adds the given class as a super class
     * and returns the corresponding statement to provide the ability to add annotations.
     *
     * @param other {@link OntCE}, not {@code null}
     * @return {@link OntStatement} to allow the subsequent annotations addition
     * @see #addSuperClass(OntCE)
     * @see #removeSuperClass(Resource)
     * @since 1.4.0
     */
    default OntStatement addSubClassOfStatement(OntCE other) {
        return addStatement(RDFS.subClassOf, other);
    }

    /**
     * Adds the given class as a disjoint class
     * and returns the corresponding statement to provide the ability to add annotations.
     *
     * @param other {@link OntCE}, not {@code null}
     * @return {@link OntStatement} to allow the subsequent annotations addition
     * @see #addDisjointClass(OntCE)
     * @see #removeDisjointClass(Resource)
     * @see OntDisjoint.Classes
     */
    default OntStatement addDisjointWithStatement(OntCE other) {
        return addStatement(OWL.disjointWith, other);
    }

    /**
     * Adds the given class as a equivalent class
     * and returns the corresponding statement to provide the ability to add annotations.
     *
     * @param other {@link OntCE}, not {@code null}
     * @return {@link OntStatement} to allow the subsequent annotations addition
     * @see #addEquivalentClass(OntCE)
     * @see #removeEquivalentClass(Resource)
     * @see OntDT#addEquivalentClassStatement(OntDR)
     * @since 1.4.0
     */
    default OntStatement addEquivalentClassStatement(OntCE other) {
        return addStatement(OWL.equivalentClass, other);
    }

    /**
     * Creates an {@code owl:hasKey} statement returning root statement to allow the subsequent annotations adding.
     *
     * @param objectProperties the collection of {@link OntOPE}s
     * @param dataProperties   the collection of {@link OntNDP}s
     * @return {@link OntStatement}
     * @see #addHasKeyStatement(OntDOP...)
     * @see #addHasKey(OntDOP...)
     * @see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>
     * @since 1.4.0
     */
    default OntStatement addHasKeyStatement(Collection<OntOPE> objectProperties, Collection<OntNDP> dataProperties) {
        return createHasKey(objectProperties, dataProperties).getRoot();
    }

    /**
     * Adds the given class as a super class
     * and returns this class expression instance to allow cascading calls.
     *
     * @param other {@link OntCE}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addSubClassOfStatement(OntCE)
     * @see #removeSuperClass(Resource)
     * @since 1.4.0
     */
    default OntCE addSuperClass(OntCE other) {
        addSubClassOfStatement(other);
        return this;
    }

    /**
     * Adds the given class as a disjoint class
     * and returns this class expression instance to allow cascading calls.
     *
     * @param other {@link OntCE}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addDisjointWithStatement(OntCE)
     * @see #removeDisjointClass(Resource)
     * @since 1.4.0
     */
    default OntCE addDisjointClass(OntCE other) {
        addDisjointWithStatement(other);
        return this;
    }

    /**
     * Adds a new equivalent class.
     *
     * @param other {@link OntCE}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addEquivalentClassStatement(OntCE)
     * @see #removeDisjointClass(Resource)
     */
    default OntCE addEquivalentClass(OntCE other) {
        addEquivalentClassStatement(other);
        return this;
    }

    /**
     * Creates an {@code owl:hasKey} statement returning this class to allow cascading calls.
     *
     * @param objectProperties the collection of {@link OntOPE}s
     * @param dataProperties   the collection of {@link OntNDP}s
     * @return <b>this</b> instance to allow cascading calls
     * @see #addHasKeyStatement(Collection, Collection)
     * @see #addHasKey(OntDOP...)
     */
    default OntCE addHasKey(Collection<OntOPE> objectProperties, Collection<OntNDP> dataProperties) {
        addHasKeyStatement(objectProperties, dataProperties);
        return this;
    }

    /**
     * Creates an {@code owl:hasKey} statement returning this class to allow cascading calls.
     *
     * @param properties Array of {@link OntDOP}s without {@code null}s
     * @return <b>this</b> instance to allow cascading calls
     * @see #addHasKeyStatement(OntDOP...)
     * @see #addHasKey(Collection, Collection)
     * @see #removeHasKey(RDFNode)
     * @see #clearHasKeys()
     * @since 1.3.0
     */
    default OntCE addHasKey(OntDOP... properties) {
        addHasKeyStatement(properties);
        return this;
    }

    /**
     * Removes a super-class relationship for the given resource including all possible annotations.
     * No-op in case no match found.
     * Removes all {@link RDFS#subClassOf rdfs:subClassOf} statements with all their annotations
     * in case {@code null} is specified.
     *
     * @param other {@link Resource} or {@code null} to remove all {@code rdfs:subClassOf} statements
     * @return <b>this</b> instance to allow cascading calls
     * @see #addSubClassOfStatement(OntCE)
     * @see #addSuperClass(OntCE)
     * @since 1.4.0
     */
    default OntCE removeSuperClass(Resource other) {
        remove(RDFS.subClassOf, other);
        return this;
    }

    /**
     * Removes the specified disjoint class resource.
     * No-op in case no match found.
     * Removes all {@link OWL#disjointWith owl:disjointWith} statements with all their annotations
     * in case {@code null} is specified.
     *
     * @param other {@link Resource}, or {@code null} to remove all disjoint classes
     * @return <b>this</b> instance to allow cascading calls
     * @see #addDisjointWithStatement(OntCE)
     * @see #addDisjointClass(OntCE)
     * @see OntDisjoint.Classes
     * @since 1.4.0
     */
    default OntCE removeDisjointClass(Resource other) {
        remove(OWL.disjointWith, other);
        return this;
    }

    /**
     * Removes the given equivalent class resource including the statement's annotations.
     * No-op in case no match found.
     * Removes all {@link OWL#equivalentClass owl:equivalentClass} statements with all their annotations
     * in case {@code null} is specified.
     *
     * @param other {@link Resource}, or {@code null} to remove all equivalent classes
     * @return <b>this</b> instance to allow cascading calls
     * @see #addEquivalentClassStatement(OntCE)
     * @see #addEquivalentClass(OntCE)
     * @see OntDT#removeEquivalentClass(Resource)
     */
    default OntCE removeEquivalentClass(Resource other) {
        remove(OWL.equivalentClass, other);
        return this;
    }

    /**
     * Deletes all {@code HasKey} []-list including its annotations
     * with predicate {@link OWL#hasKey owl:hasKey} for this resource from its associated model.
     *
     * @return <b>this</b> instance to allow cascading calls
     * @throws OntJenaException if the list is not found
     * @since 1.3.0
     */
    default OntCE clearHasKeys() {
        listHasKeys().collect(Collectors.toList()).forEach(this::removeHasKey);
        return this;
    }

    /**
     * Finds a {@code HasKey} logical construction
     * attached to this class expression by the specified rdf-node in the form of {@link OntList}.
     *
     * @param list {@link RDFNode}
     * @return Optional around {@link OntList} of {@link OntDOP data and object property expression}s
     * @since 1.3.0
     */
    default Optional<OntList<OntDOP>> findHasKey(RDFNode list) {
        try (Stream<OntList<OntDOP>> res = listHasKeys().filter(r -> Objects.equals(r, list))) {
            return res.findFirst();
        }
    }

    /**
     * Lists all key properties.
     * I.e. returns all object- and datatype- properties which belong to
     * the {@code C owl:hasKey ( P1 ... Pm R1 ... Rn )} statements,
     * where {@code C} is this class expression,
     * {@code Pi} is a property expression, and {@code Ri} is a data(-type) property.
     * If there are several []-lists in the model that satisfy these conditions,
     * all their content will be merged into the one distinct stream.
     *
     * @return <b>distinct</b> Stream of {@link OntOPE object} and {@link OntNDP data} properties
     * @see #listHasKeys()
     * @since 1.4.0
     */
    default Stream<OntDOP> fromHasKey() {
        return listHasKeys().flatMap(OntList::members).distinct();
    }

    /**
     * Adds a super class.
     *
     * @param superClass {@link OntCE}
     * @return {@link OntStatement}
     * @deprecated since 1.4.0: use the method {@link #addSubClassOfStatement(OntCE)} instead
     */
    @Deprecated
    default OntStatement addSubClassOf(OntCE superClass) {
        return addSubClassOfStatement(superClass);
    }

    /**
     * Removes the given super class.
     *
     * @param superClass {@link OntCE}, or {@code null} to remove all super classes
     * @deprecated since 1.4.0: use the method {@link #removeSuperClass(Resource)} instead
     */
    @Deprecated
    default void removeSubClassOf(OntCE superClass) {
        removeSuperClass(superClass);
    }

    /**
     * Adds a disjoint class.
     *
     * @param other {@link OntCE}
     * @return {@link OntStatement}
     * @deprecated since 1.4.0: use the method {@link #addDisjointWithStatement(OntCE)} instead
     */
    @Deprecated
    default OntStatement addDisjointWith(OntCE other) {
        return addDisjointWithStatement(other);
    }

    /**
     * Removes the specified disjoint class.
     *
     * @param other {@link OntCE}, or {@code null} to remove all disjoint classes
     * @see OntDisjoint.Classes
     * @deprecated since 1.4.0: use the method {@link #removeDisjointClass(Resource)} instead
     */
    @Deprecated
    default void removeDisjointWith(OntCE other) {
        removeDisjointClass(other);
    }

    /*
     * ============================
     * All known Class Expressions:
     * ============================
     */

    interface ObjectSomeValuesFrom extends ComponentRestrictionCE<OntCE, OntOPE>,
            SetValue<OntCE, ObjectSomeValuesFrom>, SetONProperty<OntOPE, ObjectSomeValuesFrom> {
    }

    interface DataSomeValuesFrom extends ComponentRestrictionCE<OntDR, OntNDP>,
            SetValue<OntDR, DataSomeValuesFrom>, SetONProperty<OntNDP, DataSomeValuesFrom> {
    }

    interface ObjectAllValuesFrom extends ComponentRestrictionCE<OntCE, OntOPE>,
            SetValue<OntCE, ObjectAllValuesFrom>, SetONProperty<OntOPE, ObjectAllValuesFrom> {
    }

    interface DataAllValuesFrom extends ComponentRestrictionCE<OntDR, OntNDP>,
            SetValue<OntDR, DataAllValuesFrom>, SetONProperty<OntNDP, DataAllValuesFrom> {
    }

    interface ObjectHasValue extends ComponentRestrictionCE<OntIndividual, OntOPE>,
            SetValue<OntIndividual, ObjectHasValue>, SetONProperty<OntOPE, ObjectHasValue> {
    }

    interface DataHasValue extends ComponentRestrictionCE<Literal, OntNDP>,
            SetValue<Literal, DataHasValue>, SetONProperty<OntNDP, DataHasValue> {
    }

    interface ObjectMinCardinality extends CardinalityRestrictionCE<OntCE, OntOPE>,
            SetValue<OntCE, ObjectMinCardinality>,
            SetONProperty<OntOPE, ObjectMinCardinality>,
            SetCardinality<ObjectMinCardinality> {
    }

    interface DataMinCardinality extends CardinalityRestrictionCE<OntDR, OntNDP>,
            SetValue<OntDR, DataMinCardinality>,
            SetONProperty<OntNDP, DataMinCardinality>,
            SetCardinality<DataMinCardinality> {
    }

    interface ObjectMaxCardinality extends CardinalityRestrictionCE<OntCE, OntOPE>,
            SetValue<OntCE, ObjectMaxCardinality>,
            SetONProperty<OntOPE, ObjectMaxCardinality>,
            SetCardinality<ObjectMaxCardinality> {
    }

    interface DataMaxCardinality extends CardinalityRestrictionCE<OntDR, OntNDP>,
            SetValue<OntDR, DataMaxCardinality>,
            SetONProperty<OntNDP, DataMaxCardinality>,
            SetCardinality<DataMaxCardinality> {
    }

    interface ObjectCardinality extends CardinalityRestrictionCE<OntCE, OntOPE>,
            SetValue<OntCE, ObjectCardinality>,
            SetONProperty<OntOPE, ObjectCardinality>,
            SetCardinality<ObjectCardinality> {
    }

    interface DataCardinality extends CardinalityRestrictionCE<OntDR, OntNDP>,
            SetValue<OntDR, DataCardinality>,
            SetONProperty<OntNDP, DataCardinality>,
            SetCardinality<DataCardinality> {
    }

    interface HasSelf extends PropertyRestrictionCE<OntOPE>, SetONProperty<OntOPE, HasSelf> {
    }

    interface UnionOf extends ComponentsCE<OntCE>, SetComponents<OntCE, UnionOf> {
    }

    interface OneOf extends ComponentsCE<OntIndividual>, SetComponents<OntIndividual, OneOf> {
    }

    interface IntersectionOf extends ComponentsCE<OntCE>, SetComponents<OntCE, IntersectionOf> {
    }

    interface ComplementOf extends OntCE, HasValue<OntCE>, SetValue<OntCE, ComplementOf> {
    }

    interface NaryDataAllValuesFrom extends NaryRestrictionCE<OntDR, OntNDP>,
            SetValue<OntDR, NaryDataAllValuesFrom>, SetONProperties<NaryDataAllValuesFrom> {
    }

    interface NaryDataSomeValuesFrom extends NaryRestrictionCE<OntDR, OntNDP>,
            SetValue<OntDR, NaryDataSomeValuesFrom>, SetONProperties<NaryDataSomeValuesFrom> {
    }

    /*
     * ===========================
     * Abstract class expressions:
     * ===========================
     */

    interface ComponentsCE<O extends OntObject> extends OntCE, HasRDFNodeList<O> {
    }

    interface CardinalityRestrictionCE<O extends OntObject, P extends OntDOP>
            extends HasCardinality, ComponentRestrictionCE<O, P> {
    }

    interface ComponentRestrictionCE<O extends RDFNode, P extends OntDOP>
            extends PropertyRestrictionCE<P>, HasValue<O> {
    }

    interface NaryRestrictionCE<O extends OntObject, P extends OntDOP>
            extends RestrictionCE, HasONProperties<P>, HasValue<O> {
    }

    // todo: add a factory
    interface PropertyRestrictionCE<P extends OntDOP> extends RestrictionCE, HasONProperty<P> {
    }

    // todo: the meaning has been changed -> fix
    interface RestrictionCE extends OntCE {
    }
}

