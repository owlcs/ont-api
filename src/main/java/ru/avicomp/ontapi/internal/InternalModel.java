/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
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

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphListener;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.JenaException;
import org.apache.jena.shared.Lock;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntologyID;
import ru.avicomp.ontapi.OwlObjects;
import ru.avicomp.ontapi.internal.axioms.AbstractNaryTranslator;
import ru.avicomp.ontapi.internal.axioms.DeclarationTranslator;
import ru.avicomp.ontapi.internal.axioms.EquivalentClassesTranslator;
import ru.avicomp.ontapi.internal.axioms.SubClassOfTranslator;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.RWLockedGraph;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Buffer Graph OWL model, which supports both listing OWL-API objects (OWL Axioms, Entities and Annotations)
 * and Jena interfaces (through the {@link OntGraphModel} view of RDF Graph).
 * <p>
 * It is an analogue of <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/Internals.java'>uk.ac.manchester.cs.owl.owlapi.Internals</a>.
 * This model is used by the facade model (i.e. by {@link ru.avicomp.ontapi.OntologyModel}) while reading and writing
 * the structural (axiomatic) representation of ontology.
 * <p>
 * Notice that this model is a non-serializable (while everything in OWL-API is {@link java.io.Serializable}),
 * since it is part of ONT-API internal implementation.
 * <p>
 * Unlike native OWL-API (see {@code owl-api-impl}) implementation,
 * this model does not store all possible {@link OWLObject OWL object}s in memory.
 * Instead, there are several caches.
 * The main cache is divided into buckets by OWL objects types (39 OWL Axioms types plus Header Annotations).
 * And each of these buckets is indivisible.
 * It means that only the whole bucket can be loaded or invalidated, not just part of it.
 * This is the major restriction of the current implementation:
 * if you have such a large ontology that can not even be partially placed in memory
 * (that is, the set of axioms of any type, but with the exclusion of their components, exceeds the memory limit),
 * then this solution won't work.
 * To work with such large ontologies, it is recommended to use {@link AxiomTranslator Axiom Translator}s directly
 * (see {@link AxiomParserProvider#get(AxiomType)}).
 * <p>
 * TODO: Should it return {@link ONTObjectImpl}s, not just naked {@link OWLObject}s?
 * It seems it would be more convenient and could make this class useful not only as part of inner implementation.
 * <p>
 * Created by @szuev on 26.10.2016.
 */
@SuppressWarnings({"WeakerAccess"})
public class InternalModel extends OntGraphModelImpl implements OntGraphModel, HasOntologyID {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalModel.class);
    /**
     * A factory to produce fresh instances of {@link InternalObjectFactory object factory},
     * that is responsible for mapping ONT Jena Objects to OWL-API objects.
     * The object factory may be cache objects, it depends on {@link InternalConfig} settings.
     * So it is need to obtain new object factory instance if the settings have been changed.
     */
    protected final Supplier<InternalObjectFactory> factory;
    /**
     * An object factory cache that is used while collecting axioms, may be reset to release memory.
     * Any change in the base graph must reset this cache.
     * Designed as a {@link java.lang.ref.SoftReference}
     * since it is mostly need only to optimize reading operations and may contain huge amount of objects.
     */
    protected final InternalCache.Loading<InternalModel, InternalObjectFactory> objectFactoryCache;
    /**
     * A model for axiom/object's search optimizations, containing {@link Node node}s cache.
     * Any change in the base graph must also reset this cache.
     * Designed as a {@link java.lang.ref.SoftReference}
     * since it is mostly need only to optimize reading operations and may contain huge amount of objects.
     */
    protected final InternalCache.Loading<InternalModel, SearchModel> searchModelCache;
    /**
     * OWL objects cache (to work with OWL-API 'signature' methods).
     * Any change in the graph must also reset this cache.
     */
    protected final InternalCache.Loading<Class<? extends OWLObject>, Set<? extends OWLObject>> objects;
    /**
     * Configuration settings to control behaviour.
     * This object can be modified externally.
     */
    private final InternalConfig config;
    /**
     * A {@link InternalConfig} snapshot-config, that should be used while any R/W operations through OWL-API interface.
     * It should be reset on {@link #clearCache()}.
     */
    protected volatile InternalConfig.Snapshot snapshot;
    /**
     * Ontology ID cache.
     */
    protected volatile OntologyID cachedID;
    /**
     * The {@link OWLAxiom}'s cache
     * as immutable {@code Map} with {@link AxiomType} as keys and {@link ObjectTriplesMap} as values.
     */
    protected volatile Map<AxiomKey, ObjectTriplesMap<OWLAxiom>> axioms;
    /**
     * Ontology header {@link OWLAnnotation}s cache.
     */
    protected volatile ObjectTriplesMap<OWLAnnotation> header;
    /**
     * A collection of reserved uri-{@link Node}s, that cannot be OWL-entities.
     * Used to speedup iteration in some cases (e.g. for class assertions).
     */
    protected final InternalCache<Class<? extends OntObject>, Set<Node>> systemResources;

    /**
     * Constructs an instance.
     * For internal usage only.
     *
     * @param base        {@link Graph}, not {@code null}
     * @param personality {@link OntPersonality}, not {@code null}
     * @param factory     {@link Supplier} to create {@link InternalObjectFactory} instances, not {@code null}
     * @param config      {@link InternalConfig}, not {@code null}
     */
    public InternalModel(Graph base,
                         OntPersonality personality,
                         Supplier<InternalObjectFactory> factory,
                         InternalConfig config) {
        super(base, personality);
        this.factory = Objects.requireNonNull(factory);
        this.config = Objects.requireNonNull(config);
        this.objectFactoryCache = InternalCache.createSoft(x -> factory.get(), config.parallel());
        this.searchModelCache = InternalCache.createSoft(x -> createSearchModel(), config.parallel());
        this.objects = InternalCache.createSoft(this::readOWLObjects, config.parallel());
        this.systemResources = InternalCache.createSoft(config.parallel());
        getGraph().getEventManager().register(new DirectListener());
    }

    /**
     * Gets the {@link OWLOntologyID OWL Ontology ID} from the model.
     *
     * @return {@link OntologyID}
     * @see #getID()
     */
    @Override
    public OntologyID getOntologyID() {
        // believe the last condition (which is very fast) justifies having a cache
        if (cachedID != null && getBaseGraph().contains(cachedID.asNode(), RDF.Nodes.type, OWL.Ontology.asNode())) {
            return cachedID;
        }
        return cachedID = new OntologyID(getID());
    }

    /**
     * Sets the {@link OWLOntologyID OWL Ontology ID} to the model.
     *
     * @param id {@link OWLOntologyID}
     * @throws IllegalArgumentException in case the given id is broken
     * @see #setID(String)
     */
    public void setOntologyID(OWLOntologyID id) throws IllegalArgumentException {
        this.cachedID = null;
        if (Objects.requireNonNull(id, "Null id").isAnonymous()) {
            OntID res;
            if (id instanceof OntologyID) {
                res = getNodeAs(createOntologyID(this, ((OntologyID) id).asNode()).asNode(), OntID.class);
            } else {
                res = setID(null);
            }
            res.setVersionIRI(null);
        } else {
            setID(id.getOntologyIRI().map(IRI::getIRIString).orElseThrow(IllegalArgumentException::new))
                    .setVersionIRI(id.getVersionIRI().map(IRI::getIRIString).orElse(null));
        }
        if (id instanceof OntologyID) {
            this.cachedID = (OntologyID) id;
        }
    }

    /**
     * Returns the model's {@link InternalConfig config}.
     *
     * @return {@link InternalConfig}
     */
    public InternalConfig getConfig() {
        return config;
    }

    /**
     * Returns the model;s {@link InternalConfig} snapshot instance,
     * which is immutable object.
     *
     * @return {@link InternalConfig.Snapshot}
     */
    protected InternalConfig.Snapshot getSnapshotConfig() {
        return snapshot == null ? snapshot = config.snapshot() : snapshot;
    }

    /**
     * Returns the {@code InternalDataFactory}, a helper (possibly, with cache) to read OWL-API objects.
     *
     * @return {@link InternalObjectFactory}
     */
    public InternalObjectFactory getObjectFactory() {
        if (getSnapshotConfig().useLoadObjectsCache()) {
            return objectFactoryCache.get(this);
        }
        return factory.get();
    }

    /**
     * Returns an {@link OntGraphModelImpl} version with search optimizations.
     * The return model must be used only to collect OWL-API stuff:
     * {@link OWLAxiom OWL Axiom}s and {@link OWLObject OWL Objects}.
     * Retrieving jena {@link OntObject Ont Object}s and {@link OntStatement Ont Statements} must be performed
     * through the main ({@link InternalModel this}) interface.
     *
     * @return {@link OntGraphModelImpl} with search optimizations
     */
    public OntGraphModelImpl getSearchModel() {
        if (getSnapshotConfig().useLoadNodesCache()) {
            return searchModelCache.get(this);
        }
        return this;
    }

    /**
     * Creates a {@link SearchModel}, which is used as optimization while reading OWL-API objects.
     * It contains nodes cache inside, and may take up a lot of memory.
     *
     * @return {@link SearchModel}
     */
    protected SearchModel createSearchModel() {
        return new SearchModel(getGraph(), getOntPersonality(), getSnapshotConfig());
    }

    /**
     * Gets an axioms map store.
     *
     * @return {@code Map} with {@link AxiomKey}-keys and {@link ObjectTriplesMap}-values.
     */
    public Map<AxiomKey, ObjectTriplesMap<OWLAxiom>> getAxioms() {
        return axioms == null ? axioms = createAxiomsCacheMap() : axioms;
    }

    /**
     * Gets an ontology header store.
     *
     * @return {@link ObjectTriplesMap}
     */
    public ObjectTriplesMap<OWLAnnotation> getHeader() {
        return header == null ? header = createHeaderTriplesMap() : header;
    }

    @Override
    public <N extends RDFNode> N fetchNodeAs(Node node, Class<N> type) {
        try {
            return super.fetchNodeAs(node, type);
        } catch (OntJenaException e) {
            return SearchModel.handleFetchNodeAsException(e, node, type, this, getSnapshotConfig());
        }
    }

    @Override
    public Set<Node> getSystemResources(Class<? extends OntObject> type) {
        return systemResources.get(type, x -> super.getSystemResources(type));
    }

    /**
     * Jena model method.
     * Since in ONT-API we use another kind of lock this method is disabled (i.e. R/W Lock inside manager).
     *
     * @see RWLockedGraph
     */
    @Override
    public Lock getLock() {
        throw new OntApiException.Unsupported();
    }

    /**
     * Jena model method to work with embedded lock-mechanism.
     * Disabled since in OWL-API there is a different approach (i.e. R/W Lock inside manager).
     *
     * @see #getLock()
     */
    @Override
    public void enterCriticalSection(boolean requestReadLock) {
        throw new OntApiException.Unsupported();
    }

    /**
     * Jena model method to work with embedded lock-mechanism.
     * Disabled since in the OWL-API there is a different approach (i.e. R/W Lock inside manager).
     *
     * @see #getLock()
     */
    @Override
    public void leaveCriticalSection() {
        throw new OntApiException.Unsupported();
    }

    /**
     * Lists all owl import-declarations.
     *
     * @return Stream of {@link OWLImportsDeclaration}s
     */
    public Stream<OWLImportsDeclaration> listOWLImportDeclarations() {
        InternalObjectFactory df = getObjectFactory();
        return reduce(getID().imports().map(df::toIRI).map(i -> df.getOWLDataFactory().getOWLImportsDeclaration(i)));
    }

    /**
     * Answers {@code true} if the ontology is empty (in the axiomatic point of view).
     *
     * @return {@code true}  if ontology does not contain any axioms and annotations,
     * the encapsulated graph still may contain some triples.
     */
    public boolean isOntologyEmpty() {
        return !listOWLAxioms().findFirst().isPresent() && !listOWLAnnotations().findFirst().isPresent();
    }

    /**
     * Lists {@link OWLEntity OWL Entity} for the specified IRI.
     *
     * @param iri {@link IRI}
     * @return List of {@link OWLEntity}s.
     */
    public Stream<OWLEntity> listOWLEntities(IRI iri) {
        if (iri == null) return Stream.empty();
        OntEntity e = getOntEntity(OntEntity.class, iri.getIRIString());
        List<ONTObject<? extends OWLEntity>> res = new ArrayList<>();
        InternalObjectFactory df = getObjectFactory();
        if (e.canAs(OntClass.class)) {
            res.add(df.get(e.as(OntClass.class)));
        }
        if (e.canAs(OntDT.class)) {
            res.add(df.get(e.as(OntDT.class)));
        }
        if (e.canAs(OntNAP.class)) {
            res.add(df.get(e.as(OntNAP.class)));
        }
        if (e.canAs(OntNDP.class)) {
            res.add(df.get(e.as(OntNDP.class)));
        }
        if (e.canAs(OntNOP.class)) {
            res.add(df.get(e.as(OntNOP.class)));
        }
        if (e.canAs(OntIndividual.Named.class)) {
            res.add(df.get(e.as(OntIndividual.Named.class)));
        }
        return res.stream().map(ONTObject::getObject);
    }

    /**
     * Lists all anonymous individuals in the form of OWL-API objects.
     *
     * @return Stream of {@link OWLAnonymousIndividual}s
     */
    public Stream<OWLAnonymousIndividual> listOWLAnonymousIndividuals() {
        return listOWLObjects(OWLAnonymousIndividual.class);
    }

    /**
     * Lists all named individuals in the form of OWL-API objects.
     *
     * @return Stream of {@link OWLNamedIndividual}s
     */
    public Stream<OWLNamedIndividual> listOWLNamedIndividuals() {
        return listOWLObjects(OWLNamedIndividual.class);
    }

    /**
     * Lists all OWL classes in the form of OWL-API objects.
     *
     * @return Stream of {@link OWLClass}es.
     */
    public Stream<OWLClass> listOWLClasses() {
        return listOWLObjects(OWLClass.class);
    }

    /**
     * Lists all data properties in the form of OWL-API objects.
     *
     * @return Stream of {@link OWLDataProperty}s
     */
    public Stream<OWLDataProperty> listOWLDataProperties() {
        return listOWLObjects(OWLDataProperty.class);
    }

    /**
     * Lists all object properties in the form of OWL-API objects.
     *
     * @return Stream of {@link OWLObjectProperty}s
     */
    public Stream<OWLObjectProperty> listOWLObjectProperties() {
        return listOWLObjects(OWLObjectProperty.class);
    }

    /**
     * Lists all annotation properties in the form of OWL-API objects.
     *
     * @return Stream of {@link OWLAnnotationProperty}s
     */
    public Stream<OWLAnnotationProperty> listOWLAnnotationProperties() {
        return listOWLObjects(OWLAnnotationProperty.class);
    }

    /**
     * Lists all named data-ranges (i.e. datatypes) in the form of OWL-API objects.
     *
     * @return Stream of {@link OWLDatatype}s
     */
    public Stream<OWLDatatype> listOWLDatatypes() {
        return listOWLObjects(OWLDatatype.class);
    }

    /**
     * Lists all OWL-objects of the specified class-type from the axioms and annotations cache-collections.
     *
     * @param type Class type of owl-object.
     * @param <O>  type of owl-object
     * @return Stream of {@link OWLObject}s
     */
    @SuppressWarnings("unchecked")
    protected <O extends OWLObject> Stream<O> listOWLObjects(Class<O> type) {
        return (Stream<O>) Objects.requireNonNull(objects.get(type), "Nothing found. Type: " + type).stream();
    }

    /**
     * Extracts the Set of OWL-objects of the specified class-type from the ontology header and axioms cache-collections.
     *
     * @param type Class type
     * @param <O>  subtype of {@link OWLObject}
     * @return Set of object
     */
    protected <O extends OWLObject> Set<O> readOWLObjects(Class<O> type) {
        // todo: see issue #64
        return Stream.concat(
                listOWLAnnotations().flatMap(a -> OwlObjects.objects(type, a)),
                listOWLAxioms().flatMap(a -> OwlObjects.objects(type, a)))
                .collect(Collectors.toSet());
    }

    /**
     * Gets all ontology header annotations.
     *
     * @return Stream of {@link OWLAnnotation}
     * @see #listOWLAxioms(Collection)
     */
    public Stream<OWLAnnotation> listOWLAnnotations() {
        return getHeader().objects();
    }

    /**
     * Lists {@link OWLDeclarationAxiom Declaration Axiom}s for the specified {@link OWLEntity entity}.
     * Note: method returns non-cached axioms.
     *
     * @param e {@link OWLEntity}, not {@code null}
     * @return Stream of {@link OWLDeclarationAxiom}s
     */
    public Stream<OWLDeclarationAxiom> listOWLDeclarationAxioms(OWLEntity e) {
        if (!getSnapshotConfig().isAllowReadDeclarations()) return Stream.empty();
        // Even there are no changes in OWLDeclarationAxioms,
        // they can be affected by some other user-defined axiom.
        // A direct graph reading returns uniformed axioms,
        // and a just added axiom may be absent in that list,
        // since there a lot of ways how to write the same amount of information via axioms.
        // This differs from OWL-API expectations, so need to perform traversing over whole cache
        // to get an axiom in the exactly same form as it has been specified manually:
        if (hasManuallyAddedAxioms()) {
            return listOWLAxioms(OWLDeclarationAxiom.class).filter(a -> e.equals(a.getEntity()));
        }
        OntGraphModelImpl m = getSearchModel();
        // in the case of a large ontology, the direct traverse over the graph works significantly faster:
        DeclarationTranslator t = (DeclarationTranslator) AxiomParserProvider.get(OWLDeclarationAxiom.class);
        OntEntity res = m.findNodeAs(WriteHelper.toResource(e).asNode(), WriteHelper.getEntityType(e));
        if (res == null) return Stream.empty();
        InternalObjectFactory df = getObjectFactory();
        OntStatement s = res.getRoot();
        return s == null ? Stream.empty() : Stream.of(t.toAxiom(s, df, getSnapshotConfig()).getObject());
    }

    /**
     * Lists {@link OWLAnnotationAssertionAxiom Annotation Assertion Axiom}s
     * with the given {@link OWLAnnotationSubject subject}.
     * Note: method returns non-cached axioms.
     *
     * @param s {@link OWLAnnotationSubject}, not {@code null}
     * @return Stream of {@link OWLAnnotationAssertionAxiom}s
     */
    public Stream<OWLAnnotationAssertionAxiom> listOWLAnnotationAssertionAxioms(OWLAnnotationSubject s) {
        if (!getSnapshotConfig().isLoadAnnotationAxioms()) return Stream.empty();
        if (hasManuallyAddedAxioms()) {
            return listOWLAxioms(OWLAnnotationAssertionAxiom.class).filter(a -> s.equals(a.getSubject()));
        }
        OntGraphModelImpl m = getSearchModel();
        InternalObjectFactory df = getObjectFactory();
        AxiomTranslator<OWLAnnotationAssertionAxiom> t = AxiomParserProvider.get(OWLAnnotationAssertionAxiom.class);
        ExtendedIterator<OntStatement> res = m.listLocalStatements(WriteHelper.toResource(s), null, null)
                .filterKeep(x -> t.testStatement(x, getSnapshotConfig()));
        return reduce(Iter.asStream(t.translate(res, df, getSnapshotConfig()).mapWith(ONTObject::getObject)));
    }

    /**
     * Lists {@link OWLSubClassOfAxiom SubClassOf Axiom}s by the given sub {@link OWLClass class}.
     * Note: method returns non-cached axioms.
     *
     * @param sub {@link OWLClass}, not {@code null}
     * @return Stream of {@link OWLSubClassOfAxiom}s
     */
    public Stream<OWLSubClassOfAxiom> listOWLSubClassOfAxioms(OWLClass sub) {
        if (hasManuallyAddedAxioms()) {
            return listOWLAxioms(OWLSubClassOfAxiom.class).filter(a -> Objects.equals(a.getSubClass(), sub));
        }
        OntGraphModelImpl m = getSearchModel();
        InternalObjectFactory df = getObjectFactory();
        SubClassOfTranslator t = (SubClassOfTranslator) AxiomParserProvider.get(OWLSubClassOfAxiom.class);
        ExtendedIterator<OntStatement> res = m.listLocalStatements(WriteHelper.toResource(sub), RDFS.subClassOf, null)
                .filterKeep(t::filter);
        return reduce(Iter.asStream(t.translate(res, df, getSnapshotConfig()).mapWith(ONTObject::getObject)));
    }

    /**
     * Lists {@link OWLEquivalentClassesAxiom EquivalentClasses Axiom}s by the given {@link OWLClass class}-component.
     * Note: method returns non-cached axioms.
     *
     * @param c {@link OWLClass}, not {@code null}
     * @return Stream of {@link OWLEquivalentClassesAxiom}s
     * @see AbstractNaryTranslator#axioms(OntGraphModel)
     */
    public Stream<OWLEquivalentClassesAxiom> listOWLEquivalentClassesAxioms(OWLClass c) {
        if (hasManuallyAddedAxioms()) {
            return listOWLAxioms(OWLEquivalentClassesAxiom.class).filter(a -> a.operands().anyMatch(c::equals));
        }
        InternalObjectFactory df = getObjectFactory();
        OntGraphModelImpl m = getSearchModel();
        EquivalentClassesTranslator t = (EquivalentClassesTranslator) AxiomParserProvider.get(OWLEquivalentClassesAxiom.class);
        Resource r = WriteHelper.toResource(c);
        ExtendedIterator<OntStatement> res = m.listLocalStatements(r, OWL.equivalentClass, null)
                .andThen(m.listLocalStatements(null, OWL.equivalentClass, r))
                .filterKeep(s -> t.testStatement(s, getSnapshotConfig()));
        return reduce(Iter.asStream(t.translate(res, df, getSnapshotConfig()).mapWith(ONTObject::getObject)));
    }

    /**
     * Lists all ontology axioms.
     *
     * @return Stream of {@link OWLAxiom}s
     * @see #listOWLAnnotations()
     */
    public Stream<OWLAxiom> listOWLAxioms() {
        return flatMap(getAxioms().values().stream(), ObjectTriplesMap::objects);
    }

    /**
     * Lists axioms for the specified types.
     *
     * @param types Collection of {@link AxiomType}s
     * @return Stream of {@link OWLAxiom}s
     */
    public Stream<OWLAxiom> listOWLAxioms(Collection<AxiomKey> types) {
        Map<AxiomKey, ObjectTriplesMap<OWLAxiom>> axioms = getAxioms();
        return flatMap(types.stream(), t -> axioms.get(t).objects());
    }

    /**
     * Lists axioms for the specified types.
     *
     * @param filter a {@code Iterable} of {@link AxiomType}s
     * @return Stream of {@link OWLAxiom}s
     */
    public Stream<OWLAxiom> listOWLAxioms(Iterable<AxiomType<?>> filter) {
        Map<AxiomKey, ObjectTriplesMap<OWLAxiom>> axioms = getAxioms();
        return flatMap(AxiomKey.list(filter), x -> axioms.get(x).objects());
    }

    /**
     * Lists axioms of the given class-type.
     *
     * @param view Class
     * @param <A>  type of axiom
     * @return Stream of {@link OWLAxiom}s.
     */
    @SuppressWarnings("unchecked")
    public <A extends OWLAxiom> Stream<A> listOWLAxioms(Class<A> view) {
        return (Stream<A>) getAxioms().get(AxiomKey.get(view)).objects();
    }

    /**
     * Lists axioms of the given axiom-type.
     *
     * @param type {@link AxiomType}
     * @param <A>  type of axiom
     * @return Stream of {@link OWLAxiom}s.
     */
    @SuppressWarnings("unchecked")
    public <A extends OWLAxiom> Stream<A> listOWLAxioms(AxiomType<A> type) {
        return (Stream<A>) getAxioms().get(AxiomKey.get(type)).objects();
    }

    /**
     * Performs a final operation over the specified {@code stream} before releasing it out.
     * <p>
     * It is for ensuring safety in case of multithreading environment,
     * as indicated by the parameter {@link InternalConfig#parallel()}.
     * If {@code parallel} is {@code true} and {@code stream} is unknown nature
     * then the collecting must not go beyond this method, otherwise it is allowed to be lazy.
     * Although the upper API uses {@code ReadWriteLock R/W lock} everywhere
     * (that is an original OWL-API locking style), it does not guarantee thread-safety on iterating,
     * and, therefore, without the help of this method,
     * there is a dangerous of {@link java.util.ConcurrentModificationException} (at best),
     * if some processing go outside a method who spawned the stream, in spite of the dedicated lock-section.
     * So need to make sure stream is created from a snapshot state.
     * <p>
     * Notice that this class does not produce parallel streams.
     * It is due to the dangerous of livelocks or even deadlocks while interacting with loading-caches,
     * since all of them are based on the standard Java {@code ConcurrentHashMap}.
     *
     * @param stream Stream of {@link R}s
     * @param <R>    anything
     * @return Stream of {@link R}s
     * @see #flatMap(Stream, Function)
     */
    protected <R> Stream<R> reduce(Stream<R> stream) {
        InternalConfig conf = getSnapshotConfig();
        // model is non-modifiable if cache is disabled
        if (!conf.parallel() || !conf.isContentCacheEnabled()) {
            return stream;
        }
        // use ArrayList since it is faster while iterating,
        // Uniqueness is guaranteed by other mechanisms.
        // 1024 is a magic approximate number of axioms/objects; it is not tested yet.
        ArrayList<R> res = new ArrayList<>(1024);
        stream.collect(Collectors.toCollection(() -> res));
        res.trimToSize();
        return res.stream();
    }

    /**
     * Returns a stream consisting of the results of replacing each element of this stream
     * with the contents of a mapped stream produced by applying the provided mapping function to each element.
     * The purpose of this method is the same as for {@link #reduce(Stream)}:
     * for thread-safety reasons calculations should not go beyond the bounds of this method.
     *
     * @param stream {@code Stream} of {@link X}
     * @param map    a {@link Function} for mapping {@link X} to {@code Stream} of {@link R}
     * @param <R>    anything
     * @param <X>    anything
     * @return {@code Stream} of {@link R}
     * @see #reduce(Stream)
     */
    protected <R, X> Stream<R> flatMap(Stream<X> stream, Function<X, Stream<R>> map) {
        InternalConfig conf = getSnapshotConfig();
        if (!conf.parallel() || !conf.isContentCacheEnabled()) {
            return stream.flatMap(map);
        }
        // force put everything into cache (memory) and get data snapshot
        // for now there is no any better solution
        return stream.map(map).collect(Collectors.toList()).stream().flatMap(Function.identity());
    }

    /**
     * Answers {@code true} if the given axiom is present within this buffer-model.
     * It is equivalent to the expression {@code this.listOWLAxioms().anyMatch(a::equals)}.
     *
     * @param a {@link OWLAxiom}, not {@code null}
     * @return {@code true} if axiom is present within model
     */
    public boolean contains(OWLAxiom a) {
        Map<AxiomKey, ObjectTriplesMap<OWLAxiom>> axioms = getAxioms();
        ObjectTriplesMap<OWLAxiom> map = axioms.get(AxiomKey.get(a.getAxiomType()));
        // as a hack: make sure cache is initialized.
        // it is needed in case (e.g.) of ontology is originally empty and assembled manually
        // otherwise #contains(OWLAxiom) will force collecting axioms including declarations and,
        // for example, after adding SubClassOf there would be also class declarations, which may confuse.
        if (!map.isLoaded()) {
            axioms.forEach((t, v) -> v.load());
        }
        return map.contains(a);
    }

    /**
     * Adds the given annotation to the ontology header of the model.
     *
     * @param annotation {@link OWLAnnotation}
     * @see #add(OWLAxiom)
     */
    public void add(OWLAnnotation annotation) {
        add(annotation, getHeader(), a -> WriteHelper.addAnnotations(getID(), Stream.of(annotation)));
    }

    /**
     * Adds the specified axiom to the model.
     *
     * @param axiom {@link OWLAxiom}
     * @see #add(OWLAnnotation)
     */
    public void add(OWLAxiom axiom) {
        add(axiom, getAxioms().get(AxiomKey.get(axiom.getAxiomType())),
                a -> AxiomParserProvider.getByType(a.getAxiomType()).write(a, InternalModel.this));
    }

    /**
     * Adds the OWL object to the model.
     *
     * @param object either {@link OWLAxiom} or {@link OWLAnnotation}
     * @param map    {@link ObjectTriplesMap}
     * @param writer {@link Consumer} to process writing.
     * @param <O>    type of owl-object
     */
    protected <O extends OWLObject> void add(O object, ObjectTriplesMap<O> map, Consumer<O> writer) {
        GraphListener listener = map.addListener(object);
        // todo: there is no need to invalidate *whole* objects cache
        clearObjectsCaches();
        UnionGraph.OntEventManager evm = getGraph().getEventManager();
        try {
            evm.register(listener);
            writer.accept(object);
        } catch (OntApiException e) {
            throw e;
        } catch (Exception e) {
            throw new OntApiException(String.format("OWLObject: %s, message: %s", object, e.getMessage()), e);
        } finally {
            evm.unregister(listener);
        }
    }

    /**
     * Removes the given axiom from the model.
     * Also, clears the cache for the entity type, if the entity has been belonged to the removed axiom.
     *
     * @param axiom {@link OWLAxiom}
     * @see #remove(OWLAnnotation)
     */
    public void remove(OWLAxiom axiom) {
        remove(axiom, getAxioms().get(AxiomKey.get(axiom.getAxiomType())));
        Stream.of(OWLClass.class,
                OWLDatatype.class,
                OWLAnnotationProperty.class,
                OWLDataProperty.class,
                OWLObjectProperty.class,
                OWLNamedIndividual.class,
                OWLAnonymousIndividual.class)
                .filter(type -> OwlObjects.objects(type, axiom).findAny().isPresent())
                .forEach(type -> objects.asCache().remove(type));
    }

    /**
     * Removes the given ontology header annotation from the model.
     *
     * @param annotation {@link OWLAnnotation}
     * @see #remove(OWLAxiom)
     */
    public void remove(OWLAnnotation annotation) {
        remove(annotation, getHeader());
        // todo: there is no need to invalidate *whole* objects cache
        clearObjectsCaches();
    }

    /**
     * Removes the given {@code component} from the given {@link ObjectTriplesMap map} and the model.
     * In case some component's triple is associated with other object it cannot be deleted from the graph.
     * Example of such intersection in triples is reusing b-nodes:
     * {@code <A> rdfs:subClassOf _:b0} and {@code <B> rdfs:subClassOf _:b0}.
     * Also, OWL-Entity declaration root-triples are shared between different axioms.
     * Note: need also to remove associated objects from the {@link #objects} cache!
     *
     * @param component either {@link OWLAxiom} or {@link OWLAnnotation}
     * @param map       {@link ObjectTriplesMap}
     * @param <O>       the type of OWLObject
     * @see #clearObjectsCaches()
     */
    protected <O extends OWLObject> void remove(O component, ObjectTriplesMap<O> map) {
        Set<Triple> triples = map.getTripleSet(component);
        map.delete(component);
        triples.stream().filter(t -> maps().noneMatch(m -> m.contains(t))).forEach(this::delete);
    }

    /**
     * Clears the cache for the specified triple.
     * This method is called if work directly through jena model interface.
     *
     * @param triple {@link Triple}
     */
    protected void clearCacheOnDelete(Triple triple) {
        maps().filter(ObjectTriplesMap::isLoaded)
                .filter(x -> needInvalidate(x, triple))
                .forEach(ObjectTriplesMap::clear);
        // todo: there is no need to invalidate *whole* objects cache
        clearObjectsCaches();
    }

    private static <O extends OWLObject> boolean needInvalidate(ObjectTriplesMap<O> map, Triple t) {
        return map.objects().anyMatch(o -> {
            try {
                return map.contains(o, t);
            } catch (JenaException j) {
                // may occur in case a previous operation
                // (ObjectTriplesMap#unregister() or direct working through jena interface)
                // breaks the object structure
                return true;
            }
        });
    }

    /**
     * Gets all cache buckets.
     *
     * @return {@code Stream} of {@link ObjectTriplesMap}s
     */
    private Stream<ObjectTriplesMap<? extends OWLObject>> maps() {
        return Stream.concat(Stream.of(getHeader()), getAxioms().values().stream());
    }

    /**
     * Deletes a triple from the base graph and clears the standard jena model cache for it.
     *
     * @param triple {@link Triple}
     */
    protected void delete(Triple triple) {
        getNodeCache().remove(triple.getSubject());
        getBaseGraph().delete(triple);
    }

    /**
     * The overridden jena method.
     * Makes this ontology empty given its caches.
     *
     * @return {@link Model}
     */
    @Override
    public InternalModel removeAll() {
        clearCache();
        getNodeCache().clear();
        super.removeAll();
        return this;
    }

    /**
     * Invalidates the cache if needed.
     * <p>
     * The OWL-API serialization may not work correctly without explicit expansion of axioms into
     * a strictly defined form. The cache cleaning encourages repeated reading of the encapsulated graph,
     * and, thus, leads the axioms to a uniform view.
     * Without this operation, the axiomatic representation would look slightly different
     * and the reload test (loading/saving in different formats) would not passed.
     * Also, absence of uniformed axiomatic view may lead to exceptions,
     * since some of the OWL-storers require explicit declarations, which may not be present,
     * if the ontology was assembled manually.
     * It is important to invalidate whole the cache, since user-defined axioms may content parts of other axioms,
     * such as annotation assertions, declarations, data-range definitions, etc.
     */
    public void clearCacheIfNeeded() {
        if (hasManuallyAddedAxioms()) {
            clearCache();
        }
    }

    /**
     * Answers whether there are manually added axioms in the cache.
     * For optimization: if the axiomatic model has not been changed after last reading,
     * then the cache is in a state strictly definded by the internal mechanisms,
     * and so there is no need to reset the cache.
     *
     * @return boolean
     */
    public boolean hasManuallyAddedAxioms() {
        return getAxioms().values().stream().anyMatch(ObjectTriplesMap::hasNew);
    }

    /**
     * Invalidates all caches.
     */
    public void clearCache() {
        cachedID = null;
        axioms = null;
        header = null;
        snapshot = null;
        clearObjectsCaches();
    }

    /**
     * Invalidates {@link #objects}, {@link #objectFactoryCache} and {@link #searchModelCache} caches.
     * Auxiliary method.
     */
    protected void clearObjectsCaches() {
        objects.asCache().clear();
        objectFactoryCache.asCache().clear();
        searchModelCache.asCache().clear();
        systemResources.clear();
    }

    @Override
    public String toString() {
        return String.format("[%s]%s", getClass().getSimpleName(), getID());
    }

    /**
     * Creates a {@code Map} with {@link AxiomKey} as keys and {@link ObjectTriplesMap} as values,
     * that contains all possible axiom-types.
     *
     * @return an unmodifiable {@code Map}
     */
    protected Map<AxiomKey, ObjectTriplesMap<OWLAxiom>> createAxiomsCacheMap() {
        Map<AxiomKey, ObjectTriplesMap<OWLAxiom>> res = new EnumMap<>(AxiomKey.class);
        for (AxiomKey t : AxiomKey.values()) {
            res.put(t, createAxiomTriplesMap(t.getAxiomClass()));
        }
        return Collections.unmodifiableMap(res);
    }

    /**
     * Creates a {@link ObjectTriplesMap}-container for the ontology axioms of the specified type.
     *
     * @param type {@code Class}-type of {@link OWLAxiom}
     * @param <A>  {@link OWLAxiom} of the specified {@code type}
     * @return {@link ObjectTriplesMap}
     */
    protected <A extends OWLAxiom> ObjectTriplesMap<A> createAxiomTriplesMap(Class<A> type) {
        InternalObjectFactory df = getObjectFactory();
        AxiomTranslator<A> t = AxiomParserProvider.get(type);
        return createObjectTriplesMap(type, () -> t.listAxioms(InternalModel.this.getSearchModel(), df, getSnapshotConfig()));
    }

    /**
     * Creates a {@link ObjectTriplesMap}-container for the ontology header annotations.
     *
     * @return {@link ObjectTriplesMap}
     */
    protected ObjectTriplesMap<OWLAnnotation> createHeaderTriplesMap() {
        InternalObjectFactory df = getObjectFactory();
        return createObjectTriplesMap(OWLAnnotation.class, () -> ReadHelper.listOWLAnnotations(getID(), df));
    }

    /**
     * Creates a fresh instance of the {@link ObjectTriplesMap} container.
     *
     * @param type   {@code Class}-type for the desired {@link OWLObject}
     * @param loader a {@link ONTObject}s provider as a {@code Supplier}, that returns {@code Iterator}
     * @param <O>    either {@link OWLAxiom} or {@link OWLAnnotation}
     * @return {@link ObjectTriplesMap}
     */
    protected <O extends OWLObject> ObjectTriplesMap<O> createObjectTriplesMap(Class<O> type,
                                                                               Supplier<Iterator<ONTObject<O>>> loader) {
        InternalConfig conf = getSnapshotConfig();
        if (!conf.isContentCacheEnabled())
            return new DirectObjectTripleMapImpl<>(loader);
        boolean parallel = conf.parallel();
        boolean fastIterator = conf.useIteratorContentCache();
        boolean tripleStore = conf.useTriplesContentCache();
        if (!LOGGER.isDebugEnabled()) {
            return new CacheObjectTriplesMapImpl<>(loader, parallel, fastIterator, tripleStore);
        }
        OntID id = getID();
        return new CacheObjectTriplesMapImpl<O>(loader, parallel, fastIterator, tripleStore) {
            @Override
            protected CachedMap loadMap() {
                Instant start = Instant.now();
                CachedMap res = super.loadMap();
                Duration d = Duration.between(start, Instant.now());
                // commons-lang3 is included in jena-arq (3.6.0)
                LOGGER.debug("[{}]{}:::{}s{}", id,
                        StringUtils.rightPad("[" + type.getSimpleName() + "]", 42),
                        String.format(Locale.ENGLISH, "%.3f", d.toMillis() / 1000.0),
                        res.size() != 0 ? "(" + res.size() + ")" : ""
                );
                return res;
            }
        };
    }

    /**
     * The direct listener to synchronize caches while working through OWL-API and jena at the same time.
     *
     * @see org.apache.jena.graph.GraphListener
     * @see org.apache.jena.graph.GraphEventManager
     */
    public class DirectListener extends GraphListenerBase {
        private boolean hasObjectListener() {
            return getGraph().getEventManager().hasListeners(CacheObjectTriplesMapImpl.Listener.class);
        }

        private void invalidate() {
            if (hasObjectListener()) return;
            clearCache();
        }

        /**
         * If at the moment there is an {@link CacheObjectTriplesMapImpl.Listener}
         * then it's called from {@link InternalModel#add(OWLAxiom)} =&gt; don't clear cache;
         * otherwise it is direct call and cache must be reset to have correct list of axioms.
         *
         * @param t {@link Triple}
         */
        @Override
        protected void addEvent(Triple t) {
            // we don't know which axiom would own this triple, so we clear whole cache.
            invalidate();
        }

        @Override
        protected void deleteEvent(Triple t) {
            if (hasObjectListener()) return;
            clearCacheOnDelete(t);
        }

        @Override
        public void notifyAddGraph(Graph g, Graph other) {
            invalidate();
        }

        @Override
        public void notifyDeleteGraph(Graph g, Graph other) {
            invalidate();
        }
    }

}
