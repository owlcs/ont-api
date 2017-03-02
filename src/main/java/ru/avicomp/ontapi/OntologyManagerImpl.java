package ru.avicomp.ontapi;

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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.model.parameters.OntologyCopy;
import org.semanticweb.owlapi.util.CollectionFactory;
import org.semanticweb.owlapi.util.PriorityCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multimap;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.impl.configuration.OntPersonality;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Models;
import uk.ac.manchester.cs.owl.owlapi.HasTrimToSize;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentPriorityCollection;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock;


/**
 * ONT-API Ontology Manager implementation ({@link OntologyManager}).
 * Currently it is mostly copy-paste of {@link OWLOntologyManagerImpl}.
 * <p>
 * Created by @szuev on 03.10.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntologyManagerImpl implements OntologyManager, OWLOntologyFactory.OWLOntologyCreationHandler, Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(OWLOntologyManagerImpl.class);

    private static final String BADLISTENER = "BADLY BEHAVING LISTENER: {} has been removed";

    protected final OWLDataFactory dataFactory;

    protected final Map<OWLOntologyID, OntologyModel> ontologiesByID = CollectionFactory.createSyncMap();
    protected final Map<OWLOntologyID, IRI> documentIRIsByID = CollectionFactory.createSyncMap();
    protected final Map<OWLOntologyID, OWLOntologyLoaderConfiguration> ontologyConfigurationsByOntologyID = CollectionFactory.createSyncMap();
    protected final Map<OWLOntologyID, OWLDocumentFormat> ontologyFormatsByOntology = CollectionFactory.createSyncMap();
    protected final Map<OWLImportsDeclaration, OWLOntologyID> ontologyIDsByImportsDeclaration = CollectionFactory.createSyncMap();
    protected final AtomicInteger loadCount = new AtomicInteger();
    protected final AtomicInteger importsLoadCount = new AtomicInteger();
    protected final Map<IRI, Object> importedIRIs = CollectionFactory.createSyncMap();
    protected final Map<OWLOntologyID, Set<OWLOntology>> importsClosureCache = CollectionFactory.createSyncMap();
    // listeners:
    protected final List<MissingImportListener> missingImportsListeners = CollectionFactory.createSyncList();
    protected final List<OWLOntologyLoaderListener> loaderListeners = CollectionFactory.createSyncList();
    protected final List<OWLOntologyChangeProgressListener> progressListeners = CollectionFactory.createSyncList();
    protected transient List<OWLOntologyChangesVetoedListener> vetoListeners = CollectionFactory.createList();
    protected transient Map<OWLOntologyChangeListener, OWLOntologyChangeBroadcastStrategy> listenerMap = CollectionFactory.createSyncMap();
    protected transient Map<ImpendingOWLOntologyChangeListener, ImpendingOWLOntologyChangeBroadcastStrategy> impendingChangeListenerMap = CollectionFactory.createSyncMap();

    protected final AtomicBoolean broadcastChanges = new AtomicBoolean(true);
    protected OWLOntologyChangeBroadcastStrategy defaultChangeBroadcastStrategy = new DefaultChangeBroadcastStrategy();
    protected ImpendingOWLOntologyChangeBroadcastStrategy defaultImpendingChangeBroadcastStrategy = new DefaultImpendingChangeBroadcastStrategy();

    // configs:
    protected OntConfig configProvider;
    protected transient OntConfig.LoaderConfiguration loaderConfig;
    protected transient OWLOntologyWriterConfiguration writerConfig;

    protected final PriorityCollection<OWLOntologyFactory> ontologyFactories;
    protected final PriorityCollection<OWLOntologyIRIMapper> documentMappers;
    protected final PriorityCollection<OWLParserFactory> parserFactories;
    protected final PriorityCollection<OWLStorerFactory> ontologyStorers;
    protected final ReadWriteLock lock;

    public OntologyManagerImpl(OWLDataFactory dataFactory, ReadWriteLock readWriteLock) {
        this(dataFactory, readWriteLock, PriorityCollectionSorting.ALWAYS);
    }

    public OntologyManagerImpl(OWLDataFactory dataFactory, ReadWriteLock readWriteLock, PriorityCollectionSorting sorting) {
        this.dataFactory = OntApiException.notNull(dataFactory, "Null OWLDataFactory specified.");
        this.lock = readWriteLock == null ? new NoOpReadWriteLock() : readWriteLock;
        documentMappers = new ConcurrentPriorityCollection<>(lock, sorting);
        ontologyFactories = new ConcurrentPriorityCollection<>(lock, sorting);
        parserFactories = new ConcurrentPriorityCollection<>(lock, sorting);
        ontologyStorers = new ConcurrentPriorityCollection<>(lock, sorting);
        configProvider = new OntConfig();
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
        lock.readLock().lock();
        try {
            return configProvider;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param conf {@link OntologyConfigurator}
     * @see OWLOntologyManagerImpl#setOntologyConfigurator(OntologyConfigurator)
     */
    @Override
    public void setOntologyConfigurator(@Nonnull OntologyConfigurator conf) {
        lock.writeLock().lock();
        try {
            configProvider = conf instanceof OntConfig ? (OntConfig) conf : OntConfig.copy(conf);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param conf {@link OWLOntologyLoaderConfiguration}
     * @see OWLOntologyManagerImpl#setOntologyLoaderConfiguration(OWLOntologyLoaderConfiguration)
     */
    @Override
    public void setOntologyLoaderConfiguration(@Nullable OWLOntologyLoaderConfiguration conf) {
        lock.writeLock().lock();
        try {
            loaderConfig = conf instanceof OntConfig.LoaderConfiguration ? (OntConfig.LoaderConfiguration) conf : new OntConfig.LoaderConfiguration(conf);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @return {@link OntConfig.LoaderConfiguration}
     * @see OWLOntologyManagerImpl#getOntologyLoaderConfiguration()
     */
    @Override
    @Nonnull
    public OntConfig.LoaderConfiguration getOntologyLoaderConfiguration() {
        lock.readLock().lock();
        try {
            return loaderConfig == null ? loaderConfig = configProvider.buildLoaderConfiguration() : loaderConfig;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param conf {@link OWLOntologyWriterConfiguration}
     * @see OWLOntologyManagerImpl#setOntologyWriterConfiguration(OWLOntologyWriterConfiguration)
     */
    @Override
    public void setOntologyWriterConfiguration(@Nullable OWLOntologyWriterConfiguration conf) {
        lock.writeLock().lock();
        try {
            writerConfig = conf;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @return {@link OWLOntologyWriterConfiguration}
     * @see OWLOntologyManagerImpl#getOntologyWriterConfiguration()
     */
    @Override
    @Nonnull
    public OWLOntologyWriterConfiguration getOntologyWriterConfiguration() {
        lock.readLock().lock();
        try {
            return writerConfig == null ? writerConfig = configProvider.buildWriterConfiguration() : writerConfig;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
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
        lock.writeLock().lock();
        try {
            defaultChangeBroadcastStrategy = strategy;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangeListener}
     * @see OWLOntologyManagerImpl#addOntologyChangeListener(OWLOntologyChangeListener)
     */
    @Override
    public void addOntologyChangeListener(@Nonnull OWLOntologyChangeListener listener) {
        addOntologyChangeListener(listener, defaultChangeBroadcastStrategy);
    }

    /**
     * @param listener {@link OWLOntologyChangeListener}
     * @param strategy {@link OWLOntologyChangeBroadcastStrategy}
     * @see OWLOntologyManagerImpl#addOntologyChangeListener(OWLOntologyChangeListener, OWLOntologyChangeBroadcastStrategy)
     */
    @Override
    public void addOntologyChangeListener(@Nonnull OWLOntologyChangeListener listener, @Nonnull OWLOntologyChangeBroadcastStrategy strategy) {
        lock.writeLock().lock();
        try {
            listenerMap.put(listener, strategy);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangeListener}
     * @see OWLOntologyManagerImpl#removeOntologyChangeListener(OWLOntologyChangeListener)
     */
    @Override
    public void removeOntologyChangeListener(@Nonnull OWLOntologyChangeListener listener) {
        lock.writeLock().lock();
        try {
            listenerMap.remove(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param listener {@link ImpendingOWLOntologyChangeListener}
     * @see OWLOntologyManagerImpl#addImpendingOntologyChangeListener(ImpendingOWLOntologyChangeListener)
     */
    @Override
    public void addImpendingOntologyChangeListener(@Nonnull ImpendingOWLOntologyChangeListener listener) {
        lock.writeLock().lock();
        try {
            impendingChangeListenerMap.put(listener, defaultImpendingChangeBroadcastStrategy);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param listener {@link ImpendingOWLOntologyChangeListener}
     * @see OWLOntologyManagerImpl#removeImpendingOntologyChangeListener(ImpendingOWLOntologyChangeListener)
     */
    @Override
    public void removeImpendingOntologyChangeListener(@Nonnull ImpendingOWLOntologyChangeListener listener) {
        lock.writeLock().lock();
        try {
            impendingChangeListenerMap.remove(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangesVetoedListener}
     * @see OWLOntologyManagerImpl#addOntologyChangesVetoedListener(OWLOntologyChangesVetoedListener)
     */
    @Override
    public void addOntologyChangesVetoedListener(@Nonnull OWLOntologyChangesVetoedListener listener) {
        lock.writeLock().lock();
        try {
            vetoListeners.add(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangesVetoedListener}
     * @see OWLOntologyManagerImpl#removeOntologyChangesVetoedListener(OWLOntologyChangesVetoedListener)
     */
    @Override
    public void removeOntologyChangesVetoedListener(@Nonnull OWLOntologyChangesVetoedListener listener) {
        lock.writeLock().lock();
        try {
            vetoListeners.remove(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param listener {@link MissingImportListener}
     * @see OWLOntologyManagerImpl#addMissingImportListener(MissingImportListener)
     */
    @Override
    public void addMissingImportListener(@Nonnull MissingImportListener listener) {
        lock.writeLock().lock();
        try {
            missingImportsListeners.add(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param listener {@link MissingImportListener}
     * @see OWLOntologyManagerImpl#removeMissingImportListener(MissingImportListener)
     */
    @Override
    public void removeMissingImportListener(@Nonnull MissingImportListener listener) {
        lock.writeLock().lock();
        try {
            missingImportsListeners.remove(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyLoaderListener}
     * @see OWLOntologyManagerImpl#addOntologyLoaderListener(OWLOntologyLoaderListener)
     */
    @Override
    public void addOntologyLoaderListener(@Nonnull OWLOntologyLoaderListener listener) {
        lock.writeLock().lock();
        try {
            loaderListeners.add(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyLoaderListener}
     * @see OWLOntologyManagerImpl#removeOntologyLoaderListener(OWLOntologyLoaderListener)
     */
    @Override
    public void removeOntologyLoaderListener(@Nonnull OWLOntologyLoaderListener listener) {
        lock.writeLock().lock();
        try {
            loaderListeners.remove(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangeProgressListener}
     * @see OWLOntologyManagerImpl#addOntologyChangeProgessListener(OWLOntologyChangeProgressListener)
     */
    @Override
    public void addOntologyChangeProgessListener(@Nonnull OWLOntologyChangeProgressListener listener) {
        lock.writeLock().lock();
        try {
            progressListeners.add(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param listener {@link OWLOntologyChangeProgressListener}
     * @see OWLOntologyManagerImpl#removeOntologyChangeProgessListener(OWLOntologyChangeProgressListener)
     */
    @Override
    public void removeOntologyChangeProgessListener(@Nonnull OWLOntologyChangeProgressListener listener) {
        lock.writeLock().lock();
        try {
            progressListeners.remove(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param id {@link OWLOntologyID}
     * @return {@link OntologyModel}
     * @see OWLOntologyManagerImpl#createOntology(OWLOntologyID)
     */
    @Override
    public OntologyModel createOntology(@Nonnull OWLOntologyID id) {
        lock.writeLock().lock();
        try {
            OWLOntology ontology = ontologiesByID.get(id);
            if (ontology != null) {
                throw new OWLOntologyAlreadyExistsException(id);
            }
            IRI doc = computeDocumentIRI(id);
            if (doc == null) throw new OWLOntologyCreationException("Can't compute document iri from id " + id);
            if (documentIRIsByID.values().contains(doc)) {
                throw new OWLOntologyDocumentAlreadyExistsException(doc);
            }
            for (OWLOntologyFactory factory : ontologyFactories) {
                // todo (reminder): we are working only with the one factory which returns OntologyModel, not OWLOntology
                if (factory.canCreateFromDocumentIRI(doc)) {
                    documentIRIsByID.put(id, doc);
                    return (OntologyModel) factory.createOWLOntology(this, id, doc, this);
                }
            }
            throw new OWLOntologyFactoryNotFoundException(doc);
        } catch (OWLOntologyCreationException e) {
            throw new OntApiException("Unable to create ontology " + id, e);
        } finally {
            lock.writeLock().unlock();
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
        lock.writeLock().lock();
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
            lock.writeLock().unlock();
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
        lock.writeLock().lock();
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
            lock.writeLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            OWLOntologyID id = new OWLOntologyID(Optional.of(iri), Optional.empty());
            OntologyModel res = ontologiesByID.get(id);
            if (res != null) {
                return res;
            }
            // todo: id could be changed through jena. need handle this situation also.
            return ontologiesByID.entrySet().stream().filter(e -> e.getKey().match(iri)).findFirst().map(Map.Entry::getValue).orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * the difference: in case there are many ontologies with the same ID it chooses first on the iri ignoring version,
     * while original method chooses any.
     *
     * @param id {@link OWLOntologyID}
     * @return {@link OntologyModel}
     * @see OWLOntologyManagerImpl#getOntology(OWLOntologyID)
     * @see OWLOntologyManagerImpl#checkDocumentIRI(OWLOntologyID)
     */
    @Override
    public OntologyModel getOntology(@Nonnull OWLOntologyID id) {
        lock.readLock().lock();
        try {
            OntologyModel res = ontologiesByID.get(id);
            if (res == null && !id.isAnonymous()) {
                Optional<OWLOntologyID> findFirst = ontologiesByID.keySet().stream()
                        .filter(o -> o.matchOntology(id.getOntologyIRI().get())).findFirst();
                if (findFirst.isPresent()) {
                    res = ontologiesByID.get(findFirst.get());
                }
            }
            // original comment:
            // HACK: This extra clause is necessary to make getOntology match
            // the behaviour of createOntology in cases where a documentIRI has
            // been recorded, based on the mappers, but an ontology has not
            // been stored in ontologiesByID
            if (res == null) {
                IRI doc = getDocumentIRIFromMappers(id);
                if (doc != null && documentIRIsByID.values().contains(doc)) {
                    // not a OntApiException
                    throw new OWLRuntimeException(new OWLOntologyDocumentAlreadyExistsException(doc));
                }
            }
            // todo: id could be changed through jena. need handle this situation also.
            return res;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return boolean
     * @see OWLOntologyManagerImpl#contains(OWLOntologyID)
     */
    @Override
    public boolean contains(@Nonnull OWLOntology ontology) {
        lock.readLock().lock();
        try {
            return ontology instanceof OntologyModel && ontologiesByID.containsValue(ontology);
        } finally {
            lock.readLock().unlock();
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
        lock.readLock().lock();
        try {
            return ids().anyMatch(o -> o.match(iri));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param id {@link OWLOntologyID}
     * @return boolean
     * @see OWLOntologyManagerImpl#contains(OWLOntologyID)
     */
    @Override
    public boolean contains(@Nonnull OWLOntologyID id) {
        if (id.isAnonymous()) { // WTF?
            return false;
        }
        lock.readLock().lock();
        try {
            return ontologiesByID.containsKey(id) || ids().anyMatch(id::match);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param iri {@link IRI}
     * @return boolean
     * @see OWLOntologyManagerImpl#containsVersion(IRI)
     */
    @Override
    public boolean containsVersion(@Nonnull IRI iri) {
        lock.readLock().lock();
        try {
            return ids().anyMatch(o -> o.matchVersion(iri));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @see OWLOntologyManagerImpl#removeOntology(OWLOntology)
     */
    @Override
    public void removeOntology(OWLOntology ontology) {
        lock.writeLock().lock();
        try {
            removeOntology(ontology.getOntologyID());
            ontology.setOWLOntologyManager(null);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param id {@link OWLOntologyID}
     * @see OWLOntologyManagerImpl#removeOntology(OWLOntologyID)
     */
    @Override
    public void removeOntology(@Nonnull OWLOntologyID id) {
        lock.writeLock().lock();
        try {
            OWLOntology o = ontologiesByID.remove(id);
            ontologyFormatsByOntology.remove(id);
            documentIRIsByID.remove(id);
            removeValueFromMap(ontologyIDsByImportsDeclaration, id);
            removeValueFromMap(importedIRIs, id);
            if (o != null) {
                o.setOWLOntologyManager(null);
                resetImportsClosureCache();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param map   Map
     * @param value the value to look for
     * @see OWLOntologyManagerImpl#removeValue(Map, Object)
     */
    protected <K, V> void removeValueFromMap(Map<K, V> map, V value) {
        Set<K> keys = map.entrySet().stream().filter(e -> value.equals(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toSet());
        keys.forEach(map::remove);
    }

    /**
     * @param oldID {@link OWLOntologyID}
     * @param newID {@link OWLOntologyID}
     * @see OWLOntologyManagerImpl#renameOntology(OWLOntologyID, OWLOntologyID)
     */
    protected void renameOntology(OWLOntologyID oldID, OWLOntologyID newID) {
        OntologyModel ont = ontologiesByID.get(oldID);
        if (ont == null) {
            // Nothing to rename!
            return;
        }
        ontologiesByID.remove(oldID);
        ontologiesByID.put(newID, ont);
        if (ontologyFormatsByOntology.containsKey(oldID)) {
            ontologyFormatsByOntology.put(newID, ontologyFormatsByOntology.remove(oldID));
        }
        IRI documentIRI = documentIRIsByID.remove(oldID);
        if (documentIRI != null) {
            documentIRIsByID.put(newID, documentIRI);
        }
        resetImportsClosureCache();
    }

    /**
     * @see OWLOntologyManagerImpl#clearOntologies()
     */
    @Override
    public void clearOntologies() {
        lock.writeLock().lock();
        try {
            documentIRIsByID.clear();
            impendingChangeListenerMap.clear();
            importedIRIs.clear();
            importsClosureCache.clear();
            listenerMap.clear();
            loaderListeners.clear();
            missingImportsListeners.clear();
            ontologiesByID.values().forEach(o -> o.setOWLOntologyManager(null));
            ontologiesByID.clear();
            ontologyConfigurationsByOntologyID.clear();
            ontologyFormatsByOntology.clear();
            ontologyIDsByImportsDeclaration.clear();
            progressListeners.clear();
            vetoListeners.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param declaration {@link OWLImportsDeclaration}
     * @return {@link OntologyModel}
     * @see OWLOntologyManagerImpl#getImportedOntology(OWLImportsDeclaration)
     */
    @Override
    public OntologyModel getImportedOntology(@Nonnull OWLImportsDeclaration declaration) {
        lock.readLock().lock();
        try {
            OWLOntologyID id = ontologyIDsByImportsDeclaration.get(declaration);
            if (id == null) {
                // original comment:
                // No such ontology has been loaded through an import
                // declaration, but it might have been loaded manually.
                // Using the IRI to retrieve it will either find the ontology or
                // return null.
                // Last possibility is an import by document IRI; if the
                // ontology is not found by IRI, check by document IRI.
                OntologyModel res = getOntology(declaration.getIRI());
                if (res == null) {
                    res = getOntologyByDocumentIRI(declaration.getIRI());
                }
                return res;
            } else {
                return getOntology(id);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * not a copy-past. hotfix.
     * <p>
     * we have changes in behaviour in comparison with OWL-API while add/remove imports.
     * see {@link OntologyModelImpl.RDFChangeProcessor#addImport(OWLImportsDeclaration)} and
     * {@link OntologyModelImpl.RDFChangeProcessor#removeImport(OWLImportsDeclaration)}.
     * <p>
     * While renaming some ontology OWL-API performs adding/removing imports for ontologies where that one is in use.
     * This method is to avoid exception caused by {@link OWLOntologyManagerImpl#checkDocumentIRI} during {@link #getImportedOntology}.
     *
     * @param declaration {@link OWLImportsDeclaration}
     * @return {@link OntologyModelImpl} or null.
     * @see OntologyManagerImpl#getOntology(OWLOntologyID) for exception.
     */
    public OntologyModelImpl getOntologyByImportDeclaration(@Nonnull OWLImportsDeclaration declaration) {
        try {
            // lock inside:
            // todo: return not impl
            return (OntologyModelImpl) getImportedOntology(declaration);
        } catch (Exception re) {
            if (re.getCause() instanceof OWLOntologyDocumentAlreadyExistsException) {
                OntologyModelImpl res = getOntologyByDocumentIRI(declaration.getIRI());
                if (res != null) return res;
            }
            throw re;
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
    public OntologyModelImpl getOntologyByDocumentIRI(IRI iri) {
        try {
            // todo: return not impl
            lock.readLock().lock();
            return findOntologyIDByDocumentIRI(iri)
                    .map(ontologiesByID::get)
                    .map(OntologyModelImpl.class::cast).orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    protected Optional<OWLOntologyID> findOntologyIDByDocumentIRI(IRI iri) {
        return documentIRIsByID.entrySet().stream().filter(o -> iri.equals(o.getValue()))
                .map(Map.Entry::getKey).findFirst();
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return {@link IRI}
     * @see OWLOntologyManagerImpl#getOntologyDocumentIRI(OWLOntology)
     */
    @Nonnull
    @Override
    public IRI getOntologyDocumentIRI(@Nonnull OWLOntology ontology) {
        lock.readLock().lock();
        try {
            if (!contains(ontology)) {
                throw new UnknownOWLOntologyException(ontology.getOntologyID());
            }
            IRI res = documentIRIsByID.get(ontology.getOntologyID());
            if (res == null) throw new OntApiException("Null document iri");
            return res;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param ontology    {@link OWLOntology}
     * @param documentIRI {@link IRI}
     * @see OWLOntologyManagerImpl#setOntologyDocumentIRI(OWLOntology, IRI)
     */
    @Override
    public void setOntologyDocumentIRI(@Nonnull OWLOntology ontology, @Nonnull IRI documentIRI) {
        lock.writeLock().lock();
        try {
            if (!ontologiesByID.containsKey(ontology.getOntologyID())) {
                throw new UnknownOWLOntologyException(ontology.getOntologyID());
            }
            documentIRIsByID.put(ontology.getOntologyID(), documentIRI);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param ontology       {@link OWLOntology}
     * @param ontologyFormat {@link OWLDocumentFormat}
     * @see OWLOntologyManagerImpl#setOntologyFormat(OWLOntology, OWLDocumentFormat)
     */
    @Override
    public void setOntologyFormat(@Nonnull OWLOntology ontology, @Nonnull OWLDocumentFormat ontologyFormat) {
        lock.writeLock().lock();
        try {
            OWLOntologyID ontologyID = ontology.getOntologyID();
            ontologyFormatsByOntology.put(ontologyID, ontologyFormat);
        } finally {
            lock.writeLock().unlock();
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
        lock.readLock().lock();
        try {
            OWLOntologyID ontologyID = ontology.getOntologyID();
            return ontologyFormatsByOntology.get(ontologyID);
        } finally {
            lock.readLock().unlock();
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
        lock.writeLock().lock();
        try {
            ontologiesByID.put(ont.getOntologyID(), (OntologyModel) ont);
            resetImportsClosureCache();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return Stream of {@link OWLOntology}
     * @see OWLOntologyManagerImpl#directImports(OWLOntology)
     */
    @Override
    public Stream<OWLOntology> directImports(@Nonnull OWLOntology ontology) {
        lock.readLock().lock();
        try {
            if (!contains(ontology)) {
                throw new UnknownOWLOntologyException(ontology.getOntologyID());
            }
            return ontology.importsDeclarations().map(this::getImportedOntology).map(OWLOntology.class::cast).filter(Objects::nonNull);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return Stream of {@link OWLOntology}
     * @see OWLOntologyManagerImpl#imports(OWLOntology)
     */
    @Override
    public Stream<OWLOntology> imports(@Nonnull OWLOntology ontology) {
        lock.readLock().lock();
        try {
            return getImports(ontology, new LinkedHashSet<>()).stream();
        } finally {
            lock.readLock().unlock();
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
        lock.readLock().lock();
        try {
            OWLOntologyID id = ontology.getOntologyID();
            return importsClosureCache.computeIfAbsent(id, i -> getImportsClosure(ontology, new LinkedHashSet<>())).stream();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param ontology   {@link OWLOntology}
     * @param ontologies Set {@link OWLOntology}
     * @return Set {@link OWLOntology}
     * @see OWLOntologyManagerImpl#getImportsClosure(OWLOntology, Set)
     */
    protected Set<OWLOntology> getImportsClosure(OWLOntology ontology, Set<OWLOntology> ontologies) {
        lock.readLock().lock();
        try {
            ontologies.add(ontology);
            directImports(ontology).filter(o -> !ontologies.contains(o)).forEach(o -> getImportsClosure(o, ontologies));
            return ontologies;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param ontology {@link OWLOntology}
     * @return List of {@link OWLOntology}
     * @see OWLOntologyManagerImpl#getSortedImportsClosure(OWLOntology)
     */
    @Override
    public List<OWLOntology> getSortedImportsClosure(@Nonnull OWLOntology ontology) {
        lock.readLock().lock();
        try {
            return ontology.importsClosure().sorted().collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @return Stream of {@link OWLOntology}
     * @see OWLOntologyManagerImpl#ontologies()
     */
    @Override
    public Stream<OWLOntology> ontologies() {
        lock.readLock().lock();
        try {
            // XXX investigate lockable access to streams
            return ontologiesByID.values().stream().map(Function.identity());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * @param axiom {@link OWLAxiom}
     * @return Stream of {@link OWLOntology}
     * @see OWLOntologyManagerImpl#ontologies(OWLAxiom)
     */
    @Override
    public Stream<OWLOntology> ontologies(OWLAxiom axiom) {
        lock.readLock().lock();
        try {
            // XXX check default
            return ontologies().filter(o -> o.containsAxiom(axiom));
        } finally {
            lock.readLock().lock();
        }
    }

    /**
     * @see OWLOntologyManagerImpl#resetImportsClosureCache()
     */
    protected void resetImportsClosureCache() {
        importsClosureCache.clear();
    }

    /**
     * @return stream of {@link OWLOntologyID}
     * @see OWLOntologyManagerImpl#ids()
     */
    protected Stream<OWLOntologyID> ids() {
        return ontologiesByID.keySet().stream();
    }

    /**
     * @param iri {@link IRI}
     * @return Stream of {@link OWLOntologyID}
     * @see OWLOntologyManagerImpl#ontologyIDsByVersion(IRI)
     */
    @Override
    public Stream<OWLOntologyID> ontologyIDsByVersion(@Nonnull IRI iri) {
        lock.readLock().lock();
        try {
            return ids().filter(o -> o.matchVersion(iri));
        } finally {
            lock.readLock().unlock();
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
     * @param changes List of {@link OWLOntologyChange}
     * @return {@link ChangeApplied}
     * @see OWLOntologyManagerImpl#applyChanges(List)
     */
    @Override
    public ChangeApplied applyChanges(@Nonnull List<? extends OWLOntologyChange> changes) {
        lock.writeLock().lock();
        try {
            broadcastImpendingChanges(changes);
            AtomicBoolean rollbackRequested = new AtomicBoolean(false);
            AtomicBoolean allNoOps = new AtomicBoolean(true);
            // list of changes applied successfully. These are the changes that
            // will be reverted in case of a rollback
            List<OWLOntologyChange> appliedChanges = new ArrayList<>();
            fireBeginChanges(changes.size());
            actuallyApply(changes, rollbackRequested, allNoOps, appliedChanges);
            if (rollbackRequested.get()) {
                rollBack(appliedChanges);
                appliedChanges.clear();
            }
            fireEndChanges();
            broadcastChanges(appliedChanges);
            if (rollbackRequested.get()) {
                return ChangeApplied.UNSUCCESSFULLY;
            }
            if (allNoOps.get()) {
                return ChangeApplied.NO_OPERATION;
            }
            return ChangeApplied.SUCCESSFULLY;
        } catch (OWLOntologyChangeVetoException e) {
            // Some listener blocked the changes.
            broadcastOntologyChangesVetoed(changes, e);
            return ChangeApplied.UNSUCCESSFULLY;
        } finally {
            lock.writeLock().unlock();
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
                fireChangeApplied(change);
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
        OWLOntologyLoaderConfiguration ontologyConfig = ontologyConfigurationsByOntologyID.get(change.getOntology()
                .getOntologyID());
        return !(ontologyConfig != null && !ontologyConfig.isLoadAnnotationAxioms() && change.isAddAxiom() && change
                .getAxiom() instanceof OWLAnnotationAxiom);
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
        OWLOntology existingOntology = ontologiesByID.get(setID.getNewOntologyID());
        OWLOntology o = setID.getOntology();
        if (existingOntology != null && !o.equals(existingOntology) && !o.equalAxioms(existingOntology)) {
            LOGGER.error("OWLOntologyManagerImpl.checkForOntologyIDChange() existing:{}", existingOntology);
            LOGGER.error("OWLOntologyManagerImpl.checkForOntologyIDChange() new:{}", o);
            throw new OWLOntologyRenameException(setID.getChangeData(), setID.getNewOntologyID());
        }
        renameOntology(setID.getOriginalOntologyID(), setID.getNewOntologyID());
        resetImportsClosureCache();
    }

    /**
     * @param change {@link OWLOntologyChange}
     * @see OWLOntologyManagerImpl#checkForImportsChange(OWLOntologyChange)
     */
    protected void checkForImportsChange(OWLOntologyChange change) {
        if (!change.isImportChange()) return;
        resetImportsClosureCache();
        if (change instanceof AddImport) {
            OWLImportsDeclaration addImportDeclaration = ((AddImport) change).getImportDeclaration();
            IRI iri = addImportDeclaration.getIRI();
            Optional<OWLOntologyID> findFirst = ids().filter(o -> o.match(iri) || o.matchDocument(iri))
                    .findFirst();
            findFirst.ifPresent(o -> ontologyIDsByImportsDeclaration.put(addImportDeclaration, o));
            if (!findFirst.isPresent()) {
                // then the import does not refer to a known IRI for
                // ontologies; check for a document IRI to find the ontology
                // id corresponding to the file location
                documentIRIsByID.entrySet().stream().filter(o -> o.getValue().equals(iri)).findAny().ifPresent(
                        o -> ontologyIDsByImportsDeclaration.put(addImportDeclaration, o.getKey()));
            }
        } else {
            // Remove the mapping from declaration to ontology
            OWLImportsDeclaration importDeclaration = ((RemoveImport) change).getImportDeclaration();
            ontologyIDsByImportsDeclaration.remove(importDeclaration);
            importedIRIs.remove(importDeclaration.getIRI());
        }
    }

    /**
     * @param toCopy   {@link OWLOntology}
     * @param settings {@link OntologyCopy}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException ex
     * @throws ClassCastException           in case toCopy is not OntologyModel
     * @see OWLOntologyManagerImpl#copyOntology(OWLOntology, OntologyCopy)
     */
    @Override
    public OntologyModel copyOntology(@Nonnull OWLOntology toCopy, @Nonnull OntologyCopy settings) throws OWLOntologyCreationException {
        lock.writeLock().lock();
        try {
            OntApiException.notNull(toCopy, "Null ontology.");
            OntApiException.notNull(settings, "Null settings.");
            OntologyModel toReturn;
            switch (settings) {
                case MOVE:
                    toReturn = (OntologyModel) toCopy; // class cast ex
                    ontologiesByID.put(toReturn.getOntologyID(), toReturn);
                    break;
                case SHALLOW:
                case DEEP:
                    OntologyModel o = createOntology(toCopy.getOntologyID());
                    AxiomType.AXIOM_TYPES.forEach(t -> addAxioms(o, toCopy.axioms(t)));
                    toCopy.annotations().forEach(a -> applyChange(new AddOntologyAnnotation(o, a)));
                    toCopy.importsDeclarations().forEach(a -> applyChange(new AddImport(o, a)));
                    toReturn = o;
                    break;
                default:
                    throw new OWLRuntimeException("settings value not understood: " + settings);
            }
            // toReturn now initialized
            OWLOntologyManager m = toCopy.getOWLOntologyManager();
            if (settings == OntologyCopy.MOVE || settings == OntologyCopy.DEEP) {
                setOntologyDocumentIRI(toReturn, m.getOntologyDocumentIRI(toCopy));
                OWLDocumentFormat ontologyFormat = m.getOntologyFormat(toCopy);
                if (ontologyFormat != null) {
                    setOntologyFormat(toReturn, ontologyFormat);
                }
            }
            if (settings == OntologyCopy.MOVE) {
                m.removeOntology(toCopy);
                // at this point toReturn and toCopy are the same object
                // change the manager on the ontology
                toReturn.setOWLOntologyManager(this);
            }
            return toReturn;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * @param source {@link IRI}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException ex
     * @see OWLOntologyManagerImpl#loadOntology(IRI)
     */
    @Override
    public OntologyModel loadOntology(@Nonnull IRI source) throws OWLOntologyCreationException {
        lock.writeLock().lock();
        try {
            return load(source, false, getOntologyLoaderConfiguration());
        } finally {
            lock.writeLock().unlock();
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
        lock.writeLock().lock();
        try {
            return load(null, source, conf);
        } finally {
            lock.writeLock().unlock();
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
        OntologyModel ontByID = getOntology(iri);
        if (ontByID != null) {
            return ontByID;
        }
        OWLOntologyID id = new OWLOntologyID(Optional.of(iri), Optional.empty());
        IRI documentIRI = getDocumentIRIFromMappers(id);
        if (documentIRI != null) {
            if (documentIRIsByID.values().contains(documentIRI) && !allowExists) {
                throw new OWLOntologyDocumentAlreadyExistsException(documentIRI);
            }
            // The ontology might be being loaded, but its IRI might
            // not have been set (as is probably the case with RDF/XML!)
            OntologyModel ontByDocumentIRI = findOntologyIDByDocumentIRI(documentIRI).map(this::getOntology).orElse(null);
            if (ontByDocumentIRI != null) {
                return ontByDocumentIRI;
            }
        } else {
            // Nothing we can do here. We can't get a document IRI to load
            // the ontology from.
            throw new OntologyIRIMappingNotFoundException(iri);
        }
        if (documentIRIsByID.values().contains(documentIRI) && !allowExists) {
            throw new OWLOntologyDocumentAlreadyExistsException(documentIRI);
        }
        // The ontology might be being loaded, but its IRI might
        // not have been set (as is probably the case with RDF/XML!)
        OntologyModel ontByDocumentIRI = findOntologyIDByDocumentIRI(documentIRI).map(this::getOntology).orElse(null);
        if (ontByDocumentIRI != null) {
            return ontByDocumentIRI;
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
        if (loadCount.get() != importsLoadCount.get()) {
            LOGGER.warn("Runtime Warning: Parsers should load imported ontologies using the makeImportLoadRequest method.");
        }
        fireStartedLoadingEvent(new OWLOntologyID(Optional.ofNullable(iri), Optional.empty()), source.getDocumentIRI(), loadCount.get() > 0);
        loadCount.incrementAndGet();
        broadcastChanges.set(false);
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
            if (loadCount.decrementAndGet() == 0) {
                broadcastChanges.set(true);
                // Completed loading ontology and imports
            }
            fireFinishedLoadingEvent(id, source.getDocumentIRI(), loadCount.get() > 0, ex);
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
            if (factory.canAttemptLoading(source)) { // todo: only single factory by now
                try {
                    // Note - there is no need to add the ontology here,
                    // because it will be added
                    // when the ontology is created.
                    OntologyModel ontology = (OntologyModel) factory.loadOWLOntology(this, source, this, conf);
                    fixIllegalPunnings(ontology);
                    // Store the ontology to the document IRI mapping
                    documentIRIsByID.put(ontology.getOntologyID(), source.getDocumentIRI());
                    ontologyConfigurationsByOntologyID.put(ontology.getOntologyID(), conf);
                    if (ontology instanceof HasTrimToSize) { // todo: no trimToSize in our ontologies.
                        ((HasTrimToSize) ontology).trimToSize();
                    }
                    return ontology;
                } catch (OWLOntologyRenameException e) {
                    // We loaded an ontology from a document and the
                    // ontology turned out to have an IRI the same
                    // as a previously loaded ontology
                    throw new OWLOntologyAlreadyExistsException(e.getOntologyID(), e);
                }
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
    protected void fixIllegalPunnings(OWLOntology o) {
        // nothing here.
    }

    /**
     * No lock.
     *
     * @param declaration   {@link OWLImportsDeclaration}
     * @param configuration {@link OWLOntologyLoaderConfiguration}
     * @return {@link OntologyModel}
     * @throws OWLOntologyCreationException ex
     * @see OWLOntologyManagerImpl#loadImports(OWLImportsDeclaration, OWLOntologyLoaderConfiguration)
     */
    @Nullable
    protected OntologyModel loadImports(@Nonnull OWLImportsDeclaration declaration, @Nonnull OWLOntologyLoaderConfiguration configuration) throws OWLOntologyCreationException {
        importsLoadCount.incrementAndGet();
        try {
            return load(declaration.getIRI(), true, configuration);
        } catch (OWLOntologyCreationException e) {
            if (MissingImportHandlingStrategy.THROW_EXCEPTION == configuration.getMissingImportHandlingStrategy()) {
                throw e;
            } else {
                // Silent
                MissingImportEvent evt = new MissingImportEvent(declaration.getIRI(), e);
                fireMissingImportEvent(evt);
            }
        } finally {
            importsLoadCount.decrementAndGet();
        }
        return null;
    }

    /**
     * @param evt {@link MissingImportEvent}
     * @see OWLOntologyManagerImpl#fireMissingImportEvent(MissingImportEvent)
     */
    protected void fireMissingImportEvent(MissingImportEvent evt) {
        new ArrayList<>(missingImportsListeners).forEach(l -> l.importMissing(evt));
    }

    /**
     * @param declaration   {@link OWLImportsDeclaration}
     * @param configuration {@link OWLOntologyLoaderConfiguration}
     * @see OWLOntologyManagerImpl#makeLoadImportRequest(OWLImportsDeclaration, OWLOntologyLoaderConfiguration)
     */
    @Override
    public void makeLoadImportRequest(@Nonnull OWLImportsDeclaration declaration, @Nonnull OWLOntologyLoaderConfiguration configuration) {
        lock.writeLock().lock();
        try {
            IRI iri = declaration.getIRI();
            if (!configuration.isIgnoredImport(iri) && !importedIRIs.containsKey(iri)) {
                // insert temporary value - we do not know the actual ID yet
                importedIRIs.put(iri, new Object());
                try {
                    OWLOntology ont = loadImports(declaration, configuration);
                    if (ont != null) {
                        ontologyIDsByImportsDeclaration.put(declaration, ont.getOntologyID());
                        importedIRIs.put(iri, ont.getOntologyID());
                    }
                } catch (OWLOntologyCreationException e) {
                    // Wrap as UnloadableImportException and throw
                    throw new UnloadableImportException(e, declaration);
                }
            }
        } finally {
            lock.writeLock().unlock();
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
        try {
            lock.readLock().lock();
            write(ontology, ontologyFormat, documentTarget);
        } finally {
            lock.readLock().unlock();
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
        if ("file".equals(iri.getScheme())) {
            File file = new File(iri.toURI());
            file.getParentFile().mkdirs();
            return new FileOutputStream(file);
        }
        URL url = iri.toURI().toURL();
        URLConnection conn = url.openConnection();
        return conn.getOutputStream();
    }

    /**
     * no lock
     *
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
     * no lock
     *
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
     * no lock
     *
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
     * No lock
     *
     * @param id       {@link OWLOntologyID}
     * @param doc      {@link IRI}
     * @param imported boolean
     * @see OWLOntologyManagerImpl#fireStartedLoadingEvent(OWLOntologyID, IRI, boolean)
     */
    protected void fireStartedLoadingEvent(OWLOntologyID id, IRI doc, boolean imported) {
        for (OWLOntologyLoaderListener listener : new ArrayList<>(loaderListeners)) {
            listener.startedLoadingOntology(new OWLOntologyLoaderListener.LoadingStartedEvent(id, doc, imported));
        }
    }

    /**
     * no lock.
     *
     * @param id       {@link OWLOntologyID}
     * @param doc      {@link IRI}
     * @param imported boolean
     * @param ex       {@link Exception}
     * @see OWLOntologyManagerImpl#fireFinishedLoadingEvent(OWLOntologyID, IRI, boolean, Exception)
     */
    protected void fireFinishedLoadingEvent(OWLOntologyID id, IRI doc, boolean imported, @Nullable Exception ex) {
        for (OWLOntologyLoaderListener listener : new ArrayList<>(loaderListeners)) {
            listener.finishedLoadingOntology(new OWLOntologyLoaderListener.LoadingFinishedEvent(id,
                    doc, imported, ex));
        }
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
     * no lock
     *
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

    /**
     * @param changes List of {@link OWLOntologyChange}
     * @param veto    {@link OWLOntologyChangeVetoException}
     * @see OWLOntologyManagerImpl#broadcastOntologyChangesVetoed(List, OWLOntologyChangeVetoException)
     */
    protected void broadcastOntologyChangesVetoed(List<? extends OWLOntologyChange> changes, OWLOntologyChangeVetoException veto) {
        new ArrayList<>(vetoListeners).forEach(l -> l.ontologyChangesVetoed(changes, veto));
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
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        loaderConfig = (OntConfig.LoaderConfiguration) in.readObject();
        listenerMap = CollectionFactory.createSyncMap();
        impendingChangeListenerMap = CollectionFactory.createSyncMap();
        vetoListeners = CollectionFactory.createList();
        Set<OntBaseModelImpl> models = ontologies().map(OntBaseModelImpl.class::cast).collect(Collectors.toSet());
        OntPersonality p = getOntologyLoaderConfiguration().getPersonality();
        for (OntBaseModelImpl m : models) {
            UnionGraph base = m.getBase().getGraph();
            Stream<UnionGraph> imports = Graphs.getImports(base).stream()
                    .map(s -> models.stream().map(OntBaseModelImpl::getBase).map(OntGraphModelImpl::getGraph)
                            .filter(g -> Objects.equals(s, Graphs.getURI(g))).findFirst().orElse(null))
                    .filter(Objects::nonNull);
            imports.forEach(base::addGraph);
            m.setBase(new OntInternalModel(base, p));
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeObject(getOntologyLoaderConfiguration());
    }
}
