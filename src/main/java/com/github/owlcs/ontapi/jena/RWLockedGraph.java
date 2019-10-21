/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.jena;

import org.apache.jena.graph.*;
import org.apache.jena.graph.impl.SimpleEventManager;
import org.apache.jena.graph.impl.SimpleTransactionHandler;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.shared.AddDeniedException;
import org.apache.jena.shared.DeleteDeniedException;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shared.impl.PrefixMappingImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.FilterIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import com.github.owlcs.ontapi.jena.utils.Graphs;
import com.github.owlcs.ontapi.jena.utils.Iter;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A {@code Graph} Wrapper with {@link ReadWriteLock} inside (that is the OWL-API synchronization style).
 * There is also an additional other type of synchronization that ensures thread-safety of using iterators.
 * This is necessary since {@link Iterator iterator}s are lazy in nature.
 * <p>
 * Note: the current implementation may temporarily put the iterator data in memory,
 * for more details see the {@link #find(Triple)} description.
 * <p>
 * Also note: although this {@code Graph} is expected to be thread safe,
 * it does not mean that a model, that wraps this graph, is also will be safe in multithreading:
 * it is possible to receive the data that will be invalid in the next moment,
 * which may lead to an exception during the construction of the final {@link org.apache.jena.rdf.model.Resource}.
 * For example, constructing an {@link com.github.owlcs.ontapi.jena.model.OntClass OWL Class} may be failed,
 * since the root triple {@code _:x rdf:type owl:Class} may be deleted by another thread
 * immediately after retrieving it.
 * <p>
 * Note: currently this {@code Graph} does not support transactions.
 * I.e. the method {@link TransactionHandler#transactionsSupported()} returns {@code false}.
 * <p>
 * Created by @szuev on 07.04.2017.
 */
@SuppressWarnings("WeakerAccess")
public class RWLockedGraph implements Graph {
    /**
     * The base {@link Graph}.
     */
    protected final Graph base;
    /**
     * {@link ReadWriteLock} to ensure thread-safety.
     */
    protected final ReadWriteLock lock;
    /**
     * The delay in milliseconds, that is used to select {@link ExtendedIterator} processing strategy.
     */
    protected final long delay;
    /**
     * The {@code Collection} of all {@link ExtendedIterator}, as {@link WeakHashMap},
     * since it is possible that some iterator was simply forgotten.
     */
    protected final Collection<WIT<?>> iterators = Collections.newSetFromMap(new WeakHashMap<>());
    /**
     * {@link GraphEventManager}, cannot be {@code null}
     */
    protected final GraphEventManager gem;

    /**
     * Constructs a new {@link RWLockedGraph Read/Write Locked Graph Wrapper}
     * with {@link SimpleEventManager} and {@link #delay delay} equaled to {@code 500}ms.
     *
     * @param base {@link Graph}, not {@code null}
     * @param lock {@link ReadWriteLock}, not {@code null}
     * @throws RuntimeException if any input parameter is wrong
     */
    public RWLockedGraph(Graph base, ReadWriteLock lock) {
        this(base, lock, 500);
    }

    /**
     * Constructs a new {@link RWLockedGraph Read/Write Locked Graph Wrapper} with {@link SimpleEventManager},
     * that currently (at the time of writing the jena version is <b>3.10.0</b>)
     * uses {@link java.util.concurrent.CopyOnWriteArrayList}
     * and, therefore, seems to be thread safe, at least if it is used only to register/unregister listeners.
     *
     * @param base                {@link Graph}, not {@code null}
     * @param lock                {@link ReadWriteLock}, not {@code null}
     * @param delayInMilliseconds long, positive number
     * @throws RuntimeException if any input parameter is wrong
     */
    public RWLockedGraph(Graph base, ReadWriteLock lock, long delayInMilliseconds) {
        this(base, lock, delayInMilliseconds, new SimpleEventManager());
    }

    /**
     * The base constructor.
     *
     * @param base                {@link Graph}, not {@code null}
     * @param lock                {@link ReadWriteLock}, not {@code null}
     * @param delayInMilliseconds long, positive number
     * @param gem                 {@link GraphEventManager}, not {@code null}
     * @throws RuntimeException if any input parameter is wrong
     */
    protected RWLockedGraph(Graph base, ReadWriteLock lock, long delayInMilliseconds, GraphEventManager gem) {
        this.base = Objects.requireNonNull(base, "Null base graph");
        this.lock = Objects.requireNonNull(lock, "Null lock");
        this.gem = Objects.requireNonNull(gem, "Null event manager");
        if (delayInMilliseconds <= 0)
            throw new IllegalArgumentException("Non-positive delay specified.");
        this.delay = delayInMilliseconds;
    }

    /**
     * Returns the current time in milliseconds to use as timestamp inside {@link WIT}-iterators.
     *
     * @return long
     */
    public static long currentTimeInMilliSeconds() {
        return System.currentTimeMillis();
    }

    public Graph get() {
        return base;
    }

    public ReadWriteLock lock() {
        return lock;
    }

    @Override
    public void add(Triple t) throws AddDeniedException {
        lock.writeLock().lock();
        try {
            if (!base.getCapabilities().addAllowed())
                throw new AddDeniedException("Attempt to add triple " + t);
            waitForEmptyIterators();
            base.add(t);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void delete(Triple t) throws DeleteDeniedException {
        lock.writeLock().lock();
        try {
            if (!base.getCapabilities().deleteAllowed())
                throw new DeleteDeniedException("Attempt to delete triple " + t);
            waitForEmptyIterators();
            base.delete(t);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(Node s, Node p, Node o) {
        lock.writeLock().lock();
        try {
            if (!base.getCapabilities().deleteAllowed())
                throw new DeleteDeniedException("Attempt to remove triple " + Triple.createMatch(s, p, o));
            waitForEmptyIterators();
            base.remove(s, p, o);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            if (!base.getCapabilities().deleteAllowed())
                throw new DeleteDeniedException("Attempt to clear");
            waitForEmptyIterators();
            base.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns an {@link ExtendedIterator extended iterator} over all the {@link Triple}s that match the triple pattern.
     * <p>
     * The return iterator is expected to be thread safe: if graph changes have occurred before the end of iteration
     * it may return data from the past using their internal snapshot.
     * In single-thread environment its improper use may lead to {@link ConcurrentModificationException}.
     * To avoid this, {@link org.apache.jena.util.iterator.ClosableIterator#close() close} the iterator explicitly
     * if it is not exhausted and don't mix mutation operations with partial iteration.
     *
     * @param m a {@link Triple triple} encoding the pattern to look for, not {@code null}
     * @return {@link ExtendedIterator} of all {@link Triple triple}s in this graph that match {@code m}
     * @see #find(Triple)
     */
    @Override
    public ExtendedIterator<Triple> find(Triple m) {
        lock.readLock().lock();
        try {
            return new WIT<>(base.find(m));
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns an iterator over Triples matching a pattern.
     * For more details see the {@link #find(Triple)} description.
     *
     * @param s {@link Node}, uri or blank, not {@code null}
     * @param p {@link Node}, uri, not {@code null}
     * @param o {@link Node}, uri, blank or literal, not {@code null}
     * @return {@link ExtendedIterator} of {@link Triple}s
     * @see #find(Node, Node, Node)
     */
    @Override
    public ExtendedIterator<Triple> find(Node s, Node p, Node o) {
        lock.readLock().lock();
        try {
            return new WIT<>(base.find(s, p, o));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean contains(Node s, Node p, Node o) {
        return withReadLock(() -> base.contains(s, p, o));
    }

    @Override
    public boolean contains(Triple t) {
        return withReadLock(() -> base.contains(t));
    }

    @Override
    public boolean isIsomorphicWith(Graph other) {
        lock.readLock().lock();
        try {
            if (this == other) return true;
            if (other instanceof RWLockedGraph) {
                other = ((RWLockedGraph) other).get();
            }
            return base.isIsomorphicWith(other);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean dependsOn(Graph other) {
        lock.readLock().lock();
        try {
            if (this == other) return true;
            if (other instanceof RWLockedGraph) {
                other = ((RWLockedGraph) other).get();
            }
            return Graphs.dependsOn(base, other);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void close() {
        lock.writeLock().lock();
        try {
            base.close();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            if (base instanceof GraphMem)
                return base.isEmpty();
            return !Iter.findFirst(find()).isPresent();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        return withReadLock(base::size);
    }

    @Override
    public boolean isClosed() {
        return withReadLock(base::isClosed);
    }

    @Override
    public Capabilities getCapabilities() {
        return base.getCapabilities();
    }

    @Override
    public TransactionHandler getTransactionHandler() {
        return new SimpleTransactionHandler();
    }

    @Override
    public GraphEventManager getEventManager() {
        return gem;
    }

    @Override
    public GraphStatisticsHandler getStatisticsHandler() {
        return (s, p, o) -> withReadLock(() -> base.getStatisticsHandler().getStatistic(s, p, o));
    }

    @Override
    public PrefixMapping getPrefixMapping() {
        return new WPM();
    }

    protected <X> X withWriteLock(Supplier<X> op) {
        lock.writeLock().lock();
        try {
            return op.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected <X> X withReadLock(Supplier<X> op) {
        lock.readLock().lock();
        try {
            return op.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Waits for all other (that are created by other threads) iterators.
     * After operation is finished, all iterators must be either done or cached in the from of snapshot.
     * The criteria to choose strategy is {@link WIT#timestamp},
     * if no one has worked with it for more than {@link #delay} milliseconds,
     * then all its elements will be put in memory using the {@link #makeIteratorSnapshot(WIT)} method.
     * Testing shows that only about {@code 1%} of iterators fall into memory,
     * but this number may vary depending on the environment.
     */
    protected void waitForEmptyIterators() {
        Collection<WIT<?>> iterators;
        while (!(iterators = findOtherIterators()).isEmpty()) {
            selectOldestIterator(iterators).ifPresent(this::makeIteratorSnapshot);
        }
    }

    /**
     * Makes an in-memory snapshot for the given {@link WIT}-iterator.
     *
     * @param it {@link WIT}
     */
    @SuppressWarnings("unchecked")
    protected void makeIteratorSnapshot(WIT<?> it) {
        ExtendedIterator base = it.setBase(null);
        ArrayList res = new ArrayList();
        while (base.hasNext()) {
            res.add(base.next());
        }
        res.trimToSize();
        it.setBase(Iter.create(res.iterator()));
        removeIterator(it);
    }

    /**
     * Selects the oldest iterator.
     *
     * @param iterators {@link Collection}
     * @return {@code Optional} around {@link WIT}s
     * @see #delay
     */
    protected Optional<WIT<?>> selectOldestIterator(Collection<WIT<?>> iterators) {
        return iterators.stream()
                .filter(WIT::isExpired)
                .min(Comparator.comparingLong(WIT::timestamp));
    }

    /**
     * Returns iterators that were created by other threads.
     *
     * @return {@code Collection} of {@link WIT}s
     */
    protected Collection<WIT<?>> findOtherIterators() {
        synchronized (iterators) {
            if (iterators.isEmpty()) return Collections.emptySet();
            Thread th = Thread.currentThread();
            return iterators.stream().filter(x -> th != x.thread).collect(Collectors.toSet());
        }
    }

    /**
     * Registers the given iterator into the internal {@code Map}-store.
     *
     * @param it {@link WIT}
     */
    protected void putIterator(WIT<?> it) {
        synchronized (iterators) {
            iterators.add(it);
        }
    }

    /**
     * Unregisters the given iterator from the internal {@code Map}-store.
     *
     * @param it {@link WIT}
     */
    protected void removeIterator(WIT<?> it) {
        synchronized (iterators) {
            iterators.remove(it);
        }
    }

    /**
     * A {@code WrappedIterator} with timestamp and possibility to change the base iterator.
     *
     * @param <X> anything
     * @since 1.4.0
     */
    public class WIT<X> extends WrappedIterator<X> {
        protected final Thread thread;
        protected volatile ExtendedIterator<X> base;
        protected long timestamp;

        protected WIT(ExtendedIterator<X> base) {
            super(Objects.requireNonNull(base));
            this.base = base;
            this.thread = Thread.currentThread();
            this.timestamp = currentTimeInMilliSeconds();
            putIterator(this);
        }

        protected ExtendedIterator<X> base() {
            while (base == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new IllegalStateException("Unexpected interruption", e);
                }
            }
            return base;
        }

        protected ExtendedIterator<X> setBase(ExtendedIterator<X> base) {
            ExtendedIterator<X> res = this.base;
            this.base = base;
            return res;
        }

        protected void refreshTimestamp() {
            this.timestamp = currentTimeInMilliSeconds();
        }

        protected long timestamp() {
            return timestamp;
        }

        protected boolean isExpired() {
            return currentTimeInMilliSeconds() - timestamp > delay;
        }

        protected void deleteFromCollection() {
            removeIterator(this);
        }

        @Override
        public boolean hasNext() {
            refreshTimestamp();
            try {
                boolean res = base().hasNext();
                if (!res) {
                    deleteFromCollection();
                }
                return res;
            } catch (Exception e) {
                deleteFromCollection();
                throw e;
            }
        }

        @Override
        public X next() {
            refreshTimestamp();
            try {
                return base().next();
            } catch (Exception e) {
                deleteFromCollection();
                throw e;
            }
        }

        @Override
        public void remove() {
            refreshTimestamp();
            base().remove();
        }

        @Override
        public X removeNext() {
            X res = next();
            remove();
            return res;
        }

        @Override
        public <X1 extends X> ExtendedIterator<X> andThen(Iterator<X1> other) {
            refreshTimestamp();
            return super.andThen(other);
        }

        @Override
        public FilterIterator<X> filterKeep(Predicate<X> test) {
            refreshTimestamp();
            return super.filterKeep(test);
        }

        @Override
        public FilterIterator<X> filterDrop(Predicate<X> test) {
            refreshTimestamp();
            return super.filterDrop(test);
        }

        @Override
        public <U> ExtendedIterator<U> mapWith(Function<X, U> map) {
            refreshTimestamp();
            return super.mapWith(map);
        }

        @Override
        public List<X> toList() {
            refreshTimestamp();
            try {
                return asList(this);
            } finally {
                deleteFromCollection();
            }
        }

        @Override
        public Set<X> toSet() {
            refreshTimestamp();
            try {
                return asSet(this);
            } finally {
                deleteFromCollection();
            }
        }

        @Override
        public void close() {
            refreshTimestamp();
            try {
                close(base());
            } finally {
                deleteFromCollection();
            }
        }

        @Override
        public void forEachRemaining(Consumer<? super X> action) {
            refreshTimestamp();
            Objects.requireNonNull(action);
            try {
                while (hasNext())
                    action.accept(next());
            } finally {
                deleteFromCollection();
            }
        }
    }

    /**
     * A wrapper for {@link PrefixMapping}.
     *
     * @since 1.4.0
     */
    protected class WPM implements PrefixMapping {
        private final PrefixMapping pm;

        protected WPM() {
            this.pm = Objects.requireNonNull(base.getPrefixMapping());
        }

        @Override
        public PrefixMapping setNsPrefix(String prefix, String uri) {
            return withWriteLock(() -> pm.setNsPrefix(prefix, uri));
        }

        @Override
        public PrefixMapping removeNsPrefix(String prefix) {
            return withWriteLock(() -> pm.removeNsPrefix(prefix));
        }

        @Override
        public PrefixMapping clearNsPrefixMap() {
            return withWriteLock(pm::clearNsPrefixMap);
        }

        @Override
        public PrefixMapping setNsPrefixes(PrefixMapping other) {
            return withWriteLock(() -> pm.setNsPrefixes(other));
        }

        @Override
        public PrefixMapping setNsPrefixes(Map<String, String> map) {
            return withWriteLock(() -> pm.setNsPrefixes(map));
        }

        @Override
        public PrefixMapping withDefaultMappings(PrefixMapping map) {
            return withWriteLock(() -> pm.withDefaultMappings(map));
        }

        @Override
        public PrefixMapping lock() {
            return withWriteLock(pm::lock);
        }

        @Override
        public String getNsPrefixURI(String prefix) {
            return withReadLock(() -> pm.getNsPrefixURI(prefix));
        }

        @Override
        public String getNsURIPrefix(String uri) {
            return withReadLock(() -> pm.getNsURIPrefix(uri));
        }

        @Override
        public Map<String, String> getNsPrefixMap() {
            return withReadLock(pm::getNsPrefixMap);
        }

        @Override
        public String expandPrefix(String prefixed) {
            return withReadLock(() -> pm.expandPrefix(prefixed));
        }

        @Override
        public String shortForm(String uri) {
            return withReadLock(() -> pm.shortForm(uri));
        }

        @Override
        public String qnameFor(String uri) {
            return withReadLock(() -> pm.qnameFor(uri));
        }

        @Override
        public int numPrefixes() {
            return withReadLock(pm::numPrefixes);
        }

        @Override
        public boolean samePrefixMappingAs(PrefixMapping other) {
            return withReadLock(() -> pm.samePrefixMappingAs(other));
        }

        @Override
        public String toString() {
            if (pm instanceof PrefixMappingImpl)
                return withReadLock(pm::toString);
            return super.toString();
        }
    }

}
