/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, owl.cs group.
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
import com.github.owlcs.ontapi.internal.AxiomTranslator;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.WriteHelper;
import com.github.owlcs.ontapi.internal.objects.*;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.owlapi.objects.OWLAnonymousIndividualImpl;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLAPIStreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for following axioms:
 * <ul>
 * <li>EquivalentClasses ({@link EquivalentClassesTranslator})</li>
 * <li>EquivalentObjectProperties ({@link EquivalentObjectPropertiesTranslator})</li>
 * <li>EquivalentDataProperties ({@link EquivalentDataPropertiesTranslator})</li>
 * <li>SameIndividual ({@link SameIndividualTranslator})</li>
 * </ul>
 * Also for {@link AbstractTwoWayNaryTranslator} with following subclasses:
 * <ul>
 * <li>DisjointClasses ({@link DisjointClassesTranslator})</li>
 * <li>DisjointObjectProperties ({@link DisjointObjectPropertiesTranslator})</li>
 * <li>DisjointDataProperties ({@link DisjointDataPropertiesTranslator})</li>
 * <li>DifferentIndividuals ({@link DifferentIndividualsTranslator})</li>
 * </ul>
 * <p>
 * Created by szuev on 13.10.2016.
 *
 * @param <Axiom> generic type of {@link OWLAxiom}
 * @param <OWL>   generic type of {@link OWLObject}
 * @param <ONT>   generic type of {@link OntObject}
 */
