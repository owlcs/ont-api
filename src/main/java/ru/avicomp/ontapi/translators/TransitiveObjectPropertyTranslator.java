package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;

/**
 * base classes {@link AbstractSingleTripleTranslator}, {@link AbstractObjectPropertyTranslator}
 * Example:
 * gr:equal rdf:type owl:TransitiveProperty ;
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class TransitiveObjectPropertyTranslator extends AbstractObjectPropertyTranslator<OWLTransitiveObjectPropertyAxiom> {
    @Override
    public RDFNode getObject() {
        return OWL.TransitiveProperty;
    }
}
