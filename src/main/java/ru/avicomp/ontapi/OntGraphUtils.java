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

package ru.avicomp.ontapi;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import ru.avicomp.ontapi.jena.impl.OntIDImpl;
import ru.avicomp.ontapi.jena.model.OntID;
import ru.avicomp.ontapi.jena.utils.Graphs;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Helper to work with {@link Graph Apache Jena Graphs} in OWL-API terms.
 * <p>
 * Created by @szuev on 19.08.2017.
 *
 * @see Graphs
 * @since 1.0.1
 */
@SuppressWarnings("WeakerAccess")
public class OntGraphUtils {

    /**
     * Returns OWL Ontology ID from ontology-graph
     *
     * @param graph {@link Graph graph}
     * @return Optional around {@link OWLOntologyID OWL Ontology ID} or
     * {@code Optional.empty} for anonymous ontology graph or for graph without {@code owl:Ontology} section
     * @deprecated use {@link OntGraphUtils#getOntologyID(Graph)}, this will be removed
     */
    @Deprecated
    public static Optional<OWLOntologyID> ontologyID(@Nonnull Graph graph) {
        Graph base = Graphs.getBase(graph);
        return Graphs.ontologyNode(base)
                .map(n -> new OntIDImpl(n, new ModelCom(base)))
                .map(id -> {
                    Optional<IRI> iri = Optional.ofNullable(id.getURI()).map(IRI::create);
                    Optional<IRI> ver = Optional.ofNullable(id.getVersionIRI()).map(IRI::create);
                    return new OWLOntologyID(iri, ver);
                }).filter(id -> !id.isAnonymous());
    }

    /**
     * Gets an OWL Ontology ID parsed from the given graph.
     * Treats graphs without {@code owl:Ontology} section inside as anonymous.
     *
     * @param graph {@link Graph}
     * @return {@link OWLOntologyID}, not null
     * @throws OntApiException in case it is an anonymous graph but with version iri
     */
    public static OWLOntologyID getOntologyID(@Nonnull Graph graph) throws OntApiException {
        Graph base = Graphs.getBase(graph);
        Optional<Node> node = Graphs.ontologyNode(base);
        if (!node.isPresent()) { // treat graphs without owl:Ontology as anonymous
            return new OWLOntologyID();
        }
        OntID id = new OntIDImpl(node.get(), new ModelCom(base));
        Optional<IRI> iri = Optional.ofNullable(id.getURI()).map(IRI::create);
        Optional<IRI> ver = Optional.ofNullable(id.getVersionIRI()).map(IRI::create);
        if (!iri.isPresent() && ver.isPresent())
            throw new OntApiException("Anonymous graph with version iri");
        return new OWLOntologyID(iri, ver);
    }

    /**
     * Builds map form the ontology graph.
     * If the specified graph is not composite then only one key in the map is expected.
     * The specified graph should consist of named graphs, only the root is allowed to be anonymous.
     * Also the graph-tree should not contain different children but with the same name (owl:Ontology uri).
     *
     * @param graph {@link Graph graph}
     * @return Map with {@link OWLOntologyID OWL Ontology ID} as a key and {@link Graph graph} as a value
     * @throws OntApiException the input graph has restrictions, see above.
     */
    public static Map<OWLOntologyID, Graph> toGraphMap(@Nonnull Graph graph) throws OntApiException {
        Graph base = Graphs.getBase(graph);
        return Graphs.flat(graph)
                .collect(Collectors.toMap(
                        g -> {
                            OWLOntologyID id = getOntologyID(g);
                            if (id.isAnonymous() && base != g) {
                                throw new OntApiException("Anonymous sub graph found: " + id +
                                        ". Only top-level graph is allowed to be anonymous");
                            }
                            return id;
                        }
                        , Function.identity()
                        , (a, b) -> {
                            if (a.isIsomorphicWith(b)) {
                                return a;
                            }
                            throw new OntApiException("Duplicate sub graphs " + Graphs.getName(a));
                        }
                        , LinkedHashMap::new));
    }

}
