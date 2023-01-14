/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.tests.model;

import com.github.owlcs.ontapi.CommonOntologies;
import com.github.owlcs.ontapi.OntManagers;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.semanticweb.owlapi.model.HasClassesInSignature;
import org.semanticweb.owlapi.model.HasIRI;
import org.semanticweb.owlapi.model.HasObjectPropertiesInSignature;
import org.semanticweb.owlapi.model.HasSignature;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by @ssz on 05.05.2020.
 */
public class SearchByObjectTest {

    protected static OWLOntologyManager newManager() {
        return OntManagers.createManager();
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testDeclarations(TestData data) {
        data.doTest(T.DECLARATIONS, HasSignature::signature);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testAnnotationAssertionAxioms(TestData data) {
        OWLOntology ont = data.load(newManager());
        Set<OWLAnnotationSubject> entities = new HashSet<>();
        ont.signature().map(HasIRI::getIRI).forEach(entities::add);
        entities.add(IRI.create(RDFS.comment.getURI()));
        entities.add(IRI.create("http://" + RandomStringUtils.randomAlphabetic(12)));
        ont.anonymousIndividuals().forEach(entities::add);
        entities.add(ont.getOWLOntologyManager().getOWLDataFactory().getOWLAnonymousIndividual());
        data.getTester(T.ANNOTATION_ASSERTIONS_BY_SUBJECT).testAxiomsCounts(ont, x -> entities.stream());
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testSubClassAxiomsForSubClass(TestData data) {
        data.doTest(T.SUB_CLASS_OF_BY_SUBJECT, HasClassesInSignature::classesInSignature);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testSubClassAxiomsForSuperClass(TestData data) {
        data.doTest(T.SUB_CLASS_OF_BY_OBJECT, HasClassesInSignature::classesInSignature);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testEquivalentClassesAxioms(TestData data) {
        data.doTest(T.EQUIVALENT_CLASS_BY_OPERAND, HasClassesInSignature::classesInSignature);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testDisjointClassesAxioms(TestData data) {
        data.doTest(T.DISJOINT_CLASS_BY_OPERAND, HasClassesInSignature::classesInSignature);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testDataPropertyAssertionAxioms(TestData data) {
        data.doTest(T.DATA_PROPERTY_ASSERTION_BY_SUBJECT,
                x -> Stream.concat(x.individualsInSignature(), x.anonymousIndividuals()));
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testObjectPropertyAssertionAxioms(TestData data) {
        data.doTest(T.OBJECT_PROPERTY_ASSERTION_BY_SUBJECT,
                x -> Stream.concat(x.individualsInSignature(), x.anonymousIndividuals()));
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testObjectPropertyRangeAxioms(TestData data) {
        data.doTest(T.OBJECT_PROPERTY_RANGE_BY_SUBJECT, HasObjectPropertiesInSignature::objectPropertiesInSignature);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testObjectPropertyDomainAxioms(TestData data) {
        data.doTest(T.OBJECT_PROPERTY_DOMAIN_BY_SUBJECT, HasObjectPropertiesInSignature::objectPropertiesInSignature);
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testClassAssertionAxiomsForIndividual(TestData data) {
        data.doTest(T.CLASS_ASSERTION_BY_SUBJECT,
                x -> Stream.concat(x.individualsInSignature(), x.anonymousIndividuals()));
    }

    @ParameterizedTest
    @EnumSource(value = TestData.class)
    public void testClassAssertionAxiomsForClassExpression(TestData data) {
        data.doTest(T.CLASS_ASSERTION_BY_OBJECT, OWLObject::nestedClassExpressions);
    }

    enum TestData {
        PIZZA(CommonOntologies.PIZZA,
                T.DECLARATIONS.of(-5190508530L),
                T.ANNOTATION_ASSERTIONS_BY_SUBJECT.of(5847447319L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(23994790843L),
                T.SUB_CLASS_OF_BY_OBJECT.of(15875097811L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(743207879L),
                T.DISJOINT_CLASS_BY_OPERAND.of(39992865656L),
                T.DATA_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.OBJECT_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.CLASS_ASSERTION_BY_SUBJECT.of(-3513486065L),
                T.CLASS_ASSERTION_BY_OBJECT.of(-3513486065L),
                T.OBJECT_PROPERTY_RANGE_BY_SUBJECT.of(-5171741903L),
                T.OBJECT_PROPERTY_DOMAIN_BY_SUBJECT.of(-1378986021L)
        ),
        FAMILY(CommonOntologies.FAMILY,
                T.DECLARATIONS.of(34226271096L),
                T.ANNOTATION_ASSERTIONS_BY_SUBJECT.of(375920279L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-405443220L),
                T.SUB_CLASS_OF_BY_OBJECT.of(30468706L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(1149278276L),
                T.DISJOINT_CLASS_BY_OPERAND.of(-5870114142L),
                T.DATA_PROPERTY_ASSERTION_BY_SUBJECT.of(-46062903685L),
                T.OBJECT_PROPERTY_ASSERTION_BY_SUBJECT.of(-44647629109L),
                T.CLASS_ASSERTION_BY_SUBJECT.of(-2344424939L),
                T.CLASS_ASSERTION_BY_OBJECT.of(-2344424939L),
                T.OBJECT_PROPERTY_RANGE_BY_SUBJECT.of(11112139379L),
                T.OBJECT_PROPERTY_DOMAIN_BY_SUBJECT.of(-1766543680L)
        ),
        PEOPLE(CommonOntologies.PEOPLE,
                T.DECLARATIONS.of(-31040926516L),
                T.ANNOTATION_ASSERTIONS_BY_SUBJECT.of(8991342654L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-6044474129L),
                T.SUB_CLASS_OF_BY_OBJECT.of(-8307389053L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(229986282L),
                T.DISJOINT_CLASS_BY_OPERAND.of(5151062994L),
                T.DATA_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.OBJECT_PROPERTY_ASSERTION_BY_SUBJECT.of(-1008566325L),
                T.CLASS_ASSERTION_BY_SUBJECT.of(295642609L),
                T.CLASS_ASSERTION_BY_OBJECT.of(295642609L), // see https://github.com/owlcs/owlapi/issues/930
                T.OBJECT_PROPERTY_RANGE_BY_SUBJECT.of(-2697317876L),
                T.OBJECT_PROPERTY_DOMAIN_BY_SUBJECT.of(215400502L)
        ),
        CAMERA(CommonOntologies.CAMERA,
                T.DECLARATIONS.of(2967944221L),
                T.ANNOTATION_ASSERTIONS_BY_SUBJECT.of(),
                T.SUB_CLASS_OF_BY_SUBJECT.of(3537056616L),
                T.SUB_CLASS_OF_BY_OBJECT.of(5364459487L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(2619197590L),
                T.DISJOINT_CLASS_BY_OPERAND.of(),
                T.DATA_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.OBJECT_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.CLASS_ASSERTION_BY_SUBJECT.of(-546744276L),
                T.CLASS_ASSERTION_BY_OBJECT.of(-546744276L),
                T.OBJECT_PROPERTY_RANGE_BY_SUBJECT.of(-4417906154L),
                T.OBJECT_PROPERTY_DOMAIN_BY_SUBJECT.of(1118203354L)
        ),
        KOALA(CommonOntologies.KOALA,
                T.DECLARATIONS.of(6488467972L),
                T.ANNOTATION_ASSERTIONS_BY_SUBJECT.of(2255627747L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-4740693142L),
                T.SUB_CLASS_OF_BY_OBJECT.of(-6410317539L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(1433290824L),
                T.DISJOINT_CLASS_BY_OPERAND.of(3827692310L),
                T.DATA_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.OBJECT_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.CLASS_ASSERTION_BY_SUBJECT.of(-6315703213L),
                T.CLASS_ASSERTION_BY_OBJECT.of(-6315703213L),
                T.OBJECT_PROPERTY_RANGE_BY_SUBJECT.of(166159343L),
                T.OBJECT_PROPERTY_DOMAIN_BY_SUBJECT.of(1335386108L)
        ),
        TRAVEL(CommonOntologies.TRAVEL,
                T.DECLARATIONS.of(-25825023334L),
                T.ANNOTATION_ASSERTIONS_BY_SUBJECT.of(-1500714876L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(3792566851L),
                T.SUB_CLASS_OF_BY_OBJECT.of(1596226755L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(-2616901551L),
                T.DISJOINT_CLASS_BY_OPERAND.of(13371010920L),
                T.DATA_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.OBJECT_PROPERTY_ASSERTION_BY_SUBJECT.of(1580148819L),
                T.CLASS_ASSERTION_BY_SUBJECT.of(156661309L),
                T.CLASS_ASSERTION_BY_OBJECT.of(156661309L),
                T.OBJECT_PROPERTY_RANGE_BY_SUBJECT.of(3821903939L),
                T.OBJECT_PROPERTY_DOMAIN_BY_SUBJECT.of(-1174852943L)
        ),
        WINE(CommonOntologies.WINE,
                T.DECLARATIONS.of(20065711780L),
                T.ANNOTATION_ASSERTIONS_BY_SUBJECT.of(1282021579L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(23989074593L),
                T.SUB_CLASS_OF_BY_OBJECT.of(-2872929990L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(15637987080L),
                T.DISJOINT_CLASS_BY_OPERAND.of(-2886827780L),
                T.DATA_PROPERTY_ASSERTION_BY_SUBJECT.of(2039350484L),
                T.OBJECT_PROPERTY_ASSERTION_BY_SUBJECT.of(24229827352L),
                T.CLASS_ASSERTION_BY_SUBJECT.of(-13302103928L),
                T.CLASS_ASSERTION_BY_OBJECT.of(-13302103928L),
                T.OBJECT_PROPERTY_RANGE_BY_SUBJECT.of(-3535340229L),
                T.OBJECT_PROPERTY_DOMAIN_BY_SUBJECT.of(-435085258L)
        ),
        FOOD(CommonOntologies.FOOD,
                T.DECLARATIONS.of(6794851452L),
                T.ANNOTATION_ASSERTIONS_BY_SUBJECT.of(),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-2766054837L),
                T.SUB_CLASS_OF_BY_OBJECT.of(-490371437L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(16744408703L),
                T.DISJOINT_CLASS_BY_OPERAND.of(14957310010L),
                T.DATA_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.OBJECT_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.CLASS_ASSERTION_BY_SUBJECT.of(696330992L),
                T.CLASS_ASSERTION_BY_OBJECT.of(696330992L),
                T.OBJECT_PROPERTY_RANGE_BY_SUBJECT.of(-144528768L),
                T.OBJECT_PROPERTY_DOMAIN_BY_SUBJECT.of(4534229856L)
        ),
        NCBITAXON_CUT(CommonOntologies.NCBITAXON_CUT,
                T.DECLARATIONS.of(244310200631L),
                T.ANNOTATION_ASSERTIONS_BY_SUBJECT.of(120569949408L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-1220817325L),
                T.SUB_CLASS_OF_BY_OBJECT.of(-1220817325L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(),
                T.DISJOINT_CLASS_BY_OPERAND.of(-5419911878L),
                T.DATA_PROPERTY_ASSERTION_BY_SUBJECT.of(-22700580140L),
                T.OBJECT_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.CLASS_ASSERTION_BY_SUBJECT.of(),
                T.CLASS_ASSERTION_BY_OBJECT.of(),
                T.OBJECT_PROPERTY_RANGE_BY_SUBJECT.of(-5754775670L),
                T.OBJECT_PROPERTY_DOMAIN_BY_SUBJECT.of(-6535019425L)
        ),
        HP_CUT(CommonOntologies.HP_CUT,
                T.DECLARATIONS.of(-14640456193L),
                T.ANNOTATION_ASSERTIONS_BY_SUBJECT.of(2061724906L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-2245851740L),
                T.SUB_CLASS_OF_BY_OBJECT.of(-2245851740L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(-1475922263L),
                T.DISJOINT_CLASS_BY_OPERAND.of(),
                T.DATA_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.OBJECT_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.CLASS_ASSERTION_BY_SUBJECT.of(),
                T.CLASS_ASSERTION_BY_OBJECT.of(),
                T.OBJECT_PROPERTY_RANGE_BY_SUBJECT.of(),
                T.OBJECT_PROPERTY_DOMAIN_BY_SUBJECT.of()
        ),
        FAMILY_PEOPLE_UNION(CommonOntologies.FAMILY_PEOPLE_UNION,
                T.DECLARATIONS.of(-637777500L),
                T.ANNOTATION_ASSERTIONS_BY_SUBJECT.of(-1126745941L),
                T.SUB_CLASS_OF_BY_SUBJECT.of(-730374961L),
                T.SUB_CLASS_OF_BY_OBJECT.of(-730374961L),
                T.EQUIVALENT_CLASS_BY_OPERAND.of(1108552553L),
                T.DISJOINT_CLASS_BY_OPERAND.of(),
                T.DATA_PROPERTY_ASSERTION_BY_SUBJECT.of(-362479247L),
                T.OBJECT_PROPERTY_ASSERTION_BY_SUBJECT.of(),
                T.CLASS_ASSERTION_BY_SUBJECT.of(-25907713L),
                T.CLASS_ASSERTION_BY_OBJECT.of(-25907713L),
                T.OBJECT_PROPERTY_RANGE_BY_SUBJECT.of(),
                T.OBJECT_PROPERTY_DOMAIN_BY_SUBJECT.of()
        ),
        ;
        private final CommonOntologies resource;
        private final ByPrimitiveTester[] expectations;

        TestData(CommonOntologies data, ByPrimitiveTester... expectations) {
            this.resource = data;
            this.expectations = expectations;
        }

        public ByPrimitiveTester getTester(T type) {
            return Arrays.stream(expectations)
                    .filter(x -> x.type.equals(type.name()))
                    .findFirst().orElseThrow(IllegalArgumentException::new);
        }

        void doTest(T type, Function<OWLOntology, Stream<? extends OWLObject>> getPrimitives) {
            OWLOntology ont = load(newManager());
            getTester(type).testAxiomsCounts(ont, getPrimitives);
        }

        public OWLOntology load(OWLOntologyManager manager) {
            return resource.fetch(manager);
        }
    }

    enum T {
        DECLARATIONS {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.declarationAxioms((OWLEntity) param);
            }
        },
        ANNOTATION_ASSERTIONS_BY_SUBJECT {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.annotationAssertionAxioms((OWLAnnotationSubject) param);
            }
        },
        DATA_PROPERTY_ASSERTION_BY_SUBJECT {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.dataPropertyAssertionAxioms((OWLIndividual) param);
            }
        },
        OBJECT_PROPERTY_ASSERTION_BY_SUBJECT {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.objectPropertyAssertionAxioms((OWLIndividual) param);
            }
        },
        SUB_CLASS_OF_BY_SUBJECT {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.subClassAxiomsForSubClass((OWLClass) param);
            }
        },
        SUB_CLASS_OF_BY_OBJECT {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.subClassAxiomsForSuperClass((OWLClass) param);
            }
        },
        EQUIVALENT_CLASS_BY_OPERAND {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.equivalentClassesAxioms((OWLClass) param);
            }
        },
        DISJOINT_CLASS_BY_OPERAND {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.disjointClassesAxioms((OWLClass) param);
            }
        },
        CLASS_ASSERTION_BY_SUBJECT {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.classAssertionAxioms((OWLIndividual) param);
            }
        },
        CLASS_ASSERTION_BY_OBJECT {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.classAssertionAxioms((OWLClassExpression) param);
            }
        },
        OBJECT_PROPERTY_RANGE_BY_SUBJECT {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.objectPropertyRangeAxioms((OWLObjectPropertyExpression) param);
            }
        },
        OBJECT_PROPERTY_DOMAIN_BY_SUBJECT {
            @Override
            Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param) {
                return ont.objectPropertyDomainAxioms((OWLObjectPropertyExpression) param);
            }
        },
        ;

        private ByPrimitiveTester of() {
            return of(0);
        }

        private ByPrimitiveTester of(long count) {
            return new ByPrimitiveTester(name(), count, this::listAxioms);
        }

        abstract Stream<? extends OWLObject> listAxioms(OWLOntology ont, OWLObject param);
    }


}
