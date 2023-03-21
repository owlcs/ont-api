/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
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

import com.github.owlcs.ontapi.jena.OntVocabulary;
import com.github.owlcs.ontapi.transforms.vocabulary.ONTAPI;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To remove/replace possible graph recursions
 * Example of graph with a simple recursions:
 * <pre>{@code
 * :TheClass    a                   owl:Class ;
 *              rdfs:label          "Some class"@pt ;
 *              rdfs:subClassOf     _:b0 .
 * _:b0         a                   owl:Restriction ;
 *              owl:onProperty      :someProperty ;
 *              owl:someValuesFrom  [   a                   owl:Restriction ;
 *                                      owl:onProperty      :someProperty ;
 *                                      owl:someValuesFrom  _:b0
 *                                  ] .
 * }</pre>
 * Created by @szuev on 24.01.2018.
 * <p>
 * Note: this transform is slow - the complexity is ~O(n*log(n))
 * @see ONTAPI#error(String)
 */
@SuppressWarnings("WeakerAccess")
public class RecursiveTransform extends TransformationModel {

    protected static final int EMERGENCY_EXIT_LIMIT = 10_000;
    protected final boolean replace;
    protected final boolean subject;

    /**
     * The main constructor.
     *
     * @param graph            the {@link Graph} to process
     * @param replace          if true recursive b-nodes would be replaced with named nodes, otherwise they would be deleted
     * @param startWithSubject if true starts search subjects first, otherwise - objects.
     */
    public RecursiveTransform(Graph graph, boolean replace, boolean startWithSubject) {
        super(graph, OntVocabulary.Factory.DUMMY);
        this.replace = replace;
        this.subject = startWithSubject;
    }

    public RecursiveTransform(Graph graph) {
        this(graph, true, true);
    }

    public static Stream<Triple> recursiveTriplesBySubject(Graph graph) {
        return anonymous(graph).filter(t -> testSubject(graph, t.getSubject()));
    }

    public static Stream<Triple> recursiveTriplesByObject(Graph graph) {
        return anonymous(graph).filter(t -> testObject(graph, t.getObject()));
    }

    /**
     * Returns all triples with blank subject and object
     *
     * @param graph {@link Graph}
     * @return Stream of {@link Triple triples}
     */
    public static Stream<Triple> anonymous(Graph graph) {
        return graph.stream()
                .filter(t -> t.getSubject().isBlank())
                .filter(t -> t.getObject().isBlank());
    }

    /**
     * Answers iff specified node is recursive.
     * Search starts from subject node.
     *
     * @param graph {@link Graph}
     * @param test  {@link Node}  the subject to test
     * @return true if this node in recursion
     * @see #testObject(Graph, Node)
     */
    public static boolean testSubject(Graph graph, Node test) {
        return test.isBlank() && objectsBySubject(graph, test, new HashSet<>()).anyMatch(test::equals);
    }

    /**
     * Answers iff specified node is recursive.
     * Search starts from object node.
     *
     * @param graph {@link Graph}
     * @param test  {@link Node}  the object to test
     * @return true if this node in recursion
     * @see #testSubject(Graph, Node)
     */
    public static boolean testObject(Graph graph, Node test) {
        return test.isBlank() && subjectsByObject(graph, test, new HashSet<>()).anyMatch(test::equals);
    }

    private static Stream<Node> objectsBySubject(Graph graph, Node subject, Set<Triple> visited) {
        Set<Node> nodes = graph.stream(subject, Node.ANY, Node.ANY)
                .filter(visited::add)
                .map(Triple::getObject)
                .filter(Node::isBlank)
                .collect(Collectors.toSet());
        return nodes.stream()
                .flatMap(s -> Stream.concat(Stream.of(s), objectsBySubject(graph, s, visited)));
    }

    private static Stream<Node> subjectsByObject(Graph graph, Node object, Set<Triple> visited) {
        Set<Node> nodes = graph.stream(Node.ANY, Node.ANY, object)
                .filter(visited::add)
                .map(Triple::getSubject)
                .filter(Node::isBlank)
                .collect(Collectors.toSet());
        return nodes.stream()
                .flatMap(o -> Stream.concat(Stream.of(o), subjectsByObject(graph, o, visited)));
    }

    @Override
    public void perform() {
        Graph graph = getBaseGraph();
        Optional<Triple> r = Optional.empty();
        int count = 0;
        do {
            if (count++ > EMERGENCY_EXIT_LIMIT) {
                throw new TransformException("To many recursions in the graph");
            }
            r.ifPresent(t -> {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("{} [{}]", replace ? "Replace" : "Delete", t);
                }
                graph.delete(t);
                if (!replace) return;
                graph.add(createReplacement(t));
            });
            r = recursiveTriples().findFirst();
        } while (r.isPresent());
    }

    public Triple createReplacement(Triple base) {
        return createReplacement(base, n -> ONTAPI.error(n).asNode());
    }

    public Triple createReplacement(Triple base, UnaryOperator<Node> mapper) {
        return subject ? Triple.create(mapper.apply(base.getSubject()), base.getPredicate(), base.getObject()) :
                Triple.create(base.getSubject(), base.getPredicate(), mapper.apply(base.getObject()));
    }

    public Stream<Triple> recursiveTriples() {
        return subject ? recursiveTriplesBySubject(getBaseGraph()) : recursiveTriplesByObject(getBaseGraph());
    }
}
