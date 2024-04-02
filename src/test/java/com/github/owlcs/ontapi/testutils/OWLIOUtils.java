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

package com.github.owlcs.ontapi.testutils;

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntGraphUtils;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.junit.jupiter.api.Assertions;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

/**
 * Test utils to work with io.
 * <p>
 * Created by @ssz on 27.09.2016.
 */
@SuppressWarnings({"WeakerAccess", "SameParameterValue"})
public class OWLIOUtils {
    public static final PrintStream NULL_OUT = new PrintStream(new OutputStream() {
        @Override
        public void write(int b) {
        }
    });
    private static final Logger LOGGER = LoggerFactory.getLogger(OWLIOUtils.class);

    public static void print(OWLOntology ontology) {
        print(ontology, null);
    }

    public static void print(Model model) {
        print(model, null);
    }

    public static void print(Model ontology, OntFormat ext) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\n{}", asString(ontology, ext));
        }
    }

    public static void print(OWLOntology ontology, OntFormat ext) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\n{}", asString(ontology, ext));
        }
    }

    public static String asString(OWLOntology ontology, OntFormat format) {
        return asString(ontology, format == null ? new TurtleDocumentFormat() : format.createOwlFormat());
    }

    public static String asString(OWLOntology ontology, OWLDocumentFormat format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ontology.getOWLOntologyManager().saveOntology(ontology, format, out);
            return out.toString(StandardCharsets.UTF_8);
        } catch (OWLOntologyStorageException | IOException e) {
            throw new AssertionError(e);
        }
    }

    public static String asString(Model model, OntFormat ext) {
        return toStringWriter(model, ext).toString();
    }

    public static StringWriter toStringWriter(Model model, OntFormat ext) {
        StringWriter sw = new StringWriter();
        model.write(sw, (ext == null ? OntFormat.TURTLE : ext).getID(), null);
        return sw;
    }

    public static InputStream asInputStream(String txt) {
        return IOUtils.toInputStream(txt, StandardCharsets.UTF_8.name());
    }

    public static InputStream asInputStream(Model model, OntFormat ext) {
        return asInputStream(asString(model, ext));
    }

    public static OWLOntology loadOWLOntology(OWLOntologyManager manager, IRI fileIRI) {
        LOGGER.debug("Load ontology model from {}.", fileIRI);
        OWLOntology owl = null;
        try {
            owl = manager.loadOntology(fileIRI);
        } catch (OWLOntologyCreationException e) {
            Assertions.fail(e.getMessage());
        }
        return owl;
    }

    public static Model load(URI file, OntFormat f) {
        String format = f == null ? "ttl" : f.getID();
        Model m = ModelFactory.createDefaultModel();
        LOGGER.debug("Load model from {}", file);
        try (InputStream in = file.toURL().openStream()) {
            m.read(in, null, format);
            return m;
        } catch (IOException e) {
            LOGGER.error("Can't read model", e);
            throw new AssertionError(e);
        }
    }

    public static Model loadResourceAsModel(String resource, Lang lang) {
        Model m = ModelFactory.createDefaultModel();
        try (InputStream in = openResourceStream(resource)) {
            return m.read(in, null, lang.getName());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static OWLOntology convertJenaToOWL(OWLOntologyManager manager, Model model, OntFormat convertFormat) {
        String uri = OntGraphUtils.getOntologyIRIOrNull(model.getGraph());
        LOGGER.debug("Put ontology {}({}) to the manager.", uri, convertFormat);
        try (InputStream is = asInputStream(model, convertFormat == null ? OntFormat.TURTLE : convertFormat)) {
            return manager.loadOntologyFromOntologyDocument(is);
        } catch (IOException | OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }

    public static OWLOntology convertJenaToOWL(OWLOntologyManager manager, Model model) {
        if (manager == null) manager = OntManagers.createOWLAPIImplManager();
        return convertJenaToOWL(manager, model, OntFormat.TURTLE);
    }

    public static Ontology convertJenaToONT(OntologyManager manager, Model model) {
        if (manager == null) manager = OntManagers.createManager();
        return (Ontology) convertJenaToOWL(manager, model, OntFormat.TURTLE);
    }

    public static OWLOntologyDocumentSource getFileDocumentSource(String file, OntFormat format) {
        Path path = OWLIOUtils.getResourcePath(file);
        return new FileDocumentSource(path.toFile(), format.createOwlFormat());
    }

    public static OWLOntologyDocumentSource getStringDocumentSource(String txt, OntFormat format) {
        return new StringInputStreamDocumentSource(txt, format);
    }

    public static URI getResourceURI(String resource) {
        try {
            return getResourceURL(resource).toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    public static URL getResourceURL(String resource) {
        return Objects.requireNonNull(OWLIOUtils.class.getResource(resource));
    }

    public static Path getResourcePath(String resource) {
        try {
            return Paths.get(getResourceURI(resource)).toRealPath();
        } catch (IOException e) {
            throw new IllegalStateException("Can't find path " + resource, e);
        }
    }

    public static InputStream openResourceStream(String resource) {
        return Objects.requireNonNull(OWLIOUtils.class.getResourceAsStream(resource), "Can't find resource " + resource);
    }

    public static OutputStream nullOutputStream(Runnable onClose) {
        return new OutputStream() {
            @Override
            public void write(int b) {
            }

            @Override
            public void close() {
                onClose.run();
            }
        };
    }

    public static Writer nullWriter(Runnable onClose) {
        return new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) {
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
                onClose.run();
            }
        };
    }

    public static OWLOntologyDocumentTarget newOWLOntologyDocumentTarget(OutputStream outputStream, Writer writer) {
        return new OWLOntologyDocumentTarget() {
            @Override
            public Optional<Writer> getWriter() {
                return Optional.ofNullable(writer);
            }

            @Override
            public Optional<OutputStream> getOutputStream() {
                return Optional.ofNullable(outputStream);
            }

            @Override
            public Optional<IRI> getDocumentIRI() {
                return Optional.empty();
            }
        };
    }
}
