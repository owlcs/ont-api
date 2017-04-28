package ru.avicomp.ontapi.internal;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Statement;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.model.OntObject;

/**
 * Immutable container for {@link OWLObject} and associated with it set of rdf-graph {@link Triple}s.
 * <p>
 * Created by @szuev on 27.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class InternalObject<O extends OWLObject> {
    private final O object;
    private final Set<Triple> triples;
    private int hashCode;

    public InternalObject(O object, Set<Triple> triples) {
        this.object = OntApiException.notNull(object, "Null OWLObject.");
        this.triples = Collections.unmodifiableSet(OntApiException.notNull(triples, "Null triples."));
    }

    public InternalObject(O object, Triple triple) {
        this(object, Collections.singleton(triple));
    }

    /**
     * Gets wrapped {@link OWLObject}.
     *
     * @return OWL object
     */
    public O getObject() {
        return object;
    }

    /**
     * Gets {@link Triple}s associated with encapsulated {@link OWLObject}
     *
     * @return Set of triples
     */
    public Set<Triple> getTriples() {
        return triples;
    }

    public Stream<Triple> triples() {
        return triples.stream();
    }

    /**
     * Presents this container as {@link Graph}
     *
     * @return graph
     * @see ru.avicomp.ontapi.jena.utils.Graphs#toTurtleString(Graph)
     */
    public Graph asGraph() {
        Graph res = OntModelFactory.createDefaultGraph();
        GraphUtil.add(res, triples.iterator());
        return res;
    }

    public InternalObject<O> add(java.util.Collection<Triple> _triples) {
        if (OntApiException.notNull(_triples, "Null triples.").isEmpty())
            return this;
        Set<Triple> set = new HashSet<>(this.triples);
        set.addAll(_triples);
        return new InternalObject<>(object, set);
    }

    public InternalObject<O> append(InternalObject<? extends OWLObject> other) {
        return add(other.getTriples());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InternalObject<?> that = (InternalObject<?>) o;
        return object.equals(that.object);
    }

    @Override
    public int hashCode() {
        if (hashCode != 0) return hashCode;
        return hashCode = object.hashCode();
    }

    /**
     * finds {@link InternalObject} by {@link OWLObject}
     * Note: it does not take into account the hashCode.
     * There is the violation of contract inside OWLLiteral and any other object (axiom, annotation) containing literals.
     * See description of the method {@link ReadHelper#getLiteral(Literal, OWLDataFactory)}
     *
     * @param set the collection of {@link InternalObject}
     * @param key {@link OWLObject}
     * @return Optional around {@link InternalObject}
     */
    public static <O extends OWLObject> Optional<InternalObject<O>> find(java.util.Collection<InternalObject<O>> set, O key) {
        OntApiException.notNull(key, "null key");
        return set.stream().filter(Objects::nonNull).filter(o -> key.equals(o.getObject())).findAny();
        //int h = OntApiException.notNull(key, "null key").hashCode();
        //return set.stream().filter(Objects::nonNull).filter(o -> o.hashCode() == h).filter(o -> key.equals(o.getObject())).findAny();
    }

    public static <O extends OWLObject> InternalObject<O> create(O o, Stream<? extends Statement> content) {
        return new InternalObject<>(o, content.map(FrontsTriple::asTriple).collect(Collectors.toSet()));
    }

    public static <O extends OWLObject> InternalObject<O> create(O o, OntObject content) {
        return create(o, content.content());
    }

    public static <O extends OWLObject> InternalObject<O> create(O o, Statement content) {
        return new InternalObject<>(o, content.asTriple());
    }

    /**
     * The 'collection' of {@link InternalObject}s.
     */
    public static class Collection<O extends OWLObject> {
        protected final java.util.Collection<InternalObject<O>> wraps;
        private Set<Triple> triples;

        public Collection(java.util.Collection<InternalObject<O>> wrappers) {
            this.wraps = wrappers;
        }

        /**
         * Gets naked {@link OWLObject}s.
         *
         * @return Set of objects.
         */
        public Set<O> getObjects() {
            return objects().collect(Collectors.toSet());
        }

        /**
         * Gets {@link Triple}s.
         *
         * @return Set of triples.
         */
        public Set<Triple> getTriples() {
            return triples == null ? triples = Collections.unmodifiableSet(wraps.stream()
                    .map(InternalObject::triples)
                    .flatMap(Function.identity())
                    .collect(Collectors.toSet())) : triples;
        }

        /**
         * @return Set of {@link InternalObject}
         */
        public Set<InternalObject<O>> getWraps() {
            return Collections.unmodifiableSet(wraps instanceof Set ? (Set<InternalObject<O>>) wraps : new HashSet<>(wraps));
        }

        public Stream<O> objects() {
            return wraps.stream().map(InternalObject::getObject);
        }

        public Stream<Triple> triples() {
            return getTriples().stream();
        }

        public Optional<InternalObject<O>> find(O key) {
            return InternalObject.find(wraps, key);
        }

        /**
         * Note: stream will be closed.
         *
         * @param wrappers Stream of {@link InternalObject}
         * @return {@link Collection} of {@link InternalObject}
         */
        public static <O extends OWLObject> Collection<O> create(Stream<InternalObject<O>> wrappers) {
            return new Collection<>(wrappers.collect(Collectors.toList()));
        }
    }
}
