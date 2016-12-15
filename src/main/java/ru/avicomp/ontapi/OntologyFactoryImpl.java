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
import org.apache.jena.vocabulary.OWL2;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;

import com.google.inject.Inject;
import ru.avicomp.ontapi.jena.GraphConverter;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl;

/**
 * Ontology loader.
 * TODO:
 * see {@link OWLOntologyFactory}
 * <p>
 * Created by szuev on 24.10.2016.
 */
public class OntologyFactoryImpl extends OWLOntologyFactoryImpl implements OWLOntologyFactory {
    private static final Logger LOGGER = Logger.getLogger(OntologyFactoryImpl.class);

    /**
     * @param ontologyBuilder ontology builder
     */
    @Inject
    public OntologyFactoryImpl(OWLOntologyBuilder ontologyBuilder) {
        super(ontologyBuilder);
    }

    @Override
    public OntologyModel createOWLOntology(@Nonnull OWLOntologyManager manager,
                                           @Nonnull OWLOntologyID ontologyID,
                                           @Nonnull IRI documentIRI,
                                           @Nonnull OWLOntologyCreationHandler handler) {
        return (OntologyModel) super.createOWLOntology(manager, ontologyID, documentIRI, handler);
    }

    @Override
    public OntologyModel loadOWLOntology(@Nonnull OWLOntologyManager manager,
                                         @Nonnull OWLOntologyDocumentSource source,
                                         @Nonnull OWLOntologyCreationHandler handler,
                                         @Nonnull OWLOntologyLoaderConfiguration configuration) throws OWLOntologyCreationException {
        OntologyManagerImpl _manager = (OntologyManagerImpl) manager;
        Graph graph = GraphConverter.convert(loadGraph(_manager, source));
        UnionGraph union = new UnionGraph(graph);
        graph.find(Node.ANY, OWL2.imports.asNode(), Node.ANY)
                .mapWith(Triple::getObject)
                .filterKeep(Node::isURI)
                .mapWith(Node::getURI)
                .mapWith(IRI::create)
                .mapWith(manager::getOntology)
                .filterKeep(Objects::nonNull)
                .mapWith(OntologyModel.class::cast)
                .mapWith(OntologyModel::asGraphModel)
                .mapWith(OntGraphModel::getGraph)
                .forEachRemaining(union::addGraph);
        OntInternalModel base = new OntInternalModel(union);
        OntologyModel res = new OntologyModelImpl(_manager, base);
        _manager.ontologyCreated(res);
        return res;
    }

    public static Graph loadGraph(OntologyManager manager, OWLOntologyDocumentSource source) throws OWLOntologyCreationException {
        IRI iri = OntApiException.notNull(source, "Null OWLOntologyDocumentSource.").getDocumentIRI();
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Load ONT Model from " + iri);
        Graph g = manager.getGraphFactory().create();
        try {
            if (source.getInputStream().isPresent()) {
                read(g, source);
            } else {
                RDFDataMgr.read(g, iri.getIRIString(), guessLang(source));
            }
        } catch (RiotException | OntApiException e) {
            throw new OWLOntologyCreationException("Can't parse " + source, e);
        }
        return g;
    }

    private static void read(Graph graph, OWLOntologyDocumentSource source) {
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
        throw new OntApiException("Can't read " + source);
    }

    public static Lang guessLang(OWLOntologyDocumentSource source) {
        if (OntApiException.notNull(source, "Null OWLOntologyDocumentSource.").getMIMEType().isPresent()) {
            return RDFLanguages.contentTypeToLang(source.getMIMEType().get());
        }
        return RDFLanguages.filenameToLang(source.getDocumentIRI().getIRIString());
    }

}
