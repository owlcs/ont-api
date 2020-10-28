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

package com.github.owlcs.ontapi.tests;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.tests.TestFactory.AxiomData;
import com.github.owlcs.ontapi.tests.TestFactory.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.semanticweb.owlapi.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

/**
 * Test for {@link DataFactory ONT-API Data Factory} functionality.
 * Also it compares the ONT-API and OWL-API implementations.
 * <p>
 * Created by @ssz on 11.09.2018.
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/contract/src/test/java/org/semanticweb/owlapi/api/test/OWLDataFactoryImplTestCase.java'>org.semanticweb.owlapi.api.test.OWLDataFactoryImplTestCase</a>
 */
public class DataFactoryTest {
    private static final OWLDataFactory OWL_DATA_FACTORY = new OntManagers.OWLAPIImplProfile().createDataFactory();
    private static final DataFactory ONT_DATA_FACTORY = OntManagers.getDataFactory();

    public static List<Data> getObjects() {
        return TestFactory.getObjects();
    }

    @ParameterizedTest
    @MethodSource("getObjects")
    public void testCreateAndValidate(Data data) {
        Class<? extends OWLObject> implType = data.getSuperImplClassType();
        OWLObject owl = data.create(OWL_DATA_FACTORY);
        Assertions.assertFalse(implType.isInstance(owl));
        OWLObject ont1 = data.create(ONT_DATA_FACTORY);
        Assertions.assertTrue(implType.isInstance(ont1));
        data.testCompare(owl, ont1);
        OWLObject ont2 = data.create(ONT_DATA_FACTORY);
        if (data.shouldBeSame()) {
            Assertions.assertSame(ont1, ont2);
            return;
        }
        data.testCompare(ont1, ont2);
    }

    @ParameterizedTest
    @MethodSource("getObjects")
    public void testBooleanProperties(Data data) {
        OWLObject test = data.create(ONT_DATA_FACTORY);
        OWLObject sample = data.create(OWL_DATA_FACTORY);
        checkBooleanProperties(data, sample);
        checkBooleanProperties(data, test);

        // NOTE: sometimes the properties below are contrary to common sense
        // (for example, OWLLiteral and SWRLVariable are anonymous),
        // but this is what OWL-API does, and we can't change that behaviour,
        // for more details see issue https://github.com/owlcs/owlapi/issues/867
        final boolean expectedAnonymous = sample.isAnonymous()
                || (sample instanceof OWLDataRange && !(sample instanceof OWLDatatype)); // todo: owlapi#867  (fixed in 5.1.12)
        final boolean expectedNamed = sample.isNamed()
                && (!(sample instanceof OWLDataRange) || (sample instanceof OWLDatatype)); // todo: owlapi#867 (fixed in 5.1.12)
        final boolean expectedAnonymousExpression = sample.isAnonymousExpression()
                || (sample instanceof OWLDataRange && !(sample instanceof OWLDatatype)); // todo: owlapi#867  (fixed in 5.1.12)
        final boolean expectedIndividual = sample.isIndividual();
        final boolean expectedAxiom = sample.isAxiom();
        final boolean expectedBottomEntity = sample.isBottomEntity();
        final boolean expectedTopEntity = sample.isTopEntity();

        Assertions.assertEquals(expectedAnonymous, test.isAnonymous(),
                "'" + test + "'#isAnonymous() must be " + expectedAnonymous);
        Assertions.assertEquals(expectedNamed, test.isNamed(), "'" + test + "'#isNamed() must be " + expectedNamed);

        Assertions.assertEquals(expectedIndividual, test.isIndividual(),
                "'" + test + "'#isIndividual() must be " + expectedIndividual);
        Assertions.assertEquals(expectedAxiom, test.isAxiom(),
                "'" + test + "'#isAxiom() must be " + expectedAxiom);

        Assertions.assertEquals(expectedAnonymousExpression, test.isAnonymousExpression(),
                "'" + test + "'#isAnonymousExpression() must be " + expectedAnonymousExpression);

        Assertions.assertFalse(test.isIRI(), "'" + test + "' must not be IRI");
        Assertions.assertFalse(test.isOntology(), "'" + test + "' must not be Ontology");

        Assertions.assertEquals(expectedBottomEntity, test.isBottomEntity(),
                "'" + test + "'#isBottomEntity() must be " + expectedBottomEntity);
        Assertions.assertEquals(expectedTopEntity, test.isTopEntity(),
                "'" + test + "'#isTopEntity() must be " + expectedTopEntity);
    }

    private void checkBooleanProperties(Data data, OWLObject object) {
        if (data.isEntity()) {
            Assertions.assertTrue(object instanceof OWLEntity);
            Assertions.assertEquals(data.shouldBeSame(), ((OWLEntity) object).isBuiltIn());
        } else {
            Assertions.assertFalse(object instanceof OWLEntity);
        }
        if (data.isAxiom()) {
            Assertions.assertTrue(object instanceof OWLAxiom);
            Assertions.assertEquals(((AxiomData) data).getType(), ((OWLAxiom) object).getAxiomType());
        } else {
            Assertions.assertFalse(object instanceof OWLAxiom);
        }
        Assertions.assertEquals(data.isClass(), object instanceof OWLClass);
        Assertions.assertEquals(data.isDatatype(), object instanceof OWLDatatype);
        Assertions.assertEquals(data.isAnnotationProperty(), object instanceof OWLAnnotationProperty);
        Assertions.assertEquals(data.isObjectProperty(), object instanceof OWLObjectProperty);
        Assertions.assertEquals(data.isDatatypeProperty(), object instanceof OWLDataProperty);
        Assertions.assertEquals(data.isIndividual(), object instanceof OWLIndividual);
        Assertions.assertEquals(data.isAnonymousIndividual(), object instanceof OWLAnonymousIndividual);
        Assertions.assertEquals(data.isAnonymousDataRange(),
                !(object instanceof OWLDatatype) && object instanceof OWLDataRange);
        Assertions.assertEquals(data.isAnonymousClassExpression(),
                !(object instanceof OWLClass) && object instanceof OWLClassExpression);
        Assertions.assertEquals(data.isLiteral(), object instanceof OWLLiteral);
        Assertions.assertEquals(data.isOWLAnnotation(), object instanceof OWLAnnotation);
        Assertions.assertEquals(data.isFacetRestriction(), object instanceof OWLFacetRestriction);
        Assertions.assertEquals(data.isSWRLVariable(), object instanceof SWRLVariable);
        Assertions.assertEquals(data.isSWRLIndividual(), object instanceof SWRLIndividualArgument);
        Assertions.assertEquals(data.isSWRLLiteral(), object instanceof SWRLLiteralArgument);
        Assertions.assertEquals(data.isSWRLAtom(), object instanceof SWRLAtom);
    }

    @ParameterizedTest
    @MethodSource("getObjects")
    public void testSerialization(Data data) throws Exception {
        OWLObject object = data.create(ONT_DATA_FACTORY);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream stream = new ObjectOutputStream(out);
        stream.writeObject(object);
        stream.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ObjectInputStream inStream = new ObjectInputStream(in);
        OWLObject copy = (OWLObject) inStream.readObject();
        data.testCompare(object, copy);
    }

}
