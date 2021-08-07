/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2021, owl.cs group.
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

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.owlapi.axioms.AxiomImpl;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.util.NNF;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A base axiom.
 * Created by @ssz on 15.09.2019.
 *
 * @param <X> - the {@link OWLAxiom} subtype, must be the same that this class implements
 * @see AxiomImpl
 * @since 2.0.0
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTAxiomImpl<X extends OWLAxiom> extends ONTStatementImpl
        implements WithAnnotations, WithMerge<ONTObject<X>>, HasConfig, ModelObject<X>, OWLAxiom {

    /**
     * A helper to assign new hash code; can accept only {@link ONTAxiomImpl} instance.
     */
    protected static final ObjIntConsumer<OWLAxiom> SET_HASH_CODE = (a, h) -> ((ONTAxiomImpl<?>) a).setHashCode(h);

    protected ONTAxiomImpl(Triple t, Supplier<OntModel> m) {
        this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
    }

    protected ONTAxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
        super(subject, predicate, object, m);
    }

    /**
     * Collects all annotations for the given axiom's main {@link OntStatement}.
     *
     * @param axiom   {@link OntStatement} the root axiom's statement, not {@code null}
     * @param factory {@link ONTObjectFactory} to retrieve {@link ONTObject}s, not {@code null}
     * @param config  {@link AxiomsSettings} to control reading, not {@code null}
     * @return a sorted nonnull distinct {@code Collection}
     * of {@link ONTObject}s with {@link OWLAnnotation}s (can be empty if no annotations)
     * @see ONTAnnotationImpl#collectAnnotations(OntStatement, ONTObjectFactory)
     */
    public static Collection<ONTObject<OWLAnnotation>> collectAnnotations(OntStatement axiom,
                                                                          ONTObjectFactory factory,
                                                                          AxiomsSettings config) {
        Map<OWLAnnotation, ONTObject<OWLAnnotation>> res = new TreeMap<>();
        ReadHelper.listAnnotations(axiom, config, factory).forEachRemaining(x -> WithMerge.add(res, x));
        return res.values();
    }

    @Override
    public ONTObject<X> merge(ONTObject<X> other) {
        throw new OntApiException.Unsupported("Attempt to merge " + this + " with " + other);
    }

    /**
     * Assigns new {@code hashCode}.
     * Note: this is for internal usage only.
     *
     * @param hash int
     */
    protected void setHashCode(int hash) {
        this.hashCode = hash;
    }

    /**
     * Answers {@code true} iff this triple (SPO) has an URI subject.
     *
     * @return boolean
     */
    public final boolean hasURISubject() {
        return subject instanceof String;
    }

    /**
     * Answers {@code true} iff this triple (SPO) has an URI object.
     *
     * @return boolean
     */
    public final boolean hasURIObject() {
        return object instanceof String;
    }

    /**
     * Answers the triple's subject URI or throws an exception.
     *
     * @return String, never {@code null}
     * @throws ClassCastException if the subject is not an URI Resource
     */
    public final String getSubjectURI() throws ClassCastException {
        return (String) subject;
    }

    /**
     * Answers the triple's subject blank node id or throws an exception.
     *
     * @return {@link BlankNodeId}, never {@code null}
     * @throws ClassCastException if the subject is not an URI Resource
     */
    public final BlankNodeId getSubjectBlankNodeId() throws ClassCastException {
        return (BlankNodeId) subject;
    }

    /**
     * Answers the triple's object URI or throws an exception.
     *
     * @return String, never {@code null}
     * @throws ClassCastException if the object is not an URI Resource
     */
    public final String getObjectURI() throws ClassCastException {
        return (String) object;
    }

    /**
     * Answers {@code true} if the specified {@code uri} appears either in the position of a subject or object.
     *
     * @param uri {@link Resource}, not {@code null}
     * @return boolean
     */
    protected boolean hasURIResource(Resource uri) {
        return hasURIResource(uri.getURI());
    }

    /**
     * Answers {@code true} if the specified {@code uri} appears either in the position of a subject or object.
     *
     * @param uri String, not {@code null}
     * @return boolean
     */
    protected boolean hasURIResource(String uri) {
        return subject.equals(uri) || object.equals(uri);
    }

    @SuppressWarnings("unchecked")
    @Override
    public X getOWLObject() {
        return (X) this;
    }

    @Override
    public X eraseModel() {
        return createAnnotatedAxiom(factoryAnnotations().collect(Collectors.toList()));
    }

    @Override
    public InternalConfig getConfig() {
        return HasConfig.getConfig(getModel());
    }

    @Override
    public Stream<Triple> triples() {
        return Stream.concat(super.triples(), objects().flatMap(ONTObject::triples));
    }

    @SuppressWarnings("unchecked")
    @FactoryAccessor
    @Override
    public final X getAxiomWithoutAnnotations() {
        return createAnnotatedAxiom(createSet());
    }

    @SuppressWarnings("unchecked")
    @FactoryAccessor
    @Override
    public final <T extends OWLAxiom> T getAnnotatedAxiom(@Nonnull Stream<OWLAnnotation> annotations) {
        return (T) createAnnotatedAxiom(appendAnnotations(annotations.iterator()));
    }

    /**
     * Creates a fresh system-wide {@link X axiom} with the given annotations.
     * The resulting axiom should not contain model references.
     *
     * @param annotations a {@code Collection} of {@link OWLAnnotation}s to bind to the axiom
     * @return {@link X}
     */
    @FactoryAccessor
    protected abstract X createAnnotatedAxiom(Collection<OWLAnnotation> annotations);

    @FactoryAccessor
    @Override
    public OWLAxiom getNNF() {
        return eraseModel().accept(new NNF(getDataFactory()));
    }

}
