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

package ru.avicomp.ontapi.transforms;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.utils.Iter;

/**
 * To remove possible graph recursions
 * Example of graph which includes recursions with usage:
 * <pre>
 * {@code
 * :TheClass    a                   owl:Class ;
 *              rdfs:label          "Some class"@pt ;
 *              rdfs:subClassOf     _:b0 .
 * _:b0         a                   owl:Restriction ;
 *              owl:onProperty      :someProperty ;
 *              owl:someValuesFrom  [   a                   owl:Restriction ;
 *                                      owl:onProperty      :someProperty ;
 *                                      owl:someValuesFrom  _:b0
 *                                  ] .
 * }
 * </pre>
 * <p>
 * Created by @szuev on 24.01.2018.
 */
@SuppressWarnings("WeakerAccess")
public class RecursiveTransform extends Transform {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecursiveTransform.class);

    public RecursiveTransform(Graph graph) {
        super(graph, BuiltIn.DUMMY);
    }

    @Override
    public void perform() {
        Graph graph = getBaseGraph();
        Set<Triple> delete = findRecursive(graph, true).collect(Collectors.toSet());
        if (LOGGER.isDebugEnabled() && !delete.isEmpty()) {
            LOGGER.debug("Count of triples to delete: {}", delete.size());
        }
        GraphUtil.delete(graph, delete.iterator());
    }

    /**
     * Finds all recursive triplets.
     *
     * @param graph        {@link Graph}
     * @param includeUsage if true all usages will be included to result also
     * @return Stream of {@link Triple triples}
     */
    public static Stream<Triple> findRecursive(Graph graph, boolean includeUsage) {
        Stream<Triple> res = Iter.asStream(graph.find(Triple.ANY)).filter(t -> isRecursive(graph, t));
        if (includeUsage) {
            res = res.flatMap(t -> Stream.concat(Stream.of(t), Iter.asStream(graph.find(Node.ANY, Node.ANY, t.getSubject()))));
        }
        return res;
    }

    private static boolean isRecursive(Graph graph, Triple triple) {
        return isRecursive(graph, null, triple.getSubject());
    }

    private static boolean isRecursive(Graph graph, Node subject, Node test) {
        if (!test.isBlank()) return false;
        if (test.equals(subject)) return true;
        Node s = subject == null ? test : subject;
        return Iter.asStream(graph.find(test, Node.ANY, Node.ANY))
                .map(Triple::getObject)
                .anyMatch(n -> isRecursive(graph, s, n));
    }
}
