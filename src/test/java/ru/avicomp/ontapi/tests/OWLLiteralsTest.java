/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.owlapi.objects.OWLLiteralImpl;

import java.util.Arrays;
import java.util.List;

/**
 * Created by @szz on 06.09.2018.
 */
@RunWith(Parameterized.class)
public class OWLLiteralsTest {
    private static final OWLDataFactory OWL_DATA_FACTORY = OntManagers.createOWLProfile().dataFactory();
    private static final DataFactory ONT_DATA_FACTORY = OntManagers.getDataFactory();

    private final Data data;

    public OWLLiteralsTest(Data data) {
        this.data = data;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Data> getData() {
        return Arrays.asList(
                new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral("literal", "x");
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"literal\", \"x\")";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral("literal", "x");
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"literal\", \"x\")";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral("literal");
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"literal\")";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral("literal ", (String) null);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"literal \", (String) null)";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral("literal@txt", "T");
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"literal@txt\", \"T\")";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral(12);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(12)";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral("05", OWL2Datatype.XSD_INTEGER);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"05\", OWL2Datatype.XSD_INTEGER)";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral(false);
                    }

                    @Override
                    public boolean shouldBeCached() {
                        return true;
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(false)";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral(-1.1);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(-1.1)";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral(Double.NaN);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(Double.NaN)";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral(Double.MAX_VALUE);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(Double.MAX_VALUE)";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral(-3.f);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(-3.f)";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral(Float.MIN_VALUE);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(Float.MIN_VALUE)";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral("-0.0", df.getOWLDatatype(OWL2Datatype.XSD_FLOAT));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"-0.0\", df.getOWLDatatype(OWL2Datatype.XSD_FLOAT))";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral("xxx@fff", OWL2Datatype.XSD_INT);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"xxx@fff\", OWL2Datatype.XSD_INT)";
                    }
                }
                , new Data() {
                    @Override
                    public OWLLiteral createLiteral(OWLDataFactory df) {
                        return df.getOWLLiteral("\n", df.getOWLDatatype(IRI.create("X")));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"\\n\", df.getOWLDatatype(IRI.create(\"X\")))";
                    }
                });
    }

    @Test
    public void testLiteral() {
        OWLLiteral owl = data.createLiteral(OWL_DATA_FACTORY);
        Assert.assertFalse(owl instanceof OWLLiteralImpl);
        OWLLiteral ont1 = data.createLiteral(ONT_DATA_FACTORY);
        Assert.assertTrue(ont1 instanceof OWLLiteralImpl);
        checkAssert(owl, ont1, false);
        OWLLiteral ont2 = data.createLiteral(ONT_DATA_FACTORY);
        checkAssert(ont1, ont2, data.shouldBeCached());
    }

    private void checkAssert(OWLLiteral expected, OWLLiteral actual, boolean shouldBeCached) {
        if (shouldBeCached) {
            Assert.assertSame(expected, actual);
            return;
        }
        Assert.assertNotSame(expected, actual);
        Assert.assertEquals(expected, actual);
        Assert.assertEquals(expected.hashCode(), actual.hashCode());
        Assert.assertEquals(expected.toString(), actual.toString());
        Assert.assertEquals(expected.getLiteral(), actual.getLiteral());
        Assert.assertEquals(expected.getLang(), actual.getLang());
        Assert.assertEquals(expected.getDatatype(), actual.getDatatype());
        Assert.assertEquals(expected.isRDFPlainLiteral(), actual.isRDFPlainLiteral());
        Assert.assertEquals(expected.isBoolean(), actual.isBoolean());
        Assert.assertEquals(expected.isDouble(), actual.isDouble());
        Assert.assertEquals(expected.isFloat(), actual.isFloat());
        Assert.assertEquals(expected.isInteger(), actual.isInteger());
        Assert.assertEquals(expected.isLiteral(), actual.isLiteral());
    }

    @FunctionalInterface
    interface Data {
        OWLLiteral createLiteral(OWLDataFactory df);

        default boolean shouldBeCached() {
            return false;
        }
    }

}
