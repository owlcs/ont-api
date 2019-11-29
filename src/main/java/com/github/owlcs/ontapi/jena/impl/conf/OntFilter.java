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

package com.github.owlcs.ontapi.jena.impl.conf;

import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * To filter resources.
 * Used by {@link CommonFactoryImpl default factory} and {@link MultiFactoryImpl} implementations as a component.
 * <p>
 * Created by szuev on 07.11.2016.
 */
@FunctionalInterface
public interface OntFilter {
    OntFilter TRUE = (n, g) -> true;
    OntFilter FALSE = (n, g) -> false;
    OntFilter URI = (n, g) -> n.isURI();
    OntFilter BLANK = (n, g) -> n.isBlank();

    /**
     * Tests if the given {@link Node node} suits the encapsulated conditions in bounds of the specified {@link EnhGraph graph}.
     *
     * @param n {@link Node}, not {@code null}
     * @param g {@link EnhGraph}, not {@code null}
     * @return boolean
     */
    boolean test(Node n, EnhGraph g);

    default OntFilter and(OntFilter other) {
        if (Objects.requireNonNull(other, "Null and-filter.").equals(TRUE)) {
            return this;
        }
        if (this.equals(TRUE)) return other;
        if (other.equals(FALSE)) return FALSE;
        if (this.equals(FALSE)) return FALSE;
        return (Node n, EnhGraph g) -> this.test(n, g) && other.test(n, g);
    }

    default OntFilter or(OntFilter other) {
        if (Objects.requireNonNull(other, "Null or-filter.").equals(TRUE)) {
            return TRUE;
        }
        if (this.equals(TRUE)) return TRUE;
        if (other.equals(FALSE)) return this;
        if (this.equals(FALSE)) return other;
        return (Node n, EnhGraph g) -> this.test(n, g) || other.test(n, g);
    }

    @SuppressWarnings("unused")
    default OntFilter negate() {
        if (this.equals(TRUE)) return FALSE;
        if (this.equals(FALSE)) return TRUE;
        return (Node n, EnhGraph g) -> !test(n, g);
    }

    default OntFilter accumulate(OntFilter... filters) {
        OntFilter res = this;
        for (OntFilter o : filters) {
            res = res.and(o);
        }
        return res;
    }

    class HasPredicate implements OntFilter {
        protected final Node predicate;

        public HasPredicate(Property predicate) {
            this.predicate = Objects.requireNonNull(predicate, "Null predicate.").asNode();
        }

        @Override
        public boolean test(Node n, EnhGraph g) {
            return g.asGraph().contains(n, predicate, Node.ANY);
        }
    }

    class HasType implements OntFilter {
        protected final Node type;

        public HasType(Resource type) {
            this.type = Objects.requireNonNull(type, "Null type.").asNode();
        }

        @Override
        public boolean test(Node node, EnhGraph eg) {
            return eg.asGraph().contains(node, RDF.Nodes.type, type);
        }
    }

    class OneOf implements OntFilter {
        protected final Set<Node> nodes;

        public OneOf(Collection<? extends RDFNode> types) {
            this.nodes = Objects.requireNonNull(types).stream().map(RDFNode::asNode).collect(Iter.toUnmodifiableSet());
        }

        @Override
        public boolean test(Node n, EnhGraph g) {
            return nodes.contains(n);
        }

        @Override
        public boolean equals(Object o) {
            if (nodes.isEmpty() && FALSE == o) return true;
            return super.equals(o);
        }
    }
}
