package ru.avicomp.ontapi.tests;

import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;

import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.jena.OntFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.SWRL;
import ru.avicomp.ontapi.transforms.GraphTransformers;
import ru.avicomp.ontapi.transforms.Transform;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.SpinModels;

/**
 * to test {@link GraphTransformers}
 * <p>
 * Created by @szuev on 30.10.2016.
 */
public class GraphTransformersTest {
    private static final Logger LOGGER = Logger.getLogger(GraphTransformersTest.class);

    @Test
    public void testLoadSpinLibraries() throws Exception {
        // todo: add custom graph-transformer to present spin sparql-queries as string literals and change test
        // this number reflects the default settings
        final int expectedAxiomsNum = 11094;

        OntologyManager m = OntManagers.createONT();
        m.setOntologyLoaderConfiguration(m.getOntologyLoaderConfiguration()
                .setSupportedSchemes(Stream.of(OntConfig.DefaultScheme.FILE).collect(Collectors.toList())));
        SpinModels.addMappings(m);
        IRI iri = SpinModels.SPINMAPL.getIRI();
        OntologyModel o = m.loadOntology(iri);

        Assert.assertEquals("Incorrect total number of axioms", expectedAxiomsNum, o.axioms(Imports.INCLUDED).count());
        o.axioms(Imports.INCLUDED).forEach(LOGGER::debug);
    }

    @Test
    public void testSPSignature() throws Exception {
        // global transforms:
        GraphTransformers.getTransformers().add(g -> new Transform(g) {
            @Override
            public void perform() {
                LOGGER.info("Finish transformation (" + Graphs.getName(g) + ").");
            }
        });

        OWLOntologyManager manager = OntManagers.createOWL();
        OWLOntologyManager testManager = OntManagers.createOWL();

        OntGraphModel jenaSP = OntFactory.createModel(GraphTransformers.convert(load("spin/sp.ttl").getGraph()), OntModelConfig.ONT_PERSONALITY_LAX);
        OWLOntology owlSP = load(manager, "spin/sp.ttl");
        LOGGER.info("SP(Jena): ");
        ReadWriteUtils.print(jenaSP);
        //LOGGER.info("SP(OWL): ");
        //ReadWriteUtils.print(owlSP);
        signatureTest(owlSP, jenaSP);
        OWLOntology testSP = testManager.loadOntologyFromOntologyDocument(ReadWriteUtils.toInputStream(jenaSP, OntFormat.TURTLE));
        LOGGER.info("SP signature:");
        testSP.signature().forEach(entity -> LOGGER.debug(String.format("%s(%s)", entity, entity.getEntityType())));

        // WARNING:
        // There is a difference in behaviour between ONT-API and OWL-API,
        // Example: spin:violationDetail is ObjectProperty and spin:labelTemplate is DataProperty due to rdfs:range.
        // But OWL-API treats them as AnnotationProperty only.
        // spin:Modules is treated as NamedIndividual by OWL-API and as Class by ONT-API.
        UnionGraph spinGraph = new UnionGraph(load("spin/spin.ttl").getGraph());
        spinGraph.addGraph(jenaSP.getBaseGraph());
        OntGraphModel jenaSPIN = OntFactory.createModel(GraphTransformers.convert(spinGraph));
        OWLOntology owlSPIN = load(manager, "spin/spin.ttl");
        LOGGER.info("SPIN(Jena): ");
        ReadWriteUtils.print(jenaSPIN);
        LOGGER.info("SPIN(OWL): ");
        ReadWriteUtils.print(owlSPIN);

        //testSignature(owlSPIN, jenaSPIN);
        OWLOntology testSPIN = testManager.loadOntologyFromOntologyDocument(ReadWriteUtils.toInputStream(jenaSPIN, OntFormat.TURTLE));
        LOGGER.info("SPIN signature:");
        testSPIN.signature().forEach(entity -> LOGGER.debug(String.format("%s(%s)", entity, entity.getEntityType())));
        LOGGER.info("Origin SPIN signature:");
        owlSPIN.signature().forEach(e -> LOGGER.debug(String.format("%s(%s)", e, e.getEntityType())));

        UnionGraph splGraph = new UnionGraph(load("spin/spl.spin.ttl").getGraph());
        splGraph.addGraph(jenaSPIN.getBaseGraph());
        OntGraphModel jenaSPL = OntFactory.createModel(GraphTransformers.convert(splGraph));
        LOGGER.info("SPL-SPIN(Jena): ");
        ReadWriteUtils.print(jenaSPL);
        LOGGER.info("SPL-SPIN(Jena) All entities: ");
        jenaSPL.ontEntities().forEach(LOGGER::debug);
    }

