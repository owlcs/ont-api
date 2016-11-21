package ru.avicomp.ontapi.translators;

import org.apache.jena.graph.Graph;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;

/**
 * example:
 * pizza:hasBase owl:inverseOf pizza:isBaseOf ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class InverseObjectPropertiesTranslator extends AxiomTranslator<OWLInverseObjectPropertiesAxiom> {
    @Override
    public void write(OWLInverseObjectPropertiesAxiom axiom, Graph graph) {
        TranslationHelper.processAnnotatedTriple(graph, axiom.getFirstProperty(), OWL.inverseOf, axiom.getSecondProperty(), axiom);
    }
}
