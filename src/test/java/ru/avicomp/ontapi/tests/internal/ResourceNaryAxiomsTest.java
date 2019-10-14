/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.tests.internal;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNaryAxiom;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.internal.objects.ModelObject;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by @ssz on 14.10.2019.
 */
public class ResourceNaryAxiomsTest extends NaryAxiomsTestBase {

    public ResourceNaryAxiomsTest(Data data) {
        super(data);
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Data> getData() {
        return getObjects().stream()
                .filter(Data::isAxiom)
                // TODO: see https://github.com/avicomp/ont-api/issues/87
                .filter(x -> isOneOf(x
                        , AxiomType.DISJOINT_CLASSES))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testONTObject() {
        OWLNaryAxiom owl = (OWLNaryAxiom) data.create(OWL_DATA_FACTORY);
        LOGGER.debug("Test: '{}'", owl);
        OWLNaryAxiom ont = (OWLNaryAxiom) data.create(ONT_DATA_FACTORY);

        Assert.assertTrue(ont.getClass().getName().startsWith("ru.avicomp.ontapi.owlapi"));
        Assert.assertTrue(owl.getClass().getName().startsWith("uk.ac.manchester.cs.owl.owlapi"));

        Collection<? extends OWLAxiom> expectedPairwise = owl.asPairwiseAxioms();
        Collection<? extends OWLAxiom> testPairwise = ont.asPairwiseAxioms();
        Assert.assertEquals(expectedPairwise, testPairwise);

        OWLAxiom test = createONTObject(OntManagers.createONT(), owl);

        Assert.assertTrue(test instanceof ModelObject);
        testONTObject(owl, ont, test);
    }

}
