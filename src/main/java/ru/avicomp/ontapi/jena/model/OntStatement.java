/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * An Ont Statement. This is not a {@link org.apache.jena.rdf.model.Resource}.
 * OWL2 Annotations could be attached to this statement recursively.
 *
 * @see OntAnnotation
 * @see Statement
 * Created by @szuev on 13.11.2016.
 */
public interface OntStatement extends Statement {

    /**
     * Returns reference to the attached model.
     *
     * @return {@link OntGraphModel}
     */
    OntGraphModel getModel();

    /**
     * adds annotation.
     *
     * @param property Named annotation property.
     * @param value    RDFNode (uri-resource, literal or anonymous individual)
     * @return OntStatement for newly added annotation.
     * @throws OntJenaException in case input is incorrect.
     */
    OntStatement addAnnotation(OntNAP property, RDFNode value);

    /**
     * Gets attached annotations, empty stream if it is assertion annotation
     *
     * @return Stream of annotations, could be empty.
     */
    Stream<OntStatement> annotations();

    /**
     * Deletes the child annotation if present
     *
     * @param property annotation property
     * @param value    uri-resource, literal or anonymous individual
     * @throws OntJenaException in case input is incorrect.
     */
    void deleteAnnotation(OntNAP property, RDFNode value);

    /**
     * Presents the annotation statement as an annotation object if it is possible.
     * It works only for bulk annotations.
     *
     * @return Optional around of {@link OntAnnotation}.
     */
    Optional<OntAnnotation> asAnnotationResource();

    /**
     * Answers iff this statement is root
     *
     * @return true if root.
     * @see OntObject#getRoot()
     */
    boolean isRoot();

    /**
     * Answers iff this statement is in the base graph.
     *
     * @return true if local
     * @see OntObject#isLocal()
     */
    boolean isLocal();

    /**
     * An accessor method to return the subject of the statements.
     *
     * @return {@link OntObject}
     * @see Statement#getSubject()
     */
    @Override
    OntObject getSubject();

    /**
     * @return true if predicate is rdf:type
     */
    default boolean isDeclaration() {
        return RDF.type.equals(getPredicate());
    }

    /**
     * Answers iff this is an annotation assertion.
     *
     * @return true if predicate is {@link OntNAP}
     */
    default boolean isAnnotation() {
        return getPredicate().canAs(OntNAP.class);
    }

    /**
     * Answers iff this statement is a data-property assertion.
     *
     * @return true if predicate is {@link OntNDP}
     */
    default boolean isData() {
        return getPredicate().canAs(OntNDP.class);
    }

    /**
     * Answers iff this statement is an object-property assertion.
     *
     * @return true if predicate is {@link OntNOP}
     */
    default boolean isObject() {
        return getPredicate().canAs(OntNOP.class);
    }

    /**
     * Removes all sub-annotations including their children.
     */
    default void clearAnnotations() {
        Set<OntStatement> children = annotations().collect(Collectors.toSet());
        children.forEach(OntStatement::clearAnnotations);
        children.forEach(a -> deleteAnnotation(a.getPredicate().as(OntNAP.class), a.getObject()));
    }

    /**
     * Answers iff this statement has annotations attached
     *
     * @return true if it is annotated.
     */
    default boolean hasAnnotations() {
        return annotations().count() != 0;
    }

    /**
     * Adds lang annotation assertion.
     *
     * @param predicate {@link OntNAP}, not null
     * @param message   String, the text message, not null.
     * @param lang      String, language, optional
     * @return {@link OntStatement}
     * @see OntObject#addAnnotation(OntNAP, String, String)
     */
    default OntStatement addAnnotation(OntNAP predicate, String message, String lang) {
        return addAnnotation(predicate, ResourceFactory.createLangLiteral(message, lang));
    }
}
