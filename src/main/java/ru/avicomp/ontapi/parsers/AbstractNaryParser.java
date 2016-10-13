package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.IsAnonymous;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNaryAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.OntException;

/**
 * Base class for following axioms:
 *  EquivalentClasses ({@link EquivalentClassesParser}),
 *  EquivalentObjectProperties ({@link EquivalentObjectPropertiesParser}),
 *  EquivalentDataProperties ({@link EquivalentDataPropertiesParser}), or
 *  SameIndividual ({@link SameIndividualParser}).
 *
 *  How to annotate see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Axioms_that_are_Translated_to_Multiple_Triples'>2.3.2 Axioms that are Translated to Multiple Triples</a>
 *
 * Created by szuev on 13.10.2016.
 */
abstract class AbstractNaryParser<Axiom extends OWLAxiom & OWLNaryAxiom<? extends IsAnonymous>> extends AxiomParser<Axiom> {

    private void process(Graph graph, OWLNaryAxiom<? extends IsAnonymous> axiom) {
        OWLObject first = axiom.operands().filter(e -> !e.isAnonymous()).findFirst().
                orElseThrow(() -> new OntException("Can't find a single non-anonymous expression inside " + axiom));
        OWLObject rest = axiom.operands().filter((obj) -> !first.equals(obj)).findFirst().
                orElseThrow(() -> new OntException("Should be at least two expressions inside " + axiom));
        Model model = ModelFactory.createModelForGraph(graph);
        Resource subject = AxiomParseUtils.addResource(model, first);
        Property predicate = getPredicate();
        Resource object = AxiomParseUtils.addResource(model, rest);
        model.add(subject, predicate, object);
        AnnotationsParseUtils.translate(model, subject, predicate, object, axiom);
    }

    @Override
    public void process(Graph graph) {
        getAxiom().asPairwiseAxioms().forEach(axiom -> process(graph, axiom));
    }

    public abstract Property getPredicate();
}
