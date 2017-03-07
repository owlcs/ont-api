package ru.avicomp.ontapi.translators;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;

import ru.avicomp.ontapi.jena.model.OntDR;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * see {@link AbstractPropertyRangeTranslator}
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DataPropertyRangeTranslator extends AbstractPropertyRangeTranslator<OWLDataPropertyRangeAxiom, OntNDP> {
    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
    }

    @Override
    Wrap<OWLDataPropertyRangeAxiom> asAxiom(OntStatement statement) {
        Wrap<OWLDataProperty> p = ReadHelper._getDataProperty(statement.getSubject().as(getView()), getDataFactory());
        Wrap<? extends OWLDataRange> d = ReadHelper._getDataRange(statement.getObject().as(OntDR.class), getDataFactory());
        Wrap.Collection<OWLAnnotation> annotations = annotations(statement);
        OWLDataPropertyRangeAxiom res = getDataFactory().getOWLDataPropertyRangeAxiom(p.getObject(), d.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(d);
    }
}
