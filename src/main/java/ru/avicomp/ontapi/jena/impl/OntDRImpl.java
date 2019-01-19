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
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.ObjectFactory;
import ru.avicomp.ontapi.jena.impl.conf.OntFilter;
import ru.avicomp.ontapi.jena.impl.conf.OntFinder;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

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

    public static ObjectFactory abstractDRFactory = Factories.createFrom(DR_FINDER
            , OntDT.class
            , OneOf.class
            , Restriction.class
            , ComplementOf.class
            , UnionOf.class
            , IntersectionOf.class);

    public OntDRImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public Optional<OntStatement> findRootStatement() {
        return getRequiredRootStatement(this, RDFS.Datatype);
    }

    private static Resource create(OntGraphModelImpl model) {
        Resource res = model.createResource();
        model.add(res, RDF.type, RDFS.Datatype);
        return res;
    }

    public static OneOf createOneOf(OntGraphModelImpl model, Stream<Literal> values) {
        OntJenaException.notNull(values, "Null values stream.");
        Resource res = create(model);
        model.add(res, OWL.oneOf, model.createList(values.iterator()));
        return model.getNodeAs(res.asNode(), OneOf.class);
    }

    public static Restriction createRestriction(OntGraphModelImpl model, OntDT dataType, Stream<OntFR> values) {
        OntJenaException.notNull(dataType, "Null data-type.");
        OntJenaException.notNull(values, "Null values stream.");
        Resource res = create(model);
        model.add(res, OWL.onDatatype, dataType);
        model.add(res, OWL.withRestrictions, model.createList(values.iterator()));
        return model.getNodeAs(res.asNode(), Restriction.class);
    }

    public static ComplementOf createComplementOf(OntGraphModelImpl model, OntDR other) {
        OntJenaException.notNull(other, "Null data range.");
        Resource res = create(model);
        model.add(res, OWL.datatypeComplementOf, other);
        return model.getNodeAs(res.asNode(), ComplementOf.class);
    }

    public static UnionOf createUnionOf(OntGraphModelImpl model, Stream<OntDR> values) {
        OntJenaException.notNull(values, "Null values stream.");
        Resource res = create(model);
        model.add(res, OWL.unionOf, model.createList(values.iterator()));
        return model.getNodeAs(res.asNode(), UnionOf.class);
    }

    public static IntersectionOf createIntersectionOf(OntGraphModelImpl model, Stream<OntDR> values) {
        OntJenaException.notNull(values, "Null values stream.");
        Resource res = create(model);
        model.add(res, OWL.intersectionOf, model.createList(values.iterator()));
        return model.getNodeAs(res.asNode(), IntersectionOf.class);
    }


    public static class ComplementOfImpl extends OntDRImpl implements ComplementOf {
        public ComplementOfImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public OntDR getDataRange() {
            return getRequiredObject(OWL.datatypeComplementOf, OntDR.class);
        }

        @Override
        public Stream<OntStatement> spec() {
            return Stream.concat(super.spec(), required(OWL.datatypeComplementOf));
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return ComplementOf.class;
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
        public OntDT getDatatype() {
            return getRequiredObject(OWL.onDatatype, OntDT.class);
        }

        @Override
        public Stream<OntStatement> spec() {
            return Stream.of(declaration(), required(OWL.onDatatype), withRestrictionsSpec()).flatMap(Function.identity());
        }

        public Stream<OntStatement> withRestrictionsSpec() {
            return getList().content().flatMap(s -> {
                if (!s.getObject().canAs(OntFR.class)) {
                    return Stream.of(s);
                }
                return Stream.of(s, s.getObject().as(OntFR.class).getRoot());
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
        public Stream<OntStatement> spec() {
            return Stream.concat(declaration(), getList().content());
        }

        public Stream<OntStatement> declaration() {
            return super.spec();
        }

        @Override
        public OntList<N> getList() {
            return OntListImpl.asSafeOntList(getRequiredObject(predicate, RDFList.class), getModel(), this, predicate, null, type);
        }
    }
}
