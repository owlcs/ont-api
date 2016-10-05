package ru.avicomp.ontapi;

import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * TODO:
 * inner graph should have this listener assigned to event-manager.
 * Created by @szuev on 04.10.2016.
 */
public class OntGraphListener extends GraphListenerBase {

    private final OWLAxiom axiom;
    private final OntGraphEventStore store;

    public OntGraphListener(OntGraphEventStore store, OWLAxiom axiom) {
        this.axiom = axiom;
        this.store = store;
    }

    @Override
    protected void addEvent(Triple triple) {
        store.add(OntGraphEventStore.Action.ADD, axiom, triple);
    }

    @Override
    protected void deleteEvent(Triple triple) {
        store.add(OntGraphEventStore.Action.DELETE, axiom, triple);
    }

}