    @Test
    public void testSWRLVocabulary() throws Exception {
        IRI iri = IRI.create("http://www.w3.org/2003/11/swrl");
        IRI file = IRI.create(ReadWriteUtils.getResourceURI("swrl.owl.rdf"));
        OntologyManager m = OntManagers.createONT();
        m.getOntologyConfigurator().setPersonality(OntModelConfig.ONT_PERSONALITY_LAX);
        OntologyModel o = m.loadOntology(file);
        Assert.assertTrue("No ontology", m.contains(iri));

        ReadWriteUtils.print(o);
        o.axioms().forEach(LOGGER::info);

        Assert.assertNull("rdfs:Literal should not be class", o.asGraphModel().getOntEntity(OntClass.class, RDFS.Literal));
        Assert.assertEquals("Should be DataAllValuesFrom", 1, o.asGraphModel().ontObjects(OntCE.DataAllValuesFrom.class).count());
        Assert.assertNotNull(SWRL.argument2 + " should be data property", o.asGraphModel().getOntEntity(OntNDP.class, SWRL.argument2));
        Assert.assertNotNull(SWRL.argument2 + " should be object property", o.asGraphModel().getOntEntity(OntNOP.class, SWRL.argument2));
    }

    /**
     * The ontology was taken from <a href='https://github.com/Galigator/openllet'>Open Pellet Project</a>
     */
    @Test
    public void testSWRLOntology() throws Exception {
        IRI iri = IRI.create(ReadWriteUtils.getResourceURI("anyURI-premise.rdf"));
        LOGGER.info(iri);
        OWLOntologyManager m = OntManagers.createONT();
        OWLOntology o = m.loadOntology(iri);
        ReadWriteUtils.print(o);
        o.axioms().forEach(LOGGER::info);
        Assert.assertEquals("Incorrect data properties count", 7, o.dataPropertiesInSignature().count());
    }

    private static void signatureTest(OWLOntology owl, OntGraphModel jena) {
        List<String> expectedClasses = owlToList(owl.classesInSignature(Imports.INCLUDED));
        List<String> actualClasses = jenaToList(jena.listClasses());
        Assert.assertTrue("Classes", actualClasses.containsAll(expectedClasses));

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
        //Assert.assertThat("DataProperties", actualDataProperties, IsEqual.equalTo(expectedDataProperties));
        //Assert.assertThat("ObjectProperties", actualObjectProperties, IsEqual.equalTo(expectedObjectProperties));

        List<String> expectedDatatypes = owlToList(owl.datatypesInSignature(Imports.INCLUDED));
        List<String> actualDatatypes = jenaToList(jena.listDatatypes());
        Assert.assertThat("Datatypes", actualDatatypes, IsEqual.equalTo(expectedDatatypes));

        List<String> expectedIndividuals = owlToList(owl.individualsInSignature(Imports.INCLUDED));
        List<String> actualIndividuals = jenaToList(jena.listNamedIndividuals());
        Assert.assertThat("Individuals", actualIndividuals, IsEqual.equalTo(expectedIndividuals));
    }

    private static final Set<IRI> ADDITIONAL_BUILT_IN_ENTITIES = Stream.of(RDF.List, RDFS.Resource, RDF.Property, RDFS.Class, OWL.Ontology).map(r -> IRI.create(r.getURI())).collect(Collectors.toSet());

    private static boolean isNotBuiltIn(OWLEntity entity) {
        return !entity.isBuiltIn() && !ADDITIONAL_BUILT_IN_ENTITIES.contains(entity.getIRI());
    }

    private static Stream<String> owlToStream(Stream<? extends OWLEntity> entities) {
        return entities.filter(GraphTransformersTest::isNotBuiltIn).distinct().map(HasIRI::getIRI).map(IRI::getIRIString).sorted();
    }

    private static List<String> owlToList(Stream<? extends OWLEntity> entities) {
        return owlToStream(entities).collect(Collectors.toList());
    }

    private static Stream<String> jenaToStream(Stream<? extends OntEntity> entities) {
        return entities.map(Resource::getURI).sorted();
    }

    private static List<String> jenaToList(Stream<? extends OntEntity> entities) {
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
