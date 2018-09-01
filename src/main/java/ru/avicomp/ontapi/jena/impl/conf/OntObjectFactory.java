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
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.util.iterator.ExtendedIterator;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.utils.Iter;

import java.util.stream.Stream;

/**
 * Extended {@link Implementation} factory,
 * the base class for any {@link ru.avicomp.ontapi.jena.model.OntObject Ontology Object} factories.
 * Used to bind implementation (node) and interface.
 * Also, in addition to the standard jena methods,
 * this implementation includes the functionalities of searching for nodes
 * and graph transforming in accordance with the specified interface.
 * <p>
 * Created by @szuev on 03.11.2016.
 */
public abstract class OntObjectFactory extends Implementation {

    /**
     * Makes some changes the in graph and returns the wrapped node
     *
     * @param node The node to be wrapped
     * @param eg   The graph which would contain the result{@link EnhNode}.
     * @return a new enhanced node which wraps node but presents the interface(s) that this factory encapsulates.
     * @throws OntJenaException.Creation in case modification of graph is not allowed for the specified node.
     */
    public EnhNode create(Node node, EnhGraph eg) {
        throw new OntJenaException("Creation is not allowed: " + node);
    }

    /**
     * Determines if modifying of the graph ({@link EnhGraph}) is allowed for the node ({@link Node}).
     *
     * @param node {@link Node} the node to test that changes are permitted.
     * @param eg   {@link EnhGraph} the graph to be changed.
     * @return true if creation is allowed.
     */
    public boolean canCreate(Node node, EnhGraph eg) {
        return false;
    }

    /**
     * Returns stream of nodes with interface that this factory encapsulates.
     *
     * @param eg the graph containing the node
     * @return the stream of enhanced and suitability nodes
     */
    public Stream<EnhNode> find(EnhGraph eg) {
        return Iter.asStream(iterator(eg));
    }

    /**
     * Returns an {@link ExtendedIterator ExtendedIterator} of {@link EnhNode node}s with interface that this factory encapsulates.
     *
     * @param eg {@link EnhGraph}
     * @return {@link ExtendedIterator} of {@link EnhNode}s
     */
    public abstract ExtendedIterator<EnhNode> iterator(EnhGraph eg);

    /**
     * Checks that the wrapping (node, eg) would succeed.
     *
     * @param node node the node to test for suitability
     * @param eg   the enhanced graph the node appears in
     * @return true iff the node can represent our type in that graph
     */
    public abstract boolean canWrap(Node node, EnhGraph eg);

    /**
     * Does wrapping {@link Node} to {@link EnhNode} without any checking or throwing exception.
     * Note: some realisation may return {@code null}
     * For internal usage only.
     *
     * @param node {@link Node}
     * @param eg   {@link EnhGraph}
     * @return {@link EnhNode}
     */
    protected abstract EnhNode doWrap(Node node, EnhGraph eg);

    /**
     * Creates a new {@link EnhNode} wrapping a {@link Node} node in the context of an graph {@link EnhGraph}
     *
     * @param node the node to be wrapped
     * @param eg   the graph containing the node
     * @return A new enhanced node which wraps node but presents the interface(s) that this factory encapsulates.
     * @throws ConversionException in case wrapping is impossible.
     */
    @Override
    public EnhNode wrap(Node node, EnhGraph eg) {
        if (!canWrap(node, eg))
            throw new ConversionException("Can't wrap node " + node + ". Use direct factory.");
        return doWrap(node, eg);
    }
}
