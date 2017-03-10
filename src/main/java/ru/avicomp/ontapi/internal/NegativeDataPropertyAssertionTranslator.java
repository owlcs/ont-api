package ru.avicomp.ontapi.internal;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNPA;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * example: [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :dataProp; owl:targetValue "TEST"^^xsd:string ]
 * Created by szuev on 12.10.2016.
 */
class NegativeDataPropertyAssertionTranslator extends AbstractNegativePropertyAssertionTranslator<OWLNegativeDataPropertyAssertionAxiom, OntNPA.DataAssertion> {
    @Override
    OntNPA.DataAssertion createNPA(OWLNegativeDataPropertyAssertionAxiom axiom, OntGraphModel model) {
        return WriteHelper.addDataProperty(model, axiom.getProperty())
                .addNegativeAssertion(WriteHelper.addIndividual(model, axiom.getSubject()), WriteHelper.toLiteral(axiom.getObject()));
    }

    @Override
    Class<OntNPA.DataAssertion> getView() {
        return OntNPA.DataAssertion.class;
    }

    @Override
    Wrap<OWLNegativeDataPropertyAssertionAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        OntNPA.DataAssertion npa = statement.getSubject().as(getView());
        Wrap<? extends OWLIndividual> s = ReadHelper.getIndividual(npa.getSource(), df);
        Wrap<OWLDataProperty> p = ReadHelper.getDataProperty(npa.getProperty(), df);
        Wrap<OWLLiteral> o = ReadHelper.getLiteral(npa.getTarget(), df);
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLNegativeDataPropertyAssertionAxiom res = df.getOWLNegativeDataPropertyAssertionAxiom(p.getObject(),
                s.getObject(), o.getObject(), annotations.getObjects());
        return Wrap.create(res, npa.content()).add(annotations.getTriples()).append(s).append(p).append(o);
    }
}
