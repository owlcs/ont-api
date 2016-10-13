package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLNaryPropertyAxiom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

/**
 * base class {@link AbstractTwoWayNaryParser}
 * for following axioms with two or more than two entities:
 * DisjointObjectProperties ({@link DisjointObjectPropertiesParser}),
 * DisjointDataProperties ({@link DisjointDataPropertiesParser}),
 * <p>
 * Created by szuev on 12.10.2016.
 */
abstract class AbstractDisjointPropertiesParser<Axiom extends OWLNaryPropertyAxiom<? extends OWLPropertyExpression>> extends AbstractTwoWayNaryParser<Axiom> {
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
