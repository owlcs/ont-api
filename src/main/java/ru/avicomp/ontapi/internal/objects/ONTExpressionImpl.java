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

package ru.avicomp.ontapi.internal.objects;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.HasComponents;
import org.semanticweb.owlapi.model.HasOperands;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.internal.InternalCache;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.utils.Iter;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A base class that represents any anonymous OWL2 expression.
 * Created by @szz on 14.08.2019.
 *
 * @param <R> equivalent subtype of {@link OntObject} (that must be ab anonymous resource)
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTExpressionImpl<R extends OntObject> extends ONTResourceImpl {

    /**
     * All the {@code OWLObject}'s components are store here.
     * Since this is essentially duplication of the graph information, this store is designed as a soft cache.
     * Note that any graph change will break this object.
     */
    protected final InternalCache.Loading<ONTExpressionImpl, Object[]> content;

    /**
     * Constructs an expression.
     *
     * @param n {@link BlankNodeId}, not {@code null}, that is used as reference
     *          to find the rest {@code OWLObject}'s components from a model (graph)
     * @param m the model-provider, neither {@code null} nor deriving {@code null}
     */
    protected ONTExpressionImpl(BlankNodeId n, Supplier<OntGraphModel> m) {
        super(n, m);
        this.content = InternalCache.createSoftSingleton(x -> collectContent());
    }

    /**
     * Lists all components in the form of {@code Iterator}.
     * Neither this object or component objects are not included in result: it content only top-level direct components.
     * Note that {@link #components()} contains also non-{@link OWLObject} things:
     * integers (e.g. cardinality), {@code List}s (e.g. {@code ObjectOneOf}).
     *
     * @return {@link ExtendedIterator} of {@link ONTObject}s
     * @see HasComponents#components()
     * @see HasOperands#operands()
     */
    public abstract ExtendedIterator<ONTObject<? extends OWLObject>> listComponents();

    /**
     * Collects the cache Array.
     * The array was chosen as the best option in sense of memory consumption and access speed.
     *
     * @param ce {@link R}, not {@code null}
     * @param of {@link InternalObjectFactory}, not {@code null}
     * @return {@code Array} of {@code Object}s
     */
    protected abstract Object[] collectContent(R ce, InternalObjectFactory of);

    @Override
    protected BlankNodeId getBlankNodeId() {
        return (BlankNodeId) node;
    }

    @Override
    public Node asNode() {
        return NodeFactory.createBlankNode(getBlankNodeId());
    }

    @Override
    public abstract R asResource();

    /**
     * Lists all components in the form of {@code Stream}.
     * Neither this object or component objects are not included in result: it content only top-level direct components.
     *
     * @return {@code Stream} of {@link ONTObject}s
     * @see ONTExpressionImpl#listComponents()
     */
    public final Stream<ONTObject<? extends OWLObject>> objects() {
        return Iter.asStream(listComponents(), Spliterator.NONNULL | Spliterator.ORDERED);
    }

    @Override
    public Stream<Triple> triples() {
        return Stream.concat(super.triples(), objects().flatMap(ONTObject::triples));
    }

    /**
     * Collects the cache.
     *
     * @return {@code Array} of {@code Object}s
     * @see #collectContent(OntObject, InternalObjectFactory)
     */
    protected final Object[] collectContent() {
        return collectContent(asResource(), getObjectFactory());
    }

    /**
     * Gets the content from cache.
     *
     * @return {@code Array} of {@code Object}s
     */
    protected Object[] getContent() {
        return content.get(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof OWLObject)) {
            return false;
        }
        OWLObject other = (OWLObject) obj;
        if (typeIndex() != other.typeIndex()) {
            return false;
        }
        if (other instanceof ONTExpressionImpl) {
            ONTExpressionImpl expr = (ONTExpressionImpl) other;
            if (notSame(expr)) {
                return false;
            }
            if (node.equals(expr.node)) {
                return true;
            }
            return Arrays.equals(getContent(), expr.getContent());
        }
        // then OWL-API instance is given:
        if (hashCode() != other.hashCode()) {
            return false;
        }
        return equalIterators(components().iterator(), other.components().iterator());
    }
}
