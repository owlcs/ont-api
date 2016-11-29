package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;

/**
 * see {@link AbstractTwoWayNaryTranslator}
 * examples:
 * :dataProperty1 owl:propertyDisjointWith :dataProperty2
 * [ rdf:type owl:AllDisjointProperties; owl:members ( :dataProperty1 :dataProperty2 :dataProperty3 ) ]
 * <p>
 * Created by szuev on 12.10.2016.
 */
class DisjointObjectPropertiesTranslator extends AbstractTwoWayNaryTranslator<OWLDisjointObjectPropertiesAxiom> {
    @Override
    Property getPredicate() {
        return OWL2.propertyDisjointWith;
    }

    @Override
    Resource getMembersType() {
        return OWL2.AllDisjointProperties;
    }

    @Override
    Property getMembersPredicate() {
        return OWL2.members;
    }
}
