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

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PrefixMapping;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.PrefixManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is an extended {@link OWLOntologyDocumentSource} to provide possibility of passing any graph as is.
 * There are also default implementations of {@link #getInputStream()} and {@link #getReader()},
 * so you can use this document source in pure OWL-API too.
 * These methods are not used in ONT-API, instead it there is access to direct link ({@link #getGraph()}).
 * Note: you may want to disable transformations (see {@link ru.avicomp.ontapi.config.OntConfig#setPerformTransformation(boolean)},
 * {@link ru.avicomp.ontapi.config.OntLoaderConfiguration#setPerformTransformation(boolean)})
 * while loading, otherwise the encapsulated graph may still have some changes due to tuning by transformations.
 * <p>
 * Created by szuev on 22.02.2017.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntGraphDocumentSource implements OWLOntologyDocumentSource {
    protected static final Logger LOGGER = LoggerFactory.getLogger(OntGraphDocumentSource.class);

    protected List<Exception> exceptions = new ArrayList<>();

    /**
     * Gets Graph as is.
     *
     * @return {@link Graph}
     */
    public abstract Graph getGraph();

    @Override
    public Optional<Reader> getReader() {
        return getInputStream().map(is -> new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    @Override
    public Optional<InputStream> getInputStream() {
        return format().map(OntFormat::getLang).map(this::toInputStream);
    }

    @Nullable
    protected InputStream toInputStream(Lang lang) {
        try {
            return toInputStream(getGraph(), lang);
        } catch (Exception e) {
            LOGGER.error("Can't get InputStream.", e);
            exceptions.add(e);
            return null;
        }
    }

    public static InputStream toInputStream(Graph graph, Lang lang) { // move to graph utils?
        PipedInputStream res = new PipedInputStream();
        new Thread(() -> {
            IOException ex = null;
            try (PipedOutputStream out = new PipedOutputStream(res)) {
                RDFDataMgr.write(out, graph, lang);
            } catch (IOException e) {
                LOGGER.error("Can't write.", e);
                ex = e;
            }
            if (ex != null) throw new OntApiException("Exception while converting output->input", ex);
        }).start();
        return res;
    }

    @Override
    public Optional<OWLDocumentFormat> getFormat() {
        PrefixMapping pm = getGraph().getPrefixMapping();
        return format().map(OntFormat::createOwlFormat)
                .map(f -> {
                    PrefixManager res = PrefixManager.class.cast(f);
                    pm.getNsPrefixMap().forEach(res::setPrefix);
                    return f;
                });
    }

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
        return !exceptions.isEmpty();
    }

    @Override
    public boolean hasAlredyFailedOnIRIResolution() {
        return false;
    }

    @Override
    public void setIRIResolutionFailed(boolean value) {
        throw new OntApiException.Unsupported(getClass(), "setIRIResolutionFailed");
    }
}
