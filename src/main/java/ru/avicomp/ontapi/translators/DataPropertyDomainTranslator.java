package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyDomainAxiomImpl;

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
    OWLDataPropertyDomainAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLDataProperty p = ReadHelper.getDataProperty(statement.getSubject().as(OntNDP.class));
        OWLClassExpression ce = ReadHelper.getClassExpression(statement.getObject().as(OntCE.class));
        return new OWLDataPropertyDomainAxiomImpl(p, ce, annotations);
    }

    @Override
    Wrap<OWLDataPropertyDomainAxiom> asAxiom(OntStatement statement) {
        Wrap<OWLDataProperty> p = ReadHelper._getDataProperty(statement.getSubject().as(getView()), getDataFactory());
        Wrap<? extends OWLClassExpression> ce = ReadHelper._getClassExpression(statement.getObject().as(OntCE.class), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLDataPropertyDomainAxiom res = getDataFactory().getOWLDataPropertyDomainAxiom(p.getObject(), ce.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(ce);
    }
}
