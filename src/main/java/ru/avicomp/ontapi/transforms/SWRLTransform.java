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

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.vocabulary.SWRL;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class for performing transformations that fix known syntax errors in graphs containing SWRL.
 * Created by @szuev on 09.08.2018.
 */
@SuppressWarnings("WeakerAccess")
public class SWRLTransform extends Transform {

    public SWRLTransform(Graph graph) {
        super(graph, BuiltIn.get());
    }

    @Override
    public void perform() throws TransformException {
        fixEmptyLists();
    }

    /**
     * Fixes wrong {@link SWRL#AtomList swrl:AtomicList} resources.
     * According to some OWL-API-contract tests,
     * it is possible to have {@code [ a swrl:AtomicList ]} instead of just {@code rdf:nil} in case of empty list.
     */
    protected void fixEmptyLists() {
        Stream.of(SWRL.body, SWRL.head).forEach(this::fixEmptyList);
    }

    protected void fixEmptyList(Property predicate) {
        Model m = getBaseModel();
        statements(getModel(), null, predicate, null)
                .filter(s -> s.getObject().isAnon())
                .filter(s -> s.getResource().hasProperty(RDF.type, SWRL.AtomList))
                .filter(s -> s.getResource().listProperties().toList().size() == 1)
                .collect(Collectors.toSet())
                .forEach(s -> m.removeAll(s.getResource(), null, null).remove(s).add(s.getSubject(), s.getPredicate(), RDF.nil));
    }

    @Override
    public boolean test() {
        return graph.contains(Node.ANY, RDF.type.asNode(), SWRL.Imp.asNode());
    }
}
