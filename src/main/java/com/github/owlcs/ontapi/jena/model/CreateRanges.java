/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.jena.model;

import org.apache.jena.rdf.model.Literal;

import java.util.Arrays;
import java.util.Collection;

/**
 * A technical interface to generate {@link OntDR Data Range Expression}s.
 * Created by @szz on 14.05.2019.
 *
 * @since 1.4.0
 */
interface CreateRanges {

    /**
     * Creates an Enumeration of Literals.
     * RDF (turtle) syntax:
     * <pre>{@code
     * _:x rdf:type rdfs:Datatype .
     * _:x owl:oneOf ( v1 ... vn ) .
     * }</pre>
     *
     * @param values {@code Collection} of {@link Literal literal}s, without {@code null}s
     * @return {@link OntDR.OneOf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Enumeration_of_Literals'>7.4 Enumeration of Literals</a>
     */
    OntDR.OneOf createOneOfDataRange(Collection<Literal> values);

    /**
     * Creates a Datatype Restriction.
     * RDF (turtle) syntax:
     * <pre>{@code
     * _:x rdf:type rdfs:Datatype .
     * _:x owl:onDatatype DN .
     * _:x owl:withRestrictions ( _:x1 ... _:xn ) .
     * _:xj fj vj .
     * }</pre>
     *
     * @param other  {@link OntDT}, not {@code null}
     * @param values {@code Collection} of {@link OntFR facet restriction}s, without {@code null}s
     * @return {@link OntDR.Restriction}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Datatype_Restrictions'>7.5 Datatype Restrictions</a>
     * @see OntFR
     * @see OntModel#createFacetRestriction(Class, Literal)
     */
    OntDR.Restriction createRestrictionDataRange(OntDT other, Collection<OntFR> values);

    /**
     * Creates a Complement of Data Ranges.
     * RDF (turtle) syntax:
     * <pre>{@code
     * _:x rdf:type rdfs:Datatype .
     * _:x owl:datatypeComplementOf D .
     * }</pre>
     *
     * @param other {@link OntDR}, not {@code null}
     * @return {@link OntDR.ComplementOf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Complement_of_Data_Ranges'>7.3 Complement of Data Ranges</a>
     */
    OntDR.ComplementOf createComplementOfDataRange(OntDR other);

    /**
     * Creates an Union of Data Ranges.
     * RDF (turtle) syntax:
     * <pre>{@code
     * _:x rdf:type rdfs:Datatype .
     * _:x owl:unionOf ( D1 ... Dn ) .
     * }</pre>
     *
     * @param values {@code Collection} of {@link OntDR data range}s, without {@code null}s
     * @return {@link OntDR.UnionOf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Union_of_Data_Ranges'>7.2 Union of Data Ranges</a>
     */
    OntDR.UnionOf createUnionOfDataRange(Collection<OntDR> values);

    /**
     * Creates an Intersection of Data Ranges.
     * RDF (turtle) syntax:
     * <pre>{@code
     * _:x rdf:type rdfs:Datatype .
     * _:x owl:intersectionOf ( D1 ... Dn ) .
     * }</pre>
     *
     * @param values {@code Collection} of {@link OntDR data range}s, without {@code null}s
     * @return {@link OntDR.IntersectionOf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Intersection_of_Data_Ranges'>7.1 Intersection of Data Ranges</a>
     */
    OntDR.IntersectionOf createIntersectionOfDataRange(Collection<OntDR> values);

    /**
     * Creates an Enumeration of Literals.
     *
     * @param values Array of {@link Literal literal}s, without {@code null}-elements
     * @return {@link OntDR.OneOf}
     * @see #createOneOfDataRange(Collection)
     * @since 1.4.0
     */
    default OntDR.OneOf createOneOfDataRange(Literal... values) {
        return createOneOfDataRange(Arrays.asList(values));
    }

    /**
     * Creates a Datatype Restriction.
     *
     * @param other  {@link OntDT Named Data Range}, not {@code null}
     * @param values Array of {@link OntFR facet restriction}s, without {@code null}s
     * @return {@link OntDR.Restriction}
     * @see #createRestrictionDataRange(OntDT, Collection)
     * @since 1.4.0
     */
    default OntDR.Restriction createRestrictionDataRange(OntDT other, OntFR... values) {
        return createRestrictionDataRange(other, Arrays.asList(values));
    }

    /**
     * Creates an Union of Data Ranges.
     *
     * @param values {@code Collection} of {@link OntDR data range}s, without {@code null}-elements
     * @return {@link OntDR.UnionOf}
     * @see #createUnionOfDataRange(Collection)
     * @since 1.4.0
     */
    default OntDR.UnionOf createUnionOfDataRange(OntDR... values) {
        return createUnionOfDataRange(Arrays.asList(values));
    }

    /**
     * Creates an Intersection of Data Ranges.
     *
     * @param values Array of {@link OntDR data range}s, without {@code null}-elements
     * @return {@link OntDR.IntersectionOf}
     * @see #createIntersectionOfDataRange(Collection)
     * @since 1.4.0
     */
    default OntDR.IntersectionOf createIntersectionOfDataRange(OntDR... values) {
        return createIntersectionOfDataRange(Arrays.asList(values));
    }
}
