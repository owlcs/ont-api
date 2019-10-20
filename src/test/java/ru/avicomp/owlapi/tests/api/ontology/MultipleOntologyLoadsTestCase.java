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
package ru.avicomp.owlapi.tests.api.ontology;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.io.OWLParser;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParser;
import ru.avicomp.owlapi.OWLManager;
import ru.avicomp.owlapi.tests.api.baseclasses.TestBase;

import java.util.Optional;


/**
 * @author Peter Ansell p_ansell@yahoo.com
 */

@SuppressWarnings("ALL")
public class MultipleOntologyLoadsTestCase extends TestBase {

    private final Optional<IRI> v2 = Optional.ofNullable(
            IRI.getNextDocumentIRI("http://test.example.org/ontology/0139/version:2"));
    private final Optional<IRI> v1 = Optional.ofNullable(
            IRI.getNextDocumentIRI("http://test.example.org/ontology/0139/version:1"));
    private final Optional<IRI> i139 = Optional.ofNullable(
            IRI.getNextDocumentIRI("http://test.example.org/ontology/0139"));
    private final String INPUT = "<?xml version=\"1.0\"?>\n" + "<rdf:RDF\n"
            + "    xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"\n"
            + "    xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
            + "    xmlns:owl=\"http://www.w3.org/2002/07/owl#\">\n" + "  <rdf:Description rdf:about=\""
            + i139.get().toString() + "\">\n"
            + "    <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Ontology\" />\n"
            + "    <owl:versionIRI rdf:resource=\"" + v1.get().toString() + "\" />\n" + "  </rdf:Description>  \n"
            + "</rdf:RDF>";

    @Test
    public void testMultipleVersionLoadChangeIRI() throws Throwable {
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(i139, v2);
        OWLOntology initialOntology = getOWLOntology(initialUniqueOWLOntologyID);
        OWLParser initialParser = new RDFXMLParser();
        initialParser.parse(new StringDocumentSource(INPUT), initialOntology, config);
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(i139, v2);
        try {
            getOWLOntology(secondUniqueOWLOntologyID);
            Assert.fail("Did not receive expected OWLOntologyDocumentAlreadyExistsException");
        } catch (ru.avicomp.ontapi.OntApiException ex) {
            Assert.assertFalse(OWLManager.DEBUG_USE_OWL);
            Throwable e = ex.getCause();
            Assert.assertEquals(OWLOntologyAlreadyExistsException.class, e.getClass());
            Assert.assertEquals(new OWLOntologyID(i139, v2), ((OWLOntologyAlreadyExistsException) e).getOntologyID());
        } catch (OWLOntologyAlreadyExistsException e) {
            Assert.assertTrue(OWLManager.DEBUG_USE_OWL);
            Assert.assertEquals(new OWLOntologyID(i139, v2), e.getOntologyID());
        }
    }

    @Test
    public void testMultipleVersionLoadNoChange() throws Throwable {
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(i139, v1);
        OWLOntology initialOntology = getOWLOntology(initialUniqueOWLOntologyID);
        OWLParser parser = new RDFXMLParser();
        parser.parse(new StringDocumentSource(INPUT), initialOntology, config);
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(i139, v1);
        try {
            getOWLOntology(secondUniqueOWLOntologyID);
            Assert.fail("Did not receive expected OWLOntologyAlreadyExistsException");
        } catch (ru.avicomp.ontapi.OntApiException ex) {
            Assert.assertFalse(OWLManager.DEBUG_USE_OWL);
            Throwable e = ex.getCause();
            Assert.assertEquals(OWLOntologyAlreadyExistsException.class, e.getClass());
            Assert.assertEquals(new OWLOntologyID(i139, v1), ((OWLOntologyAlreadyExistsException) e).getOntologyID());
        } catch (OWLOntologyAlreadyExistsException e) {
            Assert.assertTrue(OWLManager.DEBUG_USE_OWL);
            Assert.assertEquals(new OWLOntologyID(i139, v1), e.getOntologyID());
        }
    }

