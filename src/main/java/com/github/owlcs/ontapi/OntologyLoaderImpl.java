/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi;

import com.github.owlcs.ontapi.config.OntConfig;
import com.github.owlcs.ontapi.config.OntLoaderConfiguration;
import com.github.owlcs.ontapi.jena.UnionGraph;
import com.github.owlcs.ontapi.jena.utils.Graphs;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.transforms.GraphStats;
import com.github.owlcs.ontapi.transforms.TransformException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Jena based implementation of {@link OntologyLoader}.
 * Apache Jena is used as a primary way to load ontologies into the manager.
 * Should resolves any problems (such as cycle imports) or throws informative exceptions.
 * In case of some problems while loading there is no need to clear manager to keep it synchronized
 * since models are assembled after obtaining the graphs collection.
 * If format is not suitable for Jena, or {@link OntLoaderConfiguration#isUseOWLParsersToLoad()} is specified,
 * or some I/O error has occurred,
 * then an alternative OWL-API based {@link OntologyLoader loader} will be used (see {@link OWLFactoryWrapper}).
 * Notice that OWL-API {@code Loader} is not a substitute for this {@code Loader}
 * and its direct usage is generally dangerous:
 * <ul>
 *     <li>a resulting graph may be corrupted: OWL-API parsers are OWL-centric
 *     and they may misinterpret RDF constructions that have not formed by OWL-API itself</li>
 *     <li>OWL-API factory affects manager: in case of error there is a dangerous of its broken state</li>
 *     <li>see {@link com.github.owlcs.ontapi.config.LoadSettings#isUseOWLParsersToLoad()}</li>
 * </ul>
 *
 * @see OWLFactoryWrapper
 * @see com.github.owlcs.ontapi.config.LoadSettings#isUseOWLParsersToLoad()
 */
@SuppressWarnings("WeakerAccess")
public class OntologyLoaderImpl implements OntologyFactory.Loader {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyLoaderImpl.class);

    // to use OWL-API parsers:
    protected final OntologyLoader alternative;
    // state parameters:
    protected Map<String, GraphInfo> graphs = new LinkedHashMap<>();
    protected Map<IRI, Optional<IRI>> sourceMap = new HashMap<>();
    protected Map<IRI, GraphInfo> loaded = new HashMap<>();

    /**
     * Creates a default {@code Loader} instance.
     * This is a Jena based loader,
     * but the constructor may optionally accept another loader,
     * which will be used as alternative if the primary way fails.
     * Usually, the additional loader is used to handle documents in OWL-API native formats.
     *
     * @param alternative {@link OntologyLoader}, can be {@code null}
     */
    public OntologyLoaderImpl(OntologyLoader alternative) {
        this.alternative = alternative;
    }

    @Override
    public OWLAdapter getAdapter() {
        return OWLAdapter.get();
    }

    /**
     * Represents this loader as an ontology factory.
     *
     * @param builder {@link OntologyCreator}
     * @return {@link OntologyFactory}
     */
    @Override
    public OntologyFactory asOntologyFactory(@Nonnull OntologyCreator builder) {
        return new OntologyFactoryImpl(getAdapter().asBuilder(builder), this);
    }

    @Override
    public Ontology loadOntology(OntologyCreator builder,
                                 OntologyManager manager,
                                 OWLOntologyDocumentSource source,
                                 OntLoaderConfiguration config) throws OWLOntologyCreationException {
        if (config.isUseOWLParsersToLoad()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Load ontology using OWL-API methods. Source [{}]{}",
                        source.getClass().getSimpleName(), source.getDocumentIRI());
            }
            return OntApiException.mustNotBeNull(alternative, "No OWL loader found.").loadOntology(builder, manager, source, config);
        }
        try {
            GraphInfo primary = loadGraph(source, builder, manager, config);
            // null key in case of anonymous ontology.
            // But: only one anonymous is allowed (as root of imports tree), if there is no mapping in manager.
            graphs.put(primary.getURI(), primary);
            // first expand graphs map by creating primary model:
            Ontology res = OntApiException.notNull(createModel(primary, builder, manager, config), "Should never happen");
            // then process all the rest dependent models
            // (we have already all graphs compiled, now need populate them as models):
            List<GraphInfo> graphs = this.graphs.keySet().stream()
                    .filter(u -> !Objects.equals(u, primary.getURI()))
                    .map(k -> this.graphs.get(k)).collect(Collectors.toList());
            for (GraphInfo g : graphs) {
                createModel(g, builder, manager, config);
            }
            return res;
        } finally {
            // the possibility to reuse:
            clear();
        }
    }

    /**
     * Release the state parameters that can appear during the load.
     *
     * @see OWLFactoryWrapper#clear()
     */
    public void clear() {
        graphs.clear();
        sourceMap.clear();
        loaded.clear();
    }

    /**
     * Populates {@link Graph} as {@link Ontology} inside manager.
     *
     * @param info    {@link GraphInfo} container with info about the graph
     * @param builder {@link OntologyCreator} to construct a fresh {@link Ontology} instance
     * @param manager {@link OntologyManager}
     * @param config  {@link OntLoaderConfiguration}
     * @return {@link Ontology}, it is ready to use.
     * @throws OWLOntologyCreationException if can't assemble model from ready graph.
     */
    protected Ontology createModel(GraphInfo info,
                                   OntologyCreator builder,
                                   OntologyManager manager,
                                   OntLoaderConfiguration config) throws OWLOntologyCreationException {
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
            Graph graph = makeUnionGraph(info, builder, manager, config);
            // create ontology instance
            Ontology res = builder.createOntology(graph, manager, config);
            if (manager.contains(res)) {
                throw new OWLOntologyAlreadyExistsException(res.getOntologyID());
            }
            if (!info.isAnonymous()) {
                // Restores possible missed import links between the given ontology and existing in the manager.
                // Such situation may occur if some ontology has been added with unresolved imports,
                // which is possible, for example
                // if org.semanticweb.owlapi.model.MissingImportHandlingStrategy#SILENT was specified.
                OntModels.insert(manager::models, res.asGraphModel(), false);
            }
            // put ontology inside the manager:
            getAdapter().asHandler(manager).ontologyCreated(res);
            OntFormat format = OntApiException.notNull(info.getFormat(), "Null format while loading " + info.name());
            OWLDocumentFormat owl = format.newOWLFormat();
            if (owl.isPrefixOWLDocumentFormat()) {
                PrefixManager pm = owl.asPrefixOWLDocumentFormat();
                graph.getPrefixMapping().getNsPrefixMap().forEach(pm::setPrefix);
            }
            owl.setOntologyLoaderMetaData(OntGraphUtils.makeParserMetaData(graph, info.getStats()));
            manager.setOntologyFormat(res, owl);
            if (info.getSource() != null) {
                manager.setOntologyDocumentIRI(res, info.getSource());
            }
            return res;
        } finally { // just in case:
            info.setProcessed();
        }
    }

    /**
     * Assembles the {@link UnionGraph}, performs transformations on it and populates {@link #graphs graphs collection}.
     *
     * @param info    {@link GraphInfo} container with info about graph
     * @param builder {@link OntologyCreator} to construct a fresh {@link UnionGraph} instance
     * @param manager {@link OntologyManager}
     * @param config  {@link OntLoaderConfiguration}
     * @return {@link UnionGraph}
     * @throws OntologyFactoryImpl.OWLTransformException in case of error while transformation
     * @throws OntApiException                           if something goes wrong
     */
    protected UnionGraph makeUnionGraph(GraphInfo info,
                                        OntologyCreator builder,
                                        OntologyManager manager,
                                        OntLoaderConfiguration config) throws OntologyFactoryImpl.OWLTransformException {
        boolean isPrimary = graphs.size() == 1;
        // #makeUnionGraph will change #graphs collection:
        UnionGraph graph = makeUnionGraph(info, new HashSet<>(), builder, manager, config);

        if (!isPrimary || info.noTransforms() || !config.isPerformTransformation()) {
            // no transformations needed
            return graph;
        }
        // process transformations
        GraphStats stats;
        Set<Graph> transformed = graphs.values().stream()
                .filter(g -> !g.isFresh() || g.noTransforms())
                .map(GraphInfo::getGraph)
                .collect(Collectors.toSet());
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("Perform graph transformations on <{}>.", info.name());
        try {
            stats = config.getGraphTransformers().transform(graph, transformed);
        } catch (TransformException t) {
            throw new OntologyFactoryImpl.OWLTransformException(t);
        }
        info.setStats(stats);
        stats.stats(true)
                .filter(GraphStats::isNotEmpty)
                .forEach(s -> {
                    String uri = Graphs.getURI(s.getGraph());
                    if (uri == null) {
                        LOGGER.warn("Not a named graph {}", Graphs.getName(s.getGraph()));
                        return;
                    }
                    GraphInfo g = graphs.get(uri);
                    if (g == null) {
                        LOGGER.warn("Unable to find a graph for {}", Graphs.getName(s.getGraph()));
                        return;
                    }
                    g.setStats(s);
                });
        return graph;
    }

    /**
     * Assembles the {@link UnionGraph} from the inner collection ({@link #graphs}).
     * Note: this collection can be modified by this method.
     *
     * @param node    {@link GraphInfo} the root graph
     * @param seen    a {@code Collection} of URIs to avoid recursion infinite loops in imports
     *                (ontology A imports ontology B, which in turn imports A)
     * @param builder {@link OntologyCreator} to construct a fresh {@link UnionGraph} instance
     * @param manager {@link OntologyManager} the manager
     * @param config  {@link OntLoaderConfiguration} the config
     * @return {@link UnionGraph}
     * @throws OntApiException if something wrong
     * @see OntGraphUtils#toGraphMap(Graph)
     */
    protected UnionGraph makeUnionGraph(GraphInfo node,
                                        Collection<String> seen,
                                        OntologyCreator builder,
                                        OntologyManager manager,
                                        OntLoaderConfiguration config) {
        Graph graph = node.getGraph();
        if (graph instanceof UnionGraph) {
            // this situation may occur only in a single case
            // when the graph is passed into OntologyManager#addOntology, see OntGraphUtils#toGraphMap(Graph)
            UnionGraph u = (UnionGraph) graph;
            if (!u.getUnderlying().isEmpty())
                throw new OntApiException.IllegalState("A given graph has a hierarchy structure: " + graph);
            // always need to create a _new_ UnionGraph: the old may have listeners or caches attached
            graph = u.getBaseGraph();
        }
        UnionGraph res = builder.createUnionGraph(graph, config);
        if (config.isProcessImports()) {
            processImports(node, seen, builder, manager, config)
                    .forEach(ch -> res.addGraph(makeUnionGraph(ch, new HashSet<>(seen), builder, manager, config)));
        }
        return res;
    }

    /**
     * Processes the {@code owl:imports} declarations for the given (root) graph,
     * returns a ready collection of children {@link GraphInfo graphs}.
     *
     * @param node    {@link GraphInfo} the root graph
     * @param seen    a {@code Collection} of URIs to avoid recursion infinite loops in imports
     *                (ontology A imports ontology B, which in turn imports A)
     * @param builder {@link OntologyCreator} to create {@link UnionGraph} instances
     * @param manager {@link OntologyManager} the manager
     * @param config  {@link OntLoaderConfiguration} the config
     * @return a {@code Collection} of {@link GraphInfo}s
     * @throws OntApiException if something wrong
     */
    protected Collection<GraphInfo> processImports(GraphInfo node,
                                                   Collection<String> seen,
                                                   OntologyCreator builder,
                                                   OntologyManager manager,
                                                   OntLoaderConfiguration config) {
        Graph base = node.getGraph();
        String name = node.name();
        seen.add(node.getURI());
        // it is important to have the same order on each call
        Set<GraphInfo> res = new LinkedHashSet<>();
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
                    info = fetchGraph(uri, builder, manager, config);
                graphs.put(uri, info);
                // Anonymous ontology or ontology without header (i.e. if no "_:x rdf:type owl:Ontology") could be loaded
                // if there is some resource-mapping in the manager on the import declaration.
                // In this case we may load it as separated model or include to the parent graph:
                if (info.isAnonymous()
                        && MissingOntologyHeaderStrategy.INCLUDE_GRAPH.equals(config.getMissingOntologyHeaderStrategy())) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("<{}>: remove import declaration <{}>.", name, uri);
                    }
                    base.remove(Node.ANY, OWL.imports.asNode(), NodeFactory.createURI(uri));
                    GraphUtil.addInto(base, info.getGraph());
                    // skip assembling new model for this graph:
                    info.setProcessed();
                    // recollect imports (in case of anonymous ontology):
                    imports.addAll(i + 1, info.getImports());
                    continue;
                }
                res.add(info);
            } catch (OWLOntologyCreationException e) {
                if (MissingImportHandlingStrategy.THROW_EXCEPTION.equals(config.getMissingImportHandlingStrategy())) {
                    throw new UnloadableImportException(e, declaration);
                }
                LOGGER.warn("Ontology <{}>: can't read sub graph with {}. Exception: '{}'",
                        name, declaration, e.getMessage());
            }
        }
        return res;
    }

    /**
     * Returns the {@link Graph} wrapped by {@link GraphInfo} which corresponds the specified ontology uri.
     * If there the model ({@link Ontology}) with the specified uri already exists inside manager then
     * the method returns the base graph from it.
     * Otherwise it tries to load graph directly by uri or using predefined document iri from
     * some manager's iri mapper (see {@link OWLOntologyIRIMapper}).
     *
     * @param uri     String, the ontology uri
     * @param builder {@link OntologyCreator} to construct a fresh {@link UnionGraph} instance
     * @param manager {@link OntologyManager}
     * @param config  {@link OntLoaderConfiguration}
     * @return {@link GraphInfo} container with {@link Graph} encapsulated
     * @throws OntologyFactoryImpl.ConfigMismatchException a conflict with some settings from <code>config</code>
     * @throws OWLOntologyCreationException                some serious I/O problem while loading
     * @throws OntApiException                             some other unexpected problem occurred.
     */
    protected GraphInfo fetchGraph(String uri,
                                   OntologyCreator builder,
                                   OntologyManager manager,
                                   OntLoaderConfiguration config) throws OWLOntologyCreationException {
        IRI ontologyIRI = IRI.create(uri);
        Ontology res = findModel(manager, ontologyIRI);
        if (res != null) {
            return toGraphInfo(res, null);
        }
        // IRI Mappers:
        IRI documentIRI = documentIRI(manager, ontologyIRI).orElse(ontologyIRI);
        // handle also the strange situation when there is no resource-mapping but a mapping on some existing ontology
        res = findModel(manager, documentIRI);
        if (res != null) {
            return toGraphInfo(res, null);
        }
        // Document Source Mappers:
        OWLOntologyDocumentSource source = manager.getDocumentSourceMappers().stream()
                .map(f -> f.map(ontologyIRI))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(new IRIDocumentSource(documentIRI));
        return loadGraph(source, builder, manager, config);
    }

    /**
     * Finds ontology by the IRI.
     * <p>
     * WARNING: the jena iri-resolver ({@link org.apache.jena.riot.system.IRIResolver})
     * makes all graphs IRIs in one common form according to some inner rule, which I can't change,
     * but which, I believe, corresponds to the <a href='https://tools.ietf.org/html/rfc3986'>URI standard</a>,
     * ... and also to the <a href='http://ietf.org/rfc/rfc3987'>IRI standard</a>.
     * It happens while writing(saving) ontology as Turtle (at least).
     * And it looks like Jena(3.0.1) bug.
     * As a result if we have OWL-API IRI like this 'file:/C:/Users/admin/AppData/Local/Temp/tmp.file'
     * (which may come from expression {@code IRI.create({@link File })})
     * after reloading ontology it would looks like 'file:///C:/Users/admin/AppData/Local/Temp/tmp.file' inside graph.
     * This method is a quick workaround to handle correctly such situations.
     *
     * @param m   {@link OntologyManager}
     * @param iri {@link IRI}
     * @return {@link Ontology} or null.
     * @see org.apache.jena.riot.system.IRIResolver
     */
    protected Ontology findModel(OntologyManager m, IRI iri) {
        Ontology res = m.getOntology(ID.create(iri));
        if (res != null) return res;
        if (iri.toString().startsWith("file://")) { // hack:
            iri = IRI.create(iri.toString().replaceAll("/+", "/"));
            return m.getOntology(ID.create(iri));
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
        //noinspection OptionalAssignedToNull
        if (res != null) {
            sourceMap.remove(source);
        } else {
            res = getAdapter().asIMPL(manager).mapIRI(source);
            sourceMap.put(source, res);
        }
        return res;
    }

    /**
     * Wraps an already existed model as inner container.
     *
     * @param model {@link Ontology ontology}
     * @param src   the document source {@link IRI}, {@code null} to indicate the ontology is existing
     * @return {@link GraphInfo graph-wrapper}
     */
    protected GraphInfo toGraphInfo(Ontology model, IRI src) {
        OWLDocumentFormat owl = model.getOWLOntologyManager().getOntologyFormat(model);
        Graph graph = model.asGraphModel().getBaseGraph();
        OntFormat format = null;
        if (owl != null) {
            format = OntFormat.get(owl);
            if (src != null && owl.isPrefixOWLDocumentFormat()) {
                // pass prefixes from the manager (that is supposed to be external) to the graph
                graph.getPrefixMapping()
                        .setNsPrefixes(OntGraphUtils.prefixMapping(owl.asPrefixOWLDocumentFormat()));
            }
        }
        return createGraphInfo(graph, format, src, false);
    }

    /**
     * Creates a {@link Graph graph info} container.
     *
     * @param graph         {@link Graph}
     * @param format        {@link OntFormat}
     * @param src           {@link IRI}
     * @param withTransform boolean
     * @return {@link GraphInfo}
     */
    protected GraphInfo createGraphInfo(Graph graph, OntFormat format, IRI src, boolean withTransform) {
        return new GraphInfo(graph, format, src, withTransform);
    }

    /**
     * Loads the {@link Graph Jena Graph} from the given document source as a graph-info container.
     * It is expected that this method will not affect the state of the specified manager if any error occurs.
     * The loading performs first through Apache Jena API,
     * and in case of fail the OWL-API recursive mechanisms are invoked.
     * In special case of {@link OntGraphDocumentSource} the loading is not performed and graph is passed as is.
     *
     * @param source  {@link OWLOntologyDocumentSource the document source}
     * @param builder {@link OntologyCreator} to construct a fresh {@link Graph} instance
     * @param manager {@link OntologyManager the manager} to load
     * @param config  {@link OntLoaderConfiguration the load configuration} to manage process
     * @return {@link GraphInfo graph-info} a wrapper around jena {@link Graph}
     * @throws OWLOntologyCreationException if loading is not possible
     * @see OntGraphDocumentSource
     */
    public GraphInfo loadGraph(OWLOntologyDocumentSource source,
                               OntologyCreator builder,
                               OntologyManager manager,
                               OntLoaderConfiguration config) throws OWLOntologyCreationException {
        if (source instanceof OntGraphDocumentSource) {
            OntGraphDocumentSource src = (OntGraphDocumentSource) source;
            Graph graph = src.getGraph();
            OntFormat format = src.getOntFormat();
            return createGraphInfo(graph, format, source.getDocumentIRI(), src.withTransforms());
        }
        if (loaded.containsKey(source.getDocumentIRI())) {
            return loaded.get(source.getDocumentIRI());
        }
        IRI doc = source.getDocumentIRI();
        OWLOntologyDocumentSource src = documentIRI(manager, doc)
                .map(IRIDocumentSource::new)
                .map(OWLOntologyDocumentSource.class::cast)
                .orElse(source);
        try {
            // jena:
            Graph graph = builder.createGraph();
            OntFormat format = OntGraphUtils.readGraph(graph, src, config);
            GraphInfo res = createGraphInfo(graph, format, doc, true);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Graph <{}> is loaded by jena. Source: {}[{}]. Format: {}",
                        res.name(), source.getClass().getSimpleName(), res.getSource(), res.getFormat());
            }
            return res;
        } catch (OntologyFactoryImpl.UnsupportedFormatException jenaEx) {
            // owl-api:
            if (alternative == null) {
                throw jenaEx;
            }
            // if there is an explicit format specified:
            if (source.getFormat().map(OntFormat::get).filter(OntFormat::isJena).isPresent()) {
                if (jenaEx.getSuppressed().length == 1) {
                    LOGGER.warn("Jena loading fail: {}", jenaEx.getSuppressed()[0].getMessage());
                } else {
                    LOGGER.warn("Jena loading fail!", jenaEx);
                }
            }
            if (LOGGER.isDebugEnabled()) {
                String msg = jenaEx.getMessage();
                if (jenaEx.getCause() != null) {
                    msg += " => " + jenaEx.getCause().getMessage();
                }
                LOGGER.debug("Can't load using Apache Jena: {}. Try OWL-API mechanisms.", msg);
            }
            OntologyCreator _builder = createBuilderFrom(alternative, builder);
            OntLoaderConfiguration _config = createConfigFrom(alternative, config);
            OntologyManagerImpl _manager = createLoadCopy(_builder, manager, config);
            try {
                // WARNING: it is a recursive part:
                // The OWL-API will call some manager load methods which, in turn, will call a factory methods.
                Ontology ont = alternative.loadOntology(_builder, _manager, src, _config);
                ont.imports().forEach(o -> _manager.documentIRIByOntology(o)
                        .ifPresent(iri -> loaded.put(iri, toGraphInfo(getAdapter().asONT(o), iri))));
                GraphInfo res = toGraphInfo(ont, doc);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Graph <{}> is loaded by OWL-API. Source: {}[{}]. Format: {}",
                            res.name(), source.getClass().getSimpleName(), res.getSource(), res.getFormat());
                }
                return res;
            } catch (OWLOntologyCreationException owlEx) {
                owlEx.addSuppressed(jenaEx);
                throw owlEx;
            }
        }
    }

    /**
     * Optimizes the given builder for the given loader.
     *
     * @param loader {@link OntologyLoader}, not {@code null}
     * @param from   {@link OntologyCreator}, not {@code null}
     * @return {@link OntologyCreator}
     * @since 1.4.2
     */
    protected OntologyCreator createBuilderFrom(OntologyLoader loader, OntologyCreator from) {
        return loader instanceof OWLFactoryWrapper ? ((OWLFactoryWrapper) loader).optimize(from) : from;
    }

    /**
     * Optimizes the given config for the given loader.
     *
     * @param loader {@link OntologyLoader}, not {@code null}
     * @param from   {@link OntLoaderConfiguration}, not {@code null}
     * @return {@link OntLoaderConfiguration}
     * @since 1.4.2
     */
    protected OntLoaderConfiguration createConfigFrom(OntologyLoader loader, OntLoaderConfiguration from) {
        return loader instanceof OWLFactoryWrapper ? ((OWLFactoryWrapper) loader).optimize(from) : from;
    }


    /**
     * Creates a copy of specified manager special for loading operations through OWL-API mechanisms.
     * All loaded content would be stored inside a copy, not the original manager.
     *
     * @param builder to get {@link OntologyFactory} instance
     * @param from          {@link OntologyManager}, the source manager
     * @param defaultConfig {@link OntLoaderConfiguration}, the default loader config, nullable
     * @return {@link OntologyManager}, the target manager
     */
    protected OntologyManagerImpl createLoadCopy(OntologyCreator builder,
                                                 OntologyManager from,
                                                 OntLoaderConfiguration defaultConfig) {
        OntologyManagerImpl delegate = getAdapter().asIMPL(from);
        OntologyFactory factory = asOntologyFactory(builder);
        return new OntologyManagerImpl(delegate.getOWLDataFactory(), factory, null) {

            @Override
            protected Optional<Ontology> ontology(OWLOntologyID id) {
                Optional<Ontology> res = delegate.ontology(id);
                return res.isPresent() ? res : super.ontology(id);
            }

            @Override
            protected Optional<Ontology> importedOntology(IRI declaration) {
                Optional<Ontology> res = delegate.importedOntology(declaration);
                return res.isPresent() ? res : super.importedOntology(declaration);
            }

            @Override
            protected Ontology loadImports(IRI declaration, OWLOntologyLoaderConfiguration conf)
                    throws OWLOntologyCreationException {
                return super.loadImports(declaration, makeImportConfig(conf));
            }

            private OntLoaderConfiguration makeImportConfig(OWLOntologyLoaderConfiguration conf) {
                return conf instanceof OntLoaderConfiguration ? (OntLoaderConfiguration) conf :
                        defaultConfig == null ? delegate.getOntologyConfigurator().buildLoaderConfiguration() : defaultConfig;
            }

            @Override
            public boolean hasOntology(OWLOntology ontology) {
                return delegate.hasOntology(ontology) || super.hasOntology(ontology);
            }

            @Override
            protected Optional<IRI> documentIRIByOntology(OWLOntology ontology) {
                Optional<IRI> res = delegate.documentIRIByOntology(ontology);
                return res.isPresent() ? res : super.documentIRIByOntology(ontology);
            }

            @Override
            protected Optional<Ontology> ontologyByDocumentIRI(IRI iri) {
                Optional<Ontology> res = delegate.ontologyByDocumentIRI(iri);
                return res.isPresent() ? res : super.ontologyByDocumentIRI(iri);
            }

            @Override
            protected Optional<IRI> mapIRI(IRI iri) {
                return documentIRI(delegate, iri);
            }

            @Override
            public RWLockedCollection<OWLParserFactory> getOntologyParsers() {
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
     * A container for a {@link Graph graph} and some load parameters, such as source-iri and format.
     * Used for simplification as temporary storage by this factory only.
     */
    public static class GraphInfo {
        // constant language format
        private final OntFormat format;
        // graph is also a prefixes holder
        private final Graph graph;
        // source may be null
        private final IRI source;

        private boolean fresh;
        private final boolean transforms;
        private Node ontology;
        private Set<String> imports;
        private GraphStats stats;

        protected GraphInfo(Graph graph, OntFormat format, IRI source, boolean withTransforms) {
            this.graph = graph;
            this.format = format;
            this.source = source;
            this.fresh = source != null;
            this.transforms = withTransforms;
        }

        protected Node ontology() {
            return ontology == null ?
                    ontology = Graphs.ontologyNode(Graphs.getBase(graph))
                            .orElse(NodeFactory.createVariable("NullOntology")) :
                    ontology;
        }

        public String getURI() {
            return ontology().isURI() ? ontology().getURI() : null;
        }

        public boolean isAnonymous() {
            return getURI() == null;
        }

        protected String name() {
            return ontology().toString();
        }

        protected Set<String> getImports() {
            return imports == null ? imports = Graphs.getImports(graph) : imports;
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        protected boolean isFresh() {
            return fresh;
        }

        protected boolean noTransforms() {
            return !transforms;
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

        public GraphStats getStats() {
            return stats;
        }

        protected void setStats(GraphStats stats) {
            this.stats = Objects.requireNonNull(stats, "Null transform stats");
        }
    }
}
