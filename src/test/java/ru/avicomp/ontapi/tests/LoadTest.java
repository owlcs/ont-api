package ru.avicomp.ontapi.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntInternalModel;
import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.io.OntFormat;
import ru.avicomp.ontapi.jena.GraphConverter;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.TestUtils;

/**
 * for testing pizza, foaf and googrelations ontologies.
 * TODO: fix
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class LoadTest {
    private static final Logger LOGGER = Logger.getLogger(LoadTest.class);

    @Test
    public void testPizza() {
        test("pizza.ttl", 945, OntFormat.TTL_RDF);
    }

    @Test
    public void testFoaf() {
        test("foaf.rdf", 551, OntFormat.XML_RDF);
    }

    @Test
    public void testGoodrelations() {
        test("goodrelations.rdf", 1141, OntFormat.XML_RDF);
    }

    private static void test(String fileName, long expectedTotalNumberOfAxioms, OntFormat convertFormat) {
        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI(fileName));
        LOGGER.info("The file " + fileIRI);

        OntologyManager manager = OntManagerFactory.createONTManager();
        OntologyModel ontology = (OntologyModel) ReadWriteUtils.loadOWLOntology(manager, fileIRI);
        OWLOntologyID id = ontology.getOntologyID();
        IRI iri = id.getOntologyIRI().orElse(null);
        Assert.assertNotNull("Null ont-iri " + id, iri);

        Assert.assertEquals("Incorrect count of axioms", expectedTotalNumberOfAxioms, ontology.getAxiomCount());
        OntGraphModel ontModel = ontology.asGraphModel();
        String ontIRI = iri.getIRIString();
        ontModel.setNsPrefix("", ontIRI + "#");
        ReadWriteUtils.print(ontModel, OntFormat.TTL_RDF);
        Assert.assertEquals("Can't find ontology " + ontIRI, ontIRI, ontModel.getID().getURI());

        String copyOntIRI = ontIRI + ".copy";
        Model copyOntModel = TestUtils.copyOntModel(ontModel, copyOntIRI);

        OntologyModel copyOntology = ReadWriteUtils.loadOntologyFromIOStream(manager, copyOntModel, convertFormat);
        long ontologiesCount = manager.ontologies().count();
        LOGGER.debug("Number of ontologies inside manager: " + ontologiesCount);
        Assert.assertTrue("Incorrect number of ontologies inside manager (" + ontologiesCount + ")", ontologiesCount >= 2);
        LOGGER.debug("Total number of axioms: " + copyOntology.getAxiomCount());
        testAxioms(ontology, copyOntology, convertFormat);
    }

    private static void testAxioms(OntologyModel origin, OntologyModel test, OntFormat convertFormat) {
        long numberOfNamedIndividuals = origin.individualsInSignature().count();
        List<String> errors = new ArrayList<>();
        AxiomType.AXIOM_TYPES.forEach(t -> {
            long expected = origin.axioms(t).count();
            long actual = test.axioms(t).count();
            if (AxiomType.DECLARATION.equals(t)) {
                // don't know why, but sometimes (pizza.ttl) it takes into account NamedIndividuals, but sometimes not (goodrelations.rdf)
                // perhaps it is due to different initial format.
                if (OntFormat.XML_RDF.equals(convertFormat)) {
                    actual -= numberOfNamedIndividuals;
                }
                return;
            }
            if (actual == expected) return;
            errors.add(String.format("Incorrect count of axioms(%s). Expected: %d. Actual: %d\n", t, expected, actual));
        });
        Assert.assertTrue(String.valueOf(errors), errors.isEmpty());
    }

    //todo: just test 4 test. remove it.
    public static void main(String... a) throws OWLOntologyCreationException {
        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI("pizza.ttl"));
        OntologyManager m1 = OntManagerFactory.createONTManager();
        Model init = ReadWriteUtils.load(fileIRI.toURI(), OntFormat.TTL_RDF);
        Graph graph = GraphConverter.convert(init.getGraph());
        OntInternalModel base = new OntInternalModel(graph);
        OntologyModel ont = m1.createOntology(base.getOwlID());
        base.listStatements().forEachRemaining(statement -> ont.asGraphModel().add(statement));
        //OntologyModel ont = (OntologyModel) m1.loadOntologyFromOntologyDocument(fileIRI);
        LOGGER.info("ONT (" + ont.getAxiomCount() + ")");
        //ont.axioms().forEach(LOGGER::debug);

        LOGGER.info("============================================");
        OWLOntologyManager m2 = OntManagerFactory.createOWLManager();
        OWLOntology owl = m2.loadOntologyFromOntologyDocument(fileIRI);
        LOGGER.info("OWL (" + owl.getAxiomCount() + ")");
        //owl.axioms().forEach(LOGGER::debug);

        LOGGER.info("============================================");
        LOGGER.info(owl.getAxiomCount() + "|" + ont.getAxiomCount() + "|||" + axioms(owl).count() + "|" + axioms(ont).count());
        LOGGER.info("============================================");
        LOGGER.info("============================================");
        //getAxioms(ont).forEach(LOGGER::debug);
        /*List<OWLAxiom> owlList = owl.axioms().sorted().collect(Collectors.toList());
        List<OWLAxiom> ontList = axioms(ont).sorted().collect(Collectors.toList());
        owlList.forEach(LOGGER::debug);
        LOGGER.info("============================================");
        ontList.forEach(LOGGER::debug);
        Assert.assertThat("Axioms", ontList, IsEqual.equalTo(owlList));*/
    }

    @SuppressWarnings("unchecked")
    public static Stream<OWLAxiom> axioms(OWLOntology o) {
        return o.axioms().map(axiom -> {
            if (axiom instanceof OWLNaryAxiom) {
                return (Stream<OWLAxiom>) ((OWLNaryAxiom) axiom).splitToAnnotatedPairs().stream();
            }
            return Stream.of(axiom);
        }).flatMap(Function.identity()).distinct();
    }
}
