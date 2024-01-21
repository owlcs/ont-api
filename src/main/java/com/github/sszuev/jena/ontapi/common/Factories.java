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

package com.github.sszuev.jena.ontapi.common;

import com.github.sszuev.jena.ontapi.impl.objects.OntObjectImpl;
import com.github.sszuev.jena.ontapi.model.OntObject;
import com.github.sszuev.jena.ontapi.utils.Iterators;
import com.github.sszuev.jena.ontapi.vocabulary.RDF;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * A helper (factory) to produce {@link ObjectFactory object factory} instances or its components.
 * <p>
 * Created by @ssz on 19.01.2019.
 */
public class Factories {

    @SafeVarargs
    public static ObjectFactory createFrom(OntFinder finder, Class<? extends OntObject>... types) {
        return createFrom(finder, Arrays.stream(types));
    }

    public static ObjectFactory createFrom(OntFinder finder, Stream<Class<? extends OntObject>> types) {
        return createMulti(finder, types.map(WrappedFactoryImpl::new));
    }

    public static ObjectFactory createCommon(Class<? extends OntObjectImpl> impl,
                                             OntFinder finder,
                                             OntFilter filter,
                                             OntFilter... additional) {
        return createCommon(new OntMaker.Default(impl), finder, filter, additional);
    }

    public static ObjectFactory createCommon(OntMaker maker, OntFinder finder, OntFilter primary, OntFilter... additional) {
        return new CommonFactoryImpl(Objects.requireNonNull(maker, "Null maker"),
                Objects.requireNonNull(finder, "Null finder"),
                Objects.requireNonNull(primary, "Null filter").accumulate(additional));
    }

    public static ObjectFactory createCommon(Class<? extends OntObject> type,
                                             OntMaker maker,
                                             OntFinder finder,
                                             OntFilter filter) {
        Objects.requireNonNull(type, "Null type");
        return new CommonFactoryImpl(Objects.requireNonNull(maker, "Null maker"),
                Objects.requireNonNull(finder, "Null finder"),
                Objects.requireNonNull(filter, "Null filter")) {

            @Override
            public String toString() {
                return String.format("ObjectFactory[%s]", OntObjectImpl.viewAsString(type));
            }
        };
    }

    public static OntFinder createFinder(Resource... types) {
        return createFinder(FrontsNode::asNode, types);
    }

    @SafeVarargs
    public static <R> OntFinder createFinder(Function<R, Node> asNode, R... types) {
        return eg -> Iterators.distinct(listTriplesForTypes(eg.asGraph(), asNode, types).mapWith(Triple::getSubject));
    }

    private static ObjectFactory createMulti(OntFinder finder, Stream<ObjectFactory> factories) {
        return new MultiFactoryImpl(Objects.requireNonNull(finder, "Null finder"), null,
                factories.peek(x -> Objects.requireNonNull(x, "Null component-factory")).toArray(ObjectFactory[]::new));
    }

    @SafeVarargs
    private static <R> ExtendedIterator<Triple> listTriplesForTypes(Graph g, Function<R, Node> asNode, R... types) {
        return Iterators.flatMap(Iterators.of(types).mapWith(asNode), t -> g.find(Node.ANY, RDF.Nodes.type, t));
    }
}
