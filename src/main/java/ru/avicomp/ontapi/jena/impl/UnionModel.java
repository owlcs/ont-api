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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.enhanced.Personality;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.shared.JenaException;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.utils.Graphs;

import java.io.OutputStream;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link Model model} implementation which encapsulates {@link UnionGraph union graph}.
 * <p>
 * Created by @szuev on 12.04.2018.
 */
@SuppressWarnings("WeakerAccess")
public class UnionModel extends ModelCom {

    public UnionModel(Graph base, Personality<RDFNode> personality) {
        super(asUnionGraph(base), personality);
    }

    public static UnionGraph asUnionGraph(Graph graph) {
        return graph instanceof UnionGraph ? (UnionGraph) graph : new UnionGraph(graph);
    }

    protected Cache<Node, RDFNode> getNodeCache() {
        return enhNodes;
    }

    @Override
    public UnionGraph getGraph() {
        return (UnionGraph) super.getGraph();
    }

    public Graph getBaseGraph() {
        return getGraph().getBaseGraph();
    }

    public Model getBaseModel() {
        return new ModelCom(getBaseGraph());
    }

    @Override
    public UnionModel write(Writer writer) {
        getBaseModel().write(writer);
        return this;
    }

    @Override
    public UnionModel write(Writer writer, String lang) {
        getBaseModel().write(writer, lang);
        return this;
    }

    @Override
    public UnionModel write(Writer writer, String lang, String base) {
        getBaseModel().write(writer, lang, base);
        return this;
    }

    @Override
    public UnionModel write(OutputStream out) {
        getBaseModel().write(out);
        return this;
    }

    @Override
    public UnionModel write(OutputStream out, String lang) {
        getBaseModel().write(out, lang);
        return this;
    }

    @Override
    public UnionModel write(OutputStream out, String lang, String base) {
        getBaseModel().write(out, lang, base);
        return this;
    }

    public boolean isLocal(Statement s) {
        return isLocal(OntJenaException.notNull(s, "Null statement.").getSubject(), s.getPredicate(), s.getObject());
    }

    public boolean isLocal(Resource s, Property p, RDFNode o) {
        return getBaseGraph().contains(s.asNode(), p.asNode(), o.asNode());
    }

    /**
     * Returns a {@link RDFNode} for the given type and, if it is present, caches it in the model.
     * Works silently: no exceptions are expected.
     *
     * @param node {@link Node}
     * @param type {@link Class}-type
     * @param <N>  any subtype of {@link RDFNode}
     * @return {@link RDFNode} or {@code null}
     * @see #getNodeAs(Node, Class)
     * @since 1.3.0
     */
    public <N extends RDFNode> N findNodeAs(Node node, Class<N> type) {
        try {
            return getNodeAs(node, type);
        } catch (OntJenaException.Conversion ignore) {
            // ignore
            return null;
        }
    }

    /**
     * Answer an enhanced node that wraps the given node and conforms to the given interface type.
     *
     * @param node A node (assumed to be in this graph)
     * @param view A type denoting the enhanced facet desired
     * @param <N>  A subtype of {@link RDFNode}
     * @return An enhanced node, not null
     * @throws JenaException in case no RDFNode match found.
     */
    @Override
    public <N extends RDFNode> N getNodeAs(Node node, Class<N> view) {
        try {
            return getNodeAsInternal(node, view);
        } catch (ConversionException e) {
            throw new OntJenaException.Conversion(String.format("Failed to convert node <%s> to <%s>", node, view), e);
        }
    }

    protected ThreadLocal<Set<Node>> visited = ThreadLocal.withInitial(HashSet::new);

    /**
     * Answer an enhanced node that wraps the given node and conforms to the given interface type,
     * taking into account possible graph recursions.
     * For internal usage only.
     *
     * @param node A node (assumed to be in this graph)
     * @param view A type denoting the enhanced facet desired
     * @param <N>  A subtype of {@link RDFNode}
     * @return An enhanced node or {@code null} if no match found
     * @throws OntJenaException.Recursion if a graph recursion is indicated
     * @see #getNodeAs(Node, Class)
     */
    public <N extends RDFNode> N fetchNodeAs(Node node, Class<N> view) {
        // If node has already been seen up the stack, then a graph recursion is detected.
        // Although, in general case, using Map<Class, Set<Node>> seems to be more suitable and careful checking,
        // but it is also a little more expensive.
        // It seems, in the case of ONT-API ObjectFactory implementations, a Set is quite enough.
        Set<Node> nodes = visited.get();
        try {
            if (nodes.add(node)) {
                return getNodeAsInternal(node, view);
            }
            throw new OntJenaException.Recursion("Can't cast to " + OntObjectImpl.viewAsString(view) + ": " +
                    "graph contains a recursion for node <" + node + ">");
        } catch (OntJenaException.Recursion r) {
            throw r;
        } catch (JenaException e) {
            return null;
        } finally {
            nodes.remove(node);
        }
    }

    protected <N extends RDFNode> N getNodeAsInternal(Node node, Class<N> view) {
        return super.getNodeAs(OntJenaException.notNull(node, "Null node"),
                OntJenaException.notNull(view, "Null class view."));
    }

    @Override
    public String toString() {
        return String.format("UnionModel{%s}", Graphs.getName(getBaseGraph()));
    }
}
