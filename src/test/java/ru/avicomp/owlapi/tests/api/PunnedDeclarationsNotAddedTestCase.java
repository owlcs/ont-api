/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.owlapi.tests.api;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.semanticweb.owlapi.formats.FunctionalSyntaxDocumentFormat;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.owlapi.OWLManager;
import ru.avicomp.owlapi.tests.api.baseclasses.TestBase;

import java.util.Arrays;
import java.util.Collection;


@SuppressWarnings("javadoc")
@RunWith(Parameterized.class)
public class PunnedDeclarationsNotAddedTestCase extends TestBase {

    @Parameters(name = "{0}")
    public static Collection<OWLDocumentFormat> data() {
        return Arrays.asList(new FunctionalSyntaxDocumentFormat(), new OWLXMLDocumentFormat(), new RDFXMLDocumentFormat(), new TurtleDocumentFormat());
    }

    private final OWLDocumentFormat format;

    public PunnedDeclarationsNotAddedTestCase(OWLDocumentFormat format) {
        this.format = format;
    }

    private OWLOntology getOntologyWithPunnedInvalidDeclarations() {
        OWLOntology o = getOWLOntology();
        OWLObjectProperty op = df.getOWLObjectProperty(iri("testProperty"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testProperty"));
        o.add(df.getOWLDeclarationAxiom(op));
        o.add(df.getOWLTransitiveObjectPropertyAxiom(op));
        OWLAnnotationAssertionAxiom assertion = df.getOWLAnnotationAssertionAxiom(iri("test"), df.getOWLAnnotation(ap, iri("otherTest")));
        o.add(assertion);
        return o;
    }

    private OWLOntology getOntologyWithMissingDeclarations() {
        OWLOntology o = getOWLOntology();
        OWLObjectProperty op = df.getOWLObjectProperty(iri("testObjectProperty"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testAnnotationProperty"));
        o.add(df.getOWLTransitiveObjectPropertyAxiom(op));
        OWLAnnotationAssertionAxiom assertion = df.getOWLAnnotationAssertionAxiom(iri("test"), df.getOWLAnnotation(ap, iri("otherTest")));
        o.add(assertion);
        return o;
    }

    @Test
    public void shouldDeclareMissingEntities() throws OWLOntologyCreationException, OWLOntologyStorageException {
        OWLOntology o = getOntologyWithMissingDeclarations();
        OWLOntology reloaded = roundTrip(o, format);
        OWLObjectProperty op = df.getOWLObjectProperty(iri("testObjectProperty"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testAnnotationProperty"));
        Assert.assertTrue(reloaded.containsAxiom(df.getOWLDeclarationAxiom(ap)));
        Assert.assertTrue(reloaded.containsAxiom(df.getOWLDeclarationAxiom(op)));
    }

    @Test
    public void shouldNotAddDeclarationsForIllegalPunnings() throws Exception {
        if (OWLManager.DEBUG_USE_OWL) {
            testOWLAPI();
        } else {
            testONTAPI();
        }
    }

    private void testOWLAPI() throws Exception {
        OWLOntology o = getOntologyWithPunnedInvalidDeclarations();
        OWLOntology reloaded = roundTrip(o, format);
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testProperty"));
        OWLDeclarationAxiom ax = df.getOWLDeclarationAxiom(ap);
        Assert.assertFalse("ap testProperty should not have been declared", reloaded.containsAxiom(ax));
    }

    private void testONTAPI() throws Exception {
        OWLOntologyManager m = setupManager();
        OWLOntologyLoaderConfiguration conf = ((ru.avicomp.ontapi.config.OntLoaderConfiguration) m
                .getOntologyLoaderConfiguration())
                .setAllowReadDeclarations(false)
                .setPersonality(ru.avicomp.ontapi.jena.impl.conf.OntModelConfig.ONT_PERSONALITY_STRICT);
        m.setOntologyLoaderConfiguration(conf);

        OWLOntology o = m.createOntology(IRI.getNextDocumentIRI(uriBase));

        OWLObjectProperty op = df.getOWLObjectProperty(iri("testProperty"));
        OWLAnnotationProperty ap = df.getOWLAnnotationProperty(iri("testProperty"));
        OWLAnnotationAssertionAxiom assertion = df.getOWLAnnotationAssertionAxiom(iri("test"), df.getOWLAnnotation(ap, iri("otherTest")));

        o.add(df.getOWLDeclarationAxiom(op));
        o.add(df.getOWLTransitiveObjectPropertyAxiom(op));

        // ONT-API HACK: not even able to add illegal axioms:
        try {
            o.add(assertion);
            Assert.fail("The assetrtion succesfully added: " + assertion);
        } catch (ru.avicomp.ontapi.OntApiException e) {
            LOGGER.debug("Exception: {}", e);
            Throwable cause = e.getCause();
            if (cause instanceof ru.avicomp.ontapi.jena.OntJenaException) {
                LOGGER.debug("Cause: {}", cause);
                return;
            }
            throw e;
        }
    }

}
