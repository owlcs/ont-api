package ru.avicomp.ontapi.tests;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.TestUtils;

/**
 * for datatype axiom.
 * <p>
 * Created by @szuev on 18.10.2016.
 */
public class DatatypeGraphTest extends GraphTestBase {

    @Test
    public void test() {
        OntIRI iri = OntIRI.create("http://test.org/test");
        OntologyModel owl = TestUtils.createModel(iri);
        OWLOntologyManager manager = owl.getOWLOntologyManager();

        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLDatatype tp1 = factory.getOWLDatatype(iri.addFragment("data-type-1"));
        OWLDatatype tp2 = factory.getOWLDatatype(iri.addFragment("data-type-2"));
        OWLDatatype tp3 = factory.getOWLDatatype(iri.addFragment("data-type-3"));

        List<OWLAxiom> axioms = new ArrayList<>();
        axioms.add(factory.getOWLDatatypeDefinitionAxiom(tp1, OWL2Datatype.OWL_REAL.getDatatype(factory)));
        axioms.add(factory.getOWLDatatypeDefinitionAxiom(tp2, OWL2Datatype.OWL_RATIONAL));
        //todo:
        //axioms.add(factory.getOWLDatatypeDefinitionAxiom(tp3, factory.getOWLDataUnionOf(tp1, tp2)));

        axioms.forEach(axiom -> owl.applyChanges(new AddAxiom(owl, axiom)));
        debug(owl);
        // todo:
    }
}
