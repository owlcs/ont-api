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

package com.github.owlcs.owlapi.tests.api;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.Arrays;
import java.util.Collection;

public class PunnedDeclarationsNotAddedTestCase extends TestBase {

    public static Collection<OWLDocumentFormat> data() {
        return Arrays.asList(new FunctionalSyntaxDocumentFormat(),
                new OWLXMLDocumentFormat(), new RDFXMLDocumentFormat(), new TurtleDocumentFormat());
    }

    private OWLOntology getOntologyWithPunnedInvalidDeclarations() {
        OWLOntology o = getOWLOntology();
        OWLObjectProperty op = df.getOWLObjectProperty(iri("testProperty"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testProperty"));
        o.add(df.getOWLDeclarationAxiom(op));
        o.add(df.getOWLTransitiveObjectPropertyAxiom(op));
        OWLAnnotationAssertionAxiom assertion = df.getOWLAnnotationAssertionAxiom(iri("test"),
                df.getOWLAnnotation(ap, iri("otherTest")));
        o.add(assertion);
        return o;
    }

    private OWLOntology getOntologyWithMissingDeclarations() {
        OWLOntology o = getOWLOntology();
        OWLObjectProperty op = df.getOWLObjectProperty(iri("testObjectProperty"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testAnnotationProperty"));
        o.add(df.getOWLTransitiveObjectPropertyAxiom(op));
        OWLAnnotationAssertionAxiom assertion = df.getOWLAnnotationAssertionAxiom(iri("test"),
                df.getOWLAnnotation(ap, iri("otherTest")));
        o.add(assertion);
        return o;
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldDeclareMissingEntities(OWLDocumentFormat format) throws Exception {
        OWLOntology o = getOntologyWithMissingDeclarations();
        OWLOntology reloaded = roundTrip(o, format);
        OWLObjectProperty op = df.getOWLObjectProperty(iri("testObjectProperty"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testAnnotationProperty"));
        Assertions.assertTrue(reloaded.containsAxiom(df.getOWLDeclarationAxiom(ap)));
        Assertions.assertTrue(reloaded.containsAxiom(df.getOWLDeclarationAxiom(op)));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldNotAddDeclarationsForIllegalPunnings(OWLDocumentFormat format) throws Exception {
        Assumptions.assumeTrue(OWLManager.DEBUG_USE_OWL);
        OWLOntology o = getOntologyWithPunnedInvalidDeclarations();
        OWLOntology reloaded = roundTrip(o, format);
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testProperty"));
        OWLDeclarationAxiom ax = df.getOWLDeclarationAxiom(ap);
        Assertions.assertFalse(reloaded.containsAxiom(ax), "ap testProperty should not have been declared");
    }

    @Test
    public void shouldNotAddDeclarationsForIllegalPunnings() throws Exception {
        Assumptions.assumeFalse(OWLManager.DEBUG_USE_OWL);
        OWLOntologyManager m = setupManager();
        OWLOntologyLoaderConfiguration conf = ((com.github.owlcs.ontapi.config.OntLoaderConfiguration) m
                .getOntologyLoaderConfiguration())
                .setAllowReadDeclarations(false)
                .setPersonality(com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig.ONT_PERSONALITY_STRICT);
        m.setOntologyLoaderConfiguration(conf);

        OWLOntology o = m.createOntology(IRI.getNextDocumentIRI(URI_BASE));

        OWLObjectProperty op = df.getOWLObjectProperty(iri("testProperty"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testProperty"));
        OWLAnnotationAssertionAxiom assertion = df.getOWLAnnotationAssertionAxiom(iri("test"),
                df.getOWLAnnotation(ap, iri("otherTest")));

        o.add(df.getOWLDeclarationAxiom(op));
        o.add(df.getOWLTransitiveObjectPropertyAxiom(op));

        // ONT-API HACK: not even able to add illegal axioms:
        try {
            o.add(assertion);
            Assertions.fail("The assertion successfully added: " + assertion);
        } catch (OntApiException e) {
            LOGGER.debug("Exception: {}", e.getMessage());
            Throwable cause = e.getCause();
            if (cause instanceof com.github.owlcs.ontapi.jena.OntJenaException) {
                LOGGER.debug("Cause: {}", cause.getMessage());
                return;
            }
            throw e;
        }
    }

}
