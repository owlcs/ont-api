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

import org.apache.jena.graph.*;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.internal.HasObjectFactory;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.owlapi.OWLObjectImpl;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A base triple object-component that is attached to a model.
 * Created by @ssz on 17.08.2019.
 *
 * @see ONTResourceImpl
 * @see OntStatement
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTBaseTripleImpl extends OWLObjectImpl implements OWLObject, HasObjectFactory, FrontsTriple {

    protected final Object subject; // b-node-id or string
    protected final String predicate;
    protected final Object object; // b-node-id or string or literal-label
    protected final Supplier<OntGraphModel> model;

    /**
     * Constructs the base object.
     *
     * @param subject   - must be either {@link BlankNodeId} or {@code String}, not {@code null}
     * @param predicate - {@code String} (URI), not {@code null}
     * @param object    - must be either {@link BlankNodeId}, {@link LiteralLabel} or {@code String}, not {@code null}
     * @param m         - a facility (as {@link Supplier}) to provide nonnull {@link OntGraphModel}, not {@code null}
     */
    protected ONTBaseTripleImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
        this.subject = Objects.requireNonNull(subject);
        this.predicate = Objects.requireNonNull(predicate);
        this.object = Objects.requireNonNull(object);
        this.model = Objects.requireNonNull(m);
    }

    /**
     * Extracts a primitive base part from the given {@link RDFNode}.
     *
     * @param r {@link RDFNode}, not {@code null}
     * @return {@link BlankNodeId} or {@link LiteralLabel} or {@code String}
     */
    public static Object fromNode(RDFNode r) {
        return strip(r.asNode());
    }

    /**
     * Extracts a primitive base part from the given {@link Node}.
     *
     * @param node {@link Node}, not {@code null}
     * @return {@link BlankNodeId} or {@link LiteralLabel} or {@code String}
     */
    public static Object strip(Node node) {
        if (node.isURI())
            return node.getURI();
        if (node.isBlank())
            return node.getBlankNodeId();
        if (node.isLiteral())
            return node.getLiteral();
        throw new OntApiException.IllegalState("Wrong node: " + node);
    }

    /**
     * Answers the root triple of this statement.
     *
     * @return {@link Triple}
     */
    @Override
    public Triple asTriple() {
        return Triple.create(getSubjectNode(), getPredicateNode(), getObjectNode());
    }

    /**
     * Answers the root statement of this object.
     *
     * @return {@link OntStatement}
     */
    public OntStatement asStatement() {
        OntGraphModel m = model.get();
        Triple t = asTriple();
        return m.asStatement(Iter.findFirst(m.getGraph().find(t))
                .orElseThrow(() -> new OntApiException.IllegalState("Can't find triple " + t)));
    }

    /**
     * Lists all {@link Triple}s, associated with this object.
     *
     * @return {@code Stream} of {@link Triple}s
     */
    public Stream<Triple> triples() {
        return Stream.of(asTriple());
    }

    @Override
    public InternalObjectFactory getObjectFactory() {
        return HasObjectFactory.getObjectFactory(model.get());
    }

    protected DataFactory getDataFactory() {
        return getObjectFactory().getOWLDataFactory();
    }

    /**
     * Answers a {@code Node} that is included into this triple-object at subject position.
     *
     * @return {@link Node}
     */
    protected Node getSubjectNode() {
        if (subject instanceof String) {
            return NodeFactory.createURI((String) subject);
        }
        if (subject instanceof BlankNodeId) {
            return NodeFactory.createBlankNode((BlankNodeId) subject);
        }
        throw new OntApiException.IllegalState();
    }

    /**
     * Answers a {@code Node} that is included into this triple-object at object position.
     *
     * @return {@link Node}
     */
    protected Node getObjectNode() {
        if (object instanceof String) {
            return NodeFactory.createURI((String) object);
        }
        if (object instanceof BlankNodeId) {
            return NodeFactory.createBlankNode((BlankNodeId) object);
        }
        if (object instanceof LiteralLabel) {
            return NodeFactory.createLiteral((LiteralLabel) object);
        }
        throw new OntApiException.IllegalState();
    }

    /**
     * Answers a {@code Node} that is included into this triple-object at predicate position.
     *
     * @return {@link Node}
     */
    protected Node getPredicateNode() {
        return NodeFactory.createURI(predicate);
    }

    /**
     * Answers {@code true} if this triple and the given have some SPO.
     *
     * @param other {@link ONTBaseTripleImpl}, not {@code null}
     * @return boolean
     */
    public boolean sameAs(ONTBaseTripleImpl other) {
        return subject.equals(other.subject) && predicate.equals(other.predicate) && object.equals(other.object);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
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
        if (other instanceof ONTBaseTripleImpl) {
            ONTBaseTripleImpl t = (ONTBaseTripleImpl) other;
            if (notSame(t)) {
                return false;
            }
            if (sameAs(t)) {
                return true;
            }
        }
        // then either OWL-API instance is given or triple from different ontologies
        if (hashCode() != other.hashCode()) {
            return false;
        }
        return equalIterators(components().iterator(), other.components().iterator());
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        throw new NotSerializableException("Suspicious method call. " +
                "Serialization is unsupported for " + getClass().getSimpleName() + ".");
    }

    private void readObject(ObjectInputStream in) throws Exception {
        throw new NotSerializableException("Suspicious method call. " +
                "Deserialization is unsupported for " + getClass().getSimpleName() + ".");
    }

}
