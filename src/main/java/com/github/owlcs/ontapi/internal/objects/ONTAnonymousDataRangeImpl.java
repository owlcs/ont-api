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

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.internal.ONTObjectFactory;
import com.github.owlcs.ontapi.jena.model.OntDataRange;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.owlapi.objects.OWLLiteralImpl;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An abstraction for any anonymous data range expressions
 * (i.e. for all data ranges with except of {@code OWLDatatype}s).
 * <p>
 * Created by @ssz on 20.08.2019.
 *
 * @see com.github.owlcs.ontapi.owlapi.objects.dr.OWLAnonymousDataRangeImpl
 * @see com.github.owlcs.ontapi.internal.ReadHelper#calcDataRange(OntDataRange, ONTObjectFactory, Set)
 * @see OntDataRange
 * @since 2.0.0
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTAnonymousDataRangeImpl<ONT extends OntDataRange, OWL extends OWLDataRange>
        extends ONTExpressionImpl<ONT>
        implements OWLDataRange, ModelObject<OWL> {

    protected ONTAnonymousDataRangeImpl(BlankNodeId id, Supplier<OntModel> m) {
        super(id, m);
    }

    /**
     * Wraps the given {@link OntDataRange} as {@link OWLDataRange} and {@link ONTObject}.
     *
     * @param dr      {@link OntDataRange}, not {@code null}, must be anonymous
     * @param factory {@link ONTObjectFactory}, not {@code null}
     * @param model   a provider of non-null {@link OntModel}, not {@code null}
     * @return {@link ONTAnonymousDataRangeImpl} instance
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ONTAnonymousDataRangeImpl create(OntDataRange dr,
                                                   ONTObjectFactory factory,
                                                   Supplier<OntModel> model) {
        Class<? extends OntDataRange> type = OntModels.getOntType(dr);
        BlankNodeId id = dr.asNode().getBlankNodeId();
        ONTAnonymousDataRangeImpl res = create(id, type, model);
        res.putContent(res.initContent(dr, factory));
        return res;
    }

    /**
     * Creates a data range expression with given b-node {@code id} and {@code type}.
     * An underlying graph must contain a valid data range structure for the specified {@code id}.
     *
     * @param id    {@link BlankNodeId}, not {@code null}
     * @param type  {@code Class}, not {@code null}
     * @param model a provider of non-null {@link OntModel}, not {@code null}
     * @return {@link ONTAnonymousDataRangeImpl} instance
     */
    public static ONTAnonymousDataRangeImpl<?, ?> create(BlankNodeId id,
                                                         Class<? extends OntDataRange> type,
                                                         Supplier<OntModel> model) {
        if (OntDataRange.UnionOf.class == type) {
            return new UF(id, model);
        }
        if (OntDataRange.IntersectionOf.class == type) {
            return new IF(id, model);
        }
        if (OntDataRange.OneOf.class == type) {
            return new OF(id, model);
        }
        if (OntDataRange.Restriction.class == type) {
            return new R(id, model);
        }
        if (OntDataRange.ComplementOf.class == type) {
            return new CF(id, model);
        }
        throw new OntApiException.IllegalState();
    }

    @SuppressWarnings("unchecked")
    @Override
    public OWL getOWLObject() {
        return (OWL) this;
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
    public boolean canContainDataProperties() {
        return false;
    }

    @Override
    public boolean canContainObjectProperties() {
        return false;
    }

    @Override
    public boolean canContainAnnotationProperties() {
        return false;
    }

    @Override
    public boolean canContainClassExpressions() {
        return false;
    }

    @Override
    public boolean canContainAnonymousIndividuals() {
        return false;
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.dr.OWLDataUnionOfImpl
     * @see OntDataRange.UnionOf
     */
    public static class UF
            extends WithDRMembers<OntDataRange.UnionOf, OWLDataUnionOf> implements OWLDataUnionOf {

        protected UF(BlankNodeId id, Supplier<OntModel> m) {
            super(id, m);
        }

        @Override
        public OntDataRange.UnionOf asRDFNode() {
            return as(OntDataRange.UnionOf.class);
        }

        @Override
        public OWLDataUnionOf eraseModel() {
            return getDataFactory().getOWLDataUnionOf(factoryObjects());
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.dr.OWLDataIntersectionOfImpl
     * @see OntDataRange.IntersectionOf
     */
    public static class IF
            extends WithDRMembers<OntDataRange.IntersectionOf, OWLDataIntersectionOf>
            implements OWLDataIntersectionOf {

        protected IF(BlankNodeId id, Supplier<OntModel> m) {
            super(id, m);
        }

        @Override
        public OntDataRange.IntersectionOf asRDFNode() {
            return as(OntDataRange.IntersectionOf.class);
        }

        @Override
        public OWLDataIntersectionOf eraseModel() {
            return getDataFactory().getOWLDataIntersectionOf(factoryObjects());
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.dr.OWLDataOneOfImpl
     * @see OntDataRange.OneOf
     */
    public static class OF
            extends WithMembers<Literal, OntDataRange.OneOf, OWLLiteral, OWLDataOneOf> implements OWLDataOneOf {

        protected OF(BlankNodeId id, Supplier<OntModel> m) {
            super(id, m);
        }

        @Override
        public OntDataRange.OneOf asRDFNode() {
            return as(OntDataRange.OneOf.class);
        }

        @Override
        protected ONTObject<? extends OWLLiteral> map(Literal literal, ONTObjectFactory factory) {
            return factory.getLiteral(literal);
        }

        @Override
        public Stream<OWLLiteral> values() {
            return operands();
        }

        @Override
        protected Object toContentItem(ONTObject<? extends OWLLiteral> literal) {
            return ((OWLLiteralImpl) literal).getLiteralLabel();
        }

        @Override
        protected ONTObject<? extends OWLLiteral> fromContentItem(Object item, ModelObjectFactory factory) {
            return toLiteral(item, factory);
        }

        @Override
        public OWLDataOneOf eraseModel() {
            return getDataFactory().getOWLDataOneOf(factoryObjects());
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.dr.OWLDatatypeRestrictionImpl
     * @see OntDataRange.Restriction
     */
    public static class R
            extends ONTAnonymousDataRangeImpl<OntDataRange.Restriction, OWLDatatypeRestriction>
            implements OWLDatatypeRestriction {

        protected R(BlankNodeId id, Supplier<OntModel> m) {
            super(id, m);
        }

        @Override
        public OntDataRange.Restriction asRDFNode() {
            return as(OntDataRange.Restriction.class);
        }

        @Override
        public OWLDatatype getDatatype() {
            return getONTDatatype().getOWLObject();
        }

        public ONTObject<OWLDatatype> getONTDatatype() {
            return findONTDatatype(getObjectFactory());
        }

        protected ONTObject<OWLDatatype> findONTDatatype(ModelObjectFactory factory) {
            return toDatatype(getContent()[0], factory);
        }

        private ONTObject<OWLDatatype> toDatatype(Object item, ModelObjectFactory factory) {
            return factory.getDatatype((String) item);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Stream<OWLFacetRestriction> facetRestrictions() {
            return Arrays.stream(getContent()).skip(1)
                    .map(x -> ((ONTObject<? extends OWLFacetRestriction>) x).getOWLObject());
        }

        @FactoryAccessor
        protected Stream<OWLFacetRestriction> factoryObjects() {
            return facetRestrictions().map(ONTObjectImpl::eraseModel);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            return (Stream<ONTObject<? extends OWLObject>>) objects(getObjectFactory());
        }

        protected Stream<?> objects(ModelObjectFactory factory) {
            return Stream.concat(Stream.of(toDatatype(getContent()[0], factory)), Arrays.stream(getContent()).skip(1));
        }

        @Override
        protected Object[] collectContent(OntDataRange.Restriction dr, ONTObjectFactory factory) {
            Set<ONTObject<OWLFacetRestriction>> members = facetRestrictions(dr, factory);
            Object[] res = new Object[members.size() + 1];
            OntDataRange.Named dt = dr.getValue();
            res[0] = dt.getURI();
            int index = 1;
            for (ONTObject<OWLFacetRestriction> op : members) {
                res[index++] = op;
            }
            return res;
        }

        @Override
        protected Object[] initContent(OntDataRange.Restriction dr, ONTObjectFactory factory) {
            Set<ONTObject<OWLFacetRestriction>> members = facetRestrictions(dr, factory);
            Object[] res = new Object[members.size() + 1];
            OntDataRange.Named dt = dr.getValue();
            res[0] = dt.getURI();
            int hash = OWLObject.hashIteration(hashIndex(), factory.getDatatype(dt).hashCode());
            int index = 1;
            int h = 1;
            for (ONTObject<OWLFacetRestriction> op : members) {
                res[index++] = op;
                h = WithContent.hashIteration(h, op.hashCode());
            }
            this.hashCode = OWLObject.hashIteration(hash, h);
            return res;
        }

        protected Set<ONTObject<OWLFacetRestriction>> facetRestrictions(OntDataRange.Restriction dr,
                                                                        ONTObjectFactory factory) {
            Set<ONTObject<OWLFacetRestriction>> res = createContentSet();
            OntModels.listMembers(dr.getList()).mapWith(factory::getFacetRestriction).forEachRemaining(res::add);
            return res;
        }

        @Override
        public OWLDatatypeRestriction eraseModel() {
            return getDataFactory().getOWLDatatypeRestriction(eraseModel(getDatatype()),
                    factoryObjects().collect(Collectors.toList()));
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.dr.OWLDataComplementOfImpl
     * @see OntDataRange.ComplementOf
     */
    public static class CF
            extends ONTAnonymousDataRangeImpl<OntDataRange.ComplementOf, OWLDataComplementOf> implements OWLDataComplementOf {

        public CF(BlankNodeId id, Supplier<OntModel> m) {
            super(id, m);
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            return Stream.of(getONTDataRange());
        }

        @Override
        public OntDataRange.ComplementOf asRDFNode() {
            return as(OntDataRange.ComplementOf.class);
        }

        @Override
        public OWLDataRange getDataRange() {
            return getONTDataRange().getOWLObject();
        }

        public ONTObject<? extends OWLDataRange> getONTDataRange() {
            return toDR(getContent()[0], getObjectFactory());
        }

        @Override
        protected Object[] collectContent(OntDataRange.ComplementOf dr, ONTObjectFactory factory) {
            return new Object[]{toContentItem(dr.getValue(), factory)};
        }

        @Override
        protected Object[] initContent(OntDataRange.ComplementOf dr, ONTObjectFactory factory) {
            OntDataRange value = dr.getValue();
            Object item = factory.getDatatype(value);
            this.hashCode = OWLObject.hashIteration(hashIndex(), item.hashCode());
            if (value.isURIResource()) {
                item = value.getURI();
            }
            return new Object[]{item};
        }

        @Override
        public OWLDataComplementOf eraseModel() {
            return getDataFactory().getOWLDataComplementOf(eraseModel(getDataRange()));
        }
    }

    protected abstract static class WithDRMembers<ONT extends OntDataRange.ComponentsDR<OntDataRange>, OWL extends OWLDataRange>
            extends WithMembers<OntDataRange, ONT, OWLDataRange, OWL> {

        protected WithDRMembers(BlankNodeId id, Supplier<OntModel> m) {
            super(id, m);
        }

        @Override
        protected ONTObject<? extends OWLDataRange> map(OntDataRange dr, ONTObjectFactory factory) {
            return factory.getDatatype(dr);
        }

        @Override
        protected Object toContentItem(ONTObject<? extends OWLDataRange> dr) {
            OWLDataRange res = dr.getOWLObject();
            return res.isOWLDatatype() ? res.asOWLDatatype().toStringID() : dr;
        }

        @Override
        protected ONTObject<? extends OWLDataRange> fromContentItem(Object item, ModelObjectFactory factory) {
            return toDR(item, factory);
        }
    }

    protected abstract static class WithMembers<ONT_M extends RDFNode,
            ONT_D extends OntDataRange.ComponentsDR<ONT_M>,
            OWL_M extends OWLObject,
            OWL_D extends OWLDataRange>
            extends ONTAnonymousDataRangeImpl<ONT_D, OWL_D>
            implements HasOperands<OWL_M>, HasComponents {

        protected WithMembers(BlankNodeId id, Supplier<OntModel> m) {
            super(id, m);
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

        @Override
        public List<OWL_M> getOperandsAsList() {
            return operands().collect(Collectors.toList());
        }

        @SuppressWarnings("unchecked")
        protected Stream<ONTObject<? extends OWL_M>> members() {
            return (Stream<ONTObject<? extends OWL_M>>) objects(getObjectFactory());
        }

        @FactoryAccessor
        protected Stream<OWL_M> factoryObjects() {
            return operands().map(ONTObjectImpl::eraseModel);
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
        protected Object[] collectContent(ONT_D dr, ONTObjectFactory factory) {
            Set<ONTObject<? extends OWL_M>> operands = operands(dr, factory);
            Object[] res = new Object[operands.size()];
            int index = 0;
            for (ONTObject<? extends OWL_M> op : operands) {
                res[index++] = toContentItem(op);
            }
            return res;
        }

        @Override
        protected Object[] initContent(ONT_D dr, ONTObjectFactory factory) {
            Set<ONTObject<? extends OWL_M>> operands = operands(dr, factory);
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

        protected Set<ONTObject<? extends OWL_M>> operands(ONT_D dr, ONTObjectFactory factory) {
            Set<ONTObject<? extends OWL_M>> res = createContentSet();
            OntModels.listMembers(dr.getList()).forEachRemaining(e -> res.add(map(e, factory)));
            return res;
        }
    }
}
