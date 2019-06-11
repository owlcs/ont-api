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
import ru.avicomp.ontapi.jena.RWLockedGraph;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Function;
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

    public static final String NULL_ONTOLOGY_IDENTIFIER = "NullOntology";
    public static final String RECURSIVE_GRAPH_IDENTIFIER = "Recursion";

    /**
     * Extracts and lists all top-level sub-graphs from the given composite graph-container,
     * that is allowed to be either {@link UnionGraph} or {@link Polyadic} or {@link Dyadic}.
     * If the graph is not of the list above, an empty stream is expected.
     * The base graph is not included in the resulting stream.
     * In case of {@link Dyadic}, the left graph is considered as base.
     *
     * @param graph {@link Graph}
     * @return {@code Stream} of {@link Graph}s
     * @see Graphs#getBase(Graph)
     * @see UnionGraph
     * @see Polyadic
     * @see Dyadic
     */
    public static Stream<Graph> subGraphs(Graph graph) {
        if (graph instanceof UnionGraph) {
            return Iter.asStream(((UnionGraph) graph).getUnderlying().listGraphs());
        }
        if (graph instanceof Polyadic) {
            return ((Polyadic) graph).getSubGraphs().stream();
        }
        if (graph instanceof Dyadic) {
            return Stream.of(((Dyadic) graph).getR());
        }
        return Stream.empty();
    }

    /**
     * Extracts the base (primary) primitive graph from a composite or wrapper graph if it is possible
     * otherwise returns the same graph untouched.
     * Warning: this is a recursive method.
     * The {@link org.apache.jena.graph.impl.WrappedGraph} is intentionally not included into the consideration:
     * any sub-instances of that class are considered as indivisible.
     *
     * @param graph {@link Graph}
     * @return {@link Graph}
     * @see Graphs#subGraphs(Graph)
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
            return getBase(((Dyadic) graph).getL());
        }
        return graph;
    }

    /**
     * Lists all indivisible graphs extracted from the composite or wrapper graph
     * including the base as flat stream of non-composite (primitive) graphs.
     * Note: this method is safe for a common {@link UnionGraph}, that produced by the system,
     * but for any other composite graph there is a dangerous of {@code StackOverflowError} method
     * in case a considered graph has a recursion somewhere in its hierarchy.
     *
     * @param graph {@link Graph}
     * @return {@code Stream} of {@link Graph}
     * @throws StackOverflowError in case the given graph is not {@link UnionGraph}
     *                            and has a recursion in the hierarchy
     * @see UnionGraph#listBaseGraphs()
     * @since 1.4.2
     */
    public static Stream<Graph> baseGraphs(Graph graph) {
        if (graph == null) return Stream.empty();
        if (graph instanceof UnionGraph) {
            return Iter.asStream(((UnionGraph) graph).listBaseGraphs().mapWith(Graphs::getBase));
        }
        return Stream.concat(Stream.of(getBase(graph)), subGraphs(graph).flatMap(Graphs::baseGraphs));
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
     * @throws StackOverflowError in case there is a loop in imports (i.e. a recursion in the hierarchy)
     * @since 1.0.1
     */
    public static UnionGraph toUnion(Graph g) {
        if (g instanceof UnionGraph) return (UnionGraph) g;
        if (g instanceof GraphMem) return new UnionGraph(g);
        return toUnion(getBase(g), baseGraphs(g).collect(Collectors.toSet()));
    }

    /**
     * Builds an union-graph using the specified components.
     * Note: this is a recursive method.
     *
     * @param graph     {@link Graph} the base graph (root)
     * @param dependent a {@code Collection} of dependent {@link Graph graph}x to search in
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
     * Creates a new {@code UnionGraph} with the given base {@code Graph}
     * and the same structure and settings as in the specified {@code UnionGraph}.
     *
     * @param base  {@link Graph} new base, not {@code null}
     * @param union {@link UnionGraph} to inherit settings and hierarchy, not {@code null}
     * @return {@link UnionGraph}
     * @since 1.4.0
     */
    public static UnionGraph withBase(Graph base, UnionGraph union) {
        return new UnionGraph(base, union.getUnderlying(), union.getEventManager(), union.isDistinct());
    }

    /**
     * Gets Ontology URI from the base graph or returns {@code null}
     * if there is no {@code owl:Ontology} or it is anonymous ontology.
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
        if (!res.isPresent()) return NULL_ONTOLOGY_IDENTIFIER;
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
     * Returns a comparator for root nodes.
     * Tricky logic:
     * first compares roots as standalone nodes and the any uri-node is considered less then any blank-node,
     * then compares roots as part of the graph using the rule 'the fewer children -&gt; the greater weight'.
     *
     * @param graph {@link Graph}
     * @return {@link Comparator}
     */
    public static Comparator<Node> rootNodeComparator(Graph graph) {
        return ((Comparator<Node>) (a, b) -> Boolean.compare(b.isURI(), a.isURI()))
                .thenComparing(Comparator.comparingInt((Node subj) ->
                        graph.find(subj, Node.ANY, Node.ANY).toList().size()).reversed());
    }

    /**
     * Returns all uri-objects from the {@code _:x owl:imports _:uri} statements.
     * In case of composite graph imports are listed transitively.
     *
     * @param graph {@link Graph}, not {@code null}
     * @return unordered Set of uris from the whole graph (it may be composite)
     * @see #getURI(Graph)
     */
    public static Set<String> getImports(Graph graph) {
        return listImports(graph).toSet();
    }

    /**
     * Returns an {@code ExtendedIterator} over all URIs from the {@code _:x owl:imports _:uri} statements.
     * In case of composite graph imports are listed transitively.
     *
     * @param graph {@link Graph}, not {@code null}
     * @return {@link ExtendedIterator} of {@code String}-URIs
     * @see #getURI(Graph)
     * @since 1.4.2
     */
    public static ExtendedIterator<String> listImports(Graph graph) {
        return graph.find(Node.ANY, OWL.imports.asNode(), Node.ANY).mapWith(t -> {
            Node n = t.getObject();
            return n.isURI() ? n.getURI() : null;
        }).filterDrop(Objects::isNull);
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
        Function<Graph, String> printDefaultGraphName = g -> g.getClass().getSimpleName() + "@" + Integer.toHexString(g.hashCode());
        return makeImportsTree(graph, g -> {
            if (g.isClosed()) return "Closed(" + printDefaultGraphName.apply(g) + ")";
            String res = getName(g);
            if (NULL_ONTOLOGY_IDENTIFIER.equals(res)) {
                res += "(" + printDefaultGraphName.apply(g) + ")";
            }
            return res;
        }, "\t", "\t", new HashSet<>()).toString();
    }

    private static StringBuilder makeImportsTree(Graph graph,
                                                 Function<Graph, String> getName,
                                                 String indent,
                                                 String step,
                                                 Set<Graph> seen) {
        StringBuilder res = new StringBuilder();
        Graph base = getBase(graph);
        String name = getName.apply(base);
        try {
            if (!seen.add(graph)) {
                return res.append(RECURSIVE_GRAPH_IDENTIFIER).append(": ").append(name);
            }
            res.append(name).append("\n");
            subGraphs(graph)
                    .sorted(Comparator.comparingLong(o -> subGraphs(o).count()))
                    .forEach(sub -> res.append(indent)
                            .append(makeImportsTree(sub, getName, indent + step, step, seen)));
            return res;
        } finally {
            seen.remove(graph);
        }
    }

    /**
     * Returns a {@code Graph} as a Turtle String.
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
     * @return unmodifiable (locked) {@link PrefixMapping prefix mapping}
     */
    public static PrefixMapping collectPrefixes(Iterable<Graph> graphs) {
        PrefixMapping res = PrefixMapping.Factory.create();
        graphs.forEach(g -> res.setNsPrefixes(g.getPrefixMapping()));
        return res.lock();
    }

    /**
     * Makes a concurrent version of the given {@code Graph} by wrapping it as {@link RWLockedGraph}.
     * If the input is an {@code UnionGraph},
     * it makes an {@code UnionGraph} where only the base (primary) contains the specified R/W lock.
     * The result graph has the same structure as specified.
     *
     * @param graph {@link Graph}, not {@code null}
     * @param lock  {@link ReadWriteLock}, not {@code null}
     * @return {@link Graph} with {@link ReadWriteLock}
     * @throws StackOverflowError in case the given graph has a recursion in its hierarchy
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
        UnionGraph res = new UnionGraph(base);
        u.getUnderlying().listGraphs()
                .mapWith(Graphs::asNonConcurrent)
                .forEachRemaining(res::addGraph);
        return res;
    }

    /**
     * Removes concurrency from the given graph.
     * This operation is opposite to the {@link #asConcurrent(Graph, ReadWriteLock)} method:
     * if the input is an {@code UnionGraph}
     * it makes an {@code UnionGraph} with the same structure as specified but without R/W lock.
     *
     * @param graph {@link Graph}
     * @return {@link Graph}
     * @throws StackOverflowError in case the given graph has a recursion in its hierarchy
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
        UnionGraph res = new UnionGraph(base);
        u.getUnderlying().listGraphs()
                .mapWith(Graphs::asNonConcurrent)
                .forEachRemaining(res::addGraph);
        return res;
    }

    /**
     * Answers {@code true} if the left graph depends on the right one.
     *
     * @param left  {@link Graph}
     * @param right {@link Graph}
     * @return {@code true} if the left argument graph is dependent on the right
     * @since 1.4.0
     */
    public static boolean dependsOn(Graph left, Graph right) {
        return left == right || (left != null && left.dependsOn(right));
    }

    /**
     * Makes a fresh node instance according to the given iri.
     *
     * @param iri String, an IRI to create URI-Node or {@code null} to create Blank-Node
     * @return {@link Node}, not {@code null}
     */
    public static Node createNode(String iri) {
        return iri == null ? NodeFactory.createBlankNode() : NodeFactory.createURI(iri);
    }

    /**
     * Lists all unique subject nodes in the given graph.
     * Warning: the result is temporary stored in-memory!
     *
     * @param g {@link Graph}, not {@code null}
     * @return an {@link ExtendedIterator ExtendedIterator} (<b>distinct</b>) of all subjects in the graph
     * @throws OutOfMemoryError while iterating in case the graph is too large
     *                          so that all its subjects can be placed in memory as a {@code Set}
     * @see GraphUtil#listSubjects(Graph, Node, Node)
     * @since 1.4.2
     */
    public static ExtendedIterator<Node> listSubjects(Graph g) {
        return Iter.create(() -> Collections.unmodifiableSet(g.find().mapWith(Triple::getSubject).toSet()).iterator());
    }

    /**
     * Lists all unique nodes in the given graph, which are used in a subject or an object positions.
     * Warning: the result is temporary stored in-memory!
     *
     * @param g {@link Graph}, not {@code null}
     * @return an {@link ExtendedIterator ExtendedIterator} (<b>distinct</b>) of all subjects or objects in the graph
     * @throws OutOfMemoryError while iterating in case the graph is too large
     *                          so that all its subjects and objects can be placed in memory as a {@code Set}
     * @see GraphUtils#allNodes(Graph)
     * @since 1.4.2
     */
    public static ExtendedIterator<Node> listSubjectsAndObjects(Graph g) {
        return Iter.create(() -> Collections.unmodifiableSet(Iter.flatMap(g.find(),
                t -> Iter.of(t.getSubject(), t.getObject())).toSet()).iterator());
    }

    /**
     * Lists all unique nodes in the given graph.
     * Warning: the result is temporary stored in-memory!
     *
     * @param g {@link Graph}, not {@code null}
     * @return an {@link ExtendedIterator ExtendedIterator} (<b>distinct</b>) of all nodes in the graph
     * @throws OutOfMemoryError while iterating in case the graph is too large to be placed in memory as a {@code Set}
     * @since 1.4.2
     */
    public static ExtendedIterator<Node> listAllNodes(Graph g) {
        return Iter.create(() -> Collections.unmodifiableSet(Iter.flatMap(g.find(),
                t -> Iter.of(t.getSubject(), t.getPredicate(), t.getObject())).toSet()).iterator());
    }

    /**
     * Lists all graphs from the composite or wrapper graph
     * including the base as flat stream of non-composite (primitive) graphs.
     *
     * @param graph {@link Graph}
     * @return {@code Stream} of {@link Graph}
     * @deprecated since 1.4.2: use the method {@link #baseGraphs(Graph)} instead
     */
    @Deprecated
    public static Stream<Graph> flat(Graph graph) {
        return baseGraphs(graph);
    }

    /**
     * Lists all unique nodes in the given graph.
     * Warning: the result is stored in-memory!
     *
     * @param g {@link Graph}, not {@code null}
     * @return an {@link ExtendedIterator Extended Iterator} (<b>distinct</b>) of all nodes in the graph
     * @deprecated since 1.4.2: use the method {@link #listAllNodes(Graph)} instead
     */
    @Deprecated
    public static ExtendedIterator<Node> all(Graph g) {
        return listAllNodes(g);
    }

    /**
     * Lists all unique subject nodes in the given graph.
     * Warning: the result is stored in-memory!
     *
     * @param g {@link Graph}
     * @return an {@link ExtendedIterator Extended Iterator} (distinct) of all subjects in the graph
     * @deprecated since 1.4.2: use the method {@link #listSubjects(Graph)} instead
     */
    @Deprecated
    public static ExtendedIterator<Node> subjects(Graph g) {
        return listSubjects(g);
    }

    /**
     * Lists all unique nodes in the given graph, which are used as subject or object.
     * Warning: the result is temporary stored in-memory!
     *
     * @param g {@link Graph}, not {@code null}
     * @return an {@link ExtendedIterator Extended Iterator} (distinct) of all subjects or objects in the graph
     * @deprecated since 1.4.2: use the method {@link #listSubjectsAndObjects(Graph)} instead
     */
    @Deprecated
    public static ExtendedIterator<Node> subjectsAndObjects(Graph g) {
        return listSubjectsAndObjects(g);
    }

}
