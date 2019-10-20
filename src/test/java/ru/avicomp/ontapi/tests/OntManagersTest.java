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

package ru.avicomp.ontapi.tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * To test {@link OntManagers}.
 * Created by @ssz on 11.09.2018.
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/contract/src/test/java/org/semanticweb/owlapi/api/test/OWLManagerTestCase.java'>org.semanticweb.owlapi.api.test.OWLManagerTestCase</a>
 * @see OntManagers
 */
@RunWith(Parameterized.class)
public class OntManagersTest {

    private final TestProfile data;

    public OntManagersTest(TestProfile data) {
        this.data = data;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<TestProfile> getData() {
        return Arrays.asList(new ONTStandard(), new ONTConcurrent(), new OWLStandard(), new OWLConcurrent());
    }

    @Test
    public void testManager() {
        OWLOntologyManager m = data.createManager();
        Assert.assertSame(data.getManagerImplType(), m.getClass());
        OWLDataFactory df = m.getOWLDataFactory();
        Assert.assertSame(data.getDataFactoryImplType(), df.getClass());
        OWLOntologyFactory of = m.getOntologyFactories().iterator().next();
        Assert.assertNotNull(of);
        Assert.assertSame(data.getOntologyFactoryImplType(), of.getClass());
        Assert.assertEquals(data.getNumberOfStorers(), m.getOntologyStorers().size());
        Assert.assertEquals(data.getNumberOfParsers(), m.getOntologyParsers().size());
    }

    @Test
    public void testOntology() throws Exception {
        OWLOntologyManager m = data.createManager();
        OWLOntology o = m.createOntology();
        Assert.assertSame(data.getOntologyImplType(), o.getClass());
        Assert.assertSame(m, o.getOWLOntologyManager());
        Assert.assertNotEquals(o, m.createOntology());
    }

    @Test
    public void testShareLock() throws Exception {
        OWLOntologyManager m = data.createManager();
        OWLOntology o = m.createOntology();
        Assert.assertSame(data.getReadLock(o), data.getReadLock(m));
        Assert.assertSame(data.getWriteLock(o), data.getWriteLock(m));
        OWLOntology b = m.createOntology(IRI.create("X"));
        Assert.assertSame(data.getReadLock(b), data.getReadLock(m));
        Assert.assertSame(data.getWriteLock(b), data.getWriteLock(m));
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Class<T> fieldType,
                                  String fieldName,
                                  Class<?> holderClass,
                                  Object holderInstance) {
        AssertionError error = new AssertionError("Class " + holderClass.getName() + ": can't find field " + fieldName);
        Field res = null;
        Class<?> clazz = holderClass;
        while (clazz != null) {
            try {
                res = clazz.getDeclaredField(fieldName);
                break;
            } catch (NoSuchFieldException ex) {
                error.addSuppressed(ex);
                clazz = clazz.getSuperclass();
            }
        }
        if (res == null) throw error;
        if (!fieldType.isAssignableFrom(res.getType())) {
            throw new AssertionError(holderClass.getName() + "::" + res + " is not subtype of " + fieldType.getName());
        }
        res.setAccessible(true);
        try {
            return (T) res.get(holderInstance);
        } catch (IllegalAccessException | ClassCastException e) {
            throw new AssertionError(holderClass.getName() + "::" + e, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> findClass(String name) {
        try {
            return (Class<T>) Class.forName(name);
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new AssertionError(e);
        }
    }

    private static abstract class TestProfile {
        abstract OWLOntologyManager createManager();

        abstract Class<? extends OWLOntologyManager> getManagerImplType();

        abstract Class<? extends OWLOntology> getOntologyImplType();

        abstract Class<? extends OWLDataFactory> getDataFactoryImplType();

        abstract Class<? extends OWLOntologyFactory> getOntologyFactoryImplType();

        abstract Lock getReadLock(OWLOntologyManager m);

        abstract Lock getWriteLock(OWLOntologyManager m);

        abstract Lock getReadLock(OWLOntology o);

        abstract Lock getWriteLock(OWLOntology o);

        int getNumberOfStorers() {
            return 20;
        }

        int getNumberOfParsers() {
            return 19;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    private static abstract class OWLProfile extends TestProfile {
        @Override
        public Class<? extends OWLOntologyManager> getManagerImplType() {
            return findClass("uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl");
        }

        @Override
        public Class<? extends OWLOntology> getOntologyImplType() {
            return findClass("uk.ac.manchester.cs.owl.owlapi.concurrent.ConcurrentOWLOntologyImpl");
        }

        @Override
        Class<? extends OWLDataFactory> getDataFactoryImplType() {
            return findClass("uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl");
        }

        @Override
        Class<? extends OWLOntologyFactory> getOntologyFactoryImplType() {
            return findClass("uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl");
        }

        @Override
        public Lock getReadLock(OWLOntologyManager m) {
            return getField(Lock.class, "readLock", getManagerImplType(), m);
        }

        @Override
        public Lock getWriteLock(OWLOntologyManager m) {
            return getField(Lock.class, "writeLock", getManagerImplType(), m);
        }

        @Override
        public Lock getReadLock(OWLOntology o) {
            return getField(Lock.class, "readLock", getOntologyImplType(), o);
        }

        @Override
        public Lock getWriteLock(OWLOntology o) {
            return getField(Lock.class, "writeLock", getOntologyImplType(), o);
        }
    }

    private static abstract class ONTProfile extends TestProfile {
        @Override
        public Class<? extends OWLOntologyManager> getManagerImplType() {
            return OntologyManagerImpl.class;
        }

        @Override
        Class<? extends OWLDataFactory> getDataFactoryImplType() {
            return DataFactoryImpl.class;
        }

        @Override
        Class<? extends OWLOntologyFactory> getOntologyFactoryImplType() {
            return OntologyFactoryImpl.class;
        }

        @Override
        public Lock getReadLock(OWLOntologyManager m) {
            return getField(ReadWriteLock.class, "lock", getManagerImplType(), m).readLock();
        }

        @Override
        public Lock getWriteLock(OWLOntologyManager m) {
            return getField(ReadWriteLock.class, "lock", getManagerImplType(), m).writeLock();
        }
    }

    private static class ONTStandard extends ONTProfile {
        @Override
        public OWLOntologyManager createManager() {
            return OntManagers.createONT();
        }

        @Override
        public Class<? extends OWLOntology> getOntologyImplType() {
            return OntologyModelImpl.class;
        }

        @Override
        public Lock getReadLock(OWLOntology o) {
            return NoOpReadWriteLock.NO_OP_LOCK;
        }

        @Override
        public Lock getWriteLock(OWLOntology o) {
            return NoOpReadWriteLock.NO_OP_LOCK;
        }
    }

    private static class ONTConcurrent extends ONTProfile {
        @Override
        public OWLOntologyManager createManager() {
            return OntManagers.createConcurrentONT();
        }

        @Override
        public Class<? extends OWLOntology> getOntologyImplType() {
            return OntologyModelImpl.Concurrent.class;
        }

        @Override
        public Lock getReadLock(OWLOntology o) {
            return getField(ReadWriteLock.class, "lock", getOntologyImplType(), o).readLock();
        }

        @Override
        public Lock getWriteLock(OWLOntology o) {
            return getField(ReadWriteLock.class, "lock", getOntologyImplType(), o).writeLock();
        }
    }

    private static class OWLStandard extends OWLProfile {

        @Override
        public Class<? extends OWLOntology> getOntologyImplType() {
            return findClass("uk.ac.manchester.cs.owl.owlapi.OWLOntologyImpl");
        }

        @Override
        public Lock getReadLock(OWLOntology o) {
            return NoOpReadWriteLock.NO_OP_LOCK;
        }

        @Override
        public Lock getWriteLock(OWLOntology o) {
            return NoOpReadWriteLock.NO_OP_LOCK;
        }

        @Override
        public OWLOntologyManager createManager() {
            return OntManagers.createOWL();
        }
    }

    private static class OWLConcurrent extends OWLProfile {

        @Override
        public OWLOntologyManager createManager() {
            return OntManagers.createConcurrentOWL();
        }

        @Override
        public Lock getReadLock(OWLOntology o) {
            return getField(ReadWriteLock.class, "lock", getOntologyImplType(), o).readLock();
        }

        @Override
        public Lock getWriteLock(OWLOntology o) {
            return getField(ReadWriteLock.class, "lock", getOntologyImplType(), o).writeLock();
        }
    }

}
