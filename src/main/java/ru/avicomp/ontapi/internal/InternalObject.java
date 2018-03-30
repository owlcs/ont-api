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

package ru.avicomp.ontapi.internal;

import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.OntModelFactory;
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
 * Immutable container for {@link OWLObject} and associated with it set of rdf-graph {@link Triple}s.
 * TODO: going to rename: will be used as external resource.
 * <p>
 * Created by @szuev on 27.11.2016.
 */
public abstract class InternalObject<O extends OWLObject> {
    private final O object;
    private int hashCode;

    protected InternalObject(O object) {
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
     * Gets {@link Triple}s associated with encapsulated {@link OWLObject}
     *
     * @return Stream of triples, may be no distinct.
     */
    public abstract Stream<Triple> triples();

    /**
     * Answers if there are definitely no associated triples.
     *
     * @return boolean
     */
    protected boolean isEmpty() {
        return false;
    }

    /**
     * Presents this container as {@link Graph}
     *
     * @return graph
     */
    public Graph toGraph() {
        Graph res = OntModelFactory.createDefaultGraph();
        GraphUtil.add(res, triples().iterator());
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InternalObject)) return false;
        InternalObject<?> that = (InternalObject<?>) o;
        return object.equals(that.object);
    }

    @Override
    public int hashCode() {
        if (hashCode != 0) return hashCode;
        return hashCode = object.hashCode();
    }

    @Override
    public String toString() {
        return String.valueOf(object);
    }

    public static <O extends OWLObject> InternalObject<O> create(O o) {
        return new InternalObject<O>(o) {
            @Override
            public Stream<Triple> triples() {
                return Stream.empty();
            }

            @Override
            public boolean isEmpty() {
                return true;
            }
        };
    }

    public static <O extends OWLObject> InternalObject<O> create(O o, OntStatement root) {
        return create(o, root.asTriple());
    }

    static <O extends OWLObject> InternalObject<O> create(O o, Triple root) {
        return new InternalObject<O>(o) {
            @Override
            public Stream<Triple> triples() {
                return Stream.of(root);
            }
        };
    }

    public static <O extends OWLObject> InternalObject<O> create(O o, OntObject root) {
        return new InternalObject<O>(o) {
            @Override
            public Stream<Triple> triples() {
                return root.content().map(FrontsTriple::asTriple);
            }
        };
    }

    public InternalObject<O> append(OntObject other) {
        return append(() -> other.content().map(FrontsTriple::asTriple));
    }

    public InternalObject<O> append(InternalObject<? extends OWLObject> other) {
        return append(other::triples);
    }

    public <B extends OWLObject> InternalObject<O> append(Collection<InternalObject<B>> others) {
        return append(() -> others.stream().flatMap(InternalObject::triples));
    }

    <B extends OWLObject> InternalObject<O> appendWildcards(Collection<InternalObject<? extends B>> others) {
        return append(() -> others.stream().flatMap(InternalObject::triples));
    }

    public InternalObject<O> append(Supplier<Stream<Triple>> triples) {
        return new InternalObject<O>(object) {
            @Override
            public Stream<Triple> triples() {
                return concat(triples.get());
            }
        };
    }

    private Stream<Triple> concat(Stream<Triple> other) {
        return isEmpty() ? other : Stream.concat(this.triples(), other);
    }

    public InternalObject<O> add(Triple triple) {
        return append(() -> Stream.of(triple));
    }

    public InternalObject<O> delete(Triple triple) {
        if (isEmpty()) return this;
        return new InternalObject<O>(object) {
            @Override
            public Stream<Triple> triples() {
                return InternalObject.this.triples().filter(t -> !triple.equals(t));
            }
        };
    }

    /**
     * Finds {@link InternalObject} by {@link OWLObject}
     *
     * @param set the collection of {@link InternalObject}
     * @param key {@link OWLObject}
     * @param <O> class-type of owl-object
     * @return Optional around {@link InternalObject}
     * @see ru.avicomp.owlapi.OWLObjectImpl#equals(Object)
     */
    public static <O extends OWLObject> Optional<InternalObject<O>> find(Collection<InternalObject<O>> set, O key) {
        int h = OntApiException.notNull(key, "null key").hashCode();
        int t = key.typeIndex();
        return set.stream()
                .filter(Objects::nonNull)
                .filter(o -> o.object.typeIndex() == t)
                .filter(o -> o.hashCode() == h)
                .filter(o -> key.equals(o.object))
                .findAny();
    }

    public static <O extends OWLObject> Stream<O> objects(Collection<InternalObject<O>> wraps) {
        return wraps.stream().map(InternalObject::getObject);
    }

    public static <O extends OWLObject> Set<O> extract(Collection<InternalObject<O>> wraps) {
        return objects(wraps).collect(Collectors.toSet());
    }

    static <R extends OWLObject> Set<R> extractWildcards(Collection<InternalObject<? extends R>> wraps) {
        return wraps.stream().map(InternalObject::getObject).collect(Collectors.toSet());
    }

}
