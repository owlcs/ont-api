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

package com.github.owlcs.ontapi.internal;

import com.github.owlcs.ontapi.ID;
import com.github.sszuev.jena.ontapi.UnionGraph;
import com.github.sszuev.jena.ontapi.common.OntPersonality;
import com.github.sszuev.jena.ontapi.impl.PersonalityModel;
import com.github.sszuev.jena.ontapi.model.OntModel;
import org.apache.jena.rdf.model.Model;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.HasOntologyID;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLPrimitive;

import java.util.stream.Stream;

/**
 * A Buffer Graph OWL model,
 * that extends  {@link OntModel ONTAPI Jena Model} providing various methods to work with {@link OWLObject OWLAPI Object}s.
 * <p>
 * Created by @ssz on 24.05.2020.
 */
public interface InternalModel extends OntModel, PersonalityModel, HasOntologyID, ListAxioms {

    /**
     * Answers the {@link UnionGraph} which this Model is presenting.
     *
     * @return {@link UnionGraph}
     */
    UnionGraph getGraph();

    /**
     * Gets the {@link OWLOntologyID OWL Ontology ID} from the model.
     *
     * @return {@link ID}
     * @see #getID()
     */
    @Override
    ID getOntologyID();

    /**
     * Sets the {@link OWLOntologyID OWL Ontology ID} to the model.
     *
     * @param id {@link OWLOntologyID}
     * @throws IllegalArgumentException in case the given id is broken
     * @see #setID(String)
     */
    void setOntologyID(OWLOntologyID id);

    /**
     * Lists all owl import-declarations.
     *
     * @return a {@code Stream} of {@link OWLImportsDeclaration}s
     */
    Stream<OWLImportsDeclaration> listOWLImportDeclarations();

    /**
     * Gets all ontology header annotations.
     *
     * @return a {@code Stream} of {@link OWLAnnotation}
     * @see InternalModel#listOWLAxioms()
     */
    Stream<OWLAnnotation> listOWLAnnotations();

    /**
     * Lists all class expressions (both anonymous and named) in the form of OWL-API objects.
     *
     * @return a {@code Stream} of {@link OWLClassExpression}s
     * @see #listOWLClasses()
     */
    Stream<OWLClassExpression> listOWLClassExpressions();

    /**
     * Lists all anonymous individuals in the form of OWL-API objects.
     *
     * @return a {@code Stream} of {@link OWLAnonymousIndividual}s
     */
    Stream<OWLAnonymousIndividual> listOWLAnonymousIndividuals();

    /**
     * Lists all named individuals in the form of OWL-API objects.
     *
     * @return a {@code Stream} of {@link OWLNamedIndividual}s
     */
    Stream<OWLNamedIndividual> listOWLNamedIndividuals();

    /**
     * Lists all OWL classes in the form of OWL-API objects.
     *
     * @return a {@code Stream} of {@link OWLClass}es.
     */
    Stream<OWLClass> listOWLClasses();

    /**
     * Lists all named data-ranges (i.e. datatypes) in the form of OWL-API objects.
     *
     * @return a {@code Stream} of {@link OWLDatatype}s
     */
    Stream<OWLDatatype> listOWLDatatypes();

    /**
     * Lists all data properties in the form of OWL-API objects.
     *
     * @return a {@code Stream} of {@link OWLDataProperty}s
     */
    Stream<OWLDataProperty> listOWLDataProperties();

    /**
     * Lists all object properties in the form of OWL-API objects.
     *
     * @return a {@code Stream} of {@link OWLObjectProperty}s
     */
    Stream<OWLObjectProperty> listOWLObjectProperties();

    /**
     * Lists all annotation properties in the form of OWL-API objects.
     *
     * @return a {@code Stream} of {@link OWLAnnotationProperty}s
     */
    Stream<OWLAnnotationProperty> listOWLAnnotationProperties();

