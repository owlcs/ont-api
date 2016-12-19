package ru.avicomp.ontapi.jena.model;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;

import ru.avicomp.ontapi.jena.OntJenaException;

/**
 * Ont-statement
 * OWL2 Annotations could be attached to this statement recursively.
 * <p>
 * Created by @szuev on 13.11.2016.
 */
public interface OntStatement extends Statement {

    OntGraphModel getModel();

    /**
     * adds annotation.
     *
     * @param property Named annotation property.
     * @param value    RDFNode (uri-resource, literal or anonymous individual)
     * @return OntStatement for newly added annotation.
     * @throws OntJenaException in case input is incorrect.
     */
    OntStatement addAnnotation(OntNAP property, RDFNode value);

    /**
     * gets attached annotations, empty stream if it is assertion annotation
     *
     * @return Stream of annotations, could be empty.
     */
    Stream<OntStatement> annotations();

    /**
     * deletes the child annotation if present
     *
     * @param property annotation property
     * @param value    uri-resource, literal or anonymous individual
     * @throws OntJenaException in case input is incorrect.
     */
    void deleteAnnotation(OntNAP property, RDFNode value);

    boolean isLocal();

    default boolean isAnnotation() {
        return getPredicate().canAs(OntNAP.class);
    }

    default boolean isData() {
        return getPredicate().canAs(OntNDP.class);
    }

    default boolean isObject() {
        return getPredicate().canAs(OntNOP.class);
    }

    /**
     * removes all sub-annotations including their children.
     */
    default void clearAnnotations() {
        Set<OntStatement> children = annotations().collect(Collectors.toSet());
        children.forEach(OntStatement::clearAnnotations);
        children.forEach(a -> deleteAnnotation(a.getPredicate().as(OntNAP.class), a.getObject()));
    }

    default boolean hasAnnotations() {
        return annotations().count() != 0;
    }
}
