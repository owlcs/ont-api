package ru.avicomp.ontapi.parsers.axiom2rdf;

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
class ClassAssertionParser extends AxiomParser<OWLClassAssertionAxiom> {
    @Override
    public void process(Graph graph) {
        Model model = ModelFactory.createModelForGraph(graph);
        OWLIndividual individual = getAxiom().getIndividual();
        Resource subject = individual.isAnonymous() ?
                TranslationHelper.toResource(individual) :
                TranslationHelper.addRDFNode(model, individual).asResource();
        RDFNode object = TranslationHelper.addRDFNode(model, getAxiom().getClassExpression());
        Property predicate = RDF.type;
        model.add(subject, predicate, object);
        TranslationHelper.addAnnotations(model, subject, predicate, object, getAxiom());
    }
}
