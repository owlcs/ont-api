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
package com.github.owlcs.owlapi.tests.api.dataproperties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.semanticweb.owlapi.change.AddAxiomData;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group
 */
public class AddAxiomDataTestCase {

    private final OWLAxiom mockAxiom = Mockito.mock(OWLAxiom.class);
    private final OWLOntology mockOntology = Mockito.mock(OWLOntology.class);

    @Test
    public void testEquals() {
        AddAxiomData data1 = new AddAxiomData(mockAxiom);
        AddAxiomData data2 = new AddAxiomData(mockAxiom);
        Assertions.assertEquals(data1, data2);
    }

    @Test
    public void testOntologyChange() {
        AddAxiomData data = new AddAxiomData(mockAxiom);
        AddAxiom change = data.createOntologyChange(mockOntology);
        Assertions.assertEquals(change.getOntology(), mockOntology);
        Assertions.assertEquals(change.getAxiom(), mockAxiom);
    }

    @Test
    public void testRoundTripChange() {
        AddAxiomData data = new AddAxiomData(mockAxiom);
        AddAxiom change = new AddAxiom(mockOntology, mockAxiom);
        Assertions.assertEquals(data, change.getChangeData());
    }
}
