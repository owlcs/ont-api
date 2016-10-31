package ru.avicomp.ontapi.tests;

import java.io.InputStream;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.UniqueFilter;
import org.apache.jena.vocabulary.*;
import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.jena.GraphConverter;
import ru.avicomp.ontapi.jena.GraphModelImpl;
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

        Model sp = load("sp.ttl");
        GraphModelImpl jenaSP = new GraphModelImpl(GraphConverter.convert(sp.getGraph()));
        OWLOntology owlSP = load(manager, "sp.ttl");
        LOGGER.info("SP(Jena): ");
        ReadWriteUtils.print(jenaSP);
        LOGGER.info("SP(OWL): ");
        ReadWriteUtils.print(owlSP);
        testSignature(owlSP, jenaSP);

        /*Model spin = load("spin.ttl");
        UnionGraph spinGraph = new UnionGraph(spin.getGraph());
        spinGraph.addGraph(jenaSP.getBaseGraph());
        GraphModelImpl jenaSPIN = new GraphModelImpl(GraphConverter.convert(spinGraph));
        OWLOntology owlSPIN = load(manager, "spin.ttl");
        LOGGER.info("SPIN(Jena): ");
        ReadWriteUtils.print(jenaSPIN);
        LOGGER.info("SPIN(OWL): ");
        ReadWriteUtils.print(owlSPIN);
        owlSPIN.signature().forEach(e -> LOGGER.debug(String.format("%s(%s)", e, e.getEntityType())));
        testSignature(owlSPIN, jenaSPIN);*/

        /*Model spl = load("spl.spin.ttl");
        UnionGraph splGraph = new UnionGraph(spl.getGraph());
        splGraph.addGraph(sp.getGraph());
        GraphConverter.convert(splGraph);
        LOGGER.info("SPL: ");
        ReadWriteUtils.print(spl);*/

    }

    private static String getOntURI(Graph graph) {
        List<String> res = graph.find(Node.ANY, RDF.type.asNode(), OWL.Ontology.asNode()).mapWith(Triple::getSubject).mapWith(Node::getURI).toList();
        return res.isEmpty() ? null : res.get(0);
    }

    private static void testSignature(OWLOntology owl, GraphModelImpl jena) {
        Stream<OWLClass> owlClasses =
                //owl.classesInSignature();
                Stream.concat(owl.classesInSignature(), owl.imports().map(HasClassesInSignature::classesInSignature).flatMap(Function.identity()));
        List<String> expectedClasses = toList(owlClasses, RDFS.Resource, RDF.List, RDF.Property, OWL.Ontology, RDFS.Class);
        List<String> actualClasses = toList(jena.listEntities().filter(GraphModelImpl.OntEntity::isClass));
        Assert.assertThat("Classes", actualClasses, IsEqual.equalTo(expectedClasses));

        List<String> expectedAnnotationProperties = toList(owl.annotationPropertiesInSignature(), RDFS.comment, RDFS.label, OWL2.deprecated, OWL.versionInfo);
        List<String> actualAnnotationProperties = toList(jena.listEntities(GraphModelImpl.EntityType.ANNOTATION_PROPERTY));
        Assert.assertThat("AnnotationProperties", actualAnnotationProperties, IsEqual.equalTo(expectedAnnotationProperties));

        List<String> expectedDataProperties = toList(owl.dataPropertiesInSignature());
        List<String> actualDataProperties = toList(jena.listEntities(GraphModelImpl.EntityType.DATA_PROPERTY));
        Assert.assertThat("DataProperties", actualDataProperties, IsEqual.equalTo(expectedDataProperties));

        List<String> expectedObjectProperties = toList(owl.objectPropertiesInSignature());
        List<String> actualObjectProperties = toList(jena.listEntities(GraphModelImpl.EntityType.OBJECT_PROPERTY));
        Assert.assertThat("ObjectProperties", actualObjectProperties, IsEqual.equalTo(expectedObjectProperties));

        List<String> expectedDatatypes = toList(owl.datatypesInSignature(), XSD.xboolean, XSD.integer, XSD.xlong, XSD.xstring);
        List<String> actualDatatypes = toList(jena.listEntities(GraphModelImpl.EntityType.DATATYPE));
        Assert.assertThat("Datatypes", actualDatatypes, IsEqual.equalTo(expectedDatatypes));

        List<String> expectedIndividuals = toList(owl.individualsInSignature());
        List<String> actualIndividuals = toList(jena.listEntities(GraphModelImpl.EntityType.INDIVIDUAL));
        Assert.assertThat("Individuals", actualIndividuals, IsEqual.equalTo(expectedIndividuals));
    }

    private static Stream<String> toStream(Stream<? extends OWLEntity> entities, Resource... system) {
        List<String> excluded = Stream.of(system).map(Resource::getURI).collect(Collectors.toList());
        return entities.filter(new UniqueFilter<>()).map(HasIRI::getIRI).map(IRI::getIRIString).filter(u -> !excluded.contains(u)).sorted();
    }

    private static List<String> toList(Stream<? extends OWLEntity> entities, Resource... system) {
        return toStream(entities, system).collect(Collectors.toList());
    }

    private static Stream<String> toStream(Stream<? extends GraphModelImpl.OntEntity> entities) {
        return entities
                //.filter(GraphModelImpl.OntEntity::isLocal)
                .map(Resource::getURI).sorted();
    }

    private static List<String> toList(Stream<? extends GraphModelImpl.OntEntity> entities) {
        return toStream(entities).sorted().collect(Collectors.toList());
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
