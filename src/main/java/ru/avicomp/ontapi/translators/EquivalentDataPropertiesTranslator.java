package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;

/**
 * example:
 * gr:description rdf:type owl:DatatypeProperty ;  owl:equivalentProperty <http://schema.org/description> ;
 * Created by @szuev on 01.10.2016.
 */
class EquivalentDataPropertiesTranslator extends AbstractNaryTranslator<OWLEquivalentDataPropertiesAxiom> {

    @Override
    Property getPredicate() {
        return OWL2.equivalentProperty;
    }
}
