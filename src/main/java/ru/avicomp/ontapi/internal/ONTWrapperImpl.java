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

package ru.avicomp.ontapi.internal;

import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An unmodifiable container for {@link OWLObject} and associated with it set of rdf-graph {@link Triple triple}s.
 * <p>
 * Created by @szuev on 27.11.2016.
 *
 * @param <O> any subtype of {@link OWLObject}
 */
public abstract class ONTWrapperImpl<O extends OWLObject> implements ONTObject<O>, WithMerge<ONTObject<O>> {
    private final O object;

    protected ONTWrapperImpl(O object) {
        this.object = Objects.requireNonNull(object, "Null OWLObject.");
    }

    public static <X extends OWLObject> ONTWrapperImpl<X> create(X o, OntStatement root) {
        return create(o, root.asTriple());
    }

    public static <X extends OWLObject> ONTWrapperImpl<X> create(X o, OntObject root) {
        return new ONTWrapperImpl<X>(o) {
            @Override
            public Stream<Triple> triples() {
                return root.spec().map(FrontsTriple::asTriple);
            }
        };
    }

    public static <X extends OWLObject> ONTWrapperImpl<X> create(X o) {
        return new ONTWrapperImpl<X>(o) {
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

    protected static <X extends OWLObject> ONTWrapperImpl<X> create(X o, Triple root) {
        return new ONTWrapperImpl<X>(o) {
            @Override
            public Stream<Triple> triples() {
                return Stream.of(root);
            }
        };
    }

    protected static <X extends OWLObject> ONTWrapperImpl<X> create(ONTObject<X> other) {
        return new ONTWrapperImpl<X>(other.getOWLObject()) {
            @Override
            public Stream<Triple> triples() {
                return other.triples();
            }
        };
    }

    @Override
    public ONTWrapperImpl<O> merge(ONTObject<O> other) {
        return append(other);
    }

    /**
     * Gets wrapped {@link OWLObject}.
     *
     * @return OWL object
     */
    @Override
    public O getOWLObject() {
        return object;
    }

    /**
     * Gets {@link Triple}s associated with encapsulated {@link OWLObject}.
     *
     * @return Stream of triples, may be no distinct.
     */
    @Override
    public abstract Stream<Triple> triples();

    /**
     * Answers {@code true} if there are definitely no associated triples.
     *
     * @return boolean
     */
    protected boolean isDefinitelyEmpty() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ONTObject)) return false;
        ONTObject<?> that = (ONTObject<?>) o;
        return object.equals(that.getOWLObject());
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }

    @Override
    public String toString() {
        return String.valueOf(object);
    }

    public ONTWrapperImpl<O> append(OntObject other) {
        return append(() -> other.spec().map(FrontsTriple::asTriple));
    }

    public ONTWrapperImpl<O> append(ONTObject<? extends OWLObject> other) {
        return append(other::triples);
    }

    public <B extends OWLObject> ONTWrapperImpl<O> append(Collection<? extends ONTObject<B>> others) {
        return append(() -> others.stream().flatMap(ONTObject::triples));
    }

    public <B extends OWLObject> ONTWrapperImpl<O> appendWildcards(Collection<? extends ONTObject<? extends B>> others) {
        return append(() -> others.stream().flatMap(ONTObject::triples));
    }

    public ONTWrapperImpl<O> append(Supplier<Stream<Triple>> triples) {
        return new ONTWrapperImpl<O>(object) {
            @Override
            public Stream<Triple> triples() {
                return concat(triples.get());
            }
        };
    }

    private Stream<Triple> concat(Stream<Triple> other) {
        return isDefinitelyEmpty() ? other : Stream.concat(this.triples(), other);
    }

}
