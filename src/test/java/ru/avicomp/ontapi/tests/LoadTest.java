package ru.avicomp.ontapi.tests;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * for testing pizza, foaf and googrelations ontologies.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class LoadTest {
    private static final Logger LOGGER = Logger.getLogger(LoadTest.class);

    @Test
    public void testPizza() {
        test("pizza.ttl");
    }

    @Test
    public void testFoaf() {
        // WARNING: OWL-API works wrong with this ontology.
        // Also ontology 'foaf' is wrong in itself: there 6 entities which are DataProperty and ObjectProperty simultaneously.
        // todo: add testing for excluded axioms.
        test("foaf.rdf", AxiomType.DECLARATION, AxiomType.ANNOTATION_PROPERTY_RANGE, AxiomType.DATA_PROPERTY_DOMAIN, AxiomType.ANNOTATION_PROPERTY_DOMAIN);
    }

    @Test
    public void testGoodrelations() {
        String fileName = "goodrelations.rdf";
        OntFormat format = OntFormat.XML_RDF;
        OWLDataFactory factory = OntManagerFactory.getDataFactory();

        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI(fileName));
        LOGGER.info("The file " + fileIRI);

        OntologyModel ont = loadONT(fileIRI);
        OWLOntology owl = loadOWL(fileIRI);

        List<OWLAxiom> owlList = axioms(owl).sorted().collect(Collectors.toList());
        List<OWLAxiom> ontList = axioms(ont).sorted().collect(Collectors.toList());

        ReadWriteUtils.print(ont.asGraphModel());

        Set<AxiomType> excluded = Stream.of(AxiomType.DECLARATION, AxiomType.CLASS_ASSERTION, AxiomType.DATA_PROPERTY_ASSERTION)
                .collect(Collectors.toSet());

        test(owlList, ontList, excluded);

        LOGGER.info("Test separately skipped axioms:");
        LOGGER.debug("Test type <" + AxiomType.DECLARATION + ">");
        List<OWLAxiom> expectedDeclarations = Stream.of(
                owlList.stream()
                        .filter(a -> AxiomType.CLASS_ASSERTION.equals(a.getAxiomType()))
                        .map(OWLClassAssertionAxiom.class::cast)
                        .map(OWLClassAssertionAxiom::getIndividual)
                        .filter(OWLIndividual::isNamed)
                        .map(OWLIndividual::asOWLNamedIndividual)
                        .map(factory::getOWLDeclarationAxiom),
                owlList.stream()
                        .filter(a -> AxiomType.DECLARATION.equals(a.getAxiomType()))).flatMap(Function.identity())
                .sorted().distinct().collect(Collectors.toList());
        List<OWLAxiom> actualDeclarations = ontList.stream()
                .filter(a -> AxiomType.DECLARATION.equals(a.getAxiomType())).collect(Collectors.toList());
        Assert.assertThat("Incorrect declaration axioms (actual=" + actualDeclarations.size() + ", expected=" +
                        expectedDeclarations.size() + ")",
                actualDeclarations, IsEqual.equalTo(expectedDeclarations));

        LOGGER.debug("Test type <" + AxiomType.CLASS_ASSERTION + ">");
        List<OWLAxiom> expectedClassAssertions = Stream.of(
                owlList.stream()
                        .filter(a -> AxiomType.CLASS_ASSERTION.equals(a.getAxiomType()))
                        .map(OWLClassAssertionAxiom.class::cast).filter(a -> a.getIndividual().isNamed()),
                ontList.stream().filter(a -> AxiomType.CLASS_ASSERTION.equals(a.getAxiomType()))
                        .map(OWLClassAssertionAxiom.class::cast)
                        .filter(a -> a.getIndividual().isAnonymous())).flatMap(Function.identity())
                .sorted().distinct().collect(Collectors.toList());
        List<OWLAxiom> actualClassAssertions = ontList.stream()
                .filter(a -> AxiomType.CLASS_ASSERTION.equals(a.getAxiomType())).collect(Collectors.toList());
        Assert.assertThat("Incorrect class-assertions axioms (actual=" + actualClassAssertions.size() + ", expected=" +
                        expectedClassAssertions.size() + ")",
                actualClassAssertions, IsEqual.equalTo(expectedClassAssertions));

        LOGGER.debug("Test type <" + AxiomType.DATA_PROPERTY_ASSERTION + ">");
        List<OWLAxiom> expectedDataPropertyAssertions = Stream.of(
                owlList.stream()
                        .filter(a -> AxiomType.DATA_PROPERTY_ASSERTION.equals(a.getAxiomType()))
                        .map(OWLDataPropertyAssertionAxiom.class::cast).filter(a -> a.getSubject().isNamed()),
                ontList.stream().filter(a -> AxiomType.DATA_PROPERTY_ASSERTION.equals(a.getAxiomType()))
                        .map(OWLDataPropertyAssertionAxiom.class::cast)
                        .filter(a -> a.getSubject().isAnonymous())).flatMap(Function.identity())
                .sorted().distinct().collect(Collectors.toList());
        List<OWLAxiom> actualDataPropertyAssertions = ontList.stream()
                .filter(a -> AxiomType.DATA_PROPERTY_ASSERTION.equals(a.getAxiomType())).collect(Collectors.toList());
        Assert.assertThat("Incorrect data-property-assertions axioms (actual=" + actualDataPropertyAssertions.size() +
                        ", expected=" + expectedDataPropertyAssertions.size() + ")",
                actualDataPropertyAssertions, IsEqual.equalTo(expectedDataPropertyAssertions));

    }


    private void test(String fileName, AxiomType... toExclude) {
        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI(fileName));
        LOGGER.info("The file " + fileIRI);

        OntologyModel ont = loadONT(fileIRI);
        OWLOntology owl = loadOWL(fileIRI);

        List<OWLAxiom> owlList = axioms(owl).sorted().collect(Collectors.toList());
        List<OWLAxiom> ontList = axioms(ont).sorted().collect(Collectors.toList());

        ReadWriteUtils.print(ont.asGraphModel());

        Set<AxiomType> excluded = Stream.of(toExclude).collect(Collectors.toSet());

        test(owlList, ontList, excluded);
    }

    private void test(List<OWLAxiom> owlList, List<OWLAxiom> ontList, Set<AxiomType> excluded) {
        AxiomType.AXIOM_TYPES.forEach(type -> {
            LOGGER.debug("Test type <" + type + ">");
            if (excluded.contains(type)) {
                LOGGER.warn("Skip <" + type + ">");
                return;
            }
            List<OWLAxiom> actual = ontList.stream().filter(axiom -> type.equals(axiom.getAxiomType())).collect(Collectors.toList());
            List<OWLAxiom> expected = owlList.stream().filter(axiom -> type.equals(axiom.getAxiomType())).collect(Collectors.toList());
            Assert.assertThat("Incorrect axioms for type <" + type + "> (actual=" + actual.size() + ", expected=" + expected.size() + ")", actual, IsEqual.equalTo(expected));
        });
    }

    public OntologyModel loadONT(IRI file) {
        LOGGER.info("[ONT]Load " + file);
        try {
            return (OntologyModel) OntManagerFactory.createONTManager().loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }

    }

    public OWLOntology loadOWL(IRI file) {
        LOGGER.info("[OWL]Load " + file);
        try {
            return OntManagerFactory.createOWLManager().loadOntologyFromOntologyDocument(file);
        } catch (OWLOntologyCreationException e) {
            throw new AssertionError(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Stream<OWLAxiom> axioms(OWLOntology o) {
        return o.axioms()
                .map(a -> a instanceof OWLNaryAxiom ? (Stream<OWLAxiom>) ((OWLNaryAxiom) a).splitToAnnotatedPairs().stream() : Stream.of(a))
                .flatMap(Function.identity()).distinct();
    }
}
