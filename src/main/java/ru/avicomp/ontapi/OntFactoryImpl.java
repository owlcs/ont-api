/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.system.ErrorHandlerFactory;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.PriorityCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.config.OntWriterConfiguration;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.transforms.GraphTransformers;
import ru.avicomp.ontapi.transforms.Transform;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Ontology building&loading factory. The 'core' of the system, the point to create and load ontologies.
 * See also base interface {@link OWLOntologyFactory} and its single implementation {@link OWLOntologyFactoryImpl}.
 * <p>
 * Created by szuev on 24.10.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntFactoryImpl implements OntologyManager.Factory {

    static {
        ErrorHandlerFactory.setDefaultErrorHandler(ErrorHandlerFactory.errorHandlerNoLogging);
    }

    protected final OntBuilderImpl ontologyBuilder;
    protected final OntLoader ontologyLoader;

    public OntFactoryImpl() {
        ontologyBuilder = new OntBuilderImpl();
        ontologyLoader = new OntModelLoaderImpl(ontologyBuilder);
    }

    @Override
    public boolean canCreateFromDocumentIRI(@Nonnull IRI iri) {
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
                                           @Nonnull OWLOntologyID id,
                                           @Nonnull IRI documentIRI,
                                           @Nonnull OWLOntologyCreationHandler handler) {
        return ontologyBuilder.make(asONT(manager), id);
    }

    @Override
    public OntologyModel loadOWLOntology(@Nonnull OWLOntologyManager manager,
                                         @Nonnull OWLOntologyDocumentSource source,
                                         @Nonnull OWLOntologyCreationHandler handler,
                                         @Nonnull OWLOntologyLoaderConfiguration configuration) throws OWLOntologyCreationException {
        return ontologyLoader.load(source, asONT(manager), asONT(configuration));
    }

    /**
     * Currently it is just sugar.
     *
     * @param manager {@link OWLOntologyManager}
     * @return {@link OntologyManager}
     */
    public static OntologyManager asONT(OWLOntologyManager manager) {
        return (OntologyManager) manager;
    }

    /**
     * Wraps {@link OntologyConfigurator} as {@link OntConfig}
     *
     * @param conf {@link OntologyConfigurator}
     * @return {@link OntConfig}
     */
    public static OntConfig asONT(OntologyConfigurator conf) {
        return conf instanceof OntConfig ? (OntConfig) conf : OntConfig.copy(conf);
    }

    /**
     * Wraps {@link OWLOntologyLoaderConfiguration} as {@link OntLoaderConfiguration}
     *
     * @param conf {@link OWLOntologyLoaderConfiguration}
     * @return {@link OntLoaderConfiguration}
     */
    public static OntLoaderConfiguration asONT(OWLOntologyLoaderConfiguration conf) {
        return conf instanceof OntLoaderConfiguration ? (OntLoaderConfiguration) conf : new OntLoaderConfiguration(conf);
    }

    /**
     * Wraps {@link OWLOntologyWriterConfiguration} as {@link OntWriterConfiguration}
     *
     * @param conf {@link OWLOntologyWriterConfiguration}
     * @return {@link OntWriterConfiguration}
     */
    public static OntWriterConfiguration asONT(OWLOntologyWriterConfiguration conf) {
        return conf instanceof OntWriterConfiguration ? (OntWriterConfiguration) conf : new OntWriterConfiguration(conf);
    }

    /**
     * Casts to ONT-API manager implementation.
     * This implementation contains a lot of useful methods which uses by this factory, but
     * these methods can't be moved to the interface since they are not common.
     *
     * @param manager {@link OWLOntologyManager manager}
     * @return {@link OntologyManagerImpl}
     * @throws ClassCastException in case of wrong instance specified
     */
    public static OntologyManagerImpl asIMPL(OWLOntologyManager manager) {
        return (OntologyManagerImpl) manager;
    }

    /**
     * Base class for any model loader.
     * Currently there are two implementations:
     * - pure OWL loader which calls super method of {@link OWLOntologyFactoryImpl}
     * - the {@link OntModelLoaderImpl}.
     * <p>
     * Note: there are only two input parameters in the constructor: {@link OntologyManager} and {@link OWLOntologyLoaderConfiguration}.
     * The single instance of {@link OntologyManager} is an {@link OWLOntologyManager} as well as {@link OWLOntologyCreationHandler}.
     * And this is also true for {@link uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl).
     * The {@link OWLOntologyCreationHandler} could be considered as part of inner (OWL-API) implementation,
     * so there is no need in this parameter in our case.
     */
    @SuppressWarnings("WeakerAccess")
    public interface OntLoader extends Serializable {

        /**
         * base method to load model ({@link OntologyModel}) to the manager ({@link OntologyManager}).
         * if the result model contains imports they should come as models also.
         *
         * @param source  {@link OWLOntologyDocumentSource} the source (iri, file iri, stream or who knows what)
         * @param manager {@link OntologyManager}
         * @param conf    {@link OntLoaderConfiguration}
         * @return {@link OntologyModel} the result model in the manager.
         * @throws OWLOntologyCreationException if something wrong.
         */
        OntologyModel load(OWLOntologyDocumentSource source, OntologyManager manager, OntLoaderConfiguration conf) throws OWLOntologyCreationException;

    }

    /**
     * To load {@link OntologyModel} through pure OWL-API mechanisms (using {@link OWLOntologyFactoryImpl}).
     * Some formats (such as {@link org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat} or {@link org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat}),
     * are not supported by jena, so it is the only way.
     */
    public static class OWLLoaderImpl implements OntLoader {
        protected static final Logger LOGGER = LoggerFactory.getLogger(OWLLoaderImpl.class);

        protected final OWLOntologyFactoryImpl factory;

        // to avoid recursion loop,
        // which may happen since OWL-API parsers may use the manager again, which uses factory with the same parsers
        protected Set<IRI> sources = new HashSet<>();

        public OWLLoaderImpl(OntBuilderImpl builder) {
            factory = new OWLOntologyFactoryImpl(OntApiException.notNull(builder, "Null builder"));
        }

        @Override
        public OntologyModel load(OWLOntologyDocumentSource source, OntologyManager manager, OntLoaderConfiguration conf) throws OWLOntologyCreationException {
            IRI doc = source.getDocumentIRI();
            if (sources.contains(doc)) {
                throw new BadRecursionException("Cycle loading for source " + doc);
            }
            sources.add(doc);
            OntologyModel res = (OntologyModel) factory.loadOWLOntology(manager, source, (OWLOntologyCreationHandler) manager, conf);
            sources.clear();
            if (LOGGER.isDebugEnabled()) {
                OntFormat format = OntFormat.get(manager.getOntologyFormat(res));
                LOGGER.debug("The ontology <{}> is loaded. Format: {}[{}]", res.getOntologyID(), format, source.getClass().getSimpleName());
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
    public static class OntModelLoaderImpl implements OntLoader {
        protected static final Logger LOGGER = LoggerFactory.getLogger(OntModelLoaderImpl.class);

        // following constants are copy-pasted from org.semanticweb.owlapi.io.DocumentSource:
        protected static final String TEXTPLAIN_REQUEST_TYPE = ", text/plain; q=0.1";
        protected static final String LAST_REQUEST_TYPE = ", */*; q=0.09";
        protected static final String DEFAULT_REQUEST = "application/rdf+xml, application/xml; q=0.7, text/xml; q=0.6" + TEXTPLAIN_REQUEST_TYPE + LAST_REQUEST_TYPE;
        // to use OWL-API parsers:
        protected OntLoader alternative;
        // state:
        protected Map<String, GraphInfo> graphs = new LinkedHashMap<>();
        protected Map<IRI, Optional<IRI>> sourceMap = new HashMap<>();
        private Map<IRI, GraphInfo> loaded = new HashMap<>();

        public OntModelLoaderImpl(OntLoader alternative) {
            this.alternative = alternative;
        }

        public OntModelLoaderImpl(OntBuilderImpl builder) {
            this(new OWLLoaderImpl(OntApiException.notNull(builder, "Null builder.")));
        }

        @Override
        public OntologyModel load(OWLOntologyDocumentSource source, OntologyManager manager, OntLoaderConfiguration config) throws OWLOntologyCreationException {
            if (config.isUseOWLParsersToLoad()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Load ontology using OWL-API methods. Source [{}]{}", source.getClass().getSimpleName(), source.getDocumentIRI());
                }
                return OntApiException.notNull(alternative, "No owl loader.").load(source, manager, config);
            }
            try {
                GraphInfo primary = loadGraph(source, manager, config);
                // null key in case of anonymous ontology.
                // But: only one anonymous is allowed (as root of imports tree), if there is no mapping in manager.
                graphs.put(primary.getURI(), primary);
                // first expand graphs map by creating primary model:
                OntologyModel res = OntApiException.notNull(createModel(primary, manager, config), "Should never happen");
                // then process all the rest dependent models (we have already all graphs compiled, now need populate them as models):
                List<GraphInfo> graphs = this.graphs.keySet().stream()
                        .filter(u -> !Objects.equals(u, primary.getURI()))
                        .map(k -> this.graphs.get(k)).collect(Collectors.toList());
                for (GraphInfo c : graphs) {
                    createModel(c, manager, config);
                }
                return res;
            } finally { // the possibility to reuse.
                clear();
            }
        }

        public void clear() {
            graphs.clear();
            sourceMap.clear();
            loaded.clear();
        }

        /**
         * Populates {@link Graph} as {@link OntologyModel} inside manager.
         *
         * @param info    {@link GraphInfo} container with info about graph.
         * @param manager {@link OntologyManager}
         * @param config  {@link OntLoaderConfiguration}
         * @return {@link OntologyModel}, it is ready to use.
         * @throws OWLOntologyCreationException if can't assemble model from ready graph.
         */
        protected OntologyModel createModel(GraphInfo info, OntologyManager manager, OntLoaderConfiguration config) throws OWLOntologyCreationException {
            if (!info.isFresh()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("The ontology {} is already configured.", info.name());
                }
                return null;
            }
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Set up ontology model {}.", info.name());
                }
                boolean isPrimary = graphs.size() == 1;
                Graph graph = makeUnionGraph(info, new HashSet<>(), manager, config);
                if (isPrimary && config.isPerformTransformation()) {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("Perform graph transformations.");
                    transform(graph, new HashSet<>(), config.getGraphTransformers());
                }
                OntFormat format = info.getFormat();
                OntologyManagerImpl impl = asIMPL(manager);
                OntologyModel res = impl.newOntologyModel(graph, config);
                if (manager.contains(res)) {
                    throw new OWLOntologyAlreadyExistsException(res.getOntologyID());
                }
                impl.ontologyCreated(res);
                OWLDocumentFormat owlFormat = format.createOwlFormat();
                if (PrefixManager.class.isInstance(owlFormat)) {
                    PrefixManager pm = (PrefixManager) owlFormat;
                    graph.getPrefixMapping().getNsPrefixMap().forEach(pm::setPrefix);
                    OntologyManagerImpl.setDefaultPrefix(pm, res);
                }
                if (isPrimary) {
                    // todo: should we pass stats from transforms? do we need it?
                    OWLOntologyLoaderMetaData fake = new RDFParserMetaData(RDFOntologyHeaderStatus.PARSED_ONE_HEADER, 0,
                            Collections.emptySet(), ArrayListMultimap.create());
                    owlFormat.setOntologyLoaderMetaData(fake);
                }
                manager.setOntologyFormat(res, owlFormat);
                if (info.getSource() != null) {
                    manager.setOntologyDocumentIRI(res, info.getSource());
                }
                return res;
            } finally { // just in case.
                info.setProcessed();
            }
        }

        /**
         * Assembles the {@link UnionGraph} from the inner collection ({@link #graphs}).
         *
         * @param node    {@link GraphInfo}
         * @param seen    Collection of URIs to avoid recursion infinite loops in imports (ontology A imports ontology B, which in turn imports A).
         * @param manager {@link OntologyManager} the manager
         * @param config  {@link OntLoaderConfiguration} the config
         * @return {@link Graph}
         * @throws OntApiException if something wrong.
         */
        protected Graph makeUnionGraph(GraphInfo node, Collection<String> seen, OntologyManager manager, OntLoaderConfiguration config) {
            // it is important to have the same order on each call
            Set<GraphInfo> children = new LinkedHashSet<>();
            Graph main = node.getGraph();
            String name = node.name();
            seen.add(node.getURI());
            List<String> imports = node.getImports().stream().sorted().collect(Collectors.toCollection(ArrayList::new));
            for (int i = 0; i < imports.size(); i++) {
                String uri = imports.get(i);
                if (seen.contains(uri)) {
                    continue;
                }
                OWLImportsDeclaration declaration = manager.getOWLDataFactory().getOWLImportsDeclaration(IRI.create(uri));
                if (config.isIgnoredImport(declaration.getIRI())) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("{}: {} is ignored.", name, declaration);
                    }
                    continue;
                }
                // graphs#computeIfAbsent:
                GraphInfo info = graphs.get(uri);
                try {
                    if (info == null)
                        info = fetchGraph(uri, manager, config);
                    graphs.put(uri, info);
                    // Anonymous ontology or ontology without header (i.e. if no "_:x rdf:type owl:Ontology") could be loaded
                    // if there is some resource-mapping in the manager on the import declaration.
                    // In this case we may load it as separated model or include to the parent graph:
                    if (info.isAnonymous() && MissingOntologyHeaderStrategy.INCLUDE_GRAPH.equals(config.getMissingOntologyHeaderStrategy())) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("{}: remove import declaration <{}>.", name, uri);
                        }
                        main.remove(Node.ANY, OWL.imports.asNode(), NodeFactory.createURI(uri));
                        GraphUtil.addInto(main, info.getGraph());
                        // skip assembling new model for this graph:
                        info.setProcessed();
                        // recollect imports (in case of anonymous ontology):
                        imports.addAll(i + 1, info.getImports());
                        continue;
                    }
                    children.add(info);
                } catch (OWLOntologyCreationException e) {
                    if (MissingImportHandlingStrategy.THROW_EXCEPTION.equals(config.getMissingImportHandlingStrategy())) {
                        throw new UnloadableImportException(e, declaration);
                    }
                    LOGGER.warn("Ontology {}: can't read sub graph with {}. Exception: {}", name, declaration, e.getMessage());
                }
            }
            UnionGraph res = new UnionGraph(main);
            children.forEach(ch -> res.addGraph(makeUnionGraph(ch, new HashSet<>(seen), manager, config)));
            return res;
        }

        /**
         * Recursively makes graph transformation.
         *
         * @param graph        {@link Graph}, in most cases it is {@link UnionGraph}.
         * @param processed    Set of base {@link Graph}s to not make transformation multiple times.
         * @param transformers {@link GraphTransformers.Store} the collection of transformers.
         * @return {@link Graph}
         * @see Transform
         */
        protected Graph transform(Graph graph, Set<Graph> processed, GraphTransformers.Store transformers) {
            if (graph instanceof UnionGraph) {
                ((UnionGraph) graph).getUnderlying().graphs().forEach(g -> transform(g, processed, transformers));
            }
            Graph base = Graphs.getBase(graph);
            if (processed.contains(base)) return graph;
            transformers.actions(graph).forEach(Transform::process);
            processed.add(base);
            return graph;
        }

        /**
         * Returns the {@link Graph} wrapped by {@link GraphInfo} which corresponds the specified ontology uri.
         * If there the model ({@link OntologyModel}) with the specified uri already exists inside manager then
         * the method returns the base graph from it.
         * Otherwise it tries to load graph directly by uri or using predefined document iri from
         * some manager's iri mapper (see {@link OWLOntologyIRIMapper}).
         *
         * @param uri     String the ontology uri.
         * @param manager {@link OntologyManager}
         * @param config  {@link OntLoaderConfiguration}
         * @return {@link GraphInfo} container with {@link Graph} encapsulated.
         * @throws ConfigMismatchException      a conflict with some settings from <code>config</code>
         * @throws OWLOntologyCreationException some serious I/O problem while loading
         * @throws OntApiException              some other unexpected problem occurred.
         */
        protected GraphInfo fetchGraph(String uri, OntologyManager manager, OntLoaderConfiguration config) throws OWLOntologyCreationException {
            IRI ontologyIRI = IRI.create(uri);
            OntologyModel res = findModel(manager, ontologyIRI);
            if (res != null) {
                return toGraphInfo(res, false);
            }
            IRI documentIRI = documentIRI(manager, ontologyIRI).orElse(ontologyIRI);
            // handle also the strange situation when there is no resource-mapping but a mapping on some existing ontology
            res = findModel(manager, documentIRI);
            if (res != null) {
                return toGraphInfo(res, false);
            }
            OWLOntologyID id = new OWLOntologyID(ontologyIRI);
            OWLOntologyDocumentSource source = manager.documentSourceMappers()
                    .map(f -> f.map(id)).findFirst()
                    .orElse(new IRIDocumentSource(documentIRI));
            return loadGraph(source, manager, config);
        }

        /**
         * Finds ontology by the IRI.
         * <p>
         * WARNING: the jena iri-resolver ({@link org.apache.jena.riot.system.IRIResolver})
         * makes all graphs iri's in one common form according to some inner rule, which I can't change,
         * but which, I believe, corresponds to the <a href='https://tools.ietf.org/html/rfc3986'>URI standard</a>,
         * ... and also to the <a href='http://ietf.org/rfc/rfc3987'>IRI standard</a>.
         * It happens while writing(saving) ontology as Turtle (at least).
         * And it looks like Jena(3.0.1) bug.
         * As a result if we have OWL-API IRI like this 'file:/C:/Users/admin/AppData/Local/Temp/tmp.file'
         * (which may come from expression {@code IRI.create({@link File})})
         * after reloading ontology it would looks like 'file:///C:/Users/admin/AppData/Local/Temp/tmp.file' inside graph.
         * This method is a quick workaround to handle correctly such situations.
         *
         * @param m   {@link OntologyManager}
         * @param iri {@link IRI}
         * @return {@link OntologyModel} or null.
         * @see org.apache.jena.riot.system.IRIResolver
         */
        protected OntologyModel findModel(OntologyManager m, IRI iri) {
            OntologyModel res = m.getOntology(new OWLOntologyID(Optional.of(iri), Optional.empty()));
            if (res != null) return res;
            if (iri.toString().startsWith("file://")) { // hack:
                iri = IRI.create(iri.toString().replaceAll("/+", "/"));
                return m.getOntology(new OWLOntologyID(Optional.of(iri), Optional.empty()));
            }
            return null;
        }

        /**
         * Finds a document iri from a manager iri mappers.
         * Some tests from OWL-API contract shows that calling the method {@link OWLOntologyIRIMapper#getDocumentIRI(IRI)}
         * should be performed only once during loading.
         * But this factory implementation sometimes needs a double calling - during jena and owl-api loadings.
         *
         * @param manager {@link OntologyManager the manager}
         * @param source  {@link IRI}
         * @return Optional around a mapped iri
         */
        private Optional<IRI> documentIRI(OntologyManager manager, IRI source) {
            Optional<IRI> res = sourceMap.get(source);
            if (res != null) {
                sourceMap.remove(source);
            } else {
                res = asIMPL(manager).mapIRI(source);
                sourceMap.put(source, res);
            }
            return res;
        }

        /**
         * Wraps a model as inner container.
         *
         * @param model {@link OntologyModel}
         * @param fresh if true result should be considered as fresh.
         * @return {@link GraphInfo}
         */
        protected GraphInfo toGraphInfo(OntologyModel model, boolean fresh) { // npe in case no format?
            OWLDocumentFormat owlFormat = model.getOWLOntologyManager().getOntologyFormat(model);
            OntFormat format = OntFormat.get(owlFormat);
            Graph graph = model.asGraphModel().getBaseGraph();
            if (PrefixManager.class.isInstance(owlFormat)) {
                PrefixManager pm = (PrefixManager) owlFormat;
                Models.setNsPrefixes(graph.getPrefixMapping(), pm.getPrefixName2PrefixMap());
            }
            return new GraphInfo(graph, format, fresh);
        }

        /**
         * Wraps a true graph as inner container.
         *
         * @param graph  {@link Graph}
         * @param format {@link OntFormat}
         * @param src    {@link IRI}
         * @return {@link GraphInfo}
         */
        protected GraphInfo toGraphInfo(Graph graph, OntFormat format, IRI src) {
            return new GraphInfo(graph, format, src);
        }

        /**
         * Loads a jena graph.
         * It is expected that this method will not affect the current state of specified manager if any error occurs.
         * The loading performs first through Apache Jena API, and in case of fail the OWL-API recursive mechanisms will be invoked.
         *
         * @param source  {@link OWLOntologyDocumentSource} the source
         * @param manager {@link OntologyManager} the manager to load
         * @param config  {@link OntLoaderConfiguration} configuration to manage process.
         * @return {@link GraphInfo} a wrapper around jena {@link Graph}
         * @throws OWLOntologyCreationException if loading is not possible.
         */
        public GraphInfo loadGraph(OWLOntologyDocumentSource source,
                                   OntologyManager manager,
                                   OntLoaderConfiguration config) throws OWLOntologyCreationException {
            if (loaded.containsKey(source.getDocumentIRI())) {
                return loaded.get(source.getDocumentIRI());
            }
            try {
                return loadJenaGraph(source, manager, config);
            } catch (UnsupportedFormatException e) {
                if (alternative == null) {
                    throw e;
                }
                if (source.getFormat().map(OntFormat::get).filter(OntFormat::isJena).isPresent()) {
                    if (e.getSuppressed().length == 1) {
                        LOGGER.warn("Jena loading fail: {}", e.getSuppressed()[0].getMessage());
                    } else {
                        LOGGER.warn("Jena loading fail!", e);
                    }
                }
                if (LOGGER.isDebugEnabled()) {
                    String msg = e.getMessage();
                    if (e.getCause() != null) {
                        msg += " => " + e.getCause().getMessage();
                    }
                    LOGGER.debug("Can't load using jena: {}. Try OWL-API mechanisms.", msg);
                }
                OntologyManagerImpl copy = createLoadCopy(manager);
                try {
                    // WARNING: it is a recursive part:
                    // The OWL-API will call some manager load methods which, in turn, will call a factory methods.
                    OntologyModel res = alternative.load(source, copy, config);
                    res.imports().forEach(o -> copy.documentIRIByOntology(o)
                            .ifPresent(iri -> loaded.put(iri, toGraphInfo((OntologyModel) o, true))));
                    return toGraphInfo(res, true);
                } catch (OWLOntologyCreationException ex) {
                    ex.addSuppressed(e);
                    throw ex;
                }
            }
        }

        /**
         * Creates a copy of specified manager special for loading operations through OWL-API mechanisms.
         * All loaded content would be stored inside a copy, not the original manager.
         *
         * @param from {@link OntologyManager}, the source
         * @return {@link OntologyManager}, the destination
         */
        protected OntologyManagerImpl createLoadCopy(OntologyManager from) {
            OntologyManagerImpl delegate = asIMPL(from);
            return new OntologyManagerImpl(delegate.getOWLDataFactory(), delegate.getLoadFactory(), new NoOpReadWriteLock()) {

                @Override
                protected Optional<OntologyModel> ontology(OWLOntologyID id) {
                    Optional<OntologyModel> res = delegate.ontology(id);
                    return res.isPresent() ? res : super.ontology(id);
                }

                @Override
                protected Optional<OntologyModel> importedOntology(OWLImportsDeclaration declaration) {
                    Optional<OntologyModel> res = delegate.importedOntology(declaration);
                    return res.isPresent() ? res : super.importedOntology(declaration);
                }

                @Override
                public boolean has(OWLOntology ontology) {
                    return delegate.has(ontology) || super.has(ontology);
                }

                @Override
                protected Optional<IRI> documentIRIByOntology(OWLOntology ontology) {
                    Optional<IRI> res = delegate.documentIRIByOntology(ontology);
                    return res.isPresent() ? res : super.documentIRIByOntology(ontology);
                }

                @Override
                protected Optional<OntologyModel> ontologyByDocumentIRI(IRI iri) {
                    Optional<OntologyModel> res = delegate.ontologyByDocumentIRI(iri);
                    return res.isPresent() ? res : super.ontologyByDocumentIRI(iri);
                }

                @Override
                protected Optional<IRI> mapIRI(IRI iri) {
                    return documentIRI(delegate, iri);
                }

                @Override
                public PriorityCollection<OWLParserFactory> getOntologyParsers() {
                    return delegate.getOntologyParsers();
                }

                @Override
                public OntConfig getOntologyConfigurator() {
                    return delegate.getOntologyConfigurator();
                }

                @Override
                public OntLoaderConfiguration getOntologyLoaderConfiguration() {
                    return delegate.getOntologyLoaderConfiguration();
                }

                @Override
                public String toString() {
                    return "CopyOf-" + delegate.toString();
                }
            };
        }

        /**
         * Loads the base graph from the source using jena.
         *
         * @param source  {@link OWLOntologyDocumentSource} with instructions how to reach the graph.
         * @param manager {@link OntologyManager} the manager to obtain IRI mappings, not null
         * @param config  {@link OntLoaderConfiguration}
         * @return {@link GraphInfo} wrapper around the {@link Graph}.
         * @throws UnsupportedFormatException   if source can't be read into graph using jena way.
         * @throws ConfigMismatchException      if conflict with some config settings.
         * @throws OWLOntologyCreationException if there is some serious I/O problem.
         * @throws OntApiException              if some other problem occurred.
         */
        protected GraphInfo loadJenaGraph(OWLOntologyDocumentSource source,
                                          OntologyManager manager,
                                          OntLoaderConfiguration config) throws OWLOntologyCreationException {
            Graph graph;
            OntFormat format;
            IRI src = source.getDocumentIRI();
            if (OntGraphDocumentSource.class.isInstance(source)) {
                OntGraphDocumentSource _source = (OntGraphDocumentSource) source;
                graph = _source.getGraph();
                format = _source.getOntFormat();
            } else {
                OWLOntologyDocumentSource _source = documentIRI(manager, src)
                        .map(IRIDocumentSource::new)
                        .map(OWLOntologyDocumentSource.class::cast)
                        .orElse(source);
                src = _source.getDocumentIRI();
                graph = OntModelFactory.createDefaultGraph();
                format = readGraph(graph, _source, config);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Graph <{}> is loaded. Source: {}[{}]. Format: {}",
                        Graphs.getName(graph), source.getClass().getSimpleName(), src, format);
            }
            return toGraphInfo(graph, format, src);
        }

        /**
         * The main method to read the source document to the graph.
         * For generality it is public.
         *
         * @param graph  {@link Graph} the graph(empty) to put in.
         * @param source {@link OWLOntologyDocumentSource} the source (encapsulates IO-stream, IO-Reader or IRI of document)
         * @param conf   {@link OntLoaderConfiguration} config
         * @return {@link OntFormat} corresponding to the specified source.
         * @throws UnsupportedFormatException   if source can't be read into graph using jena.
         * @throws ConfigMismatchException      if there is some conflict with config settings, anyway we can't continue.
         * @throws OWLOntologyCreationException if there is some serious IO problem
         * @throws OntApiException              if some other problem.
         */
        public static OntFormat readGraph(Graph graph, OWLOntologyDocumentSource source, OntLoaderConfiguration conf) throws OWLOntologyCreationException {
            IRI iri = OntApiException.notNull(source, "Null document source.").getDocumentIRI();
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("Read graph from <{}>.", iri);
            Supplier<OWLOntologyInputSourceException> orElse = () -> new OWLOntologyInputSourceException("Can't get input-stream/reader from " + iri);
            if (source.getInputStream().isPresent()) {
                return read(graph, source, s -> s.getInputStream().orElseThrow(orElse));
            }
            if (source.getReader().isPresent()) {
                return read(graph, source, s -> buffer(asInputStream(s.getReader().orElseThrow(orElse))));
            }
            if (conf.getSupportedSchemes().stream().noneMatch(s -> s.same(iri))) {
                throw new ConfigMismatchException("Not allowed scheme: " + iri);
            }
            String header = source.getAcceptHeaders().orElse(DEFAULT_REQUEST);
            return read(graph, source, s -> DocumentSources.getInputStream(iri, conf, header).orElseThrow(orElse));
        }

        /**
         * Tries to compute the {@link OntFormat} from {@link OWLOntologyDocumentSource} by using content type or uri or whatever else,
         * but not encapsulated OWL-format (which may absent).
         * public, for more generality.
         *
         * @param source {@link OWLOntologyDocumentSource}
         * @return {@link OntFormat} or null if it could not guess format from source.
         */
        public static OntFormat guessFormat(OWLOntologyDocumentSource source) {
            Lang lang;
            if (OntApiException.notNull(source, "Null document source.").getMIMEType().isPresent()) {
                lang = RDFLanguages.contentTypeToLang(source.getMIMEType().orElseThrow(OntApiException.supplier("Can't get mime type")));
            } else {
                lang = RDFLanguages.filenameToLang(source.getDocumentIRI().getIRIString());
            }
            return lang == null ? null : OntFormat.get(lang);
        }

        /**
         * Returns supported formats related to the source.
         * The result (ordered set) can only contain a single format in case the source has {@link OWLDocumentFormat},
         * otherwise it will contain all supported formats.
         *
         * @param source {@link OWLOntologyDocumentSource}
         * @return Set of {@link OntFormat}s
         * @throws UnsupportedFormatException if the format is present in the source but not valid
         */
        public static Set<OntFormat> getSupportedFormats(OWLOntologyDocumentSource source) throws UnsupportedFormatException {
            Set<OntFormat> res = new LinkedHashSet<>();
            if (source.getFormat().isPresent()) {
                OntFormat f = OntFormat.get(source.getFormat().get());
                if (f == null || !f.isReadSupported()) {
                    throw new UnsupportedFormatException("Format " + source.getFormat().get() + " is not supported.");
                }
                res.add(f);
                return res;
            }
            OntFormat first = guessFormat(source);
            if (first != null) {
                res.add(first);
            }
            OntFormat.formats().filter(OntFormat::isReadSupported).forEach(res::add);
            return res;
        }

        /**
         * Performs reading to the graph from the source using ont-supplier which produces input stream each time
         *
         * @param graph    {@link Graph}
         * @param source   {@link OWLOntologyDocumentSource}
         * @param supplier {@link OntInputSupplier}
         * @return {@link OntFormat}
         * @throws OWLOntologyCreationException if something is wrong.
         */
        protected static OntFormat read(Graph graph, OWLOntologyDocumentSource source, OntInputSupplier supplier) throws OWLOntologyCreationException {
            IRI iri = source.getDocumentIRI();
            final OWLOntologyCreationException cause = new UnsupportedFormatException(String.format("Can't read %s %s.",
                    source.getClass().getSimpleName(), iri));
            for (OntFormat format : getSupportedFormats(source)) {
                if (format.isOWLOnly()) {
                    cause.addSuppressed(new UnsupportedFormatException("Not supported by jena.").putFormat(format).putSource(iri));
                    continue;
                }
                Lang lang = format.getLang();
                try (InputStream is = supplier.open(source)) {
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("try <{}>", lang);
                    // with @base:
                    RDFDataMgr.read(graph, is, iri.toString(), lang);
                    return format;
                } catch (OWLOntologyInputSourceException | IOException e) {
                    throw new OWLOntologyCreationException(source.getClass().getSimpleName() + ": can't open or close input stream from " + iri, e);
                } catch (RuntimeException e) {
                    // could be org.apache.jena.shared.JenaException || org.apache.jena.atlas.AtlasException || org.apache.jena.atlas.json.JsonParseException || ...
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("<{}> failed: '{}'", lang, e.getMessage());
                    cause.addSuppressed(new UnsupportedFormatException(e).putSource(iri).putFormat(format));
                }
            }
            throw cause;
        }

        protected static InputStream asInputStream(Reader reader) {
            return new ReaderInputStream(reader, StandardCharsets.UTF_8);
        }

        protected static InputStream buffer(InputStream is) {
            return new BufferedInputStream(is);
        }

        protected interface OntInputSupplier {

            /**
             * NOTE: the caller is responsible for ensuring that the returned stream is closed.
             *
             * @param source {@link OWLOntologyDocumentSource}
             * @return {@link InputStream}
             * @throws OWLOntologyInputSourceException in case something wrong.
             */
            InputStream open(OWLOntologyDocumentSource source) throws OWLOntologyInputSourceException;
        }

        /**
         * Just container for {@link Graph} and {@link OntFormat}.
         * Used for simplification as temporary storage.
         */
        public class GraphInfo {
            private final OntFormat format;
            private final Graph graph;
            private boolean fresh;
            private String uri;
            private Set<String> imports;
            private IRI source;

            protected GraphInfo(Graph graph, OntFormat format, boolean fresh) {
                this.graph = graph;
                this.format = format;
                this.fresh = fresh;
            }

            protected GraphInfo(Graph graph, OntFormat format, IRI source) {
                this(graph, format, true);
                this.source = source;
            }

            protected String getURI() {
                return uri == null ? uri = Graphs.getURI(graph) : uri;
            }

            protected boolean isAnonymous() {
                return getURI() == null;
            }

            protected String name() {
                return Graphs.getName(graph);
            }

            protected Set<String> getImports() {
                return imports == null ? imports = Graphs.getImports(graph) : imports;
            }

            protected boolean isFresh() {
                return fresh;
            }

            protected void setProcessed() {
                this.fresh = false;
            }

            protected OntFormat getFormat() {
                return format;
            }

            public Graph getGraph() {
                return graph;
            }

            protected IRI getSource() {
                return source;
            }
        }
    }

    public static class OntBuilderImpl implements OWLOntologyBuilder {

        @Override
        public OntologyModel createOWLOntology(@Nonnull OWLOntologyManager manager, @Nonnull OWLOntologyID id) {
            OntologyManagerImpl m = asIMPL(manager);
            OntologyModelImpl ont = new OntologyModelImpl(m, id);
            return m.isConcurrent() ? ont.asConcurrent() : ont;
        }

        /**
         * Creates fresh ontology (ONT) inside manager.
         * I hate RDF/XML. The default format is Turtle (it is difference with OWL-API).
         *
         * @param manager {@link OntologyManager}
         * @param id      {@link OWLOntologyID}
         * @return {@link OntologyModel}
         * @see OntFormat#TURTLE
         */
        public OntologyModel make(@Nonnull OntologyManager manager, @Nonnull OWLOntologyID id) {
            OntologyModel res = createOWLOntology(manager, id);
            asIMPL(manager).ontologyCreated(res);
            manager.setOntologyFormat(res, OntFormat.TURTLE.createOwlFormat());
            return res;
        }
    }

    public static class ConfigMismatchException extends OWLOntologyCreationException {
        public ConfigMismatchException(String s) {
            super(s);
        }
    }

    @SuppressWarnings("unused")
    public static class UnsupportedFormatException extends OWLOntologyCreationException {
        private OntFormat format;
        private IRI source;

        public UnsupportedFormatException(String message, Throwable cause) {
            super(message, cause);
        }

        public UnsupportedFormatException(String message) {
            super(message);
        }

        public UnsupportedFormatException(Throwable cause) {
            super(cause);
        }

        public UnsupportedFormatException putFormat(OntFormat format) {
            this.format = OntApiException.notNull(format, "Null format!");
            return this;
        }

        public UnsupportedFormatException putSource(IRI iri) {
            this.source = OntApiException.notNull(iri, "Null format!");
            return this;
        }

        @Override
        public String getMessage() {
            Throwable cause = getCause();
            String msg = super.getMessage();
            if (format != null && source != null && cause != null) {
                String suffix = String.format("Format: %s. IRI: <%s>. Cause: '%s'", format, source, cause.getMessage());
                if (StringUtils.isEmpty(msg)) {
                    msg = suffix;
                } else {
                    msg += ". " + suffix;
                }
            }
            return msg;
        }
    }

    public static class BadRecursionException extends OWLOntologyCreationException {

        public BadRecursionException(String message) {
            super(message);
        }
    }

}
