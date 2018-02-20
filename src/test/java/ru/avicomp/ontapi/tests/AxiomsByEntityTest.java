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
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyModel;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
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
        OWLOntology expected = createOntology(OntManagers.createOWL());
        OWLOntology actual = createOntology(OntManagers.createONT());

        Set<OWLEntity> actualEntities = data.entities(actual).collect(Collectors.toSet());
        Set<OWLEntity> expectedEntities = data.entities(expected).collect(Collectors.toSet());
        Assert.assertEquals("Wrong " + data + " list", actualEntities, expectedEntities);

        Map<OWLPrimitive, Set<OWLAxiom>> actualReferencingAxioms = getReferencingAxiomsByClass(expectedEntities, actual);
        Map<OWLPrimitive, Set<OWLAxiom>> expectedRefAxioms = getReferencingAxiomsByClass(expectedEntities, expected);
        expectedEntities.forEach(e -> Assert.assertEquals(toString(actual) + " - wrong referencing axioms list for " + e + ":",
                expectedRefAxioms.get(e), actualReferencingAxioms.get(e)));

        Map<OWLEntity, Set<OWLAxiom>> actualDirectAxioms = getAxioms(expectedEntities, actual);
        Map<OWLEntity, Set<OWLAxiom>> expectedDirectAxioms = getAxioms(expectedEntities, expected);
        expectedEntities.forEach(c -> Assert.assertEquals(toString(actual) + " - wrong direct axioms list for " + data + ":" + c + ":",
                expectedDirectAxioms.get(c), actualDirectAxioms.get(c)));
    }

    public OWLOntology createOntology(OWLOntologyManager manager) {
        OWLOntology res = create(data.getURI(), manager);
        data.createTestAxioms().forEach(a -> manager.applyChange(new AddAxiom(res, a)));
        res.axioms().map(String::valueOf).forEach(LOGGER::debug);
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


    private Map<OWLEntity, Set<OWLAxiom>> getAxioms(Set<OWLEntity> entities, OWLOntology o) {
        return entities.stream().collect(Collectors.toMap(Function.identity(), c -> data.axiomsBy(c, o).collect(Collectors.toSet())));
    }

    private static Map<OWLPrimitive, Set<OWLAxiom>> getReferencingAxiomsByClass(Set<? extends OWLPrimitive> entities, OWLOntology o) {
        return entities.stream().collect(Collectors.toMap(Function.identity(),
                c -> o.referencingAxioms(c).collect(Collectors.toSet())));
    }

    public static String toString(OWLOntology o) {
        IRI iri = o.getOntologyID().getOntologyIRI().orElseThrow(AssertionError::new);
        return "[" + (o instanceof OntologyModel ? "ONT" : "OWL") + "]: <" + iri + ">";
    }

    /**
     * Created by @szuev on 20.02.2018.
     */
    public enum Entity {
        CLASS {
            @Override
            Stream<OWLAxiom> createTestAxioms() {
                String ns = getURI() + "#";
                OWLClass x = DATA_FACTORY.getOWLClass(IRI.create(ns + "X"));
                OWLObjectProperty p = DATA_FACTORY.getOWLObjectProperty(IRI.create(ns + "p"));
                OWLClass y = DATA_FACTORY.getOWLClass(IRI.create(ns + "Y"));
                OWLClass z = DATA_FACTORY.getOWLClass(IRI.create(ns + "Z"));
                OWLAxiom gca = DATA_FACTORY.getOWLSubClassOfAxiom(DATA_FACTORY.getOWLObjectIntersectionOf(DATA_FACTORY.getOWLObjectSomeValuesFrom(p, y), x), z);
                return Stream.of(gca);
            }

            @Override
            Stream<OWLEntity> entities(OWLOntology o) {
                return o.classesInSignature().map(x -> x);
            }

            @Override
            Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o) {
                return o.axioms((OWLClass) e);
            }
        },
        DATATYPE {
            @Override
            Stream<OWLAxiom> createTestAxioms() {
                String ns = getURI() + "#";
                OWLDatatype x = DATA_FACTORY.getOWLDatatype(IRI.create(ns + "x"));
                OWLDatatype y = DATA_FACTORY.getOWLDatatype(IRI.create(ns + "y"));
                OWLDataPropertyExpression p = DATA_FACTORY.getOWLDataProperty(IRI.create(ns + "y"));
                OWLAxiom a = DATA_FACTORY.getOWLDeclarationAxiom(x);
                OWLAxiom b = DATA_FACTORY.getOWLSubClassOfAxiom(DATA_FACTORY.getOWLDataAllValuesFrom(p, y), DATA_FACTORY.getOWLDataAllValuesFrom(p, x));
                return Stream.of(a, b);
            }

            @Override
            Stream<OWLEntity> entities(OWLOntology o) {
                return o.datatypesInSignature().map(x -> x);
            }

            @Override
            Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o) {
                return o.axioms((OWLDatatype) e);
            }
        },
        INDIVIDUAL {
            @Override
            Stream<OWLAxiom> createTestAxioms() {
                throw new UnsupportedOperationException("TODO");
            }

            @Override
            Stream<OWLEntity> entities(OWLOntology o) {
                throw new UnsupportedOperationException("TODO");
            }

            @Override
            Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o) {
                throw new UnsupportedOperationException("TODO");
            }
        },
        OBJECT_PROPERTY {
            @Override
            Stream<OWLAxiom> createTestAxioms() {
                String ns = getURI() + "#";
                OWLObjectProperty x = DATA_FACTORY.getOWLObjectProperty(IRI.create(ns + "x"));
                OWLObjectProperty p = DATA_FACTORY.getOWLObjectProperty(IRI.create(ns + "p"));
                OWLObjectProperty y = DATA_FACTORY.getOWLObjectProperty(IRI.create(ns + "y"));
                OWLObjectProperty z = DATA_FACTORY.getOWLObjectProperty(IRI.create(ns + "z"));
                OWLAxiom a = DATA_FACTORY.getOWLSubObjectPropertyOfAxiom(p, x);
                OWLAxiom b = DATA_FACTORY.getOWLEquivalentObjectPropertiesAxiom(y, z, DATA_FACTORY.getOWLObjectInverseOf(p), x);
                OWLAxiom c = DATA_FACTORY.getOWLSubPropertyChainOfAxiom(Arrays.asList(z, p), x);
                return Stream.of(a, b, c);
            }

            @Override
            Stream<OWLEntity> entities(OWLOntology o) {
                return o.objectPropertiesInSignature().map(x -> x);
            }

            @Override
            Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o) {
                return o.axioms((OWLObjectProperty) e);
            }
        },
        DATATYPE_PROPERTY {
            @Override
            Stream<OWLAxiom> createTestAxioms() {
                throw new UnsupportedOperationException("TODO");
            }

            @Override
            Stream<OWLEntity> entities(OWLOntology o) {
                throw new UnsupportedOperationException("TODO");
            }

            @Override
            Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o) {
                throw new UnsupportedOperationException("TODO");
            }
        },
        ANNOTATION_PROPERTY {
            @Override
            Stream<OWLAxiom> createTestAxioms() {
                throw new UnsupportedOperationException("TODO");
            }

            @Override
            Stream<OWLEntity> entities(OWLOntology o) {
                throw new UnsupportedOperationException("TODO");
            }

            @Override
            Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o) {
                throw new UnsupportedOperationException("TODO");
            }
        };


        private static final OWLDataFactory DATA_FACTORY = OntManagers.getDataFactory();

        abstract Stream<OWLAxiom> createTestAxioms();

        abstract Stream<OWLEntity> entities(OWLOntology o);

        abstract Stream<? extends OWLAxiom> axiomsBy(OWLEntity e, OWLOntology o);

        String getURI() {
            return "http://test.com/" + name();
        }

    }
}

