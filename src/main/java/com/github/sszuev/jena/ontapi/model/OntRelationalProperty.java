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

package com.github.sszuev.jena.ontapi.model;

import com.github.sszuev.jena.ontapi.vocabulary.OWL;
import com.github.sszuev.jena.ontapi.vocabulary.RDF;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import java.util.stream.Stream;

/**
 * A common (abstract) interface for any Ontology Data and Object Property expressions.
 * In OWL2 terms it is any {@link OntProperty Property Expression} minus {@link OntAnnotationProperty Annotation Property}.
 * <p>
 * Created by @ssz on 21.07.2018.
 */
public interface OntRelationalProperty extends OntProperty {

    /**
     * {@inheritDoc}
     *
     * @return <b>distinct</b> {@code Stream} of data <b>or</b> object properties
     */
    Stream<? extends OntRelationalProperty> superProperties(boolean direct);

    /**
     * {@inheritDoc}
     *
     * @return <b>distinct</b> {@code Stream} of data <b>or</b> object properties
     */
    Stream<? extends OntRelationalProperty> subProperties(boolean direct);

    /**
     * Lists all property ranges,
     * i.e. all objects from statements with this property as subject and {@code rdfs:range} as predicate.
     *
     * @return {@code Stream} of {@link OntObject ontology object}s
     */
    Stream<? extends OntObject> ranges();

    /**
     * List all super properties for this property expression.
     * In other words, returns all objects {@code R} from statements like {@code P rdfs:subPropertyOf R},
     * where {@code P} is this property.
     *
     * @return {@code Stream} of {@link OntRelationalProperty}s - object <b>or</b> data properties
     */
    Stream<? extends OntRelationalProperty> superProperties();

    /**
     * Answers a {@code Stream} of all the classes in this ontology,
     * such that each returned class has this property as one of its properties in
     * {@link OntClass#declaredProperties(boolean)}.
     * This simulates a frame-like view of properties and classes;
     * for more details see the
     * <a href="https://jena.apache.org/documentation/notes/rdf-frames.html">Apache Jena: Presenting RDF as frames</a>
     * <p>
     * The behavior of this method must be identical to the behavior of the Jena method
     * {@link org.apache.jena.ontology.OntProperty#listDeclaringClasses(boolean)}.
     *
     * @param direct {@code boolean} if {@code true}, use only <em>direct</em> associations between classes and properties
     * @return a {@code Stream} of the classes having this property as one of their declared properties
     */
    Stream<OntClass> declaringClasses(boolean direct);

    /**
     * Lists all properties that are disjoint with this property.
     * In other words, returns all objects from statements of the form {@code P owl:propertyDisjointWith R},
     * where {@code P} is this property and {@code R} is a returned property of the same type.
     *
     * @return {@code Stream} of {@link OntRelationalProperty}s - object <b>or</b> data properties
     * @see OntDisjoint.Properties
     */
    Stream<? extends OntRelationalProperty> disjointProperties();

    /**
     * Lists all properties that equivalent to this one.
     * In other words, returns all objects from statements of the form {@code P owl:equivalentProperty R},
     * where {@code P} is this property and {@code R} is a returned property of the same type.
     *
     * @return {@code Stream} of {@link OntRelationalProperty}s - object <b>or</b> data properties
     */
    Stream<? extends OntRelationalProperty> equivalentProperties();

    /**
     * Lists all negative property assertions.
     * A negative property assertion is anonymous resource
     * with the type {@link OWL#NegativePropertyAssertion owl:NegativePropertyAssertion}
     * that has a data or object property expression as an object
     * on the predicate {@link OWL#assertionProperty owl:assertionProperty}.
     *
     * @return {@code Stream} of {@link OntNegativeAssertion}
     */
    Stream<? extends OntNegativeAssertion<?, ?>> negativeAssertions();

    /**
     * Adds or removes {@link OWL#FunctionalProperty owl:FunctionalProperty} declaration
     * for this property according to the given boolean flag.
     * Note: the statement is removed along with all its annotations.
     *
     * @param functional {@code true} if should be functional
     * @return <b>this</b> instance to allow cascading calls
     * @see #addFunctionalDeclaration()
     */
    OntRelationalProperty setFunctional(boolean functional);

    /**
     * Adds a statement with the {@link RDFS#domain} as predicate,
     * this property as a subject, and the specified {@link OntClass class expression} as an object.
     *
     * @param ce {@link OntClass}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addDomainStatement(OntClass)
     */
    OntRelationalProperty addDomain(OntClass ce);

    /**
     * {@inheritDoc}
     */
    @Override
    OntRelationalProperty removeDomain(Resource domain);

    /**
     * {@inheritDoc}
     */
    @Override
    OntRelationalProperty removeRange(Resource range);

