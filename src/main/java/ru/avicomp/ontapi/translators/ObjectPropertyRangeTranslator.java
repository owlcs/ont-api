package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.*;

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
        OWLDataFactory df = getDataFactory(statement.getModel());
        Wrap<? extends OWLObjectPropertyExpression> p = ReadHelper.getObjectProperty(statement.getSubject().as(getView()), df);
        Wrap<? extends OWLClassExpression> ce = ReadHelper.getClassExpression(statement.getObject().as(OntCE.class), df);
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLObjectPropertyRangeAxiom res = df.getOWLObjectPropertyRangeAxiom(p.getObject(), ce.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(ce);
    }
}
