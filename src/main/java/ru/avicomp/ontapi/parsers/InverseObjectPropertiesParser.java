package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;

/**
 * example:
 * pizza:hasBase owl:inverseOf pizza:isBaseOf ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class InverseObjectPropertiesParser extends SingleTripletParser<OWLInverseObjectPropertiesAxiom> {
    @Override
    public Resource getSubject() {
        return AxiomParseUtils.toResource(getAxiom().getFirstProperty());
    }

    @Override
    public Property getPredicate() {
        return OWL.inverseOf;
    }

    @Override
    public RDFNode getObject() {
        return AxiomParseUtils.toResource(getAxiom().getSecondProperty());
    }
}
