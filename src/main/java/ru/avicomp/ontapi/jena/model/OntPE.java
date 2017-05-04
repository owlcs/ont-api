package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

/**
 * Common interface for any Property Expressions (DataProperty, ObjectProperty(Entity and InverseOf), AnnotationProperty).
 * See for example <a href='https://www.w3.org/TR/owl2-quick-reference/'>2.2 Properties</a>
 *
 * @see OntOPE
 * @see OntNAP
 * @see OntNDP
 * Created by @szuev on 02.11.2016.
 */
public interface OntPE extends OntObject {

    /**
     * Returns all domains
     *
     * @return Stream of {@link Resource}s
     */
    Stream<? extends Resource> domain();

    /**
     * Returns all ranges
     *
     * @return Stream of {@link Resource}s
     */
    Stream<? extends Resource> range();

    /**
     * Returns all super properties.
     *
     * @return Stream of {@link Resource}s
     */
    Stream<? extends Resource> subPropertyOf();

    /**
     * Removes specified rdfs:domain.
     *
     * @param domain {@link Resource}
     */
    default void removeDomain(Resource domain) {
        remove(RDFS.domain, domain);
    }

    /**
     * Removes specified rdfs:range
     *
     * @param range {@link Resource}
     */
    default void removeRange(Resource range) {
        remove(RDFS.range, range);
    }

    /**
     * Removes specified super property (predicate rdfs:subPropertyOf)
     *
     * @param superProperty {@link Resource}
     */
    default void removeSubPropertyOf(Resource superProperty) {
        remove(RDFS.subPropertyOf, superProperty);
    }
}
