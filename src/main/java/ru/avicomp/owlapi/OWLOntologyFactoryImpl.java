/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package ru.avicomp.owlapi;

import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.PriorityCollection;
import ru.avicomp.ontapi.config.OntConfig;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TODO: rename not to be confused with uk.ac.manchester.cs.owl.owlapi.*
 * This is an original {@link OWLOntologyFactory} implementation,
 * which is used if syntax format of source can not be handle by Apache Jena.
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 10/04/15
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLOntologyFactoryImpl.java'>uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl</a>
 */
@SuppressWarnings({"NullableProblems", "WeakerAccess"})
public class OWLOntologyFactoryImpl implements OWLOntologyFactory {

    private final OWLOntologyBuilder builder;

    /**
     * @param builder ontology builder
     */
    public OWLOntologyFactoryImpl(OWLOntologyBuilder builder) {
        this.builder = Objects.requireNonNull(builder);
    }

    @Override
    public boolean canCreateFromDocumentIRI(IRI documentIRI) {
        return true;
    }

    @Override
    public boolean canAttemptLoading(OWLOntologyDocumentSource source) {
        return !source.hasAlredyFailedOnStreams() ||
                !source.hasAlredyFailedOnIRIResolution() &&
                        OntConfig.DefaultScheme.all().anyMatch(s -> s.same(source.getDocumentIRI()));
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
        OWLOntologyID id = new OWLOntologyID();
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

    /**
     * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/AcceptHeaderBuilder.java'>uk.ac.manchester.cs.AcceptHeaderBuilder</a>
     */
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
