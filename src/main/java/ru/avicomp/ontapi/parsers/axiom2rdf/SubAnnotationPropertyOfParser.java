package ru.avicomp.ontapi.parsers.axiom2rdf;

import org.apache.jena.graph.Graph;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;

/**
 * see {@link AbstractSubPropertyParser}
 * Created by @szuev on 30.09.2016.
 */
class SubAnnotationPropertyOfParser extends AxiomParser<OWLSubAnnotationPropertyOfAxiom> {
    @Override
    public void process(Graph graph) {
        TranslationHelper.processAnnotatedTriple(graph, getAxiom().getSubProperty(), RDFS.subPropertyOf, getAxiom().getSuperProperty(), getAxiom());
    }
}
