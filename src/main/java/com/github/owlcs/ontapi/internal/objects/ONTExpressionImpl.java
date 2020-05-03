/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.objects;

import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.jena.model.*;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.Literal;
import org.semanticweb.owlapi.model.*;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A base class that represents any anonymous OWL2 expression.
 * Created by @szz on 14.08.2019.
 *
 * @param <R> equivalent subtype of {@link OntObject} (that must be ab anonymous resource)
 * @since 2.0.0
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTExpressionImpl<R extends OntObject> extends ONTResourceImpl
        implements ONTComposite, WithContent<ONTExpressionImpl<R>> {

    /**
     * All the {@code OWLObject}'s components are store here.
     * Since this is essentially duplication of the graph information, this store is designed as a soft cache.
     * Note that any graph change will break this object.
     */
    protected final InternalCache.Loading<ONTExpressionImpl<R>, Object[]> content;

    /**
     * Constructs an expression.
     *
     * @param n {@link BlankNodeId}, not {@code null}, that is used as reference
     *          to find the rest {@code OWLObject}'s components from a model (graph)
     * @param m the model-provider, neither {@code null} nor deriving {@code null}
     */
    protected ONTExpressionImpl(BlankNodeId n, Supplier<OntModel> m) {
        super(n, m);
        this.content = createContentCache();
    }

    /**
     * Prepares a content item from the given object property expression using the {@code factory}.
     * This is the inverse of {@link ONTExpressionImpl#toOPE(Object, ModelObjectFactory)}.
     * For internal usage only.
     *
     * @param ope     {@link OntObjectProperty}, not {@code null}
     * @param factory {@link ONTObjectFactory}, not {@code null}
     * @return an {@code Object}, ready for cache
     * @see ONTExpressionImpl#toOPE(Object, ModelObjectFactory)
     */
    protected static Object toContentItem(OntObjectProperty ope, ONTObjectFactory factory) {
        if (ope.isURIResource()) {
            return ope.asNode().getURI();
        }
        return factory.getProperty(ope);
    }

    /**
     * Prepares a content item from the given class expression using the {@code factory}.
     * This is the inverse of {@link ONTExpressionImpl#toCE(Object, ModelObjectFactory)}.
     * For internal usage only.
     *
     * @param ce      {@link OntClass}, not {@code null}
     * @param factory {@link ONTObjectFactory}, not {@code null}
     * @return an {@code Object}, ready for cache
     * @see ONTExpressionImpl#toCE(Object, ModelObjectFactory)
     */
    protected static Object toContentItem(OntClass ce, ONTObjectFactory factory) {
        if (ce.isURIResource()) {
            return ce.asNode().getURI();
        }
        return factory.getClass(ce);
    }

    /**
     * Prepares a content item from the given data range using the {@code factory}.
     * This is the inverse of {@link ONTExpressionImpl#toDR(Object, ModelObjectFactory)}.
     * For internal usage only.
     *
     * @param dr      {@link OntDataRange}, not {@code null}
     * @param factory {@link ONTObjectFactory}, not {@code null}
     * @return an {@code Object}, ready for cache
     * @see ONTExpressionImpl#toDR(Object, ModelObjectFactory)
     */
    protected static Object toContentItem(OntDataRange dr, ONTObjectFactory factory) {
        if (dr.isURIResource()) {
            return dr.asNode().getURI();
        }
        return factory.getDatatype(dr);
    }

    /**
     * Prepares a content item from the given individual using the {@code factory}.
     * This is the inverse of {@link ONTExpressionImpl#toIndividual(Object, ModelObjectFactory)}.
     * For internal usage only.
     *
     * @param i {@link OntIndividual}, not {@code null}
     * @return an {@code Object}, ready for cache
     * @see ONTExpressionImpl#toIndividual(Object, ModelObjectFactory)
     */
    protected static Object toContentItem(OntIndividual i) {
        if (i.isURIResource()) {
            return i.asNode().getURI();
        }
        return i.asNode().getBlankNodeId();
    }

    /**
     * Prepares a content item from the given data property using the {@code factory}.
     * This is the inverse of {@link ONTExpressionImpl#toNDP(Object, ModelObjectFactory)}.
     * For internal usage only.
     *
     * @param ndp {@link OntDataProperty}, not {@code null}
     * @return an {@code Object}, ready for cache
     * @see ONTExpressionImpl#toNDP(Object, ModelObjectFactory)
     */
    protected static Object toContentItem(OntDataProperty ndp) {
        return ndp.asNode().getURI();
    }

    /**
     * Prepares a content item from the literal using the {@code factory}.
     * This is the inverse of {@link ONTExpressionImpl#toLiteral(Object, ModelObjectFactory)}.
     * For internal usage only.
     *
     * @param literal {@link Literal}, not {@code null}
     * @return an {@code Object}, ready for cache
     * @see ONTExpressionImpl#toLiteral(Object, ModelObjectFactory)
     */
    protected static Object toContentItem(Literal literal) {
        return literal.asNode().getLiteral();
    }

    /**
     * Collects the cache Array.
     * The array was chosen as the best option in sense of memory consumption and access speed.
     *
     * @param obj     {@link R}, not {@code null}
     * @param factory {@link ONTObjectFactory}, not {@code null}
     * @return an {@code Array} of {@code Object}s (content items)
     * @see ONTExpressionImpl#initContent(OntObject, ONTObjectFactory)
     */
    protected abstract Object[] collectContent(R obj, ONTObjectFactory factory);

    /**
     * Initializes the object's content and calculates its hashcode.
     * Together, this must be faster.
     *
     * @param obj     {@link R} the source Jena resource, not {@code null}
     * @param factory {@link ONTObjectFactory}, not {@code null}
     * @return an {@code Array} of {@code Object}s (content items)
     * @see ONTExpressionImpl#collectContent(OntObject, ONTObjectFactory)
     * @see OWLObject#initHashCode()
     */
    protected abstract Object[] initContent(R obj, ONTObjectFactory factory);

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
    public InternalCache.Loading<ONTExpressionImpl<R>, Object[]> getContentCache() {
        return content;
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
            ONTExpressionImpl<?> expr = (ONTExpressionImpl<?>) other;
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

    /**
     * Restores an {@link OWLIndividual} from the content {@code item} using the {@code factory}.
     * This is the inverse of {@link ONTExpressionImpl#toContentItem(OntIndividual)}.
     * For internal usage only.
     *
     * @param item    {@code Object}, for the individual, not {@code null}
     * @param factory {@link ONTObjectFactory}, not {@code null}
     * @return {@link ONTObject} with {@link OWLIndividual}
     * @see ONTExpressionImpl#toContentItem(OntIndividual)
     */
    protected ONTObject<? extends OWLIndividual> toIndividual(Object item, ModelObjectFactory factory) {
        if (item instanceof String) {
            return factory.getNamedIndividual((String) item);
        }
        return factory.getAnonymousIndividual((BlankNodeId) item);
    }

    /**
     * Restores an {@link OWLObjectPropertyExpression} from the content {@code item} using the {@code factory}.
     * This is the inverse of {@link ONTExpressionImpl#toContentItem(OntObjectProperty, ONTObjectFactory)}.
     * For internal usage only.
     *
     * @param item    {@code Object}, for the object property expression, not {@code null}
     * @param factory {@link ModelObjectFactory}, not {@code null}
     * @return {@link ONTObject} with {@link OWLObjectPropertyExpression}
     * @see ONTExpressionImpl#toContentItem(OntObjectProperty, ONTObjectFactory)
     */
    @SuppressWarnings("unchecked")
    protected ONTObject<? extends OWLObjectPropertyExpression> toOPE(Object item, ModelObjectFactory factory) {
        if (item instanceof String) {
            return factory.getObjectProperty((String) item);
        }
        return (ONTObject<? extends OWLObjectPropertyExpression>) item;
    }

    /**
     * Restores an {@link OWLDataProperty} from the content {@code item} using the {@code factory}.
     * This is the inverse of {@link ONTExpressionImpl#toContentItem(OntDataProperty)}.
     * For internal usage only.
     *
     * @param item    {@code Object}, for the data property, not {@code null}
     * @param factory {@link ModelObjectFactory}, not {@code null}
     * @return {@link ONTObject} with {@link OWLDataProperty}
     * @see ONTExpressionImpl#toContentItem(OntDataProperty)
     */
    @SuppressWarnings("unchecked")
    protected ONTObject<OWLDataProperty> toNDP(Object item, ModelObjectFactory factory) {
        if (item instanceof String) {
            return factory.getDataProperty((String) item);
        }
        return (ONTObject<OWLDataProperty>) item;
    }

    /**
     * Restores an {@link OWLClassExpression} from the content {@code item} using the {@code factory}.
     * This is the inverse of {@link ONTExpressionImpl#toContentItem(OntClass, ONTObjectFactory)}.
     * For internal usage only.
     *
     * @param item    {@code Object}, for the class expression, not {@code null}
     * @param factory {@link ModelObjectFactory}, not {@code null}
     * @return {@link ONTObject} with {@link OWLClassExpression}
     * @see ONTExpressionImpl#toContentItem(OntClass, ONTObjectFactory)
     */
    @SuppressWarnings("unchecked")
    protected ONTObject<? extends OWLClassExpression> toCE(Object item, ModelObjectFactory factory) {
        if (item instanceof String) {
            return factory.getClass((String) item);
        }
        return (ONTObject<? extends OWLClassExpression>) item;
    }

    /**
     * Restores an {@link OWLDataRange} from the content {@code item} using the {@code factory}.
     * This is the inverse of {@link ONTExpressionImpl#toContentItem(OntDataRange, ONTObjectFactory)}.
     * For internal usage only.
     *
     * @param item    {@code Object}, for the data range, not {@code null}
     * @param factory {@link ModelObjectFactory}, not {@code null}
     * @return {@link ONTObject} with {@link OWLDataRange}
     * @see ONTExpressionImpl#toContentItem(OntDataRange, ONTObjectFactory)
     */
    @SuppressWarnings("unchecked")
    protected ONTObject<? extends OWLDataRange> toDR(Object item, ModelObjectFactory factory) {
        if (item instanceof String) {
            return factory.getDatatype((String) item);
        }
        return (ONTObject<OWLDataRange>) item;
    }

    /**
     * Restores an {@link OWLLiteral} from the content {@code item} using the {@code factory}.
     * This is the inverse of {@link ONTExpressionImpl#toContentItem(Literal)}.
     * For internal usage only.
     *
     * @param item    {@code Object}, for the literal, not {@code null}
     * @param factory {@link ONTObjectFactory}, not {@code null}
     * @return {@link ONTObject} with {@link OWLLiteral}
     * @see ONTExpressionImpl#toContentItem(Literal)
     */
    protected ONTObject<OWLLiteral> toLiteral(Object item, ModelObjectFactory factory) {
        return factory.getLiteral((LiteralLabel) item);
    }
}
