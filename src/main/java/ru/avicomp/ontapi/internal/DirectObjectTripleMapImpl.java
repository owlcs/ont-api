/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.internal;

import org.apache.jena.graph.GraphListener;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.utils.Iter;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * No cache.
 * Created by @ssz on 16.03.2019.
 */
@SuppressWarnings("WeakerAccess")
public class DirectObjectTripleMapImpl<X extends OWLObject> implements ObjectTriplesMap<X> {
    private final Supplier<Iterator<ONTObject<X>>> loader;

    public DirectObjectTripleMapImpl(Supplier<Iterator<ONTObject<X>>> loader) {
        this.loader = Objects.requireNonNull(loader);
    }

    @Override
    public boolean hasNew() {
        return false;
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public void load() {
        // nothing
    }

    public ExtendedIterator<ONTObject<X>> listONTObjects() {
        return WrappedIterator.create(loader.get());
    }

    @Override
    public Stream<X> objects() {
        return Iter.asStream(listONTObjects().mapWith(ONTObject::getObject));
    }

    @Override
    public boolean contains(X key) {
        return object(key).isPresent();
    }

    public Optional<ONTObject<X>> object(X key) {
        // todo: need a straight way, this one is extremely inefficient
        return Iter.findFirst(listONTObjects().filterKeep(x -> key.equals(x.getObject())));
    }

    @Override
    public GraphListener addListener(X key) {
        throw new ModificationDeniedException("Read only model. Can't add " + key + ".");
    }

    @Override
    public void delete(X key) {
        throw new ModificationDeniedException("Read only model. Can't delete " + key + ".");
    }

    @Override
    public ONTObject<X> get(X key) {
        return object(key).orElse(null);
    }

    @Override
    public void clear() {
        // nothing
    }

    public static class ModificationDeniedException extends OntApiException.Unsupported {
        public ModificationDeniedException(String message) {
            super(message);
        }
    }
}
