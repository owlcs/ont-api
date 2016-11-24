package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * Examples:
 * foaf:LabelProperty vs:term_status "unstable" .
 * foaf:LabelProperty rdfs:isDefinedBy <http://xmlns.com/foaf/0.1/> .
 * pizza:UnclosedPizza rdfs:label "PizzaAberta"@pt .
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class AnnotationAssertionTranslator extends AxiomTranslator<OWLAnnotationAssertionAxiom> {
    @Override
    public void write(OWLAnnotationAssertionAxiom axiom, OntGraphModel model) {
        TranslationHelper.writeTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getValue(), axiom);
    }
}
