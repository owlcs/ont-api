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

import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

import java.util.stream.Stream;

/**
 * Interface encapsulating an Ontology (Named) Data Property.
 * This is an extension to the standard jena {@link Property},
 * the {@link OntEntity OWL Entity} and the {@link OntRealProperty real property} interfaces.
 * Range values for this property are are {@link OntDataRange datarange} values
 * (as distinct from object property expression valued {@link OntObjectProperty properties}).
 * In OWL2 a Data Property cannot be anonymous.
 * <p>
 * Created by szuev on 01.11.2016.
 *
 * @see <a href='https://www.w3.org/TR/owl2-syntax/#Data_Properties'>5.4 Data Properties</a>
 */
public interface OntDataProperty extends OntRealProperty, OntNamedProperty<OntDataProperty> {

    /**
     * Adds a negative data property assertion.
     *
     * @param source {@link OntIndividual}, the source
     * @param target {@link Literal}, the target
     * @return {@link OntNegativeAssertion.WithDataProperty}
     * @see OntObjectProperty#addNegativeAssertion(OntIndividual, OntIndividual)
     */
    OntNegativeAssertion.WithDataProperty addNegativeAssertion(OntIndividual source, Literal target);

    /**
     * {@inheritDoc}
     *
     * @return <b>distinct</b> {@code Stream} of datatype properties
     */
    @Override
    Stream<OntDataProperty> superProperties(boolean direct);

    /**
     * {@inheritDoc}
     *
     * @return <b>distinct</b> {@code Stream} of datatype properties
     */
    @Override
    Stream<OntDataProperty> subProperties(boolean direct);

    /**
     * {@inheritDoc}
     *
     * @return {@code Stream} of {@link OntNegativeAssertion.WithDataProperty}s
     * @see OntObjectProperty#negativeAssertions()
     */
    @Override
    default Stream<OntNegativeAssertion.WithDataProperty> negativeAssertions() {
        return getModel().ontObjects(OntNegativeAssertion.WithDataProperty.class).filter(a -> OntDataProperty.this.equals(a.getProperty()));
    }

    /**
     * Returns all associated negative data property assertions for the specified source individual.
     *
     * @param source {@link OntIndividual}
     * @return {@code Stream} of {@link OntNegativeAssertion.WithDataProperty}s.
     * @see OntObjectProperty#negativeAssertions(OntIndividual)
     */
    default Stream<OntNegativeAssertion.WithDataProperty> negativeAssertions(OntIndividual source) {
        return negativeAssertions()
                .filter(a -> a.getSource().equals(source));
    }

    /**
     * Returns all property ranges (the statement pattern: {@code R rdfs:range D}).
     *
     * @return {@code Stream} of {@link OntDataRange}s
     */
    @Override
    default Stream<OntDataRange> ranges() {
        return objects(RDFS.range, OntDataRange.class);
    }

    /**
     * Lists all super properties.
     * The pattern is {@code R1 rdfs:subPropertyOf R2},
     * where {@code R1} is this data property and {@code R2} is a retrieved data property.
     *
     * @return {@code Stream} of {@link OntDataProperty}s
     * @see #addSuperProperty(OntDataProperty)
     * @see OntProperty#removeSuperProperty(Resource)
     * @see #addSubPropertyOfStatement(OntDataProperty)
     */
    @Override
    default Stream<OntDataProperty> superProperties() {
        return objects(RDFS.subPropertyOf, OntDataProperty.class);
    }

    /**
     * Returns disjoint properties.
     * The statement pattern is: {@code Ri owl:propertyDisjointWith Rj}, where {@code Ri} - this property,
     * and {@code Rj} - the data property to return.
     *
     * @return {@code Stream} of {@link OntDataProperty}s
     * @see OntObjectProperty#disjointProperties()
     * @see OntDisjoint.DataProperties
     */
    @Override
    default Stream<OntDataProperty> disjointProperties() {
        return objects(OWL.propertyDisjointWith, OntDataProperty.class);
    }

    /**
     * Returns all equivalent data properties
     * The statement pattern is {@code Ri owl:equivalentProperty Rj},
     * where {@code Ri} - this property, {@code Rj} - the data property to return.
     *
     * @return {@code Stream} of {@link OntDataProperty}s
     * @see OntObjectProperty#equivalentProperties()
     */
    @Override
    default Stream<OntDataProperty> equivalentProperties() {
        return objects(OWL.equivalentProperty, OntDataProperty.class);
    }

    /**
     * Adds a statement {@code R rdfs:range D},
     * where {@code R} is this data property and {@code D} is the given data range expression.
     *
     * @param range {@link OntDataRange}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @see #addRange(OntDataRange)
     */
    default OntStatement addRangeStatement(OntDataRange range) {
        return addStatement(RDFS.range, range);
    }

