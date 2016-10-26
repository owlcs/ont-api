package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;

/**
 * base classes {@link AbstractSingleTripleTranslator}, {@link AbstractObjectPropertyTranslator}
 * example:
 * gr:equal rdf:type owl:ObjectProperty ;  owl:inverseOf gr:equal ;  rdf:type owl:SymmetricProperty ,  owl:TransitiveProperty ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class SymmetricObjectPropertyTranslator extends AbstractObjectPropertyTranslator<OWLSymmetricObjectPropertyAxiom> {
    @Override
    public RDFNode getObject() {
        return OWL.SymmetricProperty;
    }
}
