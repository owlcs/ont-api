package ru.avicomp.ontapi;

import org.apache.jena.graph.GraphListener;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.util.graph.GraphListenerBase;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * Graph Event listener and factory.
 * inner graph({@link OntologyModel#getInnerGraph()}) should have this listener assigned to {@link org.apache.jena.graph.GraphEventManager}.
 * <p>
 * Created by @szuev on 04.10.2016.
 */
public class OntGraphListener extends GraphListenerBase {

    public static final GraphListener NULL_GRAPH_LISTENER = new GraphListenerBase() {
        @Override
        protected void addEvent(Triple t) {

        }

        @Override
        protected void deleteEvent(Triple t) {

        }
    };

    private final OntGraphEventStore.OWLEvent objectEvent;
    private final OntGraphEventStore store;

    public OntGraphListener(OntGraphEventStore store, OntGraphEventStore.OWLEvent objectEvent) {
        this.store = store;
        this.objectEvent = objectEvent;
    }

    protected OntGraphEventStore getStore() {
        return store;
    }

    protected OntGraphEventStore.OWLEvent getObjectEvent() {
        return objectEvent;
    }

    @Override
    protected void addEvent(Triple triple) {
        store.add(objectEvent, OntGraphEventStore.TripleEvent.createAdd(triple));
    }

    @Override
    protected void deleteEvent(Triple triple) {
        store.add(objectEvent, OntGraphEventStore.TripleEvent.createDelete(triple));
    }

    public static GraphListener createAdd(OntGraphEventStore store, OWLAxiom axiom) {
        return new OntGraphListener(store, OntGraphEventStore.OWLEvent.createAdd(axiom));
    }

    public static GraphListener createRemove(OntGraphEventStore store, OWLAxiom axiom) {
        return NULL_GRAPH_LISTENER;
    }

    public static GraphListener createChangeID(OntGraphEventStore store, OWLOntologyID id) {
        return NULL_GRAPH_LISTENER;
    }

    public static GraphListener createAdd(OntGraphEventStore store, OWLImportsDeclaration declaration) {
        return new OntGraphListener(store, OntGraphEventStore.OWLEvent.createAdd(declaration));
    }

    public static GraphListener createRemove(OntGraphEventStore store, OWLImportsDeclaration declaration) {
        return NULL_GRAPH_LISTENER;
    }

    public static GraphListener createAdd(OntGraphEventStore store, OWLAnnotation annotation) {
        return new OntGraphListener(store, OntGraphEventStore.OWLEvent.createAdd(annotation));
    }

    public static GraphListener createRemove(OntGraphEventStore store, OWLAnnotation annotation) {
        return NULL_GRAPH_LISTENER;
    }
}
