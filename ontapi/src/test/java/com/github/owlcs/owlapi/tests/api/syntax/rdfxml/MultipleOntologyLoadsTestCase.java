/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
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
 * Tests the loading of a single ontology multiple times, using the same
 * ontologyIRI in the {@link OWLOntologyID} as that used in the actual ontology
 * that is being imported.
 *
 * @author Peter Ansell p_ansell@yahoo.com
 */
public class MultipleOntologyLoadsTestCase extends TestBase {

    private static final IRI CREATEV1 = OWLFunctionalSyntaxFactory.IRI("http://test.example.org/ontology/0139/version:1", "");
    private static final IRI CREATEV2 = OWLFunctionalSyntaxFactory.IRI("http://test.example.org/ontology/0139/version:2", "");
    private static final IRI CREATE0139 = OWLFunctionalSyntaxFactory.IRI("http://test.example.org/ontology/0139", "");

    @Test
    public void testMultipleVersionLoadChangeIRI() {
        Assertions.assertThrows(OWLOntologyAlreadyExistsException.class, this::multipleVersionLoadChangeIRI);
    }

    private void multipleVersionLoadChangeIRI() throws Throwable {
        // given
        OWLOntologyDocumentSource initialDocumentSource = getDocumentSource();
        OWLOntologyID expected = new OWLOntologyID(Optional.of(CREATE0139), Optional.of(CREATEV2));
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(Optional.of(CREATE0139), Optional.of(CREATEV2));
        OWLOntology initialOntology = getOWLOntology(initialUniqueOWLOntologyID);
        parseOnto(initialDocumentSource, initialOntology);
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(Optional.of(CREATE0139), Optional.of(CREATEV2));
        // when
        try {
            getOWLOntology(secondUniqueOWLOntologyID);
        } catch (OntApiException ex) {
            Throwable e = ex.getCause();
            Assertions.assertEquals(OWLOntologyAlreadyExistsException.class, e.getClass(), "Incorrect cause.");
            Assertions.assertEquals(expected, ((OWLOntologyAlreadyExistsException) e).getOntologyID(), "Incorrect ontology id.");
            throw e;
        }
    }

    @Test
    public void testMultipleVersionLoadNoChange() {
        Assertions.assertThrows(OWLOntologyAlreadyExistsException.class, this::multipleVersionLoadNoChange);
    }

    private void multipleVersionLoadNoChange() throws Throwable {
        // given
        OWLOntologyDocumentSource documentSource = getDocumentSource();
        OWLOntologyID expected = new OWLOntologyID(Optional.of(CREATE0139), Optional.of(CREATEV1));
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(Optional.of(CREATE0139), Optional.of(CREATEV1));
        OWLOntology initialOntology = getOWLOntology(initialUniqueOWLOntologyID);
        parseOnto(documentSource, initialOntology);
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(Optional.of(CREATE0139), Optional.of(CREATEV1));
        // when
        try {
            getOWLOntology(secondUniqueOWLOntologyID);
        } catch (OntApiException ex) {
            Throwable e = ex.getCause();
            Assertions.assertEquals(OWLOntologyAlreadyExistsException.class, e.getClass(), "Incorrect cause.");
            Assertions.assertEquals(expected, ((OWLOntologyAlreadyExistsException) e).getOntologyID(), "Incorrect ontology id.");
            throw e;
        }
    }

