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

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.XSD;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Named entity with rdf:type = rdfs:Datatype
 * <p>
 * Created by szuev on 03.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntDatatypeImpl extends OntObjectImpl implements OntDT {

    public OntDatatypeImpl(Node n, EnhGraph g) {
        super(OntObjectImpl.checkNamed(n), g);
    }

    @Override
    public Class<OntDT> getActualClass() {
        return OntDT.class;
    }

    @Override
    public boolean isBuiltIn() {
        return getModel().isBuiltIn(this);
    }

    @Override
    public Optional<OntStatement> findRootStatement() {
        return getOptionalRootStatement(this, RDFS.Datatype);
    }

    @Override
    public RDFDatatype toRDFDatatype() {
        RDFDatatype res = TypeMapper.getInstance().getTypeByName(getURI());
        return res == null ? new BaseDatatype(getURI()) : res;
    }

    /**
     * Gets all <b>local</b> built-in OWL Datatypes from the base graph of the specified model.
     * It seems, the faster way is to check all possible places.
     *
     * @param m {@link OntGraphModelImpl}, not {@code null}
     * @return unmodifiable {@code Set} of built-in {@link OntDT}s
     */
    public static Set<OntDT> getBuiltinDatatypes(OntGraphModelImpl m) {
        Set<OntDT> res = new HashSet<>();
        int max = m.getOntPersonality().getBuiltins().getDatatypes().size();
        BooleanSupplier exit = () -> res.size() >= max;
        Predicate<OntDT> test = d -> !exit.getAsBoolean() && d.isBuiltIn();

        // rdfs:range
        m.listLocalObjects(OntNDP.class, RDFS.range, OntDT.class)
                .filterKeep(test)
                .forEachRemaining(res::add);
        if (exit.getAsBoolean()) return Collections.unmodifiableSet(res);

        // Datatype Definitions (owl:equivalentClass)
        filterBuiltin(m, exit, m.listLocalSubjectAndObjects(OWL.equivalentClass, OntDR.class))
                .forEachRemaining(res::add);
        if (exit.getAsBoolean()) return Collections.unmodifiableSet(res);

        // UnionOf and IntersectionOf data range expressions (owl:unionOf,  owl:intersectionOf)
        filterBuiltin(m, exit, Iter.flatMap(Iter.of(OWL.unionOf, OWL.intersectionOf),
                p -> m.fromLocalList(OntDR.class, p, OntDR.class, false)))
                .forEachRemaining(res::add);
        if (exit.getAsBoolean()) return Collections.unmodifiableSet(res);

        // Literal enumeration (DataOneOf)
        filterBuiltin(m, exit, Iter.flatMap(m.listLocalOntObjects(OntDT.OneOf.class),
                x -> ((OntListImpl<Literal>) x.getList()).listMembers()).mapWith(m::getDatatype))
                .forEachRemaining(res::add);
        if (exit.getAsBoolean()) return Collections.unmodifiableSet(res);

        // DatatypeRestriction
        filterBuiltin(m, exit, Iter.flatMap(m.listLocalOntObjects(OntDT.Restriction.class),
                x -> ((OntListImpl<OntFR>) x.getList()).listMembers()).mapWith(x -> m.getDatatype(x.getValue())))
                .forEachRemaining(res::add);
        if (exit.getAsBoolean()) return Collections.unmodifiableSet(res);

        //  DataComplementOf (owl:datatypeComplementOf)
        m.listLocalObjects(OntDR.ComplementOf.class, OWL.datatypeComplementOf, OntDT.class)
                .filterKeep(test)
                .forEachRemaining(res::add);
        if (exit.getAsBoolean()) return Collections.unmodifiableSet(res);

        // DataSomeValuesFrom, DataAllValuesFrom (owl:someValuesFrom, owl:allValuesFrom)
        Iter.flatMap(Iter.of(OWL.someValuesFrom, OWL.allValuesFrom), p -> listDatatypeValuesFrom(m, p))
                .filterKeep(test)
                .forEachRemaining(res::add);
        if (exit.getAsBoolean()) return Collections.unmodifiableSet(res);

        boolean includeAll = !m.getOntPersonality().getBuiltins().matchOWLAPI();
        // Qualified restrictions
        m.listLocalStatements(null, OWL.onDataRange, null)
                .filterKeep(x -> !exit.getAsBoolean() && x.getSubject().canAs(OntCE.CardinalityRestrictionCE.class))
                .forEachRemaining(s -> {
                    if (includeAll) {
                        res.add(m.getDatatype(XSD.nonNegativeInteger));
                    }
                    OntDT dt = m.findNodeAs(s.getObject().asNode(), OntDT.class);
                    if (dt == null) return;
                    if (dt.isBuiltIn())
                        res.add(dt);
                });
        if (exit.getAsBoolean()) return Collections.unmodifiableSet(res);

        // Unqualified restrictions (rdfs:Literal)
        OntDT topDT = m.getRDFSLiteral();
        if (topDT != null  // maybe null if it is absent in OntPersonality.Builtins
                && !res.contains(topDT)) {
            Iter.findFirst(Iter.flatMap(Iter.of(OWL.maxCardinality, OWL.cardinality, OWL.minCardinality),
                    p -> m.listLocalStatements(null, p, null)).mapWith(OntStatement::getSubject)
                    .filterKeep(OntDatatypeImpl::isRestriction)).ifPresent(x -> {
                if (includeAll) {
                    res.add(m.getDatatype(XSD.nonNegativeInteger));
                }
                res.add(topDT);
            });
        }
        if (exit.getAsBoolean()) return Collections.unmodifiableSet(res);

        // HasSelf
        if (includeAll && Iter.findFirst(m.listLocalOntObjects(OntCE.HasSelf.class)).isPresent()) {
            res.add(m.getDatatype(XSD.xboolean));
        }
        if (exit.getAsBoolean()) return Collections.unmodifiableSet(res);

        // NegativeDataPropertyAssertion( R a v )
        m.listLocalStatements(null, OWL.targetValue, null)
                .filterKeep(x -> x.getObject().isLiteral() && x.getSubject().canAs(OntNPA.DataAssertion.class))
                .mapWith(x -> m.getDatatype(x.getLiteral()))
                .filterKeep(test)
                .forEachRemaining(res::add);
        if (exit.getAsBoolean()) return Collections.unmodifiableSet(res);

        //  owl:annotatedTarget
        m.listLocalStatements(null, OWL.annotatedTarget, null)
                .filterKeep(x -> x.getObject().isLiteral() && x.getSubject().canAs(OntAnnotation.class))
                .mapWith(x -> m.getDatatype(x.getLiteral()))
                .filterKeep(test)
                .forEachRemaining(res::add);
        if (exit.getAsBoolean()) return Collections.unmodifiableSet(res);

        // DataPropertyAssertion( R a v ) or AnnotationAssertion( A s t )
        m.listLocalStatements(null, null, null)
                .filterKeep(x -> x.getObject().isLiteral() && (x.isAnnotation() || x.isData()))
                .mapWith(x -> m.getDatatype(x.getLiteral()))
                .filterKeep(test)
                .forEachRemaining(res::add);

        return Collections.unmodifiableSet(res);
    }

    private static boolean isRestriction(OntObject s) {
        return s.canAs(OntCE.DataMinCardinality.class)
                || s.canAs(OntCE.DataMaxCardinality.class)
                || s.canAs(OntCE.DataCardinality.class);
    }

    private static ExtendedIterator<OntDT> listDatatypeValuesFrom(OntGraphModelImpl m, Property predicate) {
        Class<? extends OntCE.ComponentRestrictionCE> type;
        if (OWL.someValuesFrom.equals(predicate)) {
            type = OntCE.DataSomeValuesFrom.class;
        } else if (OWL.allValuesFrom.equals(predicate)) {
            type = OntCE.DataAllValuesFrom.class;
        } else {
            throw new IllegalArgumentException();
        }
        return m.listLocalObjects(type, predicate, OntDT.class);
    }

    private static ExtendedIterator<OntDT> filterBuiltin(OntGraphModelImpl m,
                                                         BooleanSupplier exit,
                                                         ExtendedIterator<? extends RDFNode> from) {

        return filterBuiltin(OntDT.class, m, exit, from);
    }

}
