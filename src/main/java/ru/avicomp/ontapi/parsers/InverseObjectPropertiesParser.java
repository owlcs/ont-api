package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;

/**
 * example:
 * pizza:hasBase owl:inverseOf pizza:isBaseOf ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class InverseObjectPropertiesParser extends AxiomParser<OWLInverseObjectPropertiesAxiom> {
    @Override
    public void process(Graph graph) {
        AxiomParseUtils.processAnnotatedTriple(graph, getAxiom().getFirstProperty(), OWL.inverseOf, getAxiom().getSecondProperty(), getAxiom());
    }
}
