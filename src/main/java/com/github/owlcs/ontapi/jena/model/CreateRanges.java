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
 * A technical interface to generate {@link OntDataRange Data Range Expression}s.
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
     * @return {@link OntDataRange.OneOf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Enumeration_of_Literals'>7.4 Enumeration of Literals</a>
     */
    OntDataRange.OneOf createOneOfDataRange(Collection<Literal> values);

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
     * @param other  {@link OntDataRange.Named}, not {@code null}
     * @param values {@code Collection} of {@link OntFacetRestriction facet restriction}s, without {@code null}s
     * @return {@link OntDataRange.Restriction}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Datatype_Restrictions'>7.5 Datatype Restrictions</a>
     * @see OntFacetRestriction
     * @see OntModel#createFacetRestriction(Class, Literal)
     */
    OntDataRange.Restriction createRestrictionDataRange(OntDataRange.Named other, Collection<OntFacetRestriction> values);

    /**
     * Creates a Complement of Data Ranges.
     * RDF (turtle) syntax:
     * <pre>{@code
     * _:x rdf:type rdfs:Datatype .
     * _:x owl:datatypeComplementOf D .
     * }</pre>
     *
     * @param other {@link OntDataRange}, not {@code null}
     * @return {@link OntDataRange.ComplementOf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Complement_of_Data_Ranges'>7.3 Complement of Data Ranges</a>
     */
    OntDataRange.ComplementOf createComplementOfDataRange(OntDataRange other);

    /**
     * Creates an Union of Data Ranges.
     * RDF (turtle) syntax:
     * <pre>{@code
     * _:x rdf:type rdfs:Datatype .
     * _:x owl:unionOf ( D1 ... Dn ) .
     * }</pre>
     *
     * @param values {@code Collection} of {@link OntDataRange data range}s, without {@code null}s
     * @return {@link OntDataRange.UnionOf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Union_of_Data_Ranges'>7.2 Union of Data Ranges</a>
     */
    OntDataRange.UnionOf createUnionOfDataRange(Collection<OntDataRange> values);

    /**
     * Creates an Intersection of Data Ranges.
     * RDF (turtle) syntax:
     * <pre>{@code
     * _:x rdf:type rdfs:Datatype .
     * _:x owl:intersectionOf ( D1 ... Dn ) .
     * }</pre>
     *
     * @param values {@code Collection} of {@link OntDataRange data range}s, without {@code null}s
     * @return {@link OntDataRange.IntersectionOf}
     * @see <a href='https://www.w3.org/TR/owl-syntax/#Intersection_of_Data_Ranges'>7.1 Intersection of Data Ranges</a>
     */
    OntDataRange.IntersectionOf createIntersectionOfDataRange(Collection<OntDataRange> values);

    /**
     * Creates an Enumeration of Literals.
     *
     * @param values Array of {@link Literal literal}s, without {@code null}-elements
     * @return {@link OntDataRange.OneOf}
     * @see #createOneOfDataRange(Collection)
     * @since 1.4.0
     */
    default OntDataRange.OneOf createOneOfDataRange(Literal... values) {
        return createOneOfDataRange(Arrays.asList(values));
    }

    /**
     * Creates a Datatype Restriction.
     *
     * @param other  {@link OntDataRange.Named Named Data Range}, not {@code null}
     * @param values Array of {@link OntFacetRestriction facet restriction}s, without {@code null}s
     * @return {@link OntDataRange.Restriction}
     * @see #createRestrictionDataRange(OntDataRange.Named, Collection)
     * @since 1.4.0
     */
    default OntDataRange.Restriction createRestrictionDataRange(OntDataRange.Named other, OntFacetRestriction... values) {
        return createRestrictionDataRange(other, Arrays.asList(values));
    }

    /**
     * Creates an Union of Data Ranges.
     *
     * @param values {@code Collection} of {@link OntDataRange data range}s, without {@code null}-elements
     * @return {@link OntDataRange.UnionOf}
     * @see #createUnionOfDataRange(Collection)
     * @since 1.4.0
     */
    default OntDataRange.UnionOf createUnionOfDataRange(OntDataRange... values) {
        return createUnionOfDataRange(Arrays.asList(values));
    }

    /**
     * Creates an Intersection of Data Ranges.
     *
     * @param values Array of {@link OntDataRange data range}s, without {@code null}-elements
     * @return {@link OntDataRange.IntersectionOf}
     * @see #createIntersectionOfDataRange(Collection)
     * @since 1.4.0
     */
    default OntDataRange.IntersectionOf createIntersectionOfDataRange(OntDataRange... values) {
        return createIntersectionOfDataRange(Arrays.asList(values));
    }
}
