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

package ru.avicomp.ontapi;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.impl.WrappedGraph;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.JenaException;
import org.apache.jena.shared.PrefixMapping;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.config.OntWriterConfiguration;
import ru.avicomp.ontapi.internal.InternalCache;
import ru.avicomp.ontapi.internal.InternalConfig;
import ru.avicomp.ontapi.internal.InternalModel;
import ru.avicomp.ontapi.internal.InternalModelHolder;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.utils.Graphs;

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
 * An ONT-API default implementation of {@link OntologyManager Ontology Manager}.
 * <p>
 * Created by @szuev on 03.10.2016.
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl</a>
 */
@SuppressWarnings("WeakerAccess")
public class OntologyManagerImpl implements OntologyManager, OWLOntologyFactory.OWLOntologyCreationHandler, Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyManagerImpl.class);
    private static final long serialVersionUID = -4764329329583952286L;
    // listeners:
    protected final ListenersHolder listeners = new ListenersHolder();
    // configs:
    protected OntConfig config;
    protected OntLoaderConfiguration loaderConfig;
    protected OntWriterConfiguration writerConfig;
    // Loading Cache for IRIs, that is shared between ontologies that belong to this manager.
    protected transient InternalCache.Loading<String, IRI> iris;
    // OntologyFactory collection:
    protected final RWLockedCollection<OWLOntologyFactory> ontologyFactories;
    // IRI mappers
    protected final RWLockedCollection<OWLOntologyIRIMapper> documentIRIMappers;
    // Graph mappers (sine 1.0.1):
    protected final RWLockedCollection<DocumentSourceMapping> documentSourceMappers;
    // OWL-API parsers (i.e. alternative to jena way to read):
    protected final RWLockedCollection<OWLParserFactory> parserFactories;
    // OWL-API storers (i.e. alternative to jena way to save):
    protected final RWLockedCollection<OWLStorerFactory> ontologyStorers;
    // primary parameters:
    protected final ReadWriteLock lock;
    protected final DataFactory dataFactory;
    // the collection of ontologies:
    protected final OntologyCollection<OntInfo> content;

    /**
     * Constructs a manager instance which is ready to use.
     * OntologyFactory as parameter since a manager without it is useless.
     *
     * @param dataFactory     {@link DataFactory} - a factory to provide OWL Axioms and other OWL objects,
     *                        not {@code null}
     * @param ontologyFactory {@link OntologyFactory} - a factory to create and load ontologies, not {@code null}
     * @param readWriteLock   {@link ReadWriteLock} - lock to synchronize multithreading behaviour,
     *                        can be not {@code null} for a single-thread applications
     */
    public OntologyManagerImpl(DataFactory dataFactory, OntologyFactory ontologyFactory, ReadWriteLock readWriteLock) {
        this(dataFactory, readWriteLock, PriorityCollectionSorting.ON_SET_INJECTION_ONLY);
        this.ontologyFactories.add(Objects.requireNonNull(ontologyFactory, "Null Ontology Factory"));
    }

    /**
     * Constructs an empty manager instance with the given settings.
     * Notice: the returned instance is not ready to use:
     * there is no any OntologyFactory inside to produce new ontologies.
     *
     * @param dataFactory {@link OWLDataFactory}, not not {@code null}
     * @param lock        {@link ReadWriteLock} or {@code null} for non-concurrent instance
     * @param sorting     {@link PriorityCollectionSorting} OWL-API enum, can be not {@code null};
     *                    Can't avoid using this parameter that is actually useless in ONT-API
     */
    public OntologyManagerImpl(DataFactory dataFactory,
                               ReadWriteLock lock, PriorityCollectionSorting sorting) {
        this.dataFactory = Objects.requireNonNull(dataFactory, "Null Data Factory");
        this.lock = lock == null ? NoOpReadWriteLock.NO_OP_RW_LOCK : lock;
        PriorityCollectionSorting _sorting = sorting == null ? PriorityCollectionSorting.NEVER : sorting;
        this.documentIRIMappers = new RWLockedCollection<>(this.lock, _sorting);
        this.documentSourceMappers = new RWLockedCollection<>(this.lock);
        this.ontologyFactories = new RWLockedCollection<OWLOntologyFactory>(this.lock, _sorting) {
            @Override
            protected void onAdd(OWLOntologyFactory f) {
                if (f instanceof OntologyFactory) return;
                throw new OntApiException("Wrong argument: " + f + ". " +
                        "Only " + OntologyFactory.class.getSimpleName() + " can be accepted.");
            }
        };
        this.parserFactories = new RWLockedCollection<>(this.lock, _sorting);
        this.ontologyStorers = new RWLockedCollection<>(this.lock, _sorting);
        this.config = OntConfig.createConfig(this.lock);
        this.content = new OntologyCollectionImpl<>(this.lock);
        this.iris = createIRICache();
    }

    /**
     * Creates a fresh {@link IRI} cache instance depending on this manager settings.
     * Note if caching is disabled ({@link OntConfig#getManagerIRIsCacheSize()} is not positive),
     * a fake empty cache is returned.
     *
     * @return {@link InternalCache.Loading} for {@link IRI}s
     */
    protected InternalCache.Loading<String, IRI> createIRICache() {
        int size = this.config.getManagerIRIsCacheSize();
        if (size < 0) {
            return InternalCache.createEmpty().asLoading(IRI::create);
        }
        return InternalCache.createBounded(IRI::create, NoOpReadWriteLock.isConcurrent(lock), size);
    }

    /**
     * Answers {@code true} if this manager must be thread-safe.
     *
     * @return boolean
     */
    public boolean isConcurrent() {
        return NoOpReadWriteLock.isConcurrent(lock);
    }

    /**
     * Returns a Read Write Lock associated with this manager.
     *
     * @return {@link ReadWriteLock}
     */
    @Nonnull
    public ReadWriteLock getLock() {
        return lock;
    }

    @Override
    public DataFactory getOWLDataFactory() {
        return dataFactory;
    }

    /**
     * Gets a manager's configuration.
     *
     * @return {@link OntConfig}
     */
    @Override
    public OntConfig getOntologyConfigurator() {
        getLock().readLock().lock();
        try {
            return this.config;
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * Sets a manager's configuration.
     *
     * @param conf {@link OntologyConfigurator}
     */
    @Override
    public void setOntologyConfigurator(OntologyConfigurator conf) {
        getLock().writeLock().lock();
        try {
            int size = this.config.getManagerIRIsCacheSize();
            this.config = OntConfig.withLock(OWLAdapter.get().asONT(conf), lock);
            if (size != this.config.getManagerIRIsCacheSize()) {
                // reset cache:
                this.iris = createIRICache();
            }
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * Sets {@link OntLoaderConfiguration} config to the manager.
     *
     * @param config {@link OWLOntologyLoaderConfiguration}
     */
    @Override
    public void setOntologyLoaderConfiguration(@Nullable OWLOntologyLoaderConfiguration config) {
        getLock().writeLock().lock();
        try {
            // NOTE: OWL-API-contract tests shows that the configurator may shared, so need to pass the same instance
            // This fact greatly and unnecessarily complicates the matter
            OntLoaderConfiguration conf = OWLAdapter.get().asONT(config);
            if (ModelConfig.hasChanges(getOntLoaderConfiguration(), conf)) {
                content.values()
                        .filter(x -> x.getModelConfig().useManagerConfig())
                        .map(OntInfo::get)
                        .forEach(OntologyModel::clearCache);
            }
            this.loaderConfig = conf;
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * Gets the manager's loader configuration.
     *
     * @return {@link OntLoaderConfiguration}
     */
    @Override
    public OntLoaderConfiguration getOntologyLoaderConfiguration() {
        getLock().readLock().lock();
        try {
            return getOntLoaderConfiguration();
        } finally {
            getLock().readLock().unlock();
        }
    }

    protected OntLoaderConfiguration getOntLoaderConfiguration() {
        if (loaderConfig != null) {
            return loaderConfig;
        }
        return config.buildLoaderConfiguration();
    }

    /**
     * Sets {@link OWLOntologyWriterConfiguration writer config} to the manager and also passes it inside interior models.
     *
     * @param conf {@link OWLOntologyWriterConfiguration}
     */
    @Override
    public void setOntologyWriterConfiguration(@Nullable OWLOntologyWriterConfiguration conf) {
        getLock().writeLock().lock();
        try {
            if (Objects.equals(writerConfig, conf)) return;
            writerConfig = OWLAdapter.get().asONT(conf);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * Gets the manager's writer configuration.
     *
     * @return {@link OntWriterConfiguration}
     */
    @Override
    @Nonnull
    public OntWriterConfiguration getOntologyWriterConfiguration() {
        getLock().readLock().lock();
        try {
            if (writerConfig != null) {
                return writerConfig;
            }
            return this.config.buildWriterConfiguration();
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @return {@link org.semanticweb.owlapi.util.PriorityCollection} of {@link OWLOntologyFactory}
     */
    @Override
    public RWLockedCollection<OWLOntologyFactory> getOntologyFactories() {
        return ontologyFactories;
    }

    @Override
    public RWLockedCollection<OWLStorerFactory> getOntologyStorers() {
        return ontologyStorers;
    }

    @Override
    public RWLockedCollection<OWLParserFactory> getOntologyParsers() {
        return parserFactories;
    }

    @Override
    public RWLockedCollection<OWLOntologyIRIMapper> getIRIMappers() {
        return documentIRIMappers;
    }

    @Override
    public RWLockedCollection<DocumentSourceMapping> getDocumentSourceMappers() {
        return documentSourceMappers;
    }

    /**
     * @param strategy {@link OWLOntologyChangeBroadcastStrategy}
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
     */
    @Override
    public void addOntologyChangeListener(@Nonnull OWLOntologyChangeListener listener,
                                          @Nonnull OWLOntologyChangeBroadcastStrategy strategy) {
        getLock().writeLock().lock();
        try {
            listeners.addOntologyChangeListener(listener, strategy);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangeListener}
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
     */
    @Override
    public void addOntologyChangeProgessListener(@Nonnull OWLOntologyChangeProgressListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.addOntologyChangeProgressListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangeProgressListener}
     */
    @Override
    public void removeOntologyChangeProgessListener(@Nonnull OWLOntologyChangeProgressListener listener) {
        getLock().writeLock().lock();
        try {
            listeners.removeOntologyChangeProgressListener(listener);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param id {@link OWLOntologyID}
     * @return {@link OntologyModel}
     */
    @Override
    public OntologyModel createOntology(@Nonnull OWLOntologyID id) {
        getLock().writeLock().lock();
        try {
            return create(id).get();
        } catch (OWLOntologyCreationException e) {
            throw new OntApiException("Unable to create ontology " + id, e);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param ontologyID {@link OWLOntologyID}
     * @return {@link OntInfo} the container with ontology
     * @throws OWLOntologyCreationException        if creation is not possible either because the
     *                                             ontology already exists or because of fail while compute document-iri
     * @throws OWLOntologyFactoryNotFoundException if no suitable factory found,
     */
    protected OntInfo create(OWLOntologyID ontologyID) throws OWLOntologyCreationException, OWLOntologyFactoryNotFoundException {
        OntologyID id = OntologyID.asONT(ontologyID);
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
        for (OWLOntologyFactory factory : getOntologyFactories()) {
            if (!factory.canCreateFromDocumentIRI(doc)) {
                continue;
            }
            factory.createOWLOntology(this, id, doc, this);
            return content.get(id).orElseThrow(() -> new UnknownOWLOntologyException(id)).addDocumentIRI(doc);
        }
        throw new OWLOntologyFactoryNotFoundException(doc);
    }

    /**
     * @param iri                   the IRI of Ontology
     * @param ontologies            Stream of {@link OWLOntology}s
     * @param copyLogicalAxiomsOnly boolean
     * @return {@link OntologyModel}
     */
    @Override
    public OntologyModel createOntology(@Nonnull IRI iri,
                                        @Nonnull Stream<OWLOntology> ontologies,
                                        boolean copyLogicalAxiomsOnly) {
        getLock().writeLock().lock();
        try {
            OntologyID id = OntologyID.create(Objects.requireNonNull(iri));
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
     */
    @Override
    public OntologyModel createOntology(@Nonnull Stream<OWLAxiom> axioms, @Nonnull IRI iri) {
        getLock().writeLock().lock();
        try {
            OntologyID id = OntologyID.create(Objects.requireNonNull(iri));
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
     * @param conf  {@link OntLoaderConfiguration} the config with settings
     * @return {@link OntologyModel}
     * @since 1.2.0
     */
    @Override
    public OntologyModel addOntology(@Nonnull Graph graph, @Nonnull OntLoaderConfiguration conf) {
        getLock().writeLock().lock();
        try {
            OntologyID id = OntGraphUtils.getOntologyID(graph);
            Map<OntologyID, Graph> graphs = OntGraphUtils.toGraphMap(graph);
            DocumentSourceMapping mapping = i -> graphs.entrySet()
                    .stream()
                    .filter(e -> matchIDs(e.getKey(), i))
                    .map(e -> OntGraphDocumentSource.wrap(e.getValue()))
                    .findFirst()
                    .orElse(null);
            RWLockedCollection<DocumentSourceMapping> store = getDocumentSourceMappers();
            try {
                store.add(mapping);
                return loadOntologyFromOntologyDocument(mapping.map(id), conf);
            } finally {
                store.remove(mapping);
            }
        } catch (OWLOntologyCreationException e) {
            throw new OntApiException("Unable put graph into the manager", e);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * Answers if the given Graph ids are matching.
     *
     * @param left  {@link OWLOntologyID}, not {@code null}
     * @param right {@link OWLOntologyID}, not {@code null}
     * @return {@code true} if ids are matching
     */
    public static boolean matchIDs(OWLOntologyID left, OWLOntologyID right) {
        // anonymous ?
        if (left.equals(right)) return true;
        // version iri is a primary according to specification
        IRI iri = right.getVersionIRI().orElse(right.getOntologyIRI().orElse(null));
        // anonymous .
        if (iri == null) return false;
        // check either ontology or version iri equal the given iri
        return left.match(iri);
    }

    /**
     * @param id {@link OWLOntologyID}
     * @return {@link IRI}
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
     */
    @Nullable
    protected IRI getDocumentIRIFromMappers(OWLOntologyID id) {
        IRI defaultIRI = id.getDefaultDocumentIRI().orElse(null);
        if (defaultIRI == null) {
            return null;
        }
        return mapIRI(defaultIRI).orElse(defaultIRI);
    }

    /**
     * Finds a {@link OWLOntologyDocumentSource} from the {@link #getDocumentSourceMappers() mappers collection}.
     *
     * @param id {@link OWLOntologyID}, not {@code null}
     * @return {@code Optional} around {@link OWLOntologyDocumentSource}
     */
    protected Optional<OWLOntologyDocumentSource> findDocumentSource(OWLOntologyID id) {
        return getDocumentSourceMappers().stream().map(x -> x.map(id)).filter(Objects::nonNull).findFirst();
    }

    /**
     * Finds a document iri by the specified iri from mappers
     *
     * @param iri {@link IRI}
     * @return Optional around document iri
     * @see #getIRIMappers()
     * @see OWLOntologyIRIMapper
     */
    protected Optional<IRI> mapIRI(IRI iri) {
        return getIRIMappers().stream()
                .map(m -> m.getDocumentIRI(iri))
                .filter(Objects::nonNull)
                .findFirst();
    }

    /**
     * the difference: in case there are many ontologies with the same IRI it chooses the first match on version or ontology iri, not any.
     *
     * @param iri {@link IRI}
     * @return {@link OntologyModel}
     */
    @Override
    @Nullable
    public OntologyModel getOntology(@Nonnull IRI iri) {
        getLock().readLock().lock();
        try {
            OntologyID id = OntologyID.create(Objects.requireNonNull(iri));
            Optional<OntInfo> res = content.get(id);
            if (!res.isPresent()) {
                res = content.values().filter(e -> e.getOntologyID().match(iri)).findFirst();
            }
            return res.map(OntInfo::get).orElse(null);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * the difference: in case there are many ontologies with the same ID it chooses the first on the iri ignoring version,
     * while original method chooses any.
     *
     * @param id {@link OWLOntologyID}
     * @return {@link OntologyModel}
     */
    @Override
    public OntologyModel getOntology(@Nonnull OWLOntologyID id) {
        getLock().readLock().lock();
        try {
            return ontology(id).orElse(null);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * Finds ontology by the id or iri.
     *
     * @param id {@link OWLOntologyID}
     * @return Optional around {@link OntologyModel}
     */
    protected Optional<OntologyModel> ontology(OWLOntologyID id) {
        Optional<OntInfo> res = content.get(id);
        if (!res.isPresent() && !id.isAnonymous()) {
            IRI iri = id.getOntologyIRI().orElseThrow(() -> new IllegalStateException("Should never happen."));
            res = content.values().filter(e -> e.getOntologyID().matchOntology(iri)).findFirst();
        }
        return res.map(OntInfo::get);
    }

    /**
     * Finds ontology by the given graph.
     *
     * @param graph {@link Graph}
     * @return Optional around {@link OntologyModel}
     */
    protected Optional<OntologyModel> ontology(Graph graph) {
        return content.values().map(OntInfo::get)
                .filter(m -> Graphs.isSameBase(graph, m.asGraphModel().getGraph()))
                .findFirst();
    }

    /**
     * @param iri {@link IRI}
     * @return boolean
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
     */
    @Override
    public boolean contains(@Nonnull OWLOntology ontology) {
        getLock().readLock().lock();
        try {
            return has(ontology);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * Answers iff ontology belongs to the manager
     *
     * @param ontology {@link OWLOntology} to test
     * @return true if the manager has the ontology
     */
    protected boolean has(OWLOntology ontology) {
        return content.values().map(OntInfo::get).anyMatch(o -> Objects.equals(o, ontology));
    }

    /**
     * @param iri {@link IRI}
     * @return boolean
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
     * Clears all ontologies, listeners and maps from the manager. Leave injected factories, storers and parsers.
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
     * Original method's comment:
     * No such ontology has been loaded through an import declaration, but it might have been loaded manually.
     * Using the IRI to retrieve it will either find the ontology or return null.
     * Last possibility is an import by document IRI; if the ontology is not found by IRI, check by document IRI.
     *
     * @param declaration {@link OWLImportsDeclaration}
     * @return {@link OntologyModel} or {@code null}
     */
    @Override
    public OntologyModel getImportedOntology(@Nonnull OWLImportsDeclaration declaration) {
        getLock().readLock().lock();
        try {
            return importedOntology(declaration.getIRI()).orElse(null);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * Finds an ontology by import declaration IRI.
     *
     * @param declaration {@link IRI}
     * @return Optional around the {@link OntologyModel}
     */
    protected Optional<OntologyModel> importedOntology(IRI declaration) {
        return content.values()
                .filter(e -> e.hasImportDeclaration(declaration))
                .map(OntInfo::get)
                .findFirst();
    }

    /**
     * Finds first ontology by specified document iri
     *
     * @param iri {@link IRI}
     * @return Optional around {@link OntologyModel}
     * @see #documentIRIByOntology(OWLOntology)
     */
    protected Optional<OntologyModel> ontologyByDocumentIRI(IRI iri) {
        return content.values().filter(o -> Objects.equals(iri, o.getDocumentIRI())).map(OntInfo::get).findFirst();
    }

    /**
     * Gets document IRI.
     *
     * @param ontology {@link OWLOntology}, not null
     * @return {@link IRI}, not null
     * @throws UnknownOWLOntologyException id ontology not found
     * @throws OntApiException             if document not found
     */
    @Nonnull
    @Override
    public IRI getOntologyDocumentIRI(@Nonnull OWLOntology ontology) {
        getLock().readLock().lock();
        try {
            if (!has(ontology)) throw new UnknownOWLOntologyException(ontology.getOntologyID());
            return documentIRIByOntology(ontology)
                    .orElseThrow(() -> new OntApiException("Null document iri, ontology id=" + ontology.getOntologyID()));
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * Finds a document iri by specified ontology
     *
     * @param ontology {@link OWLOntology}, not null
     * @return Optional around document iri
     * @see #ontologyByDocumentIRI(IRI)
     */
    protected Optional<IRI> documentIRIByOntology(OWLOntology ontology) {
        return content.get(ontology.getOntologyID()).flatMap(i -> Optional.ofNullable(i.getDocumentIRI()));
    }

    /**
     * @param ontology    {@link OWLOntology}
     * @param documentIRI {@link IRI}, the source
     * @throws UnknownOWLOntologyException e
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
     * Note: the difference with the original impl is this method throws an exception if no ontology found,
     * while the original does not.
     *
     * @param ontology {@link OWLOntology}
     * @param format   {@link OWLDocumentFormat}
     * @throws UnknownOWLOntologyException in case no ontology is found
     * @see org.semanticweb.owlapi.model.OWLOntologyFactory.OWLOntologyCreationHandler#setOntologyFormat(OWLOntology, OWLDocumentFormat)
     */
    @Override
    public void setOntologyFormat(@Nonnull OWLOntology ontology, @Nonnull OWLDocumentFormat format) {
        getLock().writeLock().lock();
        try {
            OWLOntologyID id = ontology.getOntologyID();
            content.get(id).orElseThrow(() -> new UnknownOWLOntologyException(id)).addFormat(format);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return {@link OWLDocumentFormat} or {@code null}
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
     * Adds the given ontology to the internal collection.
     *
     * @param ont {@link OWLOntology}
     * @throws ClassCastException ex
     */
    @Override
    public void ontologyCreated(@Nonnull OWLOntology ont) {
        // This method is called when a factory that we have asked to create or
        // load an ontology has created the ontology. We add the ontology to the
        // set of loaded ontologies.
        getLock().writeLock().lock();
        try {
            content.add(new OntInfo((OntologyModel) ont));
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return Stream of {@link OWLOntology}
     */
    @Override
    public Stream<OWLOntology> directImports(@Nonnull OWLOntology ontology) {
        getLock().readLock().lock();
        try {
            if (!contains(ontology)) {
                throw new UnknownOWLOntologyException(ontology.getOntologyID());
            }
            return ontology.importsDeclarations()
                    .map(this::getImportedOntology)
                    .map(OWLOntology.class::cast).filter(Objects::nonNull);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return Stream of {@link OWLOntology}
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
     */
    protected Set<OWLOntology> getImports(OWLOntology ont, Set<OWLOntology> result) {
        directImports(ont).filter(result::add).forEach(o -> getImports(o, result));
        return result;
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return Stream of {@link OWLOntology}
     */
    @Override
    public Stream<OWLOntology> importsClosure(@Nonnull OWLOntology ontology) {
        getLock().readLock().lock();
        try {
            Set<OWLOntology> res = new HashSet<>();
            collectImportsClosure(ontology, res);
            return res.stream();
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @param res      Set {@link OWLOntology}
     */
    protected void collectImportsClosure(OWLOntology ontology, Set<OWLOntology> res) {
        res.add(ontology);
        directImports(ontology).filter(o -> !res.contains(o)).forEach(o -> collectImportsClosure(o, res));
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return List of {@link OWLOntology}
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
     */
    @Override
    public Stream<OWLOntology> ontologies() {
        getLock().readLock().lock();
        try {
            return content.values().map(OntInfo::get);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param iri {@link IRI}
     * @return Stream of {@link OWLOntologyID}
     */
    @Override
    public Stream<OWLOntologyID> ontologyIDsByVersion(@Nonnull IRI iri) {
        getLock().readLock().lock();
        try {
            return content.keys().filter(o -> o.matchVersion(iri)).map(OWLOntologyID.class::cast);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * @param ont   {@link OWLOntology}
     * @param axiom {@link OWLAxiom}
     * @return {@link ChangeApplied}
     */
    @Override
    public ChangeApplied addAxiom(@Nonnull OWLOntology ont, @Nonnull OWLAxiom axiom) {
        return applyChanges(Collections.singletonList(new AddAxiom(ont, axiom)));
    }

    /**
     * @param ont    {@link OWLOntology}
     * @param axioms Stream of {@link OWLAxiom}
     * @return {@link ChangeApplied}
     */
    @Override
    public ChangeApplied addAxioms(@Nonnull OWLOntology ont, @Nonnull Stream<? extends OWLAxiom> axioms) {
        return applyChanges(axioms.map(ax -> new AddAxiom(ont, ax)).collect(Collectors.toList()));
    }

    /**
     * @param ont   {@link OWLOntology}
     * @param axiom {@link OWLAxiom}
     * @return {@link ChangeApplied}
     */
    @Override
    public ChangeApplied removeAxiom(@Nonnull OWLOntology ont, @Nonnull OWLAxiom axiom) {
        return applyChanges(Collections.singletonList(new RemoveAxiom(ont, axiom)));
    }

    /**
     * @param ont    {@link OWLOntology}
     * @param axioms Stream of {@link OWLAxiom}
     * @return {@link ChangeApplied}
     */
    @Override
    public ChangeApplied removeAxioms(@Nonnull OWLOntology ont, @Nonnull Stream<? extends OWLAxiom> axioms) {
        return applyChanges(axioms.map(ax -> new RemoveAxiom(ont, ax)).collect(Collectors.toList()));
    }

    /**
     * @param change {@link OWLOntologyChange}
     * @return {@link ChangeApplied}
     */
    @Override
    public ChangeApplied applyChange(@Nonnull OWLOntologyChange change) {
        return applyChanges(Collections.singletonList(change));
    }

    /**
     * Copy-paste from <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#applyChangesAndGetDetails(List)</a>.
     *
     * @param changes List of {@link OWLOntologyChange}s
     * @return {@link ChangeDetails}
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
     */
    protected void actuallyApply(List<? extends OWLOntologyChange> changes,
                                 AtomicBoolean rollbackRequested,
                                 AtomicBoolean allNoOps,
                                 List<OWLOntologyChange> appliedChanges) {
        for (OWLOntologyChange change : changes) {
            // once rollback is requested by a failed change, do not carry
            // out any more changes
            if (rollbackRequested.get()) {
                continue;
            }
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

    /**
     * @param appliedChanges List of {@link OWLOntologyChange}
     */
    protected void rollBack(List<OWLOntologyChange> appliedChanges) {
        for (OWLOntologyChange c : appliedChanges) {
            if (enactChangeApplication(c.reverseChange()) != ChangeApplied.UNSUCCESSFULLY) {
                continue;
            }
            // rollback could not complete, throw an exception
            throw new OWLRuntimeException("Rollback of changes unsuccessful: " +
                    "Change " + c + " could not be rolled back");
        }
    }

    /**
     * @param change {@link OWLOntologyChange}
     * @return {@link ChangeApplied}
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
     */
    protected boolean isChangeApplicable(OWLOntologyChange change) {
        OWLOntologyID id = change.getOntology().getOntologyID();
        Optional<ModelConfig> conf = content.get(id)
                .map(OntInfo::getModelConfig);
        return !(conf.isPresent()
                && !conf.get().isLoadAnnotationAxioms()
                && change.isAddAxiom()
                && change.getAxiom() instanceof OWLAnnotationAxiom);
    }

    /**
     * @param change {@link OWLOntologyChange}
     */
    protected void checkForOntologyIDChange(OWLOntologyChange change) {
        if (!(change instanceof SetOntologyID)) {
            return;
        }
        SetOntologyID setID = (SetOntologyID) change;
        Optional<OntologyModel> existing = content.get(setID.getNewOntologyID()).map(OntInfo::get);
        OWLOntology o = setID.getOntology();
        if (existing.isPresent() && !o.equals(existing.get()) && !o.equalAxioms(existing.get())) {
            LOGGER.warn("uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#checkForOntologyIDChange:: " +
                    "existing:{}, new:{}", existing, o);
            throw new OWLOntologyRenameException(setID.getChangeData(), setID.getNewOntologyID());
        }
    }

    /**
     * This method has the same signature as the original but totally different meaning.
     * In ONT-API it is for making some related changes with the ontology from {@link ImportChange},
     * not for keeping correct state of manager.
     *
     * @param change {@link OWLOntologyChange}
     */
    protected void checkForImportsChange(OWLOntologyChange change) {
        if (!change.isImportChange()) {
            return;
        }
        OWLImportsDeclaration declaration = ((ImportChange) change).getImportDeclaration();
        OntologyModel ontology = (OntologyModel) change.getOntology();
        OWLOntologyID id = ontology.getOntologyID();
        Optional<OntWriterConfiguration> conf = content.get(id).map(OntInfo::getModelConfig)
                .map(ModelConfig::getWriterConfig);
        if (!conf.isPresent() || !conf.get().isControlImports()) {
            return;
        }
        OntologyModel importedOntology = getImportedOntology(declaration);
        if (importedOntology == null) {
            return;
        }
        List<OWLOntologyChange> relatedChanges;
        if (change instanceof AddImport) {
            // remove duplicated declarations if they are present in the imported ontology
            relatedChanges = importedOntology.axioms(AxiomType.DECLARATION, Imports.INCLUDED)
                    .filter(ontology::containsAxiom)
                    .map(a -> new RemoveAxiom(ontology, a)).collect(Collectors.toList());
        } else {
            // return back declarations which are still in use:
            relatedChanges = importedOntology.signature(Imports.INCLUDED)
                    .filter(ontology::containsReference)
                    .map(e -> getOWLDataFactory().getOWLDeclarationAxiom(e))
                    .map(a -> new AddAxiom(ontology, a)).collect(Collectors.toList());
        }
        relatedChanges.forEach(ontology::applyDirectChange);
    }

    /**
     * @param source   {@link OWLOntology}
     * @param settings {@link OntologyCopy}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException ex
     */
    @Override
    public OntologyModel copyOntology(@Nonnull OWLOntology source,
                                      @Nonnull OntologyCopy settings) throws OWLOntologyCreationException {
        getLock().writeLock().lock();
        try {
            OntApiException.notNull(source, "Null ontology.");
            OntApiException.notNull(settings, "Null settings.");
            OWLOntologyManager m = source.getOWLOntologyManager();
            OntologyModel res;
            switch (settings) {
                case MOVE:
                    if (!(source instanceof OntologyModel)) {
                        throw new OWLOntologyCreationException(
                                String.format("Can't move %s: not an %s. Use %s or %s parameter.",
                                        source.getOntologyID(), OntologyModel.class.getSimpleName(),
                                        OntologyCopy.DEEP, OntologyCopy.SHALLOW));
                    }
                    // todo: what about ontologies with imports? what about moving between managers with different lock ?
                    res = (OntologyModel) source;
                    ontologyCreated(res);
                    break;
                case SHALLOW:
                case DEEP:
                    // todo: in case of ONT replace coping axioms with coping graphs
                    OntInfo info = create(source.getOntologyID());
                    OntologyModel o = info.get();
                    AxiomType.AXIOM_TYPES.forEach(t -> OntologyManagerImpl.this.addAxioms(o, source.axioms(t)));
                    source.annotations().forEach(a -> applyChange(new AddOntologyAnnotation(o, a)));
                    source.importsDeclarations().forEach(a -> applyChange(new AddImport(o, a)));
                    res = o;
                    break;
                default:
                    throw new OWLRuntimeException("settings value not understood: " + settings);
            }
            if (settings == OntologyCopy.MOVE || settings == OntologyCopy.DEEP) {
                // todo: what about configs?
                setOntologyDocumentIRI(res, m.getOntologyDocumentIRI(source));
                OWLDocumentFormat ontologyFormat = m.getOntologyFormat(source);
                if (ontologyFormat != null) {
                    setOntologyFormat(res, ontologyFormat);
                }
            }
            if (settings == OntologyCopy.MOVE) {
                m.removeOntology(source);
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
     */
    @Override
    public OntologyModel loadOntology(@Nonnull IRI source) throws OWLOntologyCreationException {
        getLock().writeLock().lock();
        try {
            return load(source, getOntologyLoaderConfiguration(), false);
        } finally {
            getLock().writeLock().unlock();
        }
    }

    /**
     * @param source {@link OWLOntologyDocumentSource}
     * @param conf   {@link OWLOntologyLoaderConfiguration}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException ex
     */
    @Override
    public OntologyModel loadOntologyFromOntologyDocument(@Nonnull OWLOntologyDocumentSource source,
                                                          @Nonnull OWLOntologyLoaderConfiguration conf) throws OWLOntologyCreationException {
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
     * @param conf        {@link OWLOntologyLoaderConfiguration}
     * @param allowExists boolean
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException ex
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyManagerImpl.java#L901'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl(IRI, boolean, OWLOntologyLoaderConfiguration)</a>
     */
    protected OntologyModel load(IRI iri,
                                 OWLOntologyLoaderConfiguration conf,
                                 boolean allowExists) throws OWLOntologyCreationException {
        // Check for matches on the ontology IRI first
        OntologyID id = OntologyID.create(Objects.requireNonNull(iri));
        OntologyModel res = getOntology(id);
        if (res != null) {
            return res;
        }
        Optional<OWLOntologyDocumentSource> source = findDocumentSource(id);
        if (source.isPresent()) {
            return load(iri, source.get(), conf);
        }
        IRI documentIRI = getDocumentIRIFromMappers(id);
        if (documentIRI == null) {
            // Nothing we can do here.
            // can't get a document IRI to load the ontology from.
            throw new OntologyIRIMappingNotFoundException(iri);
        }
        // The ontology might be being loaded, but its IRI might
        // not have been set (as is probably the case with RDF/XML!)
        Optional<OntologyModel> op = ontologyByDocumentIRI(documentIRI);
        if (op.isPresent() && !allowExists) {
            throw new OWLOntologyDocumentAlreadyExistsException(documentIRI);
        }
        if (op.isPresent()) {
            return op.get();
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
     */
    protected OntologyModel load(@Nullable IRI iri,
                                 OWLOntologyDocumentSource source,
                                 OWLOntologyLoaderConfiguration conf) throws OWLOntologyCreationException {
        listeners.fireStartedLoadingEvent(OntologyID.create(iri), source.getDocumentIRI());
        Exception ex = null;
        OWLOntologyID id = new OntologyID();
        try {
            OntologyModel res = load(source, conf);
            id = res.getOntologyID();
            return res;
        } catch (UnloadableImportException | OWLOntologyCreationException e) {
            ex = e;
            throw e;
        } catch (OWLRuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OWLOntologyCreationException) {
                ex = (OWLOntologyCreationException) cause;
                throw (OWLOntologyCreationException) cause;
            }
            throw e;
        } finally {
            listeners.fireFinishedLoadingEvent(id, source.getDocumentIRI(), ex);
        }
    }

    /**
     * @param source {@link OWLOntologyDocumentSource}
     * @param conf   {@link OWLOntologyLoaderConfiguration}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException        can't load
     * @throws OWLOntologyFactoryNotFoundException no factory
     */
    protected OntologyModel load(OWLOntologyDocumentSource source, OWLOntologyLoaderConfiguration conf)
            throws OWLOntologyCreationException, OWLOntologyFactoryNotFoundException {
        for (OWLOntologyFactory factory : getOntologyFactories()) {
            if (!factory.canAttemptLoading(source))
                continue;
            try {
                OntologyModel res = (OntologyModel) factory.loadOWLOntology(this, source, this, conf);
                OWLOntologyID id = res.getOntologyID();
                return content.get(id).orElseThrow(() -> new UnknownOWLOntologyException(id))
                        .addDocumentIRI(source.getDocumentIRI()).get();
            } catch (OWLOntologyRenameException e) {
                // original comment: we loaded an ontology from a document and
                // the ontology turned out to have an IRI the same as a previously loaded ontology
                throw new OWLOntologyAlreadyExistsException(e.getOntologyID(), e);
            }
        }
        throw new OWLOntologyFactoryNotFoundException(source.getDocumentIRI());
    }

    /**
     * Loads an ontology by import declaration.
     * No lock.
     *
     * @param declaration {@link IRI}, not null
     * @param conf        {@link OWLOntologyLoaderConfiguration}, not null
     * @return {@link OntologyModel}, can be null
     * @throws OWLOntologyCreationException ex
     */
    protected OntologyModel loadImports(IRI declaration,
                                        OWLOntologyLoaderConfiguration conf) throws OWLOntologyCreationException {
        listeners.incrementImportsLoadCount();
        try {
            return load(declaration, conf, true);
        } catch (OWLOntologyCreationException e) {
            if (MissingImportHandlingStrategy.THROW_EXCEPTION.equals(conf.getMissingImportHandlingStrategy())) {
                throw e;
            } else {
                // Silent
                MissingImportEvent evt = new MissingImportEvent(declaration, e);
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
     */
    @Override
    public void makeLoadImportRequest(@Nonnull OWLImportsDeclaration declaration,
                                      @Nonnull OWLOntologyLoaderConfiguration conf) {
        getLock().writeLock().lock();
        try {
            IRI dec = declaration.getIRI();
            if (conf.isIgnoredImport(dec)) return;
            if (importedOntology(dec).isPresent()) return;
            try {
                OntologyModel m = loadImports(dec, conf);
                if (m != null) {
                    content.get(m.getOntologyID()).ifPresent(i -> i.addImportDeclaration(dec));
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
     * @see org.semanticweb.owlapi.util.AbstractOWLStorer#storeOntology(OWLOntology, IRI, OWLDocumentFormat)
     */
    @Override
    public void saveOntology(@Nonnull OWLOntology ontology,
                             @Nonnull OWLDocumentFormat ontologyFormat,
                             @Nonnull IRI documentIRI) throws OWLOntologyStorageException {
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
     */
    @Override
    public void saveOntology(@Nonnull OWLOntology ontology,
                             @Nonnull OWLDocumentFormat ontologyFormat,
                             @Nonnull OWLOntologyDocumentTarget documentTarget) throws OWLOntologyStorageException {
        getLock().readLock().lock();
        try {
            write(ontology, ontologyFormat, documentTarget);
        } finally {
            getLock().readLock().unlock();
        }
    }

    /**
     * Writes the specified ontology to the specified output target in the specified ontology format.
     * It is a functional equivalent of the method
     * {@link OWLOntologyManager#saveOntology(OWLOntology, OWLDocumentFormat, OWLOntologyDocumentTarget)}.
     * <p>
     * Please note: currently it does not throw an {@link UnsupportedOperationException} exception
     * in case of the given ontology does not belong to the manager.
     * The reason: contrary to the javadoc in the interface,
     * the original OWL-API (e.g. 5.1.5) implementation of the method {@code saveOntology(...)}
     * does not require ontology to be present inside the manager.
     * Although I am not sure that the described behavior is correct, it is left as it is,
     * since the original method implementation seems to suit the OWL-API users.
     * <p>
     * Also, working with prefixes, which are hidden in the second method parameter (see {@code OWLDocumentFormat}),
     * in the part of handling OWL-API format, as well as in the original OWL-API implementation,
     * is delegated to the particular {@link OWLStorer}.
     * But the behaviour of these srorers is unpredictable and has very poor documentation.
     * Analysing the code shows, that some of the {@link OWLStorer}s
     * (e.g. {@code TurtleStorer}) take into account the specified prefixes,
     * but the others (such as {@code FunctionalSyntaxStorer}, {@code LatexStorer}, etc) do not.
     * Instead, the latter handle prefixes for a given ontology from its manager
     * (using the {@link #getOntologyFormat(OWLOntology)} method),
     * that may not even coincide with this manager (see the first notice).
     * Although, this cannot be correct, the behavior is left as it is (again):
     * a proper solution (i.e. without any hacks like temporary format substitution in try-finally block)
     * is not possible, since these {@link OWLStorer}s are not part of ONT-API and OWL-API-api.
     *
     * @param ontology {@link OWLOntology}, expected to be {@link OntologyModel} belonging to the manager
     * @param doc      {@link OWLDocumentFormat} format
     * @param target   {@link OWLOntologyDocumentTarget}
     * @throws OWLOntologyStorageException if the ontology could not be saved
     * @throws UnknownOWLOntologyException if the specified ontology is not managed by this manager
     * @throws OntApiException             if some other unexpected error occurred
     */
    public void write(OWLOntology ontology,
                      OWLDocumentFormat doc,
                      OWLOntologyDocumentTarget target) throws OWLOntologyStorageException {
        if (!(ontology instanceof OntologyModel))
            throw new OntApiException.Unsupported("Unsupported OWLOntology instance: " + this);
        OntFormat format = OntApiException.notNull(OntFormat.get(doc), "Can't determine format: " + doc);

        OntologyModel ont = (OntologyModel) ontology;
        if (!format.isJena()) {
            ((InternalModelHolder) ont).getBase().clearCacheIfNeeded();
            try {
                for (OWLStorerFactory storer : getOntologyStorers()) {
                    OWLStorer writer = storer.createStorer();
                    if (!writer.canStoreOntology(doc)) {
                        continue;
                    }
                    writer.storeOntology(ont, target, doc);
                    return;
                }
                throw new OWLStorerNotFoundException(doc);
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
                LOGGER.debug("Save {} to {}", ont.getOntologyID(), iri);
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
            throw new OWLOntologyStorageException("Null output stream, format = " + doc);
        }
        Graph graph = ont.asGraphModel().getBaseGraph();
        if (doc.isPrefixOWLDocumentFormat()) {
            PrefixMapping pm = OntGraphUtils.prefixMapping(doc.asPrefixOWLDocumentFormat());
            graph = new WrappedGraph(graph) {

                @Override
                public PrefixMapping getPrefixMapping() {
                    return pm;
                }
            };
        }
        try {
            RDFDataMgr.write(os, graph, format.getLang());
        } catch (JenaException e) {
            throw new OWLOntologyStorageException("Can't save " + ont.getOntologyID() + ". Format=" + format, e);
        }
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
     * This is a 'hack' methods to provide correct serialization.
     * It fixes graph links between different models:
     * ontology A with ontology B in the imports should have also {@link UnionGraph} inside,
     * that consists of the base graph from A and the base graph from B.
     *
     * @param in {@link ObjectInputStream}
     * @throws IOException            exception
     * @throws ClassNotFoundException exception
     * @see OntBaseModelImpl#readObject(ObjectInputStream)
     */
    @SuppressWarnings("JavadocReference")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.iris = createIRICache();
        this.content.values().forEach(info -> {
            ModelConfig conf = info.getModelConfig();
            InternalModelHolder m = (InternalModelHolder) info.get();
            UnionGraph baseGraph = m.getBase().getGraph();
            Stream<UnionGraph> imports = Graphs.getImports(baseGraph).stream()
                    .map(s -> this.content.values().map(OntInfo::get).map(InternalModelHolder.class::cast)
                            .map(InternalModelHolder::getBase).map(OntGraphModelImpl::getGraph)
                            .filter(g -> Objects.equals(s, Graphs.getURI(g))).findFirst().orElse(null))
                    .filter(Objects::nonNull);
            imports.forEach(baseGraph::addGraph);
            InternalModel baseModel = conf.createInternalModel(baseGraph);
            m.setBase(baseModel);
        });
    }

    /**
     * Creates {@link InternalConfig} with reference to manager inside.
     *
     * @return {@link ModelConfig}
     * @see InternalConfig
     */
    public ModelConfig createModelConfig() {
        return new ModelConfig(this);
    }

    /**
     * Listeners holder.
     * Was added is just for simplification code.
     * Any working with listeners should be placed here.
     */
    @SuppressWarnings("UnusedReturnValue")
    protected static class ListenersHolder implements Serializable {
        private static final String BAD_LISTENER = "BADLY BEHAVING LISTENER: {} has been removed";
        private static final long serialVersionUID = 6728609023804778746L;
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

        public void addOntologyChangeProgressListener(@Nonnull OWLOntologyChangeProgressListener listener) {
            progressListeners.add(listener);
        }

        public void removeOntologyChangeProgressListener(@Nonnull OWLOntologyChangeProgressListener listener) {
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

        public void addOntologyChangeListener(@Nonnull OWLOntologyChangeListener listener,
                                              @Nonnull OWLOntologyChangeBroadcastStrategy strategy) {
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
         */
        protected void fireMissingImportEvent(MissingImportEvent evt) {
            missingImportsListeners.forEach(l -> l.importMissing(evt));
        }

        /**
         * @param id       {@link OWLOntologyID}
         * @param doc      {@link IRI}
         * @param imported boolean
         */
        protected void fireStartedLoadingEvent(OWLOntologyID id, IRI doc, boolean imported) {
            for (OWLOntologyLoaderListener listener : loaderListeners) {
                listener.startedLoadingOntology(new OWLOntologyLoaderListener.LoadingStartedEvent(id, doc, imported));
            }
        }

        protected void fireStartedLoadingEvent(OWLOntologyID id, IRI doc) {
            if (loadCount.get() != importsLoadCount.get()) {
                // Contrary to the following message,
                // in ONT-API the standard (OWL-API) loading mechanisms
                // are used in case of the ontology has imports in a format syntax which is not supported by Jena.
                // This may considered as a bug or inappropriate usage - right now not sure.
                // But in these circumstances, the message does not seem informative but rather annoying.
                // That's why the logger level has been decreased from warn to debug.
                LOGGER.debug("[{} => {}] Runtime Warning: Parsers should load imported ontologies using the " +
                        "makeImportLoadRequest method.", doc, id);
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
         */
        protected void fireBeginChanges(int size) {
            if (!broadcastChanges.get()) {
                return;
            }
            for (OWLOntologyChangeProgressListener listener : progressListeners) {
                try {
                    listener.begin(size);
                } catch (Exception e) {
                    LOGGER.warn(BAD_LISTENER, e.getMessage(), e);
                    progressListeners.remove(listener);
                }
            }
        }

        /**
         *
         */
        protected void fireEndChanges() {
            if (!broadcastChanges.get()) {
                return;
            }
            for (OWLOntologyChangeProgressListener listener : progressListeners) {
                try {
                    listener.end();
                } catch (Exception e) {
                    LOGGER.warn(BAD_LISTENER, e.getMessage(), e);
                    progressListeners.remove(listener);
                }
            }
        }

        /**
         * @param change {@link OWLOntologyChange}
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
                    LOGGER.warn(BAD_LISTENER, e.getMessage(), e);
                    progressListeners.remove(listener);
                }
            }
        }

        /**
         * @param changes List of {@link OWLOntologyChange}
         * @param veto    {@link OWLOntologyChangeVetoException}
         */
        protected void broadcastOntologyChangesVetoed(List<? extends OWLOntologyChange> changes, OWLOntologyChangeVetoException veto) {
            vetoListeners.forEach(l -> l.ontologyChangesVetoed(changes, veto));
        }

        /**
         * @param changes List of {@link OWLOntologyChange}
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
                    LOGGER.warn(BAD_LISTENER, e.getMessage(), e);
                    listenerMap.remove(listener);
                }
            }
        }

        /**
         * @param changes List of {@link OWLOntologyChange}
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
     * An internal container-wrapper for {@link OntologyModel}.
     * This class is designed to provide better synchronization of various parts of
     * the {@link OntologyModel Ontology Model}s that belong to the manager.
     *
     * @see ModelConfig
     */
    @SuppressWarnings("UnusedReturnValue")
    public class OntInfo implements HasOntologyID, Serializable {
        private static final long serialVersionUID = 5894845199098931128L;
        protected final OntologyModel ont;
        protected final ModelConfig conf;
        protected IRI documentIRI;
        protected IRI declarationIRI;
        protected OWLDocumentFormat format;

        public OntInfo(@Nonnull OntologyModel ont) throws ClassCastException {
            this.ont = ont;
            this.conf = (ModelConfig) ((InternalModelHolder) ont).getBase().getConfig();
        }

        @Override
        public OntologyID getOntologyID() {
            return OntologyID.asONT(ont.getOntologyID());
        }

        public OntologyModel get() {
            return ont;
        }

        public OntInfo addFormat(OWLDocumentFormat format) {
            this.format = Objects.requireNonNull(format);
            return this;
        }

        public OntInfo addDocumentIRI(IRI iri) {
            this.documentIRI = Objects.requireNonNull(iri);
            return this;
        }

        public OntInfo addImportDeclaration(IRI declaration) {
            this.declarationIRI = Objects.requireNonNull(declaration);
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
        public ModelConfig getModelConfig() {
            return conf;
        }

        public boolean hasImportDeclaration(IRI declaration) {
            if (Objects.equals(declaration, this.declarationIRI)) return true;
            OntologyID id = getOntologyID();
            if (Objects.equals(declaration, id.getVersionIRI().orElse(null))) return true;
            IRI iri = id.getOntologyIRI().orElse(null);
            if (Objects.equals(declaration, iri)) {
                // from specification:
                // furthermore, if O is the current version of the ontology series with the IRI OI,
                // then the ontology document of O should also be accessible via the IRI OI.
                return OntologyManagerImpl.this.content.keys().filter(i -> i.matchOntology(iri)).count() == 1;
            }
            return Objects.equals(declaration, this.documentIRI);
        }
    }

}
