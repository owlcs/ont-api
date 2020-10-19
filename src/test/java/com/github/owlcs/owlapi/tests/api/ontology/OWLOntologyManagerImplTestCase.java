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
package com.github.owlcs.owlapi.tests.api.ontology;

import com.github.owlcs.owlapi.OWLManager;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.semanticweb.owlapi.model.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 */
public class OWLOntologyManagerImplTestCase extends TestBase {

    private OWLOntologyManager manager;

    @Before
    public void setUpManager() {
        manager = OWLManager.createOWLOntologyManager();
        //manager = new OWLOntologyManagerImpl(new OWLDataFactoryImpl(), new ReentrantReadWriteLock());
        //manager.getOntologyFactories().add(new OWLOntologyFactoryImpl((om, id) -> new OWLOntologyImpl(om, id)));
    }

    @Test
    public void testContains() throws OWLOntologyCreationException {
        OWLOntology ont = manager.createOntology(IRI.getNextDocumentIRI("urn:testontology"));
        Assertions.assertTrue(manager.contains(ont.getOntologyID()));
        Assertions.assertNotNull(manager.getOntology(ont.getOntologyID()));
        Assertions.assertTrue(manager.ontologies().anyMatch(ont::equals));
        Assertions.assertNotNull(manager.getOntologyDocumentIRI(ont));
        manager.removeOntology(ont);
        Assertions.assertFalse(manager.contains(ont.getOntologyID()));
    }

    @Test
    public void testImports() throws OWLOntologyCreationException {
        OWLOntology ontA = manager.createOntology(IRI.getNextDocumentIRI("urn:testontology"));
        OWLOntology ontB = manager.createOntology(IRI.getNextDocumentIRI("urn:testontology"));
        OWLImportsDeclaration decl = manager.getOWLDataFactory()
                .getOWLImportsDeclaration(ontB.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new));
        manager.applyChange(new AddImport(ontA, decl));
        Assertions.assertTrue(manager.directImports(ontA).anyMatch(ontB::equals));
        manager.removeOntology(ontB);
        Assertions.assertFalse(manager.directImports(ontA).anyMatch(ontB::equals));
    }

    @Test
    public void testImportsClosure() throws OWLException {
        // OntA -> OntB -> OntC (-> means imports)
        OWLOntology ontA = manager.createOntology(IRI.getNextDocumentIRI("urn:testontology"));
        OWLOntology ontB = manager.createOntology(IRI.getNextDocumentIRI("urn:testontology"));
        OWLOntology ontC = manager.createOntology(IRI.getNextDocumentIRI("urn:testontology"));
        OWLImportsDeclaration declA = manager.getOWLDataFactory()
                .getOWLImportsDeclaration(ontB.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new));
        OWLImportsDeclaration declB = manager.getOWLDataFactory()
                .getOWLImportsDeclaration(ontC.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new));
        manager.applyChanges(new AddImport(ontA, declA), new AddImport(ontB, declB));
        Assertions.assertTrue(manager.importsClosure(ontA).anyMatch(ontA::equals));
        Assertions.assertTrue(manager.importsClosure(ontA).anyMatch(ontB::equals));
        Assertions.assertTrue(manager.importsClosure(ontA).anyMatch(ontC::equals));
        Assertions.assertTrue(manager.importsClosure(ontB).anyMatch(ontB::equals));
        Assertions.assertTrue(manager.importsClosure(ontB).anyMatch(ontC::equals));
    }

    @Test
    public void testImportsLoad() throws OWLException {
        OWLOntology ontA = manager.createOntology(IRI.create("urn:test:", "a"));
        Assertions.assertEquals(0, ontA.directImports().count());
        IRI b = IRI.create("urn:test:", "b");
        OWLImportsDeclaration declB = manager.getOWLDataFactory().getOWLImportsDeclaration(b);
        manager.applyChange(new AddImport(ontA, declB));
        Set<IRI> directImportsDocuments = ontA.directImportsDocuments().collect(Collectors.toSet());
        Assertions.assertEquals(1, directImportsDocuments.size());
        Assertions.assertTrue(directImportsDocuments.contains(b));
        OWLOntology ontB = manager.createOntology(b);
        directImportsDocuments = ontA.directImportsDocuments().collect(Collectors.toSet());
        Assertions.assertEquals(1, directImportsDocuments.size());
        Assertions.assertTrue(directImportsDocuments.contains(b));
        Assertions.assertEquals(1, ontA.directImports().count());
        Assertions.assertTrue(ontA.directImports().anyMatch(ontB::equals));
    }
}
