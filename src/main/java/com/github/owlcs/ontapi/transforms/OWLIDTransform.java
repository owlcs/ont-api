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

import com.github.sszuev.jena.ontapi.OntVocabulary;
import com.github.sszuev.jena.ontapi.utils.Graphs;
import com.github.sszuev.jena.ontapi.utils.Iterators;
import com.github.sszuev.jena.ontapi.vocabulary.OWL;
import com.github.sszuev.jena.ontapi.vocabulary.RDF;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class to perform ontology id transformation.
 * It merges several {@code owl:Ontology} sections to single one.
 * As primary chooses the one that has the largest number of triplets.
 * If there is no any {@code owl:Ontology} then new anonymous owl:Ontology will be added to the graph.
 */
public class OWLIDTransform extends TransformationModel {

    public OWLIDTransform(Graph graph) {
        super(graph, OntVocabulary.Factory.EMPTY_VOCABULARY);
    }

    @Override
    public void perform() {
        Graph workGraph = getWorkModel().getGraph();
        // choose or create the new one:
        Graph queryGraph = getQueryModel().getGraph();
        Node ontology = Graphs.ontologyNode(queryGraph, true)
                .orElseGet(() -> {
                    Node res = NodeFactory.createBlankNode();
                    workGraph.add(Triple.create(res, RDF.type.asNode(), OWL.Ontology.asNode()));
                    return res;
                });
        Set<Triple> prev = Iterators.addAll(Iterators.flatMap(
                queryGraph.find(Node.ANY, RDF.type.asNode(), OWL.Ontology.asNode()),
                it -> queryGraph.find(it.getSubject(), Node.ANY, Node.ANY)), new HashSet<>());
        Set<Node> subjects = prev.stream().map(Triple::getSubject).collect(Collectors.toSet());
        if (subjects.contains(ontology)) {
            if (subjects.size() == 1) {
                // single header found, nothing to do
                return;
            }
        } else {
            workGraph.add(ontology, RDF.type.asNode(), OWL.Ontology.asNode());
        }
        prev.forEach(t -> {
            if (!ontology.equals(t.getSubject())) {
                workGraph.delete(t);
            }
        });
        prev.forEach(t -> {
            if (!ontology.equals(t.getSubject())) {
                workGraph.add(ontology, t.getPredicate(), t.getObject());
            }
        });
    }
}
