package ru.avicomp.ontapi.internal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.Lock;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OwlObjects;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.*;

/**
 * Buffer RDF-OWL model.
 * The analogy of {@link uk.ac.manchester.cs.owl.owlapi.Internals}
 * This is a non-serializable(!) {@link OntGraphModel} but with methods to work with the owl-axioms and owl-entities.
 * It combines jena(RDF Graph) and owl(structural, OWLAxiom) ways and
 * it is used by {@link ru.avicomp.ontapi.OntologyModel} to read and write structural representation of ontology.
 * <p>
 * todo: should return {@link InternalObject}s, not just naked {@link OWLObject}s
 * <p>
 * Created by @szuev on 26.10.2016.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class InternalModel extends OntGraphModelImpl implements OntGraphModel, ConfigProvider {

    /**
     * the experimental flag which specifies the behaviour on axioms loading.
     * if true then {@link AxiomTranslator}s works in parallel mode (see {@link #axioms(Set)}).
     * As shown by pizza-performance-test it really helps to speed up the initial loading.
     * TODO: by some unclear reasons (perhaps due to violation of contract with read/write locks)
     * there could be live-lock when we work in concurrent mode. from this point this flag is always false.
     */
    protected static boolean optimizeCollecting = false;
    // axioms & header annotations store.
    // used to work through OWL-API. the use of jena model methods will clear this cache.
    protected Map<Class<? extends OWLObject>, OwlObjectTriplesMap<? extends OWLObject>> componentsStore;
    // OWL objects store to improve performance (working through OWL-API interface with 'signature')
    // any change in the graph resets these caches.
    protected Map<Class<? extends OWLObject>, Set<? extends OWLObject>> objectsStore;
    // Temporary stores for collecting axioms, should be reset after axioms getting.
    protected Map<OntCE, InternalObject<? extends OWLClassExpression>> owlCLEStore;
    protected Map<OntDR, InternalObject<? extends OWLDataRange>> owlDRGStore;
    protected Map<OntIndividual, InternalObject<? extends OWLIndividual>> owlINDStore;
    protected Map<OntNAP, InternalObject<OWLAnnotationProperty>> owlNAPStore;
    protected Map<OntNDP, InternalObject<OWLDataProperty>> owlNDPStore;
    protected Map<OntOPE, InternalObject<? extends OWLObjectPropertyExpression>> owlOPEStore;

    private ConfigProvider.Config config;

    /**
     * For internal usage only.
     *
     * @param base   {@link Graph}
     * @param config {@link ru.avicomp.ontapi.internal.ConfigProvider.Config}
     */
    public InternalModel(Graph base, ConfigProvider.Config config) {
        super(base, config.loaderConfig().getPersonality());
        this.config = config;
        getGraph().getEventManager().register(new DirectListener());
        componentsStore = optimizeCollecting ? new ConcurrentHashMap<>(29, 0.75f, 39) : new HashMap<>();
        objectsStore = new HashMap<>();
        // Use Collections#synchronizedMap instead of ConcurrentHashMap due to some live-lock during tests,
        // the reasons for this are not clear to me (TODO: investigate!)
        owlCLEStore = optimizeCollecting ? Collections.synchronizedMap(new HashMap<>()) : new HashMap<>();
        owlDRGStore = optimizeCollecting ? Collections.synchronizedMap(new HashMap<>()) : new HashMap<>();
        owlINDStore = optimizeCollecting ? Collections.synchronizedMap(new HashMap<>()) : new HashMap<>();
        owlNAPStore = optimizeCollecting ? Collections.synchronizedMap(new HashMap<>()) : new HashMap<>();
        owlNDPStore = optimizeCollecting ? Collections.synchronizedMap(new HashMap<>()) : new HashMap<>();
        owlOPEStore = optimizeCollecting ? Collections.synchronizedMap(new HashMap<>()) : new HashMap<>();
    }

    @Override
    public ConfigProvider.Config getConfig() {
        return config;
    }

    public OWLDataFactory dataFactory() {
        return getConfig().dataFactory();
    }

    public OntLoaderConfiguration loaderConfig() {
        return getConfig().loaderConfig();
    }

    public OWLOntologyWriterConfiguration writerConfig() {
        return getConfig().writerConfig();
    }

    /**
     * Since in ONT-API we use another kind of lock this method is disabled.
     *
     * @see ru.avicomp.ontapi.jena.ConcurrentGraph
     */
    @Override
    public Lock getLock() {
        throw new OntApiException.Unsupported();
    }

    /**
     * @see this#getLock()
     */
    @Override
    public void enterCriticalSection(boolean requestReadLock) {
        throw new OntApiException.Unsupported();
    }

    /**
     * @see this#getLock()
     */
    @Override
    public void leaveCriticalSection() {
        throw new OntApiException.Unsupported();
    }

    public Stream<OWLImportsDeclaration> importDeclarations() {
        return getID().imports().map(IRI::create).map(i -> dataFactory().getOWLImportsDeclaration(i));
    }

    public boolean isOntologyEmpty() {
        return axioms().count() == 0 && annotations().count() == 0;
    }

    protected InternalObject<? extends OWLClassExpression> fetchClassExpression(OntCE ce) {
        return owlCLEStore.computeIfAbsent(ce, c -> ReadHelper.getClassExpression(c, dataFactory()));
    }

    protected InternalObject<? extends OWLDataRange> fetchDataRange(OntDR dr) {
        return owlDRGStore.computeIfAbsent(dr, d -> ReadHelper.getDataRange(d, dataFactory()));
    }

    protected InternalObject<? extends OWLIndividual> fetchIndividual(OntIndividual indi) {
        return owlINDStore.computeIfAbsent(indi, i -> ReadHelper.getIndividual(i, dataFactory()));
    }

    protected InternalObject<OWLAnnotationProperty> fetchAnnotationProperty(OntNAP nap) {
        return owlNAPStore.computeIfAbsent(nap, p -> ReadHelper.getAnnotationProperty(p, dataFactory()));
    }

    protected InternalObject<OWLDataProperty> fetchDataProperty(OntNDP ndp) {
        return owlNDPStore.computeIfAbsent(ndp, p -> ReadHelper.getDataProperty(p, dataFactory()));
    }

    protected InternalObject<? extends OWLObjectPropertyExpression> fetchObjectProperty(OntOPE ope) {
        return owlOPEStore.computeIfAbsent(ope, p -> ReadHelper.getObjectPropertyExpression(p, dataFactory()));
    }

    public Stream<OWLIndividual> individuals() {
        return objects(OWLIndividual.class);
    }

    public Stream<OWLAnonymousIndividual> anonymousIndividuals() {
        return objects(OWLAnonymousIndividual.class);
    }

    public Stream<OWLNamedIndividual> namedIndividuals() {
        return objects(OWLNamedIndividual.class);
    }

    public Stream<OWLClass> classes() {
        return objects(OWLClass.class);
    }

    public Stream<OWLDataProperty> dataProperties() {
        return objects(OWLDataProperty.class);
    }

    public Stream<OWLObjectPropertyExpression> objectPropertyExpressions() {
        return objects(OWLObjectPropertyExpression.class);
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

    public List<OWLEntity> getEntities(IRI iri) {
        if (iri == null) return Collections.emptyList();
        OntEntity e = getOntEntity(OntEntity.class, iri.getIRIString());
        List<OWLEntity> res = new ArrayList<>();
        if (e.canAs(OntClass.class)) {
            res.add(ReadHelper.fetchClass(e.as(OntClass.class), dataFactory()).getObject().asOWLClass());
        }
        if (e.canAs(OntDT.class)) {
            res.add(ReadHelper.fetchDatatype(e.as(OntDT.class), dataFactory()).getObject());
        }
        if (e.canAs(OntNAP.class)) {
            res.add(ReadHelper.fetchAnnotationProperty(e.as(OntNAP.class), dataFactory()).getObject());
        }
        if (e.canAs(OntNDP.class)) {
            res.add(ReadHelper.fetchDataProperty(e.as(OntNDP.class), dataFactory()).getObject());
        }
        if (e.canAs(OntNOP.class)) {
            res.add(ReadHelper.fetchObjectProperty(e.as(OntNOP.class), dataFactory()).getObject().asOWLObjectProperty());
        }
        if (e.canAs(OntIndividual.Named.class)) {
            res.add(ReadHelper.fetchIndividual(e.as(OntIndividual.Named.class), dataFactory()).getObject().asOWLNamedIndividual());
        }
        return res;
    }

    public Stream<OntEntity> ambiguousEntities(boolean withImports) {
        Set<Class<? extends OntEntity>> types = Stream.of(OntClass.class, OntDT.class, OntNAP.class, OntNDP.class, OntNOP.class, OntIndividual.Named.class).collect(Collectors.toSet());
        return ontEntities().filter(e -> withImports || e.isLocal()).filter(e -> types.stream()
                .filter(view -> e.canAs(view) && (withImports || e.as(view).isLocal())).count() > 1);
    }

    @SuppressWarnings("unchecked")
    protected <E extends OWLObject> Stream<E> objects(Class<E> view) {
        return (Stream<E>) objectsStore.computeIfAbsent(view, c ->
                Stream.concat(
                        annotations().map(annotation -> OwlObjects.objects(c, annotation)).flatMap(Function.identity()),
                        axioms().map(axiom -> OwlObjects.objects(c, axiom)).flatMap(Function.identity())
                ).collect(Collectors.toSet())).stream();
    }

    /**
     * Adds ontology header annotation to the model.
     *
     * @param annotation {@link OWLAnnotation}
     * @see #add(OWLAxiom)
     */
    public void add(OWLAnnotation annotation) {
        add(annotation, getAnnotationTripleStore(), a -> WriteHelper.addAnnotations(getID(), Stream.of(annotation)));
    }

    /**
     * Removes ontology header annotation from the model.
     *
     * @param annotation {@link OWLAnnotation}
     * @see #remove(OWLAxiom)
     */
    public void remove(OWLAnnotation annotation) {
        remove(annotation, getAnnotationTripleStore());
    }

    /**
     * Gets all ontology header annotations.
     *
     * @return Stream of {@link OWLAnnotation}
     * @see #getAxioms(AxiomType)
     */
    @SuppressWarnings("unchecked")
    public Stream<OWLAnnotation> annotations() {
        return getAnnotationTripleStore().getObjects().stream();
    }

    /**
     * The main method for loading/getting axioms.
     * NOTE: there is a parallel collecting in case {@link #optimizeCollecting} equals true,
     * {@link #componentsStore} is empty and the set of {@link AxiomType}s contains more then one element.
     *
     * @param types Set of {@link AxiomType}s
     * @return Stream of {@link OWLAxiom}
     * @see #annotations()
     */
    public Stream<OWLAxiom> axioms(Set<AxiomType<? extends OWLAxiom>> types) {
        if (optimizeCollecting && componentsStore.isEmpty() && types.size() > 1) {
            types.parallelStream().forEach(this::getAxioms);
            return componentsStore.values().stream()
                    .filter(v -> !Objects.equals(v.type(), OWLAnnotation.class))
                    .map(OwlObjectTriplesMap::getObjects)
                    .map(Collection::stream)
                    .flatMap(Function.identity())
                    .map(OWLAxiom.class::cast);
        }
        return types.stream()
                .map(this::getAxioms)
                .map(Collection::stream).flatMap(Function.identity());
    }

    /**
     * Adds axiom to the model
     *
     * @param axiom {@link OWLAxiom}
     * @see #add(OWLAnnotation)
     */
    public void add(OWLAxiom axiom) {
        add(axiom, getAxiomTripleStore(axiom.getAxiomType()), a -> AxiomParserProvider.get(a.getAxiomType()).write(a, InternalModel.this));
    }

    /**
     * Removes axiom from the model
     *
     * @param axiom {@link OWLAxiom}
     * @see #remove(OWLAnnotation)
     */
    public void remove(OWLAxiom axiom) {
        remove(axiom, getAxiomTripleStore(axiom.getAxiomType()));
    }

    public Stream<OWLAxiom> axioms() {
        return axioms(AxiomType.AXIOM_TYPES);
    }

    public <A extends OWLAxiom> Stream<A> axioms(Class<A> view) {
        return axioms(AxiomType.getTypeForClass(OntApiException.notNull(view, "Null axiom class type.")));
    }

    @SuppressWarnings("unchecked")
    public <A extends OWLAxiom> Stream<A> axioms(AxiomType<A> type) {
        return axioms(Collections.singleton(type)).map(x -> (A) x);
    }

    protected <A extends OWLAxiom> Set<A> getAxioms(AxiomType<A> type) {
        return type == null ? Collections.emptySet() : getAxiomTripleStore(type.getActualClass()).getObjects();
    }

    @SuppressWarnings("unchecked")
    protected <A extends OWLAxiom> OwlObjectTriplesMap<A> getAxiomTripleStore(AxiomType<? extends OWLAxiom> type) {
        return getAxiomTripleStore((Class<A>) type.getActualClass());
    }

    @SuppressWarnings("unchecked")
    protected <A extends OWLAxiom> OwlObjectTriplesMap<A> getAxiomTripleStore(Class<A> type) {
        return (OwlObjectTriplesMap<A>) componentsStore.computeIfAbsent(type,
                c -> new OwlObjectTriplesMap<>(type, AxiomParserProvider.get((Class<A>) c).read(InternalModel.this)));
    }

    @SuppressWarnings("unchecked")
    protected OwlObjectTriplesMap<OWLAnnotation> getAnnotationTripleStore() {
        return (OwlObjectTriplesMap<OWLAnnotation>) componentsStore.computeIfAbsent(OWLAnnotation.class,
                c -> new OwlObjectTriplesMap<>(OWLAnnotation.class, ReadHelper.getObjectAnnotations(getID(), dataFactory()).getWraps()));
    }

    /**
     * Adds an object to the model.
     *
     * @param object either {@link OWLAxiom} or {@link OWLAnnotation}
     * @param store  {@link OwlObjectTriplesMap}
     * @param writer {@link Consumer} to process writing.
     */
    protected <O extends OWLObject> void add(O object, OwlObjectTriplesMap<O> store, Consumer<O> writer) {
        OwlObjectListener<O> listener = store.createListener(object);
        try {
            getGraph().getEventManager().register(listener);
            writer.accept(object);
        } catch (Exception e) {
            throw new OntApiException(String.format("OWLObject: %s, message: %s", object, e.getMessage()), e);
        } finally {
            getGraph().getEventManager().unregister(listener);
        }
    }

    /**
     * Removes an object from the model.
     *
     * @param object either {@link OWLAxiom} or {@link OWLAnnotation}
     * @param store  {@link OwlObjectTriplesMap}
     */
    protected <O extends OWLObject> void remove(O object, OwlObjectTriplesMap<O> store) {
        Set<Triple> triples = store.get(object);
        store.clear(object);
        triples.stream().filter(this::canDelete).forEach(this::delete);
        // todo: clear only those objects which belong to the axiom
        clearObjectsCache();
    }

    protected Set<Class<? extends OWLAxiom>> getAxiomTypes(Triple triple) {
        return componentsStore.values().stream()
                .filter(v -> !Objects.equals(v.type(), OWLAnnotation.class))
                .map(s -> s.get(triple).stream())
                .flatMap(Function.identity())
                .map(OWLAxiom.class::cast)
                .map(a -> a.getAxiomType().getActualClass())
                .collect(Collectors.toSet());
    }

    /**
     * checks if it is possible to delete triple from the graph.
     *
     * @param triple {@link Triple}
     * @return true if there are no axiom which includes this triple, otherwise false.
     */
    protected boolean canDelete(Triple triple) {
        int count = 0;
        for (OwlObjectTriplesMap<? extends OWLObject> store : componentsStore.values()) {
            count += store.get(triple).size();
            if (count > 1) return false;
        }
        return count == 0;
    }

    /**
     * Deletes triple from base graph and clear gena cache for it.
     *
     * @param triple {@link Triple}
     */
    protected void delete(Triple triple) {
        enhNodes.remove(triple.getSubject());
        getBaseGraph().delete(triple);
    }

    @Override
    public Model removeAll() {
        clearCache();
        return super.removeAll();
    }

    protected void clearTemporaryStores() {
        owlCLEStore.clear();
        owlINDStore.clear();
        owlDRGStore.clear();
        owlOPEStore.clear();
        owlNAPStore.clear();
        owlNDPStore.clear();
    }

    protected void clearObjectsCache() {
        objectsStore.clear();
        clearTemporaryStores();
    }

    public void clearCache() {
        componentsStore.clear();
        clearObjectsCache();
    }

    public void clearCache(Triple triple) {
        getAxiomTypes(triple).forEach(componentsStore::remove);
        clearObjectsCache();
    }

    @Override
    public String toString() {
        return String.format("[%s]%s", getClass().getSimpleName(), getID());
    }

    public class OwlObjectTriplesMap<O extends OWLObject> {
        protected Map<O, Set<Triple>> cache;
        protected final Class<O> type;

        public OwlObjectTriplesMap(Class<O> type, Set<InternalObject<O>> set) {
            this.type = type;
            this.cache = set.stream().collect(Collectors.toMap(InternalObject::getObject, InternalObject::getTriples));
        }

        protected Class<O> type() {
            return type;
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

        public void delete(O object, Triple triple) {
            get(object).remove(triple);
        }

        public void clear() {
            cache.clear();
        }

        public void clear(O object) {
            cache.remove(object);
        }

        public Set<O> getObjects() {
            return cache.keySet();
        }

        public OwlObjectListener<O> createListener(O obj) {
            return new OwlObjectListener<>(this, obj);
        }
    }


    /**
     * Listener to monitor the addition and deletion of axioms.
     *
     * @param <O> {@link OWLAxiom} in our case.
     */
    public class OwlObjectListener<O extends OWLObject> extends GraphListenerBase {
        private final OwlObjectTriplesMap<O> store;
        private final O object;

        public OwlObjectListener(OwlObjectTriplesMap<O> store, O object) {
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

    /**
     * direct listener to synchronize caches while working through OWL-API and jena.
     */
    public class DirectListener extends GraphListenerBase {
        private boolean hasObjectListener() {
            return getGraph().getEventManager().hasListeners(OwlObjectListener.class);
        }

        /**
         * if at the moment there is an {@link OwlObjectListener} then it's called from {@link InternalModel#add(OWLAxiom)} => don't clear cache;
         * otherwise it is direct call and cache must be reset to have correct list of axioms.
         *
         * @param t {@link Triple}
         */
        @Override
        protected void addEvent(Triple t) {
            if (hasObjectListener()) return;
            // we don't know which axiom would own this triple, so we clear whole cache.
            clearCache();
        }

        @Override
        protected void deleteEvent(Triple t) {
            if (hasObjectListener()) return;
            clearCache(t);
        }
    }

}
