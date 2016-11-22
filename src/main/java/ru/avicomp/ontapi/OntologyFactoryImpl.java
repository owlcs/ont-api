package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RiotException;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;

import com.google.inject.Inject;
import ru.avicomp.ontapi.translators.rdf2axiom.GraphParseHelper;
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
    private static boolean disableGraphLoading = true;

    /**
     * @param ontologyBuilder ontology builder
     */
    @Inject
    public OntologyFactoryImpl(OWLOntologyBuilder ontologyBuilder) {
        super(ontologyBuilder);
    }

    @Override
    public OntologyModelImpl createOWLOntology(@Nonnull OWLOntologyManager manager,
                                               @Nonnull OWLOntologyID ontologyID,
                                               @Nonnull IRI documentIRI,
                                               @Nonnull OWLOntologyCreationHandler handler) {
        return (OntologyModelImpl) super.createOWLOntology(manager, ontologyID, documentIRI, handler);
        /*OntologyManager _manager = (OntologyManager) manager;
        OntologyModelImpl ont = new OntologyModelImpl(_manager, ontologyID);
        handler.ontologyCreated(ont);
        handler.setOntologyFormat(ont, new RDFXMLDocumentFormat());
        return ont;*/

    }

    @Override
    public OntologyModel loadOWLOntology(@Nonnull OWLOntologyManager manager,
                                         @Nonnull OWLOntologyDocumentSource documentSource,
                                         @Nonnull OWLOntologyCreationHandler handler,
                                         @Nonnull OWLOntologyLoaderConfiguration configuration) throws OWLOntologyCreationException {
        if (disableGraphLoading) {
            return (OntologyModel) super.loadOWLOntology(manager, documentSource, handler, configuration);
        }
        //TODO:
        OntologyManager _manager = (OntologyManager) manager;
        Graph graph = loadGraph(_manager, documentSource);
        return graph == null ?
                (OntologyModel) super.loadOWLOntology(_manager, documentSource, handler, configuration) :
                parse(_manager, graph, documentSource.getDocumentIRI(), handler);
    }

    private Graph loadGraph(OntologyManager manager, OWLOntologyDocumentSource documentSource) throws OWLOntologyCreationException {
        Optional<InputStream> opt = documentSource.getInputStream();
        if (!opt.isPresent()) return null;
        Model m = ModelFactory.createModelForGraph(manager.getGraphFactory().create());
        try (InputStream is = opt.get()) {
            for (JenaFormats format : JenaFormats.values()) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("try:::" + format);
                try {
                    return m.read(is, "http://dummy", format.getId()).getGraph();
                } catch (RiotException re) {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug(format + ":::" + re);
                }
            }
        } catch (IOException e) {
            throw new OWLOntologyCreationException(e);
        }
        throw new OWLOntologyCreationException("Can't parse " + documentSource);
    }


    private enum JenaFormats {
        TTL_RDF("ttl"),
        XML_RDF("rdf"),
        JSON_LD_RDF("json"),
        JSON_RDF("json"),
        NTRIPLES("nt"),
        TRIG("trig"),;

        public String getId() {
            return id;
        }

        String id;

        JenaFormats(String id) {
            this.id = id;
        }
    }

    private OntologyModel parse(OntologyManager manager, Graph graph, IRI docIRI, OWLOntologyCreationHandler handler) {
        OWLOntologyID id = GraphParseHelper.getOWLOntologyID(graph);
        OntologyModelImpl res = createOWLOntology(manager, id, docIRI, handler);
        res.getRDFChangeProcessor().load(graph);
        return res;
    }

}
