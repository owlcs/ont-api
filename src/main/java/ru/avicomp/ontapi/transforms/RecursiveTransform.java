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

import java.util.HashSet;
import java.util.Optional;
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
import ru.avicomp.ontapi.transforms.vocabulary.AVC;

/**
 * To remove/replace possible graph recursions
 * Example of graph which includes recursions:
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
 * <p>
 * Created by @szuev on 24.01.2018.
 */
@SuppressWarnings("WeakerAccess")
public class RecursiveTransform extends Transform {

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
        super(graph, BuiltIn.DUMMY);
        this.replace = replace;
        this.subject = startWithSubject;
    }

    public RecursiveTransform(Graph graph) {
        this(graph, true, true);
    }

    public static Stream<Triple> recursiveTriplesBySubject(Graph graph) {
        return Iter.asStream(graph.find(Triple.ANY)).filter(t -> testSubject(graph, t.getSubject(), new HashSet<>()));
    }

    public static Stream<Triple> recursiveTriplesByObject(Graph graph) {
        return Iter.asStream(graph.find(Triple.ANY)).filter(t -> testObject(graph, t.getObject(), new HashSet<>()));
    }

    private static boolean testSubject(Graph graph, Node test, Set<Node> viewed) {
        if (!test.isBlank()) return false;
        if (viewed.contains(test)) return true;
        viewed.add(test);
        return Iter.asStream(graph.find(test, Node.ANY, Node.ANY))
                .map(Triple::getObject)
                .anyMatch(o -> testSubject(graph, o, viewed));
    }

    private static boolean testObject(Graph graph, Node test, Set<Node> viewed) {
        if (!test.isBlank()) return false;
        if (viewed.contains(test)) return true;
        viewed.add(test);
        return Iter.asStream(graph.find(Node.ANY, Node.ANY, test))
                .map(Triple::getSubject)
                .anyMatch(o -> testObject(graph, o, viewed));
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
            r = wrongTriples().findFirst();
        } while (r.isPresent());
    }

    public Triple createReplacement(Triple base) {
        return subject ? Triple.create(AVC.error(base.getSubject()).asNode(), base.getPredicate(), base.getObject()) :
                Triple.create(base.getSubject(), base.getPredicate(), AVC.error(base.getObject()).asNode());
    }

    public Stream<Triple> wrongTriples() {
        return subject ? recursiveTriplesBySubject(getBaseGraph()) : recursiveTriplesByObject(getBaseGraph());
    }
}