    @Test
    public void testMultipleVersionLoadsExplicitOntologyIDs() throws Exception {
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(i139, v1);
        OWLOntology initialOntology = getOWLOntology(initialUniqueOWLOntologyID);
        OWLParser parser = new RDFXMLParser();
        parser.parse(new StringDocumentSource(INPUT), initialOntology, config);
        Assert.assertEquals(i139, initialOntology.getOntologyID().getOntologyIRI());
        Assert.assertEquals(v1, initialOntology.getOntologyID().getVersionIRI());
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(i139, v2);
        OWLOntology secondOntology = getOWLOntology(secondUniqueOWLOntologyID);
        OWLParser secondParser = new RDFXMLParser();
        secondParser.parse(new StringDocumentSource(INPUT), secondOntology, config);
        Assert.assertEquals(i139, secondOntology.getOntologyID().getOntologyIRI());
        Assert.assertEquals(v2, secondOntology.getOntologyID().getVersionIRI());
    }

    @Test
    public void testMultipleVersionLoadsNoOntologyIDFirstTime() throws Exception {
        OWLOntology initialOntology = getAnonymousOWLOntology();
        OWLParser parser = new RDFXMLParser();
        parser.parse(new StringDocumentSource(INPUT), initialOntology, config);
        Assert.assertEquals(i139, initialOntology.getOntologyID().getOntologyIRI());
        Assert.assertEquals(v1, initialOntology.getOntologyID().getVersionIRI());
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(i139, v2);
        OWLOntology secondOntology = getOWLOntology(secondUniqueOWLOntologyID);
        OWLParser secondParser = new RDFXMLParser();
        secondParser.parse(new StringDocumentSource(INPUT), secondOntology, config);
        Assert.assertEquals(i139, secondOntology.getOntologyID().getOntologyIRI());
        Assert.assertEquals(v2, secondOntology.getOntologyID().getVersionIRI());
    }

    @Test
    public void testMultipleVersionLoadsNoOntologyVersionIRIFirstTime() throws Exception {
        Optional<IRI> empty = Optional.empty();
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(i139, empty);
        OWLOntology initialOntology = getOWLOntology(initialUniqueOWLOntologyID);
        OWLParser parser = new RDFXMLParser();
        parser.parse(new StringDocumentSource(INPUT), initialOntology, config);
        Assert.assertEquals(i139, initialOntology.getOntologyID().getOntologyIRI());
        Assert.assertEquals(v1, initialOntology.getOntologyID().getVersionIRI());
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(i139, v2);
        OWLOntology secondOntology = getOWLOntology(secondUniqueOWLOntologyID);
        OWLParser secondParser = new RDFXMLParser();
        secondParser.parse(new StringDocumentSource(INPUT), secondOntology, config);
        Assert.assertEquals(i139, secondOntology.getOntologyID().getOntologyIRI());
        Assert.assertEquals(v2, secondOntology.getOntologyID().getVersionIRI());
    }

    @Test
    public void testSingleVersionLoadChangeIRI() throws Exception {
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(i139, v2);
        OWLOntology secondOntology = getOWLOntology(secondUniqueOWLOntologyID);
        OWLParser secondParser = new RDFXMLParser();
        // the following throws the exception
        secondParser.parse(new StringDocumentSource(INPUT), secondOntology, config);
        Assert.assertEquals(i139, secondOntology.getOntologyID().getOntologyIRI());
        Assert.assertEquals(v2, secondOntology.getOntologyID().getVersionIRI());
    }

    @Test
    public void testSingleVersionLoadNoChange() throws Exception {
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(i139, v1);
        OWLOntology initialOntology = getOWLOntology(initialUniqueOWLOntologyID);
        OWLParser parser = new RDFXMLParser();
        parser.parse(new StringDocumentSource(INPUT), initialOntology, config);
        Assert.assertEquals(i139, initialOntology.getOntologyID().getOntologyIRI());
        Assert.assertEquals(v1, initialOntology.getOntologyID().getVersionIRI());
    }
}
