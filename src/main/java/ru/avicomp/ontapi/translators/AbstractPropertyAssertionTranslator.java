package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.OWLPropertyAssertionAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * see {@link DataPropertyAssertionTranslator} and {@link ObjectPropertyAssertionTranslator}
 * Created by @szuev on 01.10.2016.
 */
abstract class AbstractPropertyAssertionTranslator<Axiom extends OWLPropertyAssertionAxiom> extends AxiomTranslator<Axiom> {
    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        OWL2RDFHelper.writeTriple(model, axiom.getSubject(), axiom.getProperty(), axiom.getObject(), axiom.annotations());
    }
}
