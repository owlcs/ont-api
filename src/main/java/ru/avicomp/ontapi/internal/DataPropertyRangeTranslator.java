package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;

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
    public Stream<OntStatement> statements(OntGraphModel model) {
        return super.statements(model).filter(s -> s.getObject().canAs(OntDR.class));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return super.testStatement(statement) && statement.getObject().canAs(OntDR.class);
    }

    @Override
    public Wrap<OWLDataPropertyRangeAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        Wrap<OWLDataProperty> p = ReadHelper.fetchDataProperty(statement.getSubject().as(getView()), conf.dataFactory());
        Wrap<? extends OWLDataRange> d = ReadHelper.fetchDataRange(statement.getObject().as(OntDR.class), conf.dataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLDataPropertyRangeAxiom res = conf.dataFactory().getOWLDataPropertyRangeAxiom(p.getObject(), d.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(d);
    }

}
