package ru.avicomp.ontapi.translators;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.*;
import org.apache.jena.rdf.model.Statement;
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

    protected Wrap(O object) { // special case for literals
        this.object = OntApiException.notNull(object, "Null OWLObject.");
        this.triples = Collections.emptySet();
    }

    public Wrap(O object, Set<Triple> triples) {
        this.object = OntApiException.notNull(object, "Null OWLObject.");
        if (OntApiException.notNull(triples, "Null triples.").isEmpty()) {
            throw new OntApiException("Empty triple set.");
        }
        this.triples = Collections.unmodifiableSet(triples);
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

    public Wrap<O> add(java.util.Collection<Triple> triples) {
        Set<Triple> set = new HashSet<>(this.triples);
        set.addAll(OntApiException.notNull(triples, "Null triples."));
        return new Wrap<>(object, set);
    }

    public Wrap<O> append(Wrap<? extends OWLObject> other) {
        Set<Triple> set = new HashSet<>(this.triples);
        set.addAll(other.getTriples());
        return new Wrap<>(object, set);
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

    public static <O extends OWLObject> Optional<Wrap<O>> find(java.util.Collection<Wrap<O>> set, O key) {
        int h = OntApiException.notNull(key, "null key").hashCode();
        return set.stream().filter(Objects::nonNull).filter(o -> o.hashCode() == h).filter(o -> key.equals(o.getObject())).findAny();
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
        private final Set<Wrap<O>> wraps;

        Collection(Set<Wrap<O>> wrappers) {
            this.wraps = wrappers;
        }

        public Set<O> getObjects() {
            return wraps.stream().map(Wrap::getObject).collect(Collectors.toSet());
        }

        public Stream<Triple> triples() {
            return wraps.stream().map(Wrap::triples).flatMap(Function.identity());
        }

        public Set<Triple> getTriples() {
            return triples().collect(Collectors.toSet());
        }
    }
}
