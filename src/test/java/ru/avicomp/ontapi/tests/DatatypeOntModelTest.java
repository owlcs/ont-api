package ru.avicomp.ontapi.tests;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;

import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.utils.OntIRI;
import ru.avicomp.ontapi.utils.TestUtils;

/**
 * for datatype axiom.
 * <p>
 * Created by @szuev on 18.10.2016.
 */
public class DatatypeOntModelTest extends OntModelTestBase {

    @Test
    public void testDefinitionAxiom() {
        OntIRI iri = OntIRI.create("http://test.org/datatypes/1");
        OntologyModel owl = TestUtils.createModel(iri);
        OWLOntologyManager manager = owl.getOWLOntologyManager();

        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLDatatype tp1 = factory.getOWLDatatype(iri.addFragment("data-type-1"));
        OWLDatatype tp2 = factory.getOWLDatatype(iri.addFragment("data-type-2"));
        OWLDatatype tp3 = factory.getOWLDatatype(iri.addFragment("data-type-3"));

        List<OWLAxiom> axioms = new ArrayList<>();
        axioms.add(factory.getOWLDatatypeDefinitionAxiom(tp1, OWL2Datatype.OWL_REAL.getDatatype(factory)));
        axioms.add(factory.getOWLDatatypeDefinitionAxiom(tp2, OWL2Datatype.OWL_RATIONAL));
        axioms.add(factory.getOWLDatatypeDefinitionAxiom(tp3, factory.getOWLDataUnionOf(tp1, tp2)));

        axioms.forEach(axiom -> owl.applyChanges(new AddAxiom(owl, axiom)));

        debug(owl);

        checkAxioms(owl, AxiomType.DECLARATION);
    }

    @Test
    public void testWithClassExpressions() {
        OntIRI iri = OntIRI.create("http://test.org/datatypes/2");
        OntologyModel owl = TestUtils.createModel(iri);
        OWLOntologyManager manager = owl.getOWLOntologyManager();

        OWLDataFactory factory = manager.getOWLDataFactory();
        OWLDatatype tp1 = factory.getOWLDatatype(iri.addFragment("data-type-1"));
        OWLDatatype tp2 = factory.getOWLDatatype(iri.addFragment("data-type-2"));
        OWLDatatype tp3 = factory.getOWLDatatype(iri.addFragment("data-type-3"));
        OWLDatatype tp4 = factory.getOWLDatatype(iri.addFragment("data-type-4"));
        OWLDatatype tp5 = factory.getOWLDatatype(iri.addFragment("data-type-5"));

        OWLIndividual ind1 = factory.getOWLNamedIndividual(iri.addFragment("indi-1"));
        OWLDataProperty dpe1 = factory.getOWLDataProperty(iri.addFragment("data-prop-1"));
        OWLDataProperty dpe2 = factory.getOWLDataProperty(iri.addFragment("data-prop-1"));

        // one of
        OWLDataRange dr1 = factory.getOWLDataOneOf(factory.getOWLLiteral(true), factory.getOWLLiteral("ttttt", "ttttt"));
        // restriction
        OWLDataRange dr2 = factory.getOWLDatatypeRestriction(tp1, OWLFacet.FRACTION_DIGITS, factory.getOWLLiteral("owl-facet-fraction-digits"));
        OWLDataRange dr3 = factory.getOWLDatatypeRestriction(tp2, factory.getOWLFacetRestriction(OWLFacet.MAX_INCLUSIVE, 12));
        // complement of
        OWLDataRange dr4 = factory.getOWLDataComplementOf(dr3);
        // union of
        OWLDataRange dr5 = factory.getOWLDataUnionOf(tp3);
        // intersection of
        OWLDataRange dr6 = factory.getOWLDataIntersectionOf(tp4, tp5);

        OWLClassExpression ce1 = factory.getOWLDataAllValuesFrom(dpe1, dr1);
        OWLClassExpression ce2 = factory.getOWLDataSomeValuesFrom(dpe1, dr2);
        OWLClassExpression ce3 = factory.getOWLDataHasValue(dpe1, factory.getOWLLiteral("has-val-s"));
        OWLClassExpression ce4 = factory.getOWLDataMaxCardinality(23, dpe2, dr4);
        OWLClassExpression ce5 = factory.getOWLDataExactCardinality(22222222, dpe2, dr5);

        List<OWLAxiom> axioms = new ArrayList<>();
        axioms.add(factory.getOWLClassAssertionAxiom(ce1, ind1));
        axioms.add(factory.getOWLClassAssertionAxiom(ce2, ind1));
        axioms.add(factory.getOWLClassAssertionAxiom(ce3, ind1));
        axioms.add(factory.getOWLClassAssertionAxiom(ce4, ind1));
        axioms.add(factory.getOWLClassAssertionAxiom(ce5, ind1));
        axioms.add(factory.getOWLDatatypeDefinitionAxiom(tp3, dr6));
        axioms.add(factory.getOWLDatatypeDefinitionAxiom(tp3, factory.getOWLDataUnionOf(tp1, tp2)));

        axioms.forEach(axiom -> owl.applyChanges(new AddAxiom(owl, axiom)));

        debug(owl);

        checkAxioms(owl, AxiomType.DECLARATION);
    }
}
