package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.semanticweb.owlapi.io.DocumentSources;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyInputSourceException;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.avicomp.ontapi.internal.InternalModel;
import ru.avicomp.ontapi.jena.OntFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.converters.TransformAction;
import ru.avicomp.ontapi.jena.utils.Graphs;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl;

/**
 * Ontology building factory.
 * See base class {@link OWLOntologyFactory}.
 * <p>
 * Created by szuev on 24.10.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntBuildingFactoryImpl implements OntologyManager.Factory {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntBuildingFactoryImpl.class);

    static {
        ErrorHandlerFactory.setDefaultErrorHandler(ErrorHandlerFactory.errorHandlerNoLogging);
    }

    protected final OntBuilderImpl ontologyBuilder;

    public OntBuildingFactoryImpl() {
        ontologyBuilder = new OntBuilderImpl();
    }

    @Override
    public boolean canCreateFromDocumentIRI(@Nonnull IRI documentIRI) {
        return true;
    }

    @Override
    public boolean canAttemptLoading(@Nonnull OWLOntologyDocumentSource source) {
        return !source.hasAlredyFailedOnStreams() ||
                !source.hasAlredyFailedOnIRIResolution() &&
                        OntConfig.Scheme.all().anyMatch(s -> s.same(source.getDocumentIRI()));
    }

    @Override
    public OntologyModel createOWLOntology(@Nonnull OWLOntologyManager manager,
                                           @Nonnull OWLOntologyID ontologyID,
                                           @Nonnull IRI documentIRI,
                                           @Nonnull OWLOntologyCreationHandler handler) {
        OntologyModel res = ontologyBuilder.createOWLOntology(manager, ontologyID);
        handler.ontologyCreated(res);
        handler.setOntologyFormat(res, OntFormat.TURTLE.createOwlFormat());
        return res;
    }

    @Override
    public OntologyModel loadOWLOntology(@Nonnull OWLOntologyManager manager,
                                         @Nonnull OWLOntologyDocumentSource source,
                                         @Nonnull OWLOntologyCreationHandler handler,
                                         @Nonnull OWLOntologyLoaderConfiguration configuration) throws OWLOntologyCreationException {
        OntLoader pureOWLLoader = new OntLoader((OntologyManager) manager, configuration) {
            @Override
            public OntologyModel load(OWLOntologyDocumentSource source) throws OWLOntologyCreationException {
                return loadThroughOWLApi(manager, source, configuration);
            }
        };
        try {
            return new OntModelLoaderImpl((OntologyManagerImpl) manager, configuration, pureOWLLoader).load(source);
        } catch (OntApiException e) { // maybe it is not supported by jena. try origin OWL-API method:
            if (LOGGER.isDebugEnabled()) {
                String cause = e.getCause() != null ? e.getCause().getMessage() : null;
                // todo: critical exceptions should be thrown up.
                LOGGER.debug(String.format("Can't load using jena (%s|%s), try original method.", e.getMessage(), cause));
            }
            return pureOWLLoader.load(source);
        }
    }

    private OntologyModel loadThroughOWLApi(OWLOntologyManager manager,
                                            OWLOntologyDocumentSource source,
                                            OWLOntologyLoaderConfiguration configuration) throws OWLOntologyCreationException {
        OntologyModel res = (OntologyModel) new OWLOntologyFactoryImpl(ontologyBuilder).loadOWLOntology(manager, source, (OWLOntologyCreationHandler) manager, configuration);
        if (LOGGER.isDebugEnabled()) {
            OntFormat format = OntFormat.get(manager.getOntologyFormat(res));
            LOGGER.debug("The format (" + source.getClass().getSimpleName() + "): " + format);
        }
        // clear cache to be sure that list of axioms is always the same and corresponds to the graph.
        res.clearCache();
        return res;
    }

    /**
     * Base class for any model loader.
     * Currently there are two implementations:
     * - pure OWL loader which calls super method of {@link OWLOntologyFactoryImpl}
     * - the {@link OntModelLoaderImpl}.
     * <p>
     * Note: only two input parameters in the constructor: {@link OntologyManager} and {@link OWLOntologyLoaderConfiguration}.
     * The single instance of {@link OntologyManager} is an {@link OWLOntologyManager} as well as {@link OWLOntologyCreationHandler}.
     * And this is also true for {@link uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl).
     * The {@link OWLOntologyCreationHandler} could be considered as part of inner (OWL-API) implementation,
     * so there is no need in this parameter in our case.
     */
    @SuppressWarnings("WeakerAccess")
    public static abstract class OntLoader implements Serializable {
        protected final OntologyManager manager;
        protected final OntConfig.LoaderConfiguration configuration;

        public OntLoader(OntologyManager manager, OWLOntologyLoaderConfiguration conf) {
            this.manager = OntApiException.notNull(manager, "Null manager.");
            this.configuration = conf instanceof OntConfig.LoaderConfiguration ? (OntConfig.LoaderConfiguration) conf : new OntConfig.LoaderConfiguration(conf);
        }

        /**
         * base method to load {@link OntologyModel}
         * will load graph to manager.
         * if the result model contains imports they should come as models also.
         *
         * @param source {@link OWLOntologyDocumentSource} the source (iri, file iri, stream or who knows what)
         * @return {@link OntologyModel} the result model in manager.
         * @throws OWLOntologyCreationException if something wrong.
         */
        public abstract OntologyModel load(OWLOntologyDocumentSource source) throws OWLOntologyCreationException;

    }

    /**
     * Extended {@link OntLoader}.
     * This is an auxiliary class to provide loading of {@link OntologyModelImpl} using jena.
     * Should resolves problems such as cycle imports or throws informative exception.
     * TODO: use configuration parameters.
     */
    public static class OntModelLoaderImpl extends OntLoader {
        private Map<String, GraphInfo> graphs = new LinkedHashMap<>();
        private OntLoader alternative;

        public OntModelLoaderImpl(OntologyManagerImpl manager, OWLOntologyLoaderConfiguration configuration, OntLoader alternative) {
            super(manager, configuration);
            this.alternative = alternative;
        }


        @Override
        public OntologyModel load(OWLOntologyDocumentSource source) {
            try {
                GraphInfo primary = loadGraph(source);
                // null key in case anonymous. But: only one anonymous ontology is allowed (as root of imports tree)
                graphs.put(primary.getURI(), primary);
                // first expand graphs map by creating primary model:
                OntologyModel res = OntApiException.notNull(createModel(primary), "Should never happen");
                // process all other models if they present:
                graphs.keySet().stream().filter(u -> !Objects.equals(u, primary.getURI())).map(k -> graphs.get(k))
                        .forEach(this::createModel);
                return res;
            } finally { // possibility to reuse
                graphs.clear();
            }
        }

        /**
         * gets graph wrapped by {@link GraphInfo}.
         * <p>
         * Returns existing (base)graph if there is a model inside manager with the specified ontology uri,
         * otherwise tries to load graph directly by uri or using document iri if it is specified by manager.
         *
         * @param uri String ontology uri
         * @return {@link GraphInfo} container with {@link Graph} encapsulated
         * @throws OntApiException if something wrong.
         */
        private GraphInfo fetchGraph(String uri) {
            IRI ontologyIRI = IRI.create(uri);
            OWLOntologyID id = new OWLOntologyID(Optional.of(ontologyIRI), Optional.empty());
            if (manager.contains(id)) {
                OntologyModel model = OntApiException.notNull(manager.getOntology(id), "Can't find ontology " + id);
                return toGraphInfo(model);
            }
            IRI documentIRI = Iter.asStream(manager.getIRIMappers().iterator())
                    .map(m -> m.getDocumentIRI(ontologyIRI)).filter(Objects::nonNull)
                    .findFirst().orElse(ontologyIRI);
            IRIDocumentSource source = new IRIDocumentSource(documentIRI);
            try {
                return loadGraph(source);
            } catch (OntApiException e) {
                if (alternative == null) throw e;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Can't load graph using jena (" + e.getMessage() + "), try original method.");
                }
                try {
                    // alternative:
                    OntologyModel model = alternative.load(source);
                    return toGraphInfo(model);
                } catch (OWLOntologyCreationException critical) {
                    throw new OntApiException("Can't load graph from " + source, critical);
                }
            }
        }

        private GraphInfo loadGraph(OWLOntologyDocumentSource source) {
            Graph graph;
            OntFormat format;
            if (OntGraphDocumentSource.class.isInstance(source)) {
                OntGraphDocumentSource _source = (OntGraphDocumentSource) source;
                graph = _source.getGraph();
                format = _source.getOntFormat();
            } else {
                graph = OntFactory.createDefaultGraph();
                format = readGraph(graph, source, configuration);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("The format (" + source.getClass().getSimpleName() + "): " + format);
            }
            return new GraphInfo(graph, format, true);
        }

        private GraphInfo toGraphInfo(OntologyModel model) {
            OntFormat format = OntFormat.get(manager.getOntologyFormat(model));
            Graph graph = model.asGraphModel().getBaseGraph();
            return new GraphInfo(graph, format, false);
        }

        private OntologyModel createModel(GraphInfo info) {
            if (!info.isFresh()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("The ontology <" + info.getURI() + "> is already configured.");
                }
                return null;
            }
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Set up ontology <" + info.getURI() + ">.");
                }
                Graph graph = makeUnionGraph(info, new HashSet<>());
                OntFormat format = info.getFormat();
                InternalModel base = new InternalModel(graph, configuration.getPersonality());
                base.setLoaderConfig(configuration);
                base.setWriterConfig(manager.getOntologyWriterConfiguration());
                base.setDataFactory(manager.getOWLDataFactory());
                OntologyModelImpl ont = new OntologyModelImpl(manager, base);
                OntologyModel res = ((OntologyManagerImpl) manager).isConcurrent() ? ont.toConcurrentModel() : ont;
                ((OntologyManagerImpl) manager).ontologyCreated(res);
                OWLDocumentFormat owlFormat = format.createOwlFormat();
                if (PrefixManager.class.isInstance(owlFormat)) {
                    PrefixManager pm = (PrefixManager) owlFormat;
                    graph.getPrefixMapping().getNsPrefixMap().entrySet().forEach(e -> pm.setPrefix(e.getKey(), e.getValue()));
                    OntologyManagerImpl.setDefaultPrefix(pm, ont);
                }
                manager.setOntologyFormat(res, owlFormat);
                return res;
            } finally { // just in case.
                info.setProcessed();
            }
        }

        /**
         * assembles the {@link UnionGraph} from the collection of graphs
         *
         * @param node {@link GraphInfo}
         * @param seen Collection to avoid cycles in imports.
         * @return {@link Graph}
         */
        private Graph makeUnionGraph(GraphInfo node, Collection<String> seen) {
            Set<GraphInfo> children = new HashSet<>();
            Graph pure = node.getGraph();
            seen.add(node.getURI());
            for (String uri : node.getImports()) {
                if (seen.contains(uri)) continue;
                GraphInfo info = graphs.computeIfAbsent(uri, g -> fetchGraph(uri));
                children.add(info);
            }
            if (children.isEmpty()) {
                if (node.isFresh() || !node.getImports().isEmpty()) {
                    pure = transform(pure);
                }
                return pure;
            }
            UnionGraph res = new UnionGraph(pure);
            children.forEach(ch -> res.addGraph(makeUnionGraph(ch, seen)));
            return transform(res);
        }

        protected Graph transform(Graph graph) {
            if (configuration.isPerformTransformation()) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Perform graph transformations");
                configuration.getGraphTransformers().actions(graph).forEach(TransformAction::process);
            }
            return graph;
        }

        /**
         * just container for {@link Graph} and {@link OntFormat}.
         * for simplification.
         */
        private class GraphInfo {
            private final OntFormat format;
            private final Graph graph;
            private boolean fresh;
            private String uri;
            private Set<String> imports;

            GraphInfo(Graph graph, OntFormat format, boolean fresh) {
                this.graph = graph;
                this.format = format;
                this.fresh = fresh;
            }

            String getURI() {
                return uri == null ? uri = Graphs.getURI(graph) : uri;
            }

            Set<String> getImports() {
                return imports == null ? imports = Graphs.getImports(graph) : imports;
            }

            boolean isFresh() {
                return fresh;
            }

            void setProcessed() {
                this.fresh = true;
            }

            OntFormat getFormat() {
                return format;
            }

            Graph getGraph() {
                return graph;
            }
        }

        /**
         * main method to read the source to the graph.
         * for generality it is public
         *
         * @param g      {@link Graph} the graph to put in. Could be empty.
         * @param source {@link OWLOntologyDocumentSource} the source (encapsulates IO-stream, IO-Reader or IRI of document)
         * @param conf {@link ru.avicomp.ontapi.OntConfig.LoaderConfiguration} config
         * @return {@link OntFormat} corresponding to the specified source.
         * @throws OntApiException if source can't be read into graph.
         */
        public static OntFormat readGraph(Graph g, OWLOntologyDocumentSource source, OntConfig.LoaderConfiguration conf) throws OntApiException {
            IRI iri = OntApiException.notNull(source, "Null document source.").getDocumentIRI();
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Read graph from <" + iri + ">.");
            if (source.getInputStream().isPresent()) {
                return readFromInputStream(g, source);
            }
            if (source.getReader().isPresent()) {
                return readFromReader(g, source);
            }
            return readFromDocument(g, source, conf);
        }

        /**
         * tries to compute the {@link OntFormat} by {@link OWLOntologyDocumentSource} by using content type or uri or something else.
         * public, for more generality.
         *
         * @param source {@link OWLOntologyDocumentSource}
         * @return {@link OntFormat} or null if it could not guess format from source.
         */
        public static OntFormat guessFormat(OWLOntologyDocumentSource source) {
            Lang lang;
            if (OntApiException.notNull(source, "Null document source.").getMIMEType().isPresent()) {
                lang = RDFLanguages.contentTypeToLang(source.getMIMEType().get());
            } else {
                lang = RDFLanguages.filenameToLang(source.getDocumentIRI().getIRIString());
            }
            return lang == null ? null : OntFormat.get(lang);
        }

        private static OntFormat readFromDocument(Graph graph, OWLOntologyDocumentSource source, OntConfig.LoaderConfiguration conf) {
            OntFormat format = source.getFormat().map(OntFormat::get).orElse(guessFormat(source));
            if (format != null && format.isOWLOnly()) // not even try
                throw new OntApiException("Format " + format + " is not supported by jena.");
            IRI iri = source.getDocumentIRI();
            if (conf.getSupportedSchemes().stream().noneMatch(s -> s.same(iri))) {
                throw new OntApiException("Not allowed scheme: " + iri);
            }
            try {
                InputStream is = DocumentSources.getInputStream(iri, conf).orElseThrow(OntApiException.supplier("Can't get input stream from " + iri));
                RDFDataMgr.read(graph, is, format == null ? null : format.getLang());
                return format;
            } catch (RiotException | OWLOntologyInputSourceException e) {
                throw new OntApiException("Can't read " + format + " from iri <" + iri + ">", e);
            }
        }

        private static OntFormat readFromInputStream(Graph graph, OWLOntologyDocumentSource source) {
            if (!source.getInputStream().isPresent()) {
                throw new OntApiException("No input stream inside " + source);
            }
            return formats(source)
                    .filter(OntFormat::isJena)
                    .filter(format -> read(buffer(source.getInputStream().get()), graph, format.getLang()))
                    .findFirst().orElseThrow(() -> new OntApiException("Can't read from stream " + source));
        }

        private static OntFormat readFromReader(Graph graph, OWLOntologyDocumentSource source) {
            if (!source.getReader().isPresent()) {
                throw new OntApiException("No reader inside " + source);
            }
            return formats(source)
                    .filter(OntFormat::isJena)
                    .filter(format -> read(buffer(asInputStream(source.getReader().get())), graph, format.getLang()))
                    .findFirst().orElseThrow(() -> new OntApiException("Can't read from reader " + source));
        }

        protected static InputStream asInputStream(Reader reader) {
            return new ReaderInputStream(reader, StandardCharsets.UTF_8);
        }

        protected static InputStream buffer(InputStream is) {
            return new BufferedInputStream(is);
        }

        private static boolean read(InputStream is, Graph graph, Lang lang) {
            try {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("try <<" + lang + ">>");
                RDFDataMgr.read(graph, is, lang);
                return true;
            } catch (Exception e) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("(" + lang + ") failed: " + e.getMessage());
                return false;
            }
        }

        private static Stream<OntFormat> formats(OWLOntologyDocumentSource source) {
            return source.getFormat().map(OntFormat::get).map(Stream::of).orElse(OntFormat.supported());
        }


    }

    public static class OntBuilderImpl implements OWLOntologyBuilder {

        public static OntologyModel createOntology(OntologyManager manager, OWLOntologyID id) {
            OntologyManagerImpl m = (OntologyManagerImpl) manager;
            OntologyModelImpl ont = new OntologyModelImpl(m, id);
            return m.isConcurrent() ? ont.toConcurrentModel() : ont;
        }

        @Override
        public OntologyModel createOWLOntology(@Nonnull OWLOntologyManager manager, @Nonnull OWLOntologyID ontologyID) {
            return createOntology((OntologyManager) manager, ontologyID);
        }
    }

}
