package ru.avicomp.ontapi.internal;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNPA;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * example:
 * [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :objProp; owl:targetIndividual :ind2 ] .
 * Created by szuev on 12.10.2016.
 */
class NegativeObjectPropertyAssertionTranslator extends AbstractNegativePropertyAssertionTranslator<OWLNegativeObjectPropertyAssertionAxiom, OntNPA.ObjectAssertion> {
    @Override
    OntNPA.ObjectAssertion createNPA(OWLNegativeObjectPropertyAssertionAxiom axiom, OntGraphModel model) {
        return WriteHelper.addObjectProperty(model, axiom.getProperty())
                .addNegativeAssertion(WriteHelper.addIndividual(model, axiom.getSubject()), WriteHelper.addIndividual(model, axiom.getObject()));
    }

    @Override
    Class<OntNPA.ObjectAssertion> getView() {
        return OntNPA.ObjectAssertion.class;
    }

    @Override
    Wrap<OWLNegativeObjectPropertyAssertionAxiom> asAxiom(OntStatement statement) {
        OWLDataFactory df = getDataFactory(statement.getModel());
        OntNPA.ObjectAssertion npa = statement.getSubject().as(getView());
        Wrap<? extends OWLIndividual> s = ReadHelper.getIndividual(npa.getSource(), df);
        Wrap<? extends OWLObjectPropertyExpression> p = ReadHelper.getObjectProperty(npa.getProperty(), df);
        Wrap<? extends OWLIndividual> o = ReadHelper.getIndividual(npa.getTarget(), df);
        Wrap.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, df);
        OWLNegativeObjectPropertyAssertionAxiom res = df.getOWLNegativeObjectPropertyAssertionAxiom(p.getObject(),
                s.getObject(), o.getObject(), annotations.getObjects());
        return Wrap.create(res, npa.content()).add(annotations.getTriples()).append(s).append(p).append(o);
    }
}
