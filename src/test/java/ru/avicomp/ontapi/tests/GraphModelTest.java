package ru.avicomp.ontapi.tests;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import ru.avicomp.ontapi.jena.impl.GraphModelImpl;
import ru.avicomp.ontapi.jena.impl.OntCEImpl;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * to test {@link ru.avicomp.ontapi.jena.impl.GraphModelImpl}
 * Created by szuev on 07.11.2016.
 */
public class GraphModelTest {
    private static final Logger LOGGER = Logger.getLogger(GraphModelTest.class);

    @Test
    public void testLoadCE() {
        LOGGER.info("load pizza");
        GraphModel m = new GraphModelImpl(loadGraph("pizza.ttl"));
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
        GraphModel m = new GraphModelImpl(loadGraph("pizza.ttl"));
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
        GraphModel m = new GraphModelImpl(loadGraph("pizza.ttl"));
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

    @Test
    public void testCreate() {
        String uri = "http://test.com/graph/1";
        String ns = uri + "#";

        GraphModel m = new GraphModelImpl();
        m.setNsPrefix("test", ns);
        m.setNsPrefix("owl", OWL2.getURI());
        m.setNsPrefix("rdfs", RDFS.getURI());

        m.setID(uri).setVersionIRI(ns + "1.0.1");
        m.getID().addComment("Some comment", "fr");

        OntClass cl = m.createOntObject(OntClass.class, ns + "ClassN1");
        cl.addLabel("some label", null);
        cl.addLabel("another label", "de");
        cl.annotations(m.getRDFSLabel()).forEach(LOGGER::debug);

        ReadWriteUtils.print(m);
    }

}
