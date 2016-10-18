package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL2;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;

/**
 * base classes {@link AbstractSingleTripleParser}, {@link AbstractObjectPropertyParser}
 * NOTE: owl AxiomType is "IrrefexiveObjectProperty", not "IrreflexiveObjectProperty"
 * example: :ob-prop-2 rdf:type owl:ObjectProperty , owl:IrreflexiveProperty .
 * Created by @szuev on 18.10.2016.
 */
class IrreflexiveObjectPropertyParser extends AbstractObjectPropertyParser<OWLIrreflexiveObjectPropertyAxiom> {
    @Override
    public RDFNode getObject() {
        return OWL2.IrreflexiveProperty;
    }
}
