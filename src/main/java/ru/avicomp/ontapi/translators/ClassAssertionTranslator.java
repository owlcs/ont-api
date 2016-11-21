package ru.avicomp.ontapi.translators;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;

/**
 * creating individual (both named and anonymous):
 * pizza:France rdf:type owl:NamedIndividual, pizza:Country, owl:Thing.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class ClassAssertionTranslator extends AxiomTranslator<OWLClassAssertionAxiom> {
    @Override
    public void write(OWLClassAssertionAxiom axiom, Graph graph) {
        Model model = ModelFactory.createModelForGraph(graph);
        OWLIndividual individual = axiom.getIndividual();
        Resource subject = individual.isAnonymous() ?
                TranslationHelper.toResource(individual) :
                TranslationHelper.addRDFNode(model, individual).asResource();
        RDFNode object = TranslationHelper.addRDFNode(model, axiom.getClassExpression());
        Property predicate = RDF.type;
        model.add(subject, predicate, object);
        TranslationHelper.addAnnotations(model, subject, predicate, object, axiom);
    }
}
