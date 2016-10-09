package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;

/**
 * creating individual:
 * pizza:France rdf:type owl:NamedIndividual, pizza:Country, owl:Thing.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class ClassAssertionParser extends AxiomParser<OWLClassAssertionAxiom> {
    @Override
    public void process(Graph graph) {
        Model model = ModelFactory.createModelForGraph(graph);
        Resource subject = ParseUtils.toResource(getAxiom().getIndividual());
        Resource object = ParseUtils.toResource(ParseUtils.toIRI(getAxiom().getClassExpression()));
        model.add(subject, RDF.type, OWL2.NamedIndividual);
        model.add(subject, RDF.type, object);
    }
}
