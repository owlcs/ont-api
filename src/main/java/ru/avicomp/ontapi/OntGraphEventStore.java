package ru.avicomp.ontapi;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntologyID;

/**
 * Store for ontology graph events and events factory.
 * NOTE: it is not synchronized.
 * <p>
 * Created by @szuev on 04.10.2016.
 */
public class OntGraphEventStore {
    private Map<OWLEvent, Set<TripleEvent>> direct = new HashMap<>();
    private Map<TripleEvent, Set<OWLEvent>> inverse = new HashMap<>();

    public void add(OWLEvent event, TripleEvent triple) {
        direct.computeIfAbsent(event, e -> new HashSet<>()).add(triple);
        inverse.computeIfAbsent(triple, e -> new HashSet<>()).add(event);
    }

    public void clear() {
        direct.clear();
        inverse.clear();
    }

    public void clear(OWLEvent event) {
        direct.remove(event);
        inverse.values().forEach(events -> events.remove(event));
    }

    /**
     * method for debug purposes
     *
     * @return List of {@link EventPair}
     */
    public List<EventPair> getLogs() {
        List<EventPair> res = new ArrayList<>();
        direct.forEach((owl, triplets) -> res.addAll(triplets.stream().map(t -> new EventPair(owl, t)).collect(Collectors.toList())));
        return res;
    }

    public Stream<TripleEvent> triples(OWLEvent owl) {
        return direct.getOrDefault(owl, Collections.emptySet()).stream();
    }

    public Stream<OWLEvent> events(TripleEvent triple) {
        return inverse.getOrDefault(triple, Collections.emptySet()).stream();
    }

    public Set<OWLEvent> getEvents(TripleEvent tripleEvent) {
        return Collections.unmodifiableSet(inverse.getOrDefault(tripleEvent, Collections.emptySet()));
    }

    public Set<TripleEvent> getEvents(OWLEvent owlEvent) {
        return Collections.unmodifiableSet(direct.getOrDefault(owlEvent, Collections.emptySet()));
    }

    public int count(TripleEvent triple) {
        Set set = inverse.get(triple);
        return set == null ? 0 : set.size();
    }

    public int count(OWLEvent owl) {
        Set set = direct.get(owl);
        return set == null ? 0 : set.size();
    }

    public OWLEvent findFirst(TripleEvent tripleEvent) {
        return direct.entrySet().stream().filter(p -> p.getValue().contains(tripleEvent)).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    public static TripleEvent createAdd(Triple triple) {
        return new TripleEvent(BaseEvent.Action.ADD, triple);
    }

    public static TripleEvent createDelete(Triple triple) {
        return new TripleEvent(BaseEvent.Action.DELETE, triple);
    }

    public static OWLEvent createChange(OWLOntologyID id) {
        return new OWLEvent(BaseEvent.Action.CHANGE, id);
    }

    public static OWLEvent createAdd(OWLImportsDeclaration declaration) {
        return new OWLEvent(BaseEvent.Action.ADD, declaration);
    }

    public static OWLEvent createRemove(OWLImportsDeclaration declaration) {
        return new OWLEvent(BaseEvent.Action.DELETE, declaration);
    }

    public static OWLEvent createAdd(OWLAnnotation annotation) {
        return new OWLEvent(BaseEvent.Action.ADD, annotation);
    }

    public static OWLEvent createRemove(OWLAnnotation annotation) {
        return new OWLEvent(BaseEvent.Action.DELETE, annotation);
    }

    public static OWLEvent createAdd(OWLAxiom axiom) {
        return new OWLEvent(BaseEvent.Action.ADD, axiom);
    }

    public static OWLEvent createRemove(OWLAxiom axiom) {
        return new OWLEvent(BaseEvent.Action.DELETE, axiom);
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
        private int hashCode;

        public BaseEvent(Action type, Object eventObject) {
            this.type = OntApiException.notNull(type, "Null action type");
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
            if (hashCode != 0) return hashCode;
            int result = type.hashCode();
            result = 31 * result + (eventObject != null ? eventObject.hashCode() : 0);
            return hashCode = result;
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

        public Action getType() {
            return type;
        }
    }

    public static class TripleEvent extends BaseEvent {

        TripleEvent(Action type, Triple triple) {
            super(type, triple);
        }

        public Triple get() {
            return (Triple) eventObject;
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

        OWLEvent reverse() {
            return isAdd() ? new OWLEvent(Action.DELETE, eventObject) : isDelete() ? new OWLEvent(Action.ADD, eventObject) : this;
        }
    }


}
