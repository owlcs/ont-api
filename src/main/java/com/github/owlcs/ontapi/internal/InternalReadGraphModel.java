/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
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
import com.github.owlcs.ontapi.internal.axioms.AbstractNaryTranslator;
import com.github.owlcs.ontapi.internal.objects.ModelObject;
import com.github.owlcs.ontapi.internal.searchers.axioms.AnnotationAssertionBySubject;
import com.github.owlcs.ontapi.internal.searchers.axioms.ByAnnotationProperty;
import com.github.owlcs.ontapi.internal.searchers.axioms.ByAnonymousIndividual;
import com.github.owlcs.ontapi.internal.searchers.axioms.ByClass;
import com.github.owlcs.ontapi.internal.searchers.axioms.ByDataProperty;
import com.github.owlcs.ontapi.internal.searchers.axioms.ByDatatype;
import com.github.owlcs.ontapi.internal.searchers.axioms.ByIRI;
import com.github.owlcs.ontapi.internal.searchers.axioms.ByLiteral;
import com.github.owlcs.ontapi.internal.searchers.axioms.ByNamedIndividual;
import com.github.owlcs.ontapi.internal.searchers.axioms.ByObjectProperty;
import com.github.owlcs.ontapi.internal.searchers.axioms.ClassAssertionByObject;
import com.github.owlcs.ontapi.internal.searchers.axioms.ClassAssertionBySubject;
import com.github.owlcs.ontapi.internal.searchers.axioms.DataAssertionBySubject;
import com.github.owlcs.ontapi.internal.searchers.axioms.DeclarationByEntity;
import com.github.owlcs.ontapi.internal.searchers.axioms.DisjointClassesByOperand;
import com.github.owlcs.ontapi.internal.searchers.axioms.EquivalentClassesByOperand;
import com.github.owlcs.ontapi.internal.searchers.axioms.ObjectAssertionBySubject;
import com.github.owlcs.ontapi.internal.searchers.axioms.ObjectPropertyDomainBySubject;
import com.github.owlcs.ontapi.internal.searchers.axioms.ObjectPropertyRangeBySubject;
import com.github.owlcs.ontapi.internal.searchers.axioms.SubClassOfByObject;
import com.github.owlcs.ontapi.internal.searchers.axioms.SubClassOfBySubject;
import com.github.owlcs.ontapi.internal.searchers.objects.AnnotationPropertySearcher;
import com.github.owlcs.ontapi.internal.searchers.objects.ClassSearcher;
import com.github.owlcs.ontapi.internal.searchers.objects.DataPropertySearcher;
import com.github.owlcs.ontapi.internal.searchers.objects.DatatypeSearcher;
import com.github.owlcs.ontapi.internal.searchers.objects.NamedIndividualSearcher;
import com.github.owlcs.ontapi.internal.searchers.objects.ObjectPropertySearcher;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.ontapi.OntJenaException;
import org.apache.jena.ontapi.common.OntPersonality;
import org.apache.jena.ontapi.impl.OntGraphModelImpl;
import org.apache.jena.ontapi.model.OntAnnotationProperty;
import org.apache.jena.ontapi.model.OntClass;
import org.apache.jena.ontapi.model.OntDataProperty;
import org.apache.jena.ontapi.model.OntDataRange;
import org.apache.jena.ontapi.model.OntEntity;
import org.apache.jena.ontapi.model.OntID;
import org.apache.jena.ontapi.model.OntIndividual;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntObject;
import org.apache.jena.ontapi.model.OntObjectProperty;
import org.apache.jena.ontapi.model.OntStatement;
import org.apache.jena.ontapi.utils.Graphs;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.Lock;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNaryAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLPrimitive;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A base {@link OntGraphModelImpl}-extension
 * that provides reading {@link OWLObject OWL Objects} from the encapsulated graph.
 * This impl is also responsible for collecting and store different caches
 * and conducting read operations from these caches.
 * <p>
 * Created by @ssz on 25.05.2020.
 */
abstract class InternalReadGraphModel extends OntGraphModelImpl implements ListAxioms, HasObjectFactory, HasConfig {
    static final Logger LOGGER = LoggerFactory.getLogger(InternalGraphModel.class);

