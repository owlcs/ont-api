package ru.avicomp.ontapi.internal;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * example:
 * pizza:isBaseOf rdf:type owl:ObjectProperty , owl:FunctionalProperty .
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class FunctionalObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLFunctionalObjectPropertyAxiom, OntOPE> {
    @Override
    Resource getType() {
        return OWL.FunctionalProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    Wrap<OWLFunctionalObjectPropertyAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        Wrap<? extends OWLObjectPropertyExpression> p = ReadHelper.getObjectProperty(getSubject(statement), df);
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLFunctionalObjectPropertyAxiom res = df.getOWLFunctionalObjectPropertyAxiom(p.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p);
    }
}
