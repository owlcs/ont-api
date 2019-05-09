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

package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Common interface for Ontology <b>D</b>ata <b>R</b>ange expressions (both named and anonymous).
 * Examples of rdf-patterns see <a href='https://www.w3.org/TR/owl2-quick-reference/'>here</a>.
 * <p>
 * Created by szuev on 01.11.2016.
 *
 * @see OntDT
 * @see <a href='https://www.w3.org/TR/owl2-quick-reference/'>2.4 Data Ranges</a>
 * @see <a href='https://www.w3.org/TR/owl2-syntax/#Data_Ranges'>7 Data Ranges</a>
 */
public interface OntDR extends OntObject {

    /**
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#Complement_of_Data_Ranges'>7.3 Complement of Data Ranges</a>
     * @see OntGraphModel#createComplementOfDataRange(OntDR)
     */
    interface ComplementOf extends OntDR {
        OntDR getDataRange();
    }

    /**
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#Intersection_of_Data_Ranges'>7.1 Intersection of Data Ranges</a>
     * @see OntGraphModel#createIntersectionOfDataRange(Collection)
     */
    interface IntersectionOf extends ComponentsDR<OntDR>, SetComponents<OntDR, IntersectionOf> {
        /**
         * Lists all data-ranges.
         *
         * @return a {@code Stream} of {@link OntDR}s
         * @deprecated since 1.4.0: use {@code getList().members()} instead
         */
        @Deprecated
        default Stream<OntDR> dataRanges() {
            return getList().members();
        }
    }

    /**
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#Union_of_Data_Ranges'>7.2 Union of Data Ranges</a>
     * @see OntGraphModel#createUnionOfDataRange(Collection)
     */
    interface UnionOf extends ComponentsDR<OntDR>, SetComponents<OntDR, UnionOf> {
        /**
         * Lists all data-ranges.
         *
         * @return a {@code Stream} of {@link OntDR}s
         * @deprecated since 1.4.0: use {@code getList().members()} instead
         */
        @Deprecated
        default Stream<OntDR> dataRanges() {
            return getList().members();
        }
    }

    /**
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#Enumeration_of_Literals'>7.4 Enumeration of Literals</a>
     * @see OntGraphModel#createOneOfDataRange(Collection)
     */
    interface OneOf extends ComponentsDR<Literal>, SetComponents<Literal, OneOf> {
        /**
         * Lists all literals.
         * @return a {@code Stream} of {@link Literal literal}s
         * @deprecated since 1.4.0: use {@code getList().members()} instead
         */
        @Deprecated
        default Stream<Literal> values() {
            return getList().members();
        }
    }

    /**
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#Datatype_Restrictions'>7.5 Datatype Restrictions</a>
     * @see OntFR
     * @see OntGraphModel#createFacetRestriction(Class, Literal)
     * @see OntGraphModel#createRestrictionDataRange(OntDT, Collection)
     */
    interface Restriction extends ComponentsDR<OntFR>, SetComponents<OntFR, Restriction> {
        /**
         * Returns the datatype from the right side of
         * the statement {@code _:x owl:onDatatype DN}, where {@code _:x} this Restriction.
         *
         * @return {@link OntDT}
         */
        OntDT getDatatype();

        /**
         * {@inheritDoc}
         * The result stream for {@link Restriction Restriction Data Range} also includes
         * {@link OntFR facet restrinction} definition triples.
         *
         * @return Stream of {@link OntStatement}s.
         */
        @Override
        Stream<OntStatement> spec();

        /**
         * Lists all facet restrictions.
         * @return a {@code Stream} of {@link OntFR}s
         * @deprecated since 1.4.0: use {@code getList().members()} instead
         */
        @Deprecated
        default Stream<OntFR> facetRestrictions() {
            return getList().members();
        }
    }

    /**
     * An abstract DataRange Expression with {@link OntList} support.
     *
     * @param <N> {@link RDFNode}
     */
    interface ComponentsDR<N extends RDFNode> extends OntDR, HasRDFNodeList<N> {
    }

}