    /**
     * Ontology ID cache.
     */
    protected volatile ID cachedID;
    /**
     * The configuration settings to control behaviour.
     * As a container that contains an immutable snapshot, which should be reset on {@link InternalGraphModelImpl#clearCache()}.
     *
     * @see InternalConfig#snapshot()
     */
    protected final InternalCache.Loading<InternalReadGraphModel, InternalConfig> config;
    /**
     * An internal object factory,
     * that is responsible for mapping {@link OntObject ONT Jena Object}s to {@link OWLObject OWL-API object}s.
     * It is used while collecting axioms, may be reset to release memory.
     * Any change in the base graph must reset this cache.
     * Designed as a {@link java.lang.ref.SoftReference}
     * since it is mostly needed only to optimize reading operations and may contain huge number of objects.
     *
     * @see InternalConfig#useLoadObjectsCache()
     * @see CacheObjectFactory
     */
    protected final InternalCache.Loading<InternalReadGraphModel, ModelObjectFactory> objectFactory;
    /**
     * A model for axiom/object's search optimizations, containing {@link Node node}s cache.
     * Any change in the base graph must also reset this cache.
     * Designed as a {@link java.lang.ref.SoftReference}
     * since it is mostly needed only to optimize reading operations and may contain huge number of objects.
     *
     * @see InternalConfig#useLoadNodesCache()
     * @see SearchModel
     */
    protected final InternalCache.Loading<InternalReadGraphModel, OntGraphModelImpl> searchModel;
    /**
     * The main cache, which contains all axioms and the ontology header.
     * It contains {@code 40} key-value pairs, {@code 39} for kinds of axioms and one for the ontology header.
     *
     * @see OWLTopObjectType#all()
     * @see ObjectMap
     */
    protected final InternalCache.Loading<InternalReadGraphModel, Map<OWLTopObjectType, ObjectMap<? extends OWLObject>>> content;
    /**
     * OWL objects cache to work with OWL-API 'signature' methods.
     * Currently, it is calculated from the {@link #content}.
     * Any direct (manual) change in the graph must also reset this cache.
     *
     * @see OWLComponentType#keys()
     * @see ObjectMap
     */
    protected final InternalCache.Loading<InternalReadGraphModel, Map<OWLComponentType, ObjectMap<OWLObject>>> components;

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
    protected final ObjectsSearcher<OWLNamedIndividual> individualSearcher = new NamedIndividualSearcher();
    protected final ObjectsSearcher<OWLDatatype> datatypeSearcher = new DatatypeSearcher();
    protected final ObjectsSearcher<OWLObjectProperty> objectPropertySearcher = new ObjectPropertySearcher();
    protected final ObjectsSearcher<OWLAnnotationProperty> annotationPropertySearcher = new AnnotationPropertySearcher();
    protected final ObjectsSearcher<OWLDataProperty> dataPropertySearcher = new DataPropertySearcher();

    InternalReadGraphModel(Graph base,
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
    }

    public ID getOntologyID() {
        // believe the last condition (which is very fast) justifies having a cache
        if (cachedID != null && getBaseGraph().contains(cachedID.asNode(), RDF.Nodes.type, OWL.Ontology.asNode())) {
            return cachedID;
        }
        return cachedID = new ID(getID());
    }

