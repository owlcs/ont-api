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
import com.github.owlcs.ontapi.Ontology;
import com.github.sszuev.jena.ontapi.UnionGraph;
import com.github.sszuev.jena.ontapi.common.OntPersonality;
import com.github.sszuev.jena.ontapi.impl.GraphListenerBase;
import com.github.sszuev.jena.ontapi.impl.OntGraphModelImpl;
import com.github.sszuev.jena.ontapi.impl.UnionGraphImpl;
import com.github.sszuev.jena.ontapi.model.OntID;
import com.github.sszuev.jena.ontapi.model.OntModel;
import com.github.sszuev.jena.ontapi.model.OntObject;
import com.github.sszuev.jena.ontapi.utils.Graphs;
import com.github.sszuev.jena.ontapi.utils.Iterators;
import javax.annotation.Nonnull;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphEventManager;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLPrimitive;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A Buffer Graph OWL model, which supports both listing OWL-API objects (OWL Axioms, Entities and Annotations)
 * and Jena interfaces (through the {@link OntModel} view of RDF Graph).
 * <p>
 * It is an analogue of <a href="https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/Internals.java">uk.ac.manchester.cs.owl.owlapi.Internals</a>.
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
 * (see {@link AxiomTranslator#get(AxiomType)}).
 * <p>
 * TODO: Should it return {@link ONTObject}s, not just naked {@link OWLObject}s (see #87, #72)?
 * It seems it would be more convenient and could make this class useful not only as part of inner implementation.
 * <p>
 * Created by @ssz on 26.10.2016.
 */
public class InternalModelImpl extends InternalReadModel implements InternalModel {

    /**
     * The direct listener, it monitors changes that occur through the main (Jena) interface.
     */
    protected final DirectListener directListener;

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
    public InternalModelImpl(Graph base,
                             OntPersonality personality,
                             InternalConfig config,
                             DataFactory dataFactory,
                             Map<Class<? extends OWLPrimitive>, InternalCache<?, ?>> fromManager) {
        super(base, personality, config, dataFactory, fromManager);
        this.directListener = createDirectListener();
        enableDirectListening();
    }

    @Override
    public UnionGraph getUnionGraph() {
        return super.getUnionGraph();
    }

    @Override
    public void setOntologyID(OWLOntologyID id) throws IllegalArgumentException {
        this.cachedID = null;
        try {
            disableDirectListening();
            // these are controlled changes; do not reset the whole cache,
            // just only annotations (an associated triples map is changed):
            getHeaderCache().clear();
            if (Objects.requireNonNull(id, "Null id").isAnonymous()) {
                OntID res;
                if (id instanceof ID) {
                    res = getNodeAs(Graphs.makeOntologyHeaderNode(getBaseGraph(), ((ID) id).asNode()), OntID.class);
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

    @Override
    public boolean add(OWLAxiom axiom) {
        return add(OWLTopObjectType.get(axiom.getAxiomType()), axiom);
    }

    @Override
    public boolean add(OWLAnnotation annotation) {
        return add(OWLTopObjectType.ANNOTATION, annotation);
    }

    @Override
    public boolean remove(OWLAxiom axiom) {
        return remove(OWLTopObjectType.get(axiom.getAxiomType()), axiom);
    }

    @Override
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
     * @throws OntApiException in case the object cannot be added into the model
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
        Iterators.flatMap(OWLTopObjectType.listAll(), type -> type.getSearcher().listONTObjects(model, f, c)
                        .filterKeep(x -> {
                            OWLObject obj = x.getOWLObject();
                            if (type != OWLTopObjectType.DECLARATION && container.equals(obj)) return false;
                            if (InternalModelImpl.this.getContentCache(type).contains(obj)) {
                                return true;
                            }
                            if (type == OWLTopObjectType.DECLARATION) {
                                OWLEntity entity = ((OWLDeclarationAxiom) obj).getEntity();
                                return InternalModelImpl.this.findUsedContentContainer(entity, obj).isPresent();
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
        UnionGraph u = new UnionGraphImpl(g, false);
        u.addSubGraph(getGraph());
        class ObjectModel extends OntGraphModelImpl implements HasConfig, HasObjectFactory {
            public ObjectModel(Graph g) {
                super(g, InternalModelImpl.this.getOntPersonality());
            }

            @Override
            public OntID getID() {
                return InternalModelImpl.this.getID().inModel(this).as(OntID.class);
            }

            @Override
            public String toString() {
                return String.format("ModelFor{%s}", o.getOWLObject());
            }

            @Override
            public InternalConfig getConfig() {
                return InternalModelImpl.this.getConfig();
            }

            @Override
            @Nonnull
            public ModelObjectFactory getObjectFactory() {
                return new InternalObjectFactory(InternalModelImpl.this.getDataFactory(), () -> ObjectModel.this);
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
        getBaseGraph().delete(triple);
    }

    /**
     * The overridden jena method.
     * Makes this ontology empty given its caches.
     *
     * @return {@link Model}
     */
    @Override
    public InternalModelImpl removeAll() {
        clearCache();
        super.removeAll();
        return this;
    }

    @Override
    public void clearCacheIfNeeded() {
        // todo: how can we diagnose only those caches, which are really affected?
        if (hasManuallyAddedAxioms()) {
            clearCache();
        }
    }

    @Override
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
            if (type.select(container).findFirst().isEmpty()) return;
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

    @Override
    public void forceLoad() {
        contentCaches().forEach(ObjectMap::load);
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
        protected void addTripleEvent(Graph g, Triple t) {
            // we don't know which axiom would own this triple, so we clear the whole cache.
            invalidate();
        }

        @Override
        protected void deleteTripleEvent(Graph g, Triple t) {
            // Although it is possible to detect only those cache elements,
            // that are really affected by deleting the triple,
            // but such a calculation would be rather too complicated and time-consuming and (therefore) possibly buggy.
            // So it seems better to just release all caches.
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
