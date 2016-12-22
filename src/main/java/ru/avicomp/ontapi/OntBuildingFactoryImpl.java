package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import java.util.Objects;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RiotException;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;

import com.google.inject.Inject;
import ru.avicomp.ontapi.jena.GraphConverter;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.vocabulary.OWL2;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl;

/**
 * Ontology building factory.
 * See base class  {@link OWLOntologyFactory}.
 * <p>
 * Created by szuev on 24.10.2016.
 */
public class OntBuildingFactoryImpl extends OWLOntologyFactoryImpl implements OWLOntologyFactory {
    private static final Logger LOGGER = Logger.getLogger(OntBuildingFactoryImpl.class);

    /**
     * @param ontologyBuilder ontology builder
     */
    @Inject
    public OntBuildingFactoryImpl(OWLOntologyBuilder ontologyBuilder) {
        super(ontologyBuilder);
    }

    @Override
    public OntologyModel createOWLOntology(@Nonnull OWLOntologyManager manager,
                                           @Nonnull OWLOntologyID ontologyID,
                                           @Nonnull IRI documentIRI,
                                           @Nonnull OWLOntologyCreationHandler handler) {
        OntologyManagerImpl m = (OntologyManagerImpl) manager;
        OntologyModelImpl ont = new OntologyModelImpl(m, ontologyID);
        OntologyModel res = m.isConcurrent() ? ont.toConcurrentModel() : ont;
        handler.ontologyCreated(res);
        handler.setOntologyFormat(res, new RDFXMLDocumentFormat());
        return res;
    }

    @Override
    public OntologyModel loadOWLOntology(@Nonnull OWLOntologyManager manager,
                                         @Nonnull OWLOntologyDocumentSource source,
                                         @Nonnull OWLOntologyCreationHandler handler,
                                         @Nonnull OWLOntologyLoaderConfiguration configuration) throws OWLOntologyCreationException {
        OntologyManagerImpl m = (OntologyManagerImpl) manager;
        Graph graph;
        try {
            graph = GraphConverter.convert(loadGraph(m, source));
        } catch (OntApiException e) { // maybe it is no jena format
            LOGGER.warn("Can't load from " + source + " ::: " + e);
            return (OntologyModel) super.loadOWLOntology(manager, source, handler, configuration);
        }
        UnionGraph union = new UnionGraph(graph);
        graph.find(Node.ANY, OWL2.imports.asNode(), Node.ANY)
                .mapWith(Triple::getObject)
                .filterKeep(Node::isURI)
                .mapWith(Node::getURI)
                .mapWith(IRI::create)
                .mapWith(iri -> fetchOntology(m, iri))
                .filterKeep(Objects::nonNull)
                .mapWith(OntologyModel::asGraphModel)
                .mapWith(OntGraphModel::getGraph)
                .forEachRemaining(union::addGraph);
        OntInternalModel base = new OntInternalModel(union);
        OntologyModelImpl ont = new OntologyModelImpl(m, base);
        OntologyModel res = m.isConcurrent() ? ont.toConcurrentModel() : ont;
        m.ontologyCreated(res);
        return res;
    }

    private OntologyModel fetchOntology(OntologyManagerImpl manager, IRI iri) {
        if (manager.contains(iri)) return manager.getOntology(iri);
        try {
            return manager.loadOntology(iri);
        } catch (OWLOntologyCreationException e) {
            LOGGER.warn(e);
        }
        return null;
    }

    public static Graph loadGraph(OntologyManager manager, OWLOntologyDocumentSource source) throws OntApiException {
        IRI iri = OntApiException.notNull(source, "Null OWLOntologyDocumentSource.").getDocumentIRI();
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Load ONT Model from " + iri);
        Graph g = manager.getGraphFactory().create();
        if (source.getInputStream().isPresent()) {
            readFromStream(g, source);
        } else {
            readFromDocument(g, source);
        }
        return g;
    }

    private static void readFromDocument(Graph graph, OWLOntologyDocumentSource source) {
        Lang lang = guessLang(source);
        String uri = source.getDocumentIRI().getIRIString();
        try {
            RDFDataMgr.read(graph, uri, lang);
        } catch (RiotException e) {
            throw new OntApiException("Can't read lang=" + lang + " from iri <" + uri + ">", e);
        }
    }

    private static void readFromStream(Graph graph, OWLOntologyDocumentSource source) {
        if (!source.getInputStream().isPresent()) {
            throw new OntApiException("No input stream inside " + source);
        }
        for (Lang lang : RDFLanguages.getRegisteredLanguages()) {
            try {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Try <<" + lang + ">> for " + source.getDocumentIRI());
                RDFDataMgr.read(graph, source.getInputStream().get(), lang);
                return;
            } catch (RiotException e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Can't read " + lang + "::" + e.getMessage());
            }
        }
        throw new OntApiException("Can't read from stream.");
    }

    public static Lang guessLang(OWLOntologyDocumentSource source) {
        if (OntApiException.notNull(source, "Null OWLOntologyDocumentSource.").getMIMEType().isPresent()) {
            return RDFLanguages.contentTypeToLang(source.getMIMEType().get());
        }
        return RDFLanguages.filenameToLang(source.getDocumentIRI().getIRIString());
    }

}
