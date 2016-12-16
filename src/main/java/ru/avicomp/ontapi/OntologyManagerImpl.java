package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.jena.graph.Factory;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.ProfileRegistry;
import org.apache.jena.rdf.model.ModelFactory;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.PriorityCollection;

import com.google.inject.Inject;
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
        // nothing here. use ru.avicomp.ontapi.jena.GraphConverter to some fixing
    }

    @Override
    public void setOntologyParsers(@Nullable Set<OWLParserFactory> parsers) {
        throw new OntApiException.Unsupported(getClass(), "setOntologyParsers");
    }

    @Override
    public PriorityCollection<OWLParserFactory> getOntologyParsers() {
        throw new OntApiException.Unsupported(getClass(), "getOntologyParsers");
    }
}
