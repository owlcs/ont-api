package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

import static ru.avicomp.ontapi.translators.TranslationHelper.*;

/**
 * example: [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :dataProp; owl:targetValue "TEST"^^xsd:string ]
 * Created by szuev on 12.10.2016.
 */
class NegativeDataPropertyAssertionTranslator extends AxiomTranslator<OWLNegativeDataPropertyAssertionAxiom> {
    @Override
    public void write(OWLNegativeDataPropertyAssertionAxiom axiom, OntGraphModel model) {
        addAnnotations(addDataProperty(model, axiom.getProperty())
                .addNegativeAssertion(addIndividual(model, axiom.getSubject()), toLiteral(axiom.getObject())), axiom.annotations());
    }
}
