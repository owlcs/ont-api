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

package ru.avicomp.ontapi;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.jena.graph.*;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.jena.impl.OntIDImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntID;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.transforms.GraphTransformers;

import javax.annotation.Nonnull;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Helper to work with {@link Graph Apache Jena Graph}s in OWL-API terms.
 * Used in different ONT-API components related to the OWL-API-api implementation.
 * Some of the methods were moved from another classes (e.g. from {@link OntologyFactoryImpl})
 * and can refer to another class namespace (e.g. can throw exceptions defined as nested static in some external classes).
 * <p>
 * Created by @szuev on 19.08.2017.
 *
 * @see Graphs
 * @see DocumentSources
 * @see RDFDataMgr
 * @since 1.0.1
 */
@SuppressWarnings("WeakerAccess")
public class OntGraphUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntGraphModel.class);

    // following constants are copy-pasted from org.semanticweb.owlapi.io.DocumentSource:
    public static final String TEXT_PLAIN_REQUEST_TYPE = ", text/plain; q=0.1";
    public static final String LAST_REQUEST_TYPE = ", */*; q=0.09";
    public static final String DEFAULT_REQUEST = "application/rdf+xml, application/xml; q=0.7, text/xml; q=0.6" +
            TEXT_PLAIN_REQUEST_TYPE + LAST_REQUEST_TYPE;

    /**
     * Returns OWL Ontology ID from ontology-graph
     *
     * @param graph {@link Graph graph}
     * @return Optional around {@link OWLOntologyID OWL Ontology ID} or
     * {@code Optional.empty} for anonymous ontology graph or for graph without {@code owl:Ontology} section
     * @deprecated use {@link OntGraphUtils#getOntologyID(Graph)}, this will be removed
     */
    @Deprecated
    public static Optional<OWLOntologyID> ontologyID(@Nonnull Graph graph) {
        Graph base = Graphs.getBase(graph);
        return Graphs.ontologyNode(base)
                .map(n -> new OntIDImpl(n, new ModelCom(base)))
                .map(id -> {
                    Optional<IRI> iri = Optional.ofNullable(id.getURI()).map(IRI::create);
                    Optional<IRI> ver = Optional.ofNullable(id.getVersionIRI()).map(IRI::create);
                    return new OWLOntologyID(iri, ver);
                }).filter(id -> !id.isAnonymous());
    }

    /**
     * Gets an OWL Ontology ID parsed from the given graph.
     * Treats graphs without {@code owl:Ontology} section inside as anonymous.
     *
     * @param graph {@link Graph}
     * @return {@link OWLOntologyID}, not null
     * @throws OntApiException in case it is an anonymous graph but with version iri
     */
    public static OWLOntologyID getOntologyID(@Nonnull Graph graph) throws OntApiException {
        Graph base = Graphs.getBase(graph);
        Optional<Node> node = Graphs.ontologyNode(base);
        if (!node.isPresent()) { // treat graphs without owl:Ontology as anonymous
            return new OWLOntologyID();
        }
        OntID id = new OntIDImpl(node.get(), new ModelCom(base));
        Optional<IRI> iri = Optional.ofNullable(id.getURI()).map(IRI::create);
        Optional<IRI> ver = Optional.ofNullable(id.getVersionIRI()).map(IRI::create);
        if (!iri.isPresent() && ver.isPresent())
            throw new OntApiException("Anonymous graph with version iri");
        return new OWLOntologyID(iri, ver);
    }

    /**
     * Builds map form the ontology graph.
     * If the specified graph is not composite then only one key in the map is expected.
     * The specified graph should consist of named graphs, only the root is allowed to be anonymous.
     * Also the graph-tree should not contain different children but with the same name (owl:Ontology uri).
     *
     * @param graph {@link Graph graph}
     * @return Map with {@link OWLOntologyID OWL Ontology ID} as a key and {@link Graph graph} as a value
     * @throws OntApiException the input graph has restrictions, see above.
     */
    public static Map<OWLOntologyID, Graph> toGraphMap(@Nonnull Graph graph) throws OntApiException {
        Graph base = Graphs.getBase(graph);
        return Graphs.flat(graph)
                .collect(Collectors.toMap(
                        g -> {
                            OWLOntologyID id = getOntologyID(g);
                            if (id.isAnonymous() && base != g) {
                                throw new OntApiException("Anonymous sub graph found: " + id +
                                        ". Only top-level graph is allowed to be anonymous");
                            }
                            return id;
                        }
                        , Function.identity()
                        , (a, b) -> {
                            if (a.isIsomorphicWith(b)) {
                                return a;
                            }
                            throw new OntApiException("Duplicate sub graphs " + Graphs.getName(a));
                        }
                        , LinkedHashMap::new));
    }

    /**
     * Converts a {@link Triple Jena Triple} to {@link RDFTriple OWL-API RDFTriple}.
     *
     * @param triple not null
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
     * Converts a {@link Node Jena Node} to {@link RDFResourceBlankNode OWL-API node object}, which pretends to be a blank node.
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
        return new RDFLiteral(node.getLiteralLexicalForm(), node.getLiteralLanguage(), IRI.create(node.getLiteralDatatypeURI()));
    }

    /**
     * Auxiliary method to produce {@link OWLOntologyLoaderMetaData} object.
     *
     * @param graph {@link Graph}
     * @param stats {@link GraphTransformers.Stats} transformation outcome, can be null for fake meta-data
     * @return {@link OWLOntologyLoaderMetaData} object
     * @throws IllegalArgumentException in case {@code graph} and {@code stats} are incompatible
     */
    protected static OWLOntologyLoaderMetaData makeParserMetaData(Graph graph, GraphTransformers.Stats stats) {
        if (stats == null)
            return OntologyMetaData.createParserMetaData(graph);
        if (Graphs.getBase(graph) != stats.getGraph())
            throw new IllegalArgumentException("Incompatible graphs: " + Graphs.getName(graph) + " != " + Graphs.getName(stats.getGraph()));
        return OntologyMetaData.createParserMetaData(stats);
    }

    /**
     * The main method to read the source document to the graph.
     * For generality it is public.
     *
     * @param graph  {@link Graph} the graph(empty) to put in.
     * @param source {@link OWLOntologyDocumentSource} the source (encapsulates IO-stream, IO-Reader or IRI of document)
     * @param conf   {@link OntLoaderConfiguration} config
     * @return {@link OntFormat} corresponding to the specified source.
     * @throws OntologyFactoryImpl.UnsupportedFormatException if source can't be read into graph using jena.
     * @throws OntologyFactoryImpl.ConfigMismatchException    if there is some conflict with config settings, anyway we can't continue.
     * @throws OWLOntologyCreationException                   if there is some serious IO problem
     * @throws OntApiException                                if some other problem.
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
            throw new OntologyFactoryImpl.ConfigMismatchException("Not allowed scheme: " + iri);
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
     * @throws OntologyFactoryImpl.UnsupportedFormatException if the format is present in the source but not valid
     */
    public static Set<OntFormat> getSupportedFormats(OWLOntologyDocumentSource source) throws OntologyFactoryImpl.UnsupportedFormatException {
        Set<OntFormat> res = new LinkedHashSet<>();
        if (source.getFormat().isPresent()) {
            OntFormat f = OntFormat.get(source.getFormat().get());
            if (f == null || !f.isReadSupported()) {
                throw new OntologyFactoryImpl.UnsupportedFormatException("Format " + source.getFormat().get() + " is not supported.");
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
        final OWLOntologyCreationException cause = new OntologyFactoryImpl.UnsupportedFormatException(String.format("Can't read %s %s.",
                source.getClass().getSimpleName(), iri));
        for (OntFormat format : getSupportedFormats(source)) {
            if (format.isOWLOnly()) {
                cause.addSuppressed(new OntologyFactoryImpl.UnsupportedFormatException("Not supported by jena.").putFormat(format).putSource(iri));
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
                cause.addSuppressed(new OntologyFactoryImpl.UnsupportedFormatException(e).putSource(iri).putFormat(format));
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

    /**
     * The analogue of {@link Function} with checked {@link OWLOntologyInputSourceException owl-exception}.
     */
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
}
