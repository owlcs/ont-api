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

package ru.avicomp.ontapi.tests.model;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyModel;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To test following {@link OWLAxiomCollection}#axioms methods:
 * <ul>
 * <li>{@link ru.avicomp.ontapi.OntBaseModelImpl#axioms(OWLClass)}</li>
 * <li>{@link ru.avicomp.ontapi.OntBaseModelImpl#axioms(OWLDatatype)}</li>
 * <li>{@link ru.avicomp.ontapi.OntBaseModelImpl#axioms(OWLIndividual)}</li>
 * <li>{@link ru.avicomp.ontapi.OntBaseModelImpl#axioms(OWLAnnotationProperty)}</li>
 * <li>{@link ru.avicomp.ontapi.OntBaseModelImpl#axioms(OWLDataProperty)}</li>
 * <li>{@link ru.avicomp.ontapi.OntBaseModelImpl#axioms(OWLObjectPropertyExpression)}</li>
 * </ul>
 * Also to test {@link ru.avicomp.ontapi.OntBaseModelImpl#referencingAxioms(OWLPrimitive)} and
 * {@link OWLAxiomCollection}#signature methods.
 * <p>
 * Created by @szuev on 20.02.2018.
 */
@RunWith(Parameterized.class)
public class AxiomsByEntityTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AxiomsByEntityTest.class);

    private final Entity data;

    public AxiomsByEntityTest(Entity data) {
        this.data = data;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Entity[] data() {
        return Entity.values();
    }

    @Test
    public void testAxioms() {
        List<OWLAxiom> axioms = data.createTestAxioms().peek(x -> LOGGER.debug("ADD: {}", x)).collect(Collectors.toList());

        OWLOntology expected = data.createOntology(OntManagers.createOWL(), axioms);
        OntologyModel actual = (OntologyModel) data.createOntology(OntManagers.createONT(), axioms);

        data.testAxioms(expected, actual);

        Set<OWLEntity> entities = data.testEntities(expected, actual);

        data.testReferencingAxioms(entities, expected, actual);

        data.testAxiomsBy(entities, expected, actual);
    }

    /**
     * Created by @szuev on 20.02.2018.
     */
    public enum Entity {
        CLASS {
            @Override
            Stream<OWLAxiom> createTestAxioms() {
                OWLObjectProperty p = FACTORY.getOWLObjectProperty(iri("p"));
                OWLClass x = FACTORY.getOWLClass(iri("X"));
                OWLClass y = FACTORY.getOWLClass(iri("Y"));
                OWLClass z = FACTORY.getOWLClass(iri("Z"));
                OWLClass h = FACTORY.getOWLClass(iri("H"));
                OWLClass r = FACTORY.getOWLClass(iri("R"));
                OWLClass s = FACTORY.getOWLClass(iri("S"));
                OWLAxiom sub = FACTORY.getOWLSubClassOfAxiom(FACTORY
                        .getOWLObjectIntersectionOf(FACTORY.getOWLObjectSomeValuesFrom(p, y), x), z);
                OWLAxiom eq = FACTORY.getOWLEquivalentClassesAxiom(x, y, FACTORY.getOWLThing());
                OWLAxiom dis = FACTORY.getOWLDisjointClassesAxiom(Arrays.asList(h, r, z),
                        Collections.singleton(FACTORY.getRDFSLabel("dis")));
                OWLAxiom un = FACTORY.getOWLDisjointUnionAxiom(s, Arrays.asList(x, y, r),
                        Arrays.asList(FACTORY.getRDFSLabel("un"), FACTORY.getRDFSComment("com")));
                return Stream.of(sub, eq, dis, un);
            }

            @Override
            Stream<OWLEntity> entities(OWLOntology o) {
                return o.classesInSignature().map(x -> x);
            }

            /**
             * Conditions:
             * <ul>
             * <li>Subclass axioms where the subclass is equal to the specified class</li>
             * <li>Equivalent class axioms where the specified class is an operand in the equivalent class axiom</li>
             * <li>Disjoint class axioms where the specified class is an operand in the disjoint class axiom</li>
             * <li>Disjoint union axioms, where the specified class is the named class that is equivalent to the disjoint union</li>
             * </ul>
             *
             * @param e {@link OWLClass}
             * @param o {@link OWLOntology}
             * @return Stream
             */
            @Override
            Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o) {
                return o.axioms((OWLClass) e);
            }
        },

        DATATYPE {
            @Override
            Stream<OWLAxiom> createTestAxioms() {
                OWLDatatype x = FACTORY.getOWLDatatype(iri("X"));
                OWLDatatype y = FACTORY.getOWLDatatype(iri("Y"));
                OWLDatatype z = FACTORY.getOWLDatatype(iri("Z"));
                OWLDatatype q = FACTORY.getOWLDatatype(iri("Q"));
                OWLClass c = FACTORY.getOWLClass(iri("C"));
                OWLDataPropertyExpression p = FACTORY.getOWLDataProperty(iri("p"));
                OWLDataPropertyExpression r = FACTORY.getOWLDataProperty(iri("r"));
                OWLAxiom dec = FACTORY.getOWLDeclarationAxiom(x);
                OWLAxiom eq = FACTORY.getOWLDatatypeDefinitionAxiom(y, OWL2Datatype.XSD_INTEGER);
                OWLAxiom ran = FACTORY.getOWLDataPropertyRangeAxiom(p,
                        FACTORY.getOWLDatatypeRestriction(z, OWLFacet.MAX_EXCLUSIVE, FACTORY.getOWLLiteral("lit", "no")),
                        Collections.singleton(FACTORY.getRDFSComment("com")));
                OWLAxiom sub = FACTORY.getOWLSubClassOfAxiom(c, FACTORY.getOWLDataMaxCardinality(12, r, q));
                return Stream.of(dec, eq, ran, sub);
            }

            @Override
            Stream<OWLEntity> entities(OWLOntology o) {
                return o.datatypesInSignature().map(x -> x);
            }

            /**
             * @param e {@link OWLDatatype}
             * @param o {@link OWLOntology}
             * @return Stream
             */
            @Override
            Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o) {
                return o.axioms((OWLDatatype) e);
            }
        },

        INDIVIDUAL {
            @Override
            Stream<OWLAxiom> createTestAxioms() {
                OWLClass c = FACTORY.getOWLClass(iri("C"));
                OWLIndividual i = FACTORY.getOWLNamedIndividual(iri("I"));
                OWLIndividual j = FACTORY.getOWLNamedIndividual(iri("J"));
                OWLIndividual k = FACTORY.getOWLNamedIndividual(iri("K"));
                OWLIndividual l = FACTORY.getOWLNamedIndividual(iri("L"));
                OWLIndividual r = FACTORY.getOWLNamedIndividual(iri("R"));
                OWLIndividual anon = FACTORY.getOWLAnonymousIndividual();
                OWLObjectProperty p1 = FACTORY.getOWLObjectProperty(iri("p1"));
                OWLDataProperty p2 = FACTORY.getOWLDataProperty(iri("p2"));
                OWLAnnotationProperty p3 = FACTORY.getOWLAnnotationProperty(iri("p3"));
                OWLObjectProperty p4 = FACTORY.getOWLObjectProperty(iri("p4"));
                OWLDataProperty p5 = FACTORY.getOWLDataProperty(iri("p5"));
                OWLAnnotation a = FACTORY.getOWLAnnotation(p3, FACTORY.getOWLLiteral(true));
                OWLAnnotation b = FACTORY.getOWLAnnotation(p3, iri("iri"), FACTORY.getRDFSComment("c"));
                OWLAxiom as = FACTORY.getOWLClassAssertionAxiom(c, i, Arrays.asList(a, b));
                OWLAxiom sa = FACTORY.getOWLSameIndividualAxiom(j, k, anon);
                OWLAxiom dif = FACTORY.getOWLSameIndividualAxiom(j, r, FACTORY.getOWLAnonymousIndividual());
                OWLAxiom opa = FACTORY.getOWLObjectPropertyAssertionAxiom(p1, l, anon);
                OWLAxiom dpa = FACTORY.getOWLDataPropertyAssertionAxiom(p2, l, 12.2);
                OWLAxiom nop1 = FACTORY.getOWLNegativeObjectPropertyAssertionAxiom(p4, i, j);
                OWLAxiom nop2 = FACTORY.getOWLNegativeObjectPropertyAssertionAxiom(p4, anon, r);
                OWLAxiom nod = FACTORY.getOWLNegativeDataPropertyAssertionAxiom(p5, i, FACTORY.getOWLLiteral(2.3f));
                return Stream.of(as, sa, dif, opa, dpa, nop2, nop1, nod);
            }

            @Override
            Stream<OWLEntity> entities(OWLOntology o) {
                // todo: anonymous individuals ?
                return o.individualsInSignature().map(x -> x);
            }

            /**
             * Conditions:
             * <ul>
             *  <li>Individual type assertions that assert the type of the specified individual</li>
             *  <li>Same individuals axioms that contain the specified individual</li>
             *  <li>Different individuals axioms that contain the specified individual</li>
             *  <li>Object property assertion axioms whose subject is the specified individual</li>
             *  <li>Data property assertion axioms whose subject is the specified individual</li>
             *  <li>Negative object property assertion axioms whose subject is the specified individual</li>
             *  <li>Negative data property assertion axioms whose subject is the specified individual</li>
             * </ul>
             * @param e {@link OWLIndividual}
             * @param o {@link OWLOntology}
             * @return Stream
             */
            @Override
            Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o) {
                return o.axioms((OWLIndividual) e);
            }

            @Override
            void assertAxioms(String message, Collection<OWLAxiom> expected, Collection<OWLAxiom> actual) {
                Set<String> _actual = actual.stream().map(Entity::toString).collect(Collectors.toSet());
                Set<String> _expected = expected.stream().map(Entity::toString).collect(Collectors.toSet());
                Assert.assertEquals(message, _expected, _actual);
            }
        },

        OBJECT_PROPERTY {
            @Override
            Stream<OWLAxiom> createTestAxioms() {
                OWLClass c = FACTORY.getOWLClass(iri("C"));
                OWLObjectProperty x = FACTORY.getOWLObjectProperty(iri("x"));
                OWLObjectProperty p = FACTORY.getOWLObjectProperty(iri("p"));
                OWLObjectProperty y = FACTORY.getOWLObjectProperty(iri("y"));
                OWLObjectProperty z = FACTORY.getOWLObjectProperty(iri("z"));
                OWLObjectProperty k = FACTORY.getOWLObjectProperty(iri("k"));
                OWLObjectProperty l = FACTORY.getOWLObjectProperty(iri("l"));
                OWLObjectProperty m = FACTORY.getOWLObjectProperty(iri("m"));
                OWLObjectProperty v = FACTORY.getOWLObjectProperty(iri("v"));
                OWLObjectProperty h = FACTORY.getOWLObjectProperty(iri("h"));
                OWLObjectProperty w = FACTORY.getOWLObjectProperty(iri("w"));

                OWLAxiom sub = FACTORY.getOWLSubObjectPropertyOfAxiom(p, x);
                OWLAxiom eq = FACTORY.getOWLEquivalentObjectPropertiesAxiom(y, z, FACTORY.getOWLObjectInverseOf(p), x);
                OWLAxiom chain = FACTORY.getOWLSubPropertyChainOfAxiom(Arrays.asList(z, k, p), x);
                OWLAxiom dis = FACTORY.getOWLDisjointObjectPropertiesAxiom(p, l);
                OWLAxiom ran = FACTORY.getOWLObjectPropertyRangeAxiom(k, c);
                OWLAxiom dom = FACTORY.getOWLObjectPropertyDomainAxiom(m, c);

                OWLAxiom fun = FACTORY.getOWLFunctionalObjectPropertyAxiom(y);
                OWLAxiom ref = FACTORY.getOWLReflexiveObjectPropertyAxiom(z);
                OWLAxiom ir = FACTORY.getOWLIrreflexiveObjectPropertyAxiom(v);
                OWLAxiom as = FACTORY.getOWLAsymmetricObjectPropertyAxiom(w);
                OWLAxiom sy = FACTORY.getOWLSymmetricObjectPropertyAxiom(k);
                OWLAxiom in = FACTORY.getOWLInverseFunctionalObjectPropertyAxiom(m);
                OWLAxiom tr = FACTORY.getOWLTransitiveObjectPropertyAxiom(y);
                OWLAxiom ina = FACTORY.getOWLInverseObjectPropertiesAxiom(h, z);
                return Stream.of(sub, eq, chain, dis, ran, dom, fun, ref, ir, as, sy, in, tr, ina);
            }

            @Override
            Stream<OWLEntity> entities(OWLOntology o) {
                // todo: inverse object properties ?
                return o.objectPropertiesInSignature().map(x -> x);
            }

            /**
             * Conditions:
             * <ul>
             *  <li>Sub-property axioms where the sub property is the specified property</li>
             *  <li>Equivalent property axioms where the axiom contains the specified property</li>
             *  <li>Equivalent property axioms that contain the inverse of the specified property</li>
             *  <li>Disjoint property axioms that contain the specified property</li>
             *  <li>Domain axioms that specify a domain of the specified property</li>
             *  <li>Range axioms that specify a range of the specified property</li>
             *  <li>Any property characteristic axiom (i.e. Functional, Symmetric, Reflexive etc.) whose subject is the specified property</li>
             *  <li>Inverse properties axioms that contain the specified property</li>
             * </ul>
             * @param e {@link OWLObjectPropertyExpression}
             * @param o {@link OWLOntology}
             * @return Stream
             */
            @Override
            Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o) {
                return o.axioms((OWLObjectPropertyExpression) e);
            }
        },

        DATATYPE_PROPERTY {
            @Override
            Stream<OWLAxiom> createTestAxioms() {
                OWLClass c = FACTORY.getOWLClass(iri("C"));
                OWLDataProperty a = FACTORY.getOWLDataProperty(iri("a"));
                OWLDataProperty b = FACTORY.getOWLDataProperty(iri("b"));
                OWLDataProperty x = FACTORY.getOWLDataProperty(iri("x"));
                OWLDataProperty d = FACTORY.getOWLDataProperty(iri("d"));

                OWLAxiom sub = FACTORY.getOWLSubDataPropertyOfAxiom(a, FACTORY.getOWLTopDataProperty());
                OWLAxiom eq = FACTORY.getOWLEquivalentDataPropertiesAxiom(a, b, x);
                OWLAxiom di = FACTORY.getOWLDisjointDataPropertiesAxiom(d, d);
                OWLAxiom dom = FACTORY.getOWLDataPropertyDomainAxiom(x, c);
                OWLAxiom ra = FACTORY.getOWLDataPropertyRangeAxiom(x, OWL2Datatype.RDF_XML_LITERAL);
                OWLAxiom fun = FACTORY.getOWLFunctionalDataPropertyAxiom(b);
                return Stream.of(sub, eq, di, dom, ra, fun);
            }

            @Override
            Stream<OWLEntity> entities(OWLOntology o) {
                return o.dataPropertiesInSignature().map(x -> x);
            }

            /*
             * Conditions:
             * <ul>
             *  <li>Sub-property axioms where the sub property is the specified property</li>
             *  <li>Equivalent property axioms where the axiom contains the specified property</li>
             *  <li>Disjoint property axioms that contain the specified property</li>
             *  <li>Domain axioms that specify a domain of the specified property</li>
             *  <li>Range axioms that specify a range of the specified property</li>
             *  <li>Functional data property characteristic axiom whose subject is the specified property</li>
             * </ul>
             *
             * @param e {@link OWLDataProperty}
             * @param o {@link OWLOntology}
             * @return Stream
             */
            @Override
            Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o) {
                return o.axioms((OWLDataProperty) e);
            }
        },

        ANNOTATION_PROPERTY {
            @Override
            Stream<OWLAxiom> createTestAxioms() {
                OWLAnnotationProperty a = FACTORY.getOWLAnnotationProperty(iri("a"));
                OWLAnnotationProperty b = FACTORY.getOWLAnnotationProperty(iri("b"));
                OWLAnnotationProperty c = FACTORY.getOWLAnnotationProperty(iri("c"));
                OWLAnnotation an1 = FACTORY.getOWLAnnotation(a, FACTORY.getOWLLiteral("", "n"));
                OWLAnnotation an2 = FACTORY.getOWLAnnotation(FACTORY.getRDFSIsDefinedBy(), FACTORY.getOWLLiteral(false),
                        FACTORY.getOWLAnnotation(FACTORY.getRDFSLabel(), iri("iri1")));
                OWLAxiom sub1 = FACTORY.getOWLSubAnnotationPropertyOfAxiom(a, FACTORY.getRDFSSeeAlso(), Arrays.asList(an1, an2));
                OWLAxiom sub2 = FACTORY.getOWLSubAnnotationPropertyOfAxiom(b, a);
                OWLAxiom dom = FACTORY.getOWLAnnotationPropertyDomainAxiom(a, b.getIRI());
                OWLAxiom ran = FACTORY.getOWLAnnotationPropertyRangeAxiom(c, iri("iri2"));
                return Stream.of(sub1, sub2, dom, ran);
            }

            @Override
            Stream<OWLEntity> entities(OWLOntology o) {
                return o.annotationPropertiesInSignature().map(x -> x);
            }

            /**
             * <ul>
             *  <li>Annotation subPropertyOf axioms where the specified property is the sub property</li>
             *  <li>Annotation property domain axioms that specify a domain for the specified property</li>
             *  <li>Annotation property range axioms that specify a range for the specified property</li>
             * </ul>
             * @param e {@link OWLAnnotationProperty}
             * @param o {@link OWLOntology}
             * @return Stream
             */
            @Override
            Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o) {
                return o.axioms((OWLAnnotationProperty) e);
            }
        };


        private static final OWLDataFactory FACTORY = OntManagers.getDataFactory();

        abstract Stream<OWLAxiom> createTestAxioms();

        abstract Stream<OWLEntity> entities(OWLOntology o);

        abstract Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o);

        String getURI() {
            return "http://test.com/" + name();
        }

        IRI iri(String name) {
            return IRI.create(getURI() + "#" + name);
        }

        Set<OWLEntity> testEntities(OWLOntology expected, OntologyModel actual) {
            Set<OWLEntity> actualEntities = entities(actual).collect(Collectors.toSet());
            Set<OWLEntity> expectedEntities = entities(expected).collect(Collectors.toSet());
            Assert.assertEquals(String.format("%s - wrong %s list:", toString(actual), this),
                    expectedEntities, actualEntities);
            return expectedEntities;
        }

        void testAxioms(OWLOntology expected, OntologyModel actual) {
            List<OWLAxiom> actualAxioms = actual.axioms().sorted().collect(Collectors.toList());
            List<OWLAxiom> expectedAxioms = expected.axioms().sorted().collect(Collectors.toList());
            assertAxioms(toString(actual) + " - wrong axioms list", expectedAxioms, actualAxioms);
        }

        void testReferencingAxioms(Set<OWLEntity> entities, OWLOntology expected, OntologyModel actual) {
            Map<OWLPrimitive, Set<OWLAxiom>> actualReferencingAxioms = getReferencingAxiomsByClass(entities, actual);
            Map<OWLPrimitive, Set<OWLAxiom>> expectedRefAxioms = getReferencingAxiomsByClass(entities, expected);
            entities.forEach(e -> assertAxioms(
                    String.format("%s - wrong referencing axioms list for %s %s:", toString(actual), this, e),
                    expectedRefAxioms.get(e), actualReferencingAxioms.get(e)));
        }

        void testAxiomsBy(Set<OWLEntity> entities, OWLOntology expected, OntologyModel actual) {
            Map<OWLEntity, Set<OWLAxiom>> actualDirectAxioms = getAxioms(entities, actual);
            Map<OWLEntity, Set<OWLAxiom>> expectedDirectAxioms = getAxioms(entities, expected);
            entities.forEach(c -> assertAxioms(
                    String.format("%s - wrong direct axioms list for %s %s:", toString(actual), this, c),
                    expectedDirectAxioms.get(c), actualDirectAxioms.get(c)));
        }

        void assertAxioms(String message, Collection<OWLAxiom> expected, Collection<OWLAxiom> actual) {
            Assert.assertEquals(message, expected, actual);
        }

        OWLOntology createOntology(OWLOntologyManager manager, List<OWLAxiom> axioms) {
            OWLOntology res = create(getURI(), manager);
            axioms.forEach(a -> manager.applyChange(new AddAxiom(res, a)));
            res.axioms().forEach(x -> LOGGER.debug("Added: {}", x));
            LOGGER.debug("{} created.", toString(res));
            return res;
        }

        private static OWLOntology create(String url, OWLOntologyManager manager) {
            try {
                return manager.createOntology(IRI.create(url));
            } catch (OWLOntologyCreationException e) {
                throw new AssertionError("Can't create ontology " + url, e);
            }
        }

        private static boolean isONT(OWLOntology o) {
            return o instanceof OntologyModel;
        }

        private static String toString(OWLOntology o) {
            IRI iri = o.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new);
            return "[" + (isONT(o) ? "ONT(Actual)" : "OWL") + "]: <" + iri + ">";
        }

        private static String toString(OWLAxiom a) {
            return a.toString().replaceAll("_:genid\\d+", "_:anon");
        }

        private static Map<OWLPrimitive, Set<OWLAxiom>> getReferencingAxiomsByClass(Set<? extends OWLPrimitive> entities, OWLOntology o) {
            return entities.stream().collect(Collectors.toMap(Function.identity(),
                    c -> o.referencingAxioms(c).collect(Collectors.toSet())));
        }

        private Map<OWLEntity, Set<OWLAxiom>> getAxioms(Set<OWLEntity> entities, OWLOntology o) {
            return entities.stream().collect(Collectors.toMap(Function.identity(), c -> axiomsBy(c, o).collect(Collectors.toSet())));
        }


    }
}

