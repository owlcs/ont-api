package ru.avicomp.ontapi.tests;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntInternalModel;
import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.jena.OntFactory;
import ru.avicomp.ontapi.jena.converters.GraphTransformConfig;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.configuration.OntPersonality;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.translators.AxiomParserProvider;
import ru.avicomp.ontapi.translators.AxiomTranslator;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * to test RDF->Axiom parsing
 * <p>
 * Created by @szuev on 27.11.2016.
 */
public class InternalModelTest {
    private static final Logger LOGGER = Logger.getLogger(InternalModelTest.class);

    @Test
    public void testAxiomRead() {
        Model m = ReadWriteUtils.loadResourceTTLFile("pizza.ttl");
        OntGraphModel model = OntFactory.createModel(m.getGraph());
        // 39 axiom types:
        Set<Class<? extends OWLAxiom>> types = AxiomType.AXIOM_TYPES.stream().map(AxiomType::getActualClass).collect(Collectors.toSet());

        types.forEach(view -> check(model, view));

        Map<OWLAxiom, Set<Triple>> axioms = types.stream()
                .map(view -> AxiomParserProvider.get(view).read(model))
                .map(Collection::stream)
                .flatMap(Function.identity())
                .collect(Collectors.toMap(AxiomTranslator.Triples::getObject, AxiomTranslator.Triples::getTriples));

        LOGGER.info("Recreate model");
        Model m2 = ModelFactory.createDefaultModel();
        model.getID().statements().forEach(m2::add);
        axioms.forEach((axiom, triples) -> {
            triples.forEach(triple -> m2.getGraph().add(triple));
        });
        m2.setNsPrefixes(m.getNsPrefixMap());

        ReadWriteUtils.print(m2);
        Set<Statement> actual = m2.listStatements().toSet();
        Set<Statement> expected = m.listStatements().toSet();
        Assert.assertThat("Incorrect statements (actual=" + actual.size() + ", expected=" + expected.size() + ")", actual, IsEqual.equalTo(expected));
    }

    @Test
    public void testOntologyAnnotations() {
        OWLDataFactory factory = OntManagerFactory.getDataFactory();

        OntInternalModel model = new OntInternalModel(ReadWriteUtils.loadResourceTTLFile("pizza.ttl").getGraph(), OntModelConfig.getPersonality());

        Set<OWLAnnotation> annotations = model.getAnnotations();
        annotations.forEach(LOGGER::debug);
        Assert.assertEquals("Incorrect annotations count", 4, annotations.size());

        LOGGER.info("Create bulk annotation.");
        OWLAnnotation bulk = factory.getOWLAnnotation(factory.getRDFSLabel(), factory.getOWLLiteral("the label"),
                Stream.of(factory.getRDFSComment("just comment to ontology annotation")));
        model.add(bulk);
        annotations = model.getAnnotations();
        annotations.forEach(LOGGER::debug);
        Assert.assertEquals("Incorrect annotations count", 5, annotations.size());

        LOGGER.info("Create plain(assertion) annotation.");
        OWLAnnotation plain = factory.getOWLAnnotation(factory.getRDFSSeeAlso(), IRI.create("http://please.click.me/"));
        model.add(plain);
        annotations = model.getAnnotations();
        annotations.forEach(LOGGER::debug);
        Assert.assertEquals("Incorrect annotations count", 6, annotations.size());

        LOGGER.info("Remove annotations.");
        OWLAnnotation comment = annotations.stream().filter(a -> a.getProperty().getIRI().toString().equals(RDFS.comment.getURI())).findFirst().orElse(null);
        LOGGER.info("Delete " + bulk);
        model.remove(bulk);
        LOGGER.info("Delete " + comment);
        model.remove(comment);

        annotations = model.getAnnotations();
        annotations.forEach(LOGGER::debug);
        Assert.assertEquals("Incorrect annotations count", 4, annotations.size());
    }

    @Test
    public void testPizzaEntities() {
        testEntities("pizza.ttl", OntFormat.TURTLE);
    }

