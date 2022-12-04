/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
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

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.model.OntAnnotationProperty;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntDataRange;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObject;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;
import com.github.owlcs.ontapi.jena.model.OntSWRL;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.SWRL;
import com.github.owlcs.ontapi.testutils.OWLIOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.XSD;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLNaryAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyCharacteristicAxiom;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * To test {@link com.github.owlcs.ontapi.internal.WithMerge#merge(ONTObject)}
 * Created by @ssz on 16.11.2019.
 */
public class ONTObjectMergeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ONTObjectMergeTest.class);

    private static <ONT extends OntObject, OWL extends OWLNaryAxiom<?>> void test(BiFunction<OntModel, String, ONT> addDeclaration,
                                                                                  BiConsumer<ONT, ONT> addAxioms,
                                                                                  int initModelSize,
                                                                                  AxiomType<OWL> type) {
        test(addDeclaration, addAxioms, initModelSize, type, 3, 3);
    }

    private static <ONT extends OntObject, OWL extends OWLAxiom> void test(BiFunction<OntModel, String, ONT> addDeclaration,
                                                                           BiConsumer<ONT, ONT> addAxioms,
                                                                           int initModelSize,
                                                                           AxiomType<OWL> type,
                                                                           int initAxiomsSize,
                                                                           int afterRemoveModelSize) {

        test(g -> {
            ONT x = addDeclaration.apply(g, "X");
            ONT y = addDeclaration.apply(g, "Y");
            addAxioms.accept(x, y);
        }, initModelSize, type, initAxiomsSize, afterRemoveModelSize);
    }

    private static <OWL extends OWLObjectPropertyCharacteristicAxiom> void test(Consumer<OntObjectProperty> prop,
                                                                                AxiomType<OWL> type) {
        test(g -> {
            OntObjectProperty.Named p = g.createObjectProperty("X");
            prop.accept(createInverse(p));
            prop.accept(createInverse(p));
        }, 6, type, 2, 2);
    }

    private static <OWL extends OWLAxiom> void test(Consumer<OntModel> contentMaker,
                                                    int initModelSize,
                                                    AxiomType<OWL> type,
                                                    int initAxiomsSize,
                                                    int afterRemoveModelSize) {
        OntologyManager m = OntManagers.createManager();
        Ontology o = m.createOntology();
        OntModel g = o.asGraphModel();

        contentMaker.accept(g);
        OWLIOUtils.print(g);

        Assertions.assertEquals(initModelSize, g.size());

        Assertions.assertEquals(initAxiomsSize, o.axioms().peek(a -> LOGGER.debug("Axiom: {}", a)).count());
        OWL a = o.axioms(type).findFirst().orElseThrow(AssertionError::new);

        o.remove(a);
        OWLIOUtils.print(g);

        Assertions.assertEquals(initAxiomsSize - 1, o.axioms().count());
        Assertions.assertEquals(afterRemoveModelSize, g.size());
    }

    private static OntObjectProperty createInverse(OntObjectProperty.Named p) {
        return p.getModel().createResource()
                .addProperty(OWL.inverseOf, p).as(OntObjectProperty.class);
    }

    @ParameterizedTest
    @EnumSource(value = Data.class)
    public void testMerge(Data data) {
        data.doTest();
    }

    public enum Data {
        HEADER {
            @Override
            void doTest() {
                OntologyManager m = OntManagers.createManager();
                m.getOntologyConfigurator().setLoadAnnotationAxioms(false);
                Ontology o = m.createOntology();
                OntModel g = o.asGraphModel();
                // two identical annotations, but one is assertion, and the second one is bulk
                g.getID().addComment("x");
                g.asStatement(g.getID().getMainStatement().asTriple()).annotate(g.getRDFSComment(), "x");
                OWLIOUtils.print(g);

                // in OWL-view must be one (merged) annotation:
                List<OWLAnnotation> owl = o.annotationsAsList();
                Assertions.assertEquals(1, owl.size());

                @SuppressWarnings("unchecked")
                ONTObject<OWLAnnotation> ont = (ONTObject<OWLAnnotation>) owl.get(0);
                Model res = ModelFactory.createModelForGraph(ont.toGraph()).setNsPrefixes(OntModelFactory.STANDARD);
                OWLIOUtils.print(res);

                m.applyChange(new RemoveOntologyAnnotation(o, ont.getOWLObject()));
                Assertions.assertEquals(1, g.size());
            }
        },

        EQUIVALENT_CLASSES {
            @Override
            void doTest() {
                OntologyManager m = OntManagers.createManager();
                Ontology o = m.createOntology();
                OntModel g = o.asGraphModel();

                OntClass x = g.createOntClass("X");
                OntClass y = g.createOntClass("Y");
                OntClass z = g.createOntClass("Z");
                x.addEquivalentClass(y.addEquivalentClass(x)).addEquivalentClass(z);
                OWLIOUtils.print(g);

                Assertions.assertEquals(5, o.axioms().count());

                DataFactory df = m.getOWLDataFactory();
                OWLEquivalentClassesAxiom xz = o.axioms(AxiomType.EQUIVALENT_CLASSES)
                        .filter(a -> a.contains(df.getOWLClass(z.getURI()))).findFirst().orElseThrow(AssertionError::new);
                OWLEquivalentClassesAxiom xy = o.axioms(AxiomType.EQUIVALENT_CLASSES)
                        .filter(a -> a.contains(df.getOWLClass(y.getURI()))).findFirst().orElseThrow(AssertionError::new);
                Assertions.assertTrue(xy.containsEntityInSignature(df.getOWLClass(x.getURI())));

                @SuppressWarnings("unchecked")
                ONTObject<OWLEquivalentClassesAxiom> xzOnt = (ONTObject<OWLEquivalentClassesAxiom>) xz;
                @SuppressWarnings("unchecked")
                ONTObject<OWLEquivalentClassesAxiom> xyOnt = (ONTObject<OWLEquivalentClassesAxiom>) xy;

                Assertions.assertEquals(3, xzOnt.triples().count());

                // can't test carefully, since no method to get value (merged axiom), only keys are available:
                Assertions.assertEquals(3, xyOnt.triples().count());
                // but can delete axiom with all its triples
                o.remove(xyOnt.getOWLObject());

                OWLIOUtils.print(g);
                Assertions.assertEquals(4, o.axioms().count());
                Assertions.assertEquals(1, o.axioms(AxiomType.EQUIVALENT_CLASSES).count());
                // header + "<X> owl:equivalentClass <Z>" + 3 declarations
                Assertions.assertEquals(5, g.size());
                Assertions.assertEquals(1, g.statements(null, OWL.equivalentClass, null).count());
            }
        },

        SUBCLASS_OF {
            @Override
            void doTest() {
                test(g -> {
                    OntClass x = g.createOntClass("X");
                    OntClass y = g.createOntClass("Y");
                    x.addSuperClass(g.createObjectComplementOf(y));
                    x.addSuperClass(g.createObjectComplementOf(y));
                }, 9, AxiomType.SUBCLASS_OF, 3, 3);
            }
        },

        INVERSE_OBJECT_PROPERTIES {
            @Override
            void doTest() {
                test(g -> {
                    OntObjectProperty x = g.createObjectProperty("X");
                    OntObjectProperty y = g.createObjectProperty("Y");
                    x.addInverseProperty(y.addInverseProperty(x));
                }, 5, AxiomType.INVERSE_OBJECT_PROPERTIES, 3, 3);
            }
        },

        SUB_OBJECT_PROPERTY {
            @Override
            void doTest() {
                test(g -> {
                    OntObjectProperty.Named x = g.createObjectProperty("X");
                    OntObjectProperty.Named y = g.createObjectProperty("Y");
                    createInverse(x).addSuperProperty(createInverse(y));
                    createInverse(x).addSuperProperty(createInverse(y));
                }, 9, AxiomType.SUB_OBJECT_PROPERTY, 3, 3);
            }
        },

        NEGATIVE_OBJECT_PROPERTY_ASSERTION {
            @Override
            void doTest() {
                OntologyManager m = OntManagers.createManager();
                Ontology o = m.createOntology();
                OntModel g = o.asGraphModel();

                OntObjectProperty op = g.createObjectProperty("OP");
                OntAnnotationProperty ap = g.createAnnotationProperty("AP");
                OntIndividual i1 = g.createIndividual("I1");
                OntIndividual i2 = g.createIndividual("I2");

                // 10 + 10 triples
                op.addNegativeAssertion(i1, i2).addAnnotation(ap, "comm1", "x").addAnnotation(g.getRDFSComment(), "comm2");
                op.addNegativeAssertion(i1, i2).addAnnotation(ap, "comm1", "x").addAnnotation(g.getRDFSComment(), "comm2");
                // 4 triples
                op.addNegativeAssertion(i1, i2);
                OWLIOUtils.print(g);
                Assertions.assertEquals(29, g.size());

                Assertions.assertEquals(2, o.axioms(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION).count());
                OWLAxiom a1 = o.axioms(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION).filter(x -> !x.isAnnotated())
                        .findFirst().orElseThrow(AssertionError::new);
                OWLAxiom a2 = o.axioms(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION).filter(OWLAxiom::isAnnotated)
                        .findFirst().orElseThrow(AssertionError::new);
                // 4 from OntNPA + 3 declarations
                Assertions.assertEquals(7, ((ONTObject<?>) a1).triples().count());

                o.remove(a2);
                Assertions.assertEquals(9, g.size());

                o.remove(a1);
                Assertions.assertEquals(5, g.size());
            }
        },

        NEGATIVE_DATA_PROPERTY_ASSERTION {
            @Override
            void doTest() {
                test(g -> {
                    OntDataProperty p = g.createDataProperty("X");
                    OntIndividual i = g.createIndividual("I");
                    p.addNegativeAssertion(i, g.createTypedLiteral(1));
                    p.addNegativeAssertion(i, g.createTypedLiteral(1));

                }, 11, AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION, 3, 3);
            }
        },

        DISJOINT_CLASSES {
            @Override
            void doTest() {
                test(OntModel::createOntClass,
                        (x, y) -> x.addDisjointClass(y.addDisjointClass(x)).getModel().createDisjointClasses(x, y),
                        11, AxiomType.DISJOINT_CLASSES);
            }
        },

        SUB_PROPERTY_CHAIN_OF {
            @Override
            void doTest() {
                test(g -> {
                    OntObjectProperty.Named x = g.createObjectProperty("P");
                    OntObjectProperty.Named y = g.createObjectProperty("Y");
                    OntObjectProperty.Named z = g.createObjectProperty("Z");
                    createInverse(x).addPropertyChain(y, z).addPropertyChain(y, z);
                    createInverse(x).addPropertyChain(y, z);
                }, 21, AxiomType.SUB_PROPERTY_CHAIN_OF, 4, 4);
            }
        },

        HAS_KEY {
            @Override
            void doTest() {
                test(g -> {
                    OntClass c = g.createOntClass("C");
                    OntObjectProperty x = g.createObjectProperty("P");
                    OntObjectProperty y = g.createObjectProperty("Y");
                    OntDataProperty z = g.createDataProperty("Z");
                    c.addHasKey(x, y, z).addHasKey(x, z, y, x);
                }, 19, AxiomType.HAS_KEY, 5, 5);
            }
        },

        DISJOINT_UNION {
            @Override
            void doTest() {
                test(g -> {
                    OntClass.Named c1 = g.createOntClass("C1");
                    OntClass c2 = g.createOntClass("C2");
                    OntClass c3 = g.createOntClass("C3");
                    c1.addDisjointUnion(g.createObjectComplementOf(c2), c3).addDisjointUnion(c3, g.createObjectComplementOf(c2));
                }, 18, AxiomType.DISJOINT_UNION, 4, 4);
            }
        },

        DIFFERENT_INDIVIDUALS {
            @Override
            void doTest() {
                test((g, u) -> g.getOWLThing().createIndividual(u),
                        (x, y) -> x.addDifferentIndividual(y.addDifferentIndividual(x))
                                .getModel().createDifferentIndividuals(x, y).getModel().createDifferentIndividuals(y, x),
                        19, AxiomType.DIFFERENT_INDIVIDUALS, 5, 5);
            }
        },

        SAME_INDIVIDUAL {
            @Override
            void doTest() {
                test((g, u) -> g.getOWLThing().createIndividual(u),
                        (x, y) -> x.addSameIndividual(y.addSameIndividual(x)),
                        7, AxiomType.SAME_INDIVIDUAL, 5, 5);
            }
        },

        DISJOINT_OBJECT_PROPERTIES {
            @Override
            void doTest() {
                test(OntModel::createObjectProperty,
                        (x, y) -> x.addDisjointProperty(y.addDisjointProperty(x)).getModel().createDisjointObjectProperties(x, y),
                        11, AxiomType.DISJOINT_OBJECT_PROPERTIES);
            }
        },

        EQUIVALENT_OBJECT_PROPERTIES {
            @Override
            void doTest() {
                test(OntModel::createObjectProperty,
                        (x, y) -> {
                            OntModel g = x.getModel();
                            x.addEquivalentPropertyStatement(y).addAnnotation(g.getRDFSComment(), "x");
                            y.addEquivalentPropertyStatement(x).addAnnotation(g.getRDFSComment(), "x");
                        }, 15, AxiomType.EQUIVALENT_OBJECT_PROPERTIES);
            }
        },

        DISJOINT_DATA_PROPERTIES {
            @Override
            void doTest() {
                test(OntModel::createDataProperty,
                        (x, y) -> x.addDisjointProperty(y.addDisjointProperty(x)).getModel().createDisjointDataProperties(x, y),
                        11, AxiomType.DISJOINT_DATA_PROPERTIES);
            }
        },

        EQUIVALENT_DATA_PROPERTIES {
            @Override
            void doTest() {
                test(OntModel::createDataProperty,
                        (x, y) -> {
                            OntModel g = x.getModel();
                            x.addEquivalentPropertyStatement(y).addAnnotation(g.getRDFSComment(), "x");
                            y.addEquivalentPropertyStatement(x).addAnnotation(g.getRDFSComment(), "x");
                        }, 15, AxiomType.EQUIVALENT_DATA_PROPERTIES);
            }
        },

        DATA_PROPERTY_DOMAIN {
            @Override
            void doTest() {
                test(g -> {
                    OntDataProperty p = g.createDataProperty("X");
                    p.addDomain(g.createObjectUnionOf(g.getOWLThing(), g.createDataHasValue(p, g.createLiteral("x"))))
                            .addDomain(g.createObjectUnionOf(g.getOWLThing(), g.createDataHasValue(p, g.createLiteral("x"))));

                }, 22, AxiomType.DATA_PROPERTY_DOMAIN, 2, 2);
            }
        },

        OBJECT_PROPERTY_DOMAIN {
            @Override
            void doTest() {
                test(g -> {
                    OntObjectProperty.Named p = g.createObjectProperty("X");
                    createInverse(p).addDomain(g.createObjectUnionOf(g.getOWLThing(), g.createObjectComplementOf(g.getOWLThing())));
                    createInverse(p).addDomain(g.createObjectUnionOf(g.getOWLThing(), g.createObjectComplementOf(g.getOWLThing())));
                }, 22, AxiomType.OBJECT_PROPERTY_DOMAIN, 2, 2);
            }
        },

        DATA_PROPERTY_RANGE {
            @Override
            void doTest() {
                test(g -> {
                    OntDataProperty p = g.createDataProperty("X");
                    p.addRange(g.createDataComplementOf(g.getDatatype(XSD.xboolean)))
                            .addRange(g.createDataComplementOf(g.getDatatype(XSD.xboolean)));

                }, 8, AxiomType.DATA_PROPERTY_RANGE, 2, 2);
            }
        },

        OBJECT_PROPERTY_RANGE {
            @Override
            void doTest() {
                test(g -> {
                    OntObjectProperty.Named p = g.createObjectProperty("X");
                    createInverse(p).addRange(g.createObjectIntersectionOf(g.getOWLThing(), g.createObjectComplementOf(g.getOWLThing())));
                    createInverse(p).addRange(g.createObjectIntersectionOf(g.getOWLThing(), g.createObjectComplementOf(g.getOWLThing())));
                }, 22, AxiomType.OBJECT_PROPERTY_RANGE, 2, 2);
            }
        },

        INVERSE_FUNCTIONAL_OBJECT_PROPERTY {
            @Override
            void doTest() {
                test(OntObjectProperty::addInverseFunctionalDeclaration, AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY);
            }
        },

        FUNCTIONAL_OBJECT_PROPERTY {
            @Override
            void doTest() {
                test(OntObjectProperty::addFunctionalDeclaration, AxiomType.FUNCTIONAL_OBJECT_PROPERTY);
            }
        },

        SYMMETRIC_OBJECT_PROPERTY {
            @Override
            void doTest() {
                test(OntObjectProperty::addSymmetricDeclaration, AxiomType.SYMMETRIC_OBJECT_PROPERTY);
            }
        },

        ASYMMETRIC_OBJECT_PROPERTY {
            @Override
            void doTest() {
                test(OntObjectProperty::addAsymmetricDeclaration, AxiomType.ASYMMETRIC_OBJECT_PROPERTY);
            }
        },

        TRANSITIVE_OBJECT_PROPERTY {
            @Override
            void doTest() {
                test(OntObjectProperty::addTransitiveDeclaration, AxiomType.TRANSITIVE_OBJECT_PROPERTY);
            }
        },

        REFLEXIVE_OBJECT_PROPERTY {
            @Override
            void doTest() {
                test(OntObjectProperty::addReflexiveDeclaration, AxiomType.REFLEXIVE_OBJECT_PROPERTY);
            }
        },

        IRREFLEXIVE_OBJECT_PROPERTY {
            @Override
            void doTest() {
                test(OntObjectProperty::addIrreflexiveDeclaration, AxiomType.IRREFLEXIVE_OBJECT_PROPERTY);
            }
        },

        DATATYPE_DEFINITION {
            @Override
            void doTest() {
                test(g -> {
                    OntDataRange dt = g.createDatatype("X");
                    dt.asNamed().addEquivalentClass(g.createDataComplementOf(g.getDatatype(XSD.xlong)));
                    dt.asNamed().addEquivalentClass(g.createDataComplementOf(g.getDatatype(XSD.xlong)));
                }, 8, AxiomType.DATATYPE_DEFINITION, 2, 2);
            }
        },

        CLASS_ASSERTION {
            @Override
            void doTest() {
                test(g -> {
                    OntIndividual i = g.createIndividual("X");
                    i.addClassAssertion(g.createObjectComplementOf(g.getOWLThing()));
                    i.addClassAssertion(g.createObjectComplementOf(g.getOWLThing()));
                }, 8, AxiomType.CLASS_ASSERTION, 2, 2);
            }
        },

        SWRL_RULE {
            @Override
            void doTest() {
                test(g -> {
                    g.setNsPrefix("s", SWRL.NS);
                    OntSWRL.Atom<?> a1 = g.createClassSWRLAtom(g.createOntClass("X"), g.createSWRLVariable("x"));
                    OntSWRL.Atom<?> a2 = g.createClassSWRLAtom(g.createOntClass("X"), g.createSWRLVariable("x"));
                    g.createSWRLImp(Arrays.asList(a1, a1), Collections.emptyList());
                    g.createSWRLImp(Arrays.asList(a1, a2), Collections.emptyList());
                }, 27, AxiomType.SWRL_RULE, 2, 2);
            }
        },
        ;

        void doTest() {
            Assertions.fail("Not supported");
        }
    }
}
