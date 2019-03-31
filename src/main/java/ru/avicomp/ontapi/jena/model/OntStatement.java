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

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An Ontology RDF {@link Statement}.
 * <p>
 * This is <b>not</b> a {@link org.apache.jena.rdf.model.Resource}.
 * This is an extended {@link Statement Jena Model Statement} with possibility to add, delete and find annotations
 * in the same form of {@code OntStatement} or {@link OntAnnotation Ontology Annotation} resources,
 * <p>
 * Created by @szuev on 13.11.2016.
 *
 * @see OntAnnotation
 * @see Statement
 * @see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.2 Translation of Annotations</a>
 */
public interface OntStatement extends Statement {

    /**
     * Gets the {@link OntGraphModel Ontology RDF Model} this {@link OntStatement Statement} was created in.
     *
     * @return {@link OntGraphModel}
     */
    @Override
    OntGraphModel getModel();

    /**
     * Annotates the statement with the given {@link OntNAP annotation property}
     * and any {@link RDFNode RDF Node} as value and returns it as the newly added annotation assertion statement.
     * In special case of root statement (i.e. if this statement is result of {@link OntObject#getRoot()})
     * the returned ont-statement (called a plain annotation) has the same subject as this statement,
     * otherwise it is an annotation assertion from a fresh or existing {@link OntAnnotation bulk annotation}
     * and it has a blank node as a subject.
     *
     * @param property {@link OntNAP} named annotation property, not null
     * @param value    {@link RDFNode} uri-resource, literal or anonymous individual, not null
     * @return {@link OntStatement Ont-Statement} for newly added annotation
     * @throws OntJenaException in case input is incorrect
     * @see OntAnnotation#addAnnotation(OntNAP, RDFNode)
     * @see OntObject#getRoot()
     */
    OntStatement addAnnotation(OntNAP property, RDFNode value);

    /**
     * Lists all annotations related to the statement.
     * The returned stream consists of annotation assertions listed from the top-level bulk annotations and
     * plain annotation assertions in the special case of the root statement.
     *
     * @return Stream (unordered) of {@link OntStatement annotation assertion statements}
     * with {@link OntNAP annotation property} as predicates, can be empty
     * @see #asAnnotationResource()
     */
    Stream<OntStatement> annotations();

    /**
     * Deletes the child annotation if present.
     * Does nothing if no assertion found.
     * Throws an exception if specified annotation has it is own annotations.
     * If this statement is not root and corresponding {@link OntAnnotation} resource has no assertions any more,
     * it deletes the whole OntAnnotation resource also.
     *
     * @param property {@link OntNAP} named annotation property, not null
     * @param value    {@link RDFNode} uri-resource, literal or anonymous individual, not null
     * @return this statement instance to allow cascading calls
     * @throws OntJenaException in case input is incorrect or deleted annotation has it is own annotations
     */
    OntStatement deleteAnnotation(OntNAP property, RDFNode value) throws OntJenaException;

    /**
     * Returns the stream of the annotation objects attached to this statement.
     * E.g. for the statement {@code s A t} the annotation object looks like
     * <pre>{@code
     * _:b0 a owl:Axiom .
     * _:b0 Aj tj .
     * _:b0 owl:annotatedSource s .
     * _:b0 owl:annotatedProperty A .
     * _:b0 owl:annotatedTarget t .
     * }</pre>
     * Technically, although it does not make sense, it is possible that the given statement has several such b-nodes.
     *
     * @return Stream (unordered) of {@link OntAnnotation} resources
     * @see #asAnnotationResource() to get first annotation-object
     * @see #getAnnotationList() to get all annotation-objects in a fixed order
     */
    Stream<OntAnnotation> annotationResources();

    /**
     * Returns the list of the annotation objects attached to this statement
     * in the form of {@link List} with a fixed order.
     *
     * @return {@link List} of {@link OntAnnotation Ontology Annotation}s
     * @see #annotationResources()
     * @since 1.3.0
     */
    List<OntAnnotation> getAnnotationList();

    /**
     * Answers {@code true} iff this statement is a root (i.e. it is a main definition of an OntObject).
     *
     * @return {@code true} if it is a root object statement
     * @see OntResource#getRoot()
     */
    boolean isRoot();

    /**
     * Answers {@code true} if this statement is in the base graph.
     * It is equivalent to the expression {@code this.getModel().getBaseGraph().contains(this.asTriple())}.
     *
     * @return {@code true} if local
     * @see OntResource#isLocal()
     */
    boolean isLocal();

    /**
     * An accessor method to return the subject of the statements in form of {@link OntObject Ontology Object}.
     *
     * @return {@link OntObject}
     * @see Statement#getSubject()
     */
    @Override
    OntObject getSubject();

