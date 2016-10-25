package ru.avicomp.ontapi.parsers.axiom2rdf;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;

/**
 * base classes {@link AbstractSingleTripleParser}, {@link AbstractObjectPropertyParser}
 * example:
 * :ob-prop-1 rdf:type owl:ObjectProperty, owl:ReflexiveProperty .
 * Created by @szuev on 18.10.2016.
 */
class ReflexiveObjectPropertyParser extends AbstractObjectPropertyParser<OWLReflexiveObjectPropertyAxiom> {
    @Override
    public RDFNode getObject() {
        return OWL2.ReflexiveProperty;
    }
}
