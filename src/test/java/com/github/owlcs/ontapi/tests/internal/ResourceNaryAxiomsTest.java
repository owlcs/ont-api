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

package com.github.owlcs.ontapi.tests.internal;

import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.internal.objects.ModelObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNaryAxiom;

import java.util.Collection;
import java.util.List;

/**
 * Created by @ssz on 14.10.2019.
 */
public class ResourceNaryAxiomsTest extends NaryAxiomsTestBase {

    public ResourceNaryAxiomsTest(Data data) {
        super(data);
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<AxiomData> getData() {
        return getAxiomData(
                AxiomType.DISJOINT_CLASSES
                , AxiomType.DIFFERENT_INDIVIDUALS
                , AxiomType.DISJOINT_OBJECT_PROPERTIES
                , AxiomType.DISJOINT_DATA_PROPERTIES
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testONTObject() {
        OWLNaryAxiom owl = (OWLNaryAxiom) data.create(OWL_DATA_FACTORY);
        LOGGER.debug("Test: '{}'", owl);
        OWLNaryAxiom ont = (OWLNaryAxiom) data.create(ONT_DATA_FACTORY);

        Assert.assertTrue(isONT(ont));
        Assert.assertTrue(isOWL(owl));

        Collection<? extends OWLAxiom> expectedPairwise = owl.asPairwiseAxioms();
        Collection<? extends OWLAxiom> testPairwise = ont.asPairwiseAxioms();
        Assert.assertEquals(expectedPairwise, testPairwise);

        OWLAxiom test = createONTObject(OntManagers.createONT(), owl);

        Assert.assertTrue(test instanceof ModelObject);
        testONTObject(owl, ont, test);
    }

}
