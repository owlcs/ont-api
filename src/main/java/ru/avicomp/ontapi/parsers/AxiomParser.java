package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * Base class for any Axiom Graph Translator (operator 'T').
 * Specification: <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_the_Structural_Specification_to_RDF_Graphs'>2.1 Translation of Axioms without Annotations</a>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public abstract class AxiomParser<Axiom extends OWLAxiom> {
    private Axiom axiom;

    void init(Axiom axiom) {
        this.axiom = axiom;
    }

    public final Axiom getAxiom() {
        return axiom;
    }

    public abstract void translate(Graph graph);

    public void process(Graph graph) {
        translate(graph);
        AnnotationsParseUtils.translate(graph, getAxiom());
    }
}
