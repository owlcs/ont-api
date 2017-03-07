package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * example:
 * gr:equal rdf:type owl:ObjectProperty ;  owl:inverseOf gr:equal ;  rdf:type owl:SymmetricProperty ,  owl:TransitiveProperty ;
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class SymmetricObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLSymmetricObjectPropertyAxiom, OntOPE> {
    @Override
    Resource getType() {
        return OWL.SymmetricProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    Wrap<OWLSymmetricObjectPropertyAxiom> asAxiom(OntStatement statement) {
        Wrap<OWLObjectPropertyExpression> p = ReadHelper._getObjectProperty(getSubject(statement), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLSymmetricObjectPropertyAxiom res = getDataFactory().getOWLSymmetricObjectPropertyAxiom(p.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p);
    }
}
