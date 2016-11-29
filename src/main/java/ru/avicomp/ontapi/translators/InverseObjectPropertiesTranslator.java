package ru.avicomp.ontapi.translators;

import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * example:
 * pizza:hasBase owl:inverseOf pizza:isBaseOf ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class InverseObjectPropertiesTranslator extends AxiomTranslator<OWLInverseObjectPropertiesAxiom> {
    @Override
    public void write(OWLInverseObjectPropertiesAxiom axiom, OntGraphModel model) {
        OWL2RDFHelper.writeTriple(model, axiom.getFirstProperty(), OWL2.inverseOf, axiom.getSecondProperty(), axiom.annotations());
    }
}
