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

package uk.ac.manchester.cs.owl.owlapi.concurrent;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Matthew Horridge Stanford Center for Biomedical Informatics Research 13/04/15
 */
class NoOpLock implements Lock, Serializable {

    public static final NoOpCondition NO_OP_CONDITION = new NoOpCondition();

    @Override
    public void lock() {
        // nothing to do
    }

    @Override
    public void lockInterruptibly() {
        // nothing to do
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
        // nothing to do
    }

    @Override
    public Condition newCondition() {
        return NO_OP_CONDITION;
    }

    private static class NoOpCondition implements Condition, Serializable {

        public NoOpCondition() {
            // nothing to do
        }

        @Override
        public void await() {
            // nothing to do
        }

        @Override
        public void awaitUninterruptibly() {
            // nothing to do
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
            // nothing to do
        }

        @Override
        public void signalAll() {
            // nothing to do
        }
    }
}
