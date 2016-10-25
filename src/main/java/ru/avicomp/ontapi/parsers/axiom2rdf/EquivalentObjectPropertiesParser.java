package ru.avicomp.ontapi.parsers.axiom2rdf;

import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;

/**
 * see {@link AbstractEquivalentPropertiesParser}
 * example:
 * <http://schema.org/image> rdf:type owl:ObjectProperty ;  owl:equivalentProperty foaf:depiction .
 * Created by @szuev on 01.10.2016.
 */
class EquivalentObjectPropertiesParser extends AbstractEquivalentPropertiesParser<OWLEquivalentObjectPropertiesAxiom> {
}
