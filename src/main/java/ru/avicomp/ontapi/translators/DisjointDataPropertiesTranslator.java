package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;

/**
 * see {@link AbstractTwoWayNaryTranslator}
 * examples:
 * :objProperty1 owl:propertyDisjointWith :objProperty2
 * [ rdf:type owl:AllDisjointProperties; owl:members ( :objProperty1 :objProperty2 :objProperty3 ) ]
 * <p>
 * Created by szuev on 12.10.2016.
 */
class DisjointDataPropertiesTranslator extends AbstractTwoWayNaryTranslator<OWLDisjointDataPropertiesAxiom> {
    @Override
    public Property getPredicate() {
        return OWL2.propertyDisjointWith;
    }

    @Override
    public Resource getMembersType() {
        return OWL2.AllDisjointProperties;
    }

    @Override
    public Property getMembersPredicate() {
        return OWL2.members;
    }
}
