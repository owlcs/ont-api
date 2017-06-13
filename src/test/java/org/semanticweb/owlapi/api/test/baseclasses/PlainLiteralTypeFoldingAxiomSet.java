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

package org.semanticweb.owlapi.api.test.baseclasses;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;

import gnu.trove.set.hash.TCustomHashSet;
import gnu.trove.strategy.HashingStrategy;

/**
 * Created by ses on 9/30/14.
 */
public class PlainLiteralTypeFoldingAxiomSet implements Set<OWLAxiom> {

    private final Set<OWLAxiom> delegate = createPlainLiteralTypeFoldingSet();

    /**
     * @param axioms axioms to be used
     */
    public PlainLiteralTypeFoldingAxiomSet(Collection<OWLAxiom> axioms) {
        // todo: hotfix, wrap as HashSet to avoid strange irregular java.util.NoSuchElementException exception while iterating inside addAll
        delegate.addAll(new HashSet<>(axioms));
    }

    static Set<OWLAxiom> createPlainLiteralTypeFoldingSet() {
        HashingStrategy<OWLAxiom> strategy = new OWLAxiomHashingStrategy();
        return new TCustomHashSet<>(strategy);
    }

    private static class OWLAxiomHashingStrategy implements HashingStrategy<OWLAxiom> {

        OWLAxiomHashingStrategy() {
        }

        /**
         * Computes a hash code for the specified object. Implementers can use
         * the object's own <tt>hashCode</tt> method, the Java runtime's
         * <tt>identityHashCode</tt>, or a custom scheme.
         *
         * @param object for which the hashcode is to be computed
         * @return the hashCode
         */
        @Override
        public int computeHashCode(@Nullable OWLAxiom object) {
            return LiteralFoldingHashCoder.hashCode(object);
        }

        /**
         * Compares o1 and o2 for equality. Strategy implementers may use the
         * objects' own equals() methods, compare object references, or
         * implement some custom scheme.
         *
         * @param o1 an <code>Object</code> value
         * @param o2 an <code>Object</code> value
         * @return true if the objects are equal according to this strategy.
         */
        @Override
        public boolean equals(@Nullable OWLAxiom o1, @Nullable OWLAxiom o2) {
            return LiteralFoldingEqualityTester.equalAxiom(o1, o2);
        }
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<OWLAxiom> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(@Nullable T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(@Nullable OWLAxiom owlAxiom) {
        return delegate.add(owlAxiom);
    }

    @Override
    public boolean remove(@Nullable Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(@Nullable Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(@Nullable Collection<? extends OWLAxiom> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean retainAll(@Nullable Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public boolean removeAll(@Nullable Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}
