package ru.avicomp.ontapi.parsers;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;

import ru.avicomp.ontapi.OntException;

/**
 * Created by @szuev on 28.09.2016.
 */
class DataPropertyAssertionParser extends SingleTripletParser<OWLDataPropertyAssertionAxiom> {

    @Override
    public OWLAnnotationValue getSubject() {
        OWLIndividual individual = getAxiom().getSubject();
        if (individual.isNamed()) {
            return individual.asOWLNamedIndividual().getIRI();
        }
        throw new OntException("Unsupported");
    }

    @Override
    public IRI getPredicate() {
        return ParseUtils.toIRI(getAxiom().getProperty());
    }

    @Override
    public OWLAnnotationValue getObject() {
        return getAxiom().getObject();
    }
}
