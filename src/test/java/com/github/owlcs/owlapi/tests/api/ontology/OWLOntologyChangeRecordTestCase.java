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
package com.github.owlcs.owlapi.tests.api.ontology;

import org.junit.Test;
import org.semanticweb.owlapi.change.AddAxiomData;
import org.semanticweb.owlapi.change.OWLOntologyChangeData;
import org.semanticweb.owlapi.change.OWLOntologyChangeRecord;
import org.semanticweb.owlapi.model.*;
import com.github.owlcs.owlapi.tests.api.baseclasses.TestBase;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics
 *         Research Group
 * @since 3.2.0
 */
@SuppressWarnings({"javadoc"})
public class OWLOntologyChangeRecordTestCase extends TestBase {

    private final
    @Nonnull
    OWLOntologyID mockOntologyID = new OWLOntologyID();
    private final
    @Nonnull
    OWLOntologyChangeData mockChangeData = mock(OWLOntologyChangeData.class);
    private final
    @Nonnull
    OWLAxiom mockAxiom = mock(OWLAxiom.class);

    @Test
    public void testEquals() {
        OWLOntologyChangeRecord record1 = new OWLOntologyChangeRecord(mockOntologyID, mockChangeData);
        OWLOntologyChangeRecord record2 = new OWLOntologyChangeRecord(mockOntologyID, mockChangeData);
        assertEquals(record1, record2);
    }

    @Test
    public void testGettersNotNull() {
        OWLOntologyChangeRecord record = new OWLOntologyChangeRecord(mockOntologyID, mockChangeData);
        assertNotNull(record.getOntologyID());
    }

    @Test
    public void testGetterEqual() {
        OWLOntologyChangeRecord record = new OWLOntologyChangeRecord(mockOntologyID, mockChangeData);
        assertEquals(mockOntologyID, record.getOntologyID());
        assertEquals(mockChangeData, record.getData());
    }

    @Test(expected = UnknownOWLOntologyException.class)
    public void testCreateOntologyChange() {
        OWLOntologyChangeRecord changeRecord = new OWLOntologyChangeRecord(mockOntologyID, mockChangeData);
        changeRecord.createOntologyChange(m);
    }

    @Test
    public void testCreateOntologyChangeEquals() throws OWLOntologyCreationException {
        OWLOntology ontology = m.createOntology();
        OWLOntologyID ontologyID = ontology.getOntologyID();
        AddAxiomData addAxiomData = new AddAxiomData(mockAxiom);
        OWLOntologyChangeRecord changeRecord = new OWLOntologyChangeRecord(ontologyID, addAxiomData);
        OWLOntologyChange change = changeRecord.createOntologyChange(m);
        assertNotNull(change);
        assertEquals(change.getOntology().getOntologyID(), ontologyID);
        assertEquals(mockAxiom, change.getAxiom());
    }
}
