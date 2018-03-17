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

import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To perform recursive transformation in terms of OWL2.
 * Example of OWL2 allowed recursions:
 * <pre>{@code
 * _:a0 owl:differentFrom _:a1 .
 * _:a1 owl:differentFrom _:a0 .
 * }</pre>.
 * It seems there triple {@code  _:a @predicate _:a} is allowed too.
 *
 * @see RecursiveTransform for more details
 */
public class OWLRecursiveTransform extends RecursiveTransform {

    private static final Set<Node> ALLOWED_PREDICATES = Stream.of(OWL.differentFrom,
            OWL.propertyDisjointWith,
            OWL.disjointWith).map(FrontsNode::asNode).collect(Collectors.toSet());

    public OWLRecursiveTransform(Graph graph) {
        super(graph, false, true);
    }

    @Override
    public Stream<Triple> recursiveTriples() {

        return super.recursiveTriples()
                .filter(t -> !t.getObject().equals(t.getSubject()))
                .filter(t -> Iter.asStream(getBaseGraph().find(createReplacement(Triple.ANY, n -> subject ? t.getSubject() : t.getObject())))
                        .map(Triple::getPredicate)
                        .noneMatch(ALLOWED_PREDICATES::contains));
    }
}
