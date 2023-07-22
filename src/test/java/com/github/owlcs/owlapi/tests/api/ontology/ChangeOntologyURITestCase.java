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
package com.github.owlcs.owlapi.tests.api.ontology;

import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.util.OWLOntologyIRIChanger;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 */
public class ChangeOntologyURITestCase extends TestBase {

    @Test
    public void testChangeURI() throws OWLOntologyCreationException {
        IRI oldIRI = IRI.create("http://www.semanticweb.org/ontologies/", "ontA");
        IRI newIRI = IRI.create("http://www.semanticweb.org/ontologies/", "ontB");
        OWLOntology ont = m.createOntology(oldIRI);
        OWLOntology importingOnt = m.createOntology(IRI.create("http://www.semanticweb.org/ontologies/", "ontC"));
        m.applyChange(new AddImport(importingOnt, df.getOWLImportsDeclaration(ont.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new))));
        Assertions.assertTrue(m.contains(oldIRI));
        LOGGER.debug("Ontology before renaming:");
        OWLIOUtils.print(importingOnt);
        OWLOntologyIRIChanger changer = new OWLOntologyIRIChanger(m);
        m.applyChanges(changer.getChanges(ont, newIRI));
        LOGGER.debug("Ontology after renaming:");
        OWLIOUtils.print(importingOnt);
        Set<IRI> imports = importingOnt.importsDeclarations().map(OWLImportsDeclaration::getIRI).collect(Collectors.toSet());
        LOGGER.debug("Imports : " + imports);

        Assertions.assertFalse(m.contains(oldIRI));
        Assertions.assertTrue(m.contains(newIRI));
        Assertions.assertTrue(m.ontologies().anyMatch(o -> o.equals(ont)));
        Assertions.assertTrue(m.directImports(importingOnt).anyMatch(o -> o.equals(ont)));

        Assertions.assertTrue(imports.contains(newIRI), "Can't find " + newIRI + " inside " + importingOnt.getOntologyID());
        Assertions.assertFalse(imports.contains(oldIRI), "There is " + oldIRI + " inside " + importingOnt.getOntologyID());

        OWLOntology ontology = m.getOntology(newIRI);
        Assertions.assertNotNull(ontology);
        Assertions.assertEquals(ontology, ont);
        //noinspection OptionalGetWithoutIsPresent
        Assertions.assertEquals(ontology.getOntologyID().getOntologyIRI().get(), newIRI);
        Assertions.assertTrue(m.importsClosure(importingOnt).anyMatch(o -> o.equals(ont)));
        Assertions.assertNotNull(m.getOntologyDocumentIRI(ont), "ontology should not be null");
        // Document IRI will still be the same (in this case the old ont URI)
        Assertions.assertEquals(m.getOntologyDocumentIRI(ont), oldIRI);
        Assertions.assertNotNull(ont.getFormat(), "ontology format should not be null");
    }

    @Test
    public void testShouldCheckContents() throws OWLOntologyCreationException {
        m.createOntology(IRI.create("http://www.test.com/", "123"));
        OWLOntologyID anonymousId = m1.createOntology().getOntologyID();
        m.contains(anonymousId);
    }
}
