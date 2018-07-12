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

package ru.avicomp.ontapi.utils;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Assert;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Objects;

/**
 * Test utils to work with io.
 * <p>
 * Created by @szuev on 27.09.2016.
 */
@SuppressWarnings({"unused", "WeakerAccess", "SameParameterValue"})
public class ReadWriteUtils {
    public static final PrintStream NULL_OUT = new PrintStream(new OutputStream() {
        @Override
        public void write(int b) {
        }
    });
    private static final Logger LOGGER = LoggerFactory.getLogger(ReadWriteUtils.class);

    private static final String DESTINATION_DIR = "out";

    public static void print(OWLOntology ontology) {
        print(ontology, null);
    }

    public static void print(Model model) {
        print(model, null);
    }

    public static void print(OWLOntology ontology, OntFormat ext) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("\n{}", toString(ontology, ext));
    }

    public static void print(Model model, OntFormat ext) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("\n{}", toString(model, ext));
    }

    public static String toString(OWLOntology ontology, OntFormat format) {
        return toString(ontology, format == null ? new TurtleDocumentFormat() : format.createOwlFormat());
    }

    public static String toString(OWLOntology ontology, OWLDocumentFormat format) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ontology.getOWLOntologyManager().saveOntology(ontology, format, out);
            return out.toString(StandardCharsets.UTF_8.name());
        } catch (OWLOntologyStorageException | IOException e) {
            throw new AssertionError(e);
        }
    }

    public static String toString(Model model, OntFormat ext) {
        return toStringWriter(model, ext).toString();
    }

    public static StringWriter toStringWriter(Model model, OntFormat ext) {
        StringWriter sw = new StringWriter();
        model.write(sw, (ext == null ? OntFormat.TURTLE : ext).getID(), null);
        return sw;
    }

    public static InputStream toInputStream(String txt) {
        try {
            return IOUtils.toInputStream(txt, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public static InputStream toInputStream(Model model, OntFormat ext) {
        return toInputStream(toString(model, ext));
    }

    public static InputStream toInputStream(OWLOntology model, OntFormat ext) {
        return toInputStream(toString(model, ext));
    }

    public static Model loadFromString(String input, OntFormat ext) {
        Model m = ModelFactory.createDefaultModel();
        m.read(toInputStream(input), null, ext.getID());
        return m;
    }

    public static Model loadResourceTTLFile(String file) {
        return load(getResourceURI(file), null);
    }

    public static Model loadOutTTLFile(String file) {
        return load(getOutURI(file), null);
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

    public static Path getFileToSave(String name, OntFormat type) {
        Path dir = Paths.get(DESTINATION_DIR);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectory(dir);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        try {
            return dir.toRealPath().resolve(name + (type != null ? "." + type.getExt() : ""));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Utilitarian method to get the File from '/resources' directory.
     * Can work with jar file as well.
     *
     * @param dirName  String, project dir, nullable
     * @param fileName String resource file name
     * @return {@link Path} to resource
     * @throws Exception if something wrong
     */
    public static Path getResourcePath(String dirName, String fileName) throws Exception {
        if (Objects.requireNonNull(fileName, "Null resource file name").isEmpty()) {
            throw new IllegalArgumentException("Empty resource file name");
        }
        String dir = dirName == null ? "/" : dirName.startsWith("/") ? dirName : ("/" + dirName);
        String file = (dir + "/" + fileName).replaceAll("/+", "/");
        URL url = ReadWriteUtils.class.getResource(file);
        if (url == null) {
            throw new IllegalArgumentException("Can't find file " + file + ".");
        } else if ("jar".equalsIgnoreCase(url.toURI().getScheme())) {
            FileSystem jar;
            try {
                jar = FileSystems.getFileSystem(url.toURI());
            } catch (FileSystemNotFoundException e) {
                jar = FileSystems.newFileSystem(url.toURI(), new HashMap<>());
            }
            Path source = jar.getPath(dir).resolve(fileName);
            Path res = Paths.get(ReadWriteUtils.TemporaryResourcesHolder.DIR + file);
            if (!Files.exists(res)) {
                LOGGER.debug("Unpack {}:{} -> {}", jar, source, res);
                Files.createDirectories(res.getParent());
                Files.copy(source, res);
            }
            return res;
        }
        Path res = Paths.get(url.toURI());
        if (!Files.exists(res)) {
            throw new NoSuchFileException(res.toString());
        }
        return res;
    }

    public static File getResourceFile(String fileName) {
        try {
            return getResourcePath("", fileName).toFile();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static URI getResourceURI(String dir, String file) {
        try {
            return getResourcePath(dir, file).toUri();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    public static URI getResourceURI(String file) {
        String dir;
        String name;
        if (file.contains("/")) {
            name = file.replaceAll(".+/([^/]+$)", "$1");
            dir = file.replace(name, "");
        } else {
            dir = "";
            name = file;
        }
        return getResourceURI(dir, name);
    }

    public static Path getOutPath(String file) {
        return Paths.get(DESTINATION_DIR).resolve(file);
    }

    public static URI getOutURI(String file) {
        return getOutPath(file).toUri();
    }

    public static Path save(Model model, String name, OntFormat type) {
        Path dst = getFileToSave(name, type);
        LOGGER.debug("Save model to " + dst.toUri() + " (" + type.getID() + ")");
        try (Writer out = Files.newBufferedWriter(dst)) {
            model.write(out, type.getID());
        } catch (IOException e) {
            LOGGER.error("Unable to save model " + dst, e);
            return null;
        }
        return dst;
    }

    public static OWLOntology loadOWLOntology(OWLOntologyManager manager, IRI fileIRI) {
        LOGGER.debug("Load ontology model from {}.", fileIRI);
        OWLOntology owl = null;
        try {
            owl = manager.loadOntology(fileIRI);
        } catch (OWLOntologyCreationException e) {
            Assert.fail(e.getMessage());
        }
        return owl;
    }

    public static OWLOntology loadOWLOntology(IRI fileIRI) {
        return loadOWLOntology(OntManagers.createOWL(), fileIRI);
    }

    public static OntologyModel loadOntologyModel(IRI fileIRI) {
        return (OntologyModel) loadOWLOntology(OntManagers.createONT(), fileIRI);
    }

    public static OWLOntology convertJenaToOWL(OWLOntologyManager manager, Model model, OntFormat convertFormat) {
        String uri = TestUtils.getURI(model);
        LOGGER.debug("Put ontology {}({}) to the manager.", uri, convertFormat);
        try (InputStream is = toInputStream(model, convertFormat == null ? OntFormat.TURTLE : convertFormat)) {
            return manager.loadOntologyFromOntologyDocument(is);
        } catch (IOException | OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }

    public static OWLOntology convertJenaToOWL(Model model) {
        return convertJenaToOWL(null, model);
    }

    public static OWLOntology convertJenaToOWL(OWLOntologyManager manager, Model model) {
        if (manager == null) manager = OntManagers.createOWL();
        return convertJenaToOWL(manager, model, OntFormat.TURTLE);
    }

    public static OntologyModel convertJenaToONT(Model model) {
        return convertJenaToONT(null, model);
    }

    public static OntologyModel convertJenaToONT(OntologyManager manager, Model model) {
        if (manager == null) manager = OntManagers.createONT();
        return (OntologyModel) convertJenaToOWL(manager, model, OntFormat.TURTLE);
    }

    private static class TemporaryResourcesHolder {
        private static final String TEMP_RESOURCES_PREFIX = "ont-api-resources-";
        static final Path DIR = create();

        private static Path create() {
            try {
                Path res = Files.createTempDirectory(TEMP_RESOURCES_PREFIX);
                Runtime.getRuntime().addShutdownHook(new Thread(
                        () -> {
                            try {
                                Files.walkFileTree(res, new SimpleFileVisitor<Path>() {
                                    @Override
                                    public FileVisitResult visitFile(Path file, BasicFileAttributes a)
                                            throws IOException {
                                        Files.delete(file);
                                        return FileVisitResult.CONTINUE;
                                    }

                                    @Override
                                    public FileVisitResult postVisitDirectory(Path dir, IOException e)
                                            throws IOException {
                                        if (e == null) {
                                            Files.delete(dir);
                                            return FileVisitResult.CONTINUE;
                                        }
                                        throw e;
                                    }
                                });
                            } catch (IOException e) {
                                throw new UncheckedIOException("Failed to delete " + res, e);
                            }
                        }));
                return res;
            } catch (IOException e) {
                throw new UncheckedIOException("Unable to create temporary dir", e);
            }
        }
    }
}
