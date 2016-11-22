package ru.avicomp.ontapi.translators;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLPropertyAssertionAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;

/**
 * for following axioms:
 * NegativeObjectPropertyAssertion ({@link NegativeObjectPropertyAssertionTranslator}),
 * NegativeDataPropertyAssertion ({@link NegativeDataPropertyAssertionTranslator}),
 * <p>
 * Created by szuev on 12.10.2016.
 */
abstract class AbstractNegativePropertyAssertionTranslator<Axiom extends OWLPropertyAssertionAxiom> extends AxiomTranslator<Axiom> {
    @Override
    public void write(Axiom axiom, OntGraphModel model) { // todo: its wrong:
        Resource root = model.createResource();
        model.add(root, RDF.type, OWL2.NegativePropertyAssertion);
        model.add(root, OWL2.sourceIndividual, TranslationHelper.toRDFNode(axiom.getSubject()));
        model.add(root, OWL2.assertionProperty, TranslationHelper.toRDFNode(axiom.getProperty()));
        model.add(root, getTargetPredicate(), TranslationHelper.toRDFNode(axiom.getObject()));
        TranslationHelper.addAnnotations(model, root, axiom);
    }

    public abstract Property getTargetPredicate();
}
