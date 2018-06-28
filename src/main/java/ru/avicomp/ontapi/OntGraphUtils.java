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

import org.apache.jena.graph.*;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import ru.avicomp.ontapi.jena.impl.OntIDImpl;
import ru.avicomp.ontapi.jena.model.OntID;
import ru.avicomp.ontapi.jena.utils.Graphs;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
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

    /**
     * Converts a {@link Triple Jena Triple} to {@link RDFTriple OWL-API RDFTriple}.
     *
     * @param triple not null
     * @return RDFTriple
     */
    public static RDFTriple triple(Triple triple) {
        RDFResource subject;
        if (triple.getSubject().isURI()) {
            subject = uri(triple.getSubject());
        } else {
            subject = blank(triple.getSubject());
        }
        RDFResourceIRI predicate = uri(triple.getPredicate());
        RDFNode object;
        if (triple.getObject().isURI()) {
            object = uri(triple.getObject());
        } else if (triple.getObject().isLiteral()) {
            object = literal(triple.getObject());
        } else {
            object = blank(triple.getObject());
        }
        return new RDFTriple(subject, predicate, object);
    }

    /**
     * Converts a {@link Node Jena Node} to {@link RDFResourceBlankNode OWL-API node object}, which pretends to be a blank node.
     *
     * @param node not null, must be {@link Node_Blank}
     * @return {@link RDFResourceBlankNode} with all flags set to {@code false}
     * @throws IllegalArgumentException in case the specified node is not blank
     */
    public static RDFResourceBlankNode blank(Node node) throws IllegalArgumentException {
        if (!Objects.requireNonNull(node, "Null node").isBlank())
            throw new IllegalArgumentException("Not a blank node: " + node);
        return new RDFResourceBlankNode(IRI.create(node.getBlankNodeId().getLabelString()), false, false, false);
    }

    /**
     * Converts a {@link Node Jena Node} to {@link RDFResourceIRI OWL-API IRI RDF-Node}.
     *
     * @param node not null, must be {@link Node_URI}
     * @return {@link RDFResourceIRI}
     * @throws IllegalArgumentException in case the specified node is not blank
     */
    public static RDFResourceIRI uri(Node node) throws IllegalArgumentException {
        if (!Objects.requireNonNull(node, "Null node").isURI())
            throw new IllegalArgumentException("Not an uri node: " + node);
        return new RDFResourceIRI(IRI.create(node.getURI()));
    }

    /**
     * Converts a {@link Node Jena Node} to {@link RDFLiteral OWL-API Literal RDF-Node}.
     *
     * @param node not null, must be {@link Node_Literal}
     * @return {@link RDFResourceIRI}
     * @throws IllegalArgumentException in case the specified node is not literal
     */
    public static RDFLiteral literal(Node node) throws IllegalArgumentException {
        if (!Objects.requireNonNull(node, "Null node").isLiteral())
            throw new IllegalArgumentException("Not a literal node: " + node);
        return new RDFLiteral(node.getLiteralLexicalForm(), node.getLiteralLanguage(), IRI.create(node.getLiteralDatatypeURI()));
    }
}
