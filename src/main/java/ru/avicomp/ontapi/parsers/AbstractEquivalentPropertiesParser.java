package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLNaryPropertyAxiom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

/**
 * see {@link AbstractNaryParser}
 * base for {@link EquivalentObjectPropertiesParser} and {@link EquivalentDataPropertiesParser}
 *
 * Created by @szuev on 01.10.2016.
 */
abstract class AbstractEquivalentPropertiesParser<Axiom extends OWLNaryPropertyAxiom<? extends OWLPropertyExpression>> extends AbstractNaryParser<Axiom> {

    @Override
    public Property getPredicate() {
        return OWL.equivalentProperty;
    }

}
