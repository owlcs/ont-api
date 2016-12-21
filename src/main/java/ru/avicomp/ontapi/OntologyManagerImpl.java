package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.jena.graph.Factory;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.ProfileRegistry;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.io.OWLOntologyStorageIOException;
import org.semanticweb.owlapi.model.*;

import com.google.inject.Inject;
import ru.avicomp.ontapi.jena.utils.Models;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;

/**
 * TODO:
 * Created by @szuev on 03.10.2016.
 */
public class OntologyManagerImpl extends OWLOntologyManagerImpl implements OntologyManager {
    private OntModelSpec spec;
    private GraphFactory graphFactory = DEFAULT_GRAPH_FACTORY;

    private static final GraphFactory DEFAULT_GRAPH_FACTORY = Factory::createGraphMem;

    @Inject
    public OntologyManagerImpl(OWLDataFactory dataFactory, ReadWriteLock readWriteLock) {
        super(dataFactory, readWriteLock);
        OntDocumentManager documentManager = new OntDocumentManager();
        documentManager.setProcessImports(false);
        this.spec = new OntModelSpec(ModelFactory.createMemModelMaker(), documentManager, null, ProfileRegistry.OWL_DL_LANG);
    }

    @Override
    public OntModelSpec getSpec() {
        return spec;
    }

    @Override
    public OntologyModel createOntology(@Nonnull OWLOntologyID ontologyID) {
        try {
            return (OntologyModel) super.createOntology(ontologyID);
        } catch (OWLOntologyCreationException e) {
            throw new OntApiException(e);
        }
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
    public OntologyModel getOntology(@Nullable IRI ontologyIRI) {
        return (OntologyModel) super.getOntology(ontologyIRI);
    }

    @Override
    public OntologyModel getOntology(@Nonnull OWLOntologyID ontologyID) {
        return (OntologyModel) super.getOntology(ontologyID);
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
        OntFormat format = OntFormat.get(ontologyFormat);
        if (format == null || !format.isJena() || !OntologyModel.class.isInstance(ontology)) {
            super.saveOntology(ontology, ontologyFormat, documentTarget);
            return;
        }
        OutputStream os = null;
        if (documentTarget.getDocumentIRI().isPresent()) {
            try {
                os = documentTarget.getDocumentIRI().get().toURI().toURL().openConnection().getOutputStream();
            } catch (IOException e) {
                throw new OWLOntologyStorageIOException(e);
            }
        } else if (documentTarget.getOutputStream().isPresent()) {
            os = documentTarget.getOutputStream().get();
        }
        if (os == null) {
            throw new OWLOntologyStorageException("Null output stream, format = " + ontologyFormat);
        }
        Model model = ((OntologyModel) ontology).asGraphModel().getBaseModel();
        Map<String, String> newPrefixes = new HashMap<>(PrefixManager.class.isInstance(ontologyFormat) ? ((PrefixManager) ontologyFormat).getPrefixName2PrefixMap() : Collections.emptyMap());
        if (ontology.getOntologyID().getOntologyIRI().isPresent())
            newPrefixes.put("", ontology.getOntologyID().getOntologyIRI().get().getIRIString() + "#");
        Map<String, String> initPrefixes = model.getNsPrefixMap();
        try {
            Models.setNsPrefixes(model, newPrefixes);
            RDFDataMgr.write(os, model, format.getLang());
        } finally {
            Models.setNsPrefixes(model, initPrefixes);
        }
    }
}
