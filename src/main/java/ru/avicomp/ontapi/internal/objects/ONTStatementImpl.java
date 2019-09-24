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
import org.semanticweb.owlapi.model.HasAnnotations;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;

import javax.annotation.Nullable;
import java.util.*;
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
public abstract class ONTStatementImpl extends ONTObjectImpl implements OWLObject, HasAnnotations, FrontsTriple {

    // a marker for the case when there is no content cache
    protected static final Object[] EMPTY = new Object[0];

    protected final Object subject; // b-node-id or string
    protected final String predicate;
    protected final Object object; // b-node-id or string or literal-label

    /**
     * Constructs the base object-triple.
     *
     * This class do not use {@link Triple Jena Triple} as reference,
     * instead it contains three separated triple parts: {@link #subject}, {@link #predicate} and {@link #object}.
     * This is because a {@link Graph} generally does not guarantee that it will return
     * the same triplets (that are equal in sense of the operation {@code ==}) for the same SPO patterns,
     * although this is true for {@link org.apache.jena.mem.GraphMem}.
     * Also, the investigation shows
     * that this way is slightly faster and economical than the way when a triple is used as a single reference.
     *
     * @param subject   - must be either {@link BlankNodeId} or {@code String}, not {@code null}
     * @param predicate - {@code String} (URI), not {@code null}
     * @param object    - must be either {@link BlankNodeId}, {@link LiteralLabel} or {@code String}, not {@code null}
     * @param m         - a facility (as {@link Supplier}) to provide nonnull {@link OntGraphModel}, not {@code null}
     */
    protected ONTStatementImpl(Object subject, String predicate, Object object, Supplier<OntGraphModel> m) {
        super(m);
        this.subject = Objects.requireNonNull(subject);
        this.predicate = Objects.requireNonNull(predicate);
        this.object = Objects.requireNonNull(object);
    }

    /**
     * Extracts a primitive base part from the given {@link RDFNode}.
     *
     * @param r {@link RDFNode}, not {@code null}
     * @return {@link BlankNodeId} or {@link LiteralLabel} or {@code String}
     */
    @SuppressWarnings("unused")
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
     * Calculates the hash code for the given {@code array} starting with the specified position.
     *
     * @param array      not {@code null}
     * @param startIndex int, non-negative
     * @return int
     * @see java.util.Arrays#hashCode(Object[])
     */
    protected static int hashCode(Object[] array, int startIndex) {
        if (array == EMPTY)
            return 1;
        int res = 1;
        for (int i = startIndex; i < array.length; i++) {
            res = 31 * res + array[i].hashCode();
        }
        return res;
    }

