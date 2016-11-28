package ru.avicomp.ontapi.translators;

import java.util.Collections;
import java.util.Set;

import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * Container for OWLObject and associated with it set of rdf-triples.
 * <p>
 * Created by @szuev on 27.11.2016.
 */
public class OWLTripleSet<O extends OWLObject> {
    private final O object;
    private final Set<Triple> triples;
    private int hashCode;

    public OWLTripleSet(O object, Set<Triple> triples) {
        this.object = object;
        this.triples = Collections.unmodifiableSet(triples);
    }

    public OWLTripleSet(O object) {
        this.object = object;
        this.triples = Collections.emptySet();
    }

    public OWLTripleSet(O object, Triple triple) {
        this.object = object;
        this.triples = Collections.singleton(triple);
    }

    public O getObject() {
        return object;
    }

    public Set<Triple> getTriples() {
        return triples;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OWLTripleSet<?> that = (OWLTripleSet<?>) o;
        return object.equals(that.object);
    }

    @Override
    public int hashCode() {
        if (hashCode != 0) return hashCode;
        return hashCode = object.hashCode();
    }
}
