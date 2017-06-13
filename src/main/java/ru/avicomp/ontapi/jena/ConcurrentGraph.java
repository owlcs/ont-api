/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

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
        lock().readLock().lock();
        try {
            return get().dependsOn(other);
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public void add(Triple t) throws AddDeniedException {
        lock().writeLock().lock();
        try {
            get().add(t);
        } finally {
            lock().writeLock().unlock();
        }
    }

    @Override
    public void delete(Triple t) throws DeleteDeniedException {
        lock().writeLock().lock();
        try {
            get().delete(t);
        } finally {
            lock().writeLock().unlock();
        }
    }

    @Override
    public ExtendedIterator<Triple> find(Triple m) {
        lock().readLock().lock();
        try {
            return get().find(m);
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public ExtendedIterator<Triple> find(Node s, Node p, Node o) {
        lock().readLock().lock();
        try {
            return get().find(s, p, o);
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public boolean isIsomorphicWith(Graph g) {
        lock().readLock().lock();
        try {
            return get().isIsomorphicWith(g);
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public boolean contains(Node s, Node p, Node o) {
        lock().readLock().lock();
        try {
            return get().contains(s, p, o);
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public boolean contains(Triple t) {
        lock().readLock().lock();
        try {
            return get().contains(t);
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock().writeLock().lock();
        try {
            get().clear();
        } finally {
            lock().writeLock().unlock();
        }
    }

    @Override
    public void remove(Node s, Node p, Node o) {
        lock().writeLock().lock();
        try {
            get().remove(s, p, o);
        } finally {
            lock().writeLock().unlock();
        }
    }

    @Override
    public void close() {
        lock().writeLock().lock();
        try {
            get().close();
        } finally {
            lock().writeLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock().readLock().lock();
        try {
            return get().isEmpty();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock().readLock().lock();
        try {
            return get().size();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public boolean isClosed() {
        lock().readLock().lock();
        try {
            return get().isClosed();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public TransactionHandler getTransactionHandler() {
        lock().readLock().lock();
        try {
            return get().getTransactionHandler();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public Capabilities getCapabilities() {
        lock().readLock().lock();
        try {
            return get().getCapabilities();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public GraphEventManager getEventManager() {
        lock().readLock().lock();
        try {
            return get().getEventManager();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public GraphStatisticsHandler getStatisticsHandler() {
        lock().readLock().lock();
        try {
            return get().getStatisticsHandler();
        } finally {
            lock().readLock().unlock();
        }
    }

    @Override
    public PrefixMapping getPrefixMapping() {
        lock().readLock().lock();
        try {
            return get().getPrefixMapping();
        } finally {
            lock().readLock().unlock();
        }
    }
}
