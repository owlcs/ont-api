/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.ID;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.internal.axioms.AbstractNaryTranslator;
import com.github.owlcs.ontapi.internal.searchers.axioms.*;
import com.github.owlcs.ontapi.internal.searchers.objects.ClassSearcher;
import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.RWLockedGraph;
import com.github.owlcs.ontapi.jena.UnionGraph;
import com.github.owlcs.ontapi.jena.impl.OntGraphModelImpl;
import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphEventManager;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.shared.Lock;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A Buffer Graph OWL model, which supports both listing OWL-API objects (OWL Axioms, Entities and Annotations)
 * and Jena interfaces (through the {@link OntModel} view of RDF Graph).
 * <p>
 * It is an analogue of <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/Internals.java'>uk.ac.manchester.cs.owl.owlapi.Internals</a>.
 * This model is used by the facade model (i.e. by {@link Ontology}) while reading and writing
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
 * TODO: Should it return {@link ONTObject}s, not just naked {@link OWLObject}s (see #87, #72)?
 * It seems it would be more convenient and could make this class useful not only as part of inner implementation.
 * <p>
 * Created by @szuev on 26.10.2016.
 */
@SuppressWarnings({"WeakerAccess", "unchecked"})
public class InternalModel extends OntGraphModelImpl
        implements OntModel, HasOntologyID, HasObjectFactory, HasConfig, ListAxioms {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalModel.class);

    /**
     * Ontology ID cache.
     */
    protected volatile ID cachedID;
    /**
     * The configuration settings to control behaviour.
     * As a container that contains an immutable snapshot, which should be reset on {@link #clearCache()}.
     *
     * @see InternalConfig#snapshot()
     */
    private final InternalCache.Loading<InternalModel, InternalConfig> config;
    /**
     * An internal object factory,
     * that is responsible for mapping {@link OntObject ONT Jena Object}s to {@link OWLObject OWL-API object}s.
     * It is used while collecting axioms, may be reset to release memory.
     * Any change in the base graph must reset this cache.
     * Designed as a {@link java.lang.ref.SoftReference}
     * since it is mostly need only to optimize reading operations and may contain huge amount of objects.
     *
     * @see InternalConfig#useLoadObjectsCache()
     * @see CacheObjectFactory
     */
    protected final InternalCache.Loading<InternalModel, ModelObjectFactory> objectFactory;
    /**
     * A model for axiom/object's search optimizations, containing {@link Node node}s cache.
     * Any change in the base graph must also reset this cache.
     * Designed as a {@link java.lang.ref.SoftReference}
     * since it is mostly need only to optimize reading operations and may contain huge amount of objects.
     *
     * @see InternalConfig#useLoadNodesCache()
     * @see SearchModel
     */
    protected final InternalCache.Loading<InternalModel, OntGraphModelImpl> searchModel;
    /**
     * The main cache, which contains all axioms and the ontology header.
     * It contains {@code 40} key-value pairs, {@code 39} for kinds of axioms and one for the ontology header.
     *
     * @see OWLTopObjectType#all()
     * @see ObjectMap
     */
    protected final InternalCache.Loading<InternalModel, Map<OWLTopObjectType, ObjectMap<? extends OWLObject>>> content;
    /**
     * OWL objects cache to work with OWL-API 'signature' methods.
     * Currently it is calculated from the {@link #content}.
     * Any direct (manual) change in the graph must also reset this cache.
     *
     * @see OWLComponentType#keys()
     * @see ObjectMap
     */
    protected final InternalCache.Loading<InternalModel, Map<OWLComponentType, ObjectMap<OWLObject>>> components;
    /**
     * The direct listener, it monitors changes that occur through the main (Jena) interface.
     */
    protected final DirectListener directListener;

    // Helpers to provide searching axioms by some objects (referencing by primitives).
    protected final ByObjectSearcher<OWLAxiom, OWLClass> byClass = new ByClass();
    protected final ByObjectSearcher<OWLAxiom, OWLDatatype> byDatatype = new ByDatatype();
    protected final ByObjectSearcher<OWLAxiom, OWLNamedIndividual> byNamedIndividual = new ByNamedIndividual();
    protected final ByObjectSearcher<OWLAxiom, OWLObjectProperty> byObjectProperty = new ByObjectProperty();
    protected final ByObjectSearcher<OWLAxiom, OWLDataProperty> byDataProperty = new ByDataProperty();
    protected final ByObjectSearcher<OWLAxiom, OWLAnnotationProperty> byAnnotationProperty = new ByAnnotationProperty();
    protected final ByObjectSearcher<OWLAxiom, OWLLiteral> byLiteral = new ByLiteral();
    protected final ByObjectSearcher<OWLAxiom, OWLAnonymousIndividual> byAnonymousIndividual = new ByAnonymousIndividual();
    protected final ByObjectSearcher<OWLAxiom, IRI> byIRI = new ByIRI();
    // Other searchers
    protected final ByObjectSearcher<OWLDeclarationAxiom, OWLEntity> declarationsByEntity = new DeclarationByEntity();
    protected final ByObjectSearcher<OWLAnnotationAssertionAxiom, OWLAnnotationSubject> annotationAssertionsBySubject
            = new AnnotationAssertionBySubject();
    protected final ByObjectSearcher<OWLDataPropertyAssertionAxiom, OWLIndividual> dataAssertionsBySubject
            = new DataAssertionBySubject();
    protected final ByObjectSearcher<OWLObjectPropertyAssertionAxiom, OWLIndividual> objectAssertionsBySubject
            = new ObjectAssertionBySubject();
    protected final ByObjectSearcher<OWLObjectPropertyRangeAxiom, OWLObjectPropertyExpression> objectPropertyRangeByProperty
            = new ObjectPropertyRangeBySubject();
    protected final ByObjectSearcher<OWLObjectPropertyDomainAxiom, OWLObjectPropertyExpression> objectPropertyDomainByProperty
            = new ObjectPropertyDomainBySubject();
    protected final ByObjectSearcher<OWLSubClassOfAxiom, OWLClass> subClassOfBySubject = new SubClassOfBySubject();
    protected final ByObjectSearcher<OWLSubClassOfAxiom, OWLClass> subClassOfByObject = new SubClassOfByObject();
    protected final ByObjectSearcher<OWLEquivalentClassesAxiom, OWLClass> equivalentClassesByClass
            = new EquivalentClassesByOperand();
    protected final ByObjectSearcher<OWLDisjointClassesAxiom, OWLClass> disjointClassesByClass
            = new DisjointClassesByOperand();
    protected final ByObjectSearcher<OWLClassAssertionAxiom, OWLIndividual> classAssertionsByIndividual
            = new ClassAssertionBySubject();
    protected final ByObjectSearcher<OWLClassAssertionAxiom, OWLClassExpression> classAssertionsByClass
            = new ClassAssertionByObject();

    // To search OWLObjects
    protected final ObjectsSearcher<OWLClass> classSearcher = new ClassSearcher();

    /**
     * Constructs a model instance.
     * For internal usage only.
     *
     * @param base        {@link Graph}, not {@code null}, a primary and single data-storage
     * @param personality {@link OntPersonality}, not {@code null},
     *                    a facility to conduct {@link Node} to {@link OntObject} mappings
     * @param config      {@link InternalConfig}, not {@code null}, to control caches and ontological views
     * @param dataFactory {@link DataFactory}, not {@code null}, to produce standard {@link OWLObject}s
     * @param fromManager {@code Map} or {@code null},
     *                    a possibility to share cache-data between different model instances
     */
    public InternalModel(Graph base,
                         OntPersonality personality,
                         InternalConfig config,
                         DataFactory dataFactory,
                         Map<Class<? extends OWLPrimitive>, InternalCache<?, ?>> fromManager) {
        super(base, personality);
        Objects.requireNonNull(dataFactory);
        Objects.requireNonNull(config);
        this.config = InternalCache.createSingleton(x -> config.snapshot());
        this.objectFactory = InternalCache.createSoftSingleton(x -> createObjectFactory(dataFactory, fromManager));
        this.searchModel = InternalCache.createSoftSingleton(x -> createSearchModel());
        this.content = InternalCache.createSingleton(x -> createContentStore());
        this.components = InternalCache.createSingleton(x -> createComponentStore());
        this.directListener = createDirectListener();
        enableDirectListening();
    }

    /**
     * Gets the {@link OWLOntologyID OWL Ontology ID} from the model.
     *
     * @return {@link ID}
     * @see #getID()
     */
    @Override
    public ID getOntologyID() {
        // believe the last condition (which is very fast) justifies having a cache
        if (cachedID != null && getBaseGraph().contains(cachedID.asNode(), RDF.Nodes.type, OWL.Ontology.asNode())) {
            return cachedID;
        }
        return cachedID = new ID(getID());
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
        try {
            disableDirectListening();
            // these are controlled changes; do not reset the whole cache,
            // just only annotations (associated triples map is changed):
            getHeaderCache().clear();
            if (Objects.requireNonNull(id, "Null id").isAnonymous()) {
                OntID res;
                if (id instanceof ID) {
                    res = getNodeAs(createOntologyID(this, ((ID) id).asNode()).asNode(), OntID.class);
                } else {
                    res = setID(null);
                }
                res.setVersionIRI(null);
            } else {
                setID(id.getOntologyIRI().map(IRI::getIRIString).orElseThrow(IllegalArgumentException::new))
                        .setVersionIRI(id.getVersionIRI().map(IRI::getIRIString).orElse(null));
            }
        } finally {
            enableDirectListening();
        }
        if (id instanceof ID) {
            this.cachedID = (ID) id;
        }
    }

    /**
     * Returns the model's {@link InternalConfig} snapshot instance, which is immutable object.
     *
     * @return {@link InternalConfig.Snapshot}
     */
    @Override
    public InternalConfig getConfig() {
        return config.get(this);
    }

    /**
     * Returns the {@code InternalDataFactory}, a helper (possibly, with cache) to read OWL-API objects.
     *
     * @return {@link ModelObjectFactory}
     */
    @Override
    @Nonnull
    public ModelObjectFactory getObjectFactory() {
        return objectFactory.get(this);
    }

    /**
     * Creates a fresh {@link ModelObjectFactory Object Factory} instance,
     * which is responsible for mapping {@link Node} (and {@link OntObject}) to {@link OWLObject}.
     * If the load objects cache is enabled,
     * the method returns a {@link CacheObjectFactory} instance,
     * that caches {@link OWLObject}s and, therefore, may take up a lot of memory.
     * Otherwise, in case the load cache is disabled, the {@link ModelObjectFactory} will be returned.
     *
     * @param df       {@link DataFactory}, not {@code null}
     * @param external a {@code Map} with shared outer caches, not {@code null}
     * @return {@link ModelObjectFactory}
     * @see com.github.owlcs.ontapi.config.CacheSettings#getLoadObjectsCacheSize()
     */
    protected ModelObjectFactory createObjectFactory(DataFactory df,
                                                     Map<Class<? extends OWLPrimitive>, InternalCache<?, ?>> external) {
        InternalConfig conf = getConfig();
        Supplier<OntModel> model = this::getSearchModel;
        if (!conf.useLoadObjectsCache()) {
            return new InternalObjectFactory(df, model);
        }
        long size = conf.getLoadObjectsCacheSize();
        boolean parallel = conf.parallel();
        Map<Class<? extends OWLPrimitive>, InternalCache<?, ?>> map = external == null ? Collections.emptyMap() : external;
        return new CacheObjectFactory(df, model, map, () -> InternalCache.createBounded(parallel, size));
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
        return searchModel.get(this);
    }

    /**
     * Derives a model to be used in read operations.
     * If the load nodes cache is enabled
     * the method returns a {@link SearchModel} - a facility to optimize read operations,
     * otherwise this same {@link InternalModel} instance with no optimizations will be returned.
     * A {@code SearchModel} contains a {@link Node}s cache inside and, therefore, may take up a lot of memory.
     *
     * @return {@link OntModel}
     * @see com.github.owlcs.ontapi.config.CacheSettings#getLoadNodesCacheSize()
     */
    protected OntGraphModelImpl createSearchModel() {
        if (!getConfig().useLoadNodesCache()) {
            return this;
        }
        return new SearchModel(getGraph(), getOntPersonality(), getConfig()) {

            @Override
            public String toString() {
                return String.format("[SearchModel]%s", getID());
            }

            @Override
            @Nonnull
            public ModelObjectFactory getObjectFactory() {
                return InternalModel.this.getObjectFactory();
            }
        };
    }

    @Override
    public <N extends RDFNode> N fetchNodeAs(Node node, Class<N> type) {
        try {
            return super.fetchNodeAs(node, type);
        } catch (OntJenaException e) {
            return SearchModel.handleFetchNodeAsException(e, node, type, this, getConfig());
        }
    }

    /**
     * Creates a direct listener.
     *
     * @return {@link DirectListener}
     */
    protected DirectListener createDirectListener() {
        return new DirectListener();
    }

    /**
     * Enables direct listening.
     * Any change through RDF interface will be tracked out.
     *
     * @see #disableDirectListening()
     */
    protected void disableDirectListening() {
        getGraph().getEventManager().unregister(directListener);
    }

    /**
     * Disables direct listening.
     *
     * @see #enableDirectListening()
     */
    protected void enableDirectListening() {
        getGraph().getEventManager().register(directListener);
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
     * @return {@code Stream} of {@link OWLImportsDeclaration}s
     */
    public Stream<OWLImportsDeclaration> listOWLImportDeclarations() {
        ModelObjectFactory of = getObjectFactory();
        DataFactory df = getDataFactory();
        return ModelIterators.reduce(getID().imports().map(of::toIRI).map(df::getOWLImportsDeclaration), getConfig());
    }

    /**
     * Answers {@code true} if the ontology is ontologically empty (no header, no axioms).
     *
     * @return {@code true} if the ontology does not contain any axioms and annotations (locally);
     * note, that the encapsulated graph still may contain some triples,
     * and the method {@link Model#isEmpty()} may return {@code false} at the same time
     */
    public boolean isOntologyEmpty() {
        Graph bg = getBaseGraph();
        if (bg instanceof GraphMem) {
            if (bg.isEmpty()) {
                // really empty:
                return true;
            }
            // has only id:
            if (bg.size() == 1 && bg.contains(Node.ANY, RDF.type.asNode(), OWL.Ontology.asNode())) {
                return true;
            }
        }
        if (!components.isEmpty()) {
            return false;
        }
        return !Stream.concat(listOWLAnnotations(), listOWLAxioms()).findFirst().isPresent();
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
        if (e == null) {
            return Stream.empty();
        }
        List<ONTObject<? extends OWLEntity>> res = new ArrayList<>();
        ModelObjectFactory df = getObjectFactory();
        if (e.canAs(OntClass.Named.class)) {
            res.add(df.getClass(e.as(OntClass.Named.class)));
        }
        if (e.canAs(OntDataRange.Named.class)) {
            res.add(df.getDatatype(e.as(OntDataRange.Named.class)));
        }
        if (e.canAs(OntAnnotationProperty.class)) {
            res.add(df.getProperty(e.as(OntAnnotationProperty.class)));
        }
        if (e.canAs(OntDataProperty.class)) {
            res.add(df.getProperty(e.as(OntDataProperty.class)));
        }
        if (e.canAs(OntObjectProperty.Named.class)) {
            res.add(df.getProperty(e.as(OntObjectProperty.Named.class)));
        }
        if (e.canAs(OntIndividual.Named.class)) {
            res.add(df.getIndividual(e.as(OntIndividual.Named.class)));
        }
        return res.stream().map(ONTObject::getOWLObject);
    }

    /**
     * Tests if the given {@link OWLEntity} has the OWL declaration.
     * A builtin entity has no declarations.
     *
     * @param e {@link OWLEntity} to test
     * @return boolean
     */
    public boolean containsOWLDeclaration(OWLEntity e) {
        InternalConfig config = getConfig();
        if (!config.isAllowReadDeclarations()) return false;
        if (useAxiomsSearchOptimization(config)) {
            return getBaseGraph().contains(WriteHelper.toNode(e), RDF.type.asNode(), WriteHelper.getRDFType(e).asNode());
        }
        return listOWLAxioms(OWLDeclarationAxiom.class).anyMatch(x -> x.getEntity().equals(e));
    }

    /**
     * Lists all anonymous individuals in the form of OWL-API objects.
     *
     * @return {@code Stream} of {@link OWLAnonymousIndividual}s
     */
    public Stream<OWLAnonymousIndividual> listOWLAnonymousIndividuals() {
        return listComponents(OWLComponentType.ANONYMOUS_INDIVIDUAL);
    }

    /**
     * Lists all named individuals in the form of OWL-API objects.
     *
     * @return {@code Stream} of {@link OWLNamedIndividual}s
     */
    public Stream<OWLNamedIndividual> listOWLNamedIndividuals() {
        return listComponents(OWLComponentType.NAMED_INDIVIDUAL);
    }

    /**
     * Lists all OWL classes in the form of OWL-API objects.
     *
     * @return {@code Stream} of {@link OWLClass}es.
     */
    public Stream<OWLClass> listOWLClasses() {
        return listComponents(OWLComponentType.CLASS);
    }

    /**
     * Lists all data properties in the form of OWL-API objects.
     *
     * @return {@code Stream} of {@link OWLDataProperty}s
     */
    public Stream<OWLDataProperty> listOWLDataProperties() {
        return listComponents(OWLComponentType.DATATYPE_PROPERTY);
    }

    /**
     * Lists all object properties in the form of OWL-API objects.
     *
     * @return {@code Stream} of {@link OWLObjectProperty}s
     */
    public Stream<OWLObjectProperty> listOWLObjectProperties() {
        return listComponents(OWLComponentType.NAMED_OBJECT_PROPERTY);
    }

    /**
     * Lists all annotation properties in the form of OWL-API objects.
     *
     * @return {@code Stream} of {@link OWLAnnotationProperty}s
     */
    public Stream<OWLAnnotationProperty> listOWLAnnotationProperties() {
        return listComponents(OWLComponentType.ANNOTATION_PROPERTY);
    }

    /**
     * Lists all named data-ranges (i.e. datatypes) in the form of OWL-API objects.
     *
     * @return {@code Stream} of {@link OWLDatatype}s
     */
    public Stream<OWLDatatype> listOWLDatatypes() {
        return listComponents(OWLComponentType.DATATYPE);
    }

    /**
     * Answers {@code true} if the given datatype is present in the ontology signature.
     *
     * @param d {@link OWLDatatype}
     * @return boolean
     */
    public boolean containsOWLEntity(OWLDatatype d) {
        return containsComponent(OWLComponentType.DATATYPE, d);
    }

    /**
     * Answers {@code true} if the given class is present in the ontology signature.
     *
     * @param c {@link OWLClass}
     * @return boolean
     */
    public boolean containsOWLEntity(OWLClass c) {
        return containsComponent(OWLComponentType.CLASS, c);
    }

    /**
     * Answers {@code true} if the given individual is present in the ontology signature.
     *
     * @param i {@link OWLNamedIndividual}
     * @return boolean
     */
    public boolean containsOWLEntity(OWLNamedIndividual i) {
        return containsComponent(OWLComponentType.NAMED_INDIVIDUAL, i);
    }

    /**
     * Answers {@code true} if the given property is present in the ontology signature.
     *
     * @param p {@link OWLDataProperty}
     * @return boolean
     */
    public boolean containsOWLEntity(OWLDataProperty p) {
        return containsComponent(OWLComponentType.DATATYPE_PROPERTY, p);
    }

    /**
     * Answers {@code true} if the given property is present in the ontology signature.
     *
     * @param p {@link OWLObjectProperty}
     * @return boolean
     */
    public boolean containsOWLEntity(OWLObjectProperty p) {
        return containsComponent(OWLComponentType.NAMED_OBJECT_PROPERTY, p);
    }

    /**
     * Answers {@code true} if the given property is present in the ontology signature.
     *
     * @param p {@link OWLAnnotationProperty}
     * @return boolean
     */
    public boolean containsOWLEntity(OWLAnnotationProperty p) {
        return containsComponent(OWLComponentType.ANNOTATION_PROPERTY, p);
    }

    /**
     * Gets all ontology header annotations.
     *
     * @return {@code Stream} of {@link OWLAnnotation}
     * @see InternalModel#listOWLAxioms()
     */
    public Stream<OWLAnnotation> listOWLAnnotations() {
        return getHeaderCache().keys();
    }

    @Override
    public Stream<OWLDeclarationAxiom> listOWLDeclarationAxioms(OWLEntity entity) {
        InternalConfig config = getConfig();
        if (!config.isAllowReadDeclarations()) return Stream.empty();
        // Even there are no changes in OWLDeclarationAxioms,
        // they can be affected by some other user-defined axiom.
        // A direct graph reading returns uniformed axioms,
        // and a just added axiom may be absent in that list,
        // since there a lot of ways how to write the same amount of information via axioms.
        // This differs from OWL-API expectations, so need to perform traversing over whole cache
        // to get an axiom in the exactly same form as it has been specified manually:
        if (!useAxiomsSearchOptimization(config)) {
            return ListAxioms.super.listOWLDeclarationAxioms(entity);
        }
        // in the case of a large ontology, the direct traverse over the graph works significantly faster:
        return listOWLAxioms(declarationsByEntity, OWLDeclarationAxiom.class, entity, config);
    }

    @Override
    public Stream<OWLAnnotationAssertionAxiom> listOWLAnnotationAssertionAxioms(OWLAnnotationSubject subject) {
        InternalConfig config = getConfig();
        if (!config.isLoadAnnotationAxioms()) return Stream.empty();
        if (!useAxiomsSearchOptimization(config)) {
            return ListAxioms.super.listOWLAnnotationAssertionAxioms(subject);
        }
        return listOWLAxioms(annotationAssertionsBySubject, OWLAnnotationAssertionAxiom.class, subject, config);
    }

    @Override
    public Stream<OWLDataPropertyAssertionAxiom> listOWLDataPropertyAssertionAxioms(OWLIndividual subject) {
        InternalConfig config = getConfig();
        if (!useAxiomsSearchOptimization(config)) {
            return ListAxioms.super.listOWLDataPropertyAssertionAxioms(subject);
        }
        return listOWLAxioms(dataAssertionsBySubject, OWLDataPropertyAssertionAxiom.class, subject, config);
    }

    @Override
    public Stream<OWLObjectPropertyAssertionAxiom> listOWLObjectPropertyAssertionAxioms(OWLIndividual subject) {
        InternalConfig config = getConfig();
        if (!useAxiomsSearchOptimization(config)) {
            return ListAxioms.super.listOWLObjectPropertyAssertionAxioms(subject);
        }
        return listOWLAxioms(objectAssertionsBySubject, OWLObjectPropertyAssertionAxiom.class, subject, config);
    }

    @Override
    public Stream<OWLObjectPropertyRangeAxiom> listOWLObjectPropertyRangeAxioms(OWLObjectPropertyExpression subject) {
        InternalConfig config = getConfig();
        if (!ObjectPropertyRangeBySubject.isSupported(subject) || !useAxiomsSearchOptimization(config)) {
            return ListAxioms.super.listOWLObjectPropertyRangeAxioms(subject);
        }
        return listOWLAxioms(objectPropertyRangeByProperty, OWLObjectPropertyRangeAxiom.class, subject, config);
    }

    @Override
    public Stream<OWLObjectPropertyDomainAxiom> listOWLObjectPropertyDomainAxioms(OWLObjectPropertyExpression subject) {
        InternalConfig config = getConfig();
        if (!ObjectPropertyRangeBySubject.isSupported(subject) || !useAxiomsSearchOptimization(config)) {
            return ListAxioms.super.listOWLObjectPropertyDomainAxioms(subject);
        }
        return listOWLAxioms(objectPropertyDomainByProperty, OWLObjectPropertyDomainAxiom.class, subject, config);
    }

    @Override
    public Stream<OWLClassAssertionAxiom> listOWLClassAssertionAxioms(OWLIndividual subject) {
        InternalConfig config = getConfig();
        if (!useAxiomsSearchOptimization(config)) {
            return ListAxioms.super.listOWLClassAssertionAxioms(subject);
        }
        return listOWLAxioms(classAssertionsByIndividual, OWLClassAssertionAxiom.class, subject, config);
    }

    @Override
    public Stream<OWLClassAssertionAxiom> listOWLClassAssertionAxioms(OWLClassExpression object) {
        InternalConfig config = getConfig();
        if (!ClassAssertionByObject.isSupported(object) || !useAxiomsSearchOptimization(config)) {
            return ListAxioms.super.listOWLClassAssertionAxioms(object);
        }
        return listOWLAxioms(classAssertionsByClass, OWLClassAssertionAxiom.class, object, config);
    }

    @Override
    public Stream<OWLSubClassOfAxiom> listOWLSubClassOfAxiomsBySubject(OWLClass subject) {
        InternalConfig config = getConfig();
        if (!useAxiomsSearchOptimization(config)) {
            return ListAxioms.super.listOWLSubClassOfAxiomsBySubject(subject);
        }
        return listOWLAxioms(subClassOfBySubject, OWLSubClassOfAxiom.class, subject, config);
    }

    @Override
    public Stream<OWLSubClassOfAxiom> listOWLSubClassOfAxiomsByObject(OWLClass object) {
        InternalConfig config = getConfig();
        if (!useAxiomsSearchOptimization(config)) {
            return ListAxioms.super.listOWLSubClassOfAxiomsByObject(object);
        }
        return listOWLAxioms(subClassOfByObject, OWLSubClassOfAxiom.class, object, config);
    }

    @Override
    public Stream<OWLEquivalentClassesAxiom> listOWLEquivalentClassesAxioms(OWLClass clazz) {
        return listOWLNaryAxiomAxiomsByOperand(clazz, OWLEquivalentClassesAxiom.class, equivalentClassesByClass);
    }

    @Override
    public Stream<OWLDisjointClassesAxiom> listOWLDisjointClassesAxioms(OWLClass clazz) {
        InternalConfig config = getConfig();
        // bad performance of the graph-reading way if
        // there is a []-list with many disjoint classes (i.e. owl:AllDisjointClasses) =>
        // it is faster to use parsing of cached axioms (i.e. a classic way), if the cache is present
        if (!config.useContentCache() ||
                (!hasManuallyAddedAxioms() && !containsLocal(null, RDF.type, OWL.AllDisjointClasses))) {
            return listOWLAxioms(disjointClassesByClass, OWLDisjointClassesAxiom.class, clazz, config);
        }
        return listOWLNaryAxiomAxiomsByOperand(OWLDisjointClassesAxiom.class, clazz);
    }

    /**
     * Lists {@link OWLNaryAxiom N-Ary Axiom}s by the given {@link OWLObject} operand.
     *
     * @param operand  {@link K}, not {@code null}
     * @param type     - class-type of {@link A}
     * @param searcher - {@link ByObjectSearcher} for {@link A} and {@link K}
     * @param <A>      - subtype of {@link OWLNaryAxiom}
     * @param <K>      - subtype of {@link OWLObject}
     * @return a {@code Stream} of {@link A}s
     * @see AbstractNaryTranslator#axioms(OntModel)
     */
    @SuppressWarnings("SameParameterValue")
    protected <A extends OWLNaryAxiom<? super K>,
            K extends OWLObject> Stream<A> listOWLNaryAxiomAxiomsByOperand(K operand,
                                                                           Class<A> type,
                                                                           ByObjectSearcher<A, K> searcher) {
        InternalConfig config = getConfig();
        if (!useAxiomsSearchOptimization(config)) {
            return listOWLNaryAxiomAxiomsByOperand(type, operand);
        }
        return listOWLAxioms(searcher, type, operand, config);
    }

    /**
     * Auxiliary method to extract axioms from the graph.
     * Note: the method returns non-cached axioms.
     *
     * @param searcher  - {@link ByObjectSearcher}
     * @param type      - {@code Class}-type of {@link A}
     * @param parameter - {@link K}, to search by
     * @param config    - {@link InternalConfig}
     * @param <A>       - {@link OWLAxiom}
     * @param <K>       - {@link OWLObject}
     * @return a {@code Stream} of {@link A}s (it is distinct if default settings)
     */
    protected <A extends OWLAxiom, K extends OWLObject> Stream<A> listOWLAxioms(ByObjectSearcher<A, K> searcher,
                                                                                Class<A> type,
                                                                                K parameter,
                                                                                InternalConfig config) {
        ExtendedIterator<A> res = searcher.listONTAxioms(parameter, getSearchModel(), getObjectFactory(), config)
                .mapWith(ONTObject::getOWLObject);
        OWLTopObjectType key = OWLTopObjectType.get(type);
        if (key.isDistinct()) {
            return ModelIterators.reduce(res, config);
        }
        return ModelIterators.reduceDistinct(res, config);
    }

    /**
     * Lists all {@code OWLAxiom}s for the given {@link OWLPrimitive}
     *
     * @param primitive not {@code null}
     * @return {@code Stream} of {@link OWLAxiom}s
     */
    public Stream<OWLAxiom> listOWLAxioms(OWLPrimitive primitive) {
        OWLComponentType filter = OWLComponentType.get(primitive);
        InternalConfig config = getConfig();
        if (useReferencingAxiomsSearchOptimization(filter, config)) {
            ExtendedIterator<ONTObject<OWLAxiom>> res;
            OntModel model = getSearchModel();
            ModelObjectFactory factory = getObjectFactory();
            if (filter == OWLComponentType.IRI) {
                res = byIRI.listONTAxioms((IRI) primitive, model, factory, config);
            } else if (filter == OWLComponentType.CLASS) {
                res = byClass.listONTAxioms((OWLClass) primitive, model, factory, config);
            } else if (filter == OWLComponentType.NAMED_OBJECT_PROPERTY) {
                res = byObjectProperty.listONTAxioms((OWLObjectProperty) primitive, model, factory, config);
            } else if (filter == OWLComponentType.ANNOTATION_PROPERTY) {
                res = byAnnotationProperty.listONTAxioms((OWLAnnotationProperty) primitive, model, factory, config);
            } else if (filter == OWLComponentType.DATATYPE_PROPERTY) {
                res = byDataProperty.listONTAxioms((OWLDataProperty) primitive, model, factory, config);
            } else if (filter == OWLComponentType.NAMED_INDIVIDUAL) {
                res = byNamedIndividual.listONTAxioms((OWLNamedIndividual) primitive, model, factory, config);
            } else if (filter == OWLComponentType.DATATYPE) {
                res = byDatatype.listONTAxioms((OWLDatatype) primitive, model, factory, config);
            } else if (filter == OWLComponentType.LITERAL) {
                res = byLiteral.listONTAxioms((OWLLiteral) primitive, model, factory, config);
            } else if (filter == OWLComponentType.ANONYMOUS_INDIVIDUAL) {
                res = byAnonymousIndividual.listONTAxioms((OWLAnonymousIndividual) primitive, model, factory, config);
            } else {
                throw new OntApiException.IllegalArgument("Wrong type: " + filter);
            }
            return ModelIterators.reduceDistinct(res.mapWith(ONTObject::getOWLObject), config);
        }
        // the default way:
        if (OWLTopObjectType.ANNOTATION.hasComponent(filter)) {
            // is type of annotation -> any axiom may contain the primitive
            return ModelIterators.reduce(OWLTopObjectType.axioms().flatMap(k -> {
                ObjectMap<OWLAxiom> axioms = getContentCache(k);
                Predicate<OWLAxiom> p = k.hasComponent(filter) ? a -> true : k::hasAnnotations;
                return axioms.keys().filter(x -> p.test(x) && filter.contains(x, primitive));
            }), config);
        }
        // select only those container-types, that are capable to contain the primitive
        return ModelIterators.flatMap(filteredAxiomsCaches(OWLTopObjectType.axioms().filter(x -> x.hasComponent(filter))),
                k -> k.keys().filter(x -> filter.contains(x, primitive)), config);
    }

    /**
     * Answers {@code true} if the graph optimization for referencing axioms functionality is allowed and makes sense.
     *
     * @param type   {@link OWLComponentType}
     * @param config {@link InternalConfig}
     * @return boolean
     * @see #useAxiomsSearchOptimization(InternalConfig)
     */
    protected boolean useReferencingAxiomsSearchOptimization(OWLComponentType type, InternalConfig config) {
        if (!config.useContentCache()) {
            // no cache at all -> always use the graph way
            return true;
        }
        if (hasManuallyAddedAxioms()) {
            // manually added axioms cannot be derived from the graph
            return false;
        }
        // if cache is loaded - decide which way to use:
        // either the graph-optimization way or straightforward cache parsing
        if (contentCaches().allMatch(ObjectMap::isLoaded)) {
            // The empirical founded threshold (right now I do not see a better solution)
            // (for small ontologies it is better to use cache traversing instead of graph searching)
            long threshold = -1;
            if (type == OWLComponentType.DATATYPE) {
                // the graph-optimized-way has usually worse performance in comparison with the classic cache parsing
                // maybe it is because there are usually only a few owl-datatypes, but many theirs entrances;
                return false;
            } else if (type == OWLComponentType.CLASS) {
                threshold = 200;
            } else if (type == OWLComponentType.NAMED_OBJECT_PROPERTY) {
                threshold = 2000;
            } else if (type == OWLComponentType.ANNOTATION_PROPERTY) {
                threshold = 2000;
            } else if (type == OWLComponentType.DATATYPE_PROPERTY) {
                threshold = 100;
            } else if (type == OWLComponentType.NAMED_INDIVIDUAL) {
                // the graph-way is usually faster, especially for big ontologies,
                // but it may be not true in case of special complexity (e.g. with owl:AllDifferent)
                threshold = 3000;
            }
            // for IRI graph optimization is always faster
            // for literals and anonymous individuals too
            return getOWLAxiomCount() >= threshold;
        }
        return true;
    }

    /**
     * Answers {@code true} if need to use {@link ByObjectSearcher}-search optimization instead of parsing cache.
     *
     * @param config {@link InternalConfig}
     * @return boolean
     * @see #useObjectsSearchOptimization(InternalConfig)
     * @see #useReferencingAxiomsSearchOptimization(OWLComponentType, InternalConfig)
     */
    protected boolean useAxiomsSearchOptimization(InternalConfig config) {
        return !config.useContentCache() || !hasManuallyAddedAxioms();
    }

    /**
     * Lists all ontology axioms.
     *
     * @return {@code Stream} of {@link OWLAxiom}s
     * @see #listOWLAnnotations()
     */
    public Stream<OWLAxiom> listOWLAxioms() {
        return ModelIterators.flatMap(filteredAxiomsCaches(OWLTopObjectType.axioms()), ObjectMap::keys, getConfig());
    }

    /**
     * Lists all logical axioms.
     *
     * @return {@code Stream} of {@link OWLAxiom}s
     */
    public Stream<OWLLogicalAxiom> listOWLLogicalAxioms() {
        return ModelIterators.flatMap(filteredAxiomsCaches(OWLTopObjectType.logical()),
                m -> (Stream<OWLLogicalAxiom>) m.keys(), getConfig());
    }

    /**
     * Lists axioms for the specified types.
     *
     * @param filter a {@code Iterable} of {@link AxiomType}s
     * @return {@code Stream} of {@link OWLAxiom}s
     */
    public Stream<OWLAxiom> listOWLAxioms(Iterable<AxiomType<?>> filter) {
        return ModelIterators.flatMap(filteredAxiomsCaches(OWLTopObjectType.axioms(filter)), ObjectMap::keys, getConfig());
    }

    @Override
    public <A extends OWLAxiom> Stream<A> listOWLAxioms(Class<A> type) {
        return listOWLAxioms(OWLTopObjectType.get(type));
    }

    private <A extends OWLAxiom> Stream<A> listOWLAxioms(OWLTopObjectType type) {
        return (Stream<A>) getAxiomsCache(type).keys();
    }

    /**
     * Selects all axioms for the given object-component.
     *
     * @param type   a class-type of {@link OWLAxiom}
     * @param object {@link OWLObject}, that is present as a component in every container of the returned stream
     * @param <A>    any subtype of {@link OWLAxiom}
     * @return {@code Stream} of {@link OWLAxiom}s
     */
    public <A extends OWLAxiom> Stream<A> listOWLAxioms(Class<A> type, OWLObject object) {
        OWLTopObjectType key = OWLTopObjectType.get(type);
        OWLComponentType filter = OWLComponentType.get(object);
        if (!OWLTopObjectType.ANNOTATION.hasComponent(filter) && !key.hasComponent(filter)) {
            return Stream.empty();
        }
        return (Stream<A>) listOWLAxioms(key).filter(x -> filter.contains(x, object));
    }

    /**
     * Lists axioms of the given axiom-type.
     *
     * @param type {@link AxiomType}
     * @param <A>  type of axiom
     * @return {@code Stream} of {@link OWLAxiom}s
     */
    public <A extends OWLAxiom> Stream<A> listOWLAxioms(AxiomType<A> type) {
        return (Stream<A>) listOWLAxioms(OWLTopObjectType.get(type));
    }

    /**
     * Returns the number of axioms in this ontology.
     *
     * @return {@code long}, the count
     */
    public long getOWLAxiomCount() {
        return getContentStore().entrySet().stream()
                .filter(x -> x.getKey().isAxiom())
                .mapToLong(x -> x.getValue().count()).sum();
    }

    /**
     * Answers {@code true} if the given axiom is present within this buffer-model.
     * It is equivalent to the expression {@code this.listOWLAxioms().anyMatch(a::equals)}.
     *
     * @param a {@link OWLAxiom}, not {@code null}
     * @return {@code true} if the axiom is present within the model
     * @see #contains(OWLAnnotation)
     */
    public boolean contains(OWLAxiom a) {
        return getAxiomsCache(OWLTopObjectType.get(a.getAxiomType())).contains(a);
    }

    /**
     * Answers {@code true} if the given annotation is present in ontology header.
     *
     * @param a {@link OWLAnnotation}, not {@code null}
     * @return {@code true} if the annotation is present within the model
     * @see #contains(OWLAxiom)
     */
    public boolean contains(OWLAnnotation a) {
        return getHeaderCache().contains(a);
    }

    /**
     * Adds the specified axiom to the model.
     *
     * @param axiom {@link OWLAxiom}
     * @return {@code true} if the {@code axiom} has been added to the graph
     * @see #add(OWLAnnotation)
     */
    public boolean add(OWLAxiom axiom) {
        return add(OWLTopObjectType.get(axiom.getAxiomType()), axiom);
    }

    /**
     * Adds the given annotation to the ontology header of the model.
     *
     * @param annotation {@link OWLAnnotation}
     * @return {@code true} if the {@code annotation} has been added to the graph
     * @see #add(OWLAxiom)
     */
    public boolean add(OWLAnnotation annotation) {
        return add(OWLTopObjectType.ANNOTATION, annotation);
    }

    /**
     * Removes the given axiom from the model.
     * Also, clears the cache for the entity type, if the entity has been belonged to the removed axiom.
     *
     * @param axiom {@link OWLAxiom}
     * @return {@code true} if the {@code axiom} has been removed from the graph
     * @see #remove(OWLAnnotation)
     */
    public boolean remove(OWLAxiom axiom) {
        return remove(OWLTopObjectType.get(axiom.getAxiomType()), axiom);
    }

    /**
     * Removes the given ontology header annotation from the model.
     *
     * @param annotation {@link OWLAnnotation}
     * @return {@code true} if the {@code annotation} has been removed from the graph
     * @see #remove(OWLAxiom)
     */
    public boolean remove(OWLAnnotation annotation) {
        return remove(OWLTopObjectType.ANNOTATION, annotation);
    }

    /**
     * Adds the specified {@code OWLObject} into the model.
     *
     * @param key       {@link OWLTopObjectType}, not {@code null}
     * @param container either {@link OWLAxiom} or {@link OWLAnnotation},
     *                  that corresponds to the {@code key}, not {@code null}
     * @return {@code true} if the graph has been changed
     * @throws OntApiException in case the object cannot be added into model
     */
    protected boolean add(OWLTopObjectType key, OWLObject container) throws OntApiException {
        OWLTriples.Listener listener = OWLTriples.createListener();
        GraphEventManager evm = getGraph().getEventManager();
        ObjectMap<OWLObject> map = getContentCache(key);
        map.load(); // before graph modification
        try {
            disableDirectListening();
            evm.register(listener);
            key.write(this, container);
        } catch (Exception e) {
            listener.getTriples().forEach(this::delete);
            if (e instanceof OntApiException)
                throw e;
            throw new OntApiException(String.format("OWLObject: %s, message: '%s'", container, e.getMessage()), e);
        } finally {
            evm.unregister(listener);
            enableDirectListening();
        }
        OWLTriples<OWLObject> value = listener.toObject(container);
        if (value.isDefinitelyEmpty()) {
            LOGGER.warn("Attempt to add empty OWL object: {}", container);
            return false;
        }
        map.add(value);
        // put new components into objects cache
        cacheComponents(container);
        // clear search model and object factory
        clearOtherCaches();
        return true;
    }

    /**
     * Removes the given {@code container} from the corresponding {@link ObjectMap cache} and the model.
     * In case some container's triple is associated with other object, it cannot be deleted from the graph.
     * Example of such intersection in triples is reusing b-nodes:
     * {@code <A> rdfs:subClassOf _:b0} and {@code <B> rdfs:subClassOf _:b0}.
     * Also, OWL-Entity declaration root-triples are shared between different axioms.
     *
     * @param key       {@link OWLTopObjectType}, not {@code null}
     * @param container either {@link OWLAxiom} or {@link OWLAnnotation},
     *                  that corresponds to the {@code key}, not {@code null}
     * @return {@code true} if the graph has been changed
     * @see #clearComponentsCaches()
     */
    protected boolean remove(OWLTopObjectType key, OWLObject container) {
        try {
            disableDirectListening();
            ObjectMap<OWLObject> map = getContentCache(key);
            ONTObject<OWLObject> value = map.get(container);
            if (value == null) {
                // this may happen in case the method is called by some native OWL-parsers:
                // they, sometimes, do not be aware what they do
                return false;
            }
            map.remove(container);
            container = value.getOWLObject();
            OntModel m = toModel(value);
            // triples that are used by other content objects:
            Set<Triple> used = getUsedTriples(m, container);
            // remove related components from the objects cache
            // (even there is no graph changes);
            // do it before graph modification since ONTObject's may rely on graph
            clearComponents(container);
            // physically delete triples:
            Graph g = m.getBaseGraph();
            long size = g.size();
            g.find().filterDrop(used::contains).forEachRemaining(this::delete);
            boolean res = size != g.size();
            // clear search model and object factory
            clearOtherCaches();
            return res;
        } finally {
            enableDirectListening();
        }
    }

    /**
     * Calculates and returns the {@link Triple triple}s,
     * that belong to both the given content-container and some other one.
     * There are three cases of triples intersections:
     * <ul>
     * <li>Shared entity declarations.
     * Each ONT-API axiom is supplied with full triple set, that includes all used declarations,
     * therefore two axioms that have some entity in common,
     * also have the same triple-declaration for that entity in their triple sets.</li>
     * <li>Punned axiom definition. If punning is allowed
     * it is possible to have same main triple shared between different axioms with distinguished type.
     * E.g. {@code rdfs:subPropertyOf}-axiom for punned properties.</li>
     * <li>Shared component. Although, this case is prohibited for a good OWL2 ontology,
     * in general it is possible to have non-entity shared components, e.g. class-expressions.
     * For example {@code SubClassOf(A, _:x)} and {@code SubClassOf(B, _:x)}
     * would have identical sets of triples for class expression {@code _:x}.</li>
     * </ul>
     *
     * @param model     {@link OntModel} the model to traverse over,
     *                  must correspond to the {@code container}, not {@code null}
     * @param container {@link OWLObject} - a content-container,
     *                  for which this operation is performed, not {@code null}
     * @return {@code Set} of {@code Triple}s in intersection
     */
    protected Set<Triple> getUsedTriples(OntModel model, OWLObject container) {
        ModelObjectFactory f = HasObjectFactory.getObjectFactory(model);
        InternalConfig c = HasConfig.getConfig(model);
        Set<Triple> res = new HashSet<>();
        // shared declaration and punned axioms:
        Iter.flatMap(OWLTopObjectType.listAll(), type -> type.read(f, c)
                .filterKeep(x -> {
                    OWLObject obj = x.getOWLObject();
                    if (type != OWLTopObjectType.DECLARATION && container.equals(obj)) return false;
                    if (getContentCache(type).contains(obj)) {
                        return true;
                    }
                    if (type == OWLTopObjectType.DECLARATION) {
                        OWLEntity entity = ((OWLDeclarationAxiom) obj).getEntity();
                        return findUsedContentContainer(entity, obj).isPresent();
                    }
                    return false;
                }))
                .forEachRemaining(x -> x.triples().forEach(res::add));
        // other shared components:
        OWLComponentType.sharedComponents().forEach(type -> {
            Set<OWLObject> candidates = new HashSet<>();
            Set<Triple> triples = new HashSet<>();
            type.select(model, f).forEach(x -> {
                candidates.add(x.getOWLObject());
                x.triples().forEach(triples::add);
            });
            if (candidates.isEmpty()) {
                return;
            }
            // search for axioms for this component type
            selectContentContainers(type)
                    .forEach(x -> {
                        OWLObject obj = x.getOWLObject();
                        if (container.equals(obj)) {
                            return;
                        }
                        if (!type.containsAny(obj, candidates)) {
                            return;
                        }
                        x.triples().filter(triples::contains).forEach(res::add);
                    });
        });
        return res;
    }

    /**
     * Finds the container object which contains the given component object somewhere in its depths.
     *
     * @param entity   {@link OWLObject} to check, not {@code null}
     * @param excludes Array, with containers
     *                 (that must be either {@link OWLAxiom} or {@link OWLAnnotation}) to exclude from consideration
     * @return {@code Optional} around the container object
     */
    protected Optional<OWLObject> findUsedContentContainer(OWLObject entity, OWLObject... excludes) {
        OWLComponentType type = OWLComponentType.get(entity);
        Stream<OWLObject> res = selectContentObjects(type);
        if (excludes.length != 0) {
            Set<OWLObject> ignore = excludes.length == 1 ?
                    Collections.singleton(excludes[0]) : new HashSet<>(Arrays.asList(excludes));
            res = res.filter(x -> !ignore.contains(x));
        }
        return res.filter(x -> type.contains(x, entity)).findFirst();
    }

    /**
     * Represents the given container as a {@link OntModel OWL Graph Model}.
     *
     * @param o {@link ONTObject}-wrapper
     * @return {@link OntModel}
     */
    protected OntModel toModel(ONTObject<? extends OWLObject> o) {
        Graph g = o.toGraph();
        if (LOGGER.isDebugEnabled()) {
            g.getPrefixMapping().setNsPrefixes(getNsPrefixMap());
        }
        UnionGraph u = new UnionGraph(g, false);
        u.addGraph(getGraph());
        class ObjectModel extends OntGraphModelImpl implements HasConfig, HasObjectFactory {
            public ObjectModel(Graph g) {
                super(g, InternalModel.this.getOntPersonality());
            }

            @Override
            public OntID getID() {
                return InternalModel.this.getID().inModel(this).as(OntID.class);
            }

            @Override
            public String toString() {
                return String.format("ModelFor{%s}", o.getOWLObject());
            }

            @Override
            public InternalConfig getConfig() {
                return InternalModel.this.getConfig();
            }

            @Override
            @Nonnull
            public ModelObjectFactory getObjectFactory() {
                return new InternalObjectFactory(InternalModel.this.getDataFactory(), () -> this);
            }
        }
        return new ObjectModel(u);
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
        // todo: how can we diagnose only those caches, which are really affected?
        if (hasManuallyAddedAxioms()) {
            clearCache();
        }
    }

    /**
     * Answers whether there are manually added axioms in the cache.
     * For optimization: if the axiomatic model has not been changed after last reading,
     * then the cache is in a state strictly defined by the internal mechanisms,
     * and so there is no need to reset the cache.
     *
     * @return boolean
     */
    public boolean hasManuallyAddedAxioms() {
        return contentCaches().anyMatch(ObjectMap::hasNew);
    }

    /**
     * Invalidates all caches.
     */
    public void clearCache() {
        cachedID = null;
        content.clear();
        config.clear();
        clearComponentsCaches();
    }

    /**
     * Invalidates {@link #components}, {@link #objectFactory} and {@link #searchModel} caches.
     * Auxiliary method.
     */
    protected void clearComponentsCaches() {
        components.clear();
        clearOtherCaches();
    }

    /**
     * Invalidates search model and object factory caches.
     * Auxiliary method.
     */
    protected void clearOtherCaches() {
        objectFactory.clear();
        searchModel.clear();
    }

    @Override
    public String toString() {
        return String.format("[%s]%s", getClass().getSimpleName(), getID());
    }

    /**
     * Creates a {@code Map} that has {@link Enum}-keys using the specified parameters.
     *
     * @param type   {@code Class}-type of {@link K}
     * @param keys   {@code Stream} with {@link K}-keys
     * @param loader {@code Function} to collect {@link V}-values
     * @param <K>    subtype of {@link Enum}
     * @param <V>    anything, a value type
     * @return {@code Map}
     */
    protected <K extends Enum<K>, V> Map<K, V> createMapStore(Class<K> type, Stream<K> keys, Function<K, V> loader) {
        Map<K, V> res = new EnumMap<>(type);
        keys.forEach(k -> res.put(k, loader.apply(k)));
        return res;
    }

    /**
     * Lists all OWL-objects of the specified {@code type}.
     *
     * @param type {@link OWLComponentType}, not {@code null}
     * @param <O>  type of owl-object
     * @return {@code Stream} of {@link OWLObject}s
     * @see OWLComponentType
     */
    protected <O extends OWLObject> Stream<O> listComponents(OWLComponentType type) {
        return (Stream<O>) getComponentCache(type).keys();
    }

    /**
     * Tests if the given component-type pair is present in the signature.
     *
     * @param type {@link OWLComponentType}, not {@code null}
     * @param o    {@link OWLObject} of the {@code type}
     * @return boolean
     * @see OWLComponentType
     */
    protected boolean containsComponent(OWLComponentType type, OWLObject o) {
        return getComponentCache(type).contains(o);
    }

    /**
     * Gets components for the given {@code type} in the form of {@code ObjectMap}.
     *
     * @param type {@link OWLComponentType}, not {@code null}
     * @param <O>  {@link OWLObject} class-type that corresponds the {@code type}
     * @return a {@link ObjectMap} of {@link O}s
     * @see #getContentCache(OWLTopObjectType)
     * @see OWLComponentType
     */
    protected <O extends OWLObject> ObjectMap<O> getComponentCache(OWLComponentType type) {
        return (ObjectMap<O>) Objects.requireNonNull(components.get(this).get(type), "Nothing found. Type: " + type);
    }

    /**
     * Creates a {@link ObjectMap} container for the given {@link OWLComponentType}.
     *
     * @param type {@link OWLComponentType}, not {@code null}
     * @return {@link ObjectMap}
     * @see #createContentObjectMap(OWLTopObjectType)
     * @see OWLComponentType
     */
    protected ObjectMap<OWLObject> createComponentObjectMap(OWLComponentType type) {
        InternalConfig conf = getConfig();
        Supplier<Iterator<ONTObject<OWLObject>>> loader = () -> listOWLObjects(type, conf);
        if (!conf.useComponentCache()) {
            ObjectsSearcher<OWLObject> searcher;
            if (OWLComponentType.CLASS == type) {
                searcher = cast(classSearcher);
            } else { // TODO: other types
                return new DirectObjectMapImpl<>(loader);
            }
            return new DirectObjectMapImpl<>(loader,
                    k -> searcher.findONTObject(k, getSearchModel(), getObjectFactory(), getConfig()),
                    k -> searcher.containsONTObject(k, getSearchModel(), getObjectFactory(), getConfig()));
        }
        boolean parallel = conf.parallel();
        boolean fastIterator = conf.useIteratorCache();
        return new CacheObjectMapImpl<>(loader, false, parallel, fastIterator);
    }

    private static ObjectsSearcher<OWLObject> cast(ObjectsSearcher<?> x) {
        return (ObjectsSearcher<OWLObject>) x;
    }

    /**
     * Lists all objects of the specified {@code type}.
     *
     * @param type {@link OWLComponentType} - owl object type, that is used in the object's cache
     * @param conf {@link InternalConfig}
     * @return an {@code Iterator} of {@link ONTObject} with the given type
     */
    protected Iterator<ONTObject<OWLObject>> listOWLObjects(OWLComponentType type, InternalConfig conf) {
        ModelObjectFactory factory = getObjectFactory();
        OntModel model = getSearchModel();
        if (OWLComponentType.CLASS == type && useObjectsSearchOptimization(conf)) {
            //noinspection rawtypes
            return ((Iterator) classSearcher.listONTObjects(model, factory, conf));
        }
        // if content cache is loaded its parsing is faster than graph-optimization
        return selectContentObjects(type).flatMap(x -> type.select(x, model, factory)).iterator();
    }

    /**
     * Answers {@code true} when need to use {@link ObjectsSearcher} optimization.
     *
     * @param config {@link InternalConfig}, not {@code null}
     * @return boolean
     * @see #useAxiomsSearchOptimization(InternalConfig)
     * @see #useReferencingAxiomsSearchOptimization(OWLComponentType, InternalConfig)
     */
    protected boolean useObjectsSearchOptimization(InternalConfig config) {
        return !config.useContentCache() || contentCaches().anyMatch(x -> !x.isLoaded());
    }

    /**
     * Creates a component store {@code Map}.
     *
     * @return {@link Map}
     * @see #createContentStore()
     * @see OWLComponentType
     */
    protected Map<OWLComponentType, ObjectMap<OWLObject>> createComponentStore() {
        return createMapStore(OWLComponentType.class, OWLComponentType.keys(), this::createComponentObjectMap);
    }

    /**
     * Selects the objects from the {@link #content} cache, that may hold a component of the given {@code type}.
     *
     * @param type {@link OWLComponentType}, not {@code null}
     * @return {@code Stream} of {@link ONTObject} - containers from the {@link #content} cache
     */
    protected Stream<ONTObject<OWLObject>> selectContentContainers(OWLComponentType type) {
        return selectContent(type, k -> getContentCache(k).values(), (k, x) -> k.hasAnnotations(x.getOWLObject()));
    }

    /**
     * Selects the objects from the {@link #content} cache, that may hold a component of the given {@code type}.
     *
     * @param type {@link OWLComponentType}, not {@code null}
     * @return {@code Stream} of {@link OWLObject} - containers from the {@link #content} cache
     */
    protected Stream<OWLObject> selectContentObjects(OWLComponentType type) {
        return selectContent(type, k -> getContentCache(k).keys(), OWLTopObjectType::hasAnnotations);
    }

    /**
     * Selects the objects from the {@link #content} cache, that may hold a component of the given {@code type}.
     *
     * @param type            {@link OWLComponentType}, not {@code null}
     * @param toStream        a {@code Function} to provide {@code Stream} of {@link R}
     *                        for a given {@link OWLTopObjectType}, not {@code null}
     * @param withAnnotations a {@code BiPredicate} to select only those {@link R},
     *                        which have OWL annotations, not {@code null}
     * @param <R>             anything
     * @return {@code Stream} of {@link R} - containers from the {@link #content} cache
     */
    protected <R> Stream<R> selectContent(OWLComponentType type,
                                          Function<OWLTopObjectType, Stream<R>> toStream,
                                          BiPredicate<OWLTopObjectType, R> withAnnotations) {
        // todo: consider the case when there is no bulk annotations at all ?
        if (!OWLTopObjectType.ANNOTATION.hasComponent(type)) {
            // select only those axiom types which are allowed to contain the component type
            return OWLTopObjectType.all().filter(k -> k.hasComponent(type)).flatMap(toStream);
        }
        // any axiom or header annotation may contain this component
        return OWLTopObjectType.all().flatMap(k -> {
            if (k.hasComponent(type)) {
                // the axiom-type (k) definitely contains the component type:
                return toStream.apply(k);
            }
            // the axiom-type (k) does not contain the component type,
            // but it still can be present in its annotations
            return toStream.apply(k).filter(x -> withAnnotations.test(k, x));
        });
    }

    /**
     * Invalidates the {@link #components cache} for all components parsed from the given {@code container}.
     * todo: is a smarter mechanism to invalidate the related components possible here?
     *
     * @param container {@link OWLObject}, not {@code null}
     * @see #clearComponentsCaches()
     * @see OWLComponentType
     */
    protected void clearComponents(OWLObject container) {
        if (components.isEmpty()) return;
        Map<OWLComponentType, ObjectMap<OWLObject>> cache = components.get(this);
        OWLComponentType.keys().forEach(type -> {
            ObjectMap<OWLObject> map = cache.get(type);
            if (!map.isLoaded()) {
                return;
            }
            if (!type.select(container).findFirst().isPresent()) return;
            map.clear();
        });
    }

    /**
     * Extracts all components from the given {@code container} and puts them into the {@link #components} cache.
     *
     * @param container {@link OWLObject}, not {@code null}
     * @see #clearComponentsCaches()
     * @see OWLComponentType
     */
    protected void cacheComponents(OWLObject container) {
        if (components.isEmpty()) return;
        Map<OWLComponentType, ObjectMap<OWLObject>> cache = components.get(this);
        ModelObjectFactory df = getObjectFactory();
        OntModel m = getSearchModel();
        OWLComponentType.keys().forEach(type -> {
            ObjectMap<OWLObject> map = cache.get(type);
            if (!map.isLoaded()) {
                return;
            }
            type.select(container, m, df).forEach(map::add);
        });
    }

    /**
     * Forcibly loads the whole content cache.
     */
    public void forceLoad() {
        contentCaches().forEach(ObjectMap::load);
    }

    /**
     * Maps the given {@code Stream} of {@link OWLTopObjectType} to {@link ObjectMap}.
     * The input must contain only those elements
     * for which the {@link OWLTopObjectType#isAxiom()} method returns {@code true}.
     *
     * @param keys {@code Stream} of {@link OWLTopObjectType}
     * @return {@code Stream} of {@link ObjectMap} containing {@link OWLAxiom}s
     */
    protected Stream<ObjectMap<? extends OWLAxiom>> filteredAxiomsCaches(Stream<OWLTopObjectType> keys) {
        Map<OWLTopObjectType, ObjectMap<? extends OWLObject>> map = getContentStore();
        return keys.map(x -> (ObjectMap<? extends OWLAxiom>) map.get(x));
    }

    /**
     * Gets the {@link ObjectMap} for the given {@link OWLTopObjectType}.
     * The {@link OWLTopObjectType#isAxiom()} method for the input must return {@code true}.
     *
     * @param key {@link OWLTopObjectType}, not {@code null}
     * @return {@link ObjectMap}
     */
    protected ObjectMap<OWLAxiom> getAxiomsCache(OWLTopObjectType key) {
        return getContentCache(key);
    }

    /**
     * Gets an ontology header content cache-store.
     *
     * @return {@link ObjectMap}
     */
    protected ObjectMap<OWLAnnotation> getHeaderCache() {
        return getContentCache(OWLTopObjectType.ANNOTATION);
    }

    /**
     * Gets an ontology content {@code ObjectMap}-cache.
     *
     * @param key {@link OWLTopObjectType}, not {@code null}
     * @param <X> either {@link OWLAxiom} or {@link OWLAnnotation}
     * @return {@link ObjectMap}
     * @see #getComponentCache(OWLComponentType)
     */
    protected <X extends OWLObject> ObjectMap<X> getContentCache(OWLTopObjectType key) {
        return (ObjectMap<X>) getContentStore().get(key);
    }

    /**
     * Gets a content store {@code Map}.
     *
     * @return {@link Map}
     */
    protected Map<OWLTopObjectType, ObjectMap<? extends OWLObject>> getContentStore() {
        return content.get(this);
    }

    /**
     * @return a {@code Stream} of {@link ObjectMap}s
     */
    protected Stream<ObjectMap<?>> contentCaches() {
        return getContentStore().values().stream();
    }

    /**
     * Creates a content store {@code Map}.
     *
     * @return {@link Map}
     * @see #createComponentStore()
     */
    protected Map<OWLTopObjectType, ObjectMap<? extends OWLObject>> createContentStore() {
        return createMapStore(OWLTopObjectType.class, OWLTopObjectType.all(), this::createContentObjectMap);
    }

    /**
     * Creates a {@link ObjectMap} container for the given {@link OWLTopObjectType}.
     *
     * @param key {@link OWLTopObjectType}
     * @return {@link ObjectMap}
     * @see #createComponentObjectMap(OWLComponentType)
     */
    protected ObjectMap<OWLObject> createContentObjectMap(OWLTopObjectType key) {
        ModelObjectFactory factory = getObjectFactory();
        Supplier<Iterator<ONTObject<OWLObject>>> loader =
                () -> (Iterator<ONTObject<OWLObject>>) key.read(factory, getConfig());
        InternalConfig conf = getConfig();
        if (!conf.useContentCache()) {
            return new DirectObjectMapImpl<>(loader,
                    k -> (Optional<ONTObject<OWLObject>>) key.find(factory, getConfig(), k),
                    k -> key.has(factory, getConfig(), k));
        }
        boolean parallel = conf.parallel();
        boolean fastIterator = conf.useIteratorCache();
        boolean withMerge = !key.isDistinct();
        if (!LOGGER.isDebugEnabled()) {
            return new CacheObjectMapImpl<>(loader, withMerge, parallel, fastIterator);
        }
        OntID id = getID();
        return new CacheObjectMapImpl<OWLObject>(loader, withMerge, parallel, fastIterator) {
            @Override
            protected CachedMap<OWLObject, ONTObject<OWLObject>> loadMap() {
                Instant start = Instant.now();
                CachedMap<OWLObject, ONTObject<OWLObject>> res = super.loadMap();
                Duration d = Duration.between(start, Instant.now());
                if (res.size() == 0) return res;
                // commons-lang3 is included in jena-arq (3.6.0)
                LOGGER.debug("[{}]{}:::{}s{}", id,
                        StringUtils.rightPad("[" + key + "]", 42),
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

        protected void invalidate() {
            clearCache();
        }

        @Override
        protected void addEvent(Triple t) {
            // we don't know which axiom would own this triple, so we clear the whole cache.
            invalidate();
        }

        @Override
        protected void deleteEvent(Triple t) {
            // Although it is possible to detect only those cache elements,
            // that are really affected by deleting the triple,
            // but such a calculation would be rather too complicated and time-consuming and (therefore) possibly buggy.
            // So it seems to be better just release all caches.
            invalidate();
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
