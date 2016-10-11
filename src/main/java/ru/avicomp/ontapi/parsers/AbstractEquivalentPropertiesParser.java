package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.semanticweb.owlapi.model.OWLNaryPropertyAxiom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;

import ru.avicomp.ontapi.OntException;

/**
 * base for {@link EquivalentObjectPropertiesParser} and {@link EquivalentDataPropertiesParser}
 * Created by @szuev on 01.10.2016.
 */
abstract class AbstractEquivalentPropertiesParser<Axiom extends OWLNaryPropertyAxiom<? extends OWLPropertyExpression>> extends AxiomParser<Axiom> {

    private static void process(Graph graph, OWLPropertyExpression first, OWLPropertyExpression rest) {
        Resource subject = AxiomParseUtils.toResource(first);
        Property predicate = OWL.equivalentProperty;
        Resource object = AxiomParseUtils.toResource(rest);
        graph.add(Triple.create(subject.asNode(), predicate.asNode(), object.asNode()));
    }

    @Override
    public void translate(Graph graph) {
        getAxiom().asPairwiseAxioms().forEach(axiom -> {
            OWLPropertyExpression first = axiom.operands().filter(e -> !e.isAnonymous()).findFirst().
                    orElseThrow(() -> new OntException("Can't find non-anonymous property expression."));
            OWLPropertyExpression rest = axiom.operands().filter(e -> !first.equals(e)).findFirst().
                    orElseThrow(() -> new OntException("Can't find the second property expression."));
            process(graph, first, rest);
        });
    }

}
