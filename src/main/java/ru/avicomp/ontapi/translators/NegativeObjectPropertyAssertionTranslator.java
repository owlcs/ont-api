package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

import static ru.avicomp.ontapi.translators.TranslationHelper.*;

/**
 * example:
 * [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :objProp; owl:targetIndividual :ind2 ] .
 * Created by szuev on 12.10.2016.
 */
class NegativeObjectPropertyAssertionTranslator extends AxiomTranslator<OWLNegativeObjectPropertyAssertionAxiom> {
    @Override
    public void write(OWLNegativeObjectPropertyAssertionAxiom axiom, OntGraphModel model) {
        addAnnotations(addObjectProperty(model, axiom.getProperty())
                .addNegativeAssertion(addIndividual(model, axiom.getSubject()), addIndividual(model, axiom.getObject())), axiom.annotations());
    }

}
