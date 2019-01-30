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

package ru.avicomp.ontapi.tests.jena;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.OntModelFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.OntCEImpl;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.SWRL;
import ru.avicomp.ontapi.jena.vocabulary.XSD;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To test {@link OntGraphModel}.
 * <p>
 * Created by szuev on 07.11.2016.
 */
public class OntModelTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntModelTest.class);

    @SafeVarargs
    private static <X> Set<X> toUnmodifiableSet(Collection<? extends X>... lists) {
        return Arrays.stream(lists).flatMap(Collection::stream).collect(Iter.toUnmodifiableSet());
    }

    private static void assertOntObjects(OntGraphModel m, Class<? extends OntObject> type, long expected) {
        Assert.assertEquals(expected, m.ontObjects(type).count());
    }

    private static void testPizzaCEs(Model m, Property predicate, List<? extends OntCE> ces) {
        String type = ces.isEmpty() ? null : ((OntCEImpl) ces.get(0)).getActualClass().getSimpleName();
        Assert.assertEquals("Incorrect count of " + type, m.listSubjectsWithProperty(predicate)
                .toSet().size(), ces.size());
    }

    private static void simplePropertiesValidation(OntGraphModel ont) {
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

        List<OntPE> actualPEs = ont.ontObjects(OntPE.class)
                .peek(x -> LOGGER.debug("PE: {}", x)).collect(Collectors.toList());

        Set<Resource> expectedNamed = toUnmodifiableSet(annotationProperties, datatypeProperties, namedObjectProperties);
        Set<Resource> expectedPEs = toUnmodifiableSet(expectedNamed, inverseObjectProperties);
        Assert.assertEquals("Incorrect number of property expressions", expectedPEs.size(), actualPEs.size());

        List<OntProperty> actualNamed = ont.ontObjects(OntProperty.class)
                .peek(x -> LOGGER.debug("Named property: {}", x))
                .collect(Collectors.toList());
        Assert.assertEquals("Incorrect number of named properties", expectedNamed.size(), actualNamed.size());

        List<OntPE> actualDOs = ont.ontObjects(OntDOP.class).collect(Collectors.toList());
        Set<Resource> expectedDOs = toUnmodifiableSet(datatypeProperties, namedObjectProperties, inverseObjectProperties);
        Assert.assertEquals("Incorrect number of data and object property expressions",
                expectedDOs.size(), actualDOs.size());

        Assert.assertEquals("Incorrect number of owl:inverseOf for object properties", inverseStatements.size(),
                ont.listObjectProperties().flatMap(OntOPE::inverseOf).count());
    }

    @Test
    public void testPizzaLoadCE() {
        LOGGER.debug("load pizza");
        OntGraphModel m = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("ontapi/pizza.ttl").getGraph());
        LOGGER.debug("Ontology: {}", m.getID());

        List<OntClass> classes = m.ontObjects(OntClass.class).collect(Collectors.toList());
        int expectedClassesCount = m.listStatements(null, RDF.type, OWL.Class)
                .mapWith(Statement::getSubject).filterKeep(RDFNode::isURIResource).toSet().size();
        int actualClassesCount = classes.size();
        LOGGER.debug("Classes Count = {}", actualClassesCount);
        Assert.assertEquals("Incorrect Classes count", expectedClassesCount, actualClassesCount);

        LOGGER.debug("Class Expressions:");
        List<OntCE> ces = m.ontObjects(OntCE.class).collect(Collectors.toList());
        ces.forEach(x -> LOGGER.debug("{}", x));
        int expectedCEsCount = m.listStatements(null, RDF.type, OWL.Class)
                .andThen(m.listStatements(null, RDF.type, OWL.Restriction)).toSet().size();
        int actualCEsCount = ces.size();
        LOGGER.debug("Class Expressions Count = {}", actualCEsCount);
        Assert.assertEquals("Incorrect CE's count", expectedCEsCount, actualCEsCount);

        List<OntCE.RestrictionCE> restrictionCEs = m.ontObjects(OntCE.RestrictionCE.class).collect(Collectors.toList());
        Assert.assertEquals("Incorrect count of restrictions ",
                m.listStatements(null, RDF.type, OWL.Restriction).toSet().size(), restrictionCEs.size());

        List<OntCE.ObjectSomeValuesFrom> objectSomeValuesFromCEs = m.ontObjects(OntCE.ObjectSomeValuesFrom.class)
                .collect(Collectors.toList());
        List<OntCE.ObjectAllValuesFrom> objectAllValuesFromCEs = m.ontObjects(OntCE.ObjectAllValuesFrom.class)
                .collect(Collectors.toList());
        List<OntCE.ObjectHasValue> objectHasValueCEs = m.ontObjects(OntCE.ObjectHasValue.class)
                .collect(Collectors.toList());
        List<OntCE.UnionOf> unionOfCEs = m.ontObjects(OntCE.UnionOf.class).collect(Collectors.toList());
        List<OntCE.IntersectionOf> intersectionOfCEs = m.ontObjects(OntCE.IntersectionOf.class)
                .collect(Collectors.toList());
        List<OntCE.ComplementOf> complementOfCEs = m.ontObjects(OntCE.ComplementOf.class).collect(Collectors.toList());
        List<OntCE.OneOf> oneOfCEs = m.ontObjects(OntCE.OneOf.class).collect(Collectors.toList());
        List<OntCE.ObjectMinCardinality> objectMinCardinalityCEs = m.ontObjects(OntCE.ObjectMinCardinality.class)
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
        OntGraphModel m = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("ontapi/pizza.ttl").getGraph());
        List<OntIndividual> individuals = m.ontObjects(OntIndividual.class).collect(Collectors.toList());
        Map<OntIndividual, Set<OntCE>> classes = individuals.stream()
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
        OntGraphModel m = OntModelFactory.createModel();
        try (InputStream in = OntModelTest.class.getResourceAsStream("/owlapi/koala.owl")) {
            m.read(in, null, Lang.RDFXML.getName());
        }
        ReadWriteUtils.print(m);

        long statementsCount = m.statements().count();

        Map<OntCE, Set<OntPE>> props = new HashMap<>();
        m.ontObjects(OntCE.class)
                .forEach(x -> props.computeIfAbsent(x, c -> new HashSet<>())
                        .addAll(x.properties().collect(Collectors.toSet())));
        props.forEach((c, ps) -> LOGGER.debug("{} => {}", c, ps));

        Assert.assertEquals(36, props.keySet().size());
        Assert.assertEquals(5, props.values().stream().mapToLong(Collection::size).sum());

        String ns = m.getID().getURI() + "#";
        OntClass animal = m.getOntEntity(OntClass.class, ns + "Animal");
        Assert.assertNotNull(animal);
        Assert.assertEquals("Wrong #Animal attached properties count", 3, animal.properties().count());
        OntClass person = m.getOntEntity(OntClass.class, ns + "Person");
        Assert.assertNotNull(person);
        Assert.assertEquals("Wrong #Person attached properties count", 2, person.properties().count());

        OntNDP isHardWorking = m.getOntEntity(OntNDP.class, ns + "isHardWorking");
        Assert.assertNotNull(isHardWorking);
        Set<OntOPE> objProperties = m.ontObjects(OntNOP.class).collect(Collectors.toSet());
        Assert.assertEquals(4, objProperties.size());

        OntStatement statement = person.createHasKey(objProperties, Collections.singleton(isHardWorking)).getRoot();
        Assert.assertTrue(statement.getObject().canAs(RDFList.class));
        statement.addAnnotation(m.getRDFSComment(), "These are keys", "xz");
        ReadWriteUtils.print(m);

        Assert.assertEquals(5, person.listHasKeys().findFirst().orElseThrow(AssertionError::new).members().count());
        Assert.assertEquals(36, m.ontObjects(OntCE.class).distinct().count());
        Assert.assertEquals(statementsCount + 16, m.statements().count());
        statement.deleteAnnotation(m.getRDFSComment());

        Assert.assertEquals(statementsCount + 11, m.statements().count());
        person.clearHasKeys();
        Assert.assertEquals(statementsCount, m.statements().count());

        OntClass marsupials = m.getOntEntity(OntClass.class, ns + "Marsupials");
        Assert.assertNotNull(marsupials);
        Assert.assertEquals(marsupials, person.disjointWith().findFirst().orElse(null));
        Assert.assertEquals(person, marsupials.disjointWith().findAny().orElse(null));

        marsupials.addDisjointWith(animal);
        Assert.assertEquals(2, marsupials.disjointWith().count());
        Assert.assertEquals(0, animal.disjointWith().count());
        Assert.assertEquals(1, person.disjointWith().count());
        marsupials.removeDisjointWith(animal);
        Assert.assertEquals(1, marsupials.disjointWith().count());
        Assert.assertEquals(0, animal.disjointWith().count());
        Assert.assertEquals(1, person.disjointWith().count());

        person.addSubClassOf(marsupials);
        Assert.assertEquals(2, person.subClassOf().count());
        person.removeSubClassOf(marsupials);
        Assert.assertEquals(1, person.subClassOf().count());

        Assert.assertEquals(statementsCount, m.statements().count());
    }

    @Test
    public void testKoalaProperties() throws IOException {
        OntGraphModel m = OntModelFactory.createModel();
        try (InputStream in = OntModelTest.class.getResourceAsStream("/owlapi/koala.owl")) {
            m.read(in, null, Lang.RDFXML.getName());
        }
        simplePropertiesValidation(m);
        OntOPE p1 = m.listObjectProperties().findFirst().orElseThrow(AssertionError::new);
        Assert.assertNull(p1.getInverseOf());
        OntOPE p2 = m.createResource().addProperty(OWL.inverseOf, p1).as(OntOPE.class);
        Assert.assertNotNull(p2.getInverseOf());
        Assert.assertEquals(1, p2.inverseOf().count());
        Assert.assertEquals(p1.asProperty(), p2.asProperty());
        Assert.assertEquals(p1, p2.getInverseOf());
        Assert.assertEquals(1, m.ontObjects(OntOPE.Inverse.class).count());
    }

    @Test
    public void testCreateProperties() {
        String ns = "http://test.com/graph/7#";

        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD).setNsPrefix("test", ns);
        OntNAP a1 = m.createOntEntity(OntNAP.class, ns + "a-p-1");
        OntNAP a2 = m.createOntEntity(OntNAP.class, ns + "a-p-2");
        m.createOntEntity(OntNOP.class, ns + "o-p-1");
        m.createOntEntity(OntNOP.class, ns + "o-p-2").createInverse();
        m.createOntEntity(OntNOP.class, ns + "o-p-3").createInverse().addComment("Anonymous property expression");
        m.createOntEntity(OntNOP.class, ns + "o-p-4")
                .addInverseOf(m.createOntEntity(OntNOP.class, ns + "o-p-5"))
                .addAnnotation(a1, m.createLiteral("inverse statement, not inverse-property"));
        m.createOntEntity(OntNDP.class, ns + "d-p-1");
        m.createOntEntity(OntNDP.class, ns + "d-p-2").addAnnotation(a2, m.createLiteral("data-property"));

        ReadWriteUtils.print(m);
        simplePropertiesValidation(m);
        Assert.assertEquals(9, m.ontObjects(OntProperty.class).count());
        Assert.assertEquals(11, m.ontObjects(OntPE.class).count());
        Assert.assertEquals(9, m.ontObjects(OntDOP.class).count());
    }

    @Test
    public void testCreateExpressions() {
        String uri = "http://test.com/graph/3";
        String ns = uri + "#";

        OntGraphModel m = OntModelFactory.createModel()
                .setNsPrefix("test", ns)
                .setNsPrefixes(OntModelFactory.STANDARD)
                .setID(uri)
                .getModel();

        OntNDP ndp1 = m.createOntEntity(OntNDP.class, ns + "dataProperty1");
        OntDT dt1 = m.createOntEntity(OntDT.class, ns + "dataType1");
        dt1.addEquivalentClass(m.getOntEntity(OntDT.class, XSD.dateTime));

        OntDT dt2 = m.createOntEntity(OntDT.class, ns + "dataType2");

        OntFR fr1 = m.createFacetRestriction(OntFR.MaxExclusive.class, ResourceFactory.createTypedLiteral(12));
        OntFR fr2 = m.createFacetRestriction(OntFR.LangRange.class, ResourceFactory.createTypedLiteral("\\d+"));

        OntDR dr1 = m.createRestrictionDataRange(dt1, Arrays.asList(fr1, fr2));

        OntCE ce1 = m.createDataSomeValuesFrom(ndp1, dr1);

        OntDR dr2 = m.createIntersectionOfDataRange(Arrays.asList(dt1, dt2));
        OntIndividual i1 = ce1.createIndividual(ns + "individual1");
        OntCE ce2 = m.createDataMaxCardinality(ndp1, 343434, dr2);
        i1.attachClass(ce2);
        i1.attachClass(m.createOntEntity(OntClass.class, ns + "Class1"));

        OntIndividual i2 = ce2.createIndividual();
        i2.addStatement(ndp1, ResourceFactory.createPlainLiteral("individual value"));

        ReadWriteUtils.print(m);
        Assert.assertEquals("Incorrect count of individuals", 2, m.ontObjects(OntIndividual.class).count());
        Assert.assertEquals("Incorrect count of class expressions", 3, m.ontObjects(OntCE.class).count());
        Assert.assertEquals("Incorrect count of restrictions", 2, m.ontObjects(OntCE.RestrictionCE.class).count());
        Assert.assertEquals("Incorrect count of cardinality restrictions", 1,
                m.ontObjects(OntCE.CardinalityRestrictionCE.class).count());
        Assert.assertEquals("Incorrect count of datatype entities", 2, m.ontObjects(OntDT.class).count());
        Assert.assertEquals("Incorrect count of data properties", 1, m.ontObjects(OntNDP.class).count());
        Assert.assertEquals("Incorrect count of facet restrictions", 2, m.ontObjects(OntFR.class).count());
        Assert.assertEquals("Incorrect count of data ranges", 4, m.ontObjects(OntDR.class).count());
        Assert.assertEquals("Incorrect count of entities", 5, m.ontObjects(OntEntity.class).count());
    }

    @Test
    public void testCreateSWRL() {
        String uri = "http://test.com/graph/4";
        String ns = uri + "#";

        OntGraphModel m = OntModelFactory.createModel()
                .setID(uri).getModel()
                .setNsPrefix("test", ns)
                .setNsPrefix("SWRL", SWRL.NS)
                .setNsPrefixes(OntModelFactory.STANDARD);

        OntClass cl1 = m.createOntEntity(OntClass.class, ns + "Class1");
        OntClass cl2 = m.createOntEntity(OntClass.class, ns + "Class2");
        OntIndividual i1 = cl1.createIndividual(ns + "Individual1");

        OntCE.UnionOf cl3 = m.createUnionOf(Arrays.asList(cl1, cl2));
        OntIndividual i2 = cl3.createIndividual();

        OntSWRL.Variable var1 = m.createSWRLVariable(ns + "Variable1");
        OntSWRL.DArg dArg1 = ResourceFactory.createTypedLiteral(12).inModel(m).as(OntSWRL.DArg.class);
        OntSWRL.DArg dArg2 = var1.as(OntSWRL.DArg.class);

        OntSWRL.Atom.BuiltIn atom1 = m.createBuiltInSWRLAtom(ResourceFactory.createResource(ns + "AtomPredicate1"),
                Arrays.asList(dArg1, dArg2));
        OntSWRL.Atom.OntClass atom2 = m.createClassSWRLAtom(cl2, i2.as(OntSWRL.IArg.class));
        OntSWRL.Atom.SameIndividuals atom3 = m.createSameIndividualsSWRLAtom(i1.as(OntSWRL.IArg.class),
                var1.as(OntSWRL.IArg.class));
        OntSWRL.Imp imp = m.createSWRLImp(Collections.singletonList(atom1), Arrays.asList(atom2, atom3));
        imp.addComment("This is SWRL Imp").addAnnotation(m.getRDFSLabel(), cl1.createIndividual());

        ReadWriteUtils.print(m);
        LOGGER.debug("All D-Args");
        m.ontObjects(OntSWRL.DArg.class).map(String::valueOf).forEach(LOGGER::debug);
        LOGGER.debug("All I-Args");
        m.ontObjects(OntSWRL.IArg.class).map(String::valueOf).forEach(LOGGER::debug);
        Assert.assertEquals("Incorrect count of atoms", 3, m.ontObjects(OntSWRL.Atom.class).count());
        Assert.assertEquals("Incorrect count of variables", 1, m.ontObjects(OntSWRL.Variable.class).count());
        Assert.assertEquals("Incorrect count of SWRL:Imp", 1, m.ontObjects(OntSWRL.Imp.class).count());
        Assert.assertEquals("Incorrect count of SWRL Objects", 5, m.ontObjects(OntSWRL.class).count());
        // literals(2) and variables(1):
        Assert.assertEquals("Incorrect count of SWRL D-Arg", 3, m.ontObjects(OntSWRL.DArg.class).count());
        // individuals(2 anonymous, 1 named) and variables(1):
        Assert.assertEquals("Incorrect count of SWRL I-Arg", 4, m.ontObjects(OntSWRL.IArg.class).count());

        Assert.assertEquals(3, m.statements(null, RDF.type, SWRL.AtomList)
                .map(OntStatement::getSubject)
                .map(s -> s.as(RDFList.class))
                .peek(s -> LOGGER.debug("SWRL-List: {}", s.asJavaList()))
                .count());
    }

    @Test
    public void testCreateImports() {
        String baseURI = "http://test.com/graph/5";
        String baseNS = baseURI + "#";
        OntGraphModel base = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID(baseURI).getModel();
        OntClass cl1 = base.createOntEntity(OntClass.class, baseNS + "Class1");
        OntClass cl2 = base.createOntEntity(OntClass.class, baseNS + "Class2");

        String childURI = "http://test.com/graph/6";
        String childNS = childURI + "#";
        OntGraphModel child = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID(childURI).getModel().addImport(base);
        OntClass cl3 = child.createOntEntity(OntClass.class, childNS + "Class3");
        cl3.addSubClassOf(child.createIntersectionOf(Arrays.asList(cl1, cl2)));
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
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        m.setID("http://example.com/xxx");

        String schemaNS = m.getID().getURI() + "#";
        String dataNS = m.getID().getURI() + "/data#";
        m.setNsPrefix("schema", schemaNS).setNsPrefix("data", dataNS);

        OntDT email = m.createOntEntity(OntDT.class, schemaNS + "email");
        OntDT phone = m.createOntEntity(OntDT.class, schemaNS + "phone");
        OntDT skype = m.createOntEntity(OntDT.class, schemaNS + "skype");
        OntNDP contactInfo = m.createOntEntity(OntNDP.class, schemaNS + "info");
        OntClass contact = m.createOntEntity(OntClass.class, schemaNS + "Contact");
        OntClass person = m.createOntEntity(OntClass.class, schemaNS + "Person");
        OntNOP hasContact = m.createOntEntity(OntNOP.class, schemaNS + "contact");

        hasContact.addDomain(person).getSubject(OntNOP.class).addRange(contact);

        contactInfo.addDomain(contact).getSubject(OntNDP.class)
                .addRange(email).getSubject(OntNDP.class)
                .addRange(phone).getSubject(OntNDP.class)
                .addRange(skype);

        // data:
        OntIndividual bobs = contact.createIndividual(dataNS + "bobs");
        bobs.addAssertion(contactInfo, email.createLiteral("bob@x-email.com"))
                .addAssertion(m.getRDFSLabel(), m.createLiteral("Bob's contacts"))
                .addAssertion(contactInfo, phone.createLiteral(String.valueOf(98_968_78_98_792L)));
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
    public void testBuiltInsGeneralFunctionality() {
        OntGraphModel m = OntModelFactory.createModel();
        Assert.assertEquals(0, m.getOWLBottomObjectProperty().spec().count());
        Assert.assertEquals(0, m.getOWLBottomObjectProperty().statements().count());
        Assert.assertFalse(m.getOWLTopObjectProperty().isLocal());
        Assert.assertNull(m.getOWLTopDataProperty().getRoot());
        Assert.assertEquals(0, m.getOWLNothing().types().count());
        Assert.assertEquals(0, m.getRDFSLabel().content().count());
    }

    @Test
    public void testCreateEntities() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        createEntityTest(m, "a-p", OntNAP.class);
        createEntityTest(m, "o-p", OntNOP.class);
        createEntityTest(m, "d-p", OntNDP.class);
        createEntityTest(m, "c", OntClass.class);
        createEntityTest(m, "d", OntDT.class);
        createEntityTest(m, "I", OntIndividual.Named.class);
        ReadWriteUtils.print(m);
    }

    private <E extends OntEntity> void createEntityTest(OntGraphModel m, String uri, Class<E> type) {
        String pref = "Annotation[" + uri + "]:::";
        E e = m.createOntEntity(type, uri);
        e.addComment(pref + "entity of type " + type.getSimpleName()).addAnnotation(m.getRDFSLabel(), pref + "label");
        m.asStatement(e.getRoot().asTriple()).addAnnotation(m.getRDFSComment(), pref + "comment");
        Assert.assertEquals(2, e.annotations().count());
        Assert.assertEquals(2, e.statements().count());
        Assert.assertSame(e, e.as(type));
        Assert.assertSame(e, ((OntGraphModelImpl) m).getNodeAs(e.asNode(), type));
    }

    @Test
    public void testObjectsContent() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        // properties:
        OntNDP p1 = m.createOntEntity(OntNDP.class, "p1");
        OntNOP p2 = m.createOntEntity(OntNOP.class, "p2");
        // classes:
        OntClass class1 = m.createOntEntity(OntClass.class, "c");
        OntCE.UnionOf class2 = m.createUnionOf(Arrays.asList(m.createOntEntity(OntClass.class, "c1"),
                m.createOntEntity(OntClass.class, "c2")));
        OntCE.DataHasValue class3 = m.createDataHasValue(p1, m.createLiteral("2"));
        OntCE.DataMinCardinality class4 = m.createDataMinCardinality(p1, 2,
                m.getOntEntity(OntDT.class, XSD.xdouble));
        OntClass class5 = m.getOWLThing();
        OntCE.ObjectCardinality class6 = m.createObjectCardinality(p2, 1234, class5);
        OntCE.HasSelf class7 = m.createHasSelf(p2);
        class3.addComment("The Restriction");
        class1.addSubClassOf(class2).getSubject(OntCE.class).addSubClassOf(class3);
        class1.addDisjointWith(class4);
        class2.addSubClassOf(m.createComplementOf(class5));
        class5.addEquivalentClass(m.getOWLNothing());
        // data-ranges:
        OntDT dr1 = m.getOntEntity(OntDT.class, XSD.xint);
        OntDR.IntersectionOf dr2 = m.createIntersectionOfDataRange(Arrays.asList(dr1,
                m.getOntEntity(OntDT.class, XSD.xdouble)));
        OntDR.ComplementOf dr3 = m.createComplementOfDataRange(dr2);
        dr3.addComment("Data range: complement of intersection int and double");
        // individuals:
        OntIndividual i1 = class5.createIndividual("i1");
        OntIndividual i2 = class6.createIndividual();
        // nap:
        OntNPA npa1 = p1.addNegativeAssertion(i1, m.createLiteral("xxx"));

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
        Assert.assertEquals(2, i1.content().map(Models::toString)
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
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);

        OntCE class1 = m.createOntEntity(OntClass.class, "C-1");
        OntCE class2 = m.createOntEntity(OntClass.class, "C-2");
        OntCE class3 = m.createOntEntity(OntClass.class, "C-3");
        OntOPE p = m.createOntEntity(OntNOP.class, "P");
        OntCE class4 = m.createComplementOf(class3);
        OntCE class5 = m.createObjectSomeValuesFrom(p, class4);
        OntCE class6 = m.createIntersectionOf(Arrays.asList(m.getOWLThing(), class2, class4, class5));
        Assert.assertEquals(6, m.ontObjects(OntCE.class).count());
        long size = m.size();
        OntDisjoint d = m.createDisjointClasses(Arrays.asList(m.getOWLNothing(), class1, class6));
        ReadWriteUtils.print(m);

        m.removeOntObject(d);
        ReadWriteUtils.print(m);
        Assert.assertEquals(size, m.statements().count());

        m.removeOntObject(class6).removeOntObject(class5).removeOntObject(class4).removeOntObject(p);
        ReadWriteUtils.print(m);

        Assert.assertEquals(3, m.size());
    }

    @Test
    public void testDataRanges() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntFR f1 = m.createFacetRestriction(OntFR.MaxExclusive.class, m.createTypedLiteral(12));
        OntFR f2 = m.createFacetRestriction(OntFR.Pattern.class, m.createTypedLiteral("\\d+"));
        OntFR f3 = m.createFacetRestriction(OntFR.LangRange.class, m.createTypedLiteral("^r.*"));

        OntDT d1 = m.getOntEntity(OntDT.class, XSD.xstring);
        OntDR d2 = m.createComplementOfDataRange(d1);
        OntDR d3 = m.createRestrictionDataRange(d1, Arrays.asList(f1, f2, f3));

        ReadWriteUtils.print(m);
        Assert.assertEquals(3, m.ontObjects(OntFR.class).count());
        Assert.assertEquals(2, m.ontObjects(OntDR.class).count());
        Assert.assertEquals(1, m.ontObjects(OntDR.ComponentsDR.class).count());
        Assert.assertEquals(d2, m.ontObjects(OntDR.class).filter(s -> s.canAs(OntDR.ComplementOf.class))
                .findFirst().orElseThrow(AssertionError::new));
        Assert.assertEquals(d3, m.ontObjects(OntDR.class).filter(s -> s.canAs(OntDR.Restriction.class))
                .findFirst().orElseThrow(AssertionError::new));

        Assert.assertEquals(XSD.xstring, d3.as(OntDR.Restriction.class).getDatatype());
        Assert.assertEquals(12, d3.spec().peek(s -> LOGGER.debug("{}", Models.toString(s))).count());
    }

    @Test
    public void testModelPrefixes() {
        OntGraphModel m = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
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
        OntGraphModel av1 = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID("a").setVersionIRI("v1").getModel();
        OntGraphModel av2 = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID("a").setVersionIRI("v2").getModel();
        OntGraphModel b = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
                .setID("b").getModel();
        OntGraphModel c = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD)
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
        Models.syncImports(b);
        tree = Graphs.importsTreeAsString(b.getGraph());
        LOGGER.debug("3) Tree: \n{}", tree);
        Assert.assertEquals(4, Models.flat(b).count());
        Assert.assertEquals(3, Models.flat(c).count());
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
        OntGraphModel a = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntGraphModel b = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        OntGraphModel c = OntModelFactory.createModel().setNsPrefixes(OntModelFactory.STANDARD);
        a.createOntEntity(OntClass.class, "A");
        b.createOntEntity(OntClass.class, "B");
        c.createOntEntity(OntClass.class, "C");
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
        a.createOntEntity(OntClass.class, "B");
        b.createOntEntity(OntClass.class, "X");
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
        OntGraphModel m = OntModelFactory.createModel(g);
        OntProperty p = m.getOntEntity(OntProperty.class, m.expandPrefix(":isIngredientOf"));
        Assert.assertNotNull(p);
        Assert.assertEquals(0, p.getOrdinal());
        Assert.assertEquals(0, m.getRDFSComment().getOrdinal());
        Assert.assertEquals(0, m.getOWLBottomDataProperty().getOrdinal());
    }

    @Test
    public void testFamilyListObjects() {
        OntGraphModel m = OntModelFactory.createModel(ReadWriteUtils.loadResourceTTLFile("ontapi/family.ttl").getGraph(),
                OntModelConfig.ONT_PERSONALITY_LAX);
        assertOntObjects(m, OntEntity.class, 656);
        assertOntObjects(m, OntProperty.class, 90);

        assertOntObjects(m, OntClass.class, 58);
        assertOntObjects(m, OntDT.class, 0);
        assertOntObjects(m, OntIndividual.Named.class, 508);
        assertOntObjects(m, OntNOP.class, 80);
        assertOntObjects(m, OntNAP.class, 1);
        assertOntObjects(m, OntNDP.class, 9);

        assertOntObjects(m, OntOPE.class, 80);
        assertOntObjects(m, OntDOP.class, 89);

        assertOntObjects(m, OntDR.class, 0);

        assertOntObjects(m, OntDisjoint.class, 1);
        assertOntObjects(m, OntDisjoint.Classes.class, 0);
        assertOntObjects(m, OntDisjoint.Individuals.class, 1);
        assertOntObjects(m, OntDisjoint.DataProperties.class, 0);
        assertOntObjects(m, OntDisjoint.ObjectProperties.class, 0);
        assertOntObjects(m, OntDisjoint.Properties.class, 0);

        // todo: handle all other types
    }

}

