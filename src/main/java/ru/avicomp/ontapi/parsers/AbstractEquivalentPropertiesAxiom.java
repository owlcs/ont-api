package ru.avicomp.ontapi.parsers;

import java.util.List;
import java.util.stream.Collectors;

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
abstract class AbstractEquivalentPropertiesAxiom<Axiom extends OWLNaryPropertyAxiom<? extends OWLPropertyExpression>> extends AxiomParser<Axiom> {
    private static void process(Graph graph, OWLNaryPropertyAxiom<? extends OWLPropertyExpression> axiom, boolean direct) {
        List<OWLPropertyExpression> expressions = axiom.properties().collect(Collectors.toList());
        if (expressions.size() != 2) throw new OntException("Something wrong " + axiom);
        Resource subject = ParseUtils.toResource(expressions.get(direct ? 0 : 1));
        Property predicate = OWL.equivalentProperty;
        Resource object = ParseUtils.toResource(expressions.get(direct ? 1 : 0));
        graph.add(Triple.create(subject.asNode(), predicate.asNode(), object.asNode()));
    }

    @Override
    public void process(Graph graph) {
        process(graph, getAxiom(), true);
    }
}
