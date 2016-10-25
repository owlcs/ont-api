package ru.avicomp.ontapi.parsers.axiom2rdf;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;

/**
 * see {@link AbstractNegativePropertyAssertionParser}
 * example: [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :dataProp; owl:targetValue "TEST"^^xsd:string ]
 * Created by szuev on 12.10.2016.
 */
class NegativeDataPropertyAssertionParser extends AbstractNegativePropertyAssertionParser<OWLNegativeDataPropertyAssertionAxiom> {
    @Override
    public Property getTargetPredicate() {
        return OWL2.targetValue;
    }
}
