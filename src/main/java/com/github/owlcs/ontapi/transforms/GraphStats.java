/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.transforms;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;

import java.util.*;
import java.util.stream.Stream;

/**
 * A transformation statistic object, which is an outcome of transform process.
 * Notice that it holds everything in memory.
 * <p>
 * Created by @szuev on 27.06.2018.
 */
public class GraphStats {
    protected final Graph graph;
    protected Map<Type, Map<String, Set<Triple>>> triples = new EnumMap<>(Type.class);
    protected Set<GraphStats> sub = new HashSet<>();

    protected GraphStats(Graph graph) {
        this.graph = Objects.requireNonNull(graph);
    }

    protected void putTriples(Transform transform,
                              Set<Triple> added,
                              Set<Triple> deleted,
                              Set<Triple> unparsed) {
        String name = transform.id();
        put(Type.ADDED, name, added);
        put(Type.DELETED, name, deleted);
        put(Type.UNPARSED, name, unparsed);
    }

    protected void put(Type type, String name, Set<Triple> triples) {
        map(type).computeIfAbsent(name, s -> new HashSet<>()).addAll(triples);
    }

    protected void putStats(GraphStats other) {
        this.sub.add(other);
    }

    public Set<Triple> getTriples(Type type, String name) {
        return getUnmodifiable(map(type), name);
    }

    public Stream<Triple> triples(Type type) {
        return map(type).values().stream().flatMap(Collection::stream);
    }

    public boolean hasTriples(Type type, String name) {
        if (!triples.containsKey(type)) return false;
        if (!triples.get(type).containsKey(name)) return false;
        return !triples.get(type).get(name).isEmpty();
    }

    public boolean hasTriples(Type type) {
        if (!triples.containsKey(type)) return false;
        return triples.get(type).values().stream().anyMatch(x -> !x.isEmpty());
    }

    public boolean hasTriples() {
        return !triples.isEmpty();
    }

    public boolean isNotEmpty() {
        return hasTriples() && Arrays.stream(Type.values()).anyMatch(this::hasTriples);
    }

    protected Map<String, Set<Triple>> map(Type type) {
        return triples.computeIfAbsent(type, t -> new HashMap<>());
    }

    public Graph getGraph() {
        return graph;
    }

    /**
     * Lists all encapsulated Stats object.
     *
     * @param deep if {@code true} all sub-stats will be included recursively in the result stream also,
     *             otherwise only top-level sub-stats are expected in the return stream
     * @return Stream of {@link GraphStats}
     */
    public Stream<GraphStats> stats(boolean deep) {
        if (!deep) return sub.stream();
        return sub.stream().flatMap(s -> Stream.concat(Stream.of(s), s.stats(true)));
    }

    private static <T> Set<T> getUnmodifiable(Map<String, Set<T>> map, String key) {
        return map.containsKey(key) ? Collections.unmodifiableSet(map.get(key)) : Collections.emptySet();
    }

    /**
     * A type of triples.
     */
    public enum Type {
        ADDED,
        DELETED,
        UNPARSED
    }
}
