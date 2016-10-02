package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;

/**
 * example:
 * gr:Saturday rdf:type owl:NamedIndividual , gr:hasNext gr:Sunday ;
 * Created by @szuev on 01.10.2016.
 */
class ObjectPropertyAssertionParser extends AbstractPropertyAssertionParser<OWLObjectPropertyAssertionAxiom> {
    @Override
    public RDFNode getObject() {
        return ParseUtils.toResource(getAxiom().getObject().asOWLNamedIndividual());
    }
}
