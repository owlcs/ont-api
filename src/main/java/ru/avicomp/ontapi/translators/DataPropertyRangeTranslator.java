package ru.avicomp.ontapi.translators;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntDR;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
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
    Stream<OntStatement> statements(OntGraphModel model) {
        return super.statements(model).filter(s -> s.getObject().canAs(OntDR.class));
    }

    @Override
    Wrap<OWLDataPropertyRangeAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        Wrap<OWLDataProperty> p = ReadHelper.getDataProperty(statement.getSubject().as(getView()), df);
        Wrap<? extends OWLDataRange> d = ReadHelper.getDataRange(statement.getObject().as(OntDR.class), df);
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLDataPropertyRangeAxiom res = df.getOWLDataPropertyRangeAxiom(p.getObject(), d.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(d);
    }

}
