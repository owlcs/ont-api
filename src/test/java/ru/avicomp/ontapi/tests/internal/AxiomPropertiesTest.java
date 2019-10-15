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
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.tests.TestFactory;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by @ssz on 15.10.2019.
 */
public class AxiomPropertiesTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(AxiomPropertiesTest.class);

    @Test
    public void testSubClassOf() {
        List<TestFactory.AxiomData> data = TestFactory.getObjects().stream().filter(TestFactory.Data::isAxiom)
                .map(x -> (TestFactory.AxiomData) x).filter(x -> AxiomType.SUBCLASS_OF.equals(x.getType()))
                .collect(Collectors.toList());
        testAxiom(data, OWLSubClassOfAxiom::isGCI);
    }

    @Test
    public void testPropertyChains() {
        List<TestFactory.AxiomData> data = TestFactory.getObjects().stream().filter(TestFactory.Data::isAxiom)
                .map(x -> (TestFactory.AxiomData) x).filter(x -> AxiomType.SUB_PROPERTY_CHAIN_OF.equals(x.getType()))
                .collect(Collectors.toList());
        testAxiom(data, OWLSubPropertyChainOfAxiom::isEncodingOfTransitiveProperty);
    }

    @SuppressWarnings("unchecked")
    private static <X extends OWLAxiom> void testAxiom(List<TestFactory.AxiomData> data, Predicate<X> property) {
        Assert.assertFalse(data.isEmpty());
        for (TestFactory.AxiomData a : data) {
            LOGGER.debug("Test properties for '{}'", a);
            X owl = (X) a.create(ObjectFactoryTestBase.OWL_DATA_FACTORY);
            X ont = (X) a.create(ObjectFactoryTestBase.ONT_DATA_FACTORY);
            X res = (X) CommonAxiomsTest.createONTObject(OntManagers.createONT(), owl);
            Assert.assertEquals(owl, res);
            boolean expected = property.test(owl);
            Assert.assertEquals(expected, property.test(ont));
            Assert.assertEquals(expected, property.test(res));
        }
    }
}
