package ru.avicomp.ontapi.internal;

import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * example:
 * pizza:hasBase rdf:type owl:FunctionalProperty
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class InverseFunctionalObjectPropertyTranslator extends AbstractPropertyTypeTranslator<OWLInverseFunctionalObjectPropertyAxiom, OntOPE> {
    @Override
    Resource getType() {
        return OWL.InverseFunctionalProperty;
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    public InternalObject<OWLInverseFunctionalObjectPropertyAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<? extends OWLObjectPropertyExpression> p = ReadHelper.fetchObjectPropertyExpression(getSubject(statement), conf.dataFactory());
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLInverseFunctionalObjectPropertyAxiom res = conf.dataFactory().getOWLInverseFunctionalObjectPropertyAxiom(p.getObject(), annotations.getObjects());
        return InternalObject.create(res, statement).add(annotations.getTriples()).append(p);
    }
}
