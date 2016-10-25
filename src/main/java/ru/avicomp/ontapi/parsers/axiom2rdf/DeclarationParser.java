package ru.avicomp.ontapi.parsers.axiom2rdf;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

/**
 * simple triplet with rdf:type predicate.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DeclarationParser extends AbstractSingleTripleParser<OWLDeclarationAxiom> {
    @Override
    public OWLEntity getSubject() {
        return getAxiom().getEntity();
    }

    @Override
    public Property getPredicate() {
        return RDF.type;
    }

    @Override
    public RDFNode getObject() {
        return TranslationHelper.getType(getSubject());
    }
}
