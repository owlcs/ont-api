package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * see {@link AbstractPropertyRangeTranslator}
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class ObjectPropertyRangeTranslator extends AbstractPropertyRangeTranslator<OWLObjectPropertyRangeAxiom, OntOPE> {
    @Override
    Class<OntOPE> getView() {
        return OntOPE.class;
    }

    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        return super.statements(model).filter(s -> s.getObject().canAs(OntCE.class));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return super.testStatement(statement) && statement.getObject().canAs(OntCE.class);
    }

    @Override
    public Wrap<OWLObjectPropertyRangeAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        Wrap<? extends OWLObjectPropertyExpression> p = ReadHelper.fetchObjectPropertyExpression(statement.getSubject().as(getView()), conf.dataFactory());
        Wrap<? extends OWLClassExpression> ce = ReadHelper.fetchClassExpression(statement.getObject().as(OntCE.class), conf.dataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLObjectPropertyRangeAxiom res = conf.dataFactory().getOWLObjectPropertyRangeAxiom(p.getObject(), ce.getObject(), annotations.getObjects());
        return Wrap.create(res, statement).add(annotations.getTriples()).append(p).append(ce);
    }
}
