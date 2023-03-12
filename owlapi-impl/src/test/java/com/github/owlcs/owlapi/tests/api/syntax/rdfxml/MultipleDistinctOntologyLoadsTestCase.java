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
package com.github.owlcs.owlapi.tests.api.syntax.rdfxml;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.owlapi.OWLFunctionalSyntaxFactory;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.OWLParser;
import org.semanticweb.owlapi.io.StreamDocumentSource;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyAlreadyExistsException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.rdf.rdfxml.parser.RDFXMLParser;

import java.util.Optional;

/**
 * Tests the loading of a single ontology multiple times, using a different
 * ontologyIRI in the OWLOntologyID as that used in the actual ontology that is
 * being imported.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class MultipleDistinctOntologyLoadsTestCase extends TestBase {

    private static final IRI JB = OWLFunctionalSyntaxFactory.IRI("http://example.purl.org.au/domainontology/", "JB_000007");
    private static final IRI V_1 = OWLFunctionalSyntaxFactory.IRI("http://test.example.org/ontology/0139/", "version:1");
    private static final IRI V_2 = OWLFunctionalSyntaxFactory.IRI("http://test.example.org/ontology/0139/", "version:2");

    @Test
    public void testMultipleVersionLoadChangeIRI() {
        Assertions.assertThrows(OWLOntologyAlreadyExistsException.class, this::multipleVersionLoadChangeIRI);
    }

    private void multipleVersionLoadChangeIRI() throws Exception {
        OWLOntologyDocumentSource initialDocumentSource = getDocument();
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(Optional.of(JB), Optional.of(V_2));
        OWLOntology initialOntology = m.createOntology(initialUniqueOWLOntologyID);
        OWLParser initialParser = new RDFXMLParser();
        initialParser.parse(initialDocumentSource, initialOntology, config);
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(Optional.of(JB), Optional.of(V_2));
        try {
            m.createOntology(secondUniqueOWLOntologyID);
        } catch (OntApiException ex) {
            OWLOntologyAlreadyExistsException e = (OWLOntologyAlreadyExistsException) ex.getCause();
            Assertions.assertEquals(new OWLOntologyID(Optional.of(JB), Optional.of(V_2)), e.getOntologyID());
            throw e;
        }
    }

    private OWLOntologyDocumentSource getDocument() {
        return new StreamDocumentSource(OWLIOUtils.openResourceStream("/owlapi/multipleOntologyLoadsTest.rdf"));
    }

    @Test
    public void testMultipleVersionLoadNoChange() {
        Assertions.assertThrows(OWLOntologyAlreadyExistsException.class, this::multipleVersionLoadNoChange);
    }

    private void multipleVersionLoadNoChange() throws Exception {
        OWLOntologyDocumentSource documentSource = getDocument();
        OWLOntologyID expected = new OWLOntologyID(Optional.of(JB), Optional.of(V_1));
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(Optional.of(JB), Optional.of(V_1));
        OWLOntology initialOntology = m.createOntology(initialUniqueOWLOntologyID);
        OWLParser parser = new RDFXMLParser();
        parser.parse(documentSource, initialOntology, config);
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(Optional.of(JB), Optional.of(V_1));
        try {
            m.createOntology(secondUniqueOWLOntologyID);
        } catch (OntApiException ex) {
            OWLOntologyAlreadyExistsException e = (OWLOntologyAlreadyExistsException) ex.getCause();
            Assertions.assertEquals(expected, e.getOntologyID());
            throw e;
        }
    }

    @Test
    public void testMultipleVersionLoadsExplicitOntologyIDs() throws Exception {
        OWLOntologyDocumentSource documentSource = getDocument();
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(Optional.of(JB), Optional.of(V_1));
        OWLOntology initialOntology = m.createOntology(initialUniqueOWLOntologyID);
        OWLParser parser = new RDFXMLParser();
        parser.parse(documentSource, initialOntology, config);
        Assertions.assertEquals(JB, initialOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(V_1, initialOntology.getOntologyID().getVersionIRI().orElse(null));
        OWLOntologyDocumentSource secondDocumentSource = getDocument();
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(Optional.of(JB), Optional.of(V_2));
        OWLOntology secondOntology = m.createOntology(secondUniqueOWLOntologyID);
        OWLParser secondParser = new RDFXMLParser();
        secondParser.parse(secondDocumentSource, secondOntology, config);
        Assertions.assertEquals(JB, secondOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(V_2, secondOntology.getOntologyID().getVersionIRI().orElse(null));
    }

    @Test
    public void testMultipleVersionLoadsNoOntologyIDFirstTime() throws Exception {
        OWLOntologyDocumentSource documentSource = getDocument();
        OWLOntology initialOntology = m.createOntology();
        OWLParser parser = new RDFXMLParser();
        parser.parse(documentSource, initialOntology, config);
        Assertions.assertEquals(OWLFunctionalSyntaxFactory.IRI("http://test.example.org/ontology/0139", ""),
                initialOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(V_1, initialOntology.getOntologyID().getVersionIRI().orElse(null));
        OWLOntologyDocumentSource secondDocumentSource = getDocument();
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(Optional.of(JB), Optional.of(V_2));
        OWLOntology secondOntology = m.createOntology(secondUniqueOWLOntologyID);
        OWLParser secondParser = new RDFXMLParser();
        secondParser.parse(secondDocumentSource, secondOntology, config);
        Assertions.assertEquals(JB, secondOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(V_2, secondOntology.getOntologyID().getVersionIRI().orElse(null));
    }

    @Test
    public void testMultipleVersionLoadsNoOntologyVersionIRIFirstTime() throws Exception {
        OWLOntologyDocumentSource documentSource = getDocument();
        IRI iri = IRI.create("http://test.example.org/ontology/0139");
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(Optional.of(iri), Optional.empty());
        OWLOntology initialOntology = m.createOntology(initialUniqueOWLOntologyID);
        OWLParser parser = new RDFXMLParser();
        parser.parse(documentSource, initialOntology, config);
        Assertions.assertEquals(iri, initialOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(V_1, initialOntology.getOntologyID().getVersionIRI().orElse(null));
        OWLOntologyDocumentSource secondDocumentSource = getDocument();
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(Optional.of(JB), Optional.of(V_2));
        OWLOntology secondOntology = m.createOntology(secondUniqueOWLOntologyID);
        OWLParser secondParser = new RDFXMLParser();
        secondParser.parse(secondDocumentSource, secondOntology, config);
        Assertions.assertEquals(JB, secondOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(V_2, secondOntology.getOntologyID().getVersionIRI().orElse(null));
    }

    @Test
    public void testSingleVersionLoadChangeIRI() throws Exception {
        OWLOntologyDocumentSource secondDocumentSource = getDocument();
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(Optional.of(JB), Optional.of(V_2));
        OWLOntology secondOntology = m.createOntology(secondUniqueOWLOntologyID);
        OWLParser secondParser = new RDFXMLParser();
        // the following throws the exception
        secondParser.parse(secondDocumentSource, secondOntology, config);
        Assertions.assertEquals(JB, secondOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(V_2, secondOntology.getOntologyID().getVersionIRI().orElse(null));
    }

    @Test
    public void testSingleVersionLoadNoChange() throws Exception {
        OWLOntologyDocumentSource documentSource = getDocument();
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(Optional.of(JB), Optional.of(V_1));
        OWLOntology initialOntology = m.createOntology(initialUniqueOWLOntologyID);
        OWLParser parser = new RDFXMLParser();
        parser.parse(documentSource, initialOntology, config);
        Assertions.assertEquals(JB, initialOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(V_1, initialOntology.getOntologyID().getVersionIRI().orElse(null));
    }
}
