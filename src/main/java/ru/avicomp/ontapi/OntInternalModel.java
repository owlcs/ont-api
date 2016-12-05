package ru.avicomp.ontapi;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.impl.OntEntityImpl;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.translators.AxiomParserProvider;
import ru.avicomp.ontapi.translators.OWL2RDFHelper;
import ru.avicomp.ontapi.translators.RDF2OWLHelper;
import uk.ac.manchester.cs.owl.owlapi.OWLImportsDeclarationImpl;

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
    private final Map<Class<? extends OWLAxiom>, TripleStore<? extends OWLAxiom>> axiomStores = new HashMap<>();
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

    public Stream<OWLImportsDeclaration> importDeclarations() {
        return super.imports().map(Resource::getURI).map(IRI::create).map(OWLImportsDeclarationImpl::new);
    }

    public boolean isEmpty() {
        return axiomStores.values().stream().allMatch(TripleStore::isEmpty) && annotationStore.isEmpty();
    }

    public Stream<OWLAnonymousIndividual> anonymousIndividuals() {
        return ontObjects(OntIndividual.Anonymous.class).filter(OntObject::isLocal).map(RDF2OWLHelper::getAnonymousIndividual);
    }

    public Stream<OWLNamedIndividual> individuals() {
        return ontEntities(OntIndividual.Named.class).filter(OntObject::isLocal).map(RDF2OWLHelper::getIndividual).map(AsOWLNamedIndividual::asOWLNamedIndividual);
    }

    private <P extends OntEntity & OntPE> Stream<P> builtInProperties(Set<Resource> candidates, Class<P> view) {
        Predicate<Resource> predicateTester = r -> getBaseGraph().contains(Node.ANY, r.asNode(), Node.ANY); // property assertions
        Predicate<Resource> objectTester = r -> getBaseGraph().contains(Node.ANY, Node.ANY, r.asNode());
        return candidates.stream()
                .filter(predicateTester.or(objectTester))
                .map(Resource::getURI)
                .map(u -> getOntEntity(view, u));
    }

    private <E extends OntEntity> Stream<E> builtInEntities(Set<Resource> candidates, Class<E> view) {
        return candidates.stream()
                .filter(r -> getBaseGraph().contains(Node.ANY, Node.ANY, r.asNode()))
                .map(Resource::getURI)
                .map(u -> getOntEntity(view, u));
    }

    public Stream<OWLClass> classes() {
        Stream<OntClass> local = ontEntities(OntClass.class).filter(OntObject::isLocal);
        Stream<OntClass> builtIn = builtInEntities(OntEntityImpl.BUILT_IN_CLASSES, OntClass.class);
        return Stream.concat(local, builtIn).distinct().map(RDF2OWLHelper::getClassExpression).map(AsOWLClass::asOWLClass);
    }

    public Stream<OWLDataProperty> dataProperties() {
        Stream<OntNDP> local = ontEntities(OntNDP.class).filter(OntObject::isLocal);
        Stream<OntNDP> builtIn = builtInProperties(OntEntityImpl.BUILT_IN_DATA_PROPERTIES, OntNDP.class);
        return Stream.concat(local, builtIn).distinct().map(RDF2OWLHelper::getDataProperty);
    }

    public Stream<OWLObjectProperty> objectProperties() {
        Stream<OntNOP> local = ontEntities(OntNOP.class).filter(OntObject::isLocal);
        Stream<OntNOP> builtIn = builtInProperties(OntEntityImpl.BUILT_IN_OBJECT_PROPERTIES, OntNOP.class);
        return Stream.concat(local, builtIn).distinct().map(RDF2OWLHelper::getObjectProperty).map(AsOWLObjectProperty::asOWLObjectProperty);
    }

    public Stream<OWLAnnotationProperty> annotationProperties() {
        Stream<OntNAP> local = ontEntities(OntNAP.class).filter(OntObject::isLocal);
        Stream<OntNAP> builtIn = builtInProperties(OntEntityImpl.BUILT_IN_ANNOTATION_PROPERTIES, OntNAP.class);
        return Stream.concat(local, builtIn).distinct().map(RDF2OWLHelper::getAnnotationProperty);
    }

    public Stream<OWLDatatype> datatypes() {
        Stream<OntDT> local = ontEntities(OntDT.class).filter(OntObject::isLocal);
        Stream<OntDT> builtIn = builtInEntities(OntEntityImpl.BUILT_IN_DATATYPES, OntDT.class);
        return Stream.concat(local, builtIn).distinct().map(RDF2OWLHelper::getDatatype);
    }

    public void add(OWLAnnotation annotation) { //todo: change
        ObjectListener<OWLAnnotation> listener = annotationStore.createListener(annotation);
        try {
            getGraph().getEventManager().register(listener);
            OWL2RDFHelper.addAnnotations(getID(), Stream.of(annotation));
        } finally {
            getGraph().getEventManager().unregister(listener);
        }
    }

    public void remove(OWLAnnotation annotation) { //todo: change
        annotationStore.get(annotation).forEach(triple -> getGraph().delete(triple));
        annotationStore.clear(annotation);
    }

    public Set<OWLAnnotation> getAnnotations() {
        return annotationStore.getObjects();
    }

    public <C extends OWLAxiom> Set<C> getAxioms(Class<C> v) {
        return getAxiomTripleStore(v).getObjects();
    }

    public <C extends OWLAxiom> Set<C> getAxioms(AxiomType<C> type) {
        return getAxiomTripleStore(type.getActualClass()).getObjects();
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
        return axiomStores.values().stream()
                .map(s -> s.get(triple).stream())
                .flatMap(Function.identity())
                .map(OWLAxiom::getClass)
                .collect(Collectors.toSet());
    }

    private boolean isSingleton(Triple triple) {
        int count = 0;
        for (TripleStore<? extends OWLAxiom> store : axiomStores.values()) {
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
        return (TripleStore<A>) axiomStores.computeIfAbsent(type, c -> new TripleStore<>(AxiomParserProvider.get(type).read(this)));
    }

    @Override
    public Model removeAll() {
        axiomStores.clear();
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

        public Set<O> getObjects() {
            return cache.keySet();
        }

        public ObjectListener<O> createListener(O obj) {
            return new ObjectListener<>(this, obj);
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
            axiomStores.clear();
        }

        @Override
        protected void deleteEvent(Triple t) {
            if (!can()) return;
            getAxiomTypes(t).forEach(axiomStores::remove);
        }
    }

}
