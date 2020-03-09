/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
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

import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A base abstraction for any Class Expressions (both named and anonymous).
 * <p>
 * Created by szuev on 01.11.2016.
 *
 * @see Named - an OWL Class
 * @see <a href='https://www.w3.org/TR/owl2-quick-reference/#Class_Expressions'>2.1 Class Expressions</a>
 * @see <a href='https://www.w3.org/TR/owl2-syntax/#Class_Expressions'>8 Class Expressions</a>
 */
public interface OntClass extends OntObject, AsNamed<OntClass.Named> {

    /**
     * Answers a {@code Stream} over the class-expressions
     * for which this class expression is declared to be sub-class.
     * The return {@code Stream} is distinct and this instance is not included into it.
     * <p>
     * The flag {@code direct} allows some selectivity over the classes that appear in the {@code Stream}.
     * If it is {@code true} only direct sub-classes are returned,
     * and the method is equivalent to the method {@link #superClasses()}
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
     * @return <b>distinct</b> {@code Stream} of super {@link OntClass class expression}s
     * @see #superClasses()
     * @see #subClasses(boolean)
     */
    Stream<OntClass> superClasses(boolean direct);

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
     * @return <b>distinct</b> {@code Stream} of sub {@link OntClass class expression}s
     * @see #superClasses(boolean)
     */
    Stream<OntClass> subClasses(boolean direct);

    /**
     * Lists all {@code HasKey} {@link OntList ontology []-list}s
     * that are attached to this class expression on predicate {@link OWL#hasKey owl:hasKey}.
     *
     * @return {@code Stream} of {@link OntList}s with parameter-type {@code OntDOP}
     */
    Stream<OntList<OntRealProperty>> hasKeys();

    /**
     * Creates an anonymous individual which is of this class-expression type.
     *
     * @return {@link OntIndividual.Anonymous}
     * @see OntIndividual#attachClass(OntClass)
     * @see #individuals()
     */
    OntIndividual.Anonymous createIndividual();

    /**
     * Creates a named individual which is of this class type.
     *
     * @param uri, String, not {@code null}
     * @return {@link OntIndividual.Named}
     * @see OntIndividual#attachClass(OntClass)
     * @see #individuals()
     */
    OntIndividual.Named createIndividual(String uri);

    /**
     * Creates a {@code HasKey} logical construction as {@link OntList ontology []-list}
     * of {@link OntRealProperty Object or Data Property Expression}s
     * that is attached to this Class Expression using the predicate {@link OWL#hasKey owl:hasKey}.
     * The resulting rdf-list will consist of all the elements of the specified collection
     * in the same order but with exclusion of duplicates.
     * Note: {@code null}s in collection will cause {@link OntJenaException.IllegalArgument exception}.
     * For additional information about {@code HasKey} logical construction see
     * <a href='https://www.w3.org/TR/owl2-syntax/#Keys'>9.5 Keys</a> specification.
     *
     * @param objectProperties {@link Collection} (preferably {@link Set})
     *                         of {@link OntObjectProperty object property expression}s
     * @param dataProperties   {@link Collection} (preferably {@link Set})
     *                         of {@link OntDataProperty data property expression}s
     * @return {@link OntList} of {@link OntRealProperty}s
     * @see #addHasKey(Collection, Collection)
     */
    OntList<OntRealProperty> createHasKey(Collection<OntObjectProperty> objectProperties,
                                          Collection<OntDataProperty> dataProperties);

    /**
     * Creates a {@code HasKey} logical construction as {@link OntList ontology list}
     * and returns the statement {@code C owl:hasKey ( P1 ... Pm R1 ... Rn )}
     * to allow the subsequent addition of annotations.
     * About RDF Graph annotation specification see, for example,
     * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>.
     *
     * @param properties Array of {@link OntRealProperty}s without {@code null}s
     * @return {@link OntStatement} with a possibility to annotate
     * @see #addHasKeyStatement(Collection, Collection)
     * @see #addHasKey(OntRealProperty...)
     * @see #removeHasKey(Resource)
     * @see #clearHasKeys()
     */
    OntStatement addHasKeyStatement(OntRealProperty... properties);

