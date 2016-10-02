package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLSubPropertyAxiom;

/**
 * base class for {@link SubObjectPropertyOfParser} and {@link SubDataPropertyOfParser}
 * Example:
 * foaf:msnChatID  rdfs:subPropertyOf foaf:nick .
 * <p>
 * Created by @szuev on 30.09.2016.
 */
abstract class AbstractSubPropertyParser<Axiom extends OWLSubPropertyAxiom> extends SingleTripletParser<Axiom> {
    @Override
    public Resource getSubject() {
        return ParseUtils.toResource(getAxiom().getSubProperty());
    }

    @Override
    public Property getPredicate() {
        return RDFS.subPropertyOf;
    }

    @Override
    public RDFNode getObject() {
        return ParseUtils.toResource(getAxiom().getSuperProperty());
    }
}
