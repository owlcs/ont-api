package ru.avicomp.ontapi.parsers;

import java.util.List;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;

import ru.avicomp.ontapi.OntException;

/**
 * creating individual:
 * pizza:France rdf:type owl:NamedIndividual, pizza:Country, owl:Thing.
 *
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

    @Override
    public void reverse(Graph graph) {
        OWLIndividual individual = getAxiom().getIndividual();
        Resource subject = ParseUtils.toResource(individual);
        List<Triple> list = graph.find(subject.asNode(), RDF.type.asNode(), OWL2.NamedIndividual.asNode()).toList();
        if (list.isEmpty()) throw new OntException("Can't find individual " + individual);
        graph.remove(subject.asNode(), Node.ANY, Node.ANY);
    }
}
