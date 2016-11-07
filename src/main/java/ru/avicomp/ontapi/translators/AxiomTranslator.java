package ru.avicomp.ontapi.translators;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * TODO: change to use {@link ru.avicomp.ontapi.jena.impl.GraphModelImpl} instead direct working with graph.
 * TODO: add way to extract an Axiom collection from GraphModel into + change signature of direct method.
 * Base class for any Axiom Graph Translator (operator 'T').
 * Specification: <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_the_Structural_Specification_to_RDF_Graphs'>2.1 Translation of Axioms without Annotations</a>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public abstract class AxiomTranslator<Axiom extends OWLAxiom> {
    private Axiom axiom;

    void init(Axiom axiom) {
        this.axiom = axiom;
    }

    public final Axiom getAxiom() {
        return axiom;
    }

    public abstract void process(Graph graph);

}
