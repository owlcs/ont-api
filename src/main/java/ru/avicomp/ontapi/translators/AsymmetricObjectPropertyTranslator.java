package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;

/**
 * base classes {@link AbstractSingleTripleTranslator}, {@link AbstractObjectPropertyTranslator}
 * Created by @szuev on 18.10.2016.
 */
class AsymmetricObjectPropertyTranslator extends AbstractObjectPropertyTranslator<OWLAsymmetricObjectPropertyAxiom> {
    @Override
    public RDFNode getObject(OWLAsymmetricObjectPropertyAxiom axiom) {
        return OWL2.AsymmetricProperty;
    }
}
