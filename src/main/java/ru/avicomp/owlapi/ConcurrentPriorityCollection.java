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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import static org.semanticweb.owlapi.util.OWLAPIPreconditions.verifyNotNull;

/**
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 09/04/15
 * A priority collection that supports concurrent reading and writing through a
 * {@link ReadWriteLock}
 *
 * @param <T> type in the collection
 */
public class ConcurrentPriorityCollection<T extends Serializable> extends PriorityCollection<T> {

    private final Lock readLock;
    private final Lock writeLock;

    /**
     * Constructs a {@link ConcurrentPriorityCollection} using the specified
     * {@link ReadWriteLock}
     *
     * @param readWriteLock The {@link java.util.concurrent.locks.ReadWriteLock} that should be used
     *                      for locking.
     * @param sorting       sorting criterion
     */
    public ConcurrentPriorityCollection(ReadWriteLock readWriteLock,
                                        PriorityCollectionSorting sorting) {
        super(sorting);
        verifyNotNull(readWriteLock);
        this.readLock = readWriteLock.readLock();
        this.writeLock = readWriteLock.writeLock();
    }

    @Override
    public boolean isEmpty() {
        readLock.lock();
        try {
            return super.isEmpty();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int size() {
        readLock.lock();
        try {
            return super.size();
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void set(Iterable<T> c) {
        writeLock.lock();
        try {
            super.set(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void add(Iterable<T> c) {
        writeLock.lock();
        try {
            super.add(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void set(@SuppressWarnings("unchecked") T... c) {
        writeLock.lock();
        try {
            super.set(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void add(@SuppressWarnings("unchecked") T... c) {
        writeLock.lock();
        try {
            super.add(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void add(T c) {
        writeLock.lock();
        try {
            super.add(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(@SuppressWarnings("unchecked") T... c) {
        writeLock.lock();
        try {
            super.remove(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(T c) {
        writeLock.lock();
        try {
            super.remove(c);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            super.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Iterator<T> iterator() {
        return copyIterable().iterator();
    }

    @Override
    public PriorityCollection<T> getByMIMEType(String mimeType) {
        readLock.lock();
        try {
            return super.getByMIMEType(mimeType);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public String toString() {
        readLock.lock();
        try {
            return super.toString();
        } finally {
            readLock.unlock();
        }
    }

    private Iterable<T> copyIterable() {
        readLock.lock();
        try {
            List<T> copy = new ArrayList<>();
            for (Iterator<T> it = super.iterator(); it.hasNext(); ) {
                T element = it.next();
                copy.add(element);
            }
            return copy;
        } finally {
            readLock.unlock();
        }
    }
}
