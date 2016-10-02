package ru.avicomp.ontapi.parsers;

import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;

/**
 * see {@link AbstractEquivalentPropertiesAxiom}
 * example:
 * <http://schema.org/image> rdf:type owl:ObjectProperty ;  owl:equivalentProperty foaf:depiction .
 * Created by @szuev on 01.10.2016.
 */
class EquivalentObjectPropertiesParser extends AbstractEquivalentPropertiesAxiom<OWLEquivalentObjectPropertiesAxiom> {
}
