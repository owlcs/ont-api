package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;

/**
 * example:
 * <http://schema.org/image> rdf:type owl:ObjectProperty ;  owl:equivalentProperty foaf:depiction .
 * Created by @szuev on 01.10.2016.
 */
class EquivalentObjectPropertiesTranslator extends AbstractNaryTranslator<OWLEquivalentObjectPropertiesAxiom> {

    @Override
    Property getPredicate() {
        return OWL2.equivalentProperty;
    }
}