    /**
     * Returns the model's {@link InternalConfig} snapshot instance, which is an immutable object.
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
        boolean parallel = conf.concurrent();
        Map<Class<? extends OWLPrimitive>, InternalCache<?, ?>> map = external == null ? Collections.emptyMap() : external;
        return new CacheObjectFactory(df, model, map, () -> InternalCache.createBounded(parallel, size));
    }

    /**
     * Returns an {@link OntGraphModelImpl} version with search optimizations.
     * The return model must be used only to collect OWL-API stuff:
     * {@link OWLAxiom OWL Axiom}s and {@link OWLObject OWL Objects}.
     * Retrieving jena {@link OntObject Ont Object}s and {@link OntStatement Ont Statements} must be performed
     * through the main ({@link InternalReadGraphModel this}) interface.
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
     * otherwise this same {@link InternalReadGraphModel} instance with no optimizations will be returned.
     * A {@code SearchModel} contains a {@link Node}s cache inside and, therefore, may take up a lot of memory.
     *
     * @return {@link OntModel}
     * @see com.github.owlcs.ontapi.config.CacheSettings#getLoadNodesCacheSize()
     */
    protected OntGraphModelImpl createSearchModel() {
        if (!useModelSearchOptimization(getConfig())) {
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
                return InternalReadGraphModel.this.getObjectFactory();
            }
        };
    }

    /**
     * Answers {@code true} if {@link SearchModel} optimization should be used to speed up content reading.
     *
     * @param config {@link InternalConfig}, not {@code null}
     * @return {@code boolean}
     */
    protected boolean useModelSearchOptimization(InternalConfig config) {
        return config.useLoadNodesCache();
    }

    @Override
    public <N extends RDFNode> N safeFindNodeAs(Node node, Class<N> type) {
        try {
            return super.safeFindNodeAs(node, type);
        } catch (OntJenaException e) {
            return SearchModel.handleFetchNodeAsException(e, node, type, this, getConfig());
        }
    }

    /**
     * Jena model method.
     * Since in ONT-API we use another kind of lock,
     * this method is disabled (i.e. R/W Lock inside manager).
     *
     * @see com.github.sszuev.graphs.ReadWriteLockingGraph
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

    public Stream<OWLImportsDeclaration> listOWLImportDeclarations() {
        ModelObjectFactory of = getObjectFactory();
        DataFactory df = getDataFactory();
        return ModelIterators.reduce(getID().imports().map(of::toIRI).map(df::getOWLImportsDeclaration), getConfig());
    }

    public boolean isOntologyEmpty() {
        Graph bg = getBaseGraph();
        if (Graphs.isGraphMem(bg)) {
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
        return Stream.concat(listOWLAnnotations(), listOWLAxioms()).findFirst().isEmpty();
    }

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
        InternalConfig config = getConfig();
        return res.stream().map(x -> getOWLObject(x, config));
    }

    public Stream<IRI> listPunningIRIs(boolean withImports) {
        ModelObjectFactory f = getObjectFactory();
        return ambiguousEntities(withImports).map(Resource::getURI).map(f::toIRI);
    }

    public boolean containsOWLDeclaration(OWLEntity e) {
        InternalConfig config = getConfig();
        if (!config.isAllowReadDeclarations()) return false;
        if (useAxiomsSearchOptimization(config)) {
            return getBaseGraph().contains(WriteHelper.toNode(e), RDF.type.asNode(), WriteHelper.getRDFType(e).asNode());
        }
        return listOWLAxioms(OWLDeclarationAxiom.class).anyMatch(x -> x.getEntity().equals(e));
    }

    public Stream<OWLAnonymousIndividual> listOWLAnonymousIndividuals() {
        return listComponents(OWLComponentType.ANONYMOUS_INDIVIDUAL);
    }

    public Stream<OWLClassExpression> listOWLClassExpressions() {
        return ModelIterators.reduceDistinct(selectContentObjects(OWLComponentType.CLASS_EXPRESSION)
                .flatMap(OWLObject::nestedClassExpressions), getConfig());
    }

    public Stream<OWLNamedIndividual> listOWLNamedIndividuals() {
        return listComponents(OWLComponentType.NAMED_INDIVIDUAL);
    }

    public Stream<OWLClass> listOWLClasses() {
        return listComponents(OWLComponentType.CLASS);
    }

    public Stream<OWLDataProperty> listOWLDataProperties() {
        return listComponents(OWLComponentType.DATATYPE_PROPERTY);
    }

    public Stream<OWLObjectProperty> listOWLObjectProperties() {
        return listComponents(OWLComponentType.NAMED_OBJECT_PROPERTY);
    }

    public Stream<OWLAnnotationProperty> listOWLAnnotationProperties() {
        return listComponents(OWLComponentType.ANNOTATION_PROPERTY);
    }

    public Stream<OWLDatatype> listOWLDatatypes() {
        return listComponents(OWLComponentType.DATATYPE);
    }

    public boolean containsOWLEntity(OWLDatatype d) {
        return containsComponent(OWLComponentType.DATATYPE, d);
    }

    public boolean containsOWLEntity(OWLClass c) {
        return containsComponent(OWLComponentType.CLASS, c);
    }

    public boolean containsOWLEntity(OWLNamedIndividual i) {
        return containsComponent(OWLComponentType.NAMED_INDIVIDUAL, i);
    }

    public boolean containsOWLEntity(OWLDataProperty p) {
        return containsComponent(OWLComponentType.DATATYPE_PROPERTY, p);
    }

    public boolean containsOWLEntity(OWLObjectProperty p) {
        return containsComponent(OWLComponentType.NAMED_OBJECT_PROPERTY, p);
    }

    public boolean containsOWLEntity(OWLAnnotationProperty p) {
        return containsComponent(OWLComponentType.ANNOTATION_PROPERTY, p);
    }

    public Stream<OWLAnnotation> listOWLAnnotations() {
        return keys(getHeaderCache(), getConfig());
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
        // This differs from OWL-API expectations, so we need to perform traversing over the whole cache
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
                .mapWith(object -> getOWLObject(object, config));
        OWLTopObjectType key = OWLTopObjectType.get(type);
        if (key.isDistinct()) {
            return ModelIterators.reduce(res, config);
        }
        return ModelIterators.reduceDistinct(res, config);
    }

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
            return ModelIterators.reduceDistinct(res.mapWith(object -> getOWLObject(object, config)), config);
        }
        // the default way:
        if (OWLTopObjectType.ANNOTATION.hasComponent(filter)) {
            // is type of annotation -> any axiom may contain the primitive
            return ModelIterators.reduce(OWLTopObjectType.axioms().flatMap(k -> {
                ObjectMap<OWLAxiom> axioms = getContentCache(k);
                Predicate<OWLAxiom> p = k.hasComponent(filter) ? a -> true : k::hasAnnotations;
                return keys(axioms, config).filter(x -> p.test(x) && filter.contains(x, primitive));
            }), config);
        }
        // select only those container-types, that are capable to contain the primitive
        return ModelIterators.flatMap(filteredAxiomsCaches(OWLTopObjectType.axioms().filter(x -> x.hasComponent(filter))),
                k -> keys(k, config).filter(x -> filter.contains(x, primitive)), config);
    }

    /**
     * Answers {@code true} if the graph optimization for referencing axiom functionality is allowed and makes sense.
     *
     * @param type   {@link OWLComponentType}
     * @param config {@link InternalConfig}
     * @return {@code boolean}
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
            // for IRI graph optimization, it is always faster
            // for literals and anonymous individuals too
            return getOWLAxiomCount() >= threshold;
        }
        return true;
    }

    /**
     * Answers {@code true} if there is a need to use {@link ByObjectSearcher}-search optimization instead of parsing cache.
     *
     * @param config {@link InternalConfig}
     * @return {@code boolean}
     * @see #useObjectsSearchOptimization(InternalConfig)
     * @see #useReferencingAxiomsSearchOptimization(OWLComponentType, InternalConfig)
     */
    protected boolean useAxiomsSearchOptimization(InternalConfig config) {
        return !config.useContentCache() || !hasManuallyAddedAxioms();
    }

    public Stream<OWLAxiom> listOWLAxioms() {
        InternalConfig config = getConfig();
        return ModelIterators.flatMap(filteredAxiomsCaches(OWLTopObjectType.axioms()), x -> keys(x, config), config);
    }

    @SuppressWarnings("unchecked")
    public Stream<OWLLogicalAxiom> listOWLLogicalAxioms() {
        InternalConfig config = getConfig();
        return ModelIterators.flatMap(filteredAxiomsCaches(OWLTopObjectType.logical()),
                m -> (Stream<OWLLogicalAxiom>) keys(m, config), config);
    }

    public Stream<OWLAxiom> listOWLAxioms(Iterable<AxiomType<?>> filter) {
        InternalConfig config = getConfig();
        return ModelIterators.flatMap(filteredAxiomsCaches(OWLTopObjectType.axioms(filter)), x -> keys(x, config), config);
    }

    @Override
    public <A extends OWLAxiom> Stream<A> listOWLAxioms(Class<A> type) {
        return listOWLAxioms(OWLTopObjectType.get(type));
    }

    @SuppressWarnings("unchecked")
    private <A extends OWLAxiom> Stream<A> listOWLAxioms(OWLTopObjectType type) {
        return (Stream<A>) keys(getAxiomsCache(type), getConfig());
    }

    @SuppressWarnings("unchecked")
    public <A extends OWLAxiom> Stream<A> listOWLAxioms(Class<A> type, OWLObject object) {
        OWLTopObjectType key = OWLTopObjectType.get(type);
        OWLComponentType filter = OWLComponentType.get(object);
        if (!OWLTopObjectType.ANNOTATION.hasComponent(filter) && !key.hasComponent(filter)) {
            return Stream.empty();
        }
        return (Stream<A>) listOWLAxioms(key).filter(x -> filter.contains(x, object));
    }

    public <A extends OWLAxiom> Stream<A> listOWLAxioms(AxiomType<A> type) {
        return listOWLAxioms(OWLTopObjectType.get(type));
    }

    public long getOWLAxiomCount() {
        return getContentStore().entrySet().stream()
                .filter(x -> x.getKey().isAxiom())
                .mapToLong(x -> x.getValue().count()).sum();
    }

    public boolean contains(OWLAxiom a) {
        return getAxiomsCache(OWLTopObjectType.get(a.getAxiomType())).contains(a);
    }

    public boolean contains(OWLAnnotation a) {
        return getHeaderCache().contains(a);
    }

    public boolean containsIgnoreAnnotations(OWLAxiom a) {
        ObjectMap<OWLAxiom> map = getAxiomsCache(OWLTopObjectType.get(a.getAxiomType()));
        if (containsNoAnnotations(map)) {
            return map.contains(a.isAnnotated() ? a.getAxiomWithoutAnnotations() : a);
        }
        return map.contains(a) ||
                (a.isAnnotated() && map.contains(a = a.getAxiomWithoutAnnotations())) ||
                keys(map, getConfig()).anyMatch(a::equalsIgnoreAnnotations);
    }

    /**
     * Answers {@code true} if the specified {@link ObjectMap map} does not contain annotated axioms.
     *
     * @param map {@link ObjectMap}
     * @return {@code boolean}
     */
    protected boolean containsNoAnnotations(ObjectMap<? extends OWLAxiom> map) {
        return map instanceof CacheObjectMapImpl && ((CacheObjectMapImpl<?>) map).definitelyHasNoAnnotatedAxioms();
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
     * Answers whether there are manually added axioms in the cache.
     * For optimization: if the axiomatic model has not been changed after last reading,
     * then the cache is in a state strictly defined by the internal mechanisms,
     * and so there is no need to reset the cache.
     *
     * @return {@code boolean}
     */
    protected boolean hasManuallyAddedAxioms() {
        return contentCaches().anyMatch(ObjectMap::hasNew);
    }

    @Override
    public String toString() {
        return String.format("[%s]%s", InternalGraphModel.class.getSimpleName(), getID());
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
        return keys(getComponentCache(type), getConfig());
    }

    /**
     * Tests if the given component-type pair is present in the signature.
     *
     * @param type {@link OWLComponentType}, not {@code null}
     * @param o    {@link OWLObject} of the {@code type}
     * @return {@code boolean}
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
    @SuppressWarnings("unchecked")
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
        Supplier<Iterator<ONTObject<OWLObject>>> loader = () -> InternalReadGraphModel.this.listOWLObjects(type, conf);
        if (!conf.useComponentCache()) {
            ObjectsSearcher<OWLObject> searcher = getEntitySearcher(type);
            if (searcher == null) {
                return new DirectObjectMapImpl<>(loader);
            }
            return new DirectObjectMapImpl<>(loader, toFinder(searcher), toTester(searcher));
        }
        boolean parallel = conf.concurrent();
        boolean fastIterator = conf.useIteratorCache();
        return new CacheObjectMapImpl<>(loader, false, parallel, fastIterator);
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
        ObjectsSearcher<OWLObject> searcher = getEntitySearcher(type);
        if (searcher != null && useObjectsSearchOptimization(conf)) {
            return searcher.listONTObjects(model, factory, conf);
        }
        // if content cache is loaded, then its parsing is faster than graph-optimization (at least for classes)
        return selectContentObjects(type).flatMap(x -> type.select(x, model, factory)).iterator();
    }

    private ObjectsSearcher<OWLObject> getEntitySearcher(OWLComponentType type) {
        return switch (type) {
            case CLASS -> BaseSearcher.cast(classSearcher);
            case NAMED_INDIVIDUAL -> BaseSearcher.cast(individualSearcher);
            case DATATYPE -> BaseSearcher.cast(datatypeSearcher);
            case NAMED_OBJECT_PROPERTY -> BaseSearcher.cast(objectPropertySearcher);
            case ANNOTATION_PROPERTY -> BaseSearcher.cast(annotationPropertySearcher);
            case DATATYPE_PROPERTY -> BaseSearcher.cast(dataPropertySearcher);
            default -> null;
        };
    }

    /**
     * Answers {@code true} if {@link ObjectsSearcher} optimization
     * should be used to fill the component cache for the specified type.
     * <p>
     *
     * @param config {@link InternalConfig}, not {@code null}
     * @return {@code boolean}
     * @see #useAxiomsSearchOptimization(InternalConfig)
     * @see #useReferencingAxiomsSearchOptimization(OWLComponentType, InternalConfig)
     */
    protected boolean useObjectsSearchOptimization(InternalConfig config) {
        // Use the graph-way (direct searchers) instead of the content (axioms) parsing
        // in case the content-cache is disabled, OR it is empty (i.e. nothing has been loaded yet).
        // Otherwise, use content loading & parsing: it is faster in general and, usually,
        // if you need components then you need also the whole content, so it is better to load it at once.
        return !config.useContentCache() || contentCaches().noneMatch(ObjectMap::isLoaded);
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
        return selectContent(type, k -> keys(getContentCache(k), getConfig()), OWLTopObjectType::hasAnnotations);
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
     * Maps the given {@code Stream} of {@link OWLTopObjectType} to {@link ObjectMap}.
     * The input must contain only those elements
     * for which the {@link OWLTopObjectType#isAxiom()} method returns {@code true}.
     *
     * @param keys {@code Stream} of {@link OWLTopObjectType}
     * @return {@code Stream} of {@link ObjectMap} containing {@link OWLAxiom}s
     */
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
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
        ObjectsSearcher<OWLObject> searcher = key.getSearcher();
        InternalConfig conf = getConfig();
        if (!conf.useContentCache()) {
            return new DirectObjectMapImpl<>(toLoader(searcher), toFinder(searcher), toTester(searcher));
        }
        boolean parallel = conf.concurrent();
        boolean fastIterator = conf.useIteratorCache();
        boolean withMerge = !key.isDistinct();
        if (!LOGGER.isDebugEnabled()) {
            return new CacheObjectMapImpl<>(toLoader(searcher), withMerge, parallel, fastIterator);
        }
        OntID id = getID();
        return new CacheObjectMapImpl<>(toLoader(searcher), withMerge, parallel, fastIterator) {
            @Override
            protected CachedMap<OWLObject, ONTObject<OWLObject>> loadMap() {
                Instant start = Instant.now();
                CachedMap<OWLObject, ONTObject<OWLObject>> res = super.loadMap();
                Duration d = Duration.between(start, Instant.now());
                if (res.size() == 0) return res;
                // commons-lang3 is included in jena-arq (3.6.0)
                LOGGER.debug("[{}]{}:::{}{}", id,
                        StringUtils.rightPad("[" + key + "]", 42),
                        StringUtils.rightPad(String.valueOf(res.size()), 8),
                        "(" + String.format(Locale.ENGLISH, "%.3f", d.toMillis() / 1000.0) + "s)");
                return res;
            }
        };
    }

    private <X extends OWLObject> Supplier<Iterator<ONTObject<X>>> toLoader(ObjectsSearcher<X> searcher) {
        return () -> searcher.listONTObjects(getSearchModel(), getObjectFactory(), getConfig());
    }

    private <X extends OWLObject> Function<X, Optional<ONTObject<X>>> toFinder(ObjectsSearcher<X> searcher) {
        return k -> searcher.findONTObject(k, getSearchModel(), getObjectFactory(), getConfig());
    }

    private <X extends OWLObject> Predicate<X> toTester(ObjectsSearcher<X> searcher) {
        return k -> searcher.containsONTObject(k, getSearchModel(), getObjectFactory(), getConfig());
    }

    private static <X extends OWLObject> Stream<X> keys(ObjectMap<X> cache, InternalConfig config) {
        return cache.keys().map(object -> strip(object, config));
    }

    private static <X extends OWLObject> X getOWLObject(ONTObject<X> object, InternalConfig config) {
        return strip(object.getOWLObject(), config);
    }

    @SuppressWarnings("unchecked")
    private static <X extends OWLObject> X strip(X object, InternalConfig config) {
        if (config.isReadONTObjects()) {
            return object;
        }
        if (object instanceof ModelObject) {
            return (X) ((ModelObject<?>) object).eraseModel();
        }
        return object;
    }
}
