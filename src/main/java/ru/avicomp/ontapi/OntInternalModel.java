package ru.avicomp.ontapi;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntID;
import ru.avicomp.ontapi.translators.AxiomParserProvider;
import ru.avicomp.ontapi.translators.OWL2RDFHelper;

/**
 * New strategy here.
 * Buffer RDF-OWL model.
 * TODO: Now there's nothing here
 * TODO: This is {@link OntGraphModel} with methods to work with the axioms. It combines jena(RDF Graph) and owl(structural, OWLAxiom) ways.
 * TODO: will be used to load and write from {@link ru.avicomp.ontapi.OntologyModel}.
 * <p>
 * Created by @szuev on 26.10.2016.
 */
public class OntInternalModel extends OntGraphModelImpl implements OntGraphModel {

    private final OWLOntologyID anonOntologyID = new OWLOntologyID();

    @Deprecated
    private final OntGraphEventStore eventStore;
    private final Map<Class<? extends OWLAxiom>, TripleStore<? extends OWLAxiom>> axiomStore = new HashMap<>();
    private final TripleStore<OWLAnnotation> annotationStore = new TripleStore<>();

    public OntInternalModel(Graph base) {
        super(base);
        this.eventStore = new OntGraphEventStore();
        getGraph().getEventManager().register(new DirectListener());
    }

    @Deprecated
    public OntGraphEventStore getEventStore() {
        return eventStore;
    }

    public OWLOntologyID getOwlID() {
        OntID id = getID();
        if (id.isAnon()) return anonOntologyID;
        IRI iri = IRI.create(id.getURI());
        IRI versionIRI = null;
        String ver = id.getVersionIRI();
        if (ver != null) {
            versionIRI = IRI.create(ver);
        }
        return new OWLOntologyID(iri, versionIRI);
    }

    public void setOwlID(OWLOntologyID id) {
        if (id.isAnonymous()) {
            setID(null).setVersionIRI(null);
            return;
        }
        IRI iri = id.getOntologyIRI().orElse(null);
        IRI versionIRI = id.getVersionIRI().orElse(null);
        setID(iri == null ? null : iri.getIRIString()).setVersionIRI(versionIRI == null ? null : versionIRI.getIRIString());
    }

    public void add(OWLAnnotation annotation) {
        ObjectListener<OWLAnnotation> listener = annotationStore.createListener(annotation);
        try {
            getGraph().getEventManager().register(listener);
            OWL2RDFHelper.addAnnotations(getID(), Stream.of(annotation));
        } finally {
            getGraph().getEventManager().unregister(listener);
        }
    }

    public void remove(OWLAnnotation annotation) {
        annotationStore.get(annotation).forEach(triple -> getGraph().delete(triple));
        annotationStore.clear(annotation);
    }

    public <C extends OWLAxiom> Set<C> getAxioms(Class<C> v) {
        return getAxiomTripleStore(v).objects();
    }

    public <C extends OWLAxiom> Set<C> getAxioms(AxiomType<C> type) {
        return getAxiomTripleStore(type.getActualClass()).objects();
    }

    public <A extends OWLAxiom> void add(A axiom) {
        ObjectListener<OWLAxiom> listener = getAxiomTripleStore(axiom.getAxiomType()).createListener(axiom);
        try {
            getGraph().getEventManager().register(listener);
            AxiomParserProvider.get(axiom.getAxiomType()).write(axiom, this);
        } finally {
            getGraph().getEventManager().unregister(listener);
        }
    }

    public <A extends OWLAxiom> void remove(A axiom) {
        TripleStore<A> store = getAxiomTripleStore(axiom.getAxiomType());
        Set<Triple> triples = store.get(axiom);
        triples.stream().filter(this::isSingleton).forEach(triple -> getGraph().delete(triple));
        store.clear(axiom);
    }

    private Set<Class<? extends OWLAxiom>> getAxiomTypes(Triple triple) {
        return axiomStore.values().stream()
                .map(s -> s.get(triple).stream())
                .flatMap(Function.identity())
                .map(OWLAxiom::getClass)
                .collect(Collectors.toSet());
    }

    private boolean isSingleton(Triple triple) {
        int count = 0;
        for (TripleStore<? extends OWLAxiom> store : axiomStore.values()) {
            count += store.get(triple).size();
            if (count > 1) return false;
        }
        return count == 1;
    }

    @SuppressWarnings("unchecked")
    private <A extends OWLAxiom> TripleStore<A> getAxiomTripleStore(AxiomType<? extends OWLAxiom> type) {
        return getAxiomTripleStore((Class<A>) type.getActualClass());
    }

    @SuppressWarnings("unchecked")
    private <A extends OWLAxiom> TripleStore<A> getAxiomTripleStore(Class<A> type) {
        return (TripleStore<A>) axiomStore.computeIfAbsent(type, c -> new TripleStore<>(AxiomParserProvider.get(type).read(this)));
    }

    @Override
    public Model removeAll() {
        axiomStore.clear();
        annotationStore.clear();
        return super.removeAll();
    }

    public class TripleStore<O extends OWLObject> {
        protected Map<O, Set<Triple>> cache;

        public TripleStore() {
            this.cache = new HashMap<>();
        }

        public TripleStore(Map<O, Set<Triple>> map) {
            this.cache = new HashMap<>(map);
        }

        public void add(O object, Triple triple) {
            cache.computeIfAbsent(object, e -> new HashSet<>()).add(triple);
        }

        public Set<Triple> get(O object) {
            return cache.getOrDefault(object, Collections.emptySet());
        }

        public Set<O> get(Triple triple) {
            return cache.entrySet().parallelStream().filter(e -> e.getValue().contains(triple)).map(Map.Entry::getKey).collect(Collectors.toSet());
        }

        public long count(Triple triple) {
            return cache.values().stream().filter(triples -> triples.contains(triple)).count();
        }

        public void delete(O object, Triple triple) {
            get(object).remove(triple);
        }

        public void put(O object, Set<Triple> triples) {
            cache.put(object, triples);
        }

        public void clear() {
            cache.clear();
        }

        public void clear(O object) {
            cache.remove(object);
        }

        public boolean isEmpty() {
            return cache.isEmpty();
        }

        public Set<O> objects() {
            return cache.keySet();
        }

        public ObjectListener<O> createListener(O obj) {
            return new ObjectListener<O>(this, obj);
        }
    }

    public class ObjectListener<O extends OWLObject> extends GraphListenerBase {
        private final TripleStore<O> store;
        private final O object;

        public ObjectListener(TripleStore<O> store, O object) {
            this.store = store;
            this.object = object;
        }

        @Override
        protected void addEvent(Triple t) {
            store.add(object, t);
        }

        @Override
        protected void deleteEvent(Triple t) {
            store.delete(object, t);
        }
    }

    public class DirectListener extends GraphListenerBase {

        private boolean can() {
            return !getGraph().getEventManager().hasListeners(ObjectListener.class);
        }

        @Override
        protected void addEvent(Triple t) {
            if (!can()) return; // we don't know which axiom would own this triple, so we clear whole cache.
            axiomStore.clear();
        }

        @Override
        protected void deleteEvent(Triple t) {
            if (!can()) return;
            getAxiomTypes(t).forEach(axiomStore::remove);
        }
    }

}
