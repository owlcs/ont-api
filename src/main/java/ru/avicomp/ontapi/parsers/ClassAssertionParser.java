package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLIndividual;

import ru.avicomp.ontapi.NodeIRIUtils;
import ru.avicomp.ontapi.OntException;

/**
 * Created by @szuev on 28.09.2016.
 */
class ClassAssertionParser extends AxiomParser<OWLClassAssertionAxiom> {

    @Override
    public void process(Graph graph) {
        Resource subject;
        OWLIndividual individual = getAxiom().getIndividual();
        if (individual.isNamed()) {
            subject = NodeIRIUtils.toResource(individual.asOWLNamedIndividual().getIRI());
        } else {
            throw new OntException("Anonymous individuals are unsupported now " + individual);
        }
        Resource object = NodeIRIUtils.toResource(ParseUtils.toIRI(getAxiom().getClassExpression()));
        Model model = ModelFactory.createModelForGraph(graph);
        model.add(subject, RDF.type, OWL2.NamedIndividual);
        model.add(subject, RDF.type, object);
    }
}
