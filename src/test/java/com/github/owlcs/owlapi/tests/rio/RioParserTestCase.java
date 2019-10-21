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

/**
 *
 */
package com.github.owlcs.owlapi.tests.rio;

import org.junit.Before;
import org.junit.Test;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RioRDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RioRDFXMLDocumentFormatFactory;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParser;
import org.semanticweb.owlapi.rio.RioParserImpl;
import org.semanticweb.owlapi.rio.RioRDFXMLParserFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 */
@SuppressWarnings({"javadoc"})
public class RioParserTestCase extends TestBase {

    @Before
    public void setUpManager() {
        // Use non-Rio storers
        // limit to Rio parsers for RioParserImpl Test
        // testManager = OWLOntologyManagerFactoryRegistry
        // .createOWLOntologyManager(
        // OWLOntologyManagerFactoryRegistry.getOWLDataFactory(),
        // storerRegistry, parserRegistry);
        m.getOntologyParsers().set(new RioRDFXMLParserFactory());
        // testOntologyKoala =
        // testManager.loadOntologyFromOntologyDocument(this.getClass().getResourceAsStream("/koala.owl"));
    }

    /*
     * Test method for
     * {@link org.semanticweb.owlapi.rio.RioParserImpl#parse(org.semanticweb.owlapi.io.OWLOntologyDocumentSource, org.semanticweb.owlapi.model.OWLOntology)}
     */
    @Test
    public void testParse() throws Exception {
        OWLOntology owlapiOntologyPrimer = getAnonymousOWLOntology();
        RDFXMLParser owlapiParser = new RDFXMLParser();
        OWLDocumentFormat owlapiOntologyFormat = owlapiParser.parse(getStream("/owlapi/koala.owl"), owlapiOntologyPrimer,
                config);
        assertEquals(70, owlapiOntologyPrimer.getAxiomCount());
        assertEquals(new RDFXMLDocumentFormat(), owlapiOntologyFormat);
        RioParserImpl rioParser = new RioParserImpl(new RioRDFXMLDocumentFormatFactory());
        // OWLOntology ontology = OWLOntologyManagerFactoryRegistry
        // .createOWLOntologyManager().createOntology(
        OWLOntology ontology = m1.createOntology(IRI.create("http://protege.stanford.edu/plugins/owl/owl-library/",
                "koala.owl"));
        OWLDocumentFormat rioOntologyFormat = rioParser.parse(getStream("/owlapi/koala.owl"), ontology, config);
        assertEquals(new RioRDFXMLDocumentFormat(), rioOntologyFormat);
        equal(owlapiOntologyPrimer, ontology);
        assertEquals(70, ontology.getAxiomCount());
    }

    /*
     * Test method for
     * {@link org.semanticweb.owlapi.rio.RioParserImpl#parse(org.semanticweb.owlapi.io.OWLOntologyDocumentSource, org.semanticweb.owlapi.model.OWLOntology)}
     */
    @Test
    public void testParsePrimer() throws Exception {
        OWLOntology owlapiOntologyPrimer = getAnonymousOWLOntology();
        RDFXMLParser owlapiParser = new RDFXMLParser();
        OWLDocumentFormat owlapiOntologyFormat = owlapiParser.parse(getStream("/owlapi/primer.rdfxml.xml"),
                owlapiOntologyPrimer, config);
        assertEquals(93, owlapiOntologyPrimer.getAxiomCount());
        assertEquals(new RDFXMLDocumentFormat(), owlapiOntologyFormat);
        RioParserImpl rioParser = new RioParserImpl(new RioRDFXMLDocumentFormatFactory());
        // OWLOntology rioOntologyPrimer = OWLOntologyManagerFactoryRegistry
        // .createOWLOntologyManager()
        OWLOntology rioOntologyPrimer = m1.createOntology(IRI.create("http://example.com/owl/", "families"));
        OWLDocumentFormat rioOntologyFormat = rioParser.parse(getStream("/owlapi/primer.rdfxml.xml"), rioOntologyPrimer,
                config);
        assertEquals(new RioRDFXMLDocumentFormat(), rioOntologyFormat);
        equal(owlapiOntologyPrimer, rioOntologyPrimer);
        assertEquals(93, rioOntologyPrimer.getAxiomCount());
    }

