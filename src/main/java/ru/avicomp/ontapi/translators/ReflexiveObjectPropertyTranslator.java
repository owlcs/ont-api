package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * base classes {@link AbstractPropertyTypeTranslator}
 * example:
 * :ob-prop-1 rdf:type owl:ObjectProperty, owl:ReflexiveProperty .
 * Created by @szuev on 18.10.2016.
 */
class ReflexiveObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLReflexiveObjectPropertyAxiom, OntOPE> {
    @Override
    Resource getType() {
        return OWL.ReflexiveProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    Wrap<OWLReflexiveObjectPropertyAxiom> asAxiom(OntStatement statement) {
        Wrap<OWLObjectPropertyExpression> p = ReadHelper.getObjectProperty(getSubject(statement), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, getDataFactory());
        OWLReflexiveObjectPropertyAxiom res = getDataFactory().getOWLReflexiveObjectPropertyAxiom(p.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p);
    }
}
