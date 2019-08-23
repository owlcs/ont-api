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

package ru.avicomp.ontapi.tests;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

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

    private static boolean isSameIRI(Resource e, OWLObject a) {
        return e.getURI().equals(((OWLEntity) a).toStringID());
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
        OWLObject object = data.create(ONT_DATA_FACTORY);
        final boolean expectedAnonymousExpression = data.isAnonymousClassExpression()
                || data.isAnonymousDataRange() || data.isAnonymousProperty();
        Assert.assertEquals(data.isAnonymousDataRange(), object instanceof OWLDataRange && !(object instanceof OWLDatatype));
        Assert.assertEquals(data.isAnonymousClassExpression(), object instanceof OWLAnonymousClassExpression);
        final boolean expectedAnonymous = data.isAxiom() || data.isAnonymousIndividual() || expectedAnonymousExpression;
        // todo: the following is wrong, but it is what OWL-API does. see https://github.com/owlcs/owlapi/issues/867
        final boolean expectedNamed = !expectedAnonymous; //data.isEntity() || data.isOWLAnnotation();
        final boolean expectedBottomEntity;
        final boolean expectedTopEntity;
        if (data.isEntity()) {
            Assert.assertTrue(object instanceof OWLEntity);
            Assert.assertEquals(data.shouldBeSame(), ((OWLEntity) object).isBuiltIn());
            if (data.isClass()) {
                Assert.assertTrue(object instanceof OWLClass);
            } else {
                Assert.assertFalse(object instanceof OWLClass);
            }
            if (data.isDatatype()) {
                Assert.assertTrue(object instanceof OWLDatatype);
            } else {
                Assert.assertFalse(object instanceof OWLDatatype);
            }
            if (data.isAnnotationProperty()) {
                Assert.assertTrue(object instanceof OWLAnnotationProperty);
            } else {
                Assert.assertFalse(object instanceof OWLAnnotationProperty);
            }
            if (data.isObjectProperty()) {
                Assert.assertTrue(object instanceof OWLObjectProperty);
            } else {
                Assert.assertFalse(object instanceof OWLObjectProperty);
            }
            if (data.isDatatypeProperty()) {
                Assert.assertTrue(object instanceof OWLDataProperty);
            } else {
                Assert.assertFalse(object instanceof OWLDataProperty);
            }
            if (data.isClass()) {
                expectedBottomEntity = isSameIRI(OWL.Nothing, object);
                expectedTopEntity = isSameIRI(OWL.Thing, object);
            } else if (data.isDatatypeProperty()) {
                expectedBottomEntity = isSameIRI(OWL.bottomDataProperty, object);
                expectedTopEntity = isSameIRI(OWL.topDataProperty, object);
            } else if (data.isObjectProperty()) {
                expectedBottomEntity = isSameIRI(OWL.bottomObjectProperty, object);
                expectedTopEntity = isSameIRI(OWL.topObjectProperty, object);
            } else if (data.isDatatype()) {
                expectedTopEntity = isSameIRI(RDFS.Literal, object);
                expectedBottomEntity = false;
            } else {
                expectedBottomEntity = false;
                expectedTopEntity = false;
            }
        } else {
            Assert.assertFalse(object instanceof OWLEntity);
            expectedBottomEntity = false;
            expectedTopEntity = false;
        }
        final boolean expectedAxiom = data.isAxiom();
        if (expectedAxiom) {
            Assert.assertTrue(object instanceof OWLAxiom);
            Assert.assertFalse(object instanceof OWLEntity);
            Assert.assertFalse(object instanceof OWLIndividual);
            Assert.assertFalse(object instanceof OWLDataRange);
            Assert.assertFalse(object instanceof OWLClassExpression);
            Assert.assertFalse(object instanceof OWLAnnotation);
        } else {
            Assert.assertFalse(object instanceof OWLAxiom);
        }

        final boolean expectedIndividual = data.isIndividual();
        if (expectedIndividual) {
            Assert.assertTrue(object instanceof OWLIndividual);
            Assert.assertFalse(object instanceof OWLDataRange);
            Assert.assertFalse(object instanceof OWLClassExpression);
        } else {
            Assert.assertFalse(object instanceof OWLIndividual);
        }
        if (data.isLiteral()) {
            Assert.assertTrue(object instanceof OWLLiteral);
            Assert.assertFalse(object instanceof OWLEntity);
            Assert.assertFalse(object instanceof OWLAxiom);
            Assert.assertFalse(object instanceof OWLIndividual);
            Assert.assertFalse(object instanceof OWLDataRange);
            Assert.assertFalse(object instanceof OWLClassExpression);
            Assert.assertFalse(object instanceof OWLAnnotation);
        } else {
            Assert.assertFalse(object instanceof OWLLiteral);
        }
        if (data.isOWLAnnotation()) {
            Assert.assertTrue(object instanceof OWLAnnotation);
            Assert.assertFalse(object instanceof OWLEntity);
            Assert.assertFalse(object instanceof OWLIndividual);
            Assert.assertFalse(object instanceof OWLDataRange);
            Assert.assertFalse(object instanceof OWLClassExpression);
        } else {
            Assert.assertFalse(object instanceof OWLAnnotation);
        }

        if (data.isFacetRestriction()) {
            Assert.assertTrue(object instanceof OWLFacetRestriction);
            Assert.assertFalse(object instanceof OWLAnnotation);
            Assert.assertFalse(object instanceof OWLEntity);
            Assert.assertFalse(object instanceof OWLIndividual);
            Assert.assertFalse(object instanceof OWLDataRange);
            Assert.assertFalse(object instanceof OWLClassExpression);
        } else {
            Assert.assertFalse(object instanceof OWLFacetRestriction);
        }
        Assert.assertEquals(data.isSWRLVariable(), object instanceof SWRLVariable);
        Assert.assertEquals(data.isSWRLIndividual(), object instanceof SWRLIndividualArgument);
        Assert.assertEquals(data.isSWRLLiteral(), object instanceof SWRLLiteralArgument);
        if (data.isSWRLAtom()) {
            Assert.assertTrue(object instanceof SWRLAtom);
        } else {
            Assert.assertFalse(object instanceof SWRLAtom);
        }

        Assert.assertEquals("'" + object + "' must be anonymous", expectedAnonymous, object.isAnonymous());
        Assert.assertEquals("'" + object + "' must be named", expectedNamed, object.isNamed());

        Assert.assertEquals("'" + object + "' must be individual", expectedIndividual, object.isIndividual());
        Assert.assertEquals("'" + object + "' must be axiom", expectedAxiom, object.isAxiom());

        Assert.assertEquals("'" + object + "' must be anonymous expression", expectedAnonymousExpression,
                object.isAnonymousExpression());

        Assert.assertFalse("'" + object + "' must not be IRI", object.isIRI());
        Assert.assertFalse("'" + object + "' must not be Ontology", object.isOntology());

        Assert.assertEquals("'" + object + "' must be bottom entity", expectedBottomEntity, object.isBottomEntity());
        Assert.assertEquals("'" + object + "' must be top entity", expectedTopEntity, object.isTopEntity());
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
