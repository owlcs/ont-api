package ru.avicomp.ontapi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * store for ontology graph events
 * <p>
 * Created by @szuev on 04.10.2016.
 */
public class OntGraphEventStore {
    private Map<OWLEvent, Set<TripleEvent>> events = new ConcurrentHashMap<>();

    public void add(OWLEvent event, TripleEvent triple) {
        events.computeIfAbsent(event, e -> new HashSet<>()).add(triple);
    }

    public void clear() {
        events.clear();
    }

    public void clear(OWLEvent event) {
        events.remove(event);
    }

    public List<EventPair> getEvents() {
        List<EventPair> res = new ArrayList<>();
        events.forEach((owl, triplets) -> res.addAll(triplets.stream().map(t -> new EventPair(owl, t)).collect(Collectors.toList())));
        return res;
    }

    public OWLEvent find(TripleEvent tripleEvent) {
        return events.entrySet().stream().filter(p -> p.getValue().contains(tripleEvent)).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public static class EventPair {
        private final OWLEvent objectEvent;
        private final TripleEvent tripleEvent;

        private EventPair(OWLEvent objectEvent, TripleEvent tripleEvent) {
            this.objectEvent = objectEvent;
            this.tripleEvent = tripleEvent;
        }

        @Override
        public String toString() {
            return String.format("%s%s", objectEvent, tripleEvent);
        }

        public TripleEvent getTripleEvent() {
            return tripleEvent;
        }

        public OWLEvent getOWLEvent() {
            return objectEvent;
        }
    }

    public static abstract class BaseEvent {
        protected final Action type;
        protected final Object eventObject;

        public BaseEvent(Action type, Object eventObject) {
            this.type = OntException.notNull(type, "Null action type");
            this.eventObject = eventObject;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BaseEvent that = (BaseEvent) o;
            return type == that.type && Objects.equals(eventObject, that.eventObject);
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + (eventObject != null ? eventObject.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return String.format("[%s][%s]", type, eventObject);
        }

        public enum Action {
            ADD,
            DELETE,
            CHANGE,
        }

        public boolean isAdd() {
            return Action.ADD.equals(type);
        }

        public boolean isDelete() {
            return Action.DELETE.equals(type);
        }
    }

    public static class TripleEvent extends BaseEvent {

        TripleEvent(Action type, Triple triple) {
            super(type, triple);
        }

        public static TripleEvent createAdd(Triple triple) {
            return new TripleEvent(Action.ADD, triple);
        }

        public static TripleEvent createDelete(Triple triple) {
            return new TripleEvent(Action.DELETE, triple);
        }
    }

    public static class OWLEvent extends BaseEvent {

        OWLEvent(Action type, Object owlObject) {
            super(type, owlObject);
        }

        public <T> T get(Class<T> view) {
            return is(view) ? view.cast(eventObject) : null;
        }

        public <T> boolean is(Class<T> view) {
            return view.isInstance(eventObject);
        }

        public static OWLEvent createAdd(OWLImportsDeclaration declaration) {
            return new OWLEvent(Action.ADD, declaration);
        }

        public static OWLEvent createChange(OWLOntologyID id) {
            return new OWLEvent(Action.CHANGE, id);
        }

        public static OWLEvent createAdd(OWLAnnotation annotation) {
            return new OWLEvent(Action.ADD, annotation);
        }

        public static OWLEvent createAdd(OWLAxiom axiom) {
            return new OWLEvent(Action.ADD, axiom);
        }
    }



}
