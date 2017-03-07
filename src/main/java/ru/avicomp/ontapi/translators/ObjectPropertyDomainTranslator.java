package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * see {@link AbstractPropertyDomainTranslator}
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class ObjectPropertyDomainTranslator extends AbstractPropertyDomainTranslator<OWLObjectPropertyDomainAxiom, OntOPE> {
    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    Wrap<OWLObjectPropertyDomainAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        Wrap<? extends OWLObjectPropertyExpression> p = ReadHelper.getObjectProperty(statement.getSubject().as(getView()), df);
        Wrap<? extends OWLClassExpression> ce = ReadHelper.getClassExpression(statement.getObject().as(OntCE.class), df);
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLObjectPropertyDomainAxiom res = df.getOWLObjectPropertyDomainAxiom(p.getObject(), ce.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(ce);
    }
}
