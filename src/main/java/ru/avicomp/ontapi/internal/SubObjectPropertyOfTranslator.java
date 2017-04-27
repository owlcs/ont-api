package ru.avicomp.ontapi.internal;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;

import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * see {@link AbstractSubPropertyTranslator}
 * <p>
 * Created by @szuev on 29.09.2016.
 */
public class SubObjectPropertyOfTranslator extends AbstractSubPropertyTranslator<OWLSubObjectPropertyOfAxiom, OntOPE> {
    @Override
    OWLPropertyExpression getSubProperty(OWLSubObjectPropertyOfAxiom axiom) {
        return axiom.getSubProperty();
    }

    @Override
    OWLPropertyExpression getSuperProperty(OWLSubObjectPropertyOfAxiom axiom) {
        return axiom.getSuperProperty();
    }

    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    public InternalObject<OWLSubObjectPropertyOfAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<? extends OWLObjectPropertyExpression> sub = ReadHelper.fetchObjectPropertyExpression(statement.getSubject().as(OntOPE.class), conf.dataFactory());
        InternalObject<? extends OWLObjectPropertyExpression> sup = ReadHelper.fetchObjectPropertyExpression(statement.getObject().as(OntOPE.class), conf.dataFactory());
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLSubObjectPropertyOfAxiom res = conf.dataFactory().getOWLSubObjectPropertyOfAxiom(sub.getObject(), sup.getObject(), annotations.getObjects());
        return InternalObject.create(res, statement).add(annotations.getTriples()).append(sub).append(sup);
    }
}
