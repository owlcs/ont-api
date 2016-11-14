package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

/**
 * TODO: not ready.
 * Interface for OWL2 Annotation.
 * NOTE: it is not an OntObject.
 * It is kind of accumulator for the pair of annotation property {@link OntNAP} and
 * annotation value {@link org.apache.jena.rdf.model.RDFNode}
 * (i.e. resource-uri, literal, anonymous individual ({@link ru.avicomp.ontapi.jena.model.OntIndividual.Anonymous})).
 * <p>
 * Created by @szuev on 11.11.2016.
 */
public interface OntAnnotation extends OntStatement {

    /**
     * Gets Annotation key
     *
     * @return Named Annotation Property OntObject
     */
    OntNAP getPredicate();

    /**
     * Gets Annotation Value.
     * <p>
     * could be uri-resource, literal or anonymous individual.
     *
     * @return RDFNode
     */
    RDFNode getObject();

    /**
     * Assertion annotation is a plain annotation like "@root_ont_object rdfs:comment "some comment"@fr"
     * It has no children.
     *
     * @return true if it is assertion annotation.
     */
    boolean isAssertion();

    OntAnnotation add(OntNAP property, Literal value);

    OntAnnotation add(OntNAP property, OntIndividual.Anonymous value);

    OntAnnotation add(OntNAP property, Resource value);

    void attach(OntAnnotation other);

    /**
     * removes the child annotation
     *
     * @param property annotation property
     * @param value    uri-resource, literal or anonymous individual
     * @return null if there is no such child annotations, otherwise instance of removed annotation.
     */
    OntAnnotation removeAnnotation(OntNAP property, RDFNode value);

    /**
     * removes all aub-annotations including its children.
     */
    void removeAll();

    /**
     * gets children, empty stream if it is assertion annotation
     *
     * @return Stream of annotations, could be empty.
     */
    Stream<OntAnnotation> annotations();

    default OntAnnotation removeAnnotation(OntAnnotation annotation) {
        return removeAnnotation(annotation.getPredicate(), annotation.getObject());
    }

}
