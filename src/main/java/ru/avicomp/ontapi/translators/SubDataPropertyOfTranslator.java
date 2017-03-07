package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * see {@link AbstractSubPropertyTranslator}
 * <p>
 * Created by @szuev on 30.09.2016.
 */
class SubDataPropertyOfTranslator extends AbstractSubPropertyTranslator<OWLSubDataPropertyOfAxiom, OntNDP> {
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
    Wrap<OWLSubDataPropertyOfAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory();
        Wrap<OWLDataProperty> sub = ReadHelper._getDataProperty(statement.getSubject().as(OntNDP.class), df);
        Wrap<OWLDataProperty> sup = ReadHelper._getDataProperty(statement.getObject().as(OntNDP.class), df);
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLSubDataPropertyOfAxiom res = df.getOWLSubDataPropertyOfAxiom(sub.getObject(), sup.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(sub).append(sup);
    }
}
