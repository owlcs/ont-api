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

import org.junit.Assert;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;
import ru.avicomp.ontapi.owlapi.OWLObjectImpl;
import ru.avicomp.ontapi.owlapi.objects.entity.OWLBuiltinDatatypeImpl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * The collection of test data for testing inputs.
 * Created by @ssz on 22.08.2019.
 */
public class TestFactory {

    public static List<Data> getObjects() {
        return Arrays.asList(
                new NamedClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLClass(IRI.create("C"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLClass(IRI.create(\"C\"))";
                    }
                }
                , new NamedRange() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDatatype(IRI.create("D"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDatatype(IRI.create(\"D\"))";
                    }
                }
                , new ObjectProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectProperty(IRI.create("O"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectProperty(IRI.create(\"O\"))";
                    }
                }
                , new DataProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataProperty(IRI.create("D"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataProperty(IRI.create(\"D\"))";
                    }
                }
                , new AnnotationProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLAnnotationProperty(IRI.create("A"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLAnnotationProperty(IRI.create(\"A\"))";
                    }
                }
                , new NamedIndividual() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLNamedIndividual(IRI.create("I"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLNamedIndividual(IRI.create(\"I\"))";
                    }
                }
                , new AnonymousIndividual() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLAnonymousIndividual("_:b0");
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLAnonymousIndividual(\"_:b0\")";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.ASYMMETRIC_OBJECT_PROPERTY;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLAsymmetricObjectPropertyAxiom(df.getOWLObjectProperty("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLAsymmetricObjectPropertyAxiom(df.getOWLObjectProperty(\"P\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.CLASS_ASSERTION;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLClassAssertionAxiom(df.getOWLClass("C"), df.getOWLNamedIndividual("I"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLClassAssertionAxiom(df.getOWLClass(\"C\"), df.getOWLNamedIndividual(\"I\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataAllValuesFrom(df.getOWLDataProperty("P"), df.getOWLDatatype("D"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataAllValuesFrom(df.getOWLDataProperty(\"P\"), df.getOWLDatatype(\"D\"))";
                    }
                }
                , new AnonymousRange() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataComplementOf(df.getOWLDatatype("D"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataComplementOf(df.getOWLDatatype(\"D\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataExactCardinality(4, df.getOWLDataProperty("P"), df.getOWLDatatype("D"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataExactCardinality(4, df.getOWLDataProperty(\"P\"), df.getOWLDatatype(\"D\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataExactCardinality(3, df.getOWLDataProperty("P"), df.getTopDatatype());
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataExactCardinality(3, df.getOWLDataProperty(\"P\"), df.getTopDatatype())";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataHasValue(df.getOWLDataProperty("P"), df.getOWLLiteral(1));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataHasValue(df.getOWLDataProperty(\"P\"), df.getOWLLiteral(1))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataMaxCardinality(23, df.getOWLDataProperty("P"), df.getOWLDatatype("D"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataMaxCardinality(23, df.getOWLDataProperty(\"P\"), df.getOWLDatatype(\"D\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataMaxCardinality(12, df.getOWLDataProperty("P"), df.getTopDatatype());
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataMaxCardinality(12, df.getOWLDataProperty(\"P\"), df.getTopDatatype())";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataMinCardinality(5454, df.getOWLDataProperty("P"), df.getOWLDatatype("D"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataMinCardinality(5454, df.getOWLDataProperty(\"P\"), df.getOWLDatatype(\"D\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataMinCardinality(2, df.getOWLDataProperty("P"), df.getTopDatatype());
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataMinCardinality(2, df.getOWLDataProperty(\"P\"), df.getTopDatatype())";
                    }
                }
                , new AnonymousRange() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataOneOf(df.getOWLLiteral(1), df.getOWLLiteral(1.0), df.getOWLLiteral(1.0F));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataOneOf(df.getOWLLiteral(1), df.getOWLLiteral(1.0), df.getOWLLiteral(1.0F))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.DATA_PROPERTY_ASSERTION;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty("P"),
                                df.getOWLNamedIndividual("I"), df.getOWLLiteral(2));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataPropertyAssertionAxiom(df.getOWLDataProperty(\"P\"), " +
                                "df.getOWLNamedIndividual(\"I\"), df.getOWLLiteral(2))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.DATA_PROPERTY_DOMAIN;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataPropertyDomainAxiom(df.getOWLDataProperty("P"), df.getOWLClass("C"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataPropertyDomainAxiom(df.getOWLDataProperty(\"P\"), df.getOWLClass(\"C\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.DATA_PROPERTY_RANGE;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataPropertyRangeAxiom(df.getOWLDataProperty("P"), df.getOWLDatatype("D"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataPropertyRangeAxiom(df.getOWLDataProperty(\"P\"), df.getOWLDatatype(\"D\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataSomeValuesFrom(df.getOWLDataProperty("P"), df.getOWLDatatype("D"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataSomeValuesFrom(df.getOWLDataProperty(\"P\"), df.getOWLDatatype(\"D\"))";
                    }
                }
                , new AnonymousRange() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDatatypeRestriction(df.getOWLDatatype("D1"),
                                df.getOWLFacetRestriction(OWLFacet.MAX_EXCLUSIVE,
                                        df.getOWLLiteral("3", df.getOWLDatatype("D2"))));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDatatypeRestriction(df.getOWLDatatype(\"D1\"), " +
                                "df.getOWLFacetRestriction(OWLFacet.MAX_EXCLUSIVE, " +
                                "df.getOWLLiteral(\"3\", df.getOWLDatatype(\"D2\"))))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.DIFFERENT_INDIVIDUALS;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDifferentIndividualsAxiom(df.getOWLNamedIndividual("A1"),
                                df.getOWLAnonymousIndividual("_:b0"), df.getOWLNamedIndividual("C1"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDifferentIndividualsAxiom(df.getOWLNamedIndividual(\"A1\"), " +
                                "df.getOWLAnonymousIndividual(\"_:b0\"), df.getOWLNamedIndividual(\"C1\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.DISJOINT_CLASSES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDisjointClassesAxiom(df.getOWLClass("A"), df.getOWLClass("B"), df.getOWLClass("C"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDisjointClassesAxiom(df.getOWLClass(\"A\"), " +
                                "df.getOWLClass(\"B\"), df.getOWLClass(\"C\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.DISJOINT_DATA_PROPERTIES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDisjointDataPropertiesAxiom(df.getOWLDataProperty("A4"),
                                df.getOWLDataProperty("B4"), df.getOWLDataProperty("C4"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDisjointDataPropertiesAxiom(df.getOWLDataProperty(\"A4\"), " +
                                "df.getOWLDataProperty(\"B4\"), df.getOWLDataProperty(\"C4\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.DISJOINT_OBJECT_PROPERTIES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDisjointObjectPropertiesAxiom(df.getOWLObjectProperty("A3"),
                                df.getOWLObjectProperty("B3"), df.getOWLObjectProperty("C3"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDisjointObjectPropertiesAxiom(df.getOWLObjectProperty(\"A3\"), " +
                                "df.getOWLObjectProperty(\"B3\"), df.getOWLObjectProperty(\"C3\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.EQUIVALENT_CLASSES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLEquivalentClassesAxiom(df.getOWLClass("A"),
                                df.getOWLClass("B"), df.getOWLClass("C"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLEquivalentClassesAxiom(df.getOWLClass(\"A\"), " +
                                "df.getOWLClass(\"B\"), df.getOWLClass(\"C\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.EQUIVALENT_CLASSES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLEquivalentClassesAxiom(new OWLClassExpression[]{df.getOWLClass("A"),
                                df.getOWLClass("B")});
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLEquivalentClassesAxiom(new OWLClassExpression[]{df.getOWLClass(\"A\"), " +
                                "df.getOWLClass(\"B\")})";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.EQUIVALENT_DATA_PROPERTIES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLEquivalentDataPropertiesAxiom(df.getOWLDataProperty("A4"),
                                df.getOWLDataProperty("B4"), df.getOWLDataProperty("C4"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLEquivalentDataPropertiesAxiom(df.getOWLDataProperty(\"A4\"), " +
                                "df.getOWLDataProperty(\"B4\"), df.getOWLDataProperty(\"C4\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.EQUIVALENT_DATA_PROPERTIES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLEquivalentDataPropertiesAxiom(new OWLDataPropertyExpression[]{
                                df.getOWLDataProperty("P1"), df.getOWLDataProperty("P2")});
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLEquivalentDataPropertiesAxiom(new OWLDataPropertyExpression[]{" +
                                "df.getOWLDataProperty(\"P1\"), df.getOWLDataProperty(\"P2\")})";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.EQUIVALENT_OBJECT_PROPERTIES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLEquivalentObjectPropertiesAxiom(df.getOWLObjectProperty("A3"),
                                df.getOWLObjectProperty("B3"), df.getOWLObjectProperty("C3"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLEquivalentObjectPropertiesAxiom(df.getOWLObjectProperty(\"A3\"), " +
                                "df.getOWLObjectProperty(\"B3\"), df.getOWLObjectProperty(\"C3\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.FUNCTIONAL_DATA_PROPERTY;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLFunctionalDataPropertyAxiom(df.getOWLDataProperty("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLFunctionalDataPropertyAxiom(df.getOWLDataProperty(\"P\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.FUNCTIONAL_OBJECT_PROPERTY;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLFunctionalObjectPropertyAxiom(df.getOWLObjectProperty("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLFunctionalObjectPropertyAxiom(df.getOWLObjectProperty(\"P\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLInverseFunctionalObjectPropertyAxiom(df.getOWLObjectProperty("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLInverseFunctionalObjectPropertyAxiom(df.getOWLObjectProperty(\"P\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.IRREFLEXIVE_OBJECT_PROPERTY;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLIrreflexiveObjectPropertyAxiom(df.getOWLObjectProperty("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLIrreflexiveObjectPropertyAxiom(df.getOWLObjectProperty(\"P\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLNegativeDataPropertyAssertionAxiom(df.getOWLDataProperty("P"),
                                df.getOWLAnonymousIndividual("_:b0"), df.getOWLLiteral(2));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLNegativeDataPropertyAssertionAxiom(df.getOWLDataProperty(\"P\"), " +
                                "df.getOWLAnonymousIndividual(\"_:b0\"), df.getOWLLiteral(2))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLNegativeObjectPropertyAssertionAxiom(df.getOWLObjectProperty("P"),
                                df.getOWLAnonymousIndividual("_:b0"), df.getOWLNamedIndividual("I"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLNegativeObjectPropertyAssertionAxiom(df.getOWLObjectProperty(\"P\"), " +
                                "df.getOWLAnonymousIndividual(\"_:b0\"), df.getOWLNamedIndividual(\"I\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectAllValuesFrom(df.getOWLObjectProperty("P"), df.getOWLClass("C"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectAllValuesFrom(df.getOWLObjectProperty(\"P\"), df.getOWLClass(\"C\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectComplementOf(df.getOWLClass("C"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectComplementOf(df.getOWLClass(\"C\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectExactCardinality(3, df.getOWLObjectProperty("P"), df.getOWLClass("C"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectExactCardinality(3, " +
                                "df.getOWLObjectProperty(\"P\"), df.getOWLClass(\"C\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectExactCardinality(3, df.getOWLObjectProperty("P"), df.getOWLThing());
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectExactCardinality(3, df.getOWLObjectProperty(\"P\"), df.getOWLThing())";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectHasSelf(df.getOWLObjectProperty("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectHasSelf(df.getOWLObjectProperty(\"P\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectHasValue(df.getOWLObjectProperty("P"), df.getOWLNamedIndividual("I"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectHasValue(df.getOWLObjectProperty(\"P\"), " +
                                "df.getOWLNamedIndividual(\"I\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectIntersectionOf(df.getOWLClass("A"), df.getOWLClass("B"), df.getOWLClass("C"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectIntersectionOf(df.getOWLClass(\"A\"), " +
                                "df.getOWLClass(\"B\"), df.getOWLClass(\"C\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectMaxCardinality(3, df.getOWLObjectProperty("P"), df.getOWLClass("A"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectMaxCardinality(3, " +
                                "df.getOWLObjectProperty(\"P\"), df.getOWLClass(\"A\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectMaxCardinality(3, df.getOWLObjectProperty("P"), df.getOWLThing());
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectMaxCardinality(3, df.getOWLObjectProperty(\"P\"), df.getOWLThing())";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectMinCardinality(3, df.getOWLObjectProperty("P"), df.getOWLClass("A"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectMinCardinality(3, " +
                                "df.getOWLObjectProperty(\"P\"), df.getOWLClass(\"A\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectMinCardinality(3, df.getOWLObjectProperty("P"), df.getOWLThing());
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectMinCardinality(3, df.getOWLObjectProperty(\"P\"), df.getOWLThing())";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectOneOf(df.getOWLNamedIndividual("A1"),
                                df.getOWLAnonymousIndividual("_:b0"), df.getOWLNamedIndividual("C1"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectOneOf(df.getOWLNamedIndividual(\"A1\"), " +
                                "df.getOWLAnonymousIndividual(\"_:b0\"), df.getOWLNamedIndividual(\"C1\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.OBJECT_PROPERTY_ASSERTION;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty("P"),
                                df.getOWLAnonymousIndividual("_:b0"), df.getOWLNamedIndividual("I"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(\"P\"), " +
                                "df.getOWLAnonymousIndividual(\"_:b0\"), df.getOWLNamedIndividual(\"I\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.OBJECT_PROPERTY_DOMAIN;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectPropertyDomainAxiom(df.getOWLObjectProperty("P"), df.getOWLClass("C"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectPropertyDomainAxiom(df.getOWLObjectProperty(\"P\"), " +
                                "df.getOWLClass(\"C\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.OBJECT_PROPERTY_RANGE;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectPropertyRangeAxiom(df.getOWLObjectProperty("P"), df.getOWLClass("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectPropertyRangeAxiom(df.getOWLObjectProperty(\"P\"), " +
                                "df.getOWLClass(\"P\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectSomeValuesFrom(df.getOWLObjectProperty("P"), df.getOWLClass("C"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectSomeValuesFrom(df.getOWLObjectProperty(\"P\"), df.getOWLClass(\"C\"))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectUnionOf(df.getOWLClass("A"), df.getOWLClass("B"), df.getOWLThing());
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectUnionOf(df.getOWLClass(\"A\"), df.getOWLClass(\"B\"), " +
                                "df.getOWLThing())";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.REFLEXIVE_OBJECT_PROPERTY;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLReflexiveObjectPropertyAxiom(df.getOWLObjectProperty("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLReflexiveObjectPropertyAxiom(df.getOWLObjectProperty(\"P\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.SAME_INDIVIDUAL;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLSameIndividualAxiom(df.getOWLNamedIndividual("A1"),
                                df.getOWLAnonymousIndividual("_:b0"), df.getOWLNamedIndividual("C1"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLSameIndividualAxiom(df.getOWLNamedIndividual(\"A1\"), " +
                                "df.getOWLAnonymousIndividual(\"_:b0\"), df.getOWLNamedIndividual(\"C1\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.SUB_DATA_PROPERTY;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLSubDataPropertyOfAxiom(df.getOWLDataProperty("P"), df.getOWLDataProperty("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLSubDataPropertyOfAxiom(df.getOWLDataProperty(\"P\"), " +
                                "df.getOWLDataProperty(\"P\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.SUB_OBJECT_PROPERTY;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLSubObjectPropertyOfAxiom(df.getOWLObjectProperty("P"),
                                df.getOWLObjectProperty("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLSubObjectPropertyOfAxiom(df.getOWLObjectProperty(\"P\"), " +
                                "df.getOWLObjectProperty(\"P\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.SYMMETRIC_OBJECT_PROPERTY;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLSymmetricObjectPropertyAxiom(df.getOWLObjectProperty("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLSymmetricObjectPropertyAxiom(df.getOWLObjectProperty(\"P\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.TRANSITIVE_OBJECT_PROPERTY;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLTransitiveObjectPropertyAxiom(df.getOWLObjectProperty("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLTransitiveObjectPropertyAxiom(df.getOWLObjectProperty(\"P\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.SWRL_RULE;
                    }

                    @Override
                    public String toString() {
                        return "SWRLRule Test";
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        SWRLVariable v = df.getSWRLVariable("urn:test#", "x");
                        OWLClass a = df.getOWLClass("urn:test#", "A");
                        OWLClass b = df.getOWLClass("urn:test#", "B");
                        OWLClass c = df.getOWLClass("urn:test#", "C");
                        OWLClass d = df.getOWLClass("urn:test#", "D");
                        List<SWRLClassAtom> body = Arrays.asList(df.getSWRLClassAtom(a, v), df.getSWRLClassAtom(c, v));
                        List<SWRLClassAtom> head = Arrays.asList(df.getSWRLClassAtom(b, v), df.getSWRLClassAtom(d, v));
                        return df.getSWRLRule(body, head);
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.SWRL_RULE;
                    }

                    @Override
                    public String toString() {
                        return "SWRLRule with Annotations Test";
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        SWRLVariable v = df.getSWRLVariable("urn:test#", "x");
                        OWLClass a = df.getOWLClass("urn:test#", "A");
                        OWLClass b = df.getOWLClass("urn:test#", "B");
                        OWLClass c = df.getOWLClass("urn:test#", "C");
                        OWLClass d = df.getOWLClass("urn:test#", "D");
                        Collection<OWLAnnotation> annotations = Arrays.asList(df.getRDFSComment("test1"),
                                df.getRDFSLabel("test2"));
                        List<SWRLClassAtom> body = Arrays.asList(df.getSWRLClassAtom(a, v), df.getSWRLClassAtom(c, v));
                        List<SWRLClassAtom> head = Arrays.asList(df.getSWRLClassAtom(b, v), df.getSWRLClassAtom(d, v));

                        return df.getSWRLRule(body, head, annotations);
                    }
                }
                , new NamedRange() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getBooleanOWLDatatype();
                    }

                    @Override
                    public Class<? extends OWLObject> getSuperImplClassType() {
                        return OWLBuiltinDatatypeImpl.class;
                    }

                    @Override
                    public String toString() {
                        return "df.getBooleanOWLDatatype()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new NamedRange() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getDoubleOWLDatatype();
                    }

                    @Override
                    public Class<? extends OWLObject> getSuperImplClassType() {
                        return OWLBuiltinDatatypeImpl.class;
                    }

                    @Override
                    public String toString() {
                        return "df.getDoubleOWLDatatype()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new NamedRange() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getFloatOWLDatatype();
                    }

                    @Override
                    public Class<? extends OWLObject> getSuperImplClassType() {
                        return OWLBuiltinDatatypeImpl.class;
                    }

                    @Override
                    public String toString() {
                        return "df.getFloatOWLDatatype()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new AnnotationProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLBackwardCompatibleWith();
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLBackwardCompatibleWith()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new DataProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLBottomDataProperty();
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLBottomDataProperty()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new ObjectProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLBottomObjectProperty();
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLBottomObjectProperty()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new AnnotationProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDeprecated();
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDeprecated()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new AnnotationProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLIncompatibleWith();
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLIncompatibleWith()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new NamedClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLNothing();
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLNothing()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new NamedClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLThing();
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLThing()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new DataProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLTopDataProperty();
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLTopDataProperty()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new ObjectProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLTopObjectProperty();
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLTopObjectProperty()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new AnnotationProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLVersionInfo();
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLVersionInfo()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new NamedRange() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getRDFPlainLiteral();
                    }

                    @Override
                    public Class<? extends OWLObject> getSuperImplClassType() {
                        return OWLBuiltinDatatypeImpl.class;
                    }

                    @Override
                    public String toString() {
                        return "df.getRDFPlainLiteral()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new AnnotationProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getRDFSIsDefinedBy();
                    }

                    @Override
                    public String toString() {
                        return "df.getRDFSIsDefinedBy()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new AnnotationProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getRDFSLabel();
                    }

                    @Override
                    public String toString() {
                        return "df.getRDFSLabel()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new AnnotationProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getRDFSSeeAlso();
                    }

                    @Override
                    public String toString() {
                        return "df.getRDFSSeeAlso()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new NamedRange() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getTopDatatype();
                    }

                    @Override
                    public Class<? extends OWLObject> getSuperImplClassType() {
                        return OWLBuiltinDatatypeImpl.class;
                    }

                    @Override
                    public String toString() {
                        return "df.getTopDatatype()";
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral("literal", "x");
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"literal\", \"x\")";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral("literal");
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"literal\")";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral("literal ", (String) null);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"literal \", (String) null)";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral("literal@txt", "T");
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"literal@txt\", \"T\")";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral(12);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(12)";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral("05", OWL2Datatype.XSD_INTEGER);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"05\", OWL2Datatype.XSD_INTEGER)";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral(false);
                    }

                    @Override
                    public boolean shouldBeSame() {
                        return true;
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(false)";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral(-1.1);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(-1.1)";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral(Double.NaN);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(Double.NaN)";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral(Double.MAX_VALUE);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(Double.MAX_VALUE)";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral(-3.f);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(-3.f)";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral(Float.MIN_VALUE);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(Float.MIN_VALUE)";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral("-0.0", df.getOWLDatatype(OWL2Datatype.XSD_FLOAT));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"-0.0\", df.getOWLDatatype(OWL2Datatype.XSD_FLOAT))";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral("xxx@fff", OWL2Datatype.XSD_INT);
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"xxx@fff\", OWL2Datatype.XSD_INT)";
                    }
                }
                , new LiteralData() {
                    @Override
                    public OWLLiteral create(OWLDataFactory df) {
                        return df.getOWLLiteral("\n", df.getOWLDatatype(IRI.create("X")));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLLiteral(\"\\n\", df.getOWLDatatype(IRI.create(\"X\")))";
                    }
                }
                , new AnonymousClass() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectUnionOf(
                                df.getOWLObjectComplementOf(df.getOWLObjectHasValue(df.getOWLObjectProperty("P1"),
                                        df.getOWLNamedIndividual("I"))),
                                df.getOWLObjectIntersectionOf(df.getOWLThing(),
                                        df.getOWLObjectSomeValuesFrom(
                                                df.getOWLObjectInverseOf(df.getOWLObjectProperty("P2")),
                                                df.getOWLClass("C")),
                                        df.getOWLDataMaxCardinality(12, df.getOWLDataProperty("P3"),
                                                df.getOWLDataOneOf(df.getOWLLiteral(1),
                                                        df.getOWLLiteral(2),
                                                        df.getOWLLiteral("3")))),
                                df.getOWLDataAllValuesFrom(df.getOWLDataProperty("P3"),
                                        df.getOWLDatatypeRestriction(df.getOWLDatatype("D"),
                                                df.getOWLFacetRestriction(OWLFacet.MIN_EXCLUSIVE, df.getOWLLiteral(3.3)),
                                                df.getOWLFacetRestriction(OWLFacet.FRACTION_DIGITS, df.getOWLLiteral("x")))));
                    }

                    @Override
                    public String toString() {
                        return "ComplexClassExpressionWithDifferentNestedExpressions";
                    }
                }
                , new AnnotationData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLAnnotation(df.getOWLDeprecated(), df.getOWLLiteral(true));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLAnnotation(df.getOWLDeprecated(), df.getOWLLiteral(true))";
                    }
                }
                , new AnnotationData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLAnnotation(df.getOWLAnnotationProperty("P"), df.getOWLLiteral("L", "lu"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLAnnotation(df.getOWLAnnotationProperty(\"P\"), df.getOWLLiteral(\"L\", \"lu\"))";
                    }
                }
                , new AnnotationData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLAnnotation(df.getOWLAnnotationProperty("P"), IRI.create("P"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLAnnotation(df.getOWLAnnotationProperty(\"P\"), IRI.create(\"P\"))";
                    }
                }
                , new AnnotationData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLAnnotation(df.getOWLAnnotationProperty("P"), df.getOWLAnonymousIndividual("_:b0"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLAnnotation(df.getOWLAnnotationProperty(\"P\"), df.getOWLAnonymousIndividual(\"_:b0\"))";
                    }
                }
                , new AnnotationData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLAnnotation(df.getRDFSComment(), df.getOWLLiteral("comm"),
                                Arrays.asList(
                                        df.getOWLAnnotation(df.getRDFSLabel(), df.getOWLLiteral("lab1")),
                                        df.getOWLAnnotation(
                                                df.getOWLAnnotationProperty("P1"),
                                                df.getOWLAnonymousIndividual("_:b0"),
                                                Collections.singletonList(df.getRDFSLabel("x"))),
                                        df.getOWLAnnotation(
                                                df.getOWLAnnotationProperty("P2"),
                                                IRI.create("X"),
                                                Arrays.asList(
                                                        df.getRDFSComment("X"),
                                                        df.getRDFSLabel("x"),
                                                        df.getRDFSComment(df.getOWLAnonymousIndividual("_:b1"),
                                                                Stream.of(df.getOWLAnnotation(df.getOWLAnnotationProperty("P2"), IRI.create("I2"))))
                                                ))));
                    }

                    @Override
                    public String toString() {
                        return "ComplexAnnotationWithDifferentNestedSubAnnotations";
                    }
                }
                , new InverseObjectProperty() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectInverseOf(df.getOWLObjectProperty("X"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectInverseOf(df.getOWLObjectProperty(\"X\"))";
                    }
                }
                , new FRData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLFacetRestriction(OWLFacet.FRACTION_DIGITS, df.getOWLLiteral(5));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLFacetRestriction(OWLFacet.FRACTION_DIGITS, df.getOWLLiteral(5))";
                    }
                }
                , new FRData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLFacetRestriction(OWLFacet.LANG_RANGE, df.getOWLLiteral("r-RR"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLFacetRestriction(OWLFacet.LANG_RANGE, df.getOWLLiteral(\"r-RR\"))";
                    }
                }
                , new AnonymousRange() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataUnionOf(df.getStringOWLDatatype(), df.getOWLDatatype("X"), df.getDoubleOWLDatatype());
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataUnionOf(df.getStringOWLDatatype(), df.getOWLDatatype(\"X\"), df.getDoubleOWLDatatype())";
                    }
                }
                , new AnonymousRange() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataUnionOf(df.getOWLDatatype("X"), df.getBooleanOWLDatatype(), df.getOWLDatatype("Y"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDataUnionOf( df.getOWLDatatype(\"X\"), df.getBooleanOWLDatatype(),  df.getOWLDatatype(\"Y\"))";
                    }
                }
                , new AnonymousRange() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDataUnionOf(df.getOWLDataComplementOf(df.getOWLDatatype("X")),
                                df.getOWLDataOneOf(df.getOWLLiteral(1), df.getOWLLiteral(2), df.getOWLLiteral(3)),
                                df.getOWLDatatypeRestriction(df.getFloatOWLDatatype(),
                                        df.getOWLFacetRestriction(OWLFacet.TOTAL_DIGITS, df.getOWLLiteral(2)),
                                        df.getOWLFacetRestriction(OWLFacet.MIN_INCLUSIVE, df.getOWLLiteral(-2.1)),
                                        df.getOWLFacetRestriction(OWLFacet.LENGTH, df.getOWLLiteral(24)),
                                        df.getOWLFacetRestriction(OWLFacet.PATTERN, df.getOWLLiteral("#.##"))
                                ));
                    }

                    @Override
                    public String toString() {
                        return "ComplexDataRangeWithDifferentNestedDataRangeExpressions";
                    }
                }
                , new SWRLVar() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getSWRLVariable("x", "y");
                    }

                    @Override
                    public String toString() {
                        return "df.getSWRLVariable(\"x\", \"y\")";
                    }
                }
                , new SWRLIndividual() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getSWRLIndividualArgument(df.getOWLAnonymousIndividual("_:b0"));
                    }

                    @Override
                    public String toString() {
                        return "df.getSWRLIndividualArgument(df.getOWLAnonymousIndividual(\"_:b0\"))";
                    }
                }
                , new SWRLIndividual() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getSWRLIndividualArgument(df.getOWLNamedIndividual("I"));
                    }

                    @Override
                    public String toString() {
                        return "df.getSWRLIndividualArgument(df.getOWLNamedIndividual(\"I\"))";
                    }
                }
                , new SWRLLiteral() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getSWRLLiteralArgument(df.getOWLLiteral("L"));
                    }

                    @Override
                    public String toString() {
                        return "df.getSWRLLiteralArgument(df.getOWLLiteral(\"L\"))";
                    }
                }
                , new SWRLLiteral() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getSWRLLiteralArgument(df.getOWLLiteral(true));
                    }

                    @Override
                    public String toString() {
                        return "df.getSWRLLiteralArgument(df.getOWLLiteral(true))";
                    }
                }
                , new SWRLAtomData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getSWRLClassAtom(df.getOWLClass("C"),
                                df.getSWRLIndividualArgument(df.getOWLNamedIndividual("I")));
                    }

                    @Override
                    public String toString() {
                        return "df.getSWRLClassAtom(df.getOWLClass(\"C\"), " +
                                "df.getSWRLIndividualArgument(df.getOWLNamedIndividual(\"I\")))";
                    }
                }
                , new SWRLAtomData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getSWRLDataRangeAtom(df.getOWLDatatype("D"),
                                df.getSWRLLiteralArgument(df.getOWLLiteral("L")));
                    }

                    @Override
                    public String toString() {
                        return "df.getSWRLDataRangeAtom(df.getOWLDatatype(\"D\"), " +
                                "df.getSWRLLiteralArgument(df.getOWLLiteral(\"L\")))";
                    }
                }
                , new SWRLAtomData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getSWRLDataPropertyAtom(df.getOWLBottomDataProperty(),
                                df.getSWRLVariable(IRI.create("V")), df.getSWRLLiteralArgument(df.getOWLLiteral("V")));
                    }

                    @Override
                    public String toString() {
                        return "df.getSWRLDataPropertyAtom(df.getOWLBottomDataProperty(), " +
                                "df.getSWRLVariable(IRI.create(\"V\")), " +
                                "df.getSWRLLiteralArgument(df.getOWLLiteral(\"V\")))";
                    }
                }
                , new SWRLAtomData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getSWRLObjectPropertyAtom(df.getOWLObjectProperty("P"),
                                df.getSWRLIndividualArgument(df.getOWLNamedIndividual("I")),
                                df.getSWRLVariable("V"));
                    }

                    @Override
                    public String toString() {
                        return "df.getSWRLObjectPropertyAtom(df.getOWLObjectProperty(\"P\"), " +
                                "df.getSWRLIndividualArgument(df.getOWLNamedIndividual(\"I\")), " +
                                "df.getSWRLVariable(\"V\"))";
                    }
                }
                , new SWRLAtomData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getSWRLSameIndividualAtom(df.getSWRLVariable("V"),
                                df.getSWRLIndividualArgument(df.getOWLAnonymousIndividual("_:b0")));
                    }

                    @Override
                    public String toString() {
                        return "df.getSWRLSameIndividualAtom(df.getSWRLVariable(\"V\"), " +
                                "df.getSWRLIndividualArgument(df.getOWLAnonymousIndividual(\"_:b0\")))";
                    }
                }
                , new SWRLAtomData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getSWRLDifferentIndividualsAtom(
                                df.getSWRLIndividualArgument(df.getOWLAnonymousIndividual("_:b1")),
                                df.getSWRLVariable("V"));
                    }

                    @Override
                    public String toString() {
                        return "df.getSWRLDifferentIndividualsAtom(" +
                                "df.getSWRLIndividualArgument(df.getOWLAnonymousIndividual(\"_:b1\")), " +
                                "df.getSWRLVariable(\"V\"))";
                    }
                }
                , new SWRLAtomData() {
                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getSWRLBuiltInAtom(IRI.create("I"),
                                Arrays.asList(df.getSWRLVariable("V"),
                                        df.getSWRLLiteralArgument(df.getOWLLiteral("V"))));
                    }

                    @Override
                    public String toString() {
                        return "df.getSWRLBuiltInAtom(IRI.create(\"I\"), " +
                                "Arrays.asList(df.getSWRLVariable(\"V\"), " +
                                "df.getSWRLLiteralArgument(df.getOWLLiteral(\"V\"))))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.SUBCLASS_OF;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLSubClassOfAxiom(df.getOWLClass("Sub"),
                                df.getOWLObjectAllValuesFrom(df.getOWLObjectProperty("P"), df.getOWLClass("Sup")));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLSubClassOfAxiom(df.getOWLClass(\"Sub\"), " +
                                "df.getOWLObjectAllValuesFrom(df.getOWLObjectProperty(\"P\"), " +
                                "df.getOWLClass(\"Sup\")))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.SUBCLASS_OF;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLSubClassOfAxiom(df.getOWLObjectComplementOf(df.getOWLClass("C2")),
                                df.getOWLClass("C"), Collections.singletonList(df.getRDFSComment("comm")));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLSubClassOfAxiom(df.getOWLObjectComplementOf(df.getOWLClass(\"C2\")), " +
                                "df.getOWLClass(\"C\"), Collections.singletonList(df.getRDFSComment(\"comm\")))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.ANNOTATION_ASSERTION;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLAnnotationAssertionAxiom(df.getOWLAnonymousIndividual("_:b0"),
                                df.getRDFSLabel("label"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLAnnotationAssertionAxiom(df.getOWLAnonymousIndividual(\"_:b0\"), " +
                                "df.getRDFSLabel(\"label\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.ANNOTATION_ASSERTION;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLAnnotationAssertionAxiom(IRI.create("I"), df.getRDFSComment("comm"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLAnnotationAssertionAxiom(IRI.create(\"I\"), df.getRDFSComment(\"comm\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.DECLARATION;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDeclarationAxiom(df.getOWLDatatype("D"),
                                Collections.singletonList(df.getOWLAnnotation(df.getRDFSLabel(),
                                        df.getOWLAnonymousIndividual("_:b0"),
                                        Arrays.asList(df.getRDFSComment("C"), df.getRDFSLabel("L")))));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDeclarationAxiom(df.getOWLDatatype(\"D\"), " +
                                "...Annotation With A Sub Annotation...)";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.DECLARATION;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLDeclarationAxiom(df.getOWLNamedIndividual("I"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLDeclarationAxiom(df.getOWLNamedIndividual(\"I\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.EQUIVALENT_CLASSES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLEquivalentClassesAxiom(df.getOWLClass("A"), df.getOWLClass("B"),
                                Arrays.asList(df.getRDFSLabel("label"), df.getRDFSComment("comment")));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLEquivalentClassesAxiom(df.getOWLClass(\"A\"), df.getOWLClass(\"B\"), " +
                                "Arrays.asList(df.getRDFSLabel(\"label\"), df.getRDFSComment(\"comment\")))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.EQUIVALENT_CLASSES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLEquivalentClassesAxiom(df.getOWLClass("A"),
                                df.getOWLObjectComplementOf(df.getOWLThing()),
                                Collections.singleton(df.getRDFSLabel("label")));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLEquivalentClassesAxiom(df.getOWLClass(\"A\"), " +
                                "df.getOWLObjectComplementOf(df.getOWLThing()), " +
                                "Collections.singleton(df.getRDFSLabel(\"label\")))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.SUB_OBJECT_PROPERTY;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLSubObjectPropertyOfAxiom(df.getOWLObjectProperty("P"),
                                df.getOWLObjectInverseOf(df.getOWLObjectProperty("P")),
                                Collections.singleton(df.getOWLAnnotation(df.getOWLAnnotationProperty("P"),
                                        df.getOWLAnonymousIndividual("_:b0"))));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLSubObjectPropertyOfAxiom(df.getOWLObjectProperty(\"P\"), " +
                                "df.getOWLObjectInverseOf(df.getOWLObjectProperty(\"P\")), " +
                                "Collections.singleton(df.getOWLAnnotation(df.getOWLAnnotationProperty(\"P\"), " +
                                "df.getOWLAnonymousIndividual(\"_:b0\"))))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.INVERSE_OBJECT_PROPERTIES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLInverseObjectPropertiesAxiom(df.getOWLObjectProperty("P1"),
                                df.getOWLObjectProperty("P2"));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLInverseObjectPropertiesAxiom(df.getOWLObjectProperty(\"P1\"), " +
                                "df.getOWLObjectProperty(\"P2\"))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.INVERSE_OBJECT_PROPERTIES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLInverseObjectPropertiesAxiom(df.getOWLObjectProperty("P"),
                                df.getOWLObjectInverseOf(df.getOWLObjectProperty("P")));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLInverseObjectPropertiesAxiom(df.getOWLObjectProperty(\"P\"), " +
                                "df.getOWLObjectInverseOf(df.getOWLObjectProperty(\"P\")))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.INVERSE_OBJECT_PROPERTIES;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLInverseObjectPropertiesAxiom(
                                df.getOWLObjectInverseOf(df.getOWLObjectProperty("P1")),
                                df.getOWLObjectInverseOf(df.getOWLObjectProperty("P2")),
                                Collections.singleton(df.getRDFSLabel("inv(P1) owl:inverseOf inv(P2)")));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLInverseObjectPropertiesAxiom(" +
                                "df.getOWLObjectInverseOf(df.getOWLObjectProperty(\"P1\")), " +
                                "df.getOWLObjectInverseOf(df.getOWLObjectProperty(\"P2\")), " +
                                "Collections.singleton(df.getRDFSLabel(\"inv(P1) owl:inverseOf inv(P2)\")))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.FUNCTIONAL_OBJECT_PROPERTY;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLFunctionalObjectPropertyAxiom(
                                df.getOWLObjectInverseOf(df.getOWLObjectProperty("O")),
                                Arrays.asList(df.getOWLAnnotation(df.getOWLAnnotationProperty("A"),
                                        df.getOWLAnonymousIndividual("_:b0")), df.getRDFSComment("comm")));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLFunctionalObjectPropertyAxiom(" +
                                "df.getOWLObjectInverseOf(df.getOWLObjectProperty(\"O\")), " +
                                "Arrays.asList(df.getOWLAnnotation(df.getOWLAnnotationProperty(\"A\"), " +
                                "df.getOWLAnonymousIndividual(\"_:b0\")), df.getRDFSComment(\"comm\")))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.OBJECT_PROPERTY_ASSERTION;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty("P"),
                                df.getOWLNamedIndividual("I"),
                                df.getOWLAnonymousIndividual("_:b0"),
                                Collections.singleton(df.getOWLAnnotation(df.getOWLAnnotationProperty("A"),
                                        df.getOWLAnonymousIndividual("_:b1"))));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLObjectPropertyAssertionAxiom(df.getOWLObjectProperty(\"P\"), " +
                                "df.getOWLNamedIndividual(\"I\"), " +
                                "df.getOWLAnonymousIndividual(\"_:b0\"), " +
                                "Collections.singleton(df.getOWLAnnotation(df.getOWLAnnotationProperty(\"A\"), " +
                                "df.getOWLAnonymousIndividual(\"_:b1\"))))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLNegativeObjectPropertyAssertionAxiom(df.getOWLObjectInverseOf(
                                df.getOWLObjectProperty("P")),
                                df.getOWLNamedIndividual("S"), df.getOWLNamedIndividual("O"),
                                Arrays.asList(df.getRDFSComment("x"),
                                        df.getOWLAnnotation(df.getOWLAnnotationProperty("A"),
                                                df.getOWLAnonymousIndividual("_:b0"))));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLNegativeObjectPropertyAssertionAxiom(df.getOWLObjectInverseOf(" +
                                "df.getOWLObjectProperty(\"P\")), " +
                                "df.getOWLNamedIndividual(\"S\"), df.getOWLNamedIndividual(\"O\"), " +
                                "Arrays.asList(df.getRDFSComment(\"x\"), " +
                                "df.getOWLAnnotation(df.getOWLAnnotationProperty(\"A\"), " +
                                "df.getOWLAnonymousIndividual(\"_:b0\"))))";
                    }
                }
                , new AxiomData() {
                    @Override
                    public AxiomType getType() {
                        return AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION;
                    }

                    @Override
                    public OWLObject create(OWLDataFactory df) {
                        return df.getOWLNegativeDataPropertyAssertionAxiom(df.getOWLDataProperty("P"),
                                df.getOWLNamedIndividual("I"), df.getOWLLiteral("test", "ex"),
                                Collections.singleton(df.getRDFSComment("comm")));
                    }

                    @Override
                    public String toString() {
                        return "df.getOWLNegativeDataPropertyAssertionAxiom(df.getOWLDataProperty(\"P\"), " +
                                "df.getOWLNamedIndividual(\"I\"), df.getOWLLiteral(\"test\", \"ex\"), " +
                                "Collections.singleton(df.getRDFSComment(\"comm\")))";
                    }
                }
        );

    }

    public interface Data {
        OWLObject create(OWLDataFactory df);

        default Class<? extends OWLObject> getSuperImplClassType() {
            return OWLObjectImpl.class;
        }

        default void assertCheckNotSame(OWLObject expected, OWLObject actual) {
            Assert.assertNotSame(expected, actual);
        }

        default void assertCheckEquals(OWLObject expected, OWLObject actual) {
            Assert.assertEquals("'" + expected + "': not equal", expected, actual);
        }

        default void assertCheckToString(OWLObject expected, OWLObject actual) {
            Assert.assertEquals("'" + expected + "': wrong toString", expected.toString(), actual.toString());
        }

        default void assertCheckHashCode(OWLObject expected, OWLObject actual) {
            Assert.assertEquals("'" + expected + "': wrong hashcode", expected.hashCode(), actual.hashCode());
        }

        default void assertCheckProperties(OWLObject expected, OWLObject actual) {
        }

        default void testCompare(OWLObject expected, OWLObject actual) {
            assertCheckNotSame(expected, actual);
            assertCheckHashCode(expected, actual);
            assertCheckEquals(expected, actual);
            assertCheckToString(expected, actual);
            assertCheckProperties(expected, actual);
        }

        default boolean shouldBeSame() {
            return false;
        }

        default boolean isLiteral() {
            return false;
        }

        default boolean isAxiom() {
            return false;
        }

        default boolean isEntity() {
            return false;
        }

        default boolean isDatatype() {
            return false;
        }

        default boolean isClass() {
            return false;
        }

        default boolean isAnonymousClassExpression() {
            return false;
        }

        default boolean isAnonymousDataRange() {
            return false;
        }

        default boolean isAnonymousIndividual() {
            return false;
        }

        default boolean isAnonymousProperty() {
            return false;
        }

        default boolean isIndividual() {
            return false;
        }

        default boolean isAnnotationProperty() {
            return false;
        }

        default boolean isDatatypeProperty() {
            return false;
        }

        default boolean isObjectProperty() {
            return false;
        }

        default boolean isOWLAnnotation() {
            return false;
        }

        default boolean isFacetRestriction() {
            return false;
        }

        default boolean isSWRLVariable() {
            return false;
        }

        default boolean isSWRLLiteral() {
            return false;
        }

        default boolean isSWRLIndividual() {
            return false;
        }

        default boolean isSWRLAtom() {
            return false;
        }
    }

    public interface AxiomData extends Data {

        AxiomType getType();

        @Override
        default boolean isAxiom() {
            return true;
        }
    }

    public interface AnnotationData extends Data {
        @Override
        default boolean isOWLAnnotation() {
            return true;
        }
    }

    public interface EntityData extends Data {
        @Override
        default boolean isEntity() {
            return true;
        }
    }

    public interface NamedRange extends EntityData {
        @Override
        default boolean isDatatype() {
            return true;
        }
    }

    public interface NamedClass extends EntityData {
        @Override
        default boolean isClass() {
            return true;
        }
    }

    public interface NamedIndividual extends EntityData {
        @Override
        default boolean isIndividual() {
            return true;
        }
    }

    public interface AnonymousIndividual extends Data {
        @Override
        default boolean isAnonymousIndividual() {
            return true;
        }

        @Override
        default boolean isIndividual() {
            return true;
        }
    }

    public interface AnonymousClass extends Data {
        @Override
        default boolean isAnonymousClassExpression() {
            return true;
        }
    }

    public interface AnonymousRange extends Data {
        @Override
        default boolean isAnonymousDataRange() {
            return true;
        }
    }

    public interface AnnotationProperty extends EntityData {
        @Override
        default boolean isAnnotationProperty() {
            return true;
        }
    }

    public interface DataProperty extends EntityData {
        @Override
        default boolean isDatatypeProperty() {
            return true;
        }
    }

    public interface ObjectProperty extends EntityData {
        @Override
        default boolean isObjectProperty() {
            return true;
        }
    }

    public interface InverseObjectProperty extends Data {
        @Override
        default boolean isAnonymousProperty() {
            return true;
        }
    }

    public interface FRData extends Data {
        @Override
        default boolean isFacetRestriction() {
            return true;
        }
    }

    public interface SWRLVar extends Data {
        @Override
        default boolean isSWRLVariable() {
            return true;
        }
    }

    public interface SWRLIndividual extends Data {

        @Override
        default boolean isSWRLIndividual() {
            return true;
        }
    }

    public interface SWRLLiteral extends Data {

        @Override
        default boolean isSWRLLiteral() {
            return true;
        }
    }

    public interface SWRLAtomData extends Data {

        @Override
        default boolean isSWRLAtom() {
            return true;
        }
    }

    public interface LiteralData extends Data {
        @Override
        OWLLiteral create(OWLDataFactory df);

        @Override
        default void assertCheckProperties(OWLObject expected, OWLObject actual) {
            OWLLiteral left = (OWLLiteral) expected;
            OWLLiteral right = (OWLLiteral) actual;
            Assert.assertEquals(left.getLiteral(), right.getLiteral());
            Assert.assertEquals(left.getLang(), right.getLang());
            Assert.assertEquals(left.getDatatype(), right.getDatatype());
            Assert.assertEquals(left.isRDFPlainLiteral(), right.isRDFPlainLiteral());
            Assert.assertEquals(left.isBoolean(), right.isBoolean());
            Assert.assertEquals(left.isDouble(), right.isDouble());
            Assert.assertEquals(left.isFloat(), right.isFloat());
            Assert.assertEquals(left.isInteger(), right.isInteger());
            Assert.assertEquals(left.isLiteral(), right.isLiteral());
        }

        @Override
        default boolean isLiteral() {
            return true;
        }
    }
}
