/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
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

import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.PriorityCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * An implementation of {@link OntologyFactory.Loader} which is actually {@link OWLOntologyFactory} decorator.
 * Used to load {@link OntologyModel ontology} through pure OWL-API mechanisms (i.e. owl-api parsers).
 * Some formats (such as {@link org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat} or
 * {@link org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat}) are not supported by jena,
 * so it is the only way to handle them by ONT-API.
 */
@SuppressWarnings("WeakerAccess")
public class OWLLoaderImpl implements OntologyFactory.Loader {
    private static final Logger LOGGER = LoggerFactory.getLogger(OWLLoaderImpl.class);

    protected final OWLOntologyFactory factory;

    // a set to avoid recursion loop,
    // which may happen since OWL-API parsers may use the manager again, which uses factory with the same parsers
    protected Set<IRI> sources = new HashSet<>();

    /**
     * Constructs a fresh OWL-API based Loader using the given Builder.
     *
     * @param builder {@link OntologyFactory.Builder}, not null
     */
    public OWLLoaderImpl(OntologyFactory.Builder builder) {
        this(new FactoryImpl(Objects.requireNonNull(builder, "Null ONT Builder specified.")));
    }

    /**
     * The main constructor.
     *
     * @param factory {@link OWLOntologyFactory}, not null
     */
    protected OWLLoaderImpl(OWLOntologyFactory factory) {
        this.factory = OntApiException.notNull(factory, "Null OWL Ontology Factory impl.");
    }

    /**
     * @param source  {@link OWLOntologyDocumentSource}
     * @param manager {@link OntologyManager}
     * @param conf    {@link OntLoaderConfiguration}
     * @return an ontology inside manager
     * @throws OntologyFactoryImpl.BadRecursionException if recursion occurs
     *                                                   (the situation looks like cohesion/coupling issue in OWL-API:
     *                                                   a manager uses a factory which uses the manager in its turn)
     * @throws OWLOntologyCreationException              if any other problem occurs
     */
    @Override
    public OntologyModel load(OWLOntologyDocumentSource source, OntologyManager manager, OntLoaderConfiguration conf)
            throws OWLOntologyCreationException {
        IRI doc = source.getDocumentIRI();
        if (sources.contains(doc)) {
            throw new OntologyFactoryImpl.BadRecursionException("Cycle loading for source " + doc);
        }
        sources.add(doc);
        OntologyModel res = (OntologyModel) factory.loadOWLOntology(manager, source, (OWLOntologyFactory.OWLOntologyCreationHandler) manager, conf);
        sources.clear();
        if (LOGGER.isDebugEnabled()) {
            OntFormat format = OntFormat.get(manager.getOntologyFormat(res));
            LOGGER.debug("The ontology <{}> is loaded. Format: {}[{}]", res.getOntologyID(), format, source.getClass().getSimpleName());
        }
        // clear cache to be sure that list of axioms is always the same and corresponds to the graph.
        res.clearCache();
        return res;
    }

    /**
     * This is a copy-paste the original OWL-API {@link OWLOntologyFactory} implementation,
     * which is used if syntax format of source can not be handle by Apache Jena.
     * Matthew Horridge Stanford Center for Biomedical Informatics Research 10/04/15
     *
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl</a>
     */
    @SuppressWarnings({"NullableProblems"})
    public static class FactoryImpl implements OWLOntologyFactory {

        private final OWLOntologyBuilder builder;

        /**
         * @param builder ontology builder
         */
        public FactoryImpl(OWLOntologyBuilder builder) {
            this.builder = Objects.requireNonNull(builder);
        }

        @Override
        public boolean canCreateFromDocumentIRI(IRI documentIRI) {
            return true;
        }

        @Override
        public boolean canAttemptLoading(OWLOntologyDocumentSource source) {
            return !source.hasAlredyFailedOnStreams() || !source.hasAlredyFailedOnIRIResolution();
        }

        @Override
        public OWLOntology createOWLOntology(OWLOntologyManager manager, OWLOntologyID ontologyID, IRI documentIRI, OWLOntologyCreationHandler handler) {
            OWLOntology ont = builder.createOWLOntology(manager, ontologyID);
            handler.ontologyCreated(ont);
            handler.setOntologyFormat(ont, new RDFXMLDocumentFormat());
            return ont;
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
            OntologyID id = new OntologyID();
            OWLOntology ont = createOWLOntology(manager, id, source.getDocumentIRI(), handler);
            // Now parse the input into the empty ontology that we created select a parser
            // if the input source has format information and MIME information
            Set<String> bannedParsers = Arrays.stream(config.getBannedParsers().split(" ")).collect(Collectors.toSet());
            Iterable<OWLParserFactory> parsers = select(source, manager.getOntologyParsers());
            // use the selection of parsers to set the accept headers explicitly, including weights
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
                        ont = createOWLOntology(manager, id, source.getDocumentIRI(), handler);
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
                        // so we stop early/
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
            // Throw an exception whose message contains the stack traces from all of the parsers that we have tried.
            throw new UnparsableOntologyException(source.getDocumentIRI(), exceptions, config);
        }

        /**
         * Selects parsers by format and MIME type of the input source, if known.
         * If format or MIME type are not known or not matched by any parser, returns all specified parsers.
         *
         * @param source  document source
         * @param parsers parsers
         * @return selected parsers
         */
        public static Iterable<OWLParserFactory> select(OWLOntologyDocumentSource source, PriorityCollection<OWLParserFactory> parsers) {
            if (parsers.isEmpty()) {
                return parsers;
            }
            Optional<OWLDocumentFormat> format = source.getFormat();
            Optional<String> mimeType = source.getMIMEType();
            if (!format.isPresent() && !mimeType.isPresent()) {
                return parsers;
            }
            PriorityCollection<OWLParserFactory> res = parsers;
            if (format.isPresent()) {
                res = new PriorityCollection<>(PriorityCollectionSorting.NEVER);
                for (OWLParserFactory p : parsers) {
                    if (!format.map(OWLDocumentFormat::getKey)
                            .filter(s -> s.equals(p.getSupportedFormat().getKey())).isPresent()) {
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

    }

    /**
     * A copy-pasted OWL-API-impl utility class.
     *
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/AcceptHeaderBuilder.java'>uk.ac.manchester.cs.AcceptHeaderBuilder</a>
     */
    public static class AcceptHeaderBuilder {

        public static String headersFromParsers(Iterable<OWLParserFactory> parsers) {
            Map<String, TreeSet<Integer>> map = new HashMap<>();
            parsers.forEach(p -> addToMap(map, p.getMIMETypes()));
            return map.entrySet().stream()
                    .sorted(OWLLoaderImpl.AcceptHeaderBuilder::compare)
                    .map(OWLLoaderImpl.AcceptHeaderBuilder::asString)
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
