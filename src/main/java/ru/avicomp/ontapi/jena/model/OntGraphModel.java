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

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * An enhanced view of a {@link Model Jena model} that is known to contain <b>OWL2</b> ontology data.
 * This is an analogue of {@link org.apache.jena.ontology.OntModel jena ont model}
 * to work with Ontology graph in accordance with OWL2 DL specification.
 * Note: since this model is only for OWL2 semantics, it does not support {@link org.apache.jena.ontology.Profile jena profiles},
 * and model configuration is delegated directly to {@link ru.avicomp.ontapi.jena.impl.conf.OntPersonality Ont Personality}.
 * Also note: it does not extends {@link InfModel} interface,
 * although encapsulated graph always can be wrapped as {@link InfModel} (see {@link OntGraphModel#getInferenceModel(Reasoner)}).
 * <p>
 * Created by @szuev on 11.11.2016.
 *
 * @see <a href='https://www.w3.org/TR/owl2-mapping-to-rdf'>OWL2 RDF mapping</a>
 * @see <a href='https://www.w3.org/TR/owl2-quick-reference/'>A Quick Guide</a>
 */
public interface OntGraphModel extends Model {

    /**
     * Returns the base Graph, i.e. the primary Graph without any sub graphs.
     * Only the base graph is allowed to be edited from this interface.
     * To get whole union graph use method {@link #getGraph()}.
     *
     * @return {@link Graph}
     */
    Graph getBaseGraph();

    /**
     * Returns the standard model that corresponds the base graph (see {@link #getBaseGraph()}).
     * Note: there is a Jena-builtin Personality ({@link org.apache.jena.enhanced.BuiltinPersonalities#model})
     * inside the returned model.
     *
     * @return {@link Model}
     */
    Model getBaseModel();

    /**
     * Returns the inference model shadow.
     * Note(1): there is a jena-builtin personality ({@link org.apache.jena.enhanced.BuiltinPersonalities#model})
     * inside the returned model.
     * Note(2): any changes in the returned {@link InfModel inf model} do not affect on this model.
     *
     * @param reasoner {@link Reasoner}, not null.
     * @return {@link InfModel}
     */
    InfModel getInferenceModel(Reasoner reasoner);

    /**
     * Gets an Ontology ID object.
     * Since OWL2 graph can only contain the one {@code @uri rdf:type owl:Ontology} triple inside,
     * this method creates such statement if it absent;
     * in case there are more than one Resource with type equaled to {@code owl:Ontology},
     * it chooses the most bulky one (i.e. those that contains the most number of associated statements)
     * and all the others leave intact.
     *
     * @return {@link OntID} an existing or new one {@link Resource} with root statement '_:x rdf:type owl:Ontology'
     * @see ru.avicomp.ontapi.jena.utils.Graphs#ontologyNode
     */
    OntID getID();

    /**
     * Creates a new {@code @uri rdf:type owl:Ontology} statement for the specified {@code uri}
     * and wraps it as Ontology ID Resource.
     * Removes all extra ontology objects if they are present and moves their content to the new one,
     * as it is required by OWL2 specification.
     *
     * @param uri String, can be null to make anonymous ontology
     * @return the new {@link OntID} instance
     * @throws OntJenaException if ontology can't be added (e.g. due to collision with imports)
     */
    OntID setID(String uri);

    /**
     * Adds a sub model to {@code owl:import} and to the graph hierarchy.
     *
     * @param m {@link OntGraphModel ont jena model} to add, not null
     * @return this model to allow cascading calls
     * @throws OntJenaException if specified ontology is anonymous
     *                          or already present in the imports (both as graph and in owl-declaration)
     * @see OntID#addImport(String)
     */
    OntGraphModel addImport(OntGraphModel m) throws OntJenaException;

    /**
     * Removes a sub-model from {@code owl:import} and from the graph hierarchy.
     * Does nothing, if the specified model does not belong to this ontology.
     *
     * @param m {@link OntGraphModel ont jena model} to remove, not null
     * @return this model to allow cascading calls
     * @see OntID#removeImport(String)
     */
    OntGraphModel removeImport(OntGraphModel m);

    /**
     * Removes import (both {@code owl:import} declaration and the corresponding graph)
     * by the given uri if it is found.
     *
     * @param uri String, an iri of ontology to find, not null
     * @return this model to allow cascading calls
     */
    OntGraphModelImpl removeImport(String uri);

    /**
     * Lists all top-level imported models which have {@code owl:import} reference inside the base graph.
     *
     * @return Stream of {@link OntGraphModel}s
     * @see OntID#imports()
     */
    Stream<OntGraphModel> imports();

    /**
     * Lists all ont-objects for the specified type.
     *
     * @param type {@link Class} the type of {@link OntObject}, not null
     * @param <O>  ont-object subtype
     * @return Stream of {@link OntObject}s
     * @see #ontEntities()
     */
    <O extends OntObject> Stream<O> ontObjects(Class<O> type);

    /**
     * Lists all entities declared in the model.
     * Builtins are not included.
     * The retrieved entities can belong to the underlying graphs also.
     * Note: this method returns non-distinct stream - the duplicate elements (by equals and hasCode, not by real type)
     * means that there is so called punning.
     *
     * @return Stream of {@link OntEntity}
     * @see #ontObjects(Class)
     * @see #ontEntities(Class)
     */
    Stream<OntEntity> ontEntities();

    /**
     * Lists all typed individuals from a model.
     * Notice that the method {@link OntGraphModel#ontObjects(Class)} called with parameter {@code OntIndividual.class}
     * (i.e. {@code model.ontObject(OntIndividual.class)}) will return all individuals from a model,
     * even those which have no explicit declarations (e.g. any part of {@code owl:sameAs} is an individual),
     * while this method returns only class-assertion individuals.
     *
     * @return Stream of {@link OntIndividual}s
     * @see OntGraphModel#listNamedIndividuals()
     */
    Stream<OntIndividual> classAssertions();

    /**
     * Returns the ont-entity for the specified type and uri.
     * This method can be used to wrap builtin entities, which are not belonging to the graph in fact.
     *
     * @param type {@link Class}, the type of {@link OntEntity}, not null.
     * @param uri, String, not null.
     * @param <E>  type of ont-entity
     * @return {@link OntEntity} or null
     */
    <E extends OntEntity> E getOntEntity(Class<E> type, String uri);

    /**
     * Lists all ont-statements.
     *
     * @return Stream of {@link OntStatement}
     * @see Model#listStatements()
     */
    Stream<OntStatement> statements();

    /**
     * Lists all statements for the specified subject, predicate and object.
     *
     * @param s {@link Resource}, the subject
     * @param p {@link Property}, the predicate
     * @param o {@link RDFNode}, the object
     * @return Stream of {@link OntStatement}
     * @see Model#listStatements(Resource, Property, RDFNode)
     */
    Stream<OntStatement> statements(Resource s, Property p, RDFNode o);

    /**
     * Lists all statements from the {@link OntGraphModel#getBaseGraph() base graph}
     * for the specified subject, predicate and object.
     * Equivalent to {@code model.statements(s, p, o).filter(OntStatement::isLocal)}
     *
     * @param s {@link Resource}, the subject
     * @param p {@link Property}, the predicate
     * @param o {@link RDFNode}, the object
     * @return Stream of {@link OntStatement}
     * @see OntGraphModel#statements(Resource, Property, RDFNode)
     * @see OntStatement#isLocal()
     */
    Stream<OntStatement> localStatements(Resource s, Property p, RDFNode o);

    /**
     * Answers an {@link OntStatement Ontology Statement} in this model who's SPO is that of the {@code triple}.
     *
     * @param triple {@link Triple}, not null
     * @return {@link OntStatement}
     */
    @Override
    OntStatement asStatement(Triple triple);

    /**
     * Answers iff the statement belongs to the base graph.
     *
     * @param statement {@link Statement}
     * @return true if statement is local.
     * @see OntStatement#isLocal()
     * @see OntObject#isLocal()
     * @see OntGraphModel#localStatements(Resource, Property, RDFNode)
     */
    boolean isLocal(Statement statement);

    /**
     * Removes the given ont-object from the graph-model.
     *
     * @param obj {@link OntObject}
     * @return this model
     * @see OntObject#spec()
     */
    OntGraphModel removeOntObject(OntObject obj);

    /**
     * Removes ont-statement including its annotations.
     *
     * @param statement {@link OntStatement}
     * @return this model
     * @see #remove(Statement)
     */
    OntGraphModel removeOntStatement(OntStatement statement);

    /**
     * Creates an owl-entity by type and uri.
     *
     * @param type {@link Class}, the type of {@link OntEntity}, not null
     * @param uri, String, not null
     * @param <E>  type of ont-entity
     * @return {@link OntEntity}
     * @throws OntJenaException.Creation in case something is wrong
     * @see #getOntEntity(Class, String)
     */
    <E extends OntEntity> E createOntEntity(Class<E> type, String uri);

    /**
     * Creates a facet restriction by the type and literal.
     *
     * @param type    {@link Class}, the type of {@link OntFR}, not null
     * @param literal {@link Literal}, not null
     * @param <F>     type of ont-facet-restriction
     * @return {@link OntFR}
     */
    <F extends OntFR> F createFacetRestriction(Class<F> type, Literal literal);

    /*
     * ===========================
     * Creation Disjoint sections:
     * ===========================
     */

    OntDisjoint.Classes createDisjointClasses(Collection<OntCE> classes);

    OntDisjoint.Individuals createDifferentIndividuals(Collection<OntIndividual> individuals);

    OntDisjoint.ObjectProperties createDisjointObjectProperties(Collection<OntOPE> properties);

    OntDisjoint.DataProperties createDisjointDataProperties(Collection<OntNDP> properties);

    /*
     * =====================
     * Creation Data Ranges:
     * =====================
     */

    OntDR.OneOf createOneOfDataRange(Collection<Literal> values);

    OntDR.Restriction createRestrictionDataRange(OntDR property, Collection<OntFR> values);

    OntDR.ComplementOf createComplementOfDataRange(OntDR other);

    OntDR.UnionOf createUnionOfDataRange(Collection<OntDR> values);

    OntDR.IntersectionOf createIntersectionOfDataRange(Collection<OntDR> values);

    /*
     * ===========================
     * Creation Class Expressions:
     * ===========================
     */

    OntCE.ObjectSomeValuesFrom createObjectSomeValuesFrom(OntOPE onProperty, OntCE other);

    OntCE.DataSomeValuesFrom createDataSomeValuesFrom(OntNDP onProperty, OntDR other);

    OntCE.ObjectAllValuesFrom createObjectAllValuesFrom(OntOPE onProperty, OntCE other);

    OntCE.DataAllValuesFrom createDataAllValuesFrom(OntNDP onProperty, OntDR other);

    OntCE.ObjectHasValue createObjectHasValue(OntOPE onProperty, OntIndividual other);

    OntCE.DataHasValue createDataHasValue(OntNDP onProperty, Literal other);

    OntCE.ObjectMinCardinality createObjectMinCardinality(OntOPE onProperty, int cardinality, OntCE onObject);

    OntCE.DataMinCardinality createDataMinCardinality(OntNDP onProperty, int cardinality, OntDR onObject);

    OntCE.ObjectMaxCardinality createObjectMaxCardinality(OntOPE onProperty, int cardinality, OntCE onObject);

    OntCE.DataMaxCardinality createDataMaxCardinality(OntNDP onProperty, int cardinality, OntDR onObject);

    OntCE.ObjectCardinality createObjectCardinality(OntOPE onProperty, int cardinality, OntCE onObject);

    OntCE.DataCardinality createDataCardinality(OntNDP onProperty, int cardinality, OntDR onObject);

    OntCE.UnionOf createUnionOf(Collection<OntCE> classes);

    OntCE.IntersectionOf createIntersectionOf(Collection<OntCE> classes);

    OntCE.OneOf createOneOf(Collection<OntIndividual> individuals);

    OntCE.HasSelf createHasSelf(OntOPE onProperty);

    OntCE.NaryDataAllValuesFrom createDataAllValuesFrom(Collection<OntNDP> onProperties, OntDR other);

    OntCE.NaryDataSomeValuesFrom createDataSomeValuesFrom(Collection<OntNDP> onProperties, OntDR other);

    OntCE.ComplementOf createComplementOf(OntCE other);

    /*
     * ===================================
     * SWRL Objects (Variable, Atoms, Imp)
     * ===================================
     */

    OntSWRL.Variable createSWRLVariable(String uri);

    OntSWRL.Atom.BuiltIn createBuiltInSWRLAtom(Resource predicate, Collection<OntSWRL.DArg> arguments);

    OntSWRL.Atom.OntClass createClassSWRLAtom(OntCE clazz, OntSWRL.IArg arg);

    OntSWRL.Atom.DataRange createDataRangeSWRLAtom(OntDR range, OntSWRL.DArg arg);

    OntSWRL.Atom.DataProperty createDataPropertySWRLAtom(OntNDP dataProperty, OntSWRL.IArg firstArg, OntSWRL.DArg secondArg);

    OntSWRL.Atom.ObjectProperty createObjectPropertySWRLAtom(OntOPE dataProperty, OntSWRL.IArg firstArg, OntSWRL.IArg secondArg);

    OntSWRL.Atom.DifferentIndividuals createDifferentIndividualsSWRLAtom(OntSWRL.IArg firstArg, OntSWRL.IArg secondArg);

    OntSWRL.Atom.SameIndividuals createSameIndividualsSWRLAtom(OntSWRL.IArg firstArg, OntSWRL.IArg secondArg);

    OntSWRL.Imp createSWRLImp(Collection<OntSWRL.Atom> head, Collection<OntSWRL.Atom> body);

    /*
     * ===================================
     * Default methods for simplification:
     * ===================================
     */

    default Stream<OntStatement> localStatements() {
        return localStatements(null, null, null);
    }

    default <E extends OntEntity> Stream<E> ontEntities(Class<E> type) {
        return ontObjects(type);
    }

    default <E extends OntEntity> E fetchOntEntity(Class<E> type, String uri) {
        E res = getOntEntity(type, uri);
        return res == null ? createOntEntity(type, uri) : res;
    }

    default Stream<OntClass> listClasses() {
        return ontEntities(OntClass.class);
    }

    default Stream<OntNAP> listAnnotationProperties() {
        return ontEntities(OntNAP.class);
    }

    default Stream<OntNDP> listDataProperties() {
        return ontEntities(OntNDP.class);
    }

    default Stream<OntNOP> listObjectProperties() {
        return ontEntities(OntNOP.class);
    }

    default Stream<OntDT> listDatatypes() {
        return ontEntities(OntDT.class);
    }

    default Stream<OntIndividual.Named> listNamedIndividuals() {
        return ontEntities(OntIndividual.Named.class);
    }

    default <E extends OntEntity> E getOntEntity(Class<E> type, Resource uri) {
        return getOntEntity(type, uri.getURI());
    }

    default OntNAP getAnnotationProperty(Resource uri) {
        return getOntEntity(OntNAP.class, uri);
    }

    /*
     * =======================
     * Some built-in entities:
     * =======================
     */

    default OntNAP getRDFSComment() {
        return getAnnotationProperty(RDFS.comment);
    }

    default OntNAP getRDFSLabel() {
        return getAnnotationProperty(RDFS.label);
    }

    default OntClass getOWLThing() {
        return getOntEntity(OntClass.class, OWL.Thing);
    }

    default OntClass getOWLNothing() {
        return getOntEntity(OntClass.class, OWL.Nothing);
    }

    default OntNOP getOWLTopObjectProperty() {
        return getOntEntity(OntNOP.class, OWL.topObjectProperty);
    }

    default OntNOP getOWLBottomObjectProperty() {
        return getOntEntity(OntNOP.class, OWL.bottomObjectProperty);
    }

    default OntNDP getOWLTopDataProperty() {
        return getOntEntity(OntNDP.class, OWL.topDataProperty);
    }

    default OntNDP getOWLBottomDataProperty() {
        return getOntEntity(OntNDP.class, OWL.bottomDataProperty);
    }

}
