package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLPropertyAssertionAxiom;

/**
 * see {@link DataPropertyAssertionParser} and {@link ObjectPropertyAssertionParser}
 * Created by @szuev on 01.10.2016.
 */
abstract class AbstractPropertyAssertionParser<Axiom extends OWLPropertyAssertionAxiom> extends SingleTripletParser<Axiom> {
    @Override
    public Resource getSubject() {
        OWLIndividual individual = getAxiom().getSubject();
        return ParseUtils.toResource(individual);
    }

    @Override
    public Property getPredicate() {
        return ParseUtils.toProperty(getAxiom().getProperty());
    }

}
