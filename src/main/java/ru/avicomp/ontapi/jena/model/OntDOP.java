/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
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

import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.stream.Stream;

/**
 * A common (abstract) interface for any Ontology <b>D</b>ata and <b>O</b>bject <b>P</b>roperty expressions.
 * In OWL2 terms it is any {@link OntPE Property Expression} minus {@link OntNAP Annotation Property}.
 * <p>
 * Created by @szuev on 21.07.2018.
 */
public interface OntDOP extends OntPE {

    /**
     * Lists all property ranges,
     * i.e. all objects from statements with this property as subject and {@code rdfs:range} as predicate.
     *
     * @return Stream of {@link OntObject ontology object}s
     */
    Stream<? extends OntObject> range();

    /**
     * List all super properties for this property expression.
     * In other words, returns all objects from statements of the form {@code P rdfs:subPropertyOf R},
     * where {@code P} is this property.
     *
     * @return Stream of {@link OntObject ontology object}s
     */
    Stream<? extends OntObject> subPropertyOf();

    /**
     * Lists all properties which are disjoint with this property.
     * In other words, returns all objects from statements of the form {@code P owl:propertyDisjointWith R},
     * where {@code P} is this property.
     *
     * @return Stream of {@link OntObject ontology object}s
     */
    Stream<? extends OntObject> disjointWith();

    /**
     * Lists all properties that equivalent to this one.
     * In other words, returns all objects from statements of the form {@code P owl:equivalentProperty R},
     * where {@code P} is this property.
     *
     * @return Stream of {@link OntObject ontology object}s
     */
    Stream<? extends OntObject> equivalentProperty();

    /**
     * Lists all negative property assertions.
     * Negative property assertion is anonymous resource with the type {@link OWL#NegativePropertyAssertion owl:NegativePropertyAssertion}
     * that has this property expression as an object with predicate {@link OWL#assertionProperty owl:assertionProperty}.
     *
     * @return Stream of {@link OntNPA}
     */
    Stream<? extends OntNPA> negativeAssertions();

    /**
     * Adds or removes {@link OWL#FunctionalProperty owl:FunctionalProperty} declaration
     * for this property according to the given boolean flag.
     *
     * @param functional {@code true} if should be functional
     */
    void setFunctional(boolean functional);

    /**
     * Lists all of the declared domain class expressions of this property expression.
     * In other words, returns the right-hand sides of statement {@code P rdfs:domain C},
     * where {@code P} is this property expression.
     *
     * @return Stream of {@link OntCE class expression}s
     */
    @Override
    default Stream<OntCE> domain() {
        return objects(RDFS.domain, OntCE.class);
    }

    /**
     * Adds a statement {@code P rdfs:domain C},
     * where {@code P} is this property expression and {@code C} is the specified class expression.
     *
     * @param domain {@link OntCE class expression}, not null
     * @return {@link OntStatement} to allow the addition of annotations
     */
    default OntStatement addDomain(OntCE domain) {
        return addStatement(RDFS.domain, domain);
    }

    /**
     * Answers {@code true} if it is a functional (data or object) property expression.
     * A functional property is defined by the statement {@code P rdf:type owl:FunctionalProperty},
     * where {@code P} is this property expression.
     *
     * @return boolean
     */
    default boolean isFunctional() {
        return hasType(OWL.FunctionalProperty);
    }

}