    /**
     * Returns an array containing all of the elements
     * in the specified collection in the same sequence (from first to last element).
     * This method is slightly simpler and faster than the standard java method.
     *
     * @param collection a {@code Collection}, not {@code null}
     * @return an {@code Array}
     * @see AbstractCollection#toArray()
     */
    protected static Object[] toArray(Collection collection) {
        if (collection.isEmpty()) return EMPTY;
        Object[] res = new Object[collection.size()];
        int index = 0;
        for (Object a : collection) {
            res[index++] = a;
        }
        return res;
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
        OntGraphModel m = getModel();
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
     * Answers {@code true} if this statement (axiom or annotation) has sub-annotations.
     *
     * @return boolean
     * @see org.semanticweb.owlapi.model.OWLAxiom#isAnnotated()
     */
    public boolean isAnnotated() {
        return false;
    }

    /**
     * Lists all {@link OWLAnnotation}s on this object.
     * The stream must be {@link java.util.Spliterator#ORDERED ordered}, {@link java.util.Spliterator#NONNULL nonull},
     * {@link java.util.Spliterator#DISTINCT distinct} and {@link java.util.Spliterator#SORTED sorted}.
     *
     * @return a {@code Stream} of {@link OWLAnnotation}s
     * @see org.semanticweb.owlapi.model.HasAnnotations#annotations()
     */
    @Override
    public Stream<OWLAnnotation> annotations() {
        return Stream.empty();
    }

    /**
     * Answers a sorted and distinct {@code List} of {@link OWLAnnotation}s on this object.
     * The returned {@code List} is unmodifiable.
     *
     * @return a unmodifiable {@code List} of {@link OWLAnnotation}s
     * @see org.semanticweb.owlapi.model.HasAnnotations#annotationsAsList()
     */
    @Override
    public List<OWLAnnotation> annotationsAsList() {
        return Collections.emptyList();
    }

    /**
     * Creates a new collection containing all the annotations of this object and all the given.
     *
     * @param other {@link Iterator} of {@link OWLAnnotation}s to append to the existing annotations
     * @return a {@code Collection} with the annotations both from this object and specified
     */
    @FactoryAccessor
    protected Collection<OWLAnnotation> appendAnnotations(Iterator<OWLAnnotation> other) {
        Set<OWLAnnotation> res = createSortedSet();
        other.forEachRemaining(x -> res.add(eraseModel(x)));
        factoryAnnotations().forEach(res::add);
        return res;
    }

    /**
     * Returns a {@code Stream} of {@link OWLAnnotation}s with erased model.
     *
     * @return a {@code Stream} of {@link OWLAnnotation}s
     */
    @FactoryAccessor
    protected Stream<OWLAnnotation> factoryAnnotations() {
        return annotations().map(ONTObjectImpl::eraseModel);
    }

    /**
     * Answers {@code true} if this triple-object and the given have the same SPO (base triple).
     *
     * @param other {@link ONTStatementImpl}, not {@code null}
     * @return boolean
     */
    public final boolean sameTriple(ONTStatementImpl other) {
        return sameSubject(other) && samePredicate(other) && sameObject(other);
    }

    /**
     * Answers {@code true} iff the subjects of this and the specified object are equal.
     *
     * @param other {@link ONTStatementImpl}, not {@code null}
     * @return boolean
     */
    protected final boolean sameSubject(ONTStatementImpl other) {
        return subject.equals(other.subject);
    }

    /**
     * Answers {@code true} iff the predicates of this and the specified object are equal.
     *
     * @param other {@link ONTStatementImpl}, not {@code null}
     * @return boolean
     */
    protected final boolean samePredicate(ONTStatementImpl other) {
        return predicate.equals(other.predicate);
    }

    /**
     * Answers {@code true} iff the objects of this and the specified object are equal.
     *
     * @param other {@link ONTStatementImpl}, not {@code null}
     * @return boolean
     */
    protected final boolean sameObject(ONTStatementImpl other) {
        return object.equals(other.object);
    }

    /**
     * Answers {@code true} iff this triple (SPO) has an URI subject.
     *
     * @return boolean
     */
    protected final boolean hasURISubject() {
        return subject instanceof String;
    }

    /**
     * Answers {@code true} iff this triple (SPO) has an URI object.
     *
     * @return boolean
     */
    protected final boolean hasURIObject() {
        return object instanceof String;
    }

    /**
     * Answers {@code true} if this object and the given have the same content.
     * Two {@link OWLObject}s may have same content,
     * but different base triples (see {@link #sameTriple(ONTStatementImpl)}).
     *
     * @param other {@link ONTStatementImpl}, not {@code null}
     * @return boolean
     */
    protected abstract boolean sameContent(ONTStatementImpl other);

    /**
     * Answers {@code true} if this object and the given are equal as {@link OWLObject} (i.e. in OWL-API terms).
     *
     * @param other {@link ONTStatementImpl}, not {@code null}
     * @return boolean
     */
    protected boolean sameAs(ONTStatementImpl other) {
        if (notSame(other)) {
            // definitely not equal
            return false;
        }
        // with a fixed configuration (InternalConfig),
        // it is impossible to have two different axioms with the same main triple:
        // all annotations are strictly defined by the config,
        // so two axioms read from the model must be equal
        // if they have identical root triple with uris as subject and object:
        if (sameTriple(other)) {
            // definitely equal
            return true;
        }
        return sameContent(other);
    }

    @Override
    public final boolean equals(@Nullable Object obj) {
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
        if (other instanceof ONTStatementImpl) {
            return sameAs((ONTStatementImpl) other);
        }
        // then OWL-API instance is given
        if (hashCode() != other.hashCode()) {
            return false;
        }
        return equalIterators(components().iterator(), other.components().iterator());
    }

}
