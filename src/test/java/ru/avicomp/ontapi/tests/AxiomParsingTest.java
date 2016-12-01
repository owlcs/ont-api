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
import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.*;

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
    public void simple() {
        Model m = ReadWriteUtils.loadFromTTL("pizza.ttl");
        OntGraphModel model = new OntGraphModelImpl(m.getGraph());
        // 39 axiom types:
        Set<Class<? extends OWLAxiom>> types = Stream.of(
                OWLDeclarationAxiom.class
                , OWLFunctionalDataPropertyAxiom.class
                , OWLFunctionalObjectPropertyAxiom.class
                , OWLReflexiveObjectPropertyAxiom.class
                , OWLIrreflexiveObjectPropertyAxiom.class
                , OWLAsymmetricObjectPropertyAxiom.class
                , OWLSymmetricObjectPropertyAxiom.class
                , OWLTransitiveObjectPropertyAxiom.class
                , OWLInverseFunctionalObjectPropertyAxiom.class

                , OWLSubClassOfAxiom.class

                , OWLObjectPropertyDomainAxiom.class
                , OWLDataPropertyDomainAxiom.class
                , OWLAnnotationPropertyDomainAxiom.class

                , OWLObjectPropertyRangeAxiom.class
                , OWLDataPropertyRangeAxiom.class
                , OWLAnnotationPropertyRangeAxiom.class

                , OWLClassAssertionAxiom.class
                , OWLAnnotationAssertionAxiom.class
                , OWLObjectPropertyAssertionAxiom.class
                , OWLDataPropertyAssertionAxiom.class

                , OWLHasKeyAxiom.class
                , OWLDisjointUnionAxiom.class
                , OWLSubPropertyChainOfAxiom.class

                , OWLSubAnnotationPropertyOfAxiom.class
                , OWLSubObjectPropertyOfAxiom.class
                , OWLSubDataPropertyOfAxiom.class

                , OWLDatatypeDefinitionAxiom.class

                , OWLNegativeDataPropertyAssertionAxiom.class
                , OWLNegativeObjectPropertyAssertionAxiom.class

                , OWLInverseObjectPropertiesAxiom.class

                , OWLEquivalentClassesAxiom.class
                , OWLEquivalentObjectPropertiesAxiom.class
                , OWLEquivalentDataPropertiesAxiom.class
                , OWLSameIndividualAxiom.class

                , OWLDisjointClassesAxiom.class
                , OWLDisjointObjectPropertiesAxiom.class
                , OWLDisjointDataPropertiesAxiom.class
                , OWLDifferentIndividualsAxiom.class

                , SWRLRule.class
        ).collect(Collectors.toSet());

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

    private static void print(OWLAxiom axiom, Set<Triple> triples) {
        Assert.assertNotNull("Null axiom", axiom);
        Assert.assertTrue("No associated triples", triples != null && !triples.isEmpty());
        LOGGER.debug(axiom + " " + triples);
    }
}
