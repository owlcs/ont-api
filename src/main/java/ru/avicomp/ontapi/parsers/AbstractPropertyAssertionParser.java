package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.OWLPropertyAssertionAxiom;

/**
 * see {@link DataPropertyAssertionParser} and {@link ObjectPropertyAssertionParser}
 * Created by @szuev on 01.10.2016.
 */
abstract class AbstractPropertyAssertionParser<Axiom extends OWLPropertyAssertionAxiom> extends AxiomParser<Axiom> {
    @Override
    public void process(Graph graph) {
        AxiomParseUtils.processAnnotatedTriple(graph, getAxiom().getSubject(), getAxiom().getProperty(), getAxiom().getObject(), getAxiom());
    }
}
