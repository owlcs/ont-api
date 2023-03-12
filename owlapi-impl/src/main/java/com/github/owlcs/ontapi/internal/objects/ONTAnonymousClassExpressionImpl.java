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

package com.github.owlcs.ontapi.internal.objects;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntDataProperty;
import com.github.owlcs.ontapi.jena.model.OntDataRange;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntObject;
import com.github.owlcs.ontapi.jena.model.OntObjectProperty;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.owlapi.OWLObjectImpl;
import com.github.owlcs.ontapi.owlapi.objects.AnonymousIndividualImpl;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.semanticweb.owlapi.model.HasComponents;
import org.semanticweb.owlapi.model.HasOperands;
import org.semanticweb.owlapi.model.OWLAnonymousClassExpression;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLRestriction;
import org.semanticweb.owlapi.util.NNF;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An abstraction for any anonymous class expressions
 * (i.e. for all class expressions with except of {@code OWLClass}es).
 * <p>
 * Created by @ssz on 10.08.2019.
 *
 * @param <ONT> any subtype of {@link OntClass}
 * @param <OWL> subtype of {@link OWLAnonymousClassExpression}, which matches {@link ONT}
 * @see com.github.owlcs.ontapi.owlapi.objects.ce.AnonymousClassExpressionImpl
 * @see ONTClassImpl
 * @see com.github.owlcs.ontapi.internal.ReadHelper#calcClassExpression(OntClass, ONTObjectFactory, Set)
 * @see OntClass
 * @see OntClass.Named
 * @since 2.0.0
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTAnonymousClassExpressionImpl<ONT extends OntClass, OWL extends OWLAnonymousClassExpression>
        extends ONTExpressionImpl<ONT>
        implements OWLAnonymousClassExpression, ModelObject<OWL> {

    protected ONTAnonymousClassExpressionImpl(BlankNodeId n, Supplier<OntModel> m) {
        super(n, m);
    }

    /**
     * Wraps the given {@link OntClass} as {@link OWLAnonymousClassExpression} and {@link ONTObject}.
     *
     * @param ce      {@link OntClass}, not {@code null}, must be anonymous
     * @param factory {@link ONTObjectFactory}, not {@code null}
     * @param model   a provider of non-null {@link OntModel}, not {@code null}
     * @return {@link ONTAnonymousClassExpressionImpl} instance
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ONTAnonymousClassExpressionImpl create(OntClass ce,
                                                         ONTObjectFactory factory,
                                                         Supplier<OntModel> model) {
        Class<? extends OntClass> type = OntModels.getOntType(ce);
        BlankNodeId id = ce.asNode().getBlankNodeId();
        ONTAnonymousClassExpressionImpl res = create(id, type, model);
        // since we have already the type information
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
     * @param model a provider of non-null {@link OntModel}, not {@code null}
     * @return {@link ONTAnonymousClassExpressionImpl} instance
     */
    public static ONTAnonymousClassExpressionImpl<?, ?> create(BlankNodeId id,
                                                               Class<? extends OntClass> type,
                                                               Supplier<OntModel> model) {
        if (OntClass.ObjectSomeValuesFrom.class == type) {
            return new OSVF(id, model);
        }
        if (OntClass.DataSomeValuesFrom.class == type) {
            return new DSVF(id, model);
        }
        if (OntClass.ObjectAllValuesFrom.class == type) {
            return new OAVF(id, model);
        }
        if (OntClass.DataAllValuesFrom.class == type) {
            return new DAVF(id, model);
        }
        if (OntClass.ObjectHasValue.class == type) {
            return new OHV(id, model);
        }
        if (OntClass.DataHasValue.class == type) {
            return new DHV(id, model);
        }
        if (OntClass.HasSelf.class == type) {
            return new OHS(id, model);
        }
        if (OntClass.ObjectCardinality.class == type) {
            return new OEC(id, model);
        }
        if (OntClass.DataCardinality.class == type) {
            return new DEC(id, model);
        }
        if (OntClass.ObjectMinCardinality.class == type) {
            return new OMIC(id, model);
        }
        if (OntClass.DataMinCardinality.class == type) {
            return new DMIC(id, model);
        }
        if (OntClass.ObjectMaxCardinality.class == type) {
            return new OMAC(id, model);
        }
        if (OntClass.DataMaxCardinality.class == type) {
            return new DMAC(id, model);
        }
        if (OntClass.UnionOf.class == type) {
            return new UF(id, model);
        }
        if (OntClass.IntersectionOf.class == type) {
            return new IF(id, model);
        }
        if (OntClass.OneOf.class == type) {
            return new OF(id, model);
        }
        if (OntClass.ComplementOf.class == type) {
            return new CF(id, model);
        }
        if (OntClass.NaryDataSomeValuesFrom.class == type) {
            return new NDSVF(id, model);
        }
        if (OntClass.NaryDataAllValuesFrom.class == type) {
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
        return eraseModel().accept(getNNFClassVisitor());
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
        return getDataFactory().getOWLObjectComplementOf(eraseModel());
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
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.ObjectSomeValuesFromImpl
     * @see OntClass.ObjectSomeValuesFrom
     */
    public static class OSVF
            extends WithClassAndObjectProperty<OntClass.ObjectSomeValuesFrom, OWLObjectSomeValuesFrom>
            implements OWLObjectSomeValuesFrom {

        public OSVF(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.ObjectSomeValuesFrom asRDFNode() {
            return as(OntClass.ObjectSomeValuesFrom.class);
        }

        @Override
        protected OWLObjectSomeValuesFrom fromFactory(OWLObjectPropertyExpression p, OWLClassExpression c) {
            return getDataFactory().getOWLObjectSomeValuesFrom(p, c);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.ObjectAllValuesFromImpl
     * @see OntClass.ObjectAllValuesFrom
     */
    public static class OAVF
            extends WithClassAndObjectProperty<OntClass.ObjectAllValuesFrom, OWLObjectAllValuesFrom>
            implements OWLObjectAllValuesFrom {

        public OAVF(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.ObjectAllValuesFrom asRDFNode() {
            return as(OntClass.ObjectAllValuesFrom.class);
        }

        @Override
        protected OWLObjectAllValuesFrom fromFactory(OWLObjectPropertyExpression p, OWLClassExpression c) {
            return getDataFactory().getOWLObjectAllValuesFrom(p, c);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.DataSomeValuesFromImpl
     * @see OntClass.DataSomeValuesFrom
     */
    public static class DSVF
            extends WithDataRangeAndDataPropertyUnary<OntClass.DataSomeValuesFrom, OWLDataSomeValuesFrom>
            implements OWLDataSomeValuesFrom {

        public DSVF(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.DataSomeValuesFrom asRDFNode() {
            return as(OntClass.DataSomeValuesFrom.class);
        }

        @Override
        protected OWLDataSomeValuesFrom fromFactory(OWLDataProperty p, OWLDataRange d) {
            return getDataFactory().getOWLDataSomeValuesFrom(p, d);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.DataAllValuesFromImpl
     * @see OntClass.DataAllValuesFrom
     */
    public static class DAVF
            extends WithDataRangeAndDataPropertyUnary<OntClass.DataAllValuesFrom, OWLDataAllValuesFrom>
            implements OWLDataAllValuesFrom {

        public DAVF(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.DataAllValuesFrom asRDFNode() {
            return as(OntClass.DataAllValuesFrom.class);
        }

        @Override
        protected OWLDataAllValuesFrom fromFactory(OWLDataProperty p, OWLDataRange d) {
            return getDataFactory().getOWLDataAllValuesFrom(p, d);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.DataSomeValuesFromImpl
     * @see OntClass.NaryDataSomeValuesFrom
     */
    public static class NDSVF
            extends WithDataRangeAndDataPropertyNary<OntClass.NaryDataSomeValuesFrom, OWLDataSomeValuesFrom>
            implements OWLDataSomeValuesFrom {

        public NDSVF(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.NaryDataSomeValuesFrom asRDFNode() {
            return as(OntClass.NaryDataSomeValuesFrom.class);
        }

        @Override
        protected OWLDataSomeValuesFrom fromFactory(OWLDataProperty p, OWLDataRange d) {
            return getDataFactory().getOWLDataSomeValuesFrom(p, d);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.DataAllValuesFromImpl
     * @see OntClass.NaryDataAllValuesFrom
     */
    public static class NDAVF
            extends WithDataRangeAndDataPropertyNary<OntClass.NaryDataAllValuesFrom, OWLDataAllValuesFrom>
            implements OWLDataAllValuesFrom {

        public NDAVF(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.NaryDataAllValuesFrom asRDFNode() {
            return as(OntClass.NaryDataAllValuesFrom.class);
        }

        @Override
        protected OWLDataAllValuesFrom fromFactory(OWLDataProperty p, OWLDataRange d) {
            return getDataFactory().getOWLDataAllValuesFrom(p, d);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.ObjectHasValueImpl
     * @see OntClass.ObjectHasValue
     */
    public static class OHV
            extends WithObjectProperty<OntClass.ObjectHasValue, OWLObjectHasValue>
            implements OWLObjectHasValue {

        protected OHV(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.ObjectHasValue asRDFNode() {
            return as(OntClass.ObjectHasValue.class);
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            ModelObjectFactory factory = getObjectFactory();
            return Stream.of(findOPE(factory), findIndividual(factory));
        }

        @FactoryAccessor
        @Override
        public OWLObjectSomeValuesFrom asSomeValuesFrom() {
            DataFactory df = getDataFactory();
            return df.getOWLObjectSomeValuesFrom(eraseModel(getProperty()),
                    df.getOWLObjectOneOf(eraseModel(getFiller())));
        }

        @Override
        public OWLIndividual getFiller() {
            return getONTIndividual().getOWLObject();
        }

        public ONTObject<? extends OWLIndividual> getONTIndividual() {
            return findIndividual(getObjectFactory());
        }

        protected ONTObject<? extends OWLIndividual> findIndividual(ModelObjectFactory factory) {
            // [property, filler] - always last:
            return toIndividual(getContent()[1], factory);
        }

        @Override
        protected Object[] collectContent(OntClass.ObjectHasValue ce, ONTObjectFactory factory) {
            // [property, filler]
            return new Object[]{toContentItem(ce.getProperty(), factory), toContentItem(ce.getValue())};
        }

        @Override
        protected Object[] initContent(OntClass.ObjectHasValue ce, ONTObjectFactory factory) {
            OntObjectProperty p = ce.getProperty();
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

        @Override
        public OWLObjectHasValue eraseModel() {
            return getDataFactory().getOWLObjectHasValue(eraseModel(getProperty()), eraseModel(getFiller()));
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.DataHasValueImpl
     * @see OntClass.DataHasValue
     */
    public static class DHV
            extends WithDataProperty<OntClass.DataHasValue, OWLDataHasValue>
            implements OWLDataHasValue {

        protected DHV(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.DataHasValue asRDFNode() {
            return as(OntClass.DataHasValue.class);
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            ModelObjectFactory factory = getObjectFactory();
            return Stream.of(findNDP(factory), findLiteral(factory));
        }

        @FactoryAccessor
        @Override
        public OWLDataSomeValuesFrom asSomeValuesFrom() {
            DataFactory df = getDataFactory();
            return df.getOWLDataSomeValuesFrom(eraseModel(getProperty()), df.getOWLDataOneOf(eraseModel(getFiller())));
        }

        @Override
        public OWLLiteral getFiller() {
            return getONTLiteral().getOWLObject();
        }

        public ONTObject<OWLLiteral> getONTLiteral() {
            return findLiteral(getObjectFactory());
        }

        protected ONTObject<OWLLiteral> findLiteral(ModelObjectFactory factory) {
            // [property, filler] -- always last:
            return toLiteral(getContent()[1], factory);
        }

        @Override
        protected Object[] collectContent(OntClass.DataHasValue ce, ONTObjectFactory factory) {
            // [property, filler]
            return new Object[]{toContentItem(ce.getProperty()), toContentItem(ce.getValue())};
        }

        @Override
        protected Object[] initContent(OntClass.DataHasValue ce, ONTObjectFactory factory) {
            OntDataProperty p = ce.getProperty();
            Literal v = ce.getValue();
            return initContent(p.asNode(), v.asNode(), factory.getProperty(p), factory.getLiteral(v));
        }

        @Override
        Object toLastContentItem(Node node, Object expr) {
            return node.getLiteral();
        }

        @Override
        public OWLDataHasValue eraseModel() {
            return getDataFactory().getOWLDataHasValue(eraseModel(getProperty()), eraseModel(getFiller()));
        }
    }

    /**
     * An {@code ObjectHasSelf} implementation.
     * It does not contain boolean datatype in signature -
     * see <a href="https://github.com/owlcs/owlapi/issues/783">owlcs/owlapi##783</a>.
     *
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.ObjectHasSelfImpl
     * @see OntClass.HasSelf
     */
    public static class OHS
            extends WithObjectProperty<OntClass.HasSelf, OWLObjectHasSelf>
            implements OWLObjectHasSelf {

        protected OHS(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.HasSelf asRDFNode() {
            return as(OntClass.HasSelf.class);
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

        @Override
        public OWLObjectHasSelf eraseModel() {
            return getDataFactory().getOWLObjectHasSelf(eraseModel(getProperty()));
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.ObjectExactCardinalityImpl
     * @see OntClass.ObjectCardinality
     */
    public static class OEC
            extends WithClassAndObjectPropertyAndCardinality<OntClass.ObjectCardinality, OWLObjectExactCardinality>
            implements OWLObjectExactCardinality {

        public OEC(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.ObjectCardinality asRDFNode() {
            return as(OntClass.ObjectCardinality.class);
        }

        @FactoryAccessor
        @Override
        public OWLObjectIntersectionOf asIntersectionOfMinMax() {
            DataFactory df = getDataFactory();
            OWLObjectPropertyExpression p = eraseModel(getProperty());
            OWLClassExpression c = eraseModel(getFiller());
            int q = getCardinality();
            return df.getOWLObjectIntersectionOf(df.getOWLObjectMinCardinality(q, p, c),
                    df.getOWLObjectMaxCardinality(q, p, c));
        }

        @Override
        public OWLObjectExactCardinality fromFactory(OWLObjectPropertyExpression p, OWLClassExpression c) {
            return getDataFactory().getOWLObjectExactCardinality(getCardinality(), p, c);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.DataExactCardinalityImpl
     * @see OntClass.DataCardinality
     */
    public static class DEC
            extends WithDataRangeAndDataPropertyAndCardinality<OntClass.DataCardinality, OWLDataExactCardinality>
            implements OWLDataExactCardinality {

        public DEC(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.DataCardinality asRDFNode() {
            return as(OntClass.DataCardinality.class);
        }

        @FactoryAccessor
        @Override
        public OWLObjectIntersectionOf asIntersectionOfMinMax() {
            DataFactory df = getDataFactory();
            OWLDataProperty p = eraseModel(getProperty());
            OWLDataRange c = eraseModel(getFiller());
            int q = getCardinality();
            return df.getOWLObjectIntersectionOf(df.getOWLDataMinCardinality(q, p, c),
                    df.getOWLDataMaxCardinality(q, p, c));
        }

        @Override
        protected OWLDataExactCardinality fromFactory(OWLDataProperty p, OWLDataRange d) {
            return getDataFactory().getOWLDataExactCardinality(getCardinality(), p, d);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.ObjectMinCardinalityImpl
     * @see OntClass.ObjectMinCardinality
     */
    public static class OMIC
            extends WithClassAndObjectPropertyAndCardinality<OntClass.ObjectMinCardinality, OWLObjectMinCardinality>
            implements OWLObjectMinCardinality {

        public OMIC(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.ObjectMinCardinality asRDFNode() {
            return as(OntClass.ObjectMinCardinality.class);
        }

        @Override
        public OWLObjectMinCardinality fromFactory(OWLObjectPropertyExpression p, OWLClassExpression c) {
            return getDataFactory().getOWLObjectMinCardinality(getCardinality(), p, c);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.DataMinCardinalityImpl
     * @see OntClass.DataMinCardinality
     */
    public static class DMIC
            extends WithDataRangeAndDataPropertyAndCardinality<OntClass.DataMinCardinality, OWLDataMinCardinality>
            implements OWLDataMinCardinality {

        public DMIC(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.DataMinCardinality asRDFNode() {
            return as(OntClass.DataMinCardinality.class);
        }

        @Override
        protected OWLDataMinCardinality fromFactory(OWLDataProperty p, OWLDataRange d) {
            return getDataFactory().getOWLDataMinCardinality(getCardinality(), p, d);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.ObjectMaxCardinalityImpl
     * @see OntClass.ObjectMaxCardinality
     */
    public static class OMAC
            extends WithClassAndObjectPropertyAndCardinality<OntClass.ObjectMaxCardinality, OWLObjectMaxCardinality>
            implements OWLObjectMaxCardinality {

        public OMAC(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.ObjectMaxCardinality asRDFNode() {
            return as(OntClass.ObjectMaxCardinality.class);
        }

        @Override
        public OWLObjectMaxCardinality fromFactory(OWLObjectPropertyExpression p, OWLClassExpression c) {
            return getDataFactory().getOWLObjectMaxCardinality(getCardinality(), p, c);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.DataMaxCardinalityImpl
     * @see OntClass.DataMaxCardinality
     */
    public static class DMAC
            extends WithDataRangeAndDataPropertyAndCardinality<OntClass.DataMaxCardinality, OWLDataMaxCardinality>
            implements OWLDataMaxCardinality {

        public DMAC(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.DataMaxCardinality asRDFNode() {
            return as(OntClass.DataMaxCardinality.class);
        }

        @Override
        protected OWLDataMaxCardinality fromFactory(OWLDataProperty p, OWLDataRange d) {
            return getDataFactory().getOWLDataMaxCardinality(getCardinality(), p, d);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.ObjectUnionOfImpl
     * @see OntClass.UnionOf
     */
    public static class UF
            extends WithClassMembers<OntClass.UnionOf, OWLObjectUnionOf>
            implements OWLObjectUnionOf {

        public UF(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.UnionOf asRDFNode() {
            return as(OntClass.UnionOf.class);
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

        @Override
        public OWLObjectUnionOf eraseModel() {
            return getDataFactory().getOWLObjectUnionOf(factoryObjects());
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.ObjectIntersectionOfImpl
     * @see OntClass.IntersectionOf
     */
    public static class IF
            extends WithClassMembers<OntClass.IntersectionOf, OWLObjectIntersectionOf>
            implements OWLObjectIntersectionOf {

        public IF(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public OntClass.IntersectionOf asRDFNode() {
            return as(OntClass.IntersectionOf.class);
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

        @Override
        public OWLObjectIntersectionOf eraseModel() {
            return getDataFactory().getOWLObjectIntersectionOf(factoryObjects());
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.ObjectOneOfImpl
     * @see OntClass.OneOf
     */
    public static class OF
            extends WithMembers<OntIndividual, OntClass.OneOf, OWLIndividual, OWLObjectOneOf>
            implements OWLObjectOneOf {

        public OF(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        protected ONTObject<? extends OWLIndividual> map(OntIndividual member, ONTObjectFactory factory) {
            return factory.getIndividual(member);
        }

        @Override
        protected Object toContentItem(ONTObject<? extends OWLIndividual> individual) {
            if (!individual.getOWLObject().isOWLNamedIndividual()) {
                return ((AnonymousIndividualImpl) individual).getBlankNodeId();
            }
            return individual.getOWLObject().asOWLNamedIndividual().toStringID();
        }

        @Override
        protected ONTObject<? extends OWLIndividual> fromContentItem(Object item, ModelObjectFactory factory) {
            return toIndividual(item, factory);
        }

        @Override
        public OntClass.OneOf asRDFNode() {
            return as(OntClass.OneOf.class);
        }

        @Override
        public Stream<OWLIndividual> individuals() {
            return operands();
        }

        @FactoryAccessor
        @Override
        public OWLClassExpression asObjectUnionOf() {
            Collection<OWLIndividual> values = factoryObjects().collect(Collectors.toSet());
            DataFactory df = getDataFactory();
            if (values.size() == 1) {
                return df.getOWLObjectOneOf(values);
            }
            return df.getOWLObjectUnionOf(values.stream().map(df::getOWLObjectOneOf));
        }

        @Override
        public OWLObjectOneOf eraseModel() {
            return getDataFactory().getOWLObjectOneOf(factoryObjects());
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
     * @see com.github.owlcs.ontapi.owlapi.objects.ce.ObjectComplementOfImpl
     * @see OntClass.ComplementOf
     */
    public static class CF
            extends Simple<OntClass.ComplementOf, OWLObjectComplementOf>
            implements OWLObjectComplementOf {

        public CF(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        public boolean isClassExpressionLiteral() {
            return getONTClassExpression().getOWLObject().isOWLClass();
        }

        @Override
        public OntClass.ComplementOf asRDFNode() {
            return as(OntClass.ComplementOf.class);
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

        protected ONTObject<? extends OWLClassExpression> findCE(ModelObjectFactory factory) {
            return toCE(getContent()[0], factory);
        }

        @Override
        protected Object[] collectContent(OntClass.ComplementOf ce, ONTObjectFactory factory) {
            return new Object[]{toContentItem(ce.getValue(), factory)};
        }

        @Override
        protected Object[] initContent(OntClass.ComplementOf ce, ONTObjectFactory factory) {
            OntClass c = ce.getValue();
            return initContent(c.asNode(), factory.getClass(c));
        }

        @Override
        public OWLObjectComplementOf eraseModel() {
            return getDataFactory().getOWLObjectComplementOf(eraseModel(getOperand()));
        }
    }

    protected abstract static class WithClassMembers<ONT extends OntClass.ComponentsCE<OntClass>,
            OWL extends OWLAnonymousClassExpression>
            extends WithMembers<OntClass, ONT, OWLClassExpression, OWL> {

        protected WithClassMembers(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        protected Object toContentItem(ONTObject<? extends OWLClassExpression> ce) {
            OWLClassExpression res = ce.getOWLObject();
            return res.isOWLClass() ? res.asOWLClass().toStringID() : ce;
        }

        @Override
        protected ONTObject<? extends OWLClassExpression> fromContentItem(Object item, ModelObjectFactory factory) {
            return toCE(item, factory);
        }

        @Override
        protected ONTObject<? extends OWLClassExpression> map(OntClass member, ONTObjectFactory factory) {
            return factory.getClass(member);
        }
    }

    /**
     * Describes a class expression that is based on []-list.
     *
     * @param <ONT_M> subtype of {@link OntObject} - member of list
     * @param <ONT_C> subtype of {@link OntClass.ComponentsCE} - the class expression
     * @param <OWL_M> subtype of {@link OWLObject} that matches {@link ONT_M}
     * @param <OWL_C> subtype of {@link OWLAnonymousClassExpression} that matches {@link ONT_C}
     */
    protected abstract static class WithMembers<ONT_M extends OntObject,
            ONT_C extends OntClass.ComponentsCE<ONT_M>,
            OWL_M extends OWLObject,
            OWL_C extends OWLAnonymousClassExpression>
            extends ONTAnonymousClassExpressionImpl<ONT_C, OWL_C>
            implements HasOperands<OWL_M>, HasComponents {

        protected WithMembers(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        /**
         * Translates Jena {@link ONT_M} resource to OWL-API {@link OWL_M} equivalent.
         *
         * @param member  {@link ONT_M}, not {@code null}
         * @param factory {@link ONTObjectFactory}, not {@code null}
         * @return an {@link ONTObject} that wraps {@link OWL_M}
         */
        protected abstract ONTObject<? extends OWL_M> map(ONT_M member, ONTObjectFactory factory);

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
         * @param factory {@link ONTObjectFactory}, not {@code null}
         * @return an {@link ONTObject} that wraps {@link OWL_M}
         */
        protected abstract ONTObject<? extends OWL_M> fromContentItem(Object item, ModelObjectFactory factory);

        @Override
        public Stream<OWL_M> operands() {
            return members().map(ONTObject::getOWLObject);
        }

        @FactoryAccessor
        protected Stream<OWL_M> factoryObjects() {
            return operands().map(ONTObjectImpl::eraseModel);
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

        protected Stream<?> objects(ModelObjectFactory factory) {
            return Arrays.stream(getContent()).map(x -> fromContentItem(x, factory));
        }

        @Override
        protected Object[] collectContent(ONT_C ce, ONTObjectFactory factory) {
            Set<ONTObject<? extends OWL_M>> operands = operands(ce, factory);
            Object[] res = new Object[operands.size()];
            int index = 0;
            for (ONTObject<? extends OWL_M> op : operands) {
                res[index++] = toContentItem(op);
            }
            return res;
        }

        @Override
        protected Object[] initContent(ONT_C ce, ONTObjectFactory factory) {
            Set<ONTObject<? extends OWL_M>> operands = operands(ce, factory);
            Object[] res = new Object[operands.size()];
            int index = 0;
            int hash = 1;
            for (ONTObject<? extends OWL_M> op : operands) {
                res[index++] = toContentItem(op);
                hash = WithContent.hashIteration(hash, op.hashCode());
            }
            this.hashCode = OWLObject.hashIteration(hashIndex(), hash);
            return res;
        }

        protected Set<ONTObject<? extends OWL_M>> operands(ONT_C ce, ONTObjectFactory factory) {
            Set<ONTObject<? extends OWL_M>> res = createContentSet();
            OntModels.listMembers(ce.getList()).forEachRemaining(e -> res.add(map(e, factory)));
            return res;
        }
    }

    /**
     * Represents a data property restriction (class expression) with cardinality.
     *
     * @param <ONT> - a subtype of {@link OntClass.CardinalityRestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithDataRangeAndDataPropertyAndCardinality<ONT extends OntClass.CardinalityRestrictionCE<OntDataRange, OntDataProperty>,
            OWL extends OWLRestriction>
            extends WithDataRangeAndDataPropertyUnary<ONT, OWL> {

        protected WithDataRangeAndDataPropertyAndCardinality(BlankNodeId n, Supplier<OntModel> m) {
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
        protected Object[] collectContent(ONT ce, ONTObjectFactory factory) {
            // [property, cardinality, filler]
            return new Object[]{toContentItem(ce.getProperty()),
                    ce.getCardinality(), toContentItem(ce.getValue(), factory)};
        }

        @Override
        protected Object[] initContent(ONT ce, ONTObjectFactory factory) {
            OntDataProperty p = ce.getProperty();
            OntDataRange v = ce.getValue();
            int c = ce.getCardinality();
            return initContent(p.asNode(), v.asNode(), factory.getProperty(p), c, factory.getDatatype(v));
        }
    }

    /**
     * Represents an n-ary universal or existential data property restriction.
     *
     * @param <ONT> - a subtype of {@link OntClass.NaryRestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithDataRangeAndDataPropertyNary<ONT extends OntClass.NaryRestrictionCE<OntDataRange, OntDataProperty>,
            OWL extends OWLRestriction>
            extends WithDataRangeAndDataProperty<ONT, OWL> {

        protected WithDataRangeAndDataPropertyNary(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        protected OntDataRange getValue(ONT ce) {
            return ce.getValue();
        }
    }

    /**
     * Represents a unary data property restriction that has a reference to a data range.
     *
     * @param <ONT> - a subtype of {@link OntClass.ComponentRestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithDataRangeAndDataPropertyUnary<ONT extends OntClass.ComponentRestrictionCE<OntDataRange, OntDataProperty>,
            OWL extends OWLRestriction>
            extends WithDataRangeAndDataProperty<ONT, OWL> {

        protected WithDataRangeAndDataPropertyUnary(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        @Override
        protected OntDataRange getValue(ONT ce) {
            return ce.getValue();
        }
    }

    /**
     * Represents a data property restriction that has a reference to a data range.
     *
     * @param <ONT> - a subtype of {@link OntClass.RestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithDataRangeAndDataProperty<ONT extends OntClass.RestrictionCE<OntDataProperty>,
            OWL extends OWLRestriction>
            extends WithDataProperty<ONT, OWL> {

        protected WithDataRangeAndDataProperty(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        public OWLDataRange getFiller() {
            return getONTDataRange().getOWLObject();
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            ModelObjectFactory factory = getObjectFactory();
            return Stream.of(findNDP(factory), findDR(factory));
        }

        public ONTObject<? extends OWLDataRange> getONTDataRange() {
            return findDR(getObjectFactory());
        }

        protected ONTObject<? extends OWLDataRange> findDR(ModelObjectFactory factory) {
            // [property, cardinality, filler] or [property, filler] - always last:
            Object[] array = getContent();
            return toDR(array[array.length - 1], factory);
        }

        @Override
        protected Object[] collectContent(ONT ce, ONTObjectFactory factory) {
            // [property, filler]
            return new Object[]{toContentItem(ce.getProperty()), toContentItem(getValue(ce), factory)};
        }

        protected abstract OntDataRange getValue(ONT ce);

        @Override
        protected Object[] initContent(ONT ce, ONTObjectFactory factory) {
            OntDataProperty p = ce.getProperty();
            OntDataRange v = getValue(ce);
            return initContent(p.asNode(), v.asNode(), factory.getProperty(p), factory.getDatatype(v));
        }

        protected abstract OWL fromFactory(OWLDataProperty p, OWLDataRange d);

        @Override
        public OWL eraseModel() {
            return fromFactory(eraseModel(getProperty()), eraseModel(getFiller()));
        }
    }

    /**
     * An abstraction for simplification code,
     * that describes {@code owl:Restriction} containing datatype property.
     * Its signature also contains {@link OWLDatatype datatype} (explicitly or implicitly).
     * It cannot contain nested class expressions.
     *
     * @param <ONT> - a subtype of {@link OntClass.RestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithDataProperty<ONT extends OntClass.RestrictionCE<OntDataProperty>,
            OWL extends OWLRestriction>
            extends Restriction<ONT, OWL> {

        protected WithDataProperty(BlankNodeId n, Supplier<OntModel> m) {
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

        protected ONTObject<OWLDataProperty> findNDP(ModelObjectFactory factory) {
            // [property, cardinality, filler] or [property, filler] or [property] - always first
            return toNDP(getContent()[0], factory);
        }

        @Override
        protected Object[] collectContent(ONT ce, ONTObjectFactory factory) {
            // [property]
            return new Object[]{toContentItem(ce.getProperty())};
        }

        @Override
        protected Object[] initContent(ONT ce, ONTObjectFactory factory) {
            OntDataProperty p = ce.getProperty();
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
     * @param <ONT> - a subtype of {@link OntClass.CardinalityRestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithClassAndObjectPropertyAndCardinality<ONT extends OntClass.CardinalityRestrictionCE<OntClass, OntObjectProperty>,
            OWL extends OWLRestriction>
            extends WithClassAndObjectProperty<ONT, OWL> {
        protected WithClassAndObjectPropertyAndCardinality(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        public int getCardinality() {
            // [property, cardinality, filler]
            return (int) getContent()[1];
        }

        @Override
        protected Object[] collectContent(ONT ce, ONTObjectFactory factory) {
            // [property, cardinality, filler]
            return new Object[]{toContentItem(ce.getProperty(), factory),
                    ce.getCardinality(), toContentItem(ce.getValue(), factory)};
        }

        @Override
        protected Object[] initContent(ONT ce, ONTObjectFactory factory) {
            OntObjectProperty p = ce.getProperty();
            OntClass v = ce.getValue();
            int c = ce.getCardinality();
            return initContent(p.asNode(), v.asNode(), factory.getProperty(p), c, factory.getClass(v));
        }
    }

    /**
     * Represents an object class expression,
     * that has reference to another class expression and object property expression.
     *
     * @param <ONT> - a subtype of {@link OntClass.ComponentRestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithClassAndObjectProperty<ONT extends OntClass.ComponentRestrictionCE<OntClass, OntObjectProperty>,
            OWL extends OWLRestriction>
            extends WithObjectProperty<ONT, OWL> {

        protected WithClassAndObjectProperty(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        public OWLClassExpression getFiller() {
            return getONTClassExpression().getOWLObject();
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            ModelObjectFactory factory = getObjectFactory();
            return Stream.of(findOPE(factory), findCE(factory));
        }

        public ONTObject<? extends OWLClassExpression> getONTClassExpression() {
            return findCE(getObjectFactory());
        }

        protected ONTObject<? extends OWLClassExpression> findCE(ModelObjectFactory factory) {
            // [property, cardinality, filler] or [property, filler] -- always last:
            Object[] array = getContent();
            return toCE(array[array.length - 1], factory);
        }

        @Override
        protected Object[] collectContent(ONT ce, ONTObjectFactory factory) {
            // [property, filler]
            return new Object[]{toContentItem(ce.getProperty(), factory), toContentItem(ce.getValue(), factory)};
        }

        @Override
        protected Object[] initContent(ONT ce, ONTObjectFactory factory) {
            OntObjectProperty p = ce.getProperty();
            OntClass v = ce.getValue();
            return initContent(p.asNode(), v.asNode(), factory.getProperty(p), factory.getClass(v));
        }

        protected abstract OWL fromFactory(OWLObjectPropertyExpression p, OWLClassExpression c);

        @Override
        public OWL eraseModel() {
            return fromFactory(eraseModel(getProperty()), eraseModel(getFiller()));
        }
    }

    /**
     * Represents a class expression with a reference to an object property expression.
     *
     * @param <ONT> - a subtype of {@link OntClass.RestrictionCE}
     * @param <OWL> - a subtype of {@link OWLRestriction} that matches {@link ONT}
     */
    protected abstract static class WithObjectProperty<ONT extends OntClass.RestrictionCE<OntObjectProperty>,
            OWL extends OWLRestriction>
            extends Restriction<ONT, OWL> {

        protected WithObjectProperty(BlankNodeId n, Supplier<OntModel> m) {
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

        protected ONTObject<? extends OWLObjectPropertyExpression> findOPE(ModelObjectFactory factory) {
            // [property, cardinality, filler] or [property, filler] or [property] - always first:
            return toOPE(getContent()[0], factory);
        }

        @Override
        protected Object[] collectContent(ONT ce, ONTObjectFactory factory) {
            // [property]
            return new Object[]{toContentItem(ce.getProperty(), factory)};
        }

        @Override
        protected Object[] initContent(ONT ce, ONTObjectFactory factory) {
            OntObjectProperty p = ce.getProperty();
            return initContent(p.asNode(), factory.getProperty(p));
        }
    }

    /**
     * A base for all {@code owl:Restriction} class expressions.
     * Contains helpers to calculate hashcode and to collect content cache.
     *
     * @param <ONT> - subtype of {@link OntClass}
     * @param <OWL> - subtype of {@link OWLAnonymousClassExpression}
     */
    protected abstract static class Restriction<ONT extends OntClass, OWL extends OWLAnonymousClassExpression>
            extends Simple<ONT, OWL> {

        protected Restriction(BlankNodeId n, Supplier<OntModel> m) {
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

        Object toLastContentItem(Node node, Object expr) {
            return node.isURI() ? node.getURI() : expr;
        }
    }

    /**
     * A base for all {@code owl:Restriction} class expressions and for {@code ObjectComplementOf} expression.
     *
     * @param <ONT> - subtype of {@link OntClass}
     * @param <OWL> - subtype of {@link OWLAnonymousClassExpression}
     */
    protected abstract static class Simple<ONT extends OntClass, OWL extends OWLAnonymousClassExpression>
            extends ONTAnonymousClassExpressionImpl<ONT, OWL> {

        protected Simple(BlankNodeId n, Supplier<OntModel> m) {
            super(n, m);
        }

        Object[] initContent(Node p, Object first) {
            this.hashCode = OWLObject.hashIteration(hashIndex(), first.hashCode());
            // [property|single value]
            return new Object[]{toFirstContentItem(p, first)};
        }

        Object toFirstContentItem(Node node, Object expr) {
            return node.isURI() ? node.getURI() : expr;
        }
    }
}
