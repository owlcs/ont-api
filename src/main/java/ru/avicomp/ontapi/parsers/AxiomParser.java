package ru.avicomp.ontapi.parsers;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphListener;
import org.semanticweb.owlapi.model.OWLAxiom;

import ru.avicomp.ontapi.OntException;
import ru.avicomp.ontapi.OntGraphEventStore;
import ru.avicomp.ontapi.OntGraphListener;

/**
 * Base class for any Axiom parser.
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

    public void reverse(Graph graph) {
        throw new OntException.Unsupported(getClass(), "reverse");
    }

    public void process(OntGraphEventStore store, Graph graph) {
        GraphListener listener = OntGraphListener.createAdd(store, axiom);
        try {
            graph.getEventManager().register(listener);
            process(graph);
        } catch (Exception e) {
            throw new OntException("Add axiom " + axiom, e);
        } finally {
            graph.getEventManager().unregister(listener);
        }
    }

    public void reverse(OntGraphEventStore store, Graph graph) {
        GraphListener listener = OntGraphListener.createRemove(store, axiom);
        try {
            graph.getEventManager().register(listener);
            reverse(graph);
        } catch (Exception e) {
            throw new OntException("Remove axiom " + axiom, e);
        } finally {
            graph.getEventManager().unregister(listener);
        }
    }

}
