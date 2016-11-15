package ru.avicomp.ontapi.tests;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.UniqueFilter;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import ru.avicomp.ontapi.jena.impl.OntCEImpl;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * to test {@link OntGraphModel}
 *
 * Created by szuev on 07.11.2016.
 */
public class GraphModelJenaTest {
    private static final Logger LOGGER = Logger.getLogger(GraphModelJenaTest.class);

    @Test
    public void testLoadCE() {
        LOGGER.info("load pizza");
        OntGraphModel m = new OntGraphModelImpl(loadGraph("pizza.ttl"));
        LOGGER.info("Ontology: " + m.getID());

        List<OntClass> classes = m.ontObjects(OntClass.class).collect(Collectors.toList());
        int expectedClassesCount = m.listStatements(null, RDF.type, OWL2.Class).mapWith(Statement::getSubject).filterKeep(RDFNode::isURIResource).toSet().size();
        int actualClassesCount = classes.size();
        LOGGER.info("Classes Count = " + actualClassesCount);
        Assert.assertEquals("Incorrect Classes count", expectedClassesCount, actualClassesCount);

        LOGGER.info("Class Expressions:");
        List<OntCE> ces = m.ontObjects(OntCE.class).collect(Collectors.toList());
        ces.forEach(LOGGER::debug);
        int expectedCEsCount = m.listStatements(null, RDF.type, OWL2.Class).andThen(m.listStatements(null, RDF.type, OWL2.Restriction)).toSet().size();
        int actualCEsCount = ces.size();
        LOGGER.info("Class Expressions Count = " + actualCEsCount);
        Assert.assertEquals("Incorrect CE's count", expectedCEsCount, actualCEsCount);

        List<OntCE.RestrictionCE> restrictionCEs = m.ontObjects(OntCE.RestrictionCE.class).collect(Collectors.toList());
        Assert.assertEquals("Incorrect count of restrictions ", m.listStatements(null, RDF.type, OWL2.Restriction).toSet().size(), restrictionCEs.size());

        List<OntCE.ObjectSomeValuesFrom> objectSomeValuesFromCEs = m.ontObjects(OntCE.ObjectSomeValuesFrom.class).collect(Collectors.toList());
        List<OntCE.ObjectAllValuesFrom> objectAllValuesFromCEs = m.ontObjects(OntCE.ObjectAllValuesFrom.class).collect(Collectors.toList());
        List<OntCE.ObjectHasValue> objectHasValueCEs = m.ontObjects(OntCE.ObjectHasValue.class).collect(Collectors.toList());
        List<OntCE.UnionOf> unionOfCEs = m.ontObjects(OntCE.UnionOf.class).collect(Collectors.toList());
        List<OntCE.IntersectionOf> intersectionOfCEs = m.ontObjects(OntCE.IntersectionOf.class).collect(Collectors.toList());
        List<OntCE.ComplementOf> complementOfCEs = m.ontObjects(OntCE.ComplementOf.class).collect(Collectors.toList());
        List<OntCE.OneOf> oneOfCEs = m.ontObjects(OntCE.OneOf.class).collect(Collectors.toList());
        List<OntCE.ObjectMinCardinality> objectMinCardinalityCEs = m.ontObjects(OntCE.ObjectMinCardinality.class).collect(Collectors.toList());

        testPizzaCEs(m, OWL2.someValuesFrom, objectSomeValuesFromCEs);
        testPizzaCEs(m, OWL2.allValuesFrom, objectAllValuesFromCEs);
        testPizzaCEs(m, OWL2.hasValue, objectHasValueCEs);
        testPizzaCEs(m, OWL2.unionOf, unionOfCEs);
        testPizzaCEs(m, OWL2.intersectionOf, intersectionOfCEs);
        testPizzaCEs(m, OWL2.complementOf, complementOfCEs);
        testPizzaCEs(m, OWL2.oneOf, oneOfCEs);
        testPizzaCEs(m, OWL2.minCardinality, objectMinCardinalityCEs);
    }

    @Test
    public void testLoadProperties() {
        LOGGER.info("load pizza");
        OntGraphModel m = new OntGraphModelImpl(loadGraph("pizza.ttl"));
        List<OntPE> actual = m.ontObjects(OntPE.class).collect(Collectors.toList());
        actual.forEach(LOGGER::debug);
        Set<Resource> expected = new HashSet<>();
        Stream.of(OWL2.AnnotationProperty, OWL2.DatatypeProperty, OWL2.ObjectProperty)
                .forEach(r -> expected.addAll(m.listStatements(null, RDF.type, r).mapWith(Statement::getSubject).toSet()));
        Assert.assertEquals("Incorrect number of properties", expected.size(), actual.size());
    }

