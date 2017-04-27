package ru.avicomp.ontapi.internal;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
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
public class FunctionalObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLFunctionalObjectPropertyAxiom, OntOPE> {
    @Override
    Resource getType() {
        return OWL.FunctionalProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    public InternalObject<OWLFunctionalObjectPropertyAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<? extends OWLObjectPropertyExpression> p = ReadHelper.fetchObjectPropertyExpression(getSubject(statement), conf.dataFactory());
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLFunctionalObjectPropertyAxiom res = conf.dataFactory().getOWLFunctionalObjectPropertyAxiom(p.getObject(), annotations.getObjects());
        return InternalObject.create(res, statement).add(annotations.getTriples()).append(p);
    }
}
