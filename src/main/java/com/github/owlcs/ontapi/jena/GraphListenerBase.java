package com.github.owlcs.ontapi.jena;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.SimpleEventManager;

import java.util.Iterator;
import java.util.List;

public abstract class GraphListenerBase extends SimpleEventManager {
    protected abstract void addEvent(Triple t);

    protected abstract void deleteEvent(Triple t);

    @Override
    public void notifyAddTriple(Graph g, Triple ts) {
        addEvent(ts);
        super.notifyAddTriple(g, ts);
    }

    @Override
    public void notifyAddArray(Graph g, Triple[] ts) {
        for (Triple t : ts) {
            addEvent(t);
        }
        super.notifyAddArray(g, ts);
    }

    @Override
    public void notifyAddList(Graph g, List<Triple> ts) {
        ts.forEach(this::addEvent);
        super.notifyAddList(g, ts);
    }

    @Override
    public void notifyAddIterator(Graph g, List<Triple> ts) {
        ts.forEach(this::addEvent);
        super.notifyAddIterator(g, ts);
    }

    @Override
    public void notifyAddIterator(Graph g, Iterator<Triple> ts) {
        ts.forEachRemaining(this::addEvent);
        super.notifyAddIterator(g, ts);
    }

    @Override
    public void notifyAddGraph(Graph g, Graph other) {
        other.find().forEachRemaining(this::addEvent);
        super.notifyAddGraph(g, other);
    }

    @Override
    public void notifyDeleteTriple(Graph g, Triple t) {
        deleteEvent(t);
        super.notifyDeleteTriple(g, t);
    }

    @Override
    public void notifyDeleteArray(Graph g, Triple[] ts) {
        for (Triple t : ts) {
            deleteEvent(t);
        }
        super.notifyDeleteArray(g, ts);
    }

    @Override
    public void notifyDeleteList(Graph g, List<Triple> ts) {
        ts.forEach(this::deleteEvent);
        super.notifyDeleteList(g, ts);
    }

    @Override
    public void notifyDeleteIterator(Graph g, List<Triple> ts) {
        ts.forEach(this::deleteEvent);
        super.notifyDeleteIterator(g, ts);
    }

    @Override
    public void notifyDeleteIterator(Graph g, Iterator<Triple> ts) {
        ts.forEachRemaining(this::deleteEvent);
        super.notifyDeleteIterator(g, ts);
    }

    @Override
    public void notifyDeleteGraph(Graph g, Graph other) {
        other.find(Triple.ANY).forEachRemaining(this::deleteEvent);
        super.notifyDeleteGraph(g, other);
    }

    @Override
    public void notifyEvent(Graph source, Object event) {
        super.notifyEvent(source, event);
    }

}
