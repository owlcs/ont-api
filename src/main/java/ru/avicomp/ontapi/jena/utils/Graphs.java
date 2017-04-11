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
     * @return String
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
        return getOntology(getBase(graph)).map(Node::toString).orElse("NullOntology");
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
        return Iter.asStream(g.find(Node.ANY, RDF.type.asNode(), OWL.Ontology.asNode()))
                .map(Triple::getSubject)
                .filter(node -> node.isBlank() || node.isURI())
                .sorted(rootNodeComparator(g))
                .findFirst();
    }

    /**
     * Returns comparator for root nodes.
     * Tricky logic:
     * first compares roots as standalone nodes and the any uri-node is considered less then any blank-node,
     * then compares roots as part of the graph using the rule 'the fewer children -> the greater the weight'.
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
        if (seen.contains(graph)) {
            throw new OntJenaException("Unexpected recursion cycle for graph " + graph);
        }
        seen.add(graph);
        sb.append("<").append(getURI(graph)).append(">");
        sb.append("\n");
        subGraphs(graph).forEach(sub -> sb.append(sep).append(makeImportsTree(sub, sep + sep, seen)));
        return sb;
    }

    public static String toTurtleString(Graph g) {
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, g, OntFormat.TURTLE.getLang());
        return sw.toString();
    }
}
