package ru.avicomp.ontapi.parsers;

import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/**
 * Examples:
 * pizza:JalapenoPepperTopping rdfs:subClassOf pizza:PepperTopping.
 * pizza:JalapenoPepperTopping rdfs:subClassOf [ a owl:Restriction; owl:onProperty pizza:hasSpiciness; owl:someValuesFrom pizza:Hot].
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class SubClassOfParser extends AxiomParser<OWLSubClassOfAxiom> {
    @Override
    public void process(Graph graph) {
        Model model = ModelFactory.createModelForGraph(graph);
        Resource subject = AxiomParseUtils.addResource(model, getAxiom().getSubClass());
        Property predicate = RDFS.subClassOf;
        Resource object = AxiomParseUtils.addResource(model, getAxiom().getSuperClass());
        Stream.of(subject, object). // just in case create class declarations
                filter(RDFNode::isURIResource).
                filter(r -> !OWL.Thing.equals(r)).
                filter(r -> !OWL.Nothing.equals(r)).
                forEach(r -> model.add(r, RDF.type, OWL.Class));
        model.add(subject, predicate, object);
        AnnotationsParseUtils.translate(model, subject, predicate, object, getAxiom());
    }
}
