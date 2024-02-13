/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
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

import com.github.owlcs.ontapi.config.CacheSettings;
import com.github.owlcs.ontapi.config.OntLoaderConfiguration;
import com.github.sszuev.jena.ontapi.UnionGraph;
import javax.annotation.ParametersAreNonnullByDefault;
import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.io.OWLOntologyCreationIOException;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyInputSourceException;
import org.semanticweb.owlapi.io.OWLParser;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyBuilder;
import org.semanticweb.owlapi.model.OWLOntologyChangeVisitorEx;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFactory;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PriorityCollectionSorting;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.model.SetOntologyID;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.util.PriorityCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * An implementation of {@link OntologyLoader} which is actually {@link OWLOntologyFactory} decorator.
 * Used to load {@link Ontology ontology} through pure OWL-API mechanisms (i.e. owl-api parsers).
 * Some formats (such as {@link org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat} or
 * {@link org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat}) are not supported by Jena,
 * so this is the only way to handle them by ONT-API.
 *
 * @see OntologyLoaderImpl
 * @see com.github.owlcs.ontapi.config.LoadSettings#isUseOWLParsersToLoad()
 */
@SuppressWarnings("WeakerAccess")
@ParametersAreNonnullByDefault
public class OWLFactoryWrapper implements OntologyFactory.Loader {
    private static final Logger LOGGER = LoggerFactory.getLogger(OWLFactoryWrapper.class);

    // a set to avoid recursion loop,
    // which may happen since OWL-API parsers may use the manager again, which uses factory with the same parsers
    protected final Set<IRI> sources = new HashSet<>();

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
    public OntologyFactory asOntologyFactory(OntologyCreator builder) {
        return new OntologyFactoryImpl(getAdapter().asBuilder(builder), this);
    }

    /**
     * Release the state parameters that can appear during the load.
     * @see OntologyLoaderImpl#clear()
     */
    public void clear() {
        sources.clear();
    }

    /**
     * Optimizes the given builder to interact with native OWL-API parsers.
     *
     * @param builder {@link OntologyCreator}, not {@code null}
     * @return {@link OntologyCreator}
     * @since 1.4.2
     */
    public OntologyCreator optimize(OntologyCreator builder) {
        return createLoadingBuilder(builder);
    }