    /**
     * Deletes the given {@code HasKey} list including its annotations.
     *
     * @param list {@link Resource} can be {@link OntList} or {@link RDFList}
     * @return <b>this</b> instance to allow cascading calls
     * @throws OntJenaException if the list is not found
     */
    OntClass removeHasKey(Resource list);

    @Override
    default Named asNamed() {
        return as(Named.class);
    }

    /**
     * Lists all individuals,
     * i.e. subjects from class-assertion statements {@code a rdf:type C}, where {@code C} is this class expression.
     *
     * @return {@code Stream} of {@link OntIndividual}s
     */
    default Stream<OntIndividual> individuals() {
        return getModel().statements(null, RDF.type, this).map(s -> s.getSubject(OntIndividual.class));
    }

    /**
     * Lists all properties attached to this class in a {@code rdfs:domain} statement.
     * The property is considered as attached if
     * the property and the class expression are both included in the property domain axiom statement:
     * <ul>
     * <li>{@code R rdfs:domain C} - {@code R} is a data property, {@code C} - this class expression</li>
     * <li>{@code P rdfs:domain C} - {@code P} is an object property expression, {@code C} - this class expression</li>
     * <li>{@code A rdfs:domain U} - {@code A} is annotation property, {@code U} is IRI (this class expression)</li>
     * </ul>
     *
     * @return {@code Stream} of {@link OntProperty}s
     * @see OntProperty#domains()
     */
    default Stream<OntProperty> properties() {
        return getModel().statements(null, RDFS.domain, this)
                .map(s -> s.getSubject().getAs(OntProperty.class))
                .filter(Objects::nonNull);
    }

    /**
     * Lists all super classes for this class expression.
     * The search pattern is {@code C rdfs:subClassOf Ci},
     * where {@code C} is this instance, and {@code Ci} is one of the returned.
     *
     * @return {@code Stream} of {@link OntClass}s
     * @see #superClasses(boolean)
     */
    default Stream<OntClass> superClasses() {
        return objects(RDFS.subClassOf, OntClass.class);
    }

    /**
     * Returns all disjoint classes.
     * The statement patter to search for is {@code C1 owl:disjointWith C2}.
     *
     * @return {@code Stream} of {@link OntClass}s
     * @see OntDisjoint.Classes
     */
    default Stream<OntClass> disjointClasses() {
        return objects(OWL.disjointWith, OntClass.class);
    }

    /**
     * Lists all equivalent classes.
     * The statement patter to search for is {@code C1 owl:equivalentClass C2}.
     *
     * @return {@code Stream} of {@link OntClass}s
     * @see OntDataRange.Named#equivalentClasses()
     */
    default Stream<OntClass> equivalentClasses() {
        return objects(OWL.equivalentClass, OntClass.class);
    }

    /**
     * Adds the given class as a super class
     * and returns the corresponding statement to provide the ability to add annotations.
     *
     * @param other {@link OntClass}, not {@code null}
     * @return {@link OntStatement} to allow the subsequent annotations addition
     * @see #addSuperClass(OntClass)
     * @see #removeSuperClass(Resource)
     */
    default OntStatement addSubClassOfStatement(OntClass other) {
        return addStatement(RDFS.subClassOf, other);
    }

    /**
     * Adds the given class as a disjoint class
     * and returns the corresponding statement to provide the ability to add annotations.
     *
     * @param other {@link OntClass}, not {@code null}
     * @return {@link OntStatement} to allow the subsequent annotations addition
     * @see #addDisjointClass(OntClass)
     * @see #removeDisjointClass(Resource)
     * @see OntDisjoint.Classes
     */
    default OntStatement addDisjointWithStatement(OntClass other) {
        return addStatement(OWL.disjointWith, other);
    }

    /**
     * Adds the given class as a equivalent class
     * and returns the corresponding statement to provide the ability to add annotations.
     *
     * @param other {@link OntClass}, not {@code null}
     * @return {@link OntStatement} to allow the subsequent annotations addition
     * @see #addEquivalentClass(OntClass)
     * @see #removeEquivalentClass(Resource)
     * @see OntDataRange.Named#addEquivalentClassStatement(OntDataRange)
     */
    default OntStatement addEquivalentClassStatement(OntClass other) {
        return addStatement(OWL.equivalentClass, other);
    }

