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

package com.github.owlcs.ontapi.internal;

import com.github.owlcs.ontapi.jena.utils.Iter;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * The implementation of {@link ObjectMap} that redirects calls directly to a graph, without any caching.
 * Created by @ssz on 16.03.2019.
 */
@SuppressWarnings("WeakerAccess")
public class DirectObjectMapImpl<X extends OWLObject> implements ObjectMap<X> {
    private final Supplier<Iterator<ONTObject<X>>> list;
    private final Function<X, Optional<ONTObject<X>>> find;
    private final Predicate<X> contains;

    /**
     * Creates a direct {@link ObjectMap} instance with default behaviour specified by the given {@code loader}.
     *
     * @param loader {@code Supplier}, that provides a {@code Stream} of {@link ONTObject}s, not {@code null}
     */
    public DirectObjectMapImpl(Supplier<Iterator<ONTObject<X>>> loader) {
        this(Objects.requireNonNull(loader),
                k -> Iter.findFirst(Iter.create(loader.get()).filterKeep(x -> x.getOWLObject().equals(k))),
                k -> Iter.findFirst(loader.get()).isPresent());
    }

    /**
     * Creates a direct {@link ObjectMap} instance.
     *
     * @param loader {@code Supplier}, that provides a {@code Stream} of {@link ONTObject}s, not {@code null}
     * @param finder {@code Function}, that maps an {@link OWLObject}-key to {@link ONTObject}-value, not {@code null}
     * @param tester {@code Predicate}, that tests if an {@link OWLObject}-key is present in this map, not {@code null}
     */
    public DirectObjectMapImpl(Supplier<Iterator<ONTObject<X>>> loader,
                               Function<X, Optional<ONTObject<X>>> finder,
                               Predicate<X> tester) {
        this.list = Objects.requireNonNull(loader);
        this.find = Objects.requireNonNull(finder);
        this.contains = Objects.requireNonNull(tester);
    }

    /**
     * Lists all {@link ONTObject} using {@code loader}.
     *
     * @return {@code ExtendedIterator} over all {@link ONTObject}s
     */
    public ExtendedIterator<ONTObject<X>> listONTObjects() {
        return Iter.create(list.get());
    }

    /**
     * Finds the {@link ONTObject} using encapsulated {@code finder}.
     *
     * @param key {@link X}
     * @return {@code Optional} of {@link ONTObject}
     */
    public Optional<ONTObject<X>> findONTObject(X key) {
        return find.apply(key);
    }

    @Override
    public Stream<X> keys() {
        return Iter.asStream(listONTObjects().mapWith(ONTObject::getOWLObject));
    }

    @Override
    public Stream<ONTObject<X>> values() {
        return Iter.asStream(listONTObjects());
    }

    @Override
    public boolean contains(X key) {
        return contains.test(key);
    }

    @Override
    public ONTObject<X> get(X key) {
        return findONTObject(key).orElse(null);
    }

    @Override
    public boolean hasNew() {
        return false;
    }

    /**
     * {@inheritDoc}
     * This {@code ObjectMap} does not contain any caches, so the method always answers {@code false}.
     *
     * @return {@code false}
     */
    @Override
    public boolean isLoaded() {
        return false;
    }

    @Override
    public void load() {
        // nothing
    }

    @Override
    public void clear() {
        // nothing
    }

    @Override
    public void remove(X key) {
        // nothing
    }

    @Override
    public void add(ONTObject<X> value) {
        // nothing
    }

}
