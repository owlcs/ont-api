package ru.avicomp.ontapi.translators;

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
class DeclarationTranslator extends AbstractSingleTripleTranslator<OWLDeclarationAxiom> {
    @Override
    public OWLEntity getSubject(OWLDeclarationAxiom axiom) {
        return axiom.getEntity();
    }

    @Override
    public Property getPredicate() {
        return RDF.type;
    }

    @Override
    public RDFNode getObject(OWLDeclarationAxiom axiom) {
        return TranslationHelper.getType(getSubject(axiom));
    }
}
