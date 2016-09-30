package ru.avicomp.ontapi.parsers;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;

/**
 * Created by @szuev on 28.09.2016.
 */
class AnnotationAssertionParser extends SingleTripletParser<OWLAnnotationAssertionAxiom> {

    @Override
    public OWLAnnotationValue getSubject() {
        return getAxiom().getSubject().asIRI().orElse(null);
    }

    @Override
    public IRI getPredicate() {
        return getAxiom().getProperty().getIRI();
    }

    @Override
    public OWLAnnotationValue getObject() {
        return getAxiom().getValue().asLiteral().orElse(null);
    }
}
