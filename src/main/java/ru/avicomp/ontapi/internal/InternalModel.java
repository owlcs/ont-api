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
 *
 */

package ru.avicomp.ontapi.internal;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.JenaException;
import org.apache.jena.shared.Lock;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OwlObjects;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.*;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalModel.class);

    // Configuration settings
    private final ConfigProvider.Config config;
    // The main axioms & header annotations cache.
    // Used to work through OWL-API interfaces. The use of jena model methods must clear this cache.
    protected LoadingCache<Class<? extends OWLObject>, InternalObjectTriplesMap<? extends OWLObject>> components =
            Caffeine.newBuilder().softValues().build(this::readObjectTriples);

    // Temporary cache for collecting axioms, should be reset after axioms getting.
    protected final InternalDataFactory cacheDataFactory;
    // OWL objects store to improve performance (working with OWL-API 'signature' methods)
    // Any change in the graph must reset these caches.
    protected LoadingCache<Class<? extends OWLObject>, Set<? extends OWLObject>> objects =
            Caffeine.newBuilder().softValues().build(this::readObjects);

    /**
     * For internal usage only.
     *
     * @param base   {@link Graph}
     * @param config {@link ru.avicomp.ontapi.internal.ConfigProvider.Config}
     */
    public InternalModel(Graph base, ConfigProvider.Config config) {
        super(base, config.loaderConfig().getPersonality());
        this.config = config;
        this.cacheDataFactory = new CacheDataFactory(config);
        //new NoCacheDataFactory(config);
        //new MapDataFactory(config);
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

    public InternalDataFactory getDataFactory() {
        return cacheDataFactory;
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
        return getID().imports().map(cacheDataFactory::toIRI).map(i -> getConfig().dataFactory().getOWLImportsDeclaration(i));
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
     * Returns an owl-entity by iri specified.
     *
     * @param iri {@link IRI}
     * @return List of {@link OWLEntity}s.
     */
    public Stream<OWLEntity> entities(IRI iri) {
        if (iri == null) return Stream.empty();
        OntEntity e = getOntEntity(OntEntity.class, iri.getIRIString());
        List<InternalObject<? extends OWLEntity>> res = new ArrayList<>();
        if (e.canAs(OntClass.class)) {
            res.add(cacheDataFactory.get(e.as(OntClass.class)));
        }
        if (e.canAs(OntDT.class)) {
            res.add(cacheDataFactory.get(e.as(OntDT.class)));
        }
        if (e.canAs(OntNAP.class)) {
            res.add(cacheDataFactory.get(e.as(OntNAP.class)));
        }
        if (e.canAs(OntNDP.class)) {
            res.add(cacheDataFactory.get(e.as(OntNDP.class)));
        }
        if (e.canAs(OntNOP.class)) {
            res.add(cacheDataFactory.get(e.as(OntNOP.class)));
        }
        if (e.canAs(OntIndividual.Named.class)) {
            res.add(cacheDataFactory.get(e.as(OntIndividual.Named.class)));
        }
        return res.stream().map(InternalObject::getObject);
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
     * @param <O>  type of owl-object
     * @return Stream of {@link OWLObject}s.
     */
    @SuppressWarnings("unchecked")
    protected <O extends OWLObject> Stream<O> objects(Class<O> type) {
        return (Stream<O>) Objects.requireNonNull(objects.get(type), "Nothing found. Type: " + type).stream();
    }

    /**
     * Extracts object with specified type from ontology header and axioms.
     *
     * @param type Class type
     * @param <O>  subtype of {@link OWLObject}
     * @return Set of object
     */
    protected <O extends OWLObject> Set<O> readObjects(Class<O> type) {
        return Stream.concat(
                annotations().map(a -> OwlObjects.objects(type, a)).flatMap(Function.identity()),
                axioms().map(a -> OwlObjects.objects(type, a)).flatMap(Function.identity()))
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
        // todo: there is no need to invalidate whole objects cache
        clearObjectsCaches();
    }

    /**
     * Gets all ontology header annotations.
     *
     * @return Stream of {@link OWLAnnotation}
     * @see #axioms(Set)
     */
    @SuppressWarnings("unchecked")
    public Stream<OWLAnnotation> annotations() {
        return getAnnotationTripleStore().objects();
    }

    /**
     * The main method for loading/getting axioms.
     * <p>
     * If {@link Config#parallel()} is true then collecting must not go beyond this method, otherwise it is allowed to be lazy.
     * This is due to the fact that OWL-API uses ReadWriteLock-mechanism everywhere and therefore there is a dangerous
     * of ConcurrentModificationException (in case use of standard java collections api) or deadlocks (in case of concurrent collections),
     * if we allow some processing outside the method.
     * <p>
     *
     * @param types Set of {@link AxiomType}s
     * @return Stream of {@link OWLAxiom}
     * @see #annotations()
     */
    public Stream<OWLAxiom> axioms(Set<AxiomType<? extends OWLAxiom>> types) {
        Stream<OWLAxiom> res = types.stream()
                .map(t -> getAxiomTripleStore(t.getActualClass()))
                .flatMap(InternalObjectTriplesMap::objects)
                .map(OWLAxiom.class::cast);
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
     * Removes axiom from the model.
     * Clears cache for an entity type, if the entity has been belonged to the removed axiom.
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
                OWLAnonymousIndividual.class)
                .filter(entityType -> OwlObjects.objects(entityType, axiom).findAny().isPresent())
                .forEach(type -> objects.invalidate(type));
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
     * @param <A>  type of axiom
     * @return Stream of {@link OWLAxiom}s.
     */
    public <A extends OWLAxiom> Stream<A> axioms(Class<A> view) {
        return axioms(AxiomType.getTypeForClass(OntApiException.notNull(view, "Null axiom class type.")));
    }

    /**
     * Gets axioms by axiom-type.
     *
     * @param type {@link AxiomType}
     * @param <A>  type of axiom
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
     * @return {@link InternalObjectTriplesMap}
     */
    @SuppressWarnings("unchecked")
    protected <A extends OWLAxiom> InternalObjectTriplesMap<A> getAxiomTripleStore(AxiomType<? extends OWLAxiom> type) {
        return getAxiomTripleStore((Class<A>) type.getActualClass());
    }

    /**
     * Auxiliary method.
     * Returns map of axioms by specified OWLAxiom class.
     *
     * @param type Class type of {@link OWLAxiom OWLAxiom}.
     * @param <A>  real type of OWLAxiom
     * @return {@link InternalObjectTriplesMap}
     */
    @SuppressWarnings("unchecked")
    protected <A extends OWLAxiom> InternalObjectTriplesMap<A> getAxiomTripleStore(Class<A> type) {
        return (InternalObjectTriplesMap<A>) components.get(type);
    }


    /**
     * Performs loading OWLObject-Triples container from underlying graph
     *
     * @param type Class type
     * @param <O>  {@link OWLAnnotation} or subtype of {@link OWLAxiom}
     * @return {@link InternalObject}
     */
    @SuppressWarnings("unchecked")
    protected <O extends OWLObject> InternalObjectTriplesMap<O> readObjectTriples(Class<? extends OWLObject> type) {
        InternalObjectTriplesMap<O> res;
        Instant start = null;
        if (LOGGER.isDebugEnabled()) {
            start = Instant.now();
        }
        if (OWLAnnotation.class.equals(type)) {
            res = (InternalObjectTriplesMap<O>) readAnnotationTriples();
        } else {
            res = (InternalObjectTriplesMap<O>) readAxiomTriples((Class<? extends OWLAxiom>) type);
        }
        if (start != null) {
            Duration d = Duration.between(start, Instant.now());
            // commons-lang3 is included in jena-arq (3.6.0)
            LOGGER.debug("[{}]{}:::{}s", getID(),
                    StringUtils.rightPad("[" + type.getSimpleName() + "]", 42), d.get(ChronoUnit.SECONDS) + d.get(ChronoUnit.NANOS) / 1_000_000_000.0);
        }
        return res;
    }

    /**
     * Reads OWLAxioms and triples by specified type.
     *
     * @param type Class type
     * @param <A>  subtype of {@link OWLAxiom}
     * @return {@link InternalObject}
     */
    protected <A extends OWLAxiom> InternalObjectTriplesMap<A> readAxiomTriples(Class<A> type) {
        return new InternalObjectTriplesMap<>(type, AxiomParserProvider.get(type).read(InternalModel.this));
    }

    /**
     * Reads ontology header from underling graph.
     *
     * @return {@link InternalObject}
     */
    protected InternalObjectTriplesMap<OWLAnnotation> readAnnotationTriples() {
        return new InternalObjectTriplesMap<>(OWLAnnotation.class, ReadHelper.getObjectAnnotations(getID(), cacheDataFactory));
    }

    /**
     * Auxiliary method.
     * Returns triples-map of owl-annotations
     *
     * @return {@link InternalObjectTriplesMap} of {@link OWLAnnotation}.
     */
    @SuppressWarnings("unchecked")
    protected InternalObjectTriplesMap<OWLAnnotation> getAnnotationTripleStore() {
        return (InternalObjectTriplesMap<OWLAnnotation>) components.get(OWLAnnotation.class);
    }

    /**
     * Adds an object to the model.
     *
     * @param object either {@link OWLAxiom} or {@link OWLAnnotation}
     * @param store  {@link InternalObjectTriplesMap}
     * @param writer {@link Consumer} to process writing.
     * @param <O>    type of owl-object
     */
    protected <O extends OWLObject> void add(O object, InternalObjectTriplesMap<O> store, Consumer<O> writer) {
        OwlObjectListener<O> listener = createListener(store, object);
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
     * Removes an OWLObject from the model.
     * Note: need also remove associated objects from {@link #objects} cache!
     *
     * @param component either {@link OWLAxiom} or {@link OWLAnnotation}
     * @param map       {@link InternalObjectTriplesMap}
     * @param <O>       the type of OWLObject
     * @see #clearObjectsCaches()
     */
    protected <O extends OWLObject> void remove(O component, InternalObjectTriplesMap<O> map) {
        Set<Triple> triples = map.getTripleSet(component);
        map.remove(component);
        triples.stream().filter(t -> !containsTriple(t)).forEach(this::delete);
    }

    protected boolean containsTriple(Triple triple) {
        return getComponents().stream().anyMatch(c -> c.contains(triple));
    }

    /**
     * Deletes a triple from the base graph and clear jena cache for it.
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
     * Clears cache for the specified triple.
     * This method is called while working with jena model.
     *
     * @param triple {@link Triple}
     */
    protected void clearCacheOnDelete(Triple triple) {
        getComponents().stream()
                .filter(map -> findObjectsToInvalidate(map, triple).findAny().isPresent())
                .forEach(o -> components.invalidate(o.type()));
        // todo: there is no need to invalidate whole objects cache
        clearObjectsCaches();
    }

    protected static <O extends OWLObject> Stream<O> findObjectsToInvalidate(InternalObjectTriplesMap<O> map, Triple triple) {
        return map.objects().filter(o -> {
            try {
                Set<Triple> res = map.get(o); // the triple set is not expected to be null, but just in case there is checking for null also.
                return res == null || res.contains(triple);
            } catch (JenaException j) { // may occur in case previous operation broke object structure
                return true;
            }
        });
    }

    protected Collection<InternalObjectTriplesMap<? extends OWLObject>> getComponents() {
        return components.asMap().values();
    }

    /**
     * Auxiliary method.
     * Invalidates {@link #objects} and {@link #cacheDataFactory} caches.
     */
    protected void clearObjectsCaches() {
        objects.invalidateAll();
        cacheDataFactory.clear();
    }

    /**
     * Invalidates all caches.
     */
    public void clearCache() {
        components.invalidateAll();
        clearObjectsCaches();
    }


    @Override
    public String toString() {
        return String.format("[%s]%s", getClass().getSimpleName(), getID());
    }

    /**
     * Auxiliary object to provide common way for working with {@link OWLObject}s and {@link Triple}s together.
     * Based on {@link InternalObject}, which is a wrapper around OWLObject with the reference to associated triples.
     *
     * @param <O> Component type: a subtype of {@link OWLAxiom} or {@link OWLAnnotation}
     */
    public static class InternalObjectTriplesMap<O extends OWLObject> {
        protected final Class<O> type;
        protected final Set<InternalObject<O>> set;
        protected LoadingCache<O, Set<Triple>> cache = Caffeine.newBuilder().softValues().build(this::loadTripleSet);

        public InternalObjectTriplesMap(Class<O> type, Set<InternalObject<O>> set) {
            this.type = type;
            this.set = set;
        }

        public Class<O> type() {
            return type;
        }

        private Optional<InternalObject<O>> find(O key) {
            // may be long: go over whole collection with concrete component type
            return InternalObject.find(set, key);
        }

        @Nonnull
        private Set<Triple> loadTripleSet(O key) {
            return find(key).map(s -> s.triples()
                    .collect(Collectors.toSet())).orElse(Collections.emptySet());
        }

        /**
         * Adds triple to this map.
         * If there is no triple-container for specified object, or it is empty, or it is in-memory,
         * then a triple will be added to inner set, otherwise appended to existing stream.
         *
         * @param key    OWLObject (axiom or annotation)
         * @param triple {@link Triple}
         */
        public void add(O key, Triple triple) {
            InternalObject<O> res = find(key).map(o -> o.isEmpty() ? new TripleSet<>(o) : o).orElseGet(() -> new TripleSet<>(key));
            set.add(res.add(triple));
            fromCache(key).ifPresent(set -> set.add(triple));
        }

        /**
         * Removes an object-triple pair from this map
         *
         * @param key    OWLObject (axiom or annotation)
         * @param triple {@link Triple}
         */
        public void remove(O key, Triple triple) {
            find(key).ifPresent(o -> set.add(o.delete(triple)));
            fromCache(key).ifPresent(set -> set.remove(triple));
        }

        /**
         * Removes an object and all associated triples
         *
         * @param key OWLObject (axiom or annotation)
         */
        public void remove(O key) {
            cache.invalidate(key);
            set.remove(new TripleSet<>(key));
        }

        protected Optional<Set<Triple>> fromCache(O key) {
            return Optional.ofNullable(cache.getIfPresent(key));
        }

        protected Set<Triple> get(O key) {
            return cache.get(key);
        }

        public Set<Triple> getTripleSet(O key) {
            return Objects.requireNonNull(get(key));
        }

        public boolean contains(Triple triple) {
            return objects().anyMatch(o -> getTripleSet(o).contains(triple));
        }

        public Stream<O> objects() {
            return set.stream().map(InternalObject::getObject);
        }

        /**
         * An {@link InternalObject} which holds triples in memory.
         * Used in caches.
         * Note: it is mutable object while the base is immutable.
         *
         * @param <V>
         */
        private class TripleSet<V extends O> extends InternalObject<V> {
            private final Set<Triple> triples;

            protected TripleSet(V object) { // empty
                this(object, new HashSet<>());
            }

            protected TripleSet(InternalObject<V> object) {
                this(object.getObject(), object.triples().collect(Collectors.toCollection(HashSet::new)));
            }

            private TripleSet(V object, Set<Triple> triples) {
                super(object);
                this.triples = triples;
            }

            @Override
            public Stream<Triple> triples() {
                return triples.stream();
            }

            @Override
            protected boolean isEmpty() {
                return triples.isEmpty();
            }

            @Override
            public InternalObject<V> add(Triple triple) {
                triples.add(triple);
                return this;
            }

            @Override
            public InternalObject<V> delete(Triple triple) {
                triples.remove(triple);
                return this;
            }
        }
    }

    public <O extends OWLObject> OwlObjectListener<O> createListener(InternalObjectTriplesMap<O> map, O obj) {
        return new OwlObjectListener<>(map, obj);
    }

    /**
     * The listener to monitor the addition and deletion of axioms and ontology annotations.
     *
     * @param <O> {@link OWLAxiom} in our case.
     */
    public static class OwlObjectListener<O extends OWLObject> extends GraphListenerBase {
        private final InternalObjectTriplesMap<O> store;
        private final O object;

        public OwlObjectListener(InternalObjectTriplesMap<O> store, O object) {
            this.store = store;
            this.object = object;
        }

        @Override
        protected void addEvent(Triple t) {
            store.add(object, t);
        }

        @Override
        protected void deleteEvent(Triple t) {
            store.remove(object, t);
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
            clearCacheOnDelete(t);
        }
    }

    /**
     * The internal cache holder which is using while reading owl-objects.
     * Currently it is based on caffeine cache since it is used widely by OWL-API.
     */
    public static class CacheDataFactory extends NoCacheDataFactory {
        protected final LoadingCache<OntCE, InternalObject<? extends OWLClassExpression>> classExpressions;
        protected final LoadingCache<OntDR, InternalObject<? extends OWLDataRange>> dataRanges;
        protected final LoadingCache<OntNAP, InternalObject<OWLAnnotationProperty>> annotationProperties;
        protected final LoadingCache<OntNDP, InternalObject<OWLDataProperty>> datatypeProperties;
        protected final LoadingCache<OntOPE, InternalObject<? extends OWLObjectPropertyExpression>> objectProperties;
        protected final LoadingCache<OntIndividual, InternalObject<? extends OWLIndividual>> individuals;
        protected final LoadingCache<Literal, InternalObject<OWLLiteral>> literals;

        public CacheDataFactory(ConfigProvider.Config config) {
            super(config);
            this.classExpressions = build(super::get);
            this.dataRanges = build(super::get);
            this.annotationProperties = build(super::get);
            this.datatypeProperties = build(super::get);
            this.objectProperties = build(super::get);
            this.individuals = build(super::get);
            this.literals = build(super::get);
        }

        @Override
        public void clear() {
            classExpressions.invalidateAll();
            dataRanges.invalidateAll();
            annotationProperties.invalidateAll();
            datatypeProperties.invalidateAll();
            objectProperties.invalidateAll();
            individuals.invalidateAll();
            literals.invalidateAll();
        }

        @Override
        public InternalObject<? extends OWLClassExpression> get(OntCE ce) {
            return classExpressions.get(ce);
        }

        @Override
        public InternalObject<? extends OWLDataRange> get(OntDR dr) {
            return dataRanges.get(dr);
        }

        @Override
        public InternalObject<OWLAnnotationProperty> get(OntNAP nap) {
            return annotationProperties.get(nap);
        }

        @Override
        public InternalObject<OWLDataProperty> get(OntNDP ndp) {
            return datatypeProperties.get(ndp);
        }

        @Override
        public InternalObject<? extends OWLObjectPropertyExpression> get(OntOPE ope) {
            return objectProperties.get(ope);
        }

        @Override
        public InternalObject<OWLLiteral> get(Literal literal) {
            return literals.get(literal);
        }

        @Override
        public InternalObject<? extends OWLIndividual> get(OntIndividual i) {
            return individuals.get(i);
        }

        @Override
        public IRI toIRI(String str) { // use global cache
            return ru.avicomp.ontapi.OntologyManagerImpl.getIRICache().get(str);
        }

        /**
         * Builds synchronized caffeine LoadingCache since
         * <a href='https://github.com/ben-manes/caffeine/issues/209'>a recursive computation is not supported in Java’s maps</a>
         *
         * @param parser {@link CacheLoader}
         * @param <K>    key type
         * @param <V>    value type
         * @return {@link LoadingCache}
         * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryInternalsImpl.java#L63'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternalsImpl#builder(CacheLoader)</a>
         */
        private static <K, V> LoadingCache<K, V> build(CacheLoader<K, V> parser) {
            return Caffeine.newBuilder() // the number from OWL-API DataFactory impl:
                    .maximumSize(2048)
                    .buildAsync(parser).synchronous();
        }

        @Override
        public SimpleMap<OntCE, InternalObject<? extends OWLClassExpression>> classExpressionStore() {
            return new CacheMap<>(classExpressions);
        }

        @Override
        public SimpleMap<OntDR, InternalObject<? extends OWLDataRange>> dataRangeStore() {
            return new CacheMap<>(dataRanges);
        }

        public class CacheMap<K, V> implements SimpleMap<K, V> {
            public CacheMap(LoadingCache<K, V> cache) {
                this.cache = cache;
            }

            private final LoadingCache<K, V> cache;

            @Override
            public V get(K key) {
                return cache.getIfPresent(key);
            }

            @Override
            public void put(K key, V value) {
                cache.put(key, value);
            }
        }
    }

    /**
     * Impl for debug.
     */
    public static class MapDataFactory extends NoCacheDataFactory {
        private Map<OntCE, InternalObject<? extends OWLClassExpression>> classExpressions = new HashMap<>();
        private Map<OntDR, InternalObject<? extends OWLDataRange>> dataRanges = new HashMap<>();
        private Map<OntNAP, InternalObject<OWLAnnotationProperty>> annotationProperties = new HashMap<>();
        private Map<OntNDP, InternalObject<OWLDataProperty>> datatypeProperties = new HashMap<>();
        private Map<OntOPE, InternalObject<? extends OWLObjectPropertyExpression>> objectProperties = new HashMap<>();
        private Map<OntIndividual, InternalObject<? extends OWLIndividual>> individuals = new HashMap<>();
        private Map<Literal, InternalObject<OWLLiteral>> literals = new HashMap<>();

        public MapDataFactory(Config config) {
            super(config);
        }

        @Override
        public void clear() {
            classExpressions.clear();
            dataRanges.clear();
            annotationProperties.clear();
            objectProperties.clear();
            datatypeProperties.clear();
            individuals.clear();
            literals.clear();
        }

        @Override
        public InternalObject<? extends OWLClassExpression> get(OntCE ce) {
            return classExpressions.computeIfAbsent(ce, super::get);
        }

        @Override
        public InternalObject<? extends OWLDataRange> get(OntDR dr) {
            return dataRanges.computeIfAbsent(dr, super::get);
        }

        @Override
        public InternalObject<OWLAnnotationProperty> get(OntNAP nap) {
            return annotationProperties.computeIfAbsent(nap, super::get);
        }

        @Override
        public InternalObject<OWLDataProperty> get(OntNDP ndp) {
            return datatypeProperties.computeIfAbsent(ndp, super::get);
        }

        @Override
        public InternalObject<? extends OWLObjectPropertyExpression> get(OntOPE ope) {
            return objectProperties.computeIfAbsent(ope, super::get);
        }

        @Override
        public InternalObject<OWLLiteral> get(Literal l) {
            return literals.computeIfAbsent(l, super::get);
        }

        @Override
        public InternalObject<? extends OWLIndividual> get(OntIndividual i) {
            return individuals.computeIfAbsent(i, super::get);
        }

        @Override
        public SimpleMap<OntCE, InternalObject<? extends OWLClassExpression>> classExpressionStore() {
            return SimpleMap.fromMap(classExpressions);
        }

        @Override
        public SimpleMap<OntDR, InternalObject<? extends OWLDataRange>> dataRangeStore() {
            return SimpleMap.fromMap(dataRanges);
        }
    }

}
