package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

/**
 * base class for following parsers:
 * {@link ReflexiveObjectPropertyTranslator},
 * {@link IrreflexiveObjectPropertyTranslator},
 * {@link AsymmetricObjectPropertyTranslator},
 * {@link SymmetricObjectPropertyTranslator},
 * {@link TransitiveObjectPropertyTranslator},
 * {@link InverseFunctionalObjectPropertyTranslator},
 * Created by @szuev on 18.10.2016.
 */
abstract class AbstractObjectPropertyTranslator<Axiom extends OWLAxiom & HasProperty<? extends OWLObjectPropertyExpression>> extends AbstractSingleTripleTranslator<Axiom> {
    @Override
    public OWLObjectPropertyExpression getSubject(Axiom axiom) {
        return axiom.getProperty();
    }

    @Override
    public Property getPredicate() {
        return RDF.type;
    }
}
