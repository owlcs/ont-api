package ru.avicomp.ontapi.jena.model;

import java.util.Collection;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * This is our analogue of {@link org.apache.jena.ontology.OntModel} to work with Ontology graph in accordance with OWL2 DL specification.
 * See <a href='https://www.w3.org/TR/owl2-mapping-to-rdf'>OWL2 RDF mapping</a>.
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
    InfModel asInferenceModel(Reasoner reasoner);

    /**
     * Gets ontology ID {@link OntObject}.
     * Since OWL2 graph must have only the one '_:x rdf:type owl:Ontology' section inside,
     * this method creates such statement if it absent;
     * in case there are more than one resource with owl:Ontology type it chooses the most bulky section.
     *
     * @return {@link OntID} an existing or new one {@link Resource} with root statement '_:x rdf:type owl:Ontology'
     * @see ru.avicomp.ontapi.jena.utils.Graphs#getOntology
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
     * @param m {@link OntGraphModel}, not null.
     * @throws OntJenaException if it is anonymous ontology
     * @see OntID#addImport(String)
     */
    void addImport(OntGraphModel m);

    /**
     * Removes sub-model from owl:import and from graph hierarchy.
     *
     * @param m {@link OntGraphModel}, not null.
     * @see OntID#removeImport(String)
     */
    void removeImport(OntGraphModel m);

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
     */
    boolean isInBaseModel(Statement statement);

    /**
     * Removes ont-object from the graph-model.
     *
     * @param obj {@link OntObject}
     */
    void removeOntObject(OntObject obj);

    /**
     * Creates an owl-entity by type and uri.
     *
     * @param type {@link Class}, the type of {@link OntEntity}, not null.
     * @param uri, String, not null.
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
