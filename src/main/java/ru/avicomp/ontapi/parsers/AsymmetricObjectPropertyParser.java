package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;

/**
 * base classes {@link AbstractSingleTripleParser}, {@link AbstractObjectPropertyParser}
 * Created by @szuev on 18.10.2016.
 */
class AsymmetricObjectPropertyParser extends AbstractObjectPropertyParser<OWLAsymmetricObjectPropertyAxiom> {
    @Override
    public RDFNode getObject() {
        return OWL2.AsymmetricProperty;
    }
}
