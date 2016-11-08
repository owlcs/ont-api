package ru.avicomp.ontapi.tests;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import ru.avicomp.ontapi.jena.impl.GraphModelImpl;
import ru.avicomp.ontapi.jena.impl.OntCEImpl;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntClassEntity;
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
        GraphModelImpl m = new GraphModelImpl(loadGraph("pizza.ttl"));

        List<OntClassEntity> classes = m.ontObjects(OntClassEntity.class).collect(Collectors.toList());
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

    private static void testPizzaCEs(Model m, Property predicate, List<? extends OntCE> ces) {
        String type = ces.isEmpty() ? null : ((OntCEImpl) ces.get(0)).getActualClass().getSimpleName();
        Assert.assertEquals("Incorrect count of " + type, m.listSubjectsWithProperty(predicate).toSet().size(), ces.size());
    }

    private static Graph loadGraph(String file) {
        return ReadWriteUtils.load(ReadWriteUtils.getResourceURI(file), null).getGraph();
    }


}
