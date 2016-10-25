package ru.avicomp.ontapi.parsers.axiom2rdf;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;

/**
 * base class : {@link AbstractSubChainedParser}
 * for SubPropertyChainOf axiom
 * example: owl:topObjectProperty owl:propertyChainAxiom ( :ob-prop-1 :ob-prop-2 ) .
 * <p>
 * Created by @szuev on 18.10.2016.
 */
class SubPropertyChainOfParser extends AbstractSubChainedParser<OWLSubPropertyChainOfAxiom> {
    @Override
    public OWLObject getSubject() {
        return getAxiom().getSuperProperty();
    }

    @Override
    public Property getPredicate() {
        return OWL2.propertyChainAxiom;
    }

    @Override
    public Stream<? extends OWLObject> getObjects() {
        return getAxiom().getPropertyChain().stream();
    }
}
