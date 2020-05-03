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

package com.github.owlcs.ontapi.internal.axioms;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.config.AxiomsSettings;
import com.github.owlcs.ontapi.internal.*;
import com.github.owlcs.ontapi.internal.objects.FactoryAccessor;
import com.github.owlcs.ontapi.internal.objects.ONTAxiomImpl;
import com.github.owlcs.ontapi.internal.objects.ONTStatementImpl;
import com.github.owlcs.ontapi.internal.objects.WithContent;
import com.github.owlcs.ontapi.jena.model.OntList;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObject;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Models;
import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Base class for three following implementations:
 * <ul>
 * <li>{@link HasKeyTranslator}</li>
 * <li>{@link SubPropertyChainOfTranslator}</li>
 * <li>{@link DisjointUnionTranslator}</li>
 * </ul>
 * Created by @szuev on 18.10.2016.
 */
public abstract class AbstractListBasedTranslator<Axiom extends OWLLogicalAxiom,
        ONT_SUBJECT extends OntObject, OWL_SUBJECT extends OWLObject,
        ONT_MEMBER extends OntObject, OWL_MEMBER extends OWLObject> extends AxiomTranslator<Axiom> {

    abstract OWLObject getSubject(Axiom axiom);

    abstract Property getPredicate();

    abstract Collection<? extends OWLObject> getObjects(Axiom axiom);

    abstract Class<ONT_SUBJECT> getView();

    @Override
    public void write(Axiom axiom, OntModel model) {
        WriteHelper.writeList(model, getSubject(axiom), getPredicate(), getObjects(axiom), axiom.annotationsAsList());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        return listByPredicate(model, getPredicate()).filterKeep(this::filter);
    }

    protected boolean filter(OntStatement statement) {
        return statement.getSubject().canAs(getView())
                && statement.getObject().canAs(RDFList.class);
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return getPredicate().equals(statement.getPredicate()) && filter(statement);
    }

    ONTObject<Axiom> makeAxiom(OntStatement statement,
                               Function<ONT_SUBJECT, ONTObject<? extends OWL_SUBJECT>> subjectExtractor,
                               BiFunction<ONT_SUBJECT, RDFNode, Optional<OntList<ONT_MEMBER>>> listExtractor,
                               Function<ONT_MEMBER, ONTObject<? extends OWL_MEMBER>> memberExtractor,
                               Collector<ONTObject<? extends OWL_MEMBER>, ?, ? extends Collection<ONTObject<? extends OWL_MEMBER>>> collector,
                               BiFunction<ONTObject<? extends OWL_SUBJECT>, Collection<ONTObject<? extends OWL_MEMBER>>, Axiom> axiomMaker) {

        ONT_SUBJECT ontSubject = statement.getSubject(getView());
        ONTObject<? extends OWL_SUBJECT> subject = subjectExtractor.apply(ontSubject);
        OntList<ONT_MEMBER> list = listExtractor.apply(ontSubject, statement.getObject())
                .orElseThrow(() -> new OntApiException("Can't get OntList for statement " + Models.toString(statement)));
        Collection<ONTObject<? extends OWL_MEMBER>> members = list.members().map(memberExtractor).collect(collector);

        Axiom res = axiomMaker.apply(subject, members);
        return ONTWrapperImpl.create(res, statement)
                .append(() -> list.spec().map(FrontsTriple::asTriple))
                .append(members);
    }

    /**
     * A base {@link ONTAxiomImpl} for simplification code.
     *
     * @param <A> - {@link OWLAxiom}, for which impl this class is base
     * @param <M> - {@link OntObject}, a list member subtype
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract static class WithListImpl<A extends OWLAxiom, M extends OntObject>
            extends ONTAxiomImpl<A> implements WithContent<A> {
        protected final InternalCache.Loading<A, Object[]> content;

        protected WithListImpl(Triple t, Supplier<OntModel> m) {
            this(strip(t.getSubject()), t.getPredicate().getURI(), strip(t.getObject()), m);
        }

        protected WithListImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
            this.content = createContentCache();
        }

        /**
         * Extracts the {@link OntList} from the statement.
         *
         * @param statement {@link OntStatement}, not {@code null}
         * @return {@link OntList} with {@link M}s
         */
        protected abstract OntList<M> findList(OntStatement statement);

        /**
         * Creates a copy of this axiom with additional triples from the specified axiom (that must equal to this).
         *
         * @param other {@link ONTObject} to get triples
         * @return {@link WithListImpl}
         */
        protected abstract WithListImpl<A, M> makeCopy(ONTObject<A> other);

        /**
         * Creates a factory axiom using the given parameters.
         *
         * @param content     a {@code Array} - the cache, not {@code null}
         * @param factory     {@link ONTObjectFactory}, not {@code null}
         * @param annotations a {@code Collection} of {@link OWLAnnotation annotation}s
         * @return {@link A}
         */
        @FactoryAccessor
        protected abstract A createAnnotatedAxiom(Object[] content,
                                                  ModelObjectFactory factory,
                                                  Collection<OWLAnnotation> annotations);

        @Override
        public InternalCache.Loading<A, Object[]> getContentCache() {
            return content;
        }

        @Override
        protected boolean sameContent(ONTStatementImpl other) {
            return Arrays.equals(getContent(), ((WithListImpl<?, ?>) other).getContent());
        }

        @Override
        public Stream<Triple> triples() {
            OntStatement s = asStatement();
            return Stream.concat(Stream.concat(Stream.of(s), findList(s).spec()).map(FrontsTriple::asTriple),
                    objects().flatMap(ONTObject::triples));
        }

        @Override
        public final ONTObject<A> merge(ONTObject<A> other) {
            if (this == other) {
                return this;
            }
            if (other instanceof WithListImpl && sameTriple((WithListImpl<A, M>) other)) {
                return this;
            }
            WithListImpl<A, M> res = makeCopy(other);
            if (hasContent()) {
                res.putContent(getContent());
            }
            res.hashCode = hashCode;
            return res;
        }

        @Override
        public final boolean canContainAnnotationProperties() {
            return isAnnotated();
        }

        @FactoryAccessor
        @Override
        protected A createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return createAnnotatedAxiom(getContent(), getObjectFactory(), annotations);
        }
    }
}
