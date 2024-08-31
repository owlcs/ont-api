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

package com.github.owlcs.ontapi.internal.axioms;

import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.WriteHelper;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontapi.utils.Graphs;
import org.semanticweb.owlapi.model.HasIRI;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by @ssz on 24.10.2020.
 *
 * @see com.github.owlcs.ontapi.internal.ReadHelper
 * @see WriteHelper
 */
public class TranslateHelper {
    /**
     * Accumulates the input {@link OWLObject}s extracted from {@link ONTObject}-containers into a {@code Set}.
     *
     * @param objects a {@code Collection} of {@link ONTObject}s, not {@code null}
     * @param <X>     a type of {@link OWLObject}
     * @return a {@code Set} of {@link X}s
     */
    public static <X extends OWLObject> Set<X> toSet(Collection<? extends ONTObject<? extends X>> objects) {
        return objects(objects).collect(Collectors.toSet());
    }

    /**
     * Lists all {@link OWLObject}s from the collection of {@link ONTObject}s
     *
     * @param objects a {@code Collection} of {@link ONTObject}s, not {@code null}
     * @param <X>     a type of {@link OWLObject}
     * @return a {@code Stream} of {@link X}s
     */
    public static <X extends OWLObject> Stream<X> objects(Collection<? extends ONTObject<? extends X>> objects) {
        return objects.stream().map(ONTObject::getOWLObject);
    }

    /**
     * Constructs or retrieves a {@code Node} from the given {@code OWLObject}.
     * Expressions and external anonymous individuals are ignored.
     * This is to perform optimization searching in a graph.
     *
     * @param obj {@link OWLObject}, not {@code null}
     * @return {@code Node} or {@code null}
     */
    public static Node getSearchNode(OWLObject obj) {
        if (obj.isIRI()) {
            return WriteHelper.toNode((IRI) obj);
        }
        if (obj instanceof HasIRI) {
            return WriteHelper.toNode((HasIRI) obj);
        }
        if (obj instanceof OWLAnonymousIndividual) {
            return WriteHelper.toNode((OWLAnonymousIndividual) obj);
        }
        if (obj instanceof OWLLiteral) {
            return WriteHelper.toNode((OWLLiteral) obj);
        }
        return null;
    }

    /**
     * Creates a {@code Node} to be used while searching.
     *
     * @param obj {@link OWLObject}, not {@code null}
     * @return {@code Node} or {@link Node#ANY ANY}
     */
    static Node getSearchNodeOrANY(OWLObject obj) {
        Node res = getSearchNode(obj);
        return res == null ? Node.ANY : res;
    }

    /**
     * Answers a triple that can be used while searching (as a pattern or a concrete triple).
     *
     * @param t {@link Triple}, not {@code null}
     * @return {@link Triple} either named or with {@link Node#ANY ANY} subject or object
     */
    static Triple toSearchTriple(Triple t) {
        return Graphs.isNamedTriple(t) ? t : Triple.create(uriOrANY(t.getSubject()), t.getPredicate(), uriOrANY(t.getObject()));
    }

    /**
     * Answers {@code true} if the given {@code triple} is good to be used in searching.
     *
     * @param t {@link Triple}, not {@code null}
     * @return {@code boolean}
     */
    static boolean isGoodSearchTriple(Triple t) {
        return t.getSubject() != Node.ANY || t.getObject() != Node.ANY;
    }

    private static Node uriOrANY(Node n) {
        return n.isURI() ? n : Node.ANY;
    }
}
