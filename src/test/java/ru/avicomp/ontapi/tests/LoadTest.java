package ru.avicomp.ontapi.tests;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntManagerFactory;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.configuration.OntPersonality;
import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.utils.ReadWriteUtils;
import ru.avicomp.ontapi.utils.TestUtils;

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
        String fileName = "foaf.rdf";
        OntPersonality p = OntModelConfig.getPersonality();
        if (OntModelConfig.ONT_PERSONALITY_STRICT.equals(p)) {
            IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI(fileName));
            LOGGER.info("The file " + fileIRI);
            OntologyModel ont = loadONT(fileIRI);
            OntGraphModel m = ont.asGraphModel();
            ReadWriteUtils.print(m);

            Set<Resource> illegalPunningURIs = Models.getIllegalPunnings(m);
            LOGGER.debug("There are following illegal punnins inside original graph: " + illegalPunningURIs);
            List<OntEntity> illegalPunnings = m.ontEntities().filter(illegalPunningURIs::contains).collect(Collectors.toList());
            Assert.assertTrue("Has illegal punnings: " + illegalPunnings, illegalPunnings.isEmpty());

            List<OWLAxiom> ontList = ont.axioms().sorted().collect(Collectors.toList());

            OWLOntology owl = loadOWL(fileIRI);
            Set<OWLAxiom> punningAxioms = illegalPunningURIs.stream()
                    .map(Resource::getURI).map(IRI::create)
                    .map(owl::referencingAxioms).flatMap(Function.identity()).collect(Collectors.toSet());
            LOGGER.debug("OWL Axioms to exclude from consideration: ");
            punningAxioms.forEach(LOGGER::debug);
            List<OWLAxiom> owlList = owl.axioms().filter(axiom -> !punningAxioms.contains(axiom)).sorted().collect(Collectors.toList());
            // by some mysterious reason OWL-API skips owl:equivalentProperty although it is a good axiom.
            test(owlList, ontList, Stream.of(AxiomType.DECLARATION, AxiomType.ANNOTATION_ASSERTION, AxiomType.EQUIVALENT_OBJECT_PROPERTIES).collect(Collectors.toSet()));
            return;
        }
        if (!OntModelConfig.ONT_PERSONALITY_LAX.equals(p)) {
            Assert.fail("Unsupported personality profile: " + p);
        }
        // WARNING: OWL-API works wrong with this ontology.
        // Also ontology 'foaf' is wrong in itself: there 7 entities which are DataProperty and ObjectProperty simultaneously (illegal punnings).
        // But 6 errors from OWLOntologyManagerImpl#fixIllegalPunnings! all but no 'http://xmlns.com/foaf/0.1/name' (maybe because this entity has no explicit declaration).
        // todo: investigate and add testing for excluded axioms if possible.
        test(fileName, AxiomType.DECLARATION, AxiomType.ANNOTATION_PROPERTY_RANGE, AxiomType.ANNOTATION_PROPERTY_DOMAIN,
                AxiomType.DATA_PROPERTY_DOMAIN, AxiomType.EQUIVALENT_OBJECT_PROPERTIES);
    }

    @Test
    public void testGoodrelations() {
        String fileName = "goodrelations.rdf";
        OWLDataFactory factory = OntManagerFactory.getDataFactory();

        IRI fileIRI = IRI.create(ReadWriteUtils.getResourceURI(fileName));
        LOGGER.info("The file " + fileIRI);

        OntologyModel ont = loadONT(fileIRI);
        OWLOntology owl = loadOWL(fileIRI);

        List<OWLAxiom> owlList = TestUtils.splitAxioms(owl).sorted().collect(Collectors.toList());
        List<OWLAxiom> ontList = TestUtils.splitAxioms(ont).sorted().collect(Collectors.toList());

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

        List<OWLAxiom> owlList = TestUtils.splitAxioms(owl).sorted().collect(Collectors.toList());
        List<OWLAxiom> ontList = TestUtils.splitAxioms(ont).sorted().collect(Collectors.toList());

        ReadWriteUtils.print(ont.asGraphModel());

        Set<AxiomType> excluded = Stream.of(toExclude).collect(Collectors.toSet());

        test(owlList, ontList, excluded);
    }

    private void test(List<OWLAxiom> owlList, List<OWLAxiom> ontList, Set<AxiomType> excluded) {
        AxiomType.AXIOM_TYPES.forEach(type -> {
            if (excluded.contains(type)) {
                LOGGER.warn("Skip <" + type + ">");
                return;
            }
            List<OWLAxiom> actual = ontList.stream()
                    .filter(axiom -> type.equals(axiom.getAxiomType()))
                    .collect(Collectors.toList());
            List<OWLAxiom> expected = owlList.stream().filter(axiom -> type.equals(axiom.getAxiomType())).collect(Collectors.toList());
            LOGGER.debug("Test type <" + type + ">" + " ::: " + expected.size());
            Assert.assertThat("Incorrect axioms for type <" + type + "> (actual(ont)=" + actual.size() + ", expected(owl)=" + expected.size() + ")", actual, IsEqual.equalTo(expected));
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

}
