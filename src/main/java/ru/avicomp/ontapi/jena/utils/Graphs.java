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

package ru.avicomp.ontapi.jena.utils;

import org.apache.jena.graph.*;
import org.apache.jena.graph.compose.Dyadic;
import org.apache.jena.graph.compose.Polyadic;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.graph.GraphWrapper;
import org.apache.jena.sparql.util.graph.GraphUtils;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import ru.avicomp.ontapi.jena.RWLockedGraph;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper to work with {@link Graph Jena Graph} (generally with our {@link UnionGraph}) and with its related objects:
 * {@link Triple} and {@link Node}.
 * <p>
 * Created by szuev on 06.02.2017.
 *
 * @see GraphUtil
 * @see GraphUtils
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class Graphs {

    /**
     * Lists all top-level sub-graphs attached to the given composite graph-container,
     * which is allowed to be either {@link UnionGraph} or {@link Polyadic} or {@link Dyadic}.
     * If the graph is not of the list above, an empty stream is expected.
     * The base graph is not included in the resulting stream.
     * In case of {@link Dyadic}, the left graph is considered as base.
     *
     * @param graph {@link Graph}
     * @return Stream of {@link Graph}s
     * @see #getBase(Graph)
     * @see UnionGraph
     * @see Polyadic
     * @see Dyadic
     */
    public static Stream<Graph> subGraphs(Graph graph) {
        if (graph instanceof UnionGraph) {
            return ((UnionGraph) graph).getUnderlying().graphs();
        }
        if (graph instanceof Polyadic) {
            return ((Polyadic) graph).getSubGraphs().stream();
        }
        if (graph instanceof Dyadic) {
            return Stream.of((Graph) ((Dyadic) graph).getR());
        }
        return Stream.empty();
    }

    /**
     * Extracts the base (primary) primitive graph from a composite or wrapper graph if it is possible
     * otherwise returns the same graph untouched.
     * Note: this is a recursive method.
     *
     * @param graph {@link Graph}
     * @return {@link Graph}
     * @see #subGraphs(Graph)
     * @see GraphWrapper
     * @see RWLockedGraph
     * @see UnionGraph
     * @see Polyadic
     * @see Dyadic
     */
    public static Graph getBase(Graph graph) {
        if (graph instanceof GraphMem) {
            return graph;
        }
        if (graph instanceof GraphWrapper) {
            return getBase(((GraphWrapper) graph).get());
        }
        if (graph instanceof RWLockedGraph) {
            return getBase(((RWLockedGraph) graph).get());
        }
        if (graph instanceof UnionGraph) {
            return getBase(((UnionGraph) graph).getBaseGraph());
        }
        if (graph instanceof Polyadic) {
            return getBase(((Polyadic) graph).getBaseGraph());
        }
        if (graph instanceof Dyadic) {
            return getBase((Graph) ((Dyadic) graph).getL());
        }
        return graph;
    }

    /**
     * Lists all graphs from the composite or wrapper graph
     * including the base as flat stream of non-composite (primitive) graphs.
     * Note: this is a recursive method.
     *
     * @param graph {@link Graph}
     * @return Stream of {@link Graph}
     */
    public static Stream<Graph> flat(Graph graph) {
        return graph == null ? Stream.empty() :
                Stream.concat(Stream.of(getBase(graph)), subGraphs(graph).flatMap(Graphs::flat));
    }

    /**
     * Answers {@code true} if the two input graphs are based on the same primitive graph.
     *
     * @param left  {@link Graph}
     * @param right {@link Graph}
     * @return boolean
     */
    public static boolean isSameBase(Graph left, Graph right) {
        return Objects.equals(getBase(left), getBase(right));
    }

    /**
     * Converts the given graph to the hierarchical {@link UnionGraph Union Graph}
     * in accordance with their {@code owl:imports} declarations.
     * Irrelevant graphs are skipped from consideration.
     * If the input graph is already {@link UnionGraph} it will be returned unchanged.
     * The method can be used, for example, to get an ONT graph from the {@link org.apache.jena.ontology.OntModel}.
     * Note: it is a recursive method.
     *
     * @param g {@link Graph}
     * @return {@link UnionGraph}
     * @throws StackOverflowError in case there is a loop in imports
     * @since 1.0.1
     */
    public static UnionGraph toUnion(Graph g) {
        if (g instanceof UnionGraph) return (UnionGraph) g;
        if (g instanceof GraphMem) return new UnionGraph(g);
        return toUnion(getBase(g), flat(g).collect(Collectors.toSet()));
    }

    /**
     * Builds an union-graph using specified components.
     * Note: this is a recursive method.
     *
     * @param graph     {@link Graph} the base graph (root)
     * @param dependent collection of dependent {@link Graph graph}x to search in
     * @return {@link UnionGraph}
     * @since 1.0.1
     */
    public static UnionGraph toUnion(Graph graph, Collection<Graph> dependent) {
        Graph base = getBase(graph);
        Set<String> imports = getImports(base);
        UnionGraph res = new UnionGraph(base);
        dependent.stream()
                .filter(x -> !isSameBase(base, x))
                .filter(g -> imports.contains(getURI(g)))
                .forEach(g -> res.addGraph(toUnion(g, dependent)));
        return res;
    }

    /**
     * Gets Ontology URI from the base graph or {@code null}
     * (if there is no {@code owl:Ontology} or it is anonymous ontology).
     *
     * @param graph {@link Graph}
     * @return String uri or {@code null}
     * @see #getImports(Graph)
     */
    public static String getURI(Graph graph) {
        return ontologyNode(getBase(graph)).filter(Node::isURI).map(Node::getURI).orElse(null);
    }

    /**
     * Gets the "name" of the base graph: uri, blank-node-id as string or dummy string if there is no ontology at all.
     * The version IRI info is also included if it is present in the graph for the found ontology node.
     *
     * @param graph {@link Graph}
     * @return String
     * @see #getURI(Graph)
     */
    public static String getName(Graph graph) {
        Optional<Node> res = ontologyNode(getBase(graph));
        if (!res.isPresent()) return "NullOntology";
        List<String> versions = graph.find(res.get(), OWL.versionIRI.asNode(), Node.ANY)
                .mapWith(Triple::getObject).mapWith(Node::toString).toList();
        if (versions.isEmpty()) {
            return String.format("<%s>", res.get().toString());
        }
        return String.format("<%s%s>", res.get().toString(), versions.toString());
    }

    /**
     * Finds and returns the primary node within the given graph,
     * which is the subject in the {@code _:x rdf:type owl:Ontology} statement.
     * If there are both uri and blank ontological nodes together in the graph then it prefers uri.
     * Of several ontological nodes the same kind, it chooses the most bulky.
     * Note: it works with any graph, not necessarily with the base;
     * for a valid composite ontology graph a lot of ontological nodes are expected.
     *
     * @param g {@link Graph}
     * @return {@link Optional} around the {@link Node} which could be uri or blank.
     */
    public static Optional<Node> ontologyNode(Graph g) {
        List<Node> res = g.find(Node.ANY, RDF.Nodes.type, OWL.Ontology.asNode())
                .mapWith(t -> {
                    Node n = t.getSubject();
                    return n.isURI() || n.isBlank() ? n : null;
                }).filterDrop(Objects::isNull).toList();
        if (res.isEmpty()) return Optional.empty();
        res.sort(rootNodeComparator(g));
        return Optional.of(res.get(0));
    }

    /**
     * Returns comparator for root nodes.
     * Tricky logic:
     * first compares roots as standalone nodes and the any uri-node is considered less then any blank-node,
     * then compares roots as part of the graph using the rule 'the fewer children -&gt; the greater weight'.
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
     * Returns all uri-objects from the {@code _:x owl:imports _:uri} statements.
     * In case of composite graph imports are listed transitively.
     *
     * @param graph {@link Graph}
     * @return unordered Set of uris from whole graph (it may be composite).
     * @see #getURI(Graph)
     */
    public static Set<String> getImports(Graph graph) {
        return graph.find(Node.ANY, OWL.imports.asNode(), Node.ANY).mapWith(t -> {
            Node n = t.getObject();
            return n.isURI() ? n.getURI() : null;
        }).filterDrop(Objects::isNull).toSet();
    }

    /**
     * Prints a graph hierarchy tree.
     * For a valid ontology it should match an imports ({@code owl:imports}) tree also.
     * For debugging.
     * <p>
     * An examples of possible output:
     * <pre> {@code
     * <http://imports.test.Main.ttl>
     *      <http://imports.test.C.ttl>
     *          <http://imports.test.A.ttl>
     *          <http://imports.test.B.ttl>
     *      <http://imports.test.D.ttl>
     * }, {@code
     * <http://imports.test.D.ttl>
     *      <http://imports.test.C.ttl>
     *          <http://imports.test.A.ttl>
     *          <http://imports.test.B.ttl>
     *              <http://imports.test.Main.ttl>
     * } </pre>
     *
     * @param graph {@link Graph}
     * @return hierarchy tree as String
     */
    public static String importsTreeAsString(Graph graph) {
        return makeImportsTree(graph, "\t", "\t", new HashSet<>()).toString();
    }

    private static StringBuilder makeImportsTree(Graph graph, String indent, String step, Set<Graph> seen) {
        StringBuilder res = new StringBuilder();
        Graph base = getBase(graph);
        try {
            String name = getName(base);
            if (seen.contains(base)) {
                return res.append("Recursion: ").append(name);
            }
            seen.add(base);
            res.append(name).append("\n");
            subGraphs(graph)
                    .sorted(Comparator.comparingLong(o -> subGraphs(o).count()))
                    .forEach(sub -> res.append(indent).append(makeImportsTree(sub, indent + step, step, seen)));
            return res;
        } finally {
            seen.remove(base);
        }
    }

    /**
     * Returns a Graph as Turtle String.
     * For debugging.
     *
     * @param g {@link Graph}
     * @return String
     */
    public static String toTurtleString(Graph g) {
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, g, Lang.TURTLE);
        return sw.toString();
    }

    /**
     * Collects a prefixes library from the collection of the graphs.
     *
     * @param graphs {@link Iterable} a collection of graphs
     * @return unmodifiable {@link PrefixMapping prefix mapping}
     */
    public static PrefixMapping collectPrefixes(Iterable<Graph> graphs) {
        PrefixMapping res = PrefixMapping.Factory.create();
        graphs.forEach(g -> res.setNsPrefixes(g.getPrefixMapping()));
        return res.lock();
    }

    /**
     * Makes a concurrent version of the given Graph by wrapping it as {@link RWLockedGraph}.
     * If the input is an UnionGraph, only the base (primary) graph will contain the specified R/W lock.
     * The result graph has the same structure as specified.
     *
     * @param graph {@link Graph}, not null
     * @param lock  {@link ReadWriteLock}, not null
     * @return {@link Graph} with {@link ReadWriteLock}
     */
    public static Graph asConcurrent(Graph graph, ReadWriteLock lock) {
        if (graph instanceof RWLockedGraph) {
            return asConcurrent(((RWLockedGraph) graph).get(), lock);
        }
        if (!(graph instanceof UnionGraph)) {
            return new RWLockedGraph(graph, lock);
        }
        UnionGraph u = (UnionGraph) graph;
        Graph base = asConcurrent(u.getBaseGraph(), lock);
        UnionGraph res = new UnionGraph(base, u.getEventManager());
        u.getUnderlying().graphs()
                .map(Graphs::asNonConcurrent)
                .forEach(res::addGraph);
        return res;
    }

    /**
     * Removes concurrency from the given graph.
     * This operation is opposite to the {@link #asConcurrent(Graph, ReadWriteLock)} method:
     * if the input is an UnionGraph it makes an UnionGraph with the same structure as specified but without R/W lock.
     *
     * @param graph {@link Graph}
     * @return {@link Graph}
     */
    public static Graph asNonConcurrent(Graph graph) {
        if (graph instanceof RWLockedGraph) {
            return ((RWLockedGraph) graph).get();
        }
        if (!(graph instanceof UnionGraph)) {
            return graph;
        }
        UnionGraph u = (UnionGraph) graph;
        Graph base = asNonConcurrent(u.getBaseGraph());
        UnionGraph res = new UnionGraph(base, u.getEventManager());
        u.getUnderlying().graphs()
                .map(Graphs::asNonConcurrent)
                .forEach(res::addGraph);
        return res;
    }

    /**
     * Makes a fresh node instance according to the given iri.
     *
     * @param iri String, an IRI to create URI-Node or {@code null} to create Blank-Node
     * @return {@link Node}, not null
     */
    public static Node createNode(String iri) {
        return iri == null ? NodeFactory.createBlankNode() : NodeFactory.createURI(iri);
    }

    /**
     * Lists all unique subject nodes in the given graph.
     * Warning: the result is stored in-memory!
     *
     * @param g {@link Graph}
     * @return an {@link ExtendedIterator Extended Iterator} (distinct) of all subjects in the graph
     */
    public static ExtendedIterator<Node> subjects(Graph g) {
        return GraphUtil.listSubjects(g, Node.ANY, Node.ANY);
    }

    /**
     * Lists all unique nodes in the given graph, which are used as subject or object.
     * Warning: the result is stored in-memory!
     *
     * @param g {@link Graph}, not null
     * @return an {@link ExtendedIterator Extended Iterator} (distinct) of all subjects or objects in the graph
     */
    public static ExtendedIterator<Node> subjectsAndObjects(Graph g) {
        return WrappedIterator.create(GraphUtils.allNodes(g));
    }

    /**
     * Lists all unique nodes in the given graph.
     * Warning: the result is stored in-memory!
     *
     * @param g {@link Graph}, not null
     * @return an {@link ExtendedIterator Extended Iterator} (distinct) of all nodes in the graph
     */
    public static ExtendedIterator<Node> all(Graph g) {
        Set<Node> res = Iter.flatMap(g.find(Triple.ANY), t -> Iter.of(t.getSubject(), t.getPredicate(), t.getObject())).toSet();
        return WrappedIterator.create(res.iterator());
    }
}
