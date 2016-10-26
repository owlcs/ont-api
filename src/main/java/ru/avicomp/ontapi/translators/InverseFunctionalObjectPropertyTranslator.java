package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;

/**
 * base classes {@link AbstractSingleTripleTranslator}, {@link AbstractObjectPropertyTranslator}
 * example:
 * pizza:hasBase rdf:type owl:FunctionalProperty
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class InverseFunctionalObjectPropertyTranslator extends AbstractObjectPropertyTranslator<OWLInverseFunctionalObjectPropertyAxiom> {
    @Override
    public RDFNode getObject() {
        return OWL.InverseFunctionalProperty;
    }
}
