package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;

/**
 * see {@link AbstractEquivalentPropertiesTranslator}
 * example:
 * <http://schema.org/image> rdf:type owl:ObjectProperty ;  owl:equivalentProperty foaf:depiction .
 * Created by @szuev on 01.10.2016.
 */
class EquivalentObjectPropertiesTranslator extends AbstractEquivalentPropertiesTranslator<OWLEquivalentObjectPropertiesAxiom> {
}
