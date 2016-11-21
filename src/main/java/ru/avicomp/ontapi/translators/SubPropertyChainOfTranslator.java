package ru.avicomp.ontapi.translators;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;

/**
 * base class : {@link AbstractSubChainedTranslator}
 * for SubPropertyChainOf axiom
 * example: owl:topObjectProperty owl:propertyChainAxiom ( :ob-prop-1 :ob-prop-2 ) .
 * <p>
 * Created by @szuev on 18.10.2016.
 */
class SubPropertyChainOfTranslator extends AbstractSubChainedTranslator<OWLSubPropertyChainOfAxiom> {
    @Override
    public OWLObject getSubject(OWLSubPropertyChainOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    public Property getPredicate() {
        return OWL2.propertyChainAxiom;
    }

    @Override
    public Stream<? extends OWLObject> getObjects(OWLSubPropertyChainOfAxiom axiom) {
        return axiom.getPropertyChain().stream();
    }
}
