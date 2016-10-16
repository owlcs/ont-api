package ru.avicomp.ontapi.parsers;

import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;

/**
 * base class {@link AbstractPropertyAssertionParser}
 * example:
 * gr:Saturday rdf:type owl:NamedIndividual , gr:hasNext gr:Sunday ;
 * Created by @szuev on 01.10.2016.
 */
class ObjectPropertyAssertionParser extends AbstractPropertyAssertionParser<OWLObjectPropertyAssertionAxiom> {
}
