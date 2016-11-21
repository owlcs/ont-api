package ru.avicomp.ontapi.translators;

import org.apache.jena.graph.Graph;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;

/**
 * see {@link AbstractSubPropertyTranslator}
 * Created by @szuev on 30.09.2016.
 */
class SubAnnotationPropertyOfTranslator extends AxiomTranslator<OWLSubAnnotationPropertyOfAxiom> {
    @Override
    public void write(OWLSubAnnotationPropertyOfAxiom axiom, Graph graph) {
        TranslationHelper.processAnnotatedTriple(graph, axiom.getSubProperty(), RDFS.subPropertyOf, axiom.getSuperProperty(), axiom);
    }
}
