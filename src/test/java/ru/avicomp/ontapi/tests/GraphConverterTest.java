package ru.avicomp.ontapi.tests;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.ontapi.jena.GraphConverter;
import ru.avicomp.ontapi.jena.GraphModelImpl;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * to test {@link GraphConverter}
 * Created by @szuev on 30.10.2016.
 */
public class GraphConverterTest {
    private static final Logger LOGGER = Logger.getLogger(GraphConverterTest.class);

    @Test
    public void test() throws Exception {
        GraphConverter.CONVERTERS.add(g -> new GraphConverter.TransformAction(g) {
            @Override
            public void perform() {
                LOGGER.info("Finish transformation (" + getOntURI(g) + ").");
            }
        });

        OWLOntologyManager manager = OntManagerFactory.createOWLManager();
        OWLOntologyManager testManager = OntManagerFactory.createOWLManager();

        GraphModelImpl jenaSP = new GraphModelImpl(GraphConverter.convert(load("sp.ttl").getGraph()));
        OWLOntology owlSP = load(manager, "sp.ttl");
        LOGGER.info("SP(Jena): ");
        ReadWriteUtils.print(jenaSP);
        LOGGER.info("SP(OWL): ");
        ReadWriteUtils.print(owlSP);
        testSignature(owlSP, jenaSP);
        OWLOntology testSP = testManager.loadOntologyFromOntologyDocument(ReadWriteUtils.toInputStream(jenaSP, OntFormat.TTL_RDF));
        LOGGER.info("SP signature:");
        testSP.signature().forEach(entity -> LOGGER.debug(String.format("%s(%s)", entity, entity.getEntityType())));

        // WARNING:
        // I believe that our GraphConverter makes transformation more correctly than OWL-API.
        // Example: spin:violationDetail is ObjectProperty and spin:labelTemplate is DataProperty due to rdfs:range. But OWL-API treats them as AnnotationProperty only.
        // spin:Modules is treated by OWL-API as NamedIndividual. Why? So i decide do not fully synchronize our API and OWL-API.
        UnionGraph spinGraph = new UnionGraph(load("spin.ttl").getGraph());
        spinGraph.addGraph(jenaSP.getBaseGraph());
        GraphModelImpl jenaSPIN = new GraphModelImpl(GraphConverter.convert(spinGraph));
        OWLOntology owlSPIN = load(manager, "spin.ttl");
        LOGGER.info("SPIN(Jena): ");
        ReadWriteUtils.print(jenaSPIN);
        LOGGER.info("SPIN(OWL): ");
        ReadWriteUtils.print(owlSPIN);

        //testSignature(owlSPIN, jenaSPIN);
        OWLOntology testSPIN = testManager.loadOntologyFromOntologyDocument(ReadWriteUtils.toInputStream(jenaSPIN, OntFormat.TTL_RDF));
        LOGGER.info("SPIN signature:");
        testSPIN.signature().forEach(entity -> LOGGER.debug(String.format("%s(%s)", entity, entity.getEntityType())));
        LOGGER.info("Origin SPIN signature:");
        owlSPIN.signature().forEach(e -> LOGGER.debug(String.format("%s(%s)", e, e.getEntityType())));

        UnionGraph splGraph = new UnionGraph(load("spl.spin.ttl").getGraph());
        splGraph.addGraph(jenaSPIN.getBaseGraph());
        GraphModelImpl jenaSPL = new GraphModelImpl(GraphConverter.convert(splGraph));
        LOGGER.info("SPL-SPIN(Jena): ");
        ReadWriteUtils.print(jenaSPL);
        jenaSPL.listEntities().forEach(e -> LOGGER.debug(String.format("%s(%s)", e, e.getEntityType())));
    }

    private static String getOntURI(Graph graph) {
        List<String> res = graph.find(Node.ANY, RDF.type.asNode(), OWL.Ontology.asNode()).mapWith(Triple::getSubject).mapWith(Node::getURI).toList();
        return res.isEmpty() ? null : res.get(0);
    }

