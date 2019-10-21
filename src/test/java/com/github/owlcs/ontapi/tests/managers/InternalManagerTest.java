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

package com.github.owlcs.ontapi.tests.managers;

import org.apache.jena.graph.Node;
import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.model.HasOntologyID;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.owlcs.ontapi.NoOpReadWriteLock;
import com.github.owlcs.ontapi.OntologyCollection;
import com.github.owlcs.ontapi.OntologyCollectionImpl;
import com.github.owlcs.ontapi.OntologyID;
import com.github.owlcs.ontapi.utils.ReadWriteUtils;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * To test some internal managers components.
 * Created by @ssz on 09.12.2018.
 */
public class InternalManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalManagerTest.class);
    private static final PrintStream OUT = ReadWriteUtils.NULL_OUT;

    private static void performSomeModifying(OntologyCollection<IDHolder> list) {
        Random r = ThreadLocalRandom.current();
        addRandom(list, r, 5000);
        OUT.println(list);

        list.clear();
        addRandom(list, r, 1500);
        changeVersionIRIs(list);
        OUT.println(list);

        changeOntologyIRIs(list);
        list.keys().skip(r.nextInt((int) list.size() + 1)).collect(Collectors.toList()).forEach(list::remove);
        OUT.println(list);

        addRandom(list, r, 3000);
        changeVersionIRIs(list);
        changeOntologyIRIs(list);
        OUT.println(list);

        list.values().limit(r.nextInt((int) list.size() + 1)).collect(Collectors.toSet()).forEach(list::delete);
        OUT.println(list);
        list.clear();
    }

    private static void addRandom(OntologyCollection<IDHolder> list, Random r, int count) {
        IntStream.rangeClosed(0, r.nextInt(count) + 1)
                .mapToObj(i -> IDHolder.of(String.valueOf(i)))
                .forEach(list::add);
    }

    private static void changeOntologyIRIs(OntologyCollection<IDHolder> list) {
        list.values().forEach(o -> {
            String iri = o.getOntologyIRI();
            Assert.assertNotNull(iri);
            o.setOntologyIRI(iri + "_x");
        });
    }

    private static void changeVersionIRIs(OntologyCollection<IDHolder> list) {
        list.values().forEach(o -> {
            String ver = o.getVersionIRI();
            if (ver == null) {
                ver = "y";
            } else {
                ver += "_y";
            }
            o.setVersionIRI(ver);
        });
    }

    @Test
    public void testCommonOntologyCollection() {
        OntologyCollection<IDHolder> list1 = new OntologyCollectionImpl<>();
        Assert.assertTrue(list1.isEmpty());
        Assert.assertEquals(0, list1.size());

        IDHolder a = IDHolder.of("a");
        IDHolder b = IDHolder.of("b");
        list1.add(a).add(b);
        LOGGER.debug("1) List: {}", list1);
        Assert.assertEquals(2, list1.size());
        Assert.assertFalse(list1.isEmpty());
        Assert.assertTrue(list1.get(OntologyID.create("a", null)).isPresent());
        Assert.assertFalse(list1.get(OntologyID.create("a", "v")).isPresent());
        Assert.assertTrue(list1.contains(b.getOntologyID()));
        Assert.assertFalse(list1.contains(OntologyID.create("a", "v")));

        // change id externally for 'b':
        b.setOntologyID(IDHolder.of("x").getOntologyID());
        LOGGER.debug("2) List: {}", list1);
        list1.delete(b).remove(a.getOntologyID());
        Assert.assertEquals(0, list1.size());
        Assert.assertTrue(list1.isEmpty());

        OntologyCollection<IDHolder> list2 = new OntologyCollectionImpl<>(NoOpReadWriteLock.NO_OP_RW_LOCK,
                Arrays.asList(a, b));
        LOGGER.debug("3) List: {}", list2);
        Assert.assertEquals(2, list2.size());
        Assert.assertEquals(2, list2.values().peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertFalse(list2.isEmpty());
        Assert.assertFalse(list2.get(OntologyID.create("b", null)).isPresent());
        Assert.assertTrue(list2.get(OntologyID.create("x", null)).isPresent());
        Assert.assertEquals(Arrays.asList("a", "x"), list2.keys()
                .map(OntologyID::asONT).map(OntologyID::asNode)
                .map(Node::getURI).sorted().collect(Collectors.toList()));
        list2.clear();
        Assert.assertTrue(list2.isEmpty());
        Assert.assertEquals(0, list2.size());
        Assert.assertEquals(0, list2.values().count());
        list2.add(b).add(a);
        LOGGER.debug("4) List: {}", list2);

        // change id externally for 'a':
        a.setOntologyID(OntologyID.create("x", null));
        LOGGER.debug("5) List: {}", list2);
        Assert.assertEquals(2, list2.size());
        Assert.assertEquals(Arrays.asList("x", "x"), list2.keys()
                .map(OntologyID::asONT).map(OntologyID::asNode)
                .map(Node::getURI).sorted().collect(Collectors.toList()));
        Assert.assertSame(b, list2.get(OntologyID.create("x", null)).orElseThrow(AssertionError::new));
        Assert.assertNotSame(a, list2.get(OntologyID.create("x", null)).orElseThrow(AssertionError::new));

        // change id externally for 'b':
        b.setOntologyID(OntologyID.create("x", "v"));
        LOGGER.debug("6) List: {}", list2);
        Set<OWLOntologyID> keys = list2.keys().collect(Collectors.toSet());
        LOGGER.debug("Keys: {}", keys);
        Assert.assertEquals(2, keys.size());
        Assert.assertSame(a, list2.get(OntologyID.create("x", null)).orElseThrow(AssertionError::new));
        Assert.assertNotSame(b, list2.get(OntologyID.create("x", null)).orElseThrow(AssertionError::new));

        // change id externally for 'a' and 'b':
        a.setOntologyID(OntologyID.create("y", null));
        b.setOntologyID(OntologyID.create("x", null));
        LOGGER.debug("7) List: {}", list2);
        Assert.assertEquals(2, list2.values().peek(x -> LOGGER.debug("{}", x)).count());
        Assert.assertSame(b, list2.get(OntologyID.create("x", null)).orElseThrow(AssertionError::new));
        Assert.assertSame(a, list2.get(OntologyID.create("y", null)).orElseThrow(AssertionError::new));
    }

    @Test(expected = Exception.class)
    public void testConcurrentModificationOfNonSynchronizedList() throws Exception {
        testConcurrentModification(new OntologyCollectionImpl<>(NoOpReadWriteLock.NO_OP_RW_LOCK));
    }

    @Test
    public void testConcurrentModificationOfRWList() throws Exception {
        testConcurrentModification(new OntologyCollectionImpl<>(new ReentrantReadWriteLock()));
    }

    private void testConcurrentModification(OntologyCollection<IDHolder> list) throws ExecutionException, InterruptedException {
        int num = 15;
        ExecutorService service = Executors.newFixedThreadPool(8);
        List<Future<?>> res = new ArrayList<>();
        LOGGER.debug("Start. The collection: {}", list);
        for (int i = 0; i < num; i++)
            res.add(service.submit(() -> performSomeModifying(list)));
        service.shutdown();
        for (Future<?> f : res) {
            f.get();
            LOGGER.debug("Run. The collection ({}): {}", list.size(), list);
        }
        LOGGER.debug("Fin. The collection: {}", list);
        Assert.assertTrue(list.isEmpty());
    }

    @SuppressWarnings("WeakerAccess")
    public static class IDHolder implements HasOntologyID {
        private OntologyID id;

        private IDHolder(OntologyID id) {
            this.id = Objects.requireNonNull(id);
        }

        public static IDHolder of(String iri, String ver) {
            return new IDHolder(OntologyID.create(iri, ver));
        }

        public static IDHolder of(String iri) {
            return of(iri, null);
        }

        @Override
        public OntologyID getOntologyID() {
            return id;
        }

        public void setOntologyID(OWLOntologyID id) {
            this.id = OntologyID.asONT(id);
        }

        public String getOntologyIRI() {
            return id.getOntologyIRI().map(IRI::getIRIString).orElse(null);
        }

        public void setOntologyIRI(String x) {
            setOntologyID(OntologyID.create(x, getVersionIRI()));
        }

        public String getVersionIRI() {
            return id.getVersionIRI().map(IRI::getIRIString).orElse(null);
        }

        public void setVersionIRI(String x) {
            setOntologyID(OntologyID.create(getOntologyIRI(), x));
        }

        @Override
        public String toString() {
            return String.format("%s[%s & %s]@%s",
                    getClass().getSimpleName(),
                    getOntologyIRI(),
                    getVersionIRI(),
                    Integer.toHexString(hashCode()));
        }
    }
}
