package ru.avicomp.ontapi.parsers;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.graph.Graph;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;

import ru.avicomp.ontapi.NodeIRIUtils;
import ru.avicomp.ontapi.OntException;

/**
 * example:
 * gr:Brand owl:disjointWith gr:BusinessEntity , gr:BusinessEntityType ,  gr:BusinessFunction .
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DisjointClassesParser extends AxiomParser<OWLDisjointClassesAxiom> {

    private static void process(final Graph graph, OWLDisjointClassesAxiom axiom, boolean direct) {
        List<OWLClassExpression> classExpression = axiom.classExpressions().collect(Collectors.toList());
        if (classExpression.size() != 2) throw new OntException("Something wrong " + axiom);
        IRI subject = ParseUtils.toIRI(classExpression.get(direct ? 0 : 1));
        IRI predicate = NodeIRIUtils.fromResource(OWL.disjointWith);
        IRI object = ParseUtils.toIRI(classExpression.get(direct ? 1 : 0));
        graph.add(NodeIRIUtils.toTriple(subject, predicate, object));
    }

    @Override
    public void process(Graph graph) {
        getAxiom().asPairwiseAxioms().forEach(a -> process(graph, a, true));
    }
}
