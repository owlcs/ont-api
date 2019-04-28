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

import java.util.stream.Stream;

/**
 * A Bulk Annotation {@link OntObject Ontology Object}.
 * It's an anonymous jena-resource with one of the two types:
 * <ul>
 * <li>{@link ru.avicomp.ontapi.jena.vocabulary.OWL#Axiom owl:Axiom} for root annotations, it is usually owned by axiomatic statements.</li>
 * <li>{@link ru.avicomp.ontapi.jena.vocabulary.OWL#Annotation owl:Annotation} for sub-annotations,
 * and also for annotation of several specific axioms with main-statement {@code _:x rdf:type @type} where @type is
 * {@code owl:AllDisjointClasses}, {@code owl:AllDisjointProperties}, {@code owl:AllDifferent} or {@code owl:NegativePropertyAssertion}.</li>
 * </ul>
 * Example:
 * <pre>{@code
 * [ a                      owl:Axiom ;
 *   rdfs:comment           "some comment 1", "some comment 2"@fr ;
 *   owl:annotatedProperty  rdf:type ;
 *   owl:annotatedSource    <Class> ;
 *   owl:annotatedTarget    owl:Class
 * ] .
 * }</pre>
 * <p>
 * Created by @szuev on 26.03.2017.
 *
 * @see OntStatement
 * @see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.2 Translation of Annotations</a>
 */
public interface OntAnnotation extends OntObject {

    /**
     * Returns the base statement, i.e. statement to which this bulk-annotation is attached.
     * In the example above it is the statement {@code <Class> rdf:type owl:Class}.
     *
     * @return {@link OntStatement}
     */
    OntStatement getBase();

    /**
     * Returns the annotations assertions attached to this annotation resource.
     * The annotation assertion is a statements with an {@link OntNAP annotation property} as predicate.
     * The example above contains two such statements:
     * {@code _:x rdfs:comment "some comment 1"} and {@code _:x rdfs:comment "some comment 2"@fr}.
     *
     * @return Stream of annotation statements {@link OntStatement}s
     * @see OntObject#annotations()
     */
    Stream<OntStatement> assertions();

    /**
     * Lists all descendants of this ont-annotation resource.
     * The resulting resources must have {@link ru.avicomp.ontapi.jena.vocabulary.OWL#Annotation owl:Annotation} type
     * and this object on predicate {@link ru.avicomp.ontapi.jena.vocabulary.OWL#annotatedSource owl:annotatedSource}.
     *
     * @return Stream of {@link OntAnnotation}s
     * @since 1.3.0
     */
    Stream<OntAnnotation> descendants();

    /**
     * Just a synonym for {@link #assertions()}.
     *
     * @return Stream of annotation statements {@link OntStatement}s
     */
    @Override
    Stream<OntStatement> annotations();

    /**
     * Adds a new annotation assertion to this annotation resource.
     * If this {@link OntAnnotation} contains annotation property assertion {@code this x y}
     * and it does not have sub-annotations yet,
     * the given annotation property {@code p} and value {@code v} will produce following {@link OntAnnotation} object:
     * <pre>{@code
     * _:x rdf:type              owl:Annotation .
     * _:x p                     v .
     * _:x owl:annotatedSource   this .
     * _:x owl:annotatedProperty x .
     * _:x owl:annotatedTarget   y .
     * }</pre>
     * and this method will return {@code _:x p v} triple wrapped as {@link OntStatement}
     * to allow adding subsequent sub-annotations.
     * If this annotation object already has a sub-annotation for the statement {@code this x y},
     * the new triple will be added to the existing anonymous resource.
     *
     * @param property {@link OntNAP}
     * @param value    {@link RDFNode}
     * @return {@link OntStatement} - an annotation assertion belonging to this object
     * @see OntStatement#addAnnotation(OntNAP, RDFNode)
     * @see OntObject#addAnnotation(OntNAP, RDFNode)
     */
    @Override
    OntStatement addAnnotation(OntNAP property, RDFNode value);

}