    @Test
    public void testFoafEntities() {
        String file = "foaf.rdf";
        OntFormat format = OntFormat.RDF_XML;

        OntPersonality profile = OntModelConfig.getPersonality();
        OWLDataFactory factory = OntManagerFactory.getDataFactory();

        OWLOntology owl = loadOWLOntology(file);
        OntInternalModel jena = loadInternalModel(file, format);
        debugPrint(jena, owl);

        test(OWLClass.class, jena.classes(), owl.classesInSignature());
        test(OWLDatatype.class, jena.datatypes(), owl.datatypesInSignature());
        test(OWLNamedIndividual.class, jena.individuals(), owl.individualsInSignature());
        test(OWLAnonymousIndividual.class, jena.anonymousIndividuals(), owl.anonymousIndividuals());
        Set<OWLAnnotationProperty> expectedAnnotationProperties = owl.annotationPropertiesInSignature().collect(Collectors.toSet());
        Set<OWLDataProperty> expectedDataProperties = owl.dataPropertiesInSignature().collect(Collectors.toSet());
        Set<OWLObjectProperty> expectedObjectProperties = owl.objectPropertiesInSignature().collect(Collectors.toSet());

        // <http://purl.org/dc/terms/creator> is owl:ObjectProperty since it is equivalent to <http://xmlns.com/foaf/0.1/maker>
        // see file <owl:equivalentProperty rdf:resource="http://purl.org/dc/terms/creator"/>
        // but OWL-API doesn't see it in entities list.
        OWLObjectProperty creator = factory.getOWLObjectProperty(IRI.create("http://purl.org/dc/terms/creator"));
        expectedObjectProperties.add(creator);

        if (OntModelConfig.ONT_PERSONALITY_STRICT.equals(profile)) { // remove all illegal punnings from OWL-API output:
            Set<Resource> illegalPunnings = Models.getIllegalPunnings(jena);
            LOGGER.debug("Illegal punnings inside graph: " + illegalPunnings);
            Set<OWLAnnotationProperty> illegalAnnotationProperties = illegalPunnings.stream().map(r -> r.inModel(jena))
                    .filter(r -> r.hasProperty(RDF.type, OWL.AnnotationProperty))
                    .map(Resource::getURI).map(IRI::create).map(factory::getOWLAnnotationProperty).collect(Collectors.toSet());
            Set<OWLDataProperty> illegalDataProperties = illegalPunnings.stream().map(r -> r.inModel(jena))
                    .filter(r -> r.hasProperty(RDF.type, OWL.DatatypeProperty))
                    .map(Resource::getURI).map(IRI::create).map(factory::getOWLDataProperty).collect(Collectors.toSet());
            Set<OWLObjectProperty> illegalObjectProperties = illegalPunnings.stream().map(r -> r.inModel(jena))
                    .filter(r -> r.hasProperty(RDF.type, OWL.ObjectProperty))
                    .map(Resource::getURI).map(IRI::create).map(factory::getOWLObjectProperty).collect(Collectors.toSet());
            expectedAnnotationProperties.removeAll(illegalAnnotationProperties);
            expectedDataProperties.removeAll(illegalDataProperties);
            expectedObjectProperties.removeAll(illegalObjectProperties);
        } else if (!OntModelConfig.ONT_PERSONALITY_LAX.equals(profile)) {
            Assert.fail("Unsupported personality profile " + profile);
        }
        test(OWLDataProperty.class, jena.dataProperties(), expectedDataProperties.stream());
        test(OWLAnnotationProperty.class, jena.annotationProperties(), expectedAnnotationProperties.stream());
        test(OWLObjectProperty.class, jena.objectProperties(), expectedObjectProperties.stream());
    }

    @Test
    public void testGoodrelationsEntities() {
        testEntities("goodrelations.rdf", OntFormat.RDF_XML);
    }

    private static <Axiom extends OWLAxiom> void check(OntGraphModel model, Class<Axiom> view) {
        LOGGER.debug("=========================");
        LOGGER.info(view.getSimpleName() + ":");
        Set<AxiomTranslator.Triples<Axiom>> axioms = AxiomParserProvider.get(view).read(model);
        axioms.forEach(e -> {
            Axiom axiom = e.getObject();
            Set<Triple> triples = e.getTriples();
            Assert.assertNotNull("Null axiom", axiom);
            Assert.assertTrue("No associated triples", triples != null && !triples.isEmpty());
            LOGGER.debug(axiom + " " + triples);
        });
    }

    private void testEntities(String file, OntFormat format) {
        OWLOntology owl = loadOWLOntology(file);
        OntInternalModel jena = loadInternalModel(file, format);
        debugPrint(jena, owl);
        test(OWLClass.class, jena.classes(), owl.classesInSignature());
        test(OWLDatatype.class, jena.datatypes(), owl.datatypesInSignature());
        test(OWLNamedIndividual.class, jena.individuals(), owl.individualsInSignature());
        test(OWLAnonymousIndividual.class, jena.anonymousIndividuals(), owl.anonymousIndividuals());
        test(OWLAnnotationProperty.class, jena.annotationProperties(), owl.annotationPropertiesInSignature());
        test(OWLObjectProperty.class, jena.objectProperties(), owl.objectPropertiesInSignature());
        test(OWLDataProperty.class, jena.dataProperties(), owl.dataPropertiesInSignature());
    }

    private void debugPrint(OntInternalModel jena, OWLOntology owl) {
        ReadWriteUtils.print(owl);
        LOGGER.debug("==============================");
        ReadWriteUtils.print(jena);
        LOGGER.debug("==============================");
    }

    private <T extends OWLObject> void test(Class<T> view, Stream<T> ont, Stream<T> owl) {
        LOGGER.info("Test <" + view.getSimpleName() + ">:");
        List<T> actual = ont.sorted().collect(Collectors.toList());
        List<T> expected = owl.sorted().collect(Collectors.toList());
        LOGGER.debug(expected.size() + "(owl, expected) ::: " + actual.size() + "(ont, actual)");
        if (OWLAnonymousIndividual.class.equals(view)) {
            Assert.assertEquals("Incorrect anonymous individuals count ", actual.size(), expected.size());
        } else {
            Assert.assertThat("Incorrect " + view.getSimpleName(), actual, IsEqual.equalTo(expected));
        }
    }

    private OWLOntology loadOWLOntology(String file) {
        URI fileURI = ReadWriteUtils.getResourceURI(file);
        OWLOntologyManager manager = OntManagerFactory.createOWLManager();
        LOGGER.info("Load pure owl from " + fileURI);
        return ReadWriteUtils.loadOWLOntology(manager, IRI.create(fileURI));
    }

    private OntInternalModel loadInternalModel(String file, OntFormat format) {
        URI fileURI = ReadWriteUtils.getResourceURI(file);
        LOGGER.info("Load jena model from " + fileURI);
        Model init = ReadWriteUtils.load(fileURI, format);
        Graph graph = GraphTransformConfig.convert(init.getGraph());
        return new OntInternalModel(graph, OntModelConfig.getPersonality());
    }

}
