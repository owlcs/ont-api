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

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.stream.Stream;

/**
 * A common super-type for all of the abstractions in this ontology representation package.
 * This is an analogue of the {@link org.apache.jena.ontology.OntResource Jena OntResource} interface,
 * but for {@link OntGraphModel OWL2 Ontology RDF Graph Model}.
 * <p>
 * Created by @szuev on 24.07.2018.
 *
 * @since 1.3.0
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
     * Determines if this Ontology Resource is local defined.
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
     * the result would contain all statements in the graph directly related to the object
     * but without statements that relate to the object components.
     * The return stream is ordered and, in most cases,
     * the expression {@code this.spec().findFirst().get()} returns the same statement as {@code this.getRoot()}.
     * Object annotations are not included to the resultant stream.
     *
     * @return Stream of {@link Statement Jena Statement}s that fully describe this object in OWL2 terms
     */
    Stream<? extends Statement> spec();

}
