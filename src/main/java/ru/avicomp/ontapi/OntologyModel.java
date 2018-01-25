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

package ru.avicomp.ontapi;

import org.semanticweb.owlapi.model.OWLMutableOntology;
import org.semanticweb.owlapi.model.OWLOntology;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * OWL 2 Ontology.
 * It is access point to the structural (OWL) representation of underlying graph.
 * Following methods are new:
 * <ul>
 *     <li>{@link #asGraphModel()}</li>
 *     <li>{@link #clearCache()}</li>
 * </ul>
 * @see OWLOntology
 * Created by szuev on 24.10.2016.
 */
public interface OntologyModel extends OWLOntology, OWLMutableOntology {

    /**
     * Returns the jena model shadow, i.e. the interface to work with the graph directly.
     *
     * @return {@link OntGraphModel}
     */
    OntGraphModel asGraphModel();

    /**
     * Clears the axioms and entities cache.
     * <p>
     * The cache are restored by recalling method {@link #axioms()}, which is called by any other axioms getter.
     * This method is necessary to obtain the list of axioms which uniquely correspond to the graph,
     * since OWL-API allows some ambiguity in the axioms definition.
     * In the structural view there could be composite and bulky axioms specified,
     * which can be replaced by different set of axioms without any loss of information.
     * This method allows to bring structural representation to the one strictly defined (by inner implementation) form.
     * An example.
     * Consider the ontology which contains only the following two axioms:
     * <pre>
     *  SubClassOf(Annotation(&lt;p&gt; "comment1"^^xsd:string) &lt;a&gt; &lt;b&gt;)
     *  Declaration(Annotation(rdfs:label "label"^^xsd:string) Datatype(&lt;d&gt;))
     * </pre>
     * After re-caching the full list of axioms would be the following:
     * <pre>
     *  Declaration(Class(&lt;a&gt;))
     *  Declaration(Class(&lt;b&gt;))
     *  Declaration(AnnotationProperty(&lt;p&gt;))
     *  Declaration(Datatype(&lt;d&gt;))
     *  SubClassOf(Annotation(&lt;p&gt; "comment"^^xsd:string) &lt;a&gt; &lt;b&gt;)
     *  AnnotationAssertion(rdfs:label &lt;d&gt; "label"^^xsd:string)
     * </pre>
     * Note: the loading behaviour and the axioms list above may vary according to various config settings,
     * for more details see {@link ru.avicomp.ontapi.config.OntLoaderConfiguration}.
     */
    void clearCache();

    /**
     * Returns the manager.
     *
     * @return {@link OntologyManager}
     */
    OntologyManager getOWLOntologyManager();

}
