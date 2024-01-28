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

import com.github.owlcs.ontapi.config.OntConfig;
import com.github.owlcs.ontapi.config.OntLoaderConfiguration;
import com.github.owlcs.ontapi.transforms.GraphStats;
import com.github.sszuev.graphs.ReadWriteLockingGraph;
import com.github.sszuev.jena.ontapi.UnionGraph;
import com.github.sszuev.jena.ontapi.impl.UnionGraphImpl;
import com.github.sszuev.jena.ontapi.model.OntID;
import com.github.sszuev.jena.ontapi.model.OntModel;
import com.github.sszuev.jena.ontapi.utils.Graphs;
import com.github.sszuev.jena.ontapi.utils.Iterators;
import com.github.sszuev.jena.ontapi.vocabulary.OWL;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Node_Blank;
import org.apache.jena.graph.Node_Literal;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.shared.JenaException;
import org.apache.jena.shared.PrefixMapping;
import org.semanticweb.owlapi.io.DocumentSources;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.io.OWLOntologyInputSourceException;
import org.semanticweb.owlapi.io.OWLOntologyLoaderMetaData;
import org.semanticweb.owlapi.io.RDFLiteral;
import org.semanticweb.owlapi.io.RDFNode;
import org.semanticweb.owlapi.io.RDFResource;
import org.semanticweb.owlapi.io.RDFResourceBlankNode;
import org.semanticweb.owlapi.io.RDFResourceIRI;
import org.semanticweb.owlapi.io.RDFTriple;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.owlcs.ontapi.OntologyFactoryImpl.ConfigMismatchException;
import static com.github.owlcs.ontapi.OntologyFactoryImpl.UnsupportedFormatException;

/**
 * Helper to work with {@link Graph Apache Jena Graph}s in OWL-API terms.
 * Used in different ONT-API components related to the OWL-API-api implementation.
 * Some methods were moved from other classes (e.g., from {@link OntologyFactoryImpl})
 * and can refer to another class namespace
 * (e.g., can throw exceptions defined as nested static in some external classes).
 * <p>
 * Created by @ssz on 19.08.2017.
 *
 * @see Graphs
 * @see DocumentSources
 * @see RDFDataMgr
 * @since 1.0.1
 */
