package ru.avicomp.ontapi.translators;

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * see {@link AbstractSubPropertyTranslator}
 * Created by @szuev on 30.09.2016.
 */
class SubAnnotationPropertyOfTranslator extends AxiomTranslator<OWLSubAnnotationPropertyOfAxiom> {
    @Override
    public void write(OWLSubAnnotationPropertyOfAxiom axiom, OntGraphModel model) {
        TranslationHelper.processAnnotatedTriple(model, axiom.getSubProperty(), RDFS.subPropertyOf, axiom.getSuperProperty(), axiom);
    }
}
