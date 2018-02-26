/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

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
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.*;

/**
 * Buffer RDF-OWL model.
 * It's our analogy of <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/Internals.java'>uk.ac.manchester.cs.owl.owlapi.Internals</a>.
 * This is a non-serializable(!) {@link OntGraphModel} but with methods to work with the owl-axioms and owl-entities directly.
 * It combines jena(RDF Graph) and owl(structural, OWLAxiom) ways and it is used by the facade model
 * ({@link ru.avicomp.ontapi.OntologyModel}) while reading and writing the structural representation of ontology.
 * <p>
 * TODO: Should it return {@link InternalObject}s, not just naked {@link OWLObject}s?
 * It seems it would be very convenient and could make this class useful not only as part of inner implementation.
 * TODO: to support not-in-memory graphs+structural-view need to add disabling cache option somewhere to configuration and fix read/add/remove operations correspondingly.
 * <p>
 * Created by @szuev on 26.10.2016.
 */
@SuppressWarnings({"WeakerAccess"})
public class InternalModel extends OntGraphModelImpl implements OntGraphModel, ConfigProvider {

    /**
     * The experimental flag which specifies the behaviour on axioms loading.
     * if true then reading of axioms through {@link AxiomTranslator}s occurs in parallel-stream mode (see {@link #axioms(Set)}).
     * As shown by pizza-performance-test it might really help to speed up the initial loading.
     * But it seems for small ontologies using this flag is dangerous and may degrade performance.
     * From this point this flag is always false.
     */
    public static boolean optimizeCollecting = false;
    // Axioms & header annotations store.
    // Used to work through OWL-API interfaces. The use of jena model methods must clear this cache.
    protected Map<Class<? extends OWLObject>, OwlObjectTriplesMap<? extends OWLObject>> componentsStore
            = optimizeCollecting ? new ConcurrentHashMap<>(29, 0.75f, 39) : new HashMap<>();
    // OWL objects store to improve performance (working with OWL-API 'signature' methods)
    // Any change in the graph must reset these caches.
    protected Map<Class<? extends OWLObject>, Set<? extends OWLObject>> objectsStore = new HashMap<>();
    // Temporary stores for collecting axioms, should be reset after axioms getting.
    protected Map<MapType, Map> temporaryObjects = new EnumMap<>(MapType.class);
    // Configuration settings
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
    }

    /**
     * Returns model config instance.
     *
     * @return {@link ConfigProvider.Config}
     */
    @Override
    public ConfigProvider.Config getConfig() {
        return config;
    }

    /**
     * Jena model method.
     * Since in ONT-API we use another kind of lock this method is disabled.
     *
     * @see ru.avicomp.ontapi.jena.ConcurrentGraph
     */
    @Override
    public Lock getLock() {
        throw new OntApiException.Unsupported();
    }

    /**
     * Jena model method to work with embedded lock-mechanism.
     * Disabled since in OWL-API there is a different approach.
     *
     * @see #getLock()
     */
    @Override
    public void enterCriticalSection(boolean requestReadLock) {
        throw new OntApiException.Unsupported();
    }

    /**
     * Jena model method to work with embedded lock-mechanism.
     * Disabled since in OWL-API there is a different approach.
     *
     * @see #getLock()
     */
    @Override
    public void leaveCriticalSection() {
        throw new OntApiException.Unsupported();
    }

    /**
     * Returns import-declarations.
     *
     * @return Stream of {@link OWLImportsDeclaration}s
     */
    public Stream<OWLImportsDeclaration> importDeclarations() {
        return getID().imports().map(IRI::create).map(i -> getConfig().dataFactory().getOWLImportsDeclaration(i));
    }

    /**
     * Returns true if ontology is empty from the semantic point of view.
     *
     * @return true if ontology does not contain any axioms and annotations
     */
    public boolean isOntologyEmpty() {
        return axioms().count() == 0 && annotations().count() == 0;
    }

    /**
     * Auxiliary method, which is used while axioms collecting.
     *
     * @param ce {@link OntCE}
     * @return {@link InternalObject} which wraps {@link OWLClassExpression}
     */
    protected InternalObject<? extends OWLClassExpression> fetchClassExpression(OntCE ce) {
        return fetch(ce, MapType.CE, c -> ReadHelper.getClassExpression(c, getConfig().dataFactory()));
    }

    /**
     * Auxiliary method, which is used while axioms collecting.
     *
     * @param dr {@link OntDR}
     * @return {@link InternalObject} which wraps {@link OWLDataRange}
     */
    protected InternalObject<? extends OWLDataRange> fetchDataRange(OntDR dr) {
        return fetch(dr, MapType.DR, d -> ReadHelper.getDataRange(d, getConfig().dataFactory()));
    }

    /**
     * Auxiliary method, which is used while axioms collecting.
     *
     * @param indi {@link OntIndividual}
     * @return {@link InternalObject} which wraps {@link OWLIndividual}
     */
    protected InternalObject<? extends OWLIndividual> fetchIndividual(OntIndividual indi) {
        return fetch(indi, MapType.I, i -> ReadHelper.getIndividual(i, getConfig().dataFactory()));
    }

    /**
     * Auxiliary method, which is used while axioms collecting.
     *
     * @param nap {@link OntNAP}
     * @return {@link InternalObject} which wraps {@link OWLAnnotationProperty}
     */
    protected InternalObject<OWLAnnotationProperty> fetchAnnotationProperty(OntNAP nap) {
        return fetch(nap, MapType.AP, p -> ReadHelper.getAnnotationProperty(p, getConfig().dataFactory()));
    }

    /**
     * Auxiliary method, which is used while axioms collecting.
     *
     * @param ndp {@link OntNDP}
     * @return {@link InternalObject} which wraps {@link OWLDataProperty}
     */
    protected InternalObject<OWLDataProperty> fetchDataProperty(OntNDP ndp) {
        return fetch(ndp, MapType.DP, p -> ReadHelper.getDataProperty(p, getConfig().dataFactory()));
    }

    /**
     * Auxiliary method, which is used while axioms collecting.
     *
     * @param ope {@link OntOPE}
     * @return {@link InternalObject} which wraps {@link OWLObjectPropertyExpression}
     */
    protected InternalObject<? extends OWLObjectPropertyExpression> fetchObjectProperty(OntOPE ope) {
        return fetch(ope, MapType.OP, p -> ReadHelper.getObjectPropertyExpression(p, getConfig().dataFactory()));
    }

    /**
     * Auxiliary method, which is used while axioms collecting.
     *
     * @param b    {@link OntObject}
     * @param type {@link MapType} enum with types
     * @param func {@link Function} to read, see {@link ReadHelper}
     * @param <A>  {@link OWLObject}
     * @param <B>  {@link OntObject}
     * @return {@link InternalObject} around the {@code &lt;A&gt;}
     * @see #fetchClassExpression(OntCE)
     * @see #fetchDataRange(OntDR)
     * @see #fetchIndividual(OntIndividual)
     * @see #fetchObjectProperty(OntOPE)
     * @see #fetchDataProperty(OntNDP)
     * @see #fetchAnnotationProperty(OntNAP)
     */
    @SuppressWarnings("unchecked")
    protected <A extends OWLObject, B extends OntObject> InternalObject<A> fetch(B b, MapType type, Function<B, InternalObject<A>> func) {
        return (InternalObject<A>) temporaryObjects.computeIfAbsent(type, t -> new HashMap<>()).computeIfAbsent(b, func);
    }

    /**
     * Returns an owl-entity by iri specified.
     *
     * @param iri {@link IRI}
     * @return List of {@link OWLEntity}s.
     */
    public List<OWLEntity> getEntities(IRI iri) {
        if (iri == null) return Collections.emptyList();
        OntEntity e = getOntEntity(OntEntity.class, iri.getIRIString());
        List<OWLEntity> res = new ArrayList<>();
        if (e.canAs(OntClass.class)) {
            res.add(fetchClassExpression(e.as(OntClass.class)).getObject().asOWLClass());
        }
        if (e.canAs(OntDT.class)) {
            res.add(fetchDataRange(e.as(OntDT.class)).getObject().asOWLDatatype());
        }
        if (e.canAs(OntNAP.class)) {
            res.add(fetchAnnotationProperty(e.as(OntNAP.class)).getObject());
        }
        if (e.canAs(OntNDP.class)) {
            res.add(fetchDataProperty(e.as(OntNDP.class)).getObject());
        }
        if (e.canAs(OntNOP.class)) {
            res.add(fetchObjectProperty(e.as(OntNOP.class)).getObject().asOWLObjectProperty());
        }
        if (e.canAs(OntIndividual.Named.class)) {
            res.add(fetchIndividual(e.as(OntIndividual.Named.class)).getObject().asOWLNamedIndividual());
        }
        return res;
    }

    /**
     * Returns named and anonymous individuals.
     *
     * @return Stream of {@link OWLIndividual}s
     */
    public Stream<OWLIndividual> individuals() {
        return objects(OWLIndividual.class);
    }

    /**
     * Returns anonymous individuals.
     *
     * @return Stream of {@link OWLAnonymousIndividual}s
     */
    public Stream<OWLAnonymousIndividual> anonymousIndividuals() {
        return objects(OWLAnonymousIndividual.class);
    }

    /**
     * Returns named individuals.
     *
     * @return Stream of {@link OWLNamedIndividual}s
     */
    public Stream<OWLNamedIndividual> namedIndividuals() {
        return objects(OWLNamedIndividual.class);
    }

    /**
     * Returns owl-classes
     *
     * @return Stream of {@link OWLClass}es.
     */
    public Stream<OWLClass> classes() {
        return objects(OWLClass.class);
    }

    /**
     * Returns data properties.
     *
     * @return Stream of {@link OWLDataProperty}s
     */
    public Stream<OWLDataProperty> dataProperties() {
        return objects(OWLDataProperty.class);
    }

    /**
     * Returns object properties.
     *
     * @return Stream of {@link OWLObjectProperty}s
     */
    public Stream<OWLObjectProperty> objectProperties() {
        return objects(OWLObjectProperty.class);
    }

    /**
     * Returns annotation properties.
     *
     * @return Stream of {@link OWLAnnotationProperty}s
     */
    public Stream<OWLAnnotationProperty> annotationProperties() {
        return objects(OWLAnnotationProperty.class);
    }

    /**
     * Returns named data ranges (datatypes)
     *
     * @return Stream of {@link OWLDatatype}s.
     */
    public Stream<OWLDatatype> datatypes() {
        return objects(OWLDatatype.class);
    }

    /**
     * Gets owl-objects from axioms and annotations.
     *
     * @param type Class type of owl-object.
     * @param <O> type of owl-object
     * @return Stream of {@link OWLObject}s.
     */
    @SuppressWarnings("unchecked")
    protected <O extends OWLObject> Stream<O> objects(Class<O> type) {
        return (Stream<O>) objectsStore.computeIfAbsent(type, this::getObjects).stream();
    }

    protected <O extends OWLObject> Set<O> getObjects(Class<O> type) {
        return Stream.concat(
                annotations().map(annotation -> OwlObjects.objects(type, annotation)).flatMap(Function.identity()),
                axioms().map(axiom -> OwlObjects.objects(type, axiom)).flatMap(Function.identity()))
                .collect(Collectors.toSet());
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
        clearObjectsCache();
    }

    /**
     * Gets all ontology header annotations.
     *
     * @return Stream of {@link OWLAnnotation}
     * @see #axioms(Set)
     */
    @SuppressWarnings("unchecked")
    public Stream<OWLAnnotation> annotations() {
        return getAnnotationTripleStore().getObjects().stream();
    }

    /**
     * The main method for loading/getting axioms.
     * <p>
     * If {@link Config#parallel()} is true then collecting must not go beyond this method, otherwise it is allowed to be lazy.
     * This is due to the fact that OWL-API uses ReadWriteLock-mechanism everywhere and therefore there is a dangerous
     * of ConcurrentModificationException (in case use of standard java collections api) or deadlocks (in case of concurrent collections),
     * if we allow some processing outside the method.
     * <p>
     * Collecting performs through parallel-stream in the following cases:
     * - {@link #optimizeCollecting} is true
     * - {@link #componentsStore} is empty
     * - the set of {@link AxiomType}s contains more then one element.
     *
     * @param types Set of {@link AxiomType}s
     * @return Stream of {@link OWLAxiom}
     * @see #annotations()
     */
    public Stream<OWLAxiom> axioms(Set<AxiomType<? extends OWLAxiom>> types) {
        Stream<OWLAxiom> res;
        if (optimizeCollecting && componentsStore.isEmpty() && types.size() > 1) {
            types.parallelStream().forEach(type -> getAxiomTripleStore(type.getActualClass()));
            res = componentsStore.values().stream()
                    .filter(v -> !Objects.equals(v.type(), OWLAnnotation.class))
                    .map(OwlObjectTriplesMap::getObjects)
                    .map(Collection::stream)
                    .flatMap(Function.identity())
                    .map(OWLAxiom.class::cast);
        } else {
            res = types.stream()
                    .map(t -> getAxiomTripleStore(t.getActualClass()).getObjects())
                    .map(Collection::stream)
                    .flatMap(Function.identity())
                    .map(OWLAxiom.class::cast);
        }
        return getConfig().parallel() ? res.collect(Collectors.toList()).stream() : res;
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
        Stream.of(OWLClass.class,
                OWLDatatype.class,
                OWLAnnotationProperty.class,
                OWLDataProperty.class,
                OWLObjectProperty.class,
                OWLNamedIndividual.class,
                OWLAnonymousIndividual.class).filter(c -> OwlObjects.objects(c, axiom).anyMatch(p -> true))
                .forEach(type -> objectsStore.remove(type));
    }

    /**
     * Gets all axioms
     *
     * @return Stream of {@link OWLAxiom}s.
     */
    public Stream<OWLAxiom> axioms() {
        return axioms(AxiomType.AXIOM_TYPES);
    }

    /**
     * Gets axioms by class-type.
     *
     * @param view Class
     * @param <A> type of axiom
     * @return Stream of {@link OWLAxiom}s.
     */
    public <A extends OWLAxiom> Stream<A> axioms(Class<A> view) {
        return axioms(AxiomType.getTypeForClass(OntApiException.notNull(view, "Null axiom class type.")));
    }

    /**
     * Gets axioms by axiom-type.
     *
     * @param type {@link AxiomType}
     * @param <A> type of axiom
     * @return Stream of {@link OWLAxiom}s.
     */
    @SuppressWarnings("unchecked")
    public <A extends OWLAxiom> Stream<A> axioms(AxiomType<A> type) {
        return axioms(Collections.singleton(type)).map(x -> (A) x);
    }

    /**
     * Auxiliary method.
     * Returns map of axioms by specified OWLAxiom type.
     *
     * @param type {@link AxiomType}
     * @param <A>  {@link OWLAxiom}
     * @return {@link OwlObjectTriplesMap}
     */
    @SuppressWarnings("unchecked")
    protected <A extends OWLAxiom> OwlObjectTriplesMap<A> getAxiomTripleStore(AxiomType<? extends OWLAxiom> type) {
        return getAxiomTripleStore((Class<A>) type.getActualClass());
    }

    /**
     * Auxiliary method.
     * Returns map of axioms by specified OWLAxiom class.
     *
     * @param type Class type of {@link OWLAxiom OWLAxiom}.
     * @param <A>  real type of OWLAxiom
     * @return {@link OwlObjectTriplesMap}
     */
    @SuppressWarnings("unchecked")
    protected <A extends OWLAxiom> OwlObjectTriplesMap<A> getAxiomTripleStore(Class<A> type) {
        return (OwlObjectTriplesMap<A>) componentsStore.computeIfAbsent(type, t -> readAxiomTriples((Class<A>) t));
    }

    protected <A extends OWLAxiom> OwlObjectTriplesMap<A> readAxiomTriples(Class<A> type) {
        return new OwlObjectTriplesMap<>(type, AxiomParserProvider.get(type).read(InternalModel.this));
    }

    /**
     * Auxiliary method.
     * Returns triples-map of owl-annotations
     *
     * @return {@link OwlObjectTriplesMap} of {@link OWLAnnotation}.
     */
    @SuppressWarnings("unchecked")
    protected OwlObjectTriplesMap<OWLAnnotation> getAnnotationTripleStore() {
        return (OwlObjectTriplesMap<OWLAnnotation>) componentsStore.computeIfAbsent(OWLAnnotation.class, t -> readAnnotationTriples());
    }

    protected OwlObjectTriplesMap<OWLAnnotation> readAnnotationTriples() {
        return new OwlObjectTriplesMap<>(OWLAnnotation.class, ReadHelper.getObjectAnnotations(getID(), getConfig().dataFactory()).getWraps());
    }

    /**
     * Adds an object to the model.
     *
     * @param object either {@link OWLAxiom} or {@link OWLAnnotation}
     * @param store  {@link OwlObjectTriplesMap}
     * @param writer {@link Consumer} to process writing.
     * @param <O> type of owl-object
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
     * Note: remove associated objects from {@link #objectsStore}!
     *
     * @param object either {@link OWLAxiom} or {@link OWLAnnotation}
     * @param store  {@link OwlObjectTriplesMap}
     * @param <O> type of owl-object
     * @see #clearObjectsCache()
     */
    protected <O extends OWLObject> void remove(O object, OwlObjectTriplesMap<O> store) {
        Set<Triple> triples = store.get(object);
        store.clear(object);
        triples.stream().filter(this::canDelete).forEach(this::delete);
    }

    /**
     * Auxiliary method.
     * Returns Set of OWLAxiom types by specified triple. The set could be empty if the triple does not belong to any axioms.
     *
     * @param triple {@link Triple}
     * @return Set of OWLAxioms types (classes)
     */
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
     * Checks if it is possible to delete triple from the graph.
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

    /**
     * The overridden base method.
     * Makes this ontology empty.
     *
     * @return {@link Model}
     */
    @Override
    public Model removeAll() {
        clearCache();
        return super.removeAll();
    }

    /**
     * Auxiliary. Clears {@link #objectsStore} and {@link #temporaryObjects}
     */
    protected void clearObjectsCache() {
        objectsStore.clear();
        temporaryObjects.clear();
    }

    /**
     * Clears a whole cache.
     */
    public void clearCache() {
        componentsStore.clear();
        clearObjectsCache();
    }

    /**
     * Clears cache for the specified triple
     *
     * @param triple {@link Triple}
     */
    public void clearCache(Triple triple) {
        getAxiomTypes(triple).forEach(componentsStore::remove);
        clearObjectsCache();
    }

    @Override
    public String toString() {
        return String.format("[%s]%s", getClass().getSimpleName(), getID());
    }

    /**
     * The internal enum which is using while reading owl-objects.
     *
     * @see #fetchClassExpression(OntCE)
     * @see #fetchDataRange(OntDR)
     * @see #fetchIndividual(OntIndividual)
     * @see #fetchObjectProperty(OntOPE)
     * @see #fetchDataProperty(OntNDP)
     * @see #fetchAnnotationProperty(OntNAP)
     */
    protected enum MapType {
        CE, DR, AP, DP, OP, I
    }

    /**
     * Auxiliary object to provide common way for working with {@link OWLObject}s and {@link Triple}s together.
     *
     * @param <O> {@link OWLAxiom} or {@link OWLAnnotation} (currently).
     */
    public class OwlObjectTriplesMap<O extends OWLObject> {
        protected final Class<O> type;
        protected Map<O, Set<Triple>> cache;

        public OwlObjectTriplesMap(Class<O> type, Set<InternalObject<O>> set) {
            this.type = type;
            this.cache = set.stream().collect(Collectors.toMap(InternalObject::getObject, InternalObject::getTriples));
        }

        public Class<O> type() {
            return type;
        }

        public void add(O object, Triple triple) {
            cache.computeIfAbsent(object, e -> new HashSet<>()).add(triple);
        }

        public Set<Triple> get(O object) {
            return cache.getOrDefault(object, Collections.emptySet());
        }

        public Set<O> get(Triple triple) {
            return cache.entrySet().stream().filter(e -> e.getValue().contains(triple)).map(Map.Entry::getKey).collect(Collectors.toSet());
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
     * The listener to monitor the addition and deletion of axioms and ontology annotations.
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
     * The direct listener to synchronize caches while working through OWL-API and jena at the same time.
     */
    public class DirectListener extends GraphListenerBase {
        private boolean hasObjectListener() {
            return getGraph().getEventManager().hasListeners(OwlObjectListener.class);
        }

        /**
         * if at the moment there is an {@link OwlObjectListener} then it's called from {@link InternalModel#add(OWLAxiom)} =&gt; don't clear cache;
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
