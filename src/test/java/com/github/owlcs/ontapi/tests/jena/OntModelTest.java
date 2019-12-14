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

package com.github.owlcs.ontapi.tests.jena;

import com.github.owlcs.ontapi.OntFormat;
import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.OntModelFactory;
import com.github.owlcs.ontapi.jena.UnionGraph;
import com.github.owlcs.ontapi.jena.impl.OntCEImpl;
import com.github.owlcs.ontapi.jena.impl.OntGraphModelImpl;
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.Graphs;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.utils.Models;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.jena.vocabulary.XSD;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To test {@link OntModel} and all its related functionality.
 * <p>
 * Created by szuev on 07.11.2016.
 */
public class OntModelTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntModelTest.class);

    @SafeVarargs
    private static <X> Set<X> toUnmodifiableSet(Collection<? extends X>... lists) {
        return Arrays.stream(lists).flatMap(Collection::stream).collect(Iter.toUnmodifiableSet());
    }

    private static void assertOntObjectsCount(OntModel m, Class<? extends OntObject> type, long expected) {
        Assert.assertEquals(expected, m.ontObjects(type).count());
    }

    private static void testPizzaCEs(Model m, Property predicate, List<? extends OntClass> ces) {
        String type = ces.isEmpty() ? null : ((OntCEImpl) ces.get(0)).getActualClass().getSimpleName();
        Assert.assertEquals("Incorrect count of " + type, m.listSubjectsWithProperty(predicate)
                .toSet().size(), ces.size());
    }

    @SuppressWarnings("rawtypes")
    static void simplePropertiesValidation(OntModel ont) {
        Model jena = ModelFactory.createModelForGraph(ont.getGraph());
        Set<Resource> annotationProperties = jena.listStatements(null, RDF.type, OWL.AnnotationProperty)
                .mapWith(Statement::getSubject).toSet();
        Set<Resource> datatypeProperties = jena.listStatements(null, RDF.type, OWL.DatatypeProperty)
                .mapWith(Statement::getSubject).toSet();
        Set<Resource> namedObjectProperties = jena.listStatements(null, RDF.type, OWL.ObjectProperty)
                .mapWith(Statement::getSubject).toSet();
        Set<Resource> inverseObjectProperties = jena.listStatements(null, OWL.inverseOf, (RDFNode) null)
                .mapWith(Statement::getSubject).filterKeep(RDFNode::isAnon).toSet();
        Set<Statement> inverseStatements = jena.listStatements(null, OWL.inverseOf, (RDFNode) null)
                .filterKeep(s -> s.getSubject().isURIResource()).filterKeep(s -> s.getObject().isURIResource()).toSet();

        List<OntProperty> actualPEs = ont.ontObjects(OntProperty.class)
                .peek(x -> LOGGER.debug("PE: {}", x)).collect(Collectors.toList());

        Set<Resource> expectedNamed = toUnmodifiableSet(annotationProperties, datatypeProperties, namedObjectProperties);
        Set<Resource> expectedPEs = toUnmodifiableSet(expectedNamed, inverseObjectProperties);
        Assert.assertEquals("Incorrect number of property expressions", expectedPEs.size(), actualPEs.size());

        List<OntNamedProperty> actualNamed = ont.ontObjects(OntNamedProperty.class)
                .peek(x -> LOGGER.debug("Named property: {}", x))
                .collect(Collectors.toList());
        Assert.assertEquals("Incorrect number of named properties", expectedNamed.size(), actualNamed.size());

        List<OntProperty> actualDOs = ont.ontObjects(OntRealProperty.class).collect(Collectors.toList());
        Set<Resource> expectedDOs = toUnmodifiableSet(datatypeProperties, namedObjectProperties, inverseObjectProperties);
        Assert.assertEquals("Incorrect number of data and object property expressions",
                expectedDOs.size(), actualDOs.size());

        Assert.assertEquals("Incorrect number of owl:inverseOf for object properties", inverseStatements.size(),
                ont.objectProperties().flatMap(OntObjectProperty::inverseProperties).count());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testPizzaLoadCE() {
        LOGGER.debug("load pizza");
        OntModel m = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("ontapi/pizza.ttl").getGraph());
        LOGGER.debug("Ontology: {}", m.getID());

        List<OntClass.Named> classes = m.ontObjects(OntClass.Named.class).collect(Collectors.toList());
        int expectedClassesCount = m.listStatements(null, RDF.type, OWL.Class)
                .mapWith(Statement::getSubject).filterKeep(RDFNode::isURIResource).toSet().size();
        int actualClassesCount = classes.size();
        LOGGER.debug("Classes Count = {}", actualClassesCount);
        Assert.assertEquals("Incorrect Classes count", expectedClassesCount, actualClassesCount);

        LOGGER.debug("Class Expressions:");
        List<OntClass> ces = m.ontObjects(OntClass.class).collect(Collectors.toList());
        ces.forEach(x -> LOGGER.debug("{}", x));
        int expectedCEsCount = m.listStatements(null, RDF.type, OWL.Class)
                .andThen(m.listStatements(null, RDF.type, OWL.Restriction)).toSet().size();
        int actualCEsCount = ces.size();
        LOGGER.debug("Class Expressions Count = {}", actualCEsCount);
        Assert.assertEquals("Incorrect CE's count", expectedCEsCount, actualCEsCount);

        List<OntClass.RestrictionCE> restrictionCEs = m.ontObjects(OntClass.RestrictionCE.class).collect(Collectors.toList());
        Assert.assertEquals("Incorrect count of restrictions ",
                m.listStatements(null, RDF.type, OWL.Restriction).toSet().size(), restrictionCEs.size());

        List<OntClass.ObjectSomeValuesFrom> objectSomeValuesFromCEs = m.ontObjects(OntClass.ObjectSomeValuesFrom.class)
                .collect(Collectors.toList());
        List<OntClass.ObjectAllValuesFrom> objectAllValuesFromCEs = m.ontObjects(OntClass.ObjectAllValuesFrom.class)
                .collect(Collectors.toList());
        List<OntClass.ObjectHasValue> objectHasValueCEs = m.ontObjects(OntClass.ObjectHasValue.class)
                .collect(Collectors.toList());
        List<OntClass.UnionOf> unionOfCEs = m.ontObjects(OntClass.UnionOf.class).collect(Collectors.toList());
        List<OntClass.IntersectionOf> intersectionOfCEs = m.ontObjects(OntClass.IntersectionOf.class)
                .collect(Collectors.toList());
        List<OntClass.ComplementOf> complementOfCEs = m.ontObjects(OntClass.ComplementOf.class).collect(Collectors.toList());
        List<OntClass.OneOf> oneOfCEs = m.ontObjects(OntClass.OneOf.class).collect(Collectors.toList());
        List<OntClass.ObjectMinCardinality> objectMinCardinalityCEs = m.ontObjects(OntClass.ObjectMinCardinality.class)
                .collect(Collectors.toList());

        testPizzaCEs(m, OWL.someValuesFrom, objectSomeValuesFromCEs);
        testPizzaCEs(m, OWL.allValuesFrom, objectAllValuesFromCEs);
        testPizzaCEs(m, OWL.hasValue, objectHasValueCEs);
        testPizzaCEs(m, OWL.unionOf, unionOfCEs);
        testPizzaCEs(m, OWL.intersectionOf, intersectionOfCEs);
        testPizzaCEs(m, OWL.complementOf, complementOfCEs);
        testPizzaCEs(m, OWL.oneOf, oneOfCEs);
        testPizzaCEs(m, OWL.minCardinality, objectMinCardinalityCEs);
    }

    @Test
    public void testPizzaLoadProperties() {
        simplePropertiesValidation(OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("ontapi/pizza.ttl")
                .getGraph()));
    }

    @Test
    public void testFamilyLoadProperties() {
        simplePropertiesValidation(OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("ontapi/family.ttl")
                .getGraph()));
    }

    @Test
    public void testPizzaLoadIndividuals() {
        LOGGER.debug("load pizza");
        OntModel m = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("ontapi/pizza.ttl").getGraph());
        List<OntIndividual> individuals = m.ontObjects(OntIndividual.class).collect(Collectors.toList());
        Map<OntIndividual, Set<OntClass>> classes = individuals.stream()
                .collect(Collectors.toMap(Function.identity(), i -> i.classes().collect(Collectors.toSet())));
        classes.forEach((i, c) -> LOGGER.debug("Individual: {}, Classes: {}", i, c));
        classes.forEach((i, c) -> c.forEach(_c -> Assert.assertEquals(1, _c.individuals()
                .filter(_i -> Objects.equals(_i, i)).count())));

        Set<Resource> namedIndividuals = m.listSubjectsWithProperty(RDF.type, OWL.NamedIndividual).toSet();
        Set<Resource> anonIndividuals = m.listStatements(null, RDF.type, (RDFNode) null)
                .filterKeep(s -> s.getSubject().isAnon())
                .filterKeep(s -> s.getObject().isResource() && m.contains(s.getObject()
                        .asResource(), RDF.type, OWL.Class))
                .mapWith(Statement::getSubject).toSet();
        Set<Resource> expected = new HashSet<>(namedIndividuals);
        expected.addAll(anonIndividuals);
        Assert.assertEquals("Incorrect number of individuals", expected.size(), individuals.size());
    }

    @Test
    public void testKoalaCommon() throws IOException {
        // koala has 4 cardinality restrictions with wrong 'xsd:int' instead of 'xsd:nonNegativeInteger'
        // see issue #56
        // OntClass => 20,
        // OntCE$ObjectSomeValuesFrom => 3,
        // OntCE$ObjectAllValuesFrom => 1,
        // OntCE$OneOf => 1,
        // OntCE$IntersectionOf => 4,
        // OntCE$ObjectHasValue => 4,
        // OntCE$DataHasValue => 3
        long numClasses = 36;

        OntModel m = OntModelFactory.createModel();
        try (InputStream in = OntModelTest.class.getResourceAsStream("/owlapi/koala.owl")) {
            m.read(in, null, Lang.RDFXML.getName());
        }
        ReadWriteUtils.print(m);

        long statementsCount = m.statements().count();

        Map<OntClass, Set<OntProperty>> props = new HashMap<>();
        m.ontObjects(OntClass.class)
                .forEach(x -> props.computeIfAbsent(x, c -> new HashSet<>())
                        .addAll(x.properties().collect(Collectors.toSet())));
        props.forEach((c, ps) -> LOGGER.debug("{} => {}", c, ps));

        Assert.assertEquals(numClasses, props.keySet().size());
        Assert.assertEquals(5, props.values().stream().mapToLong(Collection::size).sum());

        String ns = m.getID().getURI() + "#";
        OntClass.Named animal = m.getOntClass(ns + "Animal");
        Assert.assertNotNull(animal);
        Assert.assertEquals("Wrong #Animal attached properties count", 3, animal.properties().count());
        OntClass.Named person = m.getOntClass(ns + "Person");
        Assert.assertNotNull(person);
        Assert.assertEquals("Wrong #Person attached properties count", 2, person.properties().count());

        OntDataProperty isHardWorking = m.getDataProperty(ns + "isHardWorking");
        Assert.assertNotNull(isHardWorking);
        Set<OntObjectProperty> objProperties = m.ontObjects(OntObjectProperty.Named.class).collect(Collectors.toSet());
        Assert.assertEquals(4, objProperties.size());

        OntStatement statement = person.createHasKey(objProperties, Collections.singleton(isHardWorking)).getMainStatement();
        Assert.assertTrue(statement.getObject().canAs(RDFList.class));
        statement.addAnnotation(m.getRDFSComment(), "These are keys", "xz");
        ReadWriteUtils.print(m);

        Assert.assertEquals(5, person.hasKeys().findFirst().orElseThrow(AssertionError::new).members().count());
        Assert.assertEquals(numClasses, m.ontObjects(OntClass.class).distinct().count());
        Assert.assertEquals(statementsCount + 16, m.statements().count());
        Assert.assertNotNull(statement.deleteAnnotation(m.getRDFSComment()));

        Assert.assertEquals(statementsCount + 11, m.statements().count());
        person.clearHasKeys();
        Assert.assertEquals(statementsCount, m.statements().count());

        OntClass.Named marsupials = m.getOntClass(ns + "Marsupials");
        Assert.assertNotNull(marsupials);
        Assert.assertEquals(marsupials, person.disjointClasses().findFirst().orElse(null));
        Assert.assertEquals(person, marsupials.disjointClasses().findAny().orElse(null));

        marsupials.addDisjointClass(animal);
        Assert.assertEquals(2, marsupials.disjointClasses().count());
        Assert.assertEquals(0, animal.disjointClasses().count());
        Assert.assertEquals(1, person.disjointClasses().count());
        marsupials.removeDisjointClass(animal);
        Assert.assertEquals(1, marsupials.disjointClasses().count());
        Assert.assertEquals(0, animal.disjointClasses().count());
        Assert.assertEquals(1, person.disjointClasses().count());

        person.addSuperClass(marsupials);
        Assert.assertEquals(2, person.superClasses().count());
        person.removeSuperClass(marsupials);
        Assert.assertEquals(1, person.superClasses().count());

        Assert.assertEquals(statementsCount, m.statements().count());
    }

    @Test
    public void testKoalaProperties() throws IOException {
        OntModel m = OntModelFactory.createModel();
        try (InputStream in = OntModelTest.class.getResourceAsStream("/owlapi/koala.owl")) {
            m.read(in, null, Lang.RDFXML.getName());
        }
        simplePropertiesValidation(m);
        OntObjectProperty p1 = m.objectProperties().findFirst().orElseThrow(AssertionError::new);
        Assert.assertFalse(p1.findInverseProperty().isPresent());
        OntObjectProperty p2 = m.createResource().addProperty(OWL.inverseOf, p1).as(OntObjectProperty.class);
        Assert.assertTrue(p2.findInverseProperty().isPresent());
        Assert.assertEquals(1, p2.inverseProperties().count());
        Assert.assertEquals(p1.asProperty(), p2.asProperty());
        Assert.assertEquals(p1, p2.findInverseProperty().orElseThrow(AssertionError::new));
        Assert.assertEquals(1, m.ontObjects(OntObjectProperty.Inverse.class).count());
    }

    @Test
    public void testCreateImports() {
        String baseURI = "http://test.com/graph/5";
        String baseNS = baseURI + "#";
        OntModel base = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID(baseURI).getModel();
        OntClass.Named cl1 = base.createOntClass(baseNS + "Class1");
        OntClass.Named cl2 = base.createOntClass(baseNS + "Class2");

        String childURI = "http://test.com/graph/6";
        String childNS = childURI + "#";
        OntModel child = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID(childURI).getModel().addImport(base);
        OntClass.Named cl3 = child.createOntClass(childNS + "Class3");
        cl3.addSuperClass(child.createObjectIntersectionOf(cl1, cl2));
        cl3.createIndividual(childNS + "Individual1");

        LOGGER.debug("Base:");
        base = child.imports().findFirst().orElse(null);
        Assert.assertNotNull("Null base", base);
        ReadWriteUtils.print(base);
        LOGGER.debug("Child:");
        ReadWriteUtils.print(child);
        Set<String> imports = child.getID().imports().collect(Collectors.toSet());
        Assert.assertThat("Incorrect imports", imports, IsEqual.equalTo(Stream.of(baseURI)
                .collect(Collectors.toSet())));
        Assert.assertEquals("Incorrect count of entities", 4, child.ontEntities().count());
        Assert.assertEquals("Incorrect count of local entities", 2, child.ontEntities()
                .filter(OntEntity::isLocal).count());
    }

    @Test
    public void testAssemblySimplestOntology() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        m.setID("http://example.com/xxx");

        String schemaNS = m.getID().getURI() + "#";
        String dataNS = m.getID().getURI() + "/data#";
        m.setNsPrefix("schema", schemaNS).setNsPrefix("data", dataNS);

        OntDataRange.Named email = m.createDatatype(schemaNS + "email");
        OntDataRange.Named phone = m.createDatatype(schemaNS + "phone");
        OntDataRange.Named skype = m.createDatatype(schemaNS + "skype");
        OntDataProperty contactInfo = m.createDataProperty(schemaNS + "info");
        OntClass.Named contact = m.createOntClass(schemaNS + "Contact");
        OntClass.Named person = m.createOntClass(schemaNS + "Person");
        OntObjectProperty.Named hasContact = m.createObjectProperty(schemaNS + "contact");

        hasContact.addDomain(person).addRange(contact);

        contactInfo.addDomain(contact)
                .addRange(email)
                .addRange(phone)
                .addRange(skype);

        // data:
        OntIndividual bobs = contact.createIndividual(dataNS + "bobs");
        bobs.addAssertion(contactInfo, email.createLiteral("bob@x-email.com"))
                .addAssertion(m.getRDFSLabel(), m.createLiteral("Bob's contacts"))
                .addAssertion(contactInfo, phone.createLiteral(98_968_78_98_792L));
        OntIndividual bob = person.createIndividual(dataNS + "Bob").addAssertion(hasContact, bobs)
                .addAssertion(m.getRDFSLabel(), m.createLiteral("Bob Label"));

        OntIndividual jhons = contact.createIndividual(dataNS + "jhons")
                .addAssertion(contactInfo, skype.createLiteral("jhon-skype-id"));
        person.createIndividual(dataNS + "Jhon").addAssertion(hasContact, jhons);
        bob.addNegativeAssertion(hasContact, jhons)
                .addNegativeAssertion(contactInfo, phone.createLiteral("212 85 06"))
                .addNegativeAssertion(hasContact.createInverse(), bobs);

        Assert.assertEquals(2, bob.positiveAssertions().count());
        Assert.assertEquals(3, bob.negativeAssertions().count());

        ReadWriteUtils.print(m);
        Assert.assertEquals(42, m.statements().count());
    }

    @Test
    public void testCreateEntities() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        createEntityTest(m, "a-p", OntAnnotationProperty.class);
        createEntityTest(m, "o-p", OntObjectProperty.Named.class);
        createEntityTest(m, "d-p", OntDataProperty.class);
        createEntityTest(m, "c", OntClass.Named.class);
        createEntityTest(m, "d", OntDataRange.Named.class);
        createEntityTest(m, "I", OntIndividual.Named.class);
        ReadWriteUtils.print(m);
    }

    private <E extends OntEntity> void createEntityTest(OntModel m, String uri, Class<E> type) {
        String pref = "Annotation[" + uri + "]:::";
        E e = m.createOntEntity(type, uri);
        e.addAnnotation(m.getRDFSComment(), pref + "entity of type " + type.getSimpleName())
                .addAnnotation(m.getRDFSLabel(), pref + "label");
        m.asStatement(e.getMainStatement().asTriple()).addAnnotation(m.getRDFSComment(), pref + "comment");
        Assert.assertEquals(2, e.annotations().count());
        Assert.assertEquals(2, e.statements().count());
        Assert.assertSame(e, e.as(type));
        Assert.assertSame(e, ((OntGraphModelImpl) m).getNodeAs(e.asNode(), type));
    }

    @Test
    public void testObjectsContent() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        // properties:
        OntDataProperty p1 = m.createDataProperty("p1");
        OntObjectProperty.Named p2 = m.createObjectProperty("p2");
        // classes:
        OntClass.Named class1 = m.createOntClass("c");
        OntClass.UnionOf class2 = m.createObjectUnionOf(m.createOntClass("c1"), m.createOntClass("c2"));
        OntClass.DataHasValue class3 = m.createDataHasValue(p1, m.createLiteral("2"));
        OntClass.DataMinCardinality class4 = m.createDataMinCardinality(p1, 2,
                m.getDatatype(XSD.xdouble));
        OntClass.Named class5 = m.getOWLThing();
        OntClass.ObjectCardinality class6 = m.createObjectCardinality(p2, 1234, class5);
        OntClass.HasSelf class7 = m.createHasSelf(p2);
        class3.addComment("The Restriction");
        class1.addSuperClass(class2).addSuperClass(class3).addDisjointClass(class4);
        class2.addSuperClass(m.createObjectComplementOf(class5));
        class5.addEquivalentClass(m.getOWLNothing());
        // data-ranges:
        OntDataRange.Named dr1 = m.getDatatype(XSD.xint);
        OntDataRange.IntersectionOf dr2 = m.createDataIntersectionOf(dr1, m.getDatatype(XSD.xdouble));
        OntDataRange.ComplementOf dr3 = m.createDataComplementOf(dr2);
        dr3.addComment("Data range: complement of intersection int and double");
        // individuals:
        OntIndividual i1 = class5.createIndividual("i1");
        OntIndividual i2 = class6.createIndividual();
        // nap:
        OntNegativeAssertion<?, ?> npa1 = p1.addNegativeAssertion(i1, m.createLiteral("xxx"));

        ReadWriteUtils.print(m);

        Assert.assertEquals(1, class1.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("1::CLASS SPEC: {}", x)).count());
        Assert.assertEquals(4, class1.content().map(Models::toString)
                .peek(x -> LOGGER.debug("1::CLASS CONTENT: {}", x)).count());

        Assert.assertEquals(6, class2.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("2::CLASS SPEC: {}", x)).count());
        Assert.assertEquals(7, class2.content().map(Models::toString)
                .peek(x -> LOGGER.debug("2::CLASS CONTENT: {}", x)).count());

        Assert.assertEquals(3, class3.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("3::CLASS SPEC: {}", x)).count());
        Assert.assertEquals(3, class3.content().map(Models::toString)
                .peek(x -> LOGGER.debug("3::CLASS CONTENT: {}", x)).count());

        Assert.assertEquals(4, class4.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("4::CLASS SPEC: {}", x)).count());
        Assert.assertEquals(4, class4.content().map(Models::toString)
                .peek(x -> LOGGER.debug("4::CLASS CONTENT: {}", x)).count());

        Assert.assertEquals(0, class5.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("5::CLASS SPEC: {}", x)).count());
        Assert.assertEquals(1, class5.content().map(Models::toString)
                .peek(x -> LOGGER.debug("5::CLASS CONTENT: {}", x)).count());

        Assert.assertEquals(3, class6.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("6::CLASS SPEC: {}", x)).count());
        Assert.assertEquals(3, class6.content().map(Models::toString)
                .peek(x -> LOGGER.debug("6::CLASS CONTENT: {}", x)).count());

        Assert.assertEquals(3, class7.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("7::CLASS SPEC: {}", x)).count());
        Assert.assertEquals(3, class7.content().map(Models::toString)
                .peek(x -> LOGGER.debug("7::CLASS CONTENT: {}", x)).count());

        Assert.assertEquals(0, dr1.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("1::DATA-RANGE SPEC: {}", x)).count());
        Assert.assertEquals(0, dr1.content().map(Models::toString)
                .peek(x -> LOGGER.debug("1::DATA-RANGE CONTENT: {}", x)).count());

        Assert.assertEquals(6, dr2.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("2::DATA-RANGE SPEC: {}", x)).count());
        Assert.assertEquals(6, dr2.content().map(Models::toString)
                .peek(x -> LOGGER.debug("2::DATA-RANGE CONTENT: {}", x)).count());

        Assert.assertEquals(2, dr3.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("3::DATA-RANGE SPEC: {}", x)).count());
        Assert.assertEquals(2, dr3.content().map(Models::toString)
                .peek(x -> LOGGER.debug("3::DATA-RANGE CONTENT: {}", x)).count());

        Assert.assertEquals(1, i1.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("1::INDIVIDUAL SPEC: {}", x)).count());
        Assert.assertEquals(6, i1.content().map(Models::toString)
                .peek(x -> LOGGER.debug("1::INDIVIDUAL CONTENT: {}", x)).count());

        Assert.assertEquals(0, i2.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("2::INDIVIDUAL SPEC: {}", x)).count());
        Assert.assertEquals(1, i2.content().map(Models::toString)
                .peek(x -> LOGGER.debug("2::INDIVIDUAL CONTENT: {}", x)).count());

        Assert.assertEquals(4, npa1.spec().map(Models::toString)
                .peek(x -> LOGGER.debug("1::NAP SPEC: {}", x)).count());
        Assert.assertEquals(4, npa1.content().map(Models::toString)
                .peek(x -> LOGGER.debug("1::NAP CONTENT: {}", x)).count());
    }

    @Test
    public void testRemoveObjects() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);

        OntClass class1 = m.createOntClass("C-1");
        OntClass class2 = m.createOntClass("C-2");
        OntClass class3 = m.createOntClass("C-3");
        OntObjectProperty p = m.createObjectProperty("P");
        OntClass class4 = m.createObjectComplementOf(class3);
        OntClass class5 = m.createObjectSomeValuesFrom(p, class4);
        OntClass class6 = m.createObjectIntersectionOf(m.getOWLThing(), class2, class4, class5);
        Assert.assertEquals(6, m.ontObjects(OntClass.class).count());
        long size = m.size();
        OntDisjoint<?> d = m.createDisjointClasses(m.getOWLNothing(), class1, class6);
        ReadWriteUtils.print(m);

        m.removeOntObject(d);
        ReadWriteUtils.print(m);
        Assert.assertEquals(size, m.statements().count());

        m.removeOntObject(class6).removeOntObject(class5).removeOntObject(class4).removeOntObject(p);
        ReadWriteUtils.print(m);

        Assert.assertEquals(3, m.size());
    }

    @Test
    public void testModelPrefixes() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        m.setID("http://x");
        Assert.assertEquals(4, m.numPrefixes());
        Assert.assertEquals(4, m.getBaseGraph().getPrefixMapping().numPrefixes());
        String txt = ReadWriteUtils.toString(m, OntFormat.TURTLE);
        LOGGER.debug(txt);
        Assert.assertEquals(6, txt.split("\n").length);

        m.setNsPrefix("x", "http://x#");
        Assert.assertEquals(5, m.numPrefixes());
        Assert.assertEquals(5, m.getBaseGraph().getPrefixMapping().numPrefixes());
        txt = ReadWriteUtils.toString(m, OntFormat.TURTLE);
        LOGGER.debug(txt);
        Assert.assertEquals(7, txt.split("\n").length);

        m.removeNsPrefix("x");
        Assert.assertEquals(4, m.numPrefixes());
        Assert.assertEquals(4, m.getBaseGraph().getPrefixMapping().numPrefixes());
        txt = ReadWriteUtils.toString(m, OntFormat.TURTLE);
        LOGGER.debug(txt);
        Assert.assertEquals(6, txt.split("\n").length);
    }

    @Test
    public void testAdvancedModelImports() {
        OntModel av1 = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID("a").setVersionIRI("v1").getModel();
        OntModel av2 = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID("a").setVersionIRI("v2").getModel();
        OntModel b = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID("b").getModel();
        OntModel c = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID("c").getModel();

        try {
            c.addImport(av1).addImport(av1);
            Assert.fail("Can add the same model");
        } catch (OntJenaException j) {
            LOGGER.debug("Expected: '{}'", j.getMessage());
        }
        Assert.assertTrue(c.hasImport(av1));
        Assert.assertFalse(c.hasImport(av2));
        Assert.assertEquals(1, c.imports().count());

        c.removeImport(av1).addImport(av2);
        Assert.assertTrue(c.hasImport(av2));
        Assert.assertFalse(c.hasImport(av1));
        Assert.assertEquals(1, c.imports().count());

        b.addImport(c);
        Assert.assertEquals(1, b.imports().count());
        Assert.assertTrue(b.hasImport(c));
        Assert.assertFalse(b.hasImport(av1));
        Assert.assertFalse(b.hasImport(av2));

        String tree = Graphs.importsTreeAsString(b.getGraph());
        LOGGER.debug("1) Tree: \n{}", tree);
        Assert.assertEquals(Arrays.asList("<b>", "<c>", "<a[v2]>"),
                Arrays.stream(tree.split("\n")).map(String::trim).collect(Collectors.toList()));

        c.removeImport(av1);
        tree = Graphs.importsTreeAsString(b.getGraph());
        Assert.assertEquals(Arrays.asList("<b>", "<c>", "<a[v2]>"),
                Arrays.stream(tree.split("\n")).map(String::trim).collect(Collectors.toList()));

        c.removeImport(av2).addImport(av1);
        tree = Graphs.importsTreeAsString(b.getGraph());
        LOGGER.debug("2) Tree: \n{}", tree);
        Assert.assertEquals(Arrays.asList("<b>", "<c>", "<a[v1]>"),
                Arrays.stream(tree.split("\n")).map(String::trim).collect(Collectors.toList()));

        // sync imports:
        ((UnionGraph) c.getGraph()).addGraph(av2.getGraph());
        OntModels.syncImports(b);
        tree = Graphs.importsTreeAsString(b.getGraph());
        LOGGER.debug("3) Tree: \n{}", tree);
        Assert.assertEquals(4, OntModels.importsClosure(b).count());
        Assert.assertEquals(3, OntModels.importsClosure(c).count());
        Assert.assertEquals(Arrays.asList("<b>", "<c>", "<a[v1]>", "<a[v2]>"),
                Arrays.stream(tree.split("\n")).map(String::trim).collect(Collectors.toList()));
        Assert.assertEquals(Arrays.asList("v1", "v2"), c.statements(null, OWL.imports, null)
                .map(Statement::getResource)
                .map(Resource::getURI)
                .sorted()
                .collect(Collectors.toList()));
    }

    @Test
    public void testCycleModelImports() {
        OntModel a = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntModel b = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntModel c = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        a.createOntClass("A");
        b.createOntClass("B");
        c.createOntClass("C");
        a.setID("a");
        b.setID("b");
        c.setID("c");

        a.addImport(b);
        Assert.assertEquals(1, a.imports().count());
        Assert.assertEquals(0, b.imports().count());
        Assert.assertEquals(0, c.imports().count());
        Assert.assertEquals(2, a.ontEntities().count());
        Assert.assertEquals(1, b.ontEntities().count());
        Assert.assertEquals(1, c.ontEntities().count());

        b.addImport(c);
        Assert.assertEquals(1, a.imports().count());
        Assert.assertEquals(1, b.imports().count());
        Assert.assertEquals(0, c.imports().count());
        Assert.assertEquals(3, a.ontEntities().count());
        Assert.assertEquals(2, b.ontEntities().count());
        Assert.assertEquals(1, c.ontEntities().count());

        // add cycle import:
        c.addImport(a);
        Assert.assertEquals(1, a.imports().count());
        Assert.assertEquals(1, b.imports().count());
        Assert.assertEquals(1, c.imports().count());
        Assert.assertEquals(3, a.ontEntities().count());
        Assert.assertEquals(3, b.ontEntities().count());
        Assert.assertEquals(3, c.ontEntities().count());

        // add more entities:
        a.createOntClass("B");
        b.createOntClass("X");
        Assert.assertEquals(4, a.ontEntities().count());
        Assert.assertEquals(4, b.ontEntities().count());
        Assert.assertEquals(4, c.ontEntities().count());

        // remove cycle import
        b.removeImport(c);
        Assert.assertEquals(1, a.imports().count());
        Assert.assertEquals(0, b.imports().count());
        Assert.assertEquals(1, c.imports().count());
        Assert.assertEquals(3, a.ontEntities().count());
        Assert.assertEquals(2, b.ontEntities().count());
        Assert.assertEquals(4, c.ontEntities().count());
    }

    @Test
    public void testOntPropertyOrdinal() {
        Graph g = ReadWriteUtils.loadResourceTTLFile("/ontapi/pizza.ttl").getGraph();
        OntModel m = OntModelFactory.createModel(g);
        OntNamedProperty<?> p = m.getOntEntity(OntNamedProperty.class, m.expandPrefix(":isIngredientOf"));
        Assert.assertNotNull(p);
        Assert.assertEquals(0, p.getOrdinal());
        Assert.assertEquals(0, m.getRDFSComment().getOrdinal());
        Assert.assertEquals(0, m.getOWLBottomDataProperty().getOrdinal());
    }

    @Test
    public void testFamilyListObjects() {
        OntModel m = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("ontapi/family.ttl").getGraph(),
                OntModelConfig.ONT_PERSONALITY_LAX);
        assertOntObjectsCount(m, OntEntity.class, 656);
        assertOntObjectsCount(m, OntNamedProperty.class, 90);

        assertOntObjectsCount(m, OntClass.Named.class, 58);
        assertOntObjectsCount(m, OntDataRange.Named.class, 0);
        assertOntObjectsCount(m, OntIndividual.Named.class, 508);
        assertOntObjectsCount(m, OntObjectProperty.Named.class, 80);
        assertOntObjectsCount(m, OntAnnotationProperty.class, 1);
        assertOntObjectsCount(m, OntDataProperty.class, 9);

        assertOntObjectsCount(m, OntObjectProperty.class, 80);
        assertOntObjectsCount(m, OntRealProperty.class, 89);

        assertOntObjectsCount(m, OntDataRange.class, 0);

        assertOntObjectsCount(m, OntDisjoint.class, 1);
        assertOntObjectsCount(m, OntDisjoint.Classes.class, 0);
        assertOntObjectsCount(m, OntDisjoint.Individuals.class, 1);
        assertOntObjectsCount(m, OntDisjoint.DataProperties.class, 0);
        assertOntObjectsCount(m, OntDisjoint.ObjectProperties.class, 0);
        assertOntObjectsCount(m, OntDisjoint.Properties.class, 0);

        // todo: handle all other types
    }

    @Test
    public void testListIndividualTypes() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass.Named a = m.createOntClass("A");
        OntClass.Named b = m.createOntClass("B");
        OntClass.Named c = m.createOntClass("C");
        OntClass.Named d = m.createOntClass("D");
        OntClass.Named e = m.createOntClass("E");

        b.addSuperClass(m.createObjectComplementOf(c)).addSuperClass(a);
        OntClass ae = m.createObjectIntersectionOf(a, e);
        d.addSuperClass(ae);
        a.addSuperClass(d);
        ae.addSuperClass(a).addSuperClass(b);

        OntIndividual i1 = a.createIndividual("i");
        OntIndividual i2 = d.createIndividual();
        i2.attachClass(b);
        i1.attachClass(d);

        ReadWriteUtils.print(m);

        Assert.assertEquals(2, i2.classes(true).count());
        Assert.assertEquals(5, i2.classes(false).count());

        Assert.assertEquals(2, i1.classes(true).count());
        Assert.assertEquals(5, i1.classes(false).count());
    }

    @Test
    public void testRemoveStatement() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass.Named c = m.createOntClass("c");
        OntDataProperty d = m.createDataProperty("d");
        OntStatement s = d.addDomainStatement(c);
        s.addAnnotation(m.getRDFSLabel(), "a1").addAnnotation(m.getRDFSComment(), "a2");
        s.addAnnotation(m.getRDFSComment(), "a3");

        ReadWriteUtils.print(m);
        Assert.assertEquals(14, m.size());

        d.removeDomain(c);
        ReadWriteUtils.print(m);
        Assert.assertEquals(2, m.size());

        d.removeRange(c);
        Assert.assertEquals(2, m.size());
    }

    @Test
    public void testDisjointComponents() {
        OntModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntClass.Named c1 = m.createOntClass("C1");
        OntClass.Named c2 = m.createOntClass("C1");
        OntObjectProperty.Named op1 = m.createObjectProperty("OP1");
        OntObjectProperty.Named op2 = m.createObjectProperty("OP2");
        OntObjectProperty.Named op3 = m.createObjectProperty("OP3");
        OntDataProperty dp1 = m.createDataProperty("DP1");
        OntDataProperty dp2 = m.createDataProperty("DP2");
        OntDataProperty dp3 = m.createDataProperty("DP3");
        OntIndividual i1 = m.createIndividual("I1");
        OntIndividual i2 = c1.createIndividual("I2");
        OntIndividual i3 = c2.createIndividual();

        List<OntIndividual> list1 = Arrays.asList(i1, i2);
        OntDisjoint.Individuals d1 = m.createDifferentIndividuals(list1);
        Assert.assertEquals(list1, d1.getList().members().collect(Collectors.toList()));
        Assert.assertEquals(2, d1.members().count());
        Assert.assertSame(d1, d1.setComponents(i2, i3));
        Assert.assertEquals(Arrays.asList(i2, i3), d1.members().collect(Collectors.toList()));

        OntDisjoint.ObjectProperties d2 = m.createDisjointObjectProperties(op1, op2, op3);
        Assert.assertEquals(3, d2.getList().members().count());
        Assert.assertTrue(d2.setComponents().getList().isEmpty());

        OntDisjoint.DataProperties d3 = m.createDisjointDataProperties(dp1, dp2);
        Assert.assertEquals(2, d3.setComponents(Arrays.asList(dp3, m.getOWLBottomDataProperty())).members().count());

        ReadWriteUtils.print(m);

        Set<RDFNode> expected = new HashSet<>(Arrays.asList(i2, i3, dp3, OWL.bottomDataProperty));
        Set<RDFNode> actual = m.ontObjects(OntDisjoint.class)
                .map(x -> x.getList())
                .map(x -> x.as(RDFList.class))
                .map(RDFList::asJavaList)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testCreateDifferentExpressions() {
        String uri = "http://test.com/graph/3";
        String ns = uri + "#";

        OntModel m = OntModelFactory.createModel()
                .setNsPrefix("test", ns)
                .setNsPrefixes(OntModelFactory.STANDARD)
                .setID(uri)
                .getModel();

        OntDataProperty ndp1 = m.createDataProperty(ns + "dataProperty1");
        OntDataRange.Named dt1 = m.createOntEntity(OntDataRange.Named.class, ns + "dataType1");
        dt1.addEquivalentClass(m.getDatatype(XSD.dateTime));

        OntDataRange.Named dt2 = m.createOntEntity(OntDataRange.Named.class, ns + "dataType2");

        OntFacetRestriction fr1 = m.createFacetRestriction(OntFacetRestriction.MaxExclusive.class, ResourceFactory.createTypedLiteral(12));
        OntFacetRestriction fr2 = m.createFacetRestriction(OntFacetRestriction.LangRange.class, ResourceFactory.createTypedLiteral("\\d+"));

        OntDataRange dr1 = m.createDataRestriction(dt1, fr1, fr2);

        OntClass ce1 = m.createDataSomeValuesFrom(ndp1, dr1);

        OntDataRange dr2 = m.createDataIntersectionOf(dt1, dt2);
        OntIndividual i1 = ce1.createIndividual(ns + "individual1");
        OntClass ce2 = m.createDataMaxCardinality(ndp1, 343434, dr2);
        i1.attachClass(ce2).attachClass(m.createOntClass(ns + "Class1"));

        OntDataRange dr3 = m.createDataOneOf(m.getDatatype(XSD.integer).createLiteral(1), dt1.createLiteral(2));
        OntDataRange dr4 = m.createDataComplementOf(dr3);
        m.createOntEntity(OntDataRange.Named.class, ns + "dataType3")
                .addEquivalentClass(m.createDataUnionOf(dr1, dr2, m.createDataIntersectionOf(dr1, dr4)));

        OntIndividual i2 = ce2.createIndividual();
        i2.addStatement(ndp1, ResourceFactory.createPlainLiteral("individual value"));

        m.createObjectOneOf(i1, i2, ce2.createIndividual());

        ReadWriteUtils.print(m);
        Assert.assertEquals("Incorrect count of individuals", 3, m.ontObjects(OntIndividual.class).count());
        Assert.assertEquals("Incorrect count of class expressions", 4, m.ontObjects(OntClass.class).count());
        Assert.assertEquals("Incorrect count of restrictions", 2, m.ontObjects(OntClass.RestrictionCE.class).count());
        Assert.assertEquals("Incorrect count of cardinality restrictions", 1,
                m.ontObjects(OntClass.CardinalityRestrictionCE.class).count());
        Assert.assertEquals("Incorrect count of datatype entities", 3, m.ontObjects(OntDataRange.Named.class).count());
        Assert.assertEquals("Incorrect count of data properties", 1, m.ontObjects(OntDataProperty.class).count());
        Assert.assertEquals("Incorrect count of facet restrictions", 2, m.ontObjects(OntFacetRestriction.class).count());
        Assert.assertEquals("Incorrect count of data ranges", 9, m.ontObjects(OntDataRange.class).count());
        Assert.assertEquals("Incorrect count of entities", 6, m.ontObjects(OntEntity.class).count());
    }
}

