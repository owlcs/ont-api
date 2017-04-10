package ru.avicomp.ontapi.jena;

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.jena.graph.*;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.shared.DeleteDeniedException;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * Wrapper with {@link ReadWriteLock} inside (OWL-API synchronization style).
 * Note: the related objects (such as {@link PrefixMapping}) are not synchronized!
 * <p>
 * Created by @szuev on 07.04.2017.
 */
public class ConcurrentGraph implements Graph {
    protected final Graph base;
    protected final ReadWriteLock lock;

    public ConcurrentGraph(Graph base, ReadWriteLock lock) {
        this.base = OntJenaException.notNull(base, "Null base graph");
        this.lock = OntJenaException.notNull(lock, "Null lock");
    }

    public Graph get() {
        return base;
    }

    public ReadWriteLock lock() {
        return lock;
    }

    @Override
    public boolean dependsOn(Graph other) {
        try {
            lock().readLock().lock();
            return get().dependsOn(other);
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public void add(Triple t) throws AddDeniedException {
        try {
            lock().writeLock().lock();
            get().add(t);
        } finally {
            lock().writeLock().unlock();
        }
    }

    @Override
    public void delete(Triple t) throws DeleteDeniedException {
        try {
            lock().writeLock().lock();
            get().delete(t);
        } finally {
            lock().writeLock().unlock();
        }
    }

    @Override
    public ExtendedIterator<Triple> find(Triple m) {
        try {
            lock().readLock().lock();
            return get().find(m);
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public ExtendedIterator<Triple> find(Node s, Node p, Node o) {
        try {
            lock().readLock().lock();
            return get().find(s, p, o);
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public boolean isIsomorphicWith(Graph g) {
        try {
            lock().readLock().lock();
            return get().isIsomorphicWith(g);
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public boolean contains(Node s, Node p, Node o) {
        try {
            lock().readLock().lock();
            return get().contains(s, p, o);
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public boolean contains(Triple t) {
        try {
            lock().readLock().lock();
            return get().contains(t);
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public void clear() {
        try {
            lock().writeLock().lock();
            get().clear();
        } finally {
            lock().writeLock().unlock();
        }
    }

    @Override
    public void remove(Node s, Node p, Node o) {
        try {
            lock().writeLock().lock();
            get().remove(s, p, o);
        } finally {
            lock().writeLock().unlock();
        }
    }

    @Override
    public void close() {
        try {
            lock().writeLock().lock();
            get().close();
        } finally {
            lock().writeLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            lock().readLock().lock();
            return get().isEmpty();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public int size() {
        try {
            lock().readLock().lock();
            return get().size();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public boolean isClosed() {
        try {
            lock().readLock().lock();
            return get().isClosed();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public TransactionHandler getTransactionHandler() {
        try {
            lock().readLock().lock();
            return get().getTransactionHandler();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public Capabilities getCapabilities() {
        try {
            lock().readLock().lock();
            return get().getCapabilities();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public GraphEventManager getEventManager() {
        try {
            lock().readLock().lock();
            return get().getEventManager();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public GraphStatisticsHandler getStatisticsHandler() {
        try {
            lock().readLock().lock();
            return get().getStatisticsHandler();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public PrefixMapping getPrefixMapping() {
        try {
            lock().readLock().lock();
            return get().getPrefixMapping();
        } finally {
            lock().readLock().unlock();
        }
    }
}