    /**
     * Lists {@link OWLEntity OWL Entity} for the specified IRI.
     *
     * @param iri {@link IRI}
     * @return a {@code Stream} of {@link OWLEntity}s.
     */
    Stream<OWLEntity> listOWLEntities(IRI iri);

    /**
     * Lists all 'punnings', i.e. subjects of {@link OWLEntity}s with different types.
     *
     * @param withImports if {@code false} the method takes into account only the base graph
     * @return a {@code Stream} of {@link IRI}s
     * @see <a href='https://www.w3.org/TR/owl2-new-features/#F12:_Punning'>2.4.1 F12: Punning</a>
     * @see OntPersonality.Punnings
     */
    Stream<IRI> listPunningIRIs(boolean withImports);

    /**
     * Tests if the given {@link OWLEntity} has the OWL declaration.
     * A builtin entity has no declarations.
     *
     * @param entity {@link OWLEntity} to test
     * @return boolean
     */
    boolean containsOWLDeclaration(OWLEntity entity);

    /**
     * Answers {@code true} if the given class is present in the ontology signature.
     *
     * @param c {@link OWLClass}
     * @return boolean
     */
    boolean containsOWLEntity(OWLClass c);

    /**
     * Answers {@code true} if the given datatype is present in the ontology signature.
     *
     * @param d {@link OWLDatatype}
     * @return boolean
     */
    boolean containsOWLEntity(OWLDatatype d);

    /**
     * Answers {@code true} if the given individual is present in the ontology signature.
     *
     * @param i {@link OWLNamedIndividual}
     * @return boolean
     */
    boolean containsOWLEntity(OWLNamedIndividual i);

    /**
     * Answers {@code true} if the given property is present in the ontology signature.
     *
     * @param p {@link OWLDataProperty}
     * @return boolean
     */
    boolean containsOWLEntity(OWLDataProperty p);

    /**
     * Answers {@code true} if the given property is present in the ontology signature.
     *
     * @param p {@link OWLObjectProperty}
     * @return boolean
     */
    boolean containsOWLEntity(OWLObjectProperty p);

    /**
     * Answers {@code true} if the given property is present in the ontology signature.
     *
     * @param p {@link OWLAnnotationProperty}
     * @return boolean
     */
    boolean containsOWLEntity(OWLAnnotationProperty p);

    /**
     * Lists all ontology axioms.
     *
     * @return {@code Stream} of {@link OWLAxiom}s
     * @see #listOWLAnnotations()
     */
    Stream<OWLAxiom> listOWLAxioms();

    /**
     * Lists all logical axioms.
     *
     * @return a {@code Stream} of {@link OWLAxiom}s
     */
    Stream<OWLLogicalAxiom> listOWLLogicalAxioms();

    /**
     * Lists axioms of the given axiom-type.
     *
     * @param type {@link AxiomType}
     * @param <A>  type of axiom
     * @return {@code Stream} of {@link OWLAxiom}s
     */
    <A extends OWLAxiom> Stream<A> listOWLAxioms(AxiomType<A> type);

    /**
     * Lists axioms for the specified types.
     *
     * @param filter a {@code Iterable} of {@link AxiomType}s
     * @return {@code Stream} of {@link OWLAxiom}s
     */
    Stream<OWLAxiom> listOWLAxioms(Iterable<AxiomType<?>> filter);

    /**
     * Lists all {@code OWLAxiom}s for the given {@link OWLPrimitive}
     *
     * @param primitive not {@code null}
     * @return a {@code Stream} of {@link OWLAxiom}s
     */
    Stream<OWLAxiom> listOWLAxioms(OWLPrimitive primitive);

    /**
     * Selects all axioms for the given object-component.
     *
     * @param type   a class-type of {@link OWLAxiom}
     * @param object {@link OWLObject}, that is present as a component in every container of the returned stream
     * @param <A>    any subtype of {@link OWLAxiom}
     * @return a {@code Stream} of {@link OWLAxiom}s
     */
    <A extends OWLAxiom> Stream<A> listOWLAxioms(Class<A> type, OWLObject object);

