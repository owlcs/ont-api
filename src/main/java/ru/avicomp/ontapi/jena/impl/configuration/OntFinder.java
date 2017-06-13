/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena.impl.configuration;

import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * To perform the preliminary search resources in model,
 * then the result stream will be filtered by {@link OntFilter}
 * Used in the factory {@link CommonOntObjectFactory}.
 * <p>
 * Created by szuev on 07.11.2016.
 */
@FunctionalInterface
public interface OntFinder {
    OntFinder ANY_SUBJECT = eg -> Iter.asStream(eg.asGraph().find(Triple.ANY).mapWith(Triple::getSubject)).distinct();
    OntFinder ANY_SUBJECT_AND_OBJECT = eg -> Iter.asStream(eg.asGraph().find(Triple.ANY))
            .map(t -> Stream.of(t.getSubject(), t.getObject())).flatMap(Function.identity()).distinct();
    OntFinder ANYTHING = eg -> Iter.asStream(eg.asGraph().find(Triple.ANY))
            .map(t -> Stream.of(t.getSubject(), t.getPredicate(), t.getObject()))
            .flatMap(Function.identity()).distinct();
    OntFinder TYPED = new ByPredicate(RDF.type);

    Stream<Node> find(EnhGraph eg);

    default OntFinder restrict(OntFilter filter) {
        OntJenaException.notNull(filter, "Null restriction filter.");
        return eg -> find(eg).filter(n -> filter.test(n, eg));
    }

    class ByType implements OntFinder {
        protected final Node type;

        public ByType(Resource type) {
            this.type = OntJenaException.notNull(type, "Null type.").asNode();
        }

        @Override
        public Stream<Node> find(EnhGraph eg) {
            return Iter.asStream(eg.asGraph().find(Node.ANY, RDF.type.asNode(), type).mapWith(Triple::getSubject)).distinct();
        }
    }

    class ByPredicate implements OntFinder {
        protected final Node predicate;

        public ByPredicate(Property predicate) {
            this.predicate = OntJenaException.notNull(predicate, "Null predicate.").asNode();
        }

        @Override
        public Stream<Node> find(EnhGraph eg) {
            return Iter.asStream(eg.asGraph().find(Node.ANY, predicate, Node.ANY).mapWith(Triple::getSubject)).distinct();
        }
    }
}
