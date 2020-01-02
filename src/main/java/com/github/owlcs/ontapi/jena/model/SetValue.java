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

package com.github.owlcs.ontapi.jena.model;

import org.apache.jena.rdf.model.RDFNode;

/**
 * A technical generic interface to provide a possibility to assign {@link RDFNode} value (so called filler)
 * into a class expression.
 * A value can be either {@link OntClass}, {@link OntDataRange}, {@link OntIndividual}
 * or {@link org.apache.jena.rdf.model.Literal}, depending on a concrete {@link OntClass} or {@link OntDataRange} type.
 * This interface is used to construct {@link OntClass class expression}s and {@link OntDataRange data range}s as a base.
 * <p>
 * Created by @ssz on 08.05.2019.
 *
 * @param <V> - any subtype of {@link RDFNode} ({@link OntClass}, {@link OntDataRange}, {@link OntIndividual}
 *            or {@link org.apache.jena.rdf.model.Literal}).
 * @param <R> - return type, a subtype of {@link OntClass} or {@link OntDataRange}
 * @see HasValue
 */
interface SetValue<V extends RDFNode, R extends OntObject> {
    /**
     * Sets the specified value (a filler in OWL-API terms)
     * into this {@link OntClass class} or {@link OntDataRange data range} expression.
     * <p>
     * A {@code value} can be {@code null} if this is a Cardinality Restriction
     * (the null-filler is considered as {@link com.github.owlcs.ontapi.jena.vocabulary.OWL#Thing owl:Thing}
     * for an object restriction and as {@link org.apache.jena.vocabulary.RDFS#Literal} for a data restriction).
     *
     * @param value {@link V}, possible {@code null} in case of Cardinality Restriction
     * @return <b>this</b> instance to allow cascading calls
     * @see HasValue#getValue()
     */
    R setValue(V value);
}
