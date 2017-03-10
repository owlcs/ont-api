package ru.avicomp.ontapi.internal;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Statement;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.OntObject;

/**
 * Immutable container for {@link OWLObject} and associated with it set of rdf-graph {@link Triple}s.
 * <p>
 * Created by @szuev on 27.11.2016.
 */
public class Wrap<O extends OWLObject> {
    private final O object;
    private final Set<Triple> triples;
    private int hashCode;

    public Wrap(O object, Set<Triple> triples) {
        this.object = OntApiException.notNull(object, "Null OWLObject.");
        this.triples = Collections.unmodifiableSet(OntApiException.notNull(triples, "Null triples."));
    }

    public Wrap(O object, Triple triple) {
        this(object, Collections.singleton(triple));
    }

    public O getObject() {
        return object;
    }

    public Set<Triple> getTriples() {
        return triples;
    }

    public Stream<Triple> triples() {
        return triples.stream();
    }

    public Graph asGraph() {
        Graph res = Factory.createGraphMem();
        GraphUtil.add(res, triples.iterator());
        return res;
    }

    public Wrap<O> add(java.util.Collection<Triple> _triples) {
        if (OntApiException.notNull(_triples, "Null triples.").isEmpty())
            return this;
        Set<Triple> set = new HashSet<>(this.triples);
        set.addAll(_triples);
        return new Wrap<>(object, set);
    }

    public Wrap<O> append(Wrap<? extends OWLObject> other) {
        return add(other.getTriples());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wrap<?> that = (Wrap<?>) o;
        return object.equals(that.object);
    }

    @Override
    public int hashCode() {
        if (hashCode != 0) return hashCode;
        return hashCode = object.hashCode();
    }

    /**
     * finds {@link Wrap} by {@link OWLObject}
     * Note: it does not take into account the hashCode.
     * There is the violation of contract inside OWLLiteral and any other object (axiom, annotation) containing literals.
     * See description of the method {@link ReadHelper#getLiteral(Literal, OWLDataFactory)}
     *
     * @param set the collection of {@link Wrap}
     * @param key {@link OWLObject}
     * @return Optional around {@link Wrap}
     */
    public static <O extends OWLObject> Optional<Wrap<O>> find(java.util.Collection<Wrap<O>> set, O key) {
        OntApiException.notNull(key, "null key");
        return set.stream().filter(Objects::nonNull).filter(o -> key.equals(o.getObject())).findAny();
        //int h = OntApiException.notNull(key, "null key").hashCode();
        //return set.stream().filter(Objects::nonNull).filter(o -> o.hashCode() == h).filter(o -> key.equals(o.getObject())).findAny();
    }

    public static <O extends OWLObject> Wrap<O> create(O o, Stream<? extends Statement> content) {
        return new Wrap<>(o, content.map(FrontsTriple::asTriple).collect(Collectors.toSet()));
    }

    public static <O extends OWLObject> Wrap<O> create(O o, OntObject content) {
        return create(o, content.content());
    }

    public static <O extends OWLObject> Wrap<O> create(O o, Statement content) {
        return new Wrap<>(o, content.asTriple());
    }

    /**
     * The collection of {@link Wrap}
     */
    public static class Collection<O extends OWLObject> {
        protected final java.util.Collection<Wrap<O>> wraps;

        public Collection(java.util.Collection<Wrap<O>> wrappers) {
            this.wraps = wrappers;
        }

        public Set<O> getObjects() {
            return objects().collect(Collectors.toSet());
        }

        public Stream<O> objects() {
            return wraps.stream().map(Wrap::getObject);
        }

        public Stream<Triple> triples() {
            return wraps.stream().map(Wrap::triples).flatMap(Function.identity());
        }

        public Set<Triple> getTriples() {
            return triples().collect(Collectors.toSet());
        }

        public Optional<Wrap<O>> find(O key) {
            return Wrap.find(wraps, key);
        }

        /**
         * Note: stream will be closed.
         *
         * @param wrappers Stream of {@link Wrap}
         * @return {@link Collection} of {@link Wrap}
         */
        public static <O extends OWLObject> Collection<O> create(Stream<Wrap<O>> wrappers) {
            return new Collection<>(wrappers.collect(Collectors.toSet()));
        }
    }
}
