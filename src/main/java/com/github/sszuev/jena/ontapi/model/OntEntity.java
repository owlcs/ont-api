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

import com.github.sszuev.jena.ontapi.OntVocabulary;
import com.github.sszuev.jena.ontapi.impl.conf.OntPersonality;
import com.github.sszuev.jena.ontapi.utils.Iterators;
import com.github.sszuev.jena.ontapi.vocabulary.OWL;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.stream.Stream;

/**
 * The base interface for OWL entities, which are always URI-{@link org.apache.jena.rdf.model.Resource}.
 * In OWL2 there are <b>6</b> types of entities, see below.
 * <p>
 * Created @ssz on 01.11.2016.
 *
 * @see OntClass.Named
 * @see OntDataRange.Named
 * @see OntIndividual.Named
 * @see OntAnnotationProperty
 * @see OntObjectProperty.Named
 * @see OntDataProperty
 */
public interface OntEntity extends OntObject {

    /**
     * Returns all entity types as stream.
     *
     * @return a {@code Stream} of OWL-entity types
     */
    static Stream<Class<? extends OntEntity>> entityTypes() {
        return Iterators.asStream(listEntityTypes());
    }

    /**
     * Lists all OWL entity types.
     *
     * @return an {@link ExtendedIterator} of OWL entity {@code Class}-types
     */
    static ExtendedIterator<Class<? extends OntEntity>> listEntityTypes() {
        return Iterators.of(OntClass.Named.class,
                OntDataRange.Named.class,
                OntIndividual.Named.class,
                OntObjectProperty.Named.class,
                OntAnnotationProperty.class,
                OntDataProperty.class);
    }

    /**
     * Determines if this is a builtin entity.
     * In a standard (default) OWL vocabulary an entity is builtin if it is:
     * <ul>
     * <li>a {@link OntClass.Named class} and its IRI is either {@code owl:Thing} or {@code owl:Nothing}</li>
     * <li>an {@link OntObjectProperty.Named object property} and its IRI is either {@code owl:topObjectProperty}
     * or {@code owl:bottomObjectProperty}</li>
     * <li>a {@link OntDataProperty data property} and its IRI is either {@code owl:topDataProperty}
     * or {@code owl:bottomDataProperty}</li>
     * <li>a {@link OntDataRange.Named datatype} and its IRI is either {@code rdfs:Literal},
     * or {@code rdf:PlainLiteral},
     * or it is from the OWL 2 datatype map</li>
     * <li>an {@link OntAnnotationProperty annotation property} and its IRI is one of the following:
     * <ul>
     * <li>{@code rdfs:label}</li>
     * <li>{@code rdfs:comment}</li>
     * <li>{@code rdfs:seeAlso}</li>
     * <li>{@code rdfs:isDefinedBy}</li>
     * <li>{@code owl:deprecated}</li>
     * <li>{@code owl:versionInfo}</li>
     * <li>{@code owl:priorVersion}</li>
     * <li>{@code owl:backwardCompatibleWith}</li>
     * <li>{@code owl:incompatibleWith}</li>
     * </ul>
     * </li>
     * </ul>
     * Note: all the listed above IRIs refer
     * to the default {@link OntPersonality.Builtins Builtins Vocabulary}.
     * A model with different {@code Builtins} vocabulary will naturally have a different {@code Set} of builtin IRIs,
     * and this method will return a different result.
     *
     * @return {@code true} if it is a built-in entity
     * @see OWL
     * @see OntPersonality.Builtins
     * @see OntVocabulary
     */
    boolean isBuiltIn();

}
