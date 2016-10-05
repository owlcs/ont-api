package ru.avicomp.ontapi;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * store for ontology graph events
 * <p>
 * Created by @szuev on 04.10.2016.
 */
public class OntGraphEventStore {
    private List<Event> events = new CopyOnWriteArrayList<>();

    public void add(Action action, Object axiom, Triple triple) {
        events.add(new Event(action, axiom, triple));
    }

    public void clear() {
        events.clear();
    }

    public List<Event> getEvents() {
        return events;
    }

    public Stream<Triple> triplets() {
        return events().map(e -> e.triple);
    }

    public Stream<Event> events() {
        return events.stream();
    }

    public Stream<Event> events(Action action) {
        return events().filter(event -> action.equals(event.action));
    }

    public OWLAxiom findAxiom(Action action, Triple triple) {
        return events(action).filter(event -> triple.equals(event.triple)).filter(Event::hasAxiom).map(event -> event.axiom).map(OWLAxiom.class::cast).findFirst().orElse(null);
    }

    public OWLAxiom findAxiom(Triple triple) {
        return findAxiom(Action.ADD, triple);
    }

    public enum Action {
        ADD, DELETE,
    }

    public class Event {
        private final Object axiom;
        private final Action action;
        private final Triple triple;

        private Event(Action action, Object axiom, Triple triple) {
            this.axiom = axiom;
            this.action = action;
            this.triple = triple;
        }

        public boolean hasAxiom() {
            return axiom instanceof OWLAxiom;
        }

        @Override
        public String toString() {
            return String.format("[%s][%s]%s", action, axiom, triple);
        }
    }
}
