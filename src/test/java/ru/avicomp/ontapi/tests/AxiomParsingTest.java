package ru.avicomp.ontapi.tests;

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
import ru.avicomp.ontapi.translators.OWLTripleSet;
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

        Stream.of(OWLDeclarationAxiom.class
                , OWLFunctionalDataPropertyAxiom.class
                , OWLFunctionalObjectPropertyAxiom.class
                , OWLReflexiveObjectPropertyAxiom.class
                , OWLIrreflexiveObjectPropertyAxiom.class
                , OWLAsymmetricObjectPropertyAxiom.class
                , OWLSymmetricObjectPropertyAxiom.class
                , OWLTransitiveObjectPropertyAxiom.class
                , OWLIrreflexiveObjectPropertyAxiom.class
                , OWLAnnotationAssertionAxiom.class
                , OWLClassAssertionAxiom.class
                , OWLSubClassOfAxiom.class
        ).forEach(view -> check(model, view));
    }

    private static <Axiom extends OWLAxiom> void check(OntGraphModel model, Class<Axiom> view) {
        LOGGER.debug("=========================");
        LOGGER.info(view.getSimpleName() + ":");
        Set<OWLTripleSet<Axiom>> axioms = AxiomParserProvider.get(view).read(model);
        axioms.forEach(a -> {
            Axiom axiom = a.getObject();
            Set<Triple> triples = a.getTriples();
            Assert.assertNotNull("Null axiom", axiom);
            Assert.assertTrue("No associated triples", triples != null && !triples.isEmpty());
            LOGGER.debug(axiom + " " + triples);
        });
    }
}
