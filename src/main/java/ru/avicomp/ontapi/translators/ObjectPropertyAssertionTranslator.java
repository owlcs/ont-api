package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;

/**
 * base class {@link AbstractPropertyAssertionTranslator}
 * example:
 * gr:Saturday rdf:type owl:NamedIndividual , gr:hasNext gr:Sunday ;
 * Created by @szuev on 01.10.2016.
 */
class ObjectPropertyAssertionTranslator extends AbstractPropertyAssertionTranslator<OWLObjectPropertyAssertionAxiom> {
}
