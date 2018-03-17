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
 *
 */

package ru.avicomp.ontapi.tests.formats;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.IRIDocumentSource;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.StringInputStreamDocumentSource;
import ru.avicomp.owlapi.OWLManager;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * As a test.
 * To compare OWL-API and ONT-API read/write support.
 * See {@link OntFormat}.
 *
 * Created by @szuev on 10.01.2018.
 */
public class OntFormatsChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntFormatsChecker.class);

    @Test
    public void testDocumentFormatFactories() throws OWLOntologyCreationException {
        // from owlapi-apibinding.
        // use the simplest ontology to avoid any deep parsing exceptions
        OWLOntology ontology = OWLManager.createOWLOntologyManager().createOntology(IRI.create("http://test.org/empty"));
        LOGGER.info("{}", ontology);
        Set<OntFormat> writeNotSupported = new HashSet<>();
        Set<OntFormat> readNotSupported = new HashSet<>();
        for (OntFormat f : OntFormat.values()) {
            Path p = ReadWriteUtils.save(ontology, "formats-test", f);
            LOGGER.debug("Format: {}, File: {}", f, p);
            if (p == null) { // write fail, but if it is pure jena format it is expected.
                if (!f.isJenaOnly())
                    writeNotSupported.add(f);
                continue;
            }
            try {
                OWLOntologyDocumentSource source = new IRIDocumentSource(IRI.create(p.toUri()), f.createOwlFormat(), null);
                OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(source);
            } catch (UnparsableOntologyException e) {
                LOGGER.error("Can't read " + p, e);
                readNotSupported.add(f);
            } catch (OWLOntologyCreationException e) {
                throw new AssertionError(e);
            }
        }
        LOGGER.debug("Write not supported: {}", writeNotSupported);
        LOGGER.debug("Read not supported: {}", readNotSupported);
        writeNotSupported.forEach(f -> Assert.assertFalse(f.toString(), f.isWriteSupported()));
        readNotSupported.forEach(f -> Assert.assertFalse(f.toString(), f.isReadSupported()));
    }

    @Test
    public void testFormatSupporting() throws OWLOntologyCreationException, OWLOntologyStorageException {
        OWLOntologyManager m = OWLManager.createOWLOntologyManager();
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
            if (type.isJenaOnly()) continue;
            OWLDocumentFormat format = type.createOwlFormat();
            LOGGER.debug("Write test. Format: {}", format.getClass().getSimpleName());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String txt;
            try {
                ont.saveOntology(format, out);
                Assert.assertTrue(type + ": write should be supported", type.isWriteSupported());
                txt = new String(out.toByteArray(), StandardCharsets.UTF_8);
                LOGGER.debug(txt);
            } catch (OWLStorerNotFoundException e) {
                Assert.assertFalse(type + ": write should not be supported", type.isWriteSupported());
                LOGGER.info("{} is not supported to write ", type);
                continue;
            }

            LOGGER.debug("Read test. Format: {}", format.getClass().getSimpleName());
            OWLOntologyDocumentSource source = new StringInputStreamDocumentSource(txt, format);

            OWLOntology res;
            try {
                res = OWLManager.createOWLOntologyManager().loadOntologyFromOntologyDocument(source);
                Assert.assertTrue(type + ": read should be supported", type.isReadSupported());
            } catch (UnparsableOntologyException e) {
                Assert.assertFalse(type + ": should not be supported", type.isSupported());
                LOGGER.info("{} is not supported to read ", type);
                continue;
            }
            List<OWLAxiom> axioms = res.axioms().collect(Collectors.toList());
            LOGGER.debug("Format: {}. Axioms: {}", type, axioms);
            //noinspection unchecked
            if (!checkAxiomsCount(ont, res, AxiomType.CLASS_ASSERTION, AxiomType.SUBCLASS_OF)) {
                LOGGER.warn("Can't find class assertion. Format: {}" + format);
                if (!type.isSupported()) continue;
                Assert.fail("Wrong axioms. Format: " + type);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean checkAxiomsCount(OWLOntology expected, OWLOntology actual, AxiomType<? extends OWLAxiom>... types) {
        return Arrays.stream(types).allMatch(type -> actual.axioms(type).count() == expected.axioms(type).count());
    }
}
