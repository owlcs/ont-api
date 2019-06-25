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

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.CollectionGraph;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An unmodifiable container for {@link OWLObject} and associated with it {@link Triple RDF Triple}s.
 * Created by @szz on 25.06.2019.
 *
 * @param <O> any subtype of {@link OWLObject}
 */
public interface ONTObject<O extends OWLObject> {

    static <X extends OWLObject> Set<X> extract(Collection<? extends ONTObject<X>> wraps) {
        return objects(wraps).collect(Collectors.toSet());
    }

    static <X extends OWLObject> Set<X> extractWildcards(Collection<? extends ONTObject<? extends X>> wraps) {
        return wraps.stream().map(ONTObject::getObject).collect(Collectors.toSet());
    }

    static <X extends OWLObject> Stream<X> objects(Collection<? extends ONTObject<X>> objects) {
        return objects.stream().map(ONTObject::getObject);
    }

    /**
     * Gets the associated {@link O}.
     *
     * @return {@code OWLObject}
     */
    O getObject();

    /**
     * Lists all associated {@link Triple triple}s.
     *
     * @return {@code Stream} of {@link Triple}s
     */
    Stream<Triple> triples();

    /**
     * Represents this container as in-memory {@link Graph}.
     *
     * @return graph
     */
    @SuppressWarnings("unused")
    default Graph toGraph() {
        return new CollectionGraph(triples().collect(Collectors.toSet()));
    }

}
