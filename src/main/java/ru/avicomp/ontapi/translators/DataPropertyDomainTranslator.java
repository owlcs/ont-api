package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * see {@link AbstractPropertyDomainTranslator}
 * rdfs:domain
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DataPropertyDomainTranslator extends AbstractPropertyDomainTranslator<OWLDataPropertyDomainAxiom, OntNDP> {
    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    Wrap<OWLDataPropertyDomainAxiom> asAxiom(OntStatement statement) {
        Wrap<OWLDataProperty> p = ReadHelper.getDataProperty(statement.getSubject().as(getView()), getDataFactory());
        Wrap<? extends OWLClassExpression> ce = ReadHelper.getClassExpression(statement.getObject().as(OntCE.class), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, getDataFactory());
        OWLDataPropertyDomainAxiom res = getDataFactory().getOWLDataPropertyDomainAxiom(p.getObject(), ce.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(ce);
    }
}
