package ru.avicomp.ontapi.translators;

import org.apache.jena.graph.Graph;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLSubPropertyAxiom;

/**
 * base class for {@link SubObjectPropertyOfTranslator} and {@link SubDataPropertyOfTranslator}
 * Example:
 * foaf:msnChatID  rdfs:subPropertyOf foaf:nick .
 * <p>
 * Created by @szuev on 30.09.2016.
 */
abstract class AbstractSubPropertyTranslator<Axiom extends OWLSubPropertyAxiom> extends AxiomTranslator<Axiom> {
    @Override
    public void process(Graph graph) {
        TranslationHelper.processAnnotatedTriple(graph, getAxiom().getSubProperty(), RDFS.subPropertyOf, getAxiom().getSuperProperty(), getAxiom());
    }
}
