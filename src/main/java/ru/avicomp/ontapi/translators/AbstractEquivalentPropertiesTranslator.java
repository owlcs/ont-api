package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLNaryPropertyAxiom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

/**
 * see {@link AbstractNaryTranslator}
 * base for {@link EquivalentObjectPropertiesTranslator} and {@link EquivalentDataPropertiesTranslator}
 *
 * Created by @szuev on 01.10.2016.
 */
abstract class AbstractEquivalentPropertiesTranslator<Axiom extends OWLNaryPropertyAxiom<? extends OWLPropertyExpression>> extends AbstractNaryTranslator<Axiom> {
    @Override
    public Property getPredicate() {
        return OWL2.equivalentProperty;
    }
}
