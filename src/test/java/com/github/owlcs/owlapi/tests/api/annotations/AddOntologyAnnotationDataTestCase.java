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
package com.github.owlcs.owlapi.tests.api.annotations;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.semanticweb.owlapi.change.AddOntologyAnnotationData;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author Matthew Horridge, Stanford University, Bio-Medical Informatics Research Group
 */
public class AddOntologyAnnotationDataTestCase {

    private final OWLAnnotation mockAnnotation = Mockito.mock(OWLAnnotation.class);
    private final OWLOntology mockOntology = Mockito.mock(OWLOntology.class);

    private AddOntologyAnnotationData createData() {
        return new AddOntologyAnnotationData(mockAnnotation);
    }

    @Test
    public void testEquals() {
        AddOntologyAnnotationData data1 = createData();
        AddOntologyAnnotationData data2 = createData();
        Assertions.assertEquals(data1, data2);
        Assertions.assertEquals(data1.hashCode(), data2.hashCode());
    }

    @Test
    public void testGettersReturnNotNull() {
        AddOntologyAnnotationData data = createData();
        Assertions.assertNotNull(data.getAnnotation());
        Assertions.assertNotNull(data.createOntologyChange(mockOntology));
    }

    @Test
    public void testGettersEquals() {
        AddOntologyAnnotationData data = createData();
        Assertions.assertEquals(mockAnnotation, data.getAnnotation());
    }

    @Test
    public void testCreateOntologyChange() {
        AddOntologyAnnotationData data = createData();
        AddOntologyAnnotation change = data.createOntologyChange(mockOntology);
        Assertions.assertEquals(mockOntology, change.getOntology());
        Assertions.assertEquals(mockAnnotation, change.getAnnotation());
    }

    @Test
    public void testOntologyChangeSymmetry() {
        AddOntologyAnnotationData data = createData();
        AddOntologyAnnotation change = new AddOntologyAnnotation(mockOntology, mockAnnotation);
        Assertions.assertEquals(change.getChangeData(), data);
    }
}
