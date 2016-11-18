package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;

/**
 * This is our analogue of {@link org.apache.jena.ontology.OntModel} to work in accordance with OWL2 DL specification.
 * Encapsulates {@link org.apache.jena.graph.Graph} and extends {@link Model}
 * <p>
 * Created by @szuev on 11.11.2016.
 */
public interface OntGraphModel extends Model {

    Graph getBaseGraph();

    OntID getID();

    OntID setID(String uri);

    <T extends OntObject> Stream<T> ontObjects(Class<T> type);

    <T extends OntEntity> T getOntEntity(Class<T> type, String uri);

    Stream<OntEntity> ontEntities();

    <T extends OntEntity> Stream<T> ontEntities(Class<T> type);

    void removeOntObject(OntObject obj);

    <T extends OntEntity> T createOntEntity(Class<T> type, String uri);

    <T extends OntFR> T createFacetRestriction(Class<T> view, Literal literal);

    /**
     * ===========================
     * Creation Disjoint sections:
     * ===========================
     */

    OntDisjoint.Classes createDisjointClasses(Stream<OntCE> classes);

    OntDisjoint.Individuals createDifferentIndividuals(Stream<OntIndividual> individuals);

    OntDisjoint.ObjectProperties createDisjointObjectProperties(Stream<OntOPE> properties);

    OntDisjoint.DataProperties createDisjointDataProperties(Stream<OntNDP> properties);

    /**
     * =====================
     * Creation Data Ranges:
     * =====================
     */

    OntDR.OneOf createOneOfDataRange(Stream<Literal> values);

    OntDR.Restriction createRestrictionDataRange(OntDR property, Stream<OntFR> values);

    OntDR.ComplementOf createComplementOfDataRange(OntDR other);

    OntDR.UnionOf createUnionOfDataRange(Stream<OntDR> values);

    OntDR.IntersectionOf createIntersectionOfDataRange(Stream<OntDR> values);

    /**
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

    OntCE.UnionOf createUnionOf(Stream<OntCE> classes);

    OntCE.IntersectionOf createIntersectionOf(Stream<OntCE> classes);

    OntCE.OneOf createOneOf(Stream<OntCE> classes);

    OntCE.HasSelf createHasSelf(OntOPE onProperty);

    OntCE.NaryDataAllValuesFrom createDataAllValuesFrom(Stream<OntNDP> onProperties, OntDR other);

    OntCE.NaryDataSomeValuesFrom createDataSomeValuesFrom(Stream<OntNDP> onProperties, OntDR other);

    /**
     * ===================================
     * SWRL Objects (Variable, Atoms, Imp)
     * ===================================
     */

    OntSWRL.Variable createSWRLVariable(String uri);

    OntSWRL.Atom.BuiltIn createBuiltInSWRLAtom(Resource predicate, Stream<OntSWRL.DArg> arguments);

    OntSWRL.Atom.OwlClass createClassSWRLAtom(OntCE clazz, OntSWRL.IArg arg);

    OntSWRL.Atom.DataRange createDataRangeSWRLAtom(OntDR range, OntSWRL.DArg arg);

    OntSWRL.Atom.DataProperty createDataPropertySWRLAtom(OntNDP dataProperty, OntSWRL.DArg arg);

    OntSWRL.Atom.ObjectProperty createObjectPropertySWRLAtom(OntOPE dataProperty, OntSWRL.IArg firstArg, OntSWRL.IArg secondArg);

    OntSWRL.Atom.DifferentIndividuals createDifferentIndividualsSWRLAtom(OntSWRL.IArg firstArg, OntSWRL.IArg secondArg);

    OntSWRL.Atom.SameIndividuals createSameIndividualsSWRLAtom(OntSWRL.IArg firstArg, OntSWRL.IArg secondArg);

    OntSWRL.Imp createSWRLImp(Stream<OntSWRL.Atom> head, Stream<OntSWRL.Atom> body);


    /**
     * ===================================
     * default methods for simplification:
     * ===================================
     */

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

    default <T extends OntEntity> T getOntEntity(Class<T> type, Resource uri) {
        return getOntEntity(type, uri.getURI());
    }

    default OntNAP getAnnotationProperty(Resource uri) {
        return getOntEntity(OntNAP.class, uri);
    }

    /**
     * ==================
     * Built-in Entities:
     * ==================
     */

    default OntNAP getRDFSComment() {
        return getAnnotationProperty(RDFS.comment);
    }

    default OntNAP getRDFSLabel() {
        return getAnnotationProperty(RDFS.label);
    }

    default OntClass getOWLThing() {
        return getOntEntity(OntClass.class, OWL2.Thing.getURI());
    }

}
