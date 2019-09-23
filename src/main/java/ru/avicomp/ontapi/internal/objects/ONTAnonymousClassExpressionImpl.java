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
import org.apache.jena.rdf.model.Literal;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.NNF;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.OntModels;
import ru.avicomp.ontapi.owlapi.OWLObjectImpl;
import ru.avicomp.ontapi.owlapi.objects.OWLAnonymousIndividualImpl;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An abstraction for any anonymous class expressions
 * (i.e. for all class expressions with except of {@code OWLClass}es).
 * <p>
 * Created by @ssz on 10.08.2019.
 *
 * @param <ONT> any subtype of {@link OntCE}
 * @param <OWL> subtype of {@link OWLAnonymousClassExpression}, which matches {@link ONT}
 * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLAnonymousClassExpressionImpl
 * @see ONTClassImpl
 * @see ru.avicomp.ontapi.internal.ReadHelper#calcClassExpression(OntCE, InternalObjectFactory, Set)
 * @see OntCE
 * @see OntClass
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTAnonymousClassExpressionImpl<ONT extends OntCE, OWL extends OWLAnonymousClassExpression>
        extends ONTExpressionImpl<ONT>
        implements OWLAnonymousClassExpression, ONTObject<OWL> {

    protected ONTAnonymousClassExpressionImpl(BlankNodeId n, Supplier<OntGraphModel> m) {
        super(n, m);
    }

    /**
     * Wraps the given {@link OntCE} as {@link OWLAnonymousClassExpression} and {@link ONTObject}.
     *
     * @param ce    {@link OntCE}, not {@code null}, must be anonymous
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @param model a provider of non-null {@link OntGraphModel}, not {@code null}
     * @return {@link ONTAnonymousClassExpressionImpl} instance
     */
    @SuppressWarnings("unchecked")
    public static ONTAnonymousClassExpressionImpl create(OntCE ce,
                                                         InternalObjectFactory factory,
                                                         Supplier<OntGraphModel> model) {
        Class<? extends OntCE> type = OntModels.getOntType(ce);
        BlankNodeId id = ce.asNode().getBlankNodeId();
        ONTAnonymousClassExpressionImpl res = create(id, type, model);
        // since we have already type information
        // we can forcibly load the cache to reduce graph traversal operations
        // (otherwise this type information will be collected again on demand, which means double-work):
        res.putContent(res.initContent(ce, factory));
        return res;
    }

    /**
     * Creates a class expression with given b-node {@code id} and {@code type}.
     * An underlying graph must contain a valid class expression structure for the specified {@code id}.
     *
     * @param id    {@link BlankNodeId}, not {@code null}
     * @param type  {@code Class}, not {@code null}
     * @param model a provider of non-null {@link OntGraphModel}, not {@code null}
     * @return {@link ONTAnonymousClassExpressionImpl} instance
     */
    public static ONTAnonymousClassExpressionImpl create(BlankNodeId id,
                                                         Class<? extends OntCE> type,
                                                         Supplier<OntGraphModel> model) {
        if (OntCE.ObjectSomeValuesFrom.class == type) {
            return new OSVF(id, model);
        }
        if (OntCE.DataSomeValuesFrom.class == type) {
            return new DSVF(id, model);
        }
        if (OntCE.ObjectAllValuesFrom.class == type) {
            return new OAVF(id, model);
        }
        if (OntCE.DataAllValuesFrom.class == type) {
            return new DAVF(id, model);
        }
        if (OntCE.ObjectHasValue.class == type) {
            return new OHV(id, model);
        }
        if (OntCE.DataHasValue.class == type) {
            return new DHV(id, model);
        }
        if (OntCE.HasSelf.class == type) {
            return new OHS(id, model);
        }
        if (OntCE.ObjectCardinality.class == type) {
            return new OEC(id, model);
        }
        if (OntCE.DataCardinality.class == type) {
            return new DEC(id, model);
        }
        if (OntCE.ObjectMinCardinality.class == type) {
            return new OMIC(id, model);
        }
        if (OntCE.DataMinCardinality.class == type) {
            return new DMIC(id, model);
        }
        if (OntCE.ObjectMaxCardinality.class == type) {
            return new OMAC(id, model);
        }
        if (OntCE.DataMaxCardinality.class == type) {
            return new DMAC(id, model);
        }
        if (OntCE.UnionOf.class == type) {
            return new UF(id, model);
        }
        if (OntCE.IntersectionOf.class == type) {
            return new IF(id, model);
        }
        if (OntCE.OneOf.class == type) {
            return new OF(id, model);
        }
        if (OntCE.ComplementOf.class == type) {
            return new CF(id, model);
        }
        if (OntCE.NaryDataSomeValuesFrom.class == type) {
            return new NDSVF(id, model);
        }
        if (OntCE.NaryDataAllValuesFrom.class == type) {
            return new NDAVF(id, model);
        }
        throw new OntApiException.IllegalState();
    }

    @SuppressWarnings("unchecked")
    @Override
    public OWL getOWLObject() {
        return (OWL) this;
    }

    @Override
    public boolean isOWLThing() {
        return false;
    }

    @Override
    public boolean isOWLNothing() {
        return false;
    }

    @Override
    public boolean isClassExpression() {
        return true;
    }

    @FactoryAccessor
    @Override
    public OWLClassExpression getNNF() {
        return accept(getNNFClassVisitor());
    }

    @FactoryAccessor
    @Override
    public OWLClassExpression getComplementNNF() {
        return getObjectComplementOf().accept(getNNFClassVisitor());
    }

    protected OWLClassExpressionVisitorEx<OWLClassExpression> getNNFClassVisitor() {
        return new NNF(getDataFactory()).getClassVisitor();
    }

    @FactoryAccessor
    @Override
    public OWLObjectComplementOf getObjectComplementOf() {
        return getDataFactory().getOWLObjectComplementOf(this);
    }

    @Override
    public Set<OWLClassExpression> asConjunctSet() {
        return createSet(this);
    }

    @Override
    public Stream<OWLClassExpression> conjunctSet() {
        return Stream.of(this);
    }

    @Override
    public boolean containsConjunct(@Nullable OWLClassExpression ce) {
        return equals(ce);
    }

    @Override
    public Set<OWLClassExpression> asDisjunctSet() {
        return createSet(this);
    }

    @Override
    public Stream<OWLClassExpression> disjunctSet() {
        return Stream.of(this);
    }

    @Override
    public boolean canContainAnnotationProperties() {
        return false;
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectSomeValuesFromImpl
     * @see OntCE.ObjectSomeValuesFrom
     */
    public static class OSVF
            extends WithClassAndObjectProperty<OntCE.ObjectSomeValuesFrom, OWLObjectSomeValuesFrom>
            implements OWLObjectSomeValuesFrom {

        public OSVF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.ObjectSomeValuesFrom asRDFNode() {
            return as(OntCE.ObjectSomeValuesFrom.class);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectAllValuesFromImpl
     * @see OntCE.ObjectAllValuesFrom
     */
    public static class OAVF
            extends WithClassAndObjectProperty<OntCE.ObjectAllValuesFrom, OWLObjectAllValuesFrom>
            implements OWLObjectAllValuesFrom {

        public OAVF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.ObjectAllValuesFrom asRDFNode() {
            return as(OntCE.ObjectAllValuesFrom.class);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataSomeValuesFromImpl
     * @see OntCE.DataSomeValuesFrom
     */
    public static class DSVF
            extends WithDataRangeAndDataPropertyUnary<OntCE.DataSomeValuesFrom, OWLDataSomeValuesFrom>
            implements OWLDataSomeValuesFrom {

        public DSVF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.DataSomeValuesFrom asRDFNode() {
            return as(OntCE.DataSomeValuesFrom.class);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataAllValuesFromImpl
     * @see OntCE.DataAllValuesFrom
     */
    public static class DAVF
            extends WithDataRangeAndDataPropertyUnary<OntCE.DataAllValuesFrom, OWLDataAllValuesFrom>
            implements OWLDataAllValuesFrom {

        public DAVF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.DataAllValuesFrom asRDFNode() {
            return as(OntCE.DataAllValuesFrom.class);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataSomeValuesFromImpl
     * @see OntCE.NaryDataSomeValuesFrom
     */
    public static class NDSVF
            extends WithDataRangeAndDataPropertyNary<OntCE.NaryDataSomeValuesFrom, OWLDataSomeValuesFrom>
            implements OWLDataSomeValuesFrom {

        public NDSVF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.NaryDataSomeValuesFrom asRDFNode() {
            return as(OntCE.NaryDataSomeValuesFrom.class);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataAllValuesFromImpl
     * @see OntCE.NaryDataAllValuesFrom
     */
    public static class NDAVF
            extends WithDataRangeAndDataPropertyNary<OntCE.NaryDataAllValuesFrom, OWLDataAllValuesFrom>
            implements OWLDataAllValuesFrom {

        public NDAVF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.NaryDataAllValuesFrom asRDFNode() {
            return as(OntCE.NaryDataAllValuesFrom.class);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectHasValueImpl
     * @see OntCE.ObjectHasValue
     */
    public static class OHV
            extends WithObjectProperty<OntCE.ObjectHasValue, OWLObjectHasValue>
            implements OWLObjectHasValue {

        protected OHV(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.ObjectHasValue asRDFNode() {
            return as(OntCE.ObjectHasValue.class);
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            InternalObjectFactory factory = getObjectFactory();
            return Stream.of(findOPE(factory), findIndividual(factory));
        }

        @FactoryAccessor
        @Override
        public OWLObjectSomeValuesFrom asSomeValuesFrom() {
            DataFactory df = getDataFactory();
            return df.getOWLObjectSomeValuesFrom(getProperty(), df.getOWLObjectOneOf(getFiller()));
        }

        @Override
        public OWLIndividual getFiller() {
            return getONTIndividual().getOWLObject();
        }

        public ONTObject<? extends OWLIndividual> getONTIndividual() {
            return findIndividual(getObjectFactory());
        }

        protected ONTObject<? extends OWLIndividual> findIndividual(InternalObjectFactory factory) {
            // [property, filler] - always last:
            return toIndividual(getContent()[1], factory);
        }

        @Override
        protected Object[] collectContent(OntCE.ObjectHasValue ce, InternalObjectFactory factory) {
            // [property, filler]
            return new Object[]{toContentItem(ce.getProperty(), factory), toContentItem(ce.getValue())};
        }

        @Override
        protected Object[] initContent(OntCE.ObjectHasValue ce, InternalObjectFactory factory) {
            OntOPE p = ce.getProperty();
            OntIndividual v = ce.getValue();
            return initContent(p.asNode(), v.asNode(), factory.getProperty(p), factory.getIndividual(v));
        }

        @Override
        Object toLastContentItem(Node node, Object expr) {
            return node.isURI() ? node.getURI() : node.getBlankNodeId();
        }

        @Override
        public boolean containsObjectProperty(OWLObjectProperty property) {
            return getNamedProperty().equals(property);
        }

        @Override
        public boolean containsNamedIndividual(OWLNamedIndividual individual) {
            OWLIndividual i = getFiller();
            return i.isOWLNamedIndividual() && individual.equals(i);
        }

        @Override
        public Set<OWLEntity> getSignatureSet() {
            Set<OWLEntity> res = createSortedSet();
            res.add(getNamedProperty());
            OWLIndividual i = getFiller();
            if (i.isOWLNamedIndividual()) res.add(i.asOWLNamedIndividual());
            return res;
        }

        @Override
        public Set<OWLObjectProperty> getObjectPropertySet() {
            return createSet(getNamedProperty());
        }

        protected OWLObjectProperty getNamedProperty() {
            return getProperty().getNamedProperty();
        }

        @Override
        public Set<OWLNamedIndividual> getNamedIndividualSet() {
            OWLIndividual i = getFiller();
            return i.isOWLNamedIndividual() ? createSet(i.asOWLNamedIndividual()) : createSet();
        }

        @Override
        public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
            OWLIndividual i = getFiller();
            return i.isOWLNamedIndividual() ? createSet() : createSet(i.asOWLAnonymousIndividual());
        }

        @Override
        public Set<OWLClassExpression> getClassExpressionSet() {
            return createSet(this);
        }

        @Override
        public boolean canContainDataProperties() {
            return false;
        }

        @Override
        public boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public boolean canContainDatatypes() {
            return false;
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataHasValueImpl
     * @see OntCE.DataHasValue
     */
    public static class DHV
            extends WithDataProperty<OntCE.DataHasValue, OWLDataHasValue>
            implements OWLDataHasValue {

        protected DHV(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.DataHasValue asRDFNode() {
            return as(OntCE.DataHasValue.class);
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            InternalObjectFactory factory = getObjectFactory();
            return Stream.of(findNDP(factory), findLiteral(factory));
        }

        @FactoryAccessor
        @Override
        public OWLDataSomeValuesFrom asSomeValuesFrom() {
            DataFactory df = getDataFactory();
            return df.getOWLDataSomeValuesFrom(getProperty(), df.getOWLDataOneOf(getFiller()));
        }

        @Override
        public OWLLiteral getFiller() {
            return getONTLiteral().getOWLObject();
        }

        public ONTObject<OWLLiteral> getONTLiteral() {
            return findLiteral(getObjectFactory());
        }

        protected ONTObject<OWLLiteral> findLiteral(InternalObjectFactory factory) {
            // [property, filler] -- always last:
            return toLiteral(getContent()[1], factory);
        }

        @Override
        protected Object[] collectContent(OntCE.DataHasValue ce, InternalObjectFactory factory) {
            // [property, filler]
            return new Object[]{toContentItem(ce.getProperty()), toContentItem(ce.getValue())};
        }

        @Override
        protected Object[] initContent(OntCE.DataHasValue ce, InternalObjectFactory factory) {
            OntNDP p = ce.getProperty();
            Literal v = ce.getValue();
            return initContent(p.asNode(), v.asNode(), factory.getProperty(p), factory.getLiteral(v));
        }

        @Override
        Object toLastContentItem(Node node, Object expr) {
            return node.getLiteral();
        }
    }

    /**
     * An {@code ObjectHasSelf} implementation.
     * It does not contain boolean datatype in signature -
     * see <a href='https://github.com/owlcs/owlapi/issues/783'>owlcs/owlapi##783</a>.
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectHasSelfImpl
     * @see OntCE.HasSelf
     */
    public static class OHS
            extends WithObjectProperty<OntCE.HasSelf, OWLObjectHasSelf>
            implements OWLObjectHasSelf {

        protected OHS(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.HasSelf asRDFNode() {
            return as(OntCE.HasSelf.class);
        }

        @Override
        public boolean containsObjectProperty(OWLObjectProperty property) {
            return getNamedProperty().equals(property);
        }

        @Override
        public Set<OWLEntity> getSignatureSet() {
            return createSet(getNamedProperty());
        }

        @Override
        public Set<OWLObjectProperty> getObjectPropertySet() {
            return createSet(getNamedProperty());
        }

        /**
         * Returns a named object property.
         *
         * @return {@link OWLObjectProperty}
         */
        protected OWLObjectProperty getNamedProperty() {
            return getProperty().getNamedProperty();
        }

        @Override
        public boolean canContainNamedIndividuals() {
            return false;
        }

        @Override
        public boolean canContainAnonymousIndividuals() {
            return false;
        }

        @Override
        public Set<OWLClassExpression> getClassExpressionSet() {
            return createSet(this);
        }

        @Override
        public boolean canContainDataProperties() {
            return false;
        }

        @Override
        public boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public boolean canContainDatatypes() {
            return false;
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectExactCardinalityImpl
     * @see OntCE.ObjectCardinality
     */
    public static class OEC
            extends WithClassAndObjectPropertyAndCardinality<OntCE.ObjectCardinality, OWLObjectExactCardinality>
            implements OWLObjectExactCardinality {

        public OEC(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.ObjectCardinality asRDFNode() {
            return as(OntCE.ObjectCardinality.class);
        }

        @FactoryAccessor
        @Override
        public OWLObjectIntersectionOf asIntersectionOfMinMax() {
            DataFactory df = getDataFactory();
            OWLObjectPropertyExpression p = getProperty();
            OWLClassExpression c = getFiller();
            int q = getCardinality();
            return df.getOWLObjectIntersectionOf(df.getOWLObjectMinCardinality(q, p, c),
                    df.getOWLObjectMaxCardinality(q, p, c));
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataExactCardinalityImpl
     * @see OntCE.DataCardinality
     */
    public static class DEC
            extends WithDataRangeAndDataPropertyAndCardinality<OntCE.DataCardinality, OWLDataExactCardinality>
            implements OWLDataExactCardinality {

        public DEC(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.DataCardinality asRDFNode() {
            return as(OntCE.DataCardinality.class);
        }

        @FactoryAccessor
        @Override
        public OWLObjectIntersectionOf asIntersectionOfMinMax() {
            DataFactory df = getDataFactory();
            OWLDataProperty p = getProperty();
            OWLDataRange c = getFiller();
            int q = getCardinality();
            return df.getOWLObjectIntersectionOf(df.getOWLDataMinCardinality(q, p, c),
                    df.getOWLDataMaxCardinality(q, p, c));
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectMinCardinalityImpl
     * @see OntCE.ObjectMinCardinality
     */
    public static class OMIC
            extends WithClassAndObjectPropertyAndCardinality<OntCE.ObjectMinCardinality, OWLObjectMinCardinality>
            implements OWLObjectMinCardinality {

        public OMIC(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.ObjectMinCardinality asRDFNode() {
            return as(OntCE.ObjectMinCardinality.class);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataMinCardinalityImpl
     * @see OntCE.DataMinCardinality
     */
    public static class DMIC
            extends WithDataRangeAndDataPropertyAndCardinality<OntCE.DataMinCardinality, OWLDataMinCardinality>
            implements OWLDataMinCardinality {

        public DMIC(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.DataMinCardinality asRDFNode() {
            return as(OntCE.DataMinCardinality.class);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectMaxCardinalityImpl
     * @see OntCE.ObjectMaxCardinality
     */
    public static class OMAC
            extends WithClassAndObjectPropertyAndCardinality<OntCE.ObjectMaxCardinality, OWLObjectMaxCardinality>
            implements OWLObjectMaxCardinality {

        public OMAC(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.ObjectMaxCardinality asRDFNode() {
            return as(OntCE.ObjectMaxCardinality.class);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLDataMaxCardinalityImpl
     * @see OntCE.DataMaxCardinality
     */
    public static class DMAC
            extends WithDataRangeAndDataPropertyAndCardinality<OntCE.DataMaxCardinality, OWLDataMaxCardinality>
            implements OWLDataMaxCardinality {

        public DMAC(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.DataMaxCardinality asRDFNode() {
            return as(OntCE.DataMaxCardinality.class);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectUnionOfImpl
     * @see OntCE.UnionOf
     */
    public static class UF
            extends WithClassMembers<OntCE.UnionOf, OWLObjectUnionOf>
            implements OWLObjectUnionOf {

        public UF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.UnionOf asRDFNode() {
            return as(OntCE.UnionOf.class);
        }

        @Override
        public Set<OWLClassExpression> asDisjunctSet() {
            Set<OWLClassExpression> res = createSortedSet();
            members().forEach(x -> res.addAll(x.getOWLObject().asDisjunctSet()));
            return res;
        }

        @Override
        public Stream<OWLClassExpression> disjunctSet() {
            return asDisjunctSet().stream();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectIntersectionOfImpl
     * @see OntCE.IntersectionOf
     */
    public static class IF
            extends WithClassMembers<OntCE.IntersectionOf, OWLObjectIntersectionOf>
            implements OWLObjectIntersectionOf {

        public IF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntCE.IntersectionOf asRDFNode() {
            return as(OntCE.IntersectionOf.class);
        }

        @Override
        public Set<OWLClassExpression> asConjunctSet() {
            Set<OWLClassExpression> res = createSortedSet();
            members().forEach(x -> res.addAll(x.getOWLObject().asConjunctSet()));
            return res;
        }

        @Override
        public Stream<OWLClassExpression> conjunctSet() {
            return asConjunctSet().stream();
        }

        @Override
        public boolean containsConjunct(@Nullable OWLClassExpression ce) {
            return asConjunctSet().contains(ce);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectOneOfImpl
     * @see OntCE.OneOf
     */
    public static class OF
            extends WithMembers<OntIndividual, OntCE.OneOf, OWLIndividual, OWLObjectOneOf>
            implements OWLObjectOneOf {

        public OF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        protected ONTObject<? extends OWLIndividual> map(OntIndividual member, InternalObjectFactory factory) {
            return factory.getIndividual(member);
        }

        @Override
        protected Object toContentItem(ONTObject<? extends OWLIndividual> individual) {
            if (!individual.getOWLObject().isOWLNamedIndividual()) {
                return ((OWLAnonymousIndividualImpl) individual).getBlankNodeId();
            }
            return individual.getOWLObject().asOWLNamedIndividual().toStringID();
        }

        @Override
        protected ONTObject<? extends OWLIndividual> fromContentItem(Object item, InternalObjectFactory factory) {
            return toIndividual(item, factory);
        }

        @Override
        public OntCE.OneOf asRDFNode() {
            return as(OntCE.OneOf.class);
        }

        @Override
        public Stream<OWLIndividual> individuals() {
            return operands();
        }

        @FactoryAccessor
        @Override
        public OWLClassExpression asObjectUnionOf() {
            Collection<ONTObject<? extends OWLIndividual>> values = members().collect(Collectors.toSet());
            if (values.size() == 1) {
                return this;
            }
            DataFactory df = getDataFactory();
            return df.getOWLObjectUnionOf(values.stream().map(x -> df.getOWLObjectOneOf(x.getOWLObject())));
        }

        @Override
        public boolean containsNamedIndividual(OWLNamedIndividual individual) {
            return namedIndividuals().anyMatch(individual::equals);
        }

        @Override
        public Set<OWLEntity> getSignatureSet() {
            return namedIndividuals().collect(Collectors.toCollection(OWLObjectImpl::createSortedSet));
        }

        @Override
        public Set<OWLNamedIndividual> getNamedIndividualSet() {
            return namedIndividuals().collect(Collectors.toCollection(OWLObjectImpl::createSortedSet));
        }

        @Override
        public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
            return objectIndividuals()
                    .map(x -> x.getOWLObject().isOWLNamedIndividual() ? null :
                            x.getOWLObject().asOWLAnonymousIndividual())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(OWLObjectImpl::createSortedSet));
        }

        protected Stream<OWLNamedIndividual> namedIndividuals() {
            return objectIndividuals()
                    .map(x -> x.getOWLObject().isOWLNamedIndividual() ?
                            x.getOWLObject().asOWLNamedIndividual() : null)
                    .filter(Objects::nonNull);
        }

        @SuppressWarnings("unchecked")
        protected Stream<ONTObject<? extends OWLIndividual>> objectIndividuals() {
            return (Stream<ONTObject<? extends OWLIndividual>>) objects(getObjectFactory());
        }

        @Override
        public Set<OWLClassExpression> getClassExpressionSet() {
            return createSet(this);
        }

        @Override
        public boolean canContainDataProperties() {
            return false;
        }

        @Override
        public boolean canContainObjectProperties() {
            return false;
        }

        @Override
        public boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public boolean canContainDatatypes() {
            return false;
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.ce.OWLObjectComplementOfImpl
     * @see OntCE.ComplementOf
     */
    public static class CF
            extends Restriction<OntCE.ComplementOf, OWLObjectComplementOf>
            implements OWLObjectComplementOf {

        public CF(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public boolean isClassExpressionLiteral() {
            return getONTClassExpression().getOWLObject().isOWLClass();
        }

        @Override
        public OntCE.ComplementOf asRDFNode() {
            return as(OntCE.ComplementOf.class);
        }

        @Override
        public OWLClassExpression getOperand() {
            return getONTClassExpression().getOWLObject();
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            return Stream.of(getONTClassExpression());
        }

        protected ONTObject<? extends OWLClassExpression> getONTClassExpression() {
            return findCE(getObjectFactory());
        }

        protected ONTObject<? extends OWLClassExpression> findCE(InternalObjectFactory factory) {
            return toCE(getContent()[0], factory);
        }

        @Override
        protected Object[] collectContent(OntCE.ComplementOf ce, InternalObjectFactory factory) {
            return new Object[]{toContentItem(ce.getValue(), factory)};
        }

        @Override
        protected Object[] initContent(OntCE.ComplementOf ce, InternalObjectFactory factory) {
            OntCE c = ce.getValue();
            return initContent(c.asNode(), factory.getClass(c));
        }
    }

    protected abstract static class WithClassMembers<ONT extends OntCE.ComponentsCE<OntCE>,
            OWL extends OWLAnonymousClassExpression>
            extends WithMembers<OntCE, ONT, OWLClassExpression, OWL> {

        protected WithClassMembers(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        protected Object toContentItem(ONTObject<? extends OWLClassExpression> ce) {
            OWLClassExpression res = ce.getOWLObject();
            return res.isOWLClass() ? res.asOWLClass().toStringID() : ce;
        }

        @Override
        protected ONTObject<? extends OWLClassExpression> fromContentItem(Object item, InternalObjectFactory factory) {
            return toCE(item, factory);
        }

        @Override
        protected ONTObject<? extends OWLClassExpression> map(OntCE member, InternalObjectFactory factory) {
            return factory.getClass(member);
        }
    }

    /**
     * Describes a class expression that is based on []-list.
     *
     * @param <ONT_M> subtype of {@link OntObject} - member of list
     * @param <ONT_C> subtype of {@link OntCE.ComponentsCE} - the class expression
     * @param <OWL_M> subtype of {@link OWLObject} that matches {@link ONT_M}
     * @param <OWL_C> subtype of {@link OWLAnonymousClassExpression} that matches {@link ONT_C}
     */
    protected abstract static class WithMembers<ONT_M extends OntObject,
            ONT_C extends OntCE.ComponentsCE<ONT_M>,
            OWL_M extends OWLObject,
            OWL_C extends OWLAnonymousClassExpression>
            extends ONTAnonymousClassExpressionImpl<ONT_C, OWL_C>
            implements HasOperands<OWL_M>, HasComponents {

        protected WithMembers(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        /**
         * Translates Jena {@link ONT_M} resource to OWL-API {@link OWL_M} equivalent.
         *
         * @param member  {@link ONT_M}, not {@code null}
         * @param factory {@link InternalObjectFactory}, not {@code null}
         * @return an {@link ONTObject} that wraps {@link OWL_M}
         */
        protected abstract ONTObject<? extends OWL_M> map(ONT_M member, InternalObjectFactory factory);

        /**
         * Prepares an {@link OWL_M} to store in cache (content array).
         *
         * @param member the {@link ONTObject} that wraps {@link OWL_M}, not {@code null}
         * @return {@code Object}
         */
        protected abstract Object toContentItem(ONTObject<? extends OWL_M> member);

        /**
         * Extracts an {@link OWL_M} from content item.
         *
         * @param item    an {@code Object} from cache, not {@code null}
         * @param factory {@link InternalObjectFactory}, not {@code null}
         * @return an {@link ONTObject} that wraps {@link OWL_M}
         */
        protected abstract ONTObject<? extends OWL_M> fromContentItem(Object item, InternalObjectFactory factory);

        @Override
        public Stream<OWL_M> operands() {
            return members().map(ONTObject::getOWLObject);
        }

        @Override
        public List<OWL_M> getOperandsAsList() {
            return operands().collect(Collectors.toList());
        }

        @SuppressWarnings("unchecked")
        protected Stream<ONTObject<? extends OWL_M>> members() {
            return (Stream<ONTObject<? extends OWL_M>>) objects(getObjectFactory());
        }

        @SuppressWarnings("unchecked")
        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            return (Stream<ONTObject<? extends OWLObject>>) objects(getObjectFactory());
        }

        protected Stream objects(InternalObjectFactory factory) {
            return Arrays.stream(getContent()).map(x -> fromContentItem(x, factory));
        }

        @Override
        protected Object[] collectContent(ONT_C ce, InternalObjectFactory factory) {
            Set<ONTObject<? extends OWL_M>> operands = operands(ce, factory);
            Object[] res = new Object[operands.size()];
            int index = 0;
            for (ONTObject<? extends OWL_M> op : operands) {
                res[index++] = toContentItem(op);
            }
            return res;
        }

        @Override
        protected Object[] initContent(ONT_C ce, InternalObjectFactory factory) {
            Set<ONTObject<? extends OWL_M>> operands = operands(ce, factory);
            Object[] res = new Object[operands.size()];
            int index = 0;
            int hash = 1;
            for (ONTObject<? extends OWL_M> op : operands) {
                res[index++] = toContentItem(op);
                hash = 31 * hash + op.hashCode();
            }
            this.hashCode = OWLObject.hashIteration(hashIndex(), hash);
            return res;
        }

        protected Set<ONTObject<? extends OWL_M>> operands(ONT_C ce, InternalObjectFactory factory) {
            Set<ONTObject<? extends OWL_M>> res = createContentSet();
            OntModels.listMembers(ce.getList()).forEachRemaining(e -> res.add(map(e, factory)));
            return res;
        }
    }

    /**
     * Represents a data property restriction (class expression) with cardinality.
     *
     * @param <ONT> - a subtype of {@link OntCE.CardinalityRestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithDataRangeAndDataPropertyAndCardinality<ONT extends OntCE.CardinalityRestrictionCE<OntDR, OntNDP>,
            OWL extends OWLRestriction>
            extends WithDataRangeAndDataPropertyUnary<ONT, OWL> {

        protected WithDataRangeAndDataPropertyAndCardinality(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        public int getCardinality() {
            // [property, cardinality, filler]
            return (int) getContent()[1];
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            return Stream.of(getONTDataProperty(), getONTDataRange());
        }

        @Override
        protected Object[] collectContent(ONT ce, InternalObjectFactory factory) {
            // [property, cardinality, filler]
            return new Object[]{toContentItem(ce.getProperty()),
                    ce.getCardinality(), toContentItem(ce.getValue(), factory)};
        }

        @Override
        protected Object[] initContent(ONT ce, InternalObjectFactory factory) {
            OntNDP p = ce.getProperty();
            OntDR v = ce.getValue();
            int c = ce.getCardinality();
            return initContent(p.asNode(), v.asNode(), factory.getProperty(p), c, factory.getDatatype(v));
        }
    }

    /**
     * Represents a n-ary universal or existential data property restriction.
     *
     * @param <ONT> - a suptype of {@link OntCE.NaryRestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithDataRangeAndDataPropertyNary<ONT extends OntCE.NaryRestrictionCE<OntDR, OntNDP>,
            OWL extends OWLRestriction>
            extends WithDataRangeAndDataProperty<ONT, OWL> {

        protected WithDataRangeAndDataPropertyNary(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        protected OntDR getValue(ONT ce) {
            return ce.getValue();
        }
    }

    /**
     * Represents an unary data property restriction that has a reference to a data range.
     *
     * @param <ONT> - a subtype of {@link OntCE.ComponentRestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithDataRangeAndDataPropertyUnary<ONT extends OntCE.ComponentRestrictionCE<OntDR, OntNDP>,
            OWL extends OWLRestriction>
            extends WithDataRangeAndDataProperty<ONT, OWL> {

        protected WithDataRangeAndDataPropertyUnary(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        protected OntDR getValue(ONT ce) {
            return ce.getValue();
        }
    }

    /**
     * Represents a data property restriction that has a reference to a data range.
     *
     * @param <ONT> - a subtype of {@link OntCE.RestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithDataRangeAndDataProperty<ONT extends OntCE.RestrictionCE<OntNDP>,
            OWL extends OWLRestriction>
            extends WithDataProperty<ONT, OWL> {

        protected WithDataRangeAndDataProperty(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        public OWLDataRange getFiller() {
            return getONTDataRange().getOWLObject();
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            InternalObjectFactory factory = getObjectFactory();
            return Stream.of(findNDP(factory), findDR(factory));
        }

        public ONTObject<? extends OWLDataRange> getONTDataRange() {
            return findDR(getObjectFactory());
        }

        protected ONTObject<? extends OWLDataRange> findDR(InternalObjectFactory factory) {
            // [property, cardinality, filler] or [property, filler] - always last:
            Object[] array = getContent();
            return toDR(array[array.length - 1], factory);
        }

        @Override
        protected Object[] collectContent(ONT ce, InternalObjectFactory factory) {
            // [property, cardinality, filler] or [property, filler]
            return new Object[]{toContentItem(ce.getProperty()), toContentItem(getValue(ce), factory)};
        }

        protected abstract OntDR getValue(ONT ce);

        @Override
        protected Object[] initContent(ONT ce, InternalObjectFactory factory) {
            OntNDP p = ce.getProperty();
            OntDR v = getValue(ce);
            return initContent(p.asNode(), v.asNode(), factory.getProperty(p), factory.getDatatype(v));
        }
    }

    /**
     * An abstraction for simplification code,
     * that describes {@code owl:Restriction} containing datatype property.
     * Its signature also contains {@link OWLDatatype datatype} (explicitly or implicitly).
     * It cannot contain nested class expressions.
     *
     * @param <ONT> - a subtype of {@link OntCE.RestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithDataProperty<ONT extends OntCE.RestrictionCE<OntNDP>,
            OWL extends OWLRestriction>
            extends Restriction<ONT, OWL> {

        protected WithDataProperty(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        public OWLDataProperty getProperty() {
            return getONTDataProperty().getOWLObject();
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            return Stream.of(getONTDataProperty());
        }

        public ONTObject<OWLDataProperty> getONTDataProperty() {
            return findNDP(getObjectFactory());
        }

        protected ONTObject<OWLDataProperty> findNDP(InternalObjectFactory factory) {
            // [property, cardinality, filler] or [property, filler] or [property] - always first
            return toNDP(getContent()[0], factory);
        }

        @Override
        protected Object[] collectContent(ONT ce, InternalObjectFactory factory) {
            // [property, cardinality, filler] or [property, filler] or [property]
            return new Object[]{toContentItem(ce.getProperty())};
        }

        @Override
        protected Object[] initContent(ONT ce, InternalObjectFactory factory) {
            OntNDP p = ce.getProperty();
            return initContent(p.asNode(), factory.getProperty(p));
        }

        @Override
        public boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public boolean canContainNamedIndividuals() {
            return false;
        }

        @Override
        public boolean canContainObjectProperties() {
            return false;
        }

        @Override
        public Set<OWLClassExpression> getClassExpressionSet() {
            return createSet(this);
        }

        @Override
        public boolean canContainAnonymousIndividuals() {
            return false;
        }
    }

    /**
     * Represents an object class expression with cardinality.
     *
     * @param <ONT> - a subtype of {@link OntCE.CardinalityRestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithClassAndObjectPropertyAndCardinality<ONT extends OntCE.CardinalityRestrictionCE<OntCE, OntOPE>,
            OWL extends OWLRestriction>
            extends WithClassAndObjectProperty<ONT, OWL> {
        protected WithClassAndObjectPropertyAndCardinality(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        public int getCardinality() {
            // [property, cardinality, filler]
            return (int) getContent()[1];
        }

        @Override
        protected Object[] collectContent(ONT ce, InternalObjectFactory factory) {
            // [property, cardinality, filler]
            return new Object[]{toContentItem(ce.getProperty(), factory),
                    ce.getCardinality(), toContentItem(ce.getValue(), factory)};
        }

        @Override
        protected Object[] initContent(ONT ce, InternalObjectFactory factory) {
            OntOPE p = ce.getProperty();
            OntCE v = ce.getValue();
            int c = ce.getCardinality();
            return initContent(p.asNode(), v.asNode(), factory.getProperty(p), c, factory.getClass(v));
        }
    }

    /**
     * Represents an object class expression,
     * that has reference to another class expression and object property expression.
     * @param <ONT> - a subtype of {@link OntCE.ComponentRestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithClassAndObjectProperty<ONT extends OntCE.ComponentRestrictionCE<OntCE, OntOPE>,
            OWL extends OWLRestriction>
            extends WithObjectProperty<ONT, OWL> {

        protected WithClassAndObjectProperty(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        public OWLClassExpression getFiller() {
            return getONTClassExpression().getOWLObject();
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            InternalObjectFactory factory = getObjectFactory();
            return Stream.of(findOPE(factory), findCE(factory));
        }

        public ONTObject<? extends OWLClassExpression> getONTClassExpression() {
            return findCE(getObjectFactory());
        }

        protected ONTObject<? extends OWLClassExpression> findCE(InternalObjectFactory factory) {
            // [property, cardinality, filler] or [property, filler] -- always last:
            Object[] array = getContent();
            return toCE(array[array.length - 1], factory);
        }

        @Override
        protected Object[] collectContent(ONT ce, InternalObjectFactory factory) {
            return new Object[]{toContentItem(ce.getProperty(), factory), toContentItem(ce.getValue(), factory)};
        }

        @Override
        protected Object[] initContent(ONT ce, InternalObjectFactory factory) {
            OntOPE p = ce.getProperty();
            OntCE v = ce.getValue();
            return initContent(p.asNode(), v.asNode(), factory.getProperty(p), factory.getClass(v));
        }
    }

    /**
     * Represents a class expression with a reference to an object property expression.
     * @param <ONT> - a subtype of {@link OntCE.RestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithObjectProperty<ONT extends OntCE.RestrictionCE<OntOPE>,
            OWL extends OWLRestriction>
            extends Restriction<ONT, OWL> {

        protected WithObjectProperty(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            return Stream.of(getONTObjectPropertyExpression());
        }

        public OWLObjectPropertyExpression getProperty() {
            return getONTObjectPropertyExpression().getOWLObject();
        }

        public ONTObject<? extends OWLObjectPropertyExpression> getONTObjectPropertyExpression() {
            return findOPE(getObjectFactory());
        }

        protected ONTObject<? extends OWLObjectPropertyExpression> findOPE(InternalObjectFactory factory) {
            // [property, cardinality, filler] or [property, filler] or [property] - always first:
            return toOPE(getContent()[0], factory);
        }

        @Override
        protected Object[] collectContent(ONT ce, InternalObjectFactory factory) {
            return new Object[]{toContentItem(ce.getProperty(), factory)};
        }

        @Override
        protected Object[] initContent(ONT ce, InternalObjectFactory factory) {
            OntOPE p = ce.getProperty();
            return initContent(p.asNode(), factory.getProperty(p));
        }
    }

    /**
     * A base for {@code owl:Restriction} class expression
     * with helpers to calculate hashcode and to collect content cache.
     *
     * @param <ONT> - subtype of {@link OntCE}
     * @param <OWL> - subtype of {@link OWLAnonymousClassExpression}
     */
    protected abstract static class Restriction<ONT extends OntCE, OWL extends OWLAnonymousClassExpression>
            extends ONTAnonymousClassExpressionImpl<ONT, OWL> {

        protected Restriction(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        Object[] initContent(Node p, Node v, Object first, int c, Object last) {
            int res = OWLObject.hashIteration(hashIndex(), first.hashCode());
            res = OWLObject.hashIteration(res, c);
            res = OWLObject.hashIteration(res, last.hashCode());
            this.hashCode = res;
            // [property, cardinality, filler]
            return new Object[]{toFirstContentItem(p, first), c, toLastContentItem(v, last)};
        }

        Object[] initContent(Node p, Node v, Object first, Object last) {
            int res = OWLObject.hashIteration(hashIndex(), first.hashCode());
            res = OWLObject.hashIteration(res, last.hashCode());
            this.hashCode = res;
            // [property, filler]
            return new Object[]{toFirstContentItem(p, first), toLastContentItem(v, last)};
        }

        Object[] initContent(Node p, Object first) {
            this.hashCode = OWLObject.hashIteration(hashIndex(), first.hashCode());
            // [property|single value]
            return new Object[]{toFirstContentItem(p, first)};
        }

        Object toFirstContentItem(Node node, Object expr) {
            return node.isURI() ? node.getURI() : expr;
        }

        Object toLastContentItem(Node node, Object expr) {
            return node.isURI() ? node.getURI() : expr;
        }
    }
}
