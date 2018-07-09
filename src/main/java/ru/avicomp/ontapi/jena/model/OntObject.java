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
import org.apache.jena.rdf.model.ResourceFactory;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A base Ontology RDF Resource, a common super-type for all of the abstractions in the {@link OntGraphModel ontology},
 * which can be annotated and/or have some strictly defined by the specification structure.
 * It is an analogue of {@link org.apache.jena.ontology.OntResource}.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntObject extends Resource {

    /**
     * Return the model associated with this resource.
     * If the Resource was not created by a Model, the result may be null.
     *
     * @return {@link OntGraphModel}
     */
    @Override
    OntGraphModel getModel();

    /**
     * Determines if this Resource is local defined (i.e. does not belong to any other graphs from the model imports).
     *
     * @return {@code true} if this resource is local to the base model graph.
     */
    boolean isLocal();

    /**
     * Returns a root statement, i.e. the main triple in the model which determines this object.
     * Usually it is declaration (the statement with predicate {@code rdf:type}).
     *
     * @return OntStatement or {@code null}
     */
    OntStatement getRoot();

    /**
     * Lists all characteristic statements of the object,
     * i.e. all those statements which determine this object nature according to OWL2 specification.
     * For non-composite objects the result might contain only the root statement.
     * For composite objects (usually anonymous resources: disjoint sections, class expression, etc)
     * the result would contain all directly related to it statements in the graph but without statements that relate to the object components.
     *
     * @return Stream of statements that fully describe this object in OWL2 terms
     */
    Stream<OntStatement> spec();

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

    /**
     * Returns a stream of all annotations attached to this object (not only to the main-triple).
     * Each annotation could be plain (assertion) or bulk (with/without sub-annotations).
     * Sub-annotations are not included to this stream.
     * <p>
     * According to OWL2-DL specification OntObject should be an uri-resource (i.e. not anonymous),
     * but we extend this behaviour for more generality.
     *
     * @return Stream of {@link OntStatement}s, each of them has as key {@link OntNAP} and as value any {@link RDFNode}.
     */
    default Stream<OntStatement> annotations() {
        return statements().map(OntStatement::annotations).flatMap(Function.identity());
    }

    /**
     * Adds an annotation assertion.
     * It could be expanded to bulk form by adding sub-annotation.
     *
     * @param property {@link OntNAP}, Named annotation property.
     * @param value    {@link RDFNode} the value: uri-resource, literal or anonymous individual.
     * @return OntStatement for newly added annotation.
     * @throws OntJenaException in case input is wrong.
     */
    default OntStatement addAnnotation(OntNAP property, RDFNode value) {
        return getRoot().addAnnotation(property, value);
    }

    /**
     * Adds lang-string annotation assertion.
     *
     * @param predicate {@link OntNAP} predicate
     * @param message   String, text message
     * @param lang      String, language, nullable
     * @return {@link OntStatement}
     */
    default OntStatement addAnnotation(OntNAP predicate, String message, String lang) {
        return addAnnotation(predicate, ResourceFactory.createLangLiteral(message, lang));
    }

    /**
     * Creates {@code _:this rdfs:comment "txt"^^xsd:string} statement.
     *
     * @param txt String
     * @return {@link OntStatement}
     */
    default OntStatement addComment(String txt) {
        return addComment(txt, null);
    }

    /**
     * Adds text lang annotation with builtin {@code rdfs:comment} predicate.
     *
     * @param txt,  String, the message
     * @param lang, String, the language, nullable.
     * @return {@link OntStatement}
     */
    default OntStatement addComment(String txt, String lang) {
        return addAnnotation(getModel().getRDFSComment(), txt, lang);
    }

    /**
     * Creates {@code _:this rdfs:label "txt"^^xsd:string} statement.
     *
     * @param txt String
     * @return {@link OntStatement}
     */
    default OntStatement addLabel(String txt) {
        return addLabel(txt, null);
    }

    /**
     * Adds text lang annotation with builtin {@code rdfs:label} predicate.
     *
     * @param txt,  String, the message
     * @param lang, String, the language, nullable.
     * @return {@link OntStatement}
     */
    default OntStatement addLabel(String txt, String lang) {
        return addAnnotation(getModel().getRDFSLabel(), txt, lang);
    }

    /**
     * Removes all associated annotations including nested.
     *
     * @return this object to allow cascading calls
     */
    default OntObject clearAnnotations() {
        Set<OntStatement> annotated = statements().filter(OntStatement::hasAnnotations).collect(Collectors.toSet());
        annotated.forEach(OntStatement::clearAnnotations);
        annotations().forEach(a -> removeAll(a.getPredicate()));
        return this;
    }
}