    /**
     * Adds the given property as super property returning a new statement to annotate.
     * The triple pattern is {@code this rdfs:subPropertyOf property}).
     *
     * @param property {@link OntDataProperty}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     */
    default OntStatement addSubPropertyOfStatement(OntDataProperty property) {
        return addStatement(RDFS.subPropertyOf, property);
    }

    /**
     * Creates and returns a new {@link OWL#equivalentProperty owl:equivalentProperty} statement
     * with the given property as an object and this property as a subject.
     *
     * @param other {@link OntDataProperty}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @see #addEquivalentProperty(OntDataProperty)
     * @see #removeEquivalentProperty(Resource)
     * @see OntObjectProperty#addEquivalentPropertyStatement(OntObjectProperty)
     */
    default OntStatement addEquivalentPropertyStatement(OntDataProperty other) {
        return addStatement(OWL.equivalentProperty, other);
    }

    /**
     * Adds a disjoint object property (i.e. the {@code _:this owl:propertyDisjointWith @other} statement).
     *
     * @param other {@link OntDataProperty}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @see #addDisjointProperty(OntDataProperty)
     * @see #removeDisjointProperty(Resource)
     * @see OntObjectProperty#addPropertyDisjointWithStatement(OntObjectProperty)
     * @see OntDisjoint.ObjectProperties
     */
    default OntStatement addPropertyDisjointWithStatement(OntDataProperty other) {
        return addStatement(OWL.propertyDisjointWith, other);
    }

    /**
     * Adds the given property as super property returning this property itself.
     *
     * @param property {@link OntDataProperty}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #removeSuperProperty(Resource)
     */
    default OntDataProperty addSuperProperty(OntDataProperty property) {
        addSubPropertyOfStatement(property);
        return this;
    }

    /**
     * Adds a range statement.
     *
     * @param range {@link Resource}, that represents a {@link OntDataRange data range}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @throws org.apache.jena.enhanced.UnsupportedPolymorphismException in case wrong resource is specified
     */
    default OntDataProperty addRange(Resource range) {
        return addRange(range.inModel(getModel()).as(OntDataRange.class));
    }

    /**
     * Adds a statement with the {@link RDFS#range} as predicate
     * and the specified {@link OntDataRange data range} as an object.
     *
     * @param range {@link OntDataRange}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addRangeStatement(OntDataRange)
     */
    default OntDataProperty addRange(OntDataRange range) {
        addRangeStatement(range);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntDataProperty addDomain(OntClass ce) {
        addDomainStatement(ce);
        return this;
    }

    /**
     * Adds a new {@link OWL#equivalentProperty owl:equivalentProperty} statement.
     *
     * @param other {@link OntDataProperty}, not {@code null}
     * @return {@link OntDataProperty} <b>this</b> instance to allow cascading calls
     * @see #addEquivalentPropertyStatement(OntDataProperty)
     * @see OntRealProperty#removeEquivalentProperty(Resource)
     * @see OntObjectProperty#addEquivalentProperty(OntObjectProperty)
     */
    default OntDataProperty addEquivalentProperty(OntDataProperty other) {
        addEquivalentPropertyStatement(other);
        return this;
    }

    /**
     * Adds a new {@link OWL#propertyDisjointWith owl:propertyDisjointWith} statement
     * for this and the specified property.
     *
     * @param other {@link OntDataProperty}, not {@code null}
     * @return {@link OntDataProperty} <b>this</b> instance to allow cascading calls
     * @see #addPropertyDisjointWithStatement(OntDataProperty)
     * @see OntObjectProperty#addDisjointProperty(OntObjectProperty)
     * @see OntRealProperty#removeDisjointProperty(Resource)
     * @see OntDisjoint.DataProperties
     */
    default OntDataProperty addDisjointProperty(OntDataProperty other) {
        addPropertyDisjointWithStatement(other);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntDataProperty removeSuperProperty(Resource property) {
        remove(RDFS.subPropertyOf, property);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntDataProperty removeDomain(Resource domain) {
        remove(RDFS.domain, domain);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntDataProperty removeRange(Resource range) {
        remove(RDFS.range, range);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntDataProperty removeEquivalentProperty(Resource property) {
        remove(OWL.equivalentProperty, property);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntDataProperty removeDisjointProperty(Resource property) {
        remove(OWL.propertyDisjointWith, property);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntDataProperty setFunctional(boolean functional) {
        if (functional) {
            addFunctionalDeclaration();
        } else {
            remove(RDF.type, OWL.FunctionalProperty);
        }
        return this;
    }

}
