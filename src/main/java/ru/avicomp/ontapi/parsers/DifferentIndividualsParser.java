package ru.avicomp.ontapi.parsers;

import java.util.Iterator;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
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
    public void process(Graph graph) {
        OWLDifferentIndividualsAxiom axiom = getAxiom();
        Model model = ModelFactory.createModelForGraph(graph);
        Iterator<? extends RDFNode> iterator = ParseUtils.toResourceIterator(model, axiom.individuals());
        Node root = NodeFactory.createBlankNode();
        graph.add(Triple.create(root, RDF.type.asNode(), OWL.AllDifferent.asNode()));
        RDFList list = model.createList(iterator);
        model.add(model.getRDFNode(root).asResource(), OWL.distinctMembers, list);
    }
}
