package ru.avicomp.ontapi.parsers;

import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;

/**
 * see {@link AbstractEquivalentPropertiesParser}
 * example:
 * gr:description rdf:type owl:DatatypeProperty ;  owl:equivalentProperty <http://schema.org/description> ;
 * Created by @szuev on 01.10.2016.
 */
class EquivalentDataPropertiesParser extends AbstractEquivalentPropertiesParser<OWLEquivalentDataPropertiesAxiom> {
}
