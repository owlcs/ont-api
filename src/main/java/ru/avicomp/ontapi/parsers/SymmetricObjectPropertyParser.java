package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;

/**
 * example:
 * gr:equal rdf:type owl:ObjectProperty ;  owl:inverseOf gr:equal ;  rdf:type owl:SymmetricProperty ,  owl:TransitiveProperty ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class SymmetricObjectPropertyParser extends AbstractSingleTripleParser<OWLSymmetricObjectPropertyAxiom> {
    @Override
    public Resource getSubject() {
        return TranslationHelper.toResource(getAxiom().getProperty());
    }

    @Override
    public Property getPredicate() {
        return RDF.type;
    }

    @Override
    public RDFNode getObject() {
        return OWL.SymmetricProperty;
    }
}
