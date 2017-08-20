package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;

import ru.avicomp.ontapi.jena.impl.OntIDImpl;
import ru.avicomp.ontapi.jena.utils.Graphs;

/**
 * Helper to work with {@link Graph Apache Jena Graps} in OWL-API terms
 *
 * @see ru.avicomp.ontapi.jena.utils.Graphs
 * @since 1.0.1
 * Created by @szuev on 19.08.2017.
 */
@SuppressWarnings("WeakerAccess")
public class OntGraphUtils {

    /**
     * Returns owl-ontology-id from ontology-graph
     *
     * @param graph {@link Graph graph}
     * @return Optional around {@link OWLOntologyID owl-ontology-id} or Optional.empty for anonymous ontology graph
     */
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
     * Builds map form the ontology graph.
     * If the specified graph is not composite then only one key in the map is expected.
     * The specified graph should consist of named graphs, only the root is allowed to be anonymous.
     * Also the graph-tree should not contain different children but with the same name (owl:ontology uri).
     *
     * @param graph {@link Graph graph}
     * @return Map with {@link OWLOntologyID owl-ontology-id} as a key and {@link Graph graph} as a value
     * @throws OntApiException the input graph has restrictions, see above.
     */
    public static Map<OWLOntologyID, Graph> toGraphMap(@Nonnull Graph graph) {
        Graph base = Graphs.getBase(graph);
        return Graphs.flat(graph)
                .collect(Collectors.toMap(g -> {
                    Optional<OWLOntologyID> id = ontologyID(g);
                    if (!id.isPresent() && !base.isIsomorphicWith(g)) {
                        throw new OntApiException("Anonymous sub graph");
                    }
                    return id.orElse(null);
                }, Function.identity(), (a, b) -> {
                    if (a.isIsomorphicWith(b)) {
                        return a;
                    }
                    throw new OntApiException("Duplicate sub graph");
                }));
    }
}
