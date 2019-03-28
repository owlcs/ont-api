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

import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An OWL Class Entity (i.e. named class expression).
 * This is an analogue of {@link org.apache.jena.ontology.OntClass}, but for OWL2.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntClass extends OntEntity, OntCE {

    /**
     * Creates a DisjointUnion as {@link OntList ontology list} of {@link OntCE Class Expression}s
     * that is attached to this Class using the predicate {@link OWL#disjointUnionOf owl:disjointUnionOf}.
     * The resulting rdf-list will consist of all the elements of the specified collection in the same order but with exclusion of duplicates.
     * Note: {@code null}s in collection will cause {@link NullPointerException NullPointerException}.
     * For additional information about DisjointUnion logical construction see
     * <a href='https://www.w3.org/TR/owl2-syntax/#Disjoint_Union_of_Class_Expressions'>9.1.4 Disjoint Union of Class Expressions</a>.
     *
     * @param classes {@link Collection} (preferably {@link Set}) of {@link OntCE class expression}s
     * @return {@link OntList} of {@link OntCE}s
     * @since 1.3.0
     */
    OntList<OntCE> createDisjointUnion(Collection<OntCE> classes);

    /**
     * Lists all DisjointUnion {@link OntList ontology list}s that are attached to this OWL Class
     * on predicate {@link OWL#disjointUnionOf owl:disjointUnionOf}.
     *
     * @return Stream of {@link OntList}s with parameter-type {@code OntCE}
     * @since 1.3.0
     */
    Stream<OntList<OntCE>> listDisjointUnions();

    /**
     * Deletes the given DisjointUnion list including its annotations
     * with predicate {@link OWL#disjointUnionOf owl:disjointUnionOf} for this resource from its associated model.
     *
     * @param list {@link RDFNode} can be {@link OntList} or {@link RDFList}
     * @throws OntJenaException if the list is not found
     * @since 1.3.0
     */
    void removeDisjointUnion(RDFNode list);

    /**
     * Deletes all DisjointUnion lists including their annotations
     * with predicate {@link OWL#disjointUnionOf owl:disjointUnionOf} for this resource from its associated model.
     *
     * @since 1.3.0
     */
    default void clearDisjointUnions() {
        listDisjointUnions().collect(Collectors.toSet()).forEach(this::removeDisjointUnion);
    }

    /**
     * Finds a DisjointUnion logical construction attached to this class by the specified rdf-node in the form of {@link OntList}.
     *
     * @param list {@link RDFNode}
     * @return Optional around {@link OntList} of {@link OntCE class expression}s
     * @since 1.3.0
     */
    default Optional<OntList<OntCE>> findDisjointUnion(RDFNode list) {
        return listDisjointUnions()
                .filter(r -> Objects.equals(r, list))
                .findFirst();
    }

    /**
     * Creates a DisjointUnion {@link OntList ontology list} and returns statement {@code CN owl:disjointUnionOf (C1 ... CN)}
     * to allow the addition of annotations.
     * About RDF Graph annotation specification see, for example,
     * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>.
     *
     * @param classes Array of {@link OntCE class expressions} without {@code null}s, duplicates will be discarded and order will be saved
     * @return {@link OntStatement}
     * @see #createDisjointUnion(Collection)
     * @since 1.3.0
     */
    default OntStatement addDisjointUnionOf(OntCE... classes) {
        return createDisjointUnion(Arrays.stream(classes).collect(Collectors.toCollection(LinkedHashSet::new))).getRoot();
    }

    /**
     * Returns all class expressions from the right part of the statement with this class as a subject
     * and {@link OWL#disjointUnionOf owl:disjointUnionOf} as a predicate
     * (the pattern: {@code CN owl:disjointUnionOf ( C1 ... Cn )}).
     * If there are several []-lists in the model that satisfy these conditions,
     * all their content will be merged into the one distinct stream.
     *
     * @return <b>distinct</b> stream of {@link OntCE class expressions}s
     * @see #listDisjointUnions()
     */
    default Stream<OntCE> disjointUnionOf() {
        return listDisjointUnions().flatMap(OntList::members).distinct();
    }

    /**
     * Creates a disjoint-union section returning its root statement to allow adding annotations.
     * The pattern: {@code CN owl:disjointUnionOf ( C1 ... CN )}.
     *
     * @param classes the collection of {@link OntCE class expression}s
     * @return {@link OntStatement}
     * @see #createDisjointUnion(Collection)
     */
    default OntStatement addDisjointUnionOf(Collection<OntCE> classes) {
        return createDisjointUnion(classes).getRoot();
    }

}
