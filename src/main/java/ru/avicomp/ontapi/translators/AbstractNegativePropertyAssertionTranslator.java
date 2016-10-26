package ru.avicomp.ontapi.translators;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLPropertyAssertionAxiom;

import ru.avicomp.ontapi.NodeIRIUtils;

/**
 * for following axioms:
 * NegativeObjectPropertyAssertion ({@link NegativeObjectPropertyAssertionTranslator}),
 * NegativeDataPropertyAssertion ({@link NegativeDataPropertyAssertionTranslator}),
 * <p>
 * Created by szuev on 12.10.2016.
 */
abstract class AbstractNegativePropertyAssertionTranslator<Axiom extends OWLPropertyAssertionAxiom> extends AxiomTranslator<Axiom> {
    @Override
    public void process(Graph graph) {
        OWLPropertyAssertionAxiom axiom = getAxiom();
        Node root = NodeIRIUtils.toNode();
        graph.add(Triple.create(root, RDF.type.asNode(), OWL2.NegativePropertyAssertion.asNode()));
        graph.add(Triple.create(root, OWL2.sourceIndividual.asNode(), NodeIRIUtils.toNode(axiom.getSubject())));
        graph.add(Triple.create(root, OWL2.assertionProperty.asNode(), NodeIRIUtils.toNode(axiom.getProperty())));
        graph.add(Triple.create(root, getTargetPredicate().asNode(), NodeIRIUtils.toNode(axiom.getObject())));
        TranslationHelper.addAnnotations(graph, root, axiom);
    }

    public abstract Property getTargetPredicate();
}
