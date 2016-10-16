package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;

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
        Resource subject = AxiomParseUtils.addRDFNode(model, getAxiom().getIndividual()).asResource();
        RDFNode object = AxiomParseUtils.addRDFNode(model, getAxiom().getClassExpression());
        Property predicate = RDF.type;
        model.add(subject, predicate, object);
        AnnotationsParseUtils.addAnnotations(model, subject, predicate, object, getAxiom());
    }
}
