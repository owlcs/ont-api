package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;

/**
 * Example:
 * gr:equal rdf:type owl:TransitiveProperty ;
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class TransitiveObjectPropertyParser extends AbstractSingleTripleParser<OWLTransitiveObjectPropertyAxiom> {
    @Override
    public Resource getSubject() {
        return AxiomParseUtils.toResource(getAxiom().getProperty());
    }

    @Override
    public Property getPredicate() {
        return RDF.type;
    }

    @Override
    public RDFNode getObject() {
        return OWL.TransitiveProperty;
    }
}
