package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * Base class foe any Axiom parser.
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

    public abstract void process(Graph graph);
}
