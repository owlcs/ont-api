package ru.avicomp.ontapi.internal;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;

import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * see {@link AbstractSubPropertyTranslator}
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class SubDataPropertyOfTranslator extends AbstractSubPropertyTranslator<OWLSubDataPropertyOfAxiom, OntNDP> {
    @Override
    OWLPropertyExpression getSubProperty(OWLSubDataPropertyOfAxiom axiom) {
        return axiom.getSubProperty();
    }

    @Override
    OWLPropertyExpression getSuperProperty(OWLSubDataPropertyOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    public InternalObject<OWLSubDataPropertyOfAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<OWLDataProperty> sub = ReadHelper.fetchDataProperty(statement.getSubject().as(OntNDP.class), conf.dataFactory());
        InternalObject<OWLDataProperty> sup = ReadHelper.fetchDataProperty(statement.getObject().as(OntNDP.class), conf.dataFactory());
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLSubDataPropertyOfAxiom res = conf.dataFactory().getOWLSubDataPropertyOfAxiom(sub.getObject(), sup.getObject(), annotations.getObjects());
        return InternalObject.create(res, statement).add(annotations.getTriples()).append(sub).append(sup);
    }
}