    private static void testSignature(OWLOntology owl, GraphModelImpl jena) {
        List<String> expectedClasses = owlToList(owl.classesInSignature(Imports.INCLUDED));
        List<String> actualClasses = jenaToList(jena.listClasses());
        Assert.assertThat("Classes", actualClasses, IsEqual.equalTo(expectedClasses));

        List<String> expectedAnnotationProperties = owlToList(owl.annotationPropertiesInSignature(Imports.INCLUDED));//, RDFS.comment, RDFS.label, OWL2.deprecated, OWL.versionInfo);
        List<String> actualAnnotationProperties = jenaToList(jena.listAnnotationProperties());
        List<String> expectedDataProperties = owlToList(owl.dataPropertiesInSignature(Imports.INCLUDED));
        List<String> actualDataProperties = jenaToList(jena.listDataProperties());
        List<String> expectedObjectProperties = owlToList(owl.objectPropertiesInSignature(Imports.INCLUDED));
        List<String> actualObjectProperties = jenaToList(jena.listObjectProperties());
        LOGGER.debug("Actual AnnotationProperties: " + actualAnnotationProperties);
        LOGGER.debug("Actual ObjectProperties: " + actualObjectProperties);
        LOGGER.debug("Actual DataProperties: " + actualDataProperties);

        Assert.assertThat("AnnotationProperties", actualAnnotationProperties, IsEqual.equalTo(expectedAnnotationProperties));
        Assert.assertThat("DataProperties", actualDataProperties, IsEqual.equalTo(expectedDataProperties));
        Assert.assertThat("ObjectProperties", actualObjectProperties, IsEqual.equalTo(expectedObjectProperties));

        List<String> expectedDatatypes = owlToList(owl.datatypesInSignature(Imports.INCLUDED));
        List<String> actualDatatypes = jenaToList(jena.listDatatypes());
        Assert.assertThat("Datatypes", actualDatatypes, IsEqual.equalTo(expectedDatatypes));

        List<String> expectedIndividuals = owlToList(owl.individualsInSignature(Imports.INCLUDED));
        List<String> actualIndividuals = jenaToList(jena.listIndividuals());
        Assert.assertThat("Individuals", actualIndividuals, IsEqual.equalTo(expectedIndividuals));
    }

    private static final Set<IRI> ADDITIONAL_BUILT_IN_ENTITIES = Stream.of(RDF.List, RDFS.Resource, RDF.Property, RDFS.Class, OWL.Ontology).map(r -> IRI.create(r.getURI())).collect(Collectors.toSet());

    private static boolean isNotBuiltIn(OWLEntity entity) {
        return !entity.isBuiltIn() && !ADDITIONAL_BUILT_IN_ENTITIES.contains(entity.getIRI());
    }

    private static Stream<String> owlToStream(Stream<? extends OWLEntity> entities) {
        return entities.filter(GraphConverterTest::isNotBuiltIn).distinct().map(HasIRI::getIRI).map(IRI::getIRIString).sorted();
    }

    private static List<String> owlToList(Stream<? extends OWLEntity> entities) {
        return owlToStream(entities).collect(Collectors.toList());
    }

    private static Stream<String> jenaToStream(Stream<? extends GraphModelImpl.OntEntity> entities) {
        return entities.map(Resource::getURI).sorted();
    }

    private static List<String> jenaToList(Stream<? extends GraphModelImpl.OntEntity> entities) {
        return jenaToStream(entities).sorted().collect(Collectors.toList());
    }

    private static Model load(String file) throws Exception {
        Model m = ModelFactory.createDefaultModel();
        try (InputStream in = ReadWriteUtils.getResourceURI(file).toURL().openStream()) {
            m.read(in, null, "ttl");
        }
        return m;
    }

    private static OWLOntology load(OWLOntologyManager manager, String file) throws Exception {
        return manager.loadOntology(OntIRI.create(ReadWriteUtils.getResourceURI(file)));
    }
}
