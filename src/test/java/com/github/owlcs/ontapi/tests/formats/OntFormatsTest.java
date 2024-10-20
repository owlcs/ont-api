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

package com.github.owlcs.ontapi.tests.formats;

import com.github.owlcs.TempDirectory;
import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.testutils.StringInputStreamDocumentSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLStorerNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is a collection of simple acceptance test-cases
 * to control changes in OWL-API formats (parsers and storers) in conjunction with ONT-API io-mechanisms.
 * Currently, it is mostly to compare OWL-API and ONT-API read/write support.
 * <p>
 * Created by @ssz on 10.01.2018.
 *
 * @see OntFormat
 */
@ExtendWith(TempDirectory.class)
public class OntFormatsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntFormatsTest.class);

    private OWLOntologyManager newOntologyManager() {
        OWLOntologyManager res = OntManagers.createOWLAPIImplManager();
        // TRIX is banned since OWL-API:5.5.1;
        // here, we test OntFormats, so clear list of banned types
        res.getOntologyConfigurator().withBannedParsers("");
        return res;
    }

    @Test
    public void testOntFormatsCommon() {
        Arrays.stream(OntFormat.values()).forEach(f -> {
            OWLDocumentFormat owl = f.createOwlFormat();
            Assertions.assertNotNull(owl);
            Assertions.assertNotNull(owl.getKey());
            Assertions.assertSame(f, OntFormat.get(owl));
        });
    }

    @Test
    public void testDocumentFormatFactories() throws Exception {
        // from owlapi-impl to compare.
        // use the simplest ontology to avoid any deep parsing exceptions
        OWLOntology ontology = newOntologyManager().createOntology(IRI.create("http://test.org/empty"));
        LOGGER.debug("{}", ontology);
        Set<OntFormat> writeNotSupported = new HashSet<>();
        Set<OntFormat> readNotSupported = new HashSet<>();
        for (OntFormat format : OntFormat.values()) {
            Path file = save(ontology, format);
            LOGGER.debug("Format: {}, File: {}", format, file);
            if (file == null) { // write fail, but if it is pure jena format it is expected.
                if (!format.isJenaOnly()) {
                    writeNotSupported.add(format);
                }
                continue;
            }
            try {
                OWLOntologyDocumentSource source = new IRIDocumentSource(IRI.create(file.toUri()), format.createOwlFormat(), null);
                newOntologyManager().loadOntologyFromOntologyDocument(source);
            } catch (UnparsableOntologyException e) {
                LOGGER.debug("Can't read {}", file, e);
                readNotSupported.add(format);
            } catch (OWLOntologyCreationException e) {
                throw new AssertionError(e);
            }
        }
        LOGGER.debug("Write not supported: {}", writeNotSupported);
        LOGGER.debug("Read not supported: {}", readNotSupported);
        writeNotSupported.forEach(f -> Assertions.assertFalse(f.isWriteSupported(), f.toString()));
        readNotSupported.forEach(f -> Assertions.assertFalse(f.isReadSupported(), f.toString()));
    }

    @Test
    public void testFormatSupporting() throws OWLOntologyCreationException, OWLOntologyStorageException {
        OWLOntologyManager m = newOntologyManager();
        OWLDataFactory df = m.getOWLDataFactory();
        // make a simple ontology with class-assertion and sub-class-of axioms:
        OWLClass c1 = df.getOWLClass(IRI.create("http://test.org/class1"));
        OWLClass c2 = df.getOWLClass(IRI.create("http://test.org/class2"));
        OWLNamedIndividual i = df.getOWLNamedIndividual(IRI.create("http://test.org/individual"));
        OWLOntology ont = m.createOntology(IRI.create("http://test.org/simple"));
        ont.add(df.getOWLDeclarationAxiom(c1));
        ont.add(df.getOWLDeclarationAxiom(i));
        ont.add(df.getOWLClassAssertionAxiom(c1, i));
        ont.add(df.getOWLDeclarationAxiom(c2));
        ont.add(df.getOWLSubClassOfAxiom(c2, c1));

        for (OntFormat type : OntFormat.values()) {
            if (type.isJenaOnly()) {
                continue;
            }
            OWLDocumentFormat format = type.createOwlFormat();
            LOGGER.debug("Write test. Format: {}", format.getClass().getSimpleName());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String txt;
            try {
                ont.saveOntology(format, out);
                Assertions.assertTrue(type.isWriteSupported(), type + ": write should be supported");
                txt = out.toString(StandardCharsets.UTF_8);
                LOGGER.debug(txt);
            } catch (OWLStorerNotFoundException e) {
                Assertions.assertFalse(type.isWriteSupported(), type + ": write should not be supported");
                LOGGER.debug("{} is not supported to write ", type);
                continue;
            }

            LOGGER.debug("Read test. Format: {}", format.getClass().getSimpleName());
            OWLOntologyDocumentSource source = new StringInputStreamDocumentSource(txt, format);

            OWLOntology res;
            try {
                res = newOntologyManager().loadOntologyFromOntologyDocument(source);
                Assertions.assertTrue(type.isReadSupported(), type + ": read should be supported");
            } catch (UnparsableOntologyException e) {
                Assertions.assertFalse(type.isSupported(), type + ": should not be supported");
                LOGGER.debug("{} is not supported to read ", type);
                continue;
            }
            List<OWLAxiom> axioms = res.axioms().collect(Collectors.toList());
            LOGGER.debug("Format: {}. Axioms: {}", type, axioms);
            //noinspection unchecked
            if (!checkAxiomsCount(ont, res, AxiomType.CLASS_ASSERTION, AxiomType.SUBCLASS_OF)) {
                LOGGER.debug("Can't find class assertion. Format: {}", format);
                if (!type.isSupported()) continue;
                Assertions.fail("Wrong axioms. Format: " + type);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean checkAxiomsCount(OWLOntology expected, OWLOntology actual, AxiomType<? extends OWLAxiom>... types) {
        return Arrays.stream(types).allMatch(type -> actual.axioms(type).count() == expected.axioms(type).count());
    }

    private static Path save(OWLOntology ontology, OntFormat type) throws IOException {
        Path file = TempDirectory.createFile("formats-test.", "." + type.getExt());
        LOGGER.debug("Save ontology to {} (format={})", file, type);
        OWLDocumentFormat format = type.createOwlFormat();
        try (OutputStream out = Files.newOutputStream(file)) {
            ontology.getOWLOntologyManager().saveOntology(ontology, format, out);
        } catch (OWLOntologyStorageException | IOException | UnsupportedOperationException e) {
            LOGGER.warn("Unable to print owl-ontology {}: '{}'", ontology, e.getMessage());
            return null;
        }
        return file;
    }
}