    @Test
    public void testLoadIndividuals() {
        LOGGER.info("load pizza");
        OntGraphModel m = new OntGraphModelImpl(loadGraph("pizza.ttl"));
        List<OntIndividual> individuals = m.ontObjects(OntIndividual.class).collect(Collectors.toList());
        individuals.forEach(i -> LOGGER.debug(i + " classes: " + i.classes().collect(Collectors.toSet())));

        Set<Resource> namedIndividuals = m.listSubjectsWithProperty(RDF.type, OWL2.NamedIndividual).toSet();
        Set<Resource> anonIndividuals = m.listStatements(null, RDF.type, (RDFNode) null)
                .filterKeep(s -> s.getSubject().isAnon())
                .filterKeep(s -> s.getObject().isResource() && m.contains(s.getObject().asResource(), RDF.type, OWL2.Class))
                .mapWith(Statement::getSubject).toSet();
        Set<Resource> expected = new HashSet<>(namedIndividuals);
        expected.addAll(anonIndividuals);
        Assert.assertEquals("Incorrect number of individuals", expected.size(), individuals.size());
    }

    private static void testPizzaCEs(Model m, Property predicate, List<? extends OntCE> ces) {
        String type = ces.isEmpty() ? null : ((OntCEImpl) ces.get(0)).getActualClass().getSimpleName();
        Assert.assertEquals("Incorrect count of " + type, m.listSubjectsWithProperty(predicate).toSet().size(), ces.size());
    }

    private static Graph loadGraph(String file) {
        return ReadWriteUtils.load(ReadWriteUtils.getResourceURI(file), null).getGraph();
    }

    private static void setDefaultPrefixes(OntGraphModel m) {
        m.setNsPrefix("owl", OWL2.getURI());
        m.setNsPrefix("rdfs", RDFS.getURI());
        m.setNsPrefix("rdf", RDF.getURI());
    }

    @Test
    public void testCreatePlainAnnotations() {
        String uri = "http://test.com/graph/1";
        String ns = uri + "#";

        OntGraphModel m = new OntGraphModelImpl();
        m.setNsPrefix("test", ns);
        setDefaultPrefixes(m);

        LOGGER.info("1) Assign version-iri and ontology comment.");
        m.setID(uri).setVersionIRI(ns + "1.0.1");
        m.getID().addComment("Some comment", "fr");
        m.getID().annotations().forEach(LOGGER::debug);

        LOGGER.info("2) Create class with two labels.");
        OntClass cl = m.createOntEntity(OntClass.class, ns + "ClassN1");
        cl.addLabel("some label", null);
        OntStatement label2 = cl.addLabel("another label", "de");
        ReadWriteUtils.print(m);
        cl.annotations().forEach(LOGGER::debug);
        Assert.assertEquals("Incorrect count of labels.", 2, m.listObjectsOfProperty(cl, RDFS.label).toList().size());

        LOGGER.info("3) Annotate annotation " + label2);
        OntStatement seeAlsoForLabel2 = label2.addAnnotation(m.getAnnotationProperty(RDFS.seeAlso), ResourceFactory.createResource("http://see.also/1"));
        OntStatement labelForLabel2 = label2.addAnnotation(m.getRDFSLabel(), ResourceFactory.createPlainLiteral("label"));
        ReadWriteUtils.print(m);
        cl.annotations().forEach(LOGGER::debug);
        Assert.assertTrue("Can't find owl:Axiom section.", m.contains(null, RDF.type, OWL2.Axiom));
        Assert.assertTrue("Can't find owl:Annotation section.", m.contains(null, RDF.type, OWL2.Annotation));

        LOGGER.info("4) Create annotation property and annotate " + seeAlsoForLabel2 + " and " + labelForLabel2);
        OntNAP nap1 = m.createOntEntity(OntNAP.class, ns + "annotation-prop-1");
        seeAlsoForLabel2.addAnnotation(nap1, ResourceFactory.createPlainLiteral("comment to see also"));
        OntStatement annotationForLabelForLabel2 = labelForLabel2.addAnnotation(nap1, ResourceFactory.createPlainLiteral("comment to see label"));
        ReadWriteUtils.print(m);
        Assert.assertEquals("Expected two roots with owl:Annotation.", 2, m.listStatements(null, RDF.type, OWL2.Annotation)
                .filterKeep(s -> !m.contains(null, null, s.getSubject())).filterKeep(new UniqueFilter<>()).toList().size());
        Assert.assertEquals("Expected three owl:Annotation.", 3, m.listStatements(null, RDF.type, OWL2.Annotation).toList().size());
        Assert.assertEquals("Expected single owl:Axiom.", 1, m.listStatements(null, RDF.type, OWL2.Axiom).toList().size());

        LOGGER.info("5) Delete annotations for " + labelForLabel2);
        labelForLabel2.deleteAnnotation(annotationForLabelForLabel2.getPredicate().as(OntNAP.class), annotationForLabelForLabel2.getObject());
        ReadWriteUtils.print(m);
        Assert.assertEquals("Expected one root with owl:Annotation.", 1, m.listStatements(null, RDF.type, OWL2.Annotation)
                .filterKeep(s -> !m.contains(null, null, s.getSubject())).filterKeep(new UniqueFilter<>()).toList().size());
        Assert.assertEquals("Expected two owl:Annotation.", 2, m.listStatements(null, RDF.type, OWL2.Annotation).toList().size());
        Assert.assertEquals("Expected single owl:Axiom.", 1, m.listStatements(null, RDF.type, OWL2.Axiom).toList().size());


        LOGGER.info("6) Delete all annotations for " + label2);
        label2.clearAnnotations();
        ReadWriteUtils.print(m);
        Assert.assertEquals("Incorrect count of labels.", 2, m.listObjectsOfProperty(cl, RDFS.label).toList().size());
        Assert.assertFalse("There is owl:Axiom", m.contains(null, RDF.type, OWL2.Axiom));
        Assert.assertFalse("There is owl:Annotation", m.contains(null, RDF.type, OWL2.Annotation));

        LOGGER.info("7) Annotate sub-class-of");
        OntStatement subClassOfAnnotation = cl.addSubClassOf(m.getOWLThing())
                .addAnnotation(nap1, ResourceFactory.createPlainLiteral("test"));
        subClassOfAnnotation.addAnnotation(m.getRDFSLabel(), ResourceFactory.createPlainLiteral("test2"))
                .addAnnotation(m.getRDFSComment(), ResourceFactory.createPlainLiteral("test3"));
        ReadWriteUtils.print(m);
        cl.annotations().forEach(LOGGER::debug);
        Assert.assertEquals("Expected two owl:Annotation.", 2, m.listStatements(null, RDF.type, OWL2.Annotation).toList().size());
        Assert.assertEquals("Expected single owl:Axiom.", 1, m.listStatements(null, RDF.type, OWL2.Axiom).toList().size());
        Assert.assertEquals("Expected 3 root annotations for class " + cl, 3, cl.annotations().count());

        LOGGER.info("8) Deleter all annotations for class " + cl);
        cl.clearAnnotations();
        ReadWriteUtils.print(m);
        cl.annotations().forEach(LOGGER::debug);
        Assert.assertEquals("Found annotations for class " + cl, 0, cl.annotations().count());
        Assert.assertFalse("There is owl:Axiom", m.contains(null, RDF.type, OWL2.Axiom));
        Assert.assertFalse("There is owl:Annotation", m.contains(null, RDF.type, OWL2.Annotation));
    }

