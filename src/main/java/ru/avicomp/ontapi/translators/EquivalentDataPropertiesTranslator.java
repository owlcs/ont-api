package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;

/**
 * see {@link AbstractEquivalentPropertiesTranslator}
 * example:
 * gr:description rdf:type owl:DatatypeProperty ;  owl:equivalentProperty <http://schema.org/description> ;
 * Created by @szuev on 01.10.2016.
 */
class EquivalentDataPropertiesTranslator extends AbstractEquivalentPropertiesTranslator<OWLEquivalentDataPropertiesAxiom> {
}
