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
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.internal.InternalCache;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;

import java.util.Arrays;
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
public abstract class ONTExpressionImpl<R extends OntObject> extends ONTResourceImpl
        implements ONTComposite, WithContent<ONTExpressionImpl> {

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
        this.content = createContent();
    }

    /**
     * Collects the cache Array.
     * The array was chosen as the best option in sense of memory consumption and access speed.
     *
     * @param obj {@link R}, not {@code null}
     * @param of  {@link InternalObjectFactory}, not {@code null}
     * @return {@code Array} of {@code Object}s
     */
    protected abstract Object[] collectContent(R obj, InternalObjectFactory of);

    @Override
    protected BlankNodeId getBlankNodeId() {
        return (BlankNodeId) node;
    }

    @Override
    public Node asNode() {
        return NodeFactory.createBlankNode(getBlankNodeId());
    }

    @Override
    public abstract R asRDFNode();

    @Override
    public Stream<Triple> triples() {
        return Stream.concat(super.triples(), objects().flatMap(ONTObject::triples));
    }

    @Override
    public final Object[] collectContent() {
        return collectContent(asRDFNode(), getObjectFactory());
    }

    @Override
    public Object[] getContent() {
        return content.get(this);
    }

    @Override
    public void putContent(Object[] content) {
        this.content.put(this, content);
    }

    @Override
    public boolean hasContent() {
        return content.isEmpty();
    }

    @Override
    public void clearContent() {
        content.clear();
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
