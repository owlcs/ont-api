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
@SuppressWarnings("WeakerAccess")
public abstract class GraphTransformers {

    protected static Store converters = new Store()
            .add(RDFSTransform::new)
            .add(OWLTransform::new)
            .add(DeclarationTransform::new);

    public static Store setTransformers(Store store) {
        OntJenaException.notNull(store, "Null converter store specified.");
        Store prev = converters;
        converters = store;
        return prev;
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

    /**
     * immutable store of graph transform makers/
     *
     * @see Maker
     */
    public static class Store implements Serializable {
        protected Set<Maker> set = new LinkedHashSet<>();

        public Store copy() {
            Store res = new Store();
            res.set = new LinkedHashSet<>(this.set);
            return res;
        }

        public Store add(Maker f) {
            Store res = copy();
            res.set.add(f);
            return res;
        }

        public Store remove(Maker f) {
            Store res = copy();
            res.set.remove(f);
            return res;
        }

        public Stream<Transform> actions(Graph graph) {
            return set.stream().map(factory -> factory.create(graph));
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof Store && set.equals(((Store) o).set);
        }

        @Override
        public int hashCode() {
            return set.hashCode();
        }
    }

    public static class DefaultMaker implements Maker {
        protected final Class<? extends Transform> impl;

        public DefaultMaker(Class<? extends Transform> impl) {
            this.impl = impl;
        }

        @Override
        public Transform create(Graph graph) {
            try {
                return impl.getDeclaredConstructor(Graph.class).newInstance(graph);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new OntJenaException("Must have public constructor with " + Graph.class.getName() + " as the only parameter.", e);
            }
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof DefaultMaker && impl.equals(((DefaultMaker) o).impl);
        }

        @Override
        public int hashCode() {
            return impl.hashCode();
        }
    }

}
