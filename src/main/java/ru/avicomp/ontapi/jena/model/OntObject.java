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

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A base {@link OntResource Ontology Object RDF Resource}.
 * A common super-type for all of the abstractions in the {@link OntGraphModel Ontology RDF Model},
 * which support Jena Polymorphism,
 * can be annotated and have a structure that is strictly defined according to the OWL2 specification.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntObject extends OntResource {

    /**
     * Returns the ontology object declaration statement.
     * In most cases it wraps a triple with predicate {@code rdf:type}.
     * Note that the returned ont-statement differs from that
     * which could be obtained directly from the model using one of its {@code statement(..)} method:
     * an statement's annotations
     * are added in the form of annotation property assertions (so-called 'plain annotations'),
     * not as typed anonymous resources (so-called 'bulk annotations').
     * Note: for anonymous ontology objects (i.e. for not OWL Entities) this behaviour of root statement may not meet
     * OWL2 specification:
     * it describes only bulk annotations for all anonymous OWL2 components with except of an individual.
     * To get a common ontology statement with support bulk annotations
     * the expression {@code getModel().asStatement(this.getRoot().asTriple())} can be used.
     * It is legal for a root statement to have both plain and bulk annotations.
     *
     * @return {@link OntStatement} or {@code null} in some boundary cases (e.g. for built-ins)
     * @see OntGraphModel#asStatement(Triple)
     * @see OntStatement#addAnnotation(OntNAP, RDFNode)
     */
    @Override
    OntStatement getRoot();

    /**
     * Lists all objects characteristic statements according to its OWL2 specification.
     * For OWL Entities the returned stream will contain only single root statement (see {@link #getRoot()}),
     * or even will be empty for built-in entities.
     *
     * @return Stream of {@link OntStatement Ontology Statement}s
     */
    @Override
    Stream<OntStatement> spec();

    /**
     * Lists the content of the object, i.e. its all characteristic statements (see {@link #spec()}),
     * plus all the additional statements in which this object is the subject,
     * minus those of them whose predicate is an annotation property.
     *
     * @return Stream of {@link OntStatement Ontology Statement}s
     * @see #spec()
     */
    Stream<OntStatement> content();

    /**
     * Adds an ont-statement by attaching predicate and object (value) to this resource.
     *
     * @param property {@link Property} predicate, not {@code null}
     * @param value,   {@link RDFNode} object, not {@code null}
     * @return {@link OntStatement}
     * @see Resource#addProperty(Property, RDFNode)
     */
    OntStatement addStatement(Property property, RDFNode value);

    /**
     * Deletes the specific property-value pair from this object.
     * All of the corresponding statement's annotations is also deleted.
     * In case the given {@code object} is {@code null},
     * all statements with the {@code property}-predicate will be deleted.
     * No-op if no match found.
     *
     * @param property {@link Property} predicate, not {@code null}
     * @param object   {@link RDFNode} object, <b>can be {@code null}</b>
     * @return this object to allow cascading calls
     * @see #addStatement(Property, RDFNode)
     * @see OntStatement#clearAnnotations()
     */
    OntObject remove(Property property, RDFNode object);

    /**
     * Returns the <b>first</b> statement for the specified property and object.
     * What exactly is the first triple is defined at the level of graph; in general it is unpredictable.
     * Also note, that common jena implementation of in-memory graph does not allow duplicated triples,
     * and, therefore, there is be no more than one statement for the given {@code property} and {@code value}.
     *
     * @param property {@link Property}, the predicate
     * @param value    {@link RDFNode}, the object
     * @return {@link Optional} around {@link OntStatement}
     */
    Optional<OntStatement> statement(Property property, RDFNode value);

    /**
     * Returns the <b>first</b> statement for the specified property.
     * What is the first triple is defined at the level of graph; in general it is unpredictable.
     *
     * @param property {@link Property}
     * @return {@link Optional} around {@link OntStatement}
     * @see Resource#getProperty(Property)
     */
    Optional<OntStatement> statement(Property property);

    /**
     * Lists ont-statements by the predicate.
     *
     * @param property {@link Property}, predicate
     * @return Stream of {@link OntStatement}s
     */
    Stream<OntStatement> statements(Property property);

    /**
     * Lists all top-level statements related to this object (i.e. with subject={@code this}).
     *
     * @return Stream of all statements
     * @see #listProperties()
     */
    Stream<OntStatement> statements();

    /**
     * Adds an annotation assertion with the given {@link OntNAP annotation property} as predicate
     * and {@link RDFNode RDF Node} as value.
     * The method is equivalent to the expression {@code getRoot().addAnnotation(property, value)}.
     *
     * @param property {@link OntNAP} - named annotation property
     * @param value    {@link RDFNode} - the value: uri-resource, literal or anonymous individual
     * @return {@link OntStatement} for newly added annotation
     * to provide the possibility of adding subsequent sub-annotations
     * @throws OntJenaException in case input is wrong
     * @see OntStatement#addAnnotation(OntNAP, RDFNode)
     */
    OntStatement addAnnotation(OntNAP property, RDFNode value);

    /**
     * Lists all top-level annotations attached to the root statement of this object.
     * Each annotation can be plain (annotation property assertion) or bulk
     * (anonymous resource with the type {@code owl:Axiom} or {@code owl:Annotation}, possibly with sub-annotations).
     * Sub-annotations are not included into the returned stream.
     * For non-built-in ontology objects this is equivalent to the expression {@code getRoot().annotations()}.
     *
     * @return Stream of {@link OntStatement}s that have an {@link OntNAP annotation property} as predicate
     * @see OntStatement#annotations()
     * @see OntAnnotation#assertions()
     */
    Stream<OntStatement> annotations();

    /**
     * Lists all annotation literals for the given predicate and the language tag.
     * Literal tag comparison is case insensitive.
     * Partial search is also allowed, for example,
     * a literal with the tag {@code en-GB} will listed also if the input language tag is {@code en}.
     * An empty string as language tag means searching for plain no-language literals.
     *
     * @param predicate {@link OntNAP}, not {@code null}
     * @param lang      String, the language tag to restrict the listed literals to,
     *                  or {@code null} to select all literals
     * @return Stream of String's, i.e. literal lexical forms
     * @see #annotationValues(OntNAP)
     * @since 1.3.2
     */
    Stream<String> annotationValues(OntNAP predicate, String lang);

    /**
     * Removes all root annotations including their sub-annotations hierarchy.
     * Any non-root annotations are untouched.
     * For example, in case of deleting an OWL class,
     * if it is present on the left side of the {@code rdfs:subClassOf} statement,
     * all the annotations of that statement will remain in the graph,
     * but all root annotations (which belongs to the statement with the predicate {@code rdf:type})
     * will be deleted from the graph.
     * For non-built-in ontology objects this is equivalent to the expression {@code getRoot().clearAnnotations()}.
     *
     * @return this object to allow cascading calls
     * @see OntStatement#clearAnnotations()
     */
    OntObject clearAnnotations();

    /**
     * Lists all objects attached on the property to this object with the given type.
     *
     * @param predicate {@link Property} predicate
     * @param type      Interface to find and cast
     * @param <O>       a class-type of rdf-node
     * @return Stream of {@link RDFNode RDF Node}s
     */
    <O extends RDFNode> Stream<O> objects(Property predicate, Class<O> type);

    /**
     * Returns the <b>first</b> statement for the specified property.
     * What exactly is the first triple is defined at the level of graph and, in general, it is unpredictable.
     *
     * @param property {@link Property}, the predicate
     * @return {@link OntStatement}
     * @throws org.apache.jena.shared.PropertyNotFoundException if no such statement found
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
     * Lists all annotation values for the given predicate.
     *
     * @param predicate {@link OntNAP}, not {@code null}
     * @return Stream of {@link RDFNode}s
     * @see #annotations()
     * @since 1.3.2
     */
    default Stream<RDFNode> annotationValues(OntNAP predicate) {
        return annotations()
                .filter(s -> Objects.equals(predicate, s.getPredicate()))
                .map(Statement::getObject);
    }

    /**
     * Adds no-lang annotation assertion.
     *
     * @param predicate   {@link OntNAP} predicate
     * @param lexicalForm String, the literal lexical form, not {@code null}
     * @return {@link OntStatement}
     */
    default OntStatement addAnnotation(OntNAP predicate, String lexicalForm) {
        return addAnnotation(predicate, lexicalForm, null);
    }

    /**
     * Adds lang annotation assertion.
     *
     * @param predicate {@link OntNAP} predicate
     * @param txt       String, the literal lexical form, not {@code null}
     * @param lang      String, the language tag, nullable
     * @return {@link OntStatement} - new statement: {@code @subject @predicate "txt"@lang}
     */
    default OntStatement addAnnotation(OntNAP predicate, String txt, String lang) {
        return addAnnotation(predicate, getModel().createLiteral(txt, lang));
    }

    /**
     * Creates {@code _:this rdfs:comment "txt"^^xsd:string} statement.
     *
     * @param txt String, not {@code null}
     * @return {@link OntStatement} to allow the subsequent annotate
     * @see OntGraphModel#getRDFSComment()
     */
    default OntStatement addComment(String txt) {
        return addComment(txt, null);
    }

    /**
     * Adds the given localized text annotation with builtin {@code rdfs:comment} predicate.
     *
     * @param txt  String, the literal lexical form, not {@code null}
     * @param lang String, the language tag, nullable
     * @return {@link OntStatement} to allow the subsequent annotate
     * @see OntGraphModel#getRDFSComment()
     */
    default OntStatement addComment(String txt, String lang) {
        return addAnnotation(getModel().getRDFSComment(), txt, lang);
    }

    /**
     * Creates {@code _:this rdfs:label "txt"^^xsd:string} statement.
     *
     * @param txt String, the literal lexical form, not {@code null}
     * @return {@link OntStatement} to allow the subsequent annotate
     * @see OntGraphModel#getRDFSLabel()
     */
    default OntStatement addLabel(String txt) {
        return addLabel(txt, null);
    }

    /**
     * Adds the given localized text annotation with builtin {@code rdfs:label} predicate.
     *
     * @param txt  String, the literal lexical form, not {@code null}
     * @param lang String, the language tag, nullable
     * @return {@link OntStatement} to allow the subsequent annotate
     * @see OntGraphModel#getRDFSLabel()
     */
    default OntStatement addLabel(String txt, String lang) {
        return addAnnotation(getModel().getRDFSLabel(), txt, lang);
    }

    /**
     * Answers the comment string for this object.
     * If there is more than one such resource, an arbitrary selection is made.
     *
     * @return a {@code rdfs:comment} string or {@code null} if there is no comments
     * @see OntGraphModel#getRDFSComment()
     */
    default String getComment() {
        return getComment(null);
    }

    /**
     * Answers the comment string for this object.
     * If there is more than one such resource, an arbitrary selection is made.
     *
     * @param lang String, the language attribute for the desired comment (EN, FR, etc) or {@code null} for don't care.
     *             Will attempt to retrieve the most specific comment matching the given language
     * @return a {@code rdfs:comment} string matching the given language,
     * or {@code null} if there is no matching comment
     * @see OntGraphModel#getRDFSComment()
     */
    default String getComment(String lang) {
        try (Stream<String> res = annotationValues(getModel().getRDFSComment(), lang)) {
            return res.findFirst().orElse(null);
        }
    }

    /**
     * Answers the label string for this object.
     * If there is more than one such resource, an arbitrary selection is made.
     *
     * @return a {@code rdfs:label} string or {@code null} if there is no comments
     * @see OntGraphModel#getRDFSLabel()
     */
    default String getLabel() {
        return getLabel(null);
    }

    /**
     * Answers the label string for this object.
     * If there is more than one such resource, an arbitrary selection is made.
     *
     * @param lang String, the language attribute for the desired comment (EN, FR, etc) or {@code null} for don't care.
     *             Will attempt to retrieve the most specific comment matching the given language
     * @return a {@code rdfs:label} string matching the given language, or {@code null}  if there is no matching label
     * @see OntGraphModel#getRDFSLabel()
     */
    default String getLabel(String lang) {
        try (Stream<String> res = annotationValues(getModel().getRDFSLabel(), lang)) {
            return res.findFirst().orElse(null);
        }
    }
}
