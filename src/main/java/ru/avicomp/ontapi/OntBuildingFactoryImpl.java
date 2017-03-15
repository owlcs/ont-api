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
                        OntConfig.DefaultScheme.all().anyMatch(s -> s.same(source.getDocumentIRI()));
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
        OntLoader alt = new OWLLoaderImpl((OntologyManager) manager, configuration, ontologyBuilder);
        OntLoader main = new OntModelLoaderImpl((OntologyManagerImpl) manager, configuration, alt);
        return main.load(source);
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
         * base method to load model ({@link OntologyModel}) to manager ({@link OntologyManager}).
         * if the result model contains imports they should come as models also.
         *
         * @param source {@link OWLOntologyDocumentSource} the source (iri, file iri, stream or who knows what)
         * @return {@link OntologyModel} the result model in the manager.
         * @throws OWLOntologyCreationException if something wrong.
         */
        public abstract OntologyModel load(OWLOntologyDocumentSource source) throws OWLOntologyCreationException;

    }

    /**
     * To load {@link OntologyModel} through pure OWL-API mechanisms (using {@link OWLOntologyFactoryImpl}).
     * Some formats (such as {@link org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat} or {@link org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat}),
     * are not supported by jena, so it is the only way.
     */
    public static class OWLLoaderImpl extends OntLoader {
        private static final Logger LOGGER = LoggerFactory.getLogger(OWLLoaderImpl.class);

        private final OWLOntologyFactoryImpl factory;

        public OWLLoaderImpl(OntologyManager manager, OWLOntologyLoaderConfiguration conf, OntBuilderImpl builder) {
            super(manager, conf);
            factory = new OWLOntologyFactoryImpl(OntApiException.notNull(builder, "Null builder"));
        }

        @Override
        public OntologyModel load(OWLOntologyDocumentSource source) throws OWLOntologyCreationException {
            OntologyModel res = (OntologyModel) factory.loadOWLOntology(manager, source, (OWLOntologyCreationHandler) manager, configuration);
            if (LOGGER.isDebugEnabled()) {
                OntFormat format = OntFormat.get(manager.getOntologyFormat(res));
                LOGGER.debug("The format (" + source.getClass().getSimpleName() + "): " + format);
            }
            // clear cache to be sure that list of axioms is always the same and corresponds to the graph.
            res.clearCache();
            return res;
        }
    }

    /**
     * The main {@link OntLoader}.
     * This is an auxiliary class to provide loading of {@link OntologyModel} using jena.
     * Should resolves problems such as cycle imports or throws informative exceptions.
     * In case of some problems while loading there is no need to clear manager to keep it synchronized
     * since models are assembled after obtaining the graphs collection.
     */
    public static class OntModelLoaderImpl extends OntLoader {
        private static final Logger LOGGER = LoggerFactory.getLogger(OntModelLoaderImpl.class);

        private Map<String, GraphInfo> graphs = new LinkedHashMap<>();
        private OntLoader alternative;

        public OntModelLoaderImpl(OntologyManagerImpl manager, OWLOntologyLoaderConfiguration configuration, OntLoader alternative) {
            super(manager, configuration);
            this.alternative = alternative;
        }

        @Override
        public OntologyModel load(OWLOntologyDocumentSource source) throws OWLOntologyCreationException {
            try {
                GraphInfo primary;
                try {
                    primary = loadGraph(source);
                } catch (UnsupportedFormatException e) {
                    if (alternative == null) {
                        throw new OWLOntologyCreationException("Unable to load graph from " + source, e);
                    }
                    if (LOGGER.isDebugEnabled()) {
                        String cause = e.getCause() != null ? e.getCause().getMessage() : null;
                        LOGGER.debug(String.format("Can't load using jena (%s|%s), use original method.", e.getMessage(), cause));
                    }
                    // if we are not success with primary graph there is no reason to continue loading through this class.
                    return alternative.load(source);
                }
                // null key in case of anonymous ontology. But: only one anonymous is allowed (as root of imports tree).
                graphs.put(primary.getURI(), primary);
                // first expand graphs map by creating primary model:
                OntologyModel res = OntApiException.notNull(createModel(primary), "Should never happen");
                // then process all the rest dependent models (we have already all graphs compiled, now need populate them as models):
                graphs.keySet().stream().filter(u -> !Objects.equals(u, primary.getURI())).map(k -> graphs.get(k))
                        .forEach(this::createModel);
                return res;
            } finally { // the possibility to reuse.
                graphs.clear();
            }
        }

        /**
         * Populates {@link Graph} as {@link OntologyModel} inside manager
         *
         * @param info {@link GraphInfo} container with info about graph.
         * @return {@link OntologyModel}, it is ready to use.
         */
        protected OntologyModel createModel(GraphInfo info) {
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
         * Assembles the {@link UnionGraph} from the inner collection ({@link #graphs}).
         * Takes into account config settings ({@link #configuration}).
         *
         * @param node {@link GraphInfo}
         * @param seen Collection of URIs to avoid cycles in imports.
         * @return {@link Graph}
         * @throws OntApiException if something wrong.
         */
        protected Graph makeUnionGraph(GraphInfo node, Collection<String> seen) {
            Set<GraphInfo> children = new HashSet<>();
            Graph pure = node.getGraph();
            seen.add(node.getURI());
            for (String uri : node.getImports()) {
                if (seen.contains(uri)) continue;
                OWLImportsDeclaration declaration = manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create(uri));
                if (configuration.isIgnoredImport(declaration.getIRI())) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(declaration + " is ignored.");
                    }
                    continue;
                }
                // graphs#computeIfAbsent:
                GraphInfo info = graphs.get(uri);
                if (info == null) {
                    try {
                        info = fetchGraph(uri);
                        graphs.put(uri, info);
                    } catch (OWLOntologyCreationException e) {
                        if (MissingImportHandlingStrategy.THROW_EXCEPTION.equals(configuration.getMissingImportHandlingStrategy())) {
                            throw new UnloadableImportException(e, declaration);
                        }
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Can't process sub graph with " + declaration + ". Exception: " + e);
                        }
                        continue;
                    }
                }
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

        /**
         * Makes graph transformation if it is allowed in settings.
         * All sub-graphs should be already transformed.
         *
         * @param graph {@link Graph}
         * @return {@link Graph}
         * @see TransformAction
         */
        protected Graph transform(Graph graph) {
            if (configuration.isPerformTransformation()) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Perform graph transformations.");
                configuration.getGraphTransformers().actions(graph).forEach(TransformAction::process);
            }
            return graph;
        }

        /**
         * Returns the {@link Graph} wrapped by {@link GraphInfo} which corresponds the specified ontology uri.
         * If there the model ({@link OntologyModel}) with the specified uri already exists inside manager then
         * the method returns the base graph from it.
         * Otherwise it tries to load graph directly by uri or using predefined document iri from
         * some manager's iri mapper (see {@link OWLOntologyIRIMapper}).
         *
         * @param uri String the ontology uri.
         * @return {@link GraphInfo} container with {@link Graph} encapsulated.
         * @throws ConfigMismatchException      a conflict with some settings from ({@link #configuration})
         * @throws OWLOntologyCreationException some serious I/O problem while loading
         * @throws OntApiException              some other unexpected problem occurred.
         */
        protected GraphInfo fetchGraph(String uri) throws OWLOntologyCreationException {
            IRI ontologyIRI = IRI.create(uri);
            IRI documentIRI = Iter.asStream(manager.getIRIMappers().iterator())
                    .map(m -> m.getDocumentIRI(ontologyIRI)).filter(Objects::nonNull)
                    .findFirst().orElse(ontologyIRI);
            // handle also the strange situation when there is no resource-mapping but a mapping on some existing ontology
            OWLOntologyID id = new OWLOntologyID(Optional.of(documentIRI), Optional.empty());
            if (manager.contains(id)) {
                OntologyModel model = OntApiException.notNull(manager.getOntology(id), "Can't find ontology " + id);
                return toGraphInfo(model);
            }
            IRIDocumentSource source = new IRIDocumentSource(documentIRI);
            try {
                return loadGraph(source);
            } catch (UnsupportedFormatException e) {
                if (alternative == null) {
                    throw e;
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Can't load graph using jena (" + e.getMessage() + "), try alternative method.");
                }
                return toGraphInfo(alternative.load(source));
            }
        }

        /**
         * Loads graph from the source.
         *
         * @param source {@link OWLOntologyDocumentSource} with instructions how to reach the graph.
         * @return {@link GraphInfo} wrapper around the {@link Graph}.
         * @throws UnsupportedFormatException   if source can't be read into graph using jena way.
         * @throws ConfigMismatchException      if conflict with some config settings.
         * @throws OWLOntologyCreationException if there is some serious I/O problem.
         * @throws OntApiException              if some other problem occurred.
         */
        protected GraphInfo loadGraph(OWLOntologyDocumentSource source) throws OWLOntologyCreationException {
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
                LOGGER.debug(String.format("Graph loaded. Source: %s. Format: %s", source.getClass().getSimpleName(), format));
            }
            return new GraphInfo(graph, format, true);
        }

        protected GraphInfo toGraphInfo(OntologyModel model) { // npe in case no format:
            OntFormat format = OntFormat.get(manager.getOntologyFormat(model));
            Graph graph = model.asGraphModel().getBaseGraph();
            return new GraphInfo(graph, format, false);
        }

        /**
         * Just container for {@link Graph} and {@link OntFormat}.
         * Used for simplification as temporary storage.
         */
        protected class GraphInfo {
            private final OntFormat format;
            private final Graph graph;
            private boolean fresh;
            private String uri;
            private Set<String> imports;

            protected GraphInfo(Graph graph, OntFormat format, boolean fresh) {
                this.graph = graph;
                this.format = format;
                this.fresh = fresh;
            }

            protected String getURI() {
                return uri == null ? uri = Graphs.getURI(graph) : uri;
            }

            protected Set<String> getImports() {
                return imports == null ? imports = Graphs.getImports(graph) : imports;
            }

            protected boolean isFresh() {
                return fresh;
            }

            protected void setProcessed() {
                this.fresh = true;
            }

            protected OntFormat getFormat() {
                return format;
            }

            protected Graph getGraph() {
                return graph;
            }
        }

        /**
         * The main method to read the source to the graph.
         * For generality it is public.
         *
         * @param g      {@link Graph} the graph(empty) to put in.
         * @param source {@link OWLOntologyDocumentSource} the source (encapsulates IO-stream, IO-Reader or IRI of document)
         * @param conf   {@link ru.avicomp.ontapi.OntConfig.LoaderConfiguration} config
         * @return {@link OntFormat} corresponding to the specified source.
         * @throws UnsupportedFormatException   if source can't be read into graph using jena.
         * @throws ConfigMismatchException      if there is some conflict with config settings, anyway we can't continue.
         * @throws OWLOntologyCreationException if there is some serious IO problem
         * @throws OntApiException              if some other problem.
         */
        public static OntFormat readGraph(Graph g, OWLOntologyDocumentSource source, OntConfig.LoaderConfiguration conf) throws OWLOntologyCreationException {
            IRI iri = OntApiException.notNull(source, "Null document source.").getDocumentIRI();
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Read graph from <" + iri + ">.");
            if (source.getInputStream().isPresent()) {
                return readFromInputStream(g, source);
            }
            if (source.getReader().isPresent()) {
                return readFromReader(g, source);
            }
            return readFromDocumentIRI(g, source, conf);
        }

        /**
         * Tries to compute the {@link OntFormat} from {@link OWLOntologyDocumentSource} by using content type or uri or whatever else.
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

        /**
         * Reads the graph from source using only document iri (see {@link OWLOntologyDocumentSource#getDocumentIRI()})
         *
         * @param g      {@link Graph}
         * @param source {@link OWLOntologyDocumentSource}
         * @param conf   {@link ru.avicomp.ontapi.OntConfig.LoaderConfiguration}
         * @return {@link OntFormat} in case of success (otherwise throws an exception)
         * @throws UnsupportedFormatException   if format is definitely not supported by jena.
         * @throws ConfigMismatchException      if document IRI is not allowed in config.
         * @throws OWLOntologyCreationException if there are some critical problems while preparing stream.
         */
        protected static OntFormat readFromDocumentIRI(Graph g, OWLOntologyDocumentSource source, OntConfig.LoaderConfiguration conf) throws OWLOntologyCreationException {
            OntFormat format = source.getFormat().map(OntFormat::get).orElse(guessFormat(source));
            if (format != null && format.isOWLOnly()) // not even try
                throw new UnsupportedFormatException("Format " + format + " is not supported by jena.");
            IRI iri = source.getDocumentIRI();
            if (conf.getSupportedSchemes().stream().noneMatch(s -> s.same(iri))) {
                throw new ConfigMismatchException("Not allowed scheme: " + iri);
            }
            try {
                InputStream is = DocumentSources.getInputStream(iri, conf).orElseThrow(OntApiException.supplier("Can't get input stream from " + iri));
                RDFDataMgr.read(g, is, format == null ? null : format.getLang());
                return format;
            } catch (RiotException e) {
                throw new UnsupportedFormatException("Can't read " + format + " from iri <" + iri + ">", e);
            } catch (OWLOntologyInputSourceException e) {
                throw new OWLOntologyCreationException("Can't get input stream for " + iri, e);
            }
        }

        /**
         * Reads the graph from source using only encapsulated access to the input stream (see {@link OWLOntologyDocumentSource#getInputStream()})
         * Since we don't know the data format we iterate through all available.
         *
         * @param g      {@link Graph}
         * @param source {@link OWLOntologyDocumentSource}
         * @return {@link OntFormat} if success, otherwise throws an exception
         * @throws UnsupportedFormatException in case there are no jena formats which suite data inside io-stream.
         */
        protected static OntFormat readFromInputStream(Graph g, OWLOntologyDocumentSource source) throws UnsupportedFormatException {
            if (!source.getInputStream().isPresent()) {
                throw new OntApiException("No input stream inside " + source);
            }
            return formats(source)
                    .filter(OntFormat::isJena)
                    .filter(format -> read(buffer(source.getInputStream().get()), g, format.getLang()))
                    .findFirst().orElseThrow(() -> new UnsupportedFormatException("Can't read from stream " + source));
        }

        /**
         * Reads the graph from source using only encapsulated access to the reader (see {@link OWLOntologyDocumentSource#getReader()})
         * Since we don't know the data-format we iterate through all available.
         *
         * @param g      {@link Graph}
         * @param source {@link OWLOntologyDocumentSource}
         * @return {@link OntFormat} if success, otherwise throws an exception
         * @throws UnsupportedFormatException in case there are no jena formats which suite data inside reader.
         */
        protected static OntFormat readFromReader(Graph g, OWLOntologyDocumentSource source) throws UnsupportedFormatException {
            if (!source.getReader().isPresent()) {
                throw new OntApiException("No reader inside " + source);
            }
            return formats(source)
                    .filter(OntFormat::isJena)
                    .filter(format -> read(buffer(asInputStream(source.getReader().get())), g, format.getLang()))
                    .findFirst().orElseThrow(() -> new UnsupportedFormatException("Can't read from reader " + source));
        }

        protected static InputStream asInputStream(Reader reader) {
            return new ReaderInputStream(reader, StandardCharsets.UTF_8);
        }

        protected static InputStream buffer(InputStream is) {
            return new BufferedInputStream(is);
        }

        /**
         * reads graph from i/o stream without throwing any exception.
         *
         * @param is    {@link InputStream}
         * @param graph {@link Graph}
         * @param lang  {@link Lang}
         * @return true if graph has been read successfully, false if there was an exception.
         */
        protected static boolean read(InputStream is, Graph graph, Lang lang) {
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

        protected static Stream<OntFormat> formats(OWLOntologyDocumentSource source) {
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

    public static class ConfigMismatchException extends OWLOntologyCreationException {
        public ConfigMismatchException(String s) {
            super(s);
        }
    }

    public static class UnsupportedFormatException extends OWLOntologyCreationException {

        public UnsupportedFormatException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnsupportedFormatException(String message) {
            super(message);
        }
    }


}
