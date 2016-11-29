package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;

/**
 * base class {@link AbstractNaryTranslator}
 * example:
 * :indi1 owl:sameAs :indi2, :indi3 .
 * <p>
 * Created by szuev on 13.10.2016.
 */
class SameIndividualTranslator extends AbstractNaryTranslator<OWLSameIndividualAxiom> {
    @Override
    public Property getPredicate() {
        return OWL2.sameAs;
    }
}
