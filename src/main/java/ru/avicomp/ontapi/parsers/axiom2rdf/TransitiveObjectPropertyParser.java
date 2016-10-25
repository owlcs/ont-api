package ru.avicomp.ontapi.parsers.axiom2rdf;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;

/**
 * base classes {@link AbstractSingleTripleParser}, {@link AbstractObjectPropertyParser}
 * Example:
 * gr:equal rdf:type owl:TransitiveProperty ;
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class TransitiveObjectPropertyParser extends AbstractObjectPropertyParser<OWLTransitiveObjectPropertyAxiom> {
    @Override
    public RDFNode getObject() {
        return OWL.TransitiveProperty;
    }
}
