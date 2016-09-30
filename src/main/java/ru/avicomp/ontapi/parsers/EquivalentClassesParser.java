package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;

import ru.avicomp.ontapi.NodeIRIUtils;
import ru.avicomp.ontapi.OntException;

/**
 * Example of ttl:
 * pizza:SpicyTopping owl:equivalentClass [ a owl:Class; owl:intersectionOf ( pizza:PizzaTopping [a owl:Restriction; owl:onProperty pizza:hasSpiciness; owl:someValuesFrom pizza:Hot] )] ;
 * <p>
 * Created by @szuev on 29.09.2016.
 */
class EquivalentClassesParser extends AxiomParser<OWLEquivalentClassesAxiom> {

    private static void process(Graph graph, OWLEquivalentClassesAxiom axiom) {
        OWLClass clazz = (OWLClass) axiom.classExpressions().filter(e -> !e.isAnonymous()).findFirst().orElse(null);
        if (clazz == null) throw new OntException("Can't find none anonymous class expression inside " + axiom);
        OWLClassExpression rest = axiom.classExpressions().filter((obj) -> !clazz.equals(obj)).findFirst().orElse(null);
        if (rest == null) throw new OntException("Can't find another class expression inside " + axiom);
        Model model = ModelFactory.createModelForGraph(graph);
        Resource subject = NodeIRIUtils.toResource(clazz.getIRI());
        model.add(subject, OWL.equivalentClass, ParseUtils.toResource(model, rest));
    }

    @Override
    public void process(Graph graph) {
        getAxiom().asPairwiseAxioms().forEach(axiom -> process(graph, axiom));
    }
}