    /**
     * {@inheritDoc}
     */
    @Override
    OntRelationalProperty removeSuperProperty(Resource property);

    /**
     * Removes the equivalent property statement
     * (a statement with the predicate {@link OWL#equivalentProperty owl:equivalentProperty})
     * for the specified resource (considered as object), including the corresponding statement's annotations.
     * No-op in case no such equivalent property relationship is found.
     * Removes all triples with predicate {@code owl:equivalentProperty} (and all theirs annotation triples)
     * if {@code null} is given.
     *
     * @param property {@link Resource} or {@code null} to remove all equivalent properties
     * @return <b>this</b> instance to allow cascading calls
     */
    OntRelationalProperty removeEquivalentProperty(Resource property);

    /**
     * Removes the {@code owl:propertyDisjointWith} statement
     * (a statement with the predicate {@link OWL#propertyDisjointWith owl:propertyDisjointWith})
     * for the specified resource (considered as object), including the corresponding statement's annotations.
     * No-op in case no such disjoint property relationship is found.
     * Removes all triples with predicate {@code owl:propertyDisjointWith} (and all theirs annotation triples)
     * if {@code null} is given.
     *
     * @param property {@link Resource} or {@code null} to remove all disjoint properties
     * @return <b>this</b> instance to allow cascading calls
     * @see OntDisjoint.Properties
     */
    OntRelationalProperty removeDisjointProperty(Resource property);

    /**
     * Answers a {@code Stream} over any restrictions that mention this property as
     * the property that the restriction is adding some constraint to.
     * For example:
     * <pre>
     * {@code
     * _:x rdf:type owl:Restriction.
     * _:x owl:onProperty P.
     * _:x owl:qualifiedCardinality n.
     * _:x owl:onClass C.
     * }
     * </pre>
     * Note that any such restrictions do not affect the global semantics of this property itself.
     * Restrictions define new class expressions, and the property constraints are local to that class expression.
     * This method is provided as a convenience to assist callers to navigate the relationships in the model.
     * @return a {@code Stream} whose values are the restrictions from the local model that reference this property.
     */
    Stream<? extends OntClass.RestrictionCE<? extends OntRelationalProperty>> referringRestrictions();

    /**
     * Lists all the declared domain class expressions of this property expression.
     * In other words, returns the right-hand sides of statement {@code P rdfs:domain C},
     * where {@code P} is this property expression.
     *
     * @return {@code Stream} of {@link OntClass class expression}s
     */
    @Override
    default Stream<OntClass> domains() {
        return objects(RDFS.domain, OntClass.class);
    }

    /**
     * Gets all direct or indirect domains which present in RDF graph.
     * Indirect domains are calculated using {@code OntClass.superClasses(true)} relationship.
     * For example consider the following statements (only people can have names):
     * <pre>
     * {@code
     * :Primate rdf:type owl:Class .
     * :Person rdf:type owl:Class .
     * :hasName rdf:type owl:DatatypeProperty .
     * :hasName rdfs:domain :Person .
     * :Person rdfs:subClassOf :Primate .
     * }
     * </pre>
     * from these statements it can be derived that only primates can have names
     * (which does not mean that all primates have names):
     * <pre>
     * {@code
     * :hasName rdfs:domain :Primate .
     * }
     * </pre>
     * The same true for object properties: if "only people can have dogs" then "only primates can have dogs"
     *
     * @param direct if {@code true} the method behaves the same as {@link #domains()}
     * @return {@code Stream} of {@link OntClass class expression}s, distinct
     */
    default Stream<OntClass> domains(boolean direct) {
        if (direct) return domains();
        return domains().flatMap(d -> Stream.concat(Stream.of(d), d.superClasses(false))).distinct();
    }

    /**
     * Creates the {@code P rdf:type owl:FunctionalProperty} property declaration statement,
     * where {@code P} is this property.
     *
     * @return {@link OntStatement} to allow the subsequent addition of annotations
     * @see #setFunctional(boolean)
     */
    default OntStatement addFunctionalDeclaration() {
        return addStatement(RDF.type, OWL.FunctionalProperty);
    }

    /**
     * Adds a statement {@code P rdfs:domain C},
     * where {@code P} is this property expression and {@code C} is the specified class expression.
     *
     * @param ce {@link OntClass class expression}, not null
     * @return {@link OntStatement} to allow the subsequent addition of annotations
     * @see #addDomain(OntClass)
     */
    default OntStatement addDomainStatement(OntClass ce) {
        return addStatement(RDFS.domain, ce);
    }

    /**
     * Answers {@code true} iff it is a functional (data or object) property expression.
     * A functional property is defined by the statement {@code P rdf:type owl:FunctionalProperty},
     * where {@code P} is this property expression.
     *
     * @return boolean
     */
    default boolean isFunctional() {
        return hasType(OWL.FunctionalProperty);
    }

}