    /**
     * Returns the primary annotation object (resource) which is related to this statement.
     * It is assumed that this method always returns the same result if no changes in graph is made,
     * even after graph reloading.
     *
     * @return Optional around of {@link OntAnnotation}, can be empty
     * @see #getAnnotationList()
     */
    default Optional<OntAnnotation> asAnnotationResource() {
        List<OntAnnotation> res = this.getAnnotationList();
        return res.isEmpty() ? Optional.empty() : Optional.of(res.get(0));
    }

    /**
     * Lists all annotations by the property.
     *
     * @param property {@link OntNAP} the property
     * @return Stream of {@link OntStatement}s
     */
    default Stream<OntStatement> annotations(OntNAP property) {
        return annotations().filter(s -> Objects.equals(property, s.getPredicate()));
    }

    /**
     * Deletes all (sub-)annotations by the given property.
     *
     * @param property {@link OntNAP}
     */
    default void deleteAnnotation(OntNAP property) {
        annotations(property).map(Statement::getObject)
                .collect(Collectors.toSet())
                .forEach(v -> deleteAnnotation(property, v));
    }

    /**
     * Answers iff this statement is a declaration: {@code @any rdf:type @any}.
     *
     * @return {@code true} if predicate is rdf:type
     */
    default boolean isDeclaration() {
        return RDF.type.equals(getPredicate());
    }

    /**
     * Answers {@code true} if this is an annotation assertion.
     * Annotation assertion is a statement {@code s A t}, where
     * {@code s} is an IRI or anonymous individual,
     * {@code t} is an IRI, anonymous individual, or literal,
     * and {@code A} is an annotation property.
     *
     * @return {@code true} if predicate is {@link OntNAP}
     */
    default boolean isAnnotation() {
        return getPredicate().canAs(OntNAP.class);
    }

    /**
     * Answers {@code true} if this statement is a part of {@link OntAnnotation Bulk Annotation Ontology Object}.
     * This means that it is one of the following:
     * <pre>{@code
     * _:x rdf:type              owl:Annotation .
     * _:x p                     v .
     * _:x owl:annotatedSource   this .
     * _:x owl:annotatedProperty x .
     * _:x owl:annotatedTarget   y .
     * }</pre>
     *
     * @return boolean, {@code true} if it is a part of bulk annotation object
     */
    default boolean isBulkAnnotation() {
        return getSubject().canAs(OntAnnotation.class);
    }

    /**
     * Answers {@code true} if the predicate is a data(-type) property and
     * therefore this statement is likely a positive data property assertion.
     * A positive data property assertion if a statement {@code a R v},
     * where {@code a} is an individual (both named and anonymous),
     * {@code v} is a literal and {@code R} is a data property.
     *
     * @return true if predicate is {@link OntNDP}
     */
    default boolean isData() {
        return getPredicate().canAs(OntNDP.class);
    }

    /**
     * Answers {@code true} if there is an object property as a predicate and
     * therefore this statement is likely to be a positive object property assertion {@code a1 PN a2}.
     *
     * @return {@code true} if predicate is {@link OntNOP}
     */
    default boolean isObject() {
        return getPredicate().canAs(OntNOP.class);
    }

    /**
     * Removes all sub-annotations including their children.
     *
     * @see OntObject#clearAnnotations()
     */
    default void clearAnnotations() {
        annotations()
                .peek(OntStatement::clearAnnotations)
                .collect(Collectors.toSet())
                .forEach(a -> deleteAnnotation(a.getPredicate().as(OntNAP.class), a.getObject()));
    }

    /**
     * Answers {@code true} if this statement has any annotations attached (either plain or bulk).
     *
     * @return {@code true} if it is annotated
     */
    default boolean hasAnnotations() {
        try (Stream<OntStatement> annotations = annotations()) {
            return annotations.findFirst().isPresent();
        }
    }

    /**
     * Adds String sub-annotation assertion.
     *
     * @param predicate {@link OntNAP}, not null
     * @param text      String, the text message, not null.
     * @return {@link OntStatement}, new instance
     * @see OntObject#addAnnotation(OntNAP, String, String)
     */
    default OntStatement addAnnotation(OntNAP predicate, String text) {
        return addAnnotation(predicate, text, null);
    }

    /**
     * Adds lang sub-annotation assertion.
     *
     * @param predicate {@link OntNAP}, not null
     * @param text      String, the text message, not null.
     * @param lang      String, language, optional
     * @return {@link OntStatement}, new instance
     * @see OntObject#addAnnotation(OntNAP, String, String)
     */
    default OntStatement addAnnotation(OntNAP predicate, String text, String lang) {
        return addAnnotation(predicate, getModel().createLiteral(text, lang));
    }

    /**
     * Gets a typed statement subject.
     *
     * @param type Class type
     * @param <N>  subtype of {@link RDFNode}
     * @return {@link RDFNode} instance
     * @throws org.apache.jena.enhanced.UnsupportedPolymorphismException if subject node and type are incompatible
     * @since 1.3.0
     */
    default <N extends RDFNode> N getSubject(Class<N> type) {
        return getSubject().as(type);
    }

}