    /**
     * Returns the number of axioms in this ontology.
     *
     * @return {@code long}, the count
     */
    long getOWLAxiomCount();

    /**
     * Answers {@code true} if the given axiom is present within this buffer-model.
     * It is equivalent to the expression {@code this.listOWLAxioms().anyMatch(a::equals)}.
     *
     * @param axiom {@link OWLAxiom}, not {@code null}
     * @return {@code true} if the axiom is present within the model
     * @see #containsIgnoreAnnotations(OWLAxiom)
     * @see #contains(OWLAnnotation)
     */
    boolean contains(OWLAxiom axiom);

    /**
     * Answers {@code true} if the given annotation is present in ontology header.
     *
     * @param annotation {@link OWLAnnotation}, not {@code null}
     * @return {@code true} if the annotation is present within the model
     * @see #contains(OWLAxiom)
     */
    boolean contains(OWLAnnotation annotation);

    /**
     * Answers {@code true} if the given axiom is present within the model.
     * While comparing axiom annotations (if present) are ignored.
     *
     * @param axiom {@link OWLAxiom}, not {@code null}
     * @return {@code true} if base axiom (its main part) is present
     * @see #contains(OWLAxiom)
     */
    boolean containsIgnoreAnnotations(OWLAxiom axiom);

    /**
     * Answers {@code true} if the ontology is ontologically empty (no header, no axioms).
     *
     * @return {@code true} if the ontology does not contain any axioms and annotations (locally);
     * note, that the encapsulated graph still may contain some triples,
     * and the method {@link Model#isEmpty()} may return {@code false} at the same time
     */
    boolean isOntologyEmpty();

    /**
     * Adds the specified axiom to the model.
     *
     * @param axiom {@link OWLAxiom}
     * @return {@code true} if the {@code axiom} has been added to the graph
     * @see #add(OWLAnnotation)
     */
    boolean add(OWLAxiom axiom);

    /**
     * Adds the given annotation to the ontology header of the model.
     *
     * @param annotation {@link OWLAnnotation}
     * @return {@code true} if the {@code annotation} has been added to the graph
     * @see #add(OWLAxiom)
     */
    boolean add(OWLAnnotation annotation);

    /**
     * Removes the given axiom from the model.
     * Also, clears the cache for the entity type, if the entity has been belonged to the removed axiom.
     *
     * @param axiom {@link OWLAxiom}
     * @return {@code true} if the {@code axiom} has been removed from the graph
     * @see #remove(OWLAnnotation)
     */
    boolean remove(OWLAxiom axiom);

    /**
     * Removes the given ontology header annotation from the model.
     *
     * @param annotation {@link OWLAnnotation}
     * @return {@code true} if the {@code annotation} has been removed from the graph
     * @see #remove(OWLAxiom)
     */
    boolean remove(OWLAnnotation annotation);

    /**
     * Invalidates all caches.
     */
    void clearCache();

    /**
     * Forcibly loads the whole content cache.
     */
    void forceLoad();

    /**
     * Invalidates the cache if needed.
     * <p>
     * The OWL-API serialization may not work correctly without explicit expansion of axioms into
     * a strictly defined form. The cache cleaning encourages repeated reading of the encapsulated graph,
     * and, thus, leads the axioms to a uniform view.
     * Without this operation, the axiomatic representation would look slightly different
     * and the reload test (loading/saving in different formats) would not be passed.
     * Also, absence of uniformed axiomatic view may lead to exceptions,
     * since some OWL-storers require explicit declarations, which may not be present,
     * if the ontology was assembled manually.
     * It is important to invalidate whole the cache, since user-defined axioms may content parts of other axioms,
     * such as annotation assertions, declarations, data-range definitions, etc.
     */
    void clearCacheIfNeeded();
}
