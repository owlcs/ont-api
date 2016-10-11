package ru.avicomp.ontapi.parsers;

import java.util.Iterator;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;

/**
 * Note! it is for an owl-expression without any root!
 * Example:
 * [ a owl:AllDifferent; owl:distinctMembers (pizza:America pizza:Italy pizza:Germany pizza:England pizza:France) ].
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class DifferentIndividualsParser extends AxiomParser<OWLDifferentIndividualsAxiom> {
    @Override
    public void translate(Graph graph) {
        OWLDifferentIndividualsAxiom axiom = getAxiom();
        Model model = ModelFactory.createModelForGraph(graph);
        Iterator<? extends RDFNode> iterator = AxiomParseUtils.toResourceIterator(model, axiom.individuals());
        Resource root = model.createResource();
        model.add(root, RDF.type, OWL.AllDifferent);
        RDFList list = model.createList(iterator);
        model.add(root, OWL.distinctMembers, list);
    }
}
