/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.owlapi;

import org.semanticweb.owlapi.model.PriorityCollectionSorting;
import org.semanticweb.owlapi.util.PriorityCollection;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * TODO: rename not to be confused with uk.ac.manchester.cs.owl.owlapi.*
 * A priority collection that supports concurrent reading and writing through a {@link ReadWriteLock}.
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 09/04/15
 * @param <T> type in the collection
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/ConcurrentPriorityCollection.java'>ConcurrentPriorityCollection</a>
 */
@SuppressWarnings("WeakerAccess")
public class ConcurrentPriorityCollection<T extends Serializable> extends PriorityCollection<T> {

    protected final ReadWriteLock lock;

    /**
     * Constructs a PriorityCollection instance with the specified {@link ReadWriteLock}.
     *
     * @param lock {@link java.util.concurrent.locks.ReadWriteLock ReadWriteLock} that should be used for locking
     * @param sorting sorting criterion
     */
    public ConcurrentPriorityCollection(ReadWriteLock lock, PriorityCollectionSorting sorting) {
        super(sorting);
        this.lock = Objects.requireNonNull(lock);
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

    @SuppressWarnings("NullableProblems")
    @Override
    public void set(Iterable<T> iterable) {
        lock.writeLock().lock();
        try {
            super.set(iterable);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void set(Set<T> set) {
        lock.writeLock().lock();
        try {
            super.set(set);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SuppressWarnings({"unchecked", "NullableProblems"})
    @Override
    public void set(T... array) {
        lock.writeLock().lock();
        try {
            super.set(array);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void add(Iterable<T> iterable) {
        lock.writeLock().lock();
        try {
            super.add(iterable);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void add(T... array) {
        lock.writeLock().lock();
        try {
            super.add(array);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void add(T c) {
        lock.writeLock().lock();
        try {
            super.add(c);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void remove(T... c) {
        lock.writeLock().lock();
        try {
            super.remove(c);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void remove(T c) {
        lock.writeLock().lock();
        try {
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

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<T> iterator() {
        return copyIterable().iterator();
    }

    @Override
    public PriorityCollection<T> getByMIMEType(String mimeType) {
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

    protected Iterable<T> copyIterable() {
        lock.readLock().lock();
        try {
            List<T> res = new ArrayList<>();
            super.iterator().forEachRemaining(res::add);
            return res;
        } finally {
            lock.readLock().unlock();
        }
    }

}
