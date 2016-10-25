package ru.avicomp.ontapi.parsers.axiom2rdf;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;

/**
 * base class {@link AbstractNaryParser}
 * example:
 * :indi1 owl:sameAs :indi2, :indi3 .
 * <p>
 * Created by szuev on 13.10.2016.
 */
class SameIndividualParser extends AbstractNaryParser<OWLSameIndividualAxiom> {
    @Override
    public Property getPredicate() {
        return OWL.sameAs;
    }
}