    /**
     * Optimizes the given configuration to interact with native OWL-API parsers.
     * <p>
     * Impl notes:
     * Here, the nodes cache (i.e. {@link com.github.owlcs.ontapi.internal.SearchModel}) is disabled,
     * since:
     * <ul>
     * <li>1) read operations (list axioms, annotations) are not expected during assembly model</li>
     * <li>2) if these operations do occur, they may lead to exception,
     * if incomplete information has been cached
     * (for example, cache entity when there is still no declaration axiom)</li>
     * </ul>
     * Iterator cache is also disabled (just in case): no repeating axioms listing operations are expected.
     *
     * @param config {@link OntLoaderConfiguration}, not {@code null}
     * @return {@link OntLoaderConfiguration}
     * @since 1.4.2
     */
    public OntLoaderConfiguration optimize(OntLoaderConfiguration config) {
        return config.setLoadNodesCacheSize(-1).setModelCacheLevel(CacheSettings.CACHE_ITERATOR, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param builder {@link OntologyCreator} to construct a fresh {@link Ontology} instance
     * @param manager {@link OntologyManager}
     * @param source  {@link OWLOntologyDocumentSource}
     * @param conf    {@link OntLoaderConfiguration}
     * @return an ontology inside manager
     * @throws OntologyFactoryImpl.BadRecursionException if recursion occurs
     *                                                   (the situation looks like cohesion/coupling issue in OWL-API:
     *                                                   a manager uses a factory which uses the manager in its turn)
     * @throws OWLOntologyCreationException              if any other problem occurs
     */
    @Override
    public Ontology loadOntology(OntologyCreator builder,
                                 OntologyManager manager,
                                 OWLOntologyDocumentSource source,
                                 OntLoaderConfiguration conf) throws OWLOntologyCreationException {
        OWLAdapter adapter = getAdapter();
        OWLOntologyFactory factory = new FactoryImpl(builder);
        try {
            IRI doc = source.getDocumentIRI();
            if (sources.contains(doc)) {
                throw new OntologyFactoryImpl.BadRecursionException("Cycle loading for source " + doc);
            }
            sources.add(doc);
            Ontology res = getAdapter().asONT(factory.loadOWLOntology(manager, source, adapter.asHandler(manager), conf));
            if (LOGGER.isDebugEnabled()) {
                OntFormat format = OntFormat.get(manager.getOntologyFormat(res));
                LOGGER.debug("The ontology <{}> is loaded. Format: {}[{}]", res.getOntologyID(), format, source.getClass().getSimpleName());
            }
            // clear the cache to be sure that the list of axioms is always the same and corresponds to the graph.
            res.clearCache();
            return res;
        } finally {
            clear();
        }
    }

    /**
     * Creates a builder that is optimized for load operations occurring through native (OWL-API) parsers.
     *
     * @param base {@link OntologyCreator}, the builder to inherit behaviour, not {@code null}
     * @return {@link OntologyBuilderImpl}
     * @since 1.4.2
     */
    public static OntologyBuilderImpl createLoadingBuilder(OntologyCreator base) {
        Objects.requireNonNull(base);
        return new OntologyBuilderImpl() {

            public OntologyModelImpl createOntologyImpl(Graph graph,
                                                        OntologyManagerImpl manager,
                                                        OntLoaderConfiguration config) {
                return new OntologyModelImpl(wrapAsUnion(graph, config), createModelConfig(manager, config)) {

                    @Override
                    protected OWLOntologyChangeVisitorEx<ChangeApplied> createChangeProcessor() {
                        return new ChangeProcessor() {

                            @Override
                            public ChangeApplied visit(SetOntologyID change) {
                                ChangeApplied res = super.visit(change);
                                getInternalModel().forceLoad();
                                return res;
                            }

                            @Override
                            public ChangeApplied visit(AddAxiom change) {
                                return of(getInternalModel().add(change.getAxiom()));
                            }

                            @Override
                            public ChangeApplied visit(AddOntologyAnnotation change) {
                                return of(getInternalModel().add(change.getAnnotation()));
                            }

                            @Override
                            public ChangeApplied visit(RemoveAxiom change) {
                                // any remove operation is suspicious when it comes from a parser.
                                // I observe this situation only when there are grammatical mistakes in the document,
                                // so it cannot be loaded by Jena.
                                LOGGER.warn("Suspicious: {}", change);
                                return of(getInternalModel().remove(change.getAxiom()));
                            }

                            @Override
                            public ChangeApplied visit(RemoveOntologyAnnotation change) {
                                LOGGER.warn("Suspicious: {}", change);
                                return of(getInternalModel().remove(change.getAnnotation()));
                            }

                            private ChangeApplied of(boolean res) {
                                return res ? ChangeApplied.SUCCESSFULLY : ChangeApplied.NO_OPERATION;
                            }
                        };
                    }
                };
            }

            @Override
            public Graph createDataGraph() {
                return base.createDataGraph();
            }

            @Override
            public UnionGraph createUnionGraph(Graph g, OntLoaderConfiguration c) {
                return base.createUnionGraph(g, c);
            }
        };
    }

    /**
     * This is a copy-paste the original OWL-API {@link OWLOntologyFactory} implementation,
     * which is used if syntax format of source can not be handled by Apache Jena.
     * <p>
     * Matthew Horridge Stanford Center for Biomedical Informatics Research 10/04/15
     *
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl</a>
     */
    public static class FactoryImpl implements OWLOntologyFactory, HasAdapter {

        private final Builder builder;

        /**
         * Creates an OWL-API compatible factory.
         * This version cannot be used with ONT-API if the builder does not produce {@link Ontology}.
         *
         * @param builder {@link OWLOntologyBuilder}, not {@code null}
         */
        public FactoryImpl(OWLOntologyBuilder builder) {
            Objects.requireNonNull(builder);
            this.builder = (i, m, c) -> builder.createOWLOntology(m, i);
        }

        /**
         * Creates an ONT-API compatible factory.
         * Can be used both in ONT-API and OWL-API.
         * This version is used to build an alternative loader.
         *
         * @param builder {@link OntologyCreator}, not {@code null}
         */
        public FactoryImpl(OntologyCreator builder) {
            Objects.requireNonNull(builder);
            Adapter adapter = getAdapter();
            this.builder = (i, m, c) -> builder.createOntology(adapter.asONT(i), adapter.asONT(m), adapter.asONT(c));
        }

        /**
         * Selects parsers by format and MIME type of the input source, if known.
         * If format or MIME type are not known or not matched by any parser, returns all specified parsers.
         *
         * @param source  document source
         * @param parsers parsers
         * @return selected parsers
         */
        public static Iterable<OWLParserFactory> select(OWLOntologyDocumentSource source,
                                                        PriorityCollection<OWLParserFactory> parsers) {
            if (parsers.isEmpty()) {
                return parsers;
            }
            Optional<OWLDocumentFormat> format = source.getFormat();
            Optional<String> mimeType = source.getMIMEType();
            if (format.isEmpty() && mimeType.isEmpty()) {
                return parsers;
            }
            PriorityCollection<OWLParserFactory> res = parsers;
            if (format.isPresent()) {
                res = new PriorityCollection<>(PriorityCollectionSorting.NEVER);
                for (OWLParserFactory p : parsers) {
                    if (format.map(OWLDocumentFormat::getKey)
                            .filter(s -> s.equals(p.getSupportedFormat().getKey())).isEmpty()) {
                        continue;
                    }
                    res.add(p);
                }
            }
            if (mimeType.isPresent() && res.isEmpty()) {
                res = parsers.getByMIMEType(mimeType.get());
            }
            if (res.isEmpty()) {
                return parsers;
            }
            return res;
        }

        @Override
        public OWLAdapter getAdapter() {
            return OWLAdapter.get();
        }

        @Override
        public boolean canCreateFromDocumentIRI(IRI documentIRI) {
            return true;
        }

        @Override
        public boolean canAttemptLoading(OWLOntologyDocumentSource source) {
            return !source.hasAlredyFailedOnStreams() || !source.hasAlredyFailedOnIRIResolution();
        }

        @Deprecated
        @Override
        public OWLOntology createOWLOntology(OWLOntologyManager manager,
                                             OWLOntologyID id,
                                             IRI documentIRI,
                                             OWLOntologyCreationHandler handler) {
            OWLOntology res = createOWLOntology(id, manager, manager.getOntologyLoaderConfiguration());
            handler.setOntologyFormat(res, OntFormat.RDF_XML.createOwlFormat());
            return res;
        }

        public OWLOntology createOWLOntology(OWLOntologyID id,
                                             OWLOntologyManager manager,
                                             OWLOntologyLoaderConfiguration conf) {
            OWLOntology res = builder.apply(id, manager, conf);
            getAdapter().asHandler(manager).ontologyCreated(res);
            return res;
        }

        @Override
        public OWLOntology loadOWLOntology(OWLOntologyManager manager,
                                           OWLOntologyDocumentSource source,
                                           OWLOntologyCreationHandler handler,
                                           OWLOntologyLoaderConfiguration config) throws OWLOntologyCreationException {
            // Attempt to parse the ontology by looping through the parsers.
            // If the ontology is parsed successfully then we break out and return the ontology.
            // I think that this is more reliable than selecting a parser based on a file extension for example
            // (perhaps the parser list could be ordered based on most likely parser,
            // which could be determined by file extension).
            Map<OWLParser, OWLParserException> exceptions = new LinkedHashMap<>();
            // Call the super method to create the ontology - this is needed,
            // because we throw an exception if someone tries to create an ontology directly
            OWLOntology existingOntology = null;
            IRI iri = source.getDocumentIRI();
            if (manager.contains(iri)) {
                existingOntology = manager.getOntology(iri);
            }
            ID id = new ID();
            OWLOntology ont = createOWLOntology(id, manager, config);
            // Now parse the input into the empty ontology that we created select a parser
            // if the input source has format information and MIME information
            Set<String> bannedParsers = Arrays.stream(config.getBannedParsers().split(" ")).collect(Collectors.toSet());
            Iterable<OWLParserFactory> parsers = select(source, manager.getOntologyParsers());
            // use the selection of parsers to explicitly accept headers including weights
            if (source.getAcceptHeaders().isPresent()) {
                source.setAcceptHeaders(AcceptHeaderBuilder.headersFromParsers(parsers));
            }
            for (OWLParserFactory parserFactory : parsers) {
                if (bannedParsers.contains(parserFactory.getClass().getName())) {
                    continue;
                }
                OWLParser parser = parserFactory.createParser();
                try {
                    if (existingOntology == null && !ont.isEmpty()) {
                        // Junk from a previous parse. We should clear the ont
                        manager.removeOntology(ont);
                        ont = createOWLOntology(id, manager, config);
                    }
                    OWLDocumentFormat format = parser.parse(source, ont, config);
                    handler.setOntologyFormat(ont, format);
                    return ont;
                } catch (UnloadableImportException e) {
                    // If an import cannot be located, all parsers will fail.
                    // Again, terminate early
                    // First clean up
                    manager.removeOntology(ont);
                    throw e;
                } catch (OWLParserException e) {
                    if (e.getCause() instanceof IOException || e.getCause() instanceof OWLOntologyInputSourceException) {
                        // For input/output exceptions, we assume that it means
                        // the source cannot be read regardless of the parsers,
                        // so we stop early
                        // First clean up
                        manager.removeOntology(ont);
                        throw new OWLOntologyCreationIOException(e.getCause());
                    }
                    // Record this attempts and continue trying to parse.
                    exceptions.put(parser, e);
                } catch (RuntimeException e) {
                    // Clean up and rethrow
                    exceptions.put(parser, new OWLParserException(e));
                    manager.removeOntology(ont);
                    throw e;
                }
            }
            if (existingOntology == null) {
                manager.removeOntology(ont);
            }
            // We haven't found a parser that could parse the ontology properly.
            // Throw an exception whose message contains the stack traces from all the parsers that we have tried.
            throw new UnparsableOntologyException(source.getDocumentIRI(), exceptions, config);
        }

        public interface Builder {
            OWLOntology apply(OWLOntologyID id, OWLOntologyManager manager, OWLOntologyLoaderConfiguration config);
        }

    }

    /**
     * A copy-pasted OWL-API-impl utility class.
     *
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/AcceptHeaderBuilder.java'>uk.ac.manchester.cs.AcceptHeaderBuilder</a>
     */
    @SuppressWarnings("WeakerAccess")
    public static class AcceptHeaderBuilder {

        public static String headersFromParsers(Iterable<OWLParserFactory> parsers) {
            Map<String, TreeSet<Integer>> map = new HashMap<>();
            parsers.forEach(p -> addToMap(map, p.getMIMETypes()));
            return map.entrySet().stream()
                    .sorted(AcceptHeaderBuilder::compare)
                    .map(AcceptHeaderBuilder::asString)
                    .collect(Collectors.joining(", "));
        }

        private static void addToMap(Map<String, TreeSet<Integer>> map, List<String> mimes) {
            // The map will contain all mime types with their position in all lists mentioning them;
            // the smallest position first
            for (int i = 0; i < mimes.size(); i++) {
                map.computeIfAbsent(mimes.get(i), k -> new TreeSet<>()).add(i + 1);
            }
        }

        private static String asString(Map.Entry<String, TreeSet<Integer>> e) {
            return String.format("%s; q=%.1f", e.getKey(), 1D / e.getValue().first());
        }

        private static int compare(Map.Entry<String, TreeSet<Integer>> a, Map.Entry<String, TreeSet<Integer>> b) {
            return a.getValue().first().compareTo(b.getValue().first());
        }
    }
}
