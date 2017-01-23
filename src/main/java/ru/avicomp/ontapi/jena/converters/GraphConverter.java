package ru.avicomp.ontapi.jena.converters;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.log4j.Logger;

/**
 * Class to perform some transformation action on the specified graph.
 * Currently it is to convert the OWL1/RDFS ontological graph to the OWL2DL graph and to fix missed declarations.
 * Use it to fix "mistakes" in graph after loading from io-stream according OWL2 specification and before using common API.
 * <p>
 * Created by szuev on 28.10.2016.
 */
public abstract class GraphConverter {
    protected static final Logger LOGGER = Logger.getLogger(GraphConverter.class);
    public static final FactoryStore CONVERTERS = new FactoryStore()
            .add(RDFStoOWLFixer::new)
            .add(OWLtoOWL2DLFixer::new)
            .add(DeclarationFixer::new);

    /**
     * the main method to perform conversion one {@link Graph} to another.
     * Note: currently it returns the same graph, not a fixed copy.
     *
     * @param graph input graph
     * @return output graph
     */
    public static Graph convert(Graph graph) {
        CONVERTERS.actions(graph).forEach(TransformAction::process);
        return graph;
    }

    @FunctionalInterface
    public interface Factory<GC extends TransformAction> {
        GC create(Graph graph);
    }

    public static class FactoryStore {
        private Set<Factory> set = new LinkedHashSet<>();

        public FactoryStore add(Factory f) {
            set.add(f);
            return this;
        }

        public FactoryStore remove(Factory f) {
            set.remove(f);
            return this;
        }

        public Stream<TransformAction> actions(Graph graph) {
            return set.stream().map(factory -> factory.create(graph));
        }
    }


}
