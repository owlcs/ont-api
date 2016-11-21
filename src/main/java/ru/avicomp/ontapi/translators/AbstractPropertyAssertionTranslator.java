package ru.avicomp.ontapi.translators;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.OWLPropertyAssertionAxiom;

/**
 * see {@link DataPropertyAssertionTranslator} and {@link ObjectPropertyAssertionTranslator}
 * Created by @szuev on 01.10.2016.
 */
abstract class AbstractPropertyAssertionTranslator<Axiom extends OWLPropertyAssertionAxiom> extends AxiomTranslator<Axiom> {
    @Override
    public void write(Axiom axiom, Graph graph) {
        TranslationHelper.processAnnotatedTriple(graph, axiom.getSubject(), axiom.getProperty(), axiom.getObject(), axiom);
    }
}
