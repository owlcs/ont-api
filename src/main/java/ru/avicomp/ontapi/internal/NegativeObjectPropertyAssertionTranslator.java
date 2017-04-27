package ru.avicomp.ontapi.internal;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNPA;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * example:
 * [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :objProp; owl:targetIndividual :ind2 ] .
 * Created by szuev on 12.10.2016.
 */
public class NegativeObjectPropertyAssertionTranslator extends AbstractNegativePropertyAssertionTranslator<OWLNegativeObjectPropertyAssertionAxiom, OntNPA.ObjectAssertion> {
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
    public InternalObject<OWLNegativeObjectPropertyAssertionAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        OntNPA.ObjectAssertion npa = statement.getSubject().as(getView());
        InternalObject<? extends OWLIndividual> s = ReadHelper.fetchIndividual(npa.getSource(), conf.dataFactory());
        InternalObject<? extends OWLObjectPropertyExpression> p = ReadHelper.fetchObjectPropertyExpression(npa.getProperty(), conf.dataFactory());
        InternalObject<? extends OWLIndividual> o = ReadHelper.fetchIndividual(npa.getTarget(), conf.dataFactory());
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLNegativeObjectPropertyAssertionAxiom res = conf.dataFactory().getOWLNegativeObjectPropertyAssertionAxiom(p.getObject(),
                s.getObject(), o.getObject(), annotations.getObjects());
        return InternalObject.create(res, npa.content()).add(annotations.getTriples()).append(s).append(p).append(o);
    }
}
