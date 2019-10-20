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

package ru.avicomp.ontapi.utils;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.topbraid.spin.model.*;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.transforms.Transform;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * To replace spin queries with its string representations (it is alternative way to describe spin-sparql-query).
 * By default a spin query is represented in the bulky form which consists of several rdf:List.
 * The short (string, sp:text) form allows to present the query as an axiom also.
 * <p>
 * Example of a query:
 * <pre> {@code
 * spin:body [
 *    rdf:type sp:Select ;
 *    sp:resultVariables (
 *        [
 *          sp:expression [
 *              rdf:type sp:Count ;
 *              sp:expression [
 *                  sp:varName \"subject\"^^xsd:string ;
 *                ] ;
 *            ] ;
 *          sp:varName \"result\"^^xsd:string ;
 *        ]
 *      ) ;
 *    sp:where (
 *        [
 *          sp:object spin:_arg2 ;
 *          sp:predicate spin:_arg1 ;
 *          sp:subject [
 *              sp:varName \"subject\"^^xsd:string ;
 *            ] ;
 *        ]
 *      ) ;
 *  ] ;
 * } </pre>
 * And it will be replaced with:
 * <pre> {@code
 * spin:body [ a        sp:Select ;
 *             sp:text  "SELECT ((COUNT(?subject)) AS ?result)\nWHERE {\n    ?subject spin:_arg1 spin:_arg2 .\n}"
 *           ] ;
 * }</pre>
 * <p>
 * Note(1): For test purposes only.
 * Note(2): before processing add links to {@link org.apache.jena.util.FileManager} to avoid recourse to web.
 * <p>
 * Created by szuev on 21.04.2017.
 */
public class SpinTransform extends Transform {

    public SpinTransform(Graph graph) {
        super(graph);
    }

    @Override
    public void perform() {
        List<Query> queries = queries().collect(Collectors.toList());
        String name = Graphs.getName(getQueryModel().getGraph());
        if (!queries.isEmpty() && LOGGER.isDebugEnabled()) {
            LOGGER.debug("[{}] queries count: {}", name, queries.size());
        }
        queries.forEach(query -> {
            Literal literal = ResourceFactory.createTypedLiteral(String.valueOf(query));
            Resource type = statements(query, RDF.type, null)
                    .map(Statement::getObject)
                    .filter(RDFNode::isURIResource)
                    .map(RDFNode::asResource)
                    .findFirst().orElseThrow(() -> new OntJenaException("No type for " + literal));
            Set<Statement> remove = Models.getAssociatedStatements(query);
            remove.stream()
                    .filter(s -> !(RDF.type.equals(s.getPredicate()) && type.equals(s.getObject())))
                    .forEach(statement -> getWorkModel().remove(statement));
            getWorkModel().add(query, SP.text, literal);
        });
    }

    @Override
    public boolean test() {
        return Stream.concat(getGraph().getPrefixMapping().getNsPrefixMap().values().stream(),
                Graphs.getImports(getGraph()).stream()).anyMatch(u -> u.startsWith(SP.SPIN_URI));
    }

    @Override
    public Model createModel(Graph graph) {
        return SP.createModel(graph);
    }

    public Stream<Query> queries() {
        return Stream.of(QueryType.values()).map(this::queries).flatMap(Function.identity());
    }

    protected Stream<Query> queries(QueryType type) {
        return statements(null, RDF.type, type.getType()).map(Statement::getSubject)
                .filter(s -> s.canAs(type.getView())).map(s -> s.as(type.getView()));
    }

    public enum QueryType {
        SELECT(SP.Select, Select.class),
        CONSTRUCT(SP.Construct, Construct.class),
        ASK(SP.Ask, Ask.class),
        DESCRIBE(SP.Describe, Describe.class);

        private final Resource type;
        private final Class<? extends Query> view;

        QueryType(Resource type, Class<? extends Query> view) {
            this.type = type;
            this.view = view;
        }

        public Resource getType() {
            return type;
        }

        public Class<? extends Query> getView() {
            return view;
        }
    }
}
