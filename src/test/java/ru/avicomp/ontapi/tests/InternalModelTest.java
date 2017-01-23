package ru.avicomp.ontapi.tests;

import java.net.URI;
import java.util.Collection;
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
import ru.avicomp.ontapi.jena.converters.GraphConverter;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.translators.AxiomParserProvider;
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
        Model m = ReadWriteUtils.loadFromTTLFile("pizza.ttl");
        OntGraphModel model = new OntGraphModelImpl(m.getGraph());
        // 39 axiom types:
        Set<Class<? extends OWLAxiom>> types = AxiomType.AXIOM_TYPES.stream().map(AxiomType::getActualClass).collect(Collectors.toSet());

        types.forEach(view -> check(model, view));

        Map<OWLAxiom, Set<Triple>> axioms = types.stream()
                .map(view -> AxiomParserProvider.get(view).read(model))
                .map(Map::entrySet)
                .map(Collection::stream)
                .flatMap(Function.identity())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

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

        OntInternalModel model = new OntInternalModel(ReadWriteUtils.loadFromTTLFile("pizza.ttl").getGraph());

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
        // <http://purl.org/dc/terms/creator> is owl:ObjectProperty since it is equivalent to <http://xmlns.com/foaf/0.1/maker>
        // see file <owl:equivalentProperty rdf:resource="http://purl.org/dc/terms/creator"/>
        // but OWL-API doesn't return it in entities list.
        testEntities("foaf.rdf", OntFormat.RDF_XML);
    }

    @Test
    public void testGoodrelationsEntities() {
        testEntities("goodrelations.rdf", OntFormat.RDF_XML);
    }

    private static <Axiom extends OWLAxiom> void check(OntGraphModel model, Class<Axiom> view) {
        LOGGER.debug("=========================");
        LOGGER.info(view.getSimpleName() + ":");
        Map<Axiom, Set<Triple>> axioms = AxiomParserProvider.get(view).read(model);
        axioms.entrySet().forEach(e -> {
            Axiom axiom = e.getKey();
            Set<Triple> triples = e.getValue();
            Assert.assertNotNull("Null axiom", axiom);
            Assert.assertTrue("No associated triples", triples != null && !triples.isEmpty());
            LOGGER.debug(axiom + " " + triples);
        });
    }

    private void testEntities(String file, OntFormat format) {
        OWLOntology owl = loadOWLOntology(file);
        OntInternalModel jena = loadInternalModel(file, format);

        ReadWriteUtils.print(owl);
        LOGGER.debug("==============================");
        ReadWriteUtils.print(jena);
        LOGGER.debug("==============================");

        // foaf contains wrong properties (both owl:DatatypeProperty and owl:ObjectProperty, example: <http://xmlns.com/foaf/0.1/msnChatID>)
        Set<Resource> wrong = Models.asStream(jena.listStatements(null, RDF.type, OWL.ObjectProperty)
                .filterKeep(statement -> jena.contains(statement.getSubject(), RDF.type, OWL.DatatypeProperty))
                .mapWith(Statement::getSubject)).distinct().collect(Collectors.toSet());
        if (!wrong.isEmpty())
            LOGGER.info("Wrong properties:");
        wrong.forEach(LOGGER::warn);


        LOGGER.info("OWLClass:");
        Set<OWLClass> classes1 = owl.classesInSignature().collect(Collectors.toSet());
        Set<OWLClass> classes2 = jena.classes().collect(Collectors.toSet());
        LOGGER.debug(classes1.size() + " ::: " + classes2.size());
        Assert.assertThat("Incorrect classes", classes2, IsEqual.equalTo(classes1));

        LOGGER.info("OWLDatatype:");
        Set<OWLDatatype> datatypes1 = owl.datatypesInSignature().collect(Collectors.toSet());
        Set<OWLDatatype> datatypes2 = jena.datatypes().collect(Collectors.toSet());
        LOGGER.debug(datatypes1.size() + " ::: " + datatypes2.size());
        Assert.assertThat("Incorrect datatypes", datatypes2, IsEqual.equalTo(datatypes1));

        LOGGER.info("OWLNamedIndividual:");
        Set<OWLNamedIndividual> individuals1 = owl.individualsInSignature().collect(Collectors.toSet());
        Set<OWLNamedIndividual> individuals2 = jena.individuals().collect(Collectors.toSet());
        LOGGER.debug(individuals1.size() + " ::: " + individuals2.size());
        Assert.assertThat("Incorrect named individuals", individuals2, IsEqual.equalTo(individuals1));

        LOGGER.info("OWLAnonymousIndividual:");
        Set<OWLAnonymousIndividual> anonymous1 = owl.anonymousIndividuals().collect(Collectors.toSet());
        Set<OWLAnonymousIndividual> anonymous2 = jena.anonymousIndividuals().collect(Collectors.toSet());
        LOGGER.debug(anonymous1.size() + " ::: " + anonymous2.size());
        Assert.assertEquals("Incorrect anonymous individuals", anonymous1.size(), anonymous2.size());

        LOGGER.info("OWLAnnotationProperty:");
        Set<OWLAnnotationProperty> annotationProperties1 = owl.annotationPropertiesInSignature().collect(Collectors.toSet());
        Set<OWLAnnotationProperty> annotationProperties2 = jena.annotationProperties().collect(Collectors.toSet());
        LOGGER.debug(annotationProperties1.size() + " ::: " + annotationProperties2.size());
        Assert.assertThat("Incorrect annotation properties", annotationProperties2, IsEqual.equalTo(annotationProperties1));

        LOGGER.info("OWLObjectProperty:");
        Set<OWLObjectProperty> objectProperties1 = owl.objectPropertiesInSignature().collect(Collectors.toSet());
        Set<OWLObjectProperty> objectProperties2 = jena.objectProperties().collect(Collectors.toSet());
        LOGGER.debug(objectProperties1.size() + " ::: " + objectProperties2.size());
        if ("foaf.rdf".equals(file)) { // WARNING: Wrong behaviour of OWL-API:
            objectProperties2.removeIf(p -> "http://purl.org/dc/terms/creator".equals(p.getIRI().toString()));
        }
        Assert.assertThat("Incorrect object properties", objectProperties2, IsEqual.equalTo(objectProperties1));

        LOGGER.info("OWLDataProperty:");
        Set<OWLDataProperty> dataProperties1 = owl.dataPropertiesInSignature().collect(Collectors.toSet());
        Set<OWLDataProperty> dataProperties2 = jena.dataProperties().collect(Collectors.toSet());
        LOGGER.debug(dataProperties1.size() + " ::: " + dataProperties2.size());
        Assert.assertThat("Incorrect data properties", dataProperties2, IsEqual.equalTo(dataProperties1));
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
        Graph graph = GraphConverter.convert(init.getGraph());
        return new OntInternalModel(graph);
    }

}