public abstract class AbstractNaryTranslator<Axiom extends OWLAxiom & OWLNaryAxiom<OWL>,
        OWL extends OWLObject & IsAnonymous, ONT extends OntObject> extends AxiomTranslator<Axiom> {

    static final Logger LOGGER = LoggerFactory.getLogger(AbstractNaryTranslator.class);

    private static final Comparator<OWLObject> URI_FIRST_COMPARATOR = Comparator.comparing(IsAnonymous::isAnonymous);

    void write(OWLNaryAxiom<OWL> thisAxiom, Collection<OWLAnnotation> annotations, OntModel model) {
        List<OWL> operands = thisAxiom.operands().sorted(URI_FIRST_COMPARATOR).collect(Collectors.toList());
        if (operands.isEmpty() && annotations.isEmpty()) { // nothing to write, skip
            return;
        }
        if (operands.size() != 2) {
            throw new OntApiException(getClass().getSimpleName() + ": expected two operands. Axiom: " + thisAxiom);
        }
        WriteHelper.writeTriple(model, operands.get(0), getPredicate(), operands.get(1), annotations);
    }

    @Override
    public void write(Axiom axiom, OntModel model) {
        Collection<? extends OWLNaryAxiom<OWL>> axioms = axiom.asPairwiseAxioms();
        if (axioms.isEmpty()) {
            LOGGER.warn("Nothing to write, wrong axiom is given: {}", axiom);
            return;
        }
        axioms.forEach(a -> write(a, axiom.annotationsAsList(), model));
    }

    abstract Property getPredicate();

    abstract Class<ONT> getView();

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntModel model, AxiomsSettings config) {
        return listByPredicate(model, getPredicate()).filterKeep(this::filter);
    }

    @Override
    public boolean testStatement(OntStatement statement, AxiomsSettings config) {
        return getPredicate().equals(statement.getPredicate()) && filter(statement);
    }

    protected boolean filter(Statement statement) {
        return statement.getSubject().canAs(getView()) && statement.getObject().canAs(getView());
    }

    @Override
    protected Collection<Triple> getSearchTriples(Axiom axiom) {
        Triple res = getSearchTriple(axiom);
        return res == null ? Collections.emptyList() :
                Arrays.asList(res, Triple.create(res.getObject(), res.getPredicate(), res.getSubject()));
    }

    protected Triple getSearchTriple(Axiom axiom) {
        if (axiom instanceof WithManyObjects.Simple) {
            return ((WithManyObjects.Simple<?>) axiom).asTriple();
        }
        List<OWL> list = axiom.getOperandsAsList();
        if (list.size() != 2) {
            return null; // for Disjoint Objects
        }
        OWL left = list.get(0);
        OWL right = list.get(1);
        if (left instanceof HasIRI && right instanceof HasIRI) {
            return Triple.create(WriteHelper.toNode((HasIRI) left),
                    getPredicate().asNode(), WriteHelper.toNode((HasIRI) right));
        }
        return null;
    }

    /**
     * A base for N-Ary axiom impls.
     *
     * @param <A> - subtype of {@link OWLNaryAxiom}
     * @param <M> - subtype of {@link OWLObject}
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract static class NaryAxiomImpl<A extends OWLNaryAxiom<M>, M extends OWLObject>
            extends ONTAxiomImpl<A>
            implements WithManyObjects<M>, OWLNaryAxiom<M> {

        protected NaryAxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        /**
         * Returns the number of components.
         * For single-triple axioms (i.e. a with predicate such as {@code owl:sameAs}, {@code owl:equivalentClass})
         * the method returns {@code 2}.
         * Only n-ary axioms which are based on {@link OntDisjoint} resource can different members count.
         *
         * @return long
         */
        protected abstract long count();

        /**
         * Creates an instance of {@link NaryAxiomImpl}
         * with additional triples getting from the specified {@code other} object.
         * The returned instance must be equivalent to this instance.
         *
         * @param other {@link ONTObject} with {@link A}, not {@code null}
         * @return {@link NaryAxiomImpl} - a fresh instance that equals to this
         */
        protected abstract NaryAxiomImpl<A, M> makeCopyWith(ONTObject<A> other);

        /**
         * Creates a factory instance of {@link A}.
         *
         * @param members     a {@code Collection} of {@link M}-members, not {@code null}
         * @param annotations a {@code Collection} of {@link OWLAnnotation}s, can be {@code null}
         * @return {@link A}
         */
        protected abstract A createAxiom(Collection<M> members, Collection<OWLAnnotation> annotations);

        /**
         * Creates a factory instance of {@link A}.
         *
         * @param a           {@link M}, the first component, not {@code null}
         * @param b           {@link M}, the second component, not {@code null}
         * @param annotations a {@code Collection} of {@link OWLAnnotation}s, can be {@code null}
         * @return {@link A}
         */
        private A createAxiom(M a, M b, Collection<OWLAnnotation> annotations) {
            return createAxiom(Arrays.asList(eraseModel(a), eraseModel(b)), annotations);
        }

        @FactoryAccessor
        @Override
        protected final A createAnnotatedAxiom(Collection<OWLAnnotation> annotations) {
            return createAxiom(members().map(x -> eraseModel(x.getOWLObject()))
                    .collect(Collectors.toList()), annotations);
        }

        @Override
        public final NaryAxiomImpl<A, M> merge(ONTObject<A> other) {
            if (this == other) {
                return this;
            }
            if (other instanceof NaryAxiomImpl && sameTriple((NaryAxiomImpl<A, M>) other)) {
                return this;
            }
            NaryAxiomImpl<A, M> res = makeCopyWith(other);
            res.hashCode = hashCode;
            return res;
        }

        @FactoryAccessor
        public final Collection<A> asPairwiseAxioms() {
            if (count() == 2) {
                return createSet(eraseModel());
            }
            return walkPairwise((a, b) -> createAxiom(a, b, null));
        }

        @FactoryAccessor
        @Override
        public final Collection<A> splitToAnnotatedPairs() {
            if (count() == 2) {
                return createSet(eraseModel());
            }
            List<OWLAnnotation> annotations = factoryAnnotations().collect(Collectors.toList());
            return walkPairwise((a, b) -> createAxiom(a, b, annotations));
        }

        /**
         * Answers {@code true} if the given axiom has mirror triple.
         *
         * @param other {@link ONTAxiomImpl} to test, not {@code null}
         * @return boolean
         */
        protected boolean isReverseTriple(ONTAxiomImpl<A> other) {
            return subject.equals(other.getObjectURI()) && object.equals(other.getSubjectURI());
        }

        /**
         * Tests content's assuming this instance also implements {@link WithContent} interface.
         * Unsafe.
         *
         * @param other {@link ONTStatementImpl}, not {@code null}
         * @return boolean
         */
        boolean testSameContent(ONTStatementImpl other) {
            if (other instanceof WithContent) {
                return Arrays.equals(((WithContent<?>) this).getContent(), ((WithContent<?>) other).getContent());
            }
            if (other instanceof WithManyObjects) {
                ModelObjectFactory factory = getObjectFactory();
                return equalIterators(objects(factory).iterator(),
                        ((WithManyObjects<?>) other).objects(factory).iterator());
            }
            return false;
        }

        /**
         * Lists {@link T}s retrieved from the axiom internals.
         *
         * @param visitor {@link OWLPairwiseVisitor}, a visitor to apply to all pairwise elements in this axiom;
         *                pairs are ordered,  i.e., (i, j) and (j, i) will be considered. (i,i) is skipped, not {@code null}
         * @param <T>     the type returned by the {@code visitor}
         * @return a {@code Stream} of {@link T}s
         * @see OWLNaryAxiom#walkAllPairwise(OWLPairwiseVisitor)
         */
        protected <T> Stream<T> fromPairs(OWLPairwiseVisitor<T, M> visitor) {
            return OWLAPIStreamUtils.allPairs(operands()).map(v -> visitor.visit(v.i, v.j)).filter(Objects::nonNull);
        }

        @Override
        public final boolean canContainAnnotationProperties() {
            return isAnnotated();
        }
    }

    /**
     * An abstract {@link OWLNaryClassAxiom} implementation.
     *
     * @param <A> subtype of {@link OWLNaryClassAxiom}
     */
    @SuppressWarnings({"NullableProblems", "WeakerAccess"})
    protected abstract static class ClassNaryAxiomImpl<A extends OWLNaryClassAxiom>
            extends ClassOrIndividualNaryAxiomImpl<A, OWLClassExpression> implements OWLNaryClassAxiom {

        protected ClassNaryAxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLClassExpression>> listONTComponents(OntStatement statement,
                                                                                           ModelObjectFactory factory) {
            return Iter.of(factory.getClass(statement.getSubject(OntClass.class)),
                    factory.getClass(statement.getObject(OntClass.class)));
        }

        @Override
        public ONTObject<? extends OWLClassExpression> findByURI(String uri, ModelObjectFactory factory) {
            return factory.getClass(uri);
        }

        @Override
        public Stream<OWLClassExpression> classExpressions() {
            return sorted().map(ONTObject::getOWLObject);
        }

        @Override
        public Set<OWLClassExpression> getClassExpressionsMinus(OWLClassExpression... excludes) {
            return getSetMinus(excludes);
        }

        @Override
        public boolean contains(OWLClassExpression ce) {
            return members().map(ONTObject::getOWLObject).anyMatch(ce::equals);
        }
    }

    /**
     * An abstract {@link OWLNaryIndividualAxiom} implementation.
     *
     * @param <A> subtype of {@link OWLNaryIndividualAxiom}
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract static class IndividualNaryAxiomImpl<A extends OWLNaryIndividualAxiom>
            extends ClassOrIndividualNaryAxiomImpl<A, OWLIndividual> implements OWLNaryIndividualAxiom {

        protected IndividualNaryAxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLIndividual>> listONTComponents(OntStatement statement,
                                                                                      ModelObjectFactory factory) {
            return Iter.of(factory.getIndividual(statement.getSubject(OntIndividual.class)),
                    factory.getIndividual(statement.getObject(OntIndividual.class)));
        }

        @Override
        public ONTObject<? extends OWLIndividual> findByURI(String uri, ModelObjectFactory factory) {
            return factory.getNamedIndividual(uri);
        }

        @Override
        public Stream<OWLIndividual> individuals() {
            return sorted().map(ONTObject::getOWLObject);
        }

        @Override
        public final boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public final boolean canContainClassExpressions() {
            return false;
        }

        @Override
        public final boolean canContainDataProperties() {
            return false;
        }

        @Override
        public final boolean canContainObjectProperties() {
            return false;
        }

        @Override
        public final boolean canContainDatatypes() {
            return isAnnotated();
        }

        @SuppressWarnings("rawtypes")
        public ONTObject fromContentItem(Object x, ModelObjectFactory factory) {
            if (x instanceof String)
                return findByURI((String) x, factory);
            if (x instanceof BlankNodeId) {
                return factory.getAnonymousIndividual((BlankNodeId) x);
            }
            return (ONTObject) x;
        }

        @SuppressWarnings("rawtypes")
        public Object toContentItem(ONTObject x) {
            if (x instanceof OWLNamedIndividual) return ONTEntityImpl.getURI((OWLEntity) x);
            return ((OWLAnonymousIndividualImpl) x).getBlankNodeId();
        }
    }

    /**
     * An abstract {@link OWLNaryPropertyAxiom} implementation for member-type {@link OWLObjectPropertyExpression}.
     *
     * @param <A> either {@link OWLEquivalentObjectPropertiesAxiom} or {@link OWLDisjointObjectPropertiesAxiom}
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract static class ObjectPropertyNaryAxiomImpl<A extends OWLNaryPropertyAxiom<OWLObjectPropertyExpression>>
            extends PropertyNaryAxiomImpl<A, OWLObjectPropertyExpression> {

        protected ObjectPropertyNaryAxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLObjectPropertyExpression>> listONTComponents(OntStatement statement,
                                                                                                    ModelObjectFactory factory) {
            return Iter.of(factory.getProperty(statement.getSubject(OntObjectProperty.class)),
                    factory.getProperty(statement.getObject(OntObjectProperty.class)));
        }

        @Override
        public ONTObject<? extends OWLObjectPropertyExpression> findByURI(String uri, ModelObjectFactory factory) {
            return factory.getObjectProperty(uri);
        }

        @Override
        public final boolean canContainDataProperties() {
            return false;
        }
    }

    /**
     * An abstract {@link OWLNaryPropertyAxiom} implementation for member-type {@link OWLDataPropertyExpression}.
     *
     * @param <A> either {@link OWLEquivalentDataPropertiesAxiom} or {@link OWLDisjointDataPropertiesAxiom}
     */
    @SuppressWarnings("WeakerAccess")
    protected abstract static class DataPropertyNaryAxiomImpl<A extends OWLNaryPropertyAxiom<OWLDataPropertyExpression>>
            extends PropertyNaryAxiomImpl<A, OWLDataPropertyExpression> {

        protected DataPropertyNaryAxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        @Override
        public ExtendedIterator<ONTObject<? extends OWLDataPropertyExpression>> listONTComponents(OntStatement statement,
                                                                                                  ModelObjectFactory factory) {
            return Iter.of(factory.getProperty(statement.getSubject(OntDataProperty.class)),
                    factory.getProperty(statement.getObject(OntDataProperty.class)));
        }

        @Override
        public ONTObject<? extends OWLDataPropertyExpression> findByURI(String uri, ModelObjectFactory factory) {
            return factory.getDataProperty(uri);
        }

        @Override
        public final boolean canContainObjectProperties() {
            return false;
        }
    }

    /**
     * An abstraction, that combines common properties for {@link OWLNaryIndividualAxiom} and {@link OWLNaryClassAxiom}.
     *
     * @param <A> subtype of {@link OWLNaryAxiom}
     */
    abstract static class ClassOrIndividualNaryAxiomImpl<A extends OWLNaryAxiom<M>, M extends OWLObject>
            extends NaryAxiomImpl<A, M> implements OWLSubClassOfAxiomSetShortCut {

        ClassOrIndividualNaryAxiomImpl(Object s, String p, Object o, Supplier<OntModel> m) {
            super(s, p, o, m);
        }

        /**
         * Creates a {@link OWLSubClassOfAxiom} axiom from factory.
         *
         * @param a - {@link M}, the first operand, not {@code null}
         * @param b - {@link M}, the second operand, not {@code null}
         * @return {@link OWLSubClassOfAxiom}
         * @see NaryAxiomImpl#fromPairs(OWLPairwiseVisitor)
         */
        @FactoryAccessor
        protected abstract OWLSubClassOfAxiom createSubClassOf(M a, M b);

        @FactoryAccessor
        @Override
        public Collection<OWLSubClassOfAxiom> asOWLSubClassOfAxioms() {
            return walkAllPairwise((a, b) -> createSubClassOf(eraseModel(a), eraseModel(b)));
        }
    }

    /**
     * An abstract {@link OWLNaryPropertyAxiom} implementation.
     *
     * @param <A> subtype of {@link OWLNaryPropertyAxiom}
     * @param <P> subtype of {@link OWLPropertyExpression}
     */
    abstract static class PropertyNaryAxiomImpl<A extends OWLNaryPropertyAxiom<P>, P extends OWLPropertyExpression>
            extends NaryAxiomImpl<A, P> implements OWLNaryPropertyAxiom<P> {

        PropertyNaryAxiomImpl(Object subject, String predicate, Object object, Supplier<OntModel> m) {
            super(subject, predicate, object, m);
        }

        @Override
        public Stream<P> properties() {
            return sorted().map(ONTObject::getOWLObject);
        }

        @SuppressWarnings({"unchecked", "NullableProblems"})
        @Override
        public Set<P> getPropertiesMinus(P property) {
            return getSetMinus(property);
        }

        @Override
        public final boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public final boolean canContainClassExpressions() {
            return false;
        }

        @Override
        public final boolean canContainNamedIndividuals() {
            return false;
        }

        @Override
        public final boolean canContainDatatypes() {
            return isAnnotated();
        }

        @Override
        public final boolean canContainAnonymousIndividuals() {
            return isAnnotated();
        }
    }
}
