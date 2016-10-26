package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;

/**
 * Note! it is for an owl-expression without any root!
 * see {@link AbstractTwoWayNaryTranslator}
 * Example:
 * [ a owl:AllDifferent; owl:distinctMembers (pizza:America pizza:Italy pizza:Germany pizza:England pizza:France) ].
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class DifferentIndividualsTranslator extends AbstractTwoWayNaryTranslator<OWLDifferentIndividualsAxiom> {
    @Override
    public Property getPredicate() {
        return OWL.differentFrom;
    }

    @Override
    public Resource getMembersType() {
        return OWL.AllDifferent;
    }

    @Override
    public Property getMembersPredicate() {
        return OWL.distinctMembers;
    }
}
