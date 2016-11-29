package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNPA;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLNegativeDataPropertyAssertionAxiomImpl;

/**
 * example: [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :dataProp; owl:targetValue "TEST"^^xsd:string ]
 * Created by szuev on 12.10.2016.
 */
class NegativeDataPropertyAssertionTranslator extends AbstractNegativePropertyAssertionTranslator<OWLNegativeDataPropertyAssertionAxiom, OntNPA.DataAssertion> {
    @Override
    OntNPA.DataAssertion createNPA(OWLNegativeDataPropertyAssertionAxiom axiom, OntGraphModel model) {
        return OWL2RDFHelper.addDataProperty(model, axiom.getProperty())
                .addNegativeAssertion(OWL2RDFHelper.addIndividual(model, axiom.getSubject()), OWL2RDFHelper.toLiteral(axiom.getObject()));
    }

    @Override
    Class<OntNPA.DataAssertion> getView() {
        return OntNPA.DataAssertion.class;
    }

    @Override
    OWLNegativeDataPropertyAssertionAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OntNPA.DataAssertion npa = statement.getSubject().as(OntNPA.DataAssertion.class);
        OWLIndividual subject = RDF2OWLHelper.getIndividual(npa.getSource());
        OWLDataProperty property = RDF2OWLHelper.getDataProperty(npa.getProperty());
        OWLLiteral object = RDF2OWLHelper.getLiteral(npa.getTarget());
        return new OWLNegativeDataPropertyAssertionAxiomImpl(subject, property, object, annotations);
    }
}
