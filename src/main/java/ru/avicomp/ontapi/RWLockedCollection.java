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

package ru.avicomp.ontapi;

import org.semanticweb.owlapi.model.PriorityCollectionSorting;
import org.semanticweb.owlapi.util.PriorityCollection;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An extended {@link PriorityCollection OWL-API Priority Collection}
 * that supports concurrent reading and writing through a {@link ReadWriteLock}.
 * <p>
 * Is there any reason why the base class does not implement {@link java.util.Collection}?
 * Unfortunately I can't do that in this implementation: clashes with method {@link #remove(Serializable)}.
 * Also I am wondering why {@code PriorityCollection} is a concrete class, not interface. It is very annoying.
 * <p>
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 09/04/15
 *
 * @param <E> type in the collection
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/ConcurrentPriorityCollection.java'>ConcurrentPriorityCollection</a>
 */
@SuppressWarnings("WeakerAccess")
public class RWLockedCollection<E extends Serializable> extends PriorityCollection<E> {

    protected final ReadWriteLock lock;

    /**
     * Constructs a PropertyCollection without any sorting.
     *
     * @param lock {@link ReadWriteLock} instance, null for no concurrent access
     * @see PriorityCollectionSorting#NEVER
     * @since 1.3.0
     */
    public RWLockedCollection(ReadWriteLock lock) {
        this(lock, PriorityCollectionSorting.NEVER);
    }

    /**
     * Constructs a PriorityCollection instance with the specified {@link ReadWriteLock} and {@link PriorityCollectionSorting}.
     *
     * @param lock    {@link ReadWriteLock ReadWriteLock} that should be used for locking, null for no-op
     * @param sorting {@link PriorityCollectionSorting} the enum, which (as passed as parameter) makes hard to use OWL-API interfaces.
     *                (Do you know why in java (e.g. in NIO) there is interfaces passed as parameters (implemented by enum), not naked enums?)
     */
    public RWLockedCollection(ReadWriteLock lock, PriorityCollectionSorting sorting) {
        super(sorting);
        this.lock = lock == null ? NoOpReadWriteLock.NO_OP_RW_LOCK : lock;
    }

    @Override
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return super.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        lock.readLock().lock();
        try {
            return super.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void set(@Nonnull Iterable<E> iterable) {
        lock.writeLock().lock();
        try {
            onAdd(iterable);
            setIterable(iterable);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void set(Set<E> set) {
        lock.writeLock().lock();
        try {
            onAdd(set);
            super.set(set);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SafeVarargs
    @Override
    public final void set(@Nonnull E... array) {
        lock.writeLock().lock();
        try {
            onAdd(array);
            setIterable(Arrays.asList(array));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SafeVarargs
    @Override
    public final void add(E... array) {
        lock.writeLock().lock();
        try {
            onAdd(array);
            addIterable(Arrays.asList(array));
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void add(Iterable<E> iterable) {
        lock.writeLock().lock();
        try {
            onAdd(iterable);
            addIterable(iterable);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void add(@Nonnull E e) {
        lock.writeLock().lock();
        try {
            onAdd(e);
            super.add(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SafeVarargs
    @Override
    public final void remove(E... array) {
        lock.writeLock().lock();
        try {
            onDelete(Arrays.asList(array));
            super.remove(array);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void remove(@Nonnull E c) {
        lock.writeLock().lock();
        try {
            onDelete(c);
            super.remove(c);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            super.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns a {@code Stream} with this collection as its source.
     *
     * @return a {@code Stream} over the elements in this collection
     * @since 1.3.0
     */
    public Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    @Nonnull
    public Iterator<E> iterator() {
        return copyIterable().iterator();
    }

    @Override
    public Spliterator<E> spliterator() {
        return copyIterable().spliterator();
    }

    @Override
    public PriorityCollection<E> getByMIMEType(String mimeType) {
        lock.readLock().lock();
        try {
            return super.getByMIMEType(mimeType);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return super.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    protected List<E> copyIterable() {
        lock.readLock().lock();
        try {
            List<E> res = new ArrayList<>();
            super.iterator().forEachRemaining(res::add);
            return res;
        } finally {
            lock.readLock().unlock();
        }
    }

    protected void setIterable(Iterable<E> iterable) {
        super.set(iterable);
    }

    protected void addIterable(Iterable<E> iterable) {
        super.add(iterable);
    }

    @SafeVarargs
    protected final void onAdd(E... array) {
        onAdd(Arrays.asList(array));
    }

    protected void onAdd(Iterable<E> iterable) {
        // note: this causes a double iteration
        iterable.forEach(this::onAdd);
    }

    /**
     * Performs some action when element is added to this collection.
     *
     * @param e an element that is added
     */
    protected void onAdd(E e) {
    }

    protected void onDelete(Iterable<E> iterable) {
        iterable.forEach(this::onDelete);
    }

    /**
     * Performs some action when element is deleted from this collection.
     *
     * @param e an element that is deleted
     */
    protected void onDelete(E e) {
    }
}
