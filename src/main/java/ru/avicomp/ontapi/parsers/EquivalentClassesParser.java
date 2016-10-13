package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;

/**
 * Base class {@link AbstractNaryParser}
 * Example of ttl:
 * pizza:SpicyTopping owl:equivalentClass [ a owl:Class; owl:intersectionOf ( pizza:PizzaTopping [a owl:Restriction; owl:onProperty pizza:hasSpiciness; owl:someValuesFrom pizza:Hot] )] ;
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class EquivalentClassesParser extends AbstractNaryParser<OWLEquivalentClassesAxiom> {

    @Override
    public Property getPredicate() {
        return OWL.equivalentClass;
    }
}
