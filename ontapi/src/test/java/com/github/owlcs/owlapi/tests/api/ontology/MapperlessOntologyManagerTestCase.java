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
package com.github.owlcs.owlapi.tests.api.ontology;

import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.HashSet;
import java.util.Optional;

/**
 * @author Matthew Horridge, The University of Manchester, Bio-Health Informatics Group
 */
public class MapperlessOntologyManagerTestCase extends TestBase {

    private static final IRI ONTOLOGY_IRI = IRI.create("http://test.com/", "ont");

    private OWLOntologyManager createManager() {
        m.getIRIMappers().clear();
        return m;
    }

    @Test
    public void testCreateOntologyWithIRI() throws OWLOntologyCreationException {
        OWLOntologyManager manager = createManager();
        OWLOntology ontology = manager.createOntology(ONTOLOGY_IRI);
        Assertions.assertEquals(ONTOLOGY_IRI, ontology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(ONTOLOGY_IRI, manager.getOntologyDocumentIRI(ontology));
    }

    @Test
    public void testCreateOntologyWithAxioms() throws OWLOntologyCreationException {
        OWLOntologyManager manager = createManager();
        OWLOntology ontology = manager.createOntology(new HashSet<>());
        Assertions.assertNotNull(manager.getOntologyDocumentIRI(ontology));
    }

    @Test
    public void testCreateOntologyWithAxiomsAndIRI() throws OWLOntologyCreationException {
        OWLOntologyManager manager = createManager();
        OWLOntology ontology = manager.createOntology(new HashSet<>(), ONTOLOGY_IRI);
        Assertions.assertEquals(ONTOLOGY_IRI, ontology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(ONTOLOGY_IRI, manager.getOntologyDocumentIRI(ontology));
    }

    @Test
    public void testCreateOntologyWithIdWithVersionIRI() throws OWLOntologyCreationException {
        OWLOntologyManager manager = createManager();
        IRI versionIRI = IRI.create("http://version/1", "");
        OWLOntologyID id = new OWLOntologyID(Optional.of(ONTOLOGY_IRI), Optional.of(versionIRI));
        OWLOntology ontology = manager.createOntology(id);
        Assertions.assertEquals(ONTOLOGY_IRI, ontology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(versionIRI, ontology.getOntologyID().getVersionIRI().orElse(null));
        Assertions.assertEquals(versionIRI, manager.getOntologyDocumentIRI(ontology));
    }

    @Test
    public void testCreateOntologyWithId() throws OWLOntologyCreationException {
        OWLOntologyManager manager = createManager();
        OWLOntologyID id = new OWLOntologyID(Optional.of(ONTOLOGY_IRI), Optional.empty());
        OWLOntology ontology = manager.createOntology(id);
        Assertions.assertEquals(ONTOLOGY_IRI, ontology.getOntologyID().getOntologyIRI().orElse(null));
        Assertions.assertEquals(ONTOLOGY_IRI, manager.getOntologyDocumentIRI(ontology));
    }
}
