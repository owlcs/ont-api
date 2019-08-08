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
import org.apache.jena.graph.GraphEventManager;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.Lock;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntologyID;
import ru.avicomp.ontapi.internal.axioms.*;
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
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
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
 * TODO: Should it return {@link ONTObject}s, not just naked {@link OWLObject}s (see #87, #72)?
 * It seems it would be more convenient and could make this class useful not only as part of inner implementation.
 * <p>
 * Created by @szuev on 26.10.2016.
 */
@SuppressWarnings({"WeakerAccess", "unchecked"})
public class InternalModel extends OntGraphModelImpl implements OntGraphModel, HasOntologyID, HasObjectFactory, HasConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalModel.class);

    /**
     * Ontology ID cache.
     */
    protected volatile OntologyID cachedID;
    /**
     * The configuration settings to control behaviour.
     * As a container that contains an immutable snapshot, which should be reset on {@link #clearCache()}.
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
     * @see InternalConfig#useLoadObjectsCache()
     * @see CacheObjectFactory
     */
    protected final InternalCache.Loading<InternalModel, InternalObjectFactory> objectFactory;
    /**
     * A model for axiom/object's search optimizations, containing {@link Node node}s cache.
     * Any change in the base graph must also reset this cache.
     * Designed as a {@link java.lang.ref.SoftReference}
     * since it is mostly need only to optimize reading operations and may contain huge amount of objects.
     * @see InternalConfig#useLoadNodesCache()
     * @see SearchModel
     */
    protected final InternalCache.Loading<InternalModel, OntGraphModelImpl> searchModel;
    /**
     * The main cache, which contains all axioms and the ontology header.
     * It contains {@code 40} key-value pairs, {@code 39} for kinds of axioms and one for the ontology header.
     *
     * @see OWLContentType#all()
     * @see ObjectMap
     */
    protected final InternalCache.Loading<InternalModel, Map<OWLContentType, ObjectMap<? extends OWLObject>>> content;
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
        Objects.requireNonNull(factory);
        Objects.requireNonNull(config);
        this.config = InternalCache.createSingleton(x -> config.snapshot());
        this.objectFactory = InternalCache.createSoftSingleton(x -> factory.get());
        this.searchModel = InternalCache.createSoftSingleton(x -> createSearchModel());
        this.content = InternalCache.createSingleton(x -> createContentStore());
        this.components = InternalCache.createSingleton(x -> createComponentStore());
        this.directListener = createDirectListener();
        enableDirectListening();
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
        try {
            disableDirectListening();
            // these are controlled changes; do not reset the whole cache,
            // just only annotations (associated triples map is changed):
            getHeaderCache().clear();
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
        } finally {
            enableDirectListening();
        }
        if (id instanceof OntologyID) {
            this.cachedID = (OntologyID) id;
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
     * @return {@link InternalObjectFactory}
     */
    @Override
    public InternalObjectFactory getObjectFactory() {
        return objectFactory.get(this);
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
     * Creates a {@link SearchModel}, which is used as optimization while reading OWL-API objects.
     * It contains nodes cache inside, and may take up a lot of memory.
     *
     * @return {@link OntGraphModel}
     */
    protected OntGraphModelImpl createSearchModel() {
        if (!getConfig().useLoadNodesCache()) {
            return this;
        }
        return new SearchModel(getGraph(), getOntPersonality(), getConfig()) {

            @Override
            public InternalObjectFactory getObjectFactory() {
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
        InternalObjectFactory df = getObjectFactory();
        return reduce(getID().imports().map(df::toIRI).map(i -> df.getOWLDataFactory().getOWLImportsDeclaration(i)));
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
     * Tests if the given {@link OWLEntity} has the OWL declaration.
     * A builtin entity has no declarations.
     *
     * @param e {@link OWLEntity} to test
     * @return boolean
     */
    public boolean containsOWLDeclaration(OWLEntity e) {
        // do not use the commented out way since ontology can be manually assembled
        /*
        return getBaseGraph().contains(NodeFactory.createURI(e.getIRI().getIRIString()),
                RDF.type.asNode(), WriteHelper.getRDFType(e).asNode());
        */
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

    /**
     * Lists {@link OWLDeclarationAxiom Declaration Axiom}s for the specified {@link OWLEntity entity}.
     * Note: method returns non-cached axioms.
     *
     * @param e {@link OWLEntity}, not {@code null}
     * @return {@code Stream} of {@link OWLDeclarationAxiom}s
     */
    public Stream<OWLDeclarationAxiom> listOWLDeclarationAxioms(OWLEntity e) {
        if (!getConfig().isAllowReadDeclarations()) return Stream.empty();
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
        DeclarationTranslator t = (DeclarationTranslator) OWLContentType.DECLARATION.getTranslator();
        OntEntity res = m.findNodeAs(WriteHelper.toResource(e).asNode(), WriteHelper.getEntityType(e));
        if (res == null) return Stream.empty();
        InternalObjectFactory df = getObjectFactory();
        OntStatement s = res.getRoot();
        return s == null ? Stream.empty() : Stream.of(t.toAxiom(s, df, getConfig()).getObject());
    }

    /**
     * Lists {@link OWLAnnotationAssertionAxiom Annotation Assertion Axiom}s
     * with the given {@link OWLAnnotationSubject subject}.
     * Note: method returns non-cached axioms.
     *
     * @param s {@link OWLAnnotationSubject}, not {@code null}
     * @return {@code Stream} of {@link OWLAnnotationAssertionAxiom}s
     */
    public Stream<OWLAnnotationAssertionAxiom> listOWLAnnotationAssertionAxioms(OWLAnnotationSubject s) {
        if (!getConfig().isLoadAnnotationAxioms()) return Stream.empty();
        if (hasManuallyAddedAxioms()) {
            return listOWLAxioms(OWLAnnotationAssertionAxiom.class).filter(a -> s.equals(a.getSubject()));
        }
        OntGraphModelImpl m = getSearchModel();
        InternalObjectFactory df = getObjectFactory();
        AnnotationAssertionTranslator t = (AnnotationAssertionTranslator) OWLContentType.ANNOTATION_ASSERTION.getTranslator();
        ExtendedIterator<OntStatement> res = m.listLocalStatements(WriteHelper.toResource(s), null, null)
                .filterKeep(x -> t.testStatement(x, getConfig()));
        return reduce(Iter.asStream(t.translate(res, df, getConfig()).mapWith(ONTObject::getObject)));
    }

    /**
     * Lists {@link OWLSubClassOfAxiom SubClassOf Axiom}s by the given sub {@link OWLClass class}.
     * Note: method returns non-cached axioms.
     *
     * @param sub {@link OWLClass}, not {@code null}
     * @return {@code Stream} of {@link OWLSubClassOfAxiom}s
     */
    public Stream<OWLSubClassOfAxiom> listOWLSubClassOfAxioms(OWLClass sub) {
        if (hasManuallyAddedAxioms()) {
            return listOWLAxioms(OWLSubClassOfAxiom.class).filter(a -> Objects.equals(a.getSubClass(), sub));
        }
        OntGraphModelImpl m = getSearchModel();
        InternalObjectFactory df = getObjectFactory();
        SubClassOfTranslator t = (SubClassOfTranslator) OWLContentType.SUBCLASS_OF.getTranslator();
        ExtendedIterator<OntStatement> res = m.listLocalStatements(WriteHelper.toResource(sub), RDFS.subClassOf, null)
                .filterKeep(t::filter);
        return reduce(Iter.asStream(t.translate(res, df, getConfig()).mapWith(ONTObject::getObject)));
    }

    /**
     * Lists {@link OWLEquivalentClassesAxiom EquivalentClasses Axiom}s by the given {@link OWLClass class}-component.
     * Note: method returns non-cached axioms.
     *
     * @param c {@link OWLClass}, not {@code null}
     * @return {@code Stream} of {@link OWLEquivalentClassesAxiom}s
     * @see AbstractNaryTranslator#axioms(OntGraphModel)
     */
    public Stream<OWLEquivalentClassesAxiom> listOWLEquivalentClassesAxioms(OWLClass c) {
        if (hasManuallyAddedAxioms()) {
            return listOWLAxioms(OWLEquivalentClassesAxiom.class).filter(a -> a.operands().anyMatch(c::equals));
        }
        InternalObjectFactory df = getObjectFactory();
        OntGraphModelImpl m = getSearchModel();
        EquivalentClassesTranslator t = (EquivalentClassesTranslator) OWLContentType.EQUIVALENT_CLASSES.getTranslator();
        Resource r = WriteHelper.toResource(c);
        ExtendedIterator<OntStatement> res = m.listLocalStatements(r, OWL.equivalentClass, null)
                .andThen(m.listLocalStatements(null, OWL.equivalentClass, r))
                .filterKeep(s -> t.testStatement(s, getConfig()));
        return reduce(Iter.asStream(t.translate(res, df, getConfig()).mapWith(ONTObject::getObject)));
    }

    /**
     * Lists all ontology axioms.
     *
     * @return {@code Stream} of {@link OWLAxiom}s
     * @see #listOWLAnnotations()
     */
    public Stream<OWLAxiom> listOWLAxioms() {
        return flatMap(filteredAxiomsCaches(OWLContentType.axioms()), ObjectMap::keys);
    }

    /**
     * Lists all logical axioms.
     *
     * @return {@code Stream} of {@link OWLAxiom}s
     */
    public Stream<OWLLogicalAxiom> listOWLLogicalAxioms() {
        return flatMap(filteredAxiomsCaches(OWLContentType.logical()), m -> (Stream<OWLLogicalAxiom>) m.keys());
    }

    /**
     * Lists axioms for the specified types.
     *
     * @param filter a {@code Iterable} of {@link AxiomType}s
     * @return {@code Stream} of {@link OWLAxiom}s
     */
    public Stream<OWLAxiom> listOWLAxioms(Iterable<AxiomType<?>> filter) {
        return flatMap(filteredAxiomsCaches(OWLContentType.axioms(filter)), ObjectMap::keys);
    }

    /**
     * Lists all {@code OWLAxiom}s for the given {@link OWLPrimitive}
     *
     * @param primitive not {@code null}
     * @return {@code Stream} of {@link OWLAxiom}s
     */
    public Stream<OWLAxiom> listOWLAxioms(OWLPrimitive primitive) {
        OWLComponentType filter = OWLComponentType.get(primitive);
        if (OWLContentType.ANNOTATION.hasComponent(filter)) {
            // is type of annotation -> any axiom may contain the primitive
            return reduce(OWLContentType.axioms().flatMap(k -> {
                ObjectMap<OWLAxiom> axioms = getContentCache(k);
                Predicate<OWLAxiom> p = k.hasComponent(filter) ? a -> true : k::hasAnnotations;
                return axioms.keys().filter(x -> p.test(x) && filter.select(x).anyMatch(primitive::equals));
            }));
        }
        // select only those container-types, that are capable to contain the primitive
        return flatMap(filteredAxiomsCaches(OWLContentType.axioms().filter(x -> x.hasComponent(filter))),
                k -> k.keys().filter(x -> filter.select(x).anyMatch(primitive::equals)));
    }

    /**
     * Lists axioms of the given class-type.
     *
     * @param type Class
     * @param <A>  type of axiom
     * @return {@code Stream} of {@link OWLAxiom}s
     */
    @SuppressWarnings("unchecked")
    public <A extends OWLAxiom> Stream<A> listOWLAxioms(Class<A> type) {
        return (Stream<A>) getAxiomsCache(OWLContentType.get(type)).keys();
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
        OWLContentType key = OWLContentType.get(type);
        OWLComponentType filter = OWLComponentType.get(object);
        if (!OWLContentType.ANNOTATION.hasComponent(filter) && !key.hasComponent(filter)) {
            return Stream.empty();
        }
        return (Stream<A>) getAxiomsCache(key).keys().filter(x -> filter.select(x).anyMatch(object::equals));
    }

    /**
     * Lists axioms of the given axiom-type.
     *
     * @param type {@link AxiomType}
     * @param <A>  type of axiom
     * @return {@code Stream} of {@link OWLAxiom}s
     */
    @SuppressWarnings("unchecked")
    public <A extends OWLAxiom> Stream<A> listOWLAxioms(AxiomType<A> type) {
        return (Stream<A>) getAxiomsCache(OWLContentType.get(type)).keys();
    }

    /**
     * Returns the number of axioms in this ontology
     *
     * @return long
     */
    public long getOWLAxiomCount() {
        return getContentStore().entrySet().stream()
                .filter(x -> x.getKey().isAxiom())
                .mapToLong(x -> x.getValue().count()).sum();
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
     * @param stream {@code Stream} of {@link R}s
     * @param <R>    anything
     * @return {@code Stream} of {@link R}s
     * @see #flatMap(Stream, Function)
     */
    protected <R> Stream<R> reduce(Stream<R> stream) {
        InternalConfig conf = getConfig();
        // model is non-modifiable if cache is disabled
        if (!conf.parallel() || !conf.useContentCache()) {
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
    protected <R, X> Stream<R> flatMap(Stream<X> stream, Function<X, Stream<? extends R>> map) {
        InternalConfig conf = getConfig();
        if (!conf.parallel() || !conf.useContentCache()) {
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
     * @return {@code true} if the axiom is present within the model
     * @see #contains(OWLAnnotation)
     */
    public boolean contains(OWLAxiom a) {
        return getAxiomsCache(OWLContentType.get(a.getAxiomType())).contains(a);
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
        return add(OWLContentType.get(axiom.getAxiomType()), axiom);
    }

    /**
     * Adds the given annotation to the ontology header of the model.
     *
     * @param annotation {@link OWLAnnotation}
     * @return {@code true} if the {@code annotation} has been added to the graph
     * @see #add(OWLAxiom)
     */
    public boolean add(OWLAnnotation annotation) {
        return add(OWLContentType.ANNOTATION, annotation);
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
        return remove(OWLContentType.get(axiom.getAxiomType()), axiom);
    }

    /**
     * Removes the given ontology header annotation from the model.
     *
     * @param annotation {@link OWLAnnotation}
     * @return {@code true} if the {@code annotation} has been removed from the graph
     * @see #remove(OWLAxiom)
     */
    public boolean remove(OWLAnnotation annotation) {
        return remove(OWLContentType.ANNOTATION, annotation);
    }

    /**
     * Adds the specified {@code OWLObject} into the model.
     *
     * @param key       {@link OWLContentType}, not {@code null}
     * @param container either {@link OWLAxiom} or {@link OWLAnnotation},
     *                  that corresponds to the {@code key}, not {@code null}
     * @return {@code true} if the graph has been changed
     * @throws OntApiException in case the object cannot be added into model
     */
    protected boolean add(OWLContentType key, OWLObject container) throws OntApiException {
        OWLTriples.Listener listener = OWLTriples.createListener();
        GraphEventManager evm = getGraph().getEventManager();
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
        getContentCache(key).add(value);
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
     * @param key       {@link OWLContentType}, not {@code null}
     * @param container either {@link OWLAxiom} or {@link OWLAnnotation},
     *                  that corresponds to the {@code key}, not {@code null}
     * @return {@code true} if the graph has been changed
     * @see #clearComponentsCaches()
     */
    protected boolean remove(OWLContentType key, OWLObject container) {
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
            container = value.getObject();
            OntGraphModel m = toModel(value);
            // triples that are used by other content objects:
            Set<Triple> used = new HashSet<>();
            used.addAll(getUsedAxiomTriples(m, container));
            used.addAll(getUsedComponentTriples(m, container));
            Graph g = m.getBaseGraph();
            long size = g.size();
            g.find().filterDrop(used::contains).forEachRemaining(this::delete);
            boolean res = size != g.size();
            // remove related components from the objects cache
            // (even there is no graph changes)
            clearComponents(container);
            // clear search model and object factory
            clearOtherCaches();
            return res;
        } finally {
            enableDirectListening();
        }
    }

    /**
     * Returns a {@code Set} of {@link Triple}s,
     * that belongs to both the given and some other content component
     * in the form of axiom (or annotation) intersection.
     * This includes intersection in axiom definition
     * (e.g. a triple with the predicate {@code rdfs:subPropertyOf} may belong to several axiom in case of punnings)
     * and re-using OWL entity declarations.
     * Note that
     * each axiom-container {@link ONTObject} contains not only direct triples, but also all invoked declarations.
     *
     * @param m      {@link OntGraphModel} the model to traverse over
     * @param object {@link OWLObject} for which this operation is performed
     * @return {@code Set} of {@code Triple}s
     */
    protected Set<Triple> getUsedAxiomTriples(OntGraphModel m, OWLObject object) {
        InternalObjectFactory df = getObjectFactory();
        InternalConfig c = getConfig();
        Set<Triple> res = new HashSet<>();
        Iter.flatMap(OWLContentType.listAll(), k -> k.read(m, df, c)
                .filterKeep(x -> !object.equals(x.getObject()) && isUsed(k, x.getObject())))
                .forEachRemaining(x -> x.triples().forEach(res::add));
        return res;
    }

    /**
     * Answers {@code true} iff the given object-container (axiom or annotation)
     * is present in the {@link #content} cache,
     * or, if it is declaration, it is used as a part by any other content container.
     *
     * @param type   {@link OWLContentType}, the type of object
     * @param object {@link OWLObject} - the content container, axiom or annotation
     * @return boolean
     */
    protected boolean isUsed(OWLContentType type, OWLObject object) {
        ObjectMap<OWLObject> cache = getContentCache(type);
        if (cache.contains(object)) {
            return true;
        }
        if (type == OWLContentType.DECLARATION) {
            OWLEntity entity = ((OWLDeclarationAxiom) object).getEntity();
            return findUsedContentContainer(entity, object).isPresent();
        }
        return false;
    }

    /**
     * Returns a {@code Set} of {@link Triple}s,
     * that belongs to both the given and some other content component in the form of component intersection.
     * Almost any {@code OWLObject}-component - whatever named or anonymous -
     * could be shared between different content objects.
     * In this case the triples, belonging to such a component, cannot be deleted.
     *
     * @param m      {@link OntGraphModel} the model to traverse over
     * @param object {@link OWLObject} for which this operation is performed
     * @return {@code Set} of {@code Triple}s
     */
    protected Set<Triple> getUsedComponentTriples(OntGraphModel m, OWLObject object) {
        InternalObjectFactory df = getObjectFactory();
        Set<Triple> res = new HashSet<>();
        OWLComponentType.sharedComponents().forEach(type -> {
            Set<OWLObject> objects = new HashSet<>();
            Set<Triple> triples = new HashSet<>();
            type.select(m, df).forEach(x -> {
                objects.add(x.getObject());
                x.triples().forEach(triples::add);
            });
            if (objects.isEmpty()) {
                return;
            }
            selectContentContainers(type)
                    .forEach(x -> {
                        if (object.equals(x.getObject())) {
                            return;
                        }
                        if (type.components(x.getObject()).noneMatch(objects::contains)) {
                            return;
                        }
                        x.triples().filter(triples::contains).forEach(res::add);
                    });
        });
        return res;
    }

    /**
     * Represents the given container as a {@link OntGraphModel OWL Graph Model}.
     *
     * @param o {@link ONTObject}-wrapper
     * @return {@link OntGraphModel}
     */
    protected OntGraphModel toModel(ONTObject<? extends OWLObject> o) {
        Graph g = o.toGraph();
        if (LOGGER.isDebugEnabled()) {
            g.getPrefixMapping().setNsPrefixes(getNsPrefixMap());
        }
        UnionGraph u = new UnionGraph(g, false);
        u.addGraph(getGraph());
        return new OntGraphModelImpl(u, InternalModel.this.getOntPersonality()) {
            @Override
            public OntID getID() {
                return InternalModel.this.getID().inModel(this).as(OntID.class);
            }

            @Override
            public String toString() {
                return String.format("ModelFor{%s}", o.getObject());
            }
        };
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
        return getContentStore().values().stream().anyMatch(ObjectMap::hasNew);
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
     * @see #getContentCache(OWLContentType)
     * @see OWLComponentType
     */
    protected <O extends OWLObject> ObjectMap<O> getComponentCache(OWLComponentType type) {
        return (ObjectMap<O>) Objects.requireNonNull(components.get(this).get(type), "Nothing found. Type: " + type);
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
            Set<OWLObject> ignore = new HashSet<>(Arrays.asList(excludes));
            res = res.filter(x -> !ignore.contains(x));
        }
        return res.filter(x -> type.select(x).anyMatch(entity::equals)).findFirst();
    }

    /**
     * Creates a {@link ObjectMap} container for the given {@link OWLComponentType}.
     *
     * @param key {@link OWLComponentType}, not {@code null}
     * @return {@link ObjectMap}
     * @see #createContentObjectMap(OWLContentType)
     * @see OWLComponentType
     */
    protected ObjectMap<OWLObject> createComponentObjectMap(OWLComponentType key) {
        // todo: replace parsing the content cache with the direct graph reading
        InternalObjectFactory df = getObjectFactory();
        OntGraphModel m = getSearchModel();
        Supplier<Iterator<ONTObject<OWLObject>>> loader = () -> selectContentObjects(key)
                .flatMap(x -> key.select(x, m, df)).iterator();
        InternalConfig conf = getConfig();
        if (!conf.useComponentCache()) {
            // todo: need a straight way to find ONTObject that present in the graph,
            //  the default one is extremely inefficient
            return new DirectObjectMapImpl<>(loader);
        }
        boolean parallel = conf.parallel();
        boolean fastIterator = conf.useIteratorCache();
        return new CacheObjectMapImpl<>(loader, false, parallel, fastIterator);
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
        return selectContent(type, k -> getContentCache(k).values(), (k, x) -> k.hasAnnotations(x.getObject()));
    }

    /**
     * Selects the objects from the {@link #content} cache, that may hold a component of the given {@code type}.
     *
     * @param type {@link OWLComponentType}, not {@code null}
     * @return {@code Stream} of {@link OWLObject} - containers from the {@link #content} cache
     */
    protected Stream<OWLObject> selectContentObjects(OWLComponentType type) {
        return selectContent(type, k -> getContentCache(k).keys(), OWLContentType::hasAnnotations);
    }

    /**
     * Selects the objects from the {@link #content} cache, that may hold a component of the given {@code type}.
     *
     * @param type            {@link OWLComponentType}, not {@code null}
     * @param toStream        a {@code Function} to provide {@code Stream} of {@link R}
     *                        for a given {@link OWLContentType}, not {@code null}
     * @param withAnnotations a {@code BiPredicate} to select only those {@link R},
     *                        which have OWL annotations, not {@code null}
     * @param <R>             anything
     * @return {@code Stream} of {@link R} - containers from the {@link #content} cache
     */
    protected <R> Stream<R> selectContent(OWLComponentType type,
                                          Function<OWLContentType, Stream<R>> toStream,
                                          BiPredicate<OWLContentType, R> withAnnotations) {
        // todo: consider the case when there is no bulk annotations at all ?
        if (!OWLContentType.ANNOTATION.hasComponent(type)) {
            // select only those axiom types which are allowed to contain the component type
            return OWLContentType.all().filter(k -> k.hasComponent(type)).flatMap(toStream);
        }
        // any axiom or header annotation may contain this component
        return OWLContentType.all().flatMap(k -> {
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
            if (!type.components(container).findFirst().isPresent()) return;
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
        InternalObjectFactory df = getObjectFactory();
        OntGraphModel m = getSearchModel();
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
        getContentStore().values().forEach(ObjectMap::load);
    }

    /**
     * Maps the given {@code Stream} of {@link OWLContentType} to {@link ObjectMap}.
     * The input must contain only those elements
     * for which the {@link OWLContentType#isAxiom()} method returns {@code true}.
     *
     * @param keys {@code Stream} of {@link OWLContentType}
     * @return {@code Stream} of {@link ObjectMap} containing {@link OWLAxiom}s
     */
    protected Stream<ObjectMap<? extends OWLAxiom>> filteredAxiomsCaches(Stream<OWLContentType> keys) {
        Map<OWLContentType, ObjectMap<? extends OWLObject>> map = getContentStore();
        return keys.map(x -> (ObjectMap<? extends OWLAxiom>) map.get(x));
    }

    /**
     * Gets the {@link ObjectMap} for the given {@link OWLContentType}.
     * The {@link OWLContentType#isAxiom()} method for the input must return {@code true}.
     *
     * @param key {@link OWLContentType}, not {@code null}
     * @return {@link ObjectMap}
     */
    protected ObjectMap<OWLAxiom> getAxiomsCache(OWLContentType key) {
        return getContentCache(key);
    }

    /**
     * Gets an ontology header content cache-store.
     *
     * @return {@link ObjectMap}
     */
    protected ObjectMap<OWLAnnotation> getHeaderCache() {
        return getContentCache(OWLContentType.ANNOTATION);
    }

    /**
     * Gets an ontology content {@code ObjectMap}-cache.
     *
     * @param key {@link OWLContentType}, not {@code null}
     * @param <X> either {@link OWLAxiom} or {@link OWLAnnotation}
     * @return {@link ObjectMap}
     * @see #getComponentCache(OWLComponentType)
     */
    protected <X extends OWLObject> ObjectMap<X> getContentCache(OWLContentType key) {
        return (ObjectMap<X>) getContentStore().get(key);
    }

    /**
     * Gets a content store {@code Map}.
     *
     * @return {@link Map}
     */
    protected Map<OWLContentType, ObjectMap<? extends OWLObject>> getContentStore() {
        return content.get(this);
    }

    /**
     * Creates a content store {@code Map}.
     *
     * @return {@link Map}
     * @see #createComponentStore()
     */
    protected Map<OWLContentType, ObjectMap<? extends OWLObject>> createContentStore() {
        return createMapStore(OWLContentType.class, OWLContentType.all(), this::createContentObjectMap);
    }

    /**
     * Creates a {@link ObjectMap} container for the given {@link OWLContentType}.
     * @param key {@link OWLContentType}
     * @return {@link ObjectMap}
     * @see #createComponentObjectMap(OWLComponentType)
     */
    protected ObjectMap<OWLObject> createContentObjectMap(OWLContentType key) {
        InternalObjectFactory df = getObjectFactory();
        OntGraphModel m = getSearchModel();
        Supplier<Iterator<ONTObject<OWLObject>>> loader =
                () -> (Iterator<ONTObject<OWLObject>>) key.read(m, df, getConfig());
        InternalConfig conf = getConfig();
        if (!conf.useContentCache()) {
            // todo: need a straight way to find ONTObject by OWLObject,
            //  the default one is extremely inefficient
            return new DirectObjectMapImpl<>(loader);
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
            protected CachedMap loadMap() {
                Instant start = Instant.now();
                CachedMap res = super.loadMap();
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
