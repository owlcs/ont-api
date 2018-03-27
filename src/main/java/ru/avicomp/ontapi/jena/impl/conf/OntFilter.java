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

package ru.avicomp.ontapi.jena.impl.conf;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * To filter resources.
 * Used in factory ({@link CommonOntObjectFactory}).
 * <p>
 * Created by szuev on 07.11.2016.
 */
@FunctionalInterface
public interface OntFilter {
    OntFilter TRUE = (n, g) -> true;
    OntFilter FALSE = (n, g) -> false;
    OntFilter URI = (n, g) -> n.isURI();
    OntFilter BLANK = (n, g) -> n.isBlank();

    boolean test(Node n, EnhGraph g);

    default OntFilter and(OntFilter other) {
        OntJenaException.notNull(other, "Null and-filter.");
        return (Node n, EnhGraph g) -> test(n, g) && other.test(n, g);
    }

    default OntFilter or(OntFilter other) {
        OntJenaException.notNull(other, "Null or-filter.");
        return (Node n, EnhGraph g) -> test(n, g) || other.test(n, g);
    }

    default OntFilter negate() {
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
            this.predicate = OntJenaException.notNull(predicate, "Null predicate.").asNode();
        }

        @Override
        public boolean test(Node n, EnhGraph g) {
            return g.asGraph().contains(n, predicate, Node.ANY);
        }
    }

    class HasType implements OntFilter {
        protected final Node type;

        public HasType(Resource type) {
            this.type = OntJenaException.notNull(type, "Null type.").asNode();
        }

        @Override
        public boolean test(Node node, EnhGraph eg) {
            return eg.asGraph().contains(node, RDF.type.asNode(), type);
        }
    }

    class OneOf implements OntFilter {
        protected final Set<Node> nodes;

        public OneOf(Collection<? extends RDFNode> types) {
            this.nodes = Optional.ofNullable(types).orElse(Collections.emptySet())
                    .stream().map(RDFNode::asNode).collect(Collectors.toSet());
        }

        @Override
        public boolean test(Node n, EnhGraph g) {
            return nodes.contains(n);
        }
    }
}
