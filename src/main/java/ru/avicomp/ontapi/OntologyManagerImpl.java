/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi;

import com.google.common.collect.Multimap;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.JenaException;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.semanticweb.owlapi.util.CollectionFactory;
import org.semanticweb.owlapi.util.PriorityCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.config.OntWriterConfiguration;
import ru.avicomp.ontapi.internal.ConfigProvider;
import ru.avicomp.ontapi.internal.InternalModel;
import ru.avicomp.ontapi.internal.InternalModelHolder;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.impl.configuration.OntPersonality;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Models;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentPriorityCollection;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * ONT-API Ontology Manager implementation ({@link OntologyManager}).
 * Currently it is mostly copy-paste of {@link OWLOntologyManagerImpl}.
 * <p>
 * Created by @szuev on 03.10.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntologyManagerImpl implements OntologyManager, OWLOntologyFactory.OWLOntologyCreationHandler, Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(OWLOntologyManagerImpl.class);
    // listeners:
    protected final ListenersHolder listeners = new ListenersHolder();
    // configs:
    protected OntConfig configProvider;
    protected transient OntLoaderConfiguration loaderConfig;
    protected transient OntWriterConfiguration writerConfig;
    // should contain only OntologyModel.Factory implementations:
    protected final PriorityCollection<OWLOntologyFactory> ontologyFactories;
    protected final PriorityCollection<OWLOntologyIRIMapper> documentMappers;
    // new (sine 1.0.1):
    protected final Deque<DocumentSourceMapping> documentSourceMappers;
    // alternative (to jena way) factories to load and save models:
    protected final PriorityCollection<OWLParserFactory> parserFactories;
    protected final PriorityCollection<OWLStorerFactory> ontologyStorers;
    // primary parameters:
    protected final ReadWriteLock lock;
    protected final OWLDataFactory dataFactory;
    // the collection of ontologies:
    protected final OntologyCollection content;

    public OntologyManagerImpl(OWLDataFactory dataFactory, ReadWriteLock readWriteLock, PriorityCollectionSorting sorting) {
        this.dataFactory = OntApiException.notNull(dataFactory, "Null OWLDataFactory specified.");
        this.lock = readWriteLock == null ? new NoOpReadWriteLock() : readWriteLock;
        documentMappers = new ConcurrentPriorityCollection<>(lock, sorting);
        documentSourceMappers = new LinkedList<>();
        ontologyFactories = new ConcurrentPriorityCollection<>(lock, sorting);
        parserFactories = new ConcurrentPriorityCollection<>(lock, sorting);
        ontologyStorers = new ConcurrentPriorityCollection<>(lock, sorting);
        configProvider = new OntConfig();
        content = new OntologyCollection(isConcurrent() ? CollectionFactory.createSyncSet() : CollectionFactory.createSet());
    }

    public OntologyManagerImpl(OWLDataFactory dataFactory, ReadWriteLock readWriteLock) {
        this(dataFactory, readWriteLock, PriorityCollectionSorting.ON_SET_INJECTION_ONLY);
        OntologyManager.Factory core = new OntFactoryImpl();
        getOntologyFactories().add(core);
    }

    public boolean isConcurrent() {
        return !NoOpReadWriteLock.class.isInstance(lock);
    }

    @Nonnull
    public ReadWriteLock getLock() {
        return lock;
    }

    @Override
    public OWLDataFactory getOWLDataFactory() {
        return dataFactory;
    }

    /**
     * @return {@link OntConfig}
     * @see OWLOntologyManagerImpl#getOntologyConfigurator()
     */
    @Override
    public OntConfig getOntologyConfigurator() {
        getLock().readLock().lock();
        try {
            return configProvider;
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param conf {@link OntologyConfigurator}
     * @see OWLOntologyManagerImpl#setOntologyConfigurator(OntologyConfigurator)
     */
    @Override
    public void setOntologyConfigurator(@Nonnull OntologyConfigurator conf) {
        getLock().writeLock().lock();
        try {
            configProvider = OntFactoryImpl.asONT(conf);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * Sets {@link OntLoaderConfiguration} config to the manager.
     *
     * @param conf {@link OWLOntologyLoaderConfiguration}
     * @see OWLOntologyManagerImpl#setOntologyLoaderConfiguration(OWLOntologyLoaderConfiguration)
     */
    @Override
    public void setOntologyLoaderConfiguration(@Nullable OWLOntologyLoaderConfiguration conf) {
        getLock().writeLock().lock();
        try {
            OntLoaderConfiguration config = OntFactoryImpl.asONT(conf);
            if (Objects.equals(loaderConfig, config)) return;
            loaderConfig = config;
            content.values() // todo: need reset cache only if there is changes in the settings related to the axioms.
                    .filter(i -> Objects.equals(loaderConfig, i.getModelConfig().loaderConfig()))
                    .map(OntInfo::get).forEach(OntologyModel::clearCache);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @return {@link OntLoaderConfiguration}
     * @see OWLOntologyManagerImpl#getOntologyLoaderConfiguration()
     */
    @Override
    @Nonnull
    public OntLoaderConfiguration getOntologyLoaderConfiguration() {
        getLock().readLock().lock();
        try {
            return loaderConfig == null ? loaderConfig = configProvider.buildLoaderConfiguration() : loaderConfig;
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * Sets {@link OWLOntologyWriterConfiguration} config to the manager and also passes it inside interior models.
     *
     * @param conf {@link OWLOntologyWriterConfiguration}
     * @see OWLOntologyManagerImpl#setOntologyWriterConfiguration(OWLOntologyWriterConfiguration)
     */
    @Override
    public void setOntologyWriterConfiguration(@Nullable OWLOntologyWriterConfiguration conf) {
        getLock().writeLock().lock();
        try {
            if (Objects.equals(writerConfig, conf)) return;
            writerConfig = OntFactoryImpl.asONT(conf);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @return {@link OntWriterConfiguration}
     * @see OWLOntologyManagerImpl#getOntologyWriterConfiguration()
     */
    @Override
    @Nonnull
    public OntWriterConfiguration getOntologyWriterConfiguration() {
        getLock().readLock().lock();
        try {
            return writerConfig == null ? writerConfig = configProvider.buildWriterConfiguration() : writerConfig;
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * todo: wrap original OWL-API factory with our {@link OntologyManager.Factory} to produce {@link OntologyModel}
     *
     * @param factories Set of {@link OWLOntologyFactory}
     * @see OWLOntologyManagerImpl#setOntologyFactories(Set)
     */
    @Override
    public void setOntologyFactories(@Nonnull Set<OWLOntologyFactory> factories) {
        ontologyFactories.set(factories);
    }

    /**
     * @return {@link PriorityCollection} of {@link OWLOntologyFactory}
     * @see OWLOntologyManagerImpl#getOntologyFactories()
     */
    @Override
    public PriorityCollection<OWLOntologyFactory> getOntologyFactories() {
        return ontologyFactories;
    }

    /**
     * @param storer {@link OWLStorerFactory}
     * @see OWLOntologyManagerImpl#addOntologyStorer(OWLStorerFactory)
     */
    @Override
    public void addOntologyStorer(@Nonnull OWLStorerFactory storer) {
        ontologyStorers.add(storer);
    }

    /**
     * @return {@link PriorityCollection} of {@link OWLStorerFactory}
     * @see OWLOntologyManagerImpl#getOntologyStorers()
     */
    @Override
    public PriorityCollection<OWLStorerFactory> getOntologyStorers() {
        return ontologyStorers;
    }

    /**
     * @param storers Set of {@link OWLStorerFactory}
     * @see OWLOntologyManagerImpl#setOntologyStorers(Set)
     */
    @Override
    public void setOntologyStorers(@Nonnull Set<OWLStorerFactory> storers) {
        ontologyStorers.set(storers);
    }

    /**
     * @param storer {@link OWLStorerFactory}
     * @see OWLOntologyManagerImpl#removeOntologyStorer(OWLStorerFactory)
     */
    @Override
    public void removeOntologyStorer(@Nonnull OWLStorerFactory storer) {
        ontologyStorers.remove(storer);
    }

    /**
     * @see OWLOntologyManagerImpl#clearOntologyStorers()
     */
    @Override
    public void clearOntologyStorers() {
        ontologyStorers.clear();
    }

    /**
     * @return {@link PriorityCollection} of {@link OWLParserFactory}
     * @see OWLOntologyManagerImpl#getOntologyParsers()
     */
    @Override
    public PriorityCollection<OWLParserFactory> getOntologyParsers() {
        return parserFactories;
    }

    /**
     * @param parsers Set of {@link OWLParserFactory}
     * @see OWLOntologyManagerImpl#setOntologyParsers(Set)
     */
    @Override
    public void setOntologyParsers(@Nonnull Set<OWLParserFactory> parsers) {
        parserFactories.set(parsers);
    }

    /**
     * @return {@link PriorityCollection} of {@link OWLOntologyIRIMapper}
     * @see OWLOntologyManagerImpl#getIRIMappers()
     */
    @Override
    public PriorityCollection<OWLOntologyIRIMapper> getIRIMappers() {
        return documentMappers;
    }

    /**
     * @param mappers Set of {@link OWLOntologyIRIMapper}
     * @see OWLOntologyManagerImpl#setIRIMappers(Set)
     */
    @Override
    public void setIRIMappers(@Nonnull Set<OWLOntologyIRIMapper> mappers) {
        documentMappers.set(mappers);
    }

    /**
     * @param mapper {@link DocumentSourceMapping}
     */
    @Override
    public void addDocumentSourceMapper(DocumentSourceMapping mapper) {
        documentSourceMappers.addFirst(mapper);
    }

    @Override
    public void removeDocumentSourceMapper(DocumentSourceMapping mapper) {
        documentSourceMappers.remove(mapper);
    }

    @Override
    public Stream<DocumentSourceMapping> documentSourceMappers() {
        return documentSourceMappers.stream();
    }

    /**
     * @param mapper {@link OWLOntologyIRIMapper}
     * @see OWLOntologyManagerImpl#addIRIMapper(OWLOntologyIRIMapper)
     */
    @Override
    public void addIRIMapper(@Nonnull OWLOntologyIRIMapper mapper) {
        documentMappers.add(mapper);
    }

    /**
     * @param mapper {@link OWLOntologyIRIMapper}
     * @see OWLOntologyManagerImpl#removeIRIMapper(OWLOntologyIRIMapper)
     */
    @Override
    public void removeIRIMapper(@Nonnull OWLOntologyIRIMapper mapper) {
        documentMappers.remove(mapper);
    }

    /**
     * @see OWLOntologyManagerImpl#clearIRIMappers()
     */
    @Override
    public void clearIRIMappers() {
        documentMappers.clear();
    }

    /**
     * @param strategy {@link OWLOntologyChangeBroadcastStrategy}
     * @see OWLOntologyManagerImpl#setDefaultChangeBroadcastStrategy(OWLOntologyChangeBroadcastStrategy)
     */
    @Override
    public void setDefaultChangeBroadcastStrategy(@Nonnull OWLOntologyChangeBroadcastStrategy strategy) {
        getLock().writeLock().lock();
        try {
            listeners.setDefaultChangeBroadcastStrategy(strategy);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangeListener}
     * @see OWLOntologyManagerImpl#addOntologyChangeListener(OWLOntologyChangeListener)
     */
    @Override
    public void addOntologyChangeListener(@Nonnull OWLOntologyChangeListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.addOntologyChangeListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }

    }

    /**
     * @param listener {@link OWLOntologyChangeListener}
     * @param strategy {@link OWLOntologyChangeBroadcastStrategy}
     * @see OWLOntologyManagerImpl#addOntologyChangeListener(OWLOntologyChangeListener, OWLOntologyChangeBroadcastStrategy)
     */
    @Override
    public void addOntologyChangeListener(@Nonnull OWLOntologyChangeListener listener, @Nonnull OWLOntologyChangeBroadcastStrategy strategy) {
        getLock().writeLock().lock();
        try {
            listeners.addOntologyChangeListener(listener, strategy);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangeListener}
     * @see OWLOntologyManagerImpl#removeOntologyChangeListener(OWLOntologyChangeListener)
     */
    @Override
    public void removeOntologyChangeListener(@Nonnull OWLOntologyChangeListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.removeOntologyChangeListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link ImpendingOWLOntologyChangeListener}
     * @see OWLOntologyManagerImpl#addImpendingOntologyChangeListener(ImpendingOWLOntologyChangeListener)
     */
    @Override
    public void addImpendingOntologyChangeListener(@Nonnull ImpendingOWLOntologyChangeListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.addImpendingOntologyChangeListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link ImpendingOWLOntologyChangeListener}
     * @see OWLOntologyManagerImpl#removeImpendingOntologyChangeListener(ImpendingOWLOntologyChangeListener)
     */
    @Override
    public void removeImpendingOntologyChangeListener(@Nonnull ImpendingOWLOntologyChangeListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.removeImpendingOntologyChangeListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangesVetoedListener}
     * @see OWLOntologyManagerImpl#addOntologyChangesVetoedListener(OWLOntologyChangesVetoedListener)
     */
    @Override
    public void addOntologyChangesVetoedListener(@Nonnull OWLOntologyChangesVetoedListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.addOntologyChangesVetoedListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangesVetoedListener}
     * @see OWLOntologyManagerImpl#removeOntologyChangesVetoedListener(OWLOntologyChangesVetoedListener)
     */
    @Override
    public void removeOntologyChangesVetoedListener(@Nonnull OWLOntologyChangesVetoedListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.removeOntologyChangesVetoedListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link MissingImportListener}
     * @see OWLOntologyManagerImpl#addMissingImportListener(MissingImportListener)
     */
    @Override
    public void addMissingImportListener(@Nonnull MissingImportListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.addMissingImportListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link MissingImportListener}
     * @see OWLOntologyManagerImpl#removeMissingImportListener(MissingImportListener)
     */
    @Override
    public void removeMissingImportListener(@Nonnull MissingImportListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.removeMissingImportListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyLoaderListener}
     * @see OWLOntologyManagerImpl#addOntologyLoaderListener(OWLOntologyLoaderListener)
     */
    @Override
    public void addOntologyLoaderListener(@Nonnull OWLOntologyLoaderListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.addOntologyLoaderListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyLoaderListener}
     * @see OWLOntologyManagerImpl#removeOntologyLoaderListener(OWLOntologyLoaderListener)
     */
    @Override
    public void removeOntologyLoaderListener(@Nonnull OWLOntologyLoaderListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.removeOntologyLoaderListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangeProgressListener}
     * @see OWLOntologyManagerImpl#addOntologyChangeProgessListener(OWLOntologyChangeProgressListener)
     */
    @Override
    public void addOntologyChangeProgessListener(@Nonnull OWLOntologyChangeProgressListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.addOntologyChangeProgessListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangeProgressListener}
     * @see OWLOntologyManagerImpl#removeOntologyChangeProgessListener(OWLOntologyChangeProgressListener)
     */
    @Override
    public void removeOntologyChangeProgessListener(@Nonnull OWLOntologyChangeProgressListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.removeOntologyChangeProgessListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param id {@link OWLOntologyID}
     * @return {@link OntologyModel}
     * @see OWLOntologyManagerImpl#createOntology(OWLOntologyID)
     */
    @Override
    public OntologyModel createOntology(@Nonnull OWLOntologyID id) {
        getLock().writeLock().lock();
        try {
            Optional<OntInfo> ont = content.get(id);
            if (ont.isPresent()) {
                throw new OWLOntologyAlreadyExistsException(id);
            }
            IRI doc = computeDocumentIRI(id);
            if (doc == null) {
                throw new OWLOntologyCreationException("Can't compute document iri from id " + id);
            }
            if (content.values().anyMatch(o -> Objects.equals(o.getDocumentIRI(), doc))) {
                throw new OWLOntologyDocumentAlreadyExistsException(doc);
            }
            for (OWLOntologyFactory factory : ontologyFactories) {
                // we are working only with the one factory which returns OntologyModel, not OWLOntology,
                // todo: reminder: other factories should be wrapped and turn into OntologyManager.Factory.
                if (factory.canCreateFromDocumentIRI(doc)) {
                    OntologyModel res = (OntologyModel) factory.createOWLOntology(this, id, doc, this);
                    content.get(id).orElseThrow(() -> new UnknownOWLOntologyException(id)).addDocumentIRI(doc);
                    return res;
                }
            }
            throw new OWLOntologyFactoryNotFoundException(doc);
        } catch (OWLOntologyCreationException e) {
            throw new OntApiException("Unable to create ontology " + id, e);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param iri                   the IRI of Ontology
     * @param ontologies            Stream of {@link OWLOntology}s
     * @param copyLogicalAxiomsOnly boolean
     * @return {@link OntologyModel}
     * @see OWLOntologyManagerImpl#createOntology(IRI, Stream, boolean)
     */
    @Override
    public OntologyModel createOntology(@Nonnull IRI iri, @Nonnull Stream<OWLOntology> ontologies, boolean copyLogicalAxiomsOnly) {
        getLock().writeLock().lock();
        try {
            OWLOntologyID id = new OWLOntologyID(Optional.of(iri), Optional.empty());
            if (contains(iri)) {
                throw new OWLOntologyAlreadyExistsException(id);
            }
            OntologyModel res = createOntology(iri);
            addAxioms(res, ontologies.flatMap(o -> copyLogicalAxiomsOnly ? o.logicalAxioms() : o.axioms()));
            return res;
        } catch (OWLOntologyAlreadyExistsException e) {
            throw new OntApiException("Unable to create ontology " + iri, e);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param axioms Stream of {@link OWLAxiom}s
     * @param iri    {@link IRI}
     * @return {@link OntologyModel}
     * @see OWLOntologyManagerImpl#createOntology(Stream, IRI)
     */
    @Override
    public OntologyModel createOntology(@Nonnull Stream<OWLAxiom> axioms, @Nonnull IRI iri) {
        getLock().writeLock().lock();
        try {
            OWLOntologyID id = new OWLOntologyID(Optional.of(iri), Optional.empty());
            if (contains(iri)) {
                throw new OWLOntologyAlreadyExistsException(id);
            }
            OntologyModel ont = createOntology(iri);
            addAxioms(ont, axioms);
            return ont;
        } catch (OWLOntologyAlreadyExistsException e) {
            throw new OntApiException("Unable to create ontology " + iri, e);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param graph {@link Graph}
     * @return {@link OntologyModel}
     * @since 1.0.1
     */
    @Override
    public OntologyModel addOntology(@Nonnull Graph graph) {
        return addOntology(graph, false);
    }

    /**
     * @param graph             {@link Graph}
     * @param doTransformation, boolean. to turn off/on graph transformation mechanism.
     * @return {@link OntologyModel}
     * @since 1.0.1
     */
    public OntologyModel addOntology(@Nonnull Graph graph, boolean doTransformation) {
        getLock().writeLock().lock();
        try {
            OWLOntologyID id = OntGraphUtils.ontologyID(graph).orElse(null);
            Map<OWLOntologyID, Graph> graphs = OntGraphUtils.toGraphMap(graph);
            DocumentSourceMapping mapping = _id ->
                    graphs.entrySet()
                            .stream()
                            .filter(e -> Objects.equals(_id, e.getKey()))
                            .map(e -> new OntGraphDocumentSource() {
                                @Override
                                public Graph getGraph() {
                                    return e.getValue();
                                }

                                @Override
                                public IRI getDocumentIRI() {
                                    return IRI.create("graph:", _id == null ? "anonymous" : _id.getOntologyIRI().map(IRI::getIRIString).orElse("null"));
                                }
                            }).findFirst().orElse(null);
            try {
                addDocumentSourceMapper(mapping);
                return loadOntologyFromOntologyDocument(mapping.map(id), getOntologyLoaderConfiguration().setPerformTransformation(doTransformation));
            } finally {
                removeDocumentSourceMapper(mapping);
            }
        } catch (OWLOntologyCreationException e) {
            throw new OntApiException("Unable put graph to manager", e);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * Makes new detached ontology model based on the specified graph.
     *
     * @param graph  {@link Graph} the graph
     * @param config {@link OntLoaderConfiguration} the config
     * @return {@link OntologyModel} the model
     * @since 1.0.1
     */
    public OntologyModel newOntologyModel(@Nonnull Graph graph, @Nullable OntLoaderConfiguration config) {
        ModelConfig modelConfig = createModelConfig();
        if (config != null)
            modelConfig.setLoaderConf(config);
        OntologyModelImpl ont = new OntologyModelImpl(graph, modelConfig);
        return isConcurrent() ? ont.asConcurrent() : ont;
    }

    /**
     * @param id {@link OWLOntologyID}
     * @return {@link IRI}
     * @see OWLOntologyManagerImpl#computeDocumentIRI(OWLOntologyID)
     */
    @Nullable
    protected IRI computeDocumentIRI(OWLOntologyID id) {
        IRI documentIRI = getDocumentIRIFromMappers(id);
        if (documentIRI == null) {
            if (!id.isAnonymous()) {
                documentIRI = id.getDefaultDocumentIRI().orElse(null);
            } else {
                documentIRI = IRI.generateDocumentIRI();
            }
        }
        return documentIRI;
    }

    /**
     * Uses the mapper mechanism to obtain an ontology document IRI from an ontology IRI.
     *
     * @param id The ontology ID for which a document IRI is to be retrieved
     * @return The document IRI that corresponds to the ontology IRI, or null if no physical URI can be found.
     * @see OWLOntologyManagerImpl#getDocumentIRIFromMappers(OWLOntologyID)
     */
    @Nullable
    protected IRI getDocumentIRIFromMappers(OWLOntologyID id) {
        IRI defaultIRI = id.getDefaultDocumentIRI().orElse(null);
        if (defaultIRI == null) {
            return null;
        }
        for (OWLOntologyIRIMapper mapper : documentMappers) {
            IRI documentIRI = mapper.getDocumentIRI(defaultIRI);
            if (documentIRI != null) {
                return documentIRI;
            }
        }
        return defaultIRI;
    }

    /**
     * the difference: in case there are many ontologies with the same IRI it chooses the first match on version or ontology iri, not any.
     *
     * @param iri {@link IRI}
     * @return {@link OntologyModel}
     * @see OWLOntologyManagerImpl#getOntology(IRI)
     */
    @Override
    @Nullable
    public OntologyModel getOntology(@Nonnull IRI iri) {
        getLock().readLock().lock();
        try {
            OWLOntologyID id = new OWLOntologyID(Optional.of(iri), Optional.empty());
            Optional<OntInfo> res = content.get(id);
            if (!res.isPresent()) {
                res = content.values().filter(e -> e.id().match(iri)).findFirst();
            }
            return res.map(OntInfo::get).orElse(null);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * the difference: in case there are many ontologies with the same ID it chooses first on the iri ignoring version,
     * while original method chooses any.
     *
     * @param id {@link OWLOntologyID}
     * @return {@link OntologyModel}
     * @see OWLOntologyManagerImpl#getOntology(OWLOntologyID)
     */
    @Override
    public OntologyModel getOntology(@Nonnull OWLOntologyID id) {
        getLock().readLock().lock();
        try {
            Optional<OntInfo> res = content.get(id);
            if (!res.isPresent() && !id.isAnonymous()) {
                IRI iri = id.getOntologyIRI().orElseThrow(() -> new IllegalStateException("Should never happen."));
                res = content.values().filter(e -> e.id().matchOntology(iri)).findFirst();
            }
            return res.map(OntInfo::get).orElse(null);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param iri {@link IRI}
     * @return boolean
     * @see OWLOntologyManagerImpl#contains(IRI)
     */
    @Override
    public boolean contains(@Nonnull IRI iri) {
        OntApiException.notNull(iri, "Ontology IRI cannot be null");
        getLock().readLock().lock();
        try {
            return content.keys().anyMatch(o -> o.match(iri));
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param id {@link OWLOntologyID}
     * @return boolean
     * @see OWLOntologyManagerImpl#contains(OWLOntologyID)
     */
    @Override
    public boolean contains(@Nonnull OWLOntologyID id) {
        getLock().readLock().lock();
        try {
            return !id.isAnonymous() && (content.contains(id) || content.keys().anyMatch(id::match));
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return boolean
     * @see OWLOntologyManagerImpl#contains(OWLOntology)
     */
    @Override
    public boolean contains(@Nonnull OWLOntology ontology) {
        getLock().readLock().lock();
        try {
            return content.values().map(OntInfo::get).anyMatch(o -> Objects.equals(o, ontology));
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param iri {@link IRI}
     * @return boolean
     * @see OWLOntologyManagerImpl#containsVersion(IRI)
     */
    @Override
    public boolean containsVersion(@Nonnull IRI iri) {
        getLock().readLock().lock();
        try {
            return content.keys().anyMatch(o -> o.matchVersion(iri));
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @see OWLOntologyManagerImpl#removeOntology(OWLOntology)
     */
    @Override
    public void removeOntology(OWLOntology ontology) {
        getLock().writeLock().lock();
        try {
            removeOntology(ontology.getOntologyID());
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param id {@link OWLOntologyID}
     * @see OWLOntologyManagerImpl#removeOntology(OWLOntologyID)
     */
    @Override
    public void removeOntology(@Nonnull OWLOntologyID id) {
        getLock().writeLock().lock();
        try {
            content.remove(id).map(OntInfo::get).ifPresent(m -> m.setOWLOntologyManager(null));
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @see OWLOntologyManagerImpl#clearOntologies()
     */
    @Override
    public void clearOntologies() {
        getLock().writeLock().lock();
        try {
            listeners.clear();
            content.values().map(OntInfo::get).forEach(o -> o.setOWLOntologyManager(null));
            content.clear();
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param declaration {@link OWLImportsDeclaration}
     * @return {@link OntologyModel}
     * @see OWLOntologyManagerImpl#getImportedOntology(OWLImportsDeclaration)
     */
    @Override
    public OntologyModel getImportedOntology(@Nonnull OWLImportsDeclaration declaration) {
        getLock().readLock().lock();
        try {
            Optional<OntInfo> res = content.values().filter(e -> Objects.equals(e.getImportDeclaration(), declaration)).findFirst();
            if (!res.isPresent()) {
                // No such ontology has been loaded through an import
                // declaration, but it might have been loaded manually.
                // Using the IRI to retrieve it will either find the ontology or
                // return null.
                // Last possibility is an import by document IRI; if the
                // ontology is not found by IRI, check by document IRI.
                res = content.values().filter(e -> Objects.equals(e.getDocumentIRI(), declaration.getIRI())).findFirst();
            }
            return res.map(OntInfo::get).orElse(null);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * the difference: find first, not any.
     *
     * @param iri {@link IRI}
     * @return {@link OntologyModelImpl}
     * @see OWLOntologyManagerImpl#getOntologyByDocumentIRI(IRI)
     */
    @Nullable
    protected OntologyModel getOntologyByDocumentIRI(IRI iri) {
        getLock().readLock().lock();
        try {
            return content.values().filter(o -> Objects.equals(iri, o.getDocumentIRI()))
                    .map(OntInfo::get).findFirst().orElse(null);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * Gets document IRI.
     * @param ontology {@link OWLOntology}, not null
     * @return {@link IRI}, not null
     * @throws UnknownOWLOntologyException ex
     * @see OWLOntologyManagerImpl#getOntologyDocumentIRI(OWLOntology)
     */
    @Nonnull
    @Override
    public IRI getOntologyDocumentIRI(@Nonnull OWLOntology ontology) {
        return ontologyDocumentIRI(ontology).orElseThrow(() -> new OntApiException("Null document iri"));
    }

    protected Optional<IRI> ontologyDocumentIRI(OWLOntology ontology) {
        getLock().readLock().lock();
        try {
            OWLOntologyID id = ontology.getOntologyID();
            OntInfo res = content.get(id).orElseThrow(() -> new UnknownOWLOntologyException(id));
            return Optional.ofNullable(res.getDocumentIRI());
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param ontology    {@link OWLOntology}
     * @param documentIRI {@link IRI}, the source
     * @throws UnknownOWLOntologyException e
     * @see OWLOntologyManagerImpl#setOntologyDocumentIRI(OWLOntology, IRI)
     */
    @Override
    public void setOntologyDocumentIRI(@Nonnull OWLOntology ontology, @Nonnull IRI documentIRI) {
        getLock().writeLock().lock();
        try {
            OWLOntologyID id = ontology.getOntologyID();
            OntInfo info = content.get(id).orElseThrow(() -> new UnknownOWLOntologyException(id));
            info.addDocumentIRI(documentIRI);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * Note: the difference is the exception is not thrown in original implementation in case no ontology found.
     *
     * @param ontology       {@link OWLOntology}
     * @param ontologyFormat {@link OWLDocumentFormat}
     * @throws UnknownOWLOntologyException e
     * @see OWLOntologyManagerImpl#setOntologyFormat(OWLOntology, OWLDocumentFormat)
     * @see org.semanticweb.owlapi.model.OWLOntologyFactory.OWLOntologyCreationHandler#setOntologyFormat(OWLOntology, OWLDocumentFormat)
     */
    @Override
    public void setOntologyFormat(@Nonnull OWLOntology ontology, @Nonnull OWLDocumentFormat ontologyFormat) {
        getLock().writeLock().lock();
        try {
            OWLOntologyID id = ontology.getOntologyID();
            content.get(id).orElseThrow(() -> new UnknownOWLOntologyException(id)).addFormat(ontologyFormat);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return {@link OWLDocumentFormat}
     * @see OWLOntologyManagerImpl#getOntologyFormat(OWLOntology)
     */
    @Nullable
    @Override
    public OWLDocumentFormat getOntologyFormat(@Nonnull OWLOntology ontology) {
        getLock().readLock().lock();
        try {
            OWLOntologyID id = ontology.getOntologyID();
            return content.get(id).map(OntInfo::getFormat).orElse(null);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param ont {@link OWLOntology}
     * @throws ClassCastException ex
     * @see OWLOntologyManagerImpl#ontologyCreated(OWLOntology)
     */
    @Override
    public void ontologyCreated(@Nonnull OWLOntology ont) {
        // This method is called when a factory that we have asked to create or
        // load an ontology has created the ontology. We add the ontology to the
        // set of loaded ontologies.
        getLock().writeLock().lock();
        try {
            content.add((OntologyModel) ont);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return Stream of {@link OWLOntology}
     * @see OWLOntologyManagerImpl#directImports(OWLOntology)
     */
    @Override
    public Stream<OWLOntology> directImports(@Nonnull OWLOntology ontology) {
        getLock().readLock().lock();
        try {
            if (!contains(ontology)) {
                throw new UnknownOWLOntologyException(ontology.getOntologyID());
            }
            return ontology.importsDeclarations().map(this::getImportedOntology).map(OWLOntology.class::cast).filter(Objects::nonNull);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return Stream of {@link OWLOntology}
     * @see OWLOntologyManagerImpl#imports(OWLOntology)
     */
    @Override
    public Stream<OWLOntology> imports(@Nonnull OWLOntology ontology) {
        getLock().readLock().lock();
        try {
            return getImports(ontology, new LinkedHashSet<>()).stream();
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * No lock.
     *
     * @param ont    {@link OWLOntology}
     * @param result Set of {@link OWLOntology}
     * @return the same set of {@link OWLOntology}
     * @see OWLOntologyManagerImpl#getImports(OWLOntology, Set)
     */
    protected Set<OWLOntology> getImports(OWLOntology ont, Set<OWLOntology> result) {
        directImports(ont).filter(result::add).forEach(o -> getImports(o, result));
        return result;
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return Stream of {@link OWLOntology}
     * @see OWLOntologyManagerImpl#importsClosure(OWLOntology)
     */
    @Override
    public Stream<OWLOntology> importsClosure(@Nonnull OWLOntology ontology) {
        getLock().readLock().lock();
        try {
            Set<OWLOntology> res = isConcurrent() ? CollectionFactory.createSyncSet() : CollectionFactory.createSet();
            getImportsClosure(ontology, res);
            return res.stream();
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param ontology   {@link OWLOntology}
     * @param ontologies Set {@link OWLOntology}
     * @return Set {@link OWLOntology}
     * @see OWLOntologyManagerImpl#getImportsClosure(OWLOntology, Set)
     */
    protected Set<OWLOntology> getImportsClosure(OWLOntology ontology, Set<OWLOntology> ontologies) {
        getLock().readLock().lock();
        try {
            ontologies.add(ontology);
            directImports(ontology).filter(o -> !ontologies.contains(o)).forEach(o -> getImportsClosure(o, ontologies));
            return ontologies;
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return List of {@link OWLOntology}
     * @see OWLOntologyManagerImpl#getSortedImportsClosure(OWLOntology)
     */
    @Override
    public List<OWLOntology> getSortedImportsClosure(@Nonnull OWLOntology ontology) {
        getLock().readLock().lock();
        try {
            return ontology.importsClosure().sorted().collect(Collectors.toList());
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @return Stream of {@link OWLOntology}
     * @see OWLOntologyManagerImpl#ontologies()
     */
    @Override
    public Stream<OWLOntology> ontologies() {
        getLock().readLock().lock();
        try {
            // XXX investigate lockable access to streams
            return content.values().map(OntInfo::get);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param iri {@link IRI}
     * @return Stream of {@link OWLOntologyID}
     * @see OWLOntologyManagerImpl#ontologyIDsByVersion(IRI)
     */
    @Override
    public Stream<OWLOntologyID> ontologyIDsByVersion(@Nonnull IRI iri) {
        getLock().readLock().lock();
        try {
            return content.keys().filter(o -> o.matchVersion(iri));
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param ont   {@link OWLOntology}
     * @param axiom {@link OWLAxiom}
     * @return {@link ChangeApplied}
     * @see OWLOntologyManagerImpl#addAxiom(OWLOntology, OWLAxiom)
     */
    @Override
    public ChangeApplied addAxiom(@Nonnull OWLOntology ont, @Nonnull OWLAxiom axiom) {
        return applyChanges(Collections.singletonList(new AddAxiom(ont, axiom)));
    }

    /**
     * @param ont    {@link OWLOntology}
     * @param axioms Stream of {@link OWLAxiom}
     * @return {@link ChangeApplied}
     * @see OWLOntologyManagerImpl#addAxioms(OWLOntology, Stream)
     */
    @Override
    public ChangeApplied addAxioms(@Nonnull OWLOntology ont, @Nonnull Stream<? extends OWLAxiom> axioms) {
        return applyChanges(axioms.map(ax -> new AddAxiom(ont, ax)).collect(Collectors.toList()));
    }

    /**
     * @param ont   {@link OWLOntology}
     * @param axiom {@link OWLAxiom}
     * @return {@link ChangeApplied}
     * @see OWLOntologyManagerImpl#removeAxiom(OWLOntology, OWLAxiom)
     */
    @Override
    public ChangeApplied removeAxiom(@Nonnull OWLOntology ont, @Nonnull OWLAxiom axiom) {
        return applyChanges(Collections.singletonList(new RemoveAxiom(ont, axiom)));
    }

    /**
     * @param ont    {@link OWLOntology}
     * @param axioms Stream of {@link OWLAxiom}
     * @return {@link ChangeApplied}
     * @see OWLOntologyManagerImpl#removeAxioms(OWLOntology, Stream)
     */
    @Override
    public ChangeApplied removeAxioms(@Nonnull OWLOntology ont, @Nonnull Stream<? extends OWLAxiom> axioms) {
        return applyChanges(axioms.map(ax -> new RemoveAxiom(ont, ax)).collect(Collectors.toList()));
    }

    /**
     * @param change {@link OWLOntologyChange}
     * @return {@link ChangeApplied}
     * @see OWLOntologyManagerImpl#applyChange(OWLOntologyChange)
     */
    @Override
    public ChangeApplied applyChange(@Nonnull OWLOntologyChange change) {
        return applyChanges(Collections.singletonList(change));
    }

    /**
     * Copy-paste from {@link OWLOntologyManagerImpl#applyChangesAndGetDetails(List)}
     *
     * @param changes List of {@link OWLOntologyChange}s
     * @return {@link ChangeDetails}
     * @see OWLOntologyManagerImpl#applyChangesAndGetDetails(List)
     * @since owl-api 5.1.1
     * @since ont-api 1.1.0
     */
    @Override
    public ChangeDetails applyChangesAndGetDetails(@Nonnull List<? extends OWLOntologyChange> changes) {
        getLock().writeLock().lock();
        try {
            listeners.broadcastImpendingChanges(changes);
            AtomicBoolean rollbackRequested = new AtomicBoolean(false);
            AtomicBoolean allNoOps = new AtomicBoolean(true);
            // list of changes applied successfully. These are the changes that
            // will be reverted in case of a rollback
            List<OWLOntologyChange> appliedChanges = new ArrayList<>();
            listeners.fireBeginChanges(changes.size());
            actuallyApply(changes, rollbackRequested, allNoOps, appliedChanges);
            if (rollbackRequested.get()) {
                rollBack(appliedChanges);
                appliedChanges.clear();
            }
            listeners.fireEndChanges();
            listeners.broadcastChanges(appliedChanges);
            if (rollbackRequested.get()) {
                return new ChangeDetails(ChangeApplied.UNSUCCESSFULLY, appliedChanges);
            }
            if (allNoOps.get()) {
                return new ChangeDetails(ChangeApplied.NO_OPERATION, appliedChanges);
            }
            return new ChangeDetails(ChangeApplied.SUCCESSFULLY, appliedChanges);
        } catch (OWLOntologyChangeVetoException e) {
            // Some listener blocked the changes.
            listeners.broadcastOntologyChangesVetoed(changes, e);
            return new ChangeDetails(ChangeApplied.UNSUCCESSFULLY, Collections.emptyList());
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param changes           List of {@link OWLOntologyChange}
     * @param rollbackRequested boolean
     * @param allNoOps          boolean
     * @param appliedChanges    List of {@link OWLOntologyChange}
     * @see OWLOntologyManagerImpl#actuallyApply(List, AtomicBoolean, AtomicBoolean, List)
     */
    protected void actuallyApply(List<? extends OWLOntologyChange> changes, AtomicBoolean rollbackRequested,
                                 AtomicBoolean allNoOps, List<OWLOntologyChange> appliedChanges) {
        for (OWLOntologyChange change : changes) {
            // once rollback is requested by a failed change, do not carry
            // out any more changes
            if (!rollbackRequested.get()) {
                assert change != null;
                ChangeApplied enactChangeApplication = enactChangeApplication(change);
                if (enactChangeApplication == ChangeApplied.UNSUCCESSFULLY) {
                    rollbackRequested.set(true);
                }
                if (enactChangeApplication == ChangeApplied.SUCCESSFULLY) {
                    allNoOps.set(false);
                    appliedChanges.add(change);
                }
                listeners.fireChangeApplied(change);
            }
        }
    }

    /**
     * @param appliedChanges List of {@link OWLOntologyChange}
     * @see OWLOntologyManagerImpl#rollBack(List)
     */
    protected void rollBack(List<OWLOntologyChange> appliedChanges) {
        for (OWLOntologyChange c : appliedChanges) {
            if (enactChangeApplication(c.reverseChange()) == ChangeApplied.UNSUCCESSFULLY) {
                // rollback could not complete, throw an exception
                throw new OWLRuntimeException("Rollback of changes unsuccessful: Change " + c + " could not be rolled back");
            }
        }
    }

    /**
     * @param change {@link OWLOntologyChange}
     * @return {@link ChangeApplied}
     * @see OWLOntologyManagerImpl#enactChangeApplication(OWLOntologyChange)
     */
    protected ChangeApplied enactChangeApplication(OWLOntologyChange change) {
        if (!isChangeApplicable(change)) {
            return ChangeApplied.UNSUCCESSFULLY;
        }
        OWLOntology ont = change.getOntology();
        if (!(ont instanceof OWLMutableOntology)) {
            throw new ImmutableOWLOntologyChangeException(change.getChangeData(), ont.toString());
        }
        checkForOntologyIDChange(change);
        ChangeApplied appliedChange = ont.applyDirectChange(change);
        checkForImportsChange(change);
        return appliedChange;
    }

    /**
     * @param change {@link OWLOntologyChange}
     * @return boolean
     * @see OWLOntologyManagerImpl#isChangeApplicable(OWLOntologyChange)
     */
    protected boolean isChangeApplicable(OWLOntologyChange change) {
        OWLOntologyID id = change.getOntology().getOntologyID();
        Optional<OWLOntologyLoaderConfiguration> conf = content.get(id).map(OntInfo::getModelConfig).map(ConfigProvider.Config::loaderConfig);
        return !(conf.isPresent() && !conf.get().isLoadAnnotationAxioms() && change.isAddAxiom() && change.getAxiom() instanceof OWLAnnotationAxiom);
    }

    /**
     * @param change {@link OWLOntologyChange}
     * @see OWLOntologyManagerImpl#checkForOntologyIDChange(OWLOntologyChange)
     */
    protected void checkForOntologyIDChange(OWLOntologyChange change) {
        if (!(change instanceof SetOntologyID)) {
            return;
        }
        SetOntologyID setID = (SetOntologyID) change;
        Optional<OntologyModel> existingOntology = content.get(setID.getNewOntologyID()).map(OntInfo::get);
        OWLOntology o = setID.getOntology();
        if (existingOntology.isPresent() && !o.equals(existingOntology.get()) && !o.equalAxioms(existingOntology.get())) {
            LOGGER.error("OWLOntologyManagerImpl.checkForOntologyIDChange() existing:{}", existingOntology);
            LOGGER.error("OWLOntologyManagerImpl.checkForOntologyIDChange() new:{}", o);
            throw new OWLOntologyRenameException(setID.getChangeData(), setID.getNewOntologyID());
        }
    }

    /**
     * This method has the same signature as the original but totally different meaning.
     * In ONT-API it is for making some related changes with the ontology from {@link ImportChange},
     * not for keeping correct state of manager.
     *
     * @param change {@link OWLOntologyChange}
     * @see OWLOntologyManagerImpl#checkForImportsChange(OWLOntologyChange)
     */
    protected void checkForImportsChange(OWLOntologyChange change) {
        if (!change.isImportChange()) {
            return;
        }
        OWLImportsDeclaration declaration = ((ImportChange) change).getImportDeclaration();
        OntologyModel ontology = (OntologyModel) change.getOntology();
        OWLOntologyID id = ontology.getOntologyID();
        Optional<OntWriterConfiguration> conf = content.get(id).map(OntInfo::getModelConfig)
                .map(ConfigProvider.Config::writerConfig);
        if (!conf.isPresent() || !conf.get().isControlImports()) {
            return;
        }
        OntologyModel importedOntology = getImportedOntology(declaration);
        if (importedOntology == null) {
            return;
        }
        Stream<OWLOntologyChange> relatedChanges;
        if (change instanceof AddImport) {
            // remove duplicated declarations if they are present in the imported ontology
            relatedChanges = importedOntology.axioms(AxiomType.DECLARATION, Imports.INCLUDED)
                    .filter(ontology::containsAxiom)
                    .map(a -> new RemoveAxiom(ontology, a));
        } else {
            // return back declarations which are still in use:
            relatedChanges = importedOntology.signature(Imports.INCLUDED)
                    .filter(ontology::containsReference)
                    .map(e -> getOWLDataFactory().getOWLDeclarationAxiom(e))
                    .map(a -> new AddAxiom(ontology, a));
        }
        relatedChanges.forEach(ontology::applyDirectChange);
    }

    /**
     * @param toCopy   {@link OWLOntology}
     * @param settings {@link OntologyCopy}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException ex
     * @see OWLOntologyManagerImpl#copyOntology(OWLOntology, OntologyCopy)
     */
    @Override
    public OntologyModel copyOntology(@Nonnull OWLOntology toCopy, @Nonnull OntologyCopy settings) throws OWLOntologyCreationException {
        getLock().writeLock().lock();
        try {
            OntApiException.notNull(toCopy, "Null ontology.");
            OntApiException.notNull(settings, "Null settings.");
            OntologyModel res;
            switch (settings) {
                case MOVE:
                    if (!OntologyModel.class.isInstance(toCopy)) {
                        throw new OWLOntologyCreationException(String.format("Can't move %s: not an %s. Use %s or %s parameter.",
                                toCopy.getOntologyID(), OntologyModel.class.getSimpleName(), OntologyCopy.DEEP, OntologyCopy.SHALLOW));
                    }
                    res = (OntologyModel) toCopy;
                    ontologyCreated(res);
                    break;
                case SHALLOW:
                case DEEP:
                    OntologyModel o = createOntology(toCopy.getOntologyID());
                    AxiomType.AXIOM_TYPES.forEach(t -> addAxioms(o, toCopy.axioms(t)));
                    toCopy.annotations().forEach(a -> applyChange(new AddOntologyAnnotation(o, a)));
                    toCopy.importsDeclarations().forEach(a -> applyChange(new AddImport(o, a)));
                    res = o;
                    break;
                default:
                    throw new OWLRuntimeException("settings value not understood: " + settings);
            }
            // toReturn now initialized
            OWLOntologyManager m = toCopy.getOWLOntologyManager();
            if (settings == OntologyCopy.MOVE || settings == OntologyCopy.DEEP) {
                setOntologyDocumentIRI(res, m.getOntologyDocumentIRI(toCopy));
                OWLDocumentFormat ontologyFormat = m.getOntologyFormat(toCopy);
                if (ontologyFormat != null) {
                    setOntologyFormat(res, ontologyFormat);
                }
            }
            if (settings == OntologyCopy.MOVE) {
                m.removeOntology(toCopy);
                // at this point toReturn and toCopy are the same object
                // change the manager on the ontology
                res.setOWLOntologyManager(this);
            }
            return res;
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * In case of coping from ONT to OWL there will be an exception.
     * This method helps to fix the origin manager.
     *
     * @param o          {@link OWLOntology o}, must be our (ONT) object.
     * @param owlManager {@link OWLOntologyManager} any OWL manager.
     * @see OWLOntologyManagerImpl#copyOntology(OWLOntology, OntologyCopy)
     */
    protected void rollBackMoving(OWLOntology o, OWLOntologyManager owlManager) {
        ontologyCreated(o);
        OWLDocumentFormat f = owlManager.getOntologyFormat(o);
        if (f != null) {
            setOntologyFormat(o, f);
        }
        IRI doc;
        try {
            doc = owlManager.getOntologyDocumentIRI(o);
        } catch (RuntimeException e) {
            LOGGER.warn("Document IRI is not expected to be null!", e);
            return;
        }
        setOntologyDocumentIRI(o, doc);
    }

    /**
     * @param source {@link IRI}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException ex
     * @see OWLOntologyManagerImpl#loadOntology(IRI)
     */
    @Override
    public OntologyModel loadOntology(@Nonnull IRI source) throws OWLOntologyCreationException {
        getLock().writeLock().lock();
        try {
            return load(source, false, getOntologyLoaderConfiguration());
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param source {@link OWLOntologyDocumentSource}
     * @param conf   {@link OWLOntologyLoaderConfiguration}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException ex
     * @see OWLOntologyManagerImpl#loadOntologyFromOntologyDocument(OWLOntologyDocumentSource, OWLOntologyLoaderConfiguration)
     */
    @Override
    public OntologyModel loadOntologyFromOntologyDocument(@Nonnull OWLOntologyDocumentSource source, @Nonnull OWLOntologyLoaderConfiguration conf) throws OWLOntologyCreationException {
        getLock().writeLock().lock();
        try {
            return load(null, source, conf);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * Inner method. no lock.
     *
     * @param iri         {@link IRI}
     * @param allowExists boolean
     * @param conf        {@link OWLOntologyLoaderConfiguration}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException ex
     * @see OWLOntologyManagerImpl#loadOntology(IRI, boolean, OWLOntologyLoaderConfiguration)
     */
    protected OntologyModel load(@Nonnull IRI iri, boolean allowExists, OWLOntologyLoaderConfiguration conf) throws OWLOntologyCreationException {
        // Check for matches on the ontology IRI first
        OWLOntologyID id = new OWLOntologyID(Optional.of(iri), Optional.empty());
        OntologyModel ontByID = getOntology(id);
        if (ontByID != null) {
            return ontByID;
        }
        IRI documentIRI = getDocumentIRIFromMappers(id);
        if (documentIRI != null) {
            // The ontology might be being loaded, but its IRI might
            // not have been set (as is probably the case with RDF/XML!)
            Optional<OntologyModel> op = content.values().filter(o -> Objects.equals(o.getDocumentIRI(), documentIRI)).map(OntInfo::get).findFirst();
            if (op.isPresent() && !allowExists) {
                throw new OWLOntologyDocumentAlreadyExistsException(documentIRI);
            }
            if (op.isPresent()) {
                return op.get();
            }
        } else {
            // Nothing we can do here. We can't get a document IRI to load
            // the ontology from.
            throw new OntologyIRIMappingNotFoundException(iri);
        }
        return load(iri, new IRIDocumentSource(documentIRI, null, null), conf);
    }

    /**
     * no lock
     *
     * @param iri    {@link IRI}
     * @param source {@link OWLOntologyDocumentSource}
     * @param conf   {@link OWLOntologyLoaderConfiguration}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException if smth wrong
     * @see OWLOntologyManagerImpl#loadOntology(IRI, OWLOntologyDocumentSource, OWLOntologyLoaderConfiguration)
     */
    protected OntologyModel load(@Nullable IRI iri, OWLOntologyDocumentSource source, OWLOntologyLoaderConfiguration conf) throws OWLOntologyCreationException {
        listeners.fireStartedLoadingEvent(new OWLOntologyID(Optional.ofNullable(iri), Optional.empty()), source.getDocumentIRI());
        Exception ex = null;
        OWLOntologyID id = new OWLOntologyID();
        try {
            OntologyModel res = load(source, conf);
            if (res != null) {
                id = res.getOntologyID();
                return res;
            }
        } catch (UnloadableImportException | OWLOntologyCreationException e) {
            ex = e;
            throw e;
        } catch (OWLRuntimeException e) {
            if (e.getCause() instanceof OWLOntologyCreationException) {
                ex = (OWLOntologyCreationException) e.getCause();
                throw (OWLOntologyCreationException) e.getCause();
            }
            throw e;
        } finally {
            listeners.fireFinishedLoadingEvent(id, source.getDocumentIRI(), ex);
        }
        throw new OWLOntologyFactoryNotFoundException(source.getDocumentIRI());
    }

    /**
     * @param source {@link OWLOntologyDocumentSource}
     * @param conf   {@link OWLOntologyLoaderConfiguration}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException ex
     * @see OWLOntologyManagerImpl#load(OWLOntologyDocumentSource, OWLOntologyLoaderConfiguration)
     */
    @Nullable
    protected OntologyModel load(OWLOntologyDocumentSource source, OWLOntologyLoaderConfiguration conf) throws OWLOntologyCreationException {
        for (OWLOntologyFactory factory : ontologyFactories) {
            if (!factory.canAttemptLoading(source))
                continue;
            // todo: only single factory by now. implement wrapper-factory for native owl-factories.
            try {
                OntologyModel res = (OntologyModel) factory.loadOWLOntology(this, source, this, conf);
                OWLOntologyID id = res.getOntologyID();
                fixIllegalPunnings(res);
                return content.get(id).orElseThrow(() -> new UnknownOWLOntologyException(id))
                        .addDocumentIRI(source.getDocumentIRI()).get();
            } catch (OWLOntologyRenameException e) {
                // We loaded an ontology from a document and the
                // ontology turned out to have an IRI the same
                // as a previously loaded ontology
                throw new OWLOntologyAlreadyExistsException(e.getOntologyID(), e);
            }
        }
        return null;
    }

    /**
     * The punning is a situation when the same subject (uri-resource) has several declarations,
     * i.e. can be treated as entity of different types (e.g. OWLClass and OWLIndividual).
     * In OWL 2 DL a property cannot have owl:ObjectProperty, owl:DatatypeProperty or olw:AnnotationProperty declaration
     * at the same time, it is so called illegal punning. Same for owl:Class and rdfs:Datatype.     *
     * It seems that all other kind of type intersections are allowed, e.g ObjectProperty can be Datatype or Class,
     * and NamedIndividual can be anything else.
     * More about punnings see <a href='https://www.w3.org/2007/OWL/wiki/Punning'>wiki</a>.
     * See also OWL-API example of implementation this logic {@link OWLDocumentFormat#computeIllegals(Multimap)}
     * <p>
     * In general cases the original method does not do anything but printing errors (OWL-5.0.4),
     * i.e. when we have all entities explicitly declared in graph.
     * But when we use to load ontology some OWL-API parser, we could have some declarations missed
     * (i.e. the corresponding entity is proclaimed implicitly under some other axiom).
     * For this case the original method tries to fix such illegal punning by removing annotation property declaration
     * if the entity is object or date property.
     * It seems it is not correct.
     * There is unpredictable changes in 'signature' when we use different formats or/and reload ontology several times.
     * In addition ONT-API works in different way: in case it is specified in personality ({@link OntPersonality})
     * the axioms with illegal punnings can not be read from graph (even corresponding triples are present)
     * and can not be added to graph as well.
     * So we just skip this method.
     *
     * @param o {@link OWLOntology}
     * @see OWLOntologyManagerImpl#fixIllegalPunnings(OWLOntology)
     */
    @SuppressWarnings("unused")
    protected void fixIllegalPunnings(OWLOntology o) {
        // nothing here.
    }

    /**
     * No lock.
     *
     * @param declaration {@link OWLImportsDeclaration}
     * @param conf        {@link OWLOntologyLoaderConfiguration}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException ex
     * @see OWLOntologyManagerImpl#loadImports(OWLImportsDeclaration, OWLOntologyLoaderConfiguration)
     */
    @Nullable
    protected OntologyModel loadImports(@Nonnull OWLImportsDeclaration declaration, @Nonnull OWLOntologyLoaderConfiguration conf) throws OWLOntologyCreationException {
        listeners.incrementImportsLoadCount();
        try {
            return load(declaration.getIRI(), true, conf);
        } catch (OWLOntologyCreationException e) {
            if (MissingImportHandlingStrategy.THROW_EXCEPTION.equals(conf.getMissingImportHandlingStrategy())) {
                throw e;
            } else {
                // Silent
                MissingImportEvent evt = new MissingImportEvent(declaration.getIRI(), e);
                listeners.fireMissingImportEvent(evt);
            }
        } finally {
            listeners.decrementImportsLoadCount();
        }
        return null;
    }

    /**
     * @param declaration {@link OWLImportsDeclaration}
     * @param conf        {@link OWLOntologyLoaderConfiguration}
     * @see OWLOntologyManagerImpl#makeLoadImportRequest(OWLImportsDeclaration, OWLOntologyLoaderConfiguration)
     */
    @Override
    public void makeLoadImportRequest(@Nonnull OWLImportsDeclaration declaration, @Nonnull OWLOntologyLoaderConfiguration conf) {
        getLock().writeLock().lock();
        try {
            if (conf.isIgnoredImport(declaration.getIRI())) return;
            if (getImportedOntology(declaration) != null) return;
            try {
                OntologyModel m = loadImports(declaration, conf);
                if (m != null) {
                    content.get(m.getOntologyID()).ifPresent(ontInfo -> ontInfo.addImportDeclaration(declaration));
                }
            } catch (OWLOntologyCreationException e) {
                throw new UnloadableImportException(e, declaration);
            }
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param ontology       {@link OWLOntology}
     * @param ontologyFormat {@link OWLDocumentFormat}
     * @param documentIRI    {@link IRI}
     * @throws OWLOntologyStorageException ex
     * @see OWLOntologyManagerImpl#saveOntology(OWLOntology, OWLDocumentFormat, IRI)
     * @see org.semanticweb.owlapi.util.AbstractOWLStorer#storeOntology(OWLOntology, IRI, OWLDocumentFormat)
     */
    @Override
    public void saveOntology(@Nonnull OWLOntology ontology, @Nonnull OWLDocumentFormat ontologyFormat, @Nonnull IRI documentIRI) throws OWLOntologyStorageException {
        if (!documentIRI.isAbsolute()) {
            throw new OWLOntologyStorageException("Document IRI must be absolute: " + documentIRI);
        }
        saveOntology(ontology, ontologyFormat, new OWLOntologyDocumentTarget() {
            @Override
            public Optional<IRI> getDocumentIRI() {
                return Optional.of(documentIRI);
            }
        });
    }

    /**
     * @param ontology       {@link OWLOntology}
     * @param ontologyFormat {@link OWLDocumentFormat}
     * @param documentTarget {@link OWLOntologyDocumentTarget}
     * @throws OWLOntologyStorageException ex
     * @see OWLOntologyManagerImpl#saveOntology(OWLOntology, OWLDocumentFormat, OWLOntologyDocumentTarget)
     */
    @Override
    public void saveOntology(@Nonnull OWLOntology ontology, @Nonnull OWLDocumentFormat ontologyFormat, @Nonnull OWLOntologyDocumentTarget documentTarget) throws OWLOntologyStorageException {
        getLock().readLock().lock();
        try {
            write(ontology, ontologyFormat, documentTarget);
        } finally {
            getLock().readLock().unlock();
        }
    }

    protected void write(OWLOntology ontology, OWLDocumentFormat documentFormat, OWLOntologyDocumentTarget target) throws OWLOntologyStorageException {
        OntFormat format = OntFormat.get(documentFormat);
        if (format == null || !format.isJena() || !OntologyModel.class.isInstance(ontology)) {
            if (OntologyModel.class.isInstance(ontology)) {
                // It does not work correctly without expanding axioms for some OWL-API formats such as ManchesterSyntaxDocumentFormat.
                // The cache cleaning encourages extracting hidden axioms (declarations) in an explicit form while getting axioms:
                ((OntologyModel) ontology).clearCache();
            }
            try {
                for (OWLStorerFactory storerFactory : ontologyStorers) {
                    OWLStorer storer = storerFactory.createStorer();
                    if (storer.canStoreOntology(documentFormat)) {
                        storer.storeOntology(ontology, target, documentFormat);
                        return;
                    }
                }
                throw new OWLStorerNotFoundException(documentFormat);
            } catch (IOException e) {
                throw new OWLOntologyStorageIOException(e);
            }
        }
        OutputStream os = null;
        if (target.getOutputStream().isPresent()) {
            os = target.getOutputStream().get();
        } else if (target.getDocumentIRI().isPresent()) {
            IRI iri = target.getDocumentIRI().get();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Save " + ontology.getOntologyID() + " to " + iri);
            }
            try {
                os = openStream(iri);
            } catch (IOException e) {
                throw new OWLOntologyStorageIOException(e);
            }
        } else if (target.getWriter().isPresent()) {
            os = new WriterOutputStream(target.getWriter().get(), StandardCharsets.UTF_8);
        }
        if (os == null) {
            throw new OWLOntologyStorageException("Null output stream, format = " + documentFormat);
        }
        Model model = ((OntologyModel) ontology).asGraphModel().getBaseModel();
        PrefixManager pm = PrefixManager.class.isInstance(documentFormat) ? (PrefixManager) documentFormat : null;
        setDefaultPrefix(pm, ontology);
        Map<String, String> newPrefixes = pm != null ? pm.getPrefixName2PrefixMap() : Collections.emptyMap();
        Map<String, String> initPrefixes = model.getNsPrefixMap();
        try {
            Models.setNsPrefixes(model, newPrefixes);
            RDFDataMgr.write(os, model, format.getLang());
        } catch (JenaException e) {
            throw new OWLOntologyStorageException("Can't save " + ontology.getOntologyID() + ". Format=" + format, e);
        } finally {
            Models.setNsPrefixes(model, initPrefixes);
        }
    }

    /**
     * todo: currently it is only for turtle.
     * see similar fragment inside constructor of {@link org.semanticweb.owlapi.rdf.turtle.renderer.TurtleRenderer}.
     * todo: it seems we don't need default prefixes at all.
     *
     * @param pm  {@link PrefixManager}
     * @param owl {@link OWLOntology}
     */
    public static void setDefaultPrefix(PrefixManager pm, OWLOntology owl) {
        if (pm == null || owl == null) return;
        if (!TurtleDocumentFormat.class.isInstance(pm)) return;
        if (pm.getDefaultPrefix() != null) return;
        if (!owl.getOntologyID().getOntologyIRI().isPresent()) return;
        String uri = owl.getOntologyID().getOntologyIRI().get().getIRIString();
        if (!uri.endsWith("/")) {
            uri += "#";
        }
        pm.setDefaultPrefix(uri);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static OutputStream openStream(IRI iri) throws IOException {
        if (OntConfig.DefaultScheme.FILE.same(iri)) {
            File file = new File(iri.toURI());
            file.getParentFile().mkdirs();
            return new FileOutputStream(file);
        }
        URL url = iri.toURI().toURL();
        URLConnection conn = url.openConnection();
        return conn.getOutputStream();
    }

    /**
     * The 'hack' methods to provide correct serialization.
     * It fixes graph links between different models:
     * ontology A with ontology B in the imports should have also {@link UnionGraph} inside,
     * that consists of base graph from A and base graph from B.
     *
     * @param in {@link ObjectInputStream}
     * @throws IOException            exception
     * @throws ClassNotFoundException exception
     * @see OWLOntologyManagerImpl#readObject(ObjectInputStream)
     * @see OntBaseModelImpl#readObject(ObjectInputStream)
     * @see OntologyModelImpl.Concurrent#readObject(ObjectInputStream)
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        loaderConfig = (OntLoaderConfiguration) in.readObject();
        writerConfig = (OntWriterConfiguration) in.readObject();
        content.values().forEach(info -> {
            ConfigProvider.Config conf = info.getModelConfig();
            InternalModelHolder m = (InternalModelHolder) info.get();
            UnionGraph baseGraph = m.getBase().getGraph();
            Stream<UnionGraph> imports = Graphs.getImports(baseGraph).stream()
                    .map(s -> content.values().map(OntInfo::get).map(InternalModelHolder.class::cast)
                            .map(InternalModelHolder::getBase).map(OntGraphModelImpl::getGraph)
                            .filter(g -> Objects.equals(s, Graphs.getURI(g))).findFirst().orElse(null))
                    .filter(Objects::nonNull);
            imports.forEach(baseGraph::addGraph);
            InternalModel baseModel = new InternalModel(baseGraph, conf);
            m.setBase(baseModel);
        });
    }

    /**
     * @param out {@link ObjectInputStream}
     * @throws IOException exception
     * @see OWLOntologyManagerImpl#writeObject(ObjectOutputStream)
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(getOntologyLoaderConfiguration());
        out.writeObject(getOntologyWriterConfiguration());
    }

    /**
     * Creates {@link ru.avicomp.ontapi.internal.ConfigProvider.Config} with reference to manager inside.
     *
     * @return {@link ModelConfig}
     * @see ConfigProvider
     * @see ru.avicomp.ontapi.internal.ConfigProvider.Config
     */
    public ModelConfig createModelConfig() {
        return new ModelConfig(this);
    }

    /**
     * Listeners holder.
     * This is just for simplification code.
     * All working with listeners should be here.
     */
    protected static class ListenersHolder implements Serializable {
        private static final String BADLISTENER = "BADLY BEHAVING LISTENER: {} has been removed";
        protected final List<MissingImportListener> missingImportsListeners = new ArrayList<>();
        protected final List<OWLOntologyLoaderListener> loaderListeners = new ArrayList<>();
        protected final List<OWLOntologyChangeProgressListener> progressListeners = new ArrayList<>();
        protected transient List<OWLOntologyChangesVetoedListener> vetoListeners = new ArrayList<>();
        protected transient Map<OWLOntologyChangeListener, OWLOntologyChangeBroadcastStrategy> listenerMap = new HashMap<>();
        protected transient Map<ImpendingOWLOntologyChangeListener, ImpendingOWLOntologyChangeBroadcastStrategy> impendingChangeListenerMap = new HashMap<>();

        protected ImpendingOWLOntologyChangeBroadcastStrategy defaultImpendingChangeBroadcastStrategy = new DefaultImpendingChangeBroadcastStrategy();
        protected OWLOntologyChangeBroadcastStrategy defaultChangeBroadcastStrategy = new DefaultChangeBroadcastStrategy();

        protected final AtomicInteger loadCount = new AtomicInteger();
        protected final AtomicInteger importsLoadCount = new AtomicInteger();

        protected final AtomicBoolean broadcastChanges = new AtomicBoolean(true);

        public void addMissingImportListener(@Nonnull MissingImportListener listener) {
            missingImportsListeners.add(listener);
        }

        public void removeMissingImportListener(@Nonnull MissingImportListener listener) {
            missingImportsListeners.remove(listener);
        }

        public void addOntologyLoaderListener(@Nonnull OWLOntologyLoaderListener listener) {
            loaderListeners.add(listener);
        }

        public void removeOntologyLoaderListener(@Nonnull OWLOntologyLoaderListener listener) {
            loaderListeners.remove(listener);
        }

        public void addOntologyChangeProgessListener(@Nonnull OWLOntologyChangeProgressListener listener) {
            progressListeners.add(listener);
        }

        public void removeOntologyChangeProgessListener(@Nonnull OWLOntologyChangeProgressListener listener) {
            progressListeners.remove(listener);
        }

        public void addOntologyChangesVetoedListener(@Nonnull OWLOntologyChangesVetoedListener listener) {
            vetoListeners.add(listener);
        }

        public void removeOntologyChangesVetoedListener(@Nonnull OWLOntologyChangesVetoedListener listener) {
            vetoListeners.remove(listener);
        }

        public void setDefaultChangeBroadcastStrategy(@Nonnull OWLOntologyChangeBroadcastStrategy strategy) {
            defaultChangeBroadcastStrategy = strategy;
        }

        public void addOntologyChangeListener(@Nonnull OWLOntologyChangeListener listener) {
            addOntologyChangeListener(listener, defaultChangeBroadcastStrategy);
        }

        public void addOntologyChangeListener(@Nonnull OWLOntologyChangeListener listener, @Nonnull OWLOntologyChangeBroadcastStrategy strategy) {
            listenerMap.put(listener, strategy);
        }

        public void removeOntologyChangeListener(@Nonnull OWLOntologyChangeListener listener) {
            listenerMap.remove(listener);
        }

        public void addImpendingOntologyChangeListener(@Nonnull ImpendingOWLOntologyChangeListener listener) {
            impendingChangeListenerMap.put(listener, defaultImpendingChangeBroadcastStrategy);
        }

        public void removeImpendingOntologyChangeListener(@Nonnull ImpendingOWLOntologyChangeListener listener) {
            impendingChangeListenerMap.remove(listener);
        }

        protected int incrementImportsLoadCount() {
            return importsLoadCount.incrementAndGet();
        }

        protected int decrementImportsLoadCount() {
            return importsLoadCount.decrementAndGet();
        }

        /**
         * @param evt {@link MissingImportEvent}
         * @see OWLOntologyManagerImpl#fireMissingImportEvent(MissingImportEvent)
         */
        protected void fireMissingImportEvent(MissingImportEvent evt) {
            missingImportsListeners.forEach(l -> l.importMissing(evt));
        }

        /**
         * @param id       {@link OWLOntologyID}
         * @param doc      {@link IRI}
         * @param imported boolean
         * @see OWLOntologyManagerImpl#fireStartedLoadingEvent(OWLOntologyID, IRI, boolean)
         */
        protected void fireStartedLoadingEvent(OWLOntologyID id, IRI doc, boolean imported) {
            for (OWLOntologyLoaderListener listener : loaderListeners) {
                listener.startedLoadingOntology(new OWLOntologyLoaderListener.LoadingStartedEvent(id, doc, imported));
            }
        }

        protected void fireStartedLoadingEvent(OWLOntologyID id, IRI doc) {
            if (loadCount.get() != importsLoadCount.get()) {
                LOGGER.warn("Runtime Warning: Parsers should load imported ontologies using the makeImportLoadRequest method.");
            }
            fireStartedLoadingEvent(id, doc, loadCount.get() > 0);
            loadCount.incrementAndGet();
            broadcastChanges.set(false);
        }

        /**
         * @param id       {@link OWLOntologyID}
         * @param doc      {@link IRI}
         * @param imported boolean
         * @param ex       {@link Exception}
         * @see OWLOntologyManagerImpl#fireFinishedLoadingEvent(OWLOntologyID, IRI, boolean, Exception)
         */
        protected void fireFinishedLoadingEvent(OWLOntologyID id, IRI doc, boolean imported, @Nullable Exception ex) {
            for (OWLOntologyLoaderListener listener : loaderListeners) {
                listener.finishedLoadingOntology(new OWLOntologyLoaderListener.LoadingFinishedEvent(id, doc, imported, ex));
            }
        }

        protected void fireFinishedLoadingEvent(OWLOntologyID id, IRI doc, @Nullable Exception ex) {
            if (loadCount.decrementAndGet() == 0) {
                broadcastChanges.set(true);
                // Completed loading ontology and imports
            }
            fireFinishedLoadingEvent(id, doc, loadCount.get() > 0, ex);
        }

        /**
         * @param size int
         * @see OWLOntologyManagerImpl#fireBeginChanges(int)
         */
        protected void fireBeginChanges(int size) {
            if (!broadcastChanges.get()) {
                return;
            }
            for (OWLOntologyChangeProgressListener listener : progressListeners) {
                try {
                    listener.begin(size);
                } catch (Exception e) {
                    LOGGER.warn(BADLISTENER, e.getMessage(), e);
                    progressListeners.remove(listener);
                }
            }
        }

        /**
         * @see OWLOntologyManagerImpl#fireEndChanges()
         */
        protected void fireEndChanges() {
            if (!broadcastChanges.get()) {
                return;
            }
            for (OWLOntologyChangeProgressListener listener : progressListeners) {
                try {
                    listener.end();
                } catch (Exception e) {
                    LOGGER.warn(BADLISTENER, e.getMessage(), e);
                    progressListeners.remove(listener);
                }
            }
        }

        /**
         * @param change {@link OWLOntologyChange}
         * @see OWLOntologyManagerImpl#fireChangeApplied(OWLOntologyChange)
         */
        protected void fireChangeApplied(OWLOntologyChange change) {
            if (!broadcastChanges.get()) {
                return;
            }
            if (progressListeners.isEmpty()) {
                return;
            }
            for (OWLOntologyChangeProgressListener listener : progressListeners) {
                try {
                    listener.appliedChange(change);
                } catch (Exception e) {
                    LOGGER.warn(BADLISTENER, e.getMessage(), e);
                    progressListeners.remove(listener);
                }
            }
        }

        /**
         * @param changes List of {@link OWLOntologyChange}
         * @param veto    {@link OWLOntologyChangeVetoException}
         * @see OWLOntologyManagerImpl#broadcastOntologyChangesVetoed(List, OWLOntologyChangeVetoException)
         */
        protected void broadcastOntologyChangesVetoed(List<? extends OWLOntologyChange> changes, OWLOntologyChangeVetoException veto) {
            vetoListeners.forEach(l -> l.ontologyChangesVetoed(changes, veto));
        }

        /**
         * @param changes List of {@link OWLOntologyChange}
         * @see OWLOntologyManagerImpl#broadcastChanges
         */
        protected void broadcastChanges(List<? extends OWLOntologyChange> changes) {
            if (!broadcastChanges.get()) {
                return;
            }
            for (OWLOntologyChangeListener listener : new ArrayList<>(listenerMap.keySet())) {
                OWLOntologyChangeBroadcastStrategy strategy = listenerMap.get(listener);
                if (strategy == null) {
                    // This listener may have been removed during the broadcast
                    // of the changes, so when we attempt to retrieve it from
                    // the map it isn't there (because we iterate over a copy).
                    continue;
                }
                try {
                    // Handle exceptions on a per listener basis. If we have
                    // badly behaving listeners, we don't want one listener
                    // to prevent the other listeners from receiving events.
                    strategy.broadcastChanges(listener, changes);
                } catch (Exception e) {
                    LOGGER.warn(BADLISTENER, e.getMessage(), e);
                    listenerMap.remove(listener);
                }
            }
        }

        /**
         * @param changes List of {@link OWLOntologyChange}
         * @see OWLOntologyManagerImpl#broadcastImpendingChanges(List)
         */
        protected void broadcastImpendingChanges(List<? extends OWLOntologyChange> changes) {
            if (!broadcastChanges.get()) {
                return;
            }
            for (ImpendingOWLOntologyChangeListener listener : new ArrayList<>(impendingChangeListenerMap.keySet())) {
                ImpendingOWLOntologyChangeBroadcastStrategy strategy = impendingChangeListenerMap.get(listener);
                if (strategy != null) {
                    strategy.broadcastChanges(listener, changes);
                }
            }
        }

        public void clear() {
            loaderListeners.clear();
            missingImportsListeners.clear();
            progressListeners.clear();
            vetoListeners.clear();
            listenerMap.clear();
            impendingChangeListenerMap.clear();
            broadcastChanges.set(true);
            loadCount.set(0);
            importsLoadCount.set(0);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            listenerMap = new HashMap<>();
            impendingChangeListenerMap = new HashMap<>();
            vetoListeners = new ArrayList<>();
        }
    }


    /**
     * The 'collection' of {@link OntInfo}s which wrap {@link OntologyModel}s.
     * To be sure that all members are in consistent state.
     * We can't use Map like in the initial OWL-API implementation since Ontology ID ({@link OWLOntologyID})
     * could be changed externally (e.g. directly from jena graph)
     * But perhaps we can introduce some caches to speed up if necessary.
     */
    public class OntologyCollection implements Serializable {
        private final Collection<OntInfo> map;

        public OntologyCollection(Collection<OntInfo> c) {
            this.map = c;
        }

        public int size() {
            return map.size(); // no need
        }

        public boolean isEmpty() {
            return map.isEmpty(); // no need
        }

        public void clear() {
            map.clear();
        }

        protected Stream<OntInfo> values() {
            return map.stream();
        }

        protected Stream<OWLOntologyID> keys() {
            return values().map(OntInfo::id);
        }

        public Optional<OntInfo> get(@Nonnull OWLOntologyID key) {
            return values()
                    .filter(o -> o.id().hashCode() == key.hashCode())
                    .filter(o -> key.equals(o.id())).findFirst();
        }

        public boolean contains(@Nonnull OWLOntologyID key) {
            return values().filter(o -> o.id().hashCode() == key.hashCode()).anyMatch(o -> key.equals(o.id()));
        }

        public OntInfo add(OntologyModel o) {
            OntInfo res = new OntInfo(o);
            map.add(res);
            return res;
        }

        public Optional<OntInfo> remove(@Nonnull OWLOntologyID id) {
            Optional<OntInfo> res = get(id);
            res.ifPresent(map::remove);
            return res;
        }
    }

    /**
     * Container for {@link OntologyModel}.
     * For internal usage only.
     * It has been introduced to provide better synchronization of ontology different parts and also for serialization.
     * The {@link InternalModel} are not Serializable since it is an extended Jena-model and could be considered separately.
     * Also it has no reference to manager by the same architectural reasons.
     * So it is important to be sure that this container and internal model are in consistent state...
     * This applies mainly to the load and write configs which contain in the {@link ConfigProvider.Config} instance.
     *
     * @see OntologyManagerImpl#setOntologyLoaderConfiguration(OWLOntologyLoaderConfiguration)
     * @see OntologyManagerImpl#setOntologyWriterConfiguration(OWLOntologyWriterConfiguration)
     * @see ModelConfig
     */
    public class OntInfo implements Serializable {
        private final OntologyModel ont;
        private final ConfigProvider.Config conf;
        private IRI documentIRI;
        private OWLImportsDeclaration declaration;
        private OWLDocumentFormat format;

        public OntInfo(@Nonnull OntologyModel ont) {
            this.ont = ont;
            this.conf = ((ConfigProvider) ont).getConfig();
        }

        @Nonnull
        public OWLOntologyID id() {
            return ont.getOntologyID();
        }

        public OntologyModel get() {
            return ont;
        }

        public OntInfo addFormat(OWLDocumentFormat format) {
            this.format = format;
            return this;
        }

        public OntInfo addDocumentIRI(IRI iri) {
            documentIRI = iri;
            return this;
        }

        public OntInfo addImportDeclaration(OWLImportsDeclaration declaration) {
            this.declaration = declaration;
            return this;
        }

        @Nullable
        public IRI getDocumentIRI() {
            return documentIRI;
        }

        @Nullable
        public OWLDocumentFormat getFormat() {
            return format;
        }

        @Nonnull
        public ConfigProvider.Config getModelConfig() {
            return conf;
        }

        @Nullable
        public OWLImportsDeclaration getImportDeclaration() {
            return declaration != null ? declaration : id().getOntologyIRI().map(dataFactory::getOWLImportsDeclaration).orElse(null);
        }
    }

    /**
     * This implementation of {@link ConfigProvider.Config} has a reference to manager inside.
     * This is in order to provide access to the manager's settings also.
     */
    public static class ModelConfig implements ConfigProvider.Config, Serializable {
        private OntLoaderConfiguration modelLoaderConf;
        private OntWriterConfiguration modelWriterConf;
        private OntologyManagerImpl manager;

        public ModelConfig(OntologyManagerImpl m) {
            this.manager = m;
        }

        public OntologyManagerImpl manager() {
            return manager;
        }

        public OntologyManagerImpl setManager(OntologyManagerImpl other) {
            OntologyManagerImpl res = this.manager;
            this.manager = other;
            return res;
        }

        public boolean setLoaderConf(OntLoaderConfiguration conf) {
            if (Objects.equals(loaderConfig(), conf)) return false;
            this.modelLoaderConf = conf;
            return true;
        }

        public boolean setWriterConf(OntWriterConfiguration conf) {
            if (Objects.equals(writerConfig(), conf)) return false;
            this.modelWriterConf = conf;
            return true;
        }

        @Override
        public OWLDataFactory dataFactory() {
            return manager.getOWLDataFactory();
        }

        @Override
        public OntLoaderConfiguration loaderConfig() {
            return this.modelLoaderConf == null ? manager.getOntologyLoaderConfiguration() : this.modelLoaderConf;
        }

        @Override
        public OntWriterConfiguration writerConfig() {
            return this.modelWriterConf == null ? manager.getOntologyWriterConfiguration() : this.modelWriterConf;
        }

        @Override
        public boolean parallel() {
            return manager.isConcurrent();
        }
    }
}
