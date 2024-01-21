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

package com.github.sszuev.jena.ontapi.utils;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.util.NodeCmp;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;

/**
 * A class-helper to work with {@link Model Jena Model}s and its related objects and components:
 * {@link RDFNode Jena RDF Node}, {@link Literal Jena Literal}, {@link Resource Jena Resource} and
 * {@link Statement Jena Statement}.
 * <p>
 * Created @ssz on 20.10.2016.
 */
public class Models {
    public static final Comparator<RDFNode> RDF_NODE_COMPARATOR = (r1, r2) -> NodeCmp.compareRDFTerms(r1.asNode(), r2.asNode());
    public static final RDFNode BLANK = new ResourceImpl();
    public static final Comparator<Statement> STATEMENT_COMPARATOR_IGNORE_BLANK = Comparator
            .comparing((Function<Statement, RDFNode>) s -> s.getSubject().isAnon() ? BLANK : s.getSubject(),
                    RDF_NODE_COMPARATOR)
            .thenComparing(s -> s.getPredicate().isAnon() ? BLANK : s.getPredicate(), RDF_NODE_COMPARATOR)
            .thenComparing(s -> s.getObject().isAnon() ? BLANK : s.getObject(), RDF_NODE_COMPARATOR);

    public static final Literal TRUE = ResourceFactory.createTypedLiteral(Boolean.TRUE);

    /**
     * Answers a set of all the RDF statements whose subject is one of the cells of the given list.
     *
     * @param list []-list, not {@code null}
     * @return a {@code Set} of {@link Statement}s
     */
    public static Set<Statement> getListStatements(RDFList list) {
        return ((RDFListImpl) list).collectStatements();
    }

    /**
     * Returns a string representation of the given Jena statement taking into account PrefixMapping.
     *
     * @param st {@link Statement}, not {@code null}
     * @param pm {@link PrefixMapping}, not {@code null}
     * @return {@code String}
     */
    public static String toString(Statement st, PrefixMapping pm) {
        return String.format("[%s, %s, %s]",
                st.getSubject().asNode().toString(pm, false),
                st.getPredicate().asNode().toString(pm, false),
                st.getObject().asNode().toString(pm, true));
    }

    /**
     * Returns a string representation of the given Jena statement.
     *
     * @param inModel {@link Statement}, not {@code null}
     * @return {@code String}
     */
    public static String toString(Statement inModel) {
        return toString(inModel, inModel.getModel());
    }

    /**
     * Answers {@code true} if the given {@code node} contains the specified {@code uri}.
     *
     * @param node {@link RDFNode}, not {@code null}
     * @param uri  {@code String}, not {@code null}
     * @return boolean
     */
    public static boolean containsURI(RDFNode node, String uri) {
        if (node.isURIResource()) {
            return uri.equals(node.asResource().getURI());
        }
        return node.isLiteral() && uri.equals(node.asLiteral().getDatatypeURI());
    }

    /**
     * Answers {@code true} if the given {@code uri} is a part of the given {@code statement}.
     *
     * @param statement {@link Statement}, not {@code null}
     * @param uri       {@code String}, not {@code null}
     * @return boolean
     */
    public static boolean containsURI(Statement statement, String uri) {
        if (uri.equals(statement.getSubject().getURI())) return true;
        if (uri.equals(statement.getPredicate().getURI())) return true;
        return containsURI(statement.getObject(), uri);
    }
}
