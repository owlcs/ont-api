/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.owlcs.owlapi.tests.api;

import com.github.owlcs.owlapi.OWLManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.dlsyntax.parser.DLSyntaxOWLParserFactory;
import org.semanticweb.owlapi.dlsyntax.renderer.DLSyntaxStorerFactory;
import org.semanticweb.owlapi.formats.*;
import org.semanticweb.owlapi.functional.parser.OWLFunctionalSyntaxOWLParserFactory;
import org.semanticweb.owlapi.functional.renderer.FunctionalSyntaxStorerFactory;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.krss1.parser.KRSSOWLParserFactory;
import org.semanticweb.owlapi.krss2.parser.KRSS2OWLParserFactory;
import org.semanticweb.owlapi.krss2.renderer.KRSS2OWLSyntaxStorerFactory;
import org.semanticweb.owlapi.krss2.renderer.KRSSSyntaxStorerFactory;
import org.semanticweb.owlapi.latex.renderer.LatexStorerFactory;
import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntaxOntologyParserFactory;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterSyntaxStorerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.owlxml.parser.OWLXMLParserFactory;
import org.semanticweb.owlapi.owlxml.renderer.OWLXMLStorerFactory;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParserFactory;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.RDFXMLStorerFactory;
import org.semanticweb.owlapi.rdf.turtle.parser.TurtleOntologyParserFactory;
import org.semanticweb.owlapi.rdf.turtle.renderer.TurtleStorerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ParsersStorersTestCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParsersStorersTestCase.class);

    public static Collection<OWLAxiom> getData() {
        return new DataBuilder().all();
    }

    private OWLOntology ont(OWLAxiom object) {
        OWLOntology o = getAnonymousOWLOntology();
        o.add(object);
        return o;
    }

    protected OWLOntology getAnonymousOWLOntology() {
        try {
            return OWLManager.createOWLOntologyManager().createOntology();
        } catch (OWLOntologyCreationException e) {
            return Assertions.fail(e);
        }
    }

    private void test(OWLAxiom object,
                      OWLStorerFactory stores,
                      OWLParserFactory parsers,
                      OWLDocumentFormat format,
                      boolean expectParse,
                      boolean expectRoundtrip) throws Exception {
        LOGGER.debug("Test object: <{}>", object);
        LOGGER.debug("Test format: {}", format.getClass().getSimpleName());
        StringDocumentTarget target = new StringDocumentTarget();
        OWLOntology data = ont(object);

        LOGGER.debug("Test Data:");
        com.github.owlcs.ontapi.utils.ReadWriteUtils.print(data);
        LOGGER.debug("Original axioms:");
        data.axioms().forEach(a -> LOGGER.debug("DATA AXIOM {}", a));

        stores.createStorer().storeOntology(data, target, format);
        OWLOntology res = getAnonymousOWLOntology();
        LOGGER.debug("After store:\n{}", target.toString());

        try {
            OWLDocumentFormat resultFormat = parsers.createParser()
                    .parse(new StringDocumentSource(target), res, new OWLOntologyLoaderConfiguration());
            LOGGER.debug("Result format: {}", resultFormat.getClass().getSimpleName());
        } catch (OWLParserException e) {
            if (expectParse) {
                LOGGER.debug("ParsersStorersTestCase.test() {}", target);
                throw e;
            } else {
                LOGGER.debug("parse fail: {} {}", format.getKey(), object);
                return;
            }
        }
        LOGGER.debug("Axioms after parsing:");
        res.axioms().forEach(a -> LOGGER.debug("RESULT AXIOM {}", a));
        if (!expectRoundtrip) {
            LOGGER.debug("Don't check the axiom contents.");
            return;
        }
        // original method doesn't care about annotations attached to axiom. so we save the same behaviour.
        String test = String.valueOf(trimAxiom(object));
        final String bNodePattern = "_:[^\\s)]+";
        final String bNodeValue = "_:id";
        List<String> axioms = res.axioms()
                .map(ParsersStorersTestCase::trimAxiom)
                .map(String::valueOf)
                .map(s -> s.replaceAll(bNodePattern, bNodeValue)).collect(Collectors.toList());

        Assertions.assertTrue(axioms.contains(test), "Can't find " + test + " inside \n" + axioms);
    }

    private static OWLAxiom trimAxiom(OWLAxiom axiom) {
        if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
            axiom = ((OWLObjectPropertyAssertionAxiom) axiom).getSimplified();
        }
        return axiom.getAxiomWithoutAnnotations();
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testManchesterSyntax(OWLAxiom object) throws Exception {
        boolean logicalAxiom = object.isLogicalAxiom();
        test(object, new ManchesterSyntaxStorerFactory(),
                new ManchesterOWLSyntaxOntologyParserFactory(), new ManchesterSyntaxDocumentFormat(), logicalAxiom, logicalAxiom);
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testKRSS2(OWLAxiom object) throws Exception {
        // XXX at some point roundtripping should be supported
        test(object, new KRSS2OWLSyntaxStorerFactory(), new KRSS2OWLParserFactory(), new KRSS2DocumentFormat(), false, false);
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testKRSS(OWLAxiom object) throws Exception {
        // XXX at some point roundtripping should be supported
        test(object, new KRSSSyntaxStorerFactory(), new KRSSOWLParserFactory(), new KRSSDocumentFormat(), false, false);
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testTurtle(OWLAxiom object) throws Exception {
        test(object, new TurtleStorerFactory(), new TurtleOntologyParserFactory(), new TurtleDocumentFormat(), true, true);
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testFSS(OWLAxiom object) throws Exception {
        test(object, new FunctionalSyntaxStorerFactory(),
                new OWLFunctionalSyntaxOWLParserFactory(), new FunctionalSyntaxDocumentFormat(), true, true);
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testOWLXML(OWLAxiom object) throws Exception {
        test(object, new OWLXMLStorerFactory(), new OWLXMLParserFactory(), new OWLXMLDocumentFormat(), true, true);
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testRDFXML(OWLAxiom object) throws Exception {
        test(object, new RDFXMLStorerFactory(), new RDFXMLParserFactory(), new RDFXMLDocumentFormat(), true, true);
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testDLSyntax(OWLAxiom object) throws Exception {
        // XXX at some point roundtripping should be supported
        test(object, new DLSyntaxStorerFactory(), new DLSyntaxOWLParserFactory(), new DLSyntaxDocumentFormat(), false, false);
    }

    @ParameterizedTest
    @MethodSource("getData")
    public void testLatex(OWLAxiom object) throws Exception {
        LatexDocumentFormat ontologyFormat = new LatexDocumentFormat();
        LatexStorerFactory storer = new LatexStorerFactory();
        StringDocumentTarget target = new StringDocumentTarget();
        storer.createStorer().storeOntology(ont(object), target, ontologyFormat);
        // assertEquals(expected, object.toString());
    }
}
