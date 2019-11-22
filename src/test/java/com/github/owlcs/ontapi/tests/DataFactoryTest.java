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

package com.github.owlcs.ontapi.tests;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntManagers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
@RunWith(Parameterized.class)
public class DataFactoryTest extends TestFactory {
    private static final OWLDataFactory OWL_DATA_FACTORY = OntManagers.createOWLProfile().dataFactory();
    private static final DataFactory ONT_DATA_FACTORY = OntManagers.getDataFactory();

    private final Data data;

    public DataFactoryTest(Data data) {
        this.data = data;
    }

    @Test
    public void testCreateAndValidate() {
        Class<? extends OWLObject> implType = data.getSuperImplClassType();
        OWLObject owl = data.create(OWL_DATA_FACTORY);
        Assert.assertFalse(implType.isInstance(owl));
        OWLObject ont1 = data.create(ONT_DATA_FACTORY);
        Assert.assertTrue(implType.isInstance(ont1));
        data.testCompare(owl, ont1);
        OWLObject ont2 = data.create(ONT_DATA_FACTORY);
        if (data.shouldBeSame()) {
            Assert.assertSame(ont1, ont2);
            return;
        }
        data.testCompare(ont1, ont2);
    }

    @Test
    public void testBooleanProperties() {
        OWLObject test = data.create(ONT_DATA_FACTORY);
        OWLObject sample = data.create(OWL_DATA_FACTORY);
        checkBooleanProperties(sample);
        checkBooleanProperties(test);

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

        Assert.assertEquals("'" + test + "'#isAnonymous() must be " + expectedAnonymous,
                expectedAnonymous, test.isAnonymous());
        Assert.assertEquals("'" + test + "'#isNamed() must be " + expectedNamed, expectedNamed, test.isNamed());

        Assert.assertEquals("'" + test + "'#isIndividual() must be " + expectedIndividual,
                expectedIndividual, test.isIndividual());
        Assert.assertEquals("'" + test + "'#isAxiom() must be " + expectedAxiom, expectedAxiom, test.isAxiom());

        Assert.assertEquals("'" + test + "'#isAnonymousExpression() must be " + expectedAnonymousExpression,
                expectedAnonymousExpression, test.isAnonymousExpression());

        Assert.assertFalse("'" + test + "' must not be IRI", test.isIRI());
        Assert.assertFalse("'" + test + "' must not be Ontology", test.isOntology());

        Assert.assertEquals("'" + test + "'#isBottomEntity() must be " + expectedBottomEntity,
                expectedBottomEntity, test.isBottomEntity());
        Assert.assertEquals("'" + test + "'#isTopEntity() must be " + expectedTopEntity,
                expectedTopEntity, test.isTopEntity());
    }

    private void checkBooleanProperties(OWLObject object) {
        if (data.isEntity()) {
            Assert.assertTrue(object instanceof OWLEntity);
            Assert.assertEquals(data.shouldBeSame(), ((OWLEntity) object).isBuiltIn());
        } else {
            Assert.assertFalse(object instanceof OWLEntity);
        }
        if (data.isAxiom()) {
            Assert.assertTrue(object instanceof OWLAxiom);
            Assert.assertEquals(((AxiomData) data).getType(), ((OWLAxiom) object).getAxiomType());
        } else {
            Assert.assertFalse(object instanceof OWLAxiom);
        }
        Assert.assertEquals(data.isClass(), object instanceof OWLClass);
        Assert.assertEquals(data.isDatatype(), object instanceof OWLDatatype);
        Assert.assertEquals(data.isAnnotationProperty(), object instanceof OWLAnnotationProperty);
        Assert.assertEquals(data.isObjectProperty(), object instanceof OWLObjectProperty);
        Assert.assertEquals(data.isDatatypeProperty(), object instanceof OWLDataProperty);
        Assert.assertEquals(data.isIndividual(), object instanceof OWLIndividual);
        Assert.assertEquals(data.isAnonymousIndividual(), object instanceof OWLAnonymousIndividual);
        Assert.assertEquals(data.isAnonymousDataRange(),
                !(object instanceof OWLDatatype) && object instanceof OWLDataRange);
        Assert.assertEquals(data.isAnonymousClassExpression(),
                !(object instanceof OWLClass) && object instanceof OWLClassExpression);
        Assert.assertEquals(data.isLiteral(), object instanceof OWLLiteral);
        Assert.assertEquals(data.isOWLAnnotation(), object instanceof OWLAnnotation);
        Assert.assertEquals(data.isFacetRestriction(), object instanceof OWLFacetRestriction);
        Assert.assertEquals(data.isSWRLVariable(), object instanceof SWRLVariable);
        Assert.assertEquals(data.isSWRLIndividual(), object instanceof SWRLIndividualArgument);
        Assert.assertEquals(data.isSWRLLiteral(), object instanceof SWRLLiteralArgument);
        Assert.assertEquals(data.isSWRLAtom(), object instanceof SWRLAtom);
    }

    @Test
    public void testSerialization() throws Exception {
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

    @Parameterized.Parameters(name = "{0}")
    public static List<Data> getData() {
        return getObjects();
    }

}
