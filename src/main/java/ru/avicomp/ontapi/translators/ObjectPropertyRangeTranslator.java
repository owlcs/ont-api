package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * see {@link AbstractPropertyRangeTranslator}
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class ObjectPropertyRangeTranslator extends AbstractPropertyRangeTranslator<OWLObjectPropertyRangeAxiom, OntOPE> {
    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    Wrap<OWLObjectPropertyRangeAxiom> asAxiom(OntStatement statement) {
        Wrap<OWLObjectPropertyExpression> p = ReadHelper._getObjectProperty(statement.getSubject().as(getView()), getDataFactory());
        Wrap<? extends OWLClassExpression> ce = ReadHelper._getClassExpression(statement.getObject().as(OntCE.class), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLObjectPropertyRangeAxiom res = getDataFactory().getOWLObjectPropertyRangeAxiom(p.getObject(), ce.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(ce);
    }
}
