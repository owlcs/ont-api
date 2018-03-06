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
 *
 */

package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import ru.avicomp.ontapi.jena.OntJenaException;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base Ont Resource.
 * The analogue of {@link org.apache.jena.ontology.OntResource}
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntObject extends Resource {

    /**
     * Returns reference to the attached model.
     *
     * @return {@link OntGraphModel}
     */
    @Override
    OntGraphModel getModel();

    /**
     * Determines is ont-object resource local defined (i.e. does not belong to any graph from imports).
     *
     * @return true if this resource is local to the base graph.
     */
    boolean isLocal();

    /**
     * Returns the root statement, i.e. the main triple in model which determines this object.
     * usually it is declaration (statement with predicate rdf:type)
     *
     * @return OntStatement or null
     */
    OntStatement getRoot();

    /**
     * Returns the content of object: all characteristic statements,
     * i.e. all those statements which determine this object.
     * todo: rename?
     * For non-composite objects the result might contain only the root statement.
     * For composite (usually anonymous, e.g. disjoint section, class expression, etc) objects
     * the result would contain all statements in the graph but without statements related to the components.
     *
     * @return Stream of associated with this object statements
     */
    Stream<OntStatement> content();

    /**
     * Adds ont-statement
     *
     * @param property {@link Property} predicate, not null
     * @param value,   {@link RDFNode} object, not null
     * @return {@link OntStatement}
     * @see Resource#addProperty(Property, RDFNode)
     */
    OntStatement addStatement(Property property, RDFNode value);

    /**
     * Removes statement
     *
     * @param property {@link Property} predicate, not null
     * @param object,  {@link RDFNode} object, not null
     * @see #addStatement(Property, RDFNode)
     */
    void remove(Property property, RDFNode object);

    /**
     * Returns the <b>first</b> statement for specified property and object.
     *
     * @param property {@link Property}, the predicate
     * @param object   {@link RDFNode}, the object
     * @return {@link Optional} around {@link OntStatement}
     */
    Optional<OntStatement> statement(Property property, RDFNode object);

    /**
     * Returns the <b>first</b> statement for specified property.
     *
     * @param property {@link Property}
     * @return {@link Optional} around {@link OntStatement}
     * @see Resource#getProperty(Property)
     */
    Optional<OntStatement> statement(Property property);


    /**
     * Returns ont-statements by predicate
     *
     * @param property {@link Property}, predicate
     * @return Stream of {@link OntStatement}s.
     */
    Stream<OntStatement> statements(Property property);

    /**
     * Returns all statements related to this object (i.e. with subject=this)
     *
     * @return Stream of all statements.
     */
    Stream<OntStatement> statements();

    /**
     * Gets the stream of all objects attached on property to this ont-object.
     *
     * @param predicate {@link Property} predicate
     * @param view      Interface to find and cast
     * @param <O> a class-type of rdf-node
     * @return Stream of objects ({@link RDFNode}s)
     */
    <O extends RDFNode> Stream<O> objects(Property predicate, Class<O> view);

    /**
     * Returns the <b>first</b> for specified property
     *
     * @param property {@link Property}, the predicate
     * @return {@link OntStatement}
     * @throws org.apache.jena.shared.PropertyNotFoundException if no such statement
     * @see Resource#getRequiredProperty(Property)
     */
    @Override
    OntStatement getRequiredProperty(Property property);

    /**
     * Returns all declarations (statements with rdf:type predicate)
     *
     * @return Stream of {@link Resource}s
     */
    Stream<Resource> types();

    /**
     * Answers if this object has specified rdf:type
     *
     * @param type {@link Resource} to test
     * @return true if it has.
     */
    boolean hasType(Resource type);

    /**
     * Returns the stream of all annotations attached to this object (not only to main-triple).
     * Each annotation could be plain (assertion) or bulk annotation (with/without sub-annotations).
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
     * Adds annotation assertion.
     * it could be expanded to bulk form by adding sub-annotation.
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
     * Adds lang-string annotation assertion
     *
     * @param predicate {@link OntNAP} predicate
     * @param message   String, text message
     * @param lang      String, language, nullable.
     * @return {@link OntStatement}
     */
    default OntStatement addAnnotation(OntNAP predicate, String message, String lang) {
        return addAnnotation(predicate, ResourceFactory.createLangLiteral(message, lang));
    }

    /**
     * Adds text annotation with builtin rdfs:comment predicate.
     * for simplification.
     *
     * @param txt,  String, the message
     * @param lang, String, the language, nullable.
     * @return {@link OntStatement}
     */
    default OntStatement addComment(String txt, String lang) {
        return addAnnotation(getModel().getRDFSComment(), txt, lang);
    }

    /**
     * Adds text annotation with builtin rdfs:comment predicate.
     * for simplification.
     *
     * @param txt,  String, the message
     * @param lang, String, the language, nullable.
     * @return {@link OntStatement}
     */
    default OntStatement addLabel(String txt, String lang) {
        return addAnnotation(getModel().getRDFSLabel(), txt, lang);
    }

    /**
     * Removes all associated annotations included nested.
     */
    default void clearAnnotations() {
        Set<OntStatement> annotated = statements().filter(OntStatement::hasAnnotations).collect(Collectors.toSet());
        annotated.forEach(OntStatement::clearAnnotations);
        annotations().forEach(a -> removeAll(a.getPredicate()));
    }
}
