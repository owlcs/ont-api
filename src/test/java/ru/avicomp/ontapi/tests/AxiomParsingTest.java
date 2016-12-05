package ru.avicomp.ontapi.tests;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntInternalModel;
import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.translators.AxiomParserProvider;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * to test RDF->Axiom parsing
 * <p>
 * Created by @szuev on 27.11.2016.
 */
public class AxiomParsingTest {
    private static final Logger LOGGER = Logger.getLogger(AxiomParsingTest.class);

    @Test
    public void testAxiomRead() {
        Model m = ReadWriteUtils.loadFromTTL("pizza.ttl");
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
        OWLDataFactory factory = OntManagerFactory.createDataFactory();

        OntInternalModel model = new OntInternalModel(ReadWriteUtils.loadFromTTL("pizza.ttl").getGraph());
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

}
