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

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * A base {@link OntResource Ontology Object RDF Resource}.
 * A common super-type for all of the abstractions in the {@link OntGraphModel Ontology RDF Model},
 * which support Jena Polymorphism, can be annotated and have a structure that is strictly defined according to the OWL2 specification.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntObject extends OntResource {

    /**
     * Returns the ontology object declaration statement.
     * In most cases it wraps a triple with predicate {@code rdf:type}.
     * Note that the returned ont-statement differs from that
     * which could be obtained directly from the model using one of its {@code statement(..)} method:
     * an statement's annotations are added in the form of annotation property assertions (so-called 'plain annotations'),
     * not as anonymous resources with type {@code owl:Axiom} or {@code owl:Annotation} (so-called 'bulk annotations').
     * Note: for anonymous ontology objects (i.e. for not OWL Entities) such behaviour differs from the OWL2 specification.
     *
     * @return {@link OntStatement} or {@code null}
     */
    @Override
    OntStatement getRoot();

    /**
     * Lists the content of this object, i.e. its all characteristic statements.
     * The same as {@link #spec()}, but each element of that stream supports OWL2 annotations.
     *
     * @return Stream of {@link OntStatement Ontology Statement}s
     * @see #spec()
     */
    Stream<OntStatement> content();

    /**
     * Adds an ont-statement by attaching predicate and object (value) to this resource.
     *
     * @param property {@link Property} predicate, not null
     * @param value,   {@link RDFNode} object, not null
     * @return {@link OntStatement}
     * @see Resource#addProperty(Property, RDFNode)
     */
    OntStatement addStatement(Property property, RDFNode value);

    /**
     * Removes an associated statement with given predicate and object.
     * Does nothing in case no match found.
     *
     * @param property {@link Property} predicate, not null
     * @param object   {@link RDFNode} object, not null
     * @return this object to allow cascading calls
     * @see #addStatement(Property, RDFNode)
     */
    OntObject remove(Property property, RDFNode object);

    /**
     * Returns the <b>first</b> statement for the specified property and object.
     * What exactly is the first triple is defined at the level of graph; in general it is unpredictable.
     * Also note, that common jena implementation of in-memory graph does not allow duplicated triples.
     *
     * @param property {@link Property}, the predicate
     * @param object   {@link RDFNode}, the object
     * @return {@link Optional} around {@link OntStatement}
     */
    Optional<OntStatement> statement(Property property, RDFNode object);

    /**
     * Returns the <b>first</b> statement for the specified property.
     * What is the first triple is defined at the level of graph.
     *
     * @param property {@link Property}
     * @return {@link Optional} around {@link OntStatement}
     * @see Resource#getProperty(Property)
     */
    Optional<OntStatement> statement(Property property);

    /**
     * Lists ont-statements by predicate.
     *
     * @param property {@link Property}, predicate
     * @return Stream of {@link OntStatement}s
     */
    Stream<OntStatement> statements(Property property);

    /**
     * Lists all top-level statements related to this object (i.e. with subject={@code this}).
     *
     * @return Stream of all statements
     */
    Stream<OntStatement> statements();

    /**
     * Lists all top-level annotations attached to the root statement of this object.
     * Each annotation can be plain (annotation property assertion) or bulk
     * (anonymous resource with type {@code owl:Axiom} or {@code owl:Annotation}, possibly with sub-annotations).
     * Sub-annotations are not included into the returned stream.
     * For not built-in ontology objects this is equivalent to the expression: {@code getRoot().annotations()}.
     *
     * @return Stream of {@link OntStatement}s that have an {@link OntNAP annotation property} as predicate
     * @see OntStatement#annotations()
     * @see OntAnnotation#assertions()
     */
    Stream<OntStatement> annotations();

    /**
     * Removes all associated annotations including nested.
     *
     * @return this object to allow cascading calls
     */
    OntObject clearAnnotations();

    /**
     * Lists all objects attached on the property to this object with the given type
     *
     * @param predicate {@link Property} predicate
     * @param type      Interface to find and cast
     * @param <O>       a class-type of rdf-node
     * @return Stream of {@link RDFNode RDF Node}s
     */
    <O extends RDFNode> Stream<O> objects(Property predicate, Class<O> type);

    /**
     * Returns the <b>first</b> statement for specified property.
     * What exactly is the first triple is defined at the level of graph; in general it is unpredictable.
     *
     * @param property {@link Property}, the predicate
     * @return {@link OntStatement}
     * @throws org.apache.jena.shared.PropertyNotFoundException if no such statement
     * @see Resource#getRequiredProperty(Property)
     */
    @Override
    OntStatement getRequiredProperty(Property property);

    /**
     * Answers iff this object has declaration triple {@code @this rdf:type @any}.
     *
     * @param type {@link Resource} to test
     * @return true if it has
     */
    default boolean hasType(Resource type) {
        try (Stream<Resource> types = types()) {
            return types.anyMatch(type::equals);
        }
    }

    /**
     * Lists all declarations (statements with {@code rdf:type} predicate).
     *
     * @return Stream of {@link Resource}s
     */
    default Stream<Resource> types() {
        return objects(RDF.type, Resource.class);
    }
}
