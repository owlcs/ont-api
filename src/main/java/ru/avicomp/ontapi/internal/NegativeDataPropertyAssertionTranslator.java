package ru.avicomp.ontapi.internal;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNPA;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * example: [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :dataProp; owl:targetValue "TEST"^^xsd:string ]
 * Created by szuev on 12.10.2016.
 */
public class NegativeDataPropertyAssertionTranslator extends AbstractNegativePropertyAssertionTranslator<OWLNegativeDataPropertyAssertionAxiom, OntNPA.DataAssertion> {
    @Override
    OntNPA.DataAssertion createNPA(OWLNegativeDataPropertyAssertionAxiom axiom, OntGraphModel model) {
        return WriteHelper.addDataProperty(model, axiom.getProperty())
                .addNegativeAssertion(WriteHelper.addIndividual(model, axiom.getSubject()), WriteHelper.addLiteral(model, axiom.getObject()));
    }

    @Override
    Class<OntNPA.DataAssertion> getView() {
        return OntNPA.DataAssertion.class;
    }

    @Override
    public Wrap<OWLNegativeDataPropertyAssertionAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        OntNPA.DataAssertion npa = statement.getSubject().as(getView());
        Wrap<? extends OWLIndividual> s = ReadHelper.fetchIndividual(npa.getSource(), conf.dataFactory());
        Wrap<OWLDataProperty> p = ReadHelper.fetchDataProperty(npa.getProperty(), conf.dataFactory());
        Wrap<OWLLiteral> o = ReadHelper.getLiteral(npa.getTarget(), conf.dataFactory());
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLNegativeDataPropertyAssertionAxiom res = conf.dataFactory().getOWLNegativeDataPropertyAssertionAxiom(p.getObject(),
                s.getObject(), o.getObject(), annotations.getObjects());
        return Wrap.create(res, npa.content()).add(annotations.getTriples()).append(s).append(p).append(o);
    }
}
