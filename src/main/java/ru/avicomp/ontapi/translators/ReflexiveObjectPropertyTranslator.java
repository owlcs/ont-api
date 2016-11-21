package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;

/**
 * base classes {@link AbstractSingleTripleTranslator}, {@link AbstractObjectPropertyTranslator}
 * example:
 * :ob-prop-1 rdf:type owl:ObjectProperty, owl:ReflexiveProperty .
 * Created by @szuev on 18.10.2016.
 */
class ReflexiveObjectPropertyTranslator extends AbstractObjectPropertyTranslator<OWLReflexiveObjectPropertyAxiom> {
    @Override
    public RDFNode getObject(OWLReflexiveObjectPropertyAxiom axiom) {
        return OWL2.ReflexiveProperty;
    }
}
