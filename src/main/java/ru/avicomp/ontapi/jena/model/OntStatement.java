package ru.avicomp.ontapi.jena.model;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;

import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

/**
 * An Ont Statement. This is not {@link org.apache.jena.rdf.model.Resource}.
 * OWL2 Annotations could be attached to this statement recursively.
 *
 * @see OntAnnotation
 * @see Statement
 * Created by @szuev on 13.11.2016.
 */
public interface OntStatement extends Statement {

    /**
     * Returns reference to the attached model.
     *
     * @return {@link OntGraphModel}
     */
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
     * Gets attached annotations, empty stream if it is assertion annotation
     *
     * @return Stream of annotations, could be empty.
     */
    Stream<OntStatement> annotations();

    /**
     * Deletes the child annotation if present
     *
     * @param property annotation property
     * @param value    uri-resource, literal or anonymous individual
     * @throws OntJenaException in case input is incorrect.
     */
    void deleteAnnotation(OntNAP property, RDFNode value);

    /**
     * Presents the annotation statement as an annotation object if it is possible.
     * It works only for bulk annotations.
     *
     * @return Optional around of {@link OntAnnotation}.
     */
    Optional<OntAnnotation> asAnnotationResource();

    /**
     * Answers iff this statement is root
     *
     * @return true if root.
     * @see OntObject#getRoot()
     */
    boolean isRoot();

    /**
     * Answers iff this statement is in the base graph.
     *
     * @return true if local
     * @see OntObject#isLocal()
     */
    boolean isLocal();

    /**
     * An accessor method to return the subject of the statements.
     *
     * @return {@link OntObject}
     * @see Statement#getSubject()
     */
    @Override
    OntObject getSubject();

    /**
     * @return true if predicate is rdf:type
     */
    default boolean isDeclaration() {
        return RDF.type.equals(getPredicate());
    }

    /**
     * Answers iff this is an annotation assertion.
     *
     * @return true if predicate is {@link OntNAP}
     */
    default boolean isAnnotation() {
        return getPredicate().canAs(OntNAP.class);
    }

    /**
     * Answers iff this statement is a data-property assertion.
     *
     * @return true if predicate is {@link OntNDP}
     */
    default boolean isData() {
        return getPredicate().canAs(OntNDP.class);
    }

    /**
     * Answers iff this statement is an object-property assertion.
     *
     * @return true if predicate is {@link OntNOP}
     */
    default boolean isObject() {
        return getPredicate().canAs(OntNOP.class);
    }

    /**
     * Removes all sub-annotations including their children.
     */
    default void clearAnnotations() {
        Set<OntStatement> children = annotations().collect(Collectors.toSet());
        children.forEach(OntStatement::clearAnnotations);
        children.forEach(a -> deleteAnnotation(a.getPredicate().as(OntNAP.class), a.getObject()));
    }

    /**
     * Answers iff this statement has annotations attached
     *
     * @return true if it is annotated.
     */
    default boolean hasAnnotations() {
        return annotations().count() != 0;
    }
}