@SuppressWarnings("WeakerAccess")
public class OntGraphUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntGraphUtils.class);

    // following constants are copy-pasted from org.semanticweb.owlapi.io.DocumentSource:
    public static final String TEXT_PLAIN_REQUEST_TYPE = ", text/plain; q=0.1";
    public static final String LAST_REQUEST_TYPE = ", */*; q=0.09";
    public static final String DEFAULT_REQUEST = "application/rdf+xml, application/xml; q=0.7, text/xml; q=0.6" +
            TEXT_PLAIN_REQUEST_TYPE + LAST_REQUEST_TYPE;

    /**
     * Gets an OWL Ontology ID parsed from the given graph.
     * The method treats graphs without {@code owl:Ontology} section inside as anonymous.
     * Each method's call should return the same value for the same graph.
     *
     * @param graph {@link Graph}, not {@code null}
     * @return {@link ID}, not {@code null}
     * @throws OntApiException in case it is an anonymous graph but with version iri
     */
    public static ID getOrCreateOntologyID(Graph graph) throws OntApiException {
        Graph base = Graphs.getPrimary(graph);
        Node res = Graphs.ontologyNode(base, true)
                .orElseGet(() ->
                        Graphs.makeOntologyHeaderNode(base, NodeFactory.createBlankNode(toString(base)))
                );
        String versionIri = Graphs.findVersionIRI(base, res).map(Node::getURI).orElse(null);
        return new ID(res, versionIri);
    }

    /**
     * Gets Ontology URI from the base graph or returns {@code null}
     * if there is no {@code owl:Ontology} or it is anonymous ontology.
     *
     * @param graph {@link Graph}
     * @return String uri or {@code null}
     * @see Graphs#getImports(Graph)
     */
    public static String getOntologyIRIOrNull(Graph graph) {
        Graph base = Graphs.getPrimary(graph);
        return Graphs.ontologyNode(base, true).filter(Node::isURI).map(Node::getURI).orElse(null);
    }

    /**
     * Builds a map form the ontology graph with {@link ID}s as keys and component {@link Graph Graph}s as values.
     * <p>
     * If the graph has no import declarations
     * (i.e., no statements {@code _:x owl:imports uri}) then this graph is put into the map as is.
     * If it is a composite graph with imports,
     * the base graph will be unwrapped using method {@link Graphs#getPrimary(Graph)},
     * i.e., not a graph itself will go as a value, but its base sub-graph.
     * If the input graph is composite, it should consist of named graphs; only the root (top-level primary graph)
     * is allowed to be anonymous.
     * Also, the graph-tree should not contain different children but with the same iri (i.e., {@code owl:Ontology} uri).
     * To check the equivalence of two graphs, the method {@link Graph#isIsomorphicWith(Graph)} is used.
     *
     * @param graph {@link Graph graph}, not {@code null}
     * @return Map with {@link ID OWL Ontology ID} as a key and {@link Graph graph} as a value
     * @throws OntApiException in case of violation of the restrictions described above
     */
    public static Map<ID, Graph> toGraphMap(Graph graph) throws OntApiException {
        Deque<Graph> queue = new ArrayDeque<>();
        queue.add(graph);
        Map<ID, Graph> res = new LinkedHashMap<>();
        Set<ID> seen = new HashSet<>();
        Map<String, Graph> named = ontGraphs(graph).map(it -> {
            String name = Graphs.findOntologyNameNode(Graphs.getPrimary(it), true)
                    .filter(Node::isURI)
                    .map(Node::getURI)
                    .orElse(null);
            if (name == null) {
                return null;
            }
            return Map.entry(name, it);
        }).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        while (!queue.isEmpty()) {
            Graph next = queue.removeFirst();
            Graph base = getPrimary(next);
            Optional<Node> header = Graphs.ontologyNode(base, true);
            if (header.isEmpty()) {
                continue;
            }
            String versionIri = Graphs.findVersionIRI(base, header.get()).map(Node::getURI).orElse(null);
            ID id = new ID(header.get(), versionIri);
            if (!seen.add(id)) {
                continue;
            }
            Graph prev = res.get(id);
            if (prev != null) {
                if (!prev.isIsomorphicWith(graph)) {
                    throw new OntApiException("Duplicate sub graph: " + id);
                }
                continue;
            } else {
                res.put(id, base);
            }
            Set<String> imports = Graphs.getImports(base, true);
            if (imports.isEmpty()) {
                continue;
            }
            named.forEach((ig, g) -> {
                if (imports.contains(ig)) {
                    queue.add(g);
                }
            });
        }
        if (res.isEmpty()) {
            throw new OntApiException.IllegalArgument("Unable to parse ontology tree");
        }
        return res;
    }

    public static Stream<Graph> ontGraphs(Graph graph) {
        return Graphs.flatTree(graph, OntGraphUtils::getPrimary, OntGraphUtils::directSubGraphs);
    }

    public static Graph getPrimary(Graph it) {
        if (Graphs.isWrapper(it) && hasImports(it)) {
            return Graphs.getPrimary(it);
        } else {
            return it;
        }
    }

    public static Stream<Graph> directSubGraphs(Graph it) {
        if (Graphs.isWrapper(it) && hasImports(it)) {
            return Graphs.directSubGraphs(it);
        } else {
            return Stream.empty();
        }
    }

    public static boolean hasImports(Graph it) {
        return Iterators.findFirst(Graphs.listImports(Graphs.getPrimary(it), true)).isPresent();
    }

    /**
     * Converts OWL-API prefixes to Jena.
     *
     * @param pm {@link PrefixManager OWL-API PrefixManager}
     * @return {@link PrefixMapping Jena PrefixMapping}
     */
    public static PrefixMapping prefixMapping(PrefixManager pm) {
        PrefixMapping res = PrefixMapping.Factory.create();
        setNsPrefixes(res, pm.getPrefixName2PrefixMap());
        return res;
    }

    /**
     * Converts a {@link Triple Jena Triple} to {@link RDFTriple OWL-API RDFTriple}.
     *
     * @param triple not {@code null}
     * @return RDFTriple
     */
    public static RDFTriple triple(Triple triple) {
        RDFResource subject;
        if (triple.getSubject().isURI()) {
            subject = uri(triple.getSubject());
        } else {
            subject = blank(triple.getSubject());
        }
        RDFResourceIRI predicate = uri(triple.getPredicate());
        RDFNode object;
        if (triple.getObject().isURI()) {
            object = uri(triple.getObject());
        } else if (triple.getObject().isLiteral()) {
            object = literal(triple.getObject());
        } else {
            object = blank(triple.getObject());
        }
        return new RDFTriple(subject, predicate, object);
    }

    /**
     * Converts a {@link Node Jena Node} to {@link RDFResourceBlankNode OWL-API node object},
     * which pretends to be a blank node.
     *
     * @param node not null, must be {@link Node_Blank}
     * @return {@link RDFResourceBlankNode} with all flags set to {@code false}
     * @throws IllegalArgumentException in case the specified node is not blank
     */
    public static RDFResourceBlankNode blank(Node node) throws IllegalArgumentException {
        if (!Objects.requireNonNull(node, "Null node").isBlank())
            throw new IllegalArgumentException("Not a blank node: " + node);
        return new RDFResourceBlankNode(IRI.create(node.getBlankNodeId().getLabelString()), false, false, false);
    }

    /**
     * Converts a {@link Node Jena Node} to {@link RDFResourceIRI OWL-API IRI RDF-Node}.
     *
     * @param node not null, must be {@link Node_URI}
     * @return {@link RDFResourceIRI}
     * @throws IllegalArgumentException in case the specified node is not blank
     */
    public static RDFResourceIRI uri(Node node) throws IllegalArgumentException {
        if (!Objects.requireNonNull(node, "Null node").isURI())
            throw new IllegalArgumentException("Not an uri node: " + node);
        return new RDFResourceIRI(IRI.create(node.getURI()));
    }

    /**
     * Converts a {@link Node Jena Node} to {@link RDFLiteral OWL-API Literal RDF-Node}.
     *
     * @param node not null, must be {@link Node_Literal}
     * @return {@link RDFResourceIRI}
     * @throws IllegalArgumentException in case the specified node is not literal
     */
    public static RDFLiteral literal(Node node) throws IllegalArgumentException {
        if (!Objects.requireNonNull(node, "Null node").isLiteral())
            throw new IllegalArgumentException("Not a literal node: " + node);
        return new RDFLiteral(node.getLiteralLexicalForm(), node.getLiteralLanguage(),
                IRI.create(node.getLiteralDatatypeURI()));
    }

    /**
     * Auxiliary method to produce {@link OWLOntologyLoaderMetaData} object.
     *
     * @param graph {@link Graph}
     * @param stats {@link GraphStats} transformation outcome, can be null for fake meta-data
     * @return {@link OWLOntologyLoaderMetaData} object
     * @throws IllegalArgumentException in case {@code graph} and {@code stats} are incompatible
     */
    protected static OWLOntologyLoaderMetaData makeParserMetaData(Graph graph, GraphStats stats) {
        if (stats == null)
            return OntologyMetaData.createParserMetaData(graph);
        if (Graphs.getPrimary(graph) != stats.getGraph())
            throw new IllegalArgumentException("Incompatible graphs: " +
                    getOntologyGraphPrintName(graph) + " != " + getOntologyGraphPrintName(stats.getGraph()));
        return OntologyMetaData.createParserMetaData(stats);
    }

    /**
     * The main method to read the source document into the graph.
     * The method is public for more generality.
     *
     * @param graph  {@link Graph} the graph(empty) to put in
     * @param source {@link OWLOntologyDocumentSource} the source
     *               (encapsulates IO-stream,
     *               IO-Reader or IRI of the document)
     * @param conf   {@link OntLoaderConfiguration} config
     * @return {@link OntFormat} corresponding to the specified source
     * @throws UnsupportedFormatException   if the source can't be read into graph using jena.
     * @throws ConfigMismatchException      if there is some conflict with config settings,
     *                                      anyway, we can't continue
     * @throws OWLOntologyCreationException if there is some serious IO problem
     * @throws OntApiException              if some other problem
     */
    public static OntFormat readGraph(Graph graph,
                                      OWLOntologyDocumentSource source,
                                      OntLoaderConfiguration conf) throws OWLOntologyCreationException {
        IRI iri = OntApiException.notNull(source, "Null document source.").getDocumentIRI();
        final OWLOntologyCreationException error = new UnsupportedFormatException(String.format("Can't read %s %s.",
                source.getClass().getSimpleName(), iri));
        for (OntFormat format : getSupportedFormats(source)) {
            if (format.isOWLOnly()) {
                error.addSuppressed(new UnsupportedFormatException("Not supported by jena.")
                        .putFormat(format).putSource(iri));
                continue;
            }
            Lang lang = format.getLang();
            try (Closeable stream = openInputStream(source, conf)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("read {}, try <{}>", iri, lang);
                }
                readGraph(graph, stream, iri.toString(), lang);
                return format;
            } catch (OWLOntologyInputSourceException | IOException e) {
                throw new OWLOntologyCreationException(source.getClass().getSimpleName() +
                        ": can't open or close input stream from " + iri, e);
            } catch (RuntimeException e) {
                // could be org.apache.jena.shared.JenaException ||
                // org.apache.jena.atlas.AtlasException ||
                // org.apache.jena.atlas.json.JsonParseException || ...
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("<{}> failed: '{}'", lang, e.getMessage());
                }
                error.addSuppressed(new UnsupportedFormatException(e).putSource(iri).putFormat(format));
            }
        }
        throw error;
    }

    protected static void readGraph(Graph graph, Closeable stream, String base, Lang lang) {
        if (stream instanceof InputStream) {
            RDFDataMgr.read(graph, (InputStream) stream, base, lang);
        } else {
            //Jena discourages the use of Readers in favor of InputStreams (can't work with non-UTF8 encodings)
            //noinspection deprecation <- we take that risk, assuming that the source provider knows what he is doing
            RDFDataMgr.read(graph, (Reader) stream, base, lang);
        }
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
     * Tries to compute the {@link OntFormat ONT-Format} from the specified
     * {@link OWLOntologyDocumentSource OWL Document Source} by using the content type or uri
     * or whatever else, but not encapsulated OWL-format (which may absent).
     * The method is public for more generality.
     *
     * @param source {@link OWLOntologyDocumentSource}
     * @return {@link OntFormat} or null if it could not guess a format from source
     */
    public static OntFormat guessFormat(OWLOntologyDocumentSource source) {
        Lang lang;
        Optional<String> mime;
        if ((mime = OntApiException.notNull(source, "Null document source.").getMIMEType()).isPresent()) {
            lang = RDFLanguages.contentTypeToLang(mime.get());
        } else {
            lang = RDFLanguages.filenameToLang(source.getDocumentIRI().getIRIString());
        }
        return lang == null ? null : OntFormat.get(lang);
    }

    /**
     * Opens the input stream for the specified {@code source} taking settings from {@code conf} if needed.
     *
     * @param source {@link OWLOntologyDocumentSource}
     * @param conf   {@link OntLoaderConfiguration}
     * @return {@link InputStream} or {@link Reader}, never {@code null}
     * @throws OWLOntologyInputSourceException can't open or read a source
     * @throws ConfigMismatchException         if the scheme is not allowed
     */
    protected static Closeable openInputStream(OWLOntologyDocumentSource source,
                                               OntLoaderConfiguration conf) throws OWLOntologyInputSourceException, ConfigMismatchException {
        InputStream in = source.getInputStream().orElse(null);
        if (in != null) {
            return in;
        }
        Reader rd = source.getReader().orElse(null);
        if (rd != null) {
            return rd;
        }
        IRI iri = source.getDocumentIRI();
        if (conf.getSupportedSchemes().stream().noneMatch(s -> s.same(iri))) {
            throw new ConfigMismatchException("Not allowed scheme: " + iri);
        }
        // OWLAPI methods call:
        String header = source.getAcceptHeaders().orElse(DEFAULT_REQUEST);
        return DocumentSources.getInputStream(iri, conf, header)
                .orElseThrow(() -> new OWLOntologyInputSourceException("Can't get input-stream from " + iri));
    }

    /**
     * Writes the given {@code graph} to the {@code target} in the default serialization for {@code lang}.
     * <b>Note: the method calls {@link Writer#close()} produced by {@link OWLOntologyDocumentTarget#getWriter()},
     * but DOES not call {@link OutputStream#close()} produced by {@link OWLOntologyDocumentTarget#getOutputStream()}</b>.
     *
     * @param graph  {@link Graph}, not {@code null}
     * @param lang   {@link Lang}}, not {@code null}
     * @param target {@link OWLOntologyDocumentTarget}}, not empty, not {@code null}
     * @throws OWLOntologyStorageException in case of any error
     */
    @SuppressWarnings("deprecation") // for RDFDataMgr#write
    public static void writeGraph(Graph graph, Lang lang, OWLOntologyDocumentTarget target) throws OWLOntologyStorageException {
        String name = getOntologyGraphPrintName(graph);
        try {
            OutputStream outputStreamFromTarget = target.getOutputStream().orElse(null);
            if (outputStreamFromTarget != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Save {} to the output stream in the default serialization for {}", name, lang);
                }
                RDFDataMgr.write(outputStreamFromTarget, graph, lang);
                return;
            }
        } catch (JenaException ex) {
            throw new OWLOntologyStorageException(String.format("Exception while writing %s to OutputStream; format=%s", name, lang), ex);
        }
        try (Writer writerFromTarget = target.getWriter().orElse(null)) {
            if (writerFromTarget != null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Save {} to the writer in the default serialization for {}", name, lang);
                }
                //Using Java Writers risks corruption because of mismatch of a character set.
                // Only UTF-8 is safe.
                //No inspection deprecation <- we take the risk, assuming that the provider knows what he is doing
                RDFDataMgr.write(writerFromTarget, graph, lang);
                return;
            }
        } catch (JenaException | IOException ex) {
            throw new OWLOntologyStorageException(String.format("Exception while writing %s to Writer; format=%s", name, lang), ex);
        }
        IRI iri = target.getDocumentIRI().orElse(null);
        if (iri == null) {
            throw new IllegalArgumentException("Broken document target specified: no Writer, no InputStream, no IRI");
        }
        try (OutputStream outputStreamFromIri = openOutputStream(iri)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Save {} to the {} in the default serialization for {}", name, iri, lang);
            }
            RDFDataMgr.write(outputStreamFromIri, graph, lang);
        } catch (JenaException | IOException ex) {
            throw new OWLOntologyStorageException(String.format("Exception while writing %s to %s; format=%s", name, iri, lang), ex);
        }
    }

    /**
     * Opens the output stream for the specified {@code IRI}.
     *
     * @param iri {@link IRI}
     * @return {@link OutputStream}
     * @throws IOException in case can't open stream
     */
    protected static OutputStream openOutputStream(IRI iri) throws IOException {
        if (OntConfig.DefaultScheme.FILE.same(iri)) {
            Path file = Paths.get(iri.toURI());
            Files.createDirectories(file.getParent());
            return Files.newOutputStream(file);
        }
        URL url = iri.toURI().toURL();
        URLConnection conn = url.openConnection();
        return conn.getOutputStream();
    }

    /**
     * Returns the string representation of the object.
     * Each call of this method for the same object produces the same string.
     * Equivalent to {@link Object#toString()}.
     * Placed here as a temporary solution
     * (currently there is no more suitable place in the project for such misc things).
     *
     * @param o anything
     * @return String
     */
    public static String toString(Object o) {
        if (o == null) return "null";
        return o.getClass().getName() + "@" + Integer.toHexString(o.hashCode());
    }

    /**
     * Inserts the given ontology in the dependencies of each ontology from the specified collection,
     * provided as {@code Supplier} (the {@code manager} parameter).
     * Can be used to fix missed graph links or
     * to replace existing dependency with the new one in case {@code replace} is {@code true}.
     *
     * @param manager the collection of other ontologies in form of {@link Supplier} that answers a {@code Stream}
     * @param ont     {@link OntModel} the ontology to insert, must be named
     * @param replace if {@code true} then any existing graph,
     *                that is linked through the {@code owl:import} declaration,
     *                will be replaced with the given graph,
     *                otherwise the graph will be inserted only if
     *                there is a declaration {@code owl:import} without any graph associated
     * @see OntID#getImportsIRI()
     */
    public static void insert(Supplier<Stream<OntModel>> manager, OntModel ont, boolean replace) {
        String uri = Objects.requireNonNull(ont.getID().getImportsIRI(), "Must be named ontology");
        manager.get()
                .filter(m -> {
                    // select only those, that have the uri in owl:imports:
                    try (Stream<String> uris = m.getID().imports()) {
                        return uris.anyMatch(uri::equals);
                    }
                })
                .peek(m -> {
                    if (!replace) return;
                    // remove a first found previously associated graph:
                    m.imports()
                            .filter(i -> uri.equals(i.getID().getImportsIRI()))
                            .findFirst()
                            .ifPresent(i -> ((UnionGraph) m.getGraph()).removeSubGraph(i.getGraph()));
                })
                .filter(m -> m.imports().map(OntModel::getID).map(OntID::getImportsIRI).noneMatch(uri::equals))
                .forEach(m -> m.addImport(ont));
    }

    /**
     * Replaces namespaces map with new one.
     *
     * @param mapping  {@link PrefixMapping Prefix Mapping} to modify
     * @param prefixes java Map of new prefixes to set
     * @return a {@code Map} of previously associated prefixes
     */
    public static Map<String, String> setNsPrefixes(PrefixMapping mapping, Map<String, String> prefixes) {
        Map<String, String> init = mapping.getNsPrefixMap();
        init.keySet().forEach(mapping::removeNsPrefix);
        prefixes.forEach((p, u) -> mapping.setNsPrefix(p.replaceAll(":$", ""), u));
        return init;
    }

    /**
     * Wraps the specified graph as {@link UnionGraph} if it is not yet.
     *
     * @param graph {@link Graph}, not {@code null}
     * @return {@link UnionGraph}
     */
    public static UnionGraph asUnionGraph(Graph graph) {
        return graph instanceof UnionGraph ? (UnionGraph) graph : new UnionGraphImpl(graph, true);
    }

    /**
     * Makes a concurrent version of the given {@code Graph} by wrapping it as {@link ReadWriteLockingGraph}.
     * If the input is an {@code UnionGraph},
     * it makes an {@code UnionGraph} where only the base (primary) contains the specified R/W lock.
     * The result graph has the same structure as specified.
     *
     * @param graph {@link Graph}, not {@code null}
     * @param lock  {@link ReadWriteLock}, not {@code null}
     * @return {@link Graph} with {@link ReadWriteLock}
     */
    public static Graph asConcurrent(Graph graph, ReadWriteLock lock) {
        if (!(graph instanceof UnionGraph)) {
            Graph nonConcurrent = graph instanceof ReadWriteLockingGraph ? ((ReadWriteLockingGraph) graph).get() : graph;
            return new ReadWriteLockingGraph(nonConcurrent, lock);
        }
        boolean distinct = ((UnionGraph) graph).isDistinct();
        Graph base = Graphs.getPrimary(graph);
        Graph concurrentBase = new ReadWriteLockingGraph(
                base instanceof ReadWriteLockingGraph ? ((ReadWriteLockingGraph) base).get() : graph,
                lock);
        return Graphs.makeOntUnion(
                concurrentBase,
                Graphs.dataGraphs(graph).collect(Collectors.toSet()),
                it -> new UnionGraphImpl(it, distinct)
        );
    }

    /**
     * Removes concurrency from the given graph.
     * This operation is opposite to the {@link #asConcurrent(Graph, ReadWriteLock)} method:
     * if the input is an {@code UnionGraph}
     * it makes an {@code UnionGraph} with the same structure as specified but without R/W lock.
     *
     * @param graph {@link Graph}
     * @return {@link Graph}
     */
    public static Graph asNonConcurrent(Graph graph) {
        if (!(graph instanceof UnionGraph)) {
            return graph instanceof ReadWriteLockingGraph ? ((ReadWriteLockingGraph) graph).get() : graph;
        }
        boolean distinct = ((UnionGraph) graph).isDistinct();
        return Graphs.makeOntUnionFrom(graph, it -> {
            Graph g = it instanceof ReadWriteLockingGraph ? ((ReadWriteLockingGraph) it).get() : it;
            return new UnionGraphImpl(g, distinct);
        });
    }

    /**
     * Gets the "name" of the base graph: uri, blank-node-id as string or null string if there is no ontology at all.
     * The version IRI info is also included if it is present in the graph for the found ontology node.
     *
     * @param graph {@link Graph}
     * @return String (uri or blank-node label) or {@code null}
     */
    public static String getOntologyGraphPrintName(Graph graph) {
        if (graph.isClosed()) {
            return "(closed)";
        }
        Optional<Node> id = Graphs.ontologyNode(Graphs.getPrimary(graph));
        if (id.isEmpty()) {
            return "UnnamedOntology";
        }
        Set<String> versions = Iterators.takeAsSet(graph.find(id.get(), OWL.versionIRI.asNode(), Node.ANY)
                .mapWith(Triple::getObject).mapWith(Node::toString), 2);
        if (versions.size() != 1) {
            return String.format("<%s>", id.get());
        }
        return String.format("<%s>::<%s>", id.get(), versions.iterator().next());
    }
}