    @Test
    public void testMultipleVersionLoadsExplicitOntologyIDs() throws Exception {
        // given
        OWLOntologyDocumentSource documentSource = getDocumentSource();
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(Optional.of(CREATE0139), Optional.of(CREATEV1));
        OWLOntologyDocumentSource secondDocumentSource = getDocumentSource();
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(Optional.of(CREATE0139), Optional.of(CREATEV2));
        // when
        OWLOntology initialOntology = getOWLOntology(initialUniqueOWLOntologyID);
        parseOnto(documentSource, initialOntology);
        OWLOntology secondOntology = getOWLOntology(secondUniqueOWLOntologyID);
        parseOnto(secondDocumentSource, secondOntology);
        // then
        Assertions.assertEquals(CREATE0139, initialOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(CREATEV1, initialOntology.getOntologyID().getVersionIRI().orElse(null));
        Assertions.assertEquals(CREATE0139, secondOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(CREATEV2, secondOntology.getOntologyID().getVersionIRI().orElse(null));
    }

    @Test
    public void testMultipleVersionLoadsNoOntologyIDFirstTime() throws Exception {
        // given
        OWLOntologyDocumentSource documentSource = getDocumentSource();
        OWLOntologyDocumentSource secondDocumentSource = getDocumentSource();
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(Optional.of(CREATE0139), Optional.of(CREATEV2));
        // when
        OWLOntology initialOntology = getAnonymousOWLOntology();
        parseOnto(documentSource, initialOntology);
        OWLOntology secondOntology = getOWLOntology(secondUniqueOWLOntologyID);
        parseOnto(secondDocumentSource, secondOntology);
        // then
        Assertions.assertEquals(CREATE0139, initialOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(CREATEV1, initialOntology.getOntologyID().getVersionIRI().orElse(null));
        Assertions.assertEquals(CREATE0139, secondOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(CREATEV2, secondOntology.getOntologyID().getVersionIRI().orElse(null));
    }

    @Test
    public void testMultipleVersionLoadsNoOntologyVersionIRIFirstTime() throws Exception {
        // given
        OWLOntologyDocumentSource documentSource = getDocumentSource();
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(Optional.of(CREATE0139), Optional.empty());
        OWLOntologyDocumentSource secondDocumentSource = getDocumentSource();
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(Optional.of(CREATE0139), Optional.of(CREATEV2));
        // when
        OWLOntology initialOntology = getOWLOntology(initialUniqueOWLOntologyID);
        parseOnto(documentSource, initialOntology);
        OWLOntology secondOntology = getOWLOntology(secondUniqueOWLOntologyID);
        parseOnto(secondDocumentSource, secondOntology);
        // then
        Assertions.assertEquals(CREATE0139, initialOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(CREATEV1, initialOntology.getOntologyID().getVersionIRI().orElse(null));
        Assertions.assertEquals(CREATE0139, secondOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(CREATEV2, secondOntology.getOntologyID().getVersionIRI().orElse(null));
    }

    @Test
    public void testSingleVersionLoadChangeIRI() throws Exception {
        // given
        OWLOntologyDocumentSource secondDocumentSource = getDocumentSource();
        OWLOntologyID secondUniqueOWLOntologyID = new OWLOntologyID(Optional.of(CREATE0139), Optional.of(CREATEV2));
        // when
        OWLOntology secondOntology = getOWLOntology(secondUniqueOWLOntologyID);
        parseOnto(secondDocumentSource, secondOntology);
        // then
        Assertions.assertEquals(CREATE0139, secondOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(CREATEV2, secondOntology.getOntologyID().getVersionIRI().orElse(null));
    }

    @Test
    public void testSingleVersionLoadNoChange() throws Exception {
        // given
        OWLOntologyDocumentSource documentSource = getDocumentSource();
        OWLOntologyID initialUniqueOWLOntologyID = new OWLOntologyID(Optional.of(CREATE0139), Optional.of(CREATEV1));
        // when
        OWLOntology initialOntology = getOWLOntology(initialUniqueOWLOntologyID);
        parseOnto(documentSource, initialOntology);
        // then
        Assertions.assertEquals(CREATE0139, initialOntology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(CREATEV1, initialOntology.getOntologyID().getVersionIRI().orElse(null));
    }

    private void parseOnto(OWLOntologyDocumentSource initialDocumentSource, OWLOntology initialOntology) {
        OWLParser initialParser = new RDFXMLParser();
        initialParser.parse(initialDocumentSource, initialOntology, config);
    }

    private OWLOntologyDocumentSource getDocumentSource() {
        return new StreamDocumentSource(OWLIOUtils.openResourceStream("/owlapi/multipleOntologyLoadsTest.rdf"));
    }
}
