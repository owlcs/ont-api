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
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.OntDR;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.OntModels;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An abstraction for any anonymous data range expressions
 * (i.e. for all data ranges with except of {@code OWLDatatype}s).
 * <p>
 * Created by @ssz on 20.08.2019.
 *
 * @see ru.avicomp.ontapi.owlapi.objects.dr.OWLAnonymousDataRangeImpl
 * @see ru.avicomp.ontapi.internal.ReadHelper#calcDataRange(OntDR, InternalObjectFactory, Set)
 * @see OntDR
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTAnonymousDataRangeImpl<ONT extends OntDR, OWL extends OWLDataRange>
        extends ONTExpressionImpl<ONT>
        implements OWLDataRange, ONTObject<OWL> {

    protected ONTAnonymousDataRangeImpl(BlankNodeId id, Supplier<OntGraphModel> m) {
        super(id, m);
    }

    /**
     * Wraps the given {@link OntDR} as {@link OWLDataRange} and {@link ONTObject}.
     *
     * @param dr    {@link OntDR}, not {@code null}, must be anonymous
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @param model a provider of non-null {@link OntGraphModel}, not {@code null}
     * @return {@link ONTAnonymousDataRangeImpl} instance
     */
    @SuppressWarnings("unchecked")
    public static ONTAnonymousDataRangeImpl create(OntDR dr,
                                                   InternalObjectFactory factory,
                                                   Supplier<OntGraphModel> model) {
        Class<? extends OntDR> type = OntModels.getOntType(dr);
        BlankNodeId id = dr.asNode().getBlankNodeId();
        ONTAnonymousDataRangeImpl res = create(id, type, model);
        res.putContent(res.collectContent(dr, factory));
        return res;
    }

    /**
     * Creates a data range expression with given b-node {@code id} and {@code type}.
     * An underlying graph must contain a valid data range structure for the specified {@code id}.
     *
     * @param id    {@link BlankNodeId}, not {@code null}
     * @param type  {@code Class}, not {@code null}
     * @param model a provider of non-null {@link OntGraphModel}, not {@code null}
     * @return {@link ONTAnonymousDataRangeImpl} instance
     */
    public static ONTAnonymousDataRangeImpl create(BlankNodeId id,
                                                   Class<? extends OntDR> type,
                                                   Supplier<OntGraphModel> model) {
        if (OntDR.UnionOf.class == type) {
            return new UF(id, model);
        }
        if (OntDR.IntersectionOf.class == type) {
            return new IF(id, model);
        }
        if (OntDR.OneOf.class == type) {
            return new OF(id, model);
        }
        if (OntDR.Restriction.class == type) {
            return new R(id, model);
        }
        if (OntDR.ComplementOf.class == type) {
            return new C(id, model);
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

    @SuppressWarnings("unchecked")
    @Override
    public Stream<ONTObject<? extends OWLObject>> objects() {
        List res = Arrays.asList(getContent());
        return (Stream<ONTObject<? extends OWLObject>>) res.stream();
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.dr.OWLDataUnionOfImpl
     * @see OntDR.UnionOf
     */
    public static class UF
            extends WithDRMembers<OntDR.UnionOf, OWLDataUnionOf> implements OWLDataUnionOf {

        protected UF(BlankNodeId id, Supplier<OntGraphModel> m) {
            super(id, m);
        }

        @Override
        public OntDR.UnionOf asRDFNode() {
            return as(OntDR.UnionOf.class);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.dr.OWLDataIntersectionOfImpl
     * @see OntDR.IntersectionOf
     */
    public static class IF
            extends WithDRMembers<OntDR.IntersectionOf, OWLDataIntersectionOf>
            implements OWLDataIntersectionOf {

        protected IF(BlankNodeId id, Supplier<OntGraphModel> m) {
            super(id, m);
        }

        @Override
        public OntDR.IntersectionOf asRDFNode() {
            return as(OntDR.IntersectionOf.class);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.dr.OWLDataOneOfImpl
     * @see OntDR.OneOf
     */
    public static class OF
            extends WithMembers<Literal, OntDR.OneOf, OWLLiteral, OWLDataOneOf> implements OWLDataOneOf {

        protected OF(BlankNodeId id, Supplier<OntGraphModel> m) {
            super(id, m);
        }

        @Override
        public OntDR.OneOf asRDFNode() {
            return as(OntDR.OneOf.class);
        }

        @Override
        protected ONTObject<? extends OWLLiteral> map(Literal literal, InternalObjectFactory of) {
            return of.getLiteral(literal);
        }

        @Override
        public Stream<OWLLiteral> values() {
            return operands();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.dr.OWLDatatypeRestrictionImpl
     * @see OntDR.Restriction
     */
    public static class R
            extends ONTAnonymousDataRangeImpl<OntDR.Restriction, OWLDatatypeRestriction>
            implements OWLDatatypeRestriction {

        protected R(BlankNodeId id, Supplier<OntGraphModel> m) {
            super(id, m);
        }

        @Override
        public OntDR.Restriction asRDFNode() {
            return as(OntDR.Restriction.class);
        }

        @Override
        protected Object[] collectContent(OntDR.Restriction dr, InternalObjectFactory of) {
            Set<ONTObject<OWLFacetRestriction>> members = createContentSet();
            OntModels.listMembers(dr.getList()).mapWith(of::getFacetRestriction).forEachRemaining(members::add);
            List<ONTObject<?>> res = new ArrayList<>(members.size() + 1);
            res.add(of.getDatatype(dr.getValue()));
            res.addAll(members);
            return res.toArray();
        }

        @SuppressWarnings("unchecked")
        public Iterator<OWLFacetRestriction> listOWLFacetRestrictions() {
            Iterator it = Arrays.asList(getContent()).iterator();
            it.next();
            return (Iterator<OWLFacetRestriction>) it;
        }

        @Override
        public OWLDatatype getDatatype() {
            return getONTDatatype().getOWLObject();
        }

        @SuppressWarnings("unchecked")
        public ONTObject<OWLDatatype> getONTDatatype() {
            return (ONTObject<OWLDatatype>) getContent()[0];
        }

        @Override
        public Stream<OWLFacetRestriction> facetRestrictions() {
            return Iter.asStream(listOWLFacetRestrictions(),
                    Spliterator.NONNULL | Spliterator.ORDERED | Spliterator.DISTINCT);
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.dr.OWLDataComplementOfImpl
     * @see OntDR.ComplementOf
     */
    public static class C
            extends ONTAnonymousDataRangeImpl<OntDR.ComplementOf, OWLDataComplementOf> implements OWLDataComplementOf {

        public C(BlankNodeId id, Supplier<OntGraphModel> m) {
            super(id, m);
        }

        @Override
        public OntDR.ComplementOf asRDFNode() {
            return as(OntDR.ComplementOf.class);
        }

        @Override
        public OWLDataRange getDataRange() {
            return getONTDataRange().getOWLObject();
        }

        @SuppressWarnings("unchecked")
        public ONTObject<OWLDataRange> getONTDataRange() {
            return (ONTObject<OWLDataRange>) getContent()[0];
        }

        @Override
        protected Object[] collectContent(OntDR.ComplementOf dr, InternalObjectFactory of) {
            return new Object[]{of.getDatatype(dr.getValue())};
        }
    }

    protected abstract static class WithDRMembers<ONT extends OntDR.ComponentsDR<OntDR>, OWL extends OWLDataRange>
            extends WithMembers<OntDR, ONT, OWLDataRange, OWL> {

        protected WithDRMembers(BlankNodeId id, Supplier<OntGraphModel> m) {
            super(id, m);
        }

        @Override
        protected ONTObject<? extends OWLDataRange> map(OntDR dr, InternalObjectFactory of) {
            return of.getDatatype(dr);
        }
    }

    protected abstract static class WithMembers<ONT_M extends RDFNode,
            ONT_D extends OntDR.ComponentsDR<ONT_M>,
            OWL_M extends OWLObject,
            OWL_D extends OWLDataRange>
            extends ONTAnonymousDataRangeImpl<ONT_D, OWL_D>
            implements HasOperands<OWL_M>, HasComponents {

        protected WithMembers(BlankNodeId id, Supplier<OntGraphModel> m) {
            super(id, m);
        }

        protected abstract ONTObject<? extends OWL_M> map(ONT_M i, InternalObjectFactory of);

        @Override
        public Stream<OWL_M> operands() {
            return getOperandsAsList().stream();
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<OWL_M> getOperandsAsList() {
            List res = getONTMembers();
            return (List<OWL_M>) res;
        }

        protected Iterator<ONT_M> listONTMembers(ONT_D dr) {
            return OntModels.listMembers(dr.getList());
        }

        @Override
        protected Object[] collectContent(ONT_D dr, InternalObjectFactory of) {
            Set<ONTObject<? extends OWL_M>> res = createContentSet();
            listONTMembers(dr).forEachRemaining(e -> res.add(map(e, of)));
            return res.toArray();
        }

        @SuppressWarnings("unchecked")
        public List<ONTObject<? extends OWL_M>> getONTMembers() {
            List res = Arrays.asList(getContent());
            return (List<ONTObject<? extends OWL_M>>) res;
        }
    }
}
