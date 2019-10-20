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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.RDFNode;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.ObjectFactory;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;

import java.util.Set;

/**
 * An abstraction to work with {@link OntPersonality}
 * and an interface-analog of the {@link EnhGraph Jena Enhanced Graph},
 * and also a facility to provide implicit links between different
 * {@link ru.avicomp.ontapi.jena.impl.conf.ObjectFactory} factories within a model.
 * A {@link ru.avicomp.ontapi.jena.model.OntGraphModel} is assumed to be {@link PersonalityModel}.
 * <p>
 * Explicit links between object factories are undesirable, since replacing one of the factories will affect others.
 * But using this interface it is possible to build safe implicit links and
 * replacing one factory with a custom implementation will not break the whole model.
 * More about this see in the description for
 * the method {@link ru.avicomp.ontapi.jena.impl.conf.PersonalityBuilder#add(Class, ObjectFactory)}.
 * <p>
 * Created by @ssz on 18.01.2019.
 *
 * @since 1.4.0
 */
public interface PersonalityModel {

    /**
     * Returns the model personality, that is unmodifiable model's configuration storage.
     *
     * @return {@link OntPersonality}
     */
    OntPersonality getOntPersonality();

    /**
     * Answers an enhanced node that wraps the given {@link Node node} and conforms to the given interface type.
     *
     * @param node a {@link Node node}, that is assumed to be in this graph
     * @param view a type denoting the enhanced facet desired
     * @param <N>  a subtype of {@link RDFNode}
     * @return an enhanced node, not {@code null}
     * @throws OntJenaException in case no RDFNode match found
     * @see PersonalityModel#findNodeAs(Node, Class)
     * @see EnhGraph#getNodeAs(Node, Class)
     */
    <N extends RDFNode> N getNodeAs(Node node, Class<N> view);

    /**
     * Answers an enhanced node that wraps the given {@link Node node} and conforms to the given interface type.
     * It works silently: no exception is thrown, instead returns {@code null}.
     *
     * @param node {@link Node}
     * @param type {@link Class}-type
     * @param <N>  any subtype of {@link RDFNode}
     * @return {@link RDFNode} or {@code null}
     * @see PersonalityModel#getNodeAs(Node, Class)
     */
    <N extends RDFNode> N findNodeAs(Node node, Class<N> type);

    /**
     * Answers an enhanced node that wraps the given {@link Node node} and conforms to the given interface type,
     * taking into account possible graph recursions.
     *
     * @param node a {@link Node node}, that is assumed to be in this graph
     * @param view a type denoting the enhanced facet desired
     * @param <N>  a subtype of {@link RDFNode}
     * @return an enhanced node or {@code null} if no match found
     * @throws OntJenaException.Recursion if a graph recursion is indicated
     * @see PersonalityModel#getNodeAs(Node, Class)
     */
    <N extends RDFNode> N fetchNodeAs(Node node, Class<N> view);

    /**
     * Returns all {@link Node}s from the {@link OntPersonality#getReserved() reserved} vocabulary,
     * that cannot be represented as the specified {@code type} in the model.
     *
     * @param type a {@code Class}-type of {@link OntObject}, not {@code null}
     * @return a {@code Set} of {@link Node}s
     * @since 1.4.2
     */
    Set<Node> getSystemResources(Class<? extends OntObject> type);

    /**
     * Represents the given {@code EnhGraph} as a {@link PersonalityModel}.
     *
     * @param graph {@link EnhGraph enhanced graph},
     *              that is also assumed to be {@link OntGraphModel}, not {@code null}
     * @return {@link PersonalityModel}
     * @throws OntJenaException in case the conversion is not possible
     * @see OntPersonality#asJenaPersonality(OntPersonality)
     */
    static PersonalityModel asPersonalityModel(EnhGraph graph) throws OntJenaException {
        if (graph instanceof PersonalityModel) {
            return (PersonalityModel) graph;
        }
        throw new OntJenaException.IllegalArgument("The given EnhGraph is not a PersonalityModel: " + graph);
    }

    /**
     * Represents the given {@code OntGraphModel} as a {@link PersonalityModel}.
     *
     * @param graph {@link OntGraphModel OWL graph model},
     *              that is also assumed to be {@link EnhGraph}, not {@code null}
     * @return {@link PersonalityModel}
     * @throws OntJenaException in case the conversion is not possible
     * @see OntPersonality#asJenaPersonality(OntPersonality)
     */
    static PersonalityModel asPersonalityModel(OntGraphModel graph) throws OntJenaException {
        if (graph instanceof PersonalityModel) {
            return (PersonalityModel) graph;
        }
        throw new OntJenaException.IllegalArgument("The given OntGraphModel is not a PersonalityModel: " + graph);
    }

    /**
     * Checks if the given {@link Node node} can be viewed as the given type.
     * This method caches the enhanced node at the model level, if it is possible,
     * and, opposite to the method {@link PersonalityModel#findNodeAs(Node, Class)},
     * takes care about possible graph recursions.
     *
     * @param view  Class-type
     * @param node  {@link Node}
     * @param graph {@link EnhGraph}, assumed to be {@link OntGraphModelImpl}
     * @return {@code true} if the node can be safely casted to the specified type
     */
    static boolean canAs(Class<? extends RDFNode> view, Node node, EnhGraph graph) {
        return asPersonalityModel(graph).fetchNodeAs(node, view) != null;
    }

}