    @Test
    public void testCreateAnonAnnotations() {
        String uri = "http://test.com/graph/2";
        String ns = uri + "#";

        OntGraphModel m = new OntGraphModelImpl();
        m.setNsPrefix("test", ns);
        setDefaultPrefixes(m);
        m.setID(uri);

        OntClass cl1 = m.createOntEntity(OntClass.class, ns + "Class1");
        OntClass cl2 = m.createOntEntity(OntClass.class, ns + "Class2");
        OntClass cl3 = m.createOntEntity(OntClass.class, ns + "Class3");
        OntNAP nap1 = m.createOntEntity(OntNAP.class, ns + "AnnotationProperty1");

        OntDisjoint.Classes disjointClasses = m.createDisjointClasses(Stream.of(cl1, cl2, cl3));
        Assert.assertEquals("Incorrect owl:AllDisjointClasses number", 1, m.ontObjects(OntDisjoint.Classes.class).count());

        disjointClasses.addLabel("label1", "en");
        disjointClasses.addLabel("comment", "kjpopo").addAnnotation(nap1, ResourceFactory.createStringLiteral("some txt"));
        ReadWriteUtils.print(m);

        Assert.assertFalse("There is owl:Axiom", m.contains(null, RDF.type, OWL2.Axiom));
        Assert.assertEquals("Should be single owl:Annotation", 1, m.listStatements(null, RDF.type, OWL2.Annotation).toList().size());

        OntNOP nop1 = m.createOntEntity(OntNOP.class, ns + "ObjectProperty1");
        OntIndividual.Named ind1 = cl1.createIndividual(ns + "Individual1");
        OntIndividual.Anonymous ind2 = cl2.createIndividual();
        ind2.addComment("anonymous individual", "ru");
        OntNPA.ObjectAssertion nopa = nop1.addNegativeAssertion(ind1, ind2);
        Assert.assertEquals("Incorrect owl:NegativePropertyAssertion number", 1, nop1.negativeAssertions().count());
        nopa.addLabel("label1", null)
                .addAnnotation(m.getRDFSLabel(), ResourceFactory.createStringLiteral("label2"))
                .addAnnotation(m.getRDFSLabel(), ResourceFactory.createPlainLiteral("label3"));
        Assert.assertEquals("Should be 3 owl:Annotation", 3, m.listStatements(null, RDF.type, OWL2.Annotation).toList().size());

        ReadWriteUtils.print(m);
    }

}

