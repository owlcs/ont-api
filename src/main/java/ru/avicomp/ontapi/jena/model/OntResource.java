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

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import ru.avicomp.ontapi.jena.OntJenaException;

import java.util.stream.Stream;

/**
 * A common super-type for all of the abstractions in this ontology representation package.
 * An an analogue of {@link org.apache.jena.ontology.OntResource Jena Ont Resource}, but for {@link OntGraphModel OWL2 Ontology Graph Model}.
 * <p>
 * Created by @szuev on 24.07.2018.
 *
 * @since 1.2.1
 */
interface OntResource extends Resource {

    /**
     * Returns the ontology model associated with this resource.
     * If the Resource was not created by a Model, the result may be null.
     *
     * @return {@link OntGraphModel}
     */
    OntGraphModel getModel();

    /**
     * Returns the root declaration in the form of an ontology statement with supporting adding/removing OWL annotations.
     * It is the main triple in the model which determines the ontology resource.
     * Usually it is type-declaration (i.e. the triple with predicate {@code rdf:type} and with this resource as subject).
     * The result may be null in case of built-in OWL entities.
     *
     * @return {@link OntStatement} or {@code null}
     */
    OntStatement getRoot();

    /**
     * Determines if the Ontology Resource is local defined.
     * This means that the resource definition (i.e. a the {@link #getRoot() root statement})
     * belongs to the base ontology graph.
     * If the ontology contains sub-graphs (which should match {@code owl:imports} in OWL)
     * and the resource is defined in one of them,
     * than this method called from top-level interface will return {@code false}.
     *
     * @return {@code true} if this resource is local to the base model graph.
     */
    boolean isLocal();

    /**
     * Lists all characteristic statements of the ontology resource,
     * i.e. all those statements which completely determine this object nature according to the OWL2 specification.
     * For non-composite objects the result might contain only the {@link #getRoot() root statement}.
     * For composite objects (usually anonymous resources: disjoint sections, class expression, etc)
     * the result would contain all directly related to it statements in the graph
     * but without statements that relate to the object components.
     * The return stream is ordered and, in most cases,
     * the expression {@code this.spec().findFirst().get()} returns the same statement as {@code this.getRoot()}.
     * Object annotations are not included to the resultant stream.
     *
     * @return Stream of {@link Statement Jena Statement}s that fully describe this object in OWL2 terms
     */
    Stream<? extends Statement> spec();

    /**
     * Adds an annotation assertion.
     *
     * @param property {@link OntNAP} - named annotation property
     * @param value    {@link RDFNode} - the value: uri-resource, literal or anonymous individual
     * @return {@link OntStatement} for newly added annotation
     * @throws OntJenaException in case input is wrong
     */
    OntStatement addAnnotation(OntNAP property, RDFNode value);

    /**
     * Removes all associated annotations including nested.
     *
     * @return this object to allow cascading calls
     */
    OntResource clearAnnotations();

    /**
     * Adds lang-string annotation assertion.
     *
     * @param predicate {@link OntNAP} predicate
     * @param message   String, text message
     * @param lang      String, language, nullable
     * @return {@link OntStatement}
     */
    default OntStatement addAnnotation(OntNAP predicate, String message, String lang) {
        return addAnnotation(predicate, getModel().createLiteral(message, lang));
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
     * Adds text lang annotation with built-in {@code rdfs:comment} predicate.
     *
     * @param txt  String, the message
     * @param lang String, the language, nullable.
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
     * @param txt  String, the message
     * @param lang String, the language, nullable
     * @return {@link OntStatement}
     */
    default OntStatement addLabel(String txt, String lang) {
        return addAnnotation(getModel().getRDFSLabel(), txt, lang);
    }
}