    /**
     * @return stream
     */
    StreamDocumentSource getStream(String name) {
        return new StreamDocumentSource(getClass().getResourceAsStream(name));
    }

    /*
     * Test method for
     * {@link org.semanticweb.owlapi.rio.RioParserImpl#parse(org.semanticweb.owlapi.io.OWLOntologyDocumentSource, org.semanticweb.owlapi.model.OWLOntology)}
     */
    @Test
    public void testParsePrimerSubset() throws Exception {
        // XXX this test does not work yet
        // output:
        // Rio:
        // DatatypeDefinition(<http://example.com/owl/families/majorAge>
        // DataIntersectionOf(<http://org.semanticweb.owlapi/error#Error1>
        // DataComplementOf(<http://example.com/owl/families/minorAge>) ))
        // OWLAPI:
        // DatatypeDefinition(<http://example.com/owl/families/majorAge>
        // DataIntersectionOf(<http://example.com/owl/families/personAge>
        // DataComplementOf(<http://example.com/owl/families/minorAge>) ))]
        OWLOntology owlapiOntologyPrimer = getAnonymousOWLOntology();
        RDFXMLParser owlapiParser = new RDFXMLParser();
        OWLDocumentFormat owlapiOntologyFormat = owlapiParser.parse(getStream("/owlapi/rioParserTest1-minimal.rdf"),
                owlapiOntologyPrimer, config);
        assertEquals(4, owlapiOntologyPrimer.getAxiomCount());
        assertEquals(new RDFXMLDocumentFormat(), owlapiOntologyFormat);
        RioParserImpl rioParser = new RioParserImpl(new RioRDFXMLDocumentFormatFactory());
        // OWLOntology rioOntologyPrimer = OWLOntologyManagerFactoryRegistry
        // .createOWLOntologyManager().createOntology(
        OWLOntology rioOntologyPrimer = m1.createOntology(IRI.create("http://example.com/owl/", "families"));
        OWLDocumentFormat rioOntologyFormat = rioParser.parse(getStream("/owlapi/rioParserTest1-minimal.rdf"),
                rioOntologyPrimer, config);
        assertEquals(new RioRDFXMLDocumentFormat(), rioOntologyFormat);
        equal(owlapiOntologyPrimer, rioOntologyPrimer);
        assertEquals(4, rioOntologyPrimer.getAxiomCount());
    }

    /*
     * Test method for
     * {@link org.semanticweb.owlapi.rio.RioParserImpl#parse(org.semanticweb.owlapi.io.OWLOntologyDocumentSource, org.semanticweb.owlapi.model.OWLOntology)}
     */
    @Test
    public void testParsePrimerMinimalSubset() throws Exception {
        OWLOntology owlapiOntologyPrimer = getAnonymousOWLOntology();
        RDFXMLParser owlapiParser = new RDFXMLParser();
        OWLDocumentFormat owlapiOntologyFormat = owlapiParser.parse(getStream("/owlapi/rioParserTest1-minimal.rdf"),
                owlapiOntologyPrimer, config);
        assertEquals(4, owlapiOntologyPrimer.getAxiomCount());
        assertEquals(new RDFXMLDocumentFormat(), owlapiOntologyFormat);
        RioParserImpl rioParser = new RioParserImpl(new RioRDFXMLDocumentFormatFactory());
        // OWLOntology rioOntologyPrimer = OWLOntologyManagerFactoryRegistry
        // .createOWLOntologyManager().createOntology(
        OWLOntology rioOntologyPrimer = m1.createOntology(IRI.create("http://example.com/owl/", "families"));
        OWLDocumentFormat rioOntologyFormat = rioParser.parse(getStream("/owlapi/rioParserTest1-minimal.rdf"),
                rioOntologyPrimer, config);
        assertEquals(new RioRDFXMLDocumentFormat(), rioOntologyFormat);
        equal(owlapiOntologyPrimer, rioOntologyPrimer);
        assertEquals(4, rioOntologyPrimer.getAxiomCount());
    }
}
