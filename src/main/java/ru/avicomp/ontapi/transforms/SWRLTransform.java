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

package ru.avicomp.ontapi.transforms;

import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import ru.avicomp.ontapi.jena.utils.BuiltIn;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.SWRL;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Class for performing transformations that fix known syntax errors in graphs containing SWRL.
 *
 * Created by @szuev on 09.08.2018.
 * @see SWRL
 */
@SuppressWarnings("WeakerAccess")
public class SWRLTransform extends Transform {

    protected Set<Statement> unparsed = new HashSet<>();

    public SWRLTransform(Graph graph) {
        super(graph, BuiltIn.get());
    }

    @Override
    public void perform() throws TransformException {
        fixAtomLists();
    }

    /**
     * Fixes wrong {@link SWRL#AtomList swrl:AtomicList} resources.
     * According to some OWL-API-contract tests,
     * it is possible to have {@code [ a swrl:AtomicList ]} instead of just {@code rdf:nil},
     * which, it seems, means an empty []-list.
     * Also, sometimes there is no {@code swrl:AtomicList} declaration at all in case of non-empty []-list.
     */
    protected void fixAtomLists() {
        fixList(SWRL.body);
        fixList(SWRL.head);
    }

    protected void fixList(Property predicate) {
        Model m = getWorkModel();
        listStatements(null, predicate, null)
                .toList()
                .forEach(s -> {
                    if (!s.getObject().isAnon()) {
                        unparsed.add(s);
                        return;
                    }
                    Resource o = s.getResource();
                    if (isEmptyList(o)) {
                        m.removeAll(o, null, null)
                                .remove(s).add(s.getSubject(), s.getPredicate(), RDF.nil);
                        return;
                    }
                    if (!o.canAs(RDFList.class)) {
                        unparsed.add(s);
                        return;
                    }
                    if (o.hasProperty(RDF.type, SWRL.AtomList)) return;
                    Iter.create(Models.getListStatements(o.as(RDFList.class)))
                            .mapWith(Statement::getSubject)
                            .toSet()
                            .forEach(x -> declare(x, SWRL.AtomList));
                });
    }

    private boolean isEmptyList(RDFNode r) {
        return r.isAnon()
                && r.asResource().hasProperty(RDF.type, SWRL.AtomList)
                && r.asResource().listProperties().toList().size() == 1;
    }

    @Override
    public boolean test() {
        return graph.contains(Node.ANY, RDF.type.asNode(), SWRL.Imp.asNode());
    }

    @Override
    public Stream<Triple> uncertainTriples() {
        return unparsed.stream().map(FrontsTriple::asTriple);
    }
}
