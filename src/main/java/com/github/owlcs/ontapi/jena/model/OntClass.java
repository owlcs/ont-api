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

import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;

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
     * Lists all {@code DisjointUnion} {@link OntList ontology list}s that are attached to this OWL Class
     * on predicate {@link OWL#disjointUnionOf owl:disjointUnionOf}.
     *
     * @return {@code Stream} of {@link OntList}s with parameter-type {@code OntCE}
     * @since 1.4.0
     */
    Stream<OntList<OntCE>> disjointUnions();

    /**
     * Creates a {@code DisjointUnion} as {@link OntList ontology []-list} of {@link OntCE Class Expression}s
     * that is attached to this OWL Class using the predicate {@link OWL#disjointUnionOf owl:disjointUnionOf}.
     * The resulting rdf-list will consist of all the elements of the specified collection
     * in the same order but with exclusion of duplicates.
     * Note: {@code null}s in collection will cause {@link OntJenaException.IllegalArgument exception}.
     * For additional information about {@code DisjointUnion} logical construction see
     * <a href='https://www.w3.org/TR/owl2-syntax/#Disjoint_Union_of_Class_Expressions'>9.1.4 Disjoint Union of Class Expressions</a>.
     *
     * @param classes {@link Collection} (preferably {@link Set}) of {@link OntCE class expression}s
     * @return {@link OntList} of {@link OntCE}s
     * @since 1.3.0
     * @see #addDisjointUnionOfStatement(OntCE...)
     * @see #removeDisjointUnion(Resource)
     */
    OntList<OntCE> createDisjointUnion(Collection<OntCE> classes);

    /**
     * Finds a {@code DisjointUnion} logical construction
     * attached to this class by the specified rdf-node in the form of {@link OntList}.
     *
     * @param list {@link RDFNode}
     * @return Optional around {@link OntList} of {@link OntCE class expression}s
     * @since 1.3.0
     */
    default Optional<OntList<OntCE>> findDisjointUnion(RDFNode list) {
        try (Stream<OntList<OntCE>> res = disjointUnions().filter(r -> Objects.equals(r, list))) {
            return res.findFirst();
        }
    }

    /**
     * Creates a {@code DisjointUnion} {@link OntList ontology list}
     * and returns the statement {@code CN owl:disjointUnionOf ( C1 ... Cn )} to allow the addition of annotations.
     * About RDF Graph annotation specification see, for example,
     * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>.
     *
     * @param classes Array of {@link OntCE class expressions} without {@code null}s,
     *                duplicates will be discarded and order will be saved
     * @return {@link OntStatement} to allow the subsequent annotations addition
     * @see #createDisjointUnion(Collection)
     *
     * @see #createDisjointUnion(Collection)
     * @see #addDisjointUnion(OntCE...)
     * @see #addDisjointUnionOfStatement(OntCE...)
     * @see #removeDisjointUnion(Resource)
     * @since 1.4.0
     */
    default OntStatement addDisjointUnionOfStatement(OntCE... classes) {
        return addDisjointUnionOfStatement(Arrays.stream(classes).collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /**
     * Creates a disjoint-union section returning its root statement to allow adding annotations.
     * The triple pattern: {@code CN owl:disjointUnionOf ( C1 ... Cn )}.
     *
     * @param classes a collection of {@link OntCE class expression}s without {@code null}s
     * @return {@link OntStatement} to allow the subsequent annotations addition
     * @see #createDisjointUnion(Collection)
     * @see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.3.1 Axioms that Generate a Main Triple</a>
     * @see #createDisjointUnion(Collection)
     * @see #addDisjointUnion(Collection)
     * @see #addDisjointUnionOfStatement(Collection)
     * @see #removeDisjointUnion(Resource)
     * @since 1.4.0
     */
    default OntStatement addDisjointUnionOfStatement(Collection<OntCE> classes) {
        return createDisjointUnion(classes).getRoot();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntClass addSuperClass(OntCE other) {
        addSubClassOfStatement(other);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntClass addDisjointClass(OntCE other) {
        addDisjointWithStatement(other);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntClass addEquivalentClass(OntCE other) {
        addEquivalentClassStatement(other);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntClass addHasKey(Collection<OntOPE> objectProperties, Collection<OntNDP> dataProperties) {
        addHasKeyStatement(objectProperties, dataProperties);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntClass addHasKey(OntDOP... properties) {
        addHasKeyStatement(properties);
        return this;
    }

    /**
     * @param classes a collection of {@link OntCE class expression}s without {@code null}s
     * @return <b>this</b> instance to allow cascading calls
     * @since 1.4.0
     */
    default OntClass addDisjointUnion(Collection<OntCE> classes) {
        addDisjointUnionOfStatement(classes);
        return this;
    }

    /**
     * @param classes Array of {@link OntCE class expressions} without {@code null}s,
     *                duplicates will be discarded and order will be saved
     * @return <b>this</b> instance to allow cascading calls
     * @since 1.4.0
     */
    default OntClass addDisjointUnion(OntCE... classes) {
        addDisjointUnionOfStatement(classes);
        return this;
    }

    /**
     * Deletes the given {@code DisjointUnion} list including its annotations.
     *
     * @param list {@link Resource} can be {@link OntList} or {@link RDFList}
     * @return <b>this</b> instance to allow cascading calls
     * @throws OntJenaException if the list is not found
     * @see #addDisjointUnion(Collection)
     * @see #createDisjointUnion(Collection)
     * @see #addDisjointUnionOfStatement(OntCE...)
     * @see #createDisjointUnion(Collection)
     * @since 1.3.0
     */
    OntClass removeDisjointUnion(Resource list);

    /**
     * {@inheritDoc}
     */
    default OntClass removeSuperClass(Resource other) {
        OntCE.super.removeSuperClass(other);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    default OntClass removeDisjointClass(Resource other) {
        OntCE.super.removeDisjointClass(other);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    default OntClass removeEquivalentClass(Resource other) {
        OntCE.super.removeEquivalentClass(other);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    default OntClass clearHasKeys() {
        OntCE.super.clearHasKeys();
        return this;
    }

    /**
     * Deletes all {@code DisjointUnion} []-lists including their annotations,
     * i.e. all those statements with the predicate {@link OWL#disjointUnionOf owl:disjointUnionOf}
     * for which this resource is a subject.
     *
     * @return <b>this</b> instance to allow cascading calls
     * @see #removeDisjointUnion(Resource)
     * @since 1.3.0
     */
    default OntClass clearDisjointUnions() {
        disjointUnions().collect(Collectors.toSet()).forEach(this::removeDisjointUnion);
        return this;
    }

    /**
     * Returns all class expressions from the right part of the statement with this class as a subject
     * and {@link OWL#disjointUnionOf owl:disjointUnionOf} as a predicate
     * (the triple pattern: {@code CN owl:disjointUnionOf ( C1 ... Cn )}).
     * If there are several []-lists in the model that satisfy these conditions,
     * all their content will be merged into the one distinct stream.
     *
     * @return <b>distinct</b> stream of {@link OntCE class expressions}s
     * @see #disjointUnions()
     * @since 1.4.0
     */
    default Stream<OntCE> fromDisjointUnionOf() {
        return disjointUnions().flatMap(OntList::members).distinct();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntClass addComment(String txt) {
        return addComment(txt, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntClass addComment(String txt, String lang) {
        return annotate(getModel().getRDFSComment(), txt, lang);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntClass addLabel(String txt) {
        return addLabel(txt, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntClass addLabel(String txt, String lang) {
        return annotate(getModel().getRDFSLabel(), txt, lang);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntClass annotate(OntNAP predicate, String txt, String lang) {
        return annotate(predicate, getModel().createLiteral(txt, lang));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default OntClass annotate(OntNAP predicate, RDFNode value) {
        addAnnotation(predicate, value);
        return this;
    }
}
