package ru.avicomp.ontapi.transforms;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;

import ru.avicomp.ontapi.jena.OntJenaException;

/**
 * Class to perform some transformation action on the specified graph.
 * Currently it is to convert the OWL and RDFS ontological graphs to the OWL2-DL graph and to fix missed declarations.
 * Can be used to fix "mistaken" ontologies in accordance with OWL2 specification after loading from io-stream
 * but before using common API.
 * <p>
 * Created by szuev on 28.10.2016.
 */
public abstract class GraphTransformers {
    private static Store converters = new Store()
            .add(RDFSTransform::new)
            .add(OWLTransform::new)
            .add(DeclarationTransform::new);

    public static Store setTransformers(Store store) {
        OntJenaException.notNull(store, "Null converter store specified.");
        Store res = converters;
        converters = store;
        return res;
    }

    public static Store getTransformers() {
        return converters;
    }

    /**
     * helper method to perform conversion one {@link Graph} to another.
     * Note: currently it returns the same graph, not a fixed copy.
     *
     * @param graph input graph
     * @return output graph
     */
    public static Graph convert(Graph graph) {
        getTransformers().actions(graph).forEach(Transform::process);
        return graph;
    }

    @FunctionalInterface
    public interface Maker<GC extends Transform> extends Serializable {
        GC create(Graph graph);
    }

    public static class Store implements Serializable {
        private Set<Maker> set = new LinkedHashSet<>();

        public Store add(Maker f) {
            set.add(f);
            return this;
        }

        public Store remove(Maker f) {
            set.remove(f);
            return this;
        }

        public Stream<Transform> actions(Graph graph) {
            return set.stream().map(factory -> factory.create(graph));
        }
    }

    public static <GC extends Transform> GC createTransformAction(Class<GC> impl, Graph graph) {
        try {
            return impl.getDeclaredConstructor(Graph.class).newInstance(graph);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new OntJenaException("Must have public constructor with " + Graph.class.getName() + " as parameter.", e);
        }
    }

}