    /**
     * Creates an {@code owl:hasKey} statement returning root statement to allow the subsequent annotations adding.
     *
     * @param objectProperties the collection of {@link OntObjectProperty}s, not {@code null} and cannot contain {@code null}s
     * @param dataProperties   the collection of {@link OntDataProperty}s, not {@code null} and cannot contain {@code null}s
     * @return {@link OntStatement} to allow the subsequent annotations addition
     * @see #addHasKeyStatement(OntRealProperty...)
     * @see #addHasKey(OntRealProperty...)
     * @see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>
     */
    default OntStatement addHasKeyStatement(Collection<OntObjectProperty> objectProperties, Collection<OntDataProperty> dataProperties) {
        return createHasKey(objectProperties, dataProperties).getMainStatement();
    }

    /**
     * Adds the given class as a super class
     * and returns this class expression instance to allow cascading calls.
     *
     * @param other {@link OntClass}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addSubClassOfStatement(OntClass)
     * @see #removeSuperClass(Resource)
     */
    default OntClass addSuperClass(OntClass other) {
        addSubClassOfStatement(other);
        return this;
    }

    /**
     * Adds the given class as a disjoint class
     * and returns this class expression instance to allow cascading calls.
     *
     * @param other {@link OntClass}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addDisjointWithStatement(OntClass)
     * @see #removeDisjointClass(Resource)
     */
    default OntClass addDisjointClass(OntClass other) {
        addDisjointWithStatement(other);
        return this;
    }

    /**
     * Adds a new equivalent class.
     *
     * @param other {@link OntClass}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addEquivalentClassStatement(OntClass)
     * @see #removeDisjointClass(Resource)
     */
    default OntClass addEquivalentClass(OntClass other) {
        addEquivalentClassStatement(other);
        return this;
    }

    /**
     * Creates an {@code owl:hasKey} statement returning this class to allow cascading calls.
     *
     * @param objectProperties the collection of {@link OntObjectProperty}s
     * @param dataProperties   the collection of {@link OntDataProperty}s
     * @return <b>this</b> instance to allow cascading calls
     * @see #addHasKeyStatement(Collection, Collection)
     * @see #addHasKey(OntRealProperty...)
     */
    default OntClass addHasKey(Collection<OntObjectProperty> objectProperties, Collection<OntDataProperty> dataProperties) {
        addHasKeyStatement(objectProperties, dataProperties);
        return this;
    }

