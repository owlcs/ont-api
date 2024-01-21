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

package com.github.sszuev.jena.ontapi.model;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

/**
 * Interface representing the Negative Property Assertion abstraction,
 * where predicate (property) is expected to be either ontology {@link OntDataProperty data property} ({@code R}) or
 * {@link OntObjectProperty object property exception} ({@code P}).
 * Assuming {@code _:x} is a blank node, {@code ai} is an individual and {@code v} is literal,
 * a Negative Object Property Assertion in Turtle syntax looks like this:
 * <pre>{@code
 * _:x rdf:type owl:NegativePropertyAssertion ;
 * _:x owl:sourceIndividual a1 ;
 * _:x owl:assertionProperty P ;
 * _:x owl:targetIndividual a2 .
 * }</pre>
 * In turn, a Negative Data Property Assertion looks like following:
 * <pre>{@code
 * _:x rdf:type owl:NegativePropertyAssertion ;
 * _:x owl:sourceIndividual a ;
 * _:x owl:assertionProperty R ;
 * _:x owl:targetValue v .
 * }</pre>
 *
 * <p>
 * Created by @ssz on 15.11.2016.
 *
 * @param <P> - either {@link OntObjectProperty object property expression} or {@link OntDataProperty data property}
 * @param <V> - either {@link OntIndividual} or {@link Literal}
 */
public interface OntNegativeAssertion<P extends OntRelationalProperty, V extends RDFNode> extends OntObject {

    /**
     * Returns the source individual.
     *
     * @return {@link OntIndividual}
     */
    OntIndividual getSource();

    /**
     * Returns the assertion property.
     *
     * @return either {@link OntObjectProperty} or {@link OntDataProperty}
     */
    P getProperty();

    /**
     * Returns the target node.
     *
     * @return either {@link OntIndividual} or {@link Literal}
     */
    V getTarget();

    /**
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#Negative_Object_Property_Assertions'>9.6.5 Negative Object Property Assertions</a>
     */
    interface WithObjectProperty extends OntNegativeAssertion<OntObjectProperty, OntIndividual> {
    }

    /**
     * @see <a href='https://www.w3.org/TR/owl2-syntax/#Negative_Data_Property_Assertions'>9.6.7 Negative Data Property Assertions</a>
     */
    interface WithDataProperty extends OntNegativeAssertion<OntDataProperty, Literal> {
    }
}
