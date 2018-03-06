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
 *
 */

package ru.avicomp.ontapi.jena.model;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * This is our analogue of {@link org.apache.jena.ontology.OntModel} to work with Ontology graph in accordance with OWL2 DL specification.
 * See <a href='https://www.w3.org/TR/owl2-mapping-to-rdf'>OWL2 RDF mapping</a> and <a href='https://www.w3.org/TR/owl2-quick-reference/'>A Quick Guide</a>.
 * Encapsulates {@link org.apache.jena.graph.Graph} and extends {@link Model}.
 * <p>
 * Created by @szuev on 11.11.2016.
 */
public interface OntGraphModel extends Model {

    /**
     * Returns the base Graph, i.e. the primary Graph without any sub graphs.
     * to get whole graph use {@link #getGraph()}.
     *
     * @return {@link Graph}
     */
    Graph getBaseGraph();

    /**
     * Returns the standard model which corresponds the base graph {@link #getBaseGraph()}
     * Note: there is a jena-builtin personality ({@link org.apache.jena.enhanced.BuiltinPersonalities#model}) inside the result model.
     *
     * @return {@link Model}
     */
    Model getBaseModel();

    /**
     * Returns the inference model shadow.
     * Note(1): there is a jena-builtin personality ({@link org.apache.jena.enhanced.BuiltinPersonalities#model}) inside the result model.
     * Note(2): changes in {@link org.apache.jena.reasoner.InfGraph} inside result model do not affect on this graph ({@link #getGraph()}).
     *
     * @param reasoner {@link Reasoner}, not null.
     * @return {@link InfModel}
     */
    InfModel getInferenceModel(Reasoner reasoner);

    /**
     * Gets ontology ID {@link OntObject}.
     * Since OWL2 graph must have only the one '_:x rdf:type owl:Ontology' section inside,
     * this method creates such statement if it absent;
     * in case there are more than one resource with owl:Ontology type it chooses the most bulky section.
     *
     * @return {@link OntID} an existing or new one {@link Resource} with root statement '_:x rdf:type owl:Ontology'
     * @see ru.avicomp.ontapi.jena.utils.Graphs#ontologyNode
     */
    OntID getID();

    /**
     * Creates a new owl:Ontology declaration for the specified uri.
     * All extra ontologies will be removed and all their content will be moved to the new one.
     *
     * @param uri String, could be null for anonymous ontology.
     * @return the new {@link OntID} object.
     * @throws OntJenaException if ontology can't be added (e.g. due to collision with imports).
     */
    OntID setID(String uri);

    /**
     * Adds sub model to owl:import and to graph hierarchy.
     *
     * @param m {@link OntGraphModel}, other model, not null.
     * @return this model
     * @throws OntJenaException if it is anonymous ontology
     * @see OntID#addImport(String)
     */
    OntGraphModel addImport(OntGraphModel m);

    /**
     * Removes sub-model from owl:import and from graph hierarchy.
     *
     * @param m {@link OntGraphModel}, other model, not null.
     * @return this model
     * @see OntID#removeImport(String)
     */
    OntGraphModel removeImport(OntGraphModel m);

    /**
     * Returns top-level imported models which have owl:import reference inside base graph.
     *
     * @return Stream of {@link OntGraphModel}s.
     * @see OntID#imports()
     */
    Stream<OntGraphModel> imports();

    /**
     * Lists all ont-objects for the specified type.
     *
     * @param type {@link Class}, the type of {@link OntObject}, not null.
     * @param <O> type of ont-object
     * @return Stream of {@link OntObject}s.
     * @see #ontEntities()
     */
    <O extends OntObject> Stream<O> ontObjects(Class<O> type);

    /**
     * Note: this method returns not distinct stream.
     * This means that resources may have the same uri ('punnings')
     *
     * @return Stream of {@link OntEntity}
     * @see #ontObjects(Class)
     * @see #ontEntities(Class)
     */
    Stream<OntEntity> ontEntities();

    /**
     * Returns the ont-entity for the specified type and uri.
     *
     * @param type {@link Class}, the type of {@link OntEntity}, not null.
     * @param uri, String, not null.
     * @param <E> type of ont-entity
     * @return {@link OntEntity} or null
     */
    <E extends OntEntity> E getOntEntity(Class<E> type, String uri);

    /**
     * Lists all statements.
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
     * Answers if the statement belongs to the base graph
     *
     * @param statement {@link Statement}
     * @return true if statement is local.
     * @see OntStatement#isLocal()
     * @see OntObject#isLocal()
     */
    boolean isInBaseModel(Statement statement);

    /**
     * Removes ont-object from the graph-model.
     *
     * @param obj {@link OntObject}
     * @return this model
     * @see OntObject#content()
     */
    OntGraphModel removeOntObject(OntObject obj);

    /**
     * Removes ont-statement with its annotations.
     *
     * @param statement {@link OntStatement}
     * @return this model
     */
    OntGraphModel removeOntStatement(OntStatement statement);

    /**
     * Creates an owl-entity by type and uri.
     *
     * @param type {@link Class}, the type of {@link OntEntity}, not null.
     * @param uri, String, not null.
     * @param <E> type of ont-entity.
     * @return {@link OntEntity}.
     * @throws OntJenaException.Creation in case something is wrong.
     * @see #getOntEntity(Class, String)
     */
    <E extends OntEntity> E createOntEntity(Class<E> type, String uri);

    /**
     * Creates a facet restriction by the type and literal.
     *
     * @param type    {@link Class}, the type of {@link OntFR}, not null.
     * @param literal {@link Literal}, not null.
     * @param <F> type of ont-facet-restriction.
     * @return {@link OntFR}.
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

    default OntDT getRDFSLiteral() {
        return getOntEntity(OntDT.class, RDFS.Literal);
    }

}
