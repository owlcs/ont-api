package ru.avicomp.ontapi.jena.utils;

import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import org.apache.jena.atlas.iterator.Iter;
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

    public static Stream<Graph> subGraphs(Graph graph) {
        return graph instanceof UnionGraph ? ((UnionGraph) graph).getUnderlying().graphs() :
                graph instanceof Polyadic ? ((Polyadic) graph).getSubGraphs().stream() :
                        graph instanceof Dyadic ? Stream.of((Graph) ((Dyadic) graph).getR()) : Stream.empty();
    }

    public static Graph getBase(Graph graph) {
        return graph instanceof UnionGraph ? ((UnionGraph) graph).getBaseGraph() :
                graph instanceof Polyadic ? ((Polyadic) graph).getBaseGraph() :
                        graph instanceof Dyadic ? (Graph) ((Dyadic) graph).getL() : graph;
    }

    public static Stream<Graph> flat(Graph graph) {
        return Stream.concat(Stream.of(getBase(graph)), subGraphs(graph).map(Graphs::flat).flatMap(Function.identity()));
    }

    /**
     * gets Ontology URI or null (if no owl:Ontology or it is anonymous ontology).
     *
     * @param graph {@link Graph}
     * @return String
     */
    public static String getURI(Graph graph) {
        return getOntology(graph).filter(Node::isURI).map(Node::getURI).orElse(null);
    }

    /**
     * gets "name" of graph: uri, blank-node-id as string or dummy string if no ontology at all.
     *
     * @param graph {@link Graph}
     * @return String
     */
    public static String getName(Graph graph) {
        return getOntology(graph).map(Node::toString).orElse("NullOntology");
    }

    /**
     * gets ontology node (subject in "_:x rdf:type owl:Ontology") from graph or null if there are no ontology sections.
     * if there several ontologies it chooses the most bulky.
     *
     * @param graph {@link Graph}
     * @return {@link Optional} around the {@link Node} which could be uri or blank.
     */
    public static Optional<Node> getOntology(Graph graph) {
        return Iter.asStream(getBase(graph).find(Node.ANY, RDF.type.asNode(), OWL.Ontology.asNode()))
                .map(Triple::getSubject)
                .filter(node -> node.isBlank() || node.isURI())
                .sorted(Comparator.comparingInt((ToIntFunction<Node>) subj -> graph.find(subj, Node.ANY, Node.ANY).toList().size()).reversed())
                .findFirst();
    }

    public static Set<String> getImports(Graph graph) {
        return getBase(graph).find(Node.ANY, OWL.imports.asNode(), Node.ANY)
                .mapWith(Triple::getObject)
                .filterKeep(Node::isURI)
                .mapWith(Node::getURI).toSet();
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
