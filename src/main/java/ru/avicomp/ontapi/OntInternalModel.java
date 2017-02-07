package ru.avicomp.ontapi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.translators.AxiomParserProvider;
import ru.avicomp.ontapi.translators.OWL2RDFHelper;
import ru.avicomp.ontapi.translators.RDF2OWLHelper;
import uk.ac.manchester.cs.owl.owlapi.OWLImportsDeclarationImpl;

/**
 * New strategy here. Buffer RDF-OWL model.
 * This is {@link OntGraphModel} but with methods to work with the axioms and entities.
 * It combines jena(RDF Graph) and owl(structural, OWLAxiom) ways and
 * it is used to read and write structural info by {@link ru.avicomp.ontapi.OntologyModel}.
 * <p>
 * Created by @szuev on 26.10.2016.
 */
public class OntInternalModel extends OntGraphModelImpl implements OntGraphModel, Serializable {

    private OWLOntologyID anonOntologyID;

    private static final String DEFAULT_SERIALIZATION_FORMAT = OntFormat.TURTLE.getID();

    // axioms store
    private transient Map<Class<? extends OWLAxiom>, TripleStore<? extends OWLAxiom>> axiomsCache = new HashMap<>();
    // "cache" to improve performance:
    private transient Map<Class<?>, Set<?>> objectsCache = new HashMap<>();

