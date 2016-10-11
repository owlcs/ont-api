package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;

/**
 * see {@link AbstractSubPropertyParser}
 * Created by @szuev on 30.09.2016.
 */
public class SubAnnotationPropertyOfParser extends SingleTripletParser<OWLSubAnnotationPropertyOfAxiom> {
    @Override
    public Resource getSubject() {
        return AxiomParseUtils.toResource(getAxiom().getSubProperty());
    }

    @Override
    public Property getPredicate() {
        return RDFS.subPropertyOf;
    }

    @Override
    public RDFNode getObject() {
        return AxiomParseUtils.toResource(getAxiom().getSuperProperty());
    }
}
