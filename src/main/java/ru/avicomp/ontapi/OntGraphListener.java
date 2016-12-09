package ru.avicomp.ontapi;

import org.apache.jena.graph.GraphListener;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.util.graph.GraphListenerBase;

/**
 * Graph Event listener and factory.
 * inner graph({@link OntologyModelImpl#getInnerGraph()}) should have this listener assigned to {@link org.apache.jena.graph.GraphEventManager}.
 * <p>
 * Created by @szuev on 04.10.2016.
 */
@Deprecated
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

    public OntGraphEventStore getStore() {
        return store;
    }

    public OntGraphEventStore.OWLEvent getOWLEvent() {
        return objectEvent;
    }

    @Override
    protected void addEvent(Triple triple) {
        store.add(objectEvent, OntGraphEventStore.createAdd(triple));
    }

    @Override
    protected void deleteEvent(Triple triple) {
        store.add(objectEvent, OntGraphEventStore.createDelete(triple));
    }

    public static GraphListener create(OntGraphEventStore store, OntGraphEventStore.OWLEvent event) {
        return event.isAdd() ? new OntGraphListener(store, event) : NULL_GRAPH_LISTENER;
    }

}
