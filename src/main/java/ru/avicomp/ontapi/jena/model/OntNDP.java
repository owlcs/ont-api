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
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.stream.Stream;

/**
 * Interface encapsulating an Ontology <b>N</b>amed <b>D</b>ata <b>P</b>roperty.
 * The first word in this abbreviation means that it is an URI-{@link Resource Resource}.
 * This is an extension to the standard jena {@link Property},
 * the {@link OntEntity OWL Entity} and the {@link OntDOP abstract data object property} interfaces.
 * Range values for this property are are datatype values (as distinct from object property expression valued {@link OntOPE properties}).
 * In OWL2 a Data Property cannot be anonymous.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntNDP extends OntDOP, OntProperty {

    /**
     * Adds a negative data property assertion.
     *
     * @param source {@link OntIndividual}, the source
     * @param target {@link Literal}, the target
     * @return {@link OntNPA.DataAssertion}
     * @see OntOPE#addNegativeAssertion(OntIndividual, OntIndividual)
     */
    OntNPA.DataAssertion addNegativeAssertion(OntIndividual source, Literal target);

    /**
     * {@inheritDoc}
     *
     * @return <b>distinct</b> {@code Stream} of datatype properties
     */
    @Override
    Stream<OntNDP> superProperties(boolean direct);

    /**
     * {@inheritDoc}
     *
     * @return <b>distinct</b> {@code Stream} of datatype properties
     */
    @Override
    Stream<OntNDP> subProperties(boolean direct);

    /**
     * {@inheritDoc}
     *
     * @return {@code Stream} of {@link OntNPA.DataAssertion}s
     * @see OntOPE#negativeAssertions()
     */
    @Override
    default Stream<OntNPA.DataAssertion> negativeAssertions() {
        return getModel().ontObjects(OntNPA.DataAssertion.class).filter(a -> OntNDP.this.equals(a.getProperty()));
    }

    /**
     * Returns all associated negative data property assertions for the specified source individual.
     *
     * @param source {@link OntIndividual}
     * @return {@code Stream} of {@link OntNPA.DataAssertion}s.
     * @see OntOPE#negativeAssertions(OntIndividual)
     */
    default Stream<OntNPA.DataAssertion> negativeAssertions(OntIndividual source) {
        return negativeAssertions()
                .filter(a -> a.getSource().equals(source));
    }

    /**
     * Returns all property ranges (the statement pattern: {@code R rdfs:range D}).
     *
     * @return {@code Stream} of {@link OntDR}s
     * @since 1.4.0
     */
    @Override
    default Stream<OntDR> ranges() {
        return objects(RDFS.range, OntDR.class);
    }

    /**
     * Lists all super properties.
     * The pattern is {@code R1 rdfs:subPropertyOf R2},
     * where {@code R1} is this data property and {@code R2} is a retrieved data property.
     *
     * @return {@code Stream} of {@link OntNDP}s
     * @see #addSuperProperty(OntNDP)
     * @see OntPE#removeSuperProperty(Resource)
     * @see #addSubPropertyOfStatement(OntNDP)
     * @since 1.4.0
     */
    @Override
    default Stream<OntNDP> superProperties() {
        return objects(RDFS.subPropertyOf, OntNDP.class);
    }

    /**
     * Returns disjoint properties (statement: {@code R1 owl:propertyDisjointWith R2}, where {@code Ri} - this property).
     *
     * @return {@code Stream} of {@link OntNDP}s
     * @see OntOPE#disjointProperties()
     * @see OntDisjoint.DataProperties
     * @since 1.4.0
     */
    @Override
    default Stream<OntNDP> disjointProperties() {
        return objects(OWL.propertyDisjointWith, OntNDP.class);
    }

    /**
     * Returns all equivalent data properties
     * The statement pattern is {@code Ri owl:equivalentProperty Rj},
     * where {@code Ri} - this property, {@code Rj} - the property of the same type to return.
     *
     * @return {@code Stream} of {@link OntNDP}s
     * @see OntOPE#equivalentProperties()
     * @since 1.4.0
     */
    @Override
    default Stream<OntNDP> equivalentProperties() {
        return objects(OWL.equivalentProperty, OntNDP.class);
    }

    /**
     * Adds a statement {@code R rdfs:range D},
     * where {@code R} is this data property and {@code D} is the given data range expression.
     *
     * @param range {@link OntDR}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @see #addRange(OntDR)
     * @since 1.4.0
     */
    default OntStatement addRangeStatement(OntDR range) {
        return addStatement(RDFS.range, range);
    }

    /**
     * Adds the given property as super property returning a new statement to annotate.
     * The triple pattern is {@code this rdfs:subPropertyOf property}).
     *
     * @param property {@link OntNDP}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @since 1.4.0
     */
    default OntStatement addSubPropertyOfStatement(OntNDP property) {
        return addStatement(RDFS.subPropertyOf, property);
    }

    /**
     * Creates and returns a new {@link OWL#equivalentProperty owl:equivalentProperty} statement
     * with the given property as an object and this property as a subject.
     *
     * @param other {@link OntNDP}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @see #addEquivalentProperty(OntNDP)
     * @see #removeEquivalentProperty(Resource)
     * @see OntOPE#addEquivalentPropertyStatement(OntOPE)
     * @since 1.4.0
     */
    default OntStatement addEquivalentPropertyStatement(OntNDP other) {
        return addStatement(OWL.equivalentProperty, other);
    }

    /**
     * Adds a disjoint object property (i.e. the {@code _:this owl:propertyDisjointWith @other} statement).
     *
     * @param other {@link OntNDP}, not {@code null}
     * @return {@link OntStatement} to allow subsequent annotations adding
     * @see #addDisjointProperty(OntNDP)
     * @see #removeDisjointProperty(Resource)
     * @see OntOPE#addPropertyDisjointWithStatement(OntOPE)
     * @see OntDisjoint.ObjectProperties
     * @since 1.4.0
     */
    default OntStatement addPropertyDisjointWithStatement(OntNDP other) {
        return addStatement(OWL.propertyDisjointWith, other);
    }

    /**
     * Adds the given property as super property returning this property itself.
     *
     * @param property {@link OntNDP}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #removeSuperProperty(Resource)
     * @since 1.4.0
     */
    default OntNDP addSuperProperty(OntNDP property) {
        addSubPropertyOfStatement(property);
        return this;
    }

    /**
     * Adds a range statement.
     *
     * @param range {@link Resource}, that represents a {@link OntDR data range}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @throws org.apache.jena.enhanced.UnsupportedPolymorphismException in case wrong resource is specified
     * @since 1.4.1
     */
    default OntNDP addRange(Resource range) {
        return addRange(range.inModel(getModel()).as(OntDR.class));
    }

    /**
     * Adds a statement with the {@link RDFS#range} as predicate
     * and the specified {@link OntDR data range} as an object.
     *
     * @param range {@link OntDR}, not {@code null}
     * @return <b>this</b> instance to allow cascading calls
     * @see #addRangeStatement(OntDR)
     */
    default OntNDP addRange(OntDR range) {
        addRangeStatement(range);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntNDP addDomain(OntCE ce) {
        addDomainStatement(ce);
        return this;
    }

    /**
     * Adds a new {@link OWL#equivalentProperty owl:equivalentProperty} statement.
     *
     * @param other {@link OntNDP}, not {@code null}
     * @return {@link OntNDP} <b>this</b> instance to allow cascading calls
     * @see #addEquivalentPropertyStatement(OntNDP)
     * @see OntDOP#removeEquivalentProperty(Resource)
     * @see OntOPE#addEquivalentProperty(OntOPE)
     */
    default OntNDP addEquivalentProperty(OntNDP other) {
        addEquivalentPropertyStatement(other);
        return this;
    }

    /**
     * Adds a new {@link OWL#propertyDisjointWith owl:propertyDisjointWith} statement
     * for this and the specified property.
     *
     * @param other {@link OntNDP}, not {@code null}
     * @return {@link OntNDP} <b>this</b> instance to allow cascading calls
     * @see #addPropertyDisjointWithStatement(OntNDP)
     * @see OntOPE#addDisjointProperty(OntOPE)
     * @see OntDOP#removeDisjointProperty(Resource)
     * @see OntDisjoint.DataProperties
     * @since 1.4.0
     */
    default OntNDP addDisjointProperty(OntNDP other) {
        addPropertyDisjointWithStatement(other);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntNDP removeSuperProperty(Resource property) {
        remove(RDFS.subPropertyOf, property);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntNDP removeDomain(Resource domain) {
        remove(RDFS.domain, domain);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntNDP removeRange(Resource range) {
        remove(RDFS.range, range);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntNDP removeEquivalentProperty(Resource property) {
        remove(OWL.equivalentProperty, property);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntNDP removeDisjointProperty(Resource property) {
        remove(OWL.propertyDisjointWith, property);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntNDP setFunctional(boolean functional) {
        if (functional) {
            addFunctionalDeclaration();
        } else {
            remove(RDF.type, OWL.FunctionalProperty);
        }
        return this;
    }

}
