package ru.avicomp.ontapi.internal;

import java.util.stream.Stream;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * see {@link AbstractPropertyDomainTranslator}
 * rdfs:domain
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public class DataPropertyDomainTranslator extends AbstractPropertyDomainTranslator<OWLDataPropertyDomainAxiom, OntNDP> {
    @Override
    Class<OntNDP> getView() {
        return OntNDP.class;
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
    public InternalObject<OWLDataPropertyDomainAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        InternalObject<OWLDataProperty> p = ReadHelper.fetchDataProperty(statement.getSubject().as(getView()), conf.dataFactory());
        InternalObject<? extends OWLClassExpression> ce = ReadHelper.fetchClassExpression(statement.getObject().as(OntCE.class), conf.dataFactory());
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLDataPropertyDomainAxiom res = conf.dataFactory().getOWLDataPropertyDomainAxiom(p.getObject(), ce.getObject(), annotations.getObjects());
        return InternalObject.create(res, statement).add(annotations.getTriples()).append(p).append(ce);
    }
}
