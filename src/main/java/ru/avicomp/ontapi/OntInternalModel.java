package ru.avicomp.ontapi;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntID;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntStatement;
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

    public boolean isOntologyEmpty() {
        return axiomStores.values().stream().allMatch(TripleStore::isEmpty) && getAnnotations().isEmpty();
    }

    public Stream<OWLAnonymousIndividual> anonymousIndividuals() {
        return objects(OWLAnonymousIndividual.class);
    }

    public Stream<OWLNamedIndividual> individuals() {
        return objects(OWLNamedIndividual.class);
    }

    public Stream<OWLClass> classes() {
        return objects(OWLClass.class);
    }

    public Stream<OWLDataProperty> dataProperties() {
        return objects(OWLDataProperty.class);
    }

    public Stream<OWLObjectProperty> objectProperties() {
        return objects(OWLObjectProperty.class);
    }

    public Stream<OWLAnnotationProperty> annotationProperties() {
        return objects(OWLAnnotationProperty.class);
    }

    public Stream<OWLDatatype> datatypes() {
        return objects(OWLDatatype.class);
    }

    public static <T extends OWLObject> Stream<T> parseComponents(Class<T> view, HasComponents structure) {
        return structure.componentsWithoutAnnotations().map(o -> toStream(view, o)).flatMap(Function.identity());
    }

    public static <T extends OWLObject> Stream<T> parseAnnotations(Class<T> view, HasAnnotations structure) {
        return structure.annotations().map(o -> toStream(view, o)).flatMap(Function.identity());
    }

    public static <R extends OWLObject, S extends HasAnnotations & HasComponents> Stream<R> parse(Class<R> view, S container) {
        return Stream.concat(parseComponents(view, container), parseAnnotations(view, container));
    }

    private static <T extends OWLObject> Stream<T> toStream(Class<T> view, Object o) {
        if (view.isInstance(o)) {
            return Stream.of(view.cast(o));
        }
        if (o instanceof HasComponents) {
            return parseComponents(view, (HasComponents) o);
        }
        if (o instanceof HasAnnotations) {
            return parseAnnotations(view, (HasAnnotations) o);
        }
        if (o instanceof Stream) {
            return ((Stream<?>) o).map(_o -> toStream(view, _o)).flatMap(Function.identity());
        }
        return Stream.empty();
    }

    public <E extends OWLObject> Stream<E> objects(Class<E> view) {
        return Stream.concat(annotations().map(annotation -> parse(view, annotation)).flatMap(Function.identity()),
                axioms().map(axiom -> parse(view, axiom)).flatMap(Function.identity())).distinct();
    }

    public void add(OWLAnnotation annotation) {
        OWL2RDFHelper.addAnnotations(getID(), Stream.of(annotation));
    }

    public void remove(OWLAnnotation annotation) {
        Set<Triple> triples = new HashSet<>();
        if (annotation.annotations().count() == 0) { // plain annotation
            OntStatement ontAnnotation = getID().annotations().filter(a -> !a.hasAnnotations())
                    .filter(a -> RDF2OWLHelper.getAnnotationProperty(a.getPredicate().as(OntNAP.class)).equals(annotation.getProperty()))
                    .filter(a -> RDF2OWLHelper.getAnnotationValue(a.getObject()).equals(annotation.getValue())).findFirst().orElse(null);
            if (ontAnnotation != null) {
                triples.add(ontAnnotation.asTriple());
                triples.addAll(RDF2OWLHelper.getAssociatedTriples(ontAnnotation.getObject())); // as value there could be anonymous individual
            }
        } else { // bulk annotation
            RDF2OWLHelper.TripleSet<OWLAnnotation> set = RDF2OWLHelper.getBulkAnnotations(getID())
                    .stream().filter(t -> t.getObject().equals(annotation)).findFirst().orElse(null);
            if (set != null) {
                triples.addAll(set.getTriples());
            }
        }
        triples.stream().filter(this::canDelete).forEach(triple -> getGraph().delete(triple));
    }

    public Stream<OWLAnnotation> annotations() {
        return RDF2OWLHelper.annotations(getID());
    }

    public Set<OWLAnnotation> getAnnotations() {
        return annotations().collect(Collectors.toSet());
    }

    public Set<OWLAxiom> getAxioms() {
        return axioms().collect(Collectors.toSet());
    }

    public Stream<OWLAxiom> axioms() {
        return AxiomType.AXIOM_TYPES.stream()
                .map(this::getAxioms)
                .map(Collection::stream).flatMap(Function.identity());
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
        store.clear(axiom);
        triples.stream().filter(this::canDelete).forEach(triple -> getGraph().delete(triple));
    }

    private Set<Class<? extends OWLAxiom>> getAxiomTypes(Triple triple) {
        return axiomStores.values().stream()
                .map(s -> s.get(triple).stream())
                .flatMap(Function.identity())
                .map(OWLAxiom::getClass)
                .collect(Collectors.toSet());
    }

    /**
     * checks if it is possible to delete triple from the graph.
     *
     * @param triple Triple
     * @return true if there are no axiom which includes this triple, otherwise false.
     */
    private boolean canDelete(Triple triple) {
        int count = 0;
        for (TripleStore<? extends OWLAxiom> store : axiomStores.values()) {
            count += store.get(triple).size();
            if (count > 1) return false;
        }
        return count == 0;
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
        clearCache();
        return super.removeAll();
    }

    private void clearCache() {
        axiomStores.clear();
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
            if (!can()) return;
            // we don't know which axiom would own this triple, so we clear whole cache.
            clearCache();
        }

        @Override
        protected void deleteEvent(Triple t) {
            if (!can()) return;
            getAxiomTypes(t).forEach(axiomStores::remove);
        }
    }

}