    /**
     * Creates an {@code owl:hasKey} statement returning this class to allow cascading calls.
     *
     * @param properties Array of {@link OntRealProperty}s without {@code null}s
     * @return <b>this</b> instance to allow cascading calls
     * @see #addHasKeyStatement(OntRealProperty...)
     * @see #addHasKey(Collection, Collection)
     * @see #removeHasKey(Resource)
     * @see #clearHasKeys()
     */
    default OntClass addHasKey(OntRealProperty... properties) {
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
     * @see #addSubClassOfStatement(OntClass)
     * @see #addSuperClass(OntClass)
     */
    default OntClass removeSuperClass(Resource other) {
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
     * @see #addDisjointWithStatement(OntClass)
     * @see #addDisjointClass(OntClass)
     * @see OntDisjoint.Classes
     */
    default OntClass removeDisjointClass(Resource other) {
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
     * @see #addEquivalentClassStatement(OntClass)
     * @see #addEquivalentClass(OntClass)
     * @see OntDataRange.Named#removeEquivalentClass(Resource)
     */
    default OntClass removeEquivalentClass(Resource other) {
        remove(OWL.equivalentClass, other);
        return this;
    }

    /**
     * Deletes all {@code HasKey} []-list including its annotations,
     * i.e. all those statements with the predicate {@link OWL#hasKey owl:hasKey} for which this resource is a subject.
     *
     * @return <b>this</b> instance to allow cascading calls
     * @throws OntJenaException if the list is not found
     */
    default OntClass clearHasKeys() {
        hasKeys().collect(Collectors.toList()).forEach(this::removeHasKey);
        return this;
    }

    /**
     * Finds a {@code HasKey} logical construction
     * attached to this class expression by the specified rdf-node in the form of {@link OntList}.
     *
     * @param list {@link RDFNode}
     * @return {@code Optional} around {@link OntList} of {@link OntRealProperty data and object property expression}s
     */
    default Optional<OntList<OntRealProperty>> findHasKey(RDFNode list) {
        try (Stream<OntList<OntRealProperty>> res = hasKeys().filter(r -> Objects.equals(r, list))) {
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
     * @return <b>distinct</b> {@code Stream} of {@link OntObjectProperty object} and {@link OntDataProperty data} properties
     * @see #hasKeys()
     */
    default Stream<OntRealProperty> fromHasKey() {
        return hasKeys().flatMap(OntList::members).distinct();
    }

    /*
     * ============================
     * All known Class Expressions:
     * ============================
     */

    /**
     * @see OntModel#createObjectSomeValuesFrom(OntObjectProperty, OntClass)
     */
    interface ObjectSomeValuesFrom extends ComponentRestrictionCE<OntClass, OntObjectProperty>,
            SetValue<OntClass, ObjectSomeValuesFrom>, SetProperty<OntObjectProperty, ObjectSomeValuesFrom> {
    }

    /**
     * @see OntModel#createDataSomeValuesFrom(OntDataProperty, OntDataRange)
     */
    interface DataSomeValuesFrom extends ComponentRestrictionCE<OntDataRange, OntDataProperty>,
            SetValue<OntDataRange, DataSomeValuesFrom>, SetProperty<OntDataProperty, DataSomeValuesFrom> {
    }

    /**
     * @see OntModel#createObjectAllValuesFrom(OntObjectProperty, OntClass)
     */
    interface ObjectAllValuesFrom extends ComponentRestrictionCE<OntClass, OntObjectProperty>,
            SetValue<OntClass, ObjectAllValuesFrom>, SetProperty<OntObjectProperty, ObjectAllValuesFrom> {
    }

    /**
     * @see OntModel#createDataAllValuesFrom(OntDataProperty, OntDataRange)
     */
    interface DataAllValuesFrom extends ComponentRestrictionCE<OntDataRange, OntDataProperty>,
            SetValue<OntDataRange, DataAllValuesFrom>, SetProperty<OntDataProperty, DataAllValuesFrom> {
    }

    /**
     * @see OntModel#createObjectHasValue(OntObjectProperty, OntIndividual)
     */
    interface ObjectHasValue extends ComponentRestrictionCE<OntIndividual, OntObjectProperty>,
            SetValue<OntIndividual, ObjectHasValue>, SetProperty<OntObjectProperty, ObjectHasValue> {
    }

    /**
     * @see OntModel#createDataHasValue(OntDataProperty, Literal)
     */
    interface DataHasValue extends ComponentRestrictionCE<Literal, OntDataProperty>,
            SetValue<Literal, DataHasValue>, SetProperty<OntDataProperty, DataHasValue> {
    }

    /**
     * @see OntModel#createObjectMinCardinality(OntObjectProperty, int, OntClass)
     */
    interface ObjectMinCardinality extends CardinalityRestrictionCE<OntClass, OntObjectProperty>,
            SetValue<OntClass, ObjectMinCardinality>,
            SetProperty<OntObjectProperty, ObjectMinCardinality>,
            SetCardinality<ObjectMinCardinality> {
    }

    /**
     * @see OntModel#createDataMinCardinality(OntDataProperty, int, OntDataRange)
     */
    interface DataMinCardinality extends CardinalityRestrictionCE<OntDataRange, OntDataProperty>,
            SetValue<OntDataRange, DataMinCardinality>,
            SetProperty<OntDataProperty, DataMinCardinality>,
            SetCardinality<DataMinCardinality> {
    }

    /**
     * @see OntModel#createDataMaxCardinality(OntDataProperty, int, OntDataRange)
     */
    interface ObjectMaxCardinality extends CardinalityRestrictionCE<OntClass, OntObjectProperty>,
            SetValue<OntClass, ObjectMaxCardinality>,
            SetProperty<OntObjectProperty, ObjectMaxCardinality>,
            SetCardinality<ObjectMaxCardinality> {
    }

    /**
     * @see OntModel#createDataMaxCardinality(OntDataProperty, int, OntDataRange)
     */
    interface DataMaxCardinality extends CardinalityRestrictionCE<OntDataRange, OntDataProperty>,
            SetValue<OntDataRange, DataMaxCardinality>,
            SetProperty<OntDataProperty, DataMaxCardinality>,
            SetCardinality<DataMaxCardinality> {
    }

    /**
     * @see OntModel#createObjectCardinality(OntObjectProperty, int, OntClass)
     */
    interface ObjectCardinality extends CardinalityRestrictionCE<OntClass, OntObjectProperty>,
            SetValue<OntClass, ObjectCardinality>,
            SetProperty<OntObjectProperty, ObjectCardinality>,
            SetCardinality<ObjectCardinality> {
    }

    /**
     * @see OntModel#createDataCardinality(OntDataProperty, int, OntDataRange)
     */
    interface DataCardinality extends CardinalityRestrictionCE<OntDataRange, OntDataProperty>,
            SetValue<OntDataRange, DataCardinality>,
            SetProperty<OntDataProperty, DataCardinality>,
            SetCardinality<DataCardinality> {
    }

    /**
     * @see OntModel#createHasSelf(OntObjectProperty)
     */
    interface HasSelf extends UnaryRestrictionCE<OntObjectProperty>, SetProperty<OntObjectProperty, HasSelf> {
    }

    /**
     * @see OntModel#createObjectUnionOf(Collection)
     */
    interface UnionOf extends ComponentsCE<OntClass>, SetComponents<OntClass, UnionOf> {
    }

    /**
     * @see OntModel#createObjectOneOf(Collection)
     */
    interface OneOf extends ComponentsCE<OntIndividual>, SetComponents<OntIndividual, OneOf> {
    }

    /**
     * @see OntModel#createObjectIntersectionOf(Collection)
     */
    interface IntersectionOf extends ComponentsCE<OntClass>, SetComponents<OntClass, IntersectionOf> {
    }

    /**
     * @see OntModel#createObjectComplementOf(OntClass)
     */
    interface ComplementOf extends OntClass, HasValue<OntClass>, SetValue<OntClass, ComplementOf> {
    }

    /**
     * @see OntModel#createDataAllValuesFrom(Collection, OntDataRange)
     */
    interface NaryDataAllValuesFrom extends NaryRestrictionCE<OntDataRange, OntDataProperty>,
            SetValue<OntDataRange, NaryDataAllValuesFrom>, SetProperties<OntDataProperty, NaryDataAllValuesFrom> {
    }

    /**
     * @see OntModel#createDataSomeValuesFrom(Collection, OntDataRange)
     */
    interface NaryDataSomeValuesFrom extends NaryRestrictionCE<OntDataRange, OntDataProperty>,
            SetValue<OntDataRange, NaryDataSomeValuesFrom>, SetProperties<OntDataProperty, NaryDataSomeValuesFrom> {
    }

    /**
     * An OWL Class {@link OntEntity Entity}, a named class expression.
     * This is an analogue of {@link org.apache.jena.ontology.OntClass}, but for OWL2.
     * <p>
     * Created by szuev on 01.11.2016.
     *
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#Classes'>5.1 Classes</a>
     */
    interface Named extends OntEntity, OntClass {

        /**
         * Lists all {@code DisjointUnion} {@link OntList ontology list}s that are attached to this OWL Class
         * on predicate {@link OWL#disjointUnionOf owl:disjointUnionOf}.
         *
         * @return {@code Stream} of {@link OntList}s with parameter-type {@code OntCE}
         */
        Stream<OntList<OntClass>> disjointUnions();

        /**
         * Creates a {@code DisjointUnion} as {@link OntList ontology []-list} of {@link OntClass Class Expression}s
         * that is attached to this OWL Class using the predicate {@link OWL#disjointUnionOf owl:disjointUnionOf}.
         * The resulting rdf-list will consist of all the elements of the specified collection
         * in the same order but with exclusion of duplicates.
         * Note: {@code null}s in collection will cause {@link OntJenaException.IllegalArgument exception}.
         * For additional information about {@code DisjointUnion} logical construction see
         * <a href='https://www.w3.org/TR/owl2-syntax/#Disjoint_Union_of_Class_Expressions'>9.1.4 Disjoint Union of Class Expressions</a>.
         *
         * @param classes {@link Collection} (preferably {@link Set}) of {@link OntClass class expression}s
         * @return {@link OntList} of {@link OntClass}s
         * @see #addDisjointUnionOfStatement(OntClass...)
         * @see #removeDisjointUnion(Resource)
         */
        OntList<OntClass> createDisjointUnion(Collection<OntClass> classes);

        @Override
        default Named asNamed() {
            return this;
        }

        /**
         * Finds a {@code DisjointUnion} logical construction
         * attached to this class by the specified rdf-node in the form of {@link OntList}.
         *
         * @param list {@link RDFNode}
         * @return {@code Optional} around {@link OntList} of {@link OntClass class expression}s
         */
        default Optional<OntList<OntClass>> findDisjointUnion(RDFNode list) {
            try (Stream<OntList<OntClass>> res = disjointUnions().filter(r -> Objects.equals(r, list))) {
                return res.findFirst();
            }
        }

        /**
         * Creates a {@code DisjointUnion} {@link OntList ontology list}
         * and returns the statement {@code CN owl:disjointUnionOf ( C1 ... Cn )} to allow the addition of annotations.
         * About RDF Graph annotation specification see, for example,
         * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>.
         *
         * @param classes Array of {@link OntClass class expressions} without {@code null}s,
         *                duplicates will be discarded and order will be saved
         * @return {@link OntStatement} to allow the subsequent annotations addition
         * @see #createDisjointUnion(Collection)
         * @see #createDisjointUnion(Collection)
         * @see #addDisjointUnion(OntClass...)
         * @see #addDisjointUnionOfStatement(OntClass...)
         * @see #removeDisjointUnion(Resource)
         */
        default OntStatement addDisjointUnionOfStatement(OntClass... classes) {
            return addDisjointUnionOfStatement(Arrays.stream(classes).collect(Collectors.toCollection(LinkedHashSet::new)));
        }

        /**
         * Creates a disjoint-union section returning its root statement to allow adding annotations.
         * The triple pattern: {@code CN owl:disjointUnionOf ( C1 ... Cn )}.
         *
         * @param classes a collection of {@link OntClass class expression}s without {@code null}s
         * @return {@link OntStatement} to allow the subsequent annotations addition
         * @see #createDisjointUnion(Collection)
         * @see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>
         * @see #createDisjointUnion(Collection)
         * @see #addDisjointUnion(Collection)
         * @see #addDisjointUnionOfStatement(Collection)
         * @see #removeDisjointUnion(Resource)
         */
        default OntStatement addDisjointUnionOfStatement(Collection<OntClass> classes) {
            return createDisjointUnion(classes).getMainStatement();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        default Named addSuperClass(OntClass other) {
            addSubClassOfStatement(other);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        default Named addDisjointClass(OntClass other) {
            addDisjointWithStatement(other);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        default Named addEquivalentClass(OntClass other) {
            addEquivalentClassStatement(other);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        default Named addHasKey(Collection<OntObjectProperty> objectProperties, Collection<OntDataProperty> dataProperties) {
            addHasKeyStatement(objectProperties, dataProperties);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        default Named addHasKey(OntRealProperty... properties) {
            addHasKeyStatement(properties);
            return this;
        }

        /**
         * @param classes a collection of {@link OntClass class expression}s without {@code null}s
         * @return <b>this</b> instance to allow cascading calls
         */
        default Named addDisjointUnion(Collection<OntClass> classes) {
            addDisjointUnionOfStatement(classes);
            return this;
        }

        /**
         * @param classes Array of {@link OntClass class expressions} without {@code null}s,
         *                duplicates will be discarded and order will be saved
         * @return <b>this</b> instance to allow cascading calls
         */
        default Named addDisjointUnion(OntClass... classes) {
            addDisjointUnionOfStatement(classes);
            return this;
        }

        /**
         * Deletes the given {@code DisjointUnion} list including its annotations.
         *
         * @param list {@link Resource} can be {@link OntList} or {@link RDFList}
         * @return <b>this</b> instance to allow cascading calls
         * @throws OntJenaException if the list is not found
         * @see #addDisjointUnion(Collection)
         * @see #createDisjointUnion(Collection)
         * @see #addDisjointUnionOfStatement(OntClass...)
         * @see #createDisjointUnion(Collection)
         */
        Named removeDisjointUnion(Resource list);

        /**
         * {@inheritDoc}
         */
        default Named removeSuperClass(Resource other) {
            OntClass.super.removeSuperClass(other);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        default Named removeDisjointClass(Resource other) {
            OntClass.super.removeDisjointClass(other);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        default Named removeEquivalentClass(Resource other) {
            OntClass.super.removeEquivalentClass(other);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        default Named clearHasKeys() {
            OntClass.super.clearHasKeys();
            return this;
        }

        /**
         * Deletes all {@code DisjointUnion} []-lists including their annotations,
         * i.e. all those statements with the predicate {@link OWL#disjointUnionOf owl:disjointUnionOf}
         * for which this resource is a subject.
         *
         * @return <b>this</b> instance to allow cascading calls
         * @see #removeDisjointUnion(Resource)
         */
        default Named clearDisjointUnions() {
            disjointUnions().collect(Collectors.toSet()).forEach(this::removeDisjointUnion);
            return this;
        }

        /**
         * Returns all class expressions from the right part of the statement with this class as a subject
         * and {@link OWL#disjointUnionOf owl:disjointUnionOf} as a predicate
         * (the triple pattern: {@code CN owl:disjointUnionOf ( C1 ... Cn )}).
         * If there are several []-lists in the model that satisfy these conditions,
         * all their content will be merged into the one distinct stream.
         *
         * @return <b>distinct</b> stream of {@link OntClass class expressions}s
         * @see #disjointUnions()
         */
        default Stream<OntClass> fromDisjointUnionOf() {
            return disjointUnions().flatMap(OntList::members).distinct();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        default Named addComment(String txt) {
            return addComment(txt, null);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        default Named addComment(String txt, String lang) {
            return annotate(getModel().getRDFSComment(), txt, lang);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        default Named addLabel(String txt) {
            return addLabel(txt, null);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        default Named addLabel(String txt, String lang) {
            return annotate(getModel().getRDFSLabel(), txt, lang);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        default Named annotate(OntAnnotationProperty predicate, String txt, String lang) {
            return annotate(predicate, getModel().createLiteral(txt, lang));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        default Named annotate(OntAnnotationProperty predicate, RDFNode value) {
            addAnnotation(predicate, value);
            return this;
        }
    }

    /*
     * ===========================
     * Abstract class expressions:
     * ===========================
     */

    /**
     * An abstraction for Boolean Connectives (with exclude of {@link ComplementOf}) and Enumeration of Individuals.
     *
     * @param <O> a component type
     */
    interface ComponentsCE<O extends OntObject> extends OntClass, HasRDFNodeList<O> {
    }

    /**
     * An abstraction for Cardinality Restrictions.
     *
     * @param <O> a value type
     * @param <P> any subtype of {@link OntRealProperty}
     */
    interface CardinalityRestrictionCE<O extends OntObject, P extends OntRealProperty>
            extends HasCardinality, ComponentRestrictionCE<O, P> {
    }

    /**
     * An abstract class expression (Restriction) that has component (i.e. 'filler' in OWL-API terms):
     * all Cardinality Restrictions, Existential/Universal Restrictions, Individual/Literal Value Restrictions.
     *
     * @param <O> a value type
     * @param <P> any subtype of {@link OntRealProperty}
     */
    interface ComponentRestrictionCE<O extends RDFNode, P extends OntRealProperty>
            extends UnaryRestrictionCE<P>, HasValue<O> {
    }

    /**
     * An abstraction that unites all {@link RestrictionCE Restriction}s
     * with the predicate {@link OWL#onProperties owl:onProperties}.
     *
     * @param <O> a value type
     * @param <P> any subtype of {@link OntRealProperty}
     */
    interface NaryRestrictionCE<O extends OntObject, P extends OntRealProperty>
            extends RestrictionCE<P>, HasProperties<P>, HasValue<O> {
    }

    /**
     * An abstract class expression that unites all {@link RestrictionCE Restriction}s
     * with the predicate {@link OWL#onProperty owl:onProperty}.
     *
     * @param <P> any subtype of {@link OntRealProperty}
     */
    interface UnaryRestrictionCE<P extends OntRealProperty> extends RestrictionCE<P> {
    }

    /**
     * An abstract class expression that unites all class expressions with the type {@link OWL#Restriction}.
     *
     * @param <P> any subtype of {@link OntRealProperty}
     */
    interface RestrictionCE<P extends OntRealProperty> extends OntClass, HasProperty<P> {
    }
}