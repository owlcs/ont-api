/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.jena.impl.conf;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * A class-helper to perform the preliminary resource search in a model.
 * Subsequently, the search result Stream will be filtered by the {@link OntFilter} instance.
 * Used as a component in {@link CommonFactoryImpl default factory} and {@link MultiFactoryImpl} implementations
 * <p>
 * Created by szuev on 07.11.2016.
 */
@FunctionalInterface
public interface OntFinder {
    OntFinder ANY_SUBJECT = eg -> Graphs.subjects(eg.asGraph());
    OntFinder ANY_SUBJECT_AND_OBJECT = eg -> Graphs.subjectsAndObjects(eg.asGraph());
    OntFinder ANYTHING = eg -> Graphs.all(eg.asGraph());
    OntFinder TYPED = new ByPredicate(RDF.type);

    /**
     * Returns an iterator over the nodes in the given model, which satisfy some criterion,
     * specific to this {@link OntFinder}.
     * It is expected that the result does not contain duplicates.
     *
     * @param eg {@link EnhGraph}, model
     * @return {@link ExtendedIterator} of {@link Node}s
     */
    ExtendedIterator<Node> iterator(EnhGraph eg);

    /**
     * Lists the nodes from the specified model by the encapsulated criterion.
     *
     * @param eg {@link EnhGraph}, model
     * @return {@link Stream} of {@link Node}s
     */
    default Stream<Node> find(EnhGraph eg) {
        return Iter.asStream(iterator(eg));
    }

    default OntFinder restrict(OntFilter filter) {
        if (Objects.requireNonNull(filter, "Null restriction filter.").equals(OntFilter.TRUE)) return this;
        if (filter.equals(OntFilter.FALSE)) return eg -> NullIterator.instance();
        return eg -> iterator(eg).filterKeep(n -> filter.test(n, eg));
    }

    class ByType implements OntFinder {
        protected final Node type;

        public ByType(Resource type) {
            this.type = Objects.requireNonNull(type, "Null type.").asNode();
        }

        @Override
        public ExtendedIterator<Node> iterator(EnhGraph eg) {
            return eg.asGraph().find(Node.ANY, RDF.Nodes.type, type).mapWith(Triple::getSubject);
        }
    }

    class ByPredicate implements OntFinder {
        protected final Node predicate;

        public ByPredicate(Property predicate) {
            this.predicate = Objects.requireNonNull(predicate, "Null predicate.").asNode();
        }

        @Override
        public ExtendedIterator<Node> iterator(EnhGraph eg) {
            return Iter.distinct(eg.asGraph().find(Node.ANY, predicate, Node.ANY).mapWith(Triple::getSubject));
        }
    }
}
