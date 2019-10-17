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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by @ssz on 03.09.2019.
 */
@SuppressWarnings("WeakerAccess")
@RunWith(Parameterized.class)
public class CommonAxiomsTest extends StatementTestBase {

    public CommonAxiomsTest(Data data) {
        super(data);
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Data> getData() {
        return getObjects().stream()
                .filter(Data::isAxiom)
                // TODO: see https://github.com/avicomp/ont-api/issues/87
                .filter(x -> isOneOf(x
                        , AxiomType.SUBCLASS_OF
                        , AxiomType.ANNOTATION_ASSERTION
                        , AxiomType.OBJECT_PROPERTY_ASSERTION
                        , AxiomType.DATA_PROPERTY_ASSERTION
                        , AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION
                        , AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION
                        , AxiomType.SUB_OBJECT_PROPERTY
                        , AxiomType.SUB_DATA_PROPERTY
                        , AxiomType.SUB_ANNOTATION_PROPERTY_OF
                        , AxiomType.FUNCTIONAL_OBJECT_PROPERTY
                        , AxiomType.FUNCTIONAL_DATA_PROPERTY
                        , AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY
                        , AxiomType.REFLEXIVE_OBJECT_PROPERTY
                        , AxiomType.IRREFLEXIVE_OBJECT_PROPERTY
                        , AxiomType.SYMMETRIC_OBJECT_PROPERTY
                        , AxiomType.ASYMMETRIC_OBJECT_PROPERTY
                        , AxiomType.TRANSITIVE_OBJECT_PROPERTY
                        , AxiomType.SUB_PROPERTY_CHAIN_OF
                        , AxiomType.HAS_KEY
                        , AxiomType.DISJOINT_UNION
                )).collect(Collectors.toList());
    }

    static boolean isOneOf(Data o, AxiomType... types) {
        AxiomType res = ((AxiomData) o).getType();
        for (AxiomType t : types) {
            if (res.equals(t)) return true;
        }
        return false;
    }

    static OWLAxiom createONTObject(OntologyManager m, OWLAxiom ont) {
        OntologyModel o = m.createOntology();
        o.add(ont);
        o.clearCache();
        ReadWriteUtils.print(o);
        OWLAxiom res = o.axioms().filter(ont::equals).findFirst().orElseThrow(AssertionError::new);
        Assert.assertTrue(res instanceof ONTObject);
        return res;
    }

    private static OWLAxiom createWithAnnotation(OWLAxiom a, OWLDataFactory df) {
        return a.getAnnotatedAxiom(Collections.singletonList(df.getRDFSComment(CommonAxiomsTest.class.getName())));
    }

    @Override
    OWLObject fromModel() {
        OntologyManager m = OntManagers.createONT();
        DataFactory df = m.getOWLDataFactory();
        OWLAxiom ont = (OWLAxiom) data.create(df);
        return createONTObject(m, ont);
    }

    @Override
    void testEraseModel(OWLObject sample, OWLObject actual) {
        super.testEraseModel(sample, actual);

        LOGGER.debug("test NNF for '{}'", data);
        OWLAxiom expectedNNF = ((OWLAxiom) sample).getNNF();
        OWLAxiom actualNNF = ((OWLAxiom) actual).getNNF();
        Assert.assertEquals(expectedNNF, actualNNF);
        testObjectHasNoModelReference(actualNNF);

        LOGGER.debug("Test axiom without annotation for '{}'", data);
        OWLAxiom expectedNoAnnotations = ((OWLAxiom) sample).getAxiomWithoutAnnotations();
        OWLAxiom actualNoAnnotations = ((OWLAxiom) actual).getAxiomWithoutAnnotations();
        Assert.assertEquals(expectedNoAnnotations, actualNoAnnotations);
        testObjectHasNoModelReference(actualNoAnnotations);

        LOGGER.debug("Test axiom with annotation for '{}'", data);
        OWLAxiom expectedWithAnnotation = createWithAnnotation((OWLAxiom) sample, OWL_DATA_FACTORY);
        OWLAxiom actualWithAnnotation = createWithAnnotation((OWLAxiom) actual, ONT_DATA_FACTORY);
        Assert.assertEquals(expectedWithAnnotation, actualWithAnnotation);
        testObjectHasNoModelReference(actualWithAnnotation);
    }

    @Override
    void testComponents(OWLObject expected, OWLObject actual) {
        LOGGER.debug("Test annotations for '{}'", data);
        OWLAxiom owl = (OWLAxiom) expected;
        OWLAxiom ont = (OWLAxiom) actual;
        validate(owl, ont, "annotations", HasAnnotations::annotations);
        super.testComponents(owl, ont);
    }

    @Override
    void testBooleanProperties(OWLObject expected, OWLObject actual) {
        LOGGER.debug("Test isAnnotated for '{}'", data);
        OWLAxiom owl = (OWLAxiom) expected;
        OWLAxiom ont = (OWLAxiom) actual;
        Assert.assertEquals(owl.isAnnotated(), ont.isAnnotated());
        super.testBooleanProperties(owl, ont);
    }
}
