package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
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
public interface GraphModel extends Model {

    Graph getBaseGraph();

    OntID getID();

    OntID setID(String uri);

    <T extends OntObject> Stream<T> ontObjects(Class<T> type);

    <T extends OntEntity> T getOntEntity(Class<T> type, String uri);

    <T extends OntObject> T createOntObject(Class<T> type, String uri);

    Stream<OntEntity> ontEntities();

    <T extends OntEntity> Stream<T> ontEntities(Class<T> type);

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
