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

package ru.avicomp.ontapi;

import org.semanticweb.owlapi.model.OWLOntology;
import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * A Structural Ontological Model, that is an extended {@link OWLOntology OWL-API Ontology}, and
 * represents an <a href="http://www.w3.org/TR/owl2-syntax/#Ontologies">Ontology</a> in the OWL2 specification.
 * This interface provides a wide range of methods inherited from OWL-API
 * for working with structural (OWL Axioms and Annotations) representation of data
 * stored in the form of {@link org.apache.jena.graph.Graph RDF Graph}.
 * In addition to this range, there are also two new methods: {@link #asGraphModel()} and {@link #clearCache()}.
 * <p>
 * Created by szuev on 24.10.2016.
 */
public interface OntologyModel extends OWLOntology {

    /**
     * Returns the jena model shadow,
     * that is an interface to work with the {@link org.apache.jena.graph.Graph RDF Graph} directly.
     * The {@code OntGraphModel} is backed by the {@code OntologyModel},
     * so changes to the graph model are reflected in the structural model, and vice-versa.
     * <p>
     * Note: synchronisation is performed via different caches and graphs listeners
     * that are attached on the internal model level, not the base graph level.
     * Therefore changes in any other models with this same base graph
     * (for example, obtained using the expression {@code OntModelFactory.createModel(this.asGraphModel().getGraph()})
     * do not affect the axiomatic view of this model.
     * If such an uncommon situation arose and the data is shared between different external views of unknown nature,
     * then the method {@link #clearCache()} may help.
     *
     * @return {@link OntGraphModel Ontology RDF Graph Model}, not {@code null}
     * @see org.apache.jena.graph.Graph
     */
    OntGraphModel asGraphModel();

    /**
     * Clears the axioms and entities cache.
     * <p>
     * The cache lazily restores itself
     * when invoking almost any method described in the {@link OWLOntology} superinterface, e.g. {@link #axioms()}.
     * Clearing the cache is necessary to obtain a fixed list of axioms that uniquely corresponds to the RDF graph,
     * since OWL-API allows a wide ambiguity in the axioms definition.
     * In the structural (axiomatic) view there can be composite and bulky axioms specified,
     * which can be replaced by various other sets of axioms without loss any information.
     * This method brings the structural representation to the deterministic form,
     * that is strictly defined by the configuration and internal implementation.
     * For example, the ontology, initially containing only the one axiom
     * {@code SubClassOf(Annotation(<P> <I>) <A> <B>)},
     * after calling the {@code clearCache()} method will respond with a list that also includes each entity declaration:
     * {@code Declaration(AnnotationProperty(<P>))}, {@code Declaration(Class(<A>))} and {@code Declaration(Class(<B>))}.
     * In general, a complete list of axioms is configurable and depends on various settings,
     * for more details see {@link ru.avicomp.ontapi.config.OntLoaderConfiguration} and {@link ru.avicomp.ontapi.config.OntConfig}.
     */
    void clearCache();

    /**
     * Returns the manager, that is responsible for referencing between different ontologies.
     * Each ontology must have a link to the manager,
     * if the method returns {@code null}, this most likely means that the ontology is broken.
     *
     * @return {@link OntologyManager} the manager for this ontology
     */
    OntologyManager getOWLOntologyManager();

}
