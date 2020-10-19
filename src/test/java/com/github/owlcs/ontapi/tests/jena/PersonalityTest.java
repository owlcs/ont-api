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

package com.github.owlcs.ontapi.tests.jena;

import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.OntVocabulary;
import com.github.owlcs.ontapi.jena.impl.OntIndividualImpl;
import com.github.owlcs.ontapi.jena.impl.conf.*;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by @szuev on 27.03.2018.
 */
@SuppressWarnings("WeakerAccess")
public class PersonalityTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalityTest.class);

    @Test
    public void testPersonalityBuiltins() {
        Resource agent = FOAF.Agent;
        Resource document = FOAF.Document;
        String ns = "http://x#";
        Model g = ModelFactory.createDefaultModel()
                .setNsPrefixes(OntModelFactory.STANDARD)
                .setNsPrefix("x", ns)
                .setNsPrefix("foaf", FOAF.NS);
        String clazz = ns + "Class";
        g.createResource(clazz, OWL.Class).addProperty(RDFS.subClassOf, agent);
        ReadWriteUtils.print(g);

        OntPersonality p1 = PersonalityBuilder.from(OntModelConfig.ONT_PERSONALITY_STRICT)
                .setBuiltins(OntModelConfig.createBuiltinsVocabulary(OntVocabulary.Factory.OWL_VOCABULARY)).build();
        OntModel m1 = OntModelFactory.createModel(g.getGraph(), p1);
        Assertions.assertEquals(1, m1.classes().peek(x -> LOGGER.debug("Class::{}", x)).count());
        Assertions.assertNull(m1.getOntClass(agent));
        Assertions.assertNull(m1.getOntClass(document));
        Assertions.assertEquals(0, m1.getOntClass(clazz).superClasses().count());

        OntVocabulary SIMPLE_FOAF_VOC = OntVocabulary.Factory.create(OWL.Class, agent, document);
        OntVocabulary voc = OntVocabulary.Factory.create(OntVocabulary.Factory.OWL_VOCABULARY, SIMPLE_FOAF_VOC);
        OntPersonality p2 = PersonalityBuilder.from(OntModelConfig.ONT_PERSONALITY_STRICT)
                .setBuiltins(OntModelConfig.createBuiltinsVocabulary(voc)).build();
        OntModel m2 = OntModelFactory.createModel(g.getGraph(), p2);

        // listClasses only works with explicit owl-classes, it does not take into account builtins
        Assertions.assertEquals(1, m2.classes().peek(x -> LOGGER.debug("Class::{}", x)).count());
        Assertions.assertNotNull(m2.getOntClass(agent));
        Assertions.assertNotNull(m2.getOntClass(document));
        Assertions.assertEquals(1, m2.getOntClass(clazz).superClasses()
                .peek(x -> LOGGER.debug("SuperClass::{}", x)).count());
    }

    @Test
    public void testPersonalityReserved() {
        String ns = "http://x#";
        Model g = ModelFactory.createDefaultModel()
                .setNsPrefixes(OntModelFactory.STANDARD)
                .setNsPrefix("x", ns);
        Property p = g.createProperty(ns + "someProp");
        Resource individual = g.createResource(ns + "Individual", OWL.NamedIndividual);
        g.createResource().addProperty(OWL.sameAs, individual).addProperty(p, "Some assertion");
        ReadWriteUtils.print(g);

        OntModel m1 = OntModelFactory.createModel(g.getGraph());
        Assertions.assertEquals(2, m1.ontObjects(OntIndividual.class)
                .peek(x -> LOGGER.debug("1)Individual: {}", x)).count());

        OntVocabulary voc = OntVocabulary.Factory.create(RDF.Property, p);
        OntPersonality p2 = PersonalityBuilder.from(OntModelConfig.ONT_PERSONALITY_STRICT)
                .setReserved(OntModelConfig.createReservedVocabulary(voc)).build();
        OntModel m2 = OntModelFactory.createModel(g.getGraph(), p2);
        Assertions.assertEquals(1, m2.ontObjects(OntIndividual.class)
                .peek(x -> LOGGER.debug("2)Individual: {}", x)).count());
    }

    @Test
    public void testPersonalityPunnings() {
        String ns = "http://x#";
        OntModel m1 = OntModelFactory.createModel(org.apache.jena.graph.Factory.createGraphMem(), OntModelConfig.ONT_PERSONALITY_STRICT)
                .setNsPrefixes(OntModelFactory.STANDARD)
                .setNsPrefix("x", ns);
        OntClass.Named c1 = m1.createOntClass(ns + "C1");
        OntClass.Named c2 = m1.createOntClass(ns + "C2");
        OntIndividual i1 = c1.createIndividual(ns + "I1");
        OntIndividual i2 = c2.createIndividual(ns + "I2");
        c1.createIndividual(ns + "I3");
        m1.createDatatype(i2.getURI());
        m1.createOntClass(i1.getURI());
        ReadWriteUtils.print(m1);
        Assertions.assertEquals(3, m1.classes().peek(x -> LOGGER.debug("1)Class: {}", x)).count());
        Assertions.assertEquals(3, m1.individuals().peek(x -> LOGGER.debug("1)Individual: {}", x)).count());
        Assertions.assertEquals(1, m1.datatypes().peek(x -> LOGGER.debug("1)Datatype: {}", x)).count());

        OntPersonality.Punnings punnings = new OntPersonality.Punnings() {
            final OntPersonality.Punnings base = OntModelConfig.ONT_PERSONALITY_STRICT.getPunnings();

            @Override
            public Set<Node> get(Class<? extends OntObject> type) throws OntJenaException {
                if (OntIndividual.Named.class.equals(type)) {
                    return expand(type, OWL.Class, RDFS.Datatype);
                }
                if (OntClass.Named.class.equals(type) || OntDataRange.Named.class.equals(type)) {
                    return expand(type, OWL.NamedIndividual);
                }
                return base.get(type);
            }

            private Set<Node> expand(Class<? extends OntObject> type, Resource... additional) {
                Set<Node> res = new HashSet<>(base.get(type));
                Arrays.stream(additional).forEach(t -> res.add(t.asNode()));
                return Collections.unmodifiableSet(res);
            }
        };
        OntPersonality p2 = PersonalityBuilder.from(OntModelConfig.ONT_PERSONALITY_STRICT).setPunnings(punnings).build();
        OntEntity.listEntityTypes().forEachRemaining(t -> Assertions.assertEquals(2, p2.getPunnings().get(t).size()));

        OntModel m2 = OntModelFactory.createModel(m1.getGraph(), p2);
        Assertions.assertEquals(1, m2.individuals().peek(x -> LOGGER.debug("2)Individuals: {}", x)).count());
        Assertions.assertEquals(0, m2.datatypes().peek(x -> LOGGER.debug("2)Datatype: {}", x)).count());
        Assertions.assertEquals(2, m2.classes().peek(x -> LOGGER.debug("2)Classes: {}", x)).count());
    }

    @Test
    public void testClassDatatypePunn() {
        String ns = "http://ex.com#";
        OntModel m = OntModelFactory.createModel(OntModelFactory.createDefaultGraph(), OntModelConfig.ONT_PERSONALITY_LAX)
                .setNsPrefixes(OntModelFactory.STANDARD);
        OntClass c1 = m.createOntClass(ns + "class1");
        OntClass c2 = m.createOntClass(ns + "class2");
        OntIndividual i1 = c1.createIndividual(ns + "indi1");
        OntIndividual i2 = m.createObjectComplementOf(c1).createIndividual(ns + "indi2");
        OntIndividual i3 = c2.createIndividual(ns + "indi3");
        m.createDifferentIndividuals(i1, i2, i3);
        ReadWriteUtils.print(m);
        Assertions.assertEquals(0, m.datatypes().count());
        Assertions.assertEquals(2, m.classes().count());
        Assertions.assertEquals(1, m.ontObjects(OntDisjoint.Individuals.class).count());
        Assertions.assertEquals(3, m.ontObjects(OntClass.class).count());
        LOGGER.debug("===================");

        // add punn:
        m.createDatatype(ns + "class1");
        OntModel m2 = OntModelFactory.createModel(m.getBaseGraph(), OntModelConfig.ONT_PERSONALITY_STRICT);

        try {
            m2.createDatatype(ns + "class2");
            Assertions.fail("Possible to add punn");
        } catch (OntJenaException e) {
            LOGGER.debug(e.getMessage());
        }

        ReadWriteUtils.print(m2);
        Assertions.assertEquals(0, m2.datatypes().count());
        Assertions.assertEquals(1, m2.classes().count());
        Assertions.assertEquals(1, m2.ontObjects(OntDisjoint.Individuals.class).count());
        List<OntClass> ces = m2.ontObjects(OntClass.class).collect(Collectors.toList());
        Assertions.assertEquals(1, ces.size(), "Wrong ces list: " + ces);
        LOGGER.debug("===================");

        OntModel m3 = OntModelFactory.createModel(m2.getBaseGraph(), OntModelConfig.ONT_PERSONALITY_LAX);
        Assertions.assertEquals(1, m3.datatypes().count());
        Assertions.assertEquals(2, m3.classes().count());
        Assertions.assertEquals(1, m3.ontObjects(OntDisjoint.Individuals.class).count());
        Assertions.assertEquals(3, m3.ontObjects(OntClass.class).count());
    }

    @Test
    public void testPropertyPunn() {
        String ns = "http://ex.com#";
        OntModel m = OntModelFactory.createModel(OntModelFactory.createDefaultGraph(), OntModelConfig.ONT_PERSONALITY_LAX)
                .setNsPrefixes(OntModelFactory.STANDARD);
        OntClass c1 = m.createOntClass(ns + "class1");
        OntObjectProperty.Named p1 = m.createObjectProperty(ns + "prop1");
        OntObjectProperty p2 = m.createObjectProperty(ns + "prop2").createInverse();

        OntIndividual i1 = c1.createIndividual(ns + "indi1");
        OntIndividual i2 = m.createObjectComplementOf(c1).createIndividual(ns + "indi2");
        OntIndividual i3 = m.createObjectSomeValuesFrom(p1, c1).createIndividual(ns + "indi3");
        OntIndividual i4 = m.createObjectCardinality(p2, 1, c1).createIndividual(ns + "indi4");
        m.createDifferentIndividuals(i1, i2, i3, i4);
        ReadWriteUtils.print(m);
        Assertions.assertEquals(0, m.dataProperties().count());
        Assertions.assertEquals(0, m.annotationProperties().count());
        Assertions.assertEquals(2, m.objectProperties().count());
        Assertions.assertEquals(1, m.classes().count());
        Assertions.assertEquals(4, m.ontObjects(OntIndividual.class).count());
        Assertions.assertEquals(4, m.ontObjects(OntClass.class).count());
        LOGGER.debug("===================");

        // add punns:
        m.createDataProperty(ns + "prop1");
        m.createAnnotationProperty(ns + "prop2");
        OntModel m2 = OntModelFactory.createModel(m.getBaseGraph(), OntModelConfig.ONT_PERSONALITY_STRICT);

        try {
            m2.createDataProperty(ns + "prop2");
            Assertions.fail("Possible to add punn");
        } catch (OntJenaException e) {
            LOGGER.debug(e.getMessage());
        }

        ReadWriteUtils.print(m2);
        Assertions.assertEquals(0, m2.objectProperties().count());
        Assertions.assertEquals(0, m2.dataProperties().count());
        Assertions.assertEquals(0, m2.annotationProperties().count());
        List<OntClass> ces = m2.ontObjects(OntClass.class).collect(Collectors.toList());
        // no ObjectSomeValuesFrom, no ObjectCardinality
        Assertions.assertEquals(2, ces.size(), "Wrong ces list: " + ces);
        LOGGER.debug("===================");

        OntModel m3 = OntModelFactory.createModel(m2.getBaseGraph(), OntModelConfig.ONT_PERSONALITY_LAX);
        Assertions.assertEquals(1, m3.dataProperties().count());
        Assertions.assertEquals(1, m3.annotationProperties().count());
        Assertions.assertEquals(2, m3.objectProperties().count());
        Assertions.assertEquals(1, m3.ontObjects(OntDisjoint.Individuals.class).count());
        Assertions.assertEquals(4, m3.ontObjects(OntClass.class).count());
    }

    @Test
    public void testCustomPersonality() {
        OntPersonality personality = buildCustomPersonality();

        String ns = "http://ex.com#";
        OntModel m = OntModelFactory.createModel(OntModelFactory.createDefaultGraph(), OntModelConfig.ONT_PERSONALITY_LAX)
                .setNsPrefixes(OntModelFactory.STANDARD); // STANDARD PERSONALITY
        OntClass c1 = m.createOntClass(ns + "class1");
        OntDataProperty p1 = m.createDataProperty(ns + "prop1");
        OntDataRange.Named d1 = m.createDatatype(ns + "dt1");
        OntClass c2 = m.createDataAllValuesFrom(p1, d1);

        c1.createIndividual();
        c1.createIndividual(ns + "indi1");
        c2.createIndividual(ns + "indi2");
        m.createResource(ns + "indi3", c2);
        m.createResource(ns + "inid4", c1);

        ReadWriteUtils.print(m);
        Assertions.assertEquals(2, m.namedIndividuals().count());
        Assertions.assertEquals(5, m.ontObjects(OntIndividual.class).count());
        Assertions.assertEquals(5, m.individuals().count());

        LOGGER.debug("==================="); // CUSTOM PERSONALITY (owl:NamedIndividual is required)
        OntModel m2 = OntModelFactory.createModel(m.getGraph(), personality);
        Assertions.assertEquals(2, m2.namedIndividuals().count());
        Assertions.assertEquals(3, m2.ontObjects(OntIndividual.class).count());
        Resource indi5 = m.createResource(ns + "inid5", c2);
        Assertions.assertEquals(3, m2.ontObjects(OntIndividual.class).count());
        Assertions.assertEquals(3, m2.individuals().count());

        OntDisjoint.Individuals disjoint2 = m2.createDifferentIndividuals(m2.ontObjects(OntIndividual.class)
                .collect(Collectors.toList()));
        disjoint2.getList().as(RDFList.class).add(indi5);
        ReadWriteUtils.print(m2);
        Assertions.assertEquals(3, disjoint2.members().count());

        LOGGER.debug("==================="); // BACK TO STANDARD PERSONALITY
        OntModel m3 = OntModelFactory.createModel(m2.getGraph(), OntModelConfig.ONT_PERSONALITY_MEDIUM);
        Assertions.assertEquals(2, m3.namedIndividuals().count());
        Assertions.assertEquals(6, m3.ontObjects(OntIndividual.class).count());
        OntDisjoint.Individuals disjoint3 = m3.ontObjects(OntDisjoint.Individuals.class).findFirst()
                .orElseThrow(AssertionError::new);
        Assertions.assertEquals(4, disjoint3.members().count());
    }

    public static OntPersonality buildCustomPersonality() {
        LOGGER.debug("Create new Named Individual Factory");
        OntPersonality from = OntModelConfig.ONT_PERSONALITY_LAX;
        ObjectFactory factory = createNamedIndividualFactory();
        OntPersonality res = PersonalityBuilder.from(from)
                .add(OntIndividual.Named.class, factory)
                .build();
        Assertions.assertEquals(97, res.types().count());
        List<Class<? extends OntObject>> objects = res.types(OntObject.class).collect(Collectors.toList());
        List<Class<? extends OntEntity>> entities = res.types(OntEntity.class).collect(Collectors.toList());
        Assertions.assertEquals(87, objects.size());
        Assertions.assertEquals(8, entities.size());
        return res;
    }

    private static ObjectFactory createNamedIndividualFactory() {
        OntMaker maker = new OntMaker.Default(IndividualImpl.class) {
            @Override
            public EnhNode instance(Node node, EnhGraph eg) {
                return new IndividualImpl(node, eg);
            }
        };
        OntFinder finder = new OntFinder.ByPredicate(RDF.type);
        OntFilter filter = OntFilter.URI.and(new OntFilter.HasType(OWL.NamedIndividual));
        return new CommonFactoryImpl(maker, finder, filter) {
            @Override
            public String toString() {
                return "NamedIndividualFactory";
            }
        };
    }

    /**
     * Named individual that requires explicit {@code _:x rdf:type owl:NamedIndividual} declaration, just only class.
     */
    public static class IndividualImpl extends OntIndividualImpl.NamedImpl {
        private IndividualImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Optional<OntStatement> findRootStatement() {
            return getRequiredRootStatement(this, OWL.NamedIndividual);
        }
    }
}