    public OntInternalModel(Graph base) {
        super(base);
        getGraph().getEventManager().register(new DirectListener());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <O extends OntObject> Stream<O> ontObjects(Class<O> type) {
        return (Stream<O>) objectsCache.computeIfAbsent(type, c -> OntInternalModel.super.ontObjects(type).collect(Collectors.toSet())).stream();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Stream<OntStatement> statements() {
        return (Stream<OntStatement>) objectsCache.computeIfAbsent(OntStatement.class, c -> OntInternalModel.super.statements().collect(Collectors.toSet())).stream();
    }

    public OWLOntologyID getOwlID() {
        OntID id = getID();
        if (id.isAnon()) {
            return anonOntologyID == null ? anonOntologyID = new OWLOntologyID() : anonOntologyID;
        }
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
            anonOntologyID = id;
            return;
        }
        IRI iri = id.getOntologyIRI().orElse(null);
        IRI versionIRI = id.getVersionIRI().orElse(null);
        setID(iri == null ? null : iri.getIRIString()).setVersionIRI(versionIRI == null ? null : versionIRI.getIRIString());
    }

    public Stream<OWLImportsDeclaration> importDeclarations() {
        // todo: should be declared in graph owl:imports, but with restriction:
        // they should comply with documents(not ontologies) in manager.
        // see description of base class OWLOntology#importsDeclarations():
        return getID().imports().map(IRI::create).map(OWLImportsDeclarationImpl::new);
    }

    public boolean isOntologyEmpty() {
        return getAxioms().isEmpty() && getAnnotations().isEmpty();
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

    public List<OWLEntity> getEntities(IRI iri) {
        if (iri == null) return Collections.emptyList();
        OntEntity e = getOntEntity(OntEntity.class, iri.getIRIString());
        List<OWLEntity> res = new ArrayList<>();
        if (e.canAs(OntClass.class)) {
            res.add(RDF2OWLHelper.getClassExpression(e.as(OntClass.class)).asOWLClass());
        }
        if (e.canAs(OntDT.class)) {
            res.add(RDF2OWLHelper.getDatatype(e.as(OntDT.class)));
        }
        if (e.canAs(OntNAP.class)) {
            res.add(RDF2OWLHelper.getAnnotationProperty(e.as(OntNAP.class)));
        }
        if (e.canAs(OntNDP.class)) {
            res.add(RDF2OWLHelper.getDataProperty(e.as(OntNDP.class)));
        }
        if (e.canAs(OntNOP.class)) {
            res.add(RDF2OWLHelper.getObjectProperty(e.as(OntNOP.class)).asOWLObjectProperty());
        }
        if (e.canAs(OntIndividual.Named.class)) {
            res.add(RDF2OWLHelper.getIndividual(e.as(OntIndividual.Named.class)).asOWLNamedIndividual());
        }
        return res;
    }

    public Stream<OntEntity> ambiguousEntities(boolean withImports) {
        Set<Class<? extends OntEntity>> types = Stream.of(OntClass.class, OntDT.class, OntNAP.class, OntNDP.class, OntNOP.class, OntIndividual.Named.class).collect(Collectors.toSet());
        return ontEntities().filter(e -> withImports || e.isLocal()).filter(e -> types.stream()
                .filter(view -> e.canAs(view) && (withImports || e.as(view).isLocal())).count() > 1);
    }

    public static <T extends OWLObject> Stream<T> parseComponents(Class<T> view, HasComponents structure) {
        return structure.componentsWithoutAnnotations().map(o -> toStream(view, o)).flatMap(Function.identity());
    }

    public static <T extends OWLObject> Stream<T> parseAnnotations(Class<T> view, HasAnnotations structure) {
        return structure.annotations().map(o -> toStream(view, o)).flatMap(Function.identity());
    }

    public static <R extends OWLObject, S extends HasAnnotations & HasComponents> Stream<R> objects(Class<R> view, S container) {
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
        Stream<?> stream = null;
        if (o instanceof Stream) {
            stream = ((Stream<?>) o);
        }
        if (o instanceof Collection) {
            stream = ((Collection<?>) o).stream();
        }
        if (stream != null) {
            return stream.map(_o -> toStream(view, _o)).flatMap(Function.identity());
        }
        return Stream.empty();
    }

    @SuppressWarnings("unchecked")
    public <E extends OWLObject> Stream<E> objects(Class<E> view) {
        return (Stream<E>) objectsCache.computeIfAbsent(view, c ->
                Stream.concat(annotations().map(annotation -> objects(view, annotation)).flatMap(Function.identity()),
                        axioms().map(axiom -> objects(view, axiom)).flatMap(Function.identity())).collect(Collectors.toSet())).stream();
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
            RDF2OWLHelper.TripleSet<OWLAnnotation> set = RDF2OWLHelper.getAnnotations(getID())
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

    public Set<OWLAxiom> getAxioms(Set<AxiomType<? extends OWLAxiom>> types) {
        return axioms(types).collect(Collectors.toSet());
    }

    public Stream<OWLAxiom> axioms() {
        return axioms(AxiomType.AXIOM_TYPES);
    }

    public Stream<OWLAxiom> axioms(Set<AxiomType<? extends OWLAxiom>> types) {
        return //StreamSupport.stream(types.spliterator(), axiomsCache.isEmpty())
                types.stream()
                .map(this::getAxioms)
                .map(Collection::stream).flatMap(Function.identity());
    }

    public <A extends OWLAxiom> Stream<A> axioms(Class<A> view) {
        return getAxioms(view).stream();
    }

    public Stream<OWLClassAxiom> classAxioms() {
        return Stream.of(OWLDisjointClassesAxiom.class,
                OWLDisjointUnionAxiom.class,
                OWLEquivalentClassesAxiom.class,
                OWLSubClassOfAxiom.class)
                .map(this::getAxioms).map(Collection::stream).flatMap(Function.identity());
    }

    public Stream<OWLObjectPropertyAxiom> objectPropertyAxioms() {
        return Stream.of(
                OWLSubObjectPropertyOfAxiom.class,
                OWLObjectPropertyDomainAxiom.class,
                OWLObjectPropertyRangeAxiom.class,

                OWLDisjointObjectPropertiesAxiom.class,
                OWLSubPropertyChainOfAxiom.class,
                OWLEquivalentObjectPropertiesAxiom.class,
                OWLInverseObjectPropertiesAxiom.class,

                OWLTransitiveObjectPropertyAxiom.class,
                OWLIrreflexiveObjectPropertyAxiom.class,
                OWLReflexiveObjectPropertyAxiom.class,
                OWLSymmetricObjectPropertyAxiom.class,
                OWLFunctionalObjectPropertyAxiom.class,
                OWLInverseFunctionalObjectPropertyAxiom.class,
                OWLAsymmetricObjectPropertyAxiom.class
        ).map(this::getAxioms).map(Collection::stream).flatMap(Function.identity());
    }

    public Stream<OWLDataPropertyAxiom> dataPropertyAxioms() {
        return Stream.of(
                OWLDataPropertyDomainAxiom.class,
                OWLDataPropertyRangeAxiom.class,

                OWLDisjointDataPropertiesAxiom.class,
                OWLSubDataPropertyOfAxiom.class,
                OWLEquivalentDataPropertiesAxiom.class,

                OWLFunctionalDataPropertyAxiom.class
        ).map(this::getAxioms).map(Collection::stream).flatMap(Function.identity());
    }

    public Stream<OWLIndividualAxiom> individualAxioms() {
        return Stream.of(
                OWLClassAssertionAxiom.class,
                OWLObjectPropertyAssertionAxiom.class,
                OWLDataPropertyAssertionAxiom.class,

                OWLNegativeObjectPropertyAssertionAxiom.class,
                OWLNegativeDataPropertyAssertionAxiom.class,

                OWLSameIndividualAxiom.class,
                OWLDifferentIndividualsAxiom.class
        ).map(this::getAxioms).map(Collection::stream).flatMap(Function.identity());
    }

    public Stream<OWLLogicalAxiom> logicalAxioms() {
        return axioms(AxiomType.AXIOM_TYPES.stream().filter(AxiomType::isLogical).collect(Collectors.toSet())).map(OWLLogicalAxiom.class::cast);
    }

    public <A extends OWLAxiom> Set<A> getAxioms(Class<A> view) {
        return getAxiomTripleStore(view).getObjects();
    }

    public <A extends OWLAxiom> Set<A> getAxioms(AxiomType<A> type) {
        return type == null ? Collections.emptySet() : getAxiomTripleStore(type.getActualClass()).getObjects();
    }

    public <A extends OWLAxiom> void add(A axiom) {
        ObjectListener<OWLAxiom> listener = getAxiomTripleStore(axiom.getAxiomType()).createListener(axiom);
        try {
            getGraph().getEventManager().register(listener);
            AxiomParserProvider.get(axiom.getAxiomType()).write(axiom, this);
        } catch (Exception e) {
            throw new OntApiException(String.format("Axiom: %s, message: %s", axiom, e.getMessage()), e);
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
        return axiomsCache.values().stream()
                .map(s -> s.get(triple).stream())
                .flatMap(Function.identity())
                .map(a -> a.getAxiomType().getActualClass())
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
        for (TripleStore<? extends OWLAxiom> store : axiomsCache.values()) {
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
        return (TripleStore<A>) axiomsCache.computeIfAbsent(type, c -> new TripleStore<>(AxiomParserProvider.get(type).read(this)));
    }

    @Override
    public Model removeAll() {
        clearCache();
        return super.removeAll();
    }

    public void clearCache() {
        axiomsCache.clear();
        objectsCache.clear();
    }

    public void resetCache() {
        objectsCache = new HashMap<>();
        axiomsCache = new HashMap<>();
    }

    public void clearCache(Triple triple) {
        getAxiomTypes(triple).forEach(axiomsCache::remove);
        objectsCache.clear();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject(); // todo: handle situation when there are imported sub-graphs
        read(stream, null, DEFAULT_SERIALIZATION_FORMAT);
        resetCache();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject(); // todo: handle situation when there are imported sub-graphs
        write(stream, DEFAULT_SERIALIZATION_FORMAT, null);
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
            clearCache(t);
        }
    }

}
