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

package ru.avicomp.ontapi.jena.utils;

import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.compose.Dyadic;
import org.apache.jena.graph.compose.Polyadic;
import org.apache.jena.riot.RDFDataMgr;

import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * Helper to work with jena {@link Graph} (generally with our {@link UnionGraph})
 * <p>
 * Created by szuev on 06.02.2017.
 */
@SuppressWarnings("WeakerAccess")
public class Graphs {

    /**
     * Returns stream of sub-graphs or empty stream if the specified one is not composite.
     *
     * @param graph {@link Graph}
     * @return Stream of {@link Graph}s.
     */
    public static Stream<Graph> subGraphs(Graph graph) {
        return graph instanceof UnionGraph ? ((UnionGraph) graph).getUnderlying().graphs() :
                graph instanceof Polyadic ? ((Polyadic) graph).getSubGraphs().stream() :
                        graph instanceof Dyadic ? Stream.of((Graph) ((Dyadic) graph).getR()) : Stream.empty();
    }

    /**
     * Gets base (primary) graph from the specified graph if it is composite, otherwise returns the same one.
     *
     * @param graph {@link Graph}
     * @return {@link Graph}
     */
    public static Graph getBase(Graph graph) {
        return graph instanceof UnionGraph ? ((UnionGraph) graph).getBaseGraph() :
                graph instanceof Polyadic ? ((Polyadic) graph).getBaseGraph() :
                        graph instanceof Dyadic ? (Graph) ((Dyadic) graph).getL() : graph;
    }

    /**
     * Returns all graphs from composite graph including the base as flat stream of non-composite graphs.
     * Note: this is a recursive method.
     *
     * @param graph {@link Graph}
     * @return Stream of {@link Graph}
     */
    public static Stream<Graph> flat(Graph graph) {
        return Stream.concat(Stream.of(getBase(graph)), subGraphs(graph).map(Graphs::flat).flatMap(Function.identity()));
    }

    /**
     * Gets Ontology URI from the base graph or null (if no owl:Ontology or it is anonymous ontology).
     *
     * @param graph {@link Graph}
     * @return String uri or null.
     */
    public static String getURI(Graph graph) {
        return getOntology(getBase(graph)).filter(Node::isURI).map(Node::getURI).orElse(null);
    }

    /**
     * Gets "name" of the base graph: uri, blank-node-id as string or dummy string if there is no ontology at all.
     *
     * @param graph {@link Graph}
     * @return String
     */
    public static String getName(Graph graph) {
        return getOntology(getBase(graph)).map(n -> String.format("<%s>", n.toString())).orElse("NullOntology");
    }

    /**
     * Gets the ontology root node (subject in "_:x rdf:type owl:Ontology") from graph or null if there are no ontology sections.
     * If there are uri and blank node it prefers uri.
     * If there are several other ontological nodes it chooses the most bulky.
     * Note: works with any graph, not only the base.
     * If it is composite then a lot of ontology nodes expected, otherwise only single one.
     *
     * @param g {@link Graph}
     * @return {@link Optional} around the {@link Node} which could be uri or blank.
     */
    public static Optional<Node> getOntology(Graph g) {
        try (Stream<Node> nodes = Iter.asStream(g.find(Node.ANY, RDF.type.asNode(), OWL.Ontology.asNode()))
                .map(Triple::getSubject)
                .filter(node -> node.isBlank() || node.isURI())
                .sorted(rootNodeComparator(g))) {
            return nodes.findFirst();
        }
    }

    /**
     * Returns comparator for root nodes.
     * Tricky logic:
     * first compares roots as standalone nodes and the any uri-node is considered less then any blank-node,
     * then compares roots as part of the graph using the rule 'the fewer children -> the greater weight'.
     *
     * @param graph {@link Graph}
     * @return {@link Comparator}
     */
    public static Comparator<Node> rootNodeComparator(Graph graph) {
        return ((Comparator<Node>) (a, b) -> Boolean.compare(b.isURI(), a.isURI()))
                .thenComparing(Comparator.comparingInt((ToIntFunction<Node>) subj ->
                        graph.find(subj, Node.ANY, Node.ANY).toList().size()).reversed());
    }

    /**
     * Returns uri-subject from owl:imports statements
     *
     * @param graph {@link Graph}
     * @return unordered Set of uris from whole graph (it may be composite).
     */
    public static Set<String> getImports(Graph graph) {
        return Iter.asStream(graph.find(Node.ANY, OWL.imports.asNode(), Node.ANY))
                .map(Triple::getObject)
                .filter(Node::isURI)
                .map(Node::getURI).collect(Collectors.toSet());
    }

    /**
     * just for debugging.
     * Examples of output:
     * <pre> {@code
     * <http://imports.test.Main.ttl>
     *  <http://imports.test.C.ttl>
     *      <http://imports.test.A.ttl>
     *      <http://imports.test.B.ttl>
     *  <http://imports.test.D.ttl>
     * }, {@code
     * <http://imports.test.D.ttl>
     *  <http://imports.test.C.ttl>
     *      <http://imports.test.A.ttl>
     *      <http://imports.test.B.ttl>
     *              <http://imports.test.Main.ttl>
     * } </pre>
     *
     * @param graph {@link Graph}
     * @return String
     */
    public static String importsTreeAsString(Graph graph) {
        return makeImportsTree(graph, "\t", new HashSet<>()).toString();
    }

    private static StringBuilder makeImportsTree(Graph graph, String sep, Set<Graph> seen) {
        StringBuilder sb = new StringBuilder();
        Graph base = getBase(graph);
        if (seen.contains(base)) {
            throw new OntJenaException("Unexpected recursion cycle for graph " + graph);
        }
        seen.add(base);
        sb.append("<").append(getURI(graph)).append(">");
        sb.append("\n");
        subGraphs(graph).forEach(sub -> sb.append(sep).append(makeImportsTree(sub, sep + sep, seen)));
        return sb;
    }

    /**
     * Returns Graph as Turtle String.
     * For debugging.
     *
     * @param g {@link Graph}
     * @return String
     */
    public static String toTurtleString(Graph g) {
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, g, OntFormat.TURTLE.getLang());
        return sw.toString();
    }
}
