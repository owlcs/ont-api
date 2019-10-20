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

import javax.annotation.Nullable;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * An implementation of {@link ReadWriteLock} with no-operation behaviour.
 * Singleton.
 * <p>
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 13/04/15
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/concurrent/NoOpReadWriteLock.java'>uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock</a>
 */
@SuppressWarnings({"NullableProblems", "WeakerAccess"})
public final class NoOpReadWriteLock implements ReadWriteLock, Serializable {
    public static final Condition NO_OP_CONDITION = new NoOpCondition();
    public static final Lock NO_OP_LOCK = new NoOpLock();
    public static final ReadWriteLock NO_OP_RW_LOCK = new NoOpReadWriteLock();

    /**
     * Singleton: it is hard to image someone would want to override this class.
     */
    private NoOpReadWriteLock() {
    }

    /**
     * Answers {@code true} if the given {@link ReadWriteLock R/W Lock} can be used in a concurrent environment.
     * Note that the native {@code OWL-API-impl} is not in the project dependencies and, therefore,
     * is not taken into account, so this method will return {@code true}
     * even if the lock is {@code uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock}.
     *
     * @param lock {@link ReadWriteLock} to test
     * @return boolean
     */
    public static boolean isConcurrent(ReadWriteLock lock) {
        return null != lock && NO_OP_RW_LOCK != lock;
    }

    @Override
    public Lock readLock() {
        return NO_OP_LOCK;
    }

    @Override
    public Lock writeLock() {
        return NO_OP_LOCK;
    }

    private Object readResolve() throws ObjectStreamException {
        return NO_OP_RW_LOCK;
    }

    /**
     * A fake implementation of {@link Lock}.
     * Matthew Horridge Stanford Center for Biomedical Informatics Research 13/04/15
     */
    private static class NoOpLock implements Lock, Serializable {
        private NoOpLock() {
        }

        @Override
        public void lock() {
        }

        @Override
        public void lockInterruptibly() {
        }

        @Override
        public boolean tryLock() {
            return true;
        }

        @Override
        public boolean tryLock(long time, @Nullable TimeUnit unit) {
            return true;
        }

        @Override
        public void unlock() {
        }

        @Override
        public Condition newCondition() {
            return NO_OP_CONDITION;
        }
    }

    /**
     * A fake implementation of {@link Condition}.
     */
    private static class NoOpCondition implements Condition, Serializable {
        private NoOpCondition() {
        }

        @Override
        public void await() {
        }

        @Override
        public void awaitUninterruptibly() {
        }

        @Override
        public long awaitNanos(long nanosTimeout) {
            return 0;
        }

        @Override
        public boolean await(long time, @Nullable TimeUnit unit) {
            return true;
        }

        @Override
        public boolean awaitUntil(@Nullable Date deadline) {
            return true;
        }

        @Override
        public void signal() {
        }

        @Override
        public void signalAll() {
        }
    }
}
