package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

/**
 * base class for {@link FunctionalObjectPropertyTranslator} and {@link FunctionalDataPropertyTranslator}
 * <p>
 * Created by @szuev on 30.09.2016.
 */
abstract class AbstractFunctionalPropertyTranslator<Axiom extends OWLAxiom & HasProperty> extends AbstractSingleTripleTranslator<Axiom> {
    @Override
    public OWLObject getSubject() {
        return getAxiom().getProperty();
    }

    @Override
    public Property getPredicate() {
        return RDF.type;
    }

    @Override
    public RDFNode getObject() {
        return OWL.FunctionalProperty;
    }
}
