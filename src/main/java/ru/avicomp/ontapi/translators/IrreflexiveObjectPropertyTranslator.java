package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;

/**
 * base classes {@link AbstractSingleTripleTranslator}, {@link AbstractObjectPropertyTranslator}
 * NOTE: owl AxiomType is "IrrefexiveObjectProperty", not "IrreflexiveObjectProperty"
 * example: :ob-prop-2 rdf:type owl:ObjectProperty , owl:IrreflexiveProperty .
 * Created by @szuev on 18.10.2016.
 */
class IrreflexiveObjectPropertyTranslator extends AbstractObjectPropertyTranslator<OWLIrreflexiveObjectPropertyAxiom> {
    @Override
    public RDFNode getObject(OWLIrreflexiveObjectPropertyAxiom axiom) {
        return OWL2.IrreflexiveProperty;
    }
}
