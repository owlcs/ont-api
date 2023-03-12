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

import com.github.owlcs.ontapi.jena.utils.Graphs;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PrefixMapping;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.PrefixManager;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;


/**
 * This is an extended {@link OWLOntologyDocumentSource} to provide possibility to pass any graph as is.
 * <p>
 * There are default implementations of {@link #getInputStream()} and {@link #getReader()} methods,
 * so you can use this document-source with the original OWL-API impl as well.
 * But these methods are not used by ONT-API;
 * instead, the method {@link #getGraph()} (which provides a direct link to the graph) is used.
 * <p>
 * Note: you may want to disable transformations, otherwise the encapsulated graph may still have some changes
 * due to tuning by the {@link OntologyFactory loading factory}.
 * To do this, you can use method {@link com.github.owlcs.ontapi.config.OntConfig#setPerformTransformation(boolean)},
 * which turns off transformations throughout the manager,
 * or method {@link com.github.owlcs.ontapi.config.OntLoaderConfiguration#setPerformTransformation(boolean)}
 * for a particular ontology, or just override method {@link #withTransforms()}.
 * <p>
 * Created by szuev on 22.02.2017.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntGraphDocumentSourceImpl implements OntGraphDocumentSource {
    protected final AtomicReference<Exception> exception = new AtomicReference<>();

    /**
     * Gets the IRI of this ontology document source.
     * Every call to this method must return the same IRI, which must be unique within the manager.
     *
     * @return {@link IRI}, not {@code null}
     */
    @Override
    public IRI getDocumentIRI() {
        return IRI.create("graph:" + OntGraphUtils.toString(getGraph()));
    }

    /**
     * Gets a reader which an ontology can be read from.
     * This method may be called multiple times and each invocation will return a new reader.
     * This method is not used by ONT-API, but it can be used by OWL-API-impl.
     *
     * @return Optional around {@link Reader}
     */
    @Override
    public Optional<Reader> getReader() {
        return getInputStream().map(is -> new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    /**
     * Gets an input stream which an ontology can be read from.
     * This method may be called multiple times and each invocation will return a new input stream.
     * There are no direct usages in ONT-API, but it can be used by OWL-API-impl.
     *
     * @return Optional around {@link InputStream}
     */
    @Override
    public Optional<InputStream> getInputStream() {
        return format().map(OntFormat::getLang).map(lang -> toInputStream(getGraph(), lang, exception));
    }

    /**
     * Creates a new {@code InputStream} for the given {@code Graph} and {@code lang}.
     * Please don't forget to call {@link AutoCloseable#close()} - all exceptions are handled there.
     *
     * @param graph  {@link Graph} a graph to read from
     * @param lang   {@link Lang} format syntax
     * @param error {@link AtomicReference}, a container that will contain an {@code Exception} if it occurs
     * @return {@code InputStream}
     */
    protected static InputStream toInputStream(Graph graph, Lang lang, AtomicReference<Exception> error) {
        Objects.requireNonNull(graph);
        Objects.requireNonNull(lang);
        Objects.requireNonNull(error);
        PipedInputStream in = new PipedInputStream();
        CountDownLatch complete = new CountDownLatch(1);
        FilterInputStream res = new FilterInputStream(in) {
            private volatile boolean closed;

            @Override
            public void close() throws IOException {
                // next 'close' should not throw an exception
                if (closed) return;
                closed = true;
                try {
                    super.close();
                } catch (IOException e) {
                    IOException x = findIOException(error.get(), graph, lang);
                    if (x == null) {
                        error.set(e);
                    } else {
                        x.addSuppressed(e);
                    }
                }
                IOException ex = findIOException(error.get(), graph, lang);
                if (ex != null) throw ex;
            }
        };

        new Thread(() -> {
            PipedOutputStream out;
            try {
                out = new PipedOutputStream(in);
            } catch (IOException e) {
                error.set(e);
                return;
            } finally {
                complete.countDown();
            }
            try {
                RDFDataMgr.write(out, graph, lang);
            } catch (Exception ex) {
                error.set(ex);
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    Exception x = error.get();
                    if (x == null) {
                        error.set(e);
                    } else {
                        x.addSuppressed(e);
                    }
                }
            }
        }).start();
        try {
            complete.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return res;
    }

    private static IOException findIOException(Exception from, Graph graph, Lang lang) {
        if (from == null) return null;
        if (from instanceof IOException) return (IOException) from;
        String name;
        try {
            name = Graphs.getName(graph);
        } catch (Exception e) {
            name = "{unknown: '" + e.getMessage() + "'}";
        }
        return new IOException(String.format("Convert output->input. Graph: %s, %s.", name, lang), from);
    }

    /**
     * Returns an OWLDocumentFormat with prefixes from the graph (if it is supported by returned format).
     *
     * @return Optional around {@link OWLDocumentFormat}
     */
    @Override
    public Optional<OWLDocumentFormat> getFormat() {
        PrefixMapping pm = getGraph().getPrefixMapping();
        return format().map(OntFormat::createOwlFormat)
                .map(f -> {
                    if (f.isPrefixOWLDocumentFormat()) {
                        PrefixManager res = f.asPrefixOWLDocumentFormat();
                        pm.getNsPrefixMap().forEach(res::setPrefix);
                    }
                    return f;
                });
    }

    /**
     * Returns an ONT-Format
     *
     * @return {@link OntFormat}
     */
    public OntFormat getOntFormat() {
        return OntFormat.TURTLE;
    }

    private Optional<OntFormat> format() {
        return Optional.of(getOntFormat());
    }

    @Override
    public Optional<String> getMIMEType() {
        return format().map(OntFormat::getLang).map(Lang::getContentType).map(ContentType::getContentTypeStr);
    }

    @Override
    public boolean hasAlredyFailedOnStreams() {
        return exception.get() != null;
    }

    @Override
    public boolean hasAlredyFailedOnIRIResolution() {
        return false;
    }

    @Override
    public void setIRIResolutionFailed(boolean value) {
        throw new OntApiException.Unsupported("#setIRIResolutionFailed is not supported.");
    }

    /**
     * Creates an {@link OWLOntologyDocumentSource} that wraps the given graph.
     *
     * @param graph {@link Graph}, not {@code null}
     * @return {@link OntGraphDocumentSourceImpl}
     */
    public static OntGraphDocumentSourceImpl of(Graph graph) {
        Objects.requireNonNull(graph, "Null graph");
        return new OntGraphDocumentSourceImpl() {
            @Override
            public Graph getGraph() {
                return graph;
            }
        };
    }
}
