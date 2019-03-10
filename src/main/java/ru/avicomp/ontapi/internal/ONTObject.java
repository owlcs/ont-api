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

import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.CollectionGraph;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An unmodifiable container for {@link OWLObject} and associated with it set of rdf-graph {@link Triple triple}s.
 * <p>
 * Created by @szuev on 27.11.2016.
 */
public abstract class ONTObject<O extends OWLObject> {
    private final O object;

    protected ONTObject(O object) {
        this.object = Objects.requireNonNull(object, "Null OWLObject.");
    }

    /**
     * Gets wrapped {@link OWLObject}.
     *
     * @return OWL object
     */
    public O getObject() {
        return object;
    }

    /**
     * Gets {@link Triple}s associated with encapsulated {@link OWLObject}.
     *
     * @return Stream of triples, may be no distinct.
     */
    public abstract Stream<Triple> triples();

    /**
     * Answers {@code true} if there are definitely no associated triples.
     *
     * @return boolean
     */
    protected boolean isDefinitelyEmpty() {
        return false;
    }

    /**
     * Presents this container as in-memory {@link Graph}.
     *
     * @return graph
     */
    @SuppressWarnings("unused")
    public Graph toGraph() {
        return new CollectionGraph(triples().collect(Collectors.toSet()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ONTObject)) return false;
        ONTObject<?> that = (ONTObject<?>) o;
        return object.equals(that.object);
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }

    @Override
    public String toString() {
        return String.valueOf(object);
    }

    public static <O extends OWLObject> ONTObject<O> create(O o) {
        return new ONTObject<O>(o) {
            @Override
            public Stream<Triple> triples() {
                return Stream.empty();
            }

            @Override
            public boolean isDefinitelyEmpty() {
                return true;
            }
        };
    }

    public static <O extends OWLObject> ONTObject<O> create(O o, OntStatement root) {
        return create(o, root.asTriple());
    }

    public static <O extends OWLObject> ONTObject<O> create(O o, Triple root) {
        return new ONTObject<O>(o) {
            @Override
            public Stream<Triple> triples() {
                return Stream.of(root);
            }
        };
    }

    public static <O extends OWLObject> ONTObject<O> create(O o, OntObject root) {
        return new ONTObject<O>(o) {
            @Override
            public Stream<Triple> triples() {
                return root.spec().map(FrontsTriple::asTriple);
            }
        };
    }

    public ONTObject<O> append(OntObject other) {
        return append(() -> other.spec().map(FrontsTriple::asTriple));
    }

    public ONTObject<O> append(ONTObject<? extends OWLObject> other) {
        return append(other::triples);
    }

    public <B extends OWLObject> ONTObject<O> append(Collection<ONTObject<B>> others) {
        return append(() -> others.stream().flatMap(ONTObject::triples));
    }

    public <B extends OWLObject> ONTObject<O> appendWildcards(Collection<ONTObject<? extends B>> others) {
        return append(() -> others.stream().flatMap(ONTObject::triples));
    }

    public ONTObject<O> append(Supplier<Stream<Triple>> triples) {
        return new ONTObject<O>(object) {
            @Override
            public Stream<Triple> triples() {
                return concat(triples.get());
            }
        };
    }

    private Stream<Triple> concat(Stream<Triple> other) {
        return isDefinitelyEmpty() ? other : Stream.concat(this.triples(), other);
    }

    public ONTObject<O> add(Triple triple) {
        return append(() -> Stream.of(triple));
    }

    public ONTObject<O> delete(Triple triple) {
        if (isDefinitelyEmpty()) return this;
        return new ONTObject<O>(object) {
            @Override
            public Stream<Triple> triples() {
                return ONTObject.this.triples().filter(t -> !triple.equals(t));
            }
        };
    }

    /**
     * Finds {@link ONTObject} by {@link OWLObject}
     *
     * @param set the collection of {@link ONTObject}
     * @param key {@link OWLObject}
     * @param <O> class-type of owl-object
     * @return Optional around {@link ONTObject}
     * @see ru.avicomp.ontapi.owlapi.OWLObjectImpl#equals(Object)
     */
    public static <O extends OWLObject> Optional<ONTObject<O>> find(Collection<ONTObject<O>> set, O key) {
        int h = OntApiException.notNull(key, "null key").hashCode();
        int t = key.typeIndex();
        return set.stream()
                .filter(Objects::nonNull)
                .filter(o -> o.object.typeIndex() == t)
                .filter(o -> o.hashCode() == h)
                .filter(o -> key.equals(o.object))
                .findAny();
    }

    public static <O extends OWLObject> Set<O> extract(Collection<ONTObject<O>> wraps) {
        return objects(wraps).collect(Collectors.toSet());
    }

    public static <R extends OWLObject> Set<R> extractWildcards(Collection<ONTObject<? extends R>> wraps) {
        return wraps.stream().map(ONTObject::getObject).collect(Collectors.toSet());
    }

    public static <O extends OWLObject> Stream<O> objects(Collection<ONTObject<O>> wraps) {
        return wraps.stream().map(ONTObject::getObject);
    }


}
