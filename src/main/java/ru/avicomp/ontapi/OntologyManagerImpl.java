package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.jena.graph.Factory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.io.OWLOntologyStorageIOException;
import org.semanticweb.owlapi.model.*;

import com.google.inject.Inject;
import ru.avicomp.ontapi.jena.utils.Models;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock;

/**
 * Ontology Manager implementation.
 * <p>
 * Created by @szuev on 03.10.2016.
 */
public class OntologyManagerImpl extends OWLOntologyManagerImpl implements OntologyManager {
    private static final Logger LOGGER = Logger.getLogger(OntologyManagerImpl.class);
    public static final GraphFactory DEFAULT_GRAPH_FACTORY = Factory::createGraphMem;

    private GraphFactory graphFactory = DEFAULT_GRAPH_FACTORY;
    protected final ReadWriteLock lock;

    @Inject
    public OntologyManagerImpl(OWLDataFactory dataFactory, ReadWriteLock readWriteLock) {
        super(dataFactory, readWriteLock);
        this.lock = readWriteLock;
    }

    public boolean isConcurrent() {
        return !NoOpReadWriteLock.class.isInstance(lock);
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    @Override
    public OntologyModel createOntology(@Nonnull OWLOntologyID id) {
        try {
            return (OntologyModel) super.createOntology(id);
        } catch (OWLOntologyCreationException e) {
            throw new OntApiException(e);
        }
    }

    @Override
    public OntologyModel loadOntology(@Nonnull IRI source) throws OWLOntologyCreationException {
        return (OntologyModel) super.loadOntology(source);
    }

    @Override
    public void setGraphFactory(GraphFactory factory) {
        this.graphFactory = OntApiException.notNull(factory, "Null graph-factory");
    }

    @Override
    public GraphFactory getGraphFactory() {
        return graphFactory;
    }

    @Override
    public OntologyModel getOntology(@Nullable IRI iri) {
        return (OntologyModel) super.getOntology(iri);
    }

    @Override
    public OntologyModel getOntology(@Nonnull OWLOntologyID id) {
        return (OntologyModel) super.getOntology(id);
    }

    @Override
    protected void fixIllegalPunnings(OWLOntology o) {
        // todo:
        // nothing here. use ru.avicomp.ontapi.jena.GraphConverter to some fixing
    }

    @Override
    public void saveOntology(@Nonnull OWLOntology ontology, @Nonnull OWLDocumentFormat ontologyFormat, @Nonnull IRI documentIRI) throws OWLOntologyStorageException {
        saveOntology(ontology, ontologyFormat, new OWLOntologyDocumentTarget() {
            @Override
            public Optional<IRI> getDocumentIRI() {
                return Optional.of(documentIRI);
            }
        });
    }

    @Override
    public void saveOntology(@Nonnull OWLOntology ontology, @Nonnull OWLDocumentFormat ontologyFormat, @Nonnull OWLOntologyDocumentTarget documentTarget)
            throws OWLOntologyStorageException {
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
            super.saveOntology(ontology, documentFormat, target);
            return;
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
        Map<String, String> newPrefixes = new HashMap<>(PrefixManager.class.isInstance(documentFormat) ? ((PrefixManager) documentFormat).getPrefixName2PrefixMap() : Collections.emptyMap());
        String uri = ontology.getOntologyID().getOntologyIRI().isPresent() ? ontology.getOntologyID().getOntologyIRI().get().getIRIString() : null;
        if (uri != null)
            newPrefixes.put("", uri + "#");
        Map<String, String> initPrefixes = model.getNsPrefixMap();
        try {
            Models.setNsPrefixes(model, newPrefixes);
            RDFDataMgr.write(os, model, format.getLang());
        } finally {
            Models.setNsPrefixes(model, initPrefixes);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static OutputStream openStream(IRI iri) throws IOException {
        if ("file".equals(iri.getScheme())) {
            File file = new File(iri.toURI());
            file.getParentFile().mkdirs();
            return new FileOutputStream(file);
        }
        URL url = iri.toURI().toURL();
        URLConnection conn = url.openConnection();
        return conn.getOutputStream();
    }
}
