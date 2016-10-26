package ru.avicomp.ontapi.translators;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * base class : {@link AbstractSubChainedTranslator}
 * for HasKey axiom.
 * example:
 * :MyClass1 owl:hasKey ( :ob-prop-1 ) .
 * <p>
 * Created by @szuev on 17.10.2016.
 */
class HasKeyTranslator extends AbstractSubChainedTranslator<OWLHasKeyAxiom> {
    @Override
    public OWLObject getSubject() {
        return getAxiom().getClassExpression();
    }

    @Override
    public Property getPredicate() {
        return OWL2.hasKey;
    }

    @Override
    public Stream<? extends OWLObject> getObjects() {
        return getAxiom().propertyExpressions();
    }
}
