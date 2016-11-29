package ru.avicomp.ontapi.tests;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.log4j.Logger;
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
        // 23(39):
        Stream.of(OWLDeclarationAxiom.class
                , OWLFunctionalDataPropertyAxiom.class
                , OWLFunctionalObjectPropertyAxiom.class
                , OWLReflexiveObjectPropertyAxiom.class
                , OWLIrreflexiveObjectPropertyAxiom.class
                , OWLAsymmetricObjectPropertyAxiom.class
                , OWLSymmetricObjectPropertyAxiom.class
                , OWLTransitiveObjectPropertyAxiom.class
                , OWLIrreflexiveObjectPropertyAxiom.class
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

                , SWRLRule.class
        ).forEach(view -> check(model, view));
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
