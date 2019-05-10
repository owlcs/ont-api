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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.graph.Node;
import org.apache.jena.ontology.ConversionException;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.BaseFactoryImpl;
import ru.avicomp.ontapi.jena.impl.conf.ObjectFactory;
import ru.avicomp.ontapi.jena.impl.conf.OntFilter;
import ru.avicomp.ontapi.jena.impl.conf.OntFinder;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.avicomp.ontapi.jena.impl.WrappedFactoryImpl.of;

/**
 * Implementation for Data Range Expressions.
 * <p>
 * Created by @szuev on 16.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntDRImpl extends OntObjectImpl implements OntDR {

    private static final OntFinder DR_FINDER = new OntFinder.ByType(RDFS.Datatype);
    private static final OntFilter DR_FILTER = OntFilter.BLANK.and(new OntFilter.HasType(RDFS.Datatype));

    public static ObjectFactory oneOfDRFactory = Factories.createCommon(OneOfImpl.class,
            DR_FINDER, DR_FILTER.and(new OntFilter.HasPredicate(OWL.oneOf)));
    public static ObjectFactory restrictionDRFactory = Factories.createCommon(RestrictionImpl.class,
            DR_FINDER, DR_FILTER.and(new OntFilter.HasPredicate(OWL.onDatatype))
                    .and(new OntFilter.HasPredicate(OWL.withRestrictions)));
    public static ObjectFactory complementOfDRFactory = Factories.createCommon(ComplementOfImpl.class,
            DR_FINDER, DR_FILTER.and(new OntFilter.HasPredicate(OWL.datatypeComplementOf)));
    public static ObjectFactory unionOfDRFactory = Factories.createCommon(UnionOfImpl.class,
            DR_FINDER, DR_FILTER.and(new OntFilter.HasPredicate(OWL.unionOf)));
    public static ObjectFactory intersectionOfDRFactory = Factories.createCommon(IntersectionOfImpl.class,
            DR_FINDER, DR_FILTER.and(new OntFilter.HasPredicate(OWL.intersectionOf)));

    public static ObjectFactory abstractComponentsDRFactory = Factories.createFrom(DR_FINDER
            , OneOf.class
            , Restriction.class
            , UnionOf.class
            , IntersectionOf.class);

    public static ObjectFactory abstractDRFactory = DataRangeFactory.createFactory();

    public OntDRImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    private static Resource create(OntGraphModelImpl model) {
        return model.createResource().addProperty(RDF.type, RDFS.Datatype);
    }

    public static OneOf createOneOf(OntGraphModelImpl model, Stream<Literal> values) {
        OntJenaException.notNull(values, "Null values stream.");
        Resource res = create(model)
                .addProperty(OWL.oneOf, model.createList(values
                        .peek(f -> OntJenaException.notNull(f, "OntDR: null literal.")).iterator()));
        return model.getNodeAs(res.asNode(), OneOf.class);
    }

    public static Restriction createRestriction(OntGraphModelImpl model, OntDT dataType, Stream<OntFR> values) {
        OntJenaException.notNull(dataType, "Null data-type.");
        OntJenaException.notNull(values, "Null values stream.");
        Resource res = create(model)
                .addProperty(OWL.onDatatype, dataType)
                .addProperty(OWL.withRestrictions, model.createList(values
                        .peek(f -> OntJenaException.notNull(f, "OntDR: null faced restriction."))
                        .iterator()));
        return model.getNodeAs(res.asNode(), Restriction.class);
    }

    public static ComplementOf createComplementOf(OntGraphModelImpl model, OntDR other) {
        OntJenaException.notNull(other, "Null data range.");
        Resource res = create(model).addProperty(OWL.datatypeComplementOf, other);
        return model.getNodeAs(res.asNode(), ComplementOf.class);
    }

    public static UnionOf createUnionOf(OntGraphModelImpl model, Stream<OntDR> values) {
        OntJenaException.notNull(values, "Null values stream.");
        Resource res = create(model)
                .addProperty(OWL.unionOf, model.createList(values
                        .peek(f -> OntJenaException.notNull(f, "OntDR: null data range."))
                        .iterator()));
        return model.getNodeAs(res.asNode(), UnionOf.class);
    }

    public static IntersectionOf createIntersectionOf(OntGraphModelImpl model, Stream<OntDR> values) {
        OntJenaException.notNull(values, "Null values stream.");
        Resource res = create(model).addProperty(OWL.intersectionOf, model.createList(values
                .peek(f -> OntJenaException.notNull(f, "OntDR: null data range."))
                .iterator()));
        return model.getNodeAs(res.asNode(), IntersectionOf.class);
    }

    @Override
    public Optional<OntStatement> findRootStatement() {
        return getRequiredRootStatement(this, RDFS.Datatype);
    }

    public static class ComplementOfImpl extends OntDRImpl implements ComplementOf {
        public ComplementOfImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public OntDR getValue() {
            return getRequiredObject(OWL.datatypeComplementOf, OntDR.class);
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            return Iter.concat(super.listSpec(), listRequired(OWL.datatypeComplementOf));
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return ComplementOf.class;
        }

        @Override
        public ComplementOf setValue(OntDR value) {
            Objects.requireNonNull(value);
            removeAll(OWL.datatypeComplementOf).addProperty(OWL.datatypeComplementOf, value);
            return this;
        }
    }

    public static class OneOfImpl extends ComponentsDRImpl<Literal> implements OneOf {
        public OneOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL.oneOf, Literal.class);
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return OneOf.class;
        }
    }

    public static class RestrictionImpl extends ComponentsDRImpl<OntFR> implements Restriction {
        public RestrictionImpl(Node n, EnhGraph m) {
            super(n, m, OWL.withRestrictions, OntFR.class);
        }

        @Override
        public Class<Restriction> getActualClass() {
            return Restriction.class;
        }

        @Override
        public OntDT getValue() {
            return getRequiredObject(OWL.onDatatype, OntDT.class);
        }

        @Override
        public RestrictionImpl setValue(OntDT value) {
            Objects.requireNonNull(value);
            removeAll(OWL.onDatatype).addProperty(OWL.onDatatype, value);
            return this;
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            return Iter.concat(listDeclaration(), listRequired(OWL.onDatatype), withRestrictionsSpec());
        }

        public ExtendedIterator<OntStatement> withRestrictionsSpec() {
            return Iter.flatMap(getList().listContent(), s -> {
                if (!s.getObject().canAs(OntFR.class)) {
                    return Iter.of(s);
                }
                return Iter.of(s, s.getObject().as(OntFR.class).getRoot());
            });
        }

    }

    public static class UnionOfImpl extends ComponentsDRImpl<OntDR> implements UnionOf {
        public UnionOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL.unionOf, OntDR.class);
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return UnionOf.class;
        }
    }

    public static class IntersectionOfImpl extends ComponentsDRImpl<OntDR> implements IntersectionOf {

        public IntersectionOfImpl(Node n, EnhGraph m) {
            super(n, m, OWL.intersectionOf, OntDR.class);
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return IntersectionOf.class;
        }
    }

    /**
     * An abstract super-class for {@link OneOf}, {@link Restriction}, {@link UnionOf}, {@link IntersectionOf}.
     *
     * @param <N> {@link RDFNode}
     */
    protected static abstract class ComponentsDRImpl<N extends RDFNode> extends OntDRImpl implements ComponentsDR<N> {
        protected final Property predicate;
        protected final Class<N> type;

        protected ComponentsDRImpl(Node n, EnhGraph m, Property predicate, Class<N> type) {
            super(n, m);
            this.predicate = OntJenaException.notNull(predicate, "Null predicate.");
            this.type = OntJenaException.notNull(type, "Null view.");
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            return Iter.concat(listDeclaration(), getList().listContent());
        }

        public ExtendedIterator<OntStatement> listDeclaration() {
            return super.listSpec();
        }

        @Override
        public OntListImpl<N> getList() {
            return OntListImpl.asSafeOntList(getRequiredObject(predicate, RDFList.class), getModel(), this, predicate, null, type);
        }
    }

    /**
     * A factory to produce {@link OntDR}s.
     * <p>
     * Although it would be easy to produce this factory using {@link Factories#createFrom(OntFinder, Class[])},
     * this variant with explicit methods must be a little bit faster,
     * since there is a reduction of number of some possible repetition calls.
     * Also everything here is under control.
     * <p>
     * Created by @ssz on 02.02.2019.
     */
    public static class DataRangeFactory extends BaseFactoryImpl {
        private static final Node TYPE = RDF.Nodes.type;
        private static final Node ANY = Node.ANY;
        private static final Node DATATYPE = RDFS.Datatype.asNode();

        private final ObjectFactory named = of(OntDT.class);
        private final ObjectFactory oneOf = of(OneOf.class);
        private final ObjectFactory complementOf = of(ComplementOf.class);
        private final ObjectFactory unionOf = of(UnionOf.class);
        private final ObjectFactory intersectionOf = of(IntersectionOf.class);
        private final ObjectFactory restriction = of(Restriction.class);
        private final List<ObjectFactory> anonymous = Stream.of(oneOf
                , restriction
                , complementOf
                , unionOf
                , intersectionOf).collect(Collectors.toList());

        public static ObjectFactory createFactory() {
            return new DataRangeFactory();
        }

        @Override
        public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
            return eg.asGraph().find(ANY, TYPE, DATATYPE)
                    .mapWith(t -> t.getSubject().isURI() ?
                            safeWrap(t.getSubject(), eg, named) :
                            safeWrap(t.getSubject(), eg, anonymous))
                    .filterDrop(Objects::isNull);
        }

        @Override
        public EnhNode createInstance(Node node, EnhGraph eg) {
            if (node.isURI())
                return safeWrap(node, eg, named);
            if (!node.isBlank())
                return null;
            if (!eg.asGraph().contains(node, TYPE, DATATYPE))
                return null;
            return safeWrap(node, eg, anonymous);
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            if (node.isURI()) {
                return named.canWrap(node, eg);
            }
            if (!node.isBlank()) return false;
            if (!eg.asGraph().contains(node, TYPE, DATATYPE))
                return false;
            return canWrap(node, eg, anonymous);
        }

        @Override
        public EnhNode wrap(Node node, EnhGraph eg) {
            if (node.isURI())
                return named.wrap(node, eg);
            ConversionException ex = new ConversionException("Can't convert node " + node +
                    " to Data Range Expression.");
            if (!node.isBlank())
                throw ex;
            if (!eg.asGraph().contains(node, TYPE, DATATYPE))
                throw ex;
            return wrap(node, eg, ex, anonymous);
        }
    }
}
