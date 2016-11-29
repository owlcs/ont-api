package ru.avicomp.ontapi.translators;

import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntNPA;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLNegativeObjectPropertyAssertionAxiomImpl;

/**
 * example:
 * [ a owl:NegativePropertyAssertion; owl:sourceIndividual :ind1; owl:assertionProperty :objProp; owl:targetIndividual :ind2 ] .
 * Created by szuev on 12.10.2016.
 */
class NegativeObjectPropertyAssertionTranslator extends AbstractNegativePropertyAssertionTranslator<OWLNegativeObjectPropertyAssertionAxiom, OntNPA.ObjectAssertion> {
    @Override
    OntNPA.ObjectAssertion createNPA(OWLNegativeObjectPropertyAssertionAxiom axiom, OntGraphModel model) {
        return OWL2RDFHelper.addObjectProperty(model, axiom.getProperty())
                .addNegativeAssertion(OWL2RDFHelper.addIndividual(model, axiom.getSubject()), OWL2RDFHelper.addIndividual(model, axiom.getObject()));
    }

    @Override
    Class<OntNPA.ObjectAssertion> getView() {
        return OntNPA.ObjectAssertion.class;
    }

    @Override
    OWLNegativeObjectPropertyAssertionAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OntNPA.ObjectAssertion npa = statement.getSubject().as(OntNPA.ObjectAssertion.class);
        OWLIndividual subject = RDF2OWLHelper.getIndividual(npa.getSource());
        OWLObjectPropertyExpression property = RDF2OWLHelper.getObjectProperty(npa.getProperty());
        OWLIndividual object = RDF2OWLHelper.getIndividual(npa.getTarget());
        return new OWLNegativeObjectPropertyAssertionAxiomImpl(subject, property, object, annotations);
    }
}
