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

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PrefixMapping;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.PrefixManager;
import ru.avicomp.ontapi.jena.utils.Graphs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;


/**
 * This is an extended {@link OWLOntologyDocumentSource} to provide possibility to pass any graph as is.
 * <p>
 * There are default implementations of {@link #getInputStream()} and {@link #getReader()} methods,
 * so you can use this document-source with the original OWL-API impl as well.
 * But these methods are not used by ONT-API; instead, the method {@link #getGraph()} (which provides a direct link to the graph) is used.
 * <p>
 * Note: you may want to disable transformations (see {@link ru.avicomp.ontapi.config.OntConfig#setPerformTransformation(boolean)},
 * {@link ru.avicomp.ontapi.config.OntLoaderConfiguration#setPerformTransformation(boolean)})
 * while loading, otherwise the encapsulated graph may still have some changes due to tuning by {@link OntologyFactory loading factory}.
 * <p>
 * Created by szuev on 22.02.2017.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntGraphDocumentSource implements OWLOntologyDocumentSource {

    protected AtomicReference<IOException> exception = new AtomicReference<>();

    /**
     * Returns encapsulated graph.
     *
     * @return {@link Graph}
     */
    public abstract Graph getGraph();

    @Override
    public IRI getDocumentIRI() {
        return IRI.create("graph:" + Graphs.getName(getGraph()));
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
     * Creates a new InputStream from a Graph.
     *
     * @param graph  {@link Graph} a graph to read from
     * @param lang   {@link Lang} format syntax
     * @param holder {@link AtomicReference}, a container that will contain an IOException if it occurs
     * @return InputStream
     */
    public static InputStream toInputStream(Graph graph, Lang lang, AtomicReference<IOException> holder) {
        PipedInputStream in = new PipedInputStream();
        FilterInputStream res = new FilterInputStream(in) {
            private volatile boolean closed;

            @Override
            public void close() throws IOException {
                if (closed) return;
                try {
                    IOException e = holder.get();
                    if (e != null) {
                        throw new IOException(String.format("Convert output->input. Graph: %s, %s.", Graphs.getName(graph), lang), e);
                    }
                } finally {
                    super.close();
                    closed = true;
                }
            }
        };
        CountDownLatch complete = new CountDownLatch(1);
        new Thread(() -> {
            try (PipedOutputStream out = new PipedOutputStream(in)) {
                complete.countDown();
                RDFDataMgr.write(out, graph, lang);
            } catch (IOException e) {
                holder.set(e);
            }
        }).start();
        try {
            complete.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return res;
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
        return format().map(OntFormat::getLang).map(Lang::getContentType).map(ContentType::getContentType);
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
}
